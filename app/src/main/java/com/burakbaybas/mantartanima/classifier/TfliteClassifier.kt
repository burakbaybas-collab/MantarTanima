package com.burakbaybas.mantartanima.classifier

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.exp
import kotlin.math.ln

/**
 * TensorFlow Lite görüntü sınıflandırıcı.
 * Beklenen girdi: [1, H, W, 3] float32, 0..1 aralığında (H ve W modelden okunur).
 * Beklenen çıktı: [1, N] float32.
 */
class TfliteClassifier(
    context: Context,
    modelFile: String,
    labelsFile: String,
) : MushroomClassifier {

    override val isDemo = false

    private val interpreter: Interpreter
    private val labels: List<String>
    private val inputWidth: Int
    private val inputHeight: Int

    /** Sıcaklık kalibrasyonu (assets/temperature.txt; yoksa 1 = değişiklik yok). Sıralamayı değiştirmez. */
    private val temperature: Float = runCatching {
        context.assets.open("temperature.txt").bufferedReader().readText().trim().toFloat()
    }.getOrDefault(1f).takeIf { it in 0.2f..5f } ?: 1f

    init {
        interpreter = Interpreter(loadModel(context, modelFile), Interpreter.Options().setNumThreads(4))
        labels = context.assets.open(labelsFile).bufferedReader().readLines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        val shape = interpreter.getInputTensor(0).shape() // [1, H, W, 3]
        inputHeight = shape[1]
        inputWidth = shape[2]
    }

    override var lastPhotoGuesses: List<Prediction> = emptyList()
        private set
    override var photosDisagree: Boolean = false
        private set

    override fun classify(bitmaps: List<Bitmap>, topK: Int): List<Prediction> {
        if (bitmaps.isEmpty()) return emptyList()
        val probs = FloatArray(labels.size)
        val each = bitmaps.map { probabilitiesFor(it) }
        for (p in each) {
            for (i in probs.indices) probs[i] += p[i] / bitmaps.size
        }
        val tops = each.map { p -> p.indices.maxByOrNull { p[it] } ?: 0 }
        lastPhotoGuesses = each.mapIndexed { k, p -> Prediction(labels[tops[k]], p[tops[k]]) }
        // Türler yerine cinsler karşılaştırılır: bir fotoğraf tür düzeyinde kararsız kalsa da
        // (ör. birçok Russula türüne bölünmüş olasılık) cins düzeyinde genelde emindir.
        val genusProbs = each.map { p ->
            val m = HashMap<String, Float>()
            for (i in p.indices) {
                val g = labels[i].substringBefore('_')
                m[g] = (m[g] ?: 0f) + p[i]
            }
            m
        }
        val topGenus = genusProbs.map { m -> m.maxByOrNull { it.value }!! }
        val averagedTop = probs.maxOrNull() ?: 0f
        photosDisagree = averagedTop < OVERALL_SURE && each.indices.any { a ->
            each.indices.any { b ->
                val ga = topGenus[a]
                val gb = topGenus[b]
                a < b && ga.key != gb.key &&
                    ga.value >= SURE && gb.value >= SURE &&
                    (genusProbs[a][gb.key] ?: 0f) < CROSS && (genusProbs[b][ga.key] ?: 0f) < CROSS
            }
        }
        return probs.indices
            .sortedByDescending { probs[it] }
            .take(topK)
            .map { Prediction(labels[it], probs[it]) }
    }

    /**
     * Test zamanı çoğaltma (TTA): fotoğrafın kendisi, aynalanmışı ve ortadan yakınlaştırılmış hali
     * ayrı ayrı değerlendirilip ortalanır. Mantar karede küçük kaldığında ve açı farklarında isabeti artırır.
     */
    private fun probabilitiesFor(bitmap: Bitmap): FloatArray {
        val views = listOf(bitmap, mirrored(bitmap), centerCrop(bitmap, 0.8f))
        val sum = FloatArray(labels.size)
        for (v in views) {
            val p = runOnce(v)
            for (i in sum.indices) sum[i] += p[i] / views.size
        }
        return sum
    }

    private fun runOnce(bitmap: Bitmap): FloatArray {
        val output = Array(1) { FloatArray(labels.size) }
        interpreter.run(toInputBuffer(bitmap), output)
        val raw = output[0]
        // Kalibrasyon: logit/T ile softmax. Model olasılık veriyorsa log(p) logit yerine kullanılır.
        val logits = if (looksLikeProbabilities(raw)) FloatArray(raw.size) { ln(raw[it].coerceAtLeast(1e-12f)) } else raw
        return softmax(FloatArray(logits.size) { logits[it] / temperature })
    }

    private fun mirrored(b: Bitmap): Bitmap {
        val m = android.graphics.Matrix().apply { preScale(-1f, 1f) }
        return Bitmap.createBitmap(b, 0, 0, b.width, b.height, m, true)
    }

    private fun centerCrop(b: Bitmap, frac: Float): Bitmap {
        val w = (b.width * frac).toInt().coerceAtLeast(1)
        val h = (b.height * frac).toInt().coerceAtLeast(1)
        return Bitmap.createBitmap(b, (b.width - w) / 2, (b.height - h) / 2, w, h)
    }

    override fun close() = interpreter.close()

    private fun toInputBuffer(bitmap: Bitmap): ByteBuffer {
        val scaled = Bitmap.createScaledBitmap(bitmap, inputWidth, inputHeight, true)
        val buffer = ByteBuffer.allocateDirect(4 * inputWidth * inputHeight * 3)
            .order(ByteOrder.nativeOrder())
        val pixels = IntArray(inputWidth * inputHeight)
        scaled.getPixels(pixels, 0, inputWidth, 0, 0, inputWidth, inputHeight)
        for (p in pixels) {
            buffer.putFloat(((p shr 16) and 0xFF) / 255f)
            buffer.putFloat(((p shr 8) and 0xFF) / 255f)
            buffer.putFloat((p and 0xFF) / 255f)
        }
        buffer.rewind()
        return buffer
    }

    private fun looksLikeProbabilities(values: FloatArray): Boolean =
        values.all { it in 0f..1f } && values.sum() in 0.98f..1.02f

    private fun softmax(values: FloatArray): FloatArray {
        val max = values.maxOrNull() ?: 0f
        val exps = values.map { exp((it - max).toDouble()) }
        val sum = exps.sum()
        return FloatArray(values.size) { (exps[it] / sum).toFloat() }
    }

    private fun loadModel(context: Context, file: String): MappedByteBuffer =
        context.assets.openFd(file).use { fd ->
            FileInputStream(fd.fileDescriptor).use { stream ->
                stream.channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
            }
        }
}

/** Bir fotoğrafın kendi cinsinden "emin" sayılması için gereken olasılık. */
private const val SURE = 0.5f

/** Fotoğrafların ortalamasında bir tür bu kadar öne çıkıyorsa çelişki uyarısı verilmez. */
private const val OVERALL_SURE = 0.5f

/** Diğer fotoğrafın cinsine verilen olasılık bundan düşükse, iki fotoğraf birbiriyle çelişiyor sayılır. */
private const val CROSS = 0.05f
