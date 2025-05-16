package com.example.myapplication

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.ScaleAnimation
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var logoImage: ImageView
    private lateinit var welcomeText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        logoImage = findViewById(R.id.imageViewLogo)
        welcomeText = findViewById(R.id.textViewWelcome)

        startSplashSequence()
    }

    private fun startSplashSequence() {
        val handler = Handler(Looper.getMainLooper())


        val zoomAnimation = ScaleAnimation(
            1f, 2f,
            1f, 2f,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f
        )
        zoomAnimation.duration = 1500
        zoomAnimation.fillAfter = true
        logoImage.startAnimation(zoomAnimation)

        handler.postDelayed({

            logoImage.clearAnimation()
            logoImage.visibility = View.GONE

            handler.postDelayed({

                welcomeText.visibility = View.VISIBLE
                welcomeText.textSize = 50f

                handler.postDelayed({

                    welcomeText.setTextColor(Color.WHITE)

                    handler.postDelayed({

                        startActivity(Intent(this, OnboardOne::class.java))
                        finish()
                    }, 1000)

                }, 800)

            }, 500)

        }, 1500)
    }
}
