<template>
  <div class="card doctor-card">
    <div class="doctor-avatar">
      <div class="avatar-placeholder">{{ doctor.fullName.charAt(0) }}</div>
    </div>
    
    <div class="doctor-info">
      <div class="specialty-badge">{{ doctor.specialtyName }}</div>
      <h3 class="doctor-name">{{ doctor.fullName }}</h3>
      <p class="doctor-exp">Kinh nghiệm: <strong>{{ doctor.experienceYears }} năm</strong></p>
      <p class="doctor-fee">Phí khám: <span>{{ formatPrice(doctor.consultationFee) }} đ</span></p>
      
      <router-link :to="`/doctors/${doctor.userId}`" class="btn btn-primary btn-block">
        Đặt Lịch Khám
      </router-link>
    </div>
  </div>
</template>

<script setup>
defineProps({
  doctor: {
    type: Object,
    required: true
  }
});

const formatPrice = (price) => {
  if (!price) return '0';
  return new Intl.NumberFormat('vi-VN').format(price);
};
</script>

<style scoped>
.doctor-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 1rem;
}

.doctor-avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: var(--primary-light);
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-placeholder {
  font-size: 2rem;
  font-weight: 800;
  color: var(--primary);
}

.doctor-info {
  width: 100%;
}

.specialty-badge {
  display: inline-block;
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--primary);
  background: var(--primary-light);
  padding: 0.25rem 0.625rem;
  border-radius: 9999px;
  margin-bottom: 0.5rem;
}

.doctor-name {
  font-size: 1.1rem;
  font-weight: 700;
  margin-bottom: 0.25rem;
}

.doctor-exp {
  font-size: 0.875rem;
  color: var(--text-secondary);
  margin-bottom: 0.25rem;
}

.doctor-fee {
  font-size: 0.9rem;
  color: var(--text-secondary);
  margin-bottom: 1rem;
}

.doctor-fee span {
  font-weight: 800;
  color: var(--primary);
  font-size: 1.05rem;
}

.btn-block {
  width: 100%;
}
</style>
