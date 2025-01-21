import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import navigation.NavControllers
import navigation.Route
import viewmodel.AuthViewModel
import viewmodel.MainViewModel
import viewmodel.StockViewModel

@Composable
fun MerchantCenterApp(
    mainViewModel: MainViewModel,
    authViewModel: AuthViewModel,
    stockViewModel: StockViewModel
) {
    Surface(
        modifier = Modifier.fillMaxSize().background(
            color = MaterialTheme.colorScheme.background
        )
    ) {
        NavControllers(
            startDestination = Route.Sales,
            mainViewModel = mainViewModel,
            authViewModel = authViewModel,
            stockViewModel = stockViewModel
        )
    }
}