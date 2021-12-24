package ch.icken.csvtoolkit.transform.condition

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.combinedClickable
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
fun ConditionItemView(
    context: Condition.Context,
    condition: Condition,
    onEditCondition: (Condition) -> Unit,
    modifier: Modifier = Modifier,
    stateContent: @Composable RowScope.() -> Unit = {
        DefaultConditionStateContent(
            context = context,
            condition = condition
        )
    }
) {
    if (condition is ConditionCustomItemView) {
        condition.CustomItemView(
            context = context,
            onEditCondition = onEditCondition,
            modifier = modifier
        )
    } else {
        ConditionDefaultItemView(
            context = context,
            condition = condition,
            onEditCondition = onEditCondition,
            modifier = modifier,
            stateContent = stateContent
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConditionDefaultItemView(
    context: Condition.Context,
    condition: Condition,
    onEditCondition: (Condition) -> Unit,
    modifier: Modifier,
    stateContent: @Composable RowScope.() -> Unit
) = Row(
    modifier = modifier
        .combinedClickable(
            enabled = context.allowChanges,
            onClick = { onEditCondition(condition) }
        )
        .fillMaxWidth()
        .padding(start = 16.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
) {
    Text(
        text = condition.description,
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
fun DefaultConditionStateContent(
    context: Condition.Context,
    condition: Condition
) {
    when {
        condition.isValid(context) -> ConditionValidIcon()
        else -> ConditionInvalidIcon(condition.invalidMessage)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConditionValidIcon() {
    TooltipArea(
        tooltip = { Tooltip("Condition is valid") }
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Condition is valid",
            tint = Color.Green
        )
    }
}
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConditionInvalidIcon(message: String) {
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

interface ConditionCustomItemView {
    @Composable
    fun CustomItemView(
        context: Condition.Context,
        onEditCondition: (Condition) -> Unit,
        modifier: Modifier
    )
}