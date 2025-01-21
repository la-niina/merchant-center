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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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
                onAddSaleClick = { showAddSaleDialog = true }
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
                onDismiss = { showAddSaleDialog = false },
                stockViewModel = stockViewModel,
                onAddSale = { name, qty, price ->
                    coroutineScope.launch {
                        mainViewModel.addProduct(
                            productName = name,
                            qty = qty.toInt(),
                            price = price.toDouble()
                        )
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
                total = mainViewModel.getFormattedTotalPriceOfAllProducts()
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

    var selectedProduct by rememberSaveable { mutableStateOf<StockViewModel.Product?>(null) }
    var quantity by rememberSaveable { mutableStateOf("") }
    var price by rememberSaveable { mutableStateOf("") }

    // Quality options
    val qualityOptions = listOf("Normal", "Good", "Premium")
    var selectedQuality by rememberSaveable { mutableStateOf(qualityOptions[0]) }

    var quantityError by rememberSaveable { mutableStateOf<String?>(null) }
    var priceError by rememberSaveable { mutableStateOf<String?>(null) }
    var productError by rememberSaveable { mutableStateOf<String?>(null) }

    var productName by rememberSaveable { mutableStateOf(false) }
    var qualityFilter by rememberSaveable { mutableStateOf(false) }

    // Dynamically calculate price based on product and quality
    LaunchedEffect(selectedProduct, selectedQuality, quantity) {
        if (selectedProduct != null && quantity.isNotBlank()) {
            val basePrice = selectedProduct!!.estimatedPrice
            val quantityInt = quantity.toIntOrNull() ?: 0

            // Price adjustment based on quality
            val qualityMultiplier = when (selectedQuality) {
                "Normal" -> 1.0
                "Good" -> 1.2
                "Premium" -> 1.5
                else -> 1.0
            }

            // Calculate total price
            val calculatedPrice = basePrice * quantityInt * qualityMultiplier
            price = String.format("%.2f", calculatedPrice)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(5)
        ) {
            Column(
                modifier = Modifier
                    .padding(30.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Add Sale",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 10.dp)
                )

                // Product Dropdown
                ExposedDropdownMenuBox(
                    expanded = productName,
                    onExpandedChange = {
                        productName = !productName
                    }
                ) {
                    OutlinedTextField(
                        value = selectedProduct?.productName ?: "Select Product",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Product") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = productName)
                        },
                        colors = ExposedDropdownMenuDefaults.textFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = productName,
                        onDismissRequest = {
                            productName = !productName
                        }
                    ) {
                        stockState.products.forEach { product ->
                            DropdownMenuItem(
                                text = {
                                    Text("${product.productName} (${product.currentStock} in stock)")
                                },
                                onClick = {
                                    selectedProduct = product
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }

                // Quality Dropdown
                ExposedDropdownMenuBox(
                    expanded = qualityFilter,
                    onExpandedChange = {
                        qualityFilter = !qualityFilter
                    }
                ) {
                    OutlinedTextField(
                        value = selectedQuality,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Quality") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = qualityFilter)
                        },
                        colors = ExposedDropdownMenuDefaults.textFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = qualityFilter,
                        onDismissRequest = {
                            qualityFilter = !qualityFilter
                        }
                    ) {
                        qualityOptions.forEach { quality ->
                            DropdownMenuItem(
                                text = { Text(quality) },
                                onClick = {
                                    selectedQuality = quality
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }

                // Quantity Input
                OutlinedTextField(
                    value = quantity,
                    onValueChange = {
                        quantity = it.filter { char -> char.isDigit() }
                        quantityError = null
                    },
                    label = { Text("Quantity") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = quantityError != null,
                    supportingText = {
                        quantityError?.let {
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Price Display (Read-only)
                OutlinedTextField(
                    value = price,
                    onValueChange = {},
                    label = { Text("Total Price (UGX)") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Submit Button
                Button(
                    onClick = {
                        // Reset errors
                        productError = null
                        quantityError = null
                        priceError = null

                        // Validate Product
                        if (selectedProduct == null) {
                            productError = "Please select a product"
                            return@Button
                        }

                        // Validate Quantity
                        val quantityInt = quantity.toIntOrNull()
                        if (quantityInt == null || quantityInt <= 0) {
                            quantityError = "Invalid quantity"
                            return@Button
                        }

                        // Check Stock Availability
                        if (quantityInt > selectedProduct!!.currentStock) {
                            quantityError = "Insufficient stock"
                            return@Button
                        }

                        // Validate Price
                        val priceDouble = price.toDoubleOrNull()
                        if (priceDouble == null || priceDouble <= 0) {
                            priceError = "Invalid price"
                            return@Button
                        }

                        // Perform Sale
                        stockViewModel.sellProduct(
                            productId = selectedProduct!!.id,
                            quantitySold = quantityInt
                        )

                        // Add Sale to Records
                        onAddSale(
                            selectedProduct!!.productName,
                            quantityInt.toString(),
                            priceDouble.toString()
                        )

                        // Dismiss Dialog
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.MonetizationOn, contentDescription = "Add Sale")
                    Spacer(Modifier.width(8.dp))
                    Text(text = "Add Sale")
                }
            }
        }
    }
}