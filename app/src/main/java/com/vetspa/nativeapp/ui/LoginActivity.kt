package com.vetspa.nativeapp.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.vetspa.nativeapp.BuildConfig
import com.vetspa.nativeapp.R

class LoginActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("vetspa_user", Context.MODE_PRIVATE)
        if (prefs.getInt("user_id", 0) > 0) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_login)

        // Xoá sạch cookie cũ trước khi login
        CookieManager.getInstance().removeAllCookies(null)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().flush()
        }

        webView = findViewById(R.id.loginWebView)
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            userAgentString = this.userAgentString + " VetSpaNative/1.0"
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // Khi thấy URL chứa index2.php → đã login thành công
                if (url?.contains("index2.php") == true) {
                    saveUserAndNavigate()
                }
            }

            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                return false
            }
        }

        webView.webChromeClient = WebChromeClient()
        webView.loadUrl(BuildConfig.WEB_APP_URL)
    }

    private fun saveUserAndNavigate() {
        // Lấy cookie từ WebView
        val cookies = CookieManager.getInstance().getCookie(BuildConfig.WEB_APP_URL) ?: ""
        getSharedPreferences("vetspa_cookies", Context.MODE_PRIVATE).edit()
            .putString("session_cookies", cookies).apply()

        // Inject JS lấy thông tin user từ trang
        webView.evaluateJavascript(
            "(function() {" +
            "  var el = document.querySelector('.userbox span');" +
            "  var name = el ? el.textContent.trim() : '';" +
            "  var badge = document.querySelector('.badge');" +
            "  var role = badge ? badge.textContent.trim() : 'customer';" +
            "  return JSON.stringify({fullname: name, role: role});" +
            "})()",
            android.webkit.ValueCallback { raw ->
                try {
                    val gson = com.google.gson.Gson()
                    val info = gson.fromJson(raw, Map::class.java)
                    getSharedPreferences("vetspa_user", Context.MODE_PRIVATE).edit()
                        .putInt("user_id", 1)
                        .putString("username", info["fullname"]?.toString() ?: "")
                        .putString("fullname", info["fullname"]?.toString() ?: "")
                        .putString("role", info["role"]?.toString()?.lowercase() ?: "customer")
                        .apply()
                } catch (_: Exception) {}
                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                finish()
            }
        )
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }
}
