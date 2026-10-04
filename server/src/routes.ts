import type { FastifyInstance } from "fastify";
import { z } from "zod";
import { db } from "./infra/postgres.js";
import { ACCESS_CODE, issueAccessToken } from "./services/auth.js";

const authSchema = z.object({
  name: z.string().trim().min(2).max(80),
  code: z.string().regex(/^\\d{4}$/, "code must be 4 digits")
});

const attempts = new Map<string, { count: number; resetAt: number }>();

function checkAttempts(ip: string): boolean {
  const now = Date.now();
  const current = attempts.get(ip);
  if (!current || current.resetAt <= now) {
    attempts.set(ip, { count: 1, resetAt: now + 10 * 60_000 });
    return true;
  }
  if (current.count >= 10) return false;
  current.count += 1;
  return true;
}

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

  app.post("/api/v1/auth/register", async (request, reply) => {
    if (!checkAttempts(request.ip)) {
      return reply.code(429).send({ error: "too_many_attempts" });
    }

    const parsed = authSchema.safeParse(request.body);
    if (!parsed.success) {
      return reply.code(400).send({ error: "invalid_name_or_code" });
    }

    const { name, code } = parsed.data;
    if (code !== ACCESS_CODE) {
      return reply.code(403).send({ error: "invalid_access_code" });
    }

    const existing = await db.query(
      "select id from users where lower(display_name) = lower($1) limit 1",
      [name]
    );
    if (existing.rowCount) {
      return reply.code(409).send({ error: "name_already_registered" });
    }

    const username = `user_${crypto.randomUUID().replaceAll("-", "").slice(0, 20)}`;
    const created = await db.query(
      "insert into users (username, display_name, password_hash) values ($1, $2, $3) returning id, display_name",
      [username, name, "access-code-only"]
    );
    const user = created.rows[0];

    return reply.code(201).send({
      user: { id: user.id, name: user.display_name },
      accessToken: issueAccessToken(user.id)
    });
  });

  app.post("/api/v1/auth/login", async (request, reply) => {
    if (!checkAttempts(request.ip)) {
      return reply.code(429).send({ error: "too_many_attempts" });
    }

    const parsed = authSchema.safeParse(request.body);
    if (!parsed.success) {
      return reply.code(400).send({ error: "invalid_name_or_code" });
    }

    const { name, code } = parsed.data;
    if (code !== ACCESS_CODE) {
      return reply.code(403).send({ error: "invalid_access_code" });
    }

    const result = await db.query(
      "select id, display_name from users where lower(display_name) = lower($1) limit 1",
      [name]
    );
    if (!result.rowCount) {
      return reply.code(404).send({ error: "account_not_found" });
    }

    const user = result.rows[0];
    return {
      user: { id: user.id, name: user.display_name },
      accessToken: issueAccessToken(user.id)
    };
  });
}
