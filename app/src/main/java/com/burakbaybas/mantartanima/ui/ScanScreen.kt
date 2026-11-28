package com.burakbaybas.mantartanima.ui

import android.net.Uri
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.burakbaybas.mantartanima.PhotoAngle
import com.burakbaybas.mantartanima.R
import kotlinx.coroutines.delay

private val ScanGreen = Color(0xFF7CFC8A)
private val Deep = Color(0xFF17502A)

private val steps = listOf(
    "Şapka inceleniyor…",
    "Sap ve halka inceleniyor…",
    "Lameller / gözenekler inceleniyor…",
    "1000 türle karşılaştırılıyor…",
)

/** "Tanımla"ya basınca gösterilen tarama ekranı: fotoğraflar sırayla taranır. */
@Composable
fun ScanScreen(photos: Map<PhotoAngle, Uri>) {
    val list = PhotoAngle.entries.mapNotNull { a -> photos[a]?.let { a to it } }
    var idx by remember { mutableIntStateOf(0) }
    var progress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val total = 2600L
        val stepMs = 40L
        var t = 0L
        while (true) {
            delay(stepMs); t += stepMs
            progress = (t.toFloat() / total).coerceAtMost(0.97f)
            idx = ((t / (total / 4)).toInt()).coerceAtMost(3)
        }
    }
    val inf = rememberInfiniteTransition(label = "tarama")
    val line by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Reverse), label = "çizgi")
    val pulse by inf.animateFloat(0.9f, 1.1f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "nabız")

    Box(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0E2A17), Color(0xFF17502A)))),
    ) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painterResource(R.drawable.ic_mushroom), null,
                modifier = Modifier.size(64.dp).scale(pulse),
            )
            Spacer(Modifier.height(10.dp))
            Text("Tanımlanıyor", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp)
            Spacer(Modifier.height(20.dp))

            val photoIdx = idx.coerceAtMost(list.lastIndex).coerceAtLeast(0)
            Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(22.dp)).background(Color.Black)) {
                Crossfade(targetState = list.getOrNull(photoIdx), label = "foto") { item ->
                    if (item != null) {
                        AsyncImage(model = item.second, contentDescription = item.first.title,
                            contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    }
                }
                // Hafif yeşil örtü, ızgara, köşe çerçeveleri ve tarama çizgisi
                Canvas(Modifier.fillMaxSize()) {
                    drawRect(Color(0x2217502A))
                    val n = 8
                    for (i in 1 until n) {
                        val x = size.width * i / n; val y = size.height * i / n
                        drawLine(Color.White.copy(alpha = 0.08f), Offset(x, 0f), Offset(x, size.height), 1.dp.toPx())
                        drawLine(Color.White.copy(alpha = 0.08f), Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
                    }
                    val c = 34.dp.toPx(); val m = 14.dp.toPx(); val w = 4.dp.toPx()
                    listOf(
                        Offset(m, m) to Offset(1f, 1f), Offset(size.width - m, m) to Offset(-1f, 1f),
                        Offset(m, size.height - m) to Offset(1f, -1f), Offset(size.width - m, size.height - m) to Offset(-1f, -1f),
                    ).forEach { (p, d) ->
                        drawLine(ScanGreen, p, Offset(p.x + c * d.x, p.y), w, StrokeCap.Round)
                        drawLine(ScanGreen, p, Offset(p.x, p.y + c * d.y), w, StrokeCap.Round)
                    }
                    val y = size.height * line
                    val glow = 70.dp.toPx()
                    drawRect(
                        Brush.verticalGradient(listOf(Color.Transparent, ScanGreen.copy(alpha = 0.35f)), startY = y - glow, endY = y),
                        topLeft = Offset(0f, y - glow), size = Size(size.width, glow),
                    )
                    drawLine(ScanGreen, Offset(0f, y), Offset(size.width, y), 3.dp.toPx())
                }
                list.getOrNull(photoIdx)?.let { (angle, _) ->
                    Text(
                        angle.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp)
                            .clip(RoundedCornerShape(50)).background(Color(0x99000000))
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                list.forEachIndexed { i, _ ->
                    Box(Modifier.size(width = 26.dp, height = 5.dp).clip(RoundedCornerShape(50))
                        .background(if (i <= photoIdx) ScanGreen else Color.White.copy(alpha = 0.25f)))
                }
            }
            Spacer(Modifier.height(20.dp))
            Text(steps[idx.coerceIn(0, steps.lastIndex)], color = Color.White.copy(alpha = 0.9f), fontSize = 16.sp,
                modifier = Modifier.alpha(0.95f))
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)),
                color = ScanGreen, trackColor = Color.White.copy(alpha = 0.2f),
            )
        }
    }
}
