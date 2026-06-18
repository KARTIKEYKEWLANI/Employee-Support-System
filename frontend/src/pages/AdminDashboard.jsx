import React, { useEffect, useState } from "react";
import api from "../api/client.js";
import TicketList from "./TicketList.jsx";

export default function AdminDashboard() {
  const [analytics, setAnalytics] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    api
      .get("/tickets/analytics")
      .then((response) => {
        if (active) setAnalytics(response.data);
      })
      .catch(() => {
        if (active) setError("Unable to load admin analytics.");
      });
    return () => {
      active = false;
    };
  }, []);

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <h1>Admin Command Center</h1>
          <p className="muted">Review queue health, assignments, and resolution pace.</p>
        </div>
      </header>
      {error ? <div className="error-banner">{error}</div> : null}
      <div className="grid-4">
        <div className="stat-card">
          <p>Total Tickets</p>
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
      <div className="divider" />
      <TicketList />
    </section>
  );
}
