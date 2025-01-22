package components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import viewmodel.SalesProducts
import viewmodel.StockViewModel

@Composable
fun SalesComponent(
    mainViewModel: MainViewModel = MainViewModel(),
    stockViewModel: StockViewModel = StockViewModel(),
) {
    var searchInput by rememberSaveable { mutableStateOf("") }
    var showAddSaleDialog by rememberSaveable { mutableStateOf(false) }

    val productsSales by mainViewModel.productsList.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // Derive filtered products
    val filteredProducts by remember(productsSales, searchInput) {
        derivedStateOf {
            productsSales.filter {
                it.productName.contains(searchInput, ignoreCase = true)
            }
        }
    }

    // Initial data loading
    LaunchedEffect(Unit) {
        mainViewModel.loadProducts()
        mainViewModel.loadCurrentDateTime()
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column {
            // Header and Search Section
            SalesSectionHeader(
                searchInput = searchInput,
                onSearchChange = { searchInput = it },
                onAddSaleClick = { showAddSaleDialog = !showAddSaleDialog }
            )

            // Sales List
            SalesProductList(
                products = filteredProducts,
                mainViewModel = mainViewModel,
                onRemoveProduct = { product ->
                    coroutineScope.launch {
                        mainViewModel.removeProductById(product.pid)
                    }
                }
            )
        }

        // Add Sale Dialog
        if (showAddSaleDialog) {
            AddSaleDialog(
                onDismiss = { showAddSaleDialog = !showAddSaleDialog },
                stockViewModel = stockViewModel,
                onAddSale = { name, qty, price ->
                    coroutineScope.launch {
                        mainViewModel.addProduct(
                            productName = name,
                            qty = qty.toInt(),
                            price = price.toDouble()
                        )
                        showAddSaleDialog = !showAddSaleDialog
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

        Button(
            onClick = onAddSaleClick,
            shape = RoundedCornerShape(5)
        ) {
            Text("Add Sale")
        }
    }
}


@Composable
fun SalesProductList(
    products: List<SalesProducts>,
    mainViewModel: MainViewModel,
    onRemoveProduct: (SalesProducts) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        // Table Header
        item { SalesListHeader() }

        // Product Items
        items(products) { product ->
            SalesListing(
                productName = product.productName.ifBlank { "Unknown Product" },
                qty = product.qty.takeIf { it.toIntOrNull() != null } ?: "0",
                time = product.formattedTime(),
                price = product.formattedPrice(),
                onRemove = { onRemoveProduct(product) }
            )
        }

        // Total Sales
        item {
            SalesTotal(
                total = mainViewModel.getFormattedTotalPriceOfProducts()
            )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSaleDialog(
    onDismiss: () -> Unit,
    onAddSale: (String, String, String) -> Unit,
    stockViewModel: StockViewModel = StockViewModel()
) {
    val stockState by stockViewModel.stockState.collectAsState()
    val scope = rememberCoroutineScope()

    var selectedProduct by rememberSaveable { mutableStateOf<StockViewModel.Product?>(null) }
    var quantity by rememberSaveable { mutableStateOf("") }
    var price by rememberSaveable { mutableStateOf("") }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    var quantityValidation by remember { mutableStateOf(ValidationState()) }
    var priceValidation by remember { mutableStateOf(ValidationState()) }
    var productValidation by remember { mutableStateOf(ValidationState()) }

    var isProductDropdownExpanded by rememberSaveable { mutableStateOf(false) }

    // Filter products based on the search query
    val filteredProducts = remember(searchQuery, stockState.products) {
        if (searchQuery.isBlank()) {
            stockState.products
        } else {
            stockState.products.filter {
                it.productName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    LaunchedEffect(selectedProduct, quantity) {
        if (selectedProduct != null && quantity.isNotBlank()) {
            try {
                val basePrice = selectedProduct!!.estimatedPrice
                val quantityInt = quantity.toIntOrNull() ?: 0
                val calculatedPrice = basePrice * quantityInt
                price = String.format("%.2f", calculatedPrice)
                priceValidation = ValidationState()
            } catch (e: Exception) {
                priceValidation = ValidationState("Error calculating price", true)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
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
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, "Close dialog")
                    }
                }

                // Product Selection Dropdown
                ExposedDropdownMenuBox(
                    expanded = isProductDropdownExpanded,
                    onExpandedChange = { isProductDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedProduct?.productName ?: searchQuery,
                        onValueChange = {
                            searchQuery = it
                            if (selectedProduct?.productName != it) {
                                selectedProduct = null
                            }
                            isProductDropdownExpanded = true // Open dropdown when typing
                        },
                        label = { Text("Product") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = isProductDropdownExpanded)
                        },
                        isError = productValidation.isError,
                        supportingText = {
                            productValidation.message?.let { Text(it) }
                        },
                        colors = ExposedDropdownMenuDefaults.textFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = isProductDropdownExpanded,
                        onDismissRequest = { isProductDropdownExpanded = false }
                    ) {
                        if (filteredProducts.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No products found") },
                                onClick = { },
                                enabled = false
                            )
                        } else {
                            filteredProducts.forEach { product ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(product.productName)
                                            Text(
                                                "${product.currentStock} in stock",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedProduct = product
                                        searchQuery = product.productName
                                        isProductDropdownExpanded = false
                                        productValidation =
                                            ValidationState() // Reset validation state
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                )
                            }
                        }
                    }
                }

                // Quantity Input
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { newValue ->
                        if (newValue.all { it.isDigit() }) {
                            quantity = newValue
                            quantityValidation = ValidationState()
                        }
                    },
                    label = { Text("Quantity") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    isError = quantityValidation.isError,
                    supportingText = {
                        quantityValidation.message?.let { Text(it) }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Price Display
                OutlinedTextField(
                    value = price,
                    onValueChange = { },
                    label = { Text("Total Price (UGX)") },
                    readOnly = true,
                    isError = priceValidation.isError,
                    supportingText = {
                        priceValidation.message?.let { Text(it) }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Complete Sale Button
                Button(
                    onClick = {
                        scope.launch {
                            if (validateAndSubmitSale(
                                    selectedProduct,
                                    quantity,
                                    price,
                                    stockViewModel,
                                    onAddSale,
                                    onDismiss,
                                    { productValidation = it },
                                    { quantityValidation = it },
                                    { priceValidation = it }
                                )
                            ) {
                                onDismiss()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = selectedProduct != null && quantity.isNotBlank() && price.isNotBlank()
                ) {
                    Icon(Icons.Rounded.MonetizationOn, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Complete Sale")
                }
            }
        }
    }
}

private suspend fun validateAndSubmitSale(
    selectedProduct: StockViewModel.Product?,
    quantity: String,
    price: String,
    stockViewModel: StockViewModel,
    onAddSale: (String, String, String) -> Unit,
    onDismiss: () -> Unit,
    updateProductValidation: (ValidationState) -> Unit,
    updateQuantityValidation: (ValidationState) -> Unit,
    updatePriceValidation: (ValidationState) -> Unit
): Boolean {
    if (selectedProduct == null) {
        updateProductValidation(ValidationState("Please select a product", true))
        return false
    }

    val quantityInt = quantity.toIntOrNull()
    if (quantityInt == null || quantityInt <= 0) {
        updateQuantityValidation(ValidationState("Please enter a valid quantity", true))
        return false
    }

    if (quantityInt > selectedProduct.currentStock) {
        updateQuantityValidation(
            ValidationState(
                "Insufficient stock (${selectedProduct.currentStock} available)",
                true
            )
        )
        return false
    }

    val priceDouble = price.toDoubleOrNull()
    if (priceDouble == null || priceDouble <= 0) {
        updatePriceValidation(ValidationState("Invalid price", true))
        return false
    }

    try {
        stockViewModel.sellProduct(selectedProduct.id, quantityInt)
        onAddSale(
            selectedProduct.productName,
            quantityInt.toString(),
            priceDouble.toString()
        )
        return true
    } catch (e: Exception) {
        updatePriceValidation(ValidationState("Failed to complete sale: ${e.message}", true))
        return false
    }
}

data class ValidationState(
    val message: String? = null,
    val isError: Boolean = false
)

data class QualityOption(
    val displayName: String,
    val multiplier: Double
)