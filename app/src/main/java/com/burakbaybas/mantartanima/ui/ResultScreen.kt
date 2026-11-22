package com.burakbaybas.mantartanima.ui

import android.net.Uri
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.border
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

private val ResultGreen = Color(0xFF17502A)

/** Bu güvenin altındaki en iyi tahmin "emin değil" olarak gösterilir. */
private const val LOW_CONFIDENCE = 0.6f

/** Bu olasılığın altındaki 2. ve 3. tahminler gösterilmez (anlamsız düşük olasılıklar). */
private const val MIN_SHOWN = 0.05f

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
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Sonuç", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFE6F0DC),
                    titleContentColor = ResultGreen,
                    navigationIconContentColor = ResultGreen,
                ),
            )
        },
        modifier = Modifier.background(
            Brush.verticalGradient(listOf(Color(0xFFF3F8EE), Color(0xFFFDFBF7)))
        ),
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
                    val shown = predictions.filterIndexed { i, p -> i == 0 || p.confidence >= MIN_SHOWN }
                    val low = predictions.first().confidence < LOW_CONFIDENCE
                    val uncertainText = "Model bu fotoğraflardan emin değil. Mantar, veri tabanında olmayan " +
                        "bir tür olabilir ya da fotoğraflar yeterince net değil. " +
                        "Aşağıdaki sonuçlara güvenmeyin; iyi ışıkta yeniden deneyin."
                    // İlk 3 sonuçta ölümcül bir tür varsa ve sonuç kesin değilse (ya da ölümcül tür
                    // ilk sırada değilse) kırmızı uyarı göster.
                    val deadly = shown.take(3)
                        .mapNotNull { SpeciesCatalog.find(it.label) }
                        .filter { it.edibility == Edibility.DEADLY }
                    val topIsDeadly = SpeciesCatalog.find(predictions.first().label)?.edibility == Edibility.DEADLY
                    if (deadly.isNotEmpty() && (low || !topIsDeadly)) {
                        item {
                            DeadlyRiskCard(
                                names = deadly.map { it.displayName },
                                extra = if (low) uncertainText else null,
                            )
                        }
                    } else if (low) {
                        item { InfoCard(uncertainText) }
                    }
                    itemsIndexed(shown) { i, p -> PredictionCard(p, i) }
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

    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFCFE2C3)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
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
                color = Color(0xFF4E9A2F),
                trackColor = Color(0xFFE1EDD8),
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
            EdibilityStatusBox(species.edibility)
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

            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Info, null, tint = Color(0xFF1B5E2E), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Temel Bilgiler", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(6.dp))
            val rows = buildList {
                add("Cins" to species.latinName.substringBefore(' '))
                add("Sezon" to species.season)
                add("Habitat" to species.habitat)
                add("Spor izi" to species.sporePrint)
                if (species.lookalikes.isNotEmpty()) add("Benzer türler" to species.lookalikes.joinToString(", "))
            }.filter { it.second.isNotBlank() }
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, Color(0xFFE3E7E1), RoundedCornerShape(10.dp)),
            ) {
                rows.forEachIndexed { i, (k, v) ->
                    InfoTableRow(k, v, shaded = i % 2 == 0)
                }
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
private fun InfoTableRow(title: String, value: String, shaded: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (shaded) Color(0xFFF4F7F2) else Color.White)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold,
            modifier = Modifier.width(104.dp))
        Text(value, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
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
private fun DeadlyRiskCard(names: List<String>, extra: String?) {
    val red = Color(0xFFC62828)
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFFFDECEC), RoundedCornerShape(16.dp))
            .border(1.5.dp, Color(0xFFE57373), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        EdibilityIcon(Edibility.DEADLY, 30.dp)
        Spacer(Modifier.width(12.dp))
        Column {
            Text("Ölümcül türlerle karışma riski var", fontWeight = FontWeight.Bold, color = red,
                style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            Text(
                "Olası sonuçlar arasında ölümcül zehirli " +
                    (if (names.size == 1) "bir tür" else "türler") + " var: " + names.joinToString(", ") +
                    ". Bu mantarı kesinlikle tüketmeyin; bir uzmana gösterin.",
                style = MaterialTheme.typography.bodyMedium, color = Color(0xFF8C1D18),
            )
            if (extra != null) {
                Spacer(Modifier.height(6.dp))
                Text(extra, style = MaterialTheme.typography.bodySmall, color = Color(0xFF5F2120))
            }
        }
    }
}

@Composable
private fun InfoCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFCFE2C3)),
    ) {
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
