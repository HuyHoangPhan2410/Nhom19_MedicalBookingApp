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
    isDoctor: (state) => state.user?.role === 'doctor'
  },
  actions: {
    async login(email, password) {
      const res = await api.post('/api/auth/login', { email, password });
      if (res.status === 200 && res.data) {
        this.token = res.data.token;
        this.user = res.data;
        localStorage.setItem('token', this.token);
        localStorage.setItem('user', JSON.stringify(this.user));
      }
      return res;
    },
    async registerPatient(payload) {
      const res = await api.post('/api/auth/register-patient', payload);
      if (res.status === 200 && res.data) {
        this.token = res.data.token;
        this.user = res.data;
        localStorage.setItem('token', this.token);
        localStorage.setItem('user', JSON.stringify(this.user));
      }
      return res;
    },
    logout() {
      this.token = '';
      this.user = null;
      localStorage.removeItem('token');
      localStorage.removeItem('user');
    }
  }
});
