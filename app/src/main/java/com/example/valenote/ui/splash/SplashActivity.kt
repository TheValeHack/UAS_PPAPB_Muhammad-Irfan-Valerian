package com.example.valenote.ui.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.valenote.ui.MainActivity
import com.example.valenote.R
import com.example.valenote.databinding.ActivitySplashBinding
import com.example.valenote.ui.login.LoginActivity
import com.example.valenote.utils.SharedPreferencesHelper

class SplashActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val preferences = SharedPreferencesHelper(this)

        Handler(Looper.getMainLooper()).postDelayed({
            if (preferences.isLoggedIn()) {
                startActivity(Intent(this, MainActivity::class.java))
            } else {
                startActivity(Intent(this, LoginActivity::class.java))
            }
            finish()
        }, 3000)
    }
}