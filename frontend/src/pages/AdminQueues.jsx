import React, { useEffect, useState } from "react";
import api from "../api/client.js";

const initialQueue = {
  name: "",
  description: "",
  autoAssign: false
};

export default function AdminQueues() {
  const [queues, setQueues] = useState([]);
  const [agents, setAgents] = useState([]);
  const [members, setMembers] = useState([]);
  const [selectedQueue, setSelectedQueue] = useState(null);
  const [form, setForm] = useState(initialQueue);
  const [error, setError] = useState("");

  const loadQueues = async () => {
    const response = await api.get("/queues");
    setQueues(response.data);
  };

  const loadAgents = async () => {
    const response = await api.get("/users/assignable");
    setAgents(response.data);
  };

  const loadMembers = async (queueId) => {
    const response = await api.get(`/queues/${queueId}/members`);
    setMembers(response.data);
  };

  useEffect(() => {
    loadQueues().catch(() => setError("Unable to load queues."));
    loadAgents().catch(() => setError("Unable to load staff."));
  }, []);

  const handleQueueSelect = async (queue) => {
    setSelectedQueue(queue);
    if (queue) {
      await loadMembers(queue.id);
    } else {
      setMembers([]);
    }
  };

  const handleCreate = async (event) => {
    event.preventDefault();
    setError("");
    try {
      await api.post("/queues", form);
      setForm(initialQueue);
      await loadQueues();
    } catch (err) {
      setError(err?.response?.data?.message || "Unable to create queue.");
    }
  };

  const toggleMember = async (adminId) => {
    if (!selectedQueue) return;
    const isMember = members.some((member) => member.userId === adminId);
    try {
      if (isMember) {
        await api.delete(`/queues/${selectedQueue.id}/members/${adminId}`);
      } else {
        await api.post(`/queues/${selectedQueue.id}/members/${adminId}`);
      }
      await loadMembers(selectedQueue.id);
    } catch (err) {
      setError(err?.response?.data?.message || "Unable to update members.");
    }
  };

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <h1>Support Queues</h1>
          <p className="muted">Create queues, assign admins, and enable auto-routing.</p>
        </div>
      </header>

      {error ? <div className="error-banner">{error}</div> : null}

      <div className="grid-2">
        <form className="card form-grid" onSubmit={handleCreate}>
          <h3>Create Queue</h3>
          <label>
            Name
            <input
              name="name"
              value={form.name}
              onChange={(event) => setForm((prev) => ({ ...prev, name: event.target.value }))}
              required
            />
          </label>
          <label>
            Description
            <input
              name="description"
              value={form.description}
              onChange={(event) => setForm((prev) => ({ ...prev, description: event.target.value }))}
            />
          </label>
          <label className="checkbox-row">
            <input
              type="checkbox"
              checked={form.autoAssign}
              onChange={(event) => setForm((prev) => ({ ...prev, autoAssign: event.target.checked }))}
            />
            Auto-assign new tickets
          </label>
          <button className="primary-button" type="submit">
            Create Queue
          </button>
        </form>

        <div className="card">
          <h3>Queue List</h3>
          <div className="queue-list">
            {queues.map((queue) => (
              <button
                type="button"
                key={queue.id}
                className={`queue-item ${selectedQueue?.id === queue.id ? "active" : ""}`}
                onClick={() => handleQueueSelect(queue)}
              >
                <div>
                  <strong>{queue.name}</strong>
                  <p className="muted">{queue.description || "No description"}</p>
                </div>
                <span className="pill pill-medium">{queue.autoAssign ? "Auto" : "Manual"}</span>
              </button>
            ))}
          </div>
        </div>
      </div>

      {selectedQueue ? (
        <div className="card">
          <h3>Members for {selectedQueue.name}</h3>
          <div className="member-grid">
            {agents.map((agent) => {
              const isMember = members.some((member) => member.userId === agent.id);
              return (
                <label key={agent.id} className="member-item">
                  <input
                    type="checkbox"
                    checked={isMember}
                    onChange={() => toggleMember(agent.id)}
                  />
                  <span>
                    {agent.name} ({agent.email})
                  </span>
                </label>
              );
            })}
          </div>
        </div>
      ) : null}
    </section>
  );
}
