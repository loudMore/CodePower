/** 路由配置与前置守卫（登录检查、角色权限） */
import { createRouter, createWebHistory } from 'vue-router'
import { checkTokenValid } from '../api/index'

// 处理登录后跳回原页面的 redirect，防止跳到外部地址或再次跳回登录页。
const resolveSafeRedirect = (redirect: unknown) => {
  const value = Array.isArray(redirect) ? redirect[0] : redirect
  if (
    typeof value === 'string' &&
    value.startsWith('/') &&
    !value.startsWith('//') &&
    !value.startsWith('/auth')
  ) {
    return value
  }
  return '/'
}

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('../views/home/HomeView.vue')
    },
    {
      path: '/auth',
      component: () => import('../views/auth/AuthView.vue'),
      redirect: '/auth/login',
      children: [
        {
          path: 'login',
          name: 'login',
          component: () => import('../views/auth/LoginView.vue')
        },
        {
          path: 'register',
          name: 'register',
          component: () => import('../views/auth/RegisterView.vue')
        },
        {
          path: 'forgot-password',
          name: 'forgot-password',
          component: () => import('../views/auth/ForgotPasswordView.vue')
        }
      ]
    },
    {
      path: '/oauth/callback',
      name: 'oauth-callback',
      component: () => import('../views/auth/OAuthCallbackView.vue')
    },
    {
      path: '/learning-paths',
      name: 'learning-paths',
      component: () => import('../views/learning/LearningPathsView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/learning-paths/:id',
      name: 'learning-path-detail',
      component: () => import('../views/learning/LearningPathDetail.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/problems',
      name: 'problems',
      component: () => import('../views/problems/ProblemView.vue')
    },
    {
      path: '/problems/create',
      name: 'problem-create',
      component: () => import('../views/problems/ProblemCreate.vue'),
      meta: { requiresAuth: true, requiresSenior: true }
    },
    {
      path: '/problems/:id/edit',
      name: 'problem-edit',
      component: () => import('../views/problems/ProblemEdit.vue'),
      meta: { requiresAuth: true, requiresSenior: true }
    },
    {
      path: '/problems/:id',
      name: 'problem-detail',
      component: () => import('../views/problems/ProblemIDE.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/contests',
      name: 'contests',
      component: () => import('../views/contests/ContestsView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/contests/create',
      name: 'contest-create',
      component: () => import('../views/contests/ContestCreateView.vue'),
      meta: { requiresAuth: true, requiresSenior: true }
    },
    {
      path: '/contests/:id/edit',
      name: 'contest-edit',
      component: () => import('../views/contests/ContestCreateView.vue'),
      meta: { requiresAuth: true, requiresSenior: true }
    },
    {
      path: '/contests/:id',
      name: 'contest-detail',
      component: () => import('../views/contests/ContestDetailView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/contests/:id/result',
      name: 'contest-result',
      component: () => import('../views/contests/ContestDetailView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/problem-sets',
      redirect: '/problems?tab=publicSets'
    },
    {
      path: '/problem-sets/:id',
      name: 'problem-set-detail',
      component: () => import('../views/training/ProblemSetDetailView.vue')
    },
    {
      path: '/community',
      name: 'community',
      component: () => import('../views/community/CommunityView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/profile',
      name: 'profile',
      component: () => import('../views/profile/ProfileView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/users/:id',
      name: 'user-profile',
      component: () => import('../views/profile/ProfileView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/profile/settings',
      name: 'settings',
      component: () => import('../views/profile/SettingsView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/admin',
      name: 'admin',
      component: () => import('../views/admin/AdminView.vue'),
      meta: { requiresAuth: true, requiresAdmin: true }
    },
    {
      path: '/notifications',
      name: 'notifications',
      redirect: '/messages',
      meta: { requiresAuth: true }
    },
    {
      path: '/messages',
      name: 'messages',
      component: () => import('../views/message/MessageView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/messages/:userId',
      name: 'message-conversation',
      component: () => import('../views/message/MessageView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/about',
      name: 'about',
      component: () => import('../views/about/AboutView.vue')
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('../views/NotFoundView.vue')
    }
  ]
})

// 路由前置守卫：统一检查登录状态、管理员权限和高级用户权限。
router.beforeEach((to, from, next) => {
  const isLoggedIn = checkTokenValid()

  if (to.path.startsWith('/auth') && isLoggedIn) {
    next({ path: resolveSafeRedirect(to.query.redirect) })
    return
  }

  if (to.matched.some(record => record.meta.requiresAuth)) {
    if (!isLoggedIn) {
      next({
        path: '/auth/login',
        query: { redirect: to.fullPath }
      })
      return
    }

    // 管理后台只允许 ADMIN 进入。
    if (to.matched.some(record => record.meta.requiresAdmin)) {
      const userInfoStr = localStorage.getItem('userInfo')
      if (userInfoStr) {
        try {
          const userInfo = JSON.parse(userInfoStr)
          if (userInfo.role !== 'ADMIN') {
            next({ path: '/' })
            return
          }
        } catch {
          next({ path: '/' })
          return
        }
      } else {
        next({ path: '/' })
        return
      }
    }

    // 出题、编辑题目、创建竞赛允许高级用户和管理员进入。
    if (to.matched.some(record => record.meta.requiresSenior)) {
      const userInfoStr = localStorage.getItem('userInfo')
      if (userInfoStr) {
        try {
          const userInfo = JSON.parse(userInfoStr)
          if (userInfo.role !== 'SENIOR_USER' && userInfo.role !== 'ADMIN') {
            next({ path: '/' })
            return
          }
        } catch {
          next({ path: '/' })
          return
        }
      } else {
        next({ path: '/' })
        return
      }
    }

    next()
  } else {
    next()
  }
})

router.onError((error, to) => {
  const message = String(error?.message || error)
  if (/Failed to fetch dynamically imported module|Importing a module script failed|Loading chunk/i.test(message)) {
    // 线上更新前端静态资源后，旧页面可能加载不到旧 chunk，直接刷新当前路由兜底。
    window.location.assign(to.fullPath || '/')
  }
})

export default router 
