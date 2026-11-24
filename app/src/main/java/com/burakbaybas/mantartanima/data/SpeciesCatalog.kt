package com.burakbaybas.mantartanima.data

import android.content.Context

enum class Edibility(val label: String) {
    EDIBLE("Yenir"),
    EDIBLE_WITH_CAUTION("Dikkatle yenir"),
    INEDIBLE("Yenmez"),
    POISONOUS("Zehirli"),
    DEADLY("Ölümcül zehirli"),
}

data class Species(
    val id: String,
    val latinName: String,
    val turkishName: String,
    val edibility: Edibility,
    val description: String,
    val lookalikes: List<String> = emptyList(),
    val season: String = "",
    val habitat: String = "",
    val sporePrint: String = "",
    val note: String = "",
    /** Yöresel / diğer Türkçe adlar (virgülle). */
    val otherNames: String = "",
) {
    /** Türkçe adı yoksa Latince adı gösterilir. */
    val displayName: String get() = turkishName.ifBlank { latinName }
}

/**
 * Tür bilgileri `assets/species.tsv` tablosundan okunur (sekmeyle ayrılmış).
 * Aynı tablo modeli eğitirken de kullanılır; yeni tür eklemek için yalnızca
 * tabloya satır eklemek yeterlidir.
 *
 * Sütunlar: id, taxon, turkish_name, edibility, description, lookalikes (; ile ayrılmış),
 *          season, habitat, spore_print, note, other_names
 */
object SpeciesCatalog {
    private var byId: Map<String, Species> = emptyMap()

    val all: List<Species> get() = byId.values.sortedBy { it.latinName }

    fun load(context: Context) {
        if (byId.isNotEmpty()) return
        byId = context.assets.open("species.tsv").bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines.drop(1)
                .filter { it.isNotBlank() }
                .mapNotNull { parse(it) }
                .associateBy { it.id }
        }
    }

    private fun parse(line: String): Species? {
        val c = line.split('\t')
        if (c.size < 4) return null
        val edibility = runCatching { Edibility.valueOf(c[3].trim()) }.getOrNull() ?: return null
        return Species(
            id = c[0].trim(),
            latinName = c[1].trim(),
            turkishName = c[2].trim(),
            edibility = edibility,
            description = c.getOrElse(4) { "" }.trim(),
            lookalikes = c.getOrElse(5) { "" }.split(';').map { it.trim() }.filter { it.isNotEmpty() },
            season = c.getOrElse(6) { "" }.trim(),
            habitat = c.getOrElse(7) { "" }.trim(),
            sporePrint = c.getOrElse(8) { "" }.trim(),
            note = c.getOrElse(9) { "" }.trim(),
            otherNames = c.getOrElse(10) { "" }.trim(),
        )
    }

    fun find(id: String): Species? = byId[id.trim().lowercase()]
}
