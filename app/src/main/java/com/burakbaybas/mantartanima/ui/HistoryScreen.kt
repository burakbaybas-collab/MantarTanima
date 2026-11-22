package com.burakbaybas.mantartanima.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.rememberCoroutineScope
import com.burakbaybas.mantartanima.data.HistoryExport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
import androidx.compose.material.icons.outlined.DeleteSweep
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
    onDeleteAll: () -> Unit = {},
) {
    var toDelete by remember { mutableStateOf<HistoryEntry?>(null) }
    var confirmAll by remember { mutableStateOf(false) }
    var selecting by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(setOf<Long>()) }
    var exportAsk by remember { mutableStateOf(false) }
    var exporting by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    fun toggle(id: Long) { selected = if (id in selected) selected - id else selected + id }
    BackHandler(enabled = selecting) { selecting = false; selected = emptySet() }
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(if (selecting) "${selected.size} seçildi" else "Geçmiş Tanımlamalar", fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    if (selecting) {
                        IconButton(onClick = { selecting = false; selected = emptySet() }) { Icon(Icons.Filled.Close, "Seçimi bitir") }
                    } else {
                        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri") }
                    }
                },
                actions = {
                    if (selecting) {
                        TextButton(onClick = {
                            selected = if (selected.size == entries.size) emptySet() else entries.map { it.id }.toSet()
                        }) { Text(if (selected.size == entries.size) "Hiçbiri" else "Tümü", color = HGreen) }
                        IconButton(enabled = selected.isNotEmpty(), onClick = { exportAsk = true }) {
                            Icon(Icons.Outlined.Share, "Dışa aktar", tint = if (selected.isNotEmpty()) HGreen else Color.Gray)
                        }
                    } else if (entries.isNotEmpty()) {
                        TextButton(onClick = { selecting = true }) {
                            Icon(Icons.Outlined.Share, null, tint = HGreen, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Dışa aktar", color = HGreen, fontWeight = FontWeight.SemiBold)
                        }
                        IconButton(onClick = { confirmAll = true }) {
                            Icon(Icons.Outlined.DeleteSweep, "Tümünü sil", tint = Color(0xFFC62828))
                        }
                    }
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
                    Text("Henüz kayıtlı tanımlama yok.", fontWeight = FontWeight.SemiBold, color = HGreen)
                    Text(
                        "Yaptığınız her tanımlama; tarihi, fotoğrafları ve konumuyla birlikte burada saklanır.",
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
                if (selecting) {
                    item {
                        Text(
                            "Dışa aktarmak istediğiniz kayıtları işaretleyin, sonra sağ üstteki paylaş simgesine dokunun.",
                            fontSize = 13.sp, color = Color(0xFF4B5563),
                        )
                    }
                }
                items(entries, key = { it.id }) { e ->
                    HistoryCard(
                        e,
                        selecting = selecting,
                        checked = e.id in selected,
                        onClick = { if (selecting) toggle(e.id) else onOpen(e) },
                        onLongClick = { selecting = true; toggle(e.id) },
                        onDelete = { toDelete = e },
                    )
                }
            }
        }
    }
    if (exportAsk) {
        val chosen = entries.filter { it.id in selected }
        AlertDialog(
            onDismissRequest = { exportAsk = false },
            title = { Text("${chosen.size} kaydı dışa aktar") },
            text = {
                Column {
                    Text("Nasıl dışa aktarılsın?")
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(onClick = {
                        exportAsk = false; exporting = true
                        scope.launch {
                            val (f, m) = withContext(Dispatchers.IO) { HistoryExport.build(context, chosen, withPhotos = false) }
                            exporting = false; HistoryExport.send(context, f, m)
                        }
                    }, modifier = Modifier.fillMaxWidth()) { Text("Tablo (Excel / CSV)") }
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(onClick = {
                        exportAsk = false; exporting = true
                        scope.launch {
                            val (f, m) = withContext(Dispatchers.IO) { HistoryExport.build(context, chosen, withPhotos = true) }
                            exporting = false; HistoryExport.send(context, f, m)
                        }
                    }, modifier = Modifier.fillMaxWidth()) { Text("Tablo + fotoğraflar + harita (ZIP)") }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { exportAsk = false }) { Text("Vazgeç") } },
        )
    }
    if (exporting) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            title = { Text("Hazırlanıyor…") },
            text = { LinearProgressIndicator(Modifier.fillMaxWidth()) },
        )
    }
    if (confirmAll) {
        AlertDialog(
            onDismissRequest = { confirmAll = false },
            title = { Text("Tüm kayıtları sil") },
            text = { Text("${entries.size} tanımlama kaydının ve fotoğraflarının tamamı silinsin mi? Bu işlem geri alınamaz.") },
            confirmButton = {
                TextButton(onClick = { onDeleteAll(); confirmAll = false }) {
                    Text("Sil", color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { confirmAll = false }) { Text("Vazgeç") } },
        )
    }
    toDelete?.let { e ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Kaydı sil") },
            text = { Text("Bu tanımlama kaydı ve fotoğrafları silinsin mi?") },
            confirmButton = { TextButton(onClick = { onDelete(e); toDelete = null }) { Text("Sil") } },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Vazgeç") } },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryCard(
    e: HistoryEntry,
    selecting: Boolean,
    checked: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val top = e.predictions.firstOrNull()
    val sp = top?.let { SpeciesCatalog.find(it.label) }
    Card(
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick),
        colors = CardDefaults.cardColors(containerColor = if (checked) Color(0xFFEAF7EE) else Color.White),
        border = BorderStroke(if (checked) 2.dp else 1.dp, if (checked) HGreen else Color(0xFFCFE2C3)),
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
            if (selecting) {
                Checkbox(checked = checked, onCheckedChange = { onClick() },
                    colors = CheckboxDefaults.colors(checkedColor = HGreen))
            } else {
                IconButton(onClick = onDelete) { Icon(Icons.Outlined.DeleteOutline, "Sil", tint = Color(0xFF9CA3AF)) }
            }
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
