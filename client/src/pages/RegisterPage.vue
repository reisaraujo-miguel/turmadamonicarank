<script setup lang="ts">
import { ref } from "vue";
import { RouterLink } from "vue-router";

import AuthCard from "../components/AuthCard.vue";
import { messageFor } from "../lib/errors";
import { useAuthStore } from "../stores/auth";

const auth = useAuthStore();

const displayName = ref("");
const email = ref("");
const password = ref("");
const showPassword = ref(false);
const error = ref("");
const loading = ref(false);
const sent = ref(false);
const resendMessage = ref("");
const resending = ref(false);

async function submit(): Promise<void> {
  error.value = "";
  loading.value = true;
  try {
    await auth.register(
      email.value.trim(),
      password.value,
      displayName.value.trim() === "" ? null : displayName.value.trim(),
    );
    sent.value = true;
  } catch (caught) {
    error.value = messageFor(caught);
  } finally {
    loading.value = false;
  }
}

async function resend(): Promise<void> {
  resendMessage.value = "";
  resending.value = true;
  try {
    await auth.resendConfirmation(email.value.trim());
    resendMessage.value = "Se este e-mail estiver cadastrado e pendente, enviamos um novo link.";
  } catch (caught) {
    resendMessage.value = messageFor(caught);
  } finally {
    resending.value = false;
  }
}
</script>

<template>
  <AuthCard
    title="Criar conta"
    subtitle="Um e-mail, uma senha e o seu top 10 dos personagens da Turma da Mônica."
  >
    <div v-if="sent" class="space-y-5">
      <div class="rounded-lg border-2 border-ink bg-sun/40 p-5">
        <h2 class="text-2xl">Confira seu e-mail</h2>
        <p class="mt-2">
          Mandamos um link de confirmação para
          <strong class="break-all">{{ email.trim() }}</strong
          >. Abra o link para liberar o voto.
        </p>
        <p class="mt-2 text-sm text-ink/70">
          Não chegou em alguns minutos? Confira o spam ou peça outro link.
        </p>
      </div>

      <button type="button" class="btn btn-secondary w-full" :disabled="resending" @click="resend">
        {{ resending ? "Enviando…" : "Reenviar confirmação" }}
      </button>
      <p v-if="resendMessage" class="text-sm" role="status">{{ resendMessage }}</p>
      <p class="text-sm">
        Já confirmou?
        <RouterLink to="/entrar" class="font-bold underline underline-offset-4">
          Entrar
        </RouterLink>
      </p>
    </div>

    <form v-else class="space-y-5" novalidate @submit.prevent="submit">
      <div>
        <label for="display-name" class="label">Como quer ser chamado? (opcional)</label>
        <input
          id="display-name"
          v-model="displayName"
          class="field"
          type="text"
          name="displayName"
          autocomplete="nickname"
          maxlength="60"
          :disabled="loading"
        />
        <p class="hint">O nome aparece só para você, no seu perfil.</p>
      </div>

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
            autocomplete="new-password"
            minlength="10"
            maxlength="128"
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
        <p class="hint">Use pelo menos 10 caracteres.</p>
      </div>

      <p v-if="error" class="error-text" role="alert">{{ error }}</p>

      <button type="submit" class="btn btn-primary w-full" :disabled="loading">
        {{ loading ? "Criando conta…" : "Criar conta" }}
      </button>

      <p class="text-xs text-ink/65">
        Ao criar a conta, você concorda com o uso do e-mail para confirmar o cadastro e recuperar a
        senha. Nada de spam.
      </p>
    </form>

    <template #footer>
      <p>
        Já tem conta?
        <RouterLink to="/entrar" class="font-bold underline underline-offset-4">
          Entrar
        </RouterLink>
      </p>
    </template>
  </AuthCard>
</template>
