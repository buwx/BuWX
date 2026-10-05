package de.bsc.buwx

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

/**
 * Shows the weather station website in a WebView with pull-to-refresh.
 */
class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var swipeLayout: SwipeRefreshLayout
    private lateinit var errorView: View
    private var loadFailed = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, NAV_BAR_SCRIM),
        )
        super.onCreate(savedInstanceState)

        if (Wx.DEV) Log.i(LOG_TAG, "onCreate")
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webview_content)
        swipeLayout = findViewById(R.id.swipe_container)
        errorView = findViewById(R.id.error_view)
        applyWindowInsets()

        val backCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() = webView.goBack()
        }
        onBackPressedDispatcher.addCallback(this, backCallback)

        webView.settings.javaScriptEnabled = true
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                swipeLayout.isRefreshing = false
                errorView.isVisible = loadFailed
                webView.isInvisible = loadFailed
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError,
            ) {
                if (request.isForMainFrame) loadFailed = true
            }

            override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
                backCallback.isEnabled = view.canGoBack()
            }

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest,
            ): Boolean {
                if (isStationPage(request.url)) return false
                // links to other sites open in the browser
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, request.url))
                } catch (e: ActivityNotFoundException) {
                    if (Wx.DEV) Log.d(LOG_TAG, e.toString())
                }
                return true
            }
        }

        swipeLayout.setColorSchemeColors(ContextCompat.getColor(this, R.color.brand))
        swipeLayout.setOnRefreshListener { reload() }
        findViewById<View>(R.id.error_retry).setOnClickListener { reload() }

        swipeLayout.isRefreshing = true
        webView.loadUrl(Wx.WEB_URL)
    }

    override fun onRestart() {
        super.onRestart()

        if (Wx.DEV) Log.i(LOG_TAG, "onRestart")
        reload()
    }

    private fun reload() {
        loadFailed = false
        errorView.isVisible = false
        swipeLayout.isRefreshing = true
        webView.reload()
    }

    /** Keeps the content clear of the system bars and colors the status bar area. */
    private fun applyWindowInsets() {
        val statusBarBackground: View = findViewById(R.id.status_bar_background)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root)) { _, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout(),
            )
            statusBarBackground.updateLayoutParams { height = bars.top }
            for (content in listOf(swipeLayout, errorView)) {
                content.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    setMargins(bars.left, bars.top, bars.right, bars.bottom)
                }
            }
            WindowInsetsCompat.CONSUMED
        }
    }

    private fun isStationPage(uri: Uri): Boolean {
        val host = uri.host ?: return false
        val stationHost = Wx.WEB_URL.toUri().host ?: return false
        return host == stationHost || host.endsWith(".$stationHost")
    }

    companion object {
        private const val LOG_TAG = "MainActivity"

        /** Scrim behind the navigation bar on API levels without dark navigation icons. */
        private const val NAV_BAR_SCRIM = 0x801B1B1B.toInt()
    }
}
