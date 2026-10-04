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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lightspeed.messenger.ui.theme.LightMessengerTheme

data class ChatPreview(val name: String, val message: String, val time: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LightMessengerTheme { Surface(Modifier.fillMaxSize()) { MessengerHome() } }
        }
    }
}

@Composable
private fun MessengerHome() {
    val chats = remember {
        listOf(
            ChatPreview("پشتیبانی", "به Light Messenger خوش آمدید", "اکنون"),
            ChatPreview("تست سرور", "اتصال WebSocket آماده است", "۱۲:۴۰"),
            ChatPreview("گروه دوستان", "پیام جدید دارید", "دیروز")
        )
    }
    var selected by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(title = {
                Text("Light Messenger", textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
            })
        }
    ) { padding ->
        if (selected == null) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp)
            ) {
                items(chats) { chat -> ChatRow(chat) { selected = chat.name } }
            }
        } else {
            ChatScreen(selected!!, onBack = { selected = null })
        }
    }
}

@Composable
private fun ChatRow(chat: ChatPreview, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(chat.name, style = MaterialTheme.typography.titleMedium)
                Text(chat.message, style = MaterialTheme.typography.bodyMedium)
            }
            Text(chat.time, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun ChatScreen(name: String, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onBack) { Text("بازگشت") }
        Text(name, style = MaterialTheme.typography.headlineSmall)
        Text("صفحهٔ پایه برای ViewModel، Repository و WebSocket.")
        Text("پیام واقعی، صف آفلاین، رسانه و وضعیت تحویل در مرحله بعد متصل می‌شوند.")
    }
}
