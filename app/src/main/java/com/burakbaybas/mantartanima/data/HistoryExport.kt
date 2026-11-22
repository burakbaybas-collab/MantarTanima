package com.burakbaybas.mantartanima.data

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Seçilen geçmiş kayıtlarını dışa aktarır:
 *  - Tablo: Excel'in doğrudan açtığı CSV (Türkçe karakterli, ";" ayraçlı)
 *  - Paket: tablo + tüm fotoğraflar + Google Earth/Haritalar için KML, tek ZIP dosyasında
 */
object HistoryExport {

    private val tr = Locale("tr", "TR")

    fun csv(entries: List<HistoryEntry>): String {
        val header = listOf(
            "No", "Tarih", "Saat", "Türkçe ad", "Latince ad", "Yenilebilirlik", "Oran (%)",
            "2. olasılık", "3. olasılık", "Enlem", "Boylam", "Konum", "Konum kaynağı", "Harita bağlantısı", "Fotoğraflar",
        )
        val day = SimpleDateFormat("dd.MM.yyyy", tr)
        val hour = SimpleDateFormat("HH:mm", tr)
        val rows = entries.mapIndexed { i, e ->
            val top = e.predictions.firstOrNull()
            val sp = top?.let { SpeciesCatalog.find(it.label) }
            fun other(k: Int) = e.predictions.getOrNull(k)?.let { p ->
                val s = SpeciesCatalog.find(p.label)
                "${s?.displayName ?: p.label} (%${(p.confidence * 100).toInt()})"
            } ?: ""
            val p = e.place
            listOf(
                (i + 1).toString(), day.format(Date(e.time)), hour.format(Date(e.time)),
                sp?.turkishName ?: "", sp?.latinName ?: top?.label ?: "",
                sp?.edibility?.label ?: "", top?.let { (it.confidence * 100).toInt().toString() } ?: "",
                other(1), other(2),
                p?.let { "%.6f".format(Locale.US, it.lat) } ?: "", p?.let { "%.6f".format(Locale.US, it.lon) } ?: "",
                p?.name ?: "", p?.let { if (it.source == "foto") "Fotoğraf" else "Telefon" } ?: "",
                p?.let { "https://maps.google.com/?q=${it.lat},${it.lon}" } ?: "",
                e.photos.keys.joinToString(", ") { "${folderName(e)}/${it.name.lowercase()}.jpg" },
            )
        }
        return "﻿" + (listOf(header) + rows).joinToString("\r\n") { r -> r.joinToString(";") { quote(it) } }
    }

    private fun quote(s: String) = if (s.any { it == ';' || it == '"' || it == '\n' }) "\"" + s.replace("\"", "\"\"") + "\"" else s

    private fun folderName(e: HistoryEntry) =
        SimpleDateFormat("yyyy-MM-dd_HHmm", tr).format(Date(e.time)) + "_" +
            (e.predictions.firstOrNull()?.label ?: "kayit")

    private fun kml(entries: List<HistoryEntry>): String {
        val sb = StringBuilder("""<?xml version="1.0" encoding="UTF-8"?>
<kml xmlns="http://www.opengis.net/kml/2.2"><Document><name>FungiLens kayıtları</name>
""")
        entries.filter { it.place != null }.forEach { e ->
            val p = e.place!!
            val sp = e.predictions.firstOrNull()?.let { SpeciesCatalog.find(it.label) }
            val name = (sp?.displayName ?: e.predictions.firstOrNull()?.label ?: "Kayıt").replace("&", "&amp;").replace("<", "&lt;")
            sb.append("<Placemark><name>$name</name><description>")
                .append(SimpleDateFormat("dd.MM.yyyy HH:mm", tr).format(Date(e.time)))
                .append(sp?.let { " · " + it.edibility.label } ?: "")
                .append("</description><Point><coordinates>${p.lon},${p.lat},0</coordinates></Point></Placemark>\n")
        }
        return sb.append("</Document></kml>\n").toString()
    }

    /** Dışa aktarma dosyasını oluşturur (arka planda çağrılmalı). Dosya ve MIME türünü döndürür. */
    fun build(ctx: Context, entries: List<HistoryEntry>, withPhotos: Boolean): Pair<File, String> {
        val dir = File(ctx.cacheDir, "disari").apply { deleteRecursively(); mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmm", tr).format(Date())
        val sorted = entries.sortedBy { it.time }
        val file: File
        val mime: String
        if (!withPhotos) {
            file = File(dir, "FungiLens_kayitlar_$stamp.csv")
            file.writeText(csv(sorted), Charsets.UTF_8)
            mime = "text/csv"
        } else {
            file = File(dir, "FungiLens_kayitlar_$stamp.zip")
            ZipOutputStream(file.outputStream().buffered()).use { zip ->
                zip.putNextEntry(ZipEntry("kayitlar.csv")); zip.write(csv(sorted).toByteArray(Charsets.UTF_8)); zip.closeEntry()
                if (sorted.any { it.place != null }) {
                    zip.putNextEntry(ZipEntry("konumlar.kml")); zip.write(kml(sorted).toByteArray(Charsets.UTF_8)); zip.closeEntry()
                }
                sorted.forEach { e ->
                    e.photos.forEach { (angle, f) ->
                        if (f.exists()) {
                            zip.putNextEntry(ZipEntry("${folderName(e)}/${angle.name.lowercase()}.jpg"))
                            f.inputStream().use { it.copyTo(zip) }
                            zip.closeEntry()
                        }
                    }
                }
            }
            mime = "application/zip"
        }
        return file to mime
    }

    /** Android paylaşım menüsünü açar (e-posta, WhatsApp, Drive, Dosyalar'a kaydet...). */
    fun send(ctx: Context, file: File, mime: String) {
        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "FungiLens tanımlama kayıtları")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(Intent.createChooser(send, "Kayıtları paylaş / kaydet").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
