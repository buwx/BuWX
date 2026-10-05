package de.bsc.buwx

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.Window
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class MainActivity : AppCompatActivity() {
    private var swipeLayout: SwipeRefreshLayout? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Wx.DEV) Log.i(LOG_TAG, "onCreate")
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_main)

        val webView: WebView = findViewById(R.id.webview_content)

        webView.loadUrl(Wx.WEB_URL)
        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                swipeLayout!!.isRefreshing = false
            }

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest,
            ): Boolean {
                val url = request.url.toString()
                if (!url.startsWith(Wx.WEB_URL)) return false
                view.loadUrl(url)
                return true
            }
        }

        webView.setOnKeyListener { v, keyCode, event ->
            if (event!!.action == KeyEvent.ACTION_DOWN) {
                val webView1 = v as WebView
                if ((keyCode == KeyEvent.KEYCODE_BACK) && webView1.canGoBack()) {
                    webView1.goBack()
                    return@setOnKeyListener true
                }
            }
            false
        }

        swipeLayout = findViewById(R.id.swipe_container)
        swipeLayout!!.setOnRefreshListener {
            webView.reload()
        }
    }

    override fun onRestart() {
        super.onRestart()

        if (Wx.DEV) Log.i(LOG_TAG, "onRestart")

        val webView: WebView = findViewById(R.id.webview_content)
        webView.reload()
    }

    companion object {
        private const val LOG_TAG = "MainActivity"
    }
}
