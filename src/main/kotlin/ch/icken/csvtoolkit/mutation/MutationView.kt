package ch.icken.csvtoolkit.mutation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ch.icken.csvtoolkit.ToolkitInstance

@Composable
fun MutationView(
    instance: ToolkitInstance,
    onAddMutation: () -> Unit
) = Column(
    modifier = Modifier.fillMaxWidth()
) {
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
        IconButton(
            onClick = onAddMutation
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Mutation"
            )
        }
    }
    LazyColumn {
        items(instance.mutations) {
            MutationItemView(it)
        }
    }
}

@Composable
private fun MutationItemView(mutation: Mutation) = Row(
    modifier = Modifier.height(48.dp)
        .fillMaxWidth()
        .padding(start = 16.dp, end = 12.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
) {
    Text(
        text = mutation.description,
        style = MaterialTheme.typography.body1
    )
}