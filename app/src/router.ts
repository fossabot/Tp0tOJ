import Vue from "vue";
import Router from "vue-router";
import Home from "./views/Home.vue";
import ErrorPage from "@/views/ErrorPage.vue";

Vue.use(Router);

export default new Router({
  mode: "history",
  base: process.env.BASE_URL,
  routes: [
    {
      path: "/",
      name: "home",
      component: Home
    },
    {
      path: "/login",
      name: "login",
      component: () =>
        import(/* webpackChunkName: "login" */ "@/views/Login.vue")
    },
    {
      path: "/forget",
      name: "forget",
      component: () =>
        import(/* webpackChunkName: "forget" */ "@/views/Forget.vue")
    },
    {
      path: "/reset",
      name: "reset",
      component: () =>
        import(/* webpackChunkName: "reset" */ "@/views/Reset.vue")
    },
    {
      path: "/bulletin",
      name: "bulletin",
      component: () =>
        import(/* webpackChunkName: "bulletin" */ "@/views/Bulletin.vue")
    },
    {
      path: "/rank/:page",
      name: "rank",
      component: () => import(/* webpackChunkName: "rank" */ "@/views/Rank.vue")
    },
    {
      path: "/profile/:user_id",
      name: "profile",
      component: () =>
        import(/* webpackChunkName: "profile" */ "@/views/Profile.vue"),
      meta: {
        auth: "member"
      }
    },
    {
      path: "/challenge",
      name: "challenge",
      component: () =>
        import(/* webpackChunkName: "challenge" */ "@/views/Challenge.vue"),
      meta: {
        auth: "member"
      }
    },
    {
      path: "/admin/user",
      name: "admin-user",
      component: () =>
        import(
          /* webpackChunkName: "admin-challenge" */ "@/views/admin/User.vue"
        ),
      meta: {
        auth: "admin"
      }
    },
    {
      path: "/admin/challenge",
      name: "admin-challenge",
      component: () =>
        import(
          /* webpackChunkName: "admin-challenge" */ "@/views/admin/Challenge.vue"
        ),
      meta: {
        auth: "admin"
      }
    },
    {
      path: "/admin/images",
      name: "admin-images",
      component: () =>
        import(
          /* webpackChunkName: "admin-images" */ "@/views/admin/Images.vue"
        ),
      meta: {
        auth: "admin"
      }
    },
    {
      path: "/admin/cluster",
      name: "admin-cluster",
      component: () =>
        import(
          /* webpackChunkName: "admin-cluster" */ "@/views/admin/Cluster.vue"
        ),
      meta: {
        auth: "admin"
      }
    },
    {
      path: "/admin/writeup",
      name: "admin-writeup",
      component: () =>
        import(
          /* webpackChunkName: "admin-writeup" */ "@/views/admin/Writeup.vue"
        ),
      meta: {
        auth: "admin"
      }
    },
    {
      path: "/admin/event",
      name: "admin-event",
      component: () =>
        import(/* webpackChunkName: "admin-event" */ "@/views/admin/Event.vue"),
      meta: {
        auth: "admin"
      }
    },
    {
      path: "/admin/analyse",
      name: "admin-analyse",
      component: () =>
        import(
          /* webpackChunkName: "admin-analyse" */ "@/views/admin/Analyse.vue"
        ),
      meta: {
        auth: "admin"
      }
    },
    {
      path: "/monitor/:page",
      name: "rank",
      component: () => import(/* webpackChunkName: "rank" */ "@/views/Rank.vue")
    },
    {
      path: "*",
      name: "error",
      component: ErrorPage
    }
  ]
});
