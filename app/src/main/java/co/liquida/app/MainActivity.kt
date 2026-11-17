package co.liquida.app

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.webkit.WebViewAssetLoader
import android.webkit.WebViewClient
import java.io.File

class MainActivity : Activity() {

    private lateinit var web: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val root = FrameLayout(this).apply { setBackgroundColor(Color.parseColor("#EEF2EF")) }
        web = WebView(this)
        root.addView(web, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        setContentView(root)

        // Deja espacio para la barra de estado, la de navegación y el teclado
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            WindowInsetsCompat.CONSUMED
        }

        val loader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.textZoom = 100
        web.setBackgroundColor(Color.parseColor("#EEF2EF"))
        web.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
                loader.shouldInterceptRequest(request.url)
        }
        web.addJavascriptInterface(Bridge(), "Android")
        if (savedInstanceState != null) web.restoreState(savedInstanceState)
        else web.loadUrl("https://appassets.androidplatform.net/assets/index.html")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        web.saveState(outState)
    }

    @Deprecated("Se mantiene para Android 12 y anteriores")
    override fun onBackPressed() {
        web.evaluateJavascript("(window.__back && window.__back()) === true") { result ->
            if (result != "true") finish()
        }
    }

    inner class Bridge {
        @JavascriptInterface
        fun copy(text: String) {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("Liquida", text))
        }

        @JavascriptInterface
        fun savePdf(base64: String, filename: String) {
            val bytes = try { Base64.decode(base64, Base64.DEFAULT) } catch (e: Exception) { return }
            val safeName = filename.replace(Regex("[\\\\/:*?\"<>|]"), "_")
            runOnUiThread { deliverPdf(bytes, safeName) }
        }
    }

    private fun deliverPdf(bytes: ByteArray, name: String) {
        // 1) Guardar en Descargas (Android 10 o superior, sin pedir permisos)
        var saved = false
        if (Build.VERSION.SDK_INT >= 29) {
            try {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, name)
                    put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                    saved = true
                }
            } catch (e: Exception) {
                saved = false
            }
        }
        // 2) Abrir el menú para compartir (WhatsApp, correo, Drive…)
        try {
            val dir = File(cacheDir, "pdf").apply { mkdirs() }
            val file = File(dir, name)
            file.writeBytes(bytes)
            val uri = FileProvider.getUriForFile(this, "$packageName.files", file)
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(send, "Compartir PDF"))
        } catch (e: Exception) {
            // si no se puede compartir, al menos queda guardado
        }
        Toast.makeText(this, if (saved) "PDF guardado en Descargas" else "PDF listo para compartir", Toast.LENGTH_LONG).show()
    }
}
