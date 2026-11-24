package com.burakbaybas.mantartanima.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NoMeals
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.GppMaybe
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.burakbaybas.mantartanima.R
import com.burakbaybas.mantartanima.data.Edibility

/** Her yenilebilirlik kategorisinin rengi, zemini ve açıklaması. */
data class EdibilityStyle(
    val label: String,
    val color: Color,
    val background: Color,
    val border: Color,
    val description: String,
)

val Edibility.style: EdibilityStyle
    get() = when (this) {
        Edibility.EDIBLE -> EdibilityStyle(
            "Yenir", Color(0xFF1F8F4E), Color(0xFFEAF7EE), Color(0xFFB9E2C6),
            "Bilinen kaynaklara göre yenilebilir kabul edilir. Tüketim öncesi uzman doğrulaması önerilir.",
        )
        Edibility.EDIBLE_WITH_CAUTION -> EdibilityStyle(
            "Dikkatle Yenir", Color(0xFFC58A00), Color(0xFFFFF7E0), Color(0xFFF1D98C),
            "Yalnızca belirli koşullarda tüketilebilir. Karışma riski, iyi pişirme gerekliliği veya " +
                "bireysel hassasiyetler nedeniyle dikkat gerekir.",
        )
        Edibility.INEDIBLE -> EdibilityStyle(
            "Yenmez", Color(0xFF6B7280), Color(0xFFF1F2F4), Color(0xFFD3D6DB),
            "Gıda olarak tüketilmesi önerilmez. Zehirli olmayabilir; ancak acı, sert veya " +
                "tatsız olduğu için yenilebilir mantar olarak değerlendirilmez.",
        )
        Edibility.POISONOUS -> EdibilityStyle(
            "Zehirli", Color(0xFFD97706), Color(0xFFFFF1E4), Color(0xFFF5C99A),
            "Tüketilmemelidir. Sağlık açısından zararlı etkiler oluşturabilir.",
        )
        Edibility.DEADLY -> EdibilityStyle(
            "Ölümcül Zehirli", Color(0xFFC62828), Color(0xFFFDECEC), Color(0xFFF2B8B8),
            "Çok yüksek risk taşır. Kesinlikle tüketilmemelidir. Hayati tehlike oluşturabilir.",
        )
    }

/** Kategori simgesi: çatal-bıçak, uyarılı çatal-bıçak, çarpı çatal-bıçak, üçgen uyarı, kurukafa. */
@Composable
fun EdibilityIcon(edibility: Edibility, size: Dp = 24.dp, tint: Color = edibility.style.color) {
    when (edibility) {
        Edibility.EDIBLE -> Icon(Icons.Filled.Restaurant, null, tint = tint, modifier = Modifier.size(size))
        Edibility.EDIBLE_WITH_CAUTION -> Box(Modifier.size(size)) {
            Icon(Icons.Filled.Restaurant, null, tint = tint, modifier = Modifier.size(size * 0.86f))
            Box(
                Modifier
                    .size(size * 0.52f)
                    .align(Alignment.BottomEnd)
                    .offset(x = size * 0.08f)
                    .background(Color.White, CircleShape)
                    .padding(size * 0.04f)
                    .background(tint, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.PriorityHigh, null, tint = Color.White, modifier = Modifier.size(size * 0.36f))
            }
        }
        Edibility.INEDIBLE -> Icon(Icons.Filled.NoMeals, null, tint = tint, modifier = Modifier.size(size))
        Edibility.POISONOUS -> Icon(Icons.Filled.Warning, null, tint = tint, modifier = Modifier.size(size))
        Edibility.DEADLY -> Icon(painterResource(R.drawable.ic_skull), null, tint = tint, modifier = Modifier.size(size))
    }
}

/** Sonuç kartındaki renkli etiket. Dokunulunca tüm kategorilerin açıklaması açılır. */
@Composable
fun EdibilityBadge(edibility: Edibility) {
    val s = edibility.style
    var showLegend by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(s.background, RoundedCornerShape(50))
            .border(1.dp, s.border, RoundedCornerShape(50))
            .clickable { showLegend = true }
            .padding(start = 10.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
    ) {
        EdibilityIcon(edibility, 22.dp)
        Spacer(Modifier.width(8.dp))
        Text(s.label, color = s.color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.width(6.dp))
        Icon(Icons.Outlined.Info, "Kategoriler", tint = s.color.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
    }
    if (showLegend) EdibilityLegendDialog(onDismiss = { showLegend = false })
}

/** Yenilebilirlik kutusu: simge, kategori adı (Yenir, Zehirli…) ve altında açıklaması. */
@Composable
fun EdibilityStatusBox(edibility: Edibility) {
    val s = edibility.style
    Row(
        Modifier
            .fillMaxWidth()
            .background(s.background, RoundedCornerShape(12.dp))
            .border(1.dp, s.border, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EdibilityIcon(edibility, 34.dp)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(s.label, fontWeight = FontWeight.Bold, color = s.color,
                style = MaterialTheme.typography.titleMedium)
            Text(s.description, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF33373D))
        }
    }
}

/** Tüm kategorilerin simge, renk ve açıklamasını gösteren pencere. */
@Composable
fun EdibilityLegendDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            modifier = Modifier.fillMaxWidth(0.92f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.5.dp, Color(0xFFB9E2C6)),
        ) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Restaurant, null, tint = Color(0xFF1F8F4E), modifier = Modifier.size(26.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Yenilebilirlik Kategorileri", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold, color = Color(0xFF1B5E2E), modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, "Kapat") }
                }
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Edibility.entries.forEach { e ->
                        val s = e.style
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(s.background, RoundedCornerShape(12.dp))
                                .border(1.dp, s.border, RoundedCornerShape(12.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            EdibilityIcon(e, 30.dp)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(s.label, fontWeight = FontWeight.Bold, color = s.color,
                                    style = MaterialTheme.typography.titleSmall)
                                Text(s.description, style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF33373D))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFDECEC), RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(Icons.Outlined.GppMaybe, null, tint = Color(0xFFC62828), modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Güvenlik Uyarısı", fontWeight = FontWeight.Bold, color = Color(0xFFC62828),
                            style = MaterialTheme.typography.labelLarge)
                        Text(
                            "Bu değerlendirme yalnızca bilgilendirme amaçlıdır. Fotoğrafa dayalı tür tanımlaması " +
                                "ve yenilebilirlik bilgisi, tüketim kararı için tek başına kullanılmamalıdır.",
                            style = MaterialTheme.typography.bodySmall, color = Color(0xFF8C1D18),
                        )
                    }
                }
            }
        }
    }
}
