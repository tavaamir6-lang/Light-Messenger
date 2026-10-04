package com.lightspeed.messenger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lightspeed.messenger.network.*
import com.lightspeed.messenger.ui.theme.LightMessengerTheme
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import java.util.UUID

class MainActivity:ComponentActivity(){
    private val api=MessengerApi()
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContent{LightMessengerTheme{Surface(Modifier.fillMaxSize()){MessengerApp(api)}}}}
    override fun onDestroy(){api.close();super.onDestroy()}
}

@Composable
private fun MessengerApp(api:MessengerApi){
    var token by remember{mutableStateOf<String?>(null)}
    var me by remember{mutableStateOf<AuthUser?>(null)}
    var error by remember{mutableStateOf("")}
    if(token==null) AuthScreen(api,{u,t->{me=u;token=t;error=""}},{error=it})
    else ChatHome(api,token!!,me!!,{token=null;me=null},{error=it},error)
}

@Composable
private fun AuthScreen(api:MessengerApi,onSuccess:(AuthUser,String)->Unit,onError:(String)->Unit){
    val scope=rememberCoroutineScope();var name by remember{mutableStateOf("")};var code by remember{mutableStateOf("")};var loading by remember{mutableStateOf(false)}
    Column(Modifier.fillMaxSize().padding(24.dp),verticalArrangement=Arrangement.Center){
        Text("Light Messenger",style=MaterialTheme.typography.headlineLarge)
        Text("نام و کد دسترسی را وارد کنید.")
        Spacer(Modifier.height(18.dp))
        OutlinedTextField(name,{name=it},label={Text("نام")},singleLine=true,modifier=Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(code,{if(it.length<=4&&it.all(Char::isDigit))code=it},label={Text("کد دسترسی")},singleLine=true,modifier=Modifier.fillMaxWidth())
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){
            Button(enabled=!loading&&name.trim().length>=2&&code.length==4,onClick={loading=true;scope.launch{try{val r=api.login(name.trim(),code);onSuccess(r.user,r.accessToken)}catch(_:Exception){onError("ورود ناموفق بود.")}finally{loading=false}}}){Text("ورود")}
            OutlinedButton(enabled=!loading&&name.trim().length>=2&&code.length==4,onClick={loading=true;scope.launch{try{val r=api.register(name.trim(),code);onSuccess(r.user,r.accessToken)}catch(_:Exception){onError("ساخت حساب ناموفق بود.")}finally{loading=false}}}){Text("ثبت نام")}
        }
    }
}

@Composable
private fun ChatHome(api:MessengerApi,token:String,me:AuthUser,onLogout:()->Unit,onError:(String)->Unit,error:String){
    val scope=rememberCoroutineScope()
    var conversations by remember{mutableStateOf(emptyList<ConversationItem>())}
    var selected by remember{mutableStateOf<ConversationItem?>(null)}
    var socket by remember{mutableStateOf<WebSocketSession?>(null)}
    var incoming by remember{mutableStateOf(emptyMap<String,List<ChatMessage>>())}
    LaunchedEffect(token){
        try{
            conversations=api.conversations(token)
            val s=api.socket(token);socket=s
            s.incoming.consumeAsFlow().collect{frame->
                if(frame is Frame.Text){
                    val obj=Json.parseToJsonElement(frame.readText()).jsonObject
                    val type=obj["type"]?.toString()?.trim('"')
                    if(type=="message.new"||type=="message.sent"){
                        val element=obj["message"] ?: return@collect
                        val msg=Json.decodeFromJsonElement<ChatMessage>(element)
                        incoming=incoming.toMutableMap().apply{put(msg.conversationId,(get(msg.conversationId).orEmpty()+msg).distinctBy{it.id})}
                    }
                }
            }
        }catch(_:Exception){onError("اتصال پیام‌رسان برقرار نشد.")}
    }
    if(selected!=null){
        ChatScreen(api,token,me,selected!!,socket,incoming[selected!!.conversationId].orEmpty(),onBack={selected=null},onError=onError)
        return
    }
    var search by remember{mutableStateOf("")}
    var results by remember{mutableStateOf(emptyList<UserItem>())}
    LaunchedEffect(search){
        if(search.trim().length>=2)results=runCatching{api.searchUsers(token,search.trim())}.getOrDefault(emptyList()) else results=emptyList()
    }
    Column(Modifier.fillMaxSize().padding(16.dp)){
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("Light Messenger",style=MaterialTheme.typography.headlineSmall);TextButton(onClick=onLogout){Text("خروج")}}
        OutlinedTextField(search,{search=it},label={Text("جستجوی کاربر")},singleLine=true,modifier=Modifier.fillMaxWidth())
        LazyColumn(Modifier.heightIn(max=180.dp)){
            items(results){u->
                ListItem(headlineContent={Text(u.name)},trailingContent={TextButton(onClick={scope.launch{try{val id=api.createPrivate(token,u.id);selected=ConversationItem(id,u.id,u.name);conversations=api.conversations(token)}catch(_:Exception){onError("شروع گفتگو ناموفق بود.")}}}){Text("چت")}})
            }
        }
        Text("گفتگوها",style=MaterialTheme.typography.titleMedium,modifier=Modifier.padding(vertical=10.dp))
        LazyColumn{
            items(conversations){c->ListItem(headlineContent={Text(c.name)},supportingContent={Text("گفتگوی خصوصی")},trailingContent={TextButton(onClick={selected=c}){Text("باز کردن")}})}
        }
        if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun ChatScreen(api:MessengerApi,token:String,me:AuthUser,conversation:ConversationItem,socket:WebSocketSession?,liveMessages:List<ChatMessage>,onBack:()->Unit,onError:(String)->Unit){
    val scope=rememberCoroutineScope();var messages by remember{mutableStateOf(emptyList<ChatMessage>())};var text by remember{mutableStateOf("")}
    LaunchedEffect(conversation.conversationId){messages=runCatching{api.messages(token,conversation.conversationId)}.getOrDefault(emptyList())}
    LaunchedEffect(liveMessages){messages=(messages+liveMessages).distinctBy{it.id}.sortedBy{it.createdAt}}
    Column(Modifier.fillMaxSize().padding(12.dp)){
        TextButton(onClick=onBack){Text("← " + conversation.name)}
        LazyColumn(Modifier.weight(1f)){
            items(messages){m->Text((if(m.senderId==me.id)"شما" else conversation.name)+": "+m.body,modifier=Modifier.padding(8.dp))}
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
            OutlinedTextField(text,{text=it},modifier=Modifier.weight(1f),singleLine=true,placeholder={Text("پیام...")})
            Button(enabled=text.isNotBlank()&&socket!=null,onClick={
                val body=text.trim();text=""
                scope.launch{try{
                    val payload=buildJsonObject{
                        put("type",JsonPrimitive("message.send"))
                        put("conversationId",JsonPrimitive(conversation.conversationId))
                        put("clientMessageId",JsonPrimitive(UUID.randomUUID().toString()))
                        put("body",JsonPrimitive(body))
                    }.toString()
                    socket?.send(Frame.Text(payload))
                }catch(_:Exception){onError("ارسال پیام ناموفق بود.")}}
            }){Text("ارسال")}
        }
    }
}
