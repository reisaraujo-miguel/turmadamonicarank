<script setup lang="ts">
import { RouterLink } from "vue-router";

import { useAuthStore } from "../stores/auth";

const auth = useAuthStore();
</script>

<template>
  <section class="border-b-[3px] border-ink bg-paper py-10">
    <div class="page-shell">
      <p class="eyebrow">Seu voto</p>
      <h1 class="mt-2 text-4xl sm:text-5xl">Meu top 10</h1>
      <p class="mt-3 max-w-2xl text-ink/75">
        Aqui você vai montar e editar a sua lista. A votação abre na próxima etapa.
      </p>
    </div>
  </section>

  <section class="page-shell py-10 lg:py-14">
    <div class="panel p-6 sm:p-10">
      <p class="eyebrow">Próxima etapa</p>
      <h2 class="mt-2 max-w-2xl text-3xl sm:text-4xl">Sua conta já está pronta para votar.</h2>
      <p class="mt-4 max-w-2xl text-ink/75">
        O editor do top 10 chega junto com o catálogo de personagens. Enquanto isso, confira sua
        conta e confirme o e-mail para não perder a primeira contagem.
      </p>
      <div class="mt-7 flex flex-wrap gap-3">
        <RouterLink to="/conta" class="btn btn-primary">Minha conta</RouterLink>
        <RouterLink to="/sobre" class="btn btn-secondary">Como vai funcionar</RouterLink>
      </div>
      <p v-if="!auth.emailConfirmed" class="mt-5 text-sm font-semibold text-cherry">
        Seu e-mail ainda não foi confirmado. O voto só é aceito depois da confirmação.
      </p>
    </div>
  </section>
</template>
