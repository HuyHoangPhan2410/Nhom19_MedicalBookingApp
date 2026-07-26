<template>
  <div class="container auth-page">
    <div class="card auth-card">
      <template v-if="showOtpVerification">
        <form class="auth-form otp-form" @submit.prevent="handleVerifyEmail">
          <div class="otp-icon" aria-hidden="true">✉</div>
          <h2 class="form-title">Xác minh email</h2>
          <p class="otp-description">
            Chúng tôi đã gửi mã xác minh đến email của bạn.
          </p>
          <p class="otp-email">{{ pendingEmail }}</p>

          <div class="form-group">
            <label for="otp">Mã OTP gồm 6 chữ số</label>
            <input
              id="otp"
              :value="otp"
              type="text"
              inputmode="numeric"
              autocomplete="one-time-code"
              maxlength="6"
              required
              autofocus
              placeholder="000000"
              class="form-control otp-input"
              @input="handleOtpInput"
            />
          </div>

          <div v-if="errorMsg" class="error-banner">{{ errorMsg }}</div>
          <div v-if="successMsg" class="success-banner">{{ successMsg }}</div>

          <button type="submit" class="btn btn-primary btn-block btn-lg" :disabled="loading || otp.length !== 6">
            {{ loading ? 'Đang xác minh...' : 'Xác minh' }}
          </button>

          <button
            type="button"
            class="resend-btn"
            :disabled="loading || resendCountdown > 0"
            @click="handleResendOtp"
          >
            {{ resendCountdown > 0 ? `Gửi lại OTP sau ${resendCountdown}s` : 'Gửi lại OTP' }}
          </button>

          <button type="button" class="back-btn" @click="backToRegister">
            Quay lại đăng ký
          </button>
        </form>
      </template>

      <template v-else>
        <div class="auth-tabs">
          <button :class="['tab-btn', { active: isLoginTab }]" @click="switchTab(true)">
            Đăng Nhập
          </button>
          <button :class="['tab-btn', { active: !isLoginTab }]" @click="switchTab(false)">
            Đăng Ký Tài Khoản
          </button>
        </div>

        <form v-if="isLoginTab" class="auth-form" @submit.prevent="handleLogin">
          <h2 class="form-title">Chào Mừng Trở Lại</h2>

          <div class="form-group">
            <label>Email đăng nhập</label>
            <input v-model.trim="loginEmail" type="email" required placeholder="name@example.com" class="form-control" />
          </div>

          <div class="form-group">
            <label>Mật khẩu</label>
            <input v-model="loginPassword" type="password" required placeholder="••••••••" class="form-control" />
          </div>

          <div v-if="errorMsg" class="error-banner">{{ errorMsg }}</div>

          <button type="submit" class="btn btn-primary btn-block btn-lg" :disabled="loading">
            {{ loading ? 'Đang xác thực...' : 'Đăng Nhập' }}
          </button>
        </form>

        <form v-else class="auth-form" @submit.prevent="handleRegister">
          <h2 class="form-title">Đăng Ký Bệnh Nhân Mới</h2>

          <div class="form-grid">
            <div class="form-group">
              <label>Họ và tên bệnh nhân</label>
              <input v-model.trim="regForm.fullName" type="text" required placeholder="Nguyễn Văn A" class="form-control" />
            </div>

            <div class="form-group">
              <label>Email liên hệ</label>
              <input v-model.trim="regForm.email" type="email" required placeholder="name@example.com" class="form-control" />
            </div>

            <div class="form-group">
              <label>Mật khẩu</label>
              <input v-model="regForm.password" type="password" required placeholder="••••••••" class="form-control" />
            </div>

            <div class="form-group">
              <label>Ngày sinh</label>
              <input v-model="regForm.dob" type="date" required class="form-control" />
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
              <input v-model.trim="regForm.phone" type="tel" required placeholder="0901234567" class="form-control" />
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
              <input v-model.trim="regForm.address" type="text" placeholder="Nhập số nhà, tên đường, quận/huyện..." class="form-control" />
            </div>
          </div>

          <div v-if="errorMsg" class="error-banner">{{ errorMsg }}</div>

          <button type="submit" class="btn btn-primary btn-block btn-lg" :disabled="loading">
            {{ loading ? 'Đang tạo tài khoản...' : 'Tạo Tài Khoản Bệnh Nhân' }}
          </button>
        </form>
      </template>
    </div>
  </div>
</template>

<script setup>
import { onBeforeUnmount, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../stores/auth';

const authStore = useAuthStore();
const router = useRouter();

const isLoginTab = ref(true);
const showOtpVerification = ref(false);
const loading = ref(false);
const errorMsg = ref('');
const successMsg = ref('');
const pendingEmail = ref('');
const otp = ref('');
const resendCountdown = ref(0);
let countdownTimer = null;

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

const switchTab = (loginTab) => {
  isLoginTab.value = loginTab;
  errorMsg.value = '';
  successMsg.value = '';
};

const startResendCountdown = () => {
  clearInterval(countdownTimer);
  resendCountdown.value = 60;
  countdownTimer = setInterval(() => {
    resendCountdown.value -= 1;
    if (resendCountdown.value <= 0) {
      clearInterval(countdownTimer);
      countdownTimer = null;
    }
  }, 1000);
};

const handleOtpInput = (event) => {
  otp.value = event.target.value.replace(/\D/g, '').slice(0, 6);
  event.target.value = otp.value;
};

const handleLogin = async () => {
  loading.value = true;
  errorMsg.value = '';
  try {
    const res = await authStore.login(loginEmail.value, loginPassword.value);
    if (res.status === 200) {
      router.push(authStore.isDoctor ? '/doctor-dashboard' : '/');
    }
  } catch (err) {
    errorMsg.value = err.message;
    if (err.message === 'Vui lòng xác minh email trước khi đăng nhập.') {
      pendingEmail.value = loginEmail.value.trim().toLowerCase();
      otp.value = '';
      resendCountdown.value = 0;
      showOtpVerification.value = true;
    }
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
      pendingEmail.value = res.data?.email || regForm.value.email.trim().toLowerCase();
      otp.value = '';
      showOtpVerification.value = true;
      successMsg.value = '';
      startResendCountdown();
    }
  } catch (err) {
    errorMsg.value = err.message;
  } finally {
    loading.value = false;
  }
};

const handleVerifyEmail = async () => {
  loading.value = true;
  errorMsg.value = '';
  successMsg.value = '';
  try {
    const res = await authStore.verifyEmail(pendingEmail.value, otp.value);
    if (res.status === 200) {
      clearInterval(countdownTimer);
      router.push(authStore.isDoctor ? '/doctor-dashboard' : '/');
    }
  } catch (err) {
    errorMsg.value = err.message;
  } finally {
    loading.value = false;
  }
};

const handleResendOtp = async () => {
  loading.value = true;
  errorMsg.value = '';
  successMsg.value = '';
  try {
    const res = await authStore.resendOtp(pendingEmail.value);
    if (res.status === 200) {
      otp.value = '';
      successMsg.value = 'Mã OTP mới đã được gửi đến email của bạn.';
      startResendCountdown();
    }
  } catch (err) {
    errorMsg.value = err.message;
  } finally {
    loading.value = false;
  }
};

const backToRegister = () => {
  clearInterval(countdownTimer);
  countdownTimer = null;
  resendCountdown.value = 0;
  otp.value = '';
  errorMsg.value = '';
  successMsg.value = '';
  showOtpVerification.value = false;
  isLoginTab.value = false;
};

onBeforeUnmount(() => clearInterval(countdownTimer));
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

.error-banner,
.success-banner {
  padding: 0.65rem 0.8rem;
  border-radius: var(--radius-sm);
  font-size: 0.85rem;
  margin-bottom: 1rem;
}

.error-banner {
  background: var(--status-cancelled-bg);
  color: var(--status-cancelled-text);
}

.success-banner {
  background: #dcfce7;
  color: #166534;
}

.otp-form {
  text-align: center;
}

.otp-icon {
  width: 64px;
  height: 64px;
  display: grid;
  place-items: center;
  margin: 0 auto 1rem;
  border-radius: 50%;
  background: #e0f2fe;
  color: var(--primary);
  font-size: 1.8rem;
}

.otp-description {
  margin: -0.75rem 0 0.35rem;
  color: var(--text-secondary);
}

.otp-email {
  margin-bottom: 1.5rem;
  font-weight: 700;
  overflow-wrap: anywhere;
}

.otp-form .form-group {
  text-align: left;
}

.otp-input {
  padding: 0.85rem;
  text-align: center;
  font-size: 1.5rem;
  font-weight: 800;
  letter-spacing: 0.55rem;
}

.resend-btn,
.back-btn {
  display: block;
  margin: 1rem auto 0;
  border: 0;
  background: transparent;
  color: var(--primary);
  font-weight: 700;
  cursor: pointer;
}

.resend-btn:disabled {
  color: var(--text-secondary);
  cursor: not-allowed;
}

.back-btn {
  color: var(--text-secondary);
  font-weight: 600;
}

@media (max-width: 640px) {
  .auth-page {
    padding: 1.5rem 0;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .full-width {
    grid-column: auto;
  }

  .auth-form {
    padding: 1.5rem;
  }
}
</style>
