export type Role = 'ADMIN' | 'COURIER' | 'CUSTOMER';
export type ShipmentStatus = 'CREATED' | 'PICKED_UP' | 'IN_TRANSIT' | 'OUT_FOR_DELIVERY' | 'DELIVERED' | 'FAILED_DELIVERY' | 'CANCELLED';
export interface User { id: number; name: string; email: string; role: Role; phone: string; createdAt: string }
export interface Shipment { id: number; trackingNumber: string; senderId: number; receiverName: string; receiverAddress: string; receiverPhone: string; weight: number; status: ShipmentStatus; assignedCourierId: number | null; createdAt: string; updatedAt: string; version: number }
export interface Page<T> { content: T[]; totalPages: number; totalElements: number; number: number; size: number; first: boolean; last: boolean }
export interface TimelineEntry { status: ShipmentStatus; location: string | null; description: string | null; timestamp: string }
export interface Timeline { trackingNumber: string; currentStatus: ShipmentStatus; events: TimelineEntry[] }
export interface HistoryEvent { id: number; eventId: string | null; trackingNumber: string; status: ShipmentStatus; location: string | null; description: string | null; updatedBy: number | null; timestamp: string; createdAt: string }
export interface AuthResponse { token: string; tokenType: string; expiresAt: string; userId: number; email: string; role: Role }
export interface ErrorResponse { timestamp?: string; status?: number; error?: string; message?: string; path?: string; requestId?: string; fieldErrors?: Record<string, string> | Array<{ field: string; message: string }> }
