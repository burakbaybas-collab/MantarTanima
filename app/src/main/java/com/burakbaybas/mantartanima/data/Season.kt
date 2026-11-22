package com.burakbaybas.mantartanima.data

/**
 * Türlerin "Sezon" metninden (ör. "Yaz sonu–sonbahar") hangi aylarda görüldüğünü çıkarır ve
 * bugünün ayına göre tahminlere yumuşak bir ağırlık verir. Model yanılsa bile mevsim dışı
 * bir türün öne çıkma olasılığını azaltır; türü tamamen elemez.
 */
object Season {
    /** Mevsim içi */
    const val IN = 1.0f
    /** Mevsimin bir ay öncesi/sonrası */
    const val NEAR = 0.6f
    /** Mevsim dışı */
    const val OUT = 0.3f
    /** Zehirli ve ölümcül türlerde ağırlık bunun altına inmez (uyarılar gizlenmesin). */
    const val DANGEROUS_FLOOR = 0.7f

    private val seasons = mapOf(
        "erken ilkbahar" to (2 to 4),
        "sıcak mevsim" to (5 to 9),
        "ilkbahar" to (3 to 5),
        "yaz" to (6 to 8),
        "sonbahar" to (9 to 11),
        "kış" to (12 to 2),
    )

    /** Türün görüldüğü aylar (1–12). Belirlenemezse null (yıl boyu sayılır). */
    fun months(text: String): Set<Int>? {
        // Türkçe "İ" küçültülünce noktalı "i̇" olur; sade "i"ye çevir
        val t = text.replace('İ', 'i').lowercase().replace("\u0307", "")
        if (t.isBlank() || "yıl boyu" in t || "kültür" in t || "iç mekân" in t) return null
        val core = t.substringBefore("(").substringBefore(",").substringBefore(";").trim()
        val result = mutableSetOf<Int>()
        for (part in core.split(" ve ")) {
            val ends = part.split("–", "-").map { it.trim() }.filter { it.isNotEmpty() }
            if (ends.isEmpty()) continue
            val start = bound(ends.first(), first = true) ?: return null
            val end = bound(ends.last(), first = false) ?: return null
            var m = start
            while (true) {
                result += m
                if (m == end) break
                m = m % 12 + 1
            }
        }
        return result.ifEmpty { null }
    }

    /** "yaz sonu" -> 8, "yaz başı" -> 6, "yaz" -> 6 (başlangıç) / 8 (bitiş) */
    private fun bound(term: String, first: Boolean): Int? {
        val name = seasons.keys.firstOrNull { term.startsWith(it) } ?: return null
        val (a, b) = seasons.getValue(name)
        return when {
            "sonu" in term -> b
            "başı" in term -> a
            first -> a
            else -> b
        }
    }

    fun weight(species: Species?, month: Int): Float {
        val months = species?.let { months(it.season) } ?: return IN
        val w = when {
            month in months -> IN
            ((month + 10) % 12 + 1) in months || (month % 12 + 1) in months -> NEAR
            else -> OUT
        }
        val dangerous = species.edibility == Edibility.POISONOUS || species.edibility == Edibility.DEADLY
        return if (dangerous) maxOf(w, DANGEROUS_FLOOR) else w
    }
}
