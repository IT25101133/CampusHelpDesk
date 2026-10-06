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

async function loadTickets() {
    tickets = await api("/api/tickets");
}

async function loadNotifications() {
    notifications = await api("/api/notifications");
    const unread = notifications.filter((n) => !n.read).length;
    const pill = document.getElementById("unread-pill");
    pill.hidden = unread === 0;
    pill.textContent = unread + " new";
}

async function loadKb(query) {
    const path = query ? "/api/kb?q=" + encodeURIComponent(query) : "/api/kb";
    kbArticles = await api(path);
}

async function loadReports() {
    reports = await api("/api/reports");
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
    return `<div class="card">
        <div class="toolbar">
            <p class="muted">Loaded from <code>/api/tickets</code></p>
            <a class="btn btn-primary" href="#/tickets/new">New ticket</a>
        </div>
        ${ticketTable(tickets)}
    </div>`;
}

function ticketTable(rows) {
    if (!rows.length) {
        return `<div class="empty">No tickets yet.</div>`;
    }
    return `<div class="table-wrap"><table>
        <thead><tr><th>ID</th><th>Title</th><th>Category</th><th>Priority</th><th>Status</th><th>Assignee</th></tr></thead>
        <tbody>
            ${rows.map((t) => `<tr>
                <td><a href="#/tickets/${t.id}">${escapeHtml(t.ticketNumber)}</a></td>
                <td>${escapeHtml(t.title)}</td>
                <td>${escapeHtml(t.category || "—")}</td>
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
            <div class="row-actions">
                <button class="btn btn-primary" type="submit">Submit ticket</button>
                <a class="btn btn-ghost" href="#/tickets">Cancel</a>
            </div>
        </form>
    </div>`;
}

function bindNewTicketForm() {
    const form = document.getElementById("ticket-form");
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

async function renderTicketDetail(id) {
    const ticket = await api("/api/tickets/" + id);
    const comments = (ticket.comments || []).map((c) => `
        <div class="comment">
            <strong>${escapeHtml(c.author)}</strong>
            <span class="muted"> · ${formatDate(c.createdAt)}</span>
            <p>${escapeHtml(c.body)}</p>
        </div>`).join("") || `<p class="empty">No replies yet.</p>`;
    const statuses = ["OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED"]
        .map((s) => `<option value="${s}"${s === ticket.status ? " selected" : ""}>${label(s)}</option>`)
        .join("");
    const staff = (ticket.staffMembers || [])
        .map((s) => `<option value="${s.id}"${s.id === ticket.assigneeId ? " selected" : ""}>${escapeHtml(s.fullName)}</option>`)
        .join("");
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
        </div>` : "";
    return `
        <div class="stats-grid">
            <div class="card">
                <p class="muted">${escapeHtml(ticket.ticketNumber)}</p>
                <h3>${escapeHtml(ticket.title)}</h3>
                <p>${badge(ticket.status)} ${badge(ticket.priority)} · ${escapeHtml(ticket.category || "")}</p>
                <p>${escapeHtml(ticket.description)}</p>
                <p class="muted">Requester ${escapeHtml(ticket.requester || "—")} · Assignee ${escapeHtml(ticket.assignee || "Unassigned")}</p>
            </div>
            ${manage}
        </div>
        <div class="card">
            <h3>Conversation</h3>
            <div class="comment-list">${comments}</div>
            <form id="comment-form" class="stack" style="margin-top:1rem">
                <label>Reply<textarea name="body" required></textarea></label>
                <button class="btn btn-primary" type="submit">Post reply</button>
            </form>
        </div>`;
}

function bindTicketDetail(id) {
    const commentForm = document.getElementById("comment-form");
    commentForm.addEventListener("submit", async (event) => {
        event.preventDefault();
        const body = new FormData(commentForm).get("body");
        try {
            await api("/api/tickets/" + id + "/comments", { method: "POST", body: { body } });
            route();
        } catch (err) {
            toast(err.message, true);
        }
    });
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
            <input name="q" placeholder="Search articles"/>
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
        const q = new FormData(event.target).get("q");
        await loadKb(q);
        document.getElementById("viewport").innerHTML = renderKb();
        bindKbSearch();
    });
}

async function renderKbArticle(id) {
    const article = await api("/api/kb/" + id);
    return `<div class="card">
        <p class="muted">${escapeHtml(article.category || "")}</p>
        <h3>${escapeHtml(article.title)}</h3>
        <p>${escapeHtml(article.content)}</p>
        <p><a class="btn btn-primary" href="#/tickets/new">Still stuck? Open a ticket</a></p>
    </div>`;
}

function renderReports() {
    if (!reports || !reports.stats) {
        return `<div class="card empty">No report data.</div>`;
    }
    const stats = reports.stats;
    const logs = reports.auditLogs || [];
    return `
        <div class="stats-grid">
            ${statCard("Total", stats.totalTickets)}
            ${statCard("Open", stats.openTickets)}
            ${statCard("In progress", stats.inProgressTickets)}
            ${statCard("Resolved", stats.resolvedTickets)}
        </div>
        <div class="card">
            <h3>Audit log</h3>
            ${logs.length ? `<div class="table-wrap"><table>
                <thead><tr><th>When</th><th>Actor</th><th>Action</th></tr></thead>
                <tbody>${logs.map((l) => `<tr>
                    <td>${formatDate(l.createdAt)}</td>
                    <td>${escapeHtml(l.actor || "—")}</td>
                    <td>${escapeHtml(l.action)}</td>
                </tr>`).join("")}</tbody>
            </table></div>` : `<p class="empty">No audit events.</p>`}
        </div>`;
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
