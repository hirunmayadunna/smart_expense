package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class OnboardThree : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_onboard_three)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        val btnNext = findViewById<Button>(R.id.btn_next2)


        btnNext.setOnClickListener {

            val intent = Intent(this, OnboardFour::class.java)
            startActivity(intent)
        }


        val btnBack = findViewById<Button>(R.id.btn_back)


        btnBack.setOnClickListener {

            val intent = Intent(this, OnboardTwo::class.java)
            startActivity(intent)

            finish()
        }
    }
}
