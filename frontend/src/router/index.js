import { createRouter, createWebHistory } from 'vue-router';
import HomeView from '../views/HomeView.vue';
import DoctorDetailView from '../views/DoctorDetailView.vue';
import PatientProfileView from '../views/PatientProfileView.vue';
import DoctorDashboardView from '../views/DoctorDashboardView.vue';
import AuthView from '../views/AuthView.vue';
import { useAuthStore } from '../stores/auth';

const routes = [
  {
    path: '/',
    name: 'home',
    component: HomeView
  },
  {
    path: '/doctors/:id',
    name: 'doctor-detail',
    component: DoctorDetailView
  },
  {
    path: '/profile',
    name: 'profile',
    component: PatientProfileView,
    meta: { requiresAuth: true, role: 'patient' }
  },
  {
    path: '/doctor-dashboard',
    name: 'doctor-dashboard',
    component: DoctorDashboardView,
    meta: { requiresAuth: true, role: 'doctor' }
  },
  {
    path: '/auth',
    name: 'auth',
    component: AuthView
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

// Guard bảo mật điều hướng (Navigation Guard)
router.beforeEach((to, from, next) => {
  const authStore = useAuthStore();
  const isAuthenticated = authStore.isAuthenticated;
  const userRole = authStore.user?.role; // 'patient' hoặc 'doctor'

  // 1. Nếu trang yêu cầu đăng nhập
  if (to.meta.requiresAuth) {
    if (!isAuthenticated) {
      // Chưa đăng nhập mà nhập URL bảo mật -> Bắt buộc nảy ra Trang Chủ (Home)
      return next({ name: 'home' });
    }

    // 2. Kiểm tra đúng vai trò được phép truy cập
    if (to.meta.role && to.meta.role !== userRole) {
      // Sai vai trò (VD: Bệnh nhân cố nhập URL /doctor-dashboard hoặc Bác sĩ nhập URL /profile) -> Bắt buộc nhảy ra Trang Chủ
      return next({ name: 'home' });
    }
  }

  // 3. Nếu đã đăng nhập mà cố gõ URL /auth
  if (to.name === 'auth' && isAuthenticated) {
    if (userRole === 'doctor') {
      return next({ name: 'doctor-dashboard' });
    }
    return next({ name: 'home' });
  }

  next();
});

export default router;
