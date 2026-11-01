<script setup lang="ts">
import { computed, ref } from "vue";
import { RouterLink, useRoute } from "vue-router";

import AuthCard from "../components/AuthCard.vue";
import { messageFor } from "../lib/errors";
import { useAuthStore } from "../stores/auth";

const auth = useAuthStore();
const route = useRoute();

const token = computed(() => (typeof route.query.token === "string" ? route.query.token : ""));
const password = ref("");
const confirmation = ref("");
const error = ref("");
const loading = ref(false);
const success = ref(false);

async function submit(): Promise<void> {
  error.value = "";
  if (password.value.length < 10) {
    error.value = "A senha precisa ter pelo menos 10 caracteres.";
    return;
  }
  if (password.value !== confirmation.value) {
    error.value = "As senhas não conferem.";
    return;
  }
  loading.value = true;
  try {
    await auth.resetPassword(token.value, password.value);
    success.value = true;
  } catch (caught) {
    error.value = messageFor(caught);
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <AuthCard
    title="Criar nova senha"
    subtitle="Escolha uma senha nova para voltar a acessar sua conta."
  >
    <div v-if="!token" class="space-y-5">
      <p class="error-text" role="alert">Este link não trouxe um token de redefinição.</p>
      <RouterLink to="/esqueci-senha" class="btn btn-primary w-full">
        Pedir um novo link
      </RouterLink>
    </div>

    <div v-else-if="success" class="space-y-5">
      <div class="rounded-lg border-2 border-ink bg-leaf/15 p-5">
        <h2 class="text-2xl">Senha alterada!</h2>
        <p class="mt-2">Todas as sessões antigas foram encerradas. Entre com a senha nova.</p>
      </div>
      <RouterLink to="/entrar" class="btn btn-primary w-full">Entrar</RouterLink>
    </div>

    <form v-else class="space-y-5" novalidate @submit.prevent="submit">
      <div>
        <label for="password" class="label">Nova senha</label>
        <input
          id="password"
          v-model="password"
          class="field"
          type="password"
          name="password"
          autocomplete="new-password"
          minlength="10"
          maxlength="128"
          required
          :disabled="loading"
        />
        <p class="hint">Pelo menos 10 caracteres.</p>
      </div>

      <div>
        <label for="confirmation" class="label">Repita a nova senha</label>
        <input
          id="confirmation"
          v-model="confirmation"
          class="field"
          type="password"
          name="confirmation"
          autocomplete="new-password"
          minlength="10"
          maxlength="128"
          required
          :disabled="loading"
        />
      </div>

      <p v-if="error" class="error-text" role="alert">{{ error }}</p>

      <button type="submit" class="btn btn-primary w-full" :disabled="loading">
        {{ loading ? "Salvando…" : "Salvar nova senha" }}
      </button>
    </form>

    <template #footer>
      <RouterLink to="/entrar" class="font-semibold underline underline-offset-4">
        Voltar para entrar
      </RouterLink>
    </template>
  </AuthCard>
</template>
