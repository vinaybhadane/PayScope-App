package com.payscope.app.core.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.payscope.app.R

@Composable
fun PayScopeLogo(modifier: Modifier = Modifier, size: Dp = 40.dp) {
    Image(painterResource(R.drawable.payscope_logo), "PayScope logo",
        modifier = modifier.size(size), contentScale = ContentScale.Fit)
}
