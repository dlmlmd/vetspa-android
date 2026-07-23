package com.vetspa.nativeapp.ui

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.vetspa.nativeapp.BuildConfig
import com.vetspa.nativeapp.R
import com.vetspa.nativeapp.data.api.ApiClient
import com.vetspa.nativeapp.data.api.MyCookieJar
import com.vetspa.nativeapp.data.model.FcmTokenRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = false
            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            userAgentString = settings.userAgentString + " VetSpaNative/1.0"
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                syncCookiesToJar()
                injectFcmToken()
            }
        }

        webView.webChromeClient = WebChromeClient()

        CookieManager.getInstance().setAcceptCookie(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
        }

        // Android interface bridge cho JS
        webView.addJavascriptInterface(WebAppInterface(), "AndroidBridge")

        webView.loadUrl(BuildConfig.WEB_APP_URL)
    }

    // Đồng bộ cookie từ WebView sang MyCookieJar (cho WorkManager poller)
    private fun syncCookiesToJar() {
        val cookies = CookieManager.getInstance().getCookie(BuildConfig.WEB_APP_URL) ?: return
        MyCookieJar.setSessionCookies(cookies)
    }

    // Gửi FCM token vào page qua JS
    private fun injectFcmToken() {
        val token = MyCookieJar.getFcmToken()
        if (token != null) {
            webView.evaluateJavascript(
                "if (window.fcmTokenCallback) window.fcmTokenCallback('$token');",
                null
            )
        }
    }

    // Bridge cho WebView JS gọi native
    inner class WebAppInterface {
        @android.webkit.JavascriptInterface
        fun getFcmToken(): String? = MyCookieJar.getFcmToken()

        @android.webkit.JavascriptInterface
        fun notifyFromWeb(title: String, message: String) {
            com.vetspa.nativeapp.util.NotifHelper.showNotification(
                this@MainActivity, title, message
            )
        }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }
}
