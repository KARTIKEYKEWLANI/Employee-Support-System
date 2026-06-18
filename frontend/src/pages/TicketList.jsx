import React, { useEffect, useState } from "react";
import api from "../api/client.js";
import { useAuth } from "../hooks/useAuth.js";

const statusOptions = ["OPEN", "IN_PROGRESS", "RESOLVED"];

export default function TicketList() {
  const { user } = useAuth();
  const [tickets, setTickets] = useState([]);
  const [agents, setAgents] = useState([]);
  const [queues, setQueues] = useState([]);
  const [tags, setTags] = useState([]);
  const [pageData, setPageData] = useState({ page: 0, totalPages: 0 });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [expandedTicket, setExpandedTicket] = useState(null);
  const [commentsMap, setCommentsMap] = useState({});
  const [commentDrafts, setCommentDrafts] = useState({});
  const [internalFlags, setInternalFlags] = useState({});
  const [ratingMap, setRatingMap] = useState({});
  const [ratingDrafts, setRatingDrafts] = useState({});
  const [attachmentsMap, setAttachmentsMap] = useState({});
  const [tagDrafts, setTagDrafts] = useState({});
  const [filters, setFilters] = useState({
    query: "",
    status: "",
    priority: "",
    tag: "",
    includeArchived: false
  });

  const loadTickets = async (page = 0, nextFilters = filters) => {
    setLoading(true);
    setError("");
    try {
      const params = new URLSearchParams({
        page,
        size: 8
      });
      if (nextFilters.query) params.append("query", nextFilters.query);
      if (nextFilters.status) params.append("status", nextFilters.status);
      if (nextFilters.priority) params.append("priority", nextFilters.priority);
      if (nextFilters.tag) params.append("tag", nextFilters.tag);
      if (nextFilters.includeArchived) params.append("includeArchived", "true");
      const response = await api.get(`/tickets?${params.toString()}`);
      setTickets(response.data.content);
      setPageData({ page: response.data.page, totalPages: response.data.totalPages });
    } catch (err) {
      setError(err?.response?.data?.message || "Unable to load tickets.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadTickets();
  }, []);

  useEffect(() => {
    if (user?.role === "ROLE_ADMIN") {
      api
        .get("/users/assignable")
        .then((response) => setAgents(response.data))
        .catch(() => setAgents([]));
    }
  }, [user]);

  useEffect(() => {
    api
      .get("/queues")
      .then((response) => setQueues(response.data))
      .catch(() => setQueues([]));
  }, []);

  useEffect(() => {
    api
      .get("/tickets/tags")
      .then((response) => setTags(response.data))
      .catch(() => setTags([]));
  }, []);

  const handleStatusChange = async (ticketId, status) => {
    try {
      await api.put(`/tickets/${ticketId}/status`, { status });
      setTickets((prev) =>
        prev.map((ticket) => (ticket.id === ticketId ? { ...ticket, status } : ticket))
      );
    } catch (err) {
      setError(err?.response?.data?.message || "Unable to update status.");
    }
  };

  const handleAssign = async (ticketId, assigneeId) => {
    try {
      const payload = { assigneeId: assigneeId ?? null };
      const response = await api.put(`/tickets/${ticketId}/assign`, payload);
      setTickets((prev) => prev.map((ticket) => (ticket.id === ticketId ? response.data : ticket)));
    } catch (err) {
      setError(err?.response?.data?.message || "Unable to assign ticket.");
    }
  };

  const handleTeamAssign = async (ticketId, assigneeIds) => {
    try {
      const response = await api.put(`/tickets/${ticketId}/team`, { assigneeIds });
      setTickets((prev) => prev.map((ticket) => (ticket.id === ticketId ? response.data : ticket)));
    } catch (err) {
      setError(err?.response?.data?.message || "Unable to update team.");
    }
  };

  const handleQueueUpdate = async (ticketId, queueId) => {
    try {
      const response = await api.put(`/tickets/${ticketId}/queue`, { queueId });
      setTickets((prev) => prev.map((ticket) => (ticket.id === ticketId ? response.data : ticket)));
    } catch (err) {
      setError(err?.response?.data?.message || "Unable to update queue.");
    }
  };

  const loadComments = async (ticketId) => {
    try {
      const response = await api.get(`/tickets/${ticketId}/comments`);
      setCommentsMap((prev) => ({ ...prev, [ticketId]: response.data }));
    } catch (err) {
      setError(err?.response?.data?.message || "Unable to load comments.");
    }
  };

  const loadRating = async (ticketId) => {
    try {
      const response = await api.get(`/tickets/${ticketId}/rating`);
      setRatingMap((prev) => ({ ...prev, [ticketId]: response.data }));
    } catch (err) {
      setRatingMap((prev) => ({ ...prev, [ticketId]: null }));
    }
  };

  const loadAttachments = async (ticketId) => {
    try {
      const response = await api.get(`/tickets/${ticketId}/attachments`);
      setAttachmentsMap((prev) => ({ ...prev, [ticketId]: response.data }));
    } catch (err) {
      setError(err?.response?.data?.message || "Unable to load attachments.");
    }
  };

  const toggleComments = async (ticketId) => {
    if (expandedTicket === ticketId) {
      setExpandedTicket(null);
      return;
    }
    setExpandedTicket(ticketId);
    await loadComments(ticketId);
    await loadRating(ticketId);
    await loadAttachments(ticketId);
  };

  const handleCommentChange = (ticketId, value) => {
    setCommentDrafts((prev) => ({ ...prev, [ticketId]: value }));
  };

  const handleInternalChange = (ticketId, value) => {
    setInternalFlags((prev) => ({ ...prev, [ticketId]: value }));
  };

  const submitComment = async (ticketId) => {
    const message = commentDrafts[ticketId]?.trim();
    if (!message) return;
    try {
      await api.post(`/tickets/${ticketId}/comments`, {
        message,
        internalNote: internalFlags[ticketId] || false
      });
      setCommentDrafts((prev) => ({ ...prev, [ticketId]: "" }));
      setInternalFlags((prev) => ({ ...prev, [ticketId]: false }));
      await loadComments(ticketId);
    } catch (err) {
      setError(err?.response?.data?.message || "Unable to add comment.");
    }
  };

  const updateRatingDraft = (ticketId, field, value) => {
    setRatingDrafts((prev) => ({
      ...prev,
      [ticketId]: { ...(prev[ticketId] || { score: 5, feedback: "" }), [field]: value }
    }));
  };

  const submitRating = async (ticketId) => {
    const draft = ratingDrafts[ticketId] || { score: 5, feedback: "" };
    try {
      await api.post(`/tickets/${ticketId}/rating`, {
        score: Number(draft.score || 5),
        feedback: draft.feedback || ""
      });
      await loadRating(ticketId);
    } catch (err) {
      setError(err?.response?.data?.message || "Unable to submit rating.");
    }
  };

  const handleFilterChange = (field, value) => {
    const next = { ...filters, [field]: value };
    setFilters(next);
    loadTickets(0, next);
  };

  const handleAttachmentUpload = async (ticketId, file) => {
    if (!file) return;
    const formData = new FormData();
    formData.append("file", file);
    try {
      await api.post(`/tickets/${ticketId}/attachments`, formData, {
        headers: { "Content-Type": "multipart/form-data" }
      });
      await loadAttachments(ticketId);
    } catch (err) {
      setError(err?.response?.data?.message || "Unable to upload attachment.");
    }
  };

  const downloadAttachment = async (attachment) => {
    try {
      const response = await api.get(`/attachments/${attachment.id}/download`, { responseType: "blob" });
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement("a");
      link.href = url;
      link.setAttribute("download", attachment.filename);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      setError(err?.response?.data?.message || "Unable to download attachment.");
    }
  };

  const updateTags = async (ticketId) => {
    const raw = tagDrafts[ticketId] || "";
    const values = raw
      .split(",")
      .map((value) => value.trim())
      .filter(Boolean);
    try {
      const response = await api.put(`/tickets/${ticketId}/tags`, { tags: values });
      setTickets((prev) => prev.map((ticket) => (ticket.id === ticketId ? response.data : ticket)));
    } catch (err) {
      setError(err?.response?.data?.message || "Unable to update tags.");
    }
  };

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <h1>
            {user?.role === "ROLE_ADMIN"
              ? "All Tickets"
              : user?.role === "ROLE_AGENT"
              ? "Assigned Tickets"
              : "My Tickets"}
          </h1>
          <p className="muted">Review and track support requests.</p>
        </div>
        {user?.role === "ROLE_ADMIN" ? (
          <button
            className="secondary-button"
            type="button"
            onClick={async () => {
              try {
                const response = await api.get("/tickets/export", { responseType: "blob" });
                const url = window.URL.createObjectURL(new Blob([response.data]));
                const link = document.createElement("a");
                link.href = url;
                link.setAttribute("download", "tickets.csv");
                document.body.appendChild(link);
                link.click();
                link.remove();
                window.URL.revokeObjectURL(url);
              } catch (err) {
                setError(err?.response?.data?.message || "Unable to export tickets.");
              }
            }}
          >
            Export CSV
          </button>
        ) : null}
      </header>
      <div className="filter-bar">
        <input
          placeholder="Search tickets..."
          value={filters.query}
          onChange={(event) => handleFilterChange("query", event.target.value)}
        />
        <select
          value={filters.status}
          onChange={(event) => handleFilterChange("status", event.target.value)}
        >
          <option value="">All Status</option>
          {statusOptions.map((status) => (
            <option key={status} value={status}>
              {status.replace("_", " ")}
            </option>
          ))}
        </select>
        <select
          value={filters.priority}
          onChange={(event) => handleFilterChange("priority", event.target.value)}
        >
          <option value="">All Priority</option>
          <option value="LOW">LOW</option>
          <option value="MEDIUM">MEDIUM</option>
          <option value="HIGH">HIGH</option>
        </select>
        <select value={filters.tag} onChange={(event) => handleFilterChange("tag", event.target.value)}>
          <option value="">All Tags</option>
          {tags.map((tag) => (
            <option key={tag} value={tag}>
              {tag}
            </option>
          ))}
        </select>
        <label className="checkbox-row">
          <input
            type="checkbox"
            checked={filters.includeArchived}
            onChange={(event) => handleFilterChange("includeArchived", event.target.checked)}
          />
          Show Archived
        </label>
      </div>
      {error ? <div className="error-banner">{error}</div> : null}
      {loading ? (
        <div className="centered-card">Loading tickets...</div>
      ) : (
        <div className="ticket-grid">
          {tickets.map((ticket) => (
            <article key={ticket.id} className="ticket-card">
              <div className="ticket-header">
                <h3>{ticket.title}</h3>
                <span className={`pill pill-${ticket.priority.toLowerCase()}`}>
                  {ticket.priority}
                </span>
              </div>
              <p className="ticket-description">{ticket.description}</p>
              <div className="ticket-meta">
                <div>
                  <p className="muted">Created By</p>
                  <p>{ticket.userName}</p>
                </div>
                <div>
                  <p className="muted">SLA Due</p>
                  <p className={ticket.slaBreached ? "sla-breached" : ""}>
                    {ticket.dueAt ? new Date(ticket.dueAt).toLocaleString() : "N/A"}
                  </p>
                </div>
                {user?.role === "ROLE_ADMIN" ? (
                  <div>
                    <p className="muted">Assigned To</p>
                    <select
                      value={ticket.assignedToId ?? ""}
                      onChange={(event) => {
                        const value = event.target.value;
                        handleAssign(ticket.id, value ? Number(value) : null);
                      }}
                    >
                      <option value="">Unassigned</option>
                      {agents.map((agent) => (
                        <option key={agent.id} value={agent.id}>
                          {agent.name}
                        </option>
                      ))}
                    </select>
                    <button
                      className="link-button"
                      type="button"
                      onClick={() => handleAssign(ticket.id, null)}
                    >
                      Clear Primary
                    </button>
                  </div>
                ) : null}
                {user?.role === "ROLE_ADMIN" ? (
                  <div>
                    <p className="muted">Queue</p>
                    <select
                      value={ticket.queueId ?? ""}
                      onChange={(event) => {
                        const value = event.target.value;
                        handleQueueUpdate(ticket.id, value ? Number(value) : null);
                      }}
                    >
                      <option value="">Unassigned</option>
                      {queues.map((queue) => (
                        <option key={queue.id} value={queue.id}>
                          {queue.name}
                        </option>
                      ))}
                    </select>
                  </div>
                ) : (
                  <div>
                    <p className="muted">Queue</p>
                    <p>{ticket.queueName || "Unassigned"}</p>
                  </div>
                )}
                <div>
                  <p className="muted">Status</p>
                  {user?.role === "ROLE_ADMIN" || user?.role === "ROLE_AGENT" ? (
                    <select
                      value={ticket.status}
                      onChange={(event) => handleStatusChange(ticket.id, event.target.value)}
                    >
                      {statusOptions.map((status) => (
                        <option key={status} value={status}>
                          {status.replace("_", " ")}
                        </option>
                      ))}
                    </select>
                  ) : (
                    <span className={`status status-${ticket.status.toLowerCase()}`}>
                      {ticket.status.replace("_", " ")}
                    </span>
                  )}
                </div>
              </div>
              {user?.role === "ROLE_ADMIN" ? (
                <div className="team-section">
                  <p className="muted">Assignees</p>
                  <select
                    multiple
                    value={(ticket.assignees || []).map((member) => String(member.id))}
                    onChange={(event) => {
                      const values = Array.from(event.target.selectedOptions).map((opt) =>
                        Number(opt.value)
                      );
                      handleTeamAssign(ticket.id, values);
                    }}
                  >
                    {agents.map((agent) => (
                      <option key={agent.id} value={agent.id}>
                        {agent.name}
                      </option>
                    ))}
                  </select>
                  <button
                    className="link-button"
                    type="button"
                    onClick={() => handleTeamAssign(ticket.id, [])}
                  >
                    Clear Team
                  </button>
                </div>
              ) : (
                <div className="team-section">
                  <p className="muted">Assignees</p>
                  <p>
                    {(ticket.assignees || []).length
                      ? ticket.assignees
                          .map((member) => member.name + (member.active === false ? " (Former)" : ""))
                          .join(", ")
                      : "Unassigned"}
                  </p>
                </div>
              )}
              {user?.role === "ROLE_ADMIN" ? (
                <div className="team-section">
                  <p className="muted">Tags (comma separated)</p>
                  <input
                    value={tagDrafts[ticket.id] ?? (ticket.tags?.join(", ") || "")}
                    onChange={(event) =>
                      setTagDrafts((prev) => ({ ...prev, [ticket.id]: event.target.value }))
                    }
                    onBlur={() => updateTags(ticket.id)}
                  />
                </div>
              ) : (
                <div className="team-section">
                  <p className="muted">Tags</p>
                  <p>{ticket.tags?.length ? ticket.tags.join(", ") : "No tags"}</p>
                </div>
              )}
              <div className="comment-toggle">
                <button
                  className="secondary-button"
                  type="button"
                  onClick={() => toggleComments(ticket.id)}
                >
                  {expandedTicket === ticket.id ? "Hide Timeline" : "View Timeline"}
                </button>
                <button
                  className="secondary-button"
                  type="button"
                  onClick={async () => {
                    try {
                      if (ticket.archived) {
                        const response = await api.put(`/tickets/${ticket.id}/restore`);
                        setTickets((prev) =>
                          prev.map((item) => (item.id === ticket.id ? response.data : item))
                        );
                      } else {
                        const response = await api.put(`/tickets/${ticket.id}/archive`);
                        setTickets((prev) =>
                          prev.map((item) => (item.id === ticket.id ? response.data : item))
                        );
                      }
                    } catch (err) {
                      setError(err?.response?.data?.message || "Unable to update ticket.");
                    }
                  }}
                >
                  {ticket.archived ? "Restore" : "Archive"}
                </button>
              </div>
              {expandedTicket === ticket.id ? (
                <div className="comment-section">
                  <div className="attachment-section">
                    <h4>Attachments</h4>
                    <ul>
                      {(attachmentsMap[ticket.id] || []).map((file) => (
                        <li key={file.id}>
                          <button
                            className="link-button"
                            type="button"
                            onClick={() => downloadAttachment(file)}
                          >
                            {file.filename}
                          </button>
                        </li>
                      ))}
                    </ul>
                    <input
                      type="file"
                      onChange={(event) => handleAttachmentUpload(ticket.id, event.target.files[0])}
                    />
                  </div>
                  <div className="comment-list">
                    {(commentsMap[ticket.id] || []).map((comment) => (
                      <div key={comment.id} className="comment-item">
                        <div>
                          <strong>{comment.authorName}</strong>{" "}
                          <span className="muted">{comment.type}</span>
                        </div>
                        <p>{comment.message}</p>
                      </div>
                    ))}
                  </div>
                  <div className="comment-form">
                    <textarea
                      rows="3"
                      placeholder="Add a note..."
                      value={commentDrafts[ticket.id] || ""}
                      onChange={(event) => handleCommentChange(ticket.id, event.target.value)}
                    />
                    {user?.role === "ROLE_ADMIN" || user?.role === "ROLE_AGENT" ? (
                      <label className="checkbox-row">
                        <input
                          type="checkbox"
                          checked={internalFlags[ticket.id] || false}
                          onChange={(event) => handleInternalChange(ticket.id, event.target.checked)}
                        />
                        Internal note (staff only)
                      </label>
                    ) : null}
                    <button
                      className="primary-button"
                      type="button"
                      onClick={() => submitComment(ticket.id)}
                    >
                      Add Comment
                    </button>
                  </div>
                  <div className="rating-section">
                    <h4>Customer Rating</h4>
                    {ratingMap[ticket.id] ? (
                      <div className="rating-display">
                        <p>
                          Rating: <strong>{ratingMap[ticket.id].score}/5</strong>
                        </p>
                        <p className="muted">{ratingMap[ticket.id].feedback || "No feedback"}</p>
                      </div>
                    ) : ticket.status === "RESOLVED" && user?.role === "ROLE_USER" ? (
                      <div className="rating-form">
                        <label>
                          Score
                          <select
                            value={(ratingDrafts[ticket.id]?.score ?? 5)}
                            onChange={(event) =>
                              updateRatingDraft(ticket.id, "score", event.target.value)
                            }
                          >
                            {[5, 4, 3, 2, 1].map((value) => (
                              <option key={value} value={value}>
                                {value}
                              </option>
                            ))}
                          </select>
                        </label>
                        <label>
                          Feedback
                          <textarea
                            rows="3"
                            value={ratingDrafts[ticket.id]?.feedback || ""}
                            onChange={(event) =>
                              updateRatingDraft(ticket.id, "feedback", event.target.value)
                            }
                          />
                        </label>
                        <button
                          className="primary-button"
                          type="button"
                          onClick={() => submitRating(ticket.id)}
                        >
                          Submit Rating
                        </button>
                      </div>
                    ) : (
                      <p className="muted">Ratings available after resolution.</p>
                    )}
                  </div>
                </div>
              ) : null}
            </article>
          ))}
        </div>
      )}
      <div className="pagination">
        <button
          className="secondary-button"
          type="button"
          disabled={pageData.page === 0}
          onClick={() => loadTickets(pageData.page - 1)}
        >
          Previous
        </button>
        <span>
          Page {pageData.page + 1} of {Math.max(pageData.totalPages, 1)}
        </span>
        <button
          className="secondary-button"
          type="button"
          disabled={pageData.page + 1 >= pageData.totalPages}
          onClick={() => loadTickets(pageData.page + 1)}
        >
          Next
        </button>
      </div>
    </section>
  );
}
