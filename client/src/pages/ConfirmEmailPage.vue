<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { RouterLink, useRoute } from "vue-router";

import AuthCard from "../components/AuthCard.vue";
import { messageFor } from "../lib/errors";
import { useAuthStore } from "../stores/auth";

const auth = useAuthStore();
const route = useRoute();

const token = computed(() => (typeof route.query.token === "string" ? route.query.token : ""));
const state = ref<"loading" | "success" | "error">("loading");
const error = ref("");

onMounted(async () => {
  if (!token.value) {
    state.value = "error";
    error.value = "O link não trouxe um token de confirmação.";
    return;
  }
  try {
    await auth.confirmEmail(token.value);
    state.value = "success";
  } catch (caught) {
    state.value = "error";
    error.value = messageFor(caught);
  }
});
</script>

<template>
  <AuthCard
    title="Confirmar e-mail"
    subtitle="Estamos verificando o link que chegou na sua caixa de entrada."
  >
    <div v-if="state === 'loading'" class="space-y-3" role="status">
      <p class="text-lg font-semibold">Confirmando seu e-mail…</p>
      <p class="text-ink/70">Isso leva só um instante.</p>
    </div>

    <div v-else-if="state === 'success'" class="space-y-5">
      <div class="rounded-lg border-2 border-ink bg-leaf/15 p-5">
        <h2 class="text-2xl">E-mail confirmado!</h2>
        <p class="mt-2">Sua conta está liberada para votar assim que a votação abrir.</p>
      </div>
      <RouterLink to="/entrar" class="btn btn-primary w-full"> Entrar na minha conta </RouterLink>
    </div>

    <div v-else class="space-y-5">
      <p class="error-text" role="alert">{{ error }}</p>
      <p class="text-ink/75">
        O link pode ter expirado ou já ter sido usado. Entre na sua conta para pedir outro.
      </p>
      <RouterLink to="/entrar" class="btn btn-primary w-full">Entrar e reenviar</RouterLink>
      <RouterLink to="/criar-conta" class="btn btn-secondary w-full">
        Criar outra conta
      </RouterLink>
    </div>
  </AuthCard>
</template>
