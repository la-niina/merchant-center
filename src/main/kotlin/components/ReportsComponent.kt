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
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
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
import viewmodel.SalesProducts
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
    fun filterProductsByPeriod(products: List<SalesProducts>): List<SalesProducts> {
        return products.filter { product ->
            try {
                val productDate = parseProductDate(product.time)
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
        mainViewModel.loadCurrentDateTime()
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
                    IconButton(
                        onClick = {
                            isExportDialogVisible = true
                        }
                    ) {
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
                                exportPreviewData.sumOf {
                                    it.price.toDoubleOrNull() ?: 0.0
                                }.let { "UGX ${"%,d".format(it.toLong())}" }
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
                                        text = product.qty,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Column(
                                    modifier = Modifier.weight(0.5f)
                                ) {
                                    Text(
                                        text = product.formattedPrice(),
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
                onExport = { format, fileName ->
                    coroutineScope.launch {
                        exportReport(
                            fileName = fileName,
                            period = selectedPeriod,
                            format = format,
                            startDate = startDate,
                            endDate = endDate,
                            products = exportPreviewData
                        )
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
fun exportToCsv(products: List<SalesProducts>, file: File) {
    try {
        file.bufferedWriter().use { out ->
            // Write CSV header with more detailed columns
            out.write("Product ID,Product Name,Quantity,Price,Date,Total Value")
            out.newLine()

            // Write product data
            products.forEach { product ->
                val totalValue = (product.qty.toIntOrNull() ?: 0) *
                        (product.price.replace(",", "").toDoubleOrNull() ?: 0.0)

                out.write(
                    "${product.pid}," +
                            "${product.productName.replace(",", ";")}," +
                            "${product.qty}," +
                            "${product.price}," +
                            "${product.time}," +
                            "%,.2f".format(totalValue)
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
fun exportToExcel(products: List<SalesProducts>, file: File) {
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
                "Total Value",
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
                val unitPrice = product.price.replace(",", "").toDoubleOrNull() ?: 0.0
                row.createCell(3).apply {
                    setCellValue(unitPrice)
                    cellStyle = currencyStyle
                }

                // Total Value
                val totalValue = (product.qty.toIntOrNull() ?: 0) * unitPrice
                row.createCell(4).apply {
                    setCellValue(totalValue)
                    cellStyle = currencyStyle
                }

                // Date
                row.createCell(5).apply {
                    setCellValue(product.time)
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
    previewData: List<SalesProducts>,
    onDismiss: () -> Unit,
    onExport: (ExportFormat, String) -> Unit
) {
    var fileName by rememberSaveable { mutableStateOf("") }
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
                        previewData.sumOf {
                            it.price.toDoubleOrNull() ?: 0.0
                        }.let { "UGX ${"%,d".format(it.toLong())}" }
                    }",
                    style = MaterialTheme.typography.bodyMedium
                )
                EditorTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = fileName,
                    onValueChange = {
                        fileName = it
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                ExportFormat.entries.forEach { format ->
                    Button(
                        onClick = {
                            if (fileName.isNotBlank()) {
                                onExport(format, fileName)
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
    period: ReportPeriod,
    format: ExportFormat,
    startDate: LocalDate = LocalDate.now().minusWeeks(1),
    endDate: LocalDate = LocalDate.now(),
    products: List<SalesProducts>
) {
    val currentDate = LocalDate.now()

    // Filter products based on the selected period
    val filteredProducts = products.filter { product ->
        try {
            val productDate = parseProductDate(product.time)
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
    val filename = if (fileName.isNotEmpty()) {
        "Sales_Report_${period.name}_${currentDate.format(formatter)}"
    } else {
        "${fileName}_${currentDate.format(formatter)}"
    }

    // Determine export directory (cross-platform)
    val exportDir = getExportDirectory()

    // Ensure export directory exists
    exportDir.mkdirs()

    // Export based on selected format
    val exportFile = File(exportDir, "$filename.${format.name.lowercase()}")

    // Wrap export in a try-catch to handle potential errors
    try {
        when (format) {
            ExportFormat.PDF -> exportToPdf(filteredProducts, exportFile)
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

fun exportToPdf(products: List<SalesProducts>, file: File) {
    require(products.isNotEmpty()) { "No products to export" }
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

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

            // Title
            contentStream.beginText()
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 16f)
            contentStream.newLineAtOffset(margin, pageHeight - margin)
            contentStream.showText("Sales Report")
            contentStream.endText()

            // Totals Summary
            val totalProducts = products.size
            val totalQuantity = products.sumOf { it.qty.toIntOrNull() ?: 0 }
            val totalValue = products.sumOf {
                (it.price.replace(",", "").toDoubleOrNull() ?: 0.0)
            }

            contentStream.beginText()
            contentStream.setFont(PDType1Font.HELVETICA, 12f)
            contentStream.newLineAtOffset(margin, pageHeight - margin - 30f)
            contentStream.showText("Total Products: $totalProducts")
            contentStream.newLineAtOffset(0f, -20f)
            contentStream.showText("Sold Items: $totalQuantity")
            contentStream.newLineAtOffset(0f, -20f)
            contentStream.showText("Total Value: ${"%,d".format(totalValue.toLong())} UGX")
            contentStream.endText()

            // Table Header
            val headers = arrayOf("Product Name", "Quantity", "Unit Price", "Total Price", "Date")
            var yPosition = pageHeight - margin - 120f

            // Draw table header
            contentStream.setStrokingColor(
                java.awt.Color.getColor("Gray", Color.LightGray.toArgb())
            )
            contentStream.setLineWidth(0.5f)

            headers.forEachIndexed { index, header ->
                contentStream.beginText()
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 10f)
                contentStream.newLineAtOffset(margin + (index * 110f), yPosition)
                contentStream.showText(header)
                contentStream.endText()
            }

            // Draw header line
            contentStream.moveTo(margin, yPosition - 5f)
            contentStream.lineTo(pageWidth - margin, yPosition - 5f)
            contentStream.stroke()

            // Product Data
            yPosition -= 20f
            products.forEach { product ->
                // Calculate total price for this product
                val unitPrice = product.price.replace(",", "").toDoubleOrNull() ?: 0.0
                val perItem =
                    unitPrice / (product.qty.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0)

                contentStream.beginText()
                contentStream.setFont(PDType1Font.HELVETICA, 10f)
                contentStream.newLineAtOffset(margin, yPosition)
                contentStream.showText(product.productName)
                contentStream.newLineAtOffset(110f, 0f)
                contentStream.showText(product.qty)
                contentStream.newLineAtOffset(110f, 0f)
                contentStream.showText("${"%,d".format(perItem.toLong())} UGX")
                contentStream.newLineAtOffset(110f, 0f)
                contentStream.showText("${"%,d".format(unitPrice.toLong())} UGX")
                contentStream.newLineAtOffset(110f, 0f)
                contentStream.showText(formatDateForPdfExport(product.time))
                contentStream.endText()

                yPosition -= 20f

                // Add a new page if running out of space
                if (yPosition < margin) {
                    page = PDPage()
                    document.addPage(page)
                    yPosition = pageHeight - margin
                }
            }

            // Footer with timestamp
            val currentDateTime = LocalDateTime.now()

            contentStream.beginText()
            contentStream.setFont(PDType1Font.HELVETICA, 8f)
            contentStream.newLineAtOffset(margin, margin)
            contentStream.showText("Generated on: ${currentDateTime.format(formatter)}")
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