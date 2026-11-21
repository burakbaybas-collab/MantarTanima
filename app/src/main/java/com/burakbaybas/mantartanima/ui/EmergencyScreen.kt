package com.burakbaybas.mantartanima.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Red = Color(0xFFB3261E)
private val RedBg = Color(0xFFFDECEA)

/** Alt menüdeki "Acil" sekmesi: mantar zehirlenmesinde ilk yapılacaklar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyScreen() {
    val context = LocalContext.current
    fun dial(number: String) {
        // Doğrudan aramaz; numarayı telefon uygulamasında hazır açar (izin gerektirmez)
        runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Zehirlenme Acil Durum") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = RedBg, titleContentColor = Red),
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Mantar yedikten sonra herhangi bir rahatsızlık hissederseniz ya da zehirli bir mantar yediğinizden şüphelenirseniz belirtilerin geçmesini beklemeden hemen arayın.",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            CallButton("112", "Acil Çağrı Merkezi", "Ambulans ve acil yardım") { dial("112") }
            CallButton("114", "Ulusal Zehir Danışma Merkezi (UZEM)", "7/24, ücretsiz zehirlenme danışma hattı") { dial("114") }

            InfoCard(
                Icons.Outlined.CheckCircle, "Hemen yapın", Color(0xFF1F4D2A), Color(0xFFEFF5EA),
                listOf(
                    "Mantar yediyseniz ve şüpheniz varsa en yakın acil servise gidin.",
                    "Mantarı kimin, ne zaman ve ne kadar yediğini not edin.",
                    "Aynı yemeği yiyen herkes, belirtisi olmasa bile kontrol edilmelidir.",
                    "Hekim önermedikçe kendinizi kusturmaya çalışmayın.",
                ),
            )
            InfoCard(
                Icons.Outlined.Inventory2, "Yanınızda götürün", Color(0xFF3E3A1E), Color(0xFFF7F3E1),
                listOf(
                    "Artan pişmiş veya çiğ mantar ile yemeğin kalanı (kağıt torbada, buzdolabında saklayın).",
                    "Mantarın fotoğrafları; FungiLens'te çektiğiniz fotoğraflar da işe yarar.",
                    "Mantarın toplandığı yer ve ağaç türü bilgisi.",
                ),
            )
            InfoCard(
                Icons.Outlined.Schedule, "Belirtiler geç başlayabilir", Red, RedBg,
                listOf(
                    "En tehlikeli zehirlenmelerde (ör. Köygöçüren) belirtiler 6–24 saat sonra başlar ve bir süre düzelmiş gibi görünebilir; bu, iyileşme anlamına gelmez.",
                    "Böbreği etkileyen bazı türlerde (ör. Cortinarius) belirtiler günler sonra ortaya çıkabilir.",
                    "Belirtiler geç başladıysa durum daha ciddi olabilir; mutlaka hastaneye başvurun.",
                ),
            )
            InfoCard(
                Icons.Outlined.Block, "Yapmayın", Color(0xFF5A1A14), Color(0xFFFFF1EE),
                listOf(
                    "Süt, sirke, tuzlu su gibi ev çarelerine güvenmeyin.",
                    "Gümüş kaşık, sarımsak, soğan gibi yöntemler zehirli mantarı ayırt etmez.",
                    "Pişirmek, kurutmak veya dondurmak zehirli mantarları güvenli hale getirmez.",
                    "Yalnızca bir uygulamanın sonucuna göre mantar yemeyin.",
                ),
            )
            Text(
                "Bu sayfa genel bilgilendirme amaçlıdır ve tıbbi tavsiye yerine geçmez. Kesin bilgi için 114 Ulusal Zehir Danışma Merkezi'ni arayın.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun CallButton(number: String, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Red)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Call, null, tint = Color.White, modifier = Modifier.size(30.dp))
        Spacer(Modifier.width(14.dp))
        Text(number, color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun InfoCard(icon: ImageVector, title: String, fg: Color, bg: Color, lines: List<String>) {
    Card(colors = CardDefaults.cardColors(containerColor = bg), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = fg, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, color = fg, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            }
            Spacer(Modifier.height(6.dp))
            lines.forEach {
                Row(Modifier.padding(vertical = 3.dp)) {
                    Text("•", color = fg, modifier = Modifier.width(14.dp), fontWeight = FontWeight.Bold)
                    Text(it, color = fg, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
