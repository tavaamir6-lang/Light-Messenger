package com.lightspeed.messenger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.lightspeed.messenger.network.MessengerApi
import com.lightspeed.messenger.ui.theme.LightMessengerTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val api = MessengerApi()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LightMessengerTheme {
                Surface(Modifier.fillMaxSize()) {
                    AuthScreen(api)
                }
            }
        }
    }

    override fun onDestroy() {
        api.close()
        super.onDestroy()
    }
}

@Composable
private fun AuthScreen(api: MessengerApi) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var loggedIn by remember { mutableStateOf(false) }

    if (loggedIn) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text("Light Messenger", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(12.dp))
            Text("خوش آمدی، $name")
            Spacer(Modifier.height(24.dp))
            Text("حساب با موفقیت وارد شد. مرحله بعد اتصال چت واقعی است.")
        }
        return
    }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Light Messenger", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(8.dp))
        Text("برای ورود یا ساخت حساب، نام و کد دسترسی را وارد کنید.")
        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("نام") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = code,
            onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) code = it },
            label = { Text("کد دسترسی") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                enabled = !loading && name.trim().length >= 2 && code.length == 4,
                onClick = {
                    loading = true
                    message = ""
                    scope.launch {
                        try {
                            api.login(name.trim(), code)
                            loggedIn = true
                        } catch (_: Exception) {
                            message = "ورود انجام نشد؛ نام یا کد را بررسی کنید."
                        } finally {
                            loading = false
                        }
                    }
                }
            ) { Text(if (loading) "..." else "ورود") }

            OutlinedButton(
                enabled = !loading && name.trim().length >= 2 && code.length == 4,
                onClick = {
                    loading = true
                    message = ""
                    scope.launch {
                        try {
                            api.register(name.trim(), code)
                            loggedIn = true
                        } catch (_: Exception) {
                            message = "ساخت حساب انجام نشد؛ کد دسترسی یا نام را بررسی کنید."
                        } finally {
                            loading = false
                        }
                    }
                }
            ) { Text("ساخت حساب") }
        }

        if (message.isNotBlank()) {
            Spacer(Modifier.height(16.dp))
            Text(message, color = MaterialTheme.colorScheme.error)
        }
    }
}
