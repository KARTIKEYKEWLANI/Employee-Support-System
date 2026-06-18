import React, { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import api from "../api/client.js";
import { useAuth } from "../hooks/useAuth.js";
import { useNavigate } from "react-router-dom";

export default function Dashboard() {
  const { user } = useAuth();
  const [analytics, setAnalytics] = useState(null);
  const [error, setError] = useState("");
  const navigate = useNavigate();

  useEffect(() => {
    let active = true;
    api
      .get("/tickets/analytics")
      .then((response) => {
        if (active) setAnalytics(response.data);
      })
      .catch(() => {
        if (active) setError("Unable to load analytics right now.");
      });
    return () => {
      active = false;
    };
  }, []);

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <h1>Welcome, {user?.name}</h1>
          <p className="muted">Here is your personal ticket overview.</p>
        </div>
        <div className="page-actions">
          <Link className="primary-button" to="/create">
            New Ticket
          </Link>
          <Link className="secondary-button" to="/tickets">
            View Tickets
          </Link>
          <button
            className="secondary-button"
            type="button"
            onClick={async () => {
              try {
                await api.delete("/users/me");
                localStorage.removeItem("auth_token");
                localStorage.removeItem("auth_user");
                navigate("/login");
              } catch (err) {
                setError(err?.response?.data?.message || "Unable to deactivate account.");
              }
            }}
          >
            Deactivate Account
          </button>
        </div>
      </header>

      <div className="grid-4">
        {error ? <div className="error-banner">{error}</div> : null}
        <div className="stat-card">
          <p>My Tickets</p>
          <h2>{analytics?.totalTickets ?? "--"}</h2>
        </div>
        <div className="stat-card">
          <p>Open</p>
          <h2>{analytics?.openTickets ?? "--"}</h2>
        </div>
        <div className="stat-card">
          <p>In Progress</p>
          <h2>{analytics?.inProgressTickets ?? "--"}</h2>
        </div>
        <div className="stat-card">
          <p>Resolved</p>
          <h2>{analytics?.resolvedTickets ?? "--"}</h2>
        </div>
      </div>
    </section>
  );
}
