import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from './AuthContext';
import type { Role } from '../api/types';

export function ProtectedRoute() { const { session } = useAuth(); const location = useLocation(); return session ? <Outlet /> : <Navigate to="/login" replace state={{ from: location.pathname }} />; }
export function RoleRoute({ roles }: { roles: Role[] }) { const { role } = useAuth(); return role && roles.includes(role) ? <Outlet /> : <Navigate to="/unauthorized" replace />; }
