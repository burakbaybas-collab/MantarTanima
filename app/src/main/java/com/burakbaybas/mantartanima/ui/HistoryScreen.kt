package com.burakbaybas.mantartanima.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.burakbaybas.mantartanima.PhotoAngle
import com.burakbaybas.mantartanima.data.HistoryEntry
import com.burakbaybas.mantartanima.data.HistoryPlace
import com.burakbaybas.mantartanima.data.SpeciesCatalog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val HGreen = Color(0xFF1B5E2E)

fun formatDate(time: Long): String =
    SimpleDateFormat("d MMMM yyyy, HH:mm", Locale("tr", "TR")).format(Date(time))

fun placeText(p: HistoryPlace): String =
    p.name ?: "%.5f, %.5f".format(Locale.US, p.lat, p.lon)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    entries: List<HistoryEntry>,
    onBack: () -> Unit,
    onOpen: (HistoryEntry) -> Unit,
    onDelete: (HistoryEntry) -> Unit,
) {
    var toDelete by remember { mutableStateOf<HistoryEntry?>(null) }
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Geçmiş tanımalar", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFE6F0DC), titleContentColor = HGreen, navigationIconContentColor = HGreen,
                ),
            )
        },
        modifier = Modifier.background(Brush.verticalGradient(listOf(Color(0xFFF3F8EE), Color(0xFFFDFBF7)))),
    ) { padding ->
        if (entries.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding).padding(32.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.History, null, tint = HGreen.copy(alpha = 0.5f), modifier = Modifier.size(56.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Henüz kayıtlı tanıma yok.", fontWeight = FontWeight.SemiBold, color = HGreen)
                    Text(
                        "Yaptığınız her tanıma; tarihi, fotoğrafları ve konumuyla birlikte burada saklanır.",
                        style = MaterialTheme.typography.bodySmall, color = Color(0xFF4B5563),
                    )
                }
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(entries, key = { it.id }) { e -> HistoryCard(e, onClick = { onOpen(e) }, onDelete = { toDelete = e }) }
            }
        }
    }
    toDelete?.let { e ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Kaydı sil") },
            text = { Text("Bu tanıma kaydı ve fotoğrafları silinsin mi?") },
            confirmButton = { TextButton(onClick = { onDelete(e); toDelete = null }) { Text("Sil") } },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Vazgeç") } },
        )
    }
}

@Composable
private fun HistoryCard(e: HistoryEntry, onClick: () -> Unit, onDelete: () -> Unit) {
    val top = e.predictions.firstOrNull()
    val sp = top?.let { SpeciesCatalog.find(it.label) }
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFCFE2C3)),
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = (e.photos[PhotoAngle.SIDE] ?: e.photos.values.firstOrNull()),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(76.dp).clip(RoundedCornerShape(12.dp)),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(sp?.displayName ?: top?.label ?: "—", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1F2A1F))
                if (sp != null && sp.turkishName.isNotBlank()) {
                    Text(sp.latinName, fontStyle = FontStyle.Italic, fontSize = 12.sp, color = Color(0xFF4B5563))
                }
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (sp != null) {
                        EdibilityIcon(sp.edibility, 16.dp)
                        Spacer(Modifier.width(4.dp))
                        Text(sp.edibility.style.label, color = sp.edibility.style.color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.width(8.dp))
                    }
                    if (top != null) Text("%" + (top.confidence * 100).toInt(), fontSize = 12.sp, color = Color(0xFF4B5563))
                }
                Text(formatDate(e.time), fontSize = 12.sp, color = Color(0xFF4B5563))
                e.place?.let { p ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Place, null, tint = HGreen, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(2.dp))
                        Text(placeText(p), fontSize = 12.sp, color = HGreen, maxLines = 1)
                    }
                }
            }
            IconButton(onClick = onDelete) { Icon(Icons.Outlined.DeleteOutline, "Sil", tint = Color(0xFF9CA3AF)) }
        }
    }
}

/** Geçmiş ayrıntısında fotoğrafların altında gösterilen tarih ve konum kartı. */
@Composable
fun HistoryInfoCard(e: HistoryEntry) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFCFE2C3)),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(formatDate(e.time), fontWeight = FontWeight.SemiBold, color = Color(0xFF1F2A1F))
            val p = e.place
            if (p == null) {
                Text("Konum kaydedilmedi.", fontSize = 13.sp, color = Color(0xFF4B5563))
            } else {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Place, null, tint = HGreen, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Column(Modifier.weight(1f)) {
                        if (p.name != null) Text(p.name, fontSize = 14.sp, color = Color(0xFF1F2A1F))
                        Text(
                            "%.5f, %.5f".format(Locale.US, p.lat, p.lon) +
                                (p.accuracyM?.let { " (±${it.toInt()} m)" } ?: "") +
                                if (p.source == "foto") " · fotoğraftan" else " · telefon konumu",
                            fontSize = 12.sp, color = Color(0xFF4B5563),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = {
                    val uri = Uri.parse("geo:${p.lat},${p.lon}?q=${p.lat},${p.lon}")
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                }) {
                    Icon(Icons.Outlined.Map, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Haritada aç")
                }
            }
        }
    }
}
