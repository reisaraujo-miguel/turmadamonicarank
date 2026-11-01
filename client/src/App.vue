<script setup lang="ts">
import { RouterView } from "vue-router";

import AppFooter from "./components/AppFooter.vue";
import AppHeader from "./components/AppHeader.vue";
</script>

<template>
  <div class="flex min-h-screen flex-col bg-paper text-ink">
    <a class="skip-link" href="#conteudo">Pular para o conteúdo</a>
    <AppHeader />
    <main id="conteudo" class="flex-1 outline-none" tabindex="-1">
      <RouterView />
    </main>
    <AppFooter />
  </div>
</template>
