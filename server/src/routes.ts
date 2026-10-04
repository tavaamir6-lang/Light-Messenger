import type { FastifyInstance } from "fastify";

export async function registerRoutes(app: FastifyInstance) {
  app.get("/health", async () => ({
    ok: true,
    service: "light-messenger",
    time: new Date().toISOString()
  }));

  app.get("/api/v1/config", async () => ({
    apiVersion: "v1",
    websocketPath: "/ws",
    features: {
      privateChat: true,
      groups: false,
      media: false,
      calls: false
    }
  }));

  app.post("/api/v1/auth/register", async () => ({
    status: "not_implemented",
    next: "auth service"
  }));

  app.post("/api/v1/auth/login", async () => ({
    status: "not_implemented",
    next: "auth service"
  }));
}
