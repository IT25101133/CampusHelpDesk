document.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll("[data-auto-dismiss]").forEach((el) => {
        setTimeout(() => el.remove(), 4200);
    });
    if (document.getElementById("login-screen")) {
        initApp();
    }
});

const DEMO_ACCOUNTS = {
    STUDENT: { email: "student@sliit.lk", password: "password" },
    LECTURER: { email: "lecturer@sliit.lk", password: "password" },
    STAFF: { email: "staff@sliit.lk", password: "password" },
    ADMIN: { email: "admin@sliit.lk", password: "password" },
    DEPT_HEAD: { email: "head@sliit.lk", password: "password" }
};

const SESSION_KEY = "chd.session";

let currentUser = null;
let tickets = [];
let notifications = [];
let kbArticles = [];
let reports = null;

function initApp() {
    document.querySelectorAll(".role-btn").forEach((btn) => {
        btn.addEventListener("click", () => loginAs(btn.dataset.role));
    });
    document.getElementById("logout-btn").addEventListener("click", logout);
    window.addEventListener("hashchange", route);
    restoreSession();
}

async function restoreSession() {
    const saved = localStorage.getItem(SESSION_KEY);
    if (!saved) {
        showLogin();
        return;
    }
    currentUser = JSON.parse(saved);
    try {
        const me = await api("/api/auth/me");
        currentUser.fullName = me.fullName || currentUser.fullName;
        currentUser.role = me.role;
        currentUser.email = me.email;
        if (me.token) {
            currentUser.token = me.token;
        }
        localStorage.setItem(SESSION_KEY, JSON.stringify(currentUser));
        showApp();
        route();
    } catch (err) {
        logout();
    }
}

async function loginAs(role) {
    const account = DEMO_ACCOUNTS[role];
    const errorEl = document.getElementById("login-error");
    errorEl.hidden = true;
    try {
        const data = await api("/api/auth/login", {
            method: "POST",
            auth: false,
            body: account
        });
        currentUser = {
            token: data.token,
            email: data.email,
            role: data.role,
            fullName: data.fullName || data.email
        };
        localStorage.setItem(SESSION_KEY, JSON.stringify(currentUser));
        location.hash = "#/dashboard";
        showApp();
        route();
    } catch (err) {
        errorEl.textContent = err.message;
        errorEl.hidden = false;
    }
}

function logout() {
    currentUser = null;
    tickets = [];
    notifications = [];
    kbArticles = [];
    reports = null;
    localStorage.removeItem(SESSION_KEY);
    location.hash = "";
    showLogin();
}

function showLogin() {
    document.getElementById("login-screen").hidden = false;
    document.getElementById("app-shell").hidden = true;
}

function showApp() {
    document.getElementById("login-screen").hidden = true;
    document.getElementById("app-shell").hidden = false;
    document.getElementById("sidebar-name").textContent = currentUser.fullName;
    document.getElementById("sidebar-role").textContent = label(currentUser.role);
    renderNav();
}

function renderNav() {
    const canReport = currentUser.role === "ADMIN" || currentUser.role === "DEPT_HEAD";
    const items = [
        ["#/dashboard", "Dashboard"],
        ["#/tickets", "Tickets"],
        ["#/tickets/new", "New ticket"],
        ["#/notifications", "Notifications"],
        ["#/kb", "Knowledge base"]
    ];
    if (canReport) {
        items.push(["#/reports", "Reports"]);
    }
    const nav = document.getElementById("sidebar-nav");
    const active = location.hash || "#/dashboard";
    nav.innerHTML = items.map(([href, labelText]) => {
        const isActive = active === href || (href === "#/tickets" && active.startsWith("#/tickets/") && active !== "#/tickets/new");
        return `<a class="nav-link${isActive ? " active" : ""}" href="${href}">${labelText}</a>`;
    }).join("");
}

