import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import api from "../api/client.js";

const initialState = {
  title: "",
  description: "",
  priority: "LOW",
  queueId: ""
};

export default function CreateTicket() {
  const [form, setForm] = useState(initialState);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [queues, setQueues] = useState([]);
  const navigate = useNavigate();

  React.useEffect(() => {
    api
      .get("/queues")
      .then((response) => setQueues(response.data))
      .catch(() => setQueues([]));
  }, []);

  const handleChange = (event) => {
    setForm((prev) => ({ ...prev, [event.target.name]: event.target.value }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setLoading(true);
    setError("");
    try {
      const payload = {
        title: form.title,
        description: form.description,
        priority: form.priority,
        queueId: form.queueId ? Number(form.queueId) : null
      };
      await api.post("/tickets", payload);
      navigate("/tickets");
    } catch (err) {
      setError(err?.response?.data?.message || "Unable to create ticket.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <h1>Create Ticket</h1>
          <p className="muted">Provide clear details so support can respond quickly.</p>
        </div>
      </header>
      <form className="card form-grid" onSubmit={handleSubmit}>
        <label>
          Title
          <input name="title" value={form.title} onChange={handleChange} required />
        </label>
        <label>
          Description
          <textarea
            name="description"
            value={form.description}
            onChange={handleChange}
            rows="6"
            required
          />
        </label>
        <label>
          Priority
          <select name="priority" value={form.priority} onChange={handleChange}>
            <option value="LOW">LOW</option>
            <option value="MEDIUM">MEDIUM</option>
            <option value="HIGH">HIGH</option>
          </select>
        </label>
        <label>
          Queue
          <select name="queueId" value={form.queueId} onChange={handleChange}>
            <option value="">Unassigned</option>
            {queues.map((queue) => (
              <option key={queue.id} value={queue.id}>
                {queue.name}
              </option>
            ))}
          </select>
        </label>
        {error ? <div className="error-banner">{error}</div> : null}
        <div className="form-actions">
          <button className="primary-button" type="submit" disabled={loading}>
            {loading ? "Creating..." : "Create Ticket"}
          </button>
        </div>
      </form>
    </section>
  );
}
