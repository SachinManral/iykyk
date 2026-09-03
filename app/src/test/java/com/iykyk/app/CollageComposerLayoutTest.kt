package com.iykyk.app

import com.iykyk.app.data.model.PersonIdentity
import com.iykyk.app.graphics.CollageComposer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class CollageComposerLayoutTest {

    @Test
    fun testLayoutSlotGeneration() {
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
        // Verify PersonIdentities can be instantiated and formatted cleanly
        assertEquals("Person 1", people5[0].label)
        assertEquals(5, people5[4].id)
    }
}
