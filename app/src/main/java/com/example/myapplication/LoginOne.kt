package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class LoginOne : AppCompatActivity() {

    private lateinit var userDao: UserDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_one)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        userDao = AppDatabase.getDatabase(this).userDao()

        val emailEditText = findViewById<EditText>(R.id.editTextEmail)
        val passwordEditText = findViewById<EditText>(R.id.editTextPassword)
        val errorTextView = findViewById<TextView>(R.id.textViewError)
        val loginButton = findViewById<Button>(R.id.buttonLogin)
        val signUpButton = findViewById<Button>(R.id.button)

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                showError(errorTextView, "Please enter a valid email address")
            } else if (password.length < 6) {
                showError(errorTextView, "Password must be at least 6 characters")
            } else {
                lifecycleScope.launch {
                    val user = userDao.getUserByEmail(email)
                    runOnUiThread {
                        if (user == null) {
                            showError(errorTextView, "No account found with this email.")
                        } else if (user.password != password) {
                            showError(errorTextView, "Incorrect email or password.")
                        } else {
                            errorTextView.visibility = View.GONE

                            // ✅ Save user name AND email for later use
                            val sharedPrefs = getSharedPreferences("finance_app", MODE_PRIVATE)
                            with(sharedPrefs.edit()) {
                                putString("name", user.name)
                                putString("user_email", user.email) // 🔑 Important for showing correct data
                                apply()
                            }

                            Toast.makeText(this@LoginOne, "Login successful!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@LoginOne, Home::class.java))
                            finish()
                        }
                    }
                }
            }
        }

        signUpButton.setOnClickListener {
            startActivity(Intent(this, SignUpOne::class.java))
        }
    }

    private fun showError(view: TextView, message: String) {
        view.text = message
        view.visibility = View.VISIBLE
    }
}
