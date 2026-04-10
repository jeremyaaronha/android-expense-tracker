package com.example.expensetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDate


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ExpenseApp() }
    }
}


data class Expense(
    val id: String = "",
    val description: String = "",
    val amount: Double = 0.0,
    val date: String = "",
    val type: String = "income" // income or expense
)


@Composable
fun ExpenseApp() {

    val navController = rememberNavController()
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    val startDestination =
        if (auth.currentUser != null) "dashboard"
        else "login"


    NavHost(navController, startDestination = startDestination) {

        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("dashboard") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }


        composable("dashboard") {
            DashboardScreen(
                db = db,
                auth = auth,
                onLogout = {
                    auth.signOut()
                    navController.navigate("login") {
                        popUpTo("dashboard") { inclusive = true }
                    }
                }
            )
        }
    }
}

/* ---------------- DASHBOARD ---------------- */

@Composable
fun DashboardScreen(
    db: FirebaseFirestore,
    auth: FirebaseAuth,
    onLogout: () -> Unit
) {

    val userId = auth.currentUser?.uid ?: return
    var expenses by remember { mutableStateOf(listOf<Expense>()) }
    var filter by remember { mutableStateOf("All") }
    var showDialog by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<Expense?>(null) }

    LaunchedEffect(Unit) {
        db.collection("users")
            .document(userId)
            .collection("transactions")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    expenses = snapshot.documents.map {
                        Expense(
                            id = it.id,
                            description = it.getString("description") ?: "",
                            amount = it.getDouble("amount") ?: 0.0,
                            date = it.getString("date") ?: "",
                            type = it.getString("type") ?: "income"
                        )
                    }
                }
            }
    }

    // apply selected filter
    val filteredExpenses = when (filter) {
        "Today" -> expenses.filter { it.date == LocalDate.now().toString() }
        "Month" -> expenses.filter {
            it.date.substring(0, 7) ==
                    LocalDate.now().toString().substring(0, 7)
        }
        else -> expenses
    }

    val balance = filteredExpenses.sumOf { it.amount }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F7))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onLogout) {
                Text("Logout")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text("Balance", color = Color.Gray)
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "$$balance",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = if (balance >= 0) Color(0xFF1E8E3E) else Color.Red
        )

        Spacer(modifier = Modifier.height(20.dp))

        FilterSelector(filter) { filter = it }

        Spacer(modifier = Modifier.height(20.dp))

        // open dialog to add transaction
        Button(
            onClick = {
                editingExpense = null
                showDialog = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
        ) {
            Text("Add Transaction", color = Color.White)
        }

        Spacer(modifier = Modifier.height(20.dp))

        LazyColumn {
            items(filteredExpenses) { expense ->
                ModernExpenseCard(
                    expense = expense,
                    onDelete = {
                        db.collection("users")
                            .document(userId)
                            .collection("transactions")
                            .document(expense.id)
                            .delete()
                    },
                    onEdit = {
                        editingExpense = expense
                        showDialog = true
                    }
                )
                Spacer(modifier = Modifier.height(15.dp))
            }
        }

        // show add or edit dialog
        if (showDialog) {
            AddEditTransactionDialog(
                existingExpense = editingExpense,
                onDismiss = { showDialog = false },
                onSave = { expense ->
                    val data = hashMapOf(
                        "description" to expense.description,
                        "amount" to expense.amount,
                        "date" to expense.date,
                        "type" to expense.type
                    )

                    if (expense.id.isEmpty()) {
                        db.collection("users")
                            .document(userId)
                            .collection("transactions")
                            .add(data)
                    } else {
                        db.collection("users")
                            .document(userId)
                            .collection("transactions")
                            .document(expense.id)
                            .update(data as Map<String, Any>)
                    }

                    showDialog = false
                }
            )
        }
    }
}

/* -------------- LOGIN -------------- */

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit
) {

    val auth = FirebaseAuth.getInstance()
    // email input state
    var email by remember { mutableStateOf("") }
    // password input state
    var password by remember { mutableStateOf("") }
    var isLogin by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            if (isLogin) "Login" else "Register",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )


        Spacer(modifier = Modifier.height(20.dp))

        // show error message if login fails
        if (errorMessage != null) {
            Text(errorMessage!!, color = Color.Red)
            Spacer(modifier = Modifier.height(12.dp))
        }

        Button(
            onClick = {

                if (isLogin) {
                    auth.signInWithEmailAndPassword(email.trim(), password)
                        .addOnSuccessListener { onLoginSuccess() }
                        .addOnFailureListener {
                            errorMessage = it.message
                        }
                } else {
                    auth.createUserWithEmailAndPassword(email.trim(), password)
                        .addOnSuccessListener { onLoginSuccess() }
                        .addOnFailureListener {
                            errorMessage = it.message
                        }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isLogin) "Login" else "Register")
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = { isLogin = !isLogin }) {
            Text(
                if (isLogin)
                    "Don't have an account? Register"
                else
                    "Already have an account? Login"
            )
        }
    }
}

/* ---------------- COMPONENTS ---------------- */

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

@Composable
fun ModernExpenseCard(
    expense: Expense,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {

    Card(
        shape = RoundedCornerShape(25.dp),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {

        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
                .clickable { onEdit() },
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            // left side info
            Column {
                Text(expense.description, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(expense.date, fontSize = 12.sp, color = Color.Gray)
            }

            // right side amount and delete
            Column(horizontalAlignment = Alignment.End) {

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

                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                }
            }
        }
    }
}

@Composable
fun AddEditTransactionDialog(
    existingExpense: Expense?,
    onDismiss: () -> Unit,
    onSave: (Expense) -> Unit
) {

    // local state for dialog
    var description by remember { mutableStateOf(existingExpense?.description ?: "") }
    var amountText by remember { mutableStateOf(existingExpense?.amount?.toString() ?: "") }
    var type by remember { mutableStateOf(existingExpense?.type ?: "income") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                // convert text to number
                val amount = amountText.toDoubleOrNull() ?: 0.0
                val finalAmount = if (type == "expense") -kotlin.math.abs(amount) else kotlin.math.abs(amount)

                onSave(
                    Expense(
                        id = existingExpense?.id ?: "",
                        description = description,
                        amount = finalAmount,
                        date = LocalDate.now().toString(),
                        type = type
                    )
                )
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = { Text(if (existingExpense == null) "New Transaction" else "Edit Transaction") },
        text = {
            Column {

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("income", "expense").forEach { option ->
                        Button(
                            onClick = { type = option },
                            colors = ButtonDefaults.buttonColors(
                                containerColor =
                                if (type == option) Color.Black else Color.Gray
                            )
                        ) {
                            Text(option.capitalize(), color = Color.White)
                        }
                    }
                }
            }
        }
    )
}