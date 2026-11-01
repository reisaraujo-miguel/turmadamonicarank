<script setup lang="ts">
import { onMounted, ref } from "vue";
import { RouterLink, useRouter } from "vue-router";

import { formatDay } from "../lib/format";
import { messageFor } from "../lib/errors";
import { useAuthStore } from "../stores/auth";

const auth = useAuthStore();
const router = useRouter();

const resending = ref(false);
const message = ref("");
const loggingOut = ref(false);

onMounted(async () => {
  await auth.loadMe();
  if (!auth.isAuthenticated) {
    await router.replace({ name: "login", query: { redirect: "/conta" } });
  }
});

async function resend(): Promise<void> {
  if (!auth.user) {
    return;
  }
  message.value = "";
  resending.value = true;
  try {
    await auth.resendConfirmation(auth.user.email);
    message.value = "Se o e-mail estiver pendente, um novo link foi enviado.";
  } catch (caught) {
    message.value = messageFor(caught);
  } finally {
    resending.value = false;
  }
}

async function logout(): Promise<void> {
  loggingOut.value = true;
  await auth.logout();
  await router.push("/");
}
</script>

<template>
  <section class="page-shell py-10 lg:py-14">
    <div class="flex flex-wrap items-end justify-between gap-4">
      <div>
        <p class="eyebrow">Sua conta</p>
        <h1 class="mt-2 text-4xl">Minha conta</h1>
      </div>
      <RouterLink to="/votar" class="btn btn-secondary">Ir para meu top 10</RouterLink>
    </div>

    <div v-if="auth.user" class="mt-8 grid gap-6 lg:grid-cols-[1.1fr_0.9fr]">
      <div class="panel p-6 sm:p-8">
        <h2 class="text-2xl">Dados da conta</h2>
        <dl class="mt-5 space-y-4">
          <div>
            <dt class="text-sm font-semibold text-ink/60">E-mail</dt>
            <dd class="break-all text-lg font-semibold">{{ auth.user.email }}</dd>
          </div>
          <div>
            <dt class="text-sm font-semibold text-ink/60">Como quer ser chamado</dt>
            <dd class="text-lg">{{ auth.user.displayName || "Ainda não definido" }}</dd>
          </div>
          <div>
            <dt class="text-sm font-semibold text-ink/60">Na turma desde</dt>
            <dd class="text-lg">{{ formatDay(auth.user.createdAt) }}</dd>
          </div>
          <div>
            <dt class="text-sm font-semibold text-ink/60">Confirmação de e-mail</dt>
            <dd class="mt-1">
              <span
                v-if="auth.emailConfirmed"
                class="rounded-full border-2 border-ink bg-leaf/20 px-3 py-1 text-sm font-bold"
              >
                Confirmado
              </span>
              <span
                v-else
                class="rounded-full border-2 border-ink bg-sun px-3 py-1 text-sm font-bold"
              >
                Pendente
              </span>
            </dd>
          </div>
        </dl>

        <div class="mt-7 flex flex-wrap gap-3">
          <button type="button" class="btn btn-secondary" :disabled="loggingOut" @click="logout">
            {{ loggingOut ? "Saindo…" : "Sair da conta" }}
          </button>
          <RouterLink to="/sobre" class="btn-ghost">Sobre o projeto</RouterLink>
        </div>
      </div>

      <aside class="space-y-6">
        <div v-if="!auth.emailConfirmed" class="panel bg-sun/35 p-6">
          <h2 class="text-2xl">Confirme seu e-mail</h2>
          <p class="mt-2 text-ink/80">
            Enquanto o e-mail não for confirmado, seu voto não é aceito. Enviamos um link para
            <strong class="break-all">{{ auth.user.email }}</strong
            >.
          </p>
          <button
            type="button"
            class="btn btn-primary mt-4 w-full"
            :disabled="resending"
            @click="resend"
          >
            {{ resending ? "Enviando…" : "Reenviar confirmação" }}
          </button>
          <p v-if="message" class="mt-3 text-sm" role="status">{{ message }}</p>
        </div>

        <div v-else class="panel p-6">
          <h2 class="text-2xl">Tudo confirmado.</h2>
          <p class="mt-2 text-ink/75">
            Sua conta está pronta. Quando a votação abrir, é só montar o top 10 e editar quando
            quiser.
          </p>
        </div>

        <div class="rounded-panel border-2 border-dashed border-ink/35 p-6 text-sm text-ink/70">
          <p class="font-bold text-ink">Ainda em construção</p>
          <p class="mt-2">
            Exclusão de conta e exportação dos seus dados entram antes do lançamento público.
          </p>
        </div>
      </aside>
    </div>

    <div v-else class="panel mt-8 p-6">
      <p role="status">Carregando sua conta…</p>
    </div>
  </section>
</template>
