package components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import presentation.SearchTextField
import viewmodel.AuthViewModel
import viewmodel.StockViewModel

@Composable
fun ProductComponent(
    authViewModel: AuthViewModel = AuthViewModel(),
    stockViewModel: StockViewModel = StockViewModel(),
    on: () -> Unit
) {
    var searchInput by rememberSaveable { mutableStateOf("") }
    var isAddProductDialogVisible by rememberSaveable { mutableStateOf(false) }

    val authState by authViewModel.authState.collectAsState()
    val stockState by stockViewModel.stockState.collectAsState()

    val overview = listOf(
        OverviewItem(title = "Stocks Flow", value = stockState.products.size.toString()),
        OverviewItem(title = "Expenses", value = formatCurrency(stockState.totalInventoryValue)),
        OverviewItem(title = "Sales FLow", value = formatCurrency(stockState.totalSales))
    )

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            userScrollEnabled = true
        ) {
            // Overview Section
            item(span = { GridItemSpan(maxLineSpan) }) {
                OverviewSection(overview)
            }

            // Search and Action Buttons
            item(span = { GridItemSpan(maxLineSpan) }) {
                ActionSection(
                    searchInput = searchInput,
                    onSearchChange = { searchInput = it },
                    onAddProduct = { isAddProductDialogVisible = true },
                    onLogout = { authViewModel.logout() }
                )
            }

            // Product List
            items(stockState.products.filter {
                it.productName.contains(searchInput, ignoreCase = true)
            }) { product ->
                ProductCard(
                    product = product,
                    onDelete = { stockViewModel.deleteProduct(product.id) }
                )
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Spacer(modifier = Modifier.padding(vertical = 50.dp))
            }
        }

        // Authentication Dialog
        if (!authState.isAuthenticated) {
            AuthenticationDialog(
                authViewModel = authViewModel,
                onClose = on
            )
        }

        // Add Product Dialog
        if (isAddProductDialogVisible) {
            AddProductDialog(
                stockViewModel = stockViewModel,
                onDismiss = { isAddProductDialogVisible = false }
            )
        }
    }
}

@Composable
fun OverviewSection(items: List<OverviewItem>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { item ->
            OverviewCard(
                modifier = Modifier.weight(1f),
                title = item.title,
                value = item.value
            )
        }
    }
}

@Composable
fun OverviewCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(20),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )
    ) {
        Column(
            modifier = Modifier.padding(35.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Normal,
                    fontSize = 16.sp,
                    lineHeight = 16.sp
                )
                Icon(
                    Icons.Rounded.MoreHoriz,
                    contentDescription = null
                )
            }

            Text(
                text = value,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun ActionSection(
    searchInput: String,
    onSearchChange: (String) -> Unit,
    onAddProduct: () -> Unit,
    onLogout: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SearchTextField(
            value = searchInput,
            onValueChange = onSearchChange
        )

        Row {
            Button(
                onClick = onAddProduct,
                shape = RoundedCornerShape(100),
                colors = ButtonDefaults.buttonColors(
                    contentColor = MaterialTheme.colorScheme.onSecondary
                )
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add Stocks")
                Spacer(modifier = Modifier.padding(horizontal = 5.dp))
                Text("Add Stocks")
            }

            Spacer(modifier = Modifier.width(10.dp))

            Button(
                onClick = onLogout,
                shape = RoundedCornerShape(100),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                )
            ) {
                Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = "Logout")
                Spacer(modifier = Modifier.padding(horizontal = 5.dp))
                Text("Logout")
            }
        }
    }
}

@Composable
fun ProductCard(
    product: StockViewModel.Product,
    onDelete: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20),
        colors = CardDefaults.elevatedCardColors(
            containerColor = Color.Yellow.copy(alpha = 0.2f)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = product.id)
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = "Delete Product"
                    )
                }
            }

            Text(
                text = product.productName,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProductDetailRow("Price", formatCurrency(product.estimatedPrice))
                ProductDetailRow("In Stock", product.currentStock.toString())
            }
        }
    }
}

@Composable
fun ProductDetailRow(label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(text = label)
        Text(text = value)
    }
}

@Composable
fun AddProductDialog(
    stockViewModel: StockViewModel,
    onDismiss: () -> Unit
) {
    var productName by rememberSaveable { mutableStateOf("") }
    var estimatedPrice by rememberSaveable { mutableStateOf(0.0) }
    var currentStock by rememberSaveable { mutableStateOf(0) }
    var minimumStockThreshold by rememberSaveable { mutableStateOf(10) }
    var unitOfMeasurement by rememberSaveable { mutableStateOf("piece") }
    var errorMessage by rememberSaveable { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        ElevatedCard(
            modifier = Modifier.padding(20.dp),
            shape = RoundedCornerShape(5)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Add New Product", fontWeight = FontWeight.Bold, fontSize = 20.sp)

                OutlinedTextField(
                    value = productName,
                    onValueChange = { productName = it },
                    label = { Text("Product Name") },
                    isError = errorMessage.isNotEmpty(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = estimatedPrice.toString(),
                        onValueChange = { estimatedPrice = it.toDoubleOrNull() ?: 0.0 },
                        label = { Text("Estimated Price") },
                        isError = errorMessage.isNotEmpty(),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = unitOfMeasurement,
                        onValueChange = { unitOfMeasurement = it },
                        label = { Text("Unit of Measurement") },
                        isError = errorMessage.isNotEmpty(),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = currentStock.toString(),
                        onValueChange = { currentStock = it.toIntOrNull() ?: 0 },
                        label = { Text("Current Stock") },
                        isError = errorMessage.isNotEmpty(),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = minimumStockThreshold.toString(),
                        onValueChange = { minimumStockThreshold = it.toIntOrNull() ?: 10 },
                        label = { Text("Minimum Stock Threshold") },
                        isError = errorMessage.isNotEmpty(),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Button(
                    onClick = {
                        if (productName.isNotBlank() && estimatedPrice > 0 && currentStock >= 0) {
                            stockViewModel.addProduct(
                                productName = productName,
                                estimatedPrice = estimatedPrice,
                                initialStock = currentStock,
                                minimumStockThreshold = minimumStockThreshold,
                                unitOfMeasurement = unitOfMeasurement
                            )
                            onDismiss()
                        } else {
                            errorMessage = "Please fill in all fields correctly."
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Add Product")
                }
            }
        }
    }
}

@Composable
fun AuthenticationDialog(
    authViewModel: AuthViewModel,
    onClose: () -> Unit
) {
    var password by rememberSaveable { mutableStateOf("") }

    Dialog(
        onDismissRequest = { /* Prevent dismissing */ },
        properties = DialogProperties(
            dismissOnClickOutside = false,
            dismissOnBackPress = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
                .background(Color.Black.copy(alpha = 0.9f)),
            contentAlignment = Alignment.Center
        ) {
            ElevatedCard(
                modifier = Modifier.width(550.dp).fillMaxWidth().padding(20.dp),
                shape = RoundedCornerShape(5)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Management Login",
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp
                        )

                        IconButton(
                            onClick = onClose,
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close")
                        }
                    }

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        singleLine = true,
                        isError = authViewModel.authState.value.errorMessage != null,
                        supportingText = {
                            authViewModel.authState.value.errorMessage?.let { errorMsg ->
                                Text(
                                    text = errorMsg,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            authViewModel.authenticate(password)
                        },
                        shape = RoundedCornerShape(100),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Login")
                    }
                }
            }
        }
    }
}

data class OverviewItem(val title: String, val value: String)

fun formatCurrency(amount: Double): String {
    return String.format("%,.2f UGX", amount)
}
