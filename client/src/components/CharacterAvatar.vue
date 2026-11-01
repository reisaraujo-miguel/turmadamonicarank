<script setup lang="ts">
import { computed } from "vue";

import type { CharacterSummary } from "../lib/types";

const props = withDefaults(
  defineProps<{
    character: CharacterSummary;
    size?: "sm" | "md" | "lg" | "xl";
  }>(),
  { size: "md" },
);

const sizes = {
  sm: "size-10 text-sm",
  md: "size-14 text-lg",
  lg: "size-24 text-3xl",
  xl: "size-40 text-5xl",
};

const palettes = [
  "bg-sun text-ink",
  "bg-sky text-paper",
  "bg-cherry text-paper",
  "bg-leaf text-paper",
];

const palette = computed(() => {
  const hash = [...props.character.slug].reduce((total, char) => total + char.charCodeAt(0), 0);
  return palettes[hash % palettes.length];
});

const initials = computed(() =>
  props.character.name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part.charAt(0).toUpperCase())
    .join(""),
);
</script>

<template>
  <div
    class="relative shrink-0 overflow-hidden rounded-lg border-[3px] border-ink shadow-[3px_3px_0_0_var(--color-ink)]"
    :class="[sizes[size], palette]"
  >
    <img
      v-if="character.imageUrl"
      :src="character.imageUrl"
      :alt="character.imageAlt ?? character.name"
      class="h-full w-full object-cover"
      loading="lazy"
      decoding="async"
    />
    <span
      v-else
      class="flex h-full w-full items-center justify-center font-display font-extrabold"
      aria-hidden="true"
    >
      {{ initials }}
    </span>
  </div>
</template>
