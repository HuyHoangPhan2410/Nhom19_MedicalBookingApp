<template>
  <header class="navbar-header">
    <div class="container navbar-container">
      <router-link to="/" class="brand-logo">
        <div class="logo-icon">+</div>
        <div class="logo-text">Med<span>Booking</span></div>
      </router-link>

      <nav class="nav-links">
        <router-link to="/" class="nav-item">Trang Chủ</router-link>
        <router-link v-if="authStore.isPatient" to="/profile" class="nav-item">Lịch Sử Khám</router-link>
        <router-link v-if="authStore.isDoctor" to="/doctor-dashboard" class="nav-item">Lịch Khám Tuần</router-link>
      </nav>

      <div class="nav-actions">
        <div v-if="authStore.isAuthenticated" class="user-profile">
          <span class="user-greeting">
            Xin chào, <strong>{{ authStore.isDoctor ? 'BS. ' : '' }}{{ authStore.user?.fullName }}</strong>
          </span>
          <button @click="handleLogout" class="btn btn-outline btn-sm">Đăng Xuất</button>
        </div>
        <router-link v-else to="/auth" class="btn btn-primary">
          Đăng Nhập / Đăng Ký
        </router-link>
      </div>
    </div>
  </header>
</template>

<script setup>
import { useAuthStore } from '../stores/auth';
import { useRouter } from 'vue-router';

const authStore = useAuthStore();
const router = useRouter();

const handleLogout = () => {
  authStore.logout();
  router.push('/');
};
</script>

<style scoped>
.navbar-header {
  background: #ffffff;
  border-bottom: 1px solid var(--border-color);
  position: sticky;
  top: 0;
  z-index: 100;
}

.navbar-container {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 70px;
}

.brand-logo {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  text-decoration: none;
}

.logo-icon {
  width: 36px;
  height: 36px;
  background: var(--primary);
  color: #ffffff;
  font-size: 1.5rem;
  font-weight: 800;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
}

.logo-text {
  font-size: 1.35rem;
  font-weight: 800;
  color: var(--text-primary);
}

.logo-text span {
  color: var(--primary);
}

.nav-links {
  display: flex;
  gap: 2rem;
}

.nav-item {
  text-decoration: none;
  color: var(--text-secondary);
  font-weight: 600;
  transition: color 0.2s;
}

.nav-item:hover, .router-link-active {
  color: var(--primary);
}

.user-profile {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.user-greeting {
  font-size: 0.9rem;
  color: var(--text-secondary);
}

.btn-sm {
  padding: 0.375rem 0.875rem;
  font-size: 0.85rem;
}
</style>
