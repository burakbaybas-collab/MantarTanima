package com.burakbaybas.mantartanima.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import android.os.CancellationSignal
import com.burakbaybas.mantartanima.PhotoAngle
import com.burakbaybas.mantartanima.classifier.Prediction
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.coroutines.resume

/** Bir tanımanın yapıldığı yer. */
data class HistoryPlace(
    val lat: Double,
    val lon: Double,
    val accuracyM: Float?,
    /** "foto" = fotoğrafın içindeki konum bilgisi, "cihaz" = tanıma anındaki telefon konumu */
    val source: String,
    val name: String?,
)

data class HistoryEntry(
    val id: Long,
    val time: Long,
    val predictions: List<Prediction>,
    val place: HistoryPlace?,
    val photos: Map<PhotoAngle, File>,
)

/** Yapılan tanımaları (tarih, fotoğraflar, sonuçlar, konum) cihazda saklar. İnternete gönderilmez. */
object HistoryStore {
    private const val MAX_ENTRIES = 200

    private fun root(ctx: Context) = File(ctx.filesDir, "gecmis").apply { mkdirs() }
    private fun index(ctx: Context) = File(root(ctx), "liste.json")

    @Synchronized
    fun load(ctx: Context): List<HistoryEntry> {
        val f = index(ctx)
        if (!f.exists()) return emptyList()
        return try {
            val arr = JSONArray(f.readText())
            (0 until arr.length()).mapNotNull { parse(ctx, arr.getJSONObject(it)) }.sortedByDescending { it.time }
        } catch (e: Exception) {
            emptyList()
        }
    }

    @Synchronized
    fun add(ctx: Context, predictions: List<Prediction>, bitmaps: Map<PhotoAngle, Bitmap>, place: HistoryPlace?): HistoryEntry {
        val id = System.currentTimeMillis()
        val dir = File(root(ctx), id.toString()).apply { mkdirs() }
        val files = bitmaps.mapValues { (angle, bmp) ->
            val f = File(dir, "${angle.name.lowercase()}.jpg")
            val scaled = scaleDown(bmp, 900)
            f.outputStream().use { scaled.compress(Bitmap.CompressFormat.JPEG, 85, it) }
            f
        }
        val entry = HistoryEntry(id, id, predictions, place, files)
        val all = (listOf(entry) + load(ctx)).take(MAX_ENTRIES)
        // Sınırı aşan eski kayıtların klasörlerini sil
        val keep = all.map { it.id.toString() }.toSet()
        root(ctx).listFiles()?.filter { it.isDirectory && it.name !in keep }?.forEach { it.deleteRecursively() }
        write(ctx, all)
        return entry
    }

    @Synchronized
    fun delete(ctx: Context, id: Long) {
        File(root(ctx), id.toString()).deleteRecursively()
        write(ctx, load(ctx).filter { it.id != id })
    }

    @Synchronized
    fun clear(ctx: Context) {
        root(ctx).deleteRecursively()
    }

    private fun write(ctx: Context, list: List<HistoryEntry>) {
        val arr = JSONArray()
        list.forEach { e ->
            arr.put(JSONObject().apply {
                put("id", e.id)
                put("time", e.time)
                put("predictions", JSONArray().apply {
                    e.predictions.forEach { p -> put(JSONObject().put("label", p.label).put("conf", p.confidence.toDouble())) }
                })
                e.place?.let { p ->
                    put("place", JSONObject().apply {
                        put("lat", p.lat); put("lon", p.lon); put("source", p.source)
                        p.accuracyM?.let { put("acc", it.toDouble()) }
                        p.name?.let { put("name", it) }
                    })
                }
            })
        }
        val tmp = File(root(ctx), "liste.json.tmp")
        tmp.writeText(arr.toString())
        tmp.renameTo(index(ctx))
    }

    private fun parse(ctx: Context, o: JSONObject): HistoryEntry? {
        val id = o.getLong("id")
        val dir = File(root(ctx), id.toString())
        val photos = PhotoAngle.entries.associateWith { File(dir, "${it.name.lowercase()}.jpg") }.filterValues { it.exists() }
        val preds = o.getJSONArray("predictions").let { a ->
            (0 until a.length()).map { a.getJSONObject(it) }.map { Prediction(it.getString("label"), it.getDouble("conf").toFloat()) }
        }
        val place = o.optJSONObject("place")?.let { p ->
            HistoryPlace(
                p.getDouble("lat"), p.getDouble("lon"),
                if (p.has("acc")) p.getDouble("acc").toFloat() else null,
                p.optString("source", "cihaz"),
                p.optString("name").ifBlank { null },
            )
        }
        return HistoryEntry(id, o.getLong("time"), preds, place, photos)
    }

    private fun scaleDown(b: Bitmap, max: Int): Bitmap {
        val s = maxOf(b.width, b.height)
        if (s <= max) return b
        val r = max.toFloat() / s
        return Bitmap.createScaledBitmap(b, (b.width * r).toInt(), (b.height * r).toInt(), true)
    }
}

/** Konum bulma: önce fotoğrafların içindeki GPS bilgisi, yoksa telefonun o anki konumu. */
object PlaceFinder {

    fun hasPermission(ctx: Context) =
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    suspend fun find(ctx: Context, photoUris: List<Uri>): HistoryPlace? {
        val fromPhoto = photoUris.firstNotNullOfOrNull { exifLocation(ctx, it) }
        val loc = fromPhoto ?: deviceLocation(ctx)?.let {
            HistoryPlace(it.latitude, it.longitude, if (it.hasAccuracy()) it.accuracy else null, "cihaz", null)
        } ?: return null
        return loc.copy(name = placeName(ctx, loc.lat, loc.lon))
    }

    private fun exifLocation(ctx: Context, uri: Uri): HistoryPlace? = try {
        ctx.contentResolver.openInputStream(uri)?.use { stream ->
            val ll = FloatArray(2)
            if (ExifInterface(stream).getLatLong(ll) && !(ll[0] == 0f && ll[1] == 0f)) {
                HistoryPlace(ll[0].toDouble(), ll[1].toDouble(), null, "foto", null)
            } else null
        }
    } catch (e: Exception) {
        null
    }

    @SuppressLint("MissingPermission")
    private suspend fun deviceLocation(ctx: Context): Location? {
        if (!hasPermission(ctx)) return null
        val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .filter { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) }
        val fresh = providers.firstNotNullOfOrNull { provider ->
            withTimeoutOrNull(10_000) {
                suspendCancellableCoroutine<Location?> { cont ->
                    val signal = CancellationSignal()
                    cont.invokeOnCancellation { signal.cancel() }
                    try {
                        LocationManagerCompat.getCurrentLocation(lm, provider, signal, Executors.newSingleThreadExecutor()) {
                            if (cont.isActive) cont.resume(it)
                        }
                    } catch (e: Exception) {
                        if (cont.isActive) cont.resume(null)
                    }
                }
            }
        }
        if (fresh != null) return fresh
        // Son bilinen konum (en fazla 30 dakikalık)
        return providers.mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
            .filter { System.currentTimeMillis() - it.time < 30 * 60_000 }
            .maxByOrNull { it.time }
    }

    @Suppress("DEPRECATION")
    private fun placeName(ctx: Context, lat: Double, lon: Double): String? = try {
        if (!Geocoder.isPresent()) null
        else Geocoder(ctx, Locale("tr", "TR")).getFromLocation(lat, lon, 1)?.firstOrNull()?.let { a ->
            listOfNotNull(a.subLocality ?: a.locality, a.subAdminArea, a.adminArea)
                .distinct().filter { it.isNotBlank() }.joinToString(", ").ifBlank { null }
        }
    } catch (e: Exception) {
        null // internet yoksa yer adı olmadan kaydedilir
    }
}
