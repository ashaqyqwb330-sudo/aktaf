package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

/**
 * 🎨 بطاقة الخدمة الذهبية الأنيقة ذات الطابع الموحد للتطبيق
 */
@Composable
fun EktefaaBaseCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String = "",
    icon: ImageVector,
    iconColor: Color = WarmGold,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(1.dp, WarmGold.copy(0.2f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceWarm.copy(0.9f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(WarmGold.copy(0.12f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        color = TextMuted,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

/**
 * 🔒 حقل إدخال زجاجي أنيق مزود بأيقونة ذهبية
 */
@Composable
fun EktefaaGlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    isPassword: Boolean = false,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = Color.White.copy(0.5f), fontSize = 13.sp) },
        leadingIcon = { Icon(leadingIcon, null, tint = WarmGold, modifier = Modifier.size(20.dp)) },
        trailingIcon = trailingIcon,
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White.copy(alpha = 0.1f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.1f),
            focusedBorderColor = WarmGold,
            unfocusedBorderColor = WarmGold.copy(0.3f),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
        )
    )
}

/**
 * ⚡ زر مذهب فاخر يدعم تأثير النبض البصري والتفاعل
 */
@Composable
fun EktefaaGoldButton(
    modifier: Modifier = Modifier,
    text: String,
    pulse: Boolean = false,
    onClick: () -> Unit
) {
    val scaleFactor = if (pulse) {
        val infiniteTransition = rememberInfiniteTransition(label = "ButtonScaleTransition")
        val animScale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse"
        )
        animScale
    } else 1f

    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .scale(scaleFactor)
            .height(50.dp),
        colors = ButtonDefaults.buttonColors(containerColor = WarmGold),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = text,
            color = MidnightBlue,
            fontWeight = FontWeight.Black,
            fontSize = 15.sp,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * 🛠️ نافذة منبثقة موحدة بالتنسيق الفاخر لـ "اكتفاء"
 */
@Composable
fun EktefaaDialog(
    onDismiss: () -> Unit,
    title: String,
    onSave: () -> Unit,
    saveButtonText: String = "حفظ",
    content: @Composable ColumnScope.() -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onSave,
                colors = ButtonDefaults.buttonColors(containerColor = WarmGold),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(saveButtonText, color = MidnightBlue, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = Color.White.copy(0.6f))
            }
        },
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                color = WarmGold,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                content = content
            )
        },
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 6.dp,
        modifier = Modifier.border(1.5.dp, WarmGold.copy(0.4f), RoundedCornerShape(20.dp))
    )
}
