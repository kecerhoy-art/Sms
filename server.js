require("dotenv").config();
const express = require("express");
const helmet = require("helmet");
const jwt = require("jsonwebtoken");
const Database = require("better-sqlite3");
const path = require("path");

const { PORT = 3000, HOST = "127.0.0.1", ADMIN_PASSWORD, DEVICE_TOKEN,
  JWT_SECRET, DATABASE_FILE = "./sms.sqlite" } = process.env;

if (!ADMIN_PASSWORD || !DEVICE_TOKEN || !JWT_SECRET ||
    [ADMIN_PASSWORD, DEVICE_TOKEN, JWT_SECRET].some(v => v.startsWith("change-this"))) {
  throw new Error("Set strong ADMIN_PASSWORD, DEVICE_TOKEN, and JWT_SECRET in .env");
}

const db = new Database(DATABASE_FILE);
db.pragma("journal_mode = WAL");
db.exec(`
  CREATE TABLE IF NOT EXISTS messages (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    sender TEXT NOT NULL,
    body TEXT NOT NULL,
    received_at TEXT NOT NULL,
    device_id TEXT NOT NULL
  );
`);
const insertMessage = db.prepare(
  "INSERT INTO messages(sender, body, received_at, device_id) VALUES (?, ?, ?, ?)"
);

const app = express();
app.use(helmet({ contentSecurityPolicy: false }));
app.use(express.json({ limit: "32kb" }));
app.use(express.static(path.join(__dirname, "public")));

function requireAdmin(req, res, next) {
  const token = (req.headers.authorization || "").replace(/^Bearer\s+/i, "");
  try {
    const payload = jwt.verify(token, JWT_SECRET);
    if (payload.role !== "admin") throw new Error("role");
    next();
  } catch {
    res.status(401).json({ error: "Login required" });
  }
}

app.post("/api/login", async (req, res) => {
  const password = String(req.body?.password || "");
  // Constant-time comparison against configured password hash is preferred;
  // timingSafeEqual is used here with equal-length buffers.
  const a = Buffer.from(password);
  const b = Buffer.from(ADMIN_PASSWORD);
  const crypto = require("crypto");
  const valid = a.length === b.length && crypto.timingSafeEqual(a, b);
  if (!valid) return res.status(401).json({ error: "Password salah" });
  const token = jwt.sign({ role: "admin" }, JWT_SECRET, { expiresIn: "8h" });
  res.json({ token });
});

app.post("/api/messages", (req, res) => {
  const token = (req.headers.authorization || "").replace(/^Bearer\s+/i, "");
  if (!token || token !== DEVICE_TOKEN) return res.status(401).json({ error: "Device unauthorized" });
  const sender = String(req.body?.sender || "").slice(0, 80);
  const body = String(req.body?.body || "").slice(0, 5000);
  const receivedAt = String(req.body?.receivedAt || new Date().toISOString()).slice(0, 40);
  const deviceId = String(req.body?.deviceId || "android-device").slice(0, 80);
  if (!sender || !body) return res.status(400).json({ error: "sender and body required" });
  const result = insertMessage.run(sender, body, receivedAt, deviceId);
  res.status(201).json({ ok: true, id: result.lastInsertRowid });
});

app.get("/api/messages", requireAdmin, (req, res) => {
  const rows = db.prepare("SELECT id, sender, body, received_at AS receivedAt, device_id AS deviceId FROM messages ORDER BY id DESC LIMIT 500").all();
  res.json(rows);
});

app.delete("/api/messages/:id", requireAdmin, (req, res) => {
  db.prepare("DELETE FROM messages WHERE id = ?").run(Number(req.params.id));
  res.json({ ok: true });
});

app.listen(Number(PORT), HOST, () => {
  console.log(`SMS Sync server listening on http://${HOST}:${PORT}`);
});
