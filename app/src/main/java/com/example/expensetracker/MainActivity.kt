package com.example.expensetracker

// main imports for android and compose ui
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.*
import java.time.LocalDate
import org.json.JSONArray
import org.json.JSONObject

// main activity that starts the app
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ExpenseApp() } // set main composable
    }
}

// data model for each transaction
data class Expense(
    val description: String,
    val amount: Double,
    val date: String
)

@Composable
fun ExpenseApp() {

    // navigation controller
    val navController = rememberNavController()

    // get shared preferences to save data
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("expenses", Context.MODE_PRIVATE)

    // state list of expenses
    var expenses by remember {
        mutableStateOf(loadExpenses(prefs))
    }

    // navigation host with two screens
    NavHost(navController, startDestination = "dashboard") {

        // dashboard screen
        composable("dashboard") {
            DashboardScreen(
                expenses = expenses,
                onAddClick = { navController.navigate("add") },
                onDelete = { index ->
                    expenses = expenses.toMutableList().also { it.removeAt(index) }
                    saveExpenses(prefs, expenses) // save after delete
                }
            )
        }

        // add transaction screen
        composable("add") {
            AddTransactionScreen(
                onSave = {
                    expenses = expenses + it
                    saveExpenses(prefs, expenses) // save after add
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}

/* ------------ dashboard screen --------- */

@Composable
fun DashboardScreen(
    expenses: List<Expense>,
    onAddClick: () -> Unit,
    onDelete: (Int) -> Unit
) {

    // filter state
    var filter by remember { mutableStateOf("All") }

    // filter logic by today or month
    val filteredExpenses = when (filter) {
        "Today" -> expenses.filter { it.date == LocalDate.now().toString() }
        "Month" -> expenses.filter {
            it.date.substring(0, 7) ==
                    LocalDate.now().toString().substring(0, 7)
        }
        else -> expenses
    }

    // calculate total balance
    val balance = filteredExpenses.sumOf { it.amount }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F7))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.height(40.dp))

        Text("Balance", color = Color.Gray)

        Spacer(modifier = Modifier.height(8.dp))

        // show balance with color
        Text(
            "$${balance}",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = if (balance >= 0) Color(0xFF1E8E3E) else Color.Red
        )

        Spacer(modifier = Modifier.height(20.dp))

        // filter buttons
        FilterSelector(filter) { filter = it }

        Spacer(modifier = Modifier.height(20.dp))

        // go to add screen
        Button(
            onClick = onAddClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
        ) {
            Text("Add Transaction", color = Color.White)
        }

        Spacer(modifier = Modifier.height(20.dp))

        // show expense list
        LazyColumn {
            itemsIndexed(filteredExpenses.reversed()) { index, expense ->
                ModernExpenseCard(expense) {
                    val realIndex = expenses.indexOf(expense)
                    onDelete(realIndex)
                }
                Spacer(modifier = Modifier.height(15.dp))
            }
        }
    }
}

/* --------- add screen ----------- */

@Composable
fun AddTransactionScreen(
    onSave: (Expense) -> Unit,
    onBack: () -> Unit
) {

    // input states
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Income") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F7))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.height(30.dp))

        Text("New Transaction", fontSize = 22.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(30.dp))

        // select income or expense
        TransactionTypeSelector(type) { type = it }

        Spacer(modifier = Modifier.height(20.dp))

        // description input
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        // amount input
        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            label = { Text("Amount") },
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(30.dp))

        // save button
        Button(
            onClick = {
                val value = amount.toDoubleOrNull() ?: 0.0

                // negative if expense
                val finalAmount =
                    if (type == "Income") value
                    else -value

                val newExpense = Expense(
                    description,
                    finalAmount,
                    LocalDate.now().toString()
                )

                onSave(newExpense)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
        ) {
            Text("Save", color = Color.White)
        }

        Spacer(modifier = Modifier.height(10.dp))

        // cancel button
        TextButton(onClick = onBack) { Text("Cancel") }
    }
}

/* ----------- reusable components -------------- */

// selector for income or expense
@Composable
fun TransactionTypeSelector(
    selected: String,
    onSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFE0E0E0), RoundedCornerShape(50))
            .padding(4.dp)
    ) {

        listOf("Income", "Expense").forEach { option ->

            val isSelected = selected == option

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (isSelected) Color.Black else Color.Transparent
                    )
                    .clickable { onSelected(option) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    option,
                    color = if (isSelected) Color.White else Color.Black,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// filter selector buttons
@Composable
fun FilterSelector(
    selected: String,
    onSelected: (String) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("All", "Today", "Month").forEach { label ->
            val color =
                if (selected == label) Color.Black else Color.Gray

            Button(
                onClick = { onSelected(label) },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = color)
            ) {
                Text(label, color = Color.White)
            }
        }
    }
}

// card for each transaction
@Composable
fun ModernExpenseCard(
    expense: Expense,
    onDelete: () -> Unit
) {

    Card(
        shape = RoundedCornerShape(25.dp),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {

        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Column {
                Text(expense.description, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(expense.date, fontSize = 12.sp, color = Color.Gray)
            }

            Column(horizontalAlignment = Alignment.End) {

                // show amount with color
                Text(
                    if (expense.amount >= 0)
                        "+${expense.amount}"
                    else
                        "${expense.amount}",
                    color = if (expense.amount >= 0)
                        Color(0xFF1E8E3E)
                    else
                        Color.Red,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // delete button
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                }
            }
        }
    }
}

/* ---------- storage logic ------------ */

// save list to shared preferences
fun saveExpenses(prefs: SharedPreferences, expenses: List<Expense>) {
    val jsonArray = JSONArray()
    expenses.forEach {
        val obj = JSONObject()
        obj.put("description", it.description)
        obj.put("amount", it.amount)
        obj.put("date", it.date)
        jsonArray.put(obj)
    }
    prefs.edit().putString("expenses", jsonArray.toString()).apply()
}

// load list from shared preferences
fun loadExpenses(prefs: SharedPreferences): List<Expense> {
    val jsonString = prefs.getString("expenses", null) ?: return emptyList()
    val jsonArray = JSONArray(jsonString)
    val list = mutableListOf<Expense>()
    for (i in 0 until jsonArray.length()) {
        val obj = jsonArray.getJSONObject(i)
        list.add(
            Expense(
                obj.getString("description"),
                obj.getDouble("amount"),
                obj.getString("date")
            )
        )
    }
    return list
}