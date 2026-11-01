<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { RouterLink } from "vue-router";

import CharacterAvatar from "../components/CharacterAvatar.vue";
import { isApiError, messageFor } from "../lib/errors";
import { api } from "../lib/http";
import type { CharacterDetail } from "../lib/types";
import { useAuthStore } from "../stores/auth";

const props = defineProps<{ slug: string }>();
const auth = useAuthStore();

const character = ref<CharacterDetail | null>(null);
const loading = ref(true);
const notFound = ref(false);
const error = ref("");

const rankLabel = computed(() => {
  if (!character.value || character.value.status === "INACTIVE") {
    return "Fora do rank";
  }
  return character.value.currentRank
    ? `${character.value.currentRank}º no rank`
    : "Sem posição ainda";
});

const appearance = computed(() => {
  const first = character.value?.firstAppearance;
  if (!first) {
    return null;
  }
  if (first.year && first.work) {
    return `${first.work}, ${first.year}`;
  }
  return first.year ? String(first.year) : first.work;
});

async function loadCharacter(): Promise<void> {
  loading.value = true;
  notFound.value = false;
  error.value = "";
  try {
    character.value = await api.get<CharacterDetail>(
      `/characters/${encodeURIComponent(props.slug)}`,
    );
    document.title = `${character.value.name} — Turma da Mônica Rank`;
  } catch (caught) {
    character.value = null;
    if (isApiError(caught) && caught.status === 404) {
      notFound.value = true;
    } else {
      error.value = messageFor(caught);
    }
  } finally {
    loading.value = false;
  }
}

watch(() => props.slug, loadCharacter, { immediate: true });
</script>

<template>
  <section class="border-b-[3px] border-ink bg-sky py-8 text-paper">
    <div class="page-shell flex flex-wrap items-center justify-between gap-3">
      <RouterLink to="/personagens" class="text-sm font-semibold underline underline-offset-4">
        ← Todos os personagens
      </RouterLink>
      <span class="text-sm text-paper/75">Ficha do personagem</span>
    </div>
  </section>

  <section class="page-shell py-10 lg:py-14">
    <div v-if="loading" class="grid gap-6 lg:grid-cols-[18rem_1fr]">
      <div class="h-72 animate-pulse rounded-panel bg-ink/10"></div>
      <div class="space-y-4">
        <div class="h-12 w-48 animate-pulse rounded bg-ink/10"></div>
        <div class="h-24 animate-pulse rounded bg-ink/10"></div>
      </div>
      <p class="sr-only" role="status">Carregando personagem…</p>
    </div>

    <div v-else-if="notFound" class="panel mx-auto max-w-2xl p-8 text-center">
      <p class="font-display text-6xl leading-none text-cherry">404</p>
      <h1 class="mt-3 text-3xl">Personagem não encontrado.</h1>
      <p class="mt-3 text-ink/75">
        O endereço pode ter mudado ou o personagem ainda não entrou no catálogo.
      </p>
      <RouterLink to="/personagens" class="btn btn-primary mt-6"> Voltar para o elenco </RouterLink>
    </div>

    <div v-else-if="error" class="panel mx-auto max-w-2xl p-8">
      <h1 class="text-3xl">Não conseguimos carregar esta ficha.</h1>
      <p class="mt-3 text-ink/70">{{ error }}</p>
      <button type="button" class="btn btn-primary mt-5" @click="loadCharacter">
        Tentar de novo
      </button>
    </div>

    <template v-else-if="character">
      <div class="grid gap-8 lg:grid-cols-[18rem_1fr]">
        <div class="space-y-4">
          <CharacterAvatar :character="character" size="xl" />
          <div class="panel p-5">
            <p class="text-sm font-semibold text-ink/60">Posição atual</p>
            <p class="mt-1 font-display text-3xl">{{ rankLabel }}</p>
            <p
              v-if="character.status === 'INACTIVE'"
              class="mt-2 text-sm font-semibold text-cherry"
            >
              Este personagem está fora das novas votações.
            </p>
            <RouterLink
              :to="auth.isAuthenticated ? '/votar' : '/criar-conta'"
              class="btn btn-primary mt-5 w-full"
            >
              {{ auth.isAuthenticated ? "Montar meu top 10" : "Criar conta para votar" }}
            </RouterLink>
          </div>
          <p v-if="character.imageCredit" class="text-xs text-ink/55">
            {{ character.imageCredit }}
          </p>
        </div>

        <div>
          <p class="eyebrow">Personagem</p>
          <h1 class="mt-2 text-4xl sm:text-5xl">{{ character.name }}</h1>
          <p v-if="character.fullName" class="mt-2 text-lg text-ink/70">
            {{ character.fullName }}
          </p>

          <div class="mt-5 flex flex-wrap gap-2">
            <span
              v-if="character.status === 'ACTIVE'"
              class="rounded-full border-2 border-ink bg-leaf/20 px-3 py-1 text-sm font-bold"
            >
              No rank
            </span>
            <span
              v-else
              class="rounded-full border-2 border-ink bg-ink/10 px-3 py-1 text-sm font-bold"
            >
              Fora do rank
            </span>
            <span
              v-if="character.currentRank"
              class="rounded-full border-2 border-ink bg-sun px-3 py-1 text-sm font-bold"
            >
              {{ character.currentRank }}º agora
            </span>
          </div>

          <div class="panel mt-6 p-6 sm:p-8">
            <h2 class="text-2xl">Quem é</h2>
            <p class="mt-3 max-w-prose whitespace-pre-line text-ink/80">
              {{ character.bio || "A bio deste personagem ainda está sendo preparada." }}
            </p>

            <dl class="mt-6 grid gap-4 sm:grid-cols-2">
              <div v-if="character.aliases.length">
                <dt class="text-sm font-semibold text-ink/60">Também conhecido por</dt>
                <dd class="mt-1">{{ character.aliases.join(", ") }}</dd>
              </div>
              <div v-if="appearance">
                <dt class="text-sm font-semibold text-ink/60">Primeira aparição</dt>
                <dd class="mt-1">{{ appearance }}</dd>
              </div>
              <div v-if="character.sourceUrl">
                <dt class="text-sm font-semibold text-ink/60">Fonte</dt>
                <dd class="mt-1">
                  <a
                    :href="character.sourceUrl"
                    class="font-semibold underline underline-offset-4"
                    target="_blank"
                    rel="noopener noreferrer"
                  >
                    Ver referência
                  </a>
                </dd>
              </div>
            </dl>
          </div>

          <div class="panel mt-6 p-6 sm:p-8">
            <p class="eyebrow">Histórico</p>
            <h2 class="mt-2 text-2xl">Posição ao longo do tempo</h2>
            <p class="mt-3 max-w-prose text-ink/75">
              O gráfico de posições aparece quando a primeira contagem mensal fechar. Até lá, o rank
              acima é o retrato do momento.
            </p>
            <RouterLink to="/ranking" class="btn btn-secondary mt-5">
              Ver o ranking de agora
            </RouterLink>
          </div>
        </div>
      </div>
    </template>
  </section>
</template>
