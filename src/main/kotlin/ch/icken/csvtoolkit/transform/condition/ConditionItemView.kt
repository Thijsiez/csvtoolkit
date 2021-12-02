package ch.icken.csvtoolkit.transform.condition

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConditionItemView(
    condition: Condition,
    onEditCondition: (Condition) -> Unit,
    modifier: Modifier = Modifier
) = Row(
    modifier = modifier
        .combinedClickable(
            onClick = { onEditCondition(condition) }
        )
        .fillMaxWidth()
        .height(48.dp)
        .padding(start = 16.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
) {
    Text(
        text = condition.description,
        modifier = Modifier.weight(1f),
        style = MaterialTheme.typography.body1
    )
}