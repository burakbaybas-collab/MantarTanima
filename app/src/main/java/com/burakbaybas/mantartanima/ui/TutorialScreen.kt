package com.burakbaybas.mantartanima.ui

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.burakbaybas.mantartanima.data.Edibility
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/** İlk açılışta bir kez gösterilen kısa kullanım eğitimi (ana ekrandan tekrar açılabilir). */
object Tutorial {
    private const val PREFS = "ayarlar"
    private const val KEY = "egitim_goruldu"
    fun isSeen(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY, false)
    fun markSeen(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY, true).apply()
}

private val TGreen = Color(0xFF17502A)
private val TText = Color(0xFF33373D)
private val Accent = Color(0xFFFF8F00)

/**
 * Bir eğitim adımı. Koordinatlar 1080x2400 ekran görüntüsü piksel cinsindendir.
 * [crop]: gösterilecek bölge, [marks]: vurgulanan öğe ve okun başladığı nokta.
 */
private class TStep(
    val title: String,
    val text: String,
    val image: String? = null,
    val crop: Rect? = null,
    val marks: List<Pair<Rect, Offset>> = emptyList(),
    val extra: (@Composable () -> Unit)? = null,
)

private const val HOME = "home.jpg"
private const val RESULT = "result.jpg"

private fun mark(r: Rect, from: Offset) = listOf(r to from)

private val steps = listOf(
    TStep("1. Fotoğraf yükleyin",
        "Mantarın üç yüzü için üç kutu vardır: Üstten (şapka), Yandan (şapka ve sap), Alttan (lameller ya da gözenekler). \"Örnek\" resimler nasıl çekeceğinizi gösterir. \"Fotoğraf Yükle\"ye dokunup kamerayla çekin ya da galeriden seçin.",
        HOME, Rect(0f, 700f, 1080f, 1730f), mark(Rect(40f, 990f, 1040f, 1695f), Offset(880f, 770f))),
    TStep("2. İpuçları",
        "Sağdaki (i) düğmesi iyi fotoğraf çekme önerilerini açar.",
        HOME, Rect(0f, 760f, 1080f, 1400f), mark(Rect(40f, 805f, 1038f, 955f), Offset(900f, 1250f))),
    TStep("3. Tanımla",
        "Fotoğrafları ekleyince buna basın. Parantezdeki sayı eklenen fotoğraf sayısıdır. Tek fotoğraf da yeter ama üç açı daha doğru sonuç verir.",
        HOME, Rect(0f, 1400f, 1080f, 2270f), mark(Rect(118f, 1752f, 962f, 1925f), Offset(540f, 1480f))),
    TStep("4. Geçmiş ve yardım",
        "Geçmiş Tanımlamalar önceki sonuçlarınızı saklar; dışa aktarabilir ya da silebilirsiniz. \"Nasıl kullanılır?\" bu rehberi tekrar açar.",
        HOME, Rect(0f, 1400f, 1080f, 2270f),
        listOf(Rect(216f, 1956f, 864f, 2062f) to Offset(1010f, 1740f), Rect(376f, 2102f, 704f, 2174f) to Offset(120f, 1950f))),
    TStep("5. Sonuç",
        "Büyük eğik yazı türün Latince adı, altındaki Türkçe adıdır. Yüzde ve çubuk modelin ne kadar emin olduğunu gösterir. Oran düşükse \"emin değil\" uyarısı çıkar; mantar tanınamazsa \"Tanınamadı\" yazar.",
        RESULT, Rect(0f, 330f, 1080f, 1190f), mark(Rect(46f, 352f, 1034f, 772f), Offset(540f, 1080f))),
    TStep("6. Yenilebilirlik etiketi",
        "Etikete dokunursanız açıklaması açılır. Zehirli ve ölümcül türlerde simge yanıp söner.",
        RESULT, Rect(0f, 1200f, 1080f, 1720f), mark(Rect(86f, 1250f, 448f, 1358f), Offset(820f, 1290f)),
        extra = { EdibilityLegend() }),
    TStep("7. Tür bilgileri",
        "Aşağı kaydırınca türün tanımı, Temel Bilgiler (cins, sezon, yaşam alanı, spor izi), ayırt edici özellikler ve benzer türlerden farkı yazar. Zehirli türlerde kırmızı bir zehirlenme kutusu ile 112 ve 114 (Zehir Danışma) arama düğmeleri çıkar.",
        RESULT, Rect(0f, 1200f, 1080f, 2215f), mark(Rect(86f, 1370f, 996f, 2210f), Offset(860f, 1270f))),
    TStep("Unutmayın",
        "Mantar zehirlenmeleri ölümcül olabilir. Hiçbir mantarı yalnızca uygulamaya bakarak yemeyin; mutlaka bir uzmana gösterin.",
        extra = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Warning, null, tint = Color(0xFFB3261E), modifier = Modifier.size(40.dp))
                Spacer(Modifier.width(10.dp))
                Text("Şüphe varsa yemeyin.", fontWeight = FontWeight.Bold, color = Color(0xFFB3261E), fontSize = 17.sp)
            }
        }),
)

@Composable
fun TutorialScreen(onDone: () -> Unit) {
    val pager = rememberPagerState { steps.size }
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == steps.lastIndex

    Column(Modifier.fillMaxSize().background(Color(0xFFF7FAF5)).safeDrawingPadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${pager.currentPage + 1} / ${steps.size}", color = TGreen, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            if (!last) TextButton(onClick = onDone) { Text("Geç", color = TGreen, fontWeight = FontWeight.SemiBold) }
            else Spacer(Modifier.height(48.dp))
        }
        HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { i ->
            val s = steps[i]
            Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                Text(s.title, fontWeight = FontWeight.ExtraBold, fontSize = 23.sp, color = TGreen, lineHeight = 27.sp)
                Spacer(Modifier.height(10.dp))
                if (s.image != null && s.crop != null) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Shot(s.image, s.crop, s.marks)
                    }
                    Spacer(Modifier.height(10.dp))
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        Text(s.text, fontSize = 15.sp, color = TText, lineHeight = 21.sp)
                        s.extra?.let { Spacer(Modifier.height(8.dp)); it() }
                    }
                } else {
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                        Spacer(Modifier.height(8.dp))
                        Text(s.text, fontSize = 16.sp, color = TText, lineHeight = 23.sp)
                        s.extra?.let { Spacer(Modifier.height(16.dp)); it() }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (pager.currentPage > 0) {
                TextButton(onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } }) {
                    Text("Geri", color = TGreen, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = { if (last) onDone() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                colors = ButtonDefaults.buttonColors(containerColor = TGreen),
            ) { Text(if (last) "Başla" else "İleri", fontWeight = FontWeight.Bold) }
        }
    }
}

/** Ekran görüntüsünün bir bölgesini gösterir; [hl] dışını karartır, çerçeveler ve oka işaret eder. */
@Composable
private fun Shot(name: String, crop: Rect, marks: List<Pair<Rect, Offset>>) {
    val context = LocalContext.current
    val img: ImageBitmap? = remember(name) {
        runCatching { context.assets.open("tutorial/$name").use { BitmapFactory.decodeStream(it).asImageBitmap() } }.getOrNull()
    }
    val pulse by rememberInfiniteTransition(label = "vurgu").animateFloat(
        0.45f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "vurgu",
    )
    Canvas(
        Modifier
            .aspectRatio(crop.width / crop.height, matchHeightConstraintsFirst = true)
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, Color(0xFFC5D3C0), RoundedCornerShape(14.dp)),
    ) {
        if (img == null) return@Canvas
        val k = img.width / 1080f
        drawImage(
            img,
            srcOffset = IntOffset((crop.left * k).toInt(), (crop.top * k).toInt()),
            srcSize = IntSize((crop.width * k).toInt(), (crop.height * k).toInt()),
            dstSize = IntSize(size.width.toInt(), size.height.toInt()),
        )
        if (marks.isEmpty()) return@Canvas
        val sc = size.width / crop.width
        fun map(o: Offset) = Offset((o.x - crop.left) * sc, (o.y - crop.top) * sc)
        val corner = CornerRadius(14.dp.toPx())
        val rects = marks.map { Rect(map(it.first.topLeft), map(it.first.bottomRight)) }
        val dim = Path().apply {
            fillType = PathFillType.EvenOdd
            addRect(Rect(Offset.Zero, size))
            rects.forEach { addRoundRect(RoundRect(it, corner)) }
        }
        drawPath(dim, Color.Black.copy(alpha = 0.55f))
        rects.forEachIndexed { i, r ->
            drawRoundRect(Accent.copy(alpha = pulse), r.topLeft, Size(r.width, r.height), corner, style = Stroke(4.dp.toPx()))
            drawArrow(map(marks[i].second), r)
        }
    }
}

/** [start] noktasından vurgulanan kutunun kenarına kıvrık ok çizer. */
private fun DrawScope.drawArrow(start: Offset, target: Rect) {
    val gap = 6.dp.toPx()
    val inset = 12.dp.toPx()
    val px = start.x.coerceIn(target.left + inset, target.right - inset)
    val py = start.y.coerceIn(target.top, target.bottom)
    val end = when {
        start.y > target.bottom -> Offset(px, target.bottom + gap)
        start.y < target.top -> Offset(px, target.top - gap)
        start.x > target.right -> Offset(target.right + gap, py)
        else -> Offset(target.left - gap, py)
    }
    val dx = end.x - start.x
    val dy = end.y - start.y
    val len = hypot(dx, dy).coerceAtLeast(1f)
    val bend = 0.2f
    val ctrl = Offset((start.x + end.x) / 2 - dy / len * len * bend, (start.y + end.y) / 2 + dx / len * len * bend)
    val path = Path().apply { moveTo(start.x, start.y); quadraticBezierTo(ctrl.x, ctrl.y, end.x, end.y) }
    val ang = atan2(end.y - ctrl.y, end.x - ctrl.x)
    val h = 16.dp.toPx()
    val head = Path().apply {
        moveTo(end.x - h * cos(ang - 0.45f), end.y - h * sin(ang - 0.45f))
        lineTo(end.x, end.y)
        lineTo(end.x - h * cos(ang + 0.45f), end.y - h * sin(ang + 0.45f))
    }
    for ((c, w) in listOf(Color.White to 8.dp.toPx(), Accent to 4.5.dp.toPx())) {
        drawPath(path, c, style = Stroke(w, cap = StrokeCap.Round))
        drawPath(head, c, style = Stroke(w, cap = StrokeCap.Round))
    }
    drawCircle(Color.White, 8.dp.toPx(), start)
    drawCircle(Accent, 5.5.dp.toPx(), start)
}

@Composable
private fun EdibilityLegend() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf(Edibility.EDIBLE, Edibility.EDIBLE_WITH_CAUTION, Edibility.INEDIBLE, Edibility.POISONOUS, Edibility.DEADLY).forEach { e ->
            val s = e.style
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    Modifier.width(150.dp).background(s.background, RoundedCornerShape(50))
                        .border(1.dp, s.border, RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    EdibilityIcon(e, 18.dp, blink = true)
                    Spacer(Modifier.width(6.dp))
                    Text(s.label, color = s.color, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    when (e) {
                        Edibility.EDIBLE -> "Yenebilir kabul edilir."
                        Edibility.EDIBLE_WITH_CAUTION -> "Pişirme, yaş gibi koşullarla."
                        Edibility.INEDIBLE -> "Zehirsiz olabilir, yenmez."
                        Edibility.POISONOUS -> "Zehirlenme yapar."
                        Edibility.DEADLY -> "Öldürücü olabilir."
                    },
                    color = TText, fontSize = 13.sp,
                )
            }
        }
    }
}

@Composable
private fun FakeCall(label: String) {
    Row(
        Modifier.clip(RoundedCornerShape(50)).border(1.dp, Color(0xFF8C1D18), RoundedCornerShape(50))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Call, null, tint = Color(0xFF8C1D18), modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = Color(0xFF8C1D18), fontWeight = FontWeight.SemiBold)
    }
}
