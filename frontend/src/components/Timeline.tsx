import type { Timeline as TimelineData, TimelineEntry } from '../api/types';
import { StatusBadge } from './Ui';

export function Timeline({ events, currentStatus }: { events: TimelineEntry[]; currentStatus: TimelineData['currentStatus'] }) {
  const ordered = [...events].sort((a, b) => Date.parse(a.timestamp) - Date.parse(b.timestamp));
  const currentIndex = [...ordered].map((event) => event.status).lastIndexOf(currentStatus);
  if (!ordered.length) return <p className="muted">No tracking updates yet.</p>;
  return <ol className="timeline">{ordered.map((event, index) => <li className={index === currentIndex ? 'current' : ''} key={`${event.timestamp}-${index}`}><span className="timeline-dot" /><div className="timeline-card"><div className="timeline-heading"><StatusBadge status={event.status} /><time>{new Date(event.timestamp).toLocaleString()}</time></div>{event.location && <p><strong>Location:</strong> {event.location}</p>}{event.description && <p>{event.description}</p>}</div></li>)}</ol>;
}
