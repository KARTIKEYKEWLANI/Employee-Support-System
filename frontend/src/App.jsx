import React from "react";
import { Navigate, Route, Routes } from "react-router-dom";
import AuthPage from "./pages/AuthPage.jsx";
import Dashboard from "./pages/Dashboard.jsx";
import CreateTicket from "./pages/CreateTicket.jsx";
import TicketList from "./pages/TicketList.jsx";
import AdminDashboard from "./pages/AdminDashboard.jsx";
import AdminQueues from "./pages/AdminQueues.jsx";
import AdminUsers from "./pages/AdminUsers.jsx";
import NavBar from "./components/NavBar.jsx";
import ProtectedRoute from "./components/ProtectedRoute.jsx";
import { useAuth } from "./hooks/useAuth.js";

export default function App() {
  const { user } = useAuth();
  const isAdmin = user?.role === "ROLE_ADMIN";
  const isAgent = user?.role === "ROLE_AGENT";

  return (
    <div className="app-shell">
      {user ? <NavBar /> : null}
      <main className="app-main">
        <Routes>
          <Route
            path="/"
            element={
              <Navigate
                to={
                  user ? (isAdmin ? "/admin" : isAgent ? "/tickets" : "/dashboard") : "/login"
                }
              />
            }
          />
          <Route path="/login" element={<AuthPage mode="login" />} />
          <Route path="/register" element={<AuthPage mode="register" />} />
          <Route
            path="/dashboard"
            element={
              <ProtectedRoute>
                <Dashboard />
              </ProtectedRoute>
            }
          />
          <Route
            path="/tickets"
            element={
              <ProtectedRoute>
                <TicketList />
              </ProtectedRoute>
            }
          />
          <Route
            path="/create"
            element={
              <ProtectedRoute>
                <CreateTicket />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin"
            element={
              <ProtectedRoute requireAdmin>
                <AdminDashboard />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin/queues"
            element={
              <ProtectedRoute requireAdmin>
                <AdminQueues />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin/users"
            element={
              <ProtectedRoute requireAdmin>
                <AdminUsers />
              </ProtectedRoute>
            }
          />
          <Route path="*" element={<Navigate to="/" />} />
        </Routes>
      </main>
    </div>
  );
}
