import axios from 'axios';

const api = axios.create({
  baseURL: '',
  headers: {
    'Content-Type': 'application/json'
  }
});

// Request Interceptor: Tự động đính kèm Token nếu người dùng đã đăng nhập
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Response Interceptor: Tự động bóc tách ApiResponse<T> hoặc lấy thông báo lỗi chi tiết
api.interceptors.response.use(
  (response) => {
    return response.data;
  },
  (error) => {
    const errorMsg = error.response?.data?.message || error.message || 'Có lỗi hệ thống xảy ra, vui lòng thử lại sau!';
    return Promise.reject(new Error(errorMsg));
  }
);

export default api;
