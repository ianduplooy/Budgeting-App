package com.example.budgetingapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BudgetApp()
                }
            }
        }
    }
}

data class StoreSpecial(
    val storeName: String,
    val distanceKm: Double,
    val specials: List<String>
)

data class ShoppingItem(
    val id: Int,
    val name: String,
    val frequency: BasketFrequency,
    val estimatedPrice: Double,
    val purchased: Boolean = false
)

enum class BasketFrequency { WEEKLY, MONTHLY }

class BudgetViewModel : ViewModel() {
    var monthlyIncome by mutableDoubleStateOf(4500.0)
    var monthlyExpenses by mutableDoubleStateOf(2800.0)

    val stores = listOf(
        StoreSpecial("Fresh Mart", 1.2, listOf("Milk 2L - 20% off", "Bread - Buy 1 Get 1")),
        StoreSpecial("Saver Foods", 2.0, listOf("Chicken fillet - R20 off/kg", "Rice 5kg - 15% off")),
        StoreSpecial("City Grocer", 3.5, listOf("Vegetable combo pack - R49", "Eggs (18) - 10% off"))
    )

    val shoppingItems = mutableStateListOf(
        ShoppingItem(1, "Milk", BasketFrequency.WEEKLY, 2.50),
        ShoppingItem(2, "Bread", BasketFrequency.WEEKLY, 1.80),
        ShoppingItem(3, "Detergent", BasketFrequency.MONTHLY, 7.20)
    )

    fun addItem(name: String, frequency: BasketFrequency, estimatedPrice: Double) {
        if (name.isBlank()) return
        val id = (shoppingItems.maxOfOrNull { it.id } ?: 0) + 1
        shoppingItems.add(ShoppingItem(id, name.trim(), frequency, estimatedPrice.coerceAtLeast(0.0)))
    }

    fun setPurchased(id: Int, purchased: Boolean) {
        val index = shoppingItems.indexOfFirst { it.id == id }
        if (index >= 0) {
            shoppingItems[index] = shoppingItems[index].copy(purchased = purchased)
        }
    }

    fun updateItemPrice(id: Int, updatedPrice: Double) {
        val index = shoppingItems.indexOfFirst { it.id == id }
        if (index >= 0) {
            shoppingItems[index] = shoppingItems[index].copy(estimatedPrice = updatedPrice.coerceAtLeast(0.0))
        }
    }

    fun estimatedTotal(frequency: BasketFrequency): Double {
        return shoppingItems.filter { it.frequency == frequency }.sumOf { it.estimatedPrice }
    }

    fun monthlyBalance(): Double = monthlyIncome - monthlyExpenses
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetApp(viewModel: BudgetViewModel = viewModel()) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Budget", "Stores", "Shopping")

    Scaffold(
        topBar = { TopAppBar(title = { Text("Budget Buddy") }) }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title) })
                }
            }

            when (selectedTab) {
                0 -> BudgetOverview(viewModel)
                1 -> NearbyStoreSpecials(viewModel.stores)
                2 -> ShoppingBasket(viewModel)
            }
        }
    }
}

@Composable
private fun BudgetOverview(viewModel: BudgetViewModel) {
    var incomeInput by remember { mutableStateOf(viewModel.monthlyIncome.toString()) }
    var expenseInput by remember { mutableStateOf(viewModel.monthlyExpenses.toString()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Monthly budget", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        item {
            OutlinedTextField(
                value = incomeInput,
                onValueChange = {
                    incomeInput = it
                    viewModel.monthlyIncome = it.toDoubleOrNull() ?: 0.0
                },
                label = { Text("Monthly income") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            OutlinedTextField(
                value = expenseInput,
                onValueChange = {
                    expenseInput = it
                    viewModel.monthlyExpenses = it.toDoubleOrNull() ?: 0.0
                },
                label = { Text("Monthly expenditure") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Estimated monthly balance")
                    Text(
                        "$${"%.2f".format(viewModel.monthlyBalance())}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun NearbyStoreSpecials(stores: List<StoreSpecial>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Closest stores and specials", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        items(stores.sortedBy { it.distanceKm }) { store ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${store.storeName} (${store.distanceKm} km)", fontWeight = FontWeight.Bold)
                    store.specials.forEach { special ->
                        Text("• $special")
                    }
                }
            }
        }
    }
}

@Composable
private fun ShoppingBasket(viewModel: BudgetViewModel) {
    var nameInput by remember { mutableStateOf("") }
    var priceInput by remember { mutableStateOf("") }
    var selectedFrequency by remember { mutableStateOf(BasketFrequency.WEEKLY) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Shopping basket", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedFrequency == BasketFrequency.WEEKLY,
                    onClick = { selectedFrequency = BasketFrequency.WEEKLY },
                    label = { Text("Weekly") }
                )
                FilterChip(
                    selected = selectedFrequency == BasketFrequency.MONTHLY,
                    onClick = { selectedFrequency = BasketFrequency.MONTHLY },
                    label = { Text("Monthly") }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                label = { Text("Product") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = priceInput,
                onValueChange = { priceInput = it },
                label = { Text("Estimated price") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    viewModel.addItem(nameInput, selectedFrequency, priceInput.toDoubleOrNull() ?: 0.0)
                    nameInput = ""
                    priceInput = ""
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add item")
            }
        }

        val filteredItems = viewModel.shoppingItems.filter { it.frequency == selectedFrequency }
        items(filteredItems, key = { it.id }) { item ->
            ShoppingItemRow(item = item, onTogglePurchased = { checked ->
                viewModel.setPurchased(item.id, checked)
            }, onPriceChange = { updatedPrice ->
                viewModel.updateItemPrice(item.id, updatedPrice)
            })
        }

        item {
            val total = viewModel.estimatedTotal(selectedFrequency)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Total estimated ${selectedFrequency.name.lowercase()} spend")
                    Text(
                        "$${"%.2f".format(total)}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ShoppingItemRow(
    item: ShoppingItem,
    onTogglePurchased: (Boolean) -> Unit,
    onPriceChange: (Double) -> Unit
) {
    var editPrice by remember(item.id, item.estimatedPrice) { mutableStateOf(item.estimatedPrice.toString()) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(item.name, fontWeight = FontWeight.SemiBold)
                Button(onClick = { onTogglePurchased(!item.purchased) }) {
                    Text(if (item.purchased) "Bought" else "Mark bought")
                }
            }
            OutlinedTextField(
                value = editPrice,
                onValueChange = {
                    editPrice = it
                    it.toDoubleOrNull()?.let(onPriceChange)
                },
                label = { Text("Estimated price") },
                modifier = Modifier.fillMaxWidth()
            )
            val rounded = (item.estimatedPrice * 100.0).roundToInt() / 100.0
            Text("Current estimate: $${"%.2f".format(rounded)}")
        }
    }
}
