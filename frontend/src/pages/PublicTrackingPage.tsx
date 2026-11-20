import { useEffect, useState, type FormEvent } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { errorMessage } from '../api/client';
import { trackingApi } from '../api/services';
import type { Timeline as TimelineData } from '../api/types';
import { Timeline } from '../components/Timeline';
import { EmptyState, ErrorBanner, Spinner, StatusBadge } from '../components/Ui';

export function PublicTrackingPage() {
  const { trackingNumber } = useParams(); const navigate = useNavigate(); const [query, setQuery] = useState(trackingNumber || ''); const [data, setData] = useState<TimelineData | null>(null); const [loading, setLoading] = useState(Boolean(trackingNumber)); const [error, setError] = useState('');
  async function search(value = query) { const number = value.trim(); if (!number) { setError('Enter a tracking number.'); return; } setLoading(true); setError(''); setData(null); navigate(`/track/${encodeURIComponent(number)}`, { replace: true }); try { setData(await trackingApi.timeline(number)); } catch (e) { setError((e as { response?: { status?: number } }).response?.status === 404 ? 'Tracking number not found.' : errorMessage(e)); } finally { setLoading(false); } }
  useEffect(() => { if (trackingNumber) void search(trackingNumber); }, [trackingNumber]);
  function submit(e: FormEvent) { e.preventDefault(); void search(); }
  return <main className="page narrow"><div className="hero"><span className="eyebrow">PARCEL VISIBILITY</span><h1>Every delivery, in view.</h1><p>Enter your tracking number to see the latest journey update.</p><form className="track-search" onSubmit={submit}><label className="sr-only" htmlFor="tracking-number">Tracking number</label><input id="tracking-number" placeholder="CT-YYYYMMDD-XXXXXX" value={query} onChange={(e) => setQuery(e.target.value)} /><button className="button primary" disabled={loading}>{loading ? 'Searching…' : 'Track parcel'}</button></form></div>
    {loading && <Spinner label="Finding your shipment…" />}{error && <ErrorBanner>{error}</ErrorBanner>}{data && <section className="panel tracking-result"><div className="result-head"><div><span className="eyebrow">TRACKING NUMBER</span><h2>{data.trackingNumber}</h2></div><StatusBadge status={data.currentStatus} /></div><Timeline events={data.events} currentStatus={data.currentStatus} /></section>}{!loading && !error && !data && <EmptyState>Your shipment timeline will appear here.</EmptyState>}</main>;
}
