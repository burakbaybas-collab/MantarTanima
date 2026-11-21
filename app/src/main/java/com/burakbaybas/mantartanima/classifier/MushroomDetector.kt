package com.burakbaybas.mantartanima.classifier

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

/**
 * "Fotoğrafta mantar var mı?" kontrolü.
 * ImageNet'te eğitilmiş genel bir model (detector.tflite) kullanır; ImageNet'teki
 * mantar sınıflarının olasılığına bakar. Boş oda, insan, eşya gibi fotoğrafları eler.
 */
class MushroomDetector private constructor(private val interpreter: Interpreter) {

    /** Fotoğrafın mantar içerme olasılığına dair puan (0..1). */
    fun fungiScore(bitmap: Bitmap): Float {
        val output = Array(1) { FloatArray(1000) }
        interpreter.run(toInput(bitmap), output)
        val probs = output[0]
        val fungiSum = FUNGI_CLASSES.sumOf { probs[it].toDouble() }.toFloat()
        // Mantar sınıflarından biri ilk 10 tahmin arasındaysa da mantar sayılır
        val top10 = probs.indices.sortedByDescending { probs[it] }.take(10)
        val inTop10 = top10.any { it in FUNGI_CLASSES }
        return if (inTop10) maxOf(fungiSum, PHOTO_THRESHOLD) else fungiSum
    }

    fun looksLikeMushroom(bitmap: Bitmap): Boolean = fungiScore(bitmap) >= PHOTO_THRESHOLD

    fun close() = interpreter.close()

    private fun toInput(bitmap: Bitmap): ByteBuffer {
        val scaled = Bitmap.createScaledBitmap(bitmap, SIZE, SIZE, true)
        val buffer = ByteBuffer.allocateDirect(4 * SIZE * SIZE * 3).order(ByteOrder.nativeOrder())
        val pixels = IntArray(SIZE * SIZE)
        scaled.getPixels(pixels, 0, SIZE, 0, 0, SIZE, SIZE)
        for (p in pixels) {
            buffer.putFloat(((p shr 16) and 0xFF) / 255f)
            buffer.putFloat(((p shr 8) and 0xFF) / 255f)
            buffer.putFloat((p and 0xFF) / 255f)
        }
        buffer.rewind()
        return buffer
    }

    companion object {
        private const val FILE = "detector.tflite"
        private const val SIZE = 224

        /** Tek fotoğrafın "mantar var" sayılması için gereken en düşük puan. */
        const val PHOTO_THRESHOLD = 0.10f

        /** ImageNet: mushroom, coral fungus, agaric, gyromitra, stinkhorn, earthstar, hen-of-the-woods, bolete */
        private val FUNGI_CLASSES = setOf(947, 991, 992, 993, 994, 995, 996, 997)

        /** Model dosyası yoksa null döner (kontrol atlanır). */
        fun create(context: Context): MushroomDetector? = try {
            if (FILE !in context.assets.list("").orEmpty()) null
            else context.assets.openFd(FILE).use { fd ->
                FileInputStream(fd.fileDescriptor).use { stream ->
                    val model = stream.channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
                    MushroomDetector(Interpreter(model, Interpreter.Options().setNumThreads(4)))
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}
