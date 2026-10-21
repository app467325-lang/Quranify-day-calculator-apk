package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string and verify bundled Quranify HTML asset`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Quranify", appName)

    val htmlContent = context.assets
      .open("public/assets/aistudio/Quranify_-_Liquid_Glass_Player__AI_Ayah_Sync__Companion__Prayer_Times___Athkar__4__3902246a.html")
      .bufferedReader()
      .use { it.readText() }

    assertTrue(htmlContent.contains("Quranify - Liquid Glass Player"))
    assertTrue(htmlContent.contains("ayahSyncViewport"))
    assertTrue(htmlContent.contains("LIVE_QURAN_RADIO_CHANNELS"))
    assertTrue(htmlContent.contains("ALLAH_99_NAMES"))
  }
}
