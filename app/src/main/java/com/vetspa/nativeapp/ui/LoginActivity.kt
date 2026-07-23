package com.vetspa.nativeapp.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.vetspa.nativeapp.BuildConfig
import com.vetspa.nativeapp.data.api.ApiClient
import com.vetspa.nativeapp.data.model.User
import com.vetspa.nativeapp.databinding.ActivityLoginBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Nếu đã login rồi → qua thẳng MainActivity
        val saved = getSharedPreferences("vetspa_user", Context.MODE_PRIVATE)
        if (saved.getInt("user_id", 0) > 0) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.passwordInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) { doLogin(); true } else false
        }
        binding.loginBtn.setOnClickListener { doLogin() }
    }

    private fun doLogin() {
        val username = binding.usernameInput.text.toString().trim()
        val password = binding.passwordInput.text.toString().trim()

        if (username.isEmpty() || password.isEmpty()) {
            showError("Vui lòng nhập tên đăng nhập và mật khẩu")
            return
        }

        binding.errorText.visibility = android.view.View.GONE
        binding.loginBtn.isEnabled = false
        binding.loginBtn.text = "Đang đăng nhập..."

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val resp = ApiClient.api.login(username, password)
                withContext(Dispatchers.Main) {
                    binding.loginBtn.isEnabled = true
                    binding.loginBtn.text = "Đăng nhập"
                    if (resp.isSuccessful && resp.body()?.ok == true) {
                        val user = resp.body()!!.user
                        saveUser(user!!)
                        Toast.makeText(this@LoginActivity, "Xin chào ${user.fullname ?: user.username}", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        finish()
                    } else {
                        showError(resp.body()?.error ?: "Sai tên đăng nhập hoặc mật khẩu")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.loginBtn.isEnabled = true
                    binding.loginBtn.text = "Đăng nhập"
                    val msg = e.message ?: ""
                    when {
                        msg.contains("Unable to resolve host") -> showError("Không thể kết nối máy chủ")
                        msg.contains("timeout") -> showError("Hết thời gian kết nối, thử lại")
                        else -> showError("Lỗi: $msg")
                    }
                }
            }
        }
    }

    private fun saveUser(user: User) {
        getSharedPreferences("vetspa_user", Context.MODE_PRIVATE).edit().apply {
            putInt("user_id", user.id)
            putString("username", user.username)
            putString("fullname", user.fullname)
            putString("role", user.role)
            putString("email", user.email)
            putString("phone", user.phone)
            putString("profile_code", user.profileCode)
            apply()
        }
    }

    private fun showError(msg: String) {
        binding.errorText.text = msg
        binding.errorText.visibility = android.view.View.VISIBLE
    }
}
