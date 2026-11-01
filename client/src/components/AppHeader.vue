<script setup lang="ts">
import { RouterLink } from "vue-router";

import { useAuthStore } from "../stores/auth";

const auth = useAuthStore();
</script>

<template>
  <header class="border-b-[3px] border-ink bg-paper">
    <div class="page-shell flex flex-wrap items-center gap-x-5 gap-y-3 py-4">
      <RouterLink
        to="/"
        class="font-display text-xl leading-none font-extrabold tracking-tight"
        aria-label="Turma da Mônica Rank, página inicial"
      >
        Turma da Mônica <span class="text-cherry">RANK</span>
      </RouterLink>

      <nav
        aria-label="Navegação principal"
        class="order-3 flex w-full items-center gap-1 text-sm md:order-2 md:ml-6 md:w-auto"
      >
        <RouterLink to="/ranking" class="nav-link" active-class="nav-link-active">
          Ranking
        </RouterLink>
        <RouterLink to="/personagens" class="nav-link" active-class="nav-link-active">
          Personagens
        </RouterLink>
        <RouterLink to="/votar" class="nav-link" active-class="nav-link-active">
          Meu voto
        </RouterLink>
      </nav>

      <div class="order-2 ml-auto flex items-center gap-2 md:order-3">
        <template v-if="auth.status === 'authenticated'">
          <RouterLink
            v-if="!auth.emailConfirmed"
            to="/conta"
            class="rounded-lg border-2 border-ink bg-sun px-3 py-1.5 text-xs font-bold"
          >
            Confirme seu e-mail
          </RouterLink>
          <RouterLink
            to="/conta"
            class="flex items-center gap-2 font-semibold"
            aria-label="Minha conta"
          >
            <span
              class="flex size-9 items-center justify-center rounded-full border-2 border-ink bg-sky text-sm font-bold text-paper"
              aria-hidden="true"
            >
              {{ auth.initials || "?" }}
            </span>
            <span class="hidden text-sm sm:inline">Minha conta</span>
          </RouterLink>
        </template>
        <template v-else-if="auth.status === 'anonymous'">
          <RouterLink to="/entrar" class="btn-ghost text-sm">Entrar</RouterLink>
          <RouterLink to="/criar-conta" class="btn btn-primary px-4 py-2 text-sm">
            Criar conta
          </RouterLink>
        </template>
      </div>
    </div>
  </header>
</template>
