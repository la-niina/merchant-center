package components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun SalesComponent(
    mainViewModel: MainViewModel = MainViewModel(),
) {
    val productsSales by mainViewModel.productsList.collectAsState()
    val currentDateTime by mainViewModel.currentDateTime.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // Date selection
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    
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
    }
    
    // Reload data when date changes
    LaunchedEffect(selectedDate) {
        mainViewModel.loadProductsForDate(selectedDate.toString())
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerpadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerpadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxSize(),
                state = lazyListState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item(key = "header") {
                    SalesSectionHeader(
                        searchInput = searchInput,
                        selectedDate = selectedDate,
                        onDateChange = { date ->
                            selectedDate = date
                        },
                        onSearchChange = { searchInput = it },
                        onRefreshClick = { mainViewModel.refreshProducts() },
                        onAddSaleClick = { showAddSaleDialog = true },
                        onDeleteSaleClick = { coroutineScope.launch { mainViewModel.clearProducts() } }
                    )
                }

                // Table Header
                item(key = "list-header") {
                    SalesListHeader()
                }

                if (filteredProducts.isEmpty()) {
                    item(key = "empty-state") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "No sales data for ${formatDisplayDate(selectedDate)}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = { showAddSaleDialog = true },
                                modifier = Modifier.padding(top = 16.dp)
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add First Sale")
                            }
                        }
                    }
                } else {
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
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Total Sales",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = mainViewModel.getFormattedTotalPriceOfProducts(),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
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
    selectedDate: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    onSearchChange: (String) -> Unit,
    onRefreshClick: () -> Unit,
    onAddSaleClick: () -> Unit,
    onDeleteSaleClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SearchTextField(
                    value = searchInput,
                    onValueChange = onSearchChange,
                    modifier = Modifier.weight(0.8F)
                )

                Row(
                    modifier = Modifier
                        .weight(1.2F)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onAddSaleClick,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Add,
                            contentDescription = "Add Sale"
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Add Sale",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    Button(
                        onClick = onRefreshClick,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        ),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Refresh,
                            contentDescription = "Refresh"
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Refresh",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    Button(
                        onClick = onDeleteSaleClick,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                    ) {
                        Icon(
                            Icons.Rounded.Delete,
                            contentDescription = "Delete All"
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Delete All",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Button(
                    onClick = { onDateChange(LocalDate.now()) },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedDate.isEqual(LocalDate.now()))
                            MaterialTheme.colorScheme.primary else
                            MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Text("Today", style = MaterialTheme.typography.labelMedium)
                }

                Button(
                    onClick = { onDateChange(LocalDate.now().minusDays(1)) },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedDate.isEqual(LocalDate.now().minusDays(1)))
                            MaterialTheme.colorScheme.primary else
                            MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier
                        .height(32.dp)
                        .padding(start = 8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Text("Yesterday", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
fun SalesListHeader() {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .height(40.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HeaderColumn("Product Name", modifier = Modifier.weight(1f))
            HeaderColumn("Qty", modifier = Modifier.weight(0.5f))
            HeaderColumn("Time", modifier = Modifier.weight(0.5f))
            HeaderColumn("Price", modifier = Modifier.weight(0.5f))
            HeaderColumn("Action", modifier = Modifier.weight(0.5f))
        }
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
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
fun AddSaleDialog(
    onDismissRequest: () -> Unit,
    onAddSale: (String, String, String) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()

    var quantity by rememberSaveable { mutableStateOf("1") }
    var price by rememberSaveable { mutableStateOf("") }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var productNameError by remember { mutableStateOf<String?>(null) }
    var priceError by remember { mutableStateOf<String?>(null) }

    val completeSale = {
        when {
            searchQuery.isBlank() -> {
                productNameError = "Product name is required"
            }
            price.isBlank() -> {
                priceError = "Price is required"
            }
            else -> {
                coroutineScope.launch {
                    onAddSale(
                        searchQuery.trim(),
                        quantity.trim().ifBlank { "1" },
                        price.trim().replace(",", "")
                    )
                    onDismissRequest()
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnClickOutside = true,
            dismissOnBackPress = true,
            usePlatformDefaultWidth = true
        )
    ) {
        ElevatedCard(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(0.9f),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.elevatedCardElevation(8.dp)
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
                        text = "Add New Sale",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismissRequest) {
                        Icon(
                            Icons.Rounded.Close, 
                            "Close dialog",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Product Selection
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        if (it.isNotBlank()) productNameError = null
                    },
                    label = { Text("Product Name") },
                    placeholder = { Text("Enter product name") },
                    singleLine = true,
                    isError = productNameError != null,
                    supportingText = productNameError?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                // Quantity Input
                OutlinedTextField(
                    value = quantity,
                    onValueChange = {
                        quantity = it
                        /**if (it.isBlank() || it.matches(Regex("^\\d+$"))) {

                        }**/
                    },
                    label = { Text("Quantity") },
                    placeholder = { Text("Enter quantity") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                // Price Display
                OutlinedTextField(
                    value = price,
                    onValueChange = {
                        // show auto change to UGX current format 200, 2,000, 30,000, 100,000 etc
                        price = it.replace(Regex(","), "").let {
                            if (it.isEmpty()) ""
                            else try {
                                val number = it.toLong()
                                NumberFormat.getNumberInstance(Locale.US).format(number)
                            } catch (e: Exception) {
                                it
                            }
                        }

                       /** if (it.isEmpty() || it.matches(Regex("^\\d+$|^\\d{1,3}(,\\d{3})*$"))) {
                            price = it
                            if (it.isNotBlank()) priceError = null
                        } **/
                    },
                    label = { Text("Total Price") },
                    placeholder = { Text("Enter price in UGX") },
                    singleLine = true,
                    isError = priceError != null,
                    supportingText = priceError?.let { { Text(it) } },
                    trailingIcon = { Text("UGX", Modifier.padding(end = 8.dp)) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { completeSale() }),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                // Complete Sale Button
                Button(
                    onClick = { completeSale() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Rounded.MonetizationOn, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Complete Sale",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

// Helper function to format display date
private fun formatDisplayDate(date: LocalDate): String {
    val formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")
    return date.format(formatter)
}