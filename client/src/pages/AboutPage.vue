<script setup lang="ts">
import { RouterLink } from "vue-router";
</script>

<template>
  <section class="border-b-[3px] border-ink bg-sky py-10 text-paper">
    <div class="page-shell">
      <p class="eyebrow text-paper/80">Sobre</p>
      <h1 class="mt-2 max-w-3xl text-4xl sm:text-5xl">Um rank feito por fãs, para fãs.</h1>
      <p class="mt-4 max-w-2xl text-paper/85">
        A Turma da Mônica Rank é um projeto de fã, sem vínculo com Mauricio de Sousa Produções. A
        ideia é simples: descobrir, mês a mês, quem a galera mais gosta.
      </p>
    </div>
  </section>

  <section class="page-shell grid gap-6 py-10 lg:grid-cols-2 lg:py-14">
    <article class="panel p-6 sm:p-8">
      <h2 class="text-2xl">Como o voto vira rank</h2>
      <ol class="mt-4 list-decimal space-y-3 pl-5 text-ink/80">
        <li>Cada pessoa confirma uma conta e monta uma lista com até 10 personagens.</li>
        <li>A posição vale pontos: o 1º lugar soma 10 pontos, o 10º soma 1.</li>
        <li>O rank ao vivo soma os pontos de todos os votos ativos.</li>
        <li>No fim de cada mês, a contagem congela o ranking em um registro que não muda mais.</li>
      </ol>
      <p class="mt-4 text-sm text-ink/65">
        Empates são resolvidos por mais primeiros lugares, depois por quantos votos incluem o
        personagem.
      </p>
    </article>

    <article class="panel p-6 sm:p-8">
      <h2 class="text-2xl">Direitos e créditos</h2>
      <p class="mt-3 text-ink/80">
        Personagens, nomes e obras pertencem a Mauricio de Sousa Produções. As imagens e bios deste
        site serão creditadas e publicadas com autorização ou licença; até lá, usamos imagens
        ilustrativas.
      </p>
      <p class="mt-3 text-ink/80">
        Se você encontrou um erro, quer corrigir uma informação ou solicitar a remoção de algum
        conteúdo, fale com a gente pelo repositório do projeto.
      </p>
      <div class="mt-6 rounded-lg border-2 border-ink bg-sun/30 p-4 text-sm">
        <p class="font-bold">Projeto de fã, não oficial.</p>
        <p class="mt-1">
          Sem anúncios, sem venda de dados e sem vínculo comercial com os detentores dos direitos.
        </p>
      </div>
    </article>

    <article class="panel p-6 sm:p-8 lg:col-span-2">
      <h2 class="text-2xl">O que vem por aí</h2>
      <div class="mt-4 grid gap-5 sm:grid-cols-3">
        <div>
          <h3 class="text-lg">Catálogo</h3>
          <p class="mt-1 text-sm text-ink/75">
            Bios curtas e históricos de posição de cada personagem.
          </p>
        </div>
        <div>
          <h3 class="text-lg">Contagens mensais</h3>
          <p class="mt-1 text-sm text-ink/75">
            Rankings arquivados por mês e uma visão do ano fechado.
          </p>
        </div>
        <div>
          <h3 class="text-lg">Privacidade</h3>
          <p class="mt-1 text-sm text-ink/75">
            Exportar e excluir a conta antes de abrir a votação para todo mundo.
          </p>
        </div>
      </div>
      <RouterLink to="/criar-conta" class="btn btn-primary mt-6">Criar conta</RouterLink>
    </article>
  </section>
</template>
