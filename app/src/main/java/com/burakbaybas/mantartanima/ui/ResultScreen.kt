package com.burakbaybas.mantartanima.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.burakbaybas.mantartanima.data.Syndrome
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
import kotlin.math.roundToInt
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

/** Bunun altında "Tanınamadı" denir. */
private const val VERY_LOW_CONFIDENCE = 0.25f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    photos: Map<PhotoAngle, Uri>,
    predictions: List<Prediction>?, // null = analiz sürüyor
    isDemo: Boolean,
    onBack: () -> Unit,
    onStartOver: () -> Unit,
    noMushroom: UiState.NoMushroom? = null,
    /** Fotoğraflar farklı mantarlara aitse her fotoğrafın kendi tahmini (fotoğraf sırasıyla). */
    mismatch: List<Prediction>? = null,
    /** Fotoğrafların altında gösterilecek ek kart (geçmiş ayrıntısında tarih ve konum). */
    infoCard: (@Composable () -> Unit)? = null,
    /** Geçmiş ayrıntısında "Fotoğrafları değiştir" düğmesi gizlenir. */
    fromHistory: Boolean = false,
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
            if (noMushroom == null && mismatch == null && !isDemo && !predictions.isNullOrEmpty()) {
                item {
                    VerdictHeader(verdictOf(predictions.filterIndexed { i, p -> i == 0 || p.confidence >= MIN_SHOWN }))
                }
            }
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
            if (infoCard != null) item { infoCard() }
            when {
                mismatch != null -> item { MismatchCard(mismatch) }
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
                    // Hüküm kutusu yalnızca kartta yazmayan önemli bir bilgi varsa gösterilir:
                    // en olası tür zararsız görünse de diğer olasılıklarda ölümcül tür varsa.
                    val topE = SpeciesCatalog.find(shown.first().label)?.edibility
                    val deadlyOther = shown.drop(1).any { SpeciesCatalog.find(it.label)?.edibility == Edibility.DEADLY }
                    if (deadlyOther && topE != Edibility.DEADLY && topE != Edibility.POISONOUS) {
                        item { VerdictBox(verdictOf(shown)) }
                    }
                    val unknown = shown.first().confidence < VERY_LOW_CONFIDENCE
                    if (unknown) {
                        item {
                            Text(
                                "Düşük olasılıklı tahminler (yalnızca fikir için)",
                                style = MaterialTheme.typography.titleSmall, color = Color(0xFF6B7280),
                            )
                        }
                    }
                    itemsIndexed(shown) { i, p -> PredictionCard(p, i, unknown) }
                }
            }
            item { SafetyNotice() }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!fromHistory) {
                        OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                            Text("Fotoğrafları değiştir")
                        }
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
private fun PredictionCard(prediction: Prediction, rank: Int, unknown: Boolean = false) {
    val species = SpeciesCatalog.find(prediction.label)
    // Tanınamayan fotoğrafta hiçbir kart açık gelmez ve yenilebilirlik etiketi gösterilmez (yanıltmasın)
    var expanded by rememberSaveable(prediction.label) { mutableStateOf(rank == 0 && !unknown) }
    val uriHandler = LocalUriHandler.current
    val dangerous = species?.edibility == Edibility.POISONOUS || species?.edibility == Edibility.DEADLY

    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFCFE2C3)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            if (rank > 0 || unknown) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(enabled = species != null) { expanded = !expanded },
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        (if (unknown) "${rank + 1}. " else if (rank == 0) "En olası: " else "${rank + 1}. olasılık: ") +
                            (species?.latinName ?: prediction.label),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        fontStyle = FontStyle.Italic,
                    )
                    if (species != null && species.turkishName.isNotBlank()) {
                        Text(
                            species.turkishName,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
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
            if (species != null && !unknown) {
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
            }
            if (species != null && expanded) {
            if (rank > 0 || unknown) Spacer(Modifier.height(10.dp))
            // Diğer olasılıklarda kutucuk başlıkta zaten var; açılınca yalnızca açıklama kutusu
            EdibilityStatusBox(species.edibility, showBadge = rank == 0 || unknown)
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

            species.details?.syndrome?.let { PoisoningBox(it) }
            species.details?.let { d ->
                if (d.features.isNotEmpty()) FeatureChecklist(species.id, d.features)
                if (d.compare.isNotEmpty()) CompareSection(d.compare)
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
                species.details?.let { d ->
                    add("Koku" to d.smell)
                    add("Tat" to d.taste)
                    add("Renk değişimi" to d.colorChange)
                }
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

/** Zehirli türlerde: toksin, belirtilerin başlama süresi, belirtiler ve ilk yardım. */
@Composable
private fun PoisoningBox(sy: Syndrome) {
    val context = LocalContext.current
    val red = Color(0xFF8C1D18)
    Spacer(Modifier.height(10.dp))
    Column(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFFFDECEA), RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFFF2B8B5), RoundedCornerShape(12.dp))
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Warning, null, tint = red, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(sy.title, color = red, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
        }
        Spacer(Modifier.height(6.dp))
        PoisonLine("Zehir", sy.toxin)
        PoisonLine("Belirtiler başlar", sy.onset)
        PoisonLine("Belirtiler", sy.symptoms)
        PoisonLine("İlk yardım", "Belirtileri beklemeden 112'yi arayın; Ulusal Zehir Danışma Merkezi 114. " +
            "Kalan mantarı ve yemek artığını saklayın, aynı yemeği yiyen herkes başvursun. Kendiliğinizden kusturmaya çalışmayın.")
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("112", "114").forEach { n ->
                OutlinedButton(onClick = {
                    runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$n"))) }
                }) {
                    Icon(Icons.Filled.Call, null, Modifier.size(18.dp), tint = red)
                    Spacer(Modifier.width(6.dp))
                    Text(if (n == "112") "112'yi ara" else "114'ü ara", color = red)
                }
            }
        }
    }
}

