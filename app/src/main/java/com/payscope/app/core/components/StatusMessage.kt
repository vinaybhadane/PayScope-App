package com.payscope.app.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.payscope.app.core.theme.*

@Composable
fun StatusMessage(message: String, onRetry: () -> Unit) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp).fillMaxWidth()
        .background(ElevatedCardSurface, RoundedCornerShape(16.dp)).padding(16.dp)) {
        Text(message, color = WarningAmber, fontSize = 12.sp)
        TextButton(onClick = onRetry) { Text("Retry", color = SecondaryCyan) }
    }
}
