package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class SignUpOne : AppCompatActivity() {

    private lateinit var userDao: UserDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up_one)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        userDao = AppDatabase.getDatabase(this).userDao()

        val fullNameEditText = findViewById<EditText>(R.id.editTextFullName)
        val emailEditText = findViewById<EditText>(R.id.editTextEmail)
        val passwordEditText = findViewById<EditText>(R.id.editTextPassword)
        val confirmPasswordEditText = findViewById<EditText>(R.id.editTextConfirmPassword)
        val errorTextView = findViewById<TextView>(R.id.textViewError)
        val createAccountButton = findViewById<Button>(R.id.buttonCreateAccount)
        val loginButton = findViewById<Button>(R.id.button)

        createAccountButton.setOnClickListener {
            val fullName = fullNameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString()
            val confirmPassword = confirmPasswordEditText.text.toString()

            when {
                fullName.isEmpty() -> showError(errorTextView, "Full Name cannot be empty")
                !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> showError(errorTextView, "Invalid email format")
                password.length < 6 -> showError(errorTextView, "Password must be at least 6 characters")
                password != confirmPassword -> showError(errorTextView, "Passwords do not match")
                else -> {
                    errorTextView.visibility = TextView.GONE

                    lifecycleScope.launch {
                        val existingUser = userDao.getUserByEmail(email)
                        if (existingUser != null) {
                            runOnUiThread {
                                showError(errorTextView, "User already exists!")
                            }
                        } else {
                            val newUser = User(fullName = fullName, email = email, password = password)

                            userDao.insertUser(newUser)

                            runOnUiThread {
                                Toast.makeText(this@SignUpOne, "Account created successfully!", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this@SignUpOne, LoginOne::class.java))
                                finish()
                            }
                        }
                    }
                }
            }
        }

        loginButton.setOnClickListener {
            startActivity(Intent(this, LoginOne::class.java))
            finish()
        }
    }

    private fun showError(view: TextView, message: String) {
        view.text = message
        view.visibility = TextView.VISIBLE
    }
}
