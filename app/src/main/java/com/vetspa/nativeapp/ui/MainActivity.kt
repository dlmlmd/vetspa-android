package com.vetspa.nativeapp.ui

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.webkit.CookieManager
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.vetspa.nativeapp.BuildConfig
import com.vetspa.nativeapp.R
import com.vetspa.nativeapp.data.api.MyCookieJar

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("vetspa_user", Context.MODE_PRIVATE)
        if (prefs.getInt("user_id", 0) == 0) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        // Đồng bộ cookie từ Retrofit sang WebView
        syncCookiesToWebView()

        val bottomNav = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNav)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> switchFragment(HomeFragment())
                R.id.nav_bookings -> switchFragment(BookingsFragment())
                R.id.nav_profile -> switchFragment(ProfileFragment())
            }
            true
        }

        if (savedInstanceState == null) {
            switchFragment(HomeFragment())
        }
    }

    private fun switchFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    private fun syncCookiesToWebView() {
        val raw = MyCookieJar.getSessionCookies() ?: return
        val domain = BuildConfig.WEB_APP_URL.let {
            it.substringAfter("://").substringBefore("/")
        }
        CookieManager.getInstance().setAcceptCookie(true)
        raw.split("; ").forEach { pair ->
            val parts = pair.split("=", limit = 2)
            if (parts.size == 2) {
                CookieManager.getInstance().setCookie(domain, "${parts[0]}=${parts[1]}; domain=$domain; path=/")
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().flush()
        }
    }
}
