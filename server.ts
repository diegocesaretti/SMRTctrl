import express from "express";
import { createServer as createViteServer } from "vite";
import dgram from "dgram";
import path from "path";
import cors from "cors";

const app = express();
const PORT = 3000;
const UDP_SEND_PORT = 20910;
const UDP_RECV_PORT = 20911;

app.use(cors());
app.use(express.json());

// --- UDP Logic ---
const udpSocket = dgram.createSocket("udp4");

// Bind to receive port for incoming status updates
udpSocket.on("error", (err) => {
  console.error(`UDP Socket error:\n${err.stack}`);
});

udpSocket.on("message", (msg, rinfo) => {
  console.log(`UDP Received from ${rinfo.address}:${rinfo.port}: ${msg.toString("hex")}`);
  // We could use a global state or a map of devices here
});

try {
  udpSocket.bind(UDP_RECV_PORT, "0.0.0.0", () => {
    console.log(`UDP Socket listening on port ${UDP_RECV_PORT}`);
  });
} catch (e) {
  console.error("Could not bind UDP socket. It might already be in use.");
}

function hexToBytes(hex: string): Buffer {
  return Buffer.from(hex, "hex");
}

import fs from "fs";

// ... existing imports ...

// --- API Endpoints ---

app.get("/api/files", (req, res) => {
  const filePath = req.query.path as string;
  if (!filePath) return res.status(400).json({ error: "Missing path" });
  
  try {
    const absolutePath = path.join(process.cwd(), filePath);
    const content = fs.readFileSync(absolutePath, "utf-8");
    res.json({ content });
  } catch (err) {
    res.status(404).json({ error: "File not found" });
  }
});

app.get("/api/health", (req, res) => {
  res.json({ status: "ok" });
});

// Send a command to a specific IP
app.post("/api/command", (req, res) => {
  const { ip, hexCommand } = req.body;
  if (!ip || !hexCommand) {
    return res.status(400).json({ error: "Missing ip or hexCommand" });
  }

  const buffer = hexToBytes(hexCommand);
  udpSocket.send(buffer, UDP_SEND_PORT, ip, (err) => {
    if (err) {
      console.error("UDP Send Error:", err);
      return res.status(500).json({ error: "Failed to send UDP packet" });
    }
    res.json({ success: true });
  });
});

// Discovery (Simulated or actual if running locally)
app.get("/api/discover", (req, res) => {
  // In a real local environment, we'd send a broadcast packet
  // For Cloud Run, we'll return a message explaining the limitation
  res.json({ 
    message: "Discovery started. Note: Broadcast discovery only works on local networks.",
    devices: [] 
  });
});

async function startServer() {
  if (process.env.NODE_ENV !== "production") {
    const vite = await createViteServer({
      server: { middlewareMode: true },
      appType: "spa",
    });
    app.use(vite.middlewares);
  } else {
    const distPath = path.join(process.cwd(), "dist");
    app.use(express.static(distPath));
    app.get("*", (req, res) => {
      res.sendFile(path.join(distPath, "index.html"));
    });
  }

  app.listen(PORT, "0.0.0.0", () => {
    console.log(`Server running on http://localhost:${PORT}`);
  });
}

startServer();
