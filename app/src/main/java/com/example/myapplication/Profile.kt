package com.example.myapplication

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.util.*

class Profile : AppCompatActivity() {

    private lateinit var displayName: TextView
    private lateinit var editButton: Button
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var transactionDao: TransactionDao


    private val TIME_KEY = "daily_reminder_time"
    private val CURRENCY_KEY = "currency_type"
    private val NAME_KEY = "name"
    private val BUDGET_KEY = "monthly_budget"
    private val NOTIF_KEY = "notifications_enabled"
    private val DARK_MODE_KEY = "dark_mode"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        sharedPreferences = getSharedPreferences("finance_app", MODE_PRIVATE)
        transactionDao = AppDatabase.getDatabase(this).transactionDao()
        displayName = findViewById(R.id.display_name)
        editButton = findViewById(R.id.btn_edit_name)
        displayName.text = sharedPreferences.getString(NAME_KEY, "User")

        editButton.setOnClickListener { showEditNameDialog() }

        setupCurrencySpinner()
        setupNotificationSwitch()
        setupReminderTimePicker()
        setupDarkModeSwitch()
        setupExportButton()
        setupImportButton()
        setupNavigationButtons()
    }

    private fun setupCurrencySpinner() {
        val currencySpinner = findViewById<Spinner>(R.id.currency_spinner)
        val savedCurrency = sharedPreferences.getString(CURRENCY_KEY, "LKR")
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.currency_options,
            R.layout.spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        currencySpinner.adapter = adapter

        val currencyOptions = resources.getStringArray(R.array.currency_options)
        val selectedIndex = currencyOptions.indexOf(savedCurrency)
        currencySpinner.setSelection(if (selectedIndex != -1) selectedIndex else 0)

        currencySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                val selectedCurrency = parent.getItemAtPosition(position).toString()
                sharedPreferences.edit().putString(CURRENCY_KEY, selectedCurrency).apply()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun setupNotificationSwitch() {
        val switchNotif = findViewById<Switch>(R.id.switch_notifications)
        val notifEnabled = sharedPreferences.getBoolean(NOTIF_KEY, true)
        switchNotif.isChecked = notifEnabled
        switchNotif.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean(NOTIF_KEY, isChecked).apply()
        }
    }

    private fun setupReminderTimePicker() {
        val reminderText = findViewById<TextView>(R.id.reminderTimeValue)
        val changeTimeButton = findViewById<Button>(R.id.btn_change_reminder_time)
        val savedTime = sharedPreferences.getString(TIME_KEY, "09:00")
        reminderText.text = "Current: $savedTime"

        changeTimeButton.setOnClickListener {
            val (hour, minute) = (savedTime ?: "09:00").split(":").map { it.toInt() }

            TimePickerDialog(this, { _, h, m ->
                val selectedTime = String.format("%02d:%02d", h, m)
                sharedPreferences.edit().putString(TIME_KEY, selectedTime).apply()
                reminderText.text = "Current: $selectedTime"
                if (sharedPreferences.getBoolean(NOTIF_KEY, true)) scheduleDailyNotification(h, m)
            }, hour, minute, true).show()
        }
    }

    private fun setupDarkModeSwitch() {
        val switchDarkMode = findViewById<Switch>(R.id.switch_darkmode)
        val isDark = sharedPreferences.getBoolean(DARK_MODE_KEY, false)
        switchDarkMode.isChecked = isDark

        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean(DARK_MODE_KEY, isChecked).apply()
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
        }
    }

    private fun setupExportButton() {
        findViewById<Button>(R.id.btn_export_data).setOnClickListener {
            lifecycleScope.launch {
                try {
                    val userEmail = sharedPreferences.getString("user_email", "") ?: ""
                    val transactions = transactionDao.getTransactionsForUser(userEmail)

                    val jsonArray = JSONArray().apply {
                        transactions.forEach {
                            put(JSONObject().apply {
                                put("title", it.title)
                                put("amount", it.amount)
                                put("date", it.date)
                                put("type", it.type)
                                put("category", it.category)
                            })
                        }
                    }

                    // ✅ Wrap in user email
                    val backupObject = JSONObject().apply {
                        put("userEmail", userEmail)
                        put("transactions", jsonArray)
                    }

                    // ✅ Make filename unique per user
                    val FILE_NAME = "backup_${userEmail.replace("@", "_at_")}.json"

                    openFileOutput(FILE_NAME, MODE_PRIVATE).use {
                        it.write(backupObject.toString().toByteArray())
                    }

                    Toast.makeText(this@Profile, "✅ Exported to $FILE_NAME", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this@Profile, "❌ Export failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }


    private fun setupImportButton() {
        findViewById<Button>(R.id.btn_import_data).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Import Transactions")
                .setMessage("This will overwrite all transactions. Proceed?")
                .setPositiveButton("Yes") { _, _ ->
                    lifecycleScope.launch {
                        try {
                            val userEmail = sharedPreferences.getString("user_email", "") ?: ""

                            // ✅ Build user-specific file name
                            val fileName = "backup_${userEmail.replace("@", "_at_")}.json"

                            // ✅ Check if file exists before trying to open
                            val file = File(filesDir, fileName)
                            if (!file.exists()) {
                                Toast.makeText(this@Profile, "❌ No backup found for your account", Toast.LENGTH_LONG).show()
                                return@launch
                            }

                            val content = file.bufferedReader().use { it.readText() }
                            val backupObject = JSONObject(content)

                            // ✅ Step 1: Check ownership
                            val backupEmail = backupObject.getString("userEmail")
                            if (backupEmail != userEmail) {
                                Toast.makeText(this@Profile, "❌ Backup belongs to a different account!", Toast.LENGTH_LONG).show()
                                return@launch
                            }

                            val jsonArray = backupObject.getJSONArray("transactions")

                            // 🧹 Delete existing user transactions
                            transactionDao.getTransactionsForUser(userEmail).forEach {
                                transactionDao.deleteTransaction(it)
                            }

                            // 📥 Insert new transactions
                            for (i in 0 until jsonArray.length()) {
                                val item = jsonArray.getJSONObject(i)
                                transactionDao.insertTransaction(
                                    Transaction(
                                        title = item.getString("title"),
                                        amount = item.getString("amount"),
                                        date = item.getString("date"),
                                        type = item.getString("type"),
                                        category = item.getString("category"),
                                        userEmail = userEmail
                                    )
                                )
                            }

                            Toast.makeText(this@Profile, "✅ Imported successfully", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(this@Profile, "❌ Import failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }




    private fun setupNavigationButtons() {
        findViewById<Button>(R.id.btn_home).setOnClickListener {
            startActivity(Intent(this, Home::class.java)); finish()
        }
        findViewById<Button>(R.id.btn_expenses).setOnClickListener {
            startActivity(Intent(this, Expenses::class.java)); finish()
        }
        findViewById<Button>(R.id.btn_budget).setOnClickListener {
            startActivity(Intent(this, Budget::class.java)); finish()
        }
        findViewById<Button>(R.id.btn_logout).setOnClickListener {
            startActivity(Intent(this, LoginOne::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }
    }

    private fun showEditNameDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Edit Name")
        val input = EditText(this).apply {
            setText(displayName.text.toString())
        }

        builder.setView(input)
        builder.setPositiveButton("Save") { dialog, _ ->
            val newName = input.text.toString().trim()
            if (newName.isNotEmpty()) {
                sharedPreferences.edit().putString(NAME_KEY, newName).apply()
                displayName.text = newName
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        builder.show()
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
