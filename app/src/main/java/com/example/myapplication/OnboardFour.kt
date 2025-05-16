package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class OnboardFour : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_onboard_four)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        val btnBack = findViewById<Button>(R.id.btn_back2)


        btnBack.setOnClickListener {

            val intent = Intent(this, OnboardThree::class.java)
            startActivity(intent)

            finish()
        }


        val btnGetStarted = findViewById<Button>(R.id.btn_getstarted)


        btnGetStarted.setOnClickListener {

            val intent = Intent(this, LoginOne::class.java)
            startActivity(intent)

            finish()
        }
    }
}
