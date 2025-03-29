package presentation

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EditorTextField(
    modifier: Modifier = Modifier,
    value: String,
    placeHolder: String,
    default: Boolean = true,
    onValueChange: (String) -> Unit
) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(20),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.secondary
        )
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    placeHolder,
                    style = smallTextStyle()
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Rounded.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSecondary
                )
            },
            textStyle = smallTextStyle(),
            trailingIcon = {
                if (default) {
                    if (value.isNotBlank()) {
                        IconButton(onClick = { onValueChange("") }) {
                            Icon(
                                Icons.Rounded.Remove,
                                contentDescription = "Clear",
                                tint = MaterialTheme.colorScheme.onSecondary
                            )
                        }
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSecondary,
                unfocusedTextColor = MaterialTheme.colorScheme.onSecondary,
                disabledTextColor = MaterialTheme.colorScheme.onSecondary,
                errorTextColor = MaterialTheme.colorScheme.error,
                errorSupportingTextColor = MaterialTheme.colorScheme.error,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                disabledBorderColor = Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.onSecondary
            ),
            modifier = modifier
                .height(48.dp)
        )
    }
}


@Composable
fun smallTextStyle() = TextStyle(
    fontSize = 11.sp,
    lineHeight = 11.sp,
    fontWeight = MaterialTheme.typography.labelSmall.fontWeight,
    color = MaterialTheme.colorScheme.onSecondary,
)