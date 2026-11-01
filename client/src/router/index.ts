import { createRouter, createWebHistory } from "vue-router";

import { useAuthStore } from "../stores/auth";

declare module "vue-router" {
  interface RouteMeta {
    title?: string;
    requiresAuth?: boolean;
    requiresConfirmed?: boolean;
    guestOnly?: boolean;
  }
}

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: "/",
      name: "home",
      component: () => import("../pages/HomePage.vue"),
      meta: { title: "Início" },
    },
    {
      path: "/ranking",
      name: "ranking",
      component: () => import("../pages/RankingPage.vue"),
      meta: { title: "Ranking" },
    },
    {
      path: "/ranking/:ano",
      name: "ranking-year",
      component: () => import("../pages/RankingPage.vue"),
      meta: { title: "Ranking do ano" },
    },
    {
      path: "/ranking/:ano/:mes",
      name: "ranking-month",
      component: () => import("../pages/RankingPage.vue"),
      meta: { title: "Ranking do mês" },
    },
    {
      path: "/personagens",
      name: "characters",
      component: () => import("../pages/CharactersPage.vue"),
      meta: { title: "Personagens" },
    },
    {
      path: "/personagens/:slug",
      name: "character",
      component: () => import("../pages/CharactersPage.vue"),
      props: true,
      meta: { title: "Personagem" },
    },
    {
      path: "/votar",
      name: "vote",
      component: () => import("../pages/VotePage.vue"),
      meta: {
        title: "Meu top 10",
        requiresAuth: true,
        requiresConfirmed: true,
      },
    },
    {
      path: "/entrar",
      name: "login",
      component: () => import("../pages/LoginPage.vue"),
      meta: { title: "Entrar", guestOnly: true },
    },
    {
      path: "/criar-conta",
      name: "register",
      component: () => import("../pages/RegisterPage.vue"),
      meta: { title: "Criar conta", guestOnly: true },
    },
    {
      path: "/confirmar-email",
      name: "confirm-email",
      component: () => import("../pages/ConfirmEmailPage.vue"),
      meta: { title: "Confirmar e-mail" },
    },
    {
      path: "/esqueci-senha",
      name: "forgot-password",
      component: () => import("../pages/ForgotPasswordPage.vue"),
      meta: { title: "Esqueci minha senha", guestOnly: true },
    },
    {
      path: "/redefinir-senha",
      name: "reset-password",
      component: () => import("../pages/ResetPasswordPage.vue"),
      meta: { title: "Nova senha" },
    },
    {
      path: "/conta",
      name: "account",
      component: () => import("../pages/AccountPage.vue"),
      meta: { title: "Minha conta", requiresAuth: true },
    },
    {
      path: "/sobre",
      name: "about",
      component: () => import("../pages/AboutPage.vue"),
      meta: { title: "Sobre" },
    },
    {
      path: "/:pathMatch(.*)*",
      name: "not-found",
      component: () => import("../pages/NotFoundPage.vue"),
      meta: { title: "Página não encontrada" },
    },
  ],
  scrollBehavior() {
    return { top: 0 };
  },
});

router.beforeEach(async (to) => {
  const auth = useAuthStore();
  await auth.initialize();

  if (to.meta.guestOnly && auth.isAuthenticated) {
    return { name: "home" };
  }
  if (to.meta.requiresAuth && !auth.isAuthenticated) {
    return { name: "login", query: { redirect: to.fullPath } };
  }
  if (to.meta.requiresConfirmed && !auth.emailConfirmed) {
    return { name: "account" };
  }
  return true;
});

router.afterEach((to, _from, failure) => {
  if (failure) {
    return;
  }
  document.title = to.meta.title
    ? `${to.meta.title} — Turma da Mônica Rank`
    : "Turma da Mônica Rank";
  window.requestAnimationFrame(() => {
    const main = document.getElementById("conteudo");
    main?.focus({ preventScroll: true });
  });
});

export default router;
