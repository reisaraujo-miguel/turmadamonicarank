<script setup lang="ts">
import { ref } from "vue";
import { RouterLink, useRoute, useRouter } from "vue-router";

import AuthCard from "../components/AuthCard.vue";
import { messageFor } from "../lib/errors";
import { useAuthStore } from "../stores/auth";

const auth = useAuthStore();
const route = useRoute();
const router = useRouter();

const email = ref("");
const password = ref("");
const showPassword = ref(false);
const error = ref("");
const loading = ref(false);

function safeRedirect(): string {
  const redirect = route.query.redirect;
  if (typeof redirect === "string" && redirect.startsWith("/") && !redirect.startsWith("//")) {
    return redirect;
  }
  return "/";
}

async function submit(): Promise<void> {
  error.value = "";
  loading.value = true;
  try {
    await auth.login(email.value.trim(), password.value);
    await router.push(safeRedirect());
  } catch (caught) {
    error.value = messageFor(caught);
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <AuthCard title="Entrar" subtitle="Use o e-mail e a senha da sua conta para continuar.">
    <form class="space-y-5" novalidate @submit.prevent="submit">
      <div>
        <label for="email" class="label">E-mail</label>
        <input
          id="email"
          v-model="email"
          class="field"
          type="email"
          name="email"
          autocomplete="email"
          inputmode="email"
          required
          :disabled="loading"
        />
      </div>

      <div>
        <label for="password" class="label">Senha</label>
        <div class="relative">
          <input
            id="password"
            v-model="password"
            class="field pr-20"
            :type="showPassword ? 'text' : 'password'"
            name="password"
            autocomplete="current-password"
            required
            :disabled="loading"
          />
          <button
            type="button"
            class="absolute inset-y-0 right-1 my-1 rounded px-3 text-sm font-semibold text-ink/70 hover:text-ink"
            :aria-label="showPassword ? 'Ocultar senha' : 'Mostrar senha'"
            @click="showPassword = !showPassword"
          >
            {{ showPassword ? "Ocultar" : "Mostrar" }}
          </button>
        </div>
      </div>

      <p v-if="error" class="error-text" role="alert">{{ error }}</p>

      <button type="submit" class="btn btn-primary w-full" :disabled="loading">
        {{ loading ? "Entrando…" : "Entrar" }}
      </button>
    </form>

    <template #footer>
      <div class="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
        <RouterLink to="/esqueci-senha" class="font-semibold underline underline-offset-4">
          Esqueci minha senha
        </RouterLink>
        <p>
          Ainda não tem conta?
          <RouterLink to="/criar-conta" class="font-bold underline underline-offset-4">
            Criar conta
          </RouterLink>
        </p>
      </div>
    </template>
  </AuthCard>
</template>
