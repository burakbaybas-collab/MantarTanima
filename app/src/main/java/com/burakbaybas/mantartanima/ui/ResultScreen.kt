package com.burakbaybas.mantartanima.ui

import android.net.Uri
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
import com.burakbaybas.mantartanima.classifier.Prediction
import com.burakbaybas.mantartanima.data.Edibility
import com.burakbaybas.mantartanima.data.SpeciesCatalog

/** Bu güvenin altındaki en iyi tahmin "emin değil" olarak gösterilir. */
private const val LOW_CONFIDENCE = 0.6f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    imageUri: Uri,
    predictions: List<Prediction>?, // null = analiz sürüyor
    isDemo: Boolean,
    onBack: () -> Unit,
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
                AsyncImage(
                    model = imageUri,
                    contentDescription = "Seçilen fotoğraf",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f)
                        .clip(RoundedCornerShape(16.dp)),
                )
            }
            when {
                predictions == null -> item {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                isDemo -> item {
                    InfoCard(
                        "Tanıma modeli henüz eklenmedi. Fotoğraf seçme ve gösterme akışı çalışıyor; " +
                            "model eklendiğinde tahminler burada görünecek."
                    )
                }
                predictions.isEmpty() -> item { InfoCard("Bu fotoğraf için tahmin üretilemedi.") }
                else -> {
                    if (predictions.first().confidence < LOW_CONFIDENCE) {
                        item {
                            InfoCard(
                                "Model bu fotoğraftan emin değil. Farklı açılardan, " +
                                    "lameller ve sap görünecek şekilde yeni fotoğraf deneyin."
                            )
                        }
                    }
                    items(predictions) { PredictionCard(it) }
                }
            }
            item { SafetyNotice() }
            item {
                OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                    Text("Yeni fotoğraf")
                }
            }
        }
    }
}

@Composable
private fun PredictionCard(prediction: Prediction) {
    val species = SpeciesCatalog.find(prediction.label)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        species?.turkishName ?: prediction.label,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (species != null) {
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
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { prediction.confidence },
                modifier = Modifier.fillMaxWidth(),
            )
            if (species != null) {
                Spacer(Modifier.height(12.dp))
                EdibilityBadge(species.edibility)
                Spacer(Modifier.height(8.dp))
                Text(species.description, style = MaterialTheme.typography.bodyMedium)
                if (species.lookalikes.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Karışabilen türler: " + species.lookalikes.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
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
