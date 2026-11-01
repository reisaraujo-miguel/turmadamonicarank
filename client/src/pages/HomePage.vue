<script setup lang="ts">
import { RouterLink } from "vue-router";

import { useAuthStore } from "../stores/auth";

const auth = useAuthStore();
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
        <p class="mt-4 text-sm text-paper/75">
          É rápido: confirme seu e-mail e monte o seu top 10.
        </p>
      </div>

      <div class="grid content-center gap-3" aria-hidden="true">
        <div class="panel -rotate-1 p-5">
          <p class="font-display text-5xl leading-none text-cherry">1º</p>
          <p class="mt-2 font-display text-2xl">O favorito da galera</p>
          <p class="mt-1 text-sm text-ink/65">Aguardando o primeiro voto.</p>
        </div>
        <div class="grid grid-cols-2 gap-3">
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

    <div
      class="panel mt-8 flex flex-col gap-4 bg-sun/35 p-6 sm:flex-row sm:items-center sm:justify-between"
    >
      <div>
        <h3 class="text-2xl">O primeiro rank ainda está em branco.</h3>
        <p class="mt-1 text-ink/75">
          A votação e as contagens mensais entram no ar na próxima etapa do projeto.
        </p>
      </div>
      <RouterLink to="/ranking" class="btn btn-secondary whitespace-nowrap">
        Ver a página do ranking
      </RouterLink>
    </div>
  </section>
</template>
