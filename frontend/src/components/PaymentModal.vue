<template>
  <div class="modal-backdrop" @click.self="$emit('close')">
    <div class="modal-content">
      <h3 class="modal-title">Xác Nhận Thanh Toán</h3>
      <p class="modal-subtitle">Vui lòng chọn phương thức thanh toán cho đơn khám #{{ appointment.id }}</p>

      <div class="payment-methods">
        <label :class="['method-card', { active: selectedMethod === 'momo' }]">
          <input type="radio" value="momo" v-model="selectedMethod" />
          <div class="method-info">
            <strong>Ví MoMo</strong>
            <span>Thanh toán quét mã QR nhanh chóng</span>
          </div>
        </label>

        <label :class="['method-card', { active: selectedMethod === 'vnpay' }]">
          <input type="radio" value="vnpay" v-model="selectedMethod" />
          <div class="method-info">
            <strong>Cổng VNPay</strong>
            <span>Thẻ ATM / Banking nội địa</span>
          </div>
        </label>

        <label :class="['method-card', { active: selectedMethod === 'cash' }]">
          <input type="radio" value="cash" v-model="selectedMethod" />
          <div class="method-info">
            <strong>Tiền Mặt</strong>
            <span>Thanh toán trực tiếp tại viện</span>
          </div>
        </label>
      </div>

      <div v-if="errorMsg" class="error-banner">{{ errorMsg }}</div>

      <div class="modal-actions">
        <button @click="$emit('close')" class="btn btn-outline">Hủy Bỏ</button>
        <button @click="handleConfirm" class="btn btn-primary" :disabled="loading">
          {{ loading ? 'Đang xử lý...' : 'Xác Nhận Thanh Toán' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import api from '../api/axios';

const props = defineProps({
  appointment: {
    type: Object,
    required: true
  }
});

const emit = defineEmits(['close', 'success']);

const selectedMethod = ref('momo');
const loading = ref(false);
const errorMsg = ref('');

const handleConfirm = async () => {
  loading.value = true;
  errorMsg.value = '';
  try {
    const res = await api.post('/api/payments/confirm', {
      appointmentId: props.appointment.id,
      paymentMethod: selectedMethod.value
    });
    if (res.status === 200) {
      emit('success');
      emit('close');
    }
  } catch (err) {
    errorMsg.value = err.message;
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.modal-title {
  font-size: 1.25rem;
  font-weight: 800;
  margin-bottom: 0.25rem;
}

.modal-subtitle {
  font-size: 0.9rem;
  color: var(--text-secondary);
  margin-bottom: 1.5rem;
}

.payment-methods {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  margin-bottom: 1.5rem;
}

.method-card {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1rem;
  border: 1.5px solid var(--border-color);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 0.2s;
}

.method-card:hover, .method-card.active {
  border-color: var(--primary);
  background: var(--primary-light);
}

.method-info {
  display: flex;
  flex-direction: column;
}

.method-info span {
  font-size: 0.8rem;
  color: var(--text-secondary);
}

.error-banner {
  background: var(--status-cancelled-bg);
  color: var(--status-cancelled-text);
  padding: 0.5rem 0.75rem;
  border-radius: var(--radius-sm);
  font-size: 0.85rem;
  margin-bottom: 1rem;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
}
</style>
