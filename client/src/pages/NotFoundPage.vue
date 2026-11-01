<script setup lang="ts">
import { RouterLink } from "vue-router";
</script>

<template>
  <section class="page-shell py-16 lg:py-24">
    <div class="panel mx-auto max-w-2xl p-8 text-center">
      <p class="font-display text-6xl leading-none text-cherry">404</p>
      <h1 class="mt-3 text-3xl sm:text-4xl">Essa página saiu do gibi.</h1>
      <p class="mt-3 text-ink/75">
        O endereço pode ter mudado ou nunca ter existido. Volte para o começo e continue de onde
        estava.
      </p>
      <div class="mt-7 flex flex-wrap justify-center gap-3">
        <RouterLink to="/" class="btn btn-primary">Voltar ao início</RouterLink>
        <RouterLink to="/ranking" class="btn btn-secondary">Ver o ranking</RouterLink>
      </div>
    </div>
  </section>
</template>
