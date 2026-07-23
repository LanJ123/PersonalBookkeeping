package com.personalbookkeeping.ui.security

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun LockedScreen(ready: Boolean, message: String?, onUnlock: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .semantics { contentDescription = if (ready) "应用已锁定" else "正在准备本地数据" },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("个人记账", style = MaterialTheme.typography.headlineMedium)
        if (!ready) {
            CircularProgressIndicator(Modifier.padding(top = 24.dp))
        } else {
            Text("财务内容已隐藏", modifier = Modifier.padding(top = 12.dp))
            Button(onClick = onUnlock, modifier = Modifier.padding(top = 24.dp)) { Text("系统认证解锁") }
            message?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp))
            }
        }
    }
}
