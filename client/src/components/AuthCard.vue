<script setup lang="ts">
defineProps<{
  title: string;
  subtitle?: string;
}>();
</script>

<template>
  <section class="page-shell py-10 lg:py-16">
    <div class="grid items-stretch gap-8 lg:grid-cols-[minmax(0,1fr)_0.8fr]">
      <div class="panel p-6 sm:p-8">
        <h1 class="text-3xl sm:text-4xl">{{ title }}</h1>
        <p v-if="subtitle" class="mt-3 max-w-prose text-ink/75">{{ subtitle }}</p>
        <div class="mt-7">
          <slot />
        </div>
        <div v-if="$slots.footer" class="mt-6 border-t-2 border-ink/15 pt-5 text-sm">
          <slot name="footer" />
        </div>
      </div>

      <aside
        class="halftone-light relative hidden overflow-hidden rounded-panel border-[3px] border-ink bg-sky p-8 text-paper lg:flex lg:flex-col lg:justify-between"
        aria-hidden="true"
      >
        <p class="font-display text-5xl leading-none font-extrabold text-sun">10</p>
        <div>
          <p class="font-display text-3xl leading-tight">Seu top 10 começa aqui.</p>
          <p class="mt-4 max-w-xs text-paper/85">
            Escolha os personagens, ordene do favorito ao décimo e acompanhe a contagem de cada mês.
          </p>
        </div>
        <p class="text-sm text-paper/70">Voto de fã, contagem honesta.</p>
      </aside>
    </div>
  </section>
</template>
