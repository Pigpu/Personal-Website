// src/utils/request.ts
import axios from 'axios';

const service = axios.create({
  baseURL: 'https://shimizukaede.top/api'
});

service.interceptors.request.use(config => {
  const token = localStorage.getItem('token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

export default service;