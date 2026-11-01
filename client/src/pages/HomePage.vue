<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { RouterLink } from "vue-router";

import CharacterAvatar from "../components/CharacterAvatar.vue";
import { messageFor } from "../lib/errors";
import { formatNumber } from "../lib/format";
import { nextCountLabel } from "../lib/periods";
import { api } from "../lib/http";
import type { RankResponse } from "../lib/types";
import { useAuthStore } from "../stores/auth";

const auth = useAuthStore();
const ranking = ref<RankResponse | null>(null);
const loading = ref(true);
const error = ref("");

const top = computed(() => ranking.value?.items.slice(0, 3) ?? []);
const shortlist = computed(() => ranking.value?.items.slice(0, 5) ?? []);

async function loadRank(): Promise<void> {
  loading.value = true;
  error.value = "";
  try {
    ranking.value = await api.get<RankResponse>("/rank?limit=5");
  } catch (caught) {
    error.value = messageFor(caught);
  } finally {
    loading.value = false;
  }
}

onMounted(loadRank);
</script>

<template>
  <section class="halftone-light border-b-[3px] border-ink bg-sky text-paper">
    <div class="page-shell grid gap-10 py-12 lg:grid-cols-[1.1fr_0.9fr] lg:py-16">
      <div>
        <p class="eyebrow text-paper/80">Ranking de fãs da Turma da Mônica</p>
        <h1 class="mt-3 max-w-[16ch] text-[clamp(2.6rem,6vw,4.4rem)]">
          Quem é o personagem mais amado do Bairro do Limoeiro?
        </h1>
        <p class="mt-5 max-w-xl text-lg text-paper/90">
          Monte seu top 10, do favorito ao décimo lugar. Todo mês, os votos viram um ranking
          congelado para você acompanhar quem subiu, quem caiu e quem nunca sai do coração.
        </p>

        <div class="mt-8 flex flex-wrap gap-3">
          <template v-if="auth.isAuthenticated">
            <RouterLink v-if="!auth.emailConfirmed" to="/conta" class="btn bg-sun text-ink">
              Confirmar meu e-mail
            </RouterLink>
            <RouterLink v-else to="/votar" class="btn bg-paper text-ink">
              Montar meu top 10
            </RouterLink>
            <RouterLink
              to="/conta"
              class="btn border-paper bg-transparent text-paper hover:bg-paper hover:text-ink"
            >
              Minha conta
            </RouterLink>
          </template>
          <template v-else>
            <RouterLink to="/criar-conta" class="btn bg-paper text-ink"> Criar conta </RouterLink>
            <RouterLink
              to="/entrar"
              class="btn border-paper bg-transparent text-paper hover:bg-paper hover:text-ink"
            >
              Entrar
            </RouterLink>
          </template>
        </div>

        <p v-if="ranking" class="mt-5 text-sm text-paper/80">
          {{ nextCountLabel(ranking.nextCutoffAt) }} ·
          {{ formatNumber(ranking.totalBallots) }} votos confirmados
        </p>
        <p v-else-if="loading" class="mt-5 text-sm text-paper/80">Carregando o rank…</p>
      </div>

      <div class="grid content-center gap-3">
        <template v-if="top.length">
          <RouterLink
            v-for="entry in top"
            :key="entry.character.id"
            :to="`/personagens/${entry.character.slug}`"
            class="panel p-4 text-ink transition motion-safe:hover:-translate-y-1"
            :class="entry.rank === 1 ? 'col-span-2 -rotate-1 p-5' : 'rotate-1'"
          >
            <div class="flex items-center gap-3">
              <span
                class="font-display leading-none"
                :class="entry.rank === 1 ? 'text-5xl text-cherry' : 'text-3xl text-sky'"
              >
                {{ entry.rank }}º
              </span>
              <CharacterAvatar
                :character="entry.character"
                :size="entry.rank === 1 ? 'lg' : 'md'"
              />
              <div class="min-w-0">
                <p class="truncate font-display text-xl leading-tight">
                  {{ entry.character.name }}
                </p>
                <p class="text-sm text-ink/65">{{ formatNumber(entry.points) }} pontos</p>
              </div>
            </div>
          </RouterLink>
        </template>

        <div v-else-if="loading" class="panel p-6 text-ink" role="status">
          <p class="font-display text-2xl">Contando os votos…</p>
          <p class="mt-2 text-sm text-ink/65">Já já o pódio aparece aqui.</p>
        </div>

        <div v-else class="space-y-3">
          <div class="panel -rotate-1 p-5 text-ink">
            <p class="font-display text-5xl leading-none text-cherry">1º</p>
            <p class="mt-2 font-display text-2xl">O favorito da galera</p>
            <p class="mt-1 text-sm text-ink/65">
              {{ error || "Aguardando o primeiro voto." }}
            </p>
            <button v-if="error" type="button" class="btn btn-secondary mt-4" @click="loadRank">
              Tentar de novo
            </button>
          </div>
          <div class="grid grid-cols-2 gap-3 text-ink">
            <div class="panel rotate-1 p-4">
              <p class="font-display text-3xl leading-none text-sky">2º</p>
              <p class="mt-1 text-sm font-semibold">A disputa está aberta</p>
            </div>
            <div class="panel -rotate-1 p-4">
              <p class="font-display text-3xl leading-none text-leaf">3º</p>
              <p class="mt-1 text-sm font-semibold">Seu voto decide</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </section>

  <section class="page-shell py-12 lg:py-16">
    <div class="flex flex-wrap items-end justify-between gap-4">
      <div>
        <p class="eyebrow">Rank agora</p>
        <h2 class="mt-2 text-3xl sm:text-4xl">Quem está no topo neste momento.</h2>
      </div>
      <RouterLink to="/ranking" class="font-semibold underline underline-offset-4">
        Ver ranking completo
      </RouterLink>
    </div>

    <ol v-if="shortlist.length" class="mt-8 divide-y-2 divide-ink/10 border-y-2 border-ink/10">
      <li v-for="entry in shortlist" :key="entry.character.id">
        <RouterLink
          :to="`/personagens/${entry.character.slug}`"
          class="grid grid-cols-[2.5rem_1fr_auto] items-center gap-3 py-3 transition hover:bg-sun/20 sm:grid-cols-[3rem_1fr_8rem_auto]"
        >
          <span class="font-display text-2xl text-cherry">{{ entry.rank }}º</span>
          <span class="flex min-w-0 items-center gap-3">
            <CharacterAvatar :character="entry.character" size="md" />
            <span class="truncate font-bold">{{ entry.character.name }}</span>
          </span>
          <span class="hidden text-sm text-ink/65 sm:block">
            {{ formatNumber(entry.firstPlaceVotes) }} primeiros lugares
          </span>
          <span class="text-right font-display text-xl">
            {{ formatNumber(entry.points) }} <span class="text-sm font-sans font-normal">pts</span>
          </span>
        </RouterLink>
      </li>
    </ol>

    <div v-else-if="loading" class="mt-8 space-y-3" role="status">
      <div class="h-14 animate-pulse rounded-lg bg-ink/10"></div>
      <div class="h-14 animate-pulse rounded-lg bg-ink/10"></div>
      <div class="h-14 animate-pulse rounded-lg bg-ink/10"></div>
    </div>

    <div v-else class="panel mt-8 p-6 sm:p-10">
      <h2 class="text-3xl">O primeiro rank ainda está em branco.</h2>
      <p class="mt-3 max-w-2xl text-ink/75">
        A votação está aberta. Crie sua conta, confirme o e-mail e monte seu top 10 para escrever o
        primeiro ranking da turma.
      </p>
      <div class="mt-6 flex flex-wrap gap-3">
        <RouterLink :to="auth.isAuthenticated ? '/votar' : '/criar-conta'" class="btn btn-primary">
          {{ auth.isAuthenticated ? "Montar meu top 10" : "Criar conta para votar" }}
        </RouterLink>
        <RouterLink to="/personagens" class="btn btn-secondary">
          Conhecer os personagens
        </RouterLink>
      </div>
      <p v-if="error" class="mt-4 text-sm font-semibold text-cherry" role="alert">
        {{ error }}
        <button type="button" class="underline" @click="loadRank">Tentar de novo</button>
      </p>
    </div>
  </section>

  <section class="page-shell py-12 lg:py-16">
    <div class="flex flex-wrap items-end justify-between gap-4">
      <div>
        <p class="eyebrow">Como funciona</p>
        <h2 class="mt-2 text-3xl sm:text-4xl">Três passos e o seu gibi está votado.</h2>
      </div>
      <RouterLink to="/sobre" class="font-semibold underline underline-offset-4">
        Entenda as regras
      </RouterLink>
    </div>

    <ol class="mt-8 grid gap-4 md:grid-cols-3">
      <li class="panel p-6">
        <span
          class="flex size-10 items-center justify-center rounded-full border-2 border-ink bg-sun font-display text-xl font-extrabold"
        >
          1
        </span>
        <h3 class="mt-4 text-xl">Crie sua conta</h3>
        <p class="mt-2 text-ink/75">E-mail e senha. Nada de rede social, nada de besteira.</p>
      </li>
      <li class="panel p-6">
        <span
          class="flex size-10 items-center justify-center rounded-full border-2 border-ink bg-sun font-display text-xl font-extrabold"
        >
          2
        </span>
        <h3 class="mt-4 text-xl">Confirme o e-mail</h3>
        <p class="mt-2 text-ink/75">
          Um voto por pessoa de verdade. O link chega na sua caixa de entrada.
        </p>
      </li>
      <li class="panel p-6">
        <span
          class="flex size-10 items-center justify-center rounded-full border-2 border-ink bg-sun font-display text-xl font-extrabold"
        >
          3
        </span>
        <h3 class="mt-4 text-xl">Monte seu top 10</h3>
        <p class="mt-2 text-ink/75">
          Ordene os personagens e mude de ideia quando quiser. A próxima contagem registra tudo.
        </p>
      </li>
    </ol>
  </section>
</template>
