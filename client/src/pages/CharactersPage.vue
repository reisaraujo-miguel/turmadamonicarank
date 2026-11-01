<script setup lang="ts">
import { RouterLink } from "vue-router";

const props = defineProps<{ slug?: string }>();
</script>

<template>
  <section class="halftone-light border-b-[3px] border-ink bg-sky py-10 text-paper">
    <div class="page-shell">
      <p class="eyebrow text-paper/80">Personagens</p>
      <h1 class="mt-2 text-4xl sm:text-5xl">
        {{ props.slug ? "Ficha do personagem" : "Personagens" }}
      </h1>
      <p class="mt-3 max-w-2xl text-paper/85">
        Bios curtas, apelidos e o histórico de posições de cada personagem. O catálogo entra no ar
        junto com a votação.
      </p>
    </div>
  </section>

  <section class="page-shell py-10 lg:py-14">
    <div class="panel p-6 sm:p-10">
      <p class="eyebrow">Em preparação</p>
      <h2 class="mt-2 max-w-2xl text-3xl sm:text-4xl">
        O catálogo está sendo montado com carinho.
      </h2>
      <p class="mt-4 max-w-2xl text-ink/75">
        <template v-if="props.slug">
          A página de <code class="rounded bg-ink/5 px-1">{{ props.slug }}</code> ainda não está
          disponível. Assim que o catálogo entrar no ar, cada personagem terá uma bio e um gráfico
          de posições.
        </template>
        <template v-else>
          Estamos reunindo o elenco do Bairro do Limoeiro e escrevendo as bios. Enquanto isso, você
          já pode criar sua conta e garantir seu lugar na primeira contagem.
        </template>
      </p>
      <div class="mt-7 flex flex-wrap gap-3">
        <RouterLink to="/criar-conta" class="btn btn-primary">Criar conta</RouterLink>
        <RouterLink to="/sobre" class="btn btn-secondary">Sobre o projeto</RouterLink>
      </div>
    </div>
  </section>
</template>
