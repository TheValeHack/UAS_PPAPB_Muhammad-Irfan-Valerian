package com.example.valenote.ui.login

import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.valenote.ui.MainActivity
import com.example.valenote.R
import com.example.valenote.api.ApiClient
import com.example.valenote.api.ApiService
import com.example.valenote.database.AppDatabase
import com.example.valenote.databinding.ActivityLoginBinding
import com.example.valenote.repository.UserRepository
import com.example.valenote.ui.login.RegisterActivity
import com.example.valenote.utils.SharedPreferencesHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var preferences: SharedPreferencesHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val additionalPadding = 24.toPx() // Konversi 24dp ke px
            v.setPadding(
                systemBars.left + additionalPadding,
                systemBars.top + additionalPadding,
                systemBars.right + additionalPadding,
                systemBars.bottom + additionalPadding
            )
            insets
        }

        preferences = SharedPreferencesHelper(this)

        with(binding) {
            tvRegisterClick.setOnClickListener {
                val redirectToRegister = Intent(this@LoginActivity, RegisterActivity::class.java)
                startActivity(redirectToRegister)
            }

            btnLogin.setOnClickListener {
                btnLogin.setText("Loading...")
                val username = etUsername.text.toString()
                val password = etPassword.text.toString()

                if (username.isBlank() || password.isBlank()) {
                    Toast.makeText(this@LoginActivity, "Fields cannot be empty", Toast.LENGTH_SHORT).show()
                    btnLogin.setText("Login")
                    return@setOnClickListener
                }

                lifecycleScope.launch {
                    try {
                        val apiService = ApiClient.apiService
                        val response = withContext(Dispatchers.IO) { apiService.getAllUsers().execute() }

                        if (response.isSuccessful) {
                            val users = response.body()

                            val user = users?.find { it.username == username && it.password == password }

                            if (user != null) {
                                preferences.setLoggedIn(true)
                                preferences.setUserId(user._id)
                                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                                finish()
                            } else {
                                Toast.makeText(this@LoginActivity, "Invalid credentials", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(this@LoginActivity, "Failed to fetch users", Toast.LENGTH_SHORT).show()
                        }
                        btnLogin.setText("Login")
                    } catch (e: Exception) {
                        Toast.makeText(this@LoginActivity, "An error occurred: ${e.message}", Toast.LENGTH_SHORT).show()
                        btnLogin.setText("Login")
                    }

                }
            }
        }
    }
    fun Int.toPx(): Int {
        val density = Resources.getSystem().displayMetrics.density
        return (this * density).toInt()
    }
}
