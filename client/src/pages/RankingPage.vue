<script setup lang="ts">
import { RouterLink } from "vue-router";

import { useAuthStore } from "../stores/auth";

const auth = useAuthStore();
</script>

<template>
  <section class="halftone-light border-b-[3px] border-ink bg-sky py-10 text-paper">
    <div class="page-shell">
      <p class="eyebrow text-paper/80">Agora</p>
      <h1 class="mt-2 text-4xl sm:text-5xl">Ranking</h1>
      <p class="mt-3 max-w-2xl text-paper/85">
        Quando a votação abrir, esta página mostra o rank ao vivo e o histórico de cada mês fechado.
      </p>
    </div>
  </section>

  <section class="page-shell py-10 lg:py-14">
    <div class="panel p-6 sm:p-10">
      <p class="eyebrow">Primeira contagem</p>
      <h2 class="mt-2 max-w-2xl text-3xl sm:text-4xl">A primeira contagem ainda não aconteceu.</h2>
      <p class="mt-4 max-w-2xl text-ink/75">
        O ranking nasce dos votos confirmados. Crie sua conta, confirme o e-mail e monte seu top 10
        para ajudar a escrever o primeiro rank.
      </p>
      <div class="mt-7 flex flex-wrap gap-3">
        <RouterLink :to="auth.isAuthenticated ? '/votar' : '/criar-conta'" class="btn btn-primary">
          {{ auth.isAuthenticated ? "Montar meu top 10" : "Criar conta para votar" }}
        </RouterLink>
        <RouterLink to="/personagens" class="btn btn-secondary">
          Conhecer os personagens
        </RouterLink>
      </div>
    </div>

    <p class="mt-6 text-sm text-ink/65">
      A contagem mensal fecha no último dia do mês, às 23h59, no horário de Brasília.
    </p>
  </section>
</template>
