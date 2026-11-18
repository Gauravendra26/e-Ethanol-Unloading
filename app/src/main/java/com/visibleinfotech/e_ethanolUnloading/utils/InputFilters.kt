package com.visibleinfotech.e_ethanolUnloading.utils

/**
 * Centralized input filters to block Hindi (Devanagari) characters,
 * Hindi numerals (०-९), emoji, and other non-ASCII input across the app.
 *
 * All filters accept only ASCII characters (code 0-127).
 */
object InputFilters {

    // ─────────────────────────────────────────────────────────────
    // CORE FILTERS — return only allowed characters
    // ─────────────────────────────────────────────────────────────

    /**
     * Only English digits: 0-9
     * Blocks Hindi digits ०-९, Arabic digits, etc.
     */
    fun digits(input: String): String =
        input.filter { it in '0'..'9' }

    /**
     * Digits + optional single decimal point.
     * Allows: 1234.56
     * Blocks: Hindi digits, multiple dots (only first dot kept)
     */
    fun decimal(input: String): String {
        val sb = StringBuilder()
        var dotAdded = false
        for (c in input) {
            when {
                c in '0'..'9' -> sb.append(c)
                c == '.' && !dotAdded -> {
                    sb.append(c)
                    dotAdded = true
                }
            }
        }
        return sb.toString()
    }

    /**
     * English letters only: A-Z, a-z
     */
    fun alpha(input: String): String =
        input.filter { it in 'A'..'Z' || it in 'a'..'z' }

    /**
     * English letters + digits: A-Z, a-z, 0-9
     */
    fun alphanumeric(input: String): String =
        input.filter { it in 'A'..'Z' || it in 'a'..'z' || it in '0'..'9' }

    /**
     * Letters + digits + space (for names, addresses, etc.)
     */
    fun alphanumericWithSpace(input: String): String =
        input.filter { it in 'A'..'Z' || it in 'a'..'z' || it in '0'..'9' || it == ' ' }
            .replace(Regex("\\s+"), " ")   // collapse multiple spaces → single

    /**
     * Letters + space only (for driver/customer names)
     */
    fun name(input: String): String =
        input.filter { it in 'A'..'Z' || it in 'a'..'z' || it == ' ' }
            .replace(Regex("\\s+"), " ")

    /**
     * Indian vehicle number: A-Z, 0-9, hyphen, space.
     * Auto-uppercased and space-collapsed.
     */
    fun vehicleNumber(input: String): String =
        input.filter { it in 'A'..'Z' || it in 'a'..'z' || it in '0'..'9' || it == ' ' || it == '-' }
            .replace(Regex("\\s+"), " ")
            .uppercase()

    fun documentNumber(input: String): String =
        input.filter {
            it in 'A'..'Z' || it in 'a'..'z' || it in '0'..'9' ||
                    it == ' ' || it == '-' || it == '/' || it == '_'
        }.uppercase()

    /**
     * Mobile number: digits only, capped at [maxLength].
     */
    fun mobile(input: String, maxLength: Int = 10): String =
        input.filter { it in '0'..'9' }.take(maxLength)

    /**
     * ASCII-only free text (letters, digits, spaces, basic punctuation).
     * Use when you want to allow anything English but block Hindi/emoji.
     */
    fun asciiText(input: String): String =
        input.filter { it.code in 32..126 }

    // ─────────────────────────────────────────────────────────────
    // CONVENIENCE EXTENSIONS — cleaner call sites
    // ─────────────────────────────────────────────────────────────

    fun String.toDigitsOnly(): String = digits(this)
    fun String.toDecimalOnly(): String = decimal(this)
    fun String.toAlphaOnly(): String = alpha(this)
    fun String.toAlphanumericOnly(): String = alphanumeric(this)
    fun String.toNameCase(): String = name(this)
    fun String.toVehicleNumberCase(): String = vehicleNumber(this)
    fun String.toDocumentNumberCase(): String = documentNumber(this)
    fun String.toMobileNumber(maxLength: Int = 10): String = mobile(this, maxLength)
    fun String.toAsciiText(): String = asciiText(this)
}