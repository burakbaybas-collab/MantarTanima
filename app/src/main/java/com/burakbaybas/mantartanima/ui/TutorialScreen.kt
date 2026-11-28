package com.burakbaybas.mantartanima.ui

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.burakbaybas.mantartanima.data.Edibility
import kotlinx.coroutines.launch

/** İlk açılışta bir kez gösterilen kısa kullanım eğitimi (ana ekrandan tekrar açılabilir). */
object Tutorial {
    private const val PREFS = "ayarlar"
    private const val KEY = "egitim_goruldu"
    fun isSeen(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY, false)
    fun markSeen(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY, true).apply()
}

private val TGreen = Color(0xFF17502A)
private val TSoft = Color(0xFFEFF5EC)
private val TText = Color(0xFF33373D)

private class TPage(val icon: ImageVector, val title: String, val intro: String, val body: @Composable () -> Unit)

@Composable
fun TutorialScreen(onDone: () -> Unit) {
    val pages = listOf(
        TPage(Icons.Outlined.School, "FungiLens'e hoş geldiniz",
            "Bu kısa rehber uygulamadaki düğmeleri ve yazıları tanıtır. Sağa kaydırarak ilerleyin.") {
            Bullet(Icons.Outlined.WifiOff, "İnternet gerekmez", "Tanıma tamamen telefonunuzda yapılır; ormanda çekmeden de çalışır.")
            Bullet(Icons.Outlined.Checklist, "1000 tür", "Türkiye ve Avrupa'da görülen 1000 mantar türünü tanır.")
            Bullet(Icons.Filled.Warning, "Yalnızca fikir verir", "Sonuçlar bilgi amaçlıdır. Hiçbir mantarı yalnızca uygulamaya bakarak yemeyin.", tint = Color(0xFFB3261E))
        },
        TPage(Icons.Outlined.AddAPhoto, "1. Üç fotoğraf yükleyin",
            "Ana ekranda üç kutu vardır. Her biri mantarın farklı bir yüzü içindir:") {
            MiniChip("Üstten", "Şapkanın üstü: renk, pullar, desen")
            MiniChip("Yandan", "Şapka ve sap birlikte: halka, sap tabanı, volva")
            MiniChip("Alttan", "Şapkanın altı: lameller ya da gözenekler")
            Spacer(Modifier.height(8.dp))
            Bullet(Icons.Filled.PhotoCamera, "Kamera ile çek", "Kutuya dokunun ve fotoğrafı hemen çekin.")
            Bullet(Icons.Filled.PhotoLibrary, "Galeriden seç", "Daha önce çektiğiniz bir fotoğrafı seçin.")
            Bullet(Icons.Outlined.Info, "İpuçları", "Üstteki şeritteki (i) düğmesi iyi fotoğraf çekme önerilerini açar.")
        },
        TPage(Icons.Filled.AutoAwesome, "2. Tanımla düğmesi",
            "Fotoğraflar eklenince yeşil Tanımla düğmesine basın. Parantez içindeki sayı kaç fotoğraf eklediğinizi gösterir.") {
            FakeButton("Tanımla (3/3)")
            Spacer(Modifier.height(10.dp))
            Bullet(Icons.Outlined.Info, "Tek fotoğraf da olur", "Ama üç açı en doğru sonucu verir.")
            Bullet(Icons.Filled.Warning, "Farklı mantarlar uyarısı", "Fotoğraflar farklı mantarlara aitse uygulama sizi uyarır; aynı mantarın fotoğraflarını kullanın.", tint = Color(0xFFC77700))
        },
        TPage(Icons.Outlined.Checklist, "3. Sonuç kartı",
            "Sonuç sayfasının en üstünde en olası tür yazar:") {
            Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFFF1F3F1), border = BorderStroke(1.dp, Color(0xFFD5DAD3))) {
                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Boletus edulis", fontStyle = FontStyle.Italic, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, modifier = Modifier.weight(1f))
                        Text("%86", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = TGreen)
                    }
                    Text("Çörek Mantarı", fontWeight = FontWeight.SemiBold, color = TText)
                }
            }
            Spacer(Modifier.height(8.dp))
            Explain("Büyük yazı", "Türün bilimsel (Latince) adı.")
            Explain("Altındaki yazı", "Türkçe adı. Varsa yöresel adları da \"Diğer yöresel adları\" satırında yazar.")
            Explain("Yüzde ve çubuk", "Modelin ne kadar emin olduğu. Çubuk ne kadar doluysa o kadar emin.")
            Explain("\"Model emin değil\"", "Oran düşükse çıkar; sonucu yalnızca fikir olarak kullanın.")
            Explain("\"Tanınamadı\"", "Mantar listede yoksa ya da fotoğraf mantar değilse çıkar. Daha yakından ve iyi ışıkta çekin.")
        },
        TPage(Icons.Filled.Warning, "4. Yenilebilirlik etiketleri",
            "Her türün renkli bir etiketi vardır. Etikete dokunursanız tüm kategorilerin açıklaması açılır.") {
            listOf(Edibility.EDIBLE, Edibility.EDIBLE_WITH_CAUTION, Edibility.INEDIBLE, Edibility.POISONOUS, Edibility.DEADLY).forEach {
                EdibilityRow(it)
            }
            Spacer(Modifier.height(6.dp))
            Text("Zehirli ve ölümcül türlerin simgesi dikkat çekmek için yanıp söner.", fontSize = 13.sp, color = TText)
        },
        TPage(Icons.Filled.Call, "5. Zehirlenme kutusu",
            "Zehirli ve ölümcül türlerde kırmızı bir kutu çıkar:") {
            Explain("Zehir", "Mantarın içerdiği zehrin adı.")
            Explain("Belirtiler başlar", "Belirtilerin yemekten ne kadar sonra başladığı. Bazı ölümcül türlerde belirtiler saatler ya da günler sonra başlar.")
            Explain("İlk yardım", "Ne yapmanız gerektiği.")
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FakeCall("112'yi ara"); FakeCall("114'ü ara")
            }
            Spacer(Modifier.height(6.dp))
            Text("112: Acil çağrı. 114: Ulusal Zehir Danışma Merkezi. Düğmeler arama ekranını açar.", fontSize = 13.sp, color = TText)
        },
        TPage(Icons.Outlined.Checklist, "6. Tür bilgileri",
            "Sonuç kartının altında tür hakkında bilgiler bulunur:") {
            Explain("Ayırt edici özellikler", "Türü tanımaya yarayan özellikler; elinizdeki mantarla karşılaştırın.")
            Explain("Benzer türlerden farkı", "Karıştırılan türler ve onlardan nasıl ayrıldığı.")
            Explain("Temel Bilgiler", "Cins, mevsim, yaşam alanı, spor izi, koku, tat ve renk değişimi.")
            Explain("Diğer olasılıklar", "Modelin ikinci ve üçüncü tahminleri. Dokununca ayrıntıları açılır.")
        },
        TPage(Icons.Outlined.History, "7. Geçmiş Tanımlamalar",
            "Ana ekrandaki Geçmiş Tanımlamalar düğmesi önceki tanımlamalarınızı açar.") {
            Explain("Kayıt", "Her tanımlama fotoğrafları, tarihi ve (izin verdiyseniz) konumuyla saklanır.")
            Explain("Aktar", "Seçtiğiniz kayıtları tablo (Excel/CSV) ya da fotoğraflı ZIP olarak paylaşın.")
            Explain("Sil", "Seçtiğiniz kayıtları silin.")
        },
        TPage(Icons.Filled.Warning, "Unutmayın",
            "Mantar zehirlenmeleri ölümcül olabilir.") {
            Bullet(Icons.Filled.Warning, "Uzmana danışın", "Uygulamanın sonucu ne olursa olsun, yemeden önce mutlaka bir uzmana gösterin.", tint = Color(0xFFB3261E))
            Bullet(Icons.Outlined.Info, "Şüphe varsa yemeyin", "Emin olmadığınız hiçbir mantarı yemeyin.")
            Bullet(Icons.Outlined.School, "Bu rehberi tekrar açmak için", "Ana ekranın altındaki \"Nasıl kullanılır?\" yazısına dokunun.")
        },
    )
    val pager = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == pages.lastIndex

    Column(Modifier.fillMaxSize().background(Color(0xFFF7FAF5)).safeDrawingPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.End) {
            if (!last) TextButton(onClick = onDone) { Text("Geç", color = TGreen, fontWeight = FontWeight.SemiBold) }
            else Spacer(Modifier.height(48.dp))
        }
        HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { i ->
            val p = pages[i]
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
                Box(Modifier.size(64.dp).clip(CircleShape).background(TSoft), contentAlignment = Alignment.Center) {
                    Icon(p.icon, null, tint = TGreen, modifier = Modifier.size(34.dp))
                }
                Spacer(Modifier.height(14.dp))
                Text(p.title, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = TGreen, lineHeight = 28.sp)
                Spacer(Modifier.height(8.dp))
                Text(p.intro, fontSize = 15.sp, color = TText, lineHeight = 21.sp)
                Spacer(Modifier.height(14.dp))
                p.body()
                Spacer(Modifier.height(16.dp))
            }
        }
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(pages.size) { i ->
                    Box(Modifier.size(if (i == pager.currentPage) 10.dp else 7.dp).clip(CircleShape)
                        .background(if (i == pager.currentPage) TGreen else Color(0xFFC5D3C0)))
                }
            }
            Button(
                onClick = { if (last) onDone() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                colors = ButtonDefaults.buttonColors(containerColor = TGreen),
            ) { Text(if (last) "Başla" else "İleri", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun Bullet(icon: ImageVector, title: String, text: String, tint: Color = TGreen) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold, color = TText, fontSize = 15.sp)
            Text(text, color = TText, fontSize = 14.sp, lineHeight = 19.sp)
        }
    }
}

@Composable
private fun Explain(label: String, text: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text(label, fontWeight = FontWeight.Bold, color = TGreen, fontSize = 15.sp)
        Text(text, color = TText, fontSize = 14.sp, lineHeight = 19.sp)
    }
}

@Composable
private fun MiniChip(title: String, text: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.width(78.dp).clip(RoundedCornerShape(10.dp)).background(TSoft)
                .border(1.dp, Color(0xFFC5D3C0), RoundedCornerShape(10.dp)).padding(vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) { Text(title, fontWeight = FontWeight.Bold, color = TGreen, fontSize = 14.sp) }
        Spacer(Modifier.width(12.dp))
        Text(text, color = TText, fontSize = 14.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun FakeButton(label: String) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(TGreen).padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
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

@Composable
private fun EdibilityRow(e: Edibility) {
    val s = e.style
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier.width(150.dp).background(s.background, RoundedCornerShape(50))
                .border(1.dp, s.border, RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EdibilityIcon(e, 20.dp, blink = true)
            Spacer(Modifier.width(8.dp))
            Text(s.label, color = s.color, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Spacer(Modifier.width(10.dp))
        Text(
            when (e) {
                Edibility.EDIBLE -> "Yenebilir kabul edilir."
                Edibility.EDIBLE_WITH_CAUTION -> "Pişirme, yaş gibi koşullarla yenir."
                Edibility.INEDIBLE -> "Zehirsiz olabilir ama yenmez."
                Edibility.POISONOUS -> "Zehirlenmeye yol açar."
                Edibility.DEADLY -> "Öldürücü olabilir."
            },
            color = TText, fontSize = 13.sp, modifier = Modifier.weight(1f),
        )
    }
}
