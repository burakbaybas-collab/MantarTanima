"""Tür ayrıntıları (app/src/main/assets/details.tsv) için kaynak veri.
Çalıştırınca details.tsv ve syndromes.tsv yazılır. Uzman kontrolünden geçmesi gereken TASLAK veridir.

details.tsv sütunları:
  id, features (§ ile ayrılmış kontrol listesi), compare (Tür|fark, § ile ayrılmış),
  smell, taste, color_change, syndrome (syndromes.tsv anahtarı)
"""
import csv
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

# --- Zehirlenme tabloları (sendromlar) ---
# anahtar: (başlık, toksin, belirti başlama süresi, belirtiler)
SYNDROMES = {
    "AMATOKSIN": ("Amatoksin zehirlenmesi (ölümcül)", "Amatoksinler (α-amanitin)", "6–24 saat sonra (çoğunlukla 8–12 saat)",
                  "Şiddetli kusma, sulu ishal, karın ağrısı. 1–2 gün sonra yalancı bir düzelme olur; ardından karaciğer ve "
                  "böbrek yetmezliği gelişir. Belirtiler geç başladığı için mide yıkama zamanı geçmiş olur; erken hastane "
                  "tedavisi hayat kurtarır."),
    "AMATOKSIN_SUPHE": ("Amatoksin içerebilir", "Bu cinste amatoksin içeren ölümcül türler var",
                        "6–24 saat sonra", "Ölümcül amatoksin zehirlenmesi gibi değerlendirilmelidir: gecikmeli kusma ve ishal, "
                        "ardından karaciğer hasarı."),
    "ORELLANIN": ("Orellanin zehirlenmesi (ölümcül, böbrek)", "Orellanin", "2–20 gün sonra (çoğunlukla 3–7 gün)",
                  "Yoğun susuzluk, bulantı, bel ağrısı, idrarın azalması. Böbrek yetmezliği kalıcı olabilir (diyaliz, nakil). "
                  "Belirtiler çok geç başladığı için mantarla ilişkilendirilmeyebilir."),
    "CORTINARIUS_SUPHE": ("Böbrek zehiri içerebilir", "Bu cinste orellanin içeren ölümcül türler var",
                          "Günler sonra olabilir", "Bulantı, susuzluk, idrar azalması gibi böbrek belirtileri; "
                          "orellanin zehirlenmesi gibi değerlendirilmelidir."),
    "GIROMITRIN": ("Giromitrin zehirlenmesi", "Giromitrin (monometilhidrazin)", "6–12 saat sonra",
                   "Kusma, ishal, baş ağrısı, halsizlik; ağır vakada karaciğer hasarı, kan yıkımı ve nöbet. "
                   "Pişirme buharı da zehirlidir; kurutma veya haşlama güvenli hale getirmez."),
    "CIG_ZEHIRLI": ("Çiğ veya az pişmişken zehirli", "Isıya duyarlı zehirli bileşikler (bazılarında giromitrin benzeri)",
                    "1–6 saat sonra", "Bulantı, kusma, ishal, karın krampları. Toplanması ve yenmesi önerilmez."),
    "MUSKARIN": ("Muskarin zehirlenmesi", "Muskarin", "15 dakika – 2 saat sonra",
                 "Aşırı terleme, salya ve gözyaşı, göz bebeklerinde küçülme, nabız yavaşlaması, ishal, nefes darlığı. "
                 "Hastanede panzehir (atropin) uygulanır."),
    "PANTERIN": ("Panterin zehirlenmesi (sinir sistemi)", "İbotenik asit ve muskimol", "30 dakika – 3 saat sonra",
                 "Sarhoşluk hali, şaşkınlık, baş dönmesi, halüsinasyon, ajitasyon ya da derin uyku, kas seğirmesi. "
                 "Panter mantarında daha ağır seyreder."),
    "PAXILLUS": ("Paxillus sendromu (bağışıklık kaynaklı)", "Bağışıklık tepkisi (tekrarlayan tüketimde)",
                 "1–3 saat sonra", "Kusma, ishal, karın ağrısı; ardından kırmızı kan hücrelerinin yıkımı ve böbrek yetmezliği. "
                 "Yıllarca sorunsuz yiyenlerde bile aniden ve ölümcül şekilde ortaya çıkabilir."),
    "KOPRIN": ("Koprin (alkolle) zehirlenmesi", "Koprin", "Alkol alındıktan 5 dakika – 2 saat sonra",
               "Yüz ve boyunda kızarma, çarpıntı, baş ağrısı, bulantı, tansiyon düşmesi. Mantarı yemeden önceki ve sonraki "
               "3 gün içinde alınan alkolle ortaya çıkar."),
    "RABDOMIYOLIZ": ("Kas yıkımı (rabdomiyoliz)", "Tam bilinmiyor; tekrarlayan ve bol tüketimde",
                     "24–72 saat sonra", "Kas ağrısı ve güçsüzlük, koyu renkli idrar; ağır vakada kalp ve böbrek etkilenir. "
                     "Ölümcül olabilir."),
    "PSIKOAKTIF": ("Psikoaktif zehirlenme", "Psilosibin / psilosin", "20 dakika – 1 saat sonra",
                   "Algı bozukluğu, halüsinasyon, kaygı ve panik, çarpıntı. Kazalara ve kendine zarar vermeye yol açabilir."),
    "PROXIMA": ("Böbrek zehirlenmesi (Amanita proxima)", "Alenik norlösin", "8–14 saat sonra",
                "Önce kusma ve ishal; 2–4 gün içinde akut böbrek yetmezliği. Yenen Amanita ovoidea ile karıştırılır."),
    "POLIPORIK": ("Poliporik asit zehirlenmesi", "Poliporik asit", "12 saat ve sonrası",
                  "Bulantı, görme bozukluğu, mor renkli idrar; böbrek ve karaciğer etkilenebilir."),
    "ENSEFALOPATI": ("Beyin etkilenmesi (ensefalopati)", "Tam bilinmiyor", "Günler sonra",
                     "Özellikle böbrek hastalarında bilinç bulanıklığı, nöbet; ölümcül vakalar bildirilmiştir."),
    "GI_AGIR": ("Ağır mide-bağırsak zehirlenmesi", "Mide-bağırsak tahriş edici maddeler", "30 dakika – 4 saat sonra",
                "Şiddetli kusma, ishal ve karın krampları; ciddi sıvı kaybı. Hastane tedavisi gerekebilir."),
    "GI": ("Mide-bağırsak zehirlenmesi", "Mide-bağırsak tahriş edici maddeler", "15 dakika – 4 saat sonra",
           "Bulantı, kusma, ishal, karın krampları. Genellikle 1–2 günde geçer; çocuk ve yaşlılarda sıvı kaybı tehlikelidir."),
}

SYNDROME_OF = {}
def put(key, ids):
    for i in ids.split():
        SYNDROME_OF[i] = key

put("AMATOKSIN", "amanita_phalloides amanita_verna amanita_virosa lepiota_brunneoincarnata lepiota_subincarnata "
                 "lepiota_castanea galerina_marginata")
put("AMATOKSIN_SUPHE", "lepiota_cristata lepiota_boudieri lepiota_erminea lepiota_grangei lepiota_oreadiformis "
                       "lepiota_magnispora lepiota_clypeolaria galerina_pumila galerina_atkinsoniana conocybe_aporos")
put("ORELLANIN", "cortinarius_orellanus cortinarius_rubellus")
put("CORTINARIUS_SUPHE", "cortinarius_cinnabarinus cortinarius_cinnamomeus cortinarius_cotoneus cortinarius_croceus "
                         "cortinarius_hinnuleus cortinarius_semisanguineus cystinarius_rubicundulus")
put("GIROMITRIN", "gyromitra_esculenta gyromitra_infula discina_ancilis")
put("CIG_ZEHIRLI", "verpa_bohemica helvella_crispa helvella_lacunosa helvella_acetabulum sarcosphaera_coronaria")
put("MUSKARIN", "inocybe_geophylla inocybe_petiginosa inocybe_cincinnata inocybe_asterospora inocybe_corydalina "
                "inocybe_curvipes inocybe_dulcamara inocybe_mixtilis inocybe_napipes inocybe_nitidiuscula inocybe_whitei "
                "inosperma_erubescens inosperma_bongardii pseudosperma_rimosum clitocybe_dealbata clitocybe_agrestis "
                "clitocybe_diatreta clitocybe_phyllophila clitocybe_fragrans leucocybe_candicans mycena_pura mycena_rosea "
                "mycena_pelianthina")
put("PANTERIN", "amanita_muscaria amanita_pantherina amanita_gemmata")
put("PAXILLUS", "paxillus_involutus paxillus_cuprinus paxillus_ammoniavirescens")
put("KOPRIN", "coprinopsis_atramentaria")
put("RABDOMIYOLIZ", "tricholoma_equestre")
put("PSIKOAKTIF", "psilocybe_semilanceata panaeolus_papilionaceus pluteus_salicinus gymnopilus_junonius")
put("PROXIMA", "amanita_proxima")
put("POLIPORIK", "hapalopilus_rutilans")
put("ENSEFALOPATI", "pleurocybella_porrigens")
put("GI_AGIR", "entoloma_sinuatum tricholoma_pardinum omphalotus_olearius chlorophyllum_molybdites rubroboletus_satanas")

# --- Kontrol listesi, fark tablosu, koku/tat/renk değişimi ---
# id: (özellikler, [(benzer tür, fark)], koku, tat, renk değişimi)
D = {
    "amanita_phalloides": (
        ["Şapka zeytin yeşili, sarımsı yeşil veya soluk; ince ışınsal lifli",
         "Lameller beyaz, sapa bağlı değil (serbest)",
         "Sapın üst kısmında sarkık beyaz zar halka",
         "Sap tabanında soğan gibi yumru ve onu saran beyaz torba (volva)",
         "Meşe, kayın, kestane gibi yaprak döken ağaçların altında"],
        [("Agaricus campestris (Çayır mantarı)", "Agaricus'ta lameller pembe, sonra çikolata kahvesi; sap tabanında torba yok"),
         ("Russula virescens (Küflü mantar)", "Russula'da halka ve torba yok; sap tebeşir gibi kırılır"),
         ("Amanita caesarea (Yumurta mantarı)", "Yumurta mantarında lameller ve sap sarı; köygöçürende beyaz")],
        "Gençken belirsiz, yaşlanınca tatlımsı-çürük patates kokusu", "Asla tadına bakmayın", "Değişmez"),
    "amanita_verna": (
        ["Mantarın tamamı beyaz (şapka, lamel, sap)",
         "Lameller beyaz ve serbest",
         "Sapta beyaz zar halka (yaşlıda düşebilir)",
         "Sap tabanında beyaz torba (volva); toprak içinde kalabilir, kazarak çıkarın",
         "İlkbahar ve yaz başı, meşe altında"],
        [("Agaricus campestris (Çayır mantarı)", "Agaricus'ta lameller pembe-kahverengi, torba yok, çayırda çıkar"),
         ("Calocybe gambosa (Mayıs mantarı)", "Mayıs mantarında halka ve torba yok; lameller sık ve krem; belirgin un kokusu"),
         ("Leucoagaricus leucothites", "Torba yok, halka sap üzerinde hareket eder; çoğunlukla sonbaharda çayırda")],
        "Hafif, belirsiz", "Asla tadına bakmayın", "Değişmez (potasyum hidroksitle sararmaz)"),
    "amanita_virosa": (
        ["Tamamen beyaz; genç şapka sivri-çan şeklinde",
         "Sap yüzeyi pul pul, lifli",
         "Yırtık, düzensiz beyaz halka",
         "Sap tabanında beyaz torba (volva)",
         "Asitli topraklı ladin, çam ve kayın ormanları"],
        [("Agaricus arvensis (At mantarı)", "At mantarında lameller pembe-kahverengi, torba yok, anason kokulu"),
         ("Agaricus silvicola", "Lameller beyazdan kahverengiye döner, torba yok, anason kokulu")],
        "Hafif, hoş olmayan", "Asla tadına bakmayın", "Şapka potasyum hidroksitle sararır"),
    "lepiota_brunneoincarnata": (
        ["Küçük şapka (2–5 cm), pembemsi kahverengi pullarla kaplı",
         "Sapta belirgin halka yok; halka yerinde pullu bir kuşak",
         "Lameller beyaz ve serbest",
         "Sap tabanında torba yok",
         "Çimenlik, park, bahçe ve orman kenarları"],
        [("Macrolepiota procera (Şemsiye mantarı)", "Şemsiye mantarı çok büyük (10–30 cm), sapta hareketli kalın halka ve yılan derisi deseni var"),
         ("Marasmius oreades (Halka mantarı)", "Halka mantarında pul yok, lameller seyrek, sap sert ve esnek")],
        "Meyvemsi, hoş", "Asla tadına bakmayın", "Değişmez"),
    "lepiota_subincarnata": (
        ["Küçük şapka (2–4 cm), pembemsi kahve küçük pullu",
         "Halka yok; sap pullu",
         "Lameller beyaz, serbest",
         "Çimenlik ve orman kenarı"],
        [("Marasmius oreades (Halka mantarı)", "Halka mantarında pul yok, lameller seyrek ve kalın, sap sert esnek")],
        "Meyvemsi", "Asla tadına bakmayın", "Değişmez"),
    "lepiota_castanea": (
        ["Küçük şapka (1–4 cm), kestane kahvesi pullu",
         "Sap tabanı kahverengi pullu, halka yok",
         "Lameller beyaz, yaşlanınca sarımsı",
         "Yaprak çöpü arasında"],
        [("Lepiota cristata", "Ondan da zehirli; şapkası beyaz zeminde kırmızımsı kahve pullu, halkalı, keskin kokulu")],
        "Hafif meyvemsi", "Asla tadına bakmayın", "Değişmez"),
    "galerina_marginata": (
        ["Küçük şapka (1–5 cm), bal-kahve; kurudukça soluklaşır",
         "Sapta ince zar halka (çabuk kaybolabilir); halkanın altı gümüşi lifli",
         "Spor izi pas kahverengi",
         "Ölü odun üzerinde, çoğunlukla iğne yapraklı ağaçta",
         "Tek tek veya küçük gruplar halinde"],
        [("Kuehneromyces mutabilis", "Sapın alt kısmı koyu ve pullu; büyük demetler halinde, yaprak döken ağaçta"),
         ("Armillaria mellea (Bal mantarı)", "Bal mantarında spor izi beyaz, şapka koyu pullu, büyük demetler")],
        "Un kokusu", "Asla tadına bakmayın", "Değişmez"),
    "gyromitra_esculenta": (
        ["Şapka beyin gibi kıvrım kıvrım, kırmızımsı kahve",
         "Boyuna kesince şapka ve sap içi odacıklı, pamuksu",
         "Sap kısa, kalın, beyazımsı",
         "İlkbahar; çam altında, kumlu topraklarda"],
        [("Morchella esculenta (Kuzugöbeği)", "Kuzugöbeğinde şapka bal peteği gibi çukurlu; boyuna kesince şapka ve sap tek bir boşluk")],
        "Hafif", "Asla tadına bakmayın", "Değişmez"),
    "gyromitra_infula": (
        ["Şapka eyer şeklinde, 2–3 loblu, kahverengi",
         "Sap içi boşluklu",
         "Sonbahar; çürük odun ve iğne yapraklı ormanlar"],
        [("Morchella (Kuzugöbeği)", "Kuzugöbeği ilkbaharda çıkar, şapkası bal peteği gibi çukurlu")],
        "Hafif", "Asla tadına bakmayın", "Değişmez"),
    "cortinarius_orellanus": (
        ["Şapka turuncu-pas renkli, kuru, ince keçemsi",
         "Lameller kalın, seyrek, pas turuncu",
         "Sapta örümcek ağı gibi zar (kortina) izi",
         "Spor izi pas kahverengi",
         "Meşe ve kayın altında"],
        [("Cantharellus cibarius (Horoz mantarı)", "Horoz mantarında gerçek lamel yok; sapa inen kalın kıvrımlar var, kayısı kokulu")],
        "Turp kokusu", "Asla tadına bakmayın", "Değişmez"),
    "cortinarius_rubellus": (
        ["Şapka turuncu-kahve, ortası sivri tümsekli",
         "Lameller pas turuncu",
         "Sapta sarımsı kuşaklar",
         "İğne yapraklı ormanda, yosunlu nemli yerler"],
        [("Craterellus tubaeformis (Horoz bacağı)", "Horoz bacağında lamel yerine sığ, çatallanan kıvrımlar; sap içi boş, gri-sarı")],
        "Turp kokusu", "Asla tadına bakmayın", "Değişmez"),
    "paxillus_involutus": (
        ["Şapka kenarı içe doğru kıvrık ve tüylü",
         "Lameller sapa doğru iner, kolayca sıyrılır",
         "Lameller dokununca kahverengileşir",
         "Süt akmaz; sapta halka yok"],
        [("Lactarius deliciosus (Kanlıca)", "Kanlıcada kesince turuncu süt akar ve sonra yeşillenir; Paxillus'ta süt yok")],
        "Ekşimsi", "Asla tadına bakmayın", "Lameller ve et dokununca koyu kahverengiye döner"),
    "amanita_muscaria": (
        ["Şapka kırmızı veya turuncu, üzerinde beyaz pullar (yağmurla silinebilir)",
         "Lameller ve sap beyaz",
         "Sapta beyaz halka",
         "Sap tabanı soğanlı, üzeri halka halka beyaz pul kuşakları"],
        [("Amanita caesarea (Yumurta mantarı)", "Yumurta mantarında lameller ve sap sarı, tabanda bütün beyaz torba var, şapkada pul yok")],
        "Belirsiz", "Asla tadına bakmayın", "Değişmez"),
    "amanita_pantherina": (
        ["Şapka kahverengi, üzerinde beyaz pullar",
         "Şapka kenarı çizgili",
         "Sapta beyaz halka, çizgisiz",
         "Sap tabanı soğanlı, üstünde kenarı belirgin yaka (kuşak)"],
        [("Amanita rubescens (Kızaran gelin)", "Kızaran gelinde et ve yaralar pembe-kırmızıya döner; halka çizgili; pullar grimsi"),
         ("Macrolepiota procera (Şemsiye mantarı)", "Şemsiye mantarında sap yılan derisi desenli, halka hareketli, tabanda torba yok")],
        "Turp kokusu", "Asla tadına bakmayın", "Değişmez (kırmızılaşmaz)"),
    "cantharellus_cibarius": (
        ["Yumurta sarısı-turuncu renk",
         "Lamel yerine kalın, çatallanan, sapa inen kıvrımlar",
         "Et beyazımsı, lifli, sert",
         "Kayısı kokusu",
         "Toprakta, meşe, kayın veya çam altında"],
        [("Hygrophoropsis aurantiaca (Yalancı horoz)", "Daha turuncu; ince, sık, gerçek lameller; koku yok, et yumuşak"),
         ("Omphalotus olearius (Zeytin mantarı)", "Odun veya kök üzerinde demet halinde; gerçek keskin lameller; zehirli"),
         ("Cortinarius orellanus", "Pas renkli gerçek lameller; ölümcül")],
        "Kayısı", "Hafif, sonradan hafif biberimsi", "Değişmez"),
    "craterellus_tubaeformis": (
        ["Şapka kahverengi, ince, ortası çukur",
         "Altta lamel değil, sığ ve çatallanan gri-sarı kıvrımlar",
         "Sap sarı, içi boş",
         "Sonbahar; iğne yapraklı ormanda yosun arasında"],
        [("Cortinarius rubellus", "Ölümcül; gerçek pas turuncu lameller, sap içi dolu, turp kokulu")],
        "Hafif, hoş", "Hafif", "Değişmez"),
    "craterellus_lutescens": (
        ["Şapka kahve, ince ve dalgalı kenarlı",
         "Alt yüz turuncu-pembemsi, hemen hemen düz veya hafif kıvrımlı",
         "Sap parlak turuncu-sarı, içi boş",
         "Nemli, yosunlu iğne yapraklı orman"],
        [("Cortinarius rubellus", "Ölümcül; gerçek pas turuncu lameller, sap içi dolu, turp kokulu")],
        "Meyvemsi", "Hafif", "Değişmez"),
    "lactarius_deliciosus": (
        ["Şapka turuncu, halka halka koyu zonlu",
         "Kesince havuç turuncusu süt akar",
         "Kesik ve yaralar zamanla yeşillenir",
         "Sapta küçük çukurcuklar",
         "Çam altında"],
        [("Lactarius torminosus", "Beyaz süt; şapka kenarı uzun tüylü; acı ve zehirli"),
         ("Lactarius sanguifluus (Kanlıca)", "Süt bordo-şarap kırmızısı; o da yenir"),
         ("Paxillus involutus", "Süt akmaz; lameller dokununca kahverengileşir; ölümcül olabilir")],
        "Hafif meyvemsi", "Tatlımsı, hafif acımsı", "Süt ve yaralar yeşile döner"),
    "lactarius_sanguifluus": (
        ["Şapka turuncu-pembe, soluk zonlu",
         "Kesince şarap kırmızısı-bordo süt akar",
         "Yaralar zamanla yeşillenir",
         "Çam altında, kireçli toprakta"],
        [("Lactarius deliciosus (Çıntar)", "Süt havuç turuncusu; o da yenir"),
         ("Lactarius torminosus", "Beyaz süt, tüylü şapka kenarı; zehirli")],
        "Hafif meyvemsi", "Hafif", "Süt ve yaralar yeşillenir"),
    "lactarius_semisanguifluus": (
        ["Şapka turuncu, zonlu",
         "Süt önce turuncu, birkaç dakikada şarap kırmızısına döner",
         "Yaralar yeşillenir",
         "Çam altında"],
        [("Lactarius torminosus", "Beyaz süt, tüylü şapka kenarı; zehirli")],
        "Hafif", "Hafif", "Süt turuncudan kırmızıya, yaralar yeşile döner"),
    "morchella_esculenta": (
        ["Şapka bal peteği gibi çukurlu-ağsı",
         "Boyuna kesince şapka ve sap içi tek bir boşluk",
         "Şapka sapa alt kenarından bitişik",
         "İlkbahar"],
        [("Gyromitra esculenta (Kızıl kulak)", "Şapka beyin gibi kıvrımlı; içi odacıklı; ölümcül"),
         ("Verpa bohemica", "Şapka sapın yalnızca tepesine bağlı, etek gibi serbest; sap içi pamuksu")],
        "Hafif, hoş", "Çiğ yenmez", "Değişmez"),
    "morchella_elata": (
        ["Şapka koyu, dikey sıralı çukurlar",
         "Boyuna kesince içi tek boşluk",
         "İlkbahar; yanık alan, iğne yapraklı orman"],
        [("Gyromitra esculenta (Kızıl kulak)", "Şapka beyin gibi kıvrımlı; içi odacıklı; ölümcül"),
         ("Verpa bohemica", "Şapka sapın yalnızca tepesine bağlı; sap içi pamuksu")],
        "Hafif, hoş", "Çiğ yenmez", "Değişmez"),
    "boletus_edulis": (
        ["Şapka kahverengi, ekmek kabuğu gibi",
         "Altta lamel yerine gözenekler; gençken beyaz, sonra sarı-yeşil",
         "Sap kalın, üst kısmında beyaz ağsı desen",
         "Kesince et beyaz kalır"],
        [("Tylopilus felleus (Safra mantarı)", "Gözenekler pembeleşir, sapta koyu kahve ağ; çok acı"),
         ("Rubroboletus satanas (Şeytan mantarı)", "Gözenekler kırmızı, sap kırmızı ağlı, kesince maviye döner; zehirli")],
        "Hoş, mantarımsı", "Fındıksı, tatlımsı", "Değişmez"),
    "macrolepiota_procera": (
        ["Büyük şapka (10–30 cm), ortası koyu tümsekli, kahve pullu",
         "Sap uzun, kahverengi yılan derisi desenli",
         "Kalın, sap üzerinde hareket ettirilebilen çift halka",
         "Tabanda torba yok"],
        [("Lepiota brunneoincarnata", "Küçük (2–5 cm), halka yok; ölümcül"),
         ("Chlorophyllum molybdites", "Spor izi yeşilimsi, yaşlı lameller yeşile çalar; zehirli"),
         ("Amanita pantherina (Panter mantarı)", "Tabanda soğanlı yaka, şapkada beyaz pullar, halka sabit; zehirli")],
        "Fındıksı", "Fındıksı", "Değişmez"),
    "calocybe_gambosa": (
        ["Şapka beyaz-krem, etli",
         "Lameller çok sık, beyaz-krem",
         "Halka ve torba yok",
         "Belirgin un (taze hamur) kokusu",
         "İlkbahar; çayır ve orman kenarlarında halka şeklinde"],
        [("Inosperma erubescens", "Şapka lifli, yaşlanınca ve yaralanınca kırmızılaşır; lameller kahveye döner; ölümcül olabilir"),
         ("Entoloma sinuatum", "Lameller pembeleşir, spor izi pembe; ağır zehirlenme yapar"),
         ("Amanita verna (Ak köygöçüren)", "Sapta halka ve tabanda torba var; ölümcül")],
        "Un (taze hamur)", "Unumsu", "Değişmez"),
    "agaricus_campestris": (
        ["Şapka beyaz, ipeksi",
         "Lameller gençken pembe, sonra çikolata kahvesi",
         "Sapta ince halka",
         "Tabanda torba yok",
         "Çayır ve otlaklarda"],
        [("Amanita verna / virosa (Ak köygöçüren)", "Lameller her zaman beyaz kalır; tabanda torba var; ölümcül"),
         ("Agaricus xanthodermus (Sarı leke mantarı)", "Sap tabanı kesince krom sarısına döner, mürekkep-fenol kokulu; zehirli")],
        "Hoş, mantarımsı", "Hafif", "Değişmez veya hafif pembeleşir"),
    "agaricus_arvensis": (
        ["Şapka beyaz, dokununca sarımsı",
         "Lameller grimsi pembe, sonra koyu kahve",
         "Sapta çark gibi çift halka",
         "Anason kokusu"],
        [("Agaricus xanthodermus (Sarı leke mantarı)", "Sap tabanı kesince krom sarısı, mürekkep kokusu; zehirli"),
         ("Amanita virosa", "Lameller beyaz, tabanda torba; ölümcül")],
        "Anason / badem", "Hoş", "Dokununca hafif sararır (taban hariç)"),
    "agaricus_xanthodermus": (
        ["Şapka beyaz, dokununca hemen sararır",
         "Sap tabanı kesince parlak krom sarısına döner",
         "Mürekkep veya fenol kokusu"],
        [("Agaricus arvensis (At mantarı)", "Anason kokulu; sap tabanı kesince sararmaz")],
        "Mürekkep, fenol (ısıtınca belirginleşir)", "Asla tadına bakmayın", "Sap tabanı krom sarısına döner"),
    "amanita_caesarea": (
        ["Şapka turuncu-kırmızı, pulsuz, kenarı çizgili",
         "Lameller ve sap sarı",
         "Sapta sarı halka",
         "Tabanda büyük beyaz torba (volva)",
         "Yaz-sonbahar; meşe ve kestane altında"],
        [("Amanita muscaria (Sinek mantarı)", "Şapkada beyaz pullar, lamel ve sap beyaz; zehirli"),
         ("Amanita phalloides (Köygöçüren)", "Yumurta halinde benzer; kesince içi beyaz ve yeşilimsi; ölümcül")],
        "Hoş", "Hafif tatlı", "Değişmez"),
    "armillaria_mellea": (
        ["Şapka bal sarısı, ortası koyu pullu",
         "Büyük demetler halinde, odun veya kök üzerinde",
         "Sapta beyaz-sarı halka",
         "Spor izi beyaz"],
        [("Galerina marginata", "Spor izi pas kahve, tek tek veya küçük grup; ölümcül"),
         ("Hypholoma fasciculare (Kükürt mantarı)", "Kükürt sarısı, lameller yeşilimsi, çok acı; zehirli")],
        "Hafif", "Çiğken acımsı; mutlaka iyi pişirilmeli", "Değişmez"),
    "kuehneromyces_mutabilis": (
        ["Şapka iki renkli: ortası açık, kenarı koyu (nemle)",
         "Sapın halka altı koyu ve pullu",
         "Büyük demetler halinde, yaprak döken ağaç kütüklerinde"],
        [("Galerina marginata", "Sap halka altında gümüşi lifli, pulsuz; ölümcül")],
        "Hoş", "Hafif", "Değişmez"),
    "coprinus_comatus": (
        ["Şapka uzun silindirik, beyaz ve tüylü pullu",
         "Lameller beyazdan pembeye, sonra siyah mürekkebe dönüşür",
         "Sap içi boş, ince halkalı",
         "Çayır, yol kenarı"],
        [("Coprinopsis atramentaria (Mürekkep mantarı)", "Gri, pulsuz şapka; alkolle birlikte zehirler")],
        "Hafif", "Hafif", "Lameller kararıp eriyerek mürekkebe dönüşür"),
    "marasmius_oreades": (
        ["Şapka küçük, açık kahve-krem, ortası tümsekli",
         "Lameller seyrek ve kalın",
         "Sap ince, sert ve esnek; bükünce kırılmaz",
         "Çimenlikte halka şeklinde"],
        [("Clitocybe dealbata", "Beyaz-unsu şapka, sık lameller sapa hafif iner; muskarin içerir"),
         ("Lepiota brunneoincarnata", "Şapka pullu, sap pullu kuşaklı; ölümcül")],
        "Acı badem / taze talaş", "Hoş", "Değişmez"),
    "russula_virescens": (
        ["Şapka yeşil, yüzeyi küçük yamalara bölünmüş (çatlak)",
         "Lameller beyaz-krem, kırılgan",
         "Halka ve torba yok",
         "Sap beyaz, tebeşir gibi kırılır"],
        [("Amanita phalloides (Köygöçüren)", "Sapta halka, tabanda torba var; şapka düz, çatlaksız; ölümcül")],
        "Hafif", "Hoş, fındıksı", "Değişmez"),
    "amanita_rubescens": (
        ["Şapka pembemsi kahve, grimsi pullu",
         "Et ve yaralar pembe-kırmızıya döner",
         "Halka üst yüzü çizgili",
         "Sap tabanı soğanlı, torba belirsiz"],
        [("Amanita pantherina (Panter mantarı)", "Kırmızılaşmaz, pullar saf beyaz, halka çizgisiz; zehirli")],
        "Hafif", "Çiğken zehirli; mutlaka pişirilmeli", "Kesik ve yaralar pembe-kırmızıya döner"),
    "pleurotus_eryngii": (
        ["Şapka kahverengi, etli",
         "Lameller sapa iner",
         "Çakşır ve şeytan tersi gibi bitkilerin köklerinde, toprak üstünde"],
        [("Omphalotus olearius (Zeytin mantarı)", "Turuncu renkli, odunda demet halinde; zehirli")],
        "Hoş", "Hoş", "Değişmez"),
    "omphalotus_olearius": (
        ["Parlak turuncu renk",
         "Gerçek, keskin, sapa inen lameller",
         "Zeytin, meşe gibi ağaçların kök veya odunu üzerinde demet halinde"],
        [("Cantharellus cibarius (Horoz mantarı)", "Lamel yerine kalın kıvrımlar; toprakta tek tek çıkar; kayısı kokulu")],
        "Hoş, belirsiz", "Asla tadına bakmayın", "Değişmez"),
    "entoloma_sinuatum": (
        ["Şapka gri-krem, etli, büyük",
         "Lameller önce sarımsı, sonra pembe",
         "Spor izi pembe",
         "Un kokusu"],
        [("Calocybe gambosa (Mayıs mantarı)", "Lameller beyaz kalır, spor izi beyaz; ilkbaharda çıkar")],
        "Un (taze hamur)", "Asla tadına bakmayın", "Değişmez"),
    "inosperma_erubescens": (
        ["Şapka konik, ışınsal lifli, kenarı yarılır",
         "Yaşlanınca ve yaralanınca kırmızılaşır",
         "Lameller kahverengiye döner",
         "İlkbahar-yaz; kayın ve ıhlamur altında"],
        [("Calocybe gambosa (Mayıs mantarı)", "Şapka düz, kırmızılaşmaz, lameller beyaz-krem, un kokulu")],
        "Meyvemsi", "Asla tadına bakmayın", "Et ve yaralar kırmızılaşır"),
    "coprinopsis_atramentaria": (
        ["Şapka gri-kahve, yumurta-çan biçimli, pulsuz",
         "Lameller siyahlaşıp mürekkebe dönüşür",
         "Demetler halinde, gömülü odun üzerinde"],
        [("Coprinus comatus", "Beyaz, uzun, tüylü pullu şapka; alkolle sorun yaratmaz")],
        "Hafif", "Asla tadına bakmayın", "Lameller siyah mürekkebe dönüşür"),
    "clitocybe_dealbata": (
        ["Küçük şapka, beyaz-unsu kaplamalı",
         "Lameller sık, sapa hafif iner",
         "Çimenlikte, halka şeklinde"],
        [("Marasmius oreades (Halka mantarı)", "Lameller seyrek ve kalın; sap sert ve esnek")],
        "Un kokusu", "Asla tadına bakmayın", "Değişmez"),
    "hypholoma_fasciculare": (
        ["Kükürt sarısı şapka",
         "Lameller yeşilimsi-sarı, sonra mor-siyah",
         "Odun üzerinde büyük demetler"],
        [("Armillaria mellea (Bal mantarı)", "Spor izi beyaz, şapka pullu, lameller beyazımsı"),
         ("Kuehneromyces mutabilis", "Sap pullu, şapka iki renkli, tadı hafif")],
        "Hafif", "Çok acı (tadına bakmayın)", "Değişmez"),
    "tricholoma_equestre": (
        ["Şapka sarı-yeşilimsi, ortası kahve pullu",
         "Lameller parlak kükürt sarısı",
         "Kumlu çam ormanı"],
        [("Tricholoma sulphureum", "Tümüyle kükürt sarısı, kömür gazı kokulu; zehirli")],
        "Un kokusu", "Asla tadına bakmayın", "Değişmez"),
}


def main():
    with open(os.path.join(ROOT, "app/src/main/assets/species.tsv"), encoding="utf-8") as f:
        ids = [r["id"] for r in csv.DictReader(f, delimiter="\t")]
        f.seek(0)
        ed = {r["id"]: r["edibility"] for r in csv.DictReader(f, delimiter="\t")}
    missing = [i for i in list(D) + list(SYNDROME_OF) if i not in ids]
    assert not missing, missing
    with open(os.path.join(ROOT, "app/src/main/assets/details.tsv"), "w", encoding="utf-8", newline="") as f:
        w = csv.writer(f, delimiter="\t", lineterminator="\n")
        w.writerow(["id", "features", "compare", "smell", "taste", "color_change", "syndrome"])
        for i in ids:
            syn = SYNDROME_OF.get(i) or ("GI" if ed[i] == "POISONOUS" else ("AMATOKSIN" if ed[i] == "DEADLY" else ""))
            if i not in D and not syn:
                continue
            feats, comp, smell, taste, color = D.get(i, ([], [], "", "", ""))
            w.writerow([i, "§".join(feats), "§".join(f"{a}|{b}" for a, b in comp), smell, taste, color, syn])
    with open(os.path.join(ROOT, "app/src/main/assets/syndromes.tsv"), "w", encoding="utf-8", newline="") as f:
        w = csv.writer(f, delimiter="\t", lineterminator="\n")
        w.writerow(["key", "title", "toxin", "onset", "symptoms"])
        for k, v in SYNDROMES.items():
            w.writerow([k, *v])
    print(len(D), "türde ayrıntı,", sum(1 for i in ids if SYNDROME_OF.get(i) or ed[i] in ("POISONOUS", "DEADLY")), "türde zehirlenme bilgisi")


if __name__ == "__main__":
    main()
