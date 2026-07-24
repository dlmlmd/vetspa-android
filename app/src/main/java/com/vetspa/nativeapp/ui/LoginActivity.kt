package com.vetspa.nativeapp.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.vetspa.nativeapp.R
import com.vetspa.nativeapp.data.api.ApiClient
import com.vetspa.nativeapp.data.model.LoginResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var usernameInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var errorText: TextView
    private lateinit var loginBtn: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("vetspa_user", Context.MODE_PRIVATE)
        if (prefs.getInt("user_id", 0) > 0) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_login)

        usernameInput = findViewById(R.id.usernameInput)
        passwordInput = findViewById(R.id.passwordInput)
        errorText = findViewById(R.id.errorText)
        loginBtn = findViewById(R.id.loginBtn)

        passwordInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                performLogin()
                true
            } else false
        }

        loginBtn.setOnClickListener { performLogin() }

        findViewById<TextView>(R.id.staffLoginLink)?.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://spa.vetmedia.vn/staff_login.php"))
            startActivity(intent)
        }
    }

    private fun performLogin() {
        val username = usernameInput.text?.toString()?.trim() ?: ""
        val password = passwordInput.text?.toString() ?: ""

        if (username.isEmpty() || password.isEmpty()) {
            showError("Vui lòng nhập tên đăng nhập và mật khẩu")
            return
        }

        loginBtn.isEnabled = false
        loginBtn.text = "Đang đăng nhập..."
        errorText.visibility = android.view.View.GONE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val resp = ApiClient.api.login(username, password)
                withContext(Dispatchers.Main) {
                    if (resp.isSuccessful) {
                        val body = resp.body()
                        if (body != null && body.ok && body.user != null) {
                            saveUserAndNavigate(body.user)
                        } else {
                            showError(body?.error ?: "Đăng nhập thất bại")
                        }
                    } else {
                        // Try to parse error from response body
                        val errorBody = resp.errorBody()?.string()
                        val msg = try {
                            val gson = com.google.gson.Gson()
                            val err = gson.fromJson(errorBody, LoginResponse::class.java)
                            err.error ?: "Lỗi máy chủ (${resp.code()})"
                        } catch (_: Exception) {
                            "Lỗi máy chủ (${resp.code()})"
                        }
                        showError(msg)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showError("Lỗi kết nối: ${e.localizedMessage ?: "Không thể kết nối máy chủ"}")
                }
            } finally {
                withContext(Dispatchers.Main) {
                    loginBtn.isEnabled = true
                    loginBtn.text = "Đăng nhập"
                }
            }
        }
    }

    private fun saveUserAndNavigate(user: com.vetspa.nativeapp.data.model.User) {
        getSharedPreferences("vetspa_user", Context.MODE_PRIVATE).edit()
            .putInt("user_id", user.id)
            .putString("username", user.username)
            .putString("fullname", user.fullname ?: user.username)
            .putString("role", user.role)
            .putString("email", user.email ?: "")
            .putString("phone", user.phone ?: "")
            .putString("profile_code", user.profileCode ?: "")
            .apply()

        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun showError(msg: String) {
        errorText.text = msg
        errorText.visibility = android.view.View.VISIBLE
    }
}
