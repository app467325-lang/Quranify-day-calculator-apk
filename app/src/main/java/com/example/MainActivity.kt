package com.example

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.example.ui.theme.MyApplicationTheme

private const val QURANIFY_ASSET_URL =
  "file:///android_asset/public/assets/aistudio/Quranify_-_Liquid_Glass_Player__AI_Ayah_Sync__Companion__Prayer_Times___Athkar__4__3902246a.html"

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    WindowCompat.getInsetsController(window, window.decorView).apply {
      isAppearanceLightStatusBars = false
      isAppearanceLightNavigationBars = false
    }
    setContent {
      MyApplicationTheme {
        QuranifyLiquidGlassApp(
          onFinishActivity = { finish() }
        )
      }
    }
  }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun QuranifyLiquidGlassApp(
  onFinishActivity: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  var webViewRef by remember { mutableStateOf<WebView?>(null) }
  var pendingFileCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
  var pendingGeoOrigin by remember { mutableStateOf<String?>(null) }
  var pendingGeoCallback by remember { mutableStateOf<GeolocationPermissions.Callback?>(null) }

  // Zero-permission Android Photo Picker for custom Umayyad Mosque background selection
  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri: Uri? ->
    val callback = pendingFileCallback
    pendingFileCallback = null
    if (uri != null) {
      callback?.onReceiveValue(arrayOf(uri))
    } else {
      callback?.onReceiveValue(emptyArray())
    }
  }

  // Runtime Location permission for accurate GPS Qibla Compass
  val locationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions()
  ) { permissions ->
    val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
      permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    val origin = pendingGeoOrigin
    val callback = pendingGeoCallback
    pendingGeoOrigin = null
    pendingGeoCallback = null
    if (origin != null && callback != null) {
      callback.invoke(origin, granted, false)
    }
  }

  val androidBridge = remember(context) {
    QuranifyAndroidBridge(context) { webViewRef }
  }

  DisposableEffect(Unit) {
    onDispose {
      androidBridge.shutdown()
      webViewRef?.destroy()
    }
  }

  // Handle Android system Back button across modals and tabs inside the Quranify UI
  BackHandler(enabled = true) {
    val wv = webViewRef
    if (wv == null) {
      onFinishActivity()
      return@BackHandler
    }
    wv.evaluateJavascript(
      """
      (function() {
        var bgDrop = document.getElementById('bgSoundDropdown');
        if (bgDrop && bgDrop.classList.contains('show')) {
          bgDrop.classList.remove('show');
          return 'HANDLED';
        }
        var openModals = [
          'visualCardStudioModal',
          'tadabburSheetModal',
          'khatmahScheduleModal',
          'khushooSmartModal',
          'fullPlayerModal',
          'athkarSheetModal',
          'prayerScheduleModal'
        ];
        for (var i = 0; i < openModals.length; i++) {
          var m = document.getElementById(openModals[i]);
          if (m && m.classList.contains('open')) {
            m.classList.remove('open');
            return 'HANDLED';
          }
        }
        if (typeof activeTabIndex !== 'undefined' && activeTabIndex !== 0 && typeof activateTabByIndex === 'function') {
          activateTabByIndex(0);
          return 'HANDLED';
        }
        return 'EXIT';
      })();
      """.trimIndent()
    ) { result ->
      if (result?.contains("HANDLED") != true) {
        onFinishActivity()
      }
    }
  }

  Scaffold(
    modifier = modifier
      .fillMaxSize()
      .background(Color.Black),
    containerColor = Color.Black,
    contentWindowInsets = WindowInsets.safeDrawing
  ) { _ ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color.Black)
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .testTag("quranify_root_container")
    ) {
      val webViewDesc = context.getString(R.string.webview_description)
      AndroidView(
        modifier = Modifier
          .fillMaxSize()
          .testTag("quranify_webview")
          .semantics { contentDescription = webViewDesc },
        factory = { ctx ->
          WebView(ctx).apply {
            layoutParams = ViewGroup.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT,
              ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(android.graphics.Color.BLACK)
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false

            settings.apply {
              javaScriptEnabled = true
              domStorageEnabled = true
              databaseEnabled = true
              mediaPlaybackRequiresUserGesture = false
              allowFileAccess = true
              allowContentAccess = true
              @Suppress("DEPRECATION")
              allowFileAccessFromFileURLs = true
              @Suppress("DEPRECATION")
              allowUniversalAccessFromFileURLs = true
              loadsImagesAutomatically = true
              mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
              cacheMode = WebSettings.LOAD_DEFAULT
              setGeolocationEnabled(true)
              useWideViewPort = true
              loadWithOverviewMode = true
              textZoom = 100
            }

            addJavascriptInterface(androidBridge, "AndroidBridge")

            webViewClient = object : WebViewClient() {
              override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
              ): Boolean = false

              override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                view?.evaluateJavascript(
                  """
                  (function() {
                    if (window._quranifyAndroidEnhancementsApplied) return;
                    window._quranifyAndroidEnhancementsApplied = true;

                    // Ensure .phone-frame seamlessly fills all Android screen sizes while preserving exact design
                    var st = document.createElement('style');
                    st.textContent = '.phone-frame { width: 100% !important; max-width: 100% !important; height: 100vh !important; max-height: 100vh !important; border-radius: 0 !important; } .bg-sound-dropdown { max-width: 100% !important; }';
                    document.head.appendChild(st);

                    // Bridge Arabic TTS to native Android TextToSpeech in addition to Web Speech API
                    var origSpeak = window.speakArabicText;
                    window.speakArabicText = function(text) {
                      if (window.AndroidBridge && window.AndroidBridge.speakArabic) {
                        window.AndroidBridge.speakArabic(text || '');
                      }
                      if (typeof origSpeak === 'function') {
                        try { origSpeak(text); } catch (e) {}
                      }
                    };

                    // Bridge Crystal Tick to subtle native Android haptic feedback
                    var origTick = window.playCrystalTick;
                    if (typeof origTick === 'function') {
                      window.playCrystalTick = function() {
                        origTick();
                        if (window.AndroidBridge && window.AndroidBridge.hapticTick) {
                          try { window.AndroidBridge.hapticTick(); } catch (e) {}
                        }
                      };
                    }

                    // Bridge Visual Card PNG download to Android MediaStore Gallery
                    var origDownload = window.downloadVisualCardPNG;
                    window.downloadVisualCardPNG = function() {
                      var canvas = document.getElementById('liquidGlassShareCanvas');
                      if (canvas && window.AndroidBridge && window.AndroidBridge.saveCardPng) {
                        if (typeof drawLiquidGlassVisualCardOnCanvas === 'function') {
                          drawLiquidGlassVisualCardOnCanvas('liquidGlassShareCanvas');
                        }
                        var dataUrl = canvas.toDataURL('image/png');
                        window.AndroidBridge.saveCardPng(dataUrl, 'Quranify-LiquidGlass-Card-' + Date.now() + '.png');
                        if (typeof showToast === 'function') {
                          showToast('📥 تم تصدير وحفظ البطاقة الزجاجية في المعرض بنجاح ✓');
                        }
                        return;
                      }
                      if (typeof origDownload === 'function') origDownload();
                    };

                    // Bridge Visual Card Share to Android native Share Sheet
                    var origShare = window.shareVisualCardNow;
                    window.shareVisualCardNow = async function() {
                      var canvas = document.getElementById('liquidGlassShareCanvas');
                      if (canvas && window.AndroidBridge && window.AndroidBridge.shareCard) {
                        if (typeof drawLiquidGlassVisualCardOnCanvas === 'function') {
                          drawLiquidGlassVisualCardOnCanvas('liquidGlassShareCanvas');
                        }
                        var dataUrl = canvas.toDataURL('image/png');
                        var p = (typeof currentStudioCardPayload !== 'undefined' && currentStudioCardPayload) ? currentStudioCardPayload : {};
                        var shareText = (p.arabicText || '') + '\n(' + (p.subtitle || '') + ')\n💡 ' + (p.reflectionNote || '') + '\n🎙️ استمع للتلاوة بصوت ' + (p.reciterName || '') + ':\n' + (p.audioUrl || '');
                        window.AndroidBridge.shareCard(dataUrl, shareText);
                        if (typeof showToast === 'function') {
                          showToast('📤 جاري مشاركة البطاقة المصورة والتلاوة...');
                        }
                        return;
                      }
                      if (typeof origShare === 'function') await origShare();
                    };

                    // Bridge Smart Khushoo Wake Lock to native Android screen wake lock
                    var origWake = window.acquireKhushooWakeLock;
                    window.acquireKhushooWakeLock = async function(enable) {
                      if (window.AndroidBridge && window.AndroidBridge.setKeepScreenOn) {
                        var allowWake = !!enable && document.getElementById('toggleKhushooWakeLock') && document.getElementById('toggleKhushooWakeLock').classList.contains('on');
                        window.AndroidBridge.setKeepScreenOn(allowWake);
                      }
                      if (typeof origWake === 'function') {
                        try { await origWake(enable); } catch (e) {}
                      }
                    };
                  })();
                  """.trimIndent(),
                  null
                )
              }
            }

            webChromeClient = object : WebChromeClient() {
              override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
              ): Boolean {
                pendingFileCallback?.onReceiveValue(emptyArray())
                pendingFileCallback = filePathCallback
                return try {
                  photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                  )
                  true
                } catch (_: Exception) {
                  pendingFileCallback = null
                  false
                }
              }

              override fun onGeolocationPermissionsShowPrompt(
                origin: String?,
                callback: GeolocationPermissions.Callback?
              ) {
                if (origin == null || callback == null) return
                val hasFine = ContextCompat.checkSelfPermission(
                  ctx,
                  Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                val hasCoarse = ContextCompat.checkSelfPermission(
                  ctx,
                  Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

                if (hasFine || hasCoarse) {
                  callback.invoke(origin, true, false)
                } else {
                  pendingGeoOrigin = origin
                  pendingGeoCallback = callback
                  locationPermissionLauncher.launch(
                    arrayOf(
                      Manifest.permission.ACCESS_FINE_LOCATION,
                      Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                  )
                }
              }

              override fun onPermissionRequest(request: PermissionRequest?) {
                request?.grant(request.resources)
              }
            }

            loadUrl(QURANIFY_ASSET_URL)
            webViewRef = this
          }
        }
      )
    }
  }
}
