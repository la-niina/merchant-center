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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Inventory
import androidx.compose.material.icons.rounded.Search
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
import domain.model.Product
import kotlinx.coroutines.launch
import presentation.SearchTextField
import viewmodel.ProductViewModel
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale
import kotlin.text.toBigDecimalOrNull

@Composable
fun ProductComponent(
    productViewModel: ProductViewModel = ProductViewModel(),
) {
    val products by productViewModel.productsList.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // Search state
    var searchInput by remember { mutableStateOf("") }
    val filteredProducts by remember(products, searchInput) {
        derivedStateOf {
            if (searchInput.isBlank()) {
                products
            } else {
                products.filter {
                    it.productName.contains(searchInput, ignoreCase = true) ||
                    it.productNumber.contains(searchInput, ignoreCase = true)
                }
            }
        }
    }

    var showAddProductDialog by remember { mutableStateOf(false) }
    var selectedProductForEdit by remember { mutableStateOf<Product?>(null) }
    val lazyListState = rememberLazyListState()

    // Initial data loading
    LaunchedEffect(Unit) {
        productViewModel.loadProducts()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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
                    ProductSectionHeader(
                        searchInput = searchInput,
                        onSearchChange = { searchInput = it },
                        onAddProductClick = { showAddProductDialog = true }
                    )
                }

                // Table Header
                item(key = "list-header") {
                    ProductListHeader()
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
                                text = if (searchInput.isBlank()) "No products in catalog" else "No products found",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = { showAddProductDialog = true },
                                modifier = Modifier.padding(top = 16.dp)
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add First Product")
                            }
                        }
                    }
                } else {
                    // Product Items
                    items(
                        items = filteredProducts,
                        key = { it.productId }
                    ) { product ->
                        ProductListing(
                            product = product,
                            onEdit = { selectedProductForEdit = product },
                            onDelete = {
                                coroutineScope.launch {
                                    productViewModel.deactivateProduct(product.productId)
                                }
                            }
                        )
                    }
                }
            }
        }

        // Add Product Dialog
        if (showAddProductDialog) {
            AddProductDialog(
                onDismissRequest = { showAddProductDialog = false },
                onAddProduct = { productNumber, name, unitPrice, description, category, stockQuantity ->
                    coroutineScope.launch {
                        productViewModel.addProduct(
                            productNumber = productNumber,
                            productName = name,
                            unitPrice = unitPrice,
                            description = description,
                            category = category,
                            stockQuantity = stockQuantity
                        )
                        showAddProductDialog = false
                    }
                }
            )
        }

        // Edit Product Dialog
        selectedProductForEdit?.let { product ->
            EditProductDialog(
                product = product,
                onDismissRequest = { selectedProductForEdit = null },
                onUpdateProduct = { updatedProduct ->
                    coroutineScope.launch {
                        productViewModel.updateProduct(updatedProduct)
                        selectedProductForEdit = null
                    }
                }
            )
        }
    }
}

@Composable
fun ProductSectionHeader(
    searchInput: String,
    onSearchChange: (String) -> Unit,
    onAddProductClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SearchTextField(
                    value = searchInput,
                    onValueChange = onSearchChange,
                    modifier = Modifier.weight(1f)
                )

                Button(
                    onClick = onAddProductClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add Product")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Product")
                }
            }
        }
    }
}

@Composable
fun ProductListHeader() {
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
            HeaderColumn("Product #", modifier = Modifier.weight(0.8f))
            HeaderColumn("Name", modifier = Modifier.weight(1.2f))
            HeaderColumn("Price", modifier = Modifier.weight(0.8f))
            HeaderColumn("Stock", modifier = Modifier.weight(0.6f))
            HeaderColumn("Category", modifier = Modifier.weight(0.8f))
            HeaderColumn("Actions", modifier = Modifier.weight(0.8f))
        }
    }
}

