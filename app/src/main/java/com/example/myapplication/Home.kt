package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class Home : AppCompatActivity() {

    private lateinit var greetingText: TextView
    private lateinit var recentTransactionContainer: LinearLayout
    private lateinit var textIncomeAmount: TextView
    private lateinit var textExpensesAmount: TextView
    private lateinit var textBudgetLeftAmount: TextView
    private lateinit var progressBarBudgetHome: ProgressBar

    private lateinit var sharedPref: android.content.SharedPreferences
    private lateinit var transactionDao: TransactionDao
    private lateinit var budgetDao: BudgetDao
    private lateinit var userEmail: String  // ✅ Declare globally

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        sharedPref = getSharedPreferences("finance_app", Context.MODE_PRIVATE)
        val db = AppDatabase.getDatabase(this)
        transactionDao = db.transactionDao()
        budgetDao = db.budgetDao()
        userEmail = sharedPref.getString("user_email", "") ?: "" // ✅ Initialize here

        greetingText = findViewById(R.id.greetingText)
        recentTransactionContainer = findViewById(R.id.recentTransactionContainer)
        textIncomeAmount = findViewById(R.id.textIncomeAmount)
        textExpensesAmount = findViewById(R.id.textExpensesAmount)
        textBudgetLeftAmount = findViewById(R.id.textBudgetLeftAmount)
        progressBarBudgetHome = findViewById(R.id.progressBarBudgetHome)

        findViewById<Button>(R.id.btn_expenses).setOnClickListener {
            startActivity(Intent(this, Expenses::class.java))
        }

        findViewById<Button>(R.id.btn_budget).setOnClickListener {
            startActivity(Intent(this, Budget::class.java))
        }

        findViewById<Button>(R.id.btn_profile).setOnClickListener {
            startActivity(Intent(this, Profile::class.java))
        }

        updateGreeting()
        loadHomeData()
    }

    private fun updateGreeting() {
        val savedName = sharedPref.getString("name", "Guest") ?: "Guest"
        greetingText.text = "Hi, $savedName 👋"
    }

    private fun loadHomeData() {
        lifecycleScope.launch {
            val transactions = transactionDao.getTransactionsForUser(userEmail)  // ✅ FIXED!
            val budgetEntity = budgetDao.getLatestBudget()
            val budget = budgetEntity?.budgetAmount ?: 0f
            val currency = sharedPref.getString("currency_type", "LKR") ?: "LKR"

            var incomeTotal = 0f
            var expenseTotal = 0f

            for (t in transactions) {
                val amount = t.amount.toFloatOrNull() ?: 0f
                if (t.type == "Income") incomeTotal += amount
                else if (t.type == "Expense") expenseTotal += amount
            }

            val budgetLeft = budget - expenseTotal
            val percentLeft = if (budget > 0) ((budgetLeft * 100) / budget).toInt() else 0

            textIncomeAmount.text = "$currency %.2f".format(incomeTotal)
            textExpensesAmount.text = "$currency %.2f".format(expenseTotal)
            textBudgetLeftAmount.text = "$currency %.2f".format(if (budgetLeft >= 0) budgetLeft else 0.0)
            progressBarBudgetHome.progress = if (percentLeft >= 0) percentLeft else 0

            // Load recent transactions (limit to 6)
            recentTransactionContainer.removeAllViews()
            val recent = transactions.takeLast(6).reversed()

            for (item in recent) {
                val row = LinearLayout(this@Home).apply {
                    orientation = LinearLayout.HORIZONTAL
                }

                val icon = ImageView(this@Home).apply {
                    setImageResource(if (item.type == "Income") R.drawable.income else R.drawable.expense)
                    layoutParams = LinearLayout.LayoutParams(48, 48).apply {
                        setMargins(0, 0, 16, 0)
                    }
                }

                val label = TextView(this@Home).apply {
                    text = "${item.title} - $currency ${item.amount}"
                    textSize = 16f
                    setTextColor(if (item.type == "Income") getColor(R.color.green) else getColor(R.color.red))
                }

                row.addView(icon)
                row.addView(label)
                recentTransactionContainer.addView(row)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateGreeting()
        loadHomeData()
    }
}
