import Fastify from "fastify";
import cors from "@fastify/cors";
import { WebSocketServer, WebSocket } from "ws";
import { registerRoutes } from "./routes.js";
import { db } from "./infra/postgres.js";
import { verifyAccessToken } from "./services/auth.js";

const app=Fastify({logger:true});
await app.register(cors,{origin:process.env.CORS_ORIGIN??true});
await registerRoutes(app);
const port=Number(process.env.PORT??8080);
await app.listen({host:"0.0.0.0",port});

const wss=new WebSocketServer({server:app.server,path:"/ws"});
const sockets=new Map<string,Set<WebSocket>>();

function addSocket(userId:string,socket:WebSocket){let set=sockets.get(userId);if(!set){set=new Set();sockets.set(userId,set);}set.add(socket);}
function removeSocket(userId:string,socket:WebSocket){const set=sockets.get(userId);if(!set)return;set.delete(socket);if(!set.size)sockets.delete(userId);}
function sendUser(userId:string,payload:unknown){const data=JSON.stringify(payload),set=sockets.get(userId);if(!set)return;for(const s of set)if(s.readyState===WebSocket.OPEN)s.send(data);}

wss.on("connection",(socket,request)=>{
  try{
    const url=new URL(request.url??"/ws","http://localhost");
    const token=url.searchParams.get("token"); if(!token)throw new Error("missing_token");
    const userId=verifyAccessToken(token); addSocket(userId,socket);
    socket.send(JSON.stringify({type:"ready"}));
    socket.on("message",async raw=>{
      try{
        const msg=JSON.parse(raw.toString());
        if(msg.type!=="message.send")return;
        const body=String(msg.body??"").trim(),conversationId=String(msg.conversationId??""),clientMessageId=String(msg.clientMessageId??"");
        if(!body||!conversationId||!clientMessageId||body.length>4000)return;
        const member=await db.query("select user_id from conversation_members where conversation_id=$1 and user_id<>$2",[conversationId,userId]);
        if(!member.rowCount){socket.send(JSON.stringify({type:"message.error",clientMessageId,error:"forbidden"}));return;}
        const existing=await db.query("select id,conversation_id as "conversationId",sender_id as "senderId",body,created_at as "createdAt" from messages where sender_id=$1 and client_message_id=$2 limit 1",[userId,clientMessageId]);
        let saved=existing.rowCount?existing.rows[0]:null;
        if(!saved){
          const result=await db.query("insert into messages(conversation_id,sender_id,client_message_id,body) values($1,$2,$3,$4) returning id,conversation_id as "conversationId",sender_id as "senderId",body,created_at as "createdAt"",[conversationId,userId,clientMessageId,body]);
          saved=result.rows[0];
        }
        socket.send(JSON.stringify({type:"message.sent",message:saved}));
        for(const recipient of member.rows)sendUser(recipient.user_id,{type:"message.new",message:saved});
      }catch{socket.send(JSON.stringify({type:"message.error",error:"invalid_message"}));}
    });
    socket.on("close",()=>removeSocket(userId,socket));
  }catch{socket.close(1008,"unauthorized");}
});
app.log.info(`Light Messenger server listening on :${port}`);
