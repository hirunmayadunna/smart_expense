package com.example.myapplication

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import kotlinx.coroutines.launch
import java.util.*

class Budget : AppCompatActivity() {

    private lateinit var editBudget: EditText
    private lateinit var btnSetBudget: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var percentUsed: TextView
    private lateinit var amountSpent: TextView
    private lateinit var amountRemaining: TextView
    private lateinit var amountBudgetTotal: TextView
    private lateinit var warningMessage: TextView
    private lateinit var pieChart: PieChart

    private lateinit var sharedPref: android.content.SharedPreferences
    private lateinit var transactionDao: TransactionDao
    private lateinit var budgetDao: BudgetDao
    private lateinit var userEmail: String  // ✅ DECLARE GLOBALLY

    private val TIME_KEY = "daily_reminder_time"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_budget)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        sharedPref = getSharedPreferences("finance_app", Context.MODE_PRIVATE)
        val db = AppDatabase.getDatabase(this)
        transactionDao = db.transactionDao()
        budgetDao = db.budgetDao()

        userEmail = sharedPref.getString("user_email", "") ?: "" // ✅ INITIALIZE HERE

        val notifEnabled = sharedPref.getBoolean("notifications_enabled", true)
        val reminderTime = sharedPref.getString(TIME_KEY, null)
        if (notifEnabled && reminderTime != null) {
            val (hour, minute) = reminderTime.split(":").map { it.toInt() }
            scheduleDailyNotification(hour, minute)
        }

        editBudget = findViewById(R.id.editBudget)
        btnSetBudget = findViewById(R.id.btnSetBudget)
        progressBar = findViewById(R.id.progressBar)
        percentUsed = findViewById(R.id.percentUsed)
        amountSpent = findViewById(R.id.amountSpent)
        amountRemaining = findViewById(R.id.amountRemaining)
        amountBudgetTotal = findViewById(R.id.amountBudgetTotal)
        warningMessage = findViewById(R.id.warningMessage)
        pieChart = findViewById(R.id.pieChart)

        loadAndDisplayBudget()

        btnSetBudget.setOnClickListener {
            val input = editBudget.text.toString()
            if (input.isNotEmpty()) {
                val newBudget = input.toFloatOrNull()
                if (newBudget == null || newBudget <= 0f) {
                    Toast.makeText(this, "Please enter a valid budget amount.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                lifecycleScope.launch {
                    val transactions = transactionDao.getTransactionsForUser(userEmail)
                    val totalIncome = transactions.filter { it.type == "Income" }
                        .sumOf { it.amount.toDoubleOrNull() ?: 0.0 }.toFloat()

                    if (newBudget > totalIncome) {
                        val currency = sharedPref.getString("currency_type", "LKR") ?: "LKR"
                        Toast.makeText(
                            this@Budget,
                            "Budget cannot exceed total income ($currency %.2f)".format(totalIncome),
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        budgetDao.insertBudget(BudgetEntity(budgetAmount = newBudget))
                        updateBudgetUI(transactions)
                    }
                }
            } else {
                Toast.makeText(this, "Enter a valid budget", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.btn_home).setOnClickListener {
            startActivity(Intent(this, Home::class.java))
            finish()
        }
        findViewById<Button>(R.id.btn_expenses).setOnClickListener {
            startActivity(Intent(this, Expenses::class.java))
            finish()
        }
        findViewById<Button>(R.id.btn_profile).setOnClickListener {
            startActivity(Intent(this, Profile::class.java))
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        loadAndDisplayBudget()
    }

    private fun loadAndDisplayBudget() {
        lifecycleScope.launch {
            val transactions = transactionDao.getTransactionsForUser(userEmail)
            updateBudgetUI(transactions)
        }
    }

    private fun updateBudgetUI(transactions: List<Transaction>) {
        lifecycleScope.launch {
            val budgetEntity = budgetDao.getLatestBudget()
            val budget = budgetEntity?.budgetAmount ?: 0f
            val currency = sharedPref.getString("currency_type", "LKR") ?: "LKR"

            var totalExpense = 0.0
            var totalIncome = 0f

            for (item in transactions) {
                val amount = item.amount.toFloatOrNull() ?: 0f
                if (item.type == "Expense") totalExpense += amount
                else if (item.type == "Income") totalIncome += amount
            }

            val remaining = if (budget - totalExpense >= 0) budget - totalExpense else 0.0
            val percent = if (budget > 0) ((totalExpense * 100) / budget).toInt() else 0

            progressBar.progress = percent
            percentUsed.text = "$percent% used"
            amountSpent.text = "$currency %.2f".format(totalExpense)
            amountRemaining.text = "$currency %.2f".format(remaining)
            amountBudgetTotal.text = "$currency %.2f".format(budget)
            editBudget.hint = "\uD83D\uDCA1 Max: $currency %.2f".format(totalIncome)

            val notifEnabled = sharedPref.getBoolean("notifications_enabled", true)

            when {
                percent >= 100 -> {
                    warningMessage.text = "❗ You have exceeded your budget!"
                    warningMessage.setTextColor(getColor(R.color.red))
                    warningMessage.visibility = TextView.VISIBLE
                    if (notifEnabled) {
                        NotificationHelper.sendBudgetAlert(this@Budget, "🚨 Budget exceeded!")
                    }
                }
                percent >= 80 -> {
                    warningMessage.text = "⚠️ You are nearing your budget limit!"
                    warningMessage.setTextColor(getColor(R.color.black))
                    warningMessage.visibility = TextView.VISIBLE
                    if (notifEnabled) {
                        NotificationHelper.sendBudgetAlert(this@Budget, "⚠️ Nearing your budget limit.")
                    }
                }
                else -> {
                    warningMessage.text = ""
                    warningMessage.visibility = TextView.GONE
                }
            }

            loadPieChart(transactions)
        }
    }

    private fun loadPieChart(transactions: List<Transaction>) {
        val categoryMap = mutableMapOf<String, Float>()

        for (item in transactions) {
            if (item.type == "Expense") {
                val amount = item.amount.toFloatOrNull() ?: 0f
                categoryMap[item.category] = categoryMap.getOrDefault(item.category, 0f) + amount
            }
        }

        val entries = categoryMap.map { PieEntry(it.value, it.key) }

        val dataSet = PieDataSet(entries, "Expenses by Category")
        val categoryColors = entries.map {
            when (it.label) {
                "Food" -> Color.parseColor("#FFC107")
                "Bills" -> Color.parseColor("#F44336")
                "Entertainment" -> Color.parseColor("#2196F3")
                "Health" -> Color.parseColor("#8E24AA")
                "Others" -> Color.parseColor("#4CAF50")
                else -> Color.parseColor("#9E9E9E")
            }
        }

        dataSet.colors = categoryColors
        dataSet.valueTextSize = 14f
        dataSet.sliceSpace = 2f

        val pieData = PieData(dataSet)
        pieChart.data = pieData
        pieChart.setUsePercentValues(true)
        pieChart.centerText = "Spending Breakdown"
        pieChart.setCenterTextSize(16f)
        pieChart.description.isEnabled = false
        pieChart.legend.isEnabled = false
        pieChart.animateY(1000)
        pieChart.invalidate()
    }

    private fun scheduleDailyNotification(hour: Int, minute: Int) {
        val intent = Intent(this, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this, 2001, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) add(Calendar.DATE, 1)
        }

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }
}
