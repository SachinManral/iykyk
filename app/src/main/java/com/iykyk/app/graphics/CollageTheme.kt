package com.iykyk.app.graphics

/**
 * Aesthetic themes available for collage generation.
 * Each theme provides distinct color palettes, frame geometry, decorative overlays, and typography.
 */
enum class CollageTheme(
    val id: String,
    val displayName: String,
    val iconEmoji: String,
    val description: String
) {
    FLORAL_SCRAPBOOK(
        id = "floral_scrapbook",
        displayName = "Floral Scrapbook",
        iconEmoji = "🌸",
        description = "Organic polaroid tilts with washi tape & floral accents"
    ),
    VINTAGE_FILM(
        id = "vintage_film",
        displayName = "Vintage 90s",
        iconEmoji = "🎞️",
        description = "35mm photobooth film strips with date stamps & grain"
    ),
    CYBER_GLOW(
        id = "cyber_glow",
        displayName = "Cyber Glow",
        iconEmoji = "✨",
        description = "Frosted glassmorphism cards with neon gradient borders"
    );

    companion object {
        fun fromId(id: String): CollageTheme {
            return entries.find { it.id == id } ?: FLORAL_SCRAPBOOK
        }
    }
}
