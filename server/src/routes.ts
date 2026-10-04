import type { FastifyInstance } from "fastify";
import { randomUUID } from "node:crypto";
import { z } from "zod";
import { db } from "./infra/postgres.js";
import { ACCESS_CODE, issueAccessToken, verifyAccessToken } from "./services/auth.js";

const authSchema = z.object({
  name: z.string().trim().min(2).max(80),
  code: z.string().regex(/^\d{4}$/, "code must be 4 digits")
});

async function ensurePrivateConversation(userA:string,userB:string):Promise<string>{
  const found=await db.query("select c.id from conversations c join conversation_members m1 on m1.conversation_id=c.id and m1.user_id=$1 join conversation_members m2 on m2.conversation_id=c.id and m2.user_id=$2 where c.kind='private' limit 1",[userA,userB]);
  if(found.rowCount)return found.rows[0].id;
  const c=await db.query("insert into conversations(kind) values('private') returning id",[]);
  await db.query("insert into conversation_members(conversation_id,user_id) values($1,$2),($1,$3)",[c.rows[0].id,userA,userB]);
  return c.rows[0].id;
}
function bearer(request:any):string{
  const value=request.headers.authorization??"";
  if(!value.startsWith("Bearer "))throw new Error("unauthorized");
  return verifyAccessToken(value.slice(7));
}

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

    const username = `user_${randomUUID().replaceAll("-", "").slice(0, 20)}`;
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

  app.get("/api/v1/users/search", async (request, reply) => {
    let userId:string; try { userId=bearer(request); } catch { return reply.code(401).send({error:"unauthorized"}); }
    const q=String((request.query as any)?.q??"").trim();
    if(!q) return {users:[]};
    const result=await db.query("select id,display_name as name from users where id<>$1 and lower(display_name) like lower($2) order by display_name limit 20",[userId,"%"+q+"%"]);
    return {users:result.rows};
  });

  app.post("/api/v1/conversations/private", async (request, reply) => {
    let userId:string; try { userId=bearer(request); } catch { return reply.code(401).send({error:"unauthorized"}); }
    const parsed=z.object({userId:z.string().uuid()}).safeParse(request.body);
    if(!parsed.success||parsed.data.userId===userId)return reply.code(400).send({error:"invalid_user"});
    const exists=await db.query("select id from users where id=$1",[parsed.data.userId]);
    if(!exists.rowCount)return reply.code(404).send({error:"user_not_found"});
    return {conversationId:await ensurePrivateConversation(userId,parsed.data.userId)};
  });

  app.get("/api/v1/conversations", async (request, reply) => {
    let userId:string; try { userId=bearer(request); } catch { return reply.code(401).send({error:"unauthorized"}); }
    const result=await db.query("select c.id as \"conversationId\",u.id as \"userId\",u.display_name as name from conversations c join conversation_members mine on mine.conversation_id=c.id and mine.user_id=$1 join conversation_members other on other.conversation_id=c.id and other.user_id<>$1 join users u on u.id=other.user_id where c.kind='private' order by c.created_at desc",[userId]);
    return {conversations:result.rows};
  });

  app.get("/api/v1/conversations/:id/messages", async (request, reply) => {
    let userId:string; try { userId=bearer(request); } catch { return reply.code(401).send({error:"unauthorized"}); }
    const conversationId=String((request.params as any).id);
    const member=await db.query("select 1 from conversation_members where conversation_id=$1 and user_id=$2",[conversationId,userId]);
    if(!member.rowCount)return reply.code(403).send({error:"forbidden"});
    const result=await db.query("select id,conversation_id as \"conversationId\",sender_id as \"senderId\",body,created_at as \"createdAt\" from messages where conversation_id=$1 and deleted_at is null order by created_at asc limit 200",[conversationId]);
    return {messages:result.rows};
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
