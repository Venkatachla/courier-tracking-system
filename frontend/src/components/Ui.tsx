import type { ReactNode } from 'react';
import type { ShipmentStatus } from '../api/types';

export function StatusBadge({ status }: { status: ShipmentStatus }) { return <span className={`badge status-${status.toLowerCase()}`}>{status.replaceAll('_', ' ')}</span>; }
export function Spinner({ label = 'Loading' }: { label?: string }) { return <div className="spinner-wrap" role="status"><span className="spinner" /><span>{label}</span></div>; }
export function ErrorBanner({ children }: { children: ReactNode }) { return <div className="error-banner" role="alert">{children}</div>; }
export function EmptyState({ children }: { children: ReactNode }) { return <div className="empty-state">{children}</div>; }
export function Toast({ children, onClose }: { children: ReactNode; onClose: () => void }) { return <div className="toast" role="status">{children}<button aria-label="Dismiss notification" onClick={onClose}>×</button></div>; }
export function Modal({ title, onClose, children }: { title: string; onClose: () => void; children: ReactNode }) { return <div className="modal-backdrop" onMouseDown={(e) => { if (e.target === e.currentTarget) onClose(); }}><section className="modal" role="dialog" aria-modal="true" aria-label={title}><header><h2>{title}</h2><button className="icon-button" aria-label="Close dialog" onClick={onClose}>×</button></header>{children}</section></div>; }
export function DataTable({ children }: { children: ReactNode }) { return <div className="table-scroll"><table>{children}</table></div>; }
export function Pager({ page, totalPages, onChange }: { page: number; totalPages: number; onChange: (n: number) => void }) { return <nav className="pager" aria-label="Pagination"><button className="button secondary" disabled={page <= 0} onClick={() => onChange(page - 1)}>Previous</button><span>Page {totalPages ? page + 1 : 0} of {totalPages}</span><button className="button secondary" disabled={page + 1 >= totalPages} onClick={() => onChange(page + 1)}>Next</button></nav>; }
