package com.burakbaybas.mantartanima.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.CompareArrows
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Forest
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Sos
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.burakbaybas.mantartanima.data.Edibility
import com.burakbaybas.mantartanima.data.Species
import com.burakbaybas.mantartanima.data.SpeciesCatalog
import java.text.Collator
import java.util.Locale

private val TR = Locale("tr", "TR")

fun edibilityColor(e: Edibility): Color = when (e) {
    Edibility.EDIBLE -> Color(0xFF2E7D32)
    Edibility.EDIBLE_WITH_CAUTION -> Color(0xFFE0A100)
    Edibility.INEDIBLE -> Color(0xFF757575)
    Edibility.POISONOUS -> Color(0xFFE65100)
    Edibility.DEADLY -> Color(0xFFB71C1C)
}

/** Alt menüdeki "Rehber" sekmesi: tür listesi ve ayrıntı sayfası. */
@Composable
fun SpeciesGuideScreen(onOpenEmergency: () -> Unit) {
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = selectedId?.let { SpeciesCatalog.find(it) }
    if (selected != null) {
        BackHandler { selectedId = null }
        SpeciesDetailScreen(
            species = selected,
            onBack = { selectedId = null },
            onOpenSpecies = { selectedId = it.id },
            onOpenEmergency = onOpenEmergency,
        )
    } else {
        SpeciesListScreen(onOpen = { selectedId = it.id })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpeciesListScreen(onOpen: (Species) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf<Edibility?>(null) }
    val collator = remember { Collator.getInstance(TR) }
    val all = remember { SpeciesCatalog.all.sortedWith(compareBy(collator) { it.displayName }) }
    val q = query.trim().lowercase(TR)
    val shown = all.filter { s ->
        (filter == null || s.edibility == filter) &&
            (q.isEmpty() || s.turkishName.lowercase(TR).contains(q) || s.latinName.lowercase(TR).contains(q))
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Tür Rehberi") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Türkçe veya Latince ad ara") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                trailingIcon = {
                    if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Close, "Temizle") }
                },
                singleLine = true,
                shape = RoundedCornerShape(50),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    FilterChip(selected = filter == null, onClick = { filter = null }, label = { Text("Tümü (${all.size})") })
                }
                items(Edibility.entries) { e ->
                    val n = all.count { it.edibility == e }
                    FilterChip(
                        selected = filter == e,
                        onClick = { filter = if (filter == e) null else e },
                        label = { Text("${e.label} ($n)") },
                        leadingIcon = { Box(Modifier.size(10.dp).background(edibilityColor(e), CircleShape)) },
                    )
                }
            }
            Text(
                "${shown.size} tür",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp),
            )
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(shown, key = { it.id }) { s -> SpeciesRow(s) { onOpen(s) } }
            }
        }
    }
}

@Composable
private fun SpeciesRow(s: Species, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(12.dp).background(edibilityColor(s.edibility), CircleShape))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(s.displayName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                if (s.turkishName.isNotBlank()) {
                    Text(s.latinName, style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic)
                }
            }
            Text(
                s.edibility.label,
                style = MaterialTheme.typography.labelSmall,
                color = edibilityColor(s.edibility),
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeciesDetailScreen(
    species: Species,
    onBack: () -> Unit,
    onOpenSpecies: (Species) -> Unit,
    onOpenEmergency: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val dangerous = species.edibility == Edibility.POISONOUS || species.edibility == Edibility.DEADLY
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(species.displayName) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri") }
                },
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 8.dp),
        ) {
            Text(species.latinName, style = MaterialTheme.typography.titleMedium, fontStyle = FontStyle.Italic)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    species.edibility.label,
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .background(edibilityColor(species.edibility), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
                Spacer(Modifier.width(10.dp))
                val known = species.id in SpeciesCatalog.recognizable
                Text(
                    if (known) "✓ Uygulama bu türü tanıyabilir" else "Henüz tanıma modelinde yok",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (known) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(species.description, style = MaterialTheme.typography.bodyMedium)

            if (species.note.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                val bg = if (dangerous) Color(0xFFFDECEA) else Color(0xFFEFF5EA)
                val fg = if (dangerous) Color(0xFF8C1D18) else Color(0xFF1F4D2A)
                Column(Modifier.fillMaxWidth().background(bg, RoundedCornerShape(12.dp)).padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (dangerous) Icons.Filled.Warning else Icons.Outlined.Info, null, tint = fg, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (dangerous) "Zehirlilik" else "Önemli not",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = fg,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(species.note, style = MaterialTheme.typography.bodySmall, color = fg)
                    if (dangerous) {
                        Spacer(Modifier.height(8.dp))
                        AssistChip(
                            onClick = onOpenEmergency,
                            label = { Text("Zehirlenme durumunda ne yapmalı?") },
                            leadingIcon = { Icon(Icons.Outlined.Sos, null, Modifier.size(18.dp)) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            DetailRow(Icons.Outlined.CalendarMonth, "Mevsim", species.season)
            DetailRow(Icons.Outlined.Forest, "Yetişme ortamı", species.habitat)
            DetailRow(Icons.Outlined.Fingerprint, "Spor izi", species.sporePrint)

            if (species.lookalikes.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Outlined.CompareArrows, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Karışabilen türler", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary)
                }
                species.lookalikes.forEach { name ->
                    val target = SpeciesCatalog.findByLatin(name)
                    Text(
                        "• " + (target?.let { "${it.displayName} (${it.edibility.label.lowercase(TR)})" } ?: name),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (target != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (target != null) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier
                            .padding(start = 30.dp, top = 3.dp, bottom = 3.dp)
                            .then(if (target != null) Modifier.clickable { onOpenSpecies(target) } else Modifier),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val q = Uri.encode(species.latinName)
                AssistChip(
                    onClick = { uriHandler.openUri("https://tr.wikipedia.org/w/index.php?search=$q") },
                    label = { Text("Vikipedi") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Outlined.MenuBook, null, Modifier.size(18.dp)) },
                )
                AssistChip(
                    onClick = { uriHandler.openUri("https://www.inaturalist.org/taxa/search?q=$q") },
                    label = { Text("Fotoğraflar") },
                    leadingIcon = { Icon(Icons.Outlined.PhotoLibrary, null, Modifier.size(18.dp)) },
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DetailRow(icon: ImageVector, title: String, value: String) {
    if (value.isBlank()) return
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary)
            Text(value, style = MaterialTheme.typography.bodySmall)
        }
    }
}
