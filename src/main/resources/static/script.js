"use strict";

const API = new URL("/api", window.location.origin).pathname;
const PAGE_SIZE = 20;
const state = {
    token: sessionStorage.getItem("pulseToken"),
    role: null,
    adminView: true,
    email: null,
    page: 0,
    category: "",
    view: "home",
    videos: [],
    featured: [],
    featuredIndex: 0,
    watchlist: new Set(),
    selectedVideo: null,
    adminSection: "users",
    adminPage: 0,
    adminTotal: 0,
    adminRecords: [],
    adminListPosterUrls: [],
    editingVideoId: null,
    toastTimer: null
};

const $ = (selector) => document.querySelector(selector);
const authView = $("#auth-view");
const appView = $("#app-view");
const authForms = [$("#login-form"), $("#signup-form"), $("#email-action-form"), $("#reset-form")];
let authMode = "login";
let adminLoginMode = false;
let resetToken = new URLSearchParams(location.search).get("resetToken")
    || (location.pathname === "/reset-password" ? new URLSearchParams(location.search).get("token") : null);
let featuredTimer = null;
let adminPreviewUrl = null;
let adminPosterUrl = null;

function showToast(message, isError = false) {
    const toast = $("#toast");
    toast.textContent = message;
    toast.classList.toggle("error", isError);
    toast.classList.add("show");
    clearTimeout(state.toastTimer);
    state.toastTimer = setTimeout(() => toast.classList.remove("show"), 4200);
}

function togglePlayerMute() {
    const player = $("#player");
    if (player.muted || player.volume === 0) {
        player.muted = false;
        if (player.volume === 0) player.volume = 0.5;
        $("#player-volume").value = String(player.volume);
    } else {
        player.muted = true;
    }
}

async function api(path, options = {}) {
    const headers = new Headers(options.headers || {});
    if (state.token) headers.set("Authorization", `Bearer ${state.token}`);
    if (options.body && !(options.body instanceof FormData)) headers.set("Content-Type", "application/json");
    const response = await fetch(`${API}${path}`, { ...options, headers });
    if (response.status === 401 && state.token) {
        signOut(false);
        showToast("Your session ended. Please sign in again.", true);
    }
    if (response.status === 204) return null;
    const contentType = response.headers.get("content-type") || "";
    const result = contentType.includes("application/json") ? await response.json() : await response.text();
    if (!response.ok) throw new Error(readError(result, response.status));
    return result;
}

function readError(result, status) {
    if (typeof result === "string" && result.trim()) return result;
    if (result && typeof result === "object") {
        if (typeof result.message === "string") return result.message;
        if (typeof result.error === "string") return result.error;
        if (result.errors && typeof result.errors === "object") return Object.values(result.errors).join(" ");
    }
    return `Request failed (${status}). Please try again.`;
}

function setAuthMode(mode) {
    authMode = mode;
    authForms.forEach((form) => form.classList.add("hidden"));
    $("#auth-footer").classList.remove("hidden");
    $("#admin-login-note").classList.toggle("hidden", !adminLoginMode || mode !== "login");
    $("#admin-login-toggle").classList.toggle("hidden", mode !== "login");
    $("#unverified-warning").classList.add("hidden");
    $("#back-to-login").classList.toggle("hidden", mode === "login" || mode === "signup");
    $("#auth-switch").classList.toggle("hidden", mode !== "login" && mode !== "signup");
    $("#auth-switch-copy").classList.toggle("hidden", mode !== "login" && mode !== "signup");
    if (mode === "login") {
        $("#login-form").classList.remove("hidden");
        $("#auth-title").textContent = adminLoginMode ? "Administrator sign in" : "Welcome back";
        $("#auth-subtitle").textContent = adminLoginMode
            ? "Sign in with your configured administrator account."
            : "Sign in to continue watching.";
        $("#auth-switch-copy").textContent = "New to PrimeFlix?";
        $("#auth-switch").textContent = "Create an account";
    } else if (mode === "signup") {
        $("#signup-form").classList.remove("hidden");
        $("#auth-title").textContent = "Join PrimeFlix";
        $("#auth-subtitle").textContent = "Create an account and find your next favorite.";
        $("#auth-switch-copy").textContent = "Already have an account?";
        $("#auth-switch").textContent = "Sign in";
    } else if (mode === "email") {
        $("#email-action-form").classList.remove("hidden");
        $("#auth-title").textContent = "Email help";
        $("#auth-subtitle").textContent = "Choose an email action below.";
        $("#email-action-submit").textContent = "Send password reset link";
    } else if (mode === "reset") {
        $("#reset-form").classList.remove("hidden");
        $("#auth-title").textContent = "Choose a new password";
        $("#auth-subtitle").textContent = "Use at least 8 characters.";
        $("#auth-footer").classList.add("hidden");
    }
}

function showAuth() {
    clearInterval(featuredTimer);
    authView.classList.remove("hidden");
    appView.classList.add("hidden");
    $("#landing-view").classList.add("hidden");
    if (resetToken) setAuthMode("reset");
    else setAuthMode("login");
}

function showLanding() {
    clearInterval(featuredTimer);
    $("#landing-view").classList.remove("hidden");
    authView.classList.add("hidden");
    appView.classList.add("hidden");
}

function joinFromLanding() {
    const email = $("#landing-email").value.trim();
    showAuth();
    setAuthMode("signup");
    $("#signup-form [name=email]").value = email;
    $("#signup-form [name=fullName]").focus();
}

async function loadLandingPreview() {
    const section = $("#landing-preview");
    const grid = $("#landing-preview-grid");
    try {
        const page = await api("/videos?page=0&size=5");
        const videos = (page.content || []).slice(0, 5);
        if (!videos.length) return;
        grid.replaceChildren();
        videos.forEach((video) => {
            const card = document.createElement("button");
            card.type = "button";
            card.className = "landing-title-card landing-preview-card";
            card.setAttribute("aria-label", `Join PrimeFlix to watch ${video.title}`);
            const poster = document.createElement("span");
            poster.className = "landing-title-poster";
            if (video.poster) {
                const image = document.createElement("img");
                image.src = video.poster;
                image.alt = "";
                image.loading = "lazy";
                image.onerror = () => image.remove();
                poster.append(image);
            }
            const play = document.createElement("span");
            play.setAttribute("aria-hidden", "true");
            play.textContent = "▶";
            poster.append(play);
            const info = document.createElement("span");
            info.className = "landing-title-info";
            const title = document.createElement("strong");
            title.textContent = video.title;
            const meta = document.createElement("small");
            meta.textContent = [video.year, (video.categories || [])[0]].filter(Boolean).join(" · ") || "Available on PrimeFlix";
            info.append(title, meta);
            card.append(poster, info);
            card.addEventListener("click", joinFromLanding);
            grid.append(card);
        });
        section.classList.remove("hidden");
    } catch {
        section.classList.add("hidden");
    }
}

function decodeToken(token) {
    try {
        const part = token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/");
        return JSON.parse(decodeURIComponent(atob(part).split("").map((character) =>
            `%${character.charCodeAt(0).toString(16).padStart(2, "0")}`).join("")));
    } catch {
        return null;
    }
}

async function activateApp() {
    const claims = decodeToken(state.token);
    if (!claims || !claims.sub || !claims.role) {
        signOut(false);
        showAuth();
        return;
    }
    state.role = claims.role;
    state.email = claims.sub;
    state.adminView = true;
    authView.classList.add("hidden");
    appView.classList.remove("hidden");
    $("#user-label").textContent = state.email;
    $("#admin-open").classList.toggle("hidden", state.role !== "ADMIN");
    $("#admin-users-open").classList.toggle("hidden", state.role !== "ADMIN");
    $("#admin-view-toggle").classList.toggle("hidden", state.role !== "ADMIN");
    $("#admin-view-toggle").textContent = state.adminView ? "Switch to User View" : "Switch to Admin View";
    try {
        const watchlist = await api("/watchlist");
        state.watchlist = new Set(watchlist.map((video) => video.id));
    } catch (error) {
        showToast(error.message, true);
    }
    loadCatalog();
}

async function checkBackend() {
    const status = $("#backend-status");
    try {
        await api("/videos?page=0&size=1");
        status.textContent = "Backend connected";
        status.className = "backend-status online";
        return true;
    } catch {
        status.textContent = "Backend offline — run the Spring Boot app, then refresh this page.";
        status.className = "backend-status offline";
        return false;
    }
}

function signOut(showMessage = true) {
    sessionStorage.removeItem("pulseToken");
    state.token = null;
    state.role = null;
    state.watchlist.clear();
    showAuth();
    if (showMessage) showToast("You have signed out.");
}

function cleanQueryString() {
    history.replaceState({}, "", "/");
}

async function handleEmailLinks() {
    const params = new URLSearchParams(location.search);
    const verificationToken = params.get("verifyToken")
        || (location.pathname === "/verify-email" ? params.get("token") : null);
    if (verificationToken) {
        try {
            const result = await api(`/auth/verify-email?token=${encodeURIComponent(verificationToken)}`, { method: "POST" });
            showToast(result.message || "Email verified. You can sign in.");
            cleanQueryString();
        } catch (error) {
            showToast(error.message, true);
        }
    }
    if (resetToken) setAuthMode("reset");
}

async function loadCatalog() {
    const isWatchlist = state.view === "watchlist";
    $("#catalog-kicker").textContent = isWatchlist ? "YOUR SAVED TITLES" : "CURATED FOR YOU";
    $("#catalog-title").textContent = isWatchlist ? "My List" : "Explore titles";
    $("#hero").classList.toggle("hidden", isWatchlist || state.page > 0);
    $("#category-filters").classList.toggle("hidden", isWatchlist);
    $("#pagination").classList.toggle("hidden", isWatchlist);
    $("#video-grid").innerHTML = '<div class="loading">Finding something good…</div>';
    $("#empty-state").classList.add("hidden");
    try {
        let videos;
        let totalElements;
        if (isWatchlist) {
            videos = await api("/watchlist");
            state.watchlist = new Set(videos.map((video) => video.id));
            totalElements = videos.length;
        } else {
            const query = new URLSearchParams({ page: String(state.page), size: String(PAGE_SIZE) });
            if (state.category) query.set("category", state.category);
            const search = $("#search-input").value.trim();
            if (search) query.set("search", search);
            const page = await api(`/videos?${query}`);
            videos = page.content || [];
            totalElements = page.totalElements || 0;
        }
        state.videos = videos;
        renderVideos(videos, isWatchlist);
        updatePagination(totalElements);
        renderCategories(videos);
        updateFeatured(videos);
    } catch (error) {
        $("#video-grid").innerHTML = "";
        $("#empty-state").classList.remove("hidden");
        $("#empty-title").textContent = "Could not load titles";
        $("#empty-copy").textContent = "Check that the backend is running and reachable.";
        showToast(error.message, true);
    }
}

function updateFeatured(videos) {
    state.featured = state.view === "home" && state.page === 0 ? videos.slice(0, 5) : [];
    if (state.featuredIndex >= state.featured.length) state.featuredIndex = 0;
    const featured = state.featured[state.featuredIndex];
    const title = $("#hero-title");
    const description = $("#hero-copy");
    const playButton = $("#hero-play");
    const meta = $("#hero-meta");
    clearInterval(featuredTimer);
    if (!featured || state.view !== "home" || state.page !== 0) {
        title.innerHTML = "Stories that<br>stay with you.";
        description.textContent = "Find your next great watch in a library made for movie nights.";
        meta.textContent = "";
        $("#hero-index-current").textContent = "01";
        $("#hero-prev").classList.add("hidden");
        $("#hero-next").classList.add("hidden");
        playButton.classList.add("hidden");
        $("#hero").style.backgroundImage = "";
        return;
    }
    title.textContent = featured.title;
    description.textContent = featured.description || "Discover a story worth staying up for.";
    meta.textContent = [featured.year, featured.rating, featured.duration ? `${featured.duration} min` : null]
        .filter(Boolean).join("  ·  ");
    $("#hero-index-current").textContent = String(state.featuredIndex + 1).padStart(2, "0");
    const showControls = state.featured.length > 1;
    $("#hero-prev").classList.toggle("hidden", !showControls);
    $("#hero-next").classList.toggle("hidden", !showControls);
    playButton.classList.remove("hidden");
    $("#hero").style.backgroundImage = featured.poster
        ? `linear-gradient(90deg, #171714 4%, rgba(23,23,20,.96) 38%, rgba(23,23,20,.62) 68%, #101010 100%), url("${featured.poster}")`
        : "";
    $("#hero").style.backgroundSize = "cover";
    $("#hero").style.backgroundPosition = "center, right 20% center";
    playButton.onclick = () => openPlayer(featured);
    if (showControls && !window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
        featuredTimer = setInterval(() => {
            state.featuredIndex = (state.featuredIndex + 1) % state.featured.length;
            updateFeatured(state.videos);
        }, 7000);
    }
}

function moveFeatured(direction) {
    if (state.featured.length < 2) return;
    state.featuredIndex = (state.featuredIndex + direction + state.featured.length) % state.featured.length;
    updateFeatured(state.videos);
}

function updatePagination(totalElements) {
    const totalPages = Math.max(1, Math.ceil(totalElements / PAGE_SIZE));
    $("#page-label").textContent = `Page ${state.page + 1} of ${totalPages}`;
    $("#prev-page").disabled = state.page <= 0;
    $("#next-page").disabled = state.page + 1 >= totalPages;
}

function renderCategories(videos) {
    const allCategories = [...new Set(videos.flatMap((video) => video.categories || []))].sort();
    const filters = $("#category-filters");
    filters.replaceChildren();
    if (state.view === "watchlist") return;
    [["", "All titles"], ...allCategories.map((category) => [category, category])].forEach(([value, label]) => {
        const button = document.createElement("button");
        button.className = `filter-chip${state.category === value ? " active" : ""}`;
        button.type = "button";
        button.textContent = label;
        button.addEventListener("click", () => {
            state.category = value;
            state.page = 0;
            loadCatalog();
        });
        filters.append(button);
    });
}

function renderVideos(videos, isWatchlist) {
    const grid = $("#video-grid");
    const search = $("#search-input").value.trim().toLocaleLowerCase();
    const visible = !isWatchlist ? videos : videos.filter((video) => !search
        || `${video.title} ${video.description || ""} ${(video.categories || []).join(" ")}`.toLocaleLowerCase().includes(search));
    grid.replaceChildren();
    visible.forEach((video) => grid.append(makeVideoCard(video)));
    $("#empty-state").classList.toggle("hidden", visible.length > 0);
    $("#empty-title").textContent = isWatchlist ? "Your list is waiting" : (search ? "No matches found" : "No titles yet");
    $("#empty-copy").textContent = isWatchlist ? "Save a movie or series and it will appear here." : (search ? "Try another title or category." : "An administrator can publish titles here.");
    $("#pagination").classList.toggle("hidden", isWatchlist || videos.length === 0);
}

function makeVideoCard(video) {
    const card = document.createElement("article");
    card.className = "video-card";
    card.tabIndex = 0;
    const poster = document.createElement("div");
    poster.className = "poster-wrap";
    if (video.poster) {
        const image = document.createElement("img");
        image.src = video.poster;
        image.alt = `${video.title} poster`;
        image.loading = "lazy";
        image.onerror = () => {
            image.remove();
            poster.prepend(makePosterPlaceholder(video.title));
        };
        poster.append(image);
    } else poster.append(makePosterPlaceholder(video.title));
    const overlay = document.createElement("div");
    overlay.className = "poster-overlay";
    const play = document.createElement("button");
    play.className = "button button-primary";
    play.type = "button";
    play.textContent = "▶ Play";
    play.addEventListener("click", (event) => { event.stopPropagation(); openPlayer(video); });
    const list = document.createElement("button");
    list.className = "button quick-list";
    list.type = "button";
    list.title = state.watchlist.has(video.id) ? "Remove from My List" : "Add to My List";
    list.textContent = state.watchlist.has(video.id) ? "✓" : "+";
    list.addEventListener("click", (event) => { event.stopPropagation(); toggleWatchlist(video); });
    overlay.append(play, list);
    poster.append(overlay);
    const title = document.createElement("h3");
    title.className = "video-title";
    title.textContent = video.title;
    const meta = document.createElement("div");
    meta.className = "video-meta";
    if (video.year) meta.append(textSpan(video.year));
    if (video.rating) meta.append(textSpan(video.rating));
    if (video.duration) meta.append(textSpan(`${video.duration} min`));
    const category = textSpan((video.categories || []).slice(0, 1).join(""));
    category.className = "video-category";
    meta.append(category);
    card.append(poster, title, meta);
    card.addEventListener("click", () => openPlayer(video));
    card.addEventListener("keydown", (event) => {
        if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openPlayer(video); }
    });
    return card;
}

function textSpan(text) {
    const span = document.createElement("span");
    span.textContent = text;
    return span;
}

function makePosterPlaceholder(title) {
    const placeholder = document.createElement("div");
    placeholder.className = "poster-placeholder";
    placeholder.textContent = title;
    return placeholder;
}

async function openPlayer(video) {
    if (!video.src) {
        showToast("This title does not have a video file yet.", true);
        return;
    }
    state.selectedVideo = video;
    $("#player-title").textContent = video.title;
    $("#player-description").textContent = video.description || "Enjoy the show.";
    $("#player-watchlist").textContent = state.watchlist.has(video.id) ? "✓ In My List" : "＋ My List";
    const player = $("#player");
    $("#player-error").classList.add("hidden");
    $("#player-error").textContent = "";
    $("#player-seek").value = "0";
    $("#player-seek").disabled = true;
    $("#player-current-time").textContent = "0:00";
    $("#player-duration").textContent = "0:00";
    updatePlayerButtons();
    player.src = video.src;
    $("#video-dialog").showModal();
    try {
        await player.play();
    } catch {
        if (player.error) showPlayerError("This video could not be played. Check the media file format and try again.");
    }
}

function formatPlayerTime(seconds) {
    if (!Number.isFinite(seconds) || seconds < 0) return "0:00";
    const totalSeconds = Math.floor(seconds);
    const hours = Math.floor(totalSeconds / 3600);
    const minutes = Math.floor((totalSeconds % 3600) / 60);
    const remainder = totalSeconds % 60;
    return hours
        ? `${hours}:${String(minutes).padStart(2, "0")}:${String(remainder).padStart(2, "0")}`
        : `${minutes}:${String(remainder).padStart(2, "0")}`;
}

function updatePlayerProgress() {
    const player = $("#player");
    const seek = $("#player-seek");
    const hasDuration = Number.isFinite(player.duration) && player.duration > 0;
    seek.disabled = !hasDuration;
    $("#player-duration").textContent = hasDuration ? formatPlayerTime(player.duration) : "0:00";
    $("#player-current-time").textContent = formatPlayerTime(player.currentTime);
    if (hasDuration && document.activeElement !== seek) {
        const percentage = Math.min(100, Math.max(0, (player.currentTime / player.duration) * 100));
        seek.value = String(Math.round((percentage / 100) * Number(seek.max)));
        seek.style.setProperty("--seek-progress", `${percentage}%`);
    }
}

function updatePlayerButtons() {
    const player = $("#player");
    const toggle = $('[data-player="toggle"]');
    toggle.textContent = player.paused ? "▶ Play" : "❚❚ Pause";
    toggle.setAttribute("aria-label", player.paused ? "Play" : "Pause");
    toggle.setAttribute("aria-pressed", String(!player.paused));
    const mute = $('[data-player="mute"]');
    mute.textContent = player.muted || player.volume === 0 ? "Unmute" : "Mute";
    mute.setAttribute("aria-pressed", String(player.muted || player.volume === 0));
    const fullscreen = $('[data-player="fullscreen"]');
    const isFullscreen = document.fullscreenElement !== null;
    fullscreen.textContent = isFullscreen ? "Exit fullscreen" : "Fullscreen";
    fullscreen.setAttribute("aria-label", isFullscreen ? "Exit fullscreen" : "Enter fullscreen");
    fullscreen.setAttribute("aria-pressed", String(isFullscreen));
}

function showPlayerError(message) {
    const error = $("#player-error");
    error.textContent = message;
    error.classList.remove("hidden");
}

async function togglePlayerFullscreen() {
    const dialog = $("#video-dialog");
    try {
        if (document.fullscreenElement) {
            await document.exitFullscreen();
        } else if (dialog.requestFullscreen) {
            try {
                await dialog.requestFullscreen();
            } catch {
                const player = $("#player");
                if (!player.requestFullscreen) throw new Error("Fullscreen is unavailable");
                await player.requestFullscreen();
            }
        } else if ($("#player").webkitEnterFullscreen) {
            $("#player").webkitEnterFullscreen();
        } else if ($("#player").requestFullscreen) {
            await $("#player").requestFullscreen();
        } else {
            showToast("Fullscreen is not supported by this browser.", true);
        }
    } catch {
        showToast("Fullscreen could not be started. Try the browser's fullscreen permission/settings.", true);
    }
}

async function toggleWatchlist(video) {
    try {
        if (state.watchlist.has(video.id)) {
            await api(`/watchlist/${video.id}`, { method: "DELETE" });
            state.watchlist.delete(video.id);
            showToast("Removed from My List.");
        } else {
            await api(`/watchlist/${video.id}`, { method: "POST" });
            state.watchlist.add(video.id);
            showToast("Added to My List.");
        }
        if (state.view === "watchlist") await loadCatalog();
        else renderVideos(state.videos, false);
        if (state.selectedVideo && state.selectedVideo.id === video.id) {
            $("#player-watchlist").textContent = state.watchlist.has(video.id) ? "✓ In My List" : "＋ My List";
        }
    } catch (error) { showToast(error.message, true); }
}

async function uploadVideo(event) {
    event.preventDefault();
    const form = event.currentTarget;
    const button = form.querySelector('button[type="submit"]');
    const originalLabel = button.textContent;
    button.disabled = true;
    try {
        const data = new FormData(form);
        const categories = data.getAll("categories").map((item) => item.trim()).filter(Boolean);
        const metadata = {
            title: data.get("title").trim(),
            description: data.get("description").trim(),
            year: data.get("year") ? Number(data.get("year")) : null,
            rating: data.get("rating").trim() || null,
            duration: data.get("duration") ? Number(data.get("duration")) : null,
            categories,
            published: data.get("published") === "true"
        };
        const upload = async (path, file) => {
            const body = new FormData();
            body.append("file", file);
            await api(path, { method: "POST", body });
        };
        let video;
        if (state.editingVideoId) {
            const videoFile = data.get("video");
            const posterFile = data.get("poster");
            if (videoFile.size > 0) {
                button.textContent = "Replacing video…";
                await upload(`/videos/${state.editingVideoId}/video`, videoFile);
            }
            if (posterFile.size > 0) {
                button.textContent = "Replacing poster…";
                await upload(`/videos/${state.editingVideoId}/poster`, posterFile);
            }
            button.textContent = "Saving changes…";
            video = await api(`/videos/${state.editingVideoId}`, {
                method: "PUT",
                body: JSON.stringify(metadata)
            });
        } else {
            button.textContent = "Creating title…";
            video = await api("/videos", {
                method: "POST",
                body: JSON.stringify({ ...metadata, published: false })
            });
            button.textContent = "Uploading video…";
            await upload(`/videos/${video.id}/video`, data.get("video"));
            button.textContent = "Uploading poster…";
            await upload(`/videos/${video.id}/poster`, data.get("poster"));
            if (metadata.published) {
                button.textContent = "Publishing…";
                video = await api(`/videos/${video.id}`, {
                    method: "PUT",
                    body: JSON.stringify(metadata)
                });
            }
        }
        $("#admin-dialog").close();
        form.reset();
        state.editingVideoId = null;
        setVideoFormMode(false);
        showToast(metadata.published ? "Title saved and published." : "Title saved as a draft.");
        state.page = 0;
        state.view = "home";
        loadCatalog();
        if ($("#admin-users-dialog").open) loadAdminData();
    } catch (error) {
        showToast(error.message, true);
    } finally {
        button.disabled = false;
        button.textContent = originalLabel;
    }

    function mediaUuid(url) {
        if (!url) return null;
        const parts = new URL(url, window.location.origin).pathname.split("/");
        return parts.at(-1) || null;
    }

    function setVideoFormMode(editing) {
        state.editingVideoId = editing ? state.editingVideoId : null;
        $("#admin-form-title").textContent = editing ? "Edit title" : "Add a title";
        $("#admin-save").textContent = editing ? "Save changes" : "Create title";
        $("#admin-video-file").required = !editing;
        $("#admin-poster-file").required = !editing;
        $("#admin-save").disabled = !editing
            && !($("#admin-video-file").files[0] && $("#admin-poster-file").files[0]);
    }

    function openVideoEditor(video) {
        const form = $("#admin-form");
        state.editingVideoId = video.id;
        form.elements.title.value = video.title || "";
        form.elements.description.value = video.description || "";
        form.elements.year.value = video.year || "";
        form.elements.rating.value = video.rating || "";
        form.elements.duration.value = video.duration || "";
        form.elements.published.value = String(video.published);
        [...form.elements.categories.options].forEach((option) => {
            option.selected = (video.categories || []).includes(option.value);
        });
        form.elements.video.value = "";
        form.elements.poster.value = "";
        $("#admin-video-preview").removeAttribute("src");
        $("#admin-video-preview").classList.add("hidden");
        const poster = $("#admin-poster-preview");
        if (video.poster) {
            const id = mediaUuid(video.poster);
            loadAdminImagePreview(id, poster);
        } else {
            poster.removeAttribute("src");
            poster.classList.add("hidden");
        }
        setVideoFormMode(true);
        $("#admin-dialog").showModal();
    }

    async function loadAdminImagePreview(id, image) {
        try {
            const response = await fetch(`/api/admin/media/image/${encodeURIComponent(id)}`, {
                headers: { Authorization: `Bearer ${state.token}` }
            });
            if (!response.ok) throw new Error(readError(await response.text(), response.status));
            if (adminPosterUrl) URL.revokeObjectURL(adminPosterUrl);
            adminPosterUrl = URL.createObjectURL(await response.blob());
            image.src = adminPosterUrl;
            image.classList.remove("hidden");
        } catch (error) {
            showToast(error.message, true);
        }
    }

    async function previewAdminVideo(video) {
        try {
            const id = mediaUuid(video.src);
            if (!id) return;
            const response = await fetch(`/api/admin/media/video/${encodeURIComponent(id)}`, {
                headers: { Authorization: `Bearer ${state.token}` }
            });
            if (!response.ok) throw new Error(readError(await response.text(), response.status));
            if (adminPreviewUrl) URL.revokeObjectURL(adminPreviewUrl);
            adminPreviewUrl = URL.createObjectURL(await response.blob());
            await openPlayer({ ...video, src: adminPreviewUrl });
        } catch (error) {
            showToast(error.message, true);
        }
    }
}

function escapeHtml(value) {
    return String(value ?? "").replace(/[&<>"']/g, (character) => ({
        "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"
    })[character]);
}

async function openAdmin() {
    state.adminSection = "users";
    state.adminPage = 0;
    $("#admin-search").value = "";
    $("#admin-status-filter").value = "";
    $("#admin-search").placeholder = "Search email or name";
    $("#admin-status-filter").options[1].textContent = "Active";
    $("#admin-status-filter").options[2].textContent = "Inactive";
    $("#admin-users-dialog").showModal();
    await loadAdminData();
}

async function loadAdminData() {
    if (state.role !== "ADMIN") return;
    const records = $("#admin-records");
    records.innerHTML = '<div class="loading">Loading administration data…</div>';
    $("#admin-feedback").classList.add("hidden");
    try {
        const stats = await api("/admin/dashboard");
        $("#admin-stats").innerHTML = [
            ["Members", stats.totalUsers],
            ["Active", stats.activeUsers],
            ["Titles", stats.totalVideos],
            ["Published", stats.publishedVideos],
            ["Drafts", stats.draftVideos],
            ["Runtime", `${stats.totalDurationMinutes} min`]
        ].map(([label, value]) => `<div class="admin-stat"><strong>${escapeHtml(value)}</strong><span>${escapeHtml(label)}</span></div>`).join("");

        const query = new URLSearchParams({
            page: String(state.adminPage),
            size: String(PAGE_SIZE)
        });
        const search = $("#admin-search").value.trim();
        const status = $("#admin-status-filter").value;
        if (search) query.set("search", search);
        if (state.adminSection === "videos") {
            if (status) query.set("published", status);
        } else if (status) {
            query.set("active", status);
        }
        const page = await api(`/admin/${state.adminSection}?${query}`);
        state.adminTotal = page.totalElements;
        state.adminRecords = page.content || [];
        $("#admin-page-label").textContent = `${page.totalElements ? state.adminPage * PAGE_SIZE + 1 : 0}–${Math.min((state.adminPage + 1) * PAGE_SIZE, page.totalElements)} of ${page.totalElements}`;
        $("#admin-prev").disabled = state.adminPage === 0;
        $("#admin-next").disabled = (state.adminPage + 1) * PAGE_SIZE >= page.totalElements;
        renderAdminRecords(state.adminRecords);
    } catch (error) {
        records.innerHTML = "";
        $("#admin-feedback").textContent = error.message;
        $("#admin-feedback").classList.remove("hidden");
    }
}

function renderAdminRecords(items) {
    const records = $("#admin-records");
    const isVideos = state.adminSection === "videos";
    state.adminListPosterUrls.forEach((url) => URL.revokeObjectURL(url));
    state.adminListPosterUrls = [];
    if (!items.length) {
        records.innerHTML = `<div class="empty-state"><h3>No ${isVideos ? "titles" : "members"} found</h3><p>Try changing your search or status filter.</p></div>`;
        return;
    }
    records.innerHTML = `<div class="admin-table-wrap"><table class="admin-table"><thead><tr>${
        isVideos
            ? "<th>Title</th><th>Details</th><th>Status</th><th>Created</th><th>Actions</th>"
            : "<th>Member</th><th>Email</th><th>Role</th><th>Status</th><th>Registered</th><th>Actions</th>"
    }</tr></thead><tbody>${items.map((item) => isVideos
        ? `<tr><td class="admin-video-title">${item.poster ? `<img ${item.published ? `src="${escapeHtml(item.poster)}"` : `data-admin-poster-id="${escapeHtml(mediaUuid(item.poster))}"`} alt="" loading="lazy">` : ""}<span>${escapeHtml(item.title)}</span></td><td>${escapeHtml([item.year, item.rating, item.duration ? `${item.duration} min` : null, ...(item.categories || [])].filter(Boolean).join(" · ") || "—")}</td><td>${item.published ? "Public" : "Draft"}</td><td>${escapeHtml(formatDate(item.createdAt))}</td><td class="admin-actions"><button class="text-button" data-admin-action="edit-video" data-id="${item.id}">Edit</button><button class="text-button" data-admin-action="preview-video" data-id="${item.id}" ${item.src ? "" : "disabled"}>Preview</button><button class="text-button" data-admin-action="publish" data-id="${item.id}" data-published="${item.published}">${item.published ? "Unpublish" : "Publish"}</button><button class="text-button danger" data-admin-action="delete-video" data-id="${item.id}">Delete</button></td></tr>`
        : `<tr><td>${escapeHtml(item.fullname)}</td><td>${escapeHtml(item.email)}</td><td>${escapeHtml(item.role)}</td><td>${item.active ? "Active" : "Inactive"}</td><td>${escapeHtml(formatDate(item.createdAt))}</td><td class="admin-actions"><button class="text-button" data-admin-action="name" data-id="${item.id}">Edit name</button><button class="text-button" data-admin-action="role" data-id="${item.id}" data-role="${escapeHtml(item.role)}">${item.role === "ADMIN" ? "Make member" : "Make admin"}</button><button class="text-button" data-admin-action="active" data-id="${item.id}" data-active="${item.active}">${item.active ? "Deactivate" : "Activate"}</button><button class="text-button danger" data-admin-action="delete-user" data-id="${item.id}">Delete</button></td></tr>`
    ).join("")}</tbody></table></div>`;
    records.querySelectorAll("[data-admin-poster-id]").forEach(async (image) => {
        try {
            const response = await fetch(
                `/api/admin/media/image/${encodeURIComponent(image.dataset.adminPosterId)}`,
                { headers: { Authorization: `Bearer ${state.token}` } }
            );
            if (!response.ok) {
                showToast(readError(await response.text(), response.status), true);
                return;
            }
            const url = URL.createObjectURL(await response.blob());
            state.adminListPosterUrls.push(url);
            image.src = url;
        } catch (error) {
            showToast(`Could not load an admin poster preview: ${error.message}`, true);
        }
    });
}

function formatDate(value) {
    if (!value) return "—";
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? "—" : date.toLocaleDateString();
}

async function runAdminAction(button) {
    const { adminAction: action, id } = button.dataset;
    try {
        if (action === "edit-video") {
            const video = state.adminRecords.find((item) => item.id === Number(id));
            if (video) openVideoEditor(video);
            return;
        } else if (action === "preview-video") {
            const video = state.adminRecords.find((item) => item.id === Number(id));
            if (video) await previewAdminVideo(video);
            return;
        } else if (action === "publish") {
            const video = state.adminRecords?.find((item) => item.id === Number(id));
            if (!video) {
                await loadAdminData();
                return;
            }
            await api(`/videos/${id}`, {
                method: "PUT",
                body: JSON.stringify({
                    title: video.title,
                    description: video.description,
                    year: video.year,
                    rating: video.rating,
                    duration: video.duration,
                    categories: video.categories || [],
                    published: button.dataset.published !== "true"
                })
            });
        } else if (action === "delete-video") {
            if (!window.confirm("Delete this title and remove it from all watchlists?")) return;
            await api(`/videos/${id}`, { method: "DELETE" });
        } else if (action === "role") {
            const role = button.dataset.role === "ADMIN" ? "USER" : "ADMIN";
            await api(`/admin/users/${id}/role?role=${role}`, { method: "PUT" });
        } else if (action === "name") {
            const user = state.adminRecords.find((item) => item.id === Number(id));
            const fullName = window.prompt("Update this member's display name:", user?.fullname || "");
            if (fullName === null) return;
            if (!fullName.trim()) {
                showToast("Name cannot be empty.", true);
                return;
            }
            await api(`/admin/users/${id}`, {
                method: "PUT",
                body: JSON.stringify({ fullName: fullName.trim() })
            });
        } else if (action === "active") {
            const active = button.dataset.active !== "true";
            await api(`/admin/users/${id}/status?active=${active}`, { method: "PUT" });
        } else if (action === "delete-user") {
            if (!window.confirm("Delete this account? This action cannot be undone.")) return;
            await api(`/admin/users/${id}`, { method: "DELETE" });
        }
        await loadAdminData();
        showToast("Administration change saved.");
    } catch (error) {
        showToast(error.message, true);
    }
}

$("#login-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    const values = Object.fromEntries(new FormData(event.currentTarget));
    try {
        const result = await api("/auth/login", { method: "POST", body: JSON.stringify(values) });
        const claims = decodeToken(result.token);
        if (adminLoginMode && claims?.role !== "ADMIN") {
            showToast("This account is not an administrator. Configure ADMIN_EMAIL and ADMIN_PASSWORD for the server.", true);
            return;
        }
        state.token = result.token;
        sessionStorage.setItem("pulseToken", result.token);
        activateApp();
        showToast(result.message || "Welcome back.");
    } catch (error) {
        if (/verify your email/i.test(error.message)) {
            $("#unverified-warning").classList.remove("hidden");
            $("#unverified-warning").dataset.email = values.email;
        }
        showToast(error.message, true);
    }
});

document.querySelectorAll("#signup-form [name=password], #signup-form [name=confirmPassword]").forEach((input) => {
    input.addEventListener("input", () => {
        const password = $("#signup-form [name=password]").value;
        const confirm = $("#signup-form [name=confirmPassword]").value;
        const mismatch = confirm.length > 0 && password !== confirm;
        $("#password-match-error").classList.toggle("hidden", !mismatch);
        $("#signup-submit").disabled = !password || !confirm || mismatch;
    });
});

document.querySelectorAll("#reset-form [name=newPassword], #reset-form [name=confirmPassword]").forEach((input) => {
    input.addEventListener("input", () => {
        const values = new FormData($("#reset-form"));
        const mismatch = values.get("confirmPassword").length > 0
            && values.get("newPassword") !== values.get("confirmPassword");
        $("#reset-password-error").textContent = mismatch ? "Passwords do not match" : "";
        $("#reset-password-error").classList.toggle("hidden", !mismatch);
        $("#reset-submit").disabled = mismatch;
    });
});

document.querySelectorAll(".password-toggle").forEach((button) => button.addEventListener("click", () => {
    const input = button.closest("label").querySelector("input");
    const show = input.type === "password";
    input.type = show ? "text" : "password";
    button.textContent = show ? "Hide" : "Show";
    button.setAttribute("aria-label", show ? "Hide password" : "Show password");
}));

$("#change-password-error").textContent = "";
document.querySelectorAll("#password-form [name=newPassword], #password-form [name=confirmPassword]").forEach((input) => {
    input.addEventListener("input", () => {
        const values = new FormData($("#password-form"));
        const mismatch = values.get("confirmPassword").length > 0
            && values.get("newPassword") !== values.get("confirmPassword");
        $("#change-password-error").textContent = mismatch ? "Passwords do not match" : "";
        $("#change-password-error").classList.toggle("hidden", !mismatch);
        $("#password-form button[type=submit]").disabled = mismatch;
    });
});
$("#signup-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);
    const values = Object.fromEntries(formData);
    if (values.password !== values.confirmPassword) {
        $("#password-match-error").classList.remove("hidden");
        return;
    }
    delete values.confirmPassword;
    try {
        const result = await api("/auth/signup", { method: "POST", body: JSON.stringify(values) });
        showToast(result.message || "Check your email to verify your account.");
        $("#auth-subtitle").textContent = "We sent a verification link. It is valid for 24 hours; check your inbox to activate your account.";
        $("#login-form [name=email]").value = values.email;
        $("#email-action-form [name=email]").value = values.email;
        setAuthMode("login");
        $("#auth-subtitle").textContent = "We sent a verification link. It is valid for 24 hours; check your inbox to activate your account.";
    } catch (error) { showToast(error.message, true); }
});

$("#email-action-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    const email = new FormData(event.currentTarget).get("email");
    try {
        const result = await api("/auth/forgot-password", { method: "POST", body: JSON.stringify({ email }) });
        showToast(result.message);
    } catch (error) { showToast(error.message, true); }
});

$("#reset-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    const values = Object.fromEntries(new FormData(event.currentTarget));
    const errorLabel = $("#reset-password-error");
    if (values.newPassword !== values.confirmPassword) {
        errorLabel.textContent = "Passwords do not match";
        errorLabel.classList.remove("hidden");
        return;
    }
    try {
        const result = await api("/auth/reset-password", { method: "POST", body: JSON.stringify({ token: resetToken, newPassword: values.newPassword }) });
        showToast(result.message || "Password updated. Sign in with your new password.");
        resetToken = null;
        cleanQueryString();
        setAuthMode("login");
    } catch (error) {
        errorLabel.textContent = "Invalid or expired reset token. Request a new reset link.";
        errorLabel.classList.remove("hidden");
        $("#reset-request-new").classList.remove("hidden");
        showToast(error.message, true);
    }
});

$("#auth-switch").addEventListener("click", () => {
    if (authMode === "login") adminLoginMode = false;
    setAuthMode(authMode === "login" ? "signup" : "login");
});
$("#back-to-login").addEventListener("click", () => {
    adminLoginMode = false;
    setAuthMode("login");
});
$("#admin-login-toggle").addEventListener("click", () => {
    adminLoginMode = !adminLoginMode;
    $("#admin-login-toggle").textContent = adminLoginMode ? "Regular member sign in" : "Administrator sign in";
    setAuthMode("login");
});
$('[data-action="forgot"]').addEventListener("click", () => setAuthMode("email"));
$('[data-action="resend"]').addEventListener("click", async () => {
    const email = new FormData($("#email-action-form")).get("email");
    if (!email) { showToast("Enter your email address first.", true); return; }
    try {
        const result = await api("/auth/resend-verification", { method: "POST", body: JSON.stringify({ email }) });
        showToast(result.message);
    } catch (error) { showToast(error.message, true); }
});

$("#signout-button").addEventListener("click", () => $("#logout-dialog").showModal());
$("#logout-cancel").addEventListener("click", () => $("#logout-dialog").close());
$("#logout-confirm").addEventListener("click", async () => {
    $("#logout-dialog").close();
    try {
        const result = await api("/auth/logout", { method: "POST" });
        signOut(false);
        showToast(result.message || "You have signed out.");
    } catch (error) {
        signOut(false);
        showToast(`Signed out on this browser, but server logout failed: ${error.message}`, true);
    }
});
$("#landing-signin").addEventListener("click", () => {
    showAuth();
    setAuthMode("login");
});
$("#back-to-landing").addEventListener("click", showLanding);
$("#get-started-form").addEventListener("submit", (event) => {
    event.preventDefault();
    joinFromLanding();
});
document.querySelectorAll("[data-join]").forEach((button) => button.addEventListener("click", joinFromLanding));
$("#reverify-button").addEventListener("click", async () => {
    const email = $("#unverified-warning").dataset.email || $("#login-form [name=email]").value.trim();
    if (!email) { showToast("Enter your email address first.", true); return; }
    try {
        const result = await api("/auth/resend-verification", { method: "POST", body: JSON.stringify({ email }) });
        showToast(result.message);
    } catch (error) { showToast(error.message, true); }
});
$("#reset-request-new").addEventListener("click", () => {
    const email = $("#email-action-form [name=email]");
    email.value = $("#login-form [name=email]").value || $("#signup-form [name=email]").value;
    resetToken = null;
    cleanQueryString();
    setAuthMode("email");
});
$("#admin-open").addEventListener("click", () => {
    $("#admin-form").reset();
    state.editingVideoId = null;
    $("#admin-video-preview").removeAttribute("src");
    $("#admin-video-preview").classList.add("hidden");
    $("#admin-poster-preview").removeAttribute("src");
    $("#admin-poster-preview").classList.add("hidden");
    if (adminPosterUrl) {
        URL.revokeObjectURL(adminPosterUrl);
        adminPosterUrl = null;
    }
    $("#admin-form [name=year]").value = String(new Date().getFullYear());
    setVideoFormMode(false);
    $("#admin-dialog").showModal();
});
$("#admin-users-open").addEventListener("click", openAdmin);
$("#admin-create-user-open").addEventListener("click", () => {
    $("#admin-create-user-form").reset();
    $("#admin-create-user-dialog").showModal();
});
$("#admin-create-user-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    const form = event.currentTarget;
    const submit = form.querySelector('button[type="submit"]');
    submit.disabled = true;
    try {
        const user = Object.fromEntries(new FormData(form));
        const result = await api("/admin/users", {
            method: "POST",
            body: JSON.stringify(user)
        });
        $("#admin-create-user-dialog").close();
        showToast(result.message || "Member created. The user must verify their email.");
        await loadAdminData();
    } catch (error) {
        showToast(error.message, true);
    } finally {
        submit.disabled = false;
    }
});
$("#admin-tab-users").addEventListener("click", () => {
    state.adminSection = "users";
    state.adminPage = 0;
    $("#admin-search").placeholder = "Search email or name";
    $("#admin-status-filter").options[1].textContent = "Active";
    $("#admin-status-filter").options[2].textContent = "Inactive";
    loadAdminData();
});
$("#admin-tab-videos").addEventListener("click", () => {
    state.adminSection = "videos";
    state.adminPage = 0;
    $("#admin-search").placeholder = "Search titles";
    $("#admin-status-filter").options[1].textContent = "Public";
    $("#admin-status-filter").options[2].textContent = "Draft";
    loadAdminData();
});
$("#admin-search").addEventListener("input", () => {
    clearTimeout(state.adminSearchTimer);
    state.adminPage = 0;
    state.adminSearchTimer = setTimeout(loadAdminData, 250);
});
$("#admin-status-filter").addEventListener("change", () => {
    state.adminPage = 0;
    loadAdminData();
});
$("#admin-prev").addEventListener("click", () => {
    if (state.adminPage > 0) {
        state.adminPage--;
        loadAdminData();
    }
});
$("#admin-next").addEventListener("click", () => {
    if ((state.adminPage + 1) * PAGE_SIZE < state.adminTotal) {
        state.adminPage++;
        loadAdminData();
    }
});
$("#admin-records").addEventListener("click", (event) => {
    const button = event.target.closest("[data-admin-action]");
    if (button) runAdminAction(button);
});
$("#admin-users-dialog").addEventListener("close", () => {
    state.adminListPosterUrls.forEach((url) => URL.revokeObjectURL(url));
    state.adminListPosterUrls = [];
});
$("#admin-view-toggle").addEventListener("click", () => {
    state.adminView = !state.adminView;
    $("#admin-open").classList.toggle("hidden", !state.adminView);
    $("#admin-users-open").classList.toggle("hidden", !state.adminView);
    $("#admin-view-toggle").textContent = state.adminView ? "Switch to User View" : "Switch to Admin View";
});
$("#admin-form").addEventListener("submit", uploadVideo);
$("#admin-form [name=year]").value = String(new Date().getFullYear());
$("#admin-form [name=duration]").addEventListener("input", () => {
    const minutes = Number($("#admin-duration").value);
    $("#admin-duration-seconds").textContent = `Duration: ${minutes ? minutes * 60 : "—"} seconds`;
});
$("#admin-video-file").addEventListener("change", () => {
    const file = $("#admin-video-file").files[0];
    const preview = $("#admin-video-preview");
    if (!file) {
        preview.classList.add("hidden");
        $("#admin-save").disabled = !state.editingVideoId
            && !$("#admin-poster-file").files[0];
        return;
    }
    preview.src = URL.createObjectURL(file);
    preview.classList.remove("hidden");
    preview.onloadedmetadata = () => {
        if (Number.isFinite(preview.duration)) {
            $("#admin-duration").value = String(Math.max(1, Math.ceil(preview.duration / 60)));
            $("#admin-duration-seconds").textContent = `Detected duration: ${Math.round(preview.duration)} seconds`;
        }
    };
    $("#admin-save").disabled = !state.editingVideoId
        && !$("#admin-poster-file").files[0];
});
$("#admin-poster-file").addEventListener("change", () => {
    const file = $("#admin-poster-file").files[0];
    const preview = $("#admin-poster-preview");
    if (file) {
        preview.src = URL.createObjectURL(file);
        preview.classList.remove("hidden");
    } else {
        preview.removeAttribute("src");
        preview.classList.add("hidden");
    }
    $("#admin-save").disabled = !state.editingVideoId
        && (!file || !$("#admin-video-file").files[0]);
});
$("#password-open").addEventListener("click", () => $("#password-dialog").showModal());
$("#profile-open").addEventListener("click", async () => {
    try {
        const profile = await api("/auth/profile");
        $("#profile-name").textContent = profile.fullname;
        $("#profile-email").textContent = profile.email;
        $("#profile-role").textContent = profile.role === "ADMIN" ? "Administrator" : "Member";
        $("#profile-created").textContent = formatDate(profile.createdAt);
        $("#profile-dialog").showModal();
    } catch (error) {
        showToast(error.message, true);
    }
});
$("#password-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    const values = Object.fromEntries(new FormData(event.currentTarget));
    if (values.newPassword !== values.confirmPassword) {
        $("#change-password-error").textContent = "Passwords do not match";
        $("#change-password-error").classList.remove("hidden");
        return;
    }
    delete values.confirmPassword;
    try {
        const result = await api("/auth/change-password", { method: "POST", body: JSON.stringify(values) });
        $("#password-dialog").close();
        event.currentTarget.reset();
        signOut(false);
        showToast(result.message || "Password updated. Sign in with your new password.");
    } catch (error) { showToast(error.message, true); }
});
$("#player-watchlist").addEventListener("click", () => {
    if (state.selectedVideo) toggleWatchlist(state.selectedVideo);
});
document.querySelectorAll("[data-close]").forEach((button) => button.addEventListener("click", () => {
    const dialog = document.getElementById(button.dataset.close);
    if (dialog.id === "video-dialog") {
        $("#player").pause();
        $("#player").removeAttribute("src");
        $("#player").load();
    }
    dialog.close();
}));
document.querySelectorAll("[data-view]").forEach((button) => button.addEventListener("click", (event) => {
    event.preventDefault();
    state.view = button.dataset.view;
    state.page = 0;
    document.querySelectorAll(".nav-link").forEach((link) => link.classList.toggle("active", link.dataset.view === state.view));
    loadCatalog();
    window.scrollTo({ top: 0, behavior: "smooth" });
}));
$("#hero-browse").addEventListener("click", () => $("#catalog-title").scrollIntoView({ behavior: "smooth" }));
$("#prev-page").addEventListener("click", () => { if (state.page > 0) { state.page--; loadCatalog(); } });
$("#next-page").addEventListener("click", () => { state.page++; loadCatalog(); });
$("#search-input").addEventListener("input", () => {
    if (state.view === "watchlist") {
        renderVideos(state.videos, true);
        return;
    }
    clearTimeout(state.catalogSearchTimer);
    state.page = 0;
    state.catalogSearchTimer = setTimeout(loadCatalog, 250);
});
$("#hero-prev").addEventListener("click", () => moveFeatured(-1));
$("#hero-next").addEventListener("click", () => moveFeatured(1));
$("#video-dialog").addEventListener("close", () => {
    if (document.fullscreenElement === $("#video-dialog")) {
        document.exitFullscreen().catch(() => {});
    }
    $("#player").pause();
    $("#player").removeAttribute("src");
    $("#player").load();
    $("#player-seek").value = "0";
    $("#player-seek").disabled = true;
    $("#player-current-time").textContent = "0:00";
    $("#player-duration").textContent = "0:00";
    updatePlayerButtons();
    if (adminPreviewUrl) {
        URL.revokeObjectURL(adminPreviewUrl);
        adminPreviewUrl = null;
    }
});

$("#player").addEventListener("loadedmetadata", updatePlayerProgress);
$("#player").addEventListener("durationchange", updatePlayerProgress);
$("#player").addEventListener("timeupdate", updatePlayerProgress);
$("#player").addEventListener("play", updatePlayerButtons);
$("#player").addEventListener("pause", updatePlayerButtons);
$("#player").addEventListener("ended", updatePlayerButtons);
$("#player").addEventListener("volumechange", updatePlayerButtons);
$("#player").addEventListener("error", () => {
    if ($("#video-dialog").open) {
        showPlayerError("The video could not be loaded. Check that the upload is a supported MP4, WebM, OGG, or MOV file.");
    }
});
document.addEventListener("fullscreenchange", updatePlayerButtons);
$("#player").addEventListener("webkitbeginfullscreen", updatePlayerButtons);
$("#player").addEventListener("webkitendfullscreen", updatePlayerButtons);
$("#player-seek").addEventListener("input", (event) => {
    const player = $("#player");
    if (!Number.isFinite(player.duration) || player.duration <= 0) return;
    const percentage = (Number(event.currentTarget.value) / Number(event.currentTarget.max)) * 100;
    player.currentTime = (percentage / 100) * player.duration;
    event.currentTarget.style.setProperty("--seek-progress", `${percentage}%`);
    $("#player-current-time").textContent = formatPlayerTime(player.currentTime);
});

document.querySelectorAll("[data-player]").forEach((button) => button.addEventListener("click", async () => {
    const player = $("#player");
    try {
        switch (button.dataset.player) {
            case "back":
                if (Number.isFinite(player.duration)) player.currentTime = Math.max(0, player.currentTime - 10);
                break;
            case "forward":
                if (Number.isFinite(player.duration)) player.currentTime = Math.min(player.duration, player.currentTime + 10);
                break;
            case "toggle":
                if (player.paused) await player.play();
                else player.pause();
                break;
            case "mute":
                togglePlayerMute();
                break;
            case "fullscreen":
                await togglePlayerFullscreen();
                break;
            case "pip":
                if (document.pictureInPictureElement) await document.exitPictureInPicture();
                else if (document.pictureInPictureEnabled) await player.requestPictureInPicture();
                else showToast("Picture-in-Picture is not supported by this browser.", true);
                break;
        }
    } catch {
        showToast("That playback control is not available for this video or browser.", true);
    }
}));
$("#player-volume").addEventListener("input", (event) => {
    $("#player").volume = Number(event.currentTarget.value);
    $("#player").muted = Number(event.currentTarget.value) === 0;
});
document.addEventListener("keydown", async (event) => {
    if (!$("#video-dialog").open
            || ["INPUT", "TEXTAREA", "SELECT", "BUTTON"].includes(document.activeElement.tagName)
            || event.altKey || event.ctrlKey || event.metaKey) return;
    const player = $("#player");
    if (event.key === "ArrowLeft" && Number.isFinite(player.currentTime)) { event.preventDefault(); player.currentTime = Math.max(0, player.currentTime - 10); }
    if (event.key === "ArrowRight" && Number.isFinite(player.duration)) { event.preventDefault(); player.currentTime = Math.min(player.duration, player.currentTime + 10); }
    if (event.key === "ArrowUp") { event.preventDefault(); player.volume = Math.min(1, player.volume + .05); $("#player-volume").value = String(player.volume); }
    if (event.key === "ArrowDown") { event.preventDefault(); player.volume = Math.max(0, player.volume - .05); $("#player-volume").value = String(player.volume); }
    if (event.key.toLowerCase() === "m") togglePlayerMute();
    if (event.key.toLowerCase() === "f") {
        event.preventDefault();
        await togglePlayerFullscreen();
    }
});
document.querySelectorAll(".faq-list details").forEach((details) => {
    details.addEventListener("toggle", () => {
        const icon = details.querySelector("summary span");
        if (icon) icon.textContent = details.open ? "−" : "+";
    });
});
document.querySelectorAll("[data-view]").forEach((button) => {
    button.addEventListener("click", () => {
        if (button.dataset.view === "home") $("#landing-view").classList.add("hidden");
    });
});

async function start() {
    await handleEmailLinks();
    const backendOnline = await checkBackend();
    if (!state.token) {
        if (resetToken || new URLSearchParams(location.search).has("verifyToken")) showAuth();
        else {
            showLanding();
            if (backendOnline) loadLandingPreview();
        }
        return;
    }
    try {
        const valid = await api("/auth/validate");
        if (!valid) { signOut(false); showAuth(); return; }
        activateApp();
    } catch {
        signOut(false);
        showAuth();
        showToast("Can't reach the backend. Start the Spring Boot app and refresh.", true);
    }
}

start();
