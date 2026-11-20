import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { errorMessage } from '../api/client';
import { shipmentsApi, trackingApi } from '../api/services';
import type { HistoryEvent, Shipment } from '../api/types';
import { useAuth } from '../auth/AuthContext';
import { Timeline } from '../components/Timeline';
import { EmptyState, ErrorBanner, Spinner, StatusBadge } from '../components/Ui';

export function ShipmentDetailsPage() {
  const { id } = useParams(); const { role } = useAuth(); const [shipment, setShipment] = useState<Shipment | null>(null); const [history, setHistory] = useState<HistoryEvent[]>([]); const [error, setError] = useState(''); const [loading, setLoading] = useState(true);
  useEffect(() => { let active = true; async function load() { try { const item = await shipmentsApi.get(Number(id)); const events = await trackingApi.history(item.trackingNumber); if (active) { setShipment(item); setHistory(events); } } catch (e) { if (active) setError(errorMessage(e)); } finally { if (active) setLoading(false); } } void load(); return () => { active = false; }; }, [id]);
  if (loading) return <main className="page"><Spinner label="Loading shipment details…" /></main>;
  if (error) return <main className="page"><ErrorBanner>{error}</ErrorBanner><Link to="/" className="text-link">Back to tracking</Link></main>;
  if (!shipment) return <EmptyState>Shipment not found.</EmptyState>;
  const timelineEvents = history.map((event) => ({ status: event.status, location: event.location, description: event.description, timestamp: event.timestamp }));
  return <main className="page"><Link to={role === 'ADMIN' ? '/admin' : role === 'COURIER' ? '/courier' : '/customer'} className="back-link">← Back to dashboard</Link><div className="page-heading"><div><span className="eyebrow">SHIPMENT DETAILS</span><h1>{shipment.trackingNumber}</h1></div><StatusBadge status={shipment.status} /></div><div className="details-grid"><section className="panel"><h2>Shipment information</h2><dl className="details-list"><div><dt>Receiver</dt><dd>{shipment.receiverName}</dd></div><div><dt>Address</dt><dd>{shipment.receiverAddress}</dd></div><div><dt>Phone</dt><dd>{shipment.receiverPhone}</dd></div><div><dt>Weight</dt><dd>{shipment.weight} kg</dd></div><div><dt>Sender ID</dt><dd>{shipment.senderId}</dd></div><div><dt>Courier ID</dt><dd>{shipment.assignedCourierId ?? 'Not assigned'}</dd></div><div><dt>Created</dt><dd>{new Date(shipment.createdAt).toLocaleString()}</dd></div><div><dt>Last updated</dt><dd>{new Date(shipment.updatedAt).toLocaleString()}</dd></div></dl></section><section className="panel"><h2>Tracking history</h2>{history.length ? <Timeline events={timelineEvents} currentStatus={shipment.status} /> : <EmptyState>No history is available yet.</EmptyState>}</section></div></main>;
}
