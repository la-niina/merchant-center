# Merchant Center

A comprehensive shop management system built with Kotlin and Compose Multiplatform for desktop applications. Merchant Center provides robust sales tracking, product catalog management, and detailed reporting capabilities.

## 🚀 Features

### 📊 Sales Management
- **Real-time Sales Tracking**: Record and monitor sales with automatic timestamping
- **Product Integration**: Seamlessly link sales to product catalog entries
- **Stock Management**: Automatic stock decrement on sales with validation
- **Search & Filter**: Advanced search functionality across product names, prices, and quantities
- **Date-based Filtering**: View sales by specific dates (Today, Yesterday, custom ranges)

### 🛍️ Product Catalog
- **Complete CRUD Operations**: Add, edit, view, and soft-delete products
- **Stock Tracking**: Monitor inventory levels with low stock alerts
- **Product Categories**: Organize products by categories for better management
- **Unique Product Numbers**: Ensure product identification with unique numbering system
- **Price Management**: Set and update unit prices with proper formatting

### 📈 Reporting & Analytics
- **Sales Reports**: Generate comprehensive sales reports in PDF format
- **Revenue Analytics**: Track total revenue with detailed breakdowns
- **Product Performance**: Analyze product sales performance
- **Date Range Reports**: Customizable reporting periods
- **Export Capabilities**: Export data in multiple formats

### 🎯 User Experience
- **Modern UI**: Material Design 3 with intuitive navigation
- **Responsive Design**: Optimized for desktop applications
- **Real-time Updates**: Live data updates with state management
- **Error Handling**: Comprehensive error handling with user-friendly messages
- **Search Functionality**: Advanced search across all data types

## 🛠️ Technology Stack

- **Language**: Kotlin 2.0.21
- **UI Framework**: Compose Multiplatform
- **Database**: SQLite with SQLDelight for type-safe queries
- **Architecture**: MVVM with StateFlow for reactive UI
- **Build System**: Gradle with Kotlin DSL
- **Platform**: Desktop (Windows, macOS, Linux)

## 📋 Prerequisites

- **Java**: OpenJDK 21 or higher
- **Gradle**: 8.0 or higher (included in wrapper)
- **Operating System**: Windows, macOS, or Linux

## 🚀 Installation

### 1. Clone the Repository
```bash
git clone https://github.com/la-niina/merchant-center.git
cd merchant-center
```

### 2. Build the Application
```bash
# For Windows
./gradlew packageReleaseMsi

# For macOS/Linux
./gradlew build
```

### 3. Run the Application
```bash
./gradlew run
```

## 📖 Usage Guide

### Sales Management

1. **Adding Sales**:
   - Navigate to the Sales tab
   - Click "Add Sale" button
   - Search for products by name or number
   - Enter quantity and verify stock availability
   - Complete the sale (stock automatically decrements)

2. **Viewing Sales**:
   - Use the search bar to filter sales by product name, price, or quantity
   - Switch between "Today" and "Yesterday" for quick date filtering
   - View total sales at the bottom of the list

3. **Managing Sales**:
   - Remove individual sales with the delete button
   - Use "Delete All" to clear all sales (use with caution)
   - Refresh data to get the latest information

### Product Catalog Management

1. **Adding Products**:
   - Navigate to the Products tab
   - Click "Add Product" button
   - Fill in product details:
     - Product Number (unique identifier)
     - Product Name
     - Unit Price (in UGX)
     - Description (optional)
     - Category (optional)
     - Initial Stock Quantity

2. **Managing Products**:
   - Search products by name or number
   - Edit product details with the edit button
   - Soft-delete products (they remain in database but are hidden)
   - Monitor stock levels with color-coded indicators

3. **Stock Management**:
   - Stock automatically decrements when sales are made
   - Low stock warnings appear when quantity is ≤ 10
   - Out of stock indicators for zero quantity items

### Reports & Analytics

1. **Generating Reports**:
   - Navigate to the Reports tab
   - Select date range for the report
   - Choose report type (Sales, Products, Revenue)
   - Export to PDF format

2. **Viewing Analytics**:
   - Total revenue calculations
   - Product performance metrics
   - Sales trends and patterns
   - Category-wise analysis

## 🏗️ Architecture

### Project Structure
```
src/main/kotlin/
├── components/           # UI Components
│   ├── HeaderComponent.kt
│   ├── ProductComponent.kt
│   ├── ReportsComponent.kt
│   └── SalesComponent.kt
├── core/                # Core utilities
│   ├── database/
│   │   └── DatabaseMigration.kt
│   └── validation/
│       └── DataValidator.kt
├── domain/              # Domain models
│   └── model/
│       ├── Product.kt
│       └── SalesProduct.kt
├── navigation/          # Navigation logic
│   ├── NavControllers.kt
│   └── NavigationRoute.kt
├── presentation/        # UI components
│   ├── components/
│   │   └── ConfirmationDialog.kt
│   ├── SearchTextField.kt
│   └── [other UI files]
├── viewmodel/          # Business logic
│   ├── MainViewModel.kt
│   ├── ProductViewModel.kt
│   └── [other ViewModels]
└── Main.kt            # Application entry point
```

### Database Schema

#### SalesProducts Table
```sql
CREATE TABLE SalesProducts (
  pid INTEGER PRIMARY KEY AUTOINCREMENT,
  productName TEXT NOT NULL,
  qty INTEGER NOT NULL,
  unitPrice TEXT NOT NULL,
  totalPrice TEXT NOT NULL,
  time TEXT NOT NULL,
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL
);
```

#### Products Table
```sql
CREATE TABLE Products (
  productId INTEGER PRIMARY KEY AUTOINCREMENT,
  productNumber TEXT UNIQUE NOT NULL,
  productName TEXT NOT NULL,
  unitPrice TEXT NOT NULL,
  description TEXT,
  category TEXT,
  stockQuantity INTEGER DEFAULT 0,
  isActive INTEGER DEFAULT 1,
  created_at TEXT DEFAULT CURRENT_TIMESTAMP,
  updated_at TEXT DEFAULT CURRENT_TIMESTAMP
);
```

## 🔧 Configuration

### Database Settings
The application uses SQLite with optimized settings:
- Write-Ahead Logging (WAL) for better concurrency
- Optimized cache size (50MB)
- Memory-mapped I/O for better performance

### Build Configuration
- **Java Version**: 21
- **Kotlin Version**: 2.0.21
- **Compose Version**: Latest stable
- **Target Platforms**: Desktop (Windows, macOS, Linux)

## 🧪 Testing

### Running Tests
```bash
# Run all tests
./gradlew test

# Run specific test category
./gradlew test --tests "*SalesTest*"
```

### Test Coverage
- Unit tests for ViewModels
- Integration tests for database operations
- UI tests for critical workflows

## 🚀 Deployment

### Creating Distribution
```bash
# Windows MSI installer
./gradlew packageReleaseMsi

# Linux DEB package
./gradlew packageReleaseDeb

# macOS DMG
./gradlew packageReleaseDmg
```

### Distribution Files
- **Windows**: `build/compose/binaries/main/release/msi/`
- **Linux**: `build/compose/binaries/main/release/deb/`
- **macOS**: `build/compose/binaries/main/release/dmg/`

## 🐛 Troubleshooting

### Common Issues

1. **Database Migration Errors**:
   - Delete `sales_products.db` file
   - Restart the application
   - Database will be recreated automatically

2. **Build Failures**:
   - Ensure Java 21 is installed and set as default
   - Run `./gradlew clean` before building
   - Check Gradle wrapper version

3. **Performance Issues**:
   - Close other applications to free memory
   - Restart the application if it becomes slow
   - Check database file size (should be < 100MB)

### Logs and Debugging
- Application logs are printed to console
- Database operations are logged with timestamps
- Error messages include detailed context

## 🤝 Contributing

We welcome contributions! Please see our [Contributing Guidelines](CONTRIBUTING.md) for details.

### Development Setup
1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- **JetBrains** for Compose Multiplatform
- **Cash App** for SQLDelight
- **Material Design** for UI guidelines
- **Kotlin Team** for the amazing language

## 📞 Support

For support and questions:
- Create an issue on GitHub
- Check the [troubleshooting guide](troubleshooting.bash)
- Review the [improvements plan](IMPROVEMENTS.md)

---

**Version**: 1.1.7  
**Last Updated**: January 2025  
**Maintainer**: @damianochintala
