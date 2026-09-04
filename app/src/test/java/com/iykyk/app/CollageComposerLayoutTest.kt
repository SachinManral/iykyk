package com.iykyk.app

import com.iykyk.app.data.model.PersonIdentity
import com.iykyk.app.graphics.CollageLayouts
import com.iykyk.app.graphics.CollageTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class CollageComposerLayoutTest {

    @Test
    fun testLayoutSlotGenerationForThemesAndCounts() {
        val peopleCounts = listOf(1, 2, 3, 4, 5, 6, 8)
        val themes = CollageTheme.entries

        for (theme in themes) {
            for (count in peopleCounts) {
                val slots = CollageLayouts.getSlots(theme, count)
                assertEquals(
                    "Theme ${theme.name} for $count people should generate $count slots",
                    count.coerceAtMost(8),
                    slots.size
                )
                for (slot in slots) {
                    assertNotNull(slot)
                }
            }
        }
    }

    @Test
    fun testThemesExist() {
        assertEquals(3, CollageTheme.entries.size)
        assertEquals(CollageTheme.FLORAL_SCRAPBOOK, CollageTheme.fromId("floral_scrapbook"))
        assertEquals(CollageTheme.VINTAGE_FILM, CollageTheme.fromId("vintage_film"))
        assertEquals(CollageTheme.CYBER_GLOW, CollageTheme.fromId("cyber_glow"))
    }

    @Test
    fun testTapeStyles() {
        val styles = com.iykyk.app.graphics.TapeGenerator.TapeStyle.entries
        assertEquals(6, styles.size)
        assertEquals("KRAFT", com.iykyk.app.graphics.TapeGenerator.TapeStyle.KRAFT.name)
        assertEquals("BEIGE", com.iykyk.app.graphics.TapeGenerator.TapeStyle.BEIGE.name)
        assertEquals("PINK", com.iykyk.app.graphics.TapeGenerator.TapeStyle.PINK.name)
        assertEquals("SAGE_GREEN", com.iykyk.app.graphics.TapeGenerator.TapeStyle.SAGE_GREEN.name)
        assertEquals("GINGHAM_PINK", com.iykyk.app.graphics.TapeGenerator.TapeStyle.GINGHAM_PINK.name)
        assertEquals("TRANSPARENT", com.iykyk.app.graphics.TapeGenerator.TapeStyle.TRANSPARENT.name)
    }

    @Test
    fun testPersonIdentityModel() {
        val people5 = (1..5).map { id ->
            PersonIdentity(
                id = id,
                label = "Person $id",
                appearances = emptyList(),
                representativeShotTimestampMs = 1000L * id,
                representativeQualityScore = 0.9f
            )
        }

        assertEquals(5, people5.size)
        assertEquals("Person 1", people5[0].label)
        assertEquals(5, people5[4].id)
    }
}
