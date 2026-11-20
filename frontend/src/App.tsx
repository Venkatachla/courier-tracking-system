
import { Navigate, Route, Routes } from 'react-router-dom';

import { Navbar } from './components/Navbar';
import { ProtectedRoute, RoleRoute } from './auth/RouteGuards';
import { useAuth } from './auth/AuthContext';

import { LoginPage, RegisterPage } from './pages/AuthPages';
import { CustomerPage } from './pages/CustomerPage';
import { PublicTrackingPage } from './pages/PublicTrackingPage';
import { ShipmentDetailsPage } from './pages/ShipmentDetailsPage';

function UnauthorizedPage() {
  return (
    <main className="page">
      <h1>Access denied</h1>
      <p>You do not have permission to access this page.</p>
    </main>
  );
}

function NotFoundPage() {
  return (
    <main className="page">
      <h1>404 - Page not found</h1>
      <p>The requested page does not exist.</p>
    </main>
  );
}

export default function App() {
  const { session, role } = useAuth();

  return (
    <>
      <Navbar />

      <Routes>
        <Route path="/" element={<PublicTrackingPage />} />
        <Route path="/track/:trackingNumber" element={<PublicTrackingPage />} />

        <Route
          path="/login"
          element={
            session
              ? <Navigate to={role === 'CUSTOMER' ? '/customer' : '/'} replace />
              : <LoginPage />
          }
        />

        <Route
          path="/register"
          element={
            session
              ? <Navigate to="/customer" replace />
              : <RegisterPage />
          }
        />

        <Route element={<ProtectedRoute />}>
          <Route path="/shipments/:id" element={<ShipmentDetailsPage />} />

          <Route element={<RoleRoute roles={['CUSTOMER']} />}>
            <Route path="/customer" element={<CustomerPage />} />
          </Route>
        </Route>

        <Route path="/unauthorized" element={<UnauthorizedPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </>
  );
}
