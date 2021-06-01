package ch.icken.csvtoolkit.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.ButtonColors
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ButtonElevation
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxColors
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.ContentAlpha
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun CheckButton(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    elevation: ButtonElevation? = ButtonDefaults.elevation(),
    shape: Shape = MaterialTheme.shapes.small,
    border: BorderStroke? = null,
    buttonColors: ButtonColors = ButtonDefaults.buttonColors(),
    checkboxColors: CheckboxColors = CheckboxDefaults.colors(
        checkedColor = buttonColors.contentColor(enabled).value,
        uncheckedColor = buttonColors.contentColor(enabled).value.copy(alpha = .6f),
        checkmarkColor = buttonColors.backgroundColor(enabled).value,
        disabledColor = buttonColors.backgroundColor(enabled).value.copy(alpha = ContentAlpha.disabled),
        disabledIndeterminateColor = buttonColors.contentColor(enabled).value.copy(alpha = ContentAlpha.disabled)
    ),
    contentPadding: PaddingValues = PaddingValues(12.dp, 12.dp, 16.dp, 12.dp),
    content: @Composable () -> Unit
) {
    val contentColor by buttonColors.contentColor(enabled)

    Surface(
        modifier = modifier,
        shape = shape,
        color = buttonColors.backgroundColor(enabled).value,
        contentColor = contentColor.copy(alpha = 1f),
        border = border,
        elevation = elevation?.elevation(enabled, interactionSource)?.value ?: 0.dp,
        onClick = { onCheckedChange?.invoke(!checked) },
        enabled = enabled,
        role = Role.Button,
        interactionSource = interactionSource,
        indication = rememberRipple()
    ) {
        CompositionLocalProvider(LocalContentAlpha provides contentColor.alpha) {
            Row(
                modifier = Modifier
                    .defaultMinSize(96.dp, 48.dp)
                    .padding(contentPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = null,
                    colors = checkboxColors
                )
                Spacer(Modifier.width(8.dp))
                content()
            }
        }
    }
}