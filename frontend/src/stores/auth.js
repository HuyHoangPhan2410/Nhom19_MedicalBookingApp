import { defineStore } from 'pinia';
import api from '../api/axios';

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: JSON.parse(localStorage.getItem('user')) || null,
    token: localStorage.getItem('token') || ''
  }),
  getters: {
    isAuthenticated: (state) => !!state.token,
    isPatient: (state) => state.user?.role === 'patient',
    isDoctor: (state) => state.user?.role === 'doctor',
    isAdmin: (state) => state.user?.role === 'admin'
  },
  actions: {
    saveSession(user) {
      this.token = user.token;
      this.user = user;
      localStorage.setItem('token', this.token);
      localStorage.setItem('user', JSON.stringify(this.user));
    },
    async login(email, password) {
      const res = await api.post('/api/auth/login', { email, password });
      if (res.status === 200 && res.data?.token) {
        this.saveSession(res.data);
      }
      return res;
    },
    async registerPatient(payload) {
      // A pending account must not be persisted as an authenticated session.
      return api.post('/api/auth/register-patient', payload);
    },
    async verifyEmail(email, otp) {
      const res = await api.post('/api/auth/verify-email', { email, otp });
      if (res.status === 200 && res.data?.token) {
        this.saveSession(res.data);
      }
      return res;
    },
    async resendOtp(email) {
      return api.post('/api/auth/resend-otp', { email });
    },
    logout() {
      this.token = '';
      this.user = null;
      localStorage.removeItem('token');
      localStorage.removeItem('user');
    }
  }
});
