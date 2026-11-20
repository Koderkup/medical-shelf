package com.example.medical_shelf.scanner

import android.content.Context
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.medical_shelf.data.local.AppDatabase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.InputStreamReader
import java.util.regex.Pattern

enum class ScanMode {
    BARCODE,
    EXPIRATION_DATE_OCR
}

class BarcodeAnalyzer(
    private val context: Context,
    private val onResult: (barcode: String, medicineName: String?) -> Unit
) : ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient()
    private val db = AppDatabase.getDatabase(context)
    private var isProcessing = false
    private val assetsDict: Map<String, String> by lazy { loadAssetsDict(context) }

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null || isProcessing) {
            imageProxy.close()
            return
        }

        isProcessing = true
        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        scanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                if (barcodes.isNotEmpty()) {
                    val rawValue = barcodes[0].rawValue ?: barcodes[0].displayValue
                    if (!rawValue.isNullOrBlank()) {
                        val parsedBarcode = extractBarcodeNumber(rawValue, barcodes[0].format)
                        
                        CoroutineScope(Dispatchers.IO).launch {
                            val cachedName = db.userBarcodeCacheDao().findNameByBarcode(parsedBarcode)
                            val name = cachedName ?: assetsDict[parsedBarcode]
                            onResult(parsedBarcode, name)
                        }
                    }
                }
            }
            .addOnCompleteListener {
                isProcessing = false
                imageProxy.close()
            }
    }

    private fun extractBarcodeNumber(rawValue: String, format: Int): String {
        val clean = rawValue.trim()
        if (format == Barcode.FORMAT_DATA_MATRIX && clean.startsWith("01") && clean.length >= 16) {
            val gtin14 = clean.substring(2, 16)
            return if (gtin14.startsWith("0")) gtin14.substring(1) else gtin14
        }
        return clean
    }

    private fun loadAssetsDict(context: Context): Map<String, String> {
        return try {
            context.assets.open("meds_dict.json").use { inputStream ->
                val reader = InputStreamReader(inputStream)
                val type = object : TypeToken<Map<String, String>>() {}.type
                Gson().fromJson(reader, type) ?: emptyMap()
            }
        } catch (e: Exception) {
            emptyMap()
        }
    }
}

class ExpirationDateOcrAnalyzer(
    private val onDateFound: (dateString: String) -> Unit
) : ImageAnalysis.Analyzer {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private var isProcessing = false

    private val datePatterns = listOf(
        Pattern.compile("""\b(0[1-9]|1[0-2])[./](20\d{2})\b"""), // MM.YYYY or MM/YYYY
        Pattern.compile("""\b(0[1-9]|[12]\d|3[01])[./](0[1-9]|1[0-2])[./](20\d{2})\b"""), // DD.MM.YYYY
        Pattern.compile("""\b(20\d{2})[-.](0[1-9]|1[0-2])\b""") // YYYY-MM
    )

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null || isProcessing) {
            imageProxy.close()
            return
        }

        isProcessing = true
        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        recognizer.process(inputImage)
            .addOnSuccessListener { textResult ->
                val fullText = textResult.text
                if (fullText.isNotBlank()) {
                    val extractedDate = findDateInText(fullText)
                    if (extractedDate != null) {
                        onDateFound(extractedDate)
                    }
                }
            }
            .addOnCompleteListener {
                isProcessing = false
                imageProxy.close()
            }
    }

    private fun findDateInText(text: String): String? {
        for (pattern in datePatterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val group = matcher.group()
                if (group != null) {
                    return formatStandardDate(group)
                }
            }
        }
        return null
    }

    private fun formatStandardDate(rawDate: String): String {
        val clean = rawDate.replace('/', '.').replace('-', '.')
        val parts = clean.split('.')
        return if (parts.size == 2 && parts[0].length == 2 && parts[1].length == 4) {
            // MM.YYYY -> MM.YYYY
            clean
        } else if (parts.size == 3 && parts[0].length == 2 && parts[1].length == 2) {
            // DD.MM.YYYY -> MM.YYYY
            "${parts[1]}.${parts[2]}"
        } else {
            clean
        }
    }
}