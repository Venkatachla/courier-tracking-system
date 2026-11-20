import { Link } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';

export function Navbar() {
  const { session, user, role, logout } = useAuth();
  return <header className="topbar"><Link className="brand" to="/">Courier<span>Track</span></Link><nav aria-label="Main navigation"><Link to="/">Track parcel</Link>{role === 'CUSTOMER' && <Link to="/customer">My shipments</Link>}{role === 'COURIER' && <Link to="/courier">Deliveries</Link>}{role === 'ADMIN' && <Link to="/admin">Admin</Link>}{session && <><span className="account-name">{user?.name || session.email} <small>{role}</small></span><button className="button ghost" onClick={() => logout()}>Sign out</button></>}{!session && <Link className="button ghost" to="/login">Sign in</Link>}</nav></header>;
}
