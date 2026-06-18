import React, { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../hooks/useAuth.js";

const initialForm = {
  name: "",
  email: "",
  password: ""
};

export default function AuthPage({ mode }) {
  const [form, setForm] = useState(initialForm);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const { login, register } = useAuth();

  const isRegister = mode === "register";

  const handleChange = (event) => {
    setForm((prev) => ({ ...prev, [event.target.name]: event.target.value }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError("");
    setLoading(true);
    try {
      if (isRegister) {
        await register(form);
      } else {
        await login({ email: form.email, password: form.password });
      }
      navigate("/dashboard");
    } catch (err) {
      setError(err?.response?.data?.message || "Unable to continue. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-layout">
      <section className="auth-card">
        <h1>{isRegister ? "Create your account" : "Welcome back"}</h1>
        <p className="muted">
          {isRegister
            ? "Register to create and track support tickets."
            : "Log in to manage your support tickets."}
        </p>
        <form className="form-grid" onSubmit={handleSubmit}>
          {isRegister ? (
            <label>
              Name
              <input name="name" value={form.name} onChange={handleChange} required />
            </label>
          ) : null}
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
          {error ? <div className="error-banner">{error}</div> : null}
          <button className="primary-button" type="submit" disabled={loading}>
            {loading ? "Please wait..." : isRegister ? "Register" : "Login"}
          </button>
        </form>
        <div className="auth-switch">
          {isRegister ? (
            <>
              Already have an account? <Link to="/login">Login</Link>
            </>
          ) : (
            <>
              New here? <Link to="/register">Create an account</Link>
            </>
          )}
        </div>
      </section>
      <section className="auth-aside">
        <h2>Support that moves fast.</h2>
        <p>
          Track priority tickets, keep customers informed, and help your team stay on top
          of every request.
        </p>
        <ul className="auth-highlights">
          <li>Role-based access with secure JWT authentication</li>
          <li>Clear status transitions from open to resolved</li>
          <li>Instant analytics at a glance</li>
        </ul>
      </section>
    </div>
  );
}
