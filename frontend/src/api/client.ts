import axios, { AxiosError } from 'axios';

export const SESSION_KEY = 'courier.session';
export const api = axios.create({ baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080', headers: { 'Content-Type': 'application/json' } });
let unauthorizedHandler: (() => void) | undefined;
let forbiddenHandler: (() => void) | undefined;
export const setAuthFailureHandlers = (onUnauthorized: () => void, onForbidden: () => void) => { unauthorizedHandler = onUnauthorized; forbiddenHandler = onForbidden; };

api.interceptors.request.use((config) => {
  const token = localStorage.getItem(SESSION_KEY);
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

api.interceptors.response.use((response) => response, (error: AxiosError) => {
  if (error.response?.status === 401) unauthorizedHandler?.();
  if (error.response?.status === 403) forbiddenHandler?.();
  return Promise.reject(error);
});

export function errorMessage(error: unknown): string {
  if (!axios.isAxiosError(error)) return error instanceof Error ? error.message : 'Something went wrong.';
  const data = error.response?.data as { message?: string; error?: string; fieldErrors?: Record<string, string> | Array<{ field: string; message: string }> } | undefined;
  if (data?.fieldErrors) {
    const fields = Array.isArray(data.fieldErrors) ? data.fieldErrors.map((item) => `${item.field}: ${item.message}`) : Object.entries(data.fieldErrors).map(([field, message]) => `${field}: ${message}`);
    if (fields.length) return fields.join(' · ');
  }
  if (data?.message) return data.message;
  if (data?.error) return data.error;
  if (error.response?.status === 404) return 'The requested item was not found.';
  if (error.response?.status === 409) return 'This update conflicts with the current record. Refresh and try again.';
  return error.message || 'Unable to reach the service.';
}