async function route() {
    if (!currentUser) {
        showLogin();
        return;
    }
    showApp();
    const hash = location.hash || "#/dashboard";
    const viewport = document.getElementById("viewport");
    try {
        if (hash === "#/dashboard") {
            setTitle("Dashboard");
            await Promise.all([loadTickets(), loadNotifications()]);
            viewport.innerHTML = renderDashboard();
        } else if (hash === "#/tickets") {
            setTitle("Tickets");
            await loadTickets();
            viewport.innerHTML = renderTickets();
            bindTicketFilters();
        } else if (hash === "#/tickets/new") {
            setTitle("New ticket");
            viewport.innerHTML = await renderNewTicket();
            bindNewTicketForm();
        } else if (hash.startsWith("#/tickets/")) {
            const id = hash.split("/")[2];
            setTitle("Ticket");
            viewport.innerHTML = await renderTicketDetail(id);
            bindTicketDetail(id);
        } else if (hash === "#/notifications") {
            setTitle("Notifications");
            await loadNotifications();
            viewport.innerHTML = renderNotifications();
            bindNotifications();
        } else if (hash === "#/kb") {
            setTitle("Knowledge base");
            await loadKb();
            viewport.innerHTML = renderKb();
            bindKbSearch();
        } else if (hash.startsWith("#/kb/")) {
            const id = hash.split("/")[2];
            setTitle("Article");
            viewport.innerHTML = await renderKbArticle(id);
        } else if (hash === "#/reports") {
            setTitle("Reports");
            await loadReports();
            viewport.innerHTML = renderReports();
            bindReports();
        } else {
            location.hash = "#/dashboard";
        }
    } catch (err) {
        viewport.innerHTML = `<div class="card empty">${escapeHtml(err.message)}</div>`;
        toast(err.message, true);
    }
}

function setTitle(title) {
    document.getElementById("page-title").textContent = title;
}

async function loadTickets(filters = {}) {
    const params = new URLSearchParams();
    if (filters.status) {
        params.set("status", filters.status);
    }
    if (filters.priority) {
        params.set("priority", filters.priority);
    }
    if (filters.createdBy) {
        params.set("createdBy", filters.createdBy);
    }
    const query = params.toString();
    tickets = await api("/api/tickets" + (query ? "?" + query : ""));
}

async function loadNotifications() {
    notifications = await api("/api/notifications");
    const unread = notifications.filter((n) => !n.read).length;
    const pill = document.getElementById("unread-pill");
    pill.hidden = unread === 0;
    pill.textContent = unread + " new";
}

async function loadKb(filters = {}) {
    const params = new URLSearchParams();
    if (filters.search) {
        params.set("search", filters.search);
    }
    if (filters.category) {
        params.set("category", filters.category);
    }
    const query = params.toString();
    kbArticles = await api("/api/kb/articles" + (query ? "?" + query : ""));
}

async function loadReports() {
    const [summary, byCategory, staffPerformance, resolutionTimeAvg, overdue] = await Promise.all([
        api("/api/reports/summary"),
        api("/api/reports/by-category"),
        api("/api/reports/staff-performance"),
        api("/api/reports/resolution-time-avg"),
        api("/api/reports/overdue")
    ]);
    let auditLogs = [];
    if (currentUser.role === "ADMIN") {
        auditLogs = await api("/api/reports/audit-logs");
    }
    reports = { summary, byCategory, staffPerformance, resolutionTimeAvg, overdue, auditLogs };
}

function renderDashboard() {
    const open = tickets.filter((t) => t.status === "OPEN").length;
    const progress = tickets.filter((t) => t.status === "IN_PROGRESS").length;
    const resolved = tickets.filter((t) => t.status === "RESOLVED" || t.status === "CLOSED").length;
    const unread = notifications.filter((n) => !n.read).length;
    const recent = tickets.slice(0, 6);
    return `
        <div class="stats-grid">
            ${statCard("Tickets", tickets.length)}
            ${statCard("Open", open)}
            ${statCard("In progress", progress)}
            ${statCard("Resolved", resolved)}
            ${statCard("Unread", unread)}
        </div>
        <div class="card">
            <div class="toolbar"><h3>Recent tickets</h3><a href="#/tickets">View all</a></div>
            ${ticketTable(recent)}
        </div>`;
}

function statCard(labelText, value) {
    return `<div class="stat"><span>${labelText}</span><strong>${value}</strong></div>`;
}

function renderTickets() {
    const createdByFilter = currentUser.role === "STUDENT" || currentUser.role === "LECTURER" ? "" : `
        <label>Created by
            <input name="createdBy" type="number" min="1" placeholder="User ID"/>
        </label>`;
    return `<div class="card">
        <div class="toolbar">
            <p class="muted">Filter tickets from <code>/api/tickets</code></p>
            <a class="btn btn-primary" href="#/tickets/new">New ticket</a>
        </div>
        <form id="ticket-filters" class="toolbar">
            <label>Status
                <select name="status">
                    <option value="">Any</option>
                    <option value="OPEN">OPEN</option>
                    <option value="IN_PROGRESS">IN PROGRESS</option>
                    <option value="RESOLVED">RESOLVED</option>
                    <option value="CLOSED">CLOSED</option>
                </select>
            </label>
            <label>Priority
                <select name="priority">
                    <option value="">Any</option>
                    <option value="LOW">LOW</option>
                    <option value="MEDIUM">MEDIUM</option>
                    <option value="HIGH">HIGH</option>
                    <option value="CRITICAL">CRITICAL</option>
                </select>
            </label>
            ${createdByFilter}
            <button class="btn" type="submit">Apply</button>
        </form>
        ${ticketTable(tickets)}
    </div>`;
}

function ticketTable(rows) {
    if (!rows.length) {
        return `<div class="empty">No tickets yet.</div>`;
    }
    return `<div class="table-wrap"><table>
        <thead><tr><th>ID</th><th>Title</th><th>Category</th><th>Submitter</th><th>Priority</th><th>Status</th><th>Assignee</th></tr></thead>
        <tbody>
            ${rows.map((t) => `<tr>
                <td><a href="#/tickets/${t.id}">${escapeHtml(t.ticketNumber)}</a></td>
                <td>${escapeHtml(t.title)}</td>
                <td>${escapeHtml(t.category || "—")}</td>
                <td>${escapeHtml(t.submitterRole === "LECTURER" ? "Lecturer" : t.submitterRole === "STUDENT" ? "Student" : (t.submitterRole || "—"))}</td>
                <td>${badge(t.priority)}</td>
                <td>${badge(t.status)}</td>
                <td>${escapeHtml(t.assignee || "Unassigned")}</td>
            </tr>`).join("")}
        </tbody>
    </table></div>`;
}

async function renderNewTicket() {
    const options = await api("/api/tickets/options");
    const categories = options.categories.map((c) =>
        `<option value="${c.id}">${escapeHtml(c.name)}</option>`
    ).join("");
    const priorities = options.priorities.map((p) =>
        `<option value="${p}">${label(p)}</option>`
    ).join("");
    return `<div class="card">
        <form id="ticket-form" class="form-grid">
            <label>Title<input name="title" required maxlength="200"/></label>
            <label>Category<select name="categoryId" required>${categories}</select></label>
            <label>Priority<select name="priority">${priorities}</select></label>
            <label>Description<textarea name="description" required maxlength="4000"></textarea></label>
            <div id="kb-suggestions" class="kb-list" hidden></div>
            <div class="row-actions">
                <button class="btn btn-primary" type="submit">Submit ticket</button>
                <a class="btn btn-ghost" href="#/tickets">Cancel</a>
            </div>
        </form>
    </div>`;
}

function bindNewTicketForm() {
    const form = document.getElementById("ticket-form");
    const titleInput = form.querySelector("[name=title]");
    const descriptionInput = form.querySelector("[name=description]");
    const refreshSuggestions = debounce(loadTicketSuggestions, 250);
    titleInput.addEventListener("input", refreshSuggestions);
    descriptionInput.addEventListener("input", refreshSuggestions);
    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        const data = Object.fromEntries(new FormData(form).entries());
        data.categoryId = Number(data.categoryId);
        try {
            const created = await api("/api/tickets", { method: "POST", body: data });
            toast("Ticket " + created.ticketNumber + " submitted.");
            location.hash = "#/tickets/" + created.id;
        } catch (err) {
            toast(err.message, true);
        }
    });
}

async function loadTicketSuggestions() {
    const form = document.getElementById("ticket-form");
    const box = document.getElementById("kb-suggestions");
    if (!form || !box) {
        return;
    }
    const title = form.querySelector("[name=title]").value;
    const description = form.querySelector("[name=description]").value;
    const query = (title + " " + description).trim();
    if (query.length < 3) {
        box.hidden = true;
        box.innerHTML = "";
        return;
    }
    try {
        const suggestions = await api("/api/kb/suggest?query=" + encodeURIComponent(query));
        if (!suggestions.length) {
            box.hidden = true;
            box.innerHTML = "";
            return;
        }
        box.hidden = false;
        box.innerHTML = `<p class="muted">Suggested articles</p>` + suggestions.map((a) => `
            <a class="kb-item" href="#/kb/${a.id}">
                <strong>${escapeHtml(a.title)}</strong>
                <div class="muted">${escapeHtml(a.category || "")}</div>
            </a>`).join("");
    } catch {
        box.hidden = true;
    }
}

function debounce(fn, wait) {
    let timer;
    return (...args) => {
        clearTimeout(timer);
        timer = setTimeout(() => fn(...args), wait);
    };
}

async function renderTicketDetail(id) {
    const ticket = await api("/api/tickets/" + id);
    const comments = (ticket.comments || []).map((c) => `
        <div class="comment">
            <strong>${escapeHtml(c.author)}</strong>
            <span class="muted"> · ${formatDate(c.createdAt)}</span>
            <p>${escapeHtml(c.body)}</p>
        </div>`).join("") || `<p class="empty">No replies yet.</p>`;
    const attachmentItems = (ticket.attachments || []).map((a) => `
        <li><a href="${escapeHtml(a.filePath)}" target="_blank" rel="noopener">${escapeHtml(a.fileName)}</a>
            <span class="muted"> · ${formatDate(a.uploadedAt)}</span></li>`
    ).join("");
    const attachmentsHtml = attachmentItems
        ? `<ul>${attachmentItems}</ul>`
        : `<p class="empty">No attachments yet.</p>`;
    const statuses = ["OPEN", "RESOLVED"]
        .concat(ticket.status === "RESOLVED" ? [] : ["CLOSED"])
        .concat(ticket.assigneeId ? ["IN_PROGRESS"] : [])
        .map((s) => `<option value="${s}"${s === ticket.status ? " selected" : ""}>${label(s)}</option>`)
        .join("");
    const staff = (ticket.staffMembers || [])
        .map((s) => `<option value="${s.id}"${s.id === ticket.assigneeId ? " selected" : ""}>${escapeHtml(s.fullName)}</option>`)
        .join("");
    const editable = Boolean(ticket.canEdit);
    const options = await api("/api/tickets/options");
    const categoryOptions = (options.categories || []).map((c) =>
        `<option value="${c.id}"${c.id === ticket.categoryId ? " selected" : ""}>${escapeHtml(c.name)}</option>`
    ).join("");
    const editForm = editable ? `
        <form id="ticket-edit-form" class="stack" style="margin-top:1rem">
            <label>Title<input name="title" required maxlength="200" value="${escapeHtml(ticket.title || "")}"/></label>
            <label>Description<textarea name="description" required maxlength="4000">${escapeHtml(ticket.description)}</textarea></label>
            <label>Category<select name="categoryId">${categoryOptions}</select></label>
            <label>Priority<select name="priority">
                ${["LOW", "MEDIUM", "HIGH", "CRITICAL"].map((p) =>
                    `<option value="${p}"${p === ticket.priority ? " selected" : ""}>${label(p)}</option>`
                ).join("")}
            </select></label>
            <button class="btn" type="submit">Save changes</button>
        </form>` : "";
    const withdrawBtn = ticket.canDelete
        ? `<button class="btn" id="withdraw-btn" type="button">${ticket.assigneeId ? "Delete ticket" : "Withdraw ticket"}</button>`
        : "";
    const requestDeleteBtn = ticket.canRequestDelete
        ? `<button class="btn" id="request-delete-btn" type="button">Request deletion</button>`
        : "";
    const waitingDelete = ticket.deleteRequested && !ticket.deleteApproved && !ticket.canManage
        ? `<p class="muted">Deletion requested. Waiting for staff approval.</p>`
        : "";
    const approveDeleteBtn = ticket.canApproveDelete
        ? `<button class="btn btn-primary" id="approve-delete-btn" type="button">Approve deletion</button>`
        : "";
    const resolutionPanel = ticket.canConfirmClose
        ? `<div class="card">
            <h3>This ticket is resolved</h3>
            <p>Confirm and close it if the issue is fixed, or reopen it if it is not. If you do nothing for 5 days, it closes on its own.</p>
            <div class="row-actions">
                <button class="btn btn-primary" id="confirm-close-btn" type="button">Confirm &amp; Close</button>
                ${ticket.canReopenResolved ? `<button class="btn" id="reopen-resolved-btn" type="button">Reopen</button>` : ""}
            </div>
           </div>`
        : "";
    const staffCloseForm = ticket.canStaffClose
        ? `<form id="staff-close-form" class="stack" style="margin-top:1rem">
            <label>Reason for closing<textarea name="reason" required maxlength="500" placeholder="Spam, duplicate, or confirmed outside the portal"></textarea></label>
            <button class="btn btn-primary" type="submit">Close with note</button>
           </form>`
        : "";
    const closeBtn = !ticket.canManage && ticket.status === "OPEN" && !ticket.assigneeId
        ? `<button class="btn btn-primary" id="close-btn" type="button">Close ticket</button>`
        : "";
    const reopenBtn = ticket.canReopen
        ? `<form id="reopen-form" class="stack" style="margin-top:1rem">
            <label>Reason for reopening<textarea name="reason" required maxlength="500"></textarea></label>
            <button class="btn btn-primary" type="submit">Reopen ticket</button>
           </form>`
        : "";
    const closedNotice = ticket.closedNotice
        ? `<p class="muted">${escapeHtml(ticket.closedNotice)}</p>`
        : "";
    const manage = ticket.canManage ? `
        <div class="card">
            <h3>Manage</h3>
            <form id="status-form" class="stack">
                <label>Status<select name="status">${statuses}</select></label>
                <button class="btn btn-primary" type="submit">Update status</button>
            </form>
            <form id="assign-form" class="stack" style="margin-top:1rem">
                <label>Assignee<select name="assigneeId">${staff}</select></label>
                <button class="btn" type="submit">Reassign</button>
            </form>
            <div class="row-actions" style="margin-top:1rem">${reopenBtn}</div>
            ${staffCloseForm}
        </div>` : `<div class="card"><div class="row-actions">${closeBtn}${reopenBtn}${closedNotice}</div></div>`;
    return `
        <div class="stats-grid">
            <div class="card">
                <p class="muted">${escapeHtml(ticket.ticketNumber)}</p>
                <h3>${escapeHtml(ticket.title)}</h3>
                <p>${badge(ticket.status)} ${badge(ticket.priority)} · ${escapeHtml(ticket.category || "")}</p>
                <p>${escapeHtml(ticket.description)}</p>
                <p class="muted">Requester ${escapeHtml(ticket.requester || "—")} (${escapeHtml(ticket.submitterRole === "LECTURER" ? "Lecturer" : ticket.submitterRole === "STUDENT" ? "Student" : (ticket.submitterRole || "—"))}) · Assignee ${escapeHtml(ticket.assignee || "Unassigned")}</p>
                <p class="muted">SLA due ${ticket.slaDueAt ? formatDate(ticket.slaDueAt) : "—"}</p>
                ${editForm}
                <div class="row-actions" style="margin-top:1rem">${withdrawBtn}${requestDeleteBtn}${approveDeleteBtn}</div>
                ${waitingDelete}
                ${resolutionPanel}
                ${closedNotice}
            </div>
            ${manage}
        </div>
        <div class="card">
            <h3>Attachments</h3>
            ${attachmentsHtml}
            ${(editable || ticket.canManage) ? `<form id="attachment-form" class="stack" style="margin-top:1rem">
                <label>Screenshot or receipt<input name="file" type="file" accept="image/png,image/jpeg,image/gif,image/webp,application/pdf" required/></label>
                <button class="btn" type="submit">Upload file</button>
            </form>` : ""}
        </div>
        <div class="card">
            <h3>Conversation</h3>
            <div class="comment-list">${comments}</div>
            ${ticket.canComment ? `<form id="comment-form" class="stack" style="margin-top:1rem">
                <label>Reply<textarea name="body" required maxlength="4000"></textarea></label>
                <button class="btn btn-primary" type="submit">Post reply</button>
            </form>` : ""}
        </div>`;
}