@Composable
fun ProductListing(
    product: Product,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Product Number
            Text(
                text = product.productNumber,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(0.8f)
            )

            // Product Name
            Text(
                text = product.productName,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1.2f)
            )

            // Unit Price
            Text(
                text = "${product.formattedUnitPrice()} UGX",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(0.8f)
            )

            // Stock Quantity
            Text(
                text = product.formattedStockQuantity(),
                style = MaterialTheme.typography.bodyMedium,
                color = if (product.isInStock()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(0.6f)
            )

            // Category
            Text(
                text = product.category ?: "-",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(0.8f)
            )

            // Actions
            Row(
                modifier = Modifier.weight(0.8f),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Rounded.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun AddProductDialog(
    onDismissRequest: () -> Unit,
    onAddProduct: (String, String, BigDecimal, String?, String?, Int) -> Unit,
) {
    var productNumber by rememberSaveable { mutableStateOf("") }
    var productName by rememberSaveable { mutableStateOf("") }
    var unitPrice by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("") }
    var stockQuantity by rememberSaveable { mutableStateOf("0") }

    var productNumberError by remember { mutableStateOf<String?>(null) }
    var productNameError by remember { mutableStateOf<String?>(null) }
    var priceError by remember { mutableStateOf<String?>(null) }

    val completeAdd = {
        when {
            productNumber.isBlank() -> productNumberError = "Product number is required"
            productName.isBlank() -> productNameError = "Product name is required"
            unitPrice.isBlank() -> priceError = "Price is required"
            else -> {
                val priceValue = unitPrice.replace(",", "").toBigDecimalOrNull() ?: BigDecimal.ZERO
                val stockValue = stockQuantity.toIntOrNull() ?: 0
                onAddProduct(
                    productNumber.trim(),
                    productName.trim(),
                    priceValue,
                    description.trim().ifBlank { null },
                    category.trim().ifBlank { null },
                    stockValue
                )
                onDismissRequest()
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
                        text = "Add New Product",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismissRequest) {
                        Icon(Icons.Rounded.Close, "Close dialog")
                    }
                }

                // Product Number
                OutlinedTextField(
                    value = productNumber,
                    onValueChange = {
                        productNumber = it
                        if (it.isNotBlank()) productNumberError = null
                    },
                    label = { Text("Product Number") },
                    placeholder = { Text("e.g., P001") },
                    singleLine = true,
                    isError = productNumberError != null,
                    supportingText = productNumberError?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Product Name
                OutlinedTextField(
                    value = productName,
                    onValueChange = {
                        productName = it
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
                    modifier = Modifier.fillMaxWidth()
                )

                // Unit Price
                OutlinedTextField(
                    value = unitPrice,
                    onValueChange = {
                        unitPrice = it.replace(Regex(","), "").let {
                            if (it.isEmpty()) ""
                            else try {
                                val number = it.toLong()
                                NumberFormat.getNumberInstance(Locale.US).format(number)
                            } catch (e: Exception) {
                                it
                            }
                        }
                        if (it.isNotBlank()) priceError = null
                    },
                    label = { Text("Unit Price") },
                    placeholder = { Text("Enter price in UGX") },
                    singleLine = true,
                    isError = priceError != null,
                    supportingText = priceError?.let { { Text(it) } },
                    trailingIcon = { Text("UGX", Modifier.padding(end = 8.dp)) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    placeholder = { Text("Enter product description") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Category
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category (Optional)") },
                    placeholder = { Text("e.g., Electronics, Food") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Stock Quantity
                OutlinedTextField(
                    value = stockQuantity,
                    onValueChange = { stockQuantity = it },
                    label = { Text("Initial Stock Quantity") },
                    placeholder = { Text("Enter initial stock") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { completeAdd() }),
                    modifier = Modifier.fillMaxWidth()
                )

                // Add Product Button
                Button(
                    onClick = { completeAdd() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Rounded.Inventory, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add Product", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
fun EditProductDialog(
    product: Product,
    onDismissRequest: () -> Unit,
    onUpdateProduct: (Product) -> Unit,
) {
    var productName by rememberSaveable { mutableStateOf(product.productName) }
    var unitPrice by rememberSaveable { mutableStateOf(product.formattedUnitPrice()) }
    var description by rememberSaveable { mutableStateOf(product.description ?: "") }
    var category by rememberSaveable { mutableStateOf(product.category ?: "") }
    var stockQuantity by rememberSaveable { mutableStateOf(product.stockQuantity.toString()) }

    var productNameError by remember { mutableStateOf<String?>(null) }
    var priceError by remember { mutableStateOf<String?>(null) }

    val completeUpdate = {
        when {
            productName.isBlank() -> productNameError = "Product name is required"
            unitPrice.isBlank() -> priceError = "Price is required"
            else -> {
                val priceValue = unitPrice.replace(",", "").toBigDecimalOrNull() ?: BigDecimal.ZERO
                val stockValue = stockQuantity.toIntOrNull() ?: 0
                val updatedProduct = product.copy(
                    productName = productName.trim(),
                    unitPrice = priceValue,
                    description = description.trim().ifBlank { null },
                    category = category.trim().ifBlank { null },
                    stockQuantity = stockValue,
                    updatedAt = java.time.LocalDateTime.now()
                )
                onUpdateProduct(updatedProduct)
                onDismissRequest()
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
                        text = "Edit Product",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismissRequest) {
                        Icon(Icons.Rounded.Close, "Close dialog")
                    }
                }

                // Product Number (Read-only)
                OutlinedTextField(
                    value = product.productNumber,
                    onValueChange = { },
                    label = { Text("Product Number") },
                    singleLine = true,
                    enabled = false,
                    modifier = Modifier.fillMaxWidth()
                )

                // Product Name
                OutlinedTextField(
                    value = productName,
                    onValueChange = {
                        productName = it
                        if (it.isNotBlank()) productNameError = null
                    },
                    label = { Text("Product Name") },
                    singleLine = true,
                    isError = productNameError != null,
                    supportingText = productNameError?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Unit Price
                OutlinedTextField(
                    value = unitPrice,
                    onValueChange = {
                        unitPrice = it.replace(Regex(","), "").let {
                            if (it.isEmpty()) ""
                            else try {
                                val number = it.toLong()
                                NumberFormat.getNumberInstance(Locale.US).format(number)
                            } catch (e: Exception) {
                                it
                            }
                        }
                        if (it.isNotBlank()) priceError = null
                    },
                    label = { Text("Unit Price") },
                    trailingIcon = { Text("UGX", Modifier.padding(end = 8.dp)) },
                    singleLine = true,
                    isError = priceError != null,
                    supportingText = priceError?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Category
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Stock Quantity
                OutlinedTextField(
                    value = stockQuantity,
                    onValueChange = { stockQuantity = it },
                    label = { Text("Stock Quantity") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { completeUpdate() }),
                    modifier = Modifier.fillMaxWidth()
                )

                // Update Button
                Button(
                    onClick = { completeUpdate() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Rounded.Edit, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Update Product", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
} 