package components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import navigation.Route
import viewmodel.MainViewModel

@Composable
fun HeaderComponent(
    titleHeader: String = "MERCHANT CENTER",
    currentRoute: Route = Route.Sales,
    mainViewModel: MainViewModel,
    onNavigate: (Route) -> Unit,
) {
    // Use collectAsState(initial) to prevent UI jank during collection
    val currentTimeDate by mainViewModel.currentDateTime.collectAsState(
        initial = "Loading date and time..."
    )
    
    // Remember tab index with saveability for configuration changes
    var selectedTabIndex by rememberSaveable { mutableStateOf(0) }

    // Update tab selection when route changes
    LaunchedEffect(currentRoute) {
        selectedTabIndex = Route.entries.indexOf(currentRoute).takeIf { it >= 0 } ?: 0
    }

    Row(
        modifier = Modifier
            .padding(20.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = titleHeader, 
                fontSize = 23.sp, 
                lineHeight = 23.sp,
                fontWeight = FontWeight.Bold
            )
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.Transparent,
                indicator = { tabPositions ->
                    if (selectedTabIndex < tabPositions.size) {
                        TabRowDefaults.PrimaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex])
                        )
                    }
                },
                divider = {},
                tabs = {
                    Route.entries.forEachIndexed { index, route ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = {
                                selectedTabIndex = index
                                onNavigate(route)
                            },
                            text = {
                                Text(
                                    text = route.name,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        )
                    }
                }
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "DATE",
                fontWeight = FontWeight.Bold
            )
            Text(
                text = currentTimeDate,
                fontWeight = FontWeight.Light,
                fontSize = 12.sp,
                lineHeight = 12.sp
            )
        }
    }
}