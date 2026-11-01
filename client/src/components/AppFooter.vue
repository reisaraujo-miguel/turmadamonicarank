<script setup lang="ts">
import { RouterLink } from "vue-router";
</script>

<template>
  <footer class="border-t-[3px] border-ink bg-paper py-8">
    <div
      class="page-shell flex flex-col gap-3 text-sm text-ink/75 sm:flex-row sm:items-center sm:justify-between"
    >
      <p>Projeto de fã, sem vínculo com Mauricio de Sousa Produções.</p>
      <nav aria-label="Rodapé" class="flex gap-4 font-semibold">
        <RouterLink to="/sobre" class="underline-offset-4 hover:underline">Sobre</RouterLink>
        <RouterLink to="/ranking" class="underline-offset-4 hover:underline"> Ranking </RouterLink>
      </nav>
    </div>
  </footer>
</template>
