<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from "vue";
import { RouterLink, onBeforeRouteLeave } from "vue-router";

import CharacterAvatar from "../components/CharacterAvatar.vue";
import { messageFor } from "../lib/errors";
import { formatDate } from "../lib/format";
import { api } from "../lib/http";
import type { Ballot, BallotEnvelope, CharacterSummary, PageResponse } from "../lib/types";

const entries = ref<CharacterSummary[]>([]);
const updatedAt = ref<string | null>(null);
const savedIds = ref<string[]>([]);
const loading = ref(true);
const saving = ref(false);
const withdrawing = ref(false);
const error = ref("");
const notice = ref("");

const searchQuery = ref("");
const results = ref<CharacterSummary[]>([]);
const searching = ref(false);
const searchError = ref("");

const hasInactive = computed(() => entries.value.some((item) => item.status === "INACTIVE"));
const atMax = computed(() => entries.value.length >= 10);
const canSave = computed(() => entries.value.length > 0 && !hasInactive.value && !saving.value);
const dirty = computed(
  () => entries.value.map((item) => item.id).join("|") !== savedIds.value.join("|"),
);

async function loadBallot(): Promise<void> {
  loading.value = true;
  error.value = "";
  try {
    const response = await api.get<BallotEnvelope>("/ballot");
    entries.value = response.ballot?.entries.map((entry) => entry.character) ?? [];
    savedIds.value = entries.value.map((character) => character.id);
    updatedAt.value = response.ballot?.updatedAt ?? null;
  } catch (caught) {
    error.value = messageFor(caught);
  } finally {
    loading.value = false;
  }
}

async function searchCharacters(): Promise<void> {
  const term = searchQuery.value.trim();
  searchError.value = "";
  if (term.length < 3) {
    results.value = [];
    searchError.value = "Digite pelo menos 3 letras para buscar.";
    return;
  }
  searching.value = true;
  try {
    const page = await api.get<PageResponse<CharacterSummary>>(
      `/characters?q=${encodeURIComponent(term)}&limit=20`,
    );
    const selected = new Set(entries.value.map((item) => item.id));
    results.value = page.items.filter(
      (character) => character.status === "ACTIVE" && !selected.has(character.id),
    );
    if (!results.value.length) {
      searchError.value = "Nenhum personagem ativo encontrado com esse nome.";
    }
  } catch (caught) {
    results.value = [];
    searchError.value = messageFor(caught);
  } finally {
    searching.value = false;
  }
}

function addCharacter(character: CharacterSummary): void {
  notice.value = "";
  if (atMax.value || entries.value.some((item) => item.id === character.id)) {
    return;
  }
  entries.value = [...entries.value, character];
  results.value = results.value.filter((item) => item.id !== character.id);
}

function removeCharacter(index: number): void {
  notice.value = "";
  entries.value = entries.value.filter((_, itemIndex) => itemIndex !== index);
}

function move(index: number, direction: -1 | 1): void {
  const target = index + direction;
  if (target < 0 || target >= entries.value.length) {
    return;
  }
  const next = [...entries.value];
  [next[index], next[target]] = [next[target], next[index]];
  entries.value = next;
  notice.value = "";
}

async function saveBallot(): Promise<void> {
  error.value = "";
  notice.value = "";
  if (!entries.value.length) {
    error.value = "Escolha pelo menos um personagem antes de salvar.";
    return;
  }
  if (hasInactive.value) {
    error.value = "Remova os personagens que estão fora do rank antes de salvar.";
    return;
  }
  saving.value = true;
  try {
    const ballot = await api.put<Ballot>("/ballot", {
      characterIds: entries.value.map((character) => character.id),
    });
    entries.value = ballot.entries.map((entry) => entry.character);
    savedIds.value = entries.value.map((character) => character.id);
    updatedAt.value = ballot.updatedAt;
    notice.value = "Voto salvo. Ele vale a partir da próxima contagem.";
  } catch (caught) {
    error.value = messageFor(caught);
  } finally {
    saving.value = false;
  }
}

async function withdrawBallot(): Promise<void> {
  if (!window.confirm("Retirar seu voto? Seus snapshots anteriores continuam intactos.")) {
    return;
  }
  withdrawing.value = true;
  error.value = "";
  notice.value = "";
  try {
    await api.delete<void>("/ballot");
    entries.value = [];
    savedIds.value = [];
    updatedAt.value = null;
    notice.value = "Voto retirado. Você pode montar outro quando quiser.";
  } catch (caught) {
    error.value = messageFor(caught);
  } finally {
    withdrawing.value = false;
  }
}

function handleBeforeUnload(event: BeforeUnloadEvent): void {
  if (!dirty.value) {
    return;
  }
  event.preventDefault();
  event.returnValue = "";
}

onBeforeRouteLeave(() => {
  if (!dirty.value) {
    return true;
  }
  return window.confirm("Você tem alterações não salvas no seu top 10. Sair mesmo assim?");
});

onMounted(() => {
  window.addEventListener("beforeunload", handleBeforeUnload);
  void loadBallot();
});

onUnmounted(() => {
  window.removeEventListener("beforeunload", handleBeforeUnload);
});
</script>

<template>
  <section class="border-b-[3px] border-ink bg-sky py-10 text-paper">
    <div class="page-shell flex flex-wrap items-end justify-between gap-4">
      <div>
        <p class="eyebrow text-paper/80">Seu voto</p>
        <h1 class="mt-2 text-4xl sm:text-5xl">Meu top 10</h1>
        <p class="mt-3 max-w-2xl text-paper/85">
          Escolha até 10 personagens e ordene do favorito ao décimo. Você pode mudar de ideia quando
          quiser.
        </p>
      </div>
      <RouterLink to="/ranking" class="font-semibold underline underline-offset-4">
        Ver o ranking
      </RouterLink>
    </div>
  </section>

  <section class="page-shell py-10 lg:py-14">
    <div v-if="loading" class="space-y-3" role="status">
      <div v-for="item in 4" :key="item" class="h-20 animate-pulse rounded-panel bg-ink/10"></div>
      <p class="sr-only">Carregando seu voto…</p>
    </div>

    <div v-else-if="error && !entries.length" class="panel p-6 sm:p-8" role="alert">
      <h2 class="text-2xl">Não conseguimos carregar seu voto.</h2>
      <p class="mt-2 text-ink/70">{{ error }}</p>
      <button type="button" class="btn btn-primary mt-5" @click="loadBallot">Tentar de novo</button>
    </div>

    <template v-else>
      <div class="grid gap-8 lg:grid-cols-[1fr_22rem]">
        <div>
          <div class="flex flex-wrap items-center justify-between gap-3">
            <h2 class="text-2xl sm:text-3xl">Sua lista</h2>
            <p class="text-sm font-semibold text-ink/60">{{ entries.length }} de 10</p>
          </div>

          <p class="mt-2 text-sm text-ink/65">
            A posição 1 vale 10 pontos e a posição 10 vale 1. A ordem importa.
            <span v-if="dirty" class="font-semibold text-cherry"> Alterações não salvas.</span>
          </p>

          <div
            v-if="hasInactive"
            class="mt-4 rounded-lg border-2 border-cherry bg-cherry/10 p-4 text-sm"
          >
            <p class="font-bold text-cherry">Um personagem da sua lista saiu do rank.</p>
            <p class="mt-1 text-ink/75">
              Remova a linha marcada para conseguir salvar o voto novamente.
            </p>
          </div>

          <p
            v-if="notice"
            class="mt-4 rounded-lg border-2 border-ink bg-leaf/15 p-4 font-semibold"
            role="status"
          >
            {{ notice }}
          </p>
          <p v-if="error" class="mt-4 error-text" role="alert">{{ error }}</p>

          <ol v-if="entries.length" class="mt-5 space-y-3">
            <li
              v-for="(character, index) in entries"
              :key="character.id"
              class="panel flex flex-wrap items-center gap-3 p-4"
            >
              <span
                class="flex size-10 shrink-0 items-center justify-center rounded-full border-2 border-ink bg-sun font-display text-lg font-extrabold"
              >
                {{ index + 1 }}
              </span>
              <CharacterAvatar :character="character" size="md" />
              <div class="min-w-0 flex-1">
                <RouterLink
                  :to="`/personagens/${character.slug}`"
                  class="truncate font-display text-xl hover:underline"
                >
                  {{ character.name }}
                </RouterLink>
                <p v-if="character.status === 'INACTIVE'" class="text-sm font-semibold text-cherry">
                  Fora do rank
                </p>
              </div>
              <div class="flex items-center gap-1">
                <button
                  type="button"
                  class="btn-ghost px-2 text-sm disabled:opacity-40"
                  :disabled="index === 0"
                  :aria-label="`Subir ${character.name}`"
                  @click="move(index, -1)"
                >
                  ↑
                </button>
                <button
                  type="button"
                  class="btn-ghost px-2 text-sm disabled:opacity-40"
                  :disabled="index === entries.length - 1"
                  :aria-label="`Descer ${character.name}`"
                  @click="move(index, 1)"
                >
                  ↓
                </button>
                <button
                  type="button"
                  class="btn-ghost px-2 text-sm text-cherry"
                  :aria-label="`Remover ${character.name}`"
                  @click="removeCharacter(index)"
                >
                  ×
                </button>
              </div>
            </li>
          </ol>

          <div v-else class="panel mt-5 p-6">
            <h3 class="text-2xl">Seu top 10 está vazio.</h3>
            <p class="mt-2 text-ink/70">
              Escolha até 10 personagens — a ordem importa. Use a busca ao lado para começar.
            </p>
          </div>

          <div class="mt-6 flex flex-wrap gap-3">
            <button type="button" class="btn btn-primary" :disabled="!canSave" @click="saveBallot">
              {{ saving ? "Salvando…" : "Salvar voto" }}
            </button>
            <button
              v-if="entries.length"
              type="button"
              class="btn btn-secondary"
              :disabled="withdrawing"
              @click="withdrawBallot"
            >
              {{ withdrawing ? "Retirando…" : "Retirar voto" }}
            </button>
          </div>
          <p v-if="updatedAt" class="mt-3 text-sm text-ink/60">
            Último voto salvo em {{ formatDate(updatedAt) }}.
          </p>
        </div>

        <aside>
          <div class="panel p-5">
            <h2 class="text-2xl">Adicionar personagem</h2>
            <form class="mt-4" role="search" @submit.prevent="searchCharacters">
              <label for="ballot-search" class="label">Buscar no elenco</label>
              <div class="flex gap-2">
                <input
                  id="ballot-search"
                  v-model="searchQuery"
                  class="field"
                  type="search"
                  placeholder="Mônica, Magali…"
                  autocomplete="off"
                  :disabled="searching || atMax"
                  aria-describedby="ballot-search-hint"
                />
                <button type="submit" class="btn btn-secondary px-4" :disabled="searching || atMax">
                  Buscar
                </button>
              </div>
              <p id="ballot-search-hint" class="hint">
                {{ atMax ? "Sua lista já tem 10 personagens." : "Digite pelo menos 3 letras." }}
              </p>
            </form>

            <p v-if="searchError" class="mt-3 text-sm text-cherry" role="status">
              {{ searchError }}
            </p>

            <ul v-if="results.length" class="mt-4 space-y-2">
              <li v-for="character in results" :key="character.id">
                <button
                  type="button"
                  class="flex w-full items-center gap-3 rounded-lg border-2 border-ink/20 p-2 text-left hover:border-ink hover:bg-sun/20 disabled:opacity-50"
                  :disabled="atMax"
                  @click="addCharacter(character)"
                >
                  <CharacterAvatar :character="character" size="sm" />
                  <span class="min-w-0 flex-1 truncate font-semibold">{{ character.name }}</span>
                  <span class="text-sm font-bold text-cherry">Adicionar</span>
                </button>
              </li>
            </ul>

            <p class="mt-5 border-t-2 border-ink/10 pt-4 text-sm text-ink/65">
              Seu voto vale a partir da próxima contagem. Até lá, edite quantas vezes quiser.
            </p>
          </div>

          <div
            class="mt-4 rounded-panel border-2 border-dashed border-ink/30 p-5 text-sm text-ink/70"
          >
            <p class="font-bold text-ink">Antes de salvar</p>
            <ul class="mt-2 list-disc space-y-1 pl-5">
              <li>É preciso ter entre 1 e 10 personagens.</li>
              <li>Cada personagem aparece uma única vez.</li>
              <li>Personagens fora do rank não podem ser adicionados.</li>
            </ul>
          </div>
        </aside>
      </div>
    </template>
  </section>
</template>
