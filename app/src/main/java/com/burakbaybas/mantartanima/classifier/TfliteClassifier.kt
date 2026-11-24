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
        photosDisagree = each.indices.any { a ->
            each.indices.any { b ->
                a < b && tops[a] != tops[b] &&
                    each[a][tops[a]] >= SURE && each[b][tops[b]] >= SURE &&
                    each[a][tops[b]] < CROSS && each[b][tops[a]] < CROSS
            }
        }
        return probs.indices
            .sortedByDescending { probs[it] }
            .take(topK)
            .map { Prediction(labels[it], probs[it]) }
    }

    private fun probabilitiesFor(bitmap: Bitmap): FloatArray {
        val output = Array(1) { FloatArray(labels.size) }
        interpreter.run(toInputBuffer(bitmap), output)
        return output[0].let { if (looksLikeProbabilities(it)) it else softmax(it) }
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

/** Bir fotoğrafın kendi türünden "emin" sayılması için gereken olasılık. */
private const val SURE = 0.5f

/** Diğer fotoğrafın türüne verilen olasılık bundan düşükse, iki fotoğraf birbiriyle çelişiyor sayılır. */
private const val CROSS = 0.05f
