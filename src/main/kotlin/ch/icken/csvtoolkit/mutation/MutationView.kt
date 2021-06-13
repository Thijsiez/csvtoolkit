package ch.icken.csvtoolkit.mutation

import androidx.compose.foundation.BoxWithTooltip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ch.icken.csvtoolkit.ToolkitInstance
import ch.icken.csvtoolkit.ui.Tooltip

@Composable
fun MutationView(
    instance: ToolkitInstance,
    onAddMutation: (Mutation) -> Unit,
    onEditMutation: (Mutation) -> Unit
) = Column(
    modifier = Modifier.fillMaxWidth()
) {
    val atLeastTwoFiles = remember { derivedStateOf { instance.files.size >= 2 } }
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.height(56.dp)
            .fillMaxWidth()
            .padding(start = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Mutations",
            style = MaterialTheme.typography.h6
        )
        Box {
            IconButton(
                onClick = { expanded = true },
                enabled = atLeastTwoFiles.value
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Mutation"
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                Mutation.Type.values().forEach {
                    DropdownMenuItem(
                        onClick = {
                            val mutation = Mutation.create(it)
                            onAddMutation(mutation)
                            onEditMutation(mutation)
                            expanded = false
                        }
                    ) {
                        Text(it.uiName)
                    }
                }
            }
        }
    }
    LazyColumn {
        items(instance.mutations) {
            MutationItemView(
                instance = instance,
                mutation = it,
                onEditMutation = { onEditMutation(it) }
            )
        }
    }
}

@Composable
private fun MutationItemView(
    instance: ToolkitInstance,
    mutation: Mutation,
    onEditMutation: () -> Unit
) = Row(
    modifier = Modifier.height(48.dp)
        .fillMaxWidth()
        .padding(start = 16.dp, end = 12.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
) {
    var expanded by remember { mutableStateOf(false) }

    Text(
        text = mutation.description,
        style = MaterialTheme.typography.body1
    )
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            IconButton(
                onClick = { expanded = true }
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Open Mutation menu"
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    onClick = {
                        onEditMutation()
                        expanded = false
                    }
                ) {
                    Text("Edit")
                }
            }
        }
        if (mutation.isValid(instance)) {
            BoxWithTooltip(
                tooltip = { Tooltip("Mutation is valid") }
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Mutation is valid",
                    tint = Color.Green
                )
            }
        } else {
            BoxWithTooltip(
                //TODO show actual reason for being invalid
                tooltip = { Tooltip("Whoops...") }
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Mutation is invalid",
                    tint = Color.Red
                )
            }
        }
    }
}