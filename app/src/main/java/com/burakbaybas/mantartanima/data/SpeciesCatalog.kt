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
    val details: SpeciesDetails? = null,
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
/** Zehirlenme tablosu (assets/syndromes.tsv). */
data class Syndrome(val title: String, val toxin: String, val onset: String, val symptoms: String)

/** Ayrıntılar (assets/details.tsv): kontrol listesi, benzer türlerle farklar, koku/tat/renk değişimi, zehirlenme. */
data class SpeciesDetails(
    val features: List<String>,
    val compare: List<Pair<String, String>>,
    val smell: String,
    val taste: String,
    val colorChange: String,
    val syndrome: Syndrome?,
)

object SpeciesCatalog {
    private var byId: Map<String, Species> = emptyMap()

    val all: List<Species> get() = byId.values.sortedBy { it.latinName }

    fun load(context: Context) {
        if (byId.isNotEmpty()) return
        val syndromes = readTsv(context, "syndromes.tsv").associate { c ->
            c[0] to Syndrome(c.getOrElse(1) { "" }, c.getOrElse(2) { "" }, c.getOrElse(3) { "" }, c.getOrElse(4) { "" })
        }
        val details = readTsv(context, "details.tsv").associate { c ->
            fun list(i: Int) = c.getOrElse(i) { "" }.split('§').map { it.trim() }.filter { it.isNotEmpty() }
            c[0] to SpeciesDetails(
                features = list(1),
                compare = list(2).map { it.substringBefore('|').trim() to it.substringAfter('|', "").trim() },
                smell = c.getOrElse(3) { "" }.trim(),
                taste = c.getOrElse(4) { "" }.trim(),
                colorChange = c.getOrElse(5) { "" }.trim(),
                syndrome = syndromes[c.getOrElse(6) { "" }.trim()],
            )
        }
        byId = context.assets.open("species.tsv").bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines.drop(1)
                .filter { it.isNotBlank() }
                .mapNotNull { parse(it) }
                .map { it.copy(details = details[it.id]) }
                .associateBy { it.id }
        }
    }

    private fun readTsv(context: Context, name: String): List<List<String>> = runCatching {
        context.assets.open(name).bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines.drop(1).filter { it.isNotBlank() }.map { it.split('\t') }.toList()
        }
    }.getOrDefault(emptyList())

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

    private val byLatin: Map<String, Species> by lazy { byId.values.associateBy { it.latinName.lowercase() } }

    /** "Agaricus campestris (Çayır mantarı)" gibi bir metnin başındaki Latince addan türü bulur. */
    fun findByName(text: String): Species? {
        val m = Regex("""^\s*([A-Z][a-z-]+ [a-z-]+)""").find(text) ?: return null
        val name = m.groupValues[1].lowercase()
        if (name.endsWith(" türleri") || name.endsWith(" spp")) return null
        return byLatin[name] ?: byId[name.replace(' ', '_')]
    }
}
