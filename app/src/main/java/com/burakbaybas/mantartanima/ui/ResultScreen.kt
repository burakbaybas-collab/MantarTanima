package com.burakbaybas.mantartanima.ui

import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.automirrored.outlined.CompareArrows
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Forest
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.AssistChip
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.burakbaybas.mantartanima.PhotoAngle
import com.burakbaybas.mantartanima.UiState
import com.burakbaybas.mantartanima.classifier.Prediction
import com.burakbaybas.mantartanima.data.Edibility
import com.burakbaybas.mantartanima.data.SpeciesCatalog

/** Bu güvenin altındaki en iyi tahmin "emin değil" olarak gösterilir. */
private const val LOW_CONFIDENCE = 0.6f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    photos: Map<PhotoAngle, Uri>,
    predictions: List<Prediction>?, // null = analiz sürüyor
    isDemo: Boolean,
    onBack: () -> Unit,
    onStartOver: () -> Unit,
    noMushroom: UiState.NoMushroom? = null,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sonuç") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PhotoAngle.entries.forEach { angle ->
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            AsyncImage(
                                model = photos[angle],
                                contentDescription = angle.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(12.dp)),
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(angle.title, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
            when {
                noMushroom != null -> item {
                    InfoCard(
                        "Fotoğraflarda mantar algılanamadı " +
                            "(${noMushroom.totalPhotos} fotoğraftan ${noMushroom.mushroomPhotos} tanesinde mantar görüldü). " +
                            "Mantarı yakından, kadrajı dolduracak şekilde ve iyi ışıkta yeniden çekin."
                    )
                }
                predictions == null -> item {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                isDemo -> item {
                    InfoCard(
                        "Tanıma modeli henüz eklenmedi. Üç fotoğraf alındı; " +
                            "model eklendiğinde veri tabanıyla karşılaştırma sonuçları burada görünecek."
                    )
                }
                predictions.isEmpty() -> item { InfoCard("Bu fotoğraflar için tahmin üretilemedi.") }
                else -> {
                    if (predictions.first().confidence < LOW_CONFIDENCE) {
                        item {
                            InfoCard(
                                "Model bu fotoğraflardan emin değil. Mantar, veri tabanında olmayan " +
                                    "bir tür olabilir ya da fotoğraflar yeterince net değil. " +
                                    "Aşağıdaki sonuçlara güvenmeyin; iyi ışıkta yeniden deneyin."
                            )
                        }
                    }
                    itemsIndexed(predictions) { i, p -> PredictionCard(p, i) }
                }
            }
            item { SafetyNotice() }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                        Text("Fotoğrafları değiştir")
                    }
                    OutlinedButton(onClick = onStartOver, modifier = Modifier.weight(1f)) {
                        Text("Yeni tanıma")
                    }
                }
            }
        }
    }
}

@Composable
private fun PredictionCard(prediction: Prediction, rank: Int) {
    val species = SpeciesCatalog.find(prediction.label)
    var expanded by rememberSaveable(prediction.label) { mutableStateOf(rank == 0) }
    val uriHandler = LocalUriHandler.current
    val dangerous = species?.edibility == Edibility.POISONOUS || species?.edibility == Edibility.DEADLY

    Card(modifier = Modifier.fillMaxWidth().animateContentSize()) {
        Column(Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(enabled = species != null) { expanded = !expanded },
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        (if (rank == 0) "En olası: " else "${rank + 1}. olasılık: ") +
                            (species?.displayName ?: prediction.label),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (species != null && species.turkishName.isNotBlank()) {
                        Text(
                            species.latinName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontStyle = FontStyle.Italic,
                        )
                    }
                }
                Text(
                    "%%%d".format((prediction.confidence * 100).toInt()),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                if (species != null) {
                    Icon(
                        if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (expanded) "Daralt" else "Ayrıntılar",
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { prediction.confidence },
                modifier = Modifier.fillMaxWidth(),
            )
            if (species != null) {
                Spacer(Modifier.height(12.dp))
                EdibilityBadge(species.edibility)
            }
            if (species != null && !expanded) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Ayrıntılar için dokunun",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { expanded = true },
                )
            }
            if (species != null && expanded) {
            Spacer(Modifier.height(10.dp))
            Text(species.description, style = MaterialTheme.typography.bodyMedium)

            if (species.note.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                val bg = if (dangerous) Color(0xFFFDECEA) else Color(0xFFEFF5EA)
                val fg = if (dangerous) Color(0xFF8C1D18) else Color(0xFF1F4D2A)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(bg, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                ) {
                    Icon(
                        if (dangerous) Icons.Filled.Warning else Icons.Outlined.Info,
                        contentDescription = null,
                        tint = fg,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            if (dangerous) "Zehirlilik" else "Önemli not",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = fg,
                        )
                        Text(species.note, style = MaterialTheme.typography.bodySmall, color = fg)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            InfoRow(Icons.Outlined.CalendarMonth, "Mevsim", species.season)
            InfoRow(Icons.Outlined.Forest, "Yetişme ortamı", species.habitat)
            InfoRow(Icons.Outlined.Fingerprint, "Spor izi", species.sporePrint)
            if (species.lookalikes.isNotEmpty()) {
                InfoRow(Icons.AutoMirrored.Outlined.CompareArrows, "Karışabilen türler", species.lookalikes.joinToString(", "))
            }

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val q = Uri.encode(species.latinName)
                AssistChip(
                    onClick = { uriHandler.openUri("https://tr.wikipedia.org/w/index.php?search=$q") },
                    label = { Text("Vikipedi") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Outlined.MenuBook, null, Modifier.size(18.dp)) },
                )
                AssistChip(
                    onClick = {
                        uriHandler.openUri("https://www.inaturalist.org/taxa/search?q=$q")
                    },
                    label = { Text("Fotoğraflar") },
                    leadingIcon = { Icon(Icons.Outlined.PhotoLibrary, null, Modifier.size(18.dp)) },
                )
            }
            }
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, title: String, value: String) {
    if (value.isBlank()) return
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary)
            Text(value, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun EdibilityBadge(edibility: Edibility) {
    val color = when (edibility) {
        Edibility.EDIBLE -> Color(0xFF2E7D32)
        Edibility.EDIBLE_WITH_CAUTION -> Color(0xFFF9A825)
        Edibility.INEDIBLE -> Color(0xFF6D6D6D)
        Edibility.POISONOUS -> Color(0xFFE65100)
        Edibility.DEADLY -> Color(0xFFB71C1C)
    }
    Text(
        edibility.label,
        color = Color.White,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier
            .background(color, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
private fun InfoCard(text: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Text(text, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun SafetyNotice() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Icon(
                Icons.Filled.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
            )
            Spacer(Modifier.width(12.dp))
            Text(
                "Bu uygulama yalnızca bilgi amaçlıdır. Bir mantarın yenilebilir olduğuna " +
                    "asla yalnızca bu uygulamaya bakarak karar vermeyin; mutlaka uzmana danışın.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

@Composable
fun ErrorScreen(message: String, onBack: () -> Unit) {
    Scaffold { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Bir sorun oluştu", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(24.dp))
            OutlinedButton(onClick = onBack) { Text("Geri dön") }
        }
    }
}
