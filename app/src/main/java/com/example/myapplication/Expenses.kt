package com.example.myapplication

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import java.util.*

class Expenses : AppCompatActivity() {

    private lateinit var sharedPref: SharedPreferences
    private lateinit var userEmail: String

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TransactionAdapter
    private lateinit var transactionDao: TransactionDao

    private lateinit var categoryScroll: HorizontalScrollView
    private lateinit var categoryContainer: LinearLayout

    private var currentTypeFilter: String? = null

    private val incomeCategories = listOf("Salary", "Bonus", "Investment", "Gift", "Other Income")
    private val expenseCategories = listOf("Food", "Transport", "Bills", "Entertainment", "Health", "Others")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expenses)

        sharedPref = getSharedPreferences("finance_app", MODE_PRIVATE)
        userEmail = sharedPref.getString("user_email", "") ?: ""

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val db = AppDatabase.getDatabase(this)
        transactionDao = db.transactionDao()

        recyclerView = findViewById(R.id.transactionRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this).apply {
            reverseLayout = true
            stackFromEnd = true
        }

        categoryScroll = findViewById(R.id.categoryFilterScroll)
        categoryContainer = findViewById(R.id.categoryFilterContainer)

        loadTransactions()

        findViewById<Button>(R.id.button7).setOnClickListener {
            showAddDialog("Income")
        }

        findViewById<Button>(R.id.button8).setOnClickListener {
            showAddDialog("Expense")
        }

        findViewById<Button>(R.id.btn_filter_income).setOnClickListener {
            filterByType("Income")
        }

        findViewById<Button>(R.id.btn_filter_expense).setOnClickListener {
            filterByType("Expense")
        }

        findViewById<Button>(R.id.btn_home).setOnClickListener {
            startActivity(Intent(this, Home::class.java))
            finish()
        }

        findViewById<Button>(R.id.btn_budget).setOnClickListener {
            startActivity(Intent(this, Budget::class.java))
            finish()
        }

        findViewById<Button>(R.id.btn_profile).setOnClickListener {
            startActivity(Intent(this, Profile::class.java))
            finish()
        }
    }

    private fun loadTransactions() {
        val currency = sharedPref.getString("currency_type", "LKR") ?: "LKR"
        lifecycleScope.launch {
            val allTransactions = transactionDao.getTransactionsForUser(userEmail).toMutableList()
            adapter = TransactionAdapter(
                transactionList = allTransactions,
                currencyType = currency,
                updateCallback = { loadTransactions() },
                onDeleteRequest = { position -> showDeleteConfirmation(position) },
                onEditRequest = { position -> showEditDialog(position) }
            )
            recyclerView.adapter = adapter
        }
    }

    private fun filterByType(type: String) {
        lifecycleScope.launch {
            if (currentTypeFilter == type) {
                loadTransactions()
                categoryScroll.visibility = View.GONE
                currentTypeFilter = null
            } else {
                val filtered = transactionDao.getTransactionsForUser(userEmail).filter { it.type == type }
                adapter.updateData(filtered)
                showCategoryFilters(type)
                currentTypeFilter = type
            }
        }
    }

    private fun showCategoryFilters(type: String) {
        categoryContainer.removeAllViews()
        val categories = if (type == "Income") incomeCategories else expenseCategories
        categoryScroll.visibility = View.VISIBLE

        for (cat in categories) {
            val btn = Button(this).apply {
                text = cat
                textSize = 14f
                setPadding(30, 10, 30, 10)
                setAllCaps(false)
                setBackgroundResource(R.drawable.category_filter_button)
                setTextColor(resources.getColor(android.R.color.black))
            }

            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            layoutParams.setMargins(12, 8, 12, 8)
            btn.layoutParams = layoutParams

            btn.setOnClickListener {
                lifecycleScope.launch {
                    val filtered = transactionDao.getTransactionsForUser(userEmail)
                        .filter { it.type == type && it.category == cat }
                    adapter.updateData(filtered)
                }
            }

            categoryContainer.addView(btn)
        }
    }

    private fun showAddDialog(type: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_transaction, null)
        val etTitle = dialogView.findViewById<EditText>(R.id.etTitle)
        val etAmount = dialogView.findViewById<EditText>(R.id.etAmount)
        val etDate = dialogView.findViewById<EditText>(R.id.etDate)
        val spinnerCategory = dialogView.findViewById<Spinner>(R.id.spinnerCategory)

        val categoryArray = if (type == "Income") R.array.income_category_array else R.array.expense_category_array
        ArrayAdapter.createFromResource(
            this,
            categoryArray,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerCategory.adapter = adapter
        }

        etDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(this, { _, y, m, d ->
                etDate.setText(String.format("%02d/%02d/%04d", d, m + 1, y))
            }, year, month, day)

            datePickerDialog.datePicker.maxDate = calendar.timeInMillis // 👈 Prevent future dates
            datePickerDialog.show()
        }


        AlertDialog.Builder(this)
            .setTitle("Add $type")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val title = etTitle.text.toString()
                val rawAmount = etAmount.text.toString().toDoubleOrNull()
                if (rawAmount == null) {
                    Toast.makeText(this, "Enter a valid amount", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val formattedAmount = String.format("%.2f", rawAmount)
                val date = etDate.text.toString()
                val category = spinnerCategory.selectedItem.toString()

                saveTransaction(title, formattedAmount, date, type, category)
            }
            .setNegativeButton("Cancel", null)
            .create()
            .show()
    }

    private fun saveTransaction(title: String, amount: String, date: String, type: String, category: String) {
        lifecycleScope.launch {
            transactionDao.insertTransaction(
                Transaction(
                    title = title,
                    amount = amount,
                    date = date,
                    type = type,
                    category = category,
                    userEmail = userEmail // 👈 important!
                )
            )
            loadTransactions()
        }
    }

    private fun showDeleteConfirmation(position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Delete Transaction")
            .setMessage("Are you sure you want to delete this entry?")
            .setPositiveButton("Yes") { _, _ ->
                lifecycleScope.launch {
                    val transactions = transactionDao.getTransactionsForUser(userEmail)
                    transactionDao.deleteTransaction(transactions[position])
                    loadTransactions()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditDialog(position: Int) {
        lifecycleScope.launch {
            val transactions = transactionDao.getTransactionsForUser(userEmail).toMutableList()
            val transaction = transactions[position]

            val dialogView = LayoutInflater.from(this@Expenses).inflate(R.layout.dialog_add_transaction, null)
            val etTitle = dialogView.findViewById<EditText>(R.id.etTitle)
            val etAmount = dialogView.findViewById<EditText>(R.id.etAmount)
            val etDate = dialogView.findViewById<EditText>(R.id.etDate)
            val spinnerCategory = dialogView.findViewById<Spinner>(R.id.spinnerCategory)

            etTitle.setText(transaction.title)
            etAmount.setText(transaction.amount)
            etDate.setText(transaction.date)

            val categoryArray = if (transaction.type == "Income") R.array.income_category_array else R.array.expense_category_array
            ArrayAdapter.createFromResource(
                this@Expenses,
                categoryArray,
                android.R.layout.simple_spinner_item
            ).also { adapter ->
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerCategory.adapter = adapter
            }

            val categories = resources.getStringArray(categoryArray)
            val catIndex = categories.indexOf(transaction.category)
            if (catIndex != -1) spinnerCategory.setSelection(catIndex)

            etDate.setOnClickListener {
                val calendar = Calendar.getInstance()
                val datePickerDialog = DatePickerDialog(this@Expenses, { _, y, m, d ->
                    etDate.setText(String.format("%02d/%02d/%04d", d, m + 1, y))
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))

                datePickerDialog.datePicker.maxDate = calendar.timeInMillis // 👈 Prevent future dates
                datePickerDialog.show()
            }


            AlertDialog.Builder(this@Expenses)
                .setTitle("Edit ${transaction.type}")
                .setView(dialogView)
                .setPositiveButton("Update") { _, _ ->
                    val updatedTransaction = transaction.copy(
                        title = etTitle.text.toString(),
                        amount = etAmount.text.toString(),
                        date = etDate.text.toString(),
                        category = spinnerCategory.selectedItem.toString()
                    )

                    lifecycleScope.launch {
                        transactionDao.updateTransaction(updatedTransaction)
                        loadTransactions()
                    }
                }
                .setNegativeButton("Cancel", null)
                .create()
                .show()
        }
    }
}
