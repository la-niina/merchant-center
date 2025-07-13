package components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.poi.ss.usermodel.BorderStyle
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import presentation.EditorTextField
import viewmodel.MainViewModel
import domain.model.SalesProduct
import java.awt.SystemTray
import java.awt.Toolkit
import java.awt.TrayIcon
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.swing.JOptionPane

enum class ReportPeriod {
    CUSTOM, DIALY, WEEKLY, MONTHLY, YEARLY
}

enum class ExportFormat {
    PDF, CSV, XLSX
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsComponent(
    mainViewModel: MainViewModel = MainViewModel()
) {
    var selectedPeriod by remember { mutableStateOf(ReportPeriod.DIALY) }
    var startDate by remember { mutableStateOf(LocalDate.now().minusWeeks(1)) }
    var endDate by remember { mutableStateOf(LocalDate.now()) }
    var isExportDialogVisible by remember { mutableStateOf(false) }
    var expandDropDown by remember { mutableStateOf(false) }
    var isDateRangePickerVisible by remember { mutableStateOf(false) }

    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val products by mainViewModel.allproductsList.collectAsState()

    // Enhanced filtering function with custom date range support
    fun filterProductsByPeriod(products: List<SalesProduct>): List<SalesProduct> {
        return products.filter { product ->
            try {
                val productDate = product.time.toLocalDate()
                when (selectedPeriod) {
                    ReportPeriod.DIALY ->
                        productDate.isAfter(LocalDate.now().minusDays(1)) ||
                                productDate.isEqual(LocalDate.now().minusDays(1))

                    ReportPeriod.WEEKLY ->
                        productDate.isAfter(LocalDate.now().minusWeeks(1)) ||
                                productDate.isEqual(LocalDate.now().minusWeeks(1))

                    ReportPeriod.MONTHLY ->
                        productDate.isAfter(LocalDate.now().minusMonths(1)) ||
                                productDate.isEqual(LocalDate.now().minusMonths(1))

                    ReportPeriod.YEARLY ->
                        productDate.isAfter(LocalDate.now().minusYears(1)) ||
                                productDate.isEqual(LocalDate.now().minusYears(1))

                    ReportPeriod.CUSTOM ->
                        (productDate.isAfter(startDate) &&
                                productDate.isBefore(endDate))
                }
            } catch (e: Exception) {
                false
            }
        }
    }

    // Always calculate preview data
    val exportPreviewData = remember(products, selectedPeriod) {
        filterProductsByPeriod(products)
    }

    LaunchedEffect(Unit) {
        mainViewModel.loadProductsForDate(LocalDate.now().toString())
        mainViewModel.loadProducts()
        mainViewModel.loadAllProducts()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
    ) { innerpadding ->
        LazyColumn(
            modifier = Modifier.padding(horizontal = 20.dp).fillMaxSize(),
            contentPadding = innerpadding,
            state = lazyListState,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            item {
                // Period Selection and Export Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Period Dropdown
                    ExposedDropdownMenuBox(
                        expanded = expandDropDown,
                        onExpandedChange = {
                            expandDropDown = !expandDropDown
                        }
                    ) {
                        OutlinedTextField(
                            value = selectedPeriod.name.replaceFirstChar { it.titlecase() },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Report Period") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandDropDown)
                            },
                            modifier = Modifier.menuAnchor()
                        )

                        ExposedDropdownMenu(
                            expanded = expandDropDown,
                            onDismissRequest = { expandDropDown = false }
                        ) {
                            ReportPeriod.entries.forEach { period ->
                                DropdownMenuItem(
                                    text = { Text(period.name.replaceFirstChar { it.titlecase() }) },
                                    onClick = {
                                        selectedPeriod = period
                                        expandDropDown = false

                                        // Set default date ranges for predefined periods
                                        when (period) {
                                            ReportPeriod.DIALY -> {
                                                startDate = LocalDate.now()
                                                endDate = LocalDate.now()
                                            }

                                            ReportPeriod.WEEKLY -> {
                                                startDate = LocalDate.now().minusWeeks(1)
                                                endDate = LocalDate.now()
                                            }

                                            ReportPeriod.MONTHLY -> {
                                                startDate = LocalDate.now().minusMonths(1)
                                                endDate = LocalDate.now()
                                            }

                                            ReportPeriod.YEARLY -> {
                                                startDate = LocalDate.now().minusYears(1)
                                                endDate = LocalDate.now()
                                            }

                                            ReportPeriod.CUSTOM -> {
                                                isDateRangePickerVisible = true
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // Export Button
                    Button(
                        onClick = {
                            isExportDialogVisible = true
                        }
                    ) {
                        Text(
                            text = "Export Report",
                        )
                        Icon(
                            imageVector = Icons.Rounded.Download,
                            contentDescription = "Export Report"
                        )
                    }
                }
            }

            item {
                // Date Range Picker for Custom Period
                if (selectedPeriod == ReportPeriod.CUSTOM && isDateRangePickerVisible) {
                    DateRangePicker(
                        onDateRangeSelected = { start, end ->
                            startDate = start
                            endDate = end
                            isDateRangePickerVisible = false
                        },
                        initialStartDate = startDate,
                        initialEndDate = endDate
                    )
                }
            }

            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Export Preview (${selectedPeriod.name} Report)",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "Total Sales: ${exportPreviewData.size}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Total Value: ${
                                exportPreviewData.sumOf { it.totalPrice.toDouble() }
                                    .let { "UGX ${"%,d".format(it.toLong())}" }
                            }",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }
            }

            // Export Preview (Always Visible)
            if (exportPreviewData.isNotEmpty()) {
                items(exportPreviewData) { product ->
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = product.productName,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Column(
                                    modifier = Modifier.weight(0.3f)
                                ) {
                                    Text(
                                        text = product.qty.toString(),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Column(
                                    modifier = Modifier.weight(0.5f)
                                ) {
                                    Text(
                                        text = product.formattedTotalPrice(),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }

                if (exportPreviewData.size > 5) {
                    item {
                        Text(
                            text = "... and ${exportPreviewData.size - 5} more",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            } else {
                // Show a message when no products are available
                item {
                    Text(
                        text = "No products available for ${selectedPeriod.name} report",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }
            }
        }

        // Export Dialog
        if (isExportDialogVisible) {
            ExportReportDialog(
                previewData = exportPreviewData,
                onDismiss = {
                    isExportDialogVisible = false
                },
                onExport = { format, fileName, sellsTitle, CompanyName, address, contact ->
                    coroutineScope.launch {
                        if (fileName.isNotEmpty()) {
                            exportReport(
                                fileName = fileName,
                                sellsTitle = sellsTitle,
                                companyName = CompanyName,
                                address = address,
                                contact = contact,
                                period = selectedPeriod,
                                format = format,
                                startDate = startDate,
                                endDate = endDate,
                                products = exportPreviewData
                            )
                        }
                    }
                    isExportDialogVisible = false
                }
            )
        }
    }
}

// Utility extension function for more flexible date range checking
fun LocalDate.isInRange(start: LocalDate, end: LocalDate): Boolean {
    return !this.isBefore(start) && !this.isAfter(end)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangePicker(
    onDateRangeSelected: (LocalDate, LocalDate) -> Unit,
    initialStartDate: LocalDate = LocalDate.now().minusDays(30),
    initialEndDate: LocalDate = LocalDate.now()
) {
    var startDate by remember { mutableStateOf(initialStartDate) }
    var endDate by remember { mutableStateOf(initialEndDate) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    // Date selection validation
    val isValidDateRange = !startDate.isAfter(endDate)

    Column(
        modifier = Modifier
            .width(350.dp)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Start Date Selection
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = "Start Date",
                tint = MaterialTheme.colorScheme.primary
            )

            OutlinedButton(
                onClick = { showStartDatePicker = true },
                modifier = Modifier.weight(1f)
            ) {
                Text("Start Date: ${formatLocalDate(startDate)}")
            }
        }

        // End Date Selection
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = "End Date",
                tint = MaterialTheme.colorScheme.primary
            )

            OutlinedButton(
                onClick = { showEndDatePicker = true },
                modifier = Modifier.weight(1f)
            ) {
                Text("End Date: ${formatLocalDate(endDate)}")
            }
        }

        // Date Range Validation Message
        if (!isValidDateRange) {
            Text(
                text = "Start date must be before or equal to end date",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        // Confirm Button
        Button(
            onClick = { onDateRangeSelected(startDate, endDate) },
            enabled = isValidDateRange,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Confirm Date Range")
        }

        // Start Date Picker Dialog
        if (showStartDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showStartDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showStartDatePicker = false
                            // Ensure start date is not after end date
                            if (startDate.isAfter(endDate)) {
                                startDate = endDate
                            }
                        }
                    ) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showStartDatePicker = false }) {
                        Text("Cancel")
                    }
                },
                shape = RoundedCornerShape(5)
            ) {
                DatePicker(
                    state = rememberDatePickerState(
                        initialSelectedDateMillis = startDate.atStartOfDay()
                            .toInstant(java.time.ZoneOffset.UTC).toEpochMilli()
                    ),
                    title = { Text("Select Start Date") },
                    headline = {
                        Text(formatLocalDate(startDate))
                    },
                    modifier = Modifier.padding(10.dp).height(400.dp)
                )
            }
        }

        // End Date Picker Dialog
        if (showEndDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showEndDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showEndDatePicker = false
                            // Ensure end date is not before start date
                            if (endDate.isBefore(startDate)) {
                                endDate = startDate
                            }
                        }
                    ) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEndDatePicker = false }) {
                        Text("Cancel")
                    }
                },
                shape = RoundedCornerShape(5)
            ) {
                DatePicker(
                    state = rememberDatePickerState(
                        initialSelectedDateMillis = endDate.atStartOfDay()
                            .toInstant(java.time.ZoneOffset.UTC).toEpochMilli()
                    ),
                    title = { Text("Select End Date") },
                    headline = {
                        Text(formatLocalDate(endDate))
                    },
                    modifier = Modifier.padding(10.dp).height(400.dp)
                )
            }
        }
    }
}

// Utility function to format LocalDate
fun formatLocalDate(date: LocalDate): String {
    return date.format(DateTimeFormatter.ISO_LOCAL_DATE)
}

// Improved CSV Export
fun exportToCsv(products: List<SalesProduct>, file: File) {
    try {
        file.bufferedWriter().use { out ->
            // Write CSV header with more detailed columns
            out.write("Product ID,Product Name,Quantity,Unit Price,Total Price,Date")
            out.newLine()

            // Write product data
            products.forEach { product ->
                out.write(
                    "${product.pid}," +
                            "${product.productName.replace(",", ";")}," +
                            "${product.qty}," +
                            "${product.unitPrice}," +
                            "${product.formattedDateTime()}," +
                            "${product.formattedTotalPrice()}"
                )
                out.newLine()
            }
        }
        println("CSV exported successfully to ${file.absolutePath}")
    } catch (e: Exception) {
        println("CSV Export failed: ${e.message}")
        e.printStackTrace()
    }
}

// Improved Excel Export
fun exportToExcel(products: List<SalesProduct>, file: File) {
    try {
        XSSFWorkbook().use { workbook ->
            val sheet = workbook.createSheet("Sales Report")

            // Create styles
            val headerStyle = workbook.createCellStyle().apply {
                fillForegroundColor = IndexedColors.LIGHT_BLUE.index
                fillPattern = FillPatternType.SOLID_FOREGROUND
                alignment = HorizontalAlignment.CENTER
                borderBottom = BorderStyle.THIN
                borderTop = BorderStyle.THIN
                borderLeft = BorderStyle.THIN
                borderRight = BorderStyle.THIN
            }

            val headerFont = workbook.createFont().apply {
                bold = true
                color = IndexedColors.WHITE.index
                fontHeightInPoints = 12
            }
            headerStyle.setFont(headerFont)

            // Data style
            val dataStyle = workbook.createCellStyle().apply {
                alignment = HorizontalAlignment.LEFT
                borderBottom = BorderStyle.THIN
                borderTop = BorderStyle.THIN
                borderLeft = BorderStyle.THIN
                borderRight = BorderStyle.THIN
            }

            // Currency style
            val currencyStyle = workbook.createCellStyle().apply {
                dataFormat = workbook.createDataFormat().getFormat("#,##0.00")
                alignment = HorizontalAlignment.RIGHT
                borderBottom = BorderStyle.THIN
                borderTop = BorderStyle.THIN
                borderLeft = BorderStyle.THIN
                borderRight = BorderStyle.THIN
            }

            // Create header row
            val headers = arrayOf(
                "Product ID",
                "Product Name",
                "Quantity",
                "Unit Price",
                "Total Price",
                "Date"
            )
            val headerRow = sheet.createRow(0)
            headers.forEachIndexed { index, header ->
                val cell = headerRow.createCell(index)
                cell.setCellValue(header)
                cell.cellStyle = headerStyle
            }

            // Add product data
            products.forEachIndexed { rowIndex, product ->
                val row = sheet.createRow(rowIndex + 1)

                // Product ID
                row.createCell(0).apply {
                    setCellValue(product.pid.toDouble())
                    cellStyle = dataStyle
                }

                // Product Name
                row.createCell(1).apply {
                    setCellValue(product.productName)
                    cellStyle = dataStyle
                }

                // Quantity
                row.createCell(2).apply {
                    setCellValue(product.qty.toDouble())
                    cellStyle = dataStyle
                }

                // Unit Price
                row.createCell(3).apply {
                    setCellValue(product.unitPrice.toDouble())
                    cellStyle = currencyStyle
                }

                // Total Price
                row.createCell(4).apply {
                    setCellValue(product.totalPrice.toDouble())
                    cellStyle = currencyStyle
                }

                // Date
                row.createCell(5).apply {
                    setCellValue(product.formattedDateTime())
                    cellStyle = dataStyle
                }
            }

            // Auto-size columns
            headers.indices.forEach { sheet.autoSizeColumn(it) }

            // Write to file
            FileOutputStream(file).use { outputStream ->
                workbook.write(outputStream)
            }

            println("Excel exported successfully to ${file.absolutePath}")
        }
    } catch (e: Exception) {
        println("Excel Export failed: ${e.message}")
        e.printStackTrace()
    }
}

@Composable
fun ExportReportDialog(
    previewData: List<SalesProduct>,
    onDismiss: () -> Unit,
    onExport: (ExportFormat, String, String, String, String, String) -> Unit
) {
    var fileName by rememberSaveable { mutableStateOf("") }
    var sellsTitle by rememberSaveable { mutableStateOf("") }
    var companyName by rememberSaveable { mutableStateOf("") }
    var companyAddress by rememberSaveable { mutableStateOf("") }
    var companyContact by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Report") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Total Products: ${previewData.size}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Total Value: ${
                        previewData.sumOf { it.totalPrice.toDouble() }
                            .let { "UGX ${"%,d".format(it.toLong())}" }
                    }",
                    style = MaterialTheme.typography.bodyMedium
                )
                EditorTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = sellsTitle,
                    default = false,
                    placeHolder = "Report Title",
                    onValueChange = {
                        sellsTitle = it
                    }
                )
                EditorTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = companyName,
                    default = false,
                    placeHolder = "Company Name",
                    onValueChange = {
                        companyName = it
                    }
                )
                EditorTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = companyAddress,
                    default = false,
                    placeHolder = "Company Address",
                    onValueChange = {
                        companyAddress = it
                    }
                )
                EditorTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = companyContact,
                    default = false,
                    placeHolder = "Company Contact",
                    onValueChange = {
                        companyContact = it
                    }
                )
                EditorTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = fileName,
                    default = false,
                    placeHolder = "File Name",
                    onValueChange = {
                        fileName = it.trim()
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                ExportFormat.entries.forEach { format ->
                    Button(
                        onClick = {
                            if (fileName.isNotBlank() && sellsTitle.isNotBlank() && companyName.isNotBlank()) {
                                onExport(format, fileName, sellsTitle, companyName, companyAddress, companyContact)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Export as ${format.name}")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Modify the date parsing function
fun parseProductDate(timeString: String): LocalDate {
    val parsers = listOf(
        DateTimeFormatter.ISO_LOCAL_DATE_TIME,
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("dd-MM-yyyy"),
        DateTimeFormatter.ofPattern("MM/dd/yyyy")
    )

    for (parser in parsers) {
        try {
            return LocalDateTime.parse(timeString, parser).toLocalDate()
        } catch (e: Exception) {
            continue
        }
    }

    // Fallback to current date if parsing fails
    println("Failed to parse date: $timeString")
    return LocalDate.now()
}

// Modify the export report function
fun exportReport(
    fileName: String,
    sellsTitle: String,
    companyName: String,
    address: String,
    contact: String,
    period: ReportPeriod,
    format: ExportFormat,
    startDate: LocalDate = LocalDate.now().minusWeeks(1),
    endDate: LocalDate = LocalDate.now(),
    products: List<SalesProduct>,
) {
    val currentDate = LocalDate.now()

    // Filter products based on the selected period
    val filteredProducts = products.filter { product ->
        try {
            val productDate = product.time.toLocalDate()
            when (period) {
                ReportPeriod.DIALY -> productDate.isEqual(currentDate)
                ReportPeriod.WEEKLY -> productDate.isAfter(currentDate.minusWeeks(1))
                ReportPeriod.MONTHLY -> productDate.isAfter(currentDate.minusMonths(1))
                ReportPeriod.YEARLY -> productDate.isAfter(currentDate.minusYears(1))
                ReportPeriod.CUSTOM -> productDate.isInRange(startDate, endDate)
            }
        } catch (e: Exception) {
            println("Error filtering product: ${product.time}")
            false
        }
    }

    // Generate filename with date and period
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val finalFileName = if (fileName.isEmpty()) {
        "Sales_Report_${period.name}_${currentDate.format(formatter)}"
    } else {
        "${fileName}_${period.name}_${currentDate.format(formatter)}"
    }

    // Determine export directory (cross-platform)
    val exportDir = getExportDirectory()

    // Ensure export directory exists
    exportDir.mkdirs()

    // Export based on selected format
    val exportFile = File(exportDir, "$finalFileName.${format.name.lowercase()}")

    // Wrap export in a try-catch to handle potential errors
    try {
        when (format) {
            ExportFormat.PDF -> exportToPdf(filteredProducts, exportFile, sellsTitle, companyName, address, contact)
            ExportFormat.CSV -> exportToCsv(filteredProducts, exportFile)
            ExportFormat.XLSX -> exportToExcel(filteredProducts, exportFile)
        }

        // Optional: Show a success notification
        showExportSuccessNotification(exportFile)
    } catch (e: Exception) {
        // Handle export errors
        showExportErrorNotification(e)
    }
}

fun exportToPdf(
    products: List<SalesProduct>,
    file: File,
    sellsTitle: String,
    companyName: String,
    address: String,
    contact: String
) {
    require(products.isNotEmpty()) { "No products to export" }
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    val currentDateTime = LocalDateTime.now()

    PDDocument().use { document ->
        var page = PDPage()
        document.addPage(page)

        PDPageContentStream(
            document,
            page,
            PDPageContentStream.AppendMode.OVERWRITE,
            true
        ).use { contentStream ->
            // Page setup
            val pageWidth = page.mediaBox.width
            val pageHeight = page.mediaBox.height
            val margin = 50f
            // Adjust column widths to ensure date fits properly within its column
            val columnWidths = floatArrayOf(140f, 80f, 90f, 90f, 110f) 
            val titleY = pageHeight - margin
            // Increase spacing for the report summary
            val tableTop = pageHeight - margin - 180f
            val rowHeight = 20f

            // Document Header
            contentStream.beginText()
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 20f)
            contentStream.setNonStrokingColor(0, 0, 128) // Navy blue for title
            contentStream.newLineAtOffset((pageWidth / 2) - 100, titleY)
            contentStream.showText(sellsTitle)
            contentStream.endText()

            // Company Info - Add more space after title
            contentStream.beginText()
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12f)
            contentStream.setNonStrokingColor(0, 0, 0) // Reset to black
            contentStream.newLineAtOffset(margin, titleY - 40f)  // Increased vertical spacing
            contentStream.showText(companyName)
            contentStream.endText()

            contentStream.beginText()
            contentStream.setFont(PDType1Font.HELVETICA, 10f)
            contentStream.newLineAtOffset(margin, titleY - 55f)  // Adjusted spacing
            contentStream.showText(if(address.isNotEmpty()) address else "123 Power Avenue, Kampala, Uganda")
            contentStream.endText()

            contentStream.beginText()
            contentStream.setFont(PDType1Font.HELVETICA, 10f)
            contentStream.newLineAtOffset(margin, titleY - 70f)  // Adjusted spacing
            contentStream.showText(if (contact.isNotEmpty()) contact else "Tel: +256 701 234567 | Email: info@powersolutions.com")
            contentStream.endText()

            // Report Summary Box - Move down to prevent overlapping with title
            val summaryBoxX = pageWidth - margin - 200
            val summaryBoxY = titleY - 95f  // Increased spacing from title
            val summaryBoxWidth = 200f
            val summaryBoxHeight = 65f

            // Draw summary box
            contentStream.setNonStrokingColor(240, 240, 250) // Light blue background
            contentStream.addRect(summaryBoxX, summaryBoxY, summaryBoxWidth, summaryBoxHeight)
            contentStream.fill()

            // Reset color to black for text
            contentStream.setNonStrokingColor(0, 0, 0)

            // Summary Box Border
            contentStream.setStrokingColor(0, 0, 128) // Navy border
            contentStream.setLineWidth(1f)
            contentStream.addRect(summaryBoxX, summaryBoxY, summaryBoxWidth, summaryBoxHeight)
            contentStream.stroke()

            // Calculate totals
            val totalProducts = products.size
            val totalQuantity = products.sumOf { it.qty.toLong() }
            val totalValue = products.sumOf { it.totalPrice.toDouble() }

            // Summary Box Content
            contentStream.beginText()
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 11f)
            contentStream.newLineAtOffset(summaryBoxX + 10, summaryBoxY + summaryBoxHeight - 15)
            contentStream.showText("REPORT SUMMARY")
            contentStream.endText()

            contentStream.beginText()
            contentStream.setFont(PDType1Font.HELVETICA, 9f)
            contentStream.newLineAtOffset(summaryBoxX + 10, summaryBoxY + summaryBoxHeight - 30)
            contentStream.showText("Total Products: $totalProducts")
            contentStream.endText()

            contentStream.beginText()
            contentStream.setFont(PDType1Font.HELVETICA, 9f)
            contentStream.newLineAtOffset(summaryBoxX + 10, summaryBoxY + summaryBoxHeight - 45)
            contentStream.showText("Sold Items: $totalQuantity units")
            contentStream.endText()

            contentStream.beginText()
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 10f)
            contentStream.newLineAtOffset(summaryBoxX + 10, summaryBoxY + summaryBoxHeight - 60)
            contentStream.showText("Total Revenue: ${"%,d".format(totalValue.toLong())} UGX")
            contentStream.endText()

            // Add horizontal divider
            contentStream.setStrokingColor(0, 0, 128) // Navy line
            contentStream.setLineWidth(1.5f)
            contentStream.moveTo(margin, tableTop + 20)
            contentStream.lineTo(pageWidth - margin, tableTop + 20)
            contentStream.stroke()

            // Table Header
            val headers = arrayOf("Product Name", "Quantity", "Unit Price", "Total Price", "Date")

            // Draw table header background
            contentStream.setNonStrokingColor(0, 0, 128) // Navy blue for header bg
            contentStream.addRect(margin, tableTop, pageWidth - (2 * margin), rowHeight)
            contentStream.fill()

            // Draw table header text
            contentStream.setNonStrokingColor(255, 255, 255) // White text
            var xPosition = margin + 5

            headers.forEachIndexed { index, header ->
                contentStream.beginText()
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 10f)
                contentStream.newLineAtOffset(xPosition, tableTop + 7)
                contentStream.showText(header)
                contentStream.endText()
                xPosition += columnWidths[index]
            }

            // Reset to black for data rows
            contentStream.setNonStrokingColor(0, 0, 0)

            // Setup for zebra striping
            var isAlternate = false
            var yPosition = tableTop - rowHeight

            // Product Data
            products.forEach { product ->
                // Add zebra striping
                if (isAlternate) {
                    contentStream.setNonStrokingColor(
                        240,
                        240,
                        250
                    ) // Light blue for alternate rows
                    contentStream.addRect(margin, yPosition, pageWidth - (2 * margin), rowHeight)
                    contentStream.fill()
                    contentStream.setNonStrokingColor(0, 0, 0) // Reset to black for text
                }

                // Calculate total price for this product
                val unitPrice = product.unitPrice.toDouble()
                val quantity = product.qty
                val totalValue = product.totalPrice.toDouble()

                // Draw row data
                val rowData = arrayOf(
                    product.productName,
                    product.qty.toString(),
                    "${"%,d".format(unitPrice.toLong())} UGX",
                    "${"%,d".format(totalValue.toLong())} UGX",
                    formatDateForPdfExport(product.time.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                )

                xPosition = margin + 5
                rowData.forEachIndexed { index, data ->
                    contentStream.beginText()
                    contentStream.setFont(
                        if (index == 0) PDType1Font.HELVETICA_BOLD else PDType1Font.HELVETICA,
                        9f
                    )
                    contentStream.newLineAtOffset(xPosition, yPosition + 7)

                    // Truncate long text to ensure it fits in the column
                    val maxChars = when (index) {
                        0 -> 25 // Product Name
                        4 -> 12 // Date
                        else -> 12 // Other columns
                    }
                    
                    val displayText = if (data.length > maxChars)
                        "${data.take(maxChars - 3)}..." else data

                    contentStream.showText(displayText)
                    contentStream.endText()
                    xPosition += columnWidths[index]
                }

                yPosition -= rowHeight
                isAlternate = !isAlternate

                // Add a new page if running out of space
                if (yPosition < margin + 30) {
                    // Draw table border on current page
                    contentStream.setStrokingColor(0, 0, 128)
                    contentStream.setLineWidth(0.5f)
                    contentStream.addRect(
                        margin, yPosition + rowHeight,
                        pageWidth - (2 * margin), tableTop - yPosition
                    )
                    contentStream.stroke()

                    // Create and setup new page
                    page = PDPage()
                    document.addPage(page)
                    contentStream.close()

                    // Setup new content stream
                    val newContentStream = PDPageContentStream(
                        document,
                        page,
                        PDPageContentStream.AppendMode.OVERWRITE,
                        true
                    )

                    // Add continuation header with more spacing
                    newContentStream.beginText()
                    newContentStream.setFont(PDType1Font.HELVETICA_BOLD, 14f)
                    newContentStream.setNonStrokingColor(0, 0, 128)
                    newContentStream.newLineAtOffset(margin, pageHeight - margin)
                    newContentStream.showText("${sellsTitle} (Continued)")
                    newContentStream.endText()

                    // Reset table position with better spacing
                    yPosition = pageHeight - margin - 50

                    // Re-draw table header on new page
                    newContentStream.setNonStrokingColor(0, 0, 128)
                    newContentStream.addRect(margin, yPosition, pageWidth - (2 * margin), rowHeight)
                    newContentStream.fill()

                    // Table header text on new page
                    newContentStream.setNonStrokingColor(255, 255, 255)
                    xPosition = margin + 5

                    headers.forEachIndexed { index, header ->
                        newContentStream.beginText()
                        newContentStream.setFont(PDType1Font.HELVETICA_BOLD, 10f)
                        newContentStream.newLineAtOffset(xPosition, yPosition + 7)
                        newContentStream.showText(header)
                        newContentStream.endText()
                        xPosition += columnWidths[index]
                    }

                    newContentStream.setNonStrokingColor(0, 0, 0)
                    yPosition -= rowHeight
                    return@forEach
                    isAlternate = false
                }
            }

            // Draw final table border
            contentStream.setStrokingColor(0, 0, 128)
            contentStream.setLineWidth(0.5f)
            contentStream.addRect(
                margin, yPosition,
                pageWidth - (2 * margin), tableTop - yPosition
            )
            contentStream.stroke()

            // Add Summary footer
            val footerY = yPosition - 40
            contentStream.setNonStrokingColor(0, 0, 128)
            contentStream.beginText()
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 11f)
            contentStream.newLineAtOffset(margin, footerY)
            contentStream.showText("TOTALS:")
            contentStream.endText()

            contentStream.beginText()
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 11f)
            contentStream.newLineAtOffset(margin + 100, footerY)
            contentStream.showText("${"%,d".format(totalValue.toLong())} UGX")
            contentStream.endText()

            // Footer with timestamp and page info
            contentStream.setNonStrokingColor(100, 100, 100) // Gray for footer
            contentStream.beginText()
            contentStream.setFont(PDType1Font.HELVETICA, 8f)
            contentStream.newLineAtOffset(margin, margin)
            contentStream.showText("Generated on: ${currentDateTime.format(formatter)}")
            contentStream.endText()

            contentStream.beginText()
            contentStream.setFont(PDType1Font.TIMES_ITALIC, 8f)
            contentStream.newLineAtOffset(pageWidth - margin - 150, margin)
            contentStream.showText("$sellsTitle Report")
            contentStream.endText()
        }

        document.save(file)
    }
}

fun formatDateForPdfExport(timeString: String): String {
    return try {
        // Try parsing the input date string
        val inputFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
        val parsedDateTime = LocalDateTime.parse(timeString, inputFormatter)

        // Format to desired output
        val outputFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
        parsedDateTime.format(outputFormatter)
    } catch (e: Exception) {
        // Fallback to original string if parsing fails
        timeString
    }
}

// Platform-independent notification system
fun showExportSuccessNotification(file: File) {
    // Attempt system tray notification first
    if (SystemTray.isSupported()) {
        try {
            val systemTray = SystemTray.getSystemTray()
            val trayIcon = TrayIcon(
                Toolkit.getDefaultToolkit().getImage("path/to/success_icon.png"),
                "Export Successful"
            )
            trayIcon.isImageAutoSize = true
            systemTray.add(trayIcon)

            trayIcon.displayMessage(
                "Export Successful",
                "File exported to: ${file.absolutePath}",
                TrayIcon.MessageType.INFO
            )
        } catch (e: Exception) {
            // Fallback to Swing dialog if system tray fails
            JOptionPane.showMessageDialog(
                null,
                "Export Successful\nFile: ${file.absolutePath}",
                "Export Complete",
                JOptionPane.INFORMATION_MESSAGE
            )
        }
    } else {
        // Fallback to console and Swing dialog
        println("Export successful: ${file.absolutePath}")
        JOptionPane.showMessageDialog(
            null,
            "Export Successful\nFile: ${file.absolutePath}",
            "Export Complete",
            JOptionPane.INFORMATION_MESSAGE
        )
    }

    // Optional: Open the file location
    try {
        openFileLocation(file)
    } catch (e: Exception) {
        println("Could not open file location: ${e.message}")
    }
}

fun showExportErrorNotification(e: Exception) {
    // Log the full stack trace
    e.printStackTrace()

    // System tray notification for error
    if (SystemTray.isSupported()) {
        try {
            val systemTray = SystemTray.getSystemTray()
            val trayIcon = TrayIcon(
                Toolkit.getDefaultToolkit().getImage("path/to/error_icon.png"),
                "Export Failed"
            )
            trayIcon.isImageAutoSize = true
            systemTray.add(trayIcon)

            trayIcon.displayMessage(
                "Export Failed",
                e.localizedMessage ?: "Unknown error occurred",
                TrayIcon.MessageType.ERROR
            )
        } catch (trayException: Exception) {
            // Fallback to Swing dialog
            JOptionPane.showMessageDialog(
                null,
                "Export Failed\n${e.localizedMessage}",
                "Export Error",
                JOptionPane.ERROR_MESSAGE
            )
        }
    } else {
        // Fallback to console and Swing dialog
        println("Export failed: ${e.message}")
        JOptionPane.showMessageDialog(
            null,
            "Export Failed\n${e.localizedMessage}",
            "Export Error",
            JOptionPane.ERROR_MESSAGE
        )
    }
}

// Utility function to open file location
fun openFileLocation(file: File) {
    val os = System.getProperty("os.name").lowercase()

    when {
        os.contains("win") -> {
            // Windows
            ProcessBuilder("explorer.exe /select,\"${file.absolutePath}\"").start()
        }

        os.contains("mac") -> {
            // macOS
            ProcessBuilder("open -R \"${file.absolutePath}\"").start()
        }

        os.contains("nux") -> {
            // Linux (multiple methods)
            try {
                // Try xdg-open first
                ProcessBuilder("xdg-open \"${file.parent}\"").start()
            } catch (e: Exception) {
                try {
                    // Fallback to nautilus
                    ProcessBuilder("nautilus \"${file.parent}\"").start()
                } catch (e: Exception) {
                    println("Could not open file location on Linux")
                }
            }
        }

        else -> {
            println("Unsupported OS for opening file location")
        }
    }
}

// Enum for notification types
enum class NotificationType {
    SUCCESS, ERROR, WARNING
}

// Cross-platform export directory
fun getExportDirectory(): File {
    val userHome = System.getProperty("user.home")
    return File(userHome, "Documents/SalesReports")
}