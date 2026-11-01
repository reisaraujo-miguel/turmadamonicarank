<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { RouterLink } from "vue-router";

import CharacterAvatar from "../components/CharacterAvatar.vue";
import { messageFor } from "../lib/errors";
import { formatNumber } from "../lib/format";
import { api } from "../lib/http";
import type { CharacterSummary, PageResponse } from "../lib/types";

const characters = ref<CharacterSummary[]>([]);
const total = ref(0);
const query = ref("");
const loading = ref(true);
const error = ref("");
const searchHint = computed(() => {
  const length = query.value.trim().length;
  return length > 0 && length < 3 ? "Digite pelo menos 3 letras para buscar." : "";
});

async function loadCharacters(): Promise<void> {
  const term = query.value.trim();
  if (term.length > 0 && term.length < 3) {
    return;
  }
  loading.value = true;
  error.value = "";
  try {
    const params = new URLSearchParams({ limit: "100" });
    if (term) {
      params.set("q", term);
    }
    const page = await api.get<PageResponse<CharacterSummary>>(`/characters?${params.toString()}`);
    characters.value = page.items;
    total.value = page.total;
  } catch (caught) {
    error.value = messageFor(caught);
  } finally {
    loading.value = false;
  }
}

function submitSearch(): void {
  if (searchHint.value) {
    return;
  }
  void loadCharacters();
}

function clearSearch(): void {
  query.value = "";
  void loadCharacters();
}

onMounted(loadCharacters);
</script>

<template>
  <section class="halftone-light border-b-[3px] border-ink bg-sky py-10 text-paper">
    <div class="page-shell">
      <p class="eyebrow text-paper/80">Personagens</p>
      <h1 class="mt-2 text-4xl sm:text-5xl">O elenco do Bairro do Limoeiro.</h1>
      <p class="mt-3 max-w-2xl text-paper/85">
        Bios curtas e, em breve, o histórico de posições de cada personagem nas contagens mensais.
      </p>
    </div>
  </section>

  <section class="page-shell py-10 lg:py-14">
    <form
      class="flex max-w-2xl flex-wrap items-end gap-3"
      role="search"
      @submit.prevent="submitSearch"
    >
      <div class="min-w-0 flex-1">
        <label for="character-search" class="label">Buscar personagem</label>
        <input
          id="character-search"
          v-model="query"
          class="field"
          type="search"
          placeholder="Mônica, Cebolinha, Magali…"
          autocomplete="off"
          enterkeyhint="search"
          aria-describedby="character-search-hint"
        />
        <p id="character-search-hint" class="hint">
          {{ searchHint || "Sem busca, mostramos todo o elenco." }}
        </p>
      </div>
      <button type="submit" class="btn btn-primary" :disabled="loading || !!searchHint">
        Buscar
      </button>
    </form>

    <div class="mt-6 flex flex-wrap items-center justify-between gap-3">
      <p class="text-sm text-ink/65" role="status" aria-live="polite">
        {{ loading ? "Carregando personagens…" : `${formatNumber(total)} personagens` }}
      </p>
      <button
        v-if="query"
        type="button"
        class="text-sm font-semibold underline underline-offset-4"
        @click="clearSearch"
      >
        Limpar busca
      </button>
    </div>

    <div v-if="loading" class="mt-6 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
      <div v-for="item in 6" :key="item" class="h-44 animate-pulse rounded-panel bg-ink/10"></div>
    </div>

    <div v-else-if="error" class="panel mt-6 p-6 sm:p-8" role="alert">
      <h2 class="text-2xl">Não conseguimos carregar os personagens.</h2>
      <p class="mt-2 text-ink/70">{{ error }}</p>
      <button type="button" class="btn btn-primary mt-5" @click="loadCharacters">
        Tentar de novo
      </button>
    </div>

    <div v-else-if="!characters.length" class="panel mt-6 p-6 sm:p-8">
      <h2 class="text-2xl">Nenhum personagem encontrado.</h2>
      <p class="mt-2 text-ink/70">Tente outro nome ou volte para o elenco completo.</p>
      <button type="button" class="btn btn-secondary mt-5" @click="clearSearch">Ver todos</button>
    </div>

    <ul v-else class="mt-6 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
      <li v-for="character in characters" :key="character.id">
        <RouterLink
          :to="`/personagens/${character.slug}`"
          class="panel flex h-full items-center gap-4 p-4 transition motion-safe:hover:-translate-y-1"
        >
          <CharacterAvatar :character="character" size="xl" />
          <div class="min-w-0">
            <h2 class="truncate text-2xl">{{ character.name }}</h2>
            <p
              v-if="character.status === 'INACTIVE'"
              class="mt-2 text-sm font-semibold text-cherry"
            >
              Fora do rank
            </p>
            <p v-else class="mt-2 text-sm text-ink/65">Ver ficha e posição</p>
          </div>
        </RouterLink>
      </li>
    </ul>
  </section>
</template>
