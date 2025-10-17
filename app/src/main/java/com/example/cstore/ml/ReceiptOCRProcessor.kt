package com.example.cstore.ml

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

data class ParsedReceiptData(
    val itemName: String? = null,
    val price: Double? = null,
    val category: String? = null,
    val description: String? = null
)

object ReceiptOCRProcessor {
    
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    
    /**
     * Process an image and extract receipt/price tag information
     */
    suspend fun processImage(bitmap: Bitmap): Result<ParsedReceiptData> {
        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val visionText = recognizer.process(image).await()
            
            val allText = visionText.textBlocks.joinToString("\n") { it.text }
            val parsedData = parseReceiptText(allText)
            
            Result.success(parsedData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Smart parser that extracts structured data from OCR text
     */
    private fun parseReceiptText(text: String): ParsedReceiptData {
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        
        // Extract price (look for currency symbols and decimal patterns)
        val price = extractPrice(lines)
        
        // Extract item name (usually first non-price line)
        val itemName = extractItemName(lines, price)
        
        // Infer category from keywords
        val category = inferCategory(text)
        
        // Build description from remaining text
        val description = buildDescription(lines, itemName, price)
        
        return ParsedReceiptData(
            itemName = itemName,
            price = price,
            category = category,
            description = description
        )
    }
    
    private fun extractPrice(lines: List<String>): Double? {
        val pricePatterns = listOf(
            Regex("""\$\s*(\d+(?:\.\d{2})?)"""),  // $25.99 or $ 25.99
            Regex("""(\d+\.\d{2})\s*\$"""),        // 25.99$
            Regex("""(?:price|cost|total)[:\s]+\$?(\d+(?:\.\d{2})?)""", RegexOption.IGNORE_CASE),
            Regex("""(\d+\.\d{2})""")               // Plain 25.99
        )
        
        for (line in lines) {
            for (pattern in pricePatterns) {
                val match = pattern.find(line)
                if (match != null) {
                    val priceStr = match.groupValues[1]
                    priceStr.toDoubleOrNull()?.let { return it }
                }
            }
        }
        return null
    }
    
    private fun extractItemName(lines: List<String>, price: Double?): String? {
        // Filter out lines that are just prices or common receipt headers
        val excludePatterns = listOf("receipt", "total", "subtotal", "tax", "qty", "date")
        
        for (line in lines) {
            val lowerLine = line.lowercase()
            
            // Skip if line contains excluded keywords
            if (excludePatterns.any { lowerLine.contains(it) }) continue
            
            // Skip if line is just a price
            if (price != null && line.contains(price.toString())) continue
            if (line.matches(Regex("""[\d\s\$.]+"""))) continue
            
            // Must have at least 3 characters and some letters
            if (line.length >= 3 && line.any { it.isLetter() }) {
                return line.take(100) // Limit length
            }
        }
        return null
    }
    
    private fun inferCategory(text: String): String? {
        val lowerText = text.lowercase()
        
        val categoryKeywords = mapOf(
            "Electronics" to listOf("phone", "laptop", "computer", "tablet", "camera", "headphone", "speaker", "monitor", "keyboard", "mouse"),
            "Clothing" to listOf("shirt", "pants", "dress", "jacket", "shoes", "sneaker", "coat", "sweater", "jeans", "hoodie"),
            "Home" to listOf("furniture", "chair", "table", "bed", "sofa", "lamp", "desk", "shelf", "cabinet", "mirror"),
            "Books" to listOf("book", "novel", "textbook", "magazine", "comic", "manual", "guide"),
            "Sports" to listOf("bike", "bicycle", "ball", "fitness", "gym", "exercise", "weight", "tennis", "golf"),
            "Outdoor" to listOf("tent", "camping", "hiking", "backpack", "cooler", "grill", "picnic")
        )
        
        for ((category, keywords) in categoryKeywords) {
            if (keywords.any { lowerText.contains(it) }) {
                return category
            }
        }
        
        return null
    }
    
    private fun buildDescription(lines: List<String>, itemName: String?, price: Double?): String? {
        val filtered = lines.filter { line ->
            val lowerLine = line.lowercase()
            // Exclude item name, price lines, and receipt metadata
            (itemName == null || !line.contains(itemName, ignoreCase = true)) &&
            (price == null || !line.contains(price.toString())) &&
            !lowerLine.matches(Regex("""[\d\s\$.]+""")) &&
            line.length > 5 &&
            !lowerLine.contains("receipt") &&
            !lowerLine.contains("total")
        }
        
        val combined = filtered.take(3).joinToString(". ")
        return if (combined.isNotBlank()) combined.take(200) else null
    }
}

