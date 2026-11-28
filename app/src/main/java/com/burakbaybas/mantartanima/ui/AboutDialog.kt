package com.burakbaybas.mantartanima.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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

private val Ink = Color(0xFF1E211E)
private val Body = Color(0xFF2F332F)
private val Green = Color(0xFF3F7A2E)
private val GreenBorder = Color(0xFF6AA84F)
private val GreenLine = Color(0xFF9CC48A)
private val PaleGreen = Color(0xFFE6F0DC)
private val OutlineGreen = Color(0xFFCFE2C3)

/** Geliştirici adına dokununca açılan "Hakkımda" penceresi. */
@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    val cardShape = RoundedCornerShape(30.dp)
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .fillMaxWidth()
                .shadow(16.dp, cardShape, spotColor = Green.copy(alpha = 0.35f))
                .clip(cardShape)
                .background(Color.White)
                .border(1.dp, OutlineGreen, cardShape),
        ) {
            // Köşe yaprağı
            Image(
                painter = painterResource(R.drawable.deco_leaf),
                contentDescription = null,
                modifier = Modifier.size(width = 38.dp, height = 56.dp).align(Alignment.TopStart),
            )
            // Alt süsleme (mantarlar ve çimen)
            Image(
                painter = painterResource(R.drawable.deco_bottom),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
            )

            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(start = 22.dp, end = 18.dp, top = 12.dp, bottom = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Hakkımda",
                        color = Ink,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.weight(1f).padding(start = 10.dp),
                    )
                    Icon(Icons.Outlined.PersonOutline, null, tint = Green, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(12.dp))
                    Box(Modifier.width(1.dp).height(30.dp).background(OutlineGreen))
                    Box(
                        Modifier
                            .padding(start = 4.dp)
                            .size(40.dp)
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Close, "Kapat", tint = Green, modifier = Modifier.size(26.dp))
                    }
                }
                Spacer(Modifier.height(4.dp))
                AccentDivider()
                Spacer(Modifier.height(12.dp))

                val photoShape = RoundedCornerShape(22.dp)
                Image(
                    painter = painterResource(R.drawable.about_photo),
                    contentDescription = DEVELOPER_NAME,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(112.dp)
                        .height(140.dp)
                        .shadow(6.dp, photoShape)
                        .clip(photoShape)
                        .border(4.dp, GreenBorder, photoShape),
                )
                Spacer(Modifier.height(8.dp))
                Text(DEVELOPER_NAME, color = Ink, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Mühendis · Mantar Araştırmacısı",
                    color = Ink,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .background(PaleGreen, RoundedCornerShape(50))
                        .padding(horizontal = 18.dp, vertical = 4.dp),
                )

                Spacer(Modifier.height(10.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(GreenLine))
                Spacer(Modifier.height(8.dp))

                Paragraph("T.C. Tarım ve Orman Bakanlığı'na bağlı Tarımsal Araştırmalar ve Politikalar Genel Müdürlüğü'nün bünyesinde bulunan Atatürk Bahçe Kültürleri Merkez Araştırma Enstitüsü'nde mantar yetiştiriciliği, misel teknolojileri ve mantar genetik kaynakları üzerine araştırmalar yürütüyorum.")
                Spacer(Modifier.height(6.dp))
                Paragraph("FungiLens'i, tamamen ücretsiz olarak mantar türlerinin yapay zekâ destekli tanımlanmasını kolaylaştırmak ve mikolojik bilgiyi daha erişilebilir kılmak amacıyla geliştiriyorum.")

                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(GreenLine))
                Spacer(Modifier.height(10.dp))

                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(PaleGreen, RoundedCornerShape(18.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_mushroom),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.width(1.dp).height(24.dp).background(GreenLine))
                    Spacer(Modifier.width(8.dp))
                    // Tek satıra sığana kadar yazı boyutunu küçült
                    var expertFont by remember { mutableStateOf(14.sp) }
                    Text(
                        "Mikoloji · Biyoteknoloji · Yapay Zekâ",
                        color = Ink,
                        fontSize = expertFont,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        onTextLayout = { r ->
                            if (r.hasVisualOverflow && expertFont.value > 10f) expertFont = (expertFont.value - 0.5f).sp
                        },
                    )
                }
                Spacer(Modifier.height(8.dp))
                LinkBox(Icons.Outlined.Email, DEVELOPER_EMAIL) { uriHandler.openUri("mailto:$DEVELOPER_EMAIL") }
                Spacer(Modifier.height(8.dp))
                LinkBox(Icons.Outlined.PhotoCamera, "Instagram: $INSTAGRAM_HANDLE") { uriHandler.openUri(INSTAGRAM_URL) }
                Spacer(Modifier.height(8.dp))
                var showCredits by remember { mutableStateOf(false) }
                LinkBox(Icons.Outlined.PhotoLibrary, "Fotoğraf kaynakları ve lisanslar") { showCredits = true }
                if (showCredits) PhotoCreditsDialog { showCredits = false }
            }
        }
    }
}

@Composable
private fun AccentDivider() {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(GreenLine))
        Box(Modifier.width(56.dp).height(4.dp).background(GreenBorder, RoundedCornerShape(50)))
    }
}

@Composable
private fun Paragraph(text: String) {
    Text(
        text,
        color = Body,
        fontSize = 14.sp,
        lineHeight = 19.sp,
        textAlign = TextAlign.Justify,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
    )
}

@Composable
private fun LinkBox(icon: ImageVector, text: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White)
            .border(1.5.dp, OutlineGreen, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = Green, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Text(
            text,
            color = Body,
            fontSize = 14.sp,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier.weight(1f),
        )
        Icon(Icons.AutoMirrored.Filled.OpenInNew, null, tint = Green, modifier = Modifier.size(20.dp))
    }
}
