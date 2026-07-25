<script setup>
import { RouterLink, RouterView, useRoute } from 'vue-router';
import { useAuthStore } from './stores/auth';
import { computed } from 'vue';

const authStore = useAuthStore();
const route = useRoute();

// ✅ Ẩn header/footer khi ở trang admin hoặc doctor dashboard
const isAdminPage = computed(() => route.path.startsWith('/admin'));
const isDoctorDashboard = computed(() => route.path === '/doctor-dashboard');
const hideChrome = computed(() => isAdminPage.value || isDoctorDashboard.value);
</script>

<template>
  <div class="app-container">
    <header v-if="!hideChrome" class="app-header">
      <div class="header-content">
        <RouterLink to="/" class="logo">🏥 MedBooking</RouterLink>
        <nav class="nav-links">
          <RouterLink to="/">Trang chủ</RouterLink>
          <template v-if="authStore.isAuthenticated">
            <RouterLink v-if="authStore.isPatient" to="/profile">Hồ sơ của tôi</RouterLink>
            <RouterLink v-if="authStore.isDoctor" to="/doctor-dashboard">Quản lý lịch khám</RouterLink>
            <button @click="authStore.logout()" class="btn-logout">Đăng xuất</button>
          </template>
          <template v-else>
            <RouterLink to="/auth" class="btn-login">Đăng nhập</RouterLink>
          </template>
        </nav>
      </div>
    </header>

    <main class="app-main">
      <RouterView />
    </main>

    <footer v-if="!hideChrome" class="app-footer">
      <p>&copy; 2026 MedBooking - Hệ thống đặt lịch khám bệnh trực tuyến</p>
    </footer>
  </div>
</template>

<style>
.app-header {
  background: #ffffff;
  border-bottom: 1px solid var(--border-color);
  position: sticky;
  top: 0;
  z-index: 100;
}
.header-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0.875rem 1.5rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.logo {
  font-size: 1.25rem;
  font-weight: 800;
  color: var(--primary);
  text-decoration: none;
}
.nav-links {
  display: flex;
  align-items: center;
  gap: 1.25rem;
}
.nav-links a {
  text-decoration: none;
  color: var(--text-secondary);
  font-weight: 600;
  font-size: 0.9rem;
  transition: color 0.2s;
}
.nav-links a:hover, .nav-links a.router-link-active {
  color: var(--primary);
}
.btn-login {
  background: var(--primary) !important;
  color: #ffffff !important;
  padding: 0.5rem 1.25rem;
  border-radius: var(--radius-md);
}
.btn-logout {
  background: none;
  border: 1.5px solid var(--border-color);
  padding: 0.4rem 1rem;
  border-radius: var(--radius-md);
  cursor: pointer;
  font-weight: 600;
  font-size: 0.85rem;
  color: var(--text-secondary);
}
.btn-logout:hover { border-color: #ef4444; color: #ef4444; }
.app-footer {
  text-align: center;
  padding: 2rem;
  color: var(--text-secondary);
  font-size: 0.85rem;
  border-top: 1px solid var(--border-color);
  margin-top: 3rem;
}
</style>