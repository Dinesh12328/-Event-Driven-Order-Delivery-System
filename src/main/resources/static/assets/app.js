const tokenKey = "order-delivery-token";
const state = {
    token: localStorage.getItem(tokenKey) || "",
    dashboard: null,
    events: [],
    restaurants: []
};

const nodes = [
    { label: "Customer", x: 0.08, y: 0.48, color: "#61d7a1" },
    { label: "Order", x: 0.25, y: 0.30, color: "#66c7d9" },
    { label: "Restaurant", x: 0.44, y: 0.54, color: "#f2b84b" },
    { label: "Payment", x: 0.62, y: 0.34, color: "#e66b7a" },
    { label: "Delivery", x: 0.78, y: 0.56, color: "#61d7a1" },
    { label: "Notify", x: 0.92, y: 0.38, color: "#66c7d9" }
];

const canvas = document.getElementById("flowCanvas");
const ctx = canvas.getContext("2d");

document.getElementById("loginForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    await login();
});

document.getElementById("createAdminButton").addEventListener("click", async () => {
    const email = document.getElementById("email").value;
    const password = document.getElementById("password").value;
    const button = document.getElementById("createAdminButton");
    try {
        button.disabled = true;
        setText("authState", "Creating admin");
        await api("/api/auth/register", {
            method: "POST",
            body: JSON.stringify({
                fullName: "Admin One",
                email,
                password,
                role: "ADMIN"
            })
        }, false);
        setText("authState", "Admin created. Logging in");
        await login();
    } catch (error) {
        const message = error.message.includes("already registered")
                ? "Admin already exists. Try Login"
                : error.message;
        setText("authState", message);
    } finally {
        button.disabled = false;
    }
});

document.getElementById("refresh").addEventListener("click", refresh);
document.getElementById("runScenario").addEventListener("click", runScenario);

async function login() {
    const email = document.getElementById("email").value;
    const password = document.getElementById("password").value;
    const button = document.getElementById("loginButton");
    try {
        button.disabled = true;
        setText("authState", "Logging in");
        const response = await api("/api/auth/login", {
            method: "POST",
            body: JSON.stringify({ email, password })
        }, false);
        state.token = response.data.token;
        localStorage.setItem(tokenKey, state.token);
        setText("authState", `Logged in as ${response.data.user.role}`);
        await refresh();
    } catch (error) {
        setText("authState", error.message);
    } finally {
        button.disabled = false;
    }
}

async function refresh() {
    await health();
    await Promise.all([dashboard(), events(), restaurants()]);
}

async function runScenario() {
    const button = document.getElementById("runScenario");
    if (!state.token) {
        setText("journeyState", "Login as admin first");
        return;
    }
    try {
        button.disabled = true;
        setText("journeyState", "Running");
        renderJourneySteps(["Starting full order journey"]);
        const response = await api("/api/admin/scenarios/order-journey", { method: "POST" });
        setText("journeyState", "Completed");
        renderJourney(response.data);
        await refresh();
    } catch (error) {
        setText("journeyState", "Failed");
        renderJourneySteps([error.message]);
    } finally {
        button.disabled = false;
    }
}

async function health() {
    try {
        const response = await api("/api/health", {}, false);
        setText("health", response.data.database);
    } catch {
        setText("health", "Down");
    }
}

async function dashboard() {
    if (!state.token) {
        setText("authState", "Create admin once, then login");
        return;
    }
    try {
        const response = await api("/api/admin/dashboard");
        state.dashboard = response.data;
        setText("users", response.data.users);
        setText("orders", response.data.orders);
        setText("payments", response.data.payments);
        setText("deliveries", response.data.deliveries);
        setText("events", response.data.integrationEvents);
        renderOrders(response.data.recentOrders || []);
    } catch (error) {
        setText("authState", error.message);
    }
}

async function events() {
    if (!state.token) {
        return;
    }
    try {
        const response = await api("/api/admin/events");
        state.events = response.data || [];
        setText("eventCount", `${state.events.length} events`);
        renderEvents(state.events);
    } catch {
        renderEvents([]);
    }
}

async function restaurants() {
    try {
        const response = await api("/api/restaurants", {}, false);
        state.restaurants = response.data || [];
        setText("restaurantCount", `${state.restaurants.length} active`);
        renderRestaurants(state.restaurants);
    } catch {
        renderRestaurants([]);
    }
}

async function api(path, options = {}, auth = true) {
    const headers = {
        "Content-Type": "application/json",
        ...(options.headers || {})
    };
    if (auth && state.token) {
        headers.Authorization = `Bearer ${state.token}`;
    }
    const response = await fetch(path, { ...options, headers });
    const data = await response.json().catch(() => ({}));
    if (!response.ok) {
        throw new Error(data.message || `Request failed: ${response.status}`);
    }
    return data;
}

function renderOrders(orders) {
    setText("orderCount", `${orders.length} tracked`);
    const target = document.getElementById("recentOrders");
    target.innerHTML = "";
    if (!orders.length) {
        target.append(empty("No recent orders"));
        return;
    }
    orders.forEach((order) => {
        target.append(row(order.restaurantName, `${order.customerName} - ${money(order.totalPrice)}`, order.status));
    });
}

function renderEvents(events) {
    const target = document.getElementById("eventList");
    target.innerHTML = "";
    if (!events.length) {
        target.append(empty("No events yet"));
        return;
    }
    events.slice(0, 8).forEach((event) => {
        target.append(row(event.eventType, event.topic, event.status));
    });
}

function renderRestaurants(restaurants) {
    const target = document.getElementById("restaurantList");
    target.innerHTML = "";
    if (!restaurants.length) {
        target.append(empty("No restaurants yet"));
        return;
    }
    restaurants.slice(0, 8).forEach((restaurant) => {
        target.append(row(restaurant.name, `${restaurant.cuisine} - ${restaurant.location}`, restaurant.active ? "ACTIVE" : "INACTIVE"));
    });
}

function renderJourney(journey) {
    const target = document.getElementById("journeyResult");
    target.innerHTML = "";
    const summary = document.createElement("div");
    summary.className = "journey-summary";
    summary.append(metric("Restaurant", journey.restaurant.name));
    summary.append(metric("Order", shortId(journey.order.id)));
    summary.append(metric("Order status", journey.order.status));
    summary.append(metric("Payment", journey.payment.status));
    summary.append(metric("Delivery", journey.delivery.status));
    summary.append(metric("Total", money(journey.order.totalPrice)));
    target.append(summary);
    renderJourneySteps(journey.completedSteps || []);
}

function renderJourneySteps(steps) {
    const target = document.getElementById("journeyResult");
    let list = target.querySelector(".journey-steps");
    if (!list) {
        list = document.createElement("ol");
        list.className = "journey-steps";
        target.append(list);
    }
    list.innerHTML = "";
    steps.forEach((step) => {
        const item = document.createElement("li");
        item.textContent = step;
        list.append(item);
    });
}

function metric(label, value) {
    const node = document.createElement("article");
    node.innerHTML = `<span>${escapeHtml(label)}</span><strong>${escapeHtml(value)}</strong>`;
    return node;
}

function row(title, detail, badge) {
    const node = document.createElement("div");
    node.className = "row";
    node.innerHTML = `<strong>${escapeHtml(title)}</strong><span class="meta">${escapeHtml(detail)}</span><div><span class="badge">${escapeHtml(badge)}</span></div>`;
    return node;
}

function empty(text) {
    const node = document.createElement("div");
    node.className = "empty";
    node.textContent = text;
    return node;
}

function setText(id, value) {
    document.getElementById(id).textContent = value;
}

function money(value) {
    return new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR" }).format(value || 0);
}

function shortId(value) {
    return String(value || "").slice(0, 8);
}

function escapeHtml(value) {
    return String(value ?? "").replace(/[&<>"']/g, (char) => ({
        "&": "&amp;",
        "<": "&lt;",
        ">": "&gt;",
        "\"": "&quot;",
        "'": "&#039;"
    }[char]));
}

function resizeCanvas() {
    const rect = canvas.getBoundingClientRect();
    const ratio = window.devicePixelRatio || 1;
    canvas.width = Math.round(rect.width * ratio);
    canvas.height = Math.round(rect.height * ratio);
    ctx.setTransform(ratio, 0, 0, ratio, 0, 0);
}

function draw(time) {
    const width = canvas.clientWidth;
    const height = canvas.clientHeight;
    ctx.clearRect(0, 0, width, height);

    const horizon = height * 0.22;
    ctx.fillStyle = "#10110f";
    ctx.fillRect(0, 0, width, height);
    ctx.strokeStyle = "rgba(242, 184, 75, 0.16)";
    ctx.lineWidth = 1;

    for (let i = -8; i < 18; i++) {
        const x = (i / 12) * width + ((time * 0.025) % (width / 12));
        ctx.beginPath();
        ctx.moveTo(x, height);
        ctx.lineTo(width / 2 + (x - width / 2) * 0.16, horizon);
        ctx.stroke();
    }

    for (let i = 0; i < 10; i++) {
        const y = horizon + i * (height - horizon) / 10;
        ctx.beginPath();
        ctx.moveTo(0, y);
        ctx.lineTo(width, y);
        ctx.stroke();
    }

    const points = nodes.map((node) => project(node, width, height));
    ctx.lineWidth = 3;
    for (let i = 0; i < points.length - 1; i++) {
        const a = points[i];
        const b = points[i + 1];
        ctx.strokeStyle = "rgba(243, 242, 234, 0.22)";
        ctx.beginPath();
        ctx.moveTo(a.x, a.y);
        ctx.bezierCurveTo((a.x + b.x) / 2, a.y - 60, (a.x + b.x) / 2, b.y + 60, b.x, b.y);
        ctx.stroke();
    }

    for (let i = 0; i < points.length - 1; i++) {
        const progress = ((time / 1500) + i / points.length) % 1;
        const a = points[i];
        const b = points[i + 1];
        const x = lerp(a.x, b.x, progress);
        const y = lerp(a.y, b.y, progress) - Math.sin(progress * Math.PI) * 42;
        drawPackage(x, y, progress, i);
    }

    points.forEach((point, index) => drawNode(point, nodes[index]));
    requestAnimationFrame(draw);
}

function project(node, width, height) {
    const depth = 0.55 + node.y * 0.55;
    return {
        x: width * node.x,
        y: height * (0.18 + node.y * 0.66),
        scale: depth
    };
}

function drawNode(point, node) {
    const radius = 18 * point.scale;
    ctx.fillStyle = "rgba(0, 0, 0, 0.28)";
    ctx.beginPath();
    ctx.ellipse(point.x, point.y + radius * 1.15, radius * 1.45, radius * 0.36, 0, 0, Math.PI * 2);
    ctx.fill();

    ctx.fillStyle = node.color;
    ctx.strokeStyle = "rgba(243, 242, 234, 0.65)";
    ctx.lineWidth = 2;
    ctx.beginPath();
    ctx.roundRect(point.x - radius, point.y - radius, radius * 2, radius * 2, 7);
    ctx.fill();
    ctx.stroke();

    ctx.fillStyle = "#10110f";
    ctx.font = "600 12px system-ui";
    ctx.textAlign = "center";
    ctx.fillText(node.label, point.x, point.y + radius + 22);
}

function drawPackage(x, y, progress, index) {
    const size = 12 + Math.sin(progress * Math.PI) * 5;
    ctx.save();
    ctx.translate(x, y);
    ctx.rotate((progress + index) * 0.8);
    ctx.fillStyle = index % 2 ? "#f2b84b" : "#61d7a1";
    ctx.strokeStyle = "rgba(243, 242, 234, 0.68)";
    ctx.lineWidth = 2;
    ctx.beginPath();
    ctx.roundRect(-size, -size, size * 2, size * 2, 5);
    ctx.fill();
    ctx.stroke();
    ctx.restore();
}

function lerp(a, b, t) {
    return a + (b - a) * t;
}

window.addEventListener("resize", resizeCanvas);
resizeCanvas();
requestAnimationFrame(draw);

if (state.token) {
    setText("authState", "Token loaded");
} else {
    setText("authState", "Create admin once, then login");
}
refresh();
