import React, { useEffect, useState } from "react";
import api from "../api/client.js";

const initialForm = {
  name: "",
  email: "",
  password: "",
  role: "ROLE_USER"
};

export default function AdminUsers() {
  const [users, setUsers] = useState([]);
  const [form, setForm] = useState(initialForm);
  const [error, setError] = useState("");

  const loadUsers = async () => {
    const response = await api.get("/users");
    setUsers(response.data);
  };

  useEffect(() => {
    loadUsers().catch(() => setError("Unable to load users."));
  }, []);

  const handleChange = (event) => {
    const { name, value, type, checked } = event.target;
    setForm((prev) => ({ ...prev, [name]: type === "checkbox" ? checked : value }));
  };

  const createUser = async (event) => {
    event.preventDefault();
    setError("");
    try {
      await api.post("/users", form);
      setForm(initialForm);
      await loadUsers();
    } catch (err) {
      setError(err?.response?.data?.message || "Unable to create user.");
    }
  };

  const updateRole = async (userId, role) => {
    try {
      await api.put(`/users/${userId}/role`, { role });
      await loadUsers();
    } catch (err) {
      setError(err?.response?.data?.message || "Unable to update role.");
    }
  };

  const updateActive = async (userId, active) => {
    try {
      if (active) {
        await api.put(`/users/${userId}/activate`);
      } else {
        await api.put(`/users/${userId}/deactivate`);
      }
      await loadUsers();
    } catch (err) {
      setError(err?.response?.data?.message || "Unable to update status.");
    }
  };

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <h1>User Management</h1>
          <p className="muted">Create users and control who can be assigned tickets.</p>
        </div>
      </header>

      {error ? <div className="error-banner">{error}</div> : null}

      <div className="grid-2">
        <form className="card form-grid" onSubmit={createUser}>
          <h3>Create User</h3>
          <label>
            Name
            <input name="name" value={form.name} onChange={handleChange} required />
          </label>
          <label>
            Email
            <input type="email" name="email" value={form.email} onChange={handleChange} required />
          </label>
          <label>
            Password
            <input
              type="password"
              name="password"
              value={form.password}
              onChange={handleChange}
              required
            />
          </label>
          <label>
            Role
            <select name="role" value={form.role} onChange={handleChange}>
              <option value="ROLE_USER">Customer</option>
              <option value="ROLE_AGENT">Support Agent</option>
              <option value="ROLE_ADMIN">Admin</option>
            </select>
          </label>
          <button className="primary-button" type="submit">
            Create User
          </button>
        </form>

        <div className="card">
          <h3>All Users</h3>
          <div className="user-list">
            {users.map((user) => (
              <div key={user.id} className="user-row">
                <div>
                  <strong>{user.name}</strong>
                  <p className="muted">{user.email}</p>
                  <p className="muted">{user.active ? "Active" : "Deactivated"}</p>
                </div>
                <div className="user-actions">
                  <select
                    value={user.role}
                    onChange={(event) => updateRole(user.id, event.target.value)}
                  >
                    <option value="ROLE_USER">Customer</option>
                    <option value="ROLE_AGENT">Support Agent</option>
                    <option value="ROLE_ADMIN">Admin</option>
                  </select>
                  <button
                    className="secondary-button"
                    type="button"
                    onClick={() => updateActive(user.id, !user.active)}
                  >
                    {user.active ? "Remove (Deactivate)" : "Activate"}
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </section>
  );
}
