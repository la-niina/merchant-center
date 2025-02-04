package components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import presentation.SalesListing
import presentation.SalesTotal
import presentation.SearchTextField
import viewmodel.MainViewModel

@Composable
fun SalesComponent(
    mainViewModel: MainViewModel = MainViewModel(),
) {
    val productsSales by mainViewModel.productsList.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // Cache searchInput transformation
    var searchInput by remember { mutableStateOf("") }
    val filteredProducts by remember(productsSales, searchInput) {
        derivedStateOf {
            if (searchInput.isBlank()) {
                productsSales.sortedByDescending { it.time }
            } else {
                productsSales.filter {
                    it.productName.contains(searchInput, ignoreCase = true)
                }.sortedByDescending { it.time }
            }
        }
    }

    var showAddSaleDialog by remember { mutableStateOf(false) }
    val lazyListState = rememberLazyListState()

    // Initial data loading
    LaunchedEffect(Unit) {
        mainViewModel.loadProducts()
        mainViewModel.loadCurrentDateTime()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerpadding ->
        LazyColumn(
            modifier = Modifier.padding(horizontal = 20.dp).fillMaxSize(),
            state = lazyListState,
            contentPadding = innerpadding,
            verticalArrangement = Arrangement.spacedBy(5.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item(key = "header") {
                SalesSectionHeader(
                    searchInput = searchInput,
                    onSearchChange = { searchInput = it },
                    onRefreshClick = { mainViewModel.refreshProducts() },
                    onAddSaleClick = { showAddSaleDialog = true }
                )
            }

            // Table Header
            item(key = "list-header") {
                SalesListHeader()
            }

            // Product Items
            items(
                items = filteredProducts,
                key = { it.pid } // Stable keys for better list performance
            ) { product ->
                SalesListing(
                    productName = product.productName.ifBlank { "Unknown Product" },
                    qty = product.qty,
                    time = product.formattedTime(),
                    price = product.formattedPrice(),
                    onRemove = {
                        coroutineScope.launch {
                            mainViewModel.removeProductById(product.pid)
                        }
                    }
                )
            }

            // Total Sales
            item(key = "total") {
                SalesTotal(
                    total = mainViewModel.getFormattedTotalPriceOfProducts()
                )
            }
        }

        // Add Sale Dialog
        if (showAddSaleDialog) {
            AddSaleDialog(
                onDismissRequest = { showAddSaleDialog = false },
                onAddSale = { name, qty, price ->
                    mainViewModel.scope.launch {
                        mainViewModel.addProduct(name, qty, price.toDouble())
                        showAddSaleDialog = false
                    }
                }
            )
        }
    }
}

@Composable
fun SalesSectionHeader(
    searchInput: String,
    onSearchChange: (String) -> Unit,
    onRefreshClick: () -> Unit,
    onAddSaleClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .padding(20.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        SearchTextField(
            value = searchInput,
            onValueChange = onSearchChange
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onAddSaleClick,
                shape = RoundedCornerShape(10)
            ) {
                Icon(
                    Icons.Rounded.Add,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.padding(5.dp))
                Text(
                    text = "Add Sale",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Button(
                onClick = onRefreshClick,
                shape = RoundedCornerShape(10)
            ) {
                Icon(
                    Icons.Rounded.Refresh,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.padding(5.dp))
                Text(
                    text = "Refresh",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun SalesListHeader() {
    Row(
        modifier = Modifier
            .padding(20.dp)
            .height(50.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HeaderColumn("Product Name", modifier = Modifier.weight(1f))
        HeaderColumn("Qty", modifier = Modifier.weight(0.5f))
        HeaderColumn("Time", modifier = Modifier.weight(0.5f))
        HeaderColumn("Price", modifier = Modifier.weight(0.5f))
        HeaderColumn("Remove", modifier = Modifier.weight(0.5f))
    }
}

@Composable
fun HeaderColumn(
    text: String,
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start
) {
    Row(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp)
        )
    }
}

@Composable
fun AddSaleDialog(
    onDismissRequest: () -> Unit,
    onAddSale: (String, String, String) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()

    var quantity by rememberSaveable { mutableStateOf("") }
    var price by rememberSaveable { mutableStateOf("") }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnClickOutside = true,
            dismissOnBackPress = true
        )
    ) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add Sale",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismissRequest) {
                        Icon(Icons.Rounded.Close, "Close dialog")
                    }
                }

                // Product Selection Dropdown
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                    },
                    label = { Text("Product Name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Quantity Input
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Quantity") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Price Display
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Total Price (UGX)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Complete Sale Button
                Button(
                    onClick = {
                        coroutineScope.launch {
                            onAddSale(
                                searchQuery,
                                quantity,
                                price.trim().replace(",", "")
                            )
                            onDismissRequest()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Rounded.MonetizationOn, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Complete Sale")
                }
            }
        }
    }
}