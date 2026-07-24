<template>
  <div class="container auth-page">
    <div class="card auth-card">
      <div class="auth-tabs">
        <button 
          :class="['tab-btn', { active: isLoginTab }]" 
          @click="isLoginTab = true"
        >
          Đăng Nhập
        </button>
        <button 
          :class="['tab-btn', { active: !isLoginTab }]" 
          @click="isLoginTab = false"
        >
          Đăng Ký Tài Khoản
        </button>
      </div>

      <!-- Form Đăng Nhập -->
      <form v-if="isLoginTab" @submit.prevent="handleLogin" class="auth-form">
        <h2 class="form-title">Chào Mừng Trở Lại</h2>
        
        <div class="form-group">
          <label>Email đăng nhập</label>
          <input type="email" v-model="loginEmail" required placeholder="name@example.com" class="form-control" />
        </div>

        <div class="form-group">
          <label>Mật khẩu</label>
          <input type="password" v-model="loginPassword" required placeholder="••••••••" class="form-control" />
        </div>

        <div v-if="errorMsg" class="error-banner">{{ errorMsg }}</div>

        <button type="submit" class="btn btn-primary btn-block btn-lg" :disabled="loading">
          {{ loading ? 'Đang xác thực...' : 'Đăng Nhập' }}
        </button>
      </form>

      <!-- Form Đăng Ký -->
      <form v-else @submit.prevent="handleRegister" class="auth-form">
        <h2 class="form-title">Đăng Ký Bệnh Nhân Mới</h2>

        <div class="form-grid">
          <div class="form-group">
            <label>Họ và tên bệnh nhân</label>
            <input type="text" v-model="regForm.fullName" required placeholder="Nguyễn Văn A" class="form-control" />
          </div>

          <div class="form-group">
            <label>Email liên hệ</label>
            <input type="email" v-model="regForm.email" required placeholder="name@example.com" class="form-control" />
          </div>

          <div class="form-group">
            <label>Mật khẩu</label>
            <input type="password" v-model="regForm.password" required placeholder="••••••••" class="form-control" />
          </div>

          <div class="form-group">
            <label>Ngày sinh</label>
            <input type="date" v-model="regForm.dob" required class="form-control" />
          </div>

          <div class="form-group">
            <label>Giới tính</label>
            <select v-model="regForm.gender" class="form-control">
              <option value="male">Nam</option>
              <option value="female">Nữ</option>
              <option value="other">Khác</option>
            </select>
          </div>

          <div class="form-group">
            <label>Số điện thoại</label>
            <input type="tel" v-model="regForm.phone" required placeholder="0901234567" class="form-control" />
          </div>

          <div class="form-group">
            <label>Nhóm máu (không bắt buộc)</label>
            <select v-model="regForm.bloodType" class="form-control">
              <option value="">Không rõ</option>
              <option value="A+">A+</option>
              <option value="B+">B+</option>
              <option value="O+">O+</option>
              <option value="AB+">AB+</option>
            </select>
          </div>

          <div class="form-group full-width">
            <label>Địa chỉ thường trú</label>
            <input type="text" v-model="regForm.address" placeholder="Nhập số nhà, tên đường, quận/huyện..." class="form-control" />
          </div>
        </div>

        <div v-if="errorMsg" class="error-banner">{{ errorMsg }}</div>

        <button type="submit" class="btn btn-primary btn-block btn-lg" :disabled="loading">
          {{ loading ? 'Đang tạo tài khoản...' : 'Tạo Tài Khoản Bệnh Nhân' }}
        </button>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useAuthStore } from '../stores/auth';
import { useRouter } from 'vue-router';

const authStore = useAuthStore();
const router = useRouter();

const isLoginTab = ref(true);
const loading = ref(false);
const errorMsg = ref('');

const loginEmail = ref('');
const loginPassword = ref('');

const regForm = ref({
  email: '',
  password: '',
  fullName: '',
  dob: '1995-01-01',
  gender: 'male',
  phone: '',
  address: '',
  bloodType: 'O+'
});

const handleLogin = async () => {
  loading.value = true;
  errorMsg.value = '';
  try {
    const res = await authStore.login(loginEmail.value, loginPassword.value);
    if (res.status === 200) {
      if (authStore.isDoctor) {
        router.push('/doctor-dashboard');
      } else {
        router.push('/');
      }
    }
  } catch (err) {
    errorMsg.value = err.message;
  } finally {
    loading.value = false;
  }
};

const handleRegister = async () => {
  loading.value = true;
  errorMsg.value = '';
  try {
    const res = await authStore.registerPatient(regForm.value);
    if (res.status === 200) {
      router.push('/');
    }
  } catch (err) {
    errorMsg.value = err.message;
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.auth-page {
  padding: 4rem 0;
  display: flex;
  justify-content: center;
}

.auth-card {
  width: 100%;
  max-width: 550px;
  padding: 0;
  overflow: hidden;
}

.auth-tabs {
  display: flex;
  border-bottom: 1px solid var(--border-color);
  background: #f8fafc;
}

.tab-btn {
  flex: 1;
  padding: 1rem;
  font-size: 1rem;
  font-weight: 700;
  border: none;
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.2s;
}

.tab-btn.active {
  background: #ffffff;
  color: var(--primary);
  border-bottom: 2.5px solid var(--primary);
}

.auth-form {
  padding: 2rem;
}

.form-title {
  font-size: 1.5rem;
  font-weight: 800;
  margin-bottom: 1.5rem;
  text-align: center;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.full-width {
  grid-column: 1 / -1;
}

.form-group {
  margin-bottom: 1.25rem;
}

.form-group label {
  display: block;
  font-weight: 600;
  font-size: 0.875rem;
  margin-bottom: 0.375rem;
}

.form-control {
  width: 100%;
  padding: 0.625rem 0.875rem;
  border: 1.5px solid var(--border-color);
  border-radius: var(--radius-md);
  font-family: inherit;
  font-size: 0.9rem;
  outline: none;
}

.form-control:focus {
  border-color: var(--primary);
}

.btn-block {
  width: 100%;
}

.btn-lg {
  padding: 0.75rem 1.5rem;
  font-size: 1rem;
}

.error-banner {
  background: var(--status-cancelled-bg);
  color: var(--status-cancelled-text);
  padding: 0.5rem 0.75rem;
  border-radius: var(--radius-sm);
  font-size: 0.85rem;
  margin-bottom: 1rem;
}
</style>
