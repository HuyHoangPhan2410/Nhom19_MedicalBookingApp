import { createRouter, createWebHistory } from 'vue-router';
import HomeView from '../views/HomeView.vue';
import DoctorDetailView from '../views/DoctorDetailView.vue';
import PatientProfileView from '../views/PatientProfileView.vue';
import DoctorDashboardView from '../views/DoctorDashboardView.vue';
import AuthView from '../views/AuthView.vue';
import { useAuthStore } from '../stores/auth';

// ✅ Admin pages
import AdminLogin from '../views/admin/AdminLogin.vue';
import AdminDashboard from '../views/admin/AdminDashboard.vue';
import AdminUsers from '../views/admin/AdminUsers.vue';
import AdminPatients from '../views/admin/AdminPatients.vue';
import AdminDoctors from '../views/admin/AdminDoctors.vue';
import AdminAppointments from '../views/admin/AdminAppointments.vue';

const routes = [
  { path: '/', name: 'home', component: HomeView },
  { path: '/doctors/:id', name: 'doctor-detail', component: DoctorDetailView },
  { path: '/profile', name: 'profile', component: PatientProfileView, meta: { requiresAuth: true, role: 'patient' } },
  { path: '/doctor-dashboard', name: 'doctor-dashboard', component: DoctorDashboardView, meta: { requiresAuth: true, role: 'doctor' } },
  { path: '/auth', name: 'auth', component: AuthView },

  // ✅ Admin routes
  { path: '/admin/login', name: 'admin-login', component: AdminLogin },
  { path: '/admin', name: 'admin-dashboard', component: AdminDashboard, meta: { requiresAdmin: true } },
  { path: '/admin/users', name: 'admin-users', component: AdminUsers, meta: { requiresAdmin: true } },
  { path: '/admin/patients', name: 'admin-patients', component: AdminPatients, meta: { requiresAdmin: true } },
  { path: '/admin/doctors', name: 'admin-doctors', component: AdminDoctors, meta: { requiresAdmin: true } },
  { path: '/admin/appointments', name: 'admin-appointments', component: AdminAppointments, meta: { requiresAdmin: true } },

  { path: '/:pathMatch(.*)*', redirect: '/' }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

router.beforeEach((to, from, next) => {
  const authStore = useAuthStore();
  const isAuthenticated = authStore.isAuthenticated;
  const userRole = authStore.user?.role;

  // ✅ Admin guard
  if (to.meta.requiresAdmin) {
    const adminToken = localStorage.getItem('admin_token');
    if (!adminToken) {
      return next({ name: 'admin-login' });
    }
  }

  if (to.meta.requiresAuth) {
    if (!isAuthenticated) {
      return next({ name: 'home' });
    }
    if (to.meta.role && to.meta.role !== userRole) {
      return next({ name: 'home' });
    }
  }

  if (to.name === 'auth' && isAuthenticated) {
    if (userRole === 'doctor') return next({ name: 'doctor-dashboard' });
    return next({ name: 'home' });
  }

  next();
});

export default router;