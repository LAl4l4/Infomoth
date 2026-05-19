import axios from 'axios';

const instance = axios.create({
  timeout: 5000
});

const configClient = axios.create({ timeout: 5000 });
let apiBaseUrlPromise = null;

async function loadApiBaseUrl() {
  const configEndpoints = [
    '/config/baseurl',
    'http://localhost:8080/config/baseurl'
  ];

  for (const endpoint of configEndpoints) {
    try {
      const response = await configClient.get(endpoint);
      const apiBaseUrl = response?.data?.baseUrl;
      if (apiBaseUrl) {
        return apiBaseUrl;
      }
    } catch (error) {
      // try next candidate url
    }
  }

  throw new Error('Cannot load api base URL from /config/baseurl');
}

async function ensureBaseUrl() {
  if (instance.defaults.baseURL) {
    return instance.defaults.baseURL;
  }

  if (!apiBaseUrlPromise) {
    apiBaseUrlPromise = loadApiBaseUrl();
  }

  const apiBaseUrl = await apiBaseUrlPromise;
  instance.defaults.baseURL = apiBaseUrl;
  return apiBaseUrl;
}

// 请求拦截器：自动添加 Authorization header
instance.interceptors.request.use(
  async (config) => {
    await ensureBaseUrl();
    const token = localStorage.getItem('authToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// 响应拦截器：处理 401 未授权
instance.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      // Token 过期或无效，清除本地存储并重定向到登录
      localStorage.removeItem('authToken');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default instance;
