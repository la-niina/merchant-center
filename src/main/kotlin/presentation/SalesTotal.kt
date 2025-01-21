package presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun SalesTotal(
    modifier: Modifier = Modifier,
    total: String = "UGX 0"
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = modifier.padding(20.dp).height(80.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.Start
            ) {
                Text(
                    text = "TOTAL :",
                    fontSize = 23.sp,
                    lineHeight = 23.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 10.dp)
                )
            }

            Row(
                modifier = Modifier.weight(0.5f), horizontalArrangement = Arrangement.Start
            ) {}

            Row(
                modifier = Modifier.weight(0.5f), horizontalArrangement = Arrangement.Start
            ) {}

            Row(
                modifier = Modifier.weight(0.5f), horizontalArrangement = Arrangement.Start
            ) {
                Text(
                    text = total,
                    fontSize = 23.sp,
                    lineHeight = 23.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.padding(end = 10.dp)
                )
            }

            Row(
                modifier = Modifier.weight(0.5f), horizontalArrangement = Arrangement.Start
            ) {}
        }
    }
}