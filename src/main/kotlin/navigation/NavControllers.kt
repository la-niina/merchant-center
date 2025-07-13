package navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import components.HeaderComponent
import components.ProductComponent
import components.ReportsComponent
import components.SalesComponent
import viewmodel.MainViewModel
import viewmodel.ProductViewModel
import java.time.LocalDate

@Composable
fun NavControllers(
    startDestination: Route = Route.Sales,
    mainViewModel: MainViewModel = MainViewModel(),
) {
    var currentRoute by remember { mutableStateOf(startDestination) }

    LaunchedEffect(Unit, currentRoute) {
        when (currentRoute) {
            Route.Sales -> mainViewModel.loadProducts()
            Route.Products -> { /* Products are loaded by ProductComponent */ }
            Route.Reports -> mainViewModel.loadAllProducts()
        }
    }

    LaunchedEffect(Unit, mainViewModel) {
        mainViewModel.loadProducts()
        mainViewModel.loadAllProducts()
        mainViewModel.loadProductsForDate(LocalDate.now().toString())
    }

    Surface {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            HeaderComponent(
                titleHeader = "Merchant center",
                currentRoute = currentRoute,
                mainViewModel = mainViewModel,
                onNavigate = { currentRoute = it }
            )
            when (currentRoute) {
                Route.Sales -> SalesComponent(
                    mainViewModel = mainViewModel,
                )

                Route.Products -> ProductComponent(
                    productViewModel = ProductViewModel()
                )

                Route.Reports -> ReportsComponent(
                    mainViewModel = mainViewModel
                )
            }
        }
    }
}