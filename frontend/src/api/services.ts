import { api } from './client';
import type { HistoryEvent, Page, Shipment, Timeline, User } from './types';

export const shipmentsApi = {
  list: (page = 0, size = 10) => api.get<Page<Shipment>>('/api/shipments/my', { params: { page, size } }).then((r) => r.data),
  get: (id: number) => api.get<Shipment>(`/api/shipments/${id}`).then((r) => r.data),
  create: (body: { receiverName: string; receiverAddress: string; receiverPhone: string; weight: number }) => api.post<Shipment>('/api/shipments', body).then((r) => r.data),
  assign: (id: number, courierId: number) => api.put<Shipment>(`/api/shipments/${id}/assign`, { courierId }).then((r) => r.data),
  updateStatus: (id: number, status: string, location: string, description: string) => api.put<Shipment>(`/api/shipments/${id}/status`, { status, location, description }).then((r) => r.data),
  cancel: (id: number) => api.put<Shipment>(`/api/shipments/${id}/cancel`).then((r) => r.data)
};
export const usersApi = () => api.get<User[]>('/api/users').then((r) => r.data);
export const trackingApi = {
  timeline: (number: string) => api.get<Timeline>(`/api/track/${encodeURIComponent(number)}`).then((r) => r.data),
  history: (number: string) => api.get<HistoryEvent[]>(`/api/tracking/${encodeURIComponent(number)}/history`).then((r) => r.data)
};
