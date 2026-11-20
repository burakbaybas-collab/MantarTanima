package com.burakbaybas.mantartanima.data

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
)

/**
 * Uygulamada bilgi kartı gösterilen türler.
 * `id`, modelin labels.txt dosyasındaki etiketle aynı olmalıdır.
 * Bu liste bir başlangıçtır; uzman kontrolünden geçirilip genişletilmelidir.
 */
object SpeciesCatalog {
    val all: List<Species> = listOf(
        Species(
            id = "amanita_phalloides",
            latinName = "Amanita phalloides",
            turkishName = "Köygöçüren",
            edibility = Edibility.DEADLY,
            description = "Zeytin yeşili-sarımsı şapka, beyaz lameller, sapta halka ve tabanda torba (volva). " +
                "Mantar zehirlenmesine bağlı ölümlerin büyük kısmından sorumludur.",
            lookalikes = listOf("Agaricus türleri", "Russula türleri"),
        ),
        Species(
            id = "amanita_muscaria",
            latinName = "Amanita muscaria",
            turkishName = "Sinek mantarı",
            edibility = Edibility.POISONOUS,
            description = "Kırmızı şapka üzerinde beyaz benekler, beyaz lameller ve sap.",
        ),
        Species(
            id = "agaricus_bisporus",
            latinName = "Agaricus bisporus",
            turkishName = "Kültür mantarı",
            edibility = Edibility.EDIBLE,
            description = "Beyaz veya kahverengi şapka; lameller pembeden koyu kahveye döner, torbası yoktur.",
            lookalikes = listOf("Amanita phalloides (genç dönemde)"),
        ),
        Species(
            id = "pleurotus_ostreatus",
            latinName = "Pleurotus ostreatus",
            turkishName = "İstiridye mantarı",
            edibility = Edibility.EDIBLE,
            description = "Ağaç gövdelerinde kümeler halinde, yelpaze biçimli şapka, sap kısa ve yanda.",
        ),
        Species(
            id = "lentinula_edodes",
            latinName = "Lentinula edodes",
            turkishName = "Şitake",
            edibility = Edibility.EDIBLE,
            description = "Kahverengi, kenarları içe kıvrık şapka; odun üzerinde kültürü yapılır.",
        ),
        Species(
            id = "flammulina_filiformis",
            latinName = "Flammulina filiformis",
            turkishName = "Enoki",
            edibility = Edibility.EDIBLE_WITH_CAUTION,
            description = "Kültür formu ince uzun beyaz saplı ve küçük şapkalıdır. İyice pişirilerek tüketilmelidir.",
        ),
        Species(
            id = "cantharellus_cibarius",
            latinName = "Cantharellus cibarius",
            turkishName = "Horoz mantarı (Sarıkız)",
            edibility = Edibility.EDIBLE,
            description = "Yumurta sarısı renk, gerçek lamel yerine sapa doğru inen kıvrımlar.",
            lookalikes = listOf("Omphalotus olearius (zehirli)"),
        ),
        Species(
            id = "morchella_esculenta",
            latinName = "Morchella esculenta",
            turkishName = "Kuzugöbeği",
            edibility = Edibility.EDIBLE_WITH_CAUTION,
            description = "Bal peteği görünümlü, içi boş şapka. Çiğ veya az pişmiş tüketildiğinde zehirlenme yapar.",
            lookalikes = listOf("Gyromitra esculenta (zehirli)"),
        ),
        Species(
            id = "lactarius_deliciosus",
            latinName = "Lactarius deliciosus",
            turkishName = "Kanlıca",
            edibility = Edibility.EDIBLE,
            description = "Turuncu şapka, kırıldığında turuncu süt salgılar; çam altında yetişir.",
        ),
        Species(
            id = "macrolepiota_procera",
            latinName = "Macrolepiota procera",
            turkishName = "Şemsiye mantarı",
            edibility = Edibility.EDIBLE_WITH_CAUTION,
            description = "Büyük, pullu şapka, uzun alaca desenli sap ve hareketli halka.",
            lookalikes = listOf("Küçük Lepiota türleri (ölümcül)", "Chlorophyllum molybdites (zehirli)"),
        ),
        Species(
            id = "amanita_pantherina",
            latinName = "Amanita pantherina",
            turkishName = "Panter mantarı",
            edibility = Edibility.POISONOUS,
            description = "Kahverengi şapka üzerinde beyaz benekler, sapta halka ve kenarlı torba.",
            lookalikes = listOf("Amanita rubescens"),
        ),
        Species(
            id = "agaricus_xanthodermus",
            latinName = "Agaricus xanthodermus",
            turkishName = "Sarı lekeli mantar",
            edibility = Edibility.POISONOUS,
            description = "Kültür mantarına benzer; sap tabanı kesilince parlak sarıya döner, mürekkep/fenol kokar.",
            lookalikes = listOf("Agaricus bisporus", "Agaricus campestris"),
        ),
        Species(
            id = "boletus_edulis",
            latinName = "Boletus edulis",
            turkishName = "Çörek mantarı",
            edibility = Edibility.EDIBLE,
            description = "Kahverengi şapka, lamel yerine beyazdan sarı-yeşile dönen gözenekler, kalın şişkin sap.",
            lookalikes = listOf("Tylopilus felleus (acı, yenmez)"),
        ),
        Species(
            id = "coprinus_comatus",
            latinName = "Coprinus comatus",
            turkishName = "Mürekkep mantarı",
            edibility = Edibility.EDIBLE_WITH_CAUTION,
            description = "Uzun silindirik pullu beyaz şapka; yaşlandıkça siyah sıvıya dönüşür. Genç toplanıp hemen pişirilmelidir.",
        ),
        Species(
            id = "gyromitra_esculenta",
            latinName = "Gyromitra esculenta",
            turkishName = "Yalancı kuzugöbeği",
            edibility = Edibility.DEADLY,
            description = "Beyin kıvrımlarını andıran kahverengi-kırmızımsı şapka. Gyromitrin içerir.",
            lookalikes = listOf("Morchella esculenta"),
        ),
        Species(
            id = "hypholoma_fasciculare",
            latinName = "Hypholoma fasciculare",
            turkishName = "Kükürt başlı mantar",
            edibility = Edibility.POISONOUS,
            description = "Odun üzerinde yoğun kümeler, kükürt sarısı şapka, yeşilimsi lameller; tadı acıdır.",
        ),
        Species(
            id = "omphalotus_olearius",
            latinName = "Omphalotus olearius",
            turkishName = "Zeytin mantarı",
            edibility = Edibility.POISONOUS,
            description = "Turuncu, kümeler halinde, zeytin ve meşe kütüklerinde; gerçek lamelleri vardır.",
            lookalikes = listOf("Cantharellus cibarius"),
        ),
    )

    private val byId = all.associateBy { it.id }

    fun find(id: String): Species? = byId[id.trim().lowercase()]
}
