document.addEventListener("DOMContentLoaded", async () => {
    initChrome();
    const page = document.body.dataset.page;
    if (page === "login") {
        initLogin();
        return;
    }
    if (page === "register") {
        initRegister();
        return;
    }
    if (page === "reset") {
        initReset();
        return;
    }
    const session = ProApi.requireAuth();
    if (!session) {
        return;
    }
    await applyUserChrome(session);
    if (page === "dashboard") {
        initDashboard();
    } else if (page === "tickets") {
        initTickets();
    } else if (page === "submit") {
        initSubmit();
    } else if (page === "knowledge") {
        initKnowledge();
    } else if (page === "accounts") {
        initAccounts();
    } else if (page === "profile") {
        initProfile();
    } else if (page === "inbox") {
        initInbox();
    } else if (page === "insights") {
        initInsights();
    } else if (page === "departments") {
        initDepartments();
    }
});

function initChrome() {
    const sidebar = document.getElementById("sidebar");
    const overlay = document.getElementById("sidebar-overlay");
    const menuToggle = document.getElementById("menu-toggle");

    function closeSidebar() {
        sidebar?.classList.remove("open");
        overlay?.classList.remove("show");
    }

    menuToggle?.addEventListener("click", () => {
        sidebar?.classList.toggle("open");
        overlay?.classList.toggle("show");
    });
    overlay?.addEventListener("click", closeSidebar);
    document.addEventListener("keydown", (event) => {
        if (event.key === "Escape") {
            closeSidebar();
        }
    });

    document.querySelectorAll("[data-sign-out]").forEach((btn) => {
        btn.addEventListener("click", () => ProApi.logout());
    });
}

async function applyUserChrome(session) {
    if (!session.userId) {
        try {
            const profile = await ProApi.api("/api/auth/profile");
            session.userId = profile.id;
            session.fullName = profile.fullName || session.fullName;
            session.role = profile.role || session.role;
            ProApi.saveSession(session);
        } catch {
            /* keep cached session */
        }
    }
    const name = session.fullName || session.email || "Campus user";
    const marks = ProApi.initials(name);
    const role = ProApi.roleLabel(session.role);
    document.querySelectorAll("[data-user-name]").forEach((el) => {
        el.textContent = name;
    });
    document.querySelectorAll("[data-user-role]").forEach((el) => {
        el.textContent = role;
    });
    document.querySelectorAll("[data-user-initials]").forEach((el) => {
        el.textContent = marks;
        el.setAttribute("title", name);
    });
    document.querySelectorAll("[data-admin-only]").forEach((el) => {
        el.hidden = session.role !== "ADMIN";
    });
    mountNotificationBell();
    document.querySelectorAll(".topbar .avatar").forEach((el) => {
        el.addEventListener("click", () => {
            location.href = ProApi.page("profile");
        });
    });
    renderSidebarNav(session);
}

function navIcon(name) {
    const icons = {
        grid: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden="true"><rect x="3" y="3" width="7" height="7" rx="1.5" stroke="currentColor" stroke-width="1.7"/><rect x="14" y="3" width="7" height="7" rx="1.5" stroke="currentColor" stroke-width="1.7"/><rect x="3" y="14" width="7" height="7" rx="1.5" stroke="currentColor" stroke-width="1.7"/><rect x="14" y="14" width="7" height="7" rx="1.5" stroke="currentColor" stroke-width="1.7"/></svg>',
        list: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M8 6h13M8 12h13M8 18h13M3 6h.01M3 12h.01M3 18h.01" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/></svg>',
        plus: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M12 5v14M5 12h14" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/></svg>',
        book: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z" stroke="currentColor" stroke-width="1.7"/></svg>',
        bell: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M18 8A6 6 0 1 0 6 8c0 7-3 7-3 7h18s-3 0-3-7" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/><path d="M13.7 21a2 2 0 0 1-3.4 0" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/></svg>',
        user: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/><circle cx="12" cy="7" r="3.2" stroke="currentColor" stroke-width="1.7"/></svg>',
        chart: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M4 19V5M4 19h16M8 16v-5M12 16V8M16 16v-3" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/></svg>',
        layers: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="m12 3 9 5-9 5-9-5 9-5zM3 13l9 5 9-5" stroke="currentColor" stroke-width="1.7" stroke-linejoin="round"/></svg>',
        users: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/><circle cx="9" cy="7" r="3.2" stroke="currentColor" stroke-width="1.7"/><path d="M22 21v-2a3.5 3.5 0 0 0-2.6-3.4M16.2 3.8a3.2 3.2 0 0 1 0 6.2" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/></svg>'
    };
    return icons[name] || icons.grid;
}

const MAX_TICKET_ATTACHMENT_MB = 5;
const MAX_TICKET_ATTACHMENT_BYTES = MAX_TICKET_ATTACHMENT_MB * 1024 * 1024;

function validateAttachmentFiles(files, errorElement, bannerElement) {
    for (const file of files) {
        if (file && file.size > MAX_TICKET_ATTACHMENT_BYTES) {
            const message = `File "${file.name}" exceeds the ${MAX_TICKET_ATTACHMENT_MB} MB limit.`;
            if (errorElement) {
                errorElement.textContent = message;
                errorElement.hidden = false;
            }
            if (bannerElement) {
                showBanner(bannerElement, message, true);
            }
            return false;
        }
    }
    if (errorElement) {
        errorElement.textContent = "";
        errorElement.hidden = true;
    }
    return true;
}

const SIDEBAR_ROLES = ["ADMIN", "DEPT_HEAD", "STAFF", "STUDENT", "LECTURER"];

const SIDEBAR_NAV = [
    { id: "dashboard", page: "dashboard", label: "Command Center", icon: "grid", roles: SIDEBAR_ROLES },
    { id: "tickets", page: "tickets", label: "Ticket Ledger", icon: "list", roles: SIDEBAR_ROLES },
    { id: "submit", page: "submit", label: "Submit Request", icon: "plus", roles: SIDEBAR_ROLES },
    { id: "knowledge", page: "knowledge", label: "Knowledge Base", icon: "book", roles: SIDEBAR_ROLES },
    { id: "inbox", page: "inbox", label: "Inbox", icon: "bell", roles: SIDEBAR_ROLES },
    { id: "profile", page: "profile", label: "Profile", icon: "user", roles: SIDEBAR_ROLES },
    { id: "insights", page: "insights", label: "Insights", icon: "chart", roles: ["ADMIN", "DEPT_HEAD"] },
    { id: "departments", page: "departments", label: "Departments", icon: "layers", roles: ["ADMIN"] },
    { id: "accounts", page: "accounts", label: "Accounts", icon: "users", roles: ["ADMIN"] },
    { id: "sign-out", label: "Sign out", action: "sign-out", roles: SIDEBAR_ROLES }
];

function sidebarRole(session) {
    const role = session && session.role;
    return SIDEBAR_ROLES.includes(role) ? role : "STUDENT";
}

function renderSidebarNav(session) {
    const nav = document.querySelector(".sidebar-nav");
    if (!nav) {
        return;
    }
    const role = sidebarRole(session);
    const active = document.body.dataset.page;
    const visible = SIDEBAR_NAV.filter((item) => item.roles.includes(role));
    nav.innerHTML = visible
        .filter((item) => item.action !== "sign-out")
        .map((item) => `<a class="nav-link${item.id === active ? " active" : ""}" href="${ProApi.page(item.page)}">${navIcon(item.icon)}${item.label}</a>`)
        .join("");
    const signOut = visible.find((item) => item.action === "sign-out");
    document.querySelectorAll("[data-sign-out]").forEach((btn) => {
        btn.hidden = !signOut;
        if (signOut) {
            btn.textContent = signOut.label;
        }
    });
}

function showBanner(el, message, isError) {
    if (!el) {
        return;
    }
    el.textContent = message;
    el.classList.toggle("error", Boolean(isError));
    el.classList.add("show");
}

function hideBanner(el) {
    if (!el) {
        return;
    }
    el.classList.remove("show", "error");
    el.textContent = "";
}

function initLogin() {
    if (ProApi.redirectIfAuthed()) {
        return;
    }

    const emailInput = document.getElementById("email");
    const passwordInput = document.getElementById("password");
    const errorEl = document.getElementById("login-error");
    const form = document.getElementById("login-form");

    function fillPersona(key) {
        const account = ProApi.DEMO_ACCOUNTS[key];
        if (!account || !emailInput) {
            return;
        }
        emailInput.value = account.email;
        if (passwordInput) {
            passwordInput.value = account.password;
        }
    }

    document.querySelectorAll(".persona-btn").forEach((btn) => {
        btn.addEventListener("click", () => {
            document.querySelectorAll(".persona-btn").forEach((item) => {
                item.classList.remove("selected");
                item.setAttribute("aria-pressed", "false");
            });
            btn.classList.add("selected");
            btn.setAttribute("aria-pressed", "true");
            fillPersona(btn.dataset.persona);
            hideBanner(errorEl);
        });
    });

    const selected = document.querySelector(".persona-btn.selected");
    if (selected) {
        fillPersona(selected.dataset.persona);
    }

    form?.addEventListener("submit", async (event) => {
        event.preventDefault();
        hideBanner(errorEl);
        const email = (emailInput?.value || "").trim();
        const password = passwordInput?.value || "";
        if (!email || !password) {
            showBanner(errorEl, "Enter your university email and password.", true);
            return;
        }
        if (!/^[A-Za-z0-9._%+\-]+@(sliit\.lk|my\.sliit\.lk)$/i.test(email) || email.length > 150) {
            showBanner(errorEl, "Email must be a valid university address (@sliit.lk or @my.sliit.lk).", true);
            return;
        }
        if (password.length < 8 || password.length > 72) {
            showBanner(errorEl, "Password must be between 8 and 72 characters.", true);
            return;
        }
        const submitBtn = form.querySelector("[type=submit]");
        if (submitBtn) {
            submitBtn.disabled = true;
        }
        try {
            const data = await ProApi.api("/api/auth/login", {
                method: "POST",
                auth: false,
                body: { email, password }
            });
            ProApi.saveSession({
                token: data.token,
                email: data.email,
                role: data.role,
                fullName: data.fullName || data.email,
                userId: data.userId
            });
            location.replace(ProApi.page("dashboard"));
        } catch (err) {
            showBanner(errorEl, err.message || "Login failed.", true);
        } finally {
            if (submitBtn) {
                submitBtn.disabled = false;
            }
        }
    });

    const forgotForm = document.getElementById("forgot-form");
    const forgotToggle = document.getElementById("forgot-toggle");
    const forgotBanner = document.getElementById("forgot-banner");
    forgotToggle?.addEventListener("click", () => {
        if (forgotForm) {
            forgotForm.hidden = !forgotForm.hidden;
        }
    });
    forgotForm?.addEventListener("submit", async (event) => {
        event.preventDefault();
        hideBanner(forgotBanner);
        const email = (document.getElementById("forgot-email")?.value || "").trim();
        if (!email) {
            showBanner(forgotBanner, "Enter your university email.", true);
            return;
        }
        if (!/^[A-Za-z0-9._%+\-]+@(sliit\.lk|my\.sliit\.lk)$/i.test(email) || email.length > 150) {
            showBanner(forgotBanner, "Email must be a valid university address (@sliit.lk or @my.sliit.lk).", true);
            return;
        }
        try {
            const data = await ProApi.api("/api/auth/forgot-password", {
                method: "POST",
                auth: false,
                body: { email }
            });
            if (data.resetPath) {
                showBanner(forgotBanner, "Recovery token created. Open the reset page to choose a new password.");
                window.setTimeout(() => {
                    location.href = data.resetPath;
                }, 600);
            } else {
                showBanner(forgotBanner, data.message || "If that email is registered, a reset token was generated.");
            }
        } catch (err) {
            showBanner(forgotBanner, err.message || "Could not start password recovery.", true);
        }
    });
}

function toggleStudentIdField(role) {
    const field = document.getElementById("student-id-field");
    if (field) {
        field.hidden = role !== "STUDENT";
    }
}

function initRegister() {
    if (ProApi.redirectIfAuthed()) {
        return;
    }

    const form = document.getElementById("register-form");
    const roleInput = document.getElementById("role");
    const errorEl = document.getElementById("register-error");
    const emailInput = document.getElementById("email");
    const passwordInput = document.getElementById("password");
    const nameInput = document.getElementById("fullName");
    const departmentInput = document.getElementById("department");
    const studentIdInput = document.getElementById("studentId");

    function selectRole(btn) {
        document.querySelectorAll(".persona-btn").forEach((item) => {
            item.classList.remove("selected");
            item.setAttribute("aria-pressed", "false");
        });
        btn.classList.add("selected");
        btn.setAttribute("aria-pressed", "true");
        const role = btn.dataset.role || "STUDENT";
        if (roleInput) {
            roleInput.value = role;
        }
        toggleStudentIdField(role);
        hideBanner(errorEl);
    }

    document.querySelectorAll(".persona-btn").forEach((btn) => {
        btn.addEventListener("click", () => selectRole(btn));
    });
    const selected = document.querySelector(".persona-btn.selected");
    if (selected) {
        selectRole(selected);
    }

    form?.addEventListener("submit", async (event) => {
        event.preventDefault();
        hideBanner(errorEl);
        const fullName = (nameInput?.value || "").trim();
        const email = (emailInput?.value || "").trim();
        const password = passwordInput?.value || "";
        const role = "STUDENT";
        if (roleInput) {
            roleInput.value = "STUDENT";
        }
        const department = (departmentInput?.value || "").trim();
        const studentId = (studentIdInput?.value || "").trim();
        if (!fullName || !email || !password) {
            showBanner(errorEl, "Enter your name, university email, and password.", true);
            return;
        }
        if (!/^[A-Za-z0-9._%+\-]+@(sliit\.lk|my\.sliit\.lk)$/i.test(email) || email.length > 150) {
            showBanner(errorEl, "Email must be a valid university address (@sliit.lk or @my.sliit.lk).", true);
            return;
        }
        if (fullName.length > 100) {
            showBanner(errorEl, "Full name must be at most 100 characters.", true);
            return;
        }
        if (password.length < 8 || password.length > 72) {
            showBanner(errorEl, "Password must be between 8 and 72 characters.", true);
            return;
        }
        if (department.length > 100) {
            showBanner(errorEl, "Department must be at most 100 characters.", true);
            return;
        }
        if (studentId.length > 20) {
            showBanner(errorEl, "Student ID must be at most 20 characters.", true);
            return;
        }
        const submitBtn = form.querySelector("[type=submit]");
        if (submitBtn) {
            submitBtn.disabled = true;
        }
        try {
            const data = await ProApi.api("/api/auth/register", {
                method: "POST",
                auth: false,
                body: {
                    fullName,
                    email,
                    password,
                    role,
                    department: department || null,
                    studentId: role === "STUDENT" ? (studentId || null) : null
                }
            });
            ProApi.saveSession({
                token: data.token,
                email: data.email,
                role: data.role,
                fullName: data.fullName || fullName,
                userId: data.userId
            });
            location.replace(ProApi.page("dashboard"));
        } catch (err) {
            showBanner(errorEl, err.message || "Could not create the account.", true);
        } finally {
            if (submitBtn) {
                submitBtn.disabled = false;
            }
        }
    });
}

async function initAccounts() {
    const session = ProApi.getSession();
    if (!session || session.role !== "ADMIN") {
        location.replace(ProApi.page("dashboard"));
        return;
    }

    const form = document.getElementById("account-form");
    const banner = document.getElementById("account-banner");
    const body = document.getElementById("accounts-body");
    const roleSelect = document.getElementById("role");

    roleSelect?.addEventListener("change", () => toggleStudentIdField(roleSelect.value));
    toggleStudentIdField(roleSelect?.value || "STUDENT");

    async function refresh() {
        if (body) {
            body.innerHTML = `<tr><td colspan="6"><p class="empty-note">Loading accounts…</p></td></tr>`;
        }
        try {
            const users = await ProApi.api("/api/admin/users");
            renderAccounts(Array.isArray(users) ? users : []);
        } catch (err) {
            if (body) {
                body.innerHTML = `<tr><td colspan="6"><p class="empty-note">${ProApi.escapeHtml(err.message)}</p></td></tr>`;
            }
        }
    }

    function renderAccounts(users) {
        if (!body) {
            return;
        }
        if (!users.length) {
            body.innerHTML = `<tr><td colspan="6"><p class="empty-note">No accounts yet.</p></td></tr>`;
            return;
        }
        body.innerHTML = users.map((user) => {
            const active = user.enabled !== false;
            const adminAccount = String(user.role || "").trim().toUpperCase() === "ADMIN";
            const activity = `<a class="btn btn-ghost" href="${ProApi.page("profile", "userId=" + user.id)}">Activity</a>`;
            let actions = "";
            if (user.email !== session.email) {
                actions = adminAccount
                    ? `${activity}<span class="hint">Admin accounts cannot be deactivated</span>`
                    : (active
                        ? `<label class="hint"><input type="checkbox" data-permanent="${user.id}"/> Permanent</label>
                           ${activity}
                           <button class="btn btn-ghost" type="button" data-deactivate="${user.id}">Disable</button>`
                        : activity);
            }
            return `<tr>
                <td>${ProApi.escapeHtml(user.fullName || "—")}</td>
                <td>${ProApi.escapeHtml(user.email || "")}</td>
                <td>${ProApi.escapeHtml(ProApi.roleLabel(user.role))}</td>
                <td>${ProApi.escapeHtml(user.department || "—")}</td>
                <td><span class="badge ${active ? "open" : "resolved"}">${active ? "Active" : "Disabled"}</span></td>
                <td>${actions}</td>
            </tr>`;
        }).join("");
        body.querySelectorAll("[data-deactivate]").forEach((btn) => {
            btn.addEventListener("click", async () => {
                const id = btn.getAttribute("data-deactivate");
                const permanent = Boolean(body.querySelector("[data-permanent='" + id + "']")?.checked);
                if (!id || !confirm(permanent
                    ? "Permanently delete this account and all of its data?"
                    : "Disable this account?")) {
                    return;
                }
                try {
                    await ProApi.api("/api/admin/users/" + id + (permanent ? "?permanent=true" : ""), { method: "DELETE" });
                    showBanner(banner, permanent ? "Account permanently deleted." : "Account disabled.");
                    await refresh();
                } catch (err) {
                    showBanner(banner, err.message || "Could not disable the account.", true);
                }
            });
        });
    }

    form?.addEventListener("submit", async (event) => {
        event.preventDefault();
        hideBanner(banner);
        const fullName = (document.getElementById("fullName")?.value || "").trim();
        const email = (document.getElementById("email")?.value || "").trim();
        const password = document.getElementById("password")?.value || "";
        const role = roleSelect?.value || "STUDENT";
        const department = (document.getElementById("department")?.value || "").trim();
        const studentId = (document.getElementById("studentId")?.value || "").trim();
        if (!fullName || !email || !password) {
            showBanner(banner, "Name, email, and password are required.", true);
            return;
        }
        if (!/^[A-Za-z0-9._%+\-]+@(sliit\.lk|my\.sliit\.lk)$/i.test(email)) {
            showBanner(banner, "Email must be a valid university address (@sliit.lk or @my.sliit.lk).", true);
            return;
        }
        if (fullName.length > 100) {
            showBanner(banner, "Full name must be at most 100 characters.", true);
            return;
        }
        if (password.length < 8 || password.length > 72) {
            showBanner(banner, "Password must be between 8 and 72 characters.", true);
            return;
        }
        if (department.length > 100) {
            showBanner(banner, "Department must be at most 100 characters.", true);
            return;
        }
        if (studentId.length > 20) {
            showBanner(banner, "Student ID must be at most 20 characters.", true);
            return;
        }
        const submitBtn = form.querySelector("[type=submit]");
        if (submitBtn) {
            submitBtn.disabled = true;
        }
        try {
            await ProApi.api("/api/admin/users", {
                method: "POST",
                body: {
                    fullName,
                    email,
                    password,
                    role,
                    department: department || null,
                    studentId: role === "STUDENT" ? (studentId || null) : null,
                    enabled: true
                }
            });
            form.reset();
            if (roleSelect) {
                roleSelect.value = "STUDENT";
            }
            toggleStudentIdField("STUDENT");
            showBanner(banner, "Account created. They can sign in with the email and password you set.");
            await refresh();
        } catch (err) {
            showBanner(banner, err.message || "Could not create the account.", true);
        } finally {
            if (submitBtn) {
                submitBtn.disabled = false;
            }
        }
    });

    await refresh();
}

async function initDashboard() {
    const heroTitle = document.getElementById("hero-title");
    const heroCopy = document.getElementById("hero-copy");
    const activityList = document.getElementById("activity-list");
    const session = ProApi.getSession();
    if (heroTitle) {
        heroTitle.textContent = "Welcome back, " + ProApi.firstName(session?.fullName) + "!";
    }
    try {
        const [ticketRows, notifications] = await Promise.all([
            ProApi.api("/api/tickets"),
            ProApi.api("/api/notifications").catch(() => [])
        ]);
        const tickets = Array.isArray(ticketRows) ? ticketRows : [];
        const inProgress = tickets.filter((t) => t.status === "IN_PROGRESS").length;
        const resolved = tickets.filter((t) => t.status === "RESOLVED" || t.status === "CLOSED").length;
        const unread = (notifications || []).filter((n) => !n.read).length;
        setText("stat-total", tickets.length);
        setText("stat-progress", inProgress);
        setText("stat-resolved", resolved);
        const requestWord = inProgress === 1 ? "request" : "requests";
        let copy = "You have " + inProgress + " " + requestWord + " in progress.";
        if (unread) {
            copy += " " + unread + " unread notification" + (unread === 1 ? "" : "s") + ".";
        }
        if (heroCopy) {
            heroCopy.textContent = copy;
        }
        renderNotifications(document.getElementById("notification-list"), notifications || []);
        renderActivity(activityList, tickets.slice(0, 8));
        const dashSearch = document.querySelector(".topbar-search .search-input");
        if (dashSearch && !dashSearch.dataset.bound) {
            dashSearch.dataset.bound = "true";
            dashSearch.addEventListener("keydown", (event) => {
                if (event.key !== "Enter") {
                    return;
                }
                const q = dashSearch.value.trim();
                location.href = ProApi.page("tickets", q ? "q=" + encodeURIComponent(q) : "");
            });
        }
    } catch (err) {
        if (heroCopy) {
            heroCopy.textContent = err.message || "Unable to load your workspace.";
        }
        if (activityList) {
            activityList.innerHTML = `<div class="activity-item"><div><strong>Could not load activity</strong><span>${ProApi.escapeHtml(err.message)}</span></div></div>`;
        }
    }
}

function setText(id, value) {
    const el = document.getElementById(id);
    if (el) {
        el.textContent = value;
    }
}

function renderActivity(list, tickets) {
    if (!list) {
        return;
    }
    if (!tickets.length) {
        list.innerHTML = `<div class="activity-item"><div><strong>No recent tickets yet</strong><span>Raise a request to see live updates here.</span></div></div>`;
        return;
    }
    list.innerHTML = tickets.map((ticket) => {
        const assignee = ticket.assignee ? "Assigned to " + ticket.assignee : "Unassigned";
        const meta = [assignee, departmentLabel(ticket), prettyPriority(ticket.priority)].join(" · ");
        return `<div class="activity-item">
            <span class="dot ${ProApi.activityDot(ticket)}"></span>
            <div>
                <strong>${ProApi.escapeHtml(ticket.ticketNumber || "")} — ${ProApi.escapeHtml(ticket.title || "Untitled")}</strong>
                <span>${ProApi.escapeHtml(meta)}</span>
            </div>
            <time>${ProApi.escapeHtml(ProApi.relativeTime(ticket.updatedAt || ticket.createdAt))}</time>
        </div>`;
    }).join("");
}

function prettyPriority(priority) {
    const key = ProApi.priorityKey(priority);
    return key.charAt(0).toUpperCase() + key.slice(1);
}

function departmentLabel(ticket) {
    return ticket.department || ticket.category || "—";
}

function categoryCell(ticket) {
    const category = ticket.category || "—";
    const department = ticket.department && ticket.department !== ticket.category
        ? ticket.department
        : "";
    return `${ProApi.escapeHtml(category)}${department ? `<span class="ticket-assignee">${ProApi.escapeHtml(department)}</span>` : ""}`;
}

function renderNotifications(list, notifications) {
    if (!list) {
        return;
    }
    const state = notificationState(notifications);
    if (!notifications.length) {
        list.innerHTML = state;
        bindNotificationActions();
        return;
    }
    list.innerHTML = state + notifications.slice(0, 8).map((item) => {
        const unread = item.read !== true;
        const href = item.ticketId ? ProApi.page("tickets", "id=" + item.ticketId) : ProApi.page("tickets");
        const when = item.relativeTime || ProApi.relativeTime(item.createdAt);
        return `<div class="activity-item">
            <span class="dot ${unread ? "rose" : "green"}"></span>
            <div>
                <strong>${ProApi.escapeHtml(item.message || "Notification")}</strong>
                <span>${unread ? "Unread" : "Read"} · ${ProApi.escapeHtml(when)}</span>
            </div>
            <div class="ticket-actions">
                <a class="btn btn-ghost" href="${href}">Open</a>
                ${unread ? `<button class="btn btn-ghost" type="button" data-read="${item.id}">Mark read</button>` : ""}
            </div>
        </div>`;
    }).join("");
    bindNotificationActions();
}

function bindNotificationActions() {
    const markAll = document.getElementById("mark-all-read");
    if (markAll && !markAll.dataset.bound) {
        markAll.dataset.bound = "true";
        markAll.addEventListener("click", async () => {
            try {
                await ProApi.api("/api/notifications/read-all", { method: "POST" });
                initDashboard();
            } catch (err) {
                window.alert(err.message || "Could not mark notifications as read.");
            }
        });
    }
    document.querySelectorAll("[data-read]").forEach((btn) => {
        btn.addEventListener("click", async () => {
            try {
                await ProApi.api("/api/notifications/" + btn.dataset.read + "/read", { method: "PUT" });
                initDashboard();
            } catch (err) {
                window.alert(err.message || "Could not mark as read.");
            }
        });
    });
}

async function initTickets() {
    const tbody = document.getElementById("ticket-rows");
    const presetQuery = new URLSearchParams(location.search).get("q");
    if (presetQuery) {
        const localSearch = document.getElementById("ticket-search");
        const topSearch = document.querySelector(".topbar-search .search-input");
        if (localSearch) {
            localSearch.value = presetQuery;
        }
        if (topSearch) {
            topSearch.value = presetQuery;
        }
    }
    await fillCategoryFilter();
    bindTicketFilters();
    try {
        const tickets = await loadTicketRows();
        const selectedId = new URLSearchParams(location.search).get("id");
        if (selectedId) {
            openTicketDetail(selectedId);
        }
        return tickets;
    } catch (err) {
        if (tbody) {
            tbody.innerHTML = `<tr><td colspan="7">${ProApi.escapeHtml(err.message)}</td></tr>`;
        }
        return [];
    }
}

async function loadTicketRows() {
    const tbody = document.getElementById("ticket-rows");
    const params = new URLSearchParams();
    const query = (document.getElementById("ticket-search")?.value
        || document.querySelector(".topbar-search .search-input")?.value
        || "").trim();
    const categoryId = document.getElementById("ticket-category-filter")?.value;
    if (query) {
        params.set("q", query);
    }
    if (categoryId) {
        params.set("categoryId", categoryId);
    }
    const rows = await ProApi.api("/api/tickets" + (params.toString() ? "?" + params : ""));
    const tickets = Array.isArray(rows) ? rows : [];
    window.__ticketCache = tickets;
    updatePillCounts(tickets);
    renderTicketRows(tbody, tickets);
    applyTicketVisibility();
    return tickets;
}

async function fillCategoryFilter() {
    const select = document.getElementById("ticket-category-filter");
    if (!select || select.dataset.filled) {
        return;
    }
    try {
        const categories = await loadSubmitCategories();
        select.innerHTML = `<option value="">All departments</option>` + categories.map((item) => {
            const id = item.id != null ? item.id : item.categoryId;
            return `<option value="${id}">${ProApi.escapeHtml(categoryOptionLabel(item))}</option>`;
        }).join("");
        select.dataset.filled = "true";
    } catch {
        /* keep default option */
    }
}

function bindTicketFilters() {
    if (document.body.dataset.ticketFiltersBound) {
        return;
    }
    document.body.dataset.ticketFiltersBound = "true";
    document.querySelectorAll(".pill-tab").forEach((tab) => {
        tab.addEventListener("click", () => {
            document.querySelectorAll(".pill-tab").forEach((item) => item.classList.remove("active"));
            tab.classList.add("active");
            applyTicketVisibility();
        });
    });
    const runSearch = ProApi.debounce(() => loadTicketRows().catch(() => undefined), 220);
    document.getElementById("ticket-search")?.addEventListener("input", runSearch);
    document.querySelector(".topbar-search .search-input")?.addEventListener("input", (event) => {
        const local = document.getElementById("ticket-search");
        if (local) {
            local.value = event.target.value;
        }
        runSearch();
    });
    document.getElementById("ticket-category-filter")?.addEventListener("change", () => {
        loadTicketRows().catch(() => undefined);
    });
}

function applyTicketVisibility() {
    const filter = document.querySelector(".pill-tab.active")?.dataset.filter || "all";
    document.querySelectorAll(".ticket-row").forEach((row) => {
        row.hidden = !(filter === "all" || row.dataset.status === filter);
    });
}

function updatePillCounts(tickets) {
    const open = tickets.filter((t) => t.status === "OPEN").length;
    const progress = tickets.filter((t) => t.status === "IN_PROGRESS").length;
    const resolved = tickets.filter((t) => t.status === "RESOLVED" || t.status === "CLOSED").length;
    setText("count-all", tickets.length);
    setText("count-open", open);
    setText("count-progress", progress);
    setText("count-resolved", resolved);
}

function renderTicketRows(tbody, tickets) {
    if (!tbody) {
        return;
    }
    if (!tickets.length) {
        tbody.innerHTML = `<tr><td colspan="7">No tickets in your ledger yet.</td></tr>`;
        return;
    }
    tbody.innerHTML = tickets.map((ticket) => {
        const prio = ProApi.priorityKey(ticket.priority);
        const status = ProApi.statusFilter(ticket.status);
        return `<tr class="ticket-row priority-${ProApi.escapeHtml(prio)}" data-status="${ProApi.escapeHtml(status)}" data-id="${ticket.id || ""}">
            <td class="ticket-id">${ProApi.escapeHtml(ticket.ticketNumber || "—")}</td>
            <td>
                <span class="ticket-subject">${ProApi.escapeHtml(ticket.title || "Untitled")}</span>
                <span class="ticket-assignee">Assignee: ${ProApi.escapeHtml(ticket.assignee || "Unassigned")}</span>
            </td>
            <td>${ProApi.escapeHtml(submitterRoleLabel(ticket.submitterRole))}</td>
            <td>${categoryCell(ticket)}</td>
            <td><span class="badge ${ProApi.escapeHtml(prio)}">${ProApi.escapeHtml(prettyPriority(ticket.priority))}</span></td>
            <td><span class="badge ${ProApi.escapeHtml(ProApi.statusBadgeClass(ticket.status))}">${ProApi.escapeHtml(ProApi.statusLabel(ticket.status))}</span></td>
            <td>${ProApi.escapeHtml(ProApi.relativeTime(ticket.updatedAt || ticket.createdAt))}</td>
        </tr>`;
    }).join("");
    tbody.querySelectorAll(".ticket-row[data-id]").forEach((row) => {
        row.addEventListener("click", () => openTicketDetail(row.dataset.id));
    });
}

const TICKET_CATEGORIES = [
    "IT Support",
    "Academic",
    "Library Services",
    "Finance Office",
    "Hostel",
    "Facilities",
    "Examinations",
    "General"
];

function canonicalCategories(items) {
    const byName = new Map();
    (items || []).forEach((item) => {
        const name = item && item.name;
        if (TICKET_CATEGORIES.includes(name) && !byName.has(name)) {
            byName.set(name, item);
        }
    });
    return TICKET_CATEGORIES.map((name) => byName.get(name)).filter(Boolean);
}

function submitterRoleLabel(role) {
    if (role === "STUDENT") {
        return "Student";
    }
    if (role === "LECTURER") {
        return "Lecturer";
    }
    return role ? ProApi.roleLabel(role) : "—";
}

function categoryOptionLabel(item) {
    const name = item.name || "Category";
    const department = item.department || "";
    return department && department !== name ? name + " — " + department : name;
}

function setCategoryError(visible) {
    const errorEl = document.getElementById("category-error");
    if (!errorEl) {
        return;
    }
    errorEl.hidden = !visible;
}

async function openTicketDetail(id) {
    const panel = document.getElementById("ticket-detail");
    if (!panel || !id) {
        return;
    }
    const session = ProApi.getSession() || {};
    document.querySelectorAll(".ticket-row").forEach((row) => {
        row.classList.toggle("is-open", row.dataset.id === String(id));
    });
    panel.hidden = false;
    panel.innerHTML = `<p class="empty-note">Loading ticket…</p>`;
    try {
        const [ticket, auditHistory] = await Promise.all([
            ProApi.api("/api/tickets/" + id),
            ProApi.api("/api/tickets/" + id + "/history").catch(() => [])
        ]);
        const canManage = Boolean(ticket.canManage);
        const canEditDetails = Boolean(ticket.canEdit);
        const isRequester = String(ticket.requesterId) === String(session.userId);
        const comments = Array.isArray(ticket.comments) ? ticket.comments : [];
        const attachments = Array.isArray(ticket.attachments) ? ticket.attachments : [];
        const staff = Array.isArray(ticket.staffMembers) ? ticket.staffMembers : [];
        const categories = await loadSubmitCategories().catch(() => []);
        const commentHtml = comments.length
            ? comments.map((comment) => {
                const own = String(comment.authorId) === String(session.userId);
                return `<div class="comment-item" data-comment="${comment.id || ""}">
                <strong>${ProApi.escapeHtml(comment.author || "Campus user")}</strong>
                <small>${ProApi.escapeHtml(ProApi.relativeTime(comment.createdAt))}</small>
                <p>${ProApi.escapeHtml(comment.body || "")}</p>
                ${own ? `<div class="ticket-actions">
                    <button class="btn btn-ghost" type="button" data-edit-comment="${comment.id}">Edit</button>
                    <button class="btn btn-ghost" type="button" data-delete-comment="${comment.id}">Delete</button>
                </div>` : ""}
            </div>`;
            }).join("")
            : `<p class="empty-note">No replies yet.</p>`;
        const canChangeFiles = canEditDetails || canManage;
        const attachmentHtml = attachments.length
            ? `<div class="attachment-list">${attachments.map((file) =>
                `<div class="ticket-actions">
                    <a href="${ProApi.escapeHtml(file.filePath || "#")}" target="_blank" rel="noopener">${ProApi.escapeHtml(file.fileName || "file")}</a>
                    ${canChangeFiles ? `<button class="btn btn-ghost" type="button" data-delete-attachment="${file.id}">Remove</button>` : ""}
                </div>`
            ).join("")}</div>`
            : `<p class="empty-note">No files attached.</p>`;
        const historyHtml = (auditHistory || []).length
            ? `<div class="timeline">${auditHistory.map((item) =>
                `<div class="activity-item"><span class="dot cyan"></span><div>
                    <strong>${ProApi.escapeHtml(item.action || "Update")}</strong>
                    <span>${ProApi.escapeHtml(item.actor || "System")} · ${ProApi.escapeHtml(ProApi.relativeTime(item.createdAt))}</span>
                </div></div>`
            ).join("")}</div>`
            : `<p class="empty-note">No history recorded yet.</p>`;
        const staffOptions = staff.map((member) =>
            `<option value="${member.id}" ${String(member.id) === String(ticket.assigneeId) ? "selected" : ""}>${ProApi.escapeHtml(member.fullName || "Staff")}</option>`
        ).join("");
        const categoryOptions = categories.map((item) => {
            const optionId = item.id != null ? item.id : item.categoryId;
            return `<option value="${optionId}" ${String(optionId) === String(ticket.categoryId) ? "selected" : ""}>${ProApi.escapeHtml(categoryOptionLabel(item))}</option>`;
        }).join("");
        panel.innerHTML = `
            <div class="ticket-detail-head">
                <div>
                    <p class="ticket-id">${ProApi.escapeHtml(ticket.ticketNumber || "")}</p>
                    <h3>${ProApi.escapeHtml(ticket.title || "Untitled")}</h3>
                </div>
                <span class="badge ${ProApi.escapeHtml(ProApi.statusBadgeClass(ticket.status))}">${ProApi.escapeHtml(ProApi.statusLabel(ticket.status))}</span>
            </div>
            <p>${ProApi.escapeHtml(ticket.description || "")}</p>
            <div class="ticket-meta">
                <p>Category<strong>${ProApi.escapeHtml(ticket.category || "—")}</strong></p>
                <p>Department<strong>${ProApi.escapeHtml(ticket.department || "—")}</strong></p>
                <p>Submitter<strong>${ProApi.escapeHtml(submitterRoleLabel(ticket.submitterRole))}</strong></p>
                <p>Priority<strong>${ProApi.escapeHtml(prettyPriority(ticket.priority))}</strong></p>
                <p>Assignee<strong>${ProApi.escapeHtml(ticket.assignee || "Unassigned")}</strong></p>
                <p>SLA due<strong>${ProApi.escapeHtml(ticket.slaDueAt ? ProApi.relativeTime(ticket.slaDueAt) : "—")}</strong></p>
            </div>
            <h4>Attachments</h4>
            ${attachmentHtml}
            ${canChangeFiles ? `
            <form id="attach-form" class="ticket-actions">
                <input id="more-files" type="file" multiple accept="image/*,.pdf"/>
                <button class="btn btn-ghost" type="submit">Upload files</button>
            </form>` : ""}
            ${canEditDetails ? `
                <form id="details-form" class="field">
                    <label for="ticket-title">Title</label>
                    <input id="ticket-title" maxlength="200" value="${ProApi.escapeHtml(ticket.title || "")}" required/>
                    <label for="ticket-description">Description</label>
                    <textarea id="ticket-description" required maxlength="4000">${ProApi.escapeHtml(ticket.description || "")}</textarea>
                    <div class="ticket-actions">
                        ${categoryOptions ? `<label>Category
                            <select id="ticket-edit-category">${categoryOptions}</select>
                        </label>` : ""}
                        ${canManage ? `<label>Priority
                            <select id="ticket-priority">
                                <option value="LOW" ${ticket.priority === "LOW" ? "selected" : ""}>Low</option>
                                <option value="MEDIUM" ${ticket.priority === "MEDIUM" ? "selected" : ""}>Medium</option>
                                <option value="HIGH" ${ticket.priority === "HIGH" ? "selected" : ""}>High</option>
                                <option value="CRITICAL" ${ticket.priority === "CRITICAL" ? "selected" : ""}>Critical</option>
                            </select>
                        </label>` : ""}
                        <button class="btn btn-ghost" type="submit">Save details</button>
                    </div>
                </form>
            ` : ""}
            <h4>History</h4>
            ${historyHtml}
            <h4>Conversation</h4>
            <div class="comment-thread">${commentHtml}</div>
            ${ticket.canComment ? `
            <form id="comment-form" class="field">
                <label for="comment-body">Reply</label>
                <textarea id="comment-body" required maxlength="4000" placeholder="Write a reply…"></textarea>
                <div class="ticket-actions">
                    <button class="btn btn-primary" type="submit">Post reply</button>
                </div>
            </form>` : ""}
            ${isRequester && (ticket.status === "RESOLVED" || ticket.status === "CLOSED") ? `
                <form id="feedback-form" class="field">
                    <label>Rate this resolution
                        <select id="feedback-rating">
                            <option value="5">5 — Excellent</option>
                            <option value="4">4 — Good</option>
                            <option value="3" selected>3 — OK</option>
                            <option value="2">2 — Poor</option>
                            <option value="1">1 — Unresolved</option>
                        </select>
                    </label>
                    <textarea id="feedback-comment" placeholder="Optional comment"></textarea>
                    <div class="ticket-actions">
                        <button class="btn btn-primary" type="submit">Send feedback</button>
                    </div>
                </form>
            ` : ""}
            ${ticket.canConfirmClose ? `
                <div class="resolution-panel">
                    <h4>This ticket is resolved</h4>
                    <p>Confirm and close it if the issue is fixed, or reopen it if it is not. If you do nothing for 5 days, it closes on its own. The same staff member stays assigned.</p>
                    <div class="ticket-actions">
                        <button class="btn btn-primary" type="button" id="confirm-close">Confirm &amp; Close</button>
                        ${ticket.canReopenResolved ? `<button class="btn btn-ghost" type="button" id="reopen-resolved">Reopen</button>` : ""}
                    </div>
                </div>` : ""}
            ${ticket.canStaffClose ? `
                <form id="staff-close-form" class="resolution-panel field">
                    <h4>Close this resolved ticket</h4>
                    <label for="staff-close-reason">Reason</label>
                    <textarea id="staff-close-reason" required maxlength="500" placeholder="Spam, duplicate, or confirmed outside the portal"></textarea>
                    <div class="ticket-actions">
                        <button class="btn btn-primary" type="submit">Close with note</button>
                    </div>
                </form>` : ""}
            ${ticket.closedNotice ? `<p class="empty-note">${ProApi.escapeHtml(ticket.closedNotice)}</p>` : ""}
            ${ticket.canReopen ? `
                <form id="reopen-form" class="field">
                    <label for="reopen-reason">Reason for reopening</label>
                    <textarea id="reopen-reason" required maxlength="500" placeholder="Short reason"></textarea>
                    <div class="ticket-actions">
                        <button class="btn btn-primary" type="submit">Reopen ticket</button>
                    </div>
                </form>` : ""}
            <div class="ticket-actions">
                ${!canManage && ticket.status === "OPEN" && !ticket.assigneeId ? `<button class="btn btn-primary" type="button" id="close-ticket">Close ticket</button>` : ""}
                ${ticket.canDelete ? `<button class="btn btn-ghost" type="button" id="withdraw-ticket">${ticket.assigneeId ? "Delete ticket" : "Withdraw ticket"}</button>` : ""}
                ${ticket.canRequestDelete ? `<button class="btn btn-ghost" type="button" id="request-delete">Request deletion</button>` : ""}
                ${ticket.deleteRequested && !ticket.deleteApproved && !canManage ? `<p class="empty-note">Deletion requested. Waiting for staff approval.</p>` : ""}
                ${ticket.canApproveDelete ? `<button class="btn btn-primary" type="button" id="approve-delete">Approve deletion</button>` : ""}
                ${canManage && ticket.status !== "CLOSED" ? `<button class="btn btn-ghost" type="button" id="escalate-ticket">Escalate</button>` : ""}
            </div>
            ${canManage ? `
                <div class="ticket-actions">
                    <label>Status
                        <select id="ticket-status">
                            <option value="OPEN" ${ticket.status === "OPEN" ? "selected" : ""}>Open</option>
                            ${ticket.assigneeId ? `<option value="IN_PROGRESS" ${ticket.status === "IN_PROGRESS" ? "selected" : ""}>In Progress</option>` : ""}
                            <option value="RESOLVED" ${ticket.status === "RESOLVED" ? "selected" : ""}>Resolved</option>
                            ${ticket.status === "RESOLVED" ? "" : `<option value="CLOSED" ${ticket.status === "CLOSED" ? "selected" : ""}>Closed</option>`}
                        </select>
                    </label>
                    <button class="btn btn-ghost" type="button" id="save-status">Update status</button>
                    ${staffOptions ? `<label>Assign
                        <select id="ticket-assignee">${staffOptions}</select>
                    </label>
                    <button class="btn btn-ghost" type="button" id="save-assignee">Assign</button>` : ""}
                    ${categoryOptions ? `<label>Department / category
                        <select id="ticket-category">${categoryOptions}</select>
                    </label>
                    <button class="btn btn-ghost" type="button" id="save-category">Update category</button>` : ""}
                </div>
            ` : ""}`;
        history.replaceState({}, "", ProApi.page("tickets", "id=" + id));
        bindTicketDetailActions(id);
    } catch (err) {
        panel.innerHTML = `<p class="empty-note">${ProApi.escapeHtml(err.message || "Unable to load this ticket.")}</p>`;
    }
}

function bindTicketDetailActions(id) {
    document.getElementById("comment-form")?.addEventListener("submit", async (event) => {
        event.preventDefault();
        const body = (document.getElementById("comment-body")?.value || "").trim();
        if (!body) {
            window.alert("Comment cannot be empty.");
            return;
        }
        if (body.length > 4000) {
            window.alert("Comment must be at most 4000 characters.");
            return;
        }
        try {
            await ProApi.api("/api/tickets/" + id + "/comments", { method: "POST", body: { body } });
            openTicketDetail(id);
        } catch (err) {
            window.alert(err.message || "Could not add the reply.");
        }
    });
    document.getElementById("details-form")?.addEventListener("submit", async (event) => {
        event.preventDefault();
        const title = (document.getElementById("ticket-title")?.value || "").trim();
        const description = (document.getElementById("ticket-description")?.value || "").trim();
        if (!title || !description) {
            window.alert("Title and description are required.");
            return;
        }
        if (title.length > 200 || description.length > 4000) {
            window.alert("Title must be at most 200 characters and description at most 4000.");
            return;
        }
        const categoryValue = document.getElementById("ticket-edit-category")?.value;
        try {
            await ProApi.api("/api/tickets/" + id, {
                method: "PUT",
                body: {
                    title,
                    description,
                    priority: document.getElementById("ticket-priority")?.value || null,
                    categoryId: categoryValue ? Number(categoryValue) : null
                }
            });
            await loadTicketRows();
            openTicketDetail(id);
        } catch (err) {
            window.alert(err.message || "Could not save the ticket.");
        }
    });
    const moreFilesInput = document.getElementById("more-files");
    moreFilesInput?.addEventListener("change", () => {
        const files = Array.from(moreFilesInput.files || []);
        for (const file of files) {
            if (file && file.size > MAX_TICKET_ATTACHMENT_BYTES) {
                window.alert(`File "${file.name}" exceeds the ${MAX_TICKET_ATTACHMENT_MB} MB limit.`);
                moreFilesInput.value = "";
                return;
            }
        }
    });
    document.getElementById("attach-form")?.addEventListener("submit", async (event) => {
        event.preventDefault();
        const files = Array.from(document.getElementById("more-files")?.files || []);
        for (const file of files) {
            if (file && file.size > MAX_TICKET_ATTACHMENT_BYTES) {
                window.alert(`File "${file.name}" exceeds the ${MAX_TICKET_ATTACHMENT_MB} MB limit.`);
                return;
            }
        }
        try {
            for (const file of files) {
                await ProApi.uploadFile("/api/tickets/" + id + "/attachments", file);
            }
            openTicketDetail(id);
        } catch (err) {
            window.alert(err.message || "Could not upload the attachment.");
        }
    });
    document.querySelectorAll("[data-delete-attachment]").forEach((btn) => {
        btn.addEventListener("click", async () => {
            const attachmentId = btn.getAttribute("data-delete-attachment");
            if (!attachmentId || !confirm("Remove this attachment?")) {
                return;
            }
            try {
                await ProApi.api("/api/tickets/" + id + "/attachments/" + attachmentId, { method: "DELETE" });
                openTicketDetail(id);
            } catch (err) {
                window.alert(err.message || "Could not remove the attachment.");
            }
        });
    });
    document.getElementById("feedback-form")?.addEventListener("submit", async (event) => {
        event.preventDefault();
        try {
            await ProApi.api("/api/tickets/" + id + "/feedback", {
                method: "POST",
                body: {
                    rating: Number(document.getElementById("feedback-rating")?.value || 3),
                    comment: document.getElementById("feedback-comment")?.value || ""
                }
            });
            openTicketDetail(id);
        } catch (err) {
            window.alert(err.message || "Could not save the feedback.");
        }
    });
    document.querySelectorAll("[data-edit-comment]").forEach((btn) => {
        btn.addEventListener("click", async () => {
            const next = window.prompt("Edit your reply");
            if (!next || !next.trim()) {
                return;
            }
            try {
                await ProApi.api("/api/comments/" + btn.dataset.editComment, { method: "PUT", body: { body: next.trim() } });
                openTicketDetail(id);
            } catch (err) {
                window.alert(err.message || "Could not edit the reply.");
            }
        });
    });
    document.querySelectorAll("[data-delete-comment]").forEach((btn) => {
        btn.addEventListener("click", async () => {
            if (!window.confirm("Delete this reply?")) {
                return;
            }
            try {
                await ProApi.api("/api/comments/" + btn.dataset.deleteComment, { method: "DELETE" });
                openTicketDetail(id);
            } catch (err) {
                window.alert(err.message || "Could not delete the reply.");
            }
        });
    });
    document.getElementById("confirm-close")?.addEventListener("click", async () => {
        try {
            await ProApi.api("/api/tickets/" + id + "/confirm-close", { method: "POST" });
            await loadTicketRows();
            openTicketDetail(id);
        } catch (err) {
            window.alert(err.message || "Could not close the ticket.");
        }
    });
    document.getElementById("reopen-resolved")?.addEventListener("click", async () => {
        try {
            await ProApi.api("/api/tickets/" + id + "/reopen-resolved", { method: "POST" });
            await loadTicketRows();
            openTicketDetail(id);
        } catch (err) {
            window.alert(err.message || "Could not reopen the ticket.");
        }
    });
    document.getElementById("staff-close-form")?.addEventListener("submit", async (event) => {
        event.preventDefault();
        const reason = (document.getElementById("staff-close-reason")?.value || "").trim();
        if (!reason) {
            window.alert("A reason is required to close this ticket.");
            return;
        }
        try {
            await ProApi.api("/api/tickets/" + id + "/staff-close", { method: "POST", body: { reason } });
            await loadTicketRows();
            openTicketDetail(id);
        } catch (err) {
            window.alert(err.message || "Could not close the ticket.");
        }
    });
    document.getElementById("request-delete")?.addEventListener("click", async () => {
        try {
            await ProApi.api("/api/tickets/" + id + "/delete-request", { method: "POST" });
            openTicketDetail(id);
        } catch (err) {
            window.alert(err.message || "Could not request deletion.");
        }
    });
    document.getElementById("approve-delete")?.addEventListener("click", async () => {
        try {
            await ProApi.api("/api/tickets/" + id + "/delete-approval", { method: "POST" });
            openTicketDetail(id);
        } catch (err) {
            window.alert(err.message || "Could not approve deletion.");
        }
    });
    document.getElementById("close-ticket")?.addEventListener("click", async () => {
        try {
            await ProApi.api("/api/tickets/" + id + "/status", { method: "POST", body: { status: "CLOSED" } });
            await loadTicketRows();
            openTicketDetail(id);
        } catch (err) {
            window.alert(err.message || "Could not close the ticket.");
        }
    });
    document.getElementById("reopen-form")?.addEventListener("submit", async (event) => {
        event.preventDefault();
        const reason = (document.getElementById("reopen-reason")?.value || "").trim();
        if (!reason) {
            window.alert("A reason is required to reopen this ticket.");
            return;
        }
        if (reason.length > 500) {
            window.alert("Reason must be at most 500 characters.");
            return;
        }
        try {
            await ProApi.api("/api/tickets/" + id + "/reopen", { method: "PUT", body: { reason } });
            await initTickets();
            openTicketDetail(id);
        } catch (err) {
            window.alert(err.message || "Could not reopen the ticket.");
        }
    });
    document.getElementById("withdraw-ticket")?.addEventListener("click", async () => {
        if (!window.confirm("Withdraw this ticket?")) {
            return;
        }
        try {
            await ProApi.api("/api/tickets/" + id, { method: "DELETE" });
            window.location.href = ProApi.page("tickets");
        } catch (err) {
            window.alert(err.message || "Could not delete the ticket.");
        }
    });
    document.getElementById("escalate-ticket")?.addEventListener("click", async () => {
        try {
            await ProApi.api("/api/tickets/" + id + "/escalate", { method: "PUT" });
            await initTickets();
            openTicketDetail(id);
        } catch (err) {
            window.alert(err.message || "Could not escalate the ticket.");
        }
    });
    document.getElementById("save-status")?.addEventListener("click", async () => {
        const status = document.getElementById("ticket-status")?.value;
        try {
            await ProApi.api("/api/tickets/" + id + "/status", { method: "POST", body: { status } });
            await initTickets();
            openTicketDetail(id);
        } catch (err) {
            window.alert(err.message || "Could not update the status.");
        }
    });
    document.getElementById("save-assignee")?.addEventListener("click", async () => {
        const assigneeId = Number(document.getElementById("ticket-assignee")?.value);
        try {
            await ProApi.api("/api/tickets/" + id + "/assign", { method: "POST", body: { assigneeId } });
            await initTickets();
            openTicketDetail(id);
        } catch (err) {
            window.alert(err.message || "Could not assign the ticket.");
        }
    });
    document.getElementById("save-category")?.addEventListener("click", async () => {
        const categoryId = Number(document.getElementById("ticket-category")?.value);
        try {
            await ProApi.api("/api/tickets/" + id, { method: "PUT", body: { categoryId } });
            await initTickets();
            openTicketDetail(id);
        } catch (err) {
            window.alert(err.message || "Could not update the category.");
        }
    });
}

async function loadSubmitCategories() {
    try {
        const options = await ProApi.api("/api/tickets/options");
        const fromOptions = Array.isArray(options?.categories) ? options.categories : [];
        if (fromOptions.length) {
            return canonicalCategories(fromOptions);
        }
    } catch {
        /* fall through to public category list */
    }
    const fallback = await ProApi.api("/api/categories");
    return canonicalCategories(Array.isArray(fallback) ? fallback : []);
}

async function initSubmit() {
    const form = document.getElementById("ticket-form");
    const category = document.getElementById("category");
    const banner = document.getElementById("form-banner");
    const dropzone = document.getElementById("dropzone");
    const fileInput = document.getElementById("attachments");

    document.querySelectorAll(".priority-btn").forEach((btn) => {
        btn.addEventListener("click", () => {
            document.querySelectorAll(".priority-btn").forEach((item) => item.classList.remove("selected"));
            btn.classList.add("selected");
            const input = document.getElementById("priority-value");
            if (input) {
                input.value = btn.dataset.level;
            }
        });
    });

    if (dropzone && fileInput) {
        const label = dropzone.querySelector("[data-drop-label]");
        const defaultText = label ? label.textContent : "";
        dropzone.addEventListener("click", () => fileInput.click());
        dropzone.addEventListener("keydown", (event) => {
            if (event.key === "Enter" || event.key === " ") {
                event.preventDefault();
                fileInput.click();
            }
        });
        dropzone.addEventListener("dragover", (event) => {
            event.preventDefault();
            dropzone.classList.add("dragover");
        });
        ["dragleave", "dragend"].forEach((type) => {
            dropzone.addEventListener(type, () => dropzone.classList.remove("dragover"));
        });
        dropzone.addEventListener("drop", (event) => {
            event.preventDefault();
            dropzone.classList.remove("dragover");
            const dropped = Array.from(event.dataTransfer.files || []);
            const attachErr = document.getElementById("attachment-error");
            if (dropped.length) {
                if (!validateAttachmentFiles(dropped, attachErr, banner)) {
                    fileInput.value = "";
                    if (label) {
                        label.textContent = defaultText;
                    }
                    return;
                }
                fileInput.files = event.dataTransfer.files;
                if (label) {
                    label.textContent = dropped.map((file) => file.name).join(", ");
                }
            }
        });
        fileInput.addEventListener("change", () => {
            const attachErr = document.getElementById("attachment-error");
            const selected = Array.from(fileInput.files || []);
            if (!validateAttachmentFiles(selected, attachErr, banner)) {
                fileInput.value = "";
                if (label) {
                    label.textContent = defaultText;
                }
                return;
            }
            if (label) {
                label.textContent = selected.length
                    ? selected.map((file) => file.name).join(", ")
                    : defaultText;
            }
        });
    }

    try {
        const categories = await loadSubmitCategories();
        if (category) {
            if (!categories.length) {
                category.innerHTML = `<option value="">No departments available</option>`;
                showBanner(banner, "No department categories were found. Restart the app so they can be seeded, or add them as admin.", true);
            } else {
                category.innerHTML = `<option value="">Select a department</option>` +
                    categories.map((item) => {
                        const id = item.id != null ? item.id : item.categoryId;
                        return `<option value="${id}">${ProApi.escapeHtml(categoryOptionLabel(item))}</option>`;
                    }).join("");
            }
        }
    } catch (err) {
        if (category) {
            category.innerHTML = `<option value="">Unable to load categories</option>`;
        }
        showBanner(banner, err.message || "Unable to load ticket options.", true);
    }

    category?.addEventListener("change", () => setCategoryError(false));

    const suggestionBox = document.getElementById("kb-suggestions");
    const refreshSuggestions = ProApi.debounce(async () => {
        if (!suggestionBox) {
            return;
        }
        const query = [
            document.getElementById("subject")?.value || "",
            document.getElementById("description")?.value || ""
        ].join(" ").trim();
        if (query.length < 4) {
            suggestionBox.hidden = true;
            suggestionBox.innerHTML = "";
            return;
        }
        try {
            const suggestions = await ProApi.api("/api/kb/suggest?query=" + encodeURIComponent(query));
            if (!Array.isArray(suggestions) || !suggestions.length) {
                suggestionBox.hidden = true;
                suggestionBox.innerHTML = "";
                return;
            }
            suggestionBox.hidden = false;
            suggestionBox.innerHTML = `<p>Related knowledge articles</p>` + suggestions.slice(0, 4).map((article) =>
                `<a href="${ProApi.page("knowledge", "id=" + article.id)}">${ProApi.escapeHtml(article.title || "Article")}</a>`
            ).join("");
        } catch {
            suggestionBox.hidden = true;
        }
    }, 280);
    document.getElementById("subject")?.addEventListener("input", refreshSuggestions);
    document.getElementById("description")?.addEventListener("input", refreshSuggestions);

    form?.addEventListener("submit", async (event) => {
        event.preventDefault();
        hideBanner(banner);
        setCategoryError(false);
        const title = (document.getElementById("subject")?.value || "").trim();
        const description = (document.getElementById("description")?.value || "").trim();
        const categoryId = Number(category?.value);
        const priority = ProApi.toPriority(document.getElementById("priority-value")?.value || "high");
        if (!categoryId) {
            setCategoryError(true);
            showBanner(banner, "Please select a department", true);
            return;
        }
        if (!title || !description) {
            showBanner(banner, "Title and description are required.", true);
            return;
        }
        if (title.length > 200) {
            showBanner(banner, "Title must be at most 200 characters.", true);
            return;
        }
        if (description.length > 4000) {
            showBanner(banner, "Description must be at most 4000 characters.", true);
            return;
        }
        const files = fileInput ? Array.from(fileInput.files || []) : [];
        const attachErr = document.getElementById("attachment-error");
        if (!validateAttachmentFiles(files, attachErr, banner)) {
            return;
        }
        const submitBtn = form.querySelector("[type=submit]");
        if (submitBtn) {
            submitBtn.disabled = true;
        }
        try {
            const created = await ProApi.api("/api/tickets", {
                method: "POST",
                body: { title, description, categoryId, priority }
            });
            let attachError = "";
            for (const file of files) {
                try {
                    await ProApi.uploadFile("/api/tickets/" + created.id + "/attachments", file);
                } catch (err) {
                    attachError = err.message || "Attachment upload failed.";
                }
            }
            if (attachError) {
                showBanner(banner, "Ticket " + (created.ticketNumber || "") + " created, but an attachment failed: " + attachError, true);
            } else {
                showBanner(banner, "Ticket " + (created.ticketNumber || "") + " submitted.");
            }
            window.setTimeout(() => {
                window.location.href = ProApi.page("tickets", "id=" + created.id);
            }, 800);
        } catch (err) {
            showBanner(banner, err.message || "Unable to submit this request.", true);
            if (submitBtn) {
                submitBtn.disabled = false;
            }
        }
    });
}

function initKnowledge() {
    const grid = document.getElementById("kb-grid");
    const search = document.getElementById("kb-search-input");
    const topSearch = document.querySelector(".topbar-search .search-input");
    const filters = document.getElementById("kb-filters");
    const browse = document.getElementById("kb-browse");
    const detail = document.getElementById("kb-article");
    const session = ProApi.getSession() || {};
    const canWrite = ProApi.canWriteKb(session.role);
    let activeCategory = "";
    let catalog = [];

    bindKbEditor(canWrite, () => refresh());

    function applyView() {
        renderKbFilters(filters, catalog, activeCategory, (name) => {
            activeCategory = name;
            applyView();
        });
        const visible = activeCategory
            ? catalog.filter((article) => (article.category || "Campus") === activeCategory)
            : catalog;
        renderArticles(grid, visible);
    }

    async function refresh() {
        if (grid) {
            grid.innerHTML = `<p class="empty-note">Loading articles…</p>`;
        }
        try {
            const query = (search?.value || topSearch?.value || "").trim();
            const articles = await ProApi.loadArticles(query);
            catalog = Array.isArray(articles) ? articles : [];
            applyView();
        } catch (err) {
            if (grid) {
                grid.innerHTML = `<p class="empty-note">${ProApi.escapeHtml(err.message)}</p>`;
            }
        }
    }

    async function openArticle(id) {
        if (!id) {
            return;
        }
        try {
            const article = await ProApi.viewArticle(id);
            if (browse) {
                browse.hidden = true;
            }
            if (detail) {
                detail.hidden = false;
                const tone = ProApi.categoryTone(article.category);
                const cover = ProApi.kbPhoto(article);
                detail.innerHTML = `
                    <button class="btn btn-ghost" type="button" id="kb-back">← Back to articles</button>
                    <img class="kb-cover" src="${cover}" alt="${ProApi.escapeHtml(article.category || "Campus")}" onerror="this.onerror=null;this.src='/images/kb-campus.jpg'"/>
                    <div class="kb-cat ${tone}">${ProApi.escapeHtml(article.category || "Campus")}</div>
                    <h2>${ProApi.escapeHtml(article.title || "Untitled article")}</h2>
                    <p class="kb-article-meta">${ProApi.escapeHtml(article.author || "Campus support")} · 👁 ${ProApi.escapeHtml(ProApi.formatViews(article.viewCount))} views · ${ProApi.escapeHtml(ProApi.relativeTime(article.createdAt))}</p>
                    <div class="kb-article-body">${formatArticleBody(article.content)}</div>
                    <div class="ticket-actions">
                        <a class="btn btn-primary" href="${ProApi.page("submit")}">Still stuck? Raise a ticket</a>
                        ${article.content ? `<a class="btn btn-outline" href="${ProApi.page("knowledge", "id=" + id)}" download="${ProApi.escapeHtml((article.title || "guide").replace(/\s+/g, "-"))}.txt" id="kb-download">Download guide</a>` : ""}
                        ${canWrite ? `<button class="btn btn-ghost" type="button" id="kb-edit">Edit</button>
                        <button class="btn btn-ghost" type="button" id="kb-delete">Delete</button>` : ""}
                    </div>`;
                document.getElementById("kb-back")?.addEventListener("click", () => {
                    window.history.replaceState({}, "", ProApi.page("knowledge"));
                    if (browse) {
                        browse.hidden = false;
                    }
                    detail.hidden = true;
                    refresh();
                });
                document.getElementById("kb-download")?.addEventListener("click", (event) => {
                    event.preventDefault();
                    const blob = new Blob([article.content || ""], { type: "text/plain" });
                    const url = URL.createObjectURL(blob);
                    const link = document.createElement("a");
                    link.href = url;
                    link.download = (article.title || "guide").replace(/\s+/g, "-") + ".txt";
                    link.click();
                    URL.revokeObjectURL(url);
                });
                document.getElementById("kb-edit")?.addEventListener("click", () => {
                    fillKbEditor(article);
                    window.history.replaceState({}, "", ProApi.page("knowledge"));
                    if (browse) {
                        browse.hidden = false;
                    }
                    detail.hidden = true;
                });
                document.getElementById("kb-delete")?.addEventListener("click", async () => {
                    if (!window.confirm("Remove this knowledge article?")) {
                        return;
                    }
                    await ProApi.api("/api/kb/articles/" + id, { method: "DELETE" });
                    window.history.replaceState({}, "", ProApi.page("knowledge"));
                    if (browse) {
                        browse.hidden = false;
                    }
                    detail.hidden = true;
                    refresh();
                });
            }
            history.replaceState({}, "", ProApi.page("knowledge", "id=" + id));
        } catch (err) {
            if (detail) {
                detail.hidden = false;
                detail.innerHTML = `<p class="empty-note">${ProApi.escapeHtml(err.message)}</p>`;
            }
        }
    }

    const runSearch = ProApi.debounce(() => refresh(), 280);
    search?.addEventListener("input", () => {
        if (topSearch) {
            topSearch.value = search.value;
        }
        runSearch();
    });
    topSearch?.addEventListener("input", () => {
        if (search) {
            search.value = topSearch.value;
        }
        runSearch();
    });

    const selectedId = new URLSearchParams(location.search).get("id");
    if (selectedId) {
        openArticle(selectedId);
    } else {
        refresh();
    }

    window.openKnowledgeArticle = openArticle;
}

function renderKbFilters(container, articles, active, onSelect) {
    if (!container) {
        return;
    }
    const names = [];
    articles.forEach((article) => {
        const name = article.category || "Campus";
        if (!names.includes(name)) {
            names.push(name);
        }
    });
    container.innerHTML =
        `<button class="pill-tab${active ? "" : " active"}" type="button" data-category="">All</button>` +
        names.map((name) =>
            `<button class="pill-tab${active === name ? " active" : ""}" type="button" data-category="${ProApi.escapeHtml(name)}">${ProApi.escapeHtml(name)}</button>`
        ).join("");
    container.querySelectorAll(".pill-tab").forEach((tab) => {
        tab.addEventListener("click", () => onSelect(tab.dataset.category || ""));
    });
}

function formatArticleBody(content) {
    return String(content || "")
        .trim()
        .split(/\n{2,}/)
        .map((block) => `<p>${ProApi.escapeHtml(block).replaceAll("\n", "<br>")}</p>`)
        .join("");
}

function renderArticles(grid, articles) {
    if (!grid) {
        return;
    }
    if (!articles.length) {
        grid.innerHTML = `<p class="empty-note">No knowledge articles match this search.</p>`;
        return;
    }
    grid.innerHTML = articles.map((article, index) => {
        const tone = ProApi.categoryTone(article.category);
        return `<article class="kb-card" data-id="${article.id || ""}">
            <div class="kb-card-photo">
                <img src="${ProApi.kbPhoto(article, index)}" alt="${ProApi.escapeHtml(article.category || "Campus")}" loading="lazy" onerror="this.onerror=null;this.src='/images/kb-campus.jpg'"/>
            </div>
            <div class="kb-card-body">
                <div class="kb-cat ${tone}">${ProApi.escapeHtml(article.category || "Campus")}</div>
                <h3>${ProApi.escapeHtml(article.title || "Untitled article")}</h3>
                <p>${ProApi.escapeHtml(ProApi.snippet(article.content))}</p>
                <div class="kb-meta">
                    <span>👁 ${ProApi.escapeHtml(ProApi.formatViews(article.viewCount))} views</span>
                    <span>📅 ${ProApi.escapeHtml(ProApi.relativeTime(article.createdAt))}</span>
                </div>
            </div>
        </article>`;
    }).join("");

    grid.querySelectorAll(".kb-card[data-id]").forEach((card) => {
        card.addEventListener("click", () => {
            const id = card.getAttribute("data-id");
            if (id && typeof window.openKnowledgeArticle === "function") {
                window.openKnowledgeArticle(id);
            }
        });
    });
}

function fillKbEditor(article) {
    const editor = document.getElementById("kb-editor");
    if (editor) {
        editor.hidden = false;
    }
    const titleEl = document.getElementById("kb-editor-title");
    if (titleEl) {
        titleEl.textContent = article ? "Update article" : "Publish an article";
    }
    const idInput = document.getElementById("kb-article-id");
    if (idInput) {
        idInput.value = article?.id || "";
    }
    const titleInput = document.getElementById("kb-title");
    if (titleInput) {
        titleInput.value = article?.title || "";
    }
    const contentInput = document.getElementById("kb-content");
    if (contentInput) {
        contentInput.value = article?.content || "";
    }
    if (article?.categoryId && document.getElementById("kb-category")) {
        document.getElementById("kb-category").value = article.categoryId;
    }
    const cancel = document.getElementById("kb-cancel");
    if (cancel) {
        cancel.hidden = !article;
    }
}

async function bindKbEditor(canWrite, onSaved) {
    const editor = document.getElementById("kb-editor");
    if (!editor) {
        return;
    }
    editor.hidden = !canWrite;
    if (!canWrite) {
        return;
    }
    const select = document.getElementById("kb-category");
    const banner = document.getElementById("kb-banner");
    try {
        const categories = await ProApi.api("/api/kb/categories");
        if (select) {
            select.innerHTML = (Array.isArray(categories) ? categories : []).map((item) =>
                `<option value="${item.id}">${ProApi.escapeHtml(item.name || "Category")}</option>`
            ).join("") || `<option value="">No categories</option>`;
        }
    } catch (err) {
        showBanner(banner, err.message || "Unable to load knowledge categories.", true);
    }
    document.getElementById("kb-cancel")?.addEventListener("click", () => fillKbEditor(null));
    document.getElementById("kb-form")?.addEventListener("submit", async (event) => {
        event.preventDefault();
        hideBanner(banner);
        const id = document.getElementById("kb-article-id")?.value;
        const body = {
            title: (document.getElementById("kb-title")?.value || "").trim(),
            content: (document.getElementById("kb-content")?.value || "").trim(),
            categoryId: Number(document.getElementById("kb-category")?.value) || null
        };
        if (!body.title || !body.content) {
            showBanner(banner, "Title and content are required.", true);
            return;
        }
        if (body.title.length > 200) {
            showBanner(banner, "Title must be at most 200 characters.", true);
            return;
        }
        if (body.content.length > 20000) {
            showBanner(banner, "Content must be at most 20000 characters.", true);
            return;
        }
        try {
            if (id) {
                await ProApi.api("/api/kb/articles/" + id, { method: "PUT", body });
                showBanner(banner, "Article updated.");
            } else {
                await ProApi.api("/api/kb/articles", { method: "POST", body });
                showBanner(banner, "Article published.");
            }
            fillKbEditor(null);
            onSaved();
        } catch (err) {
            showBanner(banner, err.message || "Could not save the article.", true);
        }
    });
}

function initReset() {
    const form = document.getElementById("reset-form");
    const banner = document.getElementById("reset-banner");
    const tokenInput = document.getElementById("token");
    const token = new URLSearchParams(location.search).get("token");
    if (token && tokenInput) {
        tokenInput.value = token;
    }
    form?.addEventListener("submit", async (event) => {
        event.preventDefault();
        hideBanner(banner);
        const resetToken = (tokenInput?.value || "").trim();
        const newPassword = document.getElementById("newPassword")?.value || "";
        if (!resetToken || resetToken.length > 255 || newPassword.length < 8 || newPassword.length > 72) {
            showBanner(banner, "Enter the reset token and a password between 8 and 72 characters.", true);
            return;
        }
        try {
            await ProApi.api("/api/auth/reset-password", {
                method: "POST",
                auth: false,
                body: { token: resetToken, newPassword }
            });
            showBanner(banner, "Password updated. You can sign in now.");
            window.setTimeout(() => {
                location.replace(ProApi.page("login"));
            }, 800);
        } catch (err) {
            showBanner(banner, err.message || "Could not reset the password.", true);
        }
    });
}

function activityQuery(userId, page) {
    const params = new URLSearchParams();
    if (userId) params.set("userId", userId);
    params.set("page", String(page || 0));
    const action = document.getElementById("activity-action")?.value;
    const from = document.getElementById("activity-from")?.value;
    const to = document.getElementById("activity-to")?.value;
    if (action) params.set("action", action);
    if (from) params.set("from", from);
    if (to) params.set("to", to);
    return "/api/auth/activity?" + params.toString();
}

function renderActivity(container, items, append) {
    if (!container) return;
    const html = (items || []).map((item) =>
        `<div class="activity-item"><span class="dot cyan"></span><div>
            <strong>${ProApi.escapeHtml(item.action || "Update")}</strong>
            <span>${ProApi.escapeHtml(item.entityType || "ACCOUNT")} · ${ProApi.escapeHtml(ProApi.relativeTime(item.createdAt))}</span>
        </div></div>`
    ).join("");
    if (!append) {
        container.innerHTML = html || `<div class="activity-item"><div><strong>No activity yet</strong><span>Ticket and profile changes will appear here.</span></div></div>`;
    } else if (html) {
        container.insertAdjacentHTML("beforeend", html);
    }
}

async function refreshActivity(container, userId, append) {
    const page = append ? Number(container.dataset.page || 0) + 1 : 0;
    const payload = await ProApi.api(activityQuery(userId, page)).catch(() => ({ items: [], hasMore: false }));
    const items = Array.isArray(payload) ? payload : (payload.items || []);
    renderActivity(container, items, append);
    container.dataset.page = String(page);
    container.dataset.userId = userId || "";
    const more = document.getElementById("activity-more");
    if (more) {
        more.hidden = !payload.hasMore;
    }
}

function bindActivity(container, userId) {
    const form = document.getElementById("activity-filters");
    if (form && !form.dataset.bound) {
        form.dataset.bound = "true";
        form.addEventListener("submit", async (event) => {
            event.preventDefault();
            await refreshActivity(container, userId, false);
        });
    }
    const more = document.getElementById("activity-more");
    if (more && !more.dataset.bound) {
        more.dataset.bound = "true";
        more.addEventListener("click", async () => {
            await refreshActivity(container, container.dataset.userId || userId, true);
        });
    }
}

function mountNotificationBell() {
    const topbar = document.querySelector(".topbar");
    if (!topbar || document.getElementById("notify-bell")) {
        refreshNotificationBell();
        return;
    }
    const wrap = document.createElement("div");
    wrap.className = "notify-bell";
    wrap.innerHTML = `<button class="btn btn-ghost" type="button" id="notify-bell" aria-label="Notifications">${navIcon("bell")} <span id="notify-count" hidden>0</span></button><div class="notify-menu" id="notify-menu" hidden></div>`;
    const avatar = topbar.querySelector(".avatar");
    if (avatar) {
        topbar.insertBefore(wrap, avatar);
    } else {
        topbar.appendChild(wrap);
    }
    document.getElementById("notify-bell")?.addEventListener("click", async () => {
        const menu = document.getElementById("notify-menu");
        if (!menu) return;
        menu.hidden = !menu.hidden;
        if (!menu.hidden) {
            await refreshNotificationBell(true);
        }
    });
    refreshNotificationBell();
}

async function refreshNotificationBell(open) {
    const count = document.getElementById("notify-count");
    const menu = document.getElementById("notify-menu");
    if (!count && !menu) return;
    try {
        const notifications = await ProApi.api("/api/notifications");
        const rows = Array.isArray(notifications) ? notifications : [];
        const unread = rows.filter((item) => item.read !== true).length;
        if (count) {
            count.hidden = unread === 0;
            count.textContent = String(unread);
        }
        if (menu && (open || !menu.hidden)) {
            const state = notificationState(rows);
            menu.innerHTML = state + rows.slice(0, 8).map((item) => {
                const when = item.relativeTime || ProApi.relativeTime(item.createdAt);
                return `<button class="notify-item" type="button" data-bell-read="${item.id}">
                    <strong>${ProApi.escapeHtml(item.message || "Notification")}</strong>
                    <span>${ProApi.escapeHtml(when)}</span>
                </button>`;
            }).join("");
            menu.querySelectorAll("[data-bell-read]").forEach((btn) => {
                btn.addEventListener("click", async () => {
                    await ProApi.api("/api/notifications/" + btn.dataset.bellRead + "/read", { method: "PUT" });
                    await refreshNotificationBell(true);
                });
            });
        }
    } catch {
        if (count) count.hidden = true;
    }
}

async function initProfile() {
    const banner = document.getElementById("profile-banner");
    const activity = document.getElementById("profile-activity");
    try {
        const self = await ProApi.api("/api/auth/profile");
        const viewedUserId = new URLSearchParams(location.search).get("userId");
        const viewingOther = Boolean(viewedUserId && String(viewedUserId) !== String(self.id));
        const profile = viewingOther
            ? await ProApi.api("/api/admin/users/" + viewedUserId)
            : self;
        const activityUserId = viewingOther ? viewedUserId : null;
        document.getElementById("fullName").value = profile.fullName || "";
        document.getElementById("email").value = profile.email || "";
        document.getElementById("role").value = ProApi.roleLabel(profile.role);
        document.getElementById("department").value = profile.department || "";
        const studentField = document.getElementById("student-id-field");
        if (studentField) {
            studentField.hidden = profile.role !== "STUDENT";
        }
        document.getElementById("studentId").value = profile.studentId || "";
        const adminAccount = String(profile.role || "").trim().toUpperCase() === "ADMIN"
            || String(self.role || "").trim().toUpperCase() === "ADMIN";
        const deactivate = document.getElementById("deactivate-self");
        if (deactivate) {
            deactivate.hidden = viewingOther || adminAccount;
        }
        if (viewingOther) {
            const title = document.querySelector(".page-title");
            const sub = document.querySelector(".page-sub");
            if (title) {
                title.textContent = profile.fullName || "Account";
            }
            if (sub) {
                sub.textContent = "Activity for " + (profile.fullName || "this account")
                    + " · " + ProApi.roleLabel(profile.role);
            }
            ["fullName", "department", "studentId", "newPassword"].forEach((fieldId) => {
                const field = document.getElementById(fieldId);
                if (field) {
                    field.disabled = true;
                }
            });
            const save = document.querySelector("#profile-form [type=submit]");
            if (save) {
                save.hidden = true;
            }
            const passwordField = document.getElementById("newPassword")?.closest(".field");
            if (passwordField) {
                passwordField.hidden = true;
            }
            const info = document.querySelector("#profile-form")?.closest("section")?.querySelector("h3");
            if (info) {
                info.textContent = "Account information";
            }
        }
        if (activity) {
            await refreshActivity(activity, activityUserId, false);
        }
        bindActivity(activity, activityUserId);
        document.getElementById("profile-form")?.addEventListener("submit", async (event) => {
            event.preventDefault();
            if (viewingOther) {
                return;
            }
            hideBanner(banner);
            const fullName = (document.getElementById("fullName")?.value || "").trim();
            const newPassword = document.getElementById("newPassword")?.value || "";
            if (!fullName) {
                showBanner(banner, "Name is required.", true);
                return;
            }
            if (fullName.length > 100) {
                showBanner(banner, "Full name must be at most 100 characters.", true);
                return;
            }
            const department = (document.getElementById("department")?.value || "").trim();
            const studentId = (document.getElementById("studentId")?.value || "").trim();
            if (department.length > 100) {
                showBanner(banner, "Department must be at most 100 characters.", true);
                return;
            }
            if (studentId.length > 20) {
                showBanner(banner, "Student ID must be at most 20 characters.", true);
                return;
            }
            if (newPassword && (newPassword.length < 8 || newPassword.length > 72)) {
                showBanner(banner, "New password must be between 8 and 72 characters.", true);
                return;
            }
            try {
                const updated = await ProApi.api("/api/auth/profile", {
                    method: "PUT",
                    body: {
                        fullName,
                        department,
                        studentId,
                        newPassword: newPassword || null
                    }
                });
                const session = ProApi.getSession() || {};
                session.fullName = updated.fullName || fullName;
                session.token = updated.token || session.token;
                session.userId = updated.userId || session.userId;
                ProApi.saveSession(session);
                document.getElementById("newPassword").value = "";
                showBanner(banner, "Profile saved.");
                applyUserChrome(session);
            } catch (err) {
                showBanner(banner, err.message || "Could not save the profile.", true);
            }
        });
        document.getElementById("deactivate-self")?.addEventListener("click", async () => {
            if (!window.confirm("Deactivate this account? You will be signed out.")) {
                return;
            }
            try {
                await ProApi.api("/api/auth/deactivate/" + profile.id, { method: "PUT" });
                ProApi.logout();
            } catch (err) {
                showBanner(banner, err.message || "Could not deactivate the account.", true);
            }
        });
    } catch (err) {
        showBanner(banner, err.message || "Unable to load profile.", true);
    }
}

async function initInbox() {
    const list = document.getElementById("inbox-list");
    const markAll = document.getElementById("mark-all-read");
    async function refresh() {
        try {
            const notifications = await ProApi.api("/api/notifications");
            renderInbox(list, Array.isArray(notifications) ? notifications : [], refresh);
        } catch (err) {
            if (list) {
                list.innerHTML = `<div class="activity-item"><div><strong>Could not load inbox</strong><span>${ProApi.escapeHtml(err.message)}</span></div></div>`;
            }
        }
    }
    if (markAll && !markAll.dataset.bound) {
        markAll.dataset.bound = "true";
        markAll.addEventListener("click", async () => {
            await ProApi.api("/api/notifications/read-all", { method: "POST" });
            refresh();
        });
    }
    await refresh();
}

function notificationState(notifications) {
    if (!notifications.length) {
        return `<div class="activity-item"><div><strong>No notifications yet</strong><span>Status and assignment alerts will appear here.</span></div></div>`;
    }
    const unread = notifications.some((item) => item.read !== true);
    if (!unread) {
        return `<div class="activity-item"><div><strong>All caught up</strong><span>Read notifications stay in the list below.</span></div></div>`;
    }
    return "";
}

function renderInbox(list, notifications, refresh) {
    if (!list) {
        return;
    }
    const state = notificationState(notifications);
    if (!notifications.length) {
        list.innerHTML = state;
        return;
    }
    list.innerHTML = state + notifications.map((item) => {
        const unread = item.read !== true;
        const href = item.ticketId ? ProApi.page("tickets", "id=" + item.ticketId) : ProApi.page("tickets");
        const when = item.relativeTime || ProApi.relativeTime(item.createdAt);
        return `<div class="activity-item">
            <span class="dot ${unread ? "rose" : "green"}"></span>
            <div>
                <strong>${ProApi.escapeHtml(item.message || "Notification")}</strong>
                <span>${unread ? "Unread" : "Read"} · ${ProApi.escapeHtml(when)}</span>
            </div>
            <div class="ticket-actions">
                <a class="btn btn-ghost" href="${href}">Open</a>
                ${unread ? `<button class="btn btn-ghost" type="button" data-read="${item.id}">Mark read</button>` : ""}
                <button class="btn btn-ghost" type="button" data-delete="${item.id}">Clear</button>
            </div>
        </div>`;
    }).join("");
    list.querySelectorAll("[data-read]").forEach((btn) => {
        btn.addEventListener("click", async () => {
            await ProApi.api("/api/notifications/" + btn.dataset.read + "/read", { method: "PUT" });
            refresh();
        });
    });
    list.querySelectorAll("[data-delete]").forEach((btn) => {
        btn.addEventListener("click", async () => {
            await ProApi.api("/api/notifications/" + btn.dataset.delete, { method: "DELETE" });
            refresh();
        });
    });
}

function reportFilterError() {
    const missing = [];
    if (!document.getElementById("filter-category")?.value) missing.push("category");
    if (!document.getElementById("filter-department")?.value) missing.push("department");
    const statuses = Array.from(document.getElementById("filter-status")?.selectedOptions || []).filter((option) => option.value);
    const priorities = Array.from(document.getElementById("filter-priority")?.selectedOptions || []).filter((option) => option.value);
    if (!statuses.length) missing.push("status");
    if (!priorities.length) missing.push("priority");
    if (!document.getElementById("filter-from")?.value) missing.push("from date");
    if (!document.getElementById("filter-to")?.value) missing.push("to date");
    if (!document.getElementById("filter-assigned")?.value) missing.push("assigned to");
    if (!document.getElementById("filter-created")?.value) missing.push("created by");
    if (!missing.length) {
        return "";
    }
    return "Fill in every filter before applying or downloading: " + missing.join(", ") + ".";
}

function reportFilterQuery() {
    const params = new URLSearchParams();
    const categoryId = document.getElementById("filter-category")?.value;
    const department = document.getElementById("filter-department")?.value;
    const from = document.getElementById("filter-from")?.value;
    const to = document.getElementById("filter-to")?.value;
    const assignedTo = document.getElementById("filter-assigned")?.value;
    const createdBy = document.getElementById("filter-created")?.value;
    if (categoryId) params.set("categoryId", categoryId);
    if (department) params.set("department", department);
    if (from) params.set("from", from);
    if (to) params.set("to", to);
    if (assignedTo) params.set("assignedTo", assignedTo);
    if (createdBy) params.set("createdBy", createdBy);
    Array.from(document.getElementById("filter-status")?.selectedOptions || []).forEach((option) => {
        if (option.value) params.append("status", option.value);
    });
    Array.from(document.getElementById("filter-priority")?.selectedOptions || []).forEach((option) => {
        if (option.value) params.append("priority", option.value);
    });
    const query = params.toString();
    return query ? "?" + query : "";
}

async function loadReportFilters() {
    const options = await ProApi.api("/api/reports/filter-options");
    const category = document.getElementById("filter-category");
    if (category && category.options.length <= 1) {
        (options.categories || []).forEach((item) => {
            const option = document.createElement("option");
            option.value = item.id;
            option.textContent = item.label || item.name;
            category.appendChild(option);
        });
        (options.departments || []).forEach((name) => {
            const option = document.createElement("option");
            option.value = name;
            option.textContent = name;
            document.getElementById("filter-department")?.appendChild(option);
        });
        (options.statuses || []).forEach((name) => {
            const option = document.createElement("option");
            option.value = name;
            option.textContent = String(name).replaceAll("_", " ");
            document.getElementById("filter-status")?.appendChild(option);
        });
        (options.priorities || []).forEach((name) => {
            const option = document.createElement("option");
            option.value = name;
            option.textContent = name;
            document.getElementById("filter-priority")?.appendChild(option);
        });
        (options.assignees || []).forEach((person) => {
            const assigned = document.createElement("option");
            assigned.value = person.id;
            assigned.textContent = person.fullName || "User";
            document.getElementById("filter-assigned")?.appendChild(assigned);
            const creator = document.createElement("option");
            creator.value = person.id;
            creator.textContent = person.fullName || "User";
            document.getElementById("filter-created")?.appendChild(creator);
        });
    }
    return options;
}

async function loadFilteredTickets() {
    const rows = await ProApi.api("/api/reports/tickets" + reportFilterQuery());
    const body = document.getElementById("rpt-rows");
    const count = document.getElementById("rpt-row-count");
    const list = Array.isArray(rows) ? rows : [];
    if (count) {
        count.textContent = list.length + " ticket" + (list.length === 1 ? "" : "s") + " match the filters.";
    }
    if (body) {
        body.innerHTML = list.length
            ? list.map((row) => `<tr>
                <td>${ProApi.escapeHtml(row.ticketNumber || "—")}</td>
                <td>${ProApi.escapeHtml(row.title || "")}</td>
                <td>${ProApi.escapeHtml(row.category || "—")}</td>
                <td>${ProApi.escapeHtml(ProApi.statusLabel(row.status))}</td>
                <td>${ProApi.escapeHtml(row.priority || "—")}</td>
                <td>${ProApi.escapeHtml(row.assignee || "Unassigned")}</td>
                <td>${ProApi.escapeHtml(row.requester || "—")}</td>
            </tr>`).join("")
            : `<tr><td colspan="7">No tickets match these filters.</td></tr>`;
    }
    return list;
}

async function initInsights() {
    const session = ProApi.getSession();
    if (!ProApi.canSeeInsights(session?.role)) {
        location.replace(ProApi.page("dashboard"));
        return;
    }
    const filterForm = document.getElementById("report-filters");
    if (filterForm && !filterForm.dataset.bound) {
        filterForm.dataset.bound = "true";
        filterForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            const banner = document.getElementById("report-filter-error");
            const message = reportFilterError();
            if (message) {
                showBanner(banner, message, true);
                return;
            }
            hideBanner(banner);
            try {
                await loadFilteredTickets();
            } catch (err) {
                showBanner(banner, err.message || "Could not apply the filters.", true);
            }
        });
        document.getElementById("filter-clear")?.addEventListener("click", () => {
            filterForm.reset();
            Array.from(document.getElementById("filter-status")?.options || []).forEach((option) => {
                option.selected = false;
            });
            Array.from(document.getElementById("filter-priority")?.options || []).forEach((option) => {
                option.selected = false;
            });
            hideBanner(document.getElementById("report-filter-error"));
            const body = document.getElementById("rpt-rows");
            const count = document.getElementById("rpt-row-count");
            if (count) {
                count.textContent = "Fill in every filter, then apply.";
            }
            if (body) {
                body.innerHTML = `<tr><td colspan="7">Fill in every filter, then apply.</td></tr>`;
            }
        });
    }
    try {
        await loadReportFilters();
        const count = document.getElementById("rpt-row-count");
        const body = document.getElementById("rpt-rows");
        if (count) {
            count.textContent = "Fill in every filter, then apply.";
        }
        if (body) {
            body.innerHTML = `<tr><td colspan="7">Fill in every filter, then apply.</td></tr>`;
        }
        const [summary, avg, categories, staff, overdue] = await Promise.all([
            ProApi.api("/api/reports/summary"),
            ProApi.api("/api/reports/resolution-time-avg"),
            ProApi.api("/api/reports/by-category"),
            ProApi.api("/api/reports/staff-performance"),
            ProApi.api("/api/reports/overdue")
        ]);
        setText("rpt-total", summary.totalTickets ?? "0");
        setText("rpt-open", (summary.openTickets || 0) + (summary.pendingTickets || 0));
        setText("rpt-resolved", summary.resolvedTickets ?? "0");
        setText("rpt-avg", Number(avg.averageHours || 0).toFixed(1));
        const catBody = document.getElementById("rpt-categories");
        if (catBody) {
            catBody.innerHTML = (categories || []).length
                ? categories.map((row) => `<tr><td>${ProApi.escapeHtml(row.category || "—")}</td><td>${row.ticketCount || 0}</td></tr>`).join("")
                : `<tr><td colspan="2">No category data yet.</td></tr>`;
        }
        const staffBody = document.getElementById("rpt-staff");
        if (staffBody) {
            staffBody.innerHTML = (staff || []).length
                ? staff.map((row) => `<tr><td>${ProApi.escapeHtml(row.staffName || "Staff")}</td><td>${row.ticketsResolved || 0}</td></tr>`).join("")
                : `<tr><td colspan="2">No technician stats yet.</td></tr>`;
        }
        const overdueBody = document.getElementById("rpt-overdue");
        if (overdueBody) {
            overdueBody.innerHTML = (overdue || []).length
                ? overdue.map((row) => `<tr>
                    <td><a href="${ProApi.page("tickets", "id=" + row.id)}">${ProApi.escapeHtml(row.ticketNumber || "—")}</a></td>
                    <td>${ProApi.escapeHtml(row.category || "—")}</td>
                    <td>${Number(row.hoursOverdue || 0).toFixed(1)}</td>
                    <td>${ProApi.escapeHtml(ProApi.statusLabel(row.status))}</td>
                </tr>`).join("")
                : `<tr><td colspan="4">No SLA breaches right now.</td></tr>`;
        }
        if (session.role === "ADMIN") {
            const auditCard = document.getElementById("audit-card");
            if (auditCard) {
                auditCard.hidden = false;
            }
            const logs = await ProApi.api("/api/reports/audit-logs");
            const auditBody = document.getElementById("rpt-audit");
            if (auditBody) {
                auditBody.innerHTML = (logs || []).length
                    ? logs.slice(0, 30).map((item) => `<tr>
                        <td>${ProApi.escapeHtml(ProApi.relativeTime(item.createdAt))}</td>
                        <td>${ProApi.escapeHtml(item.actor || "System")}</td>
                        <td>${ProApi.escapeHtml(item.action || "")}</td>
                        <td><button class="btn btn-ghost" type="button" data-audit-delete="${item.id}">Archive</button></td>
                    </tr>`).join("")
                    : `<tr><td colspan="4">No audit entries.</td></tr>`;
                auditBody.querySelectorAll("[data-audit-delete]").forEach((btn) => {
                    btn.addEventListener("click", async () => {
                        if (!window.confirm("Archive this audit entry?")) {
                            return;
                        }
                        await ProApi.api("/api/reports/audit-logs/" + btn.dataset.auditDelete, { method: "DELETE" });
                        initInsights();
                    });
                });
            }
        }
        document.querySelectorAll("[data-export]").forEach((btn) => {
            if (btn.dataset.bound) {
                return;
            }
            btn.dataset.bound = "true";
            btn.addEventListener("click", async () => {
                const banner = document.getElementById("report-filter-error");
                const message = reportFilterError();
                if (message) {
                    showBanner(banner, message, true);
                    return;
                }
                hideBanner(banner);
                const format = btn.dataset.export;
                const filters = reportFilterQuery();
                const path = "/api/reports/export" + (filters || "?") + (filters ? "&" : "") + "format=" + encodeURIComponent(format);
                try {
                    await ProApi.download(path, format === "pdf" ? "report-summary.pdf" : "");
                } catch (err) {
                    showBanner(banner, err.message || "Could not download the report.", true);
                }
            });
        });
    } catch (err) {
        const message = err.message || "Unable to load reports.";
        setText("rpt-total", "—");
        setText("rpt-open", "—");
        setText("rpt-resolved", "—");
        setText("rpt-avg", "—");
        const catBody = document.getElementById("rpt-categories");
        const staffBody = document.getElementById("rpt-staff");
        const overdueBody = document.getElementById("rpt-overdue");
        const auditBody = document.getElementById("rpt-audit");
        if (catBody) {
            catBody.innerHTML = `<tr><td colspan="2">${ProApi.escapeHtml(message)}</td></tr>`;
        }
        if (staffBody) {
            staffBody.innerHTML = `<tr><td colspan="2">${ProApi.escapeHtml(message)}</td></tr>`;
        }
        if (overdueBody) {
            overdueBody.innerHTML = `<tr><td colspan="4">${ProApi.escapeHtml(message)}</td></tr>`;
        }
        if (auditBody) {
            auditBody.innerHTML = `<tr><td colspan="4">${ProApi.escapeHtml(message)}</td></tr>`;
        }
        window.alert(message);
    }
}

async function initDepartments() {
    const session = ProApi.getSession();
    if (session?.role !== "ADMIN") {
        location.replace(ProApi.page("dashboard"));
        return;
    }
    const banner = document.getElementById("category-banner");
    const body = document.getElementById("category-rows");
    const form = document.getElementById("category-form");

    async function refresh() {
        const rows = await ProApi.api("/api/categories");
        if (!body) {
            return;
        }
        if (!rows.length) {
            body.innerHTML = `<tr><td colspan="4">No categories yet.</td></tr>`;
            return;
        }
        body.innerHTML = rows.map((item) => `<tr>
            <td>${ProApi.escapeHtml(item.name || "—")}</td>
            <td>${ProApi.escapeHtml(item.department || "—")}</td>
            <td>${item.slaHours || 0}h</td>
            <td>
                <button class="btn btn-ghost" type="button" data-edit="${item.id}">Edit</button>
                <button class="btn btn-ghost" type="button" data-delete="${item.id}">Delete</button>
            </td>
        </tr>`).join("");
        body.querySelectorAll("[data-edit]").forEach((btn) => {
            btn.addEventListener("click", () => {
                const item = rows.find((row) => String(row.id) === btn.dataset.edit);
                if (!item) {
                    return;
                }
                document.getElementById("category-form-title").textContent = "Update category";
                document.getElementById("category-id").value = item.id;
                document.getElementById("category-name").value = item.name || "";
                document.getElementById("category-department").value = item.department || "";
                document.getElementById("category-sla").value = item.slaHours || 48;
                document.getElementById("category-cancel").hidden = false;
            });
        });
        body.querySelectorAll("[data-delete]").forEach((btn) => {
            btn.addEventListener("click", async () => {
                if (!window.confirm("Remove this category?")) {
                    return;
                }
                try {
                    await ProApi.api("/api/categories/" + btn.dataset.delete, { method: "DELETE" });
                    showBanner(banner, "Category removed.");
                    await refresh();
                } catch (err) {
                    showBanner(banner, err.message || "Could not delete the category.", true);
                }
            });
        });
    }

    document.getElementById("category-cancel")?.addEventListener("click", () => {
        form.reset();
        document.getElementById("category-id").value = "";
        document.getElementById("category-form-title").textContent = "Add a category";
        document.getElementById("category-cancel").hidden = true;
    });

    form?.addEventListener("submit", async (event) => {
        event.preventDefault();
        hideBanner(banner);
        const id = document.getElementById("category-id")?.value;
        const payload = {
            name: (document.getElementById("category-name")?.value || "").trim(),
            department: (document.getElementById("category-department")?.value || "").trim(),
            slaHours: Number(document.getElementById("category-sla")?.value || 48)
        };
        if (!payload.name || !payload.department) {
            showBanner(banner, "Name and department are required.", true);
            return;
        }
        if (!payload.slaHours || payload.slaHours < 1 || payload.slaHours > 720) {
            showBanner(banner, "SLA hours must be between 1 and 720.", true);
            return;
        }
        if (payload.name.length > 100 || payload.department.length > 100) {
            showBanner(banner, "Name and department must be at most 100 characters.", true);
            return;
        }
        if (!TICKET_CATEGORIES.includes(payload.name)) {
            showBanner(banner, "Choose one of the standard ticket categories.", true);
            return;
        }
        try {
            if (id) {
                await ProApi.api("/api/categories/" + id, { method: "PUT", body: payload });
                showBanner(banner, "Category updated.");
            } else {
                await ProApi.api("/api/categories", { method: "POST", body: payload });
                showBanner(banner, "Category created.");
            }
            form.reset();
            document.getElementById("category-id").value = "";
            document.getElementById("category-form-title").textContent = "Add a category";
            document.getElementById("category-cancel").hidden = true;
            await refresh();
        } catch (err) {
            showBanner(banner, err.message || "Could not save the category.", true);
        }
    });

    try {
        await refresh();
    } catch (err) {
        if (body) {
            body.innerHTML = `<tr><td colspan="4">${ProApi.escapeHtml(err.message)}</td></tr>`;
        }
    }
}
