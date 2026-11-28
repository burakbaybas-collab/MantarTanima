package com.burakbaybas.mantartanima.ui

import android.net.Uri
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
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
import com.burakbaybas.mantartanima.PhotoAngle
import com.burakbaybas.mantartanima.R
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.shape.CircleShape
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import com.burakbaybas.mantartanima.data.SpeciesCatalog
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import android.graphics.BitmapFactory

private val ScanGreen = Color(0xFF5BE36B)
private val Deep = Color(0xFF17502A)
private val Fresh = Color(0xFF4E9A2F)

private val steps = listOf(
    "Fotoğraflar inceleniyor…",
    "Şapka, sap ve lameller karşılaştırılıyor…",
    "1000 türle karşılaştırılıyor…",
    "En olası türler sıralanıyor…",
)

/** "Tanımla"ya basınca gösterilen tarama ekranı: fotoğraflar sırayla taranır. */
@Composable
fun ScanScreen(photos: Map<PhotoAngle, Uri>, found: String? = null) {
    val context = LocalContext.current
    // Uygulamadaki 1000 türün örnek fotoğrafları hızla akar (aranıyor görüntüsü)
    val files = remember { runCatching { context.assets.list("examples")?.toList().orEmpty() }.getOrDefault(emptyList()).shuffled() }
    var frame by remember { mutableStateOf<Pair<ImageBitmap, String>?>(null) }
    LaunchedEffect(files, found) {
        if (found != null) {
            val f = files.filter { it.startsWith(found + "_") }.minOrNull()
            val bmp = f?.let { name ->
                withContext(Dispatchers.IO) {
                    runCatching { context.assets.open("examples/$name").use { BitmapFactory.decodeStream(it) } }.getOrNull()
                }
            }
            frame = bmp?.asImageBitmap()?.let { it to (SpeciesCatalog.find(found)?.latinName ?: "") } ?: frame
            return@LaunchedEffect
        }
        var k = 0
        while (files.isNotEmpty() && found == null) {
            val f = files[k % files.size]; k++
            val bmp = withContext(Dispatchers.IO) {
                runCatching { context.assets.open("examples/$f").use { BitmapFactory.decodeStream(it) } }.getOrNull()
            }
            if (bmp != null) {
                val id = f.substringBeforeLast('_')
                frame = bmp.asImageBitmap() to (SpeciesCatalog.find(id)?.latinName ?: "")
            }
            delay(75)
        }
    }
    var idx by remember { mutableIntStateOf(0) }
    var progress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(found) {
        if (found != null) { progress = 1f; idx = 3; return@LaunchedEffect }
        val total = 4000L
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
    val spin by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "halka")
    val pulse by inf.animateFloat(0.9f, 1.1f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "nabız")

    Box(Modifier.fillMaxSize()) {
        // Ana sayfadaki orman arka planı ve açık perde
        Image(painterResource(R.drawable.bg_forest), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(0f to Color(0xF2FBF8F2), 0.55f to Color(0xD9FBF8F2), 1f to Color(0x99FBF8F2))
            )
        )
        Column(
            Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painterResource(R.drawable.logo_mark), "FungiLens logosu",
                modifier = Modifier.size(84.dp).scale(pulse),
            )
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = Deep)) { append("Fungi") }
                    withStyle(SpanStyle(color = Fresh)) { append("Lens") }
                },
                fontSize = 30.sp, fontWeight = FontWeight.ExtraBold,
            )
            Spacer(Modifier.height(4.dp))
            Text("Tanımlanıyor", color = Deep, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(Modifier.height(16.dp))

            // Yuvarlak mercek: foto daire içinde akar, çevresinde dönen yeşil halka (logodaki büyüteç gibi)
            Box(Modifier.fillMaxWidth(0.72f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize().rotate(if (found != null) 0f else spin)) {
                    val w = 6.dp.toPx()
                    val inset = w / 2
                    val arcSize = Size(size.width - w, size.height - w)
                    if (found != null) {
                        drawCircle(Fresh, radius = size.minDimension / 2 - inset, style = Stroke(w))
                    } else {
                        drawCircle(Deep.copy(alpha = 0.12f), radius = size.minDimension / 2 - inset, style = Stroke(w))
                        drawArc(
                            Brush.sweepGradient(listOf(Color.Transparent, Fresh.copy(alpha = 0.6f), ScanGreen)),
                            startAngle = 0f, sweepAngle = 300f, useCenter = false,
                            topLeft = Offset(inset, inset), size = arcSize, style = Stroke(w, cap = StrokeCap.Round),
                        )
                    }
                }
                Box(
                    Modifier.fillMaxSize(0.9f).shadow(10.dp, CircleShape).clip(CircleShape).background(Color.Black)
                        .border(4.dp, Color.White, CircleShape),
                ) {
                    frame?.let { (img, _) ->
                        Image(img, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    }
                    if (found == null) {
                        Canvas(Modifier.fillMaxSize()) {
                            drawRect(Color(0x1A17502A))
                            val y = size.height * line
                            val glow = 60.dp.toPx()
                            drawRect(
                                Brush.verticalGradient(listOf(Color.Transparent, ScanGreen.copy(alpha = 0.35f)), startY = y - glow, endY = y),
                                topLeft = Offset(0f, y - glow), size = Size(size.width, glow),
                            )
                            drawLine(ScanGreen, Offset(0f, y), Offset(size.width, y), 3.dp.toPx())
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                frame?.second.orEmpty(), color = Deep, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold,
                fontSize = 15.sp, maxLines = 1, modifier = Modifier.height(22.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Taranan tür: ${if (found != null) 1000 else (progress / 0.97f * 1000).toInt().coerceIn(0, 1000)} / 1000",
                color = Deep.copy(alpha = 0.8f), fontSize = 14.sp, fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(20.dp))
            val foundSp = found?.let { SpeciesCatalog.find(it) }
            if (found != null) {
                Text(
                    buildAnnotatedString {
                        append("Bulundu: ")
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(foundSp?.latinName ?: found) }
                    },
                    color = Deep, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp,
                )
                foundSp?.turkishName?.takeIf { it.isNotBlank() }?.let {
                    Text(it, color = Deep.copy(alpha = 0.8f), fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
            } else Text(
                steps[idx.coerceIn(0, steps.lastIndex)],
                color = Deep, fontWeight = if (found != null) FontWeight.ExtraBold else FontWeight.SemiBold,
                fontSize = if (found != null) 18.sp else 16.sp,
                modifier = Modifier.alpha(0.95f))
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)),
                color = Fresh, trackColor = Deep.copy(alpha = 0.15f),
            )
        }
    }
}
