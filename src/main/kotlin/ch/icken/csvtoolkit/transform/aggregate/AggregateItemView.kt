package ch.icken.csvtoolkit.transform.aggregate

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ch.icken.csvtoolkit.ui.Tooltip

@Composable
fun AggregateItemView(
    context: Aggregate.Context,
    aggregate: Aggregate,
    onEditAggregate: (Aggregate) -> Unit,
    modifier: Modifier = Modifier,
    stateContent: @Composable RowScope.() -> Unit = {
        DefaultAggregateStateContent(
            context = context,
            aggregate = aggregate
        )
    }
) = Row(
    modifier = modifier
        .clickable(
            enabled = context.allowChanges,
            onClick = { onEditAggregate(aggregate) }
        )
        .fillMaxWidth()
        .padding(start = 16.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
) {
    Text(
        text = aggregate.description,
        modifier = Modifier.weight(1f)
            .padding(vertical = 8.dp),
        style = MaterialTheme.typography.body1
    )
    Row(
        modifier = Modifier.requiredHeight(48.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = stateContent
    )
}

@Composable
fun DefaultAggregateStateContent(
    context: Aggregate.Context,
    aggregate: Aggregate
) {
    when {
        aggregate.isValid(context) -> AggregateValidIcon()
        else -> AggregateInvalidIcon(aggregate.invalidMessage)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AggregateValidIcon() {
    TooltipArea(
        tooltip = { Tooltip("Aggregate is valid") }
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Aggregate is valid",
            tint = Color.Green
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AggregateInvalidIcon(message: String) {
    TooltipArea(
        tooltip = { Tooltip(message) }
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Condition is invalid",
            tint = Color.Red
        )
    }
}