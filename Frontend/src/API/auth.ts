import instance from './axios';
import type { AxiosResponse } from 'axios';
import type { AuthResponse } from '../customTypes';

export async function checkLogin(email: string, password: string): Promise<AxiosResponse<AuthResponse>> {
  return instance.post<AuthResponse>('/auth/login', { username: email, pass: password });
}

export async function checkSession(): Promise<AxiosResponse<AuthResponse>> {
  return instance.get<AuthResponse>('/auth/session');
}

export async function logout(): Promise<AxiosResponse<void>> {
  return instance.post<void>('/auth/logout');
}

export async function register(email: string, password: string, username: string): Promise<AxiosResponse<AuthResponse>> {
  return instance.post<AuthResponse>('/auth/register', { username, pass: password, email });
}
