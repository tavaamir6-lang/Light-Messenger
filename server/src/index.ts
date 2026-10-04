import Fastify from "fastify";
import cors from "@fastify/cors";
import { WebSocketServer } from "ws";
import { registerRoutes } from "./routes.js";

const app = Fastify({ logger: true });

await app.register(cors, { origin: process.env.CORS_ORIGIN ?? true });
await registerRoutes(app);

const port = Number(process.env.PORT ?? 8080);
await app.listen({ host: "0.0.0.0", port });

const wss = new WebSocketServer({ server: app.server, path: "/ws" });

wss.on("connection", (socket) => {
  socket.send(JSON.stringify({
    type: "ready",
    service: "light-messenger",
    version: "0.1.0"
  }));

  socket.on("message", (raw) => {
    socket.send(JSON.stringify({
      type: "ack",
      receivedAt: Date.now(),
      payload: raw.toString()
    }));
  });
});

app.log.info(`Light Messenger server listening on :${port}`);
