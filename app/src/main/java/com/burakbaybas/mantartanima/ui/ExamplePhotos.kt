package com.burakbaybas.mantartanima.ui

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.font.FontStyle
import com.burakbaybas.mantartanima.data.Species
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

data class ExamplePhoto(val file: String, val license: String, val attribution: String)

/** assets/examples.tsv: id, file, license, attribution. Uygulamaya gömülü, internet gerekmez. */
object ExampleCatalog {
    @Volatile private var cache: Map<String, List<ExamplePhoto>>? = null

    fun forSpecies(context: Context, id: String): List<ExamplePhoto> {
        val map = cache ?: load(context).also { cache = it }
        return map[id].orEmpty()
    }

    private fun load(context: Context): Map<String, List<ExamplePhoto>> = runCatching {
        context.assets.open("examples.tsv").bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines.drop(1).mapNotNull { line ->
                val c = line.split('\t')
                if (c.size < 4) null else c[0] to ExamplePhoto(c[1], c[2], c[3])
            }.groupBy({ it.first }, { it.second })
        }
    }.getOrDefault(emptyMap())
}

private fun credit(p: ExamplePhoto): String {
    // iNaturalist biçimi: "(c) Ad, some rights reserved (CC BY-NC)" -> "© Ad · CC BY-NC"
    val name = p.attribution
        .replace(Regex("""^\(c\)\s*""", RegexOption.IGNORE_CASE), "")
        .replace(Regex(""",?\s*(some|all|no) rights reserved.*$""", RegexOption.IGNORE_CASE), "")
        .trim()
    return if (name.isBlank()) p.license else "© $name · ${p.license}"
}

@Composable
fun ExamplePhotos(speciesId: String) {
    val context = LocalContext.current
    val photos = remember(speciesId) { ExampleCatalog.forSpecies(context, speciesId) }
    if (photos.isEmpty()) return
    val images: List<ImageBitmap?> = remember(speciesId) { loadExampleImages(context, photos) }
    var open by remember { mutableStateOf<Int?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFCFE2C3)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.PhotoLibrary, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text("Bu türün örnek fotoğrafları", fontWeight = FontWeight.Bold, color = Color(0xFF1F2A22), fontSize = 17.sp)
            }
            Spacer(Modifier.height(2.dp))
            Text("Kendi mantarınızla karşılaştırmak için dokunup büyütün.", fontSize = 13.sp, color = Color(0xFF5F6660))
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                images.forEachIndexed { i, img ->
                    if (img != null) {
                        Image(
                            img, contentDescription = "Örnek fotoğraf ${i + 1}", contentScale = ContentScale.Crop,
                            modifier = Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(10.dp))
                                .clickable { open = i },
                        )
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                }
                repeat(3 - images.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }

    open?.let { i ->
        val img = images.getOrNull(i)
        if (img != null) {
            Dialog(onDismissRequest = { open = null }) {
                Column(
                    Modifier.clip(RoundedCornerShape(14.dp)).background(Color.White).clickable { open = null },
                ) {
                    Image(img, null, contentScale = ContentScale.FillWidth, modifier = Modifier.fillMaxWidth())
                    Text(
                        credit(photos[i]), fontSize = 12.sp, color = Color(0xFF444A45),
                        modifier = Modifier.padding(10.dp),
                    )
                }
            }
        }
    }
}

data class Taxonomy(val kingdom: String, val phylum: String, val klass: String, val order: String, val family: String)

/** assets/taxonomy.tsv: id, kingdom, phylum, class, order, family (GBIF). */
object TaxonomyCatalog {
    @Volatile private var cache: Map<String, Taxonomy>? = null

    fun forSpecies(context: Context, id: String): Taxonomy? {
        val map = cache ?: load(context).also { cache = it }
        return map[id]
    }

    private fun load(context: Context): Map<String, Taxonomy> = runCatching {
        context.assets.open("taxonomy.tsv").bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines.drop(1).mapNotNull { line ->
                val c = line.split('\t')
                if (c.size < 6) null else c[0] to Taxonomy(c[1], c[2], c[3], c[4], c[5])
            }.toMap()
        }
    }.getOrDefault(emptyMap())
}

private fun loadExampleImages(context: Context, photos: List<ExamplePhoto>): List<ImageBitmap?> = photos.map { p ->
    runCatching {
        context.assets.open("examples/${p.file}").use { BitmapFactory.decodeStream(it).asImageBitmap() }
    }.getOrNull()
}

/** Benzer türün adına dokununca: üstte türün adı, altında 3 örnek fotoğrafı. */
@Composable
fun SpeciesPhotosDialog(species: Species, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val photos = remember(species.id) { ExampleCatalog.forSpecies(context, species.id) }
    val images = remember(species.id) { loadExampleImages(context, photos) }
    var sel by remember(species.id) { mutableStateOf(0) }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.clip(RoundedCornerShape(18.dp)).background(Color.White)
                .verticalScroll(rememberScrollState()).padding(16.dp),
        ) {
            Text(species.latinName, fontStyle = FontStyle.Italic, fontWeight = FontWeight.ExtraBold, fontSize = 21.sp,
                color = Color(0xFF1F2A22))
            if (species.turkishName.isNotBlank()) {
                Text(species.turkishName, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Color(0xFF444A45))
            }
            Spacer(Modifier.height(8.dp))
            EdibilityBadge(species.edibility)
            Spacer(Modifier.height(12.dp))
            if (images.none { it != null }) {
                Text("Bu tür için örnek fotoğraf yok.", fontSize = 14.sp, color = Color(0xFF5F6660))
            } else {
                images.getOrNull(sel)?.let { img ->
                    Image(img, null, contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(12.dp)))
                    Text(credit(photos[sel]), fontSize = 11.sp, color = Color(0xFF5F6660),
                        modifier = Modifier.padding(top = 4.dp))
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    images.forEachIndexed { i, img ->
                        if (img != null) {
                            Image(
                                img, "Örnek fotoğraf ${i + 1}", contentScale = ContentScale.Crop,
                                modifier = Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(8.dp))
                                    .border(if (i == sel) 3.dp else 0.dp, Color(0xFF2E7D32), RoundedCornerShape(8.dp))
                                    .clickable { sel = i },
                            )
                        } else Spacer(Modifier.weight(1f))
                    }
                    repeat(3 - images.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            Spacer(Modifier.height(10.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Text("Kapat", color = Color(0xFF17502A), fontWeight = FontWeight.Bold)
            }
        }
    }
}
