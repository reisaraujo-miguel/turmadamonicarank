<script setup lang="ts">
import { ref } from "vue";
import { RouterLink } from "vue-router";

import AuthCard from "../components/AuthCard.vue";
import { messageFor } from "../lib/errors";
import { useAuthStore } from "../stores/auth";

const auth = useAuthStore();

const email = ref("");
const error = ref("");
const loading = ref(false);
const sent = ref(false);

async function submit(): Promise<void> {
  error.value = "";
  loading.value = true;
  try {
    await auth.requestPasswordReset(email.value.trim());
    sent.value = true;
  } catch (caught) {
    error.value = messageFor(caught);
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <AuthCard
    title="Esqueci minha senha"
    subtitle="Informe o e-mail da conta e enviaremos um link para você criar uma nova senha."
  >
    <div v-if="sent" class="space-y-5">
      <div class="rounded-lg border-2 border-ink bg-sky/10 p-5">
        <h2 class="text-2xl">Confira sua caixa de entrada</h2>
        <p class="mt-2">
          Se este e-mail estiver cadastrado, o link de redefinição chega em instantes. Ele vale por
          1 hora.
        </p>
      </div>
      <RouterLink to="/entrar" class="btn btn-primary w-full">Voltar para entrar</RouterLink>
    </div>

    <form v-else class="space-y-5" novalidate @submit.prevent="submit">
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

      <p v-if="error" class="error-text" role="alert">{{ error }}</p>

      <button type="submit" class="btn btn-primary w-full" :disabled="loading">
        {{ loading ? "Enviando…" : "Enviar link" }}
      </button>
    </form>

    <template #footer>
      <p>
        Lembrou a senha?
        <RouterLink to="/entrar" class="font-bold underline underline-offset-4">
          Entrar
        </RouterLink>
      </p>
    </template>
  </AuthCard>
</template>
