package com.vetspa.nativeapp.data.api

import android.content.Context
import android.content.SharedPreferences
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class MyCookieJar : CookieJar {
    companion object {
        private const val PREFS_NAME = "vetspa_cookies"
        private var prefs: SharedPreferences? = null

        fun init(context: Context) {
            prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        prefs?.edit()?.putString("session_cookies", cookies.joinToString("; ") { "${it.name}=${it.value}" })?.apply()
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val raw = prefs?.getString("session_cookies", null) ?: return emptyList()
        return raw.split("; ").mapNotNull { pair ->
            val parts = pair.split("=", limit = 2)
            if (parts.size == 2) Cookie.Builder().name(parts[0]).value(parts[1]).domain(url.host()).build() else null
        }
    }
}
