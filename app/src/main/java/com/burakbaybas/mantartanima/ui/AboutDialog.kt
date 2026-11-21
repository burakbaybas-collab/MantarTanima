package com.burakbaybas.mantartanima.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Biotech
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.burakbaybas.mantartanima.R

const val DEVELOPER_EMAIL = "burakbaybas@gmail.com"

private val CardBlack = Color(0xFF0E0F0E)
private val Line = Color(0xFF2A2C2A)
private val Muted = Color(0xFFA8ACA8)
private val Accent = Color(0xFF5FB86A)

/** Geliştirici adına dokununca açılan "Hakkımda" penceresi. */
@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(CardBlack)
                .border(1.dp, Line, RoundedCornerShape(28.dp))
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Başlık
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Hakkımda",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Icon(Icons.Outlined.PersonOutline, null, tint = Muted, modifier = Modifier.size(26.dp))
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, "Kapat", tint = Muted)
                }
            }
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = Line)
            Spacer(Modifier.height(22.dp))

            // Vesikalık fotoğraf (3:4)
            val photoShape = RoundedCornerShape(14.dp)
            Image(
                painter = painterResource(R.drawable.about_photo),
                contentDescription = "Burak BAYBAŞ",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(132.dp)
                    .height(176.dp)
                    .clip(photoShape)
                    .border(3.dp, Accent.copy(alpha = 0.8f), photoShape),
            )
            Spacer(Modifier.height(16.dp))
            Text(DEVELOPER_NAME, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                "Mühendis · Mantar Araştırmacısı",
                color = Muted,
                fontSize = 17.sp,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(20.dp))
            HorizontalDivider(color = Line)
            Spacer(Modifier.height(18.dp))

            Text(
                "Mantar yetiştiriciliği, misel teknolojileri ve mantar genetik kaynakları üzerine araştırmalar yürütüyorum.",
                color = Color.White,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "FungiLens'i, mantar türlerinin yapay zekâ destekli tanımlanmasını kolaylaştırmak ve mikolojik bilgiyi daha erişilebilir kılmak amacıyla geliştiriyorum.",
                color = Color.White,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(18.dp))
            HorizontalDivider(color = Line)
            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Icon(Icons.Outlined.Biotech, null, tint = Accent, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Mikoloji · Biyoteknoloji · Yapay Zekâ",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(12.dp))
            LinkRow(Icons.Outlined.Email, DEVELOPER_EMAIL) { uriHandler.openUri("mailto:$DEVELOPER_EMAIL") }
            Spacer(Modifier.height(6.dp))
            LinkRow(Icons.Outlined.PhotoCamera, "Instagram: $INSTAGRAM_HANDLE") { uriHandler.openUri(INSTAGRAM_URL) }
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun LinkRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = Accent, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text,
            color = Color.White,
            fontSize = 16.sp,
            textDecoration = TextDecoration.Underline,
        )
        Spacer(Modifier.width(4.dp))
        Icon(Icons.AutoMirrored.Filled.OpenInNew, null, tint = Muted, modifier = Modifier.size(16.dp))
    }
}
