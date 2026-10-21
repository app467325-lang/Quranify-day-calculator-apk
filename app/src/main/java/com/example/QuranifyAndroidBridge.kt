package com.example

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.Toast
import java.io.OutputStream
import java.util.Locale

class QuranifyAndroidBridge(
  private val context: Context,
  private val webViewProvider: () -> WebView?,
) {
  private val mainHandler = Handler(Looper.getMainLooper())
  private var tts: TextToSpeech? = null
  private var ttsReady = false

  init {
    try {
      tts = TextToSpeech(context.applicationContext) { status ->
        if (status == TextToSpeech.SUCCESS) {
          val result = tts?.setLanguage(Locale("ar"))
          ttsReady = result != TextToSpeech.LANG_MISSING_DATA &&
            result != TextToSpeech.LANG_NOT_SUPPORTED
          tts?.setSpeechRate(0.92f)
        }
      }
    } catch (_: Exception) {
    }
  }

  @JavascriptInterface
  fun speakArabic(text: String) {
    if (text.isBlank()) return
    mainHandler.post {
      try {
        if (ttsReady) {
          tts?.stop()
          tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "quranify_tts_${System.currentTimeMillis()}")
        }
      } catch (_: Exception) {
      }
    }
  }

  @JavascriptInterface
  fun stopSpeaking() {
    mainHandler.post {
      try {
        tts?.stop()
      } catch (_: Exception) {
      }
    }
  }

  @JavascriptInterface
  fun setKeepScreenOn(enable: Boolean) {
    mainHandler.post {
      webViewProvider()?.keepScreenOn = enable
    }
  }

  @JavascriptInterface
  fun hapticTick() {
    mainHandler.post {
      try {
        @Suppress("DEPRECATION")
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (vibrator?.hasVibrator() == true) {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(14L, VibrationEffect.DEFAULT_AMPLITUDE))
          } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(14L)
          }
        }
      } catch (_: Exception) {
      }
    }
  }

  @JavascriptInterface
  fun saveCardPng(base64DataUrl: String, fileName: String) {
    mainHandler.post {
      try {
        val cleanBase64 = base64DataUrl.substringAfter("base64,", "")
        if (cleanBase64.isEmpty()) return@post
        val bytes = Base64.decode(cleanBase64, Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@post

        val safeName = if (fileName.endsWith(".png")) fileName else "$fileName.png"
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
          put(MediaStore.MediaColumns.DISPLAY_NAME, safeName)
          put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Quranify")
          }
        }

        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        if (uri != null) {
          val stream: OutputStream? = resolver.openOutputStream(uri)
          stream?.use {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
          }
          Toast.makeText(
            context,
            context.getString(R.string.card_saved_toast),
            Toast.LENGTH_SHORT
          ).show()
        }
      } catch (_: Exception) {
      }
    }
  }

  @JavascriptInterface
  fun shareCard(base64DataUrl: String, shareText: String) {
    mainHandler.post {
      try {
        val cleanBase64 = base64DataUrl.substringAfter("base64,", "")
        var imageUri: android.net.Uri? = null
        if (cleanBase64.isNotEmpty()) {
          val bytes = Base64.decode(cleanBase64, Base64.DEFAULT)
          val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
          if (bitmap != null) {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
              put(MediaStore.MediaColumns.DISPLAY_NAME, "Quranify_Card_${System.currentTimeMillis()}.png")
              put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Quranify")
              }
            }
            imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (imageUri != null) {
              resolver.openOutputStream(imageUri)?.use {
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
              }
            }
          }
        }

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
          if (imageUri != null) {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, imageUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
          } else {
            type = "text/plain"
          }
          putExtra(Intent.EXTRA_TEXT, shareText)
        }
        val chooser = Intent.createChooser(sendIntent, context.getString(R.string.share_card_title)).apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
      } catch (_: Exception) {
      }
    }
  }

  fun shutdown() {
    try {
      tts?.stop()
      tts?.shutdown()
    } catch (_: Exception) {
    }
  }
}
