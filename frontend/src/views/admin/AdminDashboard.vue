<template>
  <div class="admin-page">
    <div class="admin-header">
      <h1>🛡️ Bảng Điều Khiển Quản Trị</h1>
      <button @click="logout" class="btn btn-danger btn-sm">Đăng xuất</button>
    </div>

    <div class="stats-grid" v-if="stats">
      <div class="stat-card"><div class="stat-icon">👥</div><div class="stat-info"><h3>Tổng tài khoản</h3><p class="stat-number">{{ stats.totalUsers }}</p></div></div>
      <div class="stat-card"><div class="stat-icon">🩺</div><div class="stat-info"><h3>Bác sĩ</h3><p class="stat-number">{{ stats.totalDoctors }}</p></div></div>
      <div class="stat-card"><div class="stat-icon">🧑‍🦽</div><div class="stat-info"><h3>Bệnh nhân</h3><p class="stat-number">{{ stats.totalPatients }}</p></div></div>
      <div class="stat-card"><div class="stat-icon">🏥</div><div class="stat-info"><h3>Chuyên khoa</h3><p class="stat-number">{{ stats.totalSpecialties }}</p></div></div>
      <div class="stat-card"><div class="stat-icon">📅</div><div class="stat-info"><h3>Tổng lịch hẹn</h3><p class="stat-number">{{ stats.totalAppointments }}</p></div></div>
      <div class="stat-card"><div class="stat-icon">⏳</div><div class="stat-info"><h3>Chờ thanh toán</h3><p class="stat-number">{{ stats.pendingAppointments }}</p></div></div>
      <div class="stat-card"><div class="stat-icon">✅</div><div class="stat-info"><h3>Đã xác nhận</h3><p class="stat-number">{{ stats.confirmedAppointments }}</p></div></div>
      <div class="stat-card"><div class="stat-icon">❌</div><div class="stat-info"><h3>Đã hủy</h3><p class="stat-number">{{ stats.cancelledAppointments }}</p></div></div>
    </div>

    <nav class="admin-nav">
      <router-link to="/admin/users" class="nav-card">👤 Quản lý Tài khoản</router-link>
      <router-link to="/admin/patients" class="nav-card">🧑‍🦽 Quản lý Bệnh nhân</router-link>
      <router-link to="/admin/doctors" class="nav-card">🩺 Quản lý Bác sĩ</router-link>
      <router-link to="/admin/appointments" class="nav-card">📅 Quản lý Lịch hẹn</router-link>
    </nav>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import api from '../../api/axios';

const router = useRouter();
const stats = ref(null);

onMounted(async () => {
  try {
    const res = await api.get('/api/admin/dashboard');
    if (res.status === 200) stats.value = res.data;
  } catch (e) { console.error(e); }
});

const logout = () => {
  localStorage.removeItem('admin_token');
  localStorage.removeItem('admin_user');
  router.push('/admin/login');
};
</script>

<style scoped>
.admin-page { max-width: 1100px; margin: 0 auto; padding: 2rem 1.5rem; }
.admin-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 2rem; }
.admin-header h1 { font-size: 1.5rem; font-weight: 800; }
.stats-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 1rem; margin-bottom: 2.5rem; }
.stat-card { background: #fff; border: 1px solid #e2e8f0; border-radius: 12px; padding: 1.25rem; display: flex; align-items: center; gap: 1rem; }
.stat-icon { font-size: 2rem; }
.stat-info h3 { font-size: 0.8rem; color: #64748b; }
.stat-number { font-size: 1.5rem; font-weight: 800; color: #0d9488; }
.admin-nav { display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 1rem; }
.nav-card {
  display: block; padding: 1.5rem; background: #fff; border: 1.5px solid #e2e8f0;
  border-radius: 12px; text-decoration: none; color: #0f172a; font-weight: 700;
  font-size: 1rem; transition: all 0.2s; text-align: center;
}
.nav-card:hover { border-color: #0d9488; background: #ccfbf1; transform: translateY(-2px); }
.btn-sm { padding: 0.4rem 1rem; font-size: 0.85rem; }
</style>