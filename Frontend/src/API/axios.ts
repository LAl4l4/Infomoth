import axios from 'axios';
import { loadApiBaseUrl } from './config';

const instance = axios.create({
  timeout: 5000,
  withCredentials: true,
});

let apiBaseUrlPromise: Promise<string> | null = null;

async function ensureBaseUrl(): Promise<string> {
  if (instance.defaults.baseURL) {
    return instance.defaults.baseURL;
  }

  if (!apiBaseUrlPromise) {
    apiBaseUrlPromise = loadApiBaseUrl().catch((error) => {
      apiBaseUrlPromise = null;
      throw error;
    });
  }

  const apiBaseUrl = await apiBaseUrlPromise;
  instance.defaults.baseURL = apiBaseUrl;
  return apiBaseUrl;
}

// 请求拦截器：确保运行时 API 地址已加载；认证 Cookie 由浏览器自动携带
instance.interceptors.request.use(
  async (config) => {
    await ensureBaseUrl();
    return config;
  },
  (error) => Promise.reject(error)
);

// 响应拦截器：处理 401 未授权，提取后端错误信息
instance.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      // Cookie 由服务端管理，过期或无效时回到登录页
      window.location.href = '/login';
    }
    // 将后端返回的错误信息提取到 error.message 中，
    // 使所有 catch(e => setError(e.message)) 能显示真实错误原因
    if (error.response && error.response.data) {
      const data = error.response.data;
      if (typeof data === 'string') {
        error.message = data;
      } else if (data.error) {
        error.message = data.error;
      }
    }
    return Promise.reject(error);
  }
);

export default instance;
