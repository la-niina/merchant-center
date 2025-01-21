package presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Money
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ElevatedSuggestionChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SalesListing(
    modifier: Modifier = Modifier,
    productName: String,
    qty: String,
    time: String,
    price: String,
    onRemove: () -> Unit = {}
) {
    ElevatedCard(
        shape = RoundedCornerShape(20),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.secondary
        )
    ) {
        Column(
            modifier = modifier.padding(vertical = 10.dp, horizontal = 25.dp).fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.Start
                ) {
                    Text(text = productName)
                }
                Row(
                    modifier = Modifier.weight(0.5f),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = qty)
                }

                Row(
                    modifier = Modifier.weight(0.5f),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.CalendarToday, null)
                    Text(text = time)
                }
                Row(
                    modifier = Modifier.weight(0.5f),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ElevatedSuggestionChip(
                        onClick = {},
                        icon = {
                            Icon(Icons.Rounded.Money, null)
                        },
                        label = {
                            Text(text = price)
                        },
                        colors = SuggestionChipDefaults.elevatedSuggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        shape = RoundedCornerShape(100),
                        modifier = Modifier.padding(end = 5.dp)
                    )
                }

                Row(
                    modifier = Modifier.weight(0.5f),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onRemove != {}) {
                        IconButton(
                            onClick = onRemove,
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Icon(
                                Icons.Rounded.Remove, null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}