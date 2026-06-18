import React from "react";
import { NavLink } from "react-router-dom";
import { useAuth } from "../hooks/useAuth.js";

export default function NavBar() {
  const { user, logout } = useAuth();

  return (
    <header className="nav-bar">
      <div className="nav-brand">
        <span className="brand-title">Support Desk</span>
        <span className="brand-subtitle">Ticketing System</span>
      </div>
      <nav className="nav-links">
        <NavLink to="/dashboard">Dashboard</NavLink>
        <NavLink to="/tickets">Tickets</NavLink>
        <NavLink to="/create">Create</NavLink>
        {user?.role === "ROLE_ADMIN" ? (
          <>
            <NavLink to="/admin">Admin</NavLink>
            <NavLink to="/admin/queues">Queues</NavLink>
            <NavLink to="/admin/users">Users</NavLink>
          </>
        ) : null}
      </nav>
      <div className="nav-user">
        <div>
          <p className="nav-name">{user?.name}</p>
          <p className="nav-role">{user?.role?.replace("ROLE_", "")}</p>
        </div>
        <button className="secondary-button" onClick={logout} type="button">
          Logout
        </button>
      </div>
    </header>
  );
}
