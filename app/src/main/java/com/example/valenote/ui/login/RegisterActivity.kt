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
import com.example.valenote.R
import com.example.valenote.api.ApiClient
import com.example.valenote.database.models.User
import com.example.valenote.database.models.UserRequest
import com.example.valenote.databinding.ActivityRegisterBinding
import com.example.valenote.ui.login.LoginActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val additionalPadding = 24.toPx()
            v.setPadding(
                systemBars.left + additionalPadding,
                systemBars.top + additionalPadding,
                systemBars.right + additionalPadding,
                systemBars.bottom + additionalPadding
            )
            insets
        }

        with(binding) {
            tvLoginClick.setOnClickListener {
                val redirectToLogin = Intent(this@RegisterActivity, LoginActivity::class.java)
                startActivity(redirectToLogin)
            }

            btnRegister.setOnClickListener {
                btnRegister.setText("Loading...")
                val username = etUsername.text.toString()
                val email = etEmail.text.toString()
                val password = etPassword.text.toString()
                val confirmPassword = etConfirmPassword.text.toString()

                if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                    Toast.makeText(this@RegisterActivity, R.string.error_empty_fields, Toast.LENGTH_SHORT).show()
                    btnRegister.setText("Register")
                    return@setOnClickListener
                }

                if (password != confirmPassword) {
                    Toast.makeText(this@RegisterActivity, R.string.error_password_mismatch, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val newUser = UserRequest(username = username, email = email, password = password)

                lifecycleScope.launch {
                    try {
                        val apiService = ApiClient.apiService
                        val response = withContext(Dispatchers.IO) { apiService.saveUser(newUser).execute() }

                        if (response.isSuccessful) {
                            Toast.makeText(this@RegisterActivity, R.string.success_register, Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
                            finish()
                        } else {
                            Log.d("ERROR_APP_REGISTER", response.message())
                            Log.d("ERROR_APP_REGISTER", response.toString())
                            Log.d("ERROR_APP_REGISTER", response.errorBody().toString())
                            Toast.makeText(this@RegisterActivity, R.string.error_register_failed, Toast.LENGTH_SHORT).show()
                        }
                        btnRegister.setText("Register")
                    } catch (e: Exception) {
                        Log.d("ERROR_APP", "${e.message}")
                        Toast.makeText(this@RegisterActivity, "An error occurred: ${e.message}", Toast.LENGTH_SHORT).show()
                        btnRegister.setText("Register")
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
