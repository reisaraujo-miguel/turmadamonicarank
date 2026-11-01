<script setup lang="ts">
import { onMounted, ref } from "vue";
import { RouterLink } from "vue-router";

import CharacterAvatar from "../components/CharacterAvatar.vue";
import { messageFor } from "../lib/errors";
import { formatDate, formatNumber } from "../lib/format";
import { nextCountLabel } from "../lib/periods";
import { api } from "../lib/http";
import type { RankEntry, RankResponse } from "../lib/types";
import { useAuthStore } from "../stores/auth";

const auth = useAuthStore();
const ranking = ref<RankResponse | null>(null);
const loading = ref(true);
const error = ref("");

async function loadRank(): Promise<void> {
  loading.value = true;
  error.value = "";
  try {
    ranking.value = await api.get<RankResponse>("/rank?limit=100");
  } catch (caught) {
    error.value = messageFor(caught);
  } finally {
    loading.value = false;
  }
}

function position(entry: RankEntry): string {
  return `${entry.rank}º`;
}

onMounted(loadRank);
</script>

<template>
  <section class="halftone-light border-b-[3px] border-ink bg-sky py-10 text-paper">
    <div class="page-shell">
      <p class="eyebrow text-paper/80">Agora</p>
      <h1 class="mt-2 text-4xl sm:text-5xl">Ranking</h1>
      <p class="mt-3 max-w-2xl text-paper/85">
        O rank ao vivo dos votos confirmados. As contagens mensais entram no histórico assim que o
        primeiro mês fechar.
      </p>
      <p v-if="ranking" class="mt-4 text-sm text-paper/75">
        Atualizado em {{ formatDate(ranking.generatedAt) }} ·
        {{ formatNumber(ranking.totalBallots) }} votos ·
        {{ nextCountLabel(ranking.nextCutoffAt) }}
      </p>
    </div>
  </section>

  <section class="page-shell py-10 lg:py-14">
    <div class="flex flex-wrap items-end justify-between gap-4">
      <div>
        <p class="eyebrow">Top {{ ranking?.total ?? "—" }}</p>
        <h2 class="mt-2 text-3xl">Quem os fãs estão colocando no topo.</h2>
      </div>
      <div class="flex flex-wrap gap-3">
        <RouterLink :to="auth.isAuthenticated ? '/votar' : '/criar-conta'" class="btn btn-primary">
          {{ auth.isAuthenticated ? "Montar meu top 10" : "Criar conta para votar" }}
        </RouterLink>
        <RouterLink to="/personagens" class="btn btn-secondary">Personagens</RouterLink>
      </div>
    </div>

    <div v-if="loading" class="mt-8 space-y-3" role="status">
      <div v-for="item in 6" :key="item" class="h-20 animate-pulse rounded-lg bg-ink/10"></div>
      <p class="sr-only">Carregando ranking…</p>
    </div>

    <div v-else-if="error" class="panel mt-8 p-6 sm:p-10" role="alert">
      <h2 class="text-2xl">Não conseguimos carregar o rank.</h2>
      <p class="mt-2 text-ink/70">{{ error }}</p>
      <button type="button" class="btn btn-primary mt-5" @click="loadRank">Tentar de novo</button>
    </div>

    <div v-else-if="!ranking?.items.length" class="panel mt-8 p-6 sm:p-10">
      <h2 class="text-3xl">O primeiro rank ainda está em branco.</h2>
      <p class="mt-3 max-w-2xl text-ink/75">
        Assim que alguém confirmar o e-mail e salvar o primeiro top 10, o podium começa a aparecer
        aqui.
      </p>
      <RouterLink
        :to="auth.isAuthenticated ? '/votar' : '/criar-conta'"
        class="btn btn-primary mt-6"
      >
        {{ auth.isAuthenticated ? "Montar meu top 10" : "Criar conta para votar" }}
      </RouterLink>
    </div>

    <ol v-else class="mt-8 divide-y-2 divide-ink/10 border-y-2 border-ink/10">
      <li v-for="entry in ranking.items" :key="entry.character.id">
        <div
          class="grid grid-cols-[2.5rem_1fr_auto] items-center gap-3 py-4 sm:grid-cols-[3.5rem_1fr_9rem_7rem_auto]"
        >
          <RouterLink
            :to="`/personagens/${entry.character.slug}`"
            class="font-display text-2xl leading-none text-cherry transition hover:text-ink sm:text-3xl"
            :aria-label="`Ver ${entry.character.name}, ${position(entry)} no ranking`"
          >
            {{ entry.rank }}º
          </RouterLink>
          <RouterLink
            :to="`/personagens/${entry.character.slug}`"
            class="flex min-w-0 items-center gap-3"
          >
            <CharacterAvatar :character="entry.character" :size="entry.rank <= 3 ? 'lg' : 'md'" />
            <span class="truncate font-display text-xl sm:text-2xl">
              {{ entry.character.name }}
            </span>
          </RouterLink>
          <span class="hidden text-sm text-ink/65 sm:block">
            {{ formatNumber(entry.firstPlaceVotes) }}
            {{ entry.firstPlaceVotes === 1 ? "primeiro lugar" : "primeiros lugares" }}
          </span>
          <span class="hidden text-sm text-ink/65 sm:block">
            {{ formatNumber(entry.ballotCount) }}
            {{ entry.ballotCount === 1 ? "voto" : "votos" }}
          </span>
          <span class="text-right">
            <span class="block font-display text-2xl leading-none">
              {{ formatNumber(entry.points) }}
            </span>
            <span class="text-xs text-ink/55">pontos</span>
          </span>
        </div>
      </li>
    </ol>

    <p class="mt-6 text-sm text-ink/60">
      O rank ao vivo pode mudar até a próxima contagem; a partir dela, o resultado do mês vira
      história.
    </p>
  </section>
</template>
