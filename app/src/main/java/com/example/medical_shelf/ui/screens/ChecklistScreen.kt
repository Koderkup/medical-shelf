package com.example.medical_shelf.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.medical_shelf.R
import com.example.medical_shelf.data.local.AppDatabase
import com.example.medical_shelf.data.local.entity.BoxTemplate
import com.example.medical_shelf.data.local.entity.ChecklistItemEntity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistScreen(
    boxId: Long
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context) }

    val box by db.boxDao().getBoxById(boxId).collectAsState(initial = null)
    val checklistItems by db.checklistDao().getChecklistForBox(boxId).collectAsState(initial = emptyList())
    val homeMedicines by db.medicineDao().getMedicinesByBox(1L).collectAsState(initial = emptyList())

    var showAddItemDialog by remember { mutableStateOf(false) }

    val boxName = box?.let {
        if (it.template == BoxTemplate.CUSTOM && !it.customName.isNullOrBlank()) it.customName
        else stringResource(id = it.template.titleResId)
    } ?: ""

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.checklist_title, boxName)) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { showAddItemDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = stringResource(id = R.string.checklist_add_custom))
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(checklistItems, key = { it.id }) { item ->
                    val itemTitle = when {
                        !item.customTitle.isNullOrBlank() -> item.customTitle
                        !item.titleResKey.isNullOrBlank() -> {
                            val resId = context.resources.getIdentifier(item.titleResKey, "string", context.packageName)
                            if (resId != 0) context.getString(resId) else item.titleResKey
                        }
                        else -> ""
                    }

                    // Check if similar med exists in Home Pharmacy
                    val matchingHomeMed = homeMedicines.find {
                        it.name.contains(itemTitle, ignoreCase = true) || itemTitle.contains(it.name, ignoreCase = true)
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = item.isChecked,
                                onCheckedChange = { isChecked ->
                                    scope.launch {
                                        db.checklistDao().updateChecklistItem(item.copy(isChecked = isChecked))
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Column {
                                Text(
                                    text = itemTitle,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )

                                if (matchingHomeMed != null) {
                                    Text(
                                        text = stringResource(id = R.string.checklist_hint_in_home, matchingHomeMed.name),
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddItemDialog) {
        var newItemTitle by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddItemDialog = false },
            title = { Text(text = stringResource(id = R.string.checklist_add_custom)) },
            text = {
                OutlinedTextField(
                    value = newItemTitle,
                    onValueChange = { newItemTitle = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newItemTitle.isNotBlank()) {
                            scope.launch {
                                val item = ChecklistItemEntity(
                                    boxId = boxId,
                                    customTitle = newItemTitle.trim()
                                )
                                db.checklistDao().insertChecklistItem(item)
                                showAddItemDialog = false
                            }
                        }
                    }
                ) {
                    Text(text = stringResource(id = R.string.btn_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddItemDialog = false }) {
                    Text(text = stringResource(id = R.string.btn_cancel))
                }
            }
        )
    }
}