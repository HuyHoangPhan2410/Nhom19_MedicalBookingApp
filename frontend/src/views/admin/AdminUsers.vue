<template>
  <div class="admin-page">
    <div class="admin-header">
      <h1>👤 Quản Lý Tài Khoản</h1>
      <div class="header-actions">
        <router-link to="/admin" class="btn btn-outline btn-sm">← Dashboard</router-link>
        <button @click="openCreate" class="btn btn-primary btn-sm">+ Tạo tài khoản</button>
      </div>
    </div>

    <table class="data-table" v-if="users.length">
      <thead><tr><th>ID</th><th>Email</th><th>Vai trò</th><th>Trạng thái</th><th>Thao tác</th></tr></thead>
      <tbody>
        <tr v-for="u in users" :key="u.id">
          <td>{{ u.id }}</td>
          <td>{{ u.email }}</td>
          <td><span :class="['badge', 'badge-' + u.role]">{{ u.role }}</span></td>
          <td>{{ u.isActive ? '✅ Hoạt động' : '🔒 Khóa' }}</td>
          <td class="actions">
            <button @click="openEdit(u)" class="btn btn-outline btn-xs">Sửa</button>
            <button @click="toggleActive(u)" class="btn btn-outline btn-xs">{{ u.isActive ? 'Khóa' : 'Mở' }}</button>
            <button @click="handleDelete(u)" class="btn btn-danger btn-xs">Xóa</button>
          </td>
        </tr>
      </tbody>
    </table>

    <!-- Modal -->
    <div v-if="showModal" class="modal-backdrop" @click.self="showModal = false">
      <div class="modal-content">
        <h3>{{ editingId ? 'Sửa tài khoản' : 'Tạo tài khoản mới' }}</h3>
        <div class="form-group"><label>Email</label><input v-model="form.email" class="form-control" /></div>
        <div class="form-group"><label>Mật khẩu {{ editingId ? '(để trống nếu không đổi)' : '' }}</label><input v-model="form.password" type="password" class="form-control" /></div>
        <div class="form-group"><label>Vai trò</label>
          <select v-model="form.role" class="form-control">
            <option value="patient">patient</option><option value="doctor">doctor</option><option value="admin">admin</option>
          </select>
        </div>
        <div v-if="errorMsg" class="error-banner">{{ errorMsg }}</div>
        <div class="modal-actions">
          <button @click="showModal = false" class="btn btn-outline">Hủy</button>
          <button @click="handleSave" class="btn btn-primary">Lưu</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import api from '../../api/axios';

const users = ref([]);
const showModal = ref(false);
const editingId = ref(null);
const errorMsg = ref('');
const form = ref({ email: '', password: '', role: 'patient' });

const fetchUsers = async () => {
  const res = await api.get('/api/admin/users');
  if (res.status === 200) users.value = res.data;
};
onMounted(fetchUsers);

const openCreate = () => { editingId.value = null; form.value = { email: '', password: '', role: 'patient' }; errorMsg.value = ''; showModal.value = true; };
const openEdit = (u) => { editingId.value = u.id; form.value = { email: u.email, password: '', role: u.role }; errorMsg.value = ''; showModal.value = true; };

const handleSave = async () => {
  errorMsg.value = '';
  try {
    if (editingId.value) {
      const payload = { email: form.value.email, role: form.value.role };
      if (form.value.password) payload.password = form.value.password;
      await api.put(`/api/admin/users/${editingId.value}`, payload);
    } else {
      await api.post('/api/admin/users', form.value);
    }
    showModal.value = false;
    await fetchUsers();
  } catch (e) { errorMsg.value = e.message; }
};

const toggleActive = async (u) => {
  await api.put(`/api/admin/users/${u.id}`, { isActive: String(!u.isActive) });
  await fetchUsers();
};

const handleDelete = async (u) => {
  if (!confirm(`Xóa tài khoản ${u.email}?`)) return;
  try { await api.delete(`/api/admin/users/${u.id}`); await fetchUsers(); } catch (e) { alert(e.message); }
};
</script>

<style scoped>
.admin-page { max-width: 1100px; margin: 0 auto; padding: 2rem 1.5rem; }
.admin-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.5rem; }
.admin-header h1 { font-size: 1.4rem; font-weight: 800; }
.header-actions { display: flex; gap: 0.5rem; }
.data-table { width: 100%; border-collapse: collapse; background: #fff; border-radius: 12px; overflow: hidden; border: 1px solid #e2e8f0; }
.data-table th, .data-table td { padding: 0.75rem 1rem; text-align: left; border-bottom: 1px solid #e2e8f0; font-size: 0.9rem; }
.data-table th { background: #f8fafc; font-weight: 700; }
.actions { display: flex; gap: 0.35rem; }
.btn-xs { padding: 0.25rem 0.5rem; font-size: 0.75rem; }
.btn-sm { padding: 0.4rem 1rem; font-size: 0.85rem; }
.badge { padding: 0.2rem 0.6rem; border-radius: 9999px; font-size: 0.75rem; font-weight: 700; }
.badge-patient { background: #d1fae5; color: #047857; }
.badge-doctor { background: #e0e7ff; color: #4338ca; }
.badge-admin { background: #fef3c7; color: #b45309; }
.form-group { margin-bottom: 1rem; }
.form-group label { display: block; font-weight: 600; font-size: 0.85rem; margin-bottom: 0.25rem; }
.form-control { width: 100%; padding: 0.6rem 0.8rem; border: 1.5px solid #e2e8f0; border-radius: 8px; font-size: 0.9rem; outline: none; }
.error-banner { background: #ffe4e6; color: #be123c; padding: 0.5rem; border-radius: 8px; font-size: 0.85rem; margin-bottom: 1rem; }
.modal-actions { display: flex; justify-content: flex-end; gap: 0.75rem; }
</style>