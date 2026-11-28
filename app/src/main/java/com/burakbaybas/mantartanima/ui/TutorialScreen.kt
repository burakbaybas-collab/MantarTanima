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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.onSizeChanged
import kotlinx.coroutines.delay
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
private val Accent = Color(0xFFFF8F00)

/** Ekran görüntüsündeki bir öğe (1080x2400 piksel koordinatı) ve açıklaması. */
private class Mark(val r: Rect, val text: String)

private class TPage(val title: String, val image: String, val marks: List<Mark>)

private val CROP = Rect(0f, 130f, 1080f, 2268f)  // durum çubuğu ve alt gezinme çubuğu hariç

private val pages = listOf(
    TPage("Ana sayfa", "home.jpg", listOf(
        Mark(Rect(45f, 788f, 1035f, 940f), "Ne yapacağınızı hatırlatır. Sağdaki (i) düğmesi iyi fotoğraf çekme ipuçlarını açar."),
        Mark(Rect(45f, 975f, 1035f, 1678f), "Mantarın üstten, yandan ve alttan fotoğrafını ekleyin. Kutuya dokunup kamerayla çekin ya da galeriden seçin."),
        Mark(Rect(62f, 995f, 342f, 1276f), "Yeşil tik fotoğrafın eklendiğini gösterir. Köşedeki X fotoğrafı kaldırır."),
        Mark(Rect(60f, 1458f, 344f, 1662f), "\"Değiştir\" ile fotoğrafı yenisiyle değiştirin."),
        Mark(Rect(118f, 1720f, 962f, 1892f), "Üç fotoğraf eklenince \"Tanımla\"ya basın. Uygulama 1000 türle karşılaştırır."),
        Mark(Rect(220f, 1926f, 860f, 2024f), "Önceki tanımlamalarınız fotoğraf, tarih ve konumla saklanır; dışa aktarabilir ya da silebilirsiniz."),
        Mark(Rect(300f, 2058f, 780f, 2205f), "\"Nasıl kullanılır?\" bu rehberi açar. Geliştirici adına dokununca uygulama hakkındaki bilgiler açılır."),
    )),
    TPage("Sonuç", "sonuc1.jpg", listOf(
        Mark(Rect(80f, 395f, 740f, 550f), "En olası tür: büyük yazı Latince adı, altındaki Türkçe adı."),
        Mark(Rect(820f, 420f, 1005f, 615f), "Yüzde ve çubuk modelin ne kadar emin olduğunu gösterir. Düşükse \"emin değil\" uyarısı çıkar."),
        Mark(Rect(40f, 675f, 1040f, 1050f), "Tanımlamada kullanılan sizin fotoğraflarınız."),
        Mark(Rect(88f, 1128f, 572f, 1230f), "Yenilebilirlik etiketi. Dokununca tüm kategorilerin açıklaması açılır. Zehirli türlerde simge yanıp söner."),
        Mark(Rect(88f, 1645f, 992f, 1955f), "Zehirli türlerde zehirlilik özeti."),
        Mark(Rect(88f, 1978f, 992f, 2265f), "Zehirlenme kutusu: zehir, belirtilerin başlama süresi, ilk yardım ve 112 / 114 arama düğmeleri."),
    )),
    TPage("Tür bilgileri", "sonuc2.jpg", listOf(
        Mark(Rect(85f, 320f, 995f, 835f), "Türü tanımaya yarayan özellikler; elinizdeki mantarla karşılaştırın."),
        Mark(Rect(88f, 868f, 992f, 1518f), "Bu türle karıştırılabilecek türler. Altı çizili adlara dokununca o türün fotoğrafları açılır."),
        Mark(Rect(88f, 1550f, 992f, 2265f), "Temel Bilgiler: alemden cinse sınıflandırma, sezon, yaşam alanı, spor izi, koku, tat ve renk değişimi."),
    )),
    TPage("Örnekler ve uyarı", "sonuc3.jpg", listOf(
        Mark(Rect(42f, 1155f, 1038f, 1772f), "Bu türün örnek fotoğrafları. Dokununca büyür; fotoğrafçının adı ve lisansı yazar."),
        Mark(Rect(42f, 1800f, 1038f, 2030f), "Unutmayın: hiçbir mantarı yalnızca uygulamaya bakarak yemeyin, mutlaka uzmana danışın."),
        Mark(Rect(42f, 2058f, 1038f, 2228f), "\"Fotoğrafları değiştir\" aynı mantarı yeniden dener; \"Yeni tanıma\" baştan başlar."),
    )),
)

@Composable
fun TutorialScreen(onDone: () -> Unit) {
    val pager = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == pages.lastIndex

    Column(Modifier.fillMaxSize().background(Color(0xFF1B1F1C)).safeDrawingPadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${pager.currentPage + 1}/${pages.size}  ${pages[pager.currentPage].title}", color = Color.White,
                fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
            if (!last) TextButton(onClick = onDone) { Text("Geç", color = Color.White) }
            else Spacer(Modifier.height(48.dp))
        }
        HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { i ->
            Box(Modifier.fillMaxSize().padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
                Shot(pages[i], active = pager.currentPage == i)
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (pager.currentPage > 0) {
                TextButton(onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } }) {
                    Text("Geri", color = Color.White)
                }
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = { if (last) onDone() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4E9A2F)),
            ) { Text(if (last) "Başla" else "İleri", fontWeight = FontWeight.Bold) }
        }
    }
}

/** Tam sayfa ekran görüntüsü; vurgu, ok ve açıklama öğeden öğeye kendiliğinden geçer (dokununca hemen). */
@Composable
private fun Shot(page: TPage, active: Boolean) {
    val context = LocalContext.current
    val img: ImageBitmap? = remember(page.image) {
        runCatching { context.assets.open("tutorial/${page.image}").use { BitmapFactory.decodeStream(it).asImageBitmap() } }.getOrNull()
    }
    var step by remember(page) { mutableIntStateOf(0) }
    LaunchedEffect(active, step) {
        if (!active) return@LaunchedEffect
        delay(4500)
        step = (step + 1) % page.marks.size
    }
    val m = page.marks[step]
    val anim = tween<Float>(500)
    val l by animateFloatAsState(m.r.left, anim, label = "l")
    val t by animateFloatAsState(m.r.top, anim, label = "t")
    val rr by animateFloatAsState(m.r.right, anim, label = "r")
    val b by animateFloatAsState(m.r.bottom, anim, label = "b")
    val pulse by rememberInfiniteTransition(label = "vurgu").animateFloat(
        0.5f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "vurgu",
    )
    // Açıklama, vurgulanan öğe ekranın alt yarısındaysa üstte, değilse altta durur
    val bubbleTop = (m.r.center.y - CROP.top) / CROP.height > 0.5f
    var bubbleH by remember { mutableIntStateOf(0) }

    Box(
        Modifier
            .aspectRatio(CROP.width / CROP.height, matchHeightConstraintsFirst = true)
            .clip(RoundedCornerShape(16.dp))
            .clickable { step = (step + 1) % page.marks.size },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            if (img == null) return@Canvas
            val k = img.width / 1080f
            drawImage(
                img,
                srcOffset = IntOffset((CROP.left * k).toInt(), (CROP.top * k).toInt()),
                srcSize = IntSize((CROP.width * k).toInt(), (CROP.height * k).toInt()),
                dstSize = IntSize(size.width.toInt(), size.height.toInt()),
            )
            val sc = size.width / CROP.width
            val r = Rect((l - CROP.left) * sc, (t - CROP.top) * sc, (rr - CROP.left) * sc, (b - CROP.top) * sc)
            val corner = CornerRadius(12.dp.toPx())
            val dim = Path().apply {
                fillType = PathFillType.EvenOdd
                addRect(Rect(Offset.Zero, size))
                addRoundRect(RoundRect(r, corner))
            }
            drawPath(dim, Color.Black.copy(alpha = 0.6f))
            drawRoundRect(Accent.copy(alpha = pulse), r.topLeft, Size(r.width, r.height), corner, style = Stroke(3.5.dp.toPx()))
            val margin = 12.dp.toPx()
            val start = if (bubbleTop) Offset(size.width * 0.5f, margin + bubbleH + 4.dp.toPx())
            else Offset(size.width * 0.5f, size.height - margin - bubbleH - 4.dp.toPx())
            val gap = (if (bubbleTop) r.top - start.y else start.y - r.bottom)
            if (gap > 24.dp.toPx()) drawArrow(start, r)
        }
        Column(
            Modifier
                .align(if (bubbleTop) Alignment.TopCenter else Alignment.BottomCenter)
                .padding(12.dp)
                .fillMaxWidth()
                .onSizeChanged { bubbleH = it.height }
                .shadow(8.dp, RoundedCornerShape(14.dp))
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White)
                .border(2.dp, Accent, RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text(m.text, color = Color(0xFF22262A), fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                page.marks.indices.forEach { j ->
                    Box(Modifier.size(if (j == step) 8.dp else 6.dp).clip(RoundedCornerShape(50))
                        .background(if (j == step) Accent else Color(0xFFCCCCCC)))
                }
            }
        }
    }
}

/** [start] noktasından vurgulanan kutunun kenarına kıvrık ok çizer. */
private fun DrawScope.drawArrow(start: Offset, target: Rect) {
    val gap = 6.dp.toPx()
    val inset = 14.dp.toPx()
    val px = start.x.coerceIn(target.left + inset, target.right - inset)
    val end = if (start.y > target.bottom) Offset(px, target.bottom + gap) else Offset(px, target.top - gap)
    val dx = end.x - start.x
    val dy = end.y - start.y
    val ctrl = Offset((start.x + end.x) / 2 - dy * 0.25f, (start.y + end.y) / 2 + dx * 0.25f)
    val path = Path().apply { moveTo(start.x, start.y); quadraticBezierTo(ctrl.x, ctrl.y, end.x, end.y) }
    val ang = atan2(end.y - ctrl.y, end.x - ctrl.x)
    val h = 14.dp.toPx()
    val head = Path().apply {
        moveTo(end.x - h * cos(ang - 0.45f), end.y - h * sin(ang - 0.45f))
        lineTo(end.x, end.y)
        lineTo(end.x - h * cos(ang + 0.45f), end.y - h * sin(ang + 0.45f))
    }
    for ((c, w) in listOf(Color.White to 7.dp.toPx(), Accent to 4.dp.toPx())) {
        drawPath(path, c, style = Stroke(w, cap = StrokeCap.Round))
        drawPath(head, c, style = Stroke(w, cap = StrokeCap.Round))
    }
}