@Composable
private fun PoisonLine(k: String, v: String) {
    if (v.isBlank()) return
    Text(buildAnnotatedString {
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("$k: ") }
        append(v)
    }, style = MaterialTheme.typography.bodySmall, color = Color(0xFF410E0B), modifier = Modifier.padding(vertical = 2.dp))
}

/** Türün ayırt edici özellikleri (madde madde). */
@Composable
private fun FeatureChecklist(id: String, features: List<String>) {
    Spacer(Modifier.height(12.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.Checklist, null, tint = Color(0xFF1B5E2E), modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text("Ayırt edici özellikler", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    }
    Spacer(Modifier.height(4.dp))
    features.forEach { f ->
        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.Top) {
            Text("•", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF1B5E2E),
                fontWeight = FontWeight.Bold, modifier = Modifier.width(16.dp))
            Text(f, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        }
    }
}

/** Benzer türlerden nasıl ayrılır. */
@Composable
private fun CompareSection(items: List<Pair<String, String>>) {
    Spacer(Modifier.height(12.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.AutoMirrored.Outlined.CompareArrows, null, tint = Color(0xFF1B5E2E), modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text("Benzer türlerden farkı", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    }
    Spacer(Modifier.height(6.dp))
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).border(1.dp, Color(0xFFE3E7E1), RoundedCornerShape(10.dp)),
    ) {
        items.forEachIndexed { i, (name, diff) ->
            Column(Modifier.fillMaxWidth().background(if (i % 2 == 0) Color(0xFFF4F7F2) else Color.White)
                .padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(name, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)
                Text(diff, style = MaterialTheme.typography.bodySmall)
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

private data class Verdict(
    val title: String, val sub: String, val icon: Edibility?,
    val color: Color, val bg: Color, val border: Color,
    val name: String, val latin: String?, val confidence: Float,
    val otherNames: String? = null,
)

/**
 * En olası türün yenilebilirliğine, modelin ne kadar emin olduğuna ve diğer olasılıklar
 * arasında tehlikeli tür olup olmadığına göre sonuç sayfasının hükmünü oluşturur.
 * Yenir bir tür için bile hiçbir zaman "yiyebilirsiniz" denmez; uzman doğrulaması istenir.
 */
private fun verdictOf(shown: List<Prediction>): Verdict {
    val top = shown.first()
    val sp = SpeciesCatalog.find(top.label)
    val name = sp?.displayName ?: top.label
    val others = shown.drop(1).mapNotNull { SpeciesCatalog.find(it.label) }
    val deadlyOthers = others.filter { it.edibility == Edibility.DEADLY }
    val poisonOthers = others.filter { it.edibility == Edibility.POISONOUS }
    val low = top.confidence < LOW_CONFIDENCE
    val veryLow = top.confidence < VERY_LOW_CONFIDENCE
    val e = sp?.edibility

    val grey = Triple(Color(0xFF4B5563), Color(0xFFF1F2F4), Color(0xFFD3D6DB))
    fun st(x: Edibility) = x.style
    val V = { t: String, sub: String, ic: Edibility?, c: Color, bg: Color, br: Color ->
        Verdict(t, sub, ic, c, bg, br, sp?.latinName ?: name, sp?.turkishName?.takeIf { it.isNotBlank() }, top.confidence,
            sp?.otherNames?.takeIf { it.isNotBlank() })
    }
    return when {
        e == Edibility.DEADLY -> V(
            "ÖLÜMCÜL ZEHİRLİ – kesinlikle yemeyin",
            if (low) "Model tam emin değil, ancak bu mantarı kesinlikle tüketmeyin."
            else "Bu mantarı kesinlikle tüketmeyin. Zehirlenme şüphesinde 112'yi arayın.",
            Edibility.DEADLY, st(Edibility.DEADLY).color, st(Edibility.DEADLY).background, st(Edibility.DEADLY).border)
        e == Edibility.POISONOUS -> V(
            "ZEHİRLİ – yemeyin",
            if (low) "Model tam emin değil, ancak bu mantarı tüketmeyin."
            else "Tüketilmemelidir; sağlık açısından zararlı etkiler oluşturabilir.",
            Edibility.POISONOUS, st(Edibility.POISONOUS).color, st(Edibility.POISONOUS).background, st(Edibility.POISONOUS).border)
        deadlyOthers.isNotEmpty() -> V(
            "ÖLÜMCÜL TÜRLERLE KARIŞABİLİR – yemeyin",
            "Olası sonuçlar arasında ölümcül zehirli " + deadlyOthers.joinToString(", ") { it.displayName } +
                " de var. Bir uzmana gösterin.",
            Edibility.DEADLY, st(Edibility.DEADLY).color, st(Edibility.DEADLY).background, st(Edibility.DEADLY).border)
        veryLow -> V(
            "TANINAMADI – yemeyin",
            "Bu mantar veri tabanında olmayabilir ya da fotoğraflar yeterince net değil. " +
                "Yakından ve iyi ışıkta yeniden çekin.",
            null, grey.first, grey.second, grey.third)
        low -> V(
            "EMİN DEĞİL – yemeyin",
            "Model bu fotoğraflardan emin değil. Aşağıdaki sonuçları yalnızca fikir için kullanın.",
            null, grey.first, grey.second, grey.third)
        e == Edibility.INEDIBLE || e == null -> V(
            "YENMEZ",
            "Zehirli olmayabilir, ancak yenen bir mantar değildir.",
            Edibility.INEDIBLE, st(Edibility.INEDIBLE).color, st(Edibility.INEDIBLE).background, st(Edibility.INEDIBLE).border)
        else -> {
            val x = if (poisonOthers.isNotEmpty()) Edibility.EDIBLE_WITH_CAUTION else (e ?: Edibility.INEDIBLE)
            V(
                if (e == Edibility.EDIBLE) "YENİR" else "DİKKATLE YENİR",
                (if (poisonOthers.isNotEmpty()) "Zehirli " + poisonOthers.joinToString(", ") { it.displayName } +
                    " ile karışabilir. " else "") +
                    "Yemeden önce mutlaka bir uzmana doğrulatın.",
                x, st(x).color, st(x).background, st(x).border)
        }
    }
}

/** En üstte: tür adı, Latince adı, oran ve çubuk. */
@Composable
private fun VerdictHeader(v: Verdict) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(v.bg, RoundedCornerShape(18.dp))
            .border(1.5.dp, v.border, RoundedCornerShape(18.dp))
            .padding(16.dp),
    ) {
        if (v.confidence < VERY_LOW_CONFIDENCE) {
            // Tanınamadı: tür adı büyük gösterilmez (mantar olmayan fotoğraflarda yanıltıcı olur)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Info, null, tint = Color(0xFF4B5563), modifier = Modifier.size(30.dp))
                Spacer(Modifier.width(10.dp))
                Text("Tanınamadı", fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, color = Color(0xFF1F2A1F))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Fotoğraftaki mantar veri tabanında olmayabilir ya da bu bir mantar olmayabilir. " +
                    "Mantarı yakından, iyi ışıkta ve kadrajı dolduracak şekilde yeniden çekin. Tanınmayan mantarı yemeyin.",
                fontSize = 14.sp, lineHeight = 19.sp, color = Color(0xFF4B5563),
            )
            return@Column
        }
        // Tür adı ve oran
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(v.name, fontWeight = FontWeight.ExtraBold, fontStyle = FontStyle.Italic, fontSize = 26.sp, lineHeight = 30.sp,
                    color = Color(0xFF1F2A1F))
                if (v.latin != null) {
                    Text(v.latin, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = Color(0xFF374151),
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (v.otherNames != null) {
                    Text("Diğer yöresel adları: ${v.otherNames}", fontSize = 13.sp, color = Color(0xFF6B7280),
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.width(8.dp))
            Text("%" + (v.confidence * 100).toInt(), fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, color = v.color)
        }
        Spacer(Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = { v.confidence },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            color = v.color,
            trackColor = Color.White.copy(alpha = 0.8f),
        )
        if (v.confidence < LOW_CONFIDENCE) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Model bu fotoğraflardan emin değil; sonuçları yalnızca fikir için kullanın.",
                fontSize = 14.sp, lineHeight = 18.sp, color = Color(0xFF4B5563),
            )
        }
    }
}

/** Fotoğrafların altında: tek cümlelik hüküm (ör. "ZEHİRLİ – yemeyin"). */
@Composable
private fun VerdictBox(v: Verdict) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(v.bg, RoundedCornerShape(18.dp))
            .border(1.5.dp, v.border, RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
            val ic = v.icon
            if (ic != null) {
                EdibilityIcon(ic, 34.dp, tint = v.color)
            } else {
                Icon(Icons.Outlined.Info, null, tint = v.color, modifier = Modifier.size(34.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(v.title, color = v.color, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, lineHeight = 24.sp)
                Spacer(Modifier.height(2.dp))
                Text(v.sub, fontSize = 14.5.sp, lineHeight = 19.sp, color = Color(0xFF33373D))
            }
    }
}

@Composable
private fun MismatchCard(guesses: List<Prediction>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF4E5)),
        border = BorderStroke(1.dp, Color(0xFFE8B86A)),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Warning, contentDescription = null, tint = Color(0xFFC77700))
                Spacer(Modifier.width(10.dp))
                Text(
                    "Fotoğraflar farklı mantarlara ait görünüyor",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF8A5300),
                )
            }
            Text(
                "Doğru sonuç için üç fotoğrafın da aynı mantarın üstten, yandan ve alttan " +
                    "görünüşü olması gerekir. Her fotoğraf ayrı ayrı değerlendirildiğinde:",
                style = MaterialTheme.typography.bodyMedium,
            )
            PhotoAngle.entries.zip(guesses).forEach { (angle, g) ->
                val name = SpeciesCatalog.find(g.label)?.latinName ?: g.label.replace('_', ' ')
                Text(
                    "• ${angle.title}: $name (%${(g.confidence * 100).roundToInt()})",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
            }
            Text(
                "Aynı mantarın fotoğraflarıyla yeniden deneyin.",
                style = MaterialTheme.typography.bodyMedium,
            )
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