function bindTicketFilters() {
    const form = document.getElementById("ticket-filters");
    if (!form) {
        return;
    }
    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        const data = Object.fromEntries(new FormData(form).entries());
        try {
            await loadTickets(data);
            document.getElementById("viewport").innerHTML = renderTickets();
            bindTicketFilters();
        } catch (err) {
            toast(err.message, true);
        }
    });
}

function bindTicketDetail(id) {
    const commentForm = document.getElementById("comment-form");
    commentForm?.addEventListener("submit", async (event) => {
        event.preventDefault();
        const body = new FormData(commentForm).get("body");
        try {
            await api("/api/tickets/" + id + "/comments", { method: "POST", body: { body } });
            route();
        } catch (err) {
            toast(err.message, true);
        }
    });
    const editForm = document.getElementById("ticket-edit-form");
    if (editForm) {
        editForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            const data = Object.fromEntries(new FormData(editForm).entries());
            data.categoryId = Number(data.categoryId);
            try {
                await api("/api/tickets/" + id, { method: "PUT", body: data });
                toast("Ticket updated.");
                route();
            } catch (err) {
                toast(err.message, true);
            }
        });
    }
    const withdrawBtn = document.getElementById("withdraw-btn");
    if (withdrawBtn) {
        withdrawBtn.addEventListener("click", async () => {
            try {
                await api("/api/tickets/" + id, { method: "DELETE" });
                toast("Ticket withdrawn.");
                location.hash = "#/tickets";
            } catch (err) {
                toast(err.message, true);
            }
        });
    }
    document.getElementById("request-delete-btn")?.addEventListener("click", async () => {
        try {
            await api("/api/tickets/" + id + "/delete-request", { method: "POST" });
            toast("Deletion requested.");
            route();
        } catch (err) {
            toast(err.message, true);
        }
    });
    document.getElementById("approve-delete-btn")?.addEventListener("click", async () => {
        try {
            await api("/api/tickets/" + id + "/delete-approval", { method: "POST" });
            toast("Deletion approved. The owner can now delete the ticket.");
            route();
        } catch (err) {
            toast(err.message, true);
        }
    });
    document.getElementById("confirm-close-btn")?.addEventListener("click", async () => {
        try {
            await api("/api/tickets/" + id + "/confirm-close", { method: "POST" });
            toast("Ticket closed.");
            route();
        } catch (err) {
            toast(err.message, true);
        }
    });
    document.getElementById("reopen-resolved-btn")?.addEventListener("click", async () => {
        try {
            await api("/api/tickets/" + id + "/reopen-resolved", { method: "POST" });
            toast("Ticket reopened.");
            route();
        } catch (err) {
            toast(err.message, true);
        }
    });
    const staffCloseForm = document.getElementById("staff-close-form");
    if (staffCloseForm) {
        staffCloseForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            const reason = String(new FormData(staffCloseForm).get("reason") || "").trim();
            if (!reason) {
                toast("A reason is required to close this ticket.", true);
                return;
            }
            try {
                await api("/api/tickets/" + id + "/staff-close", { method: "POST", body: { reason } });
                toast("Ticket closed.");
                route();
            } catch (err) {
                toast(err.message, true);
            }
        });
    }
    const closeBtn = document.getElementById("close-btn");
    if (closeBtn) {
        closeBtn.addEventListener("click", async () => {
            try {
                await api("/api/tickets/" + id + "/status", { method: "POST", body: { status: "CLOSED" } });
                toast("Ticket closed.");
                route();
            } catch (err) {
                toast(err.message, true);
            }
        });
    }
    const requestDeleteBtn = document.getElementById("request-delete-btn");
    if (requestDeleteBtn) {
        requestDeleteBtn.addEventListener("click", async () => {
            try {
                await api("/api/tickets/" + id + "/delete-request", { method: "POST" });
                toast("Deletion requested.");
                route();
            } catch (err) {
                toast(err.message, true);
            }
        });
    }
    const approveDeleteBtn = document.getElementById("approve-delete-btn");
    if (approveDeleteBtn) {
        approveDeleteBtn.addEventListener("click", async () => {
            try {
                await api("/api/tickets/" + id + "/delete-approval", { method: "POST" });
                toast("Deletion approved.");
                route();
            } catch (err) {
                toast(err.message, true);
            }
        });
    }
    const reopenForm = document.getElementById("reopen-form");
    if (reopenForm) {
        reopenForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            const reason = String(new FormData(reopenForm).get("reason") || "").trim();
            if (!reason) {
                toast("A reason is required to reopen this ticket.", true);
                return;
            }
            try {
                await api("/api/tickets/" + id + "/reopen", { method: "PUT", body: { reason } });
                toast("Ticket reopened.");
                route();
            } catch (err) {
                toast(err.message, true);
            }
        });
    }
    const attachmentForm = document.getElementById("attachment-form");
    if (attachmentForm) {
        attachmentForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            const file = new FormData(attachmentForm).get("file");
            if (file && file.size > 5 * 1024 * 1024) {
                toast("File exceeds the 5 MB limit.", true);
                return;
            }
            try {
                await uploadFile("/api/tickets/" + id + "/attachments", file);
                toast("Attachment uploaded.");
                route();
            } catch (err) {
                toast(err.message, true);
            }
        });
    }
    const statusForm = document.getElementById("status-form");
    if (statusForm) {
        statusForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            const status = new FormData(statusForm).get("status");
            try {
                await api("/api/tickets/" + id + "/status", { method: "POST", body: { status } });
                toast("Status updated.");
                route();
            } catch (err) {
                toast(err.message, true);
            }
        });
    }
    const assignForm = document.getElementById("assign-form");
    if (assignForm) {
        assignForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            const assigneeId = Number(new FormData(assignForm).get("assigneeId"));
            try {
                await api("/api/tickets/" + id + "/assign", { method: "POST", body: { assigneeId } });
                toast("Ticket reassigned.");
                route();
            } catch (err) {
                toast(err.message, true);
            }
        });
    }
}

function renderNotifications() {
    if (!notifications.length) {
        return `<div class="card empty">No notifications.</div>`;
    }
    return `<div class="toolbar"><button class="btn" id="read-all">Mark all read</button></div>
        <div class="note-list">${notifications.map((n) => `
            <div class="note-item${n.read ? "" : " unread"}">
                <p>${escapeHtml(n.message)}</p>
                <p class="muted">${formatDate(n.createdAt)}</p>
                <div class="row-actions">
                    ${n.ticketId ? `<a class="btn btn-small" href="#/tickets/${n.ticketId}">Open ticket</a>` : ""}
                    ${n.read ? "" : `<button class="btn btn-small" data-read="${n.id}">Mark read</button>`}
                </div>
            </div>`).join("")}
        </div>`;
}

function bindNotifications() {
    const readAll = document.getElementById("read-all");
    if (readAll) {
        readAll.addEventListener("click", async () => {
            await api("/api/notifications/read-all", { method: "POST" });
            route();
        });
    }
    document.querySelectorAll("[data-read]").forEach((btn) => {
        btn.addEventListener("click", async () => {
            await api("/api/notifications/" + btn.dataset.read + "/read", { method: "POST" });
            route();
        });
    });
}

function renderKb() {
    return `<div class="card">
        <form id="kb-search" class="toolbar">
            <input name="search" placeholder="Search articles"/>
            <input name="category" placeholder="Category"/>
            <button class="btn btn-primary" type="submit">Search</button>
        </form>
        <div class="kb-list">${kbArticles.length ? kbArticles.map((a) => `
            <a class="kb-item" href="#/kb/${a.id}">
                <strong>${escapeHtml(a.title)}</strong>
                <div class="muted">${escapeHtml(a.category || "")} · ${a.viewCount || 0} views</div>
            </a>`).join("") : `<p class="empty">No articles found.</p>`}
        </div>
    </div>`;
}

function bindKbSearch() {
    document.getElementById("kb-search").addEventListener("submit", async (event) => {
        event.preventDefault();
        const data = Object.fromEntries(new FormData(event.target).entries());
        await loadKb({ search: data.search, category: data.category });
        document.getElementById("viewport").innerHTML = renderKb();
        bindKbSearch();
        const form = document.getElementById("kb-search");
        form.search.value = data.search || "";
        form.category.value = data.category || "";
    });
}

async function renderKbArticle(id) {
    const article = await api("/api/kb/articles/" + id + "/view");
    return `<div class="card">
        <p class="muted">${escapeHtml(article.category || "")} · ${article.viewCount || 0} views</p>
        <h3>${escapeHtml(article.title)}</h3>
        <p>${escapeHtml(article.content)}</p>
        <p><a class="btn btn-primary" href="#/tickets/new">Still stuck? Open a ticket</a></p>
    </div>`;
}

function renderReports() {
    if (!reports || !reports.summary) {
        return `<div class="card empty">No report data.</div>`;
    }
    const stats = reports.summary;
    const byCategory = reports.byCategory || [];
    const staff = reports.staffPerformance || [];
    const avg = reports.resolutionTimeAvg || {};
    const overdue = reports.overdue || [];
    const logs = reports.auditLogs || [];
    const isAdmin = currentUser.role === "ADMIN";
    return `
        <div class="toolbar">
            <button class="btn btn-primary" data-export="pdf" type="button">Export PDF</button>
            <button class="btn btn-ghost" data-export="excel" type="button">Export Excel</button>
        </div>
        <div class="stats-grid">
            ${statCard("Total", stats.totalTickets)}
            ${statCard("Open", stats.openTickets)}
            ${statCard("Pending", stats.pendingTickets)}
            ${statCard("Resolved", stats.resolvedTickets)}
        </div>
        <div class="card" style="margin-top:1rem;">
            <h3>Average resolution time</h3>
            <p>${avg.resolvedCount ? `${avg.averageHours} hours across ${avg.resolvedCount} resolved tickets.` : "No resolved tickets yet."}</p>
        </div>
        <div class="card" style="margin-top:1rem;">
            <h3>Tickets by category</h3>
            ${byCategory.length ? `<div class="table-wrap"><table>
                <thead><tr><th>Category</th><th>Tickets</th></tr></thead>
                <tbody>${byCategory.map((row) => `<tr>
                    <td>${escapeHtml(row.category)}</td>
                    <td>${row.ticketCount}</td>
                </tr>`).join("")}</tbody>
            </table></div>` : `<p class="empty">No category data.</p>`}
        </div>
        <div class="card" style="margin-top:1rem;">
            <h3>Staff performance</h3>
            ${staff.length ? `<div class="table-wrap"><table>
                <thead><tr><th>Staff</th><th>Tickets resolved</th></tr></thead>
                <tbody>${staff.map((row) => `<tr>
                    <td>${escapeHtml(row.staffName)}</td>
                    <td>${row.ticketsResolved}</td>
                </tr>`).join("")}</tbody>
            </table></div>` : `<p class="empty">No resolved tickets assigned to staff.</p>`}
        </div>
        <div class="card" style="margin-top:1rem;">
            <h3>Overdue tickets</h3>
            ${overdue.length ? `<div class="table-wrap"><table>
                <thead><tr><th>Ticket</th><th>Category</th><th>SLA</th><th>Hours overdue</th></tr></thead>
                <tbody>${overdue.map((row) => `<tr>
                    <td>${escapeHtml(row.ticketNumber || "")} ${escapeHtml(row.title || "")}</td>
                    <td>${escapeHtml(row.category || "—")}</td>
                    <td>${row.slaHours}h</td>
                    <td>${row.hoursOverdue}</td>
                </tr>`).join("")}</tbody>
            </table></div>` : `<p class="empty">No overdue tickets.</p>`}
        </div>
        ${isAdmin ? `<div class="card" style="margin-top:1rem;">
            <h3>Audit log</h3>
            ${logs.length ? `<div class="table-wrap"><table>
                <thead><tr><th>When</th><th>Actor</th><th>Action</th></tr></thead>
                <tbody>${logs.map((l) => `<tr>
                    <td>${formatDate(l.createdAt)}</td>
                    <td>${escapeHtml(l.actor || "—")}</td>
                    <td>${escapeHtml(l.action)}</td>
                </tr>`).join("")}</tbody>
            </table></div>` : `<p class="empty">No audit events.</p>`}
        </div>` : ""}`;
}

function bindReports() {
    document.querySelectorAll("[data-export]").forEach((btn) => {
        btn.addEventListener("click", async () => {
            try {
                await downloadReport(btn.dataset.export);
                toast("Report downloaded");
            } catch (err) {
                toast(err.message, true);
            }
        });
    });
}

async function downloadReport(format) {
    const headers = {};
    if (currentUser && currentUser.token) {
        headers.Authorization = "Bearer " + currentUser.token;
    }
    const response = await fetch("/api/reports/export?format=" + encodeURIComponent(format), { headers });
    if (!response.ok) {
        const text = await response.text();
        let message = "Export failed";
        try {
            const data = text ? JSON.parse(text) : null;
            if (data && data.error) {
                message = data.error;
            }
        } catch (_) {
            /* keep default */
        }
        if (response.status === 401) {
            logout();
        }
        throw new Error(message);
    }
    const blob = await response.blob();
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    const header = response.headers.get("content-disposition") || "";
    const quoted = /filename="([^"]+)"/i.exec(header);
    link.download = quoted ? quoted[1] : (format === "pdf" ? "report-summary.pdf" : "ticket-report.xlsx");
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(url);
}

async function uploadFile(path, file) {
    const headers = {};
    if (currentUser && currentUser.token) {
        headers.Authorization = "Bearer " + currentUser.token;
    }
    const body = new FormData();
    body.append("file", file);
    const response = await fetch(path, { method: "POST", headers, body });
    const text = await response.text();
    const data = text ? JSON.parse(text) : null;
    if (!response.ok) {
        if (response.status === 401) {
            logout();
        }
        throw new Error((data && data.error) || "Request failed");
    }
    return data;
}

async function api(path, options = {}) {
    const headers = { "Content-Type": "application/json" };
    if (options.auth !== false && currentUser && currentUser.token) {
        headers.Authorization = "Bearer " + currentUser.token;
    }
    const response = await fetch(path, {
        method: options.method || "GET",
        headers,
        body: options.body ? JSON.stringify(options.body) : undefined
    });
    const text = await response.text();
    const data = text ? JSON.parse(text) : null;
    if (!response.ok) {
        if (response.status === 401 && options.auth !== false) {
            logout();
        }
        throw new Error((data && data.error) || "Request failed");
    }
    return data;
}

function badge(value) {
    const text = label(value || "");
    return `<span class="badge ${value || ""}">${escapeHtml(text)}</span>`;
}

function label(value) {
    return String(value || "").replaceAll("_", " ");
}

function formatDate(value) {
    if (!value) {
        return "";
    }
    return String(value).replace("T", " ").slice(0, 16);
}

function escapeHtml(value) {
    return String(value == null ? "" : value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;");
}

function toast(message, isError) {
    const el = document.getElementById("toast");
    el.textContent = message;
    el.classList.toggle("error", Boolean(isError));
    el.hidden = false;
    setTimeout(() => { el.hidden = true; }, 3200);
}
