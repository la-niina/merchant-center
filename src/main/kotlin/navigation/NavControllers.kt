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
import viewmodel.AuthViewModel
import viewmodel.MainViewModel
import viewmodel.StockViewModel

@Composable
fun NavControllers(
    startDestination: Route = Route.Sales,
    mainViewModel: MainViewModel = MainViewModel(),
    authViewModel: AuthViewModel = AuthViewModel(),
    stockViewModel: StockViewModel = StockViewModel()
) {
    var currentRoute by remember { mutableStateOf(startDestination) }

    LaunchedEffect(Unit, currentRoute) {
        when (currentRoute) {
            Route.Sales -> mainViewModel.loadProducts()
            Route.Products -> stockViewModel.fetchProducts()
            Route.Reports -> mainViewModel.loadProducts()
        }
    }

    LaunchedEffect(Unit, mainViewModel) {
        mainViewModel.loadCurrentDateTime()
    }

    Surface {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            HeaderComponent(
                titleHeader = "Merchant center",
                currentRoute = currentRoute,
                onNavigate = { currentRoute = it }
            )
            when (currentRoute) {
                Route.Sales -> SalesComponent(
                    mainViewModel = mainViewModel,
                    stockViewModel = stockViewModel,
                )

                Route.Products -> ProductComponent(
                    authViewModel = authViewModel,
                    stockViewModel = stockViewModel,
                    on = {
                        currentRoute = Route.Sales
                    }
                )

                Route.Reports -> ReportsComponent(
                    mainViewModel = mainViewModel
                )
            }
        }
    }
}