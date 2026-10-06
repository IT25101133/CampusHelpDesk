const ProApi = (() => {
    const PAGES = {
        login: "/welcome",
        dashboard: "/workspace",
        tickets: "/ledger",
        submit: "/request",
        knowledge: "/kb",
        register: "/register",
        accounts: "/accounts",
        profile: "/settings",
        inbox: "/inbox",
        insights: "/insights",
        departments: "/departments",
        reset: "/reset"
    };
    const SESSION_KEY = "chd.session";
    const DEMO_ACCOUNTS = {
        student: { email: "student@sliit.lk", password: "password" },
        lecturer: { email: "lecturer@sliit.lk", password: "password" },
        staff: { email: "staff@sliit.lk", password: "password" },
        admin: { email: "admin@sliit.lk", password: "password" },
        "dept-head": { email: "head@sliit.lk", password: "password" }
    };
    const ROLE_LABELS = {
        STUDENT: "Undergraduate Student",
        LECTURER: "Lecturer",
        STAFF: "Staff",
        ADMIN: "Administrator",
        DEPT_HEAD: "Dept Head"
    };
    const PRIORITY_MAP = {
        low: "LOW",
        med: "MEDIUM",
        high: "HIGH",
        crit: "CRITICAL"
    };
    const KB_PHOTOS = {
        it: "/images/kb-it.jpg",
        finance: "/images/kb-finance.jpg",
        hostel: "/images/kb-hostel.jpg",
        academic: "/images/kb-academic.jpg",
        library: "/images/kb-library.jpg",
        campus: "/images/kb-campus.jpg"
    };

    function page(name, query) {
        const base = PAGES[name] || PAGES.login;
        if (!query) {
            return base;
        }
        if (query.charAt(0) === "?" || query.charAt(0) === "#") {
            return base + query;
        }
        return base + "?" + query;
    }

    function getSession() {
        try {
            const raw = localStorage.getItem(SESSION_KEY);
            return raw ? JSON.parse(raw) : null;
        } catch {
            return null;
        }
    }

    function saveSession(user) {
        localStorage.setItem(SESSION_KEY, JSON.stringify(user));
        return user;
    }

    function clearSession() {
        localStorage.removeItem(SESSION_KEY);
    }

    function hasToken() {
        const session = getSession();
        return Boolean(session && session.token);
    }

    function isLoginPage() {
        return /\/welcome\/?$|^\/$|login\.html$|register\.html$|\/register\/?$|reset\.html$|\/reset\/?$/i.test(location.pathname)
            || document.body.classList.contains("login-page");
    }

    async function logout() {
        try {
            if (hasToken()) {
                await api("/api/auth/logout", { method: "POST" });
            }
        } catch {
            /* Leave the page even if the logout note could not be saved. */
        }
        clearSession();
        if (!isLoginPage()) {
            location.replace(page("login"));
        }
    }

    function requireAuth() {
        if (!hasToken()) {
            location.replace(page("login"));
            return null;
        }
        return getSession();
    }

    function redirectIfAuthed() {
        if (hasToken()) {
            location.replace(page("dashboard"));
            return true;
        }
        return false;
    }

    function parseBody(text) {
        if (!text) {
            return null;
        }
        try {
            return JSON.parse(text);
        } catch {
            return { error: text.slice(0, 180) };
        }
    }

    function errorMessage(data, fallback) {
        if (!data) {
            return fallback;
        }
        if (data.fields && typeof data.fields === "object") {
            const parts = Object.entries(data.fields)
                .filter(([, message]) => message)
                .map(([field, message]) => field + ": " + message);
            if (parts.length) {
                return parts.join(" ");
            }
        }
        if (typeof data.error === "string" && data.error.trim()) {
            return data.error;
        }
        if (typeof data.message === "string" && data.message.trim()) {
            return data.message;
        }
        return fallback;
    }

    async function api(path, options = {}) {
        const headers = {};
        if (!options.multipart) {
            headers["Content-Type"] = "application/json";
        }
        const session = getSession();
        if (options.auth !== false && session && session.token) {
            headers.Authorization = "Bearer " + session.token;
        }
        const response = await fetch(path, {
            method: options.method || "GET",
            headers,
            body: options.multipart
                ? options.body
                : (options.body ? JSON.stringify(options.body) : undefined)
        });
        const text = await response.text();
        const data = parseBody(text);
        if (!response.ok) {
            if (response.status === 401 && options.auth !== false) {
                logout();
            }
            throw new Error(errorMessage(data, "Request failed"));
        }
        return data;
    }

    async function uploadFile(path, file) {
        const body = new FormData();
        body.append("file", file);
        return api(path, { method: "POST", multipart: true, body });
    }

    async function download(path, filename) {
        const headers = {};
        const session = getSession();
        if (session && session.token) {
            headers.Authorization = "Bearer " + session.token;
        }
        const response = await fetch(path, { headers });
        const textProbe = response.headers.get("content-type") || "";
        if (!response.ok) {
            const text = await response.text();
            const data = parseBody(text);
            if (response.status === 401) {
                logout();
            }
            throw new Error(errorMessage(data, "Download failed"));
        }
        const blob = textProbe.includes("json") ? new Blob([await response.text()]) : await response.blob();
        const url = URL.createObjectURL(blob);
        const link = document.createElement("a");
        link.href = url;
        const headerName = filenameFromDisposition(response.headers.get("content-disposition"));
        link.download = headerName || filename || "download";
        document.body.appendChild(link);
        link.click();
        link.remove();
        URL.revokeObjectURL(url);
    }

    function filenameFromDisposition(header) {
        if (!header) {
            return "";
        }
        const quoted = /filename="([^"]+)"/i.exec(header);
        if (quoted) {
            return quoted[1];
        }
        const plain = /filename=([^;]+)/i.exec(header);
        return plain ? plain[1].trim() : "";
    }

    function canWriteKb(role) {
        return role === "STAFF" || role === "ADMIN";
    }

    function canSeeInsights(role) {
        return role === "ADMIN" || role === "DEPT_HEAD";
    }

    function escapeHtml(value) {
        return String(value == null ? "" : value)
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll('"', "&quot;");
    }

    function initials(name) {
        const parts = String(name || "")
            .trim()
            .split(/\s+/)
            .filter(Boolean);
        if (!parts.length) {
            return "SP";
        }
        if (parts.length === 1) {
            return parts[0].slice(0, 2).toUpperCase();
        }
        return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
    }

    function firstName(name) {
        const parts = String(name || "").trim().split(/\s+/).filter(Boolean);
        return parts[0] || "there";
    }

    function roleLabel(role) {
        return ROLE_LABELS[role] || String(role || "").replaceAll("_", " ");
    }

    function parseDate(value) {
        if (!value) {
            return null;
        }
        if (Array.isArray(value)) {
            const [year, month, day, hour = 0, minute = 0, second = 0] = value;
            return new Date(year, month - 1, day, hour, minute, Math.floor(second));
        }
        const date = new Date(value);
        return Number.isNaN(date.getTime()) ? null : date;
    }

    function relativeTime(value) {
        const date = parseDate(value);
        if (!date) {
            return "—";
        }
        const diff = Date.now() - date.getTime();
        const minute = 60 * 1000;
        const hour = 60 * minute;
        const day = 24 * hour;
        if (diff < minute) {
            return "just now";
        }
        if (diff < hour) {
            return Math.max(1, Math.round(diff / minute)) + "m ago";
        }
        if (diff < day) {
            return Math.max(1, Math.round(diff / hour)) + "h ago";
        }
        if (diff < 7 * day) {
            return Math.max(1, Math.round(diff / day)) + "d ago";
        }
        if (diff < 30 * day) {
            return Math.max(1, Math.round(diff / (7 * day))) + "w ago";
        }
        return date.toLocaleDateString();
    }

    function debounce(fn, wait) {
        let timer;
        return (...args) => {
            clearTimeout(timer);
            timer = setTimeout(() => fn(...args), wait);
        };
    }

    function statusFilter(status) {
        if (status === "OPEN") {
            return "open";
        }
        if (status === "IN_PROGRESS") {
            return "progress";
        }
        if (status === "RESOLVED" || status === "CLOSED") {
            return "resolved";
        }
        return "open";
    }

    function statusBadgeClass(status) {
        return statusFilter(status);
    }

    function statusLabel(status) {
        if (status === "IN_PROGRESS") {
            return "progress";
        }
        if (status === "CLOSED") {
            return "resolved";
        }
        return String(status || "").toLowerCase().replaceAll("_", " ");
    }

    function priorityKey(priority) {
        return String(priority || "medium").toLowerCase();
    }

    function categoryTone(name) {
        const text = String(name || "").toLowerCase();
        if (text.includes("hostel")) {
            return "hostel";
        }
        if (text.includes("library")) {
            return "library";
        }
        if (text.includes("finance") || text.includes("fee") || text.includes("payment")) {
            return "finance";
        }
        if (text.includes("academic") || text.includes("lms") || text.includes("moodle")
            || text.includes("exam") || text.includes("record") || text.includes("transcript")) {
            return "academic";
        }
        if (text.includes("account") || text.includes("wifi") || text.includes("wi-fi")
            || text.includes("vpn") || text.includes("password") || text.includes("infrastructure")) {
            return "it";
        }
        if (text.includes("campus") || text.includes("facilit") || text.includes("classroom") || text.includes("estate")) {
            return "campus";
        }
        return "campus";
    }

    function kbPhoto(article, index) {
        const tone = categoryTone(article && article.category);
        return KB_PHOTOS[tone] || KB_PHOTOS.campus || KB_PHOTOS.it;
    }

    function snippet(text, max = 140) {
        const clean = String(text || "").replace(/\s+/g, " ").trim();
        if (clean.length <= max) {
            return clean || "No summary available for this article.";
        }
        return clean.slice(0, max).replace(/\s+\S*$/, "") + "…";
    }

    function formatViews(count) {
        const value = Number(count) || 0;
        if (value >= 1000) {
            const compact = value / 1000;
            return (Number.isInteger(compact) ? compact : compact.toFixed(1).replace(/\.0$/, "")) + "k";
        }
        return String(value);
    }

    function toPriority(level) {
        return PRIORITY_MAP[level] || String(level || "HIGH").toUpperCase();
    }

    function activityDot(ticket) {
        if (ticket.priority === "CRITICAL" && ticket.status !== "RESOLVED" && ticket.status !== "CLOSED") {
            return "rose";
        }
        if (ticket.status === "IN_PROGRESS") {
            return "amber";
        }
        if (ticket.status === "RESOLVED" || ticket.status === "CLOSED") {
            return "green";
        }
        return "cyan";
    }

    async function loadArticles(search, category) {
        const params = new URLSearchParams();
        if (search) {
            params.set("search", search);
        }
        if (category) {
            params.set("category", category);
        }
        const query = params.toString();
        try {
            return await api("/api/kb/articles" + (query ? "?" + query : ""));
        } catch {
            const fallback = new URLSearchParams();
            if (search) {
                fallback.set("q", search);
            }
            const extra = fallback.toString();
            const rows = await api("/api/kb" + (extra ? "?" + extra : ""));
            if (!category || !Array.isArray(rows)) {
                return rows;
            }
            const needle = String(category).toLowerCase();
            return rows.filter((article) => String(article.category || "").toLowerCase().includes(needle));
        }
    }

    async function viewArticle(id) {
        try {
            return await api("/api/kb/articles/" + id + "/view");
        } catch {
            return await api("/api/kb/" + id);
        }
    }

    return {
        SESSION_KEY,
        DEMO_ACCOUNTS,
        getSession,
        saveSession,
        clearSession,
        hasToken,
        logout,
        requireAuth,
        redirectIfAuthed,
        api,
        uploadFile,
        download,
        canWriteKb,
        canSeeInsights,
        escapeHtml,
        initials,
        firstName,
        roleLabel,
        relativeTime,
        debounce,
        statusFilter,
        statusBadgeClass,
        statusLabel,
        priorityKey,
        categoryTone,
        kbPhoto,
        snippet,
        formatViews,
        toPriority,
        activityDot,
        loadArticles,
        viewArticle,
        page
    };
})();
