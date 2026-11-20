package com.example.medical_shelf.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.medical_shelf.R
import com.example.medical_shelf.data.local.AppDatabase
import com.example.medical_shelf.data.local.entity.BoxEntity
import com.example.medical_shelf.data.local.entity.BoxTemplate
import com.example.medical_shelf.data.local.entity.MedicineEntity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoveCopyMedicineDialog(
    medicine: MedicineEntity,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context) }

    val boxes by db.boxDao().getAllBoxes().collectAsState(initial = emptyList())
    var selectedTargetBox by remember { mutableStateOf<BoxEntity?>(null) }
    var expandedMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.move_copy_title)) },
        text = {
            Column {
                ExposedDropdownMenuBox(
                    expanded = expandedMenu,
                    onExpandedChange = { expandedMenu = !expandedMenu }
                ) {
                    val boxTitle = selectedTargetBox?.let { box ->
                        if (box.template == BoxTemplate.CUSTOM && !box.customName.isNullOrBlank()) {
                            box.customName
                        } else {
                            stringResource(id = box.template.titleResId)
                        }
                    } ?: ""

                    OutlinedTextField(
                        value = boxTitle,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(text = stringResource(id = R.string.target_box_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMenu) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = expandedMenu,
                        onDismissRequest = { expandedMenu = false }
                    ) {
                        boxes.forEach { box ->
                            val itemTitle = if (box.template == BoxTemplate.CUSTOM && !box.customName.isNullOrBlank()) {
                                box.customName
                            } else {
                                stringResource(id = box.template.titleResId)
                            }
                            DropdownMenuItem(
                                text = { Text(text = itemTitle) },
                                onClick = {
                                    selectedTargetBox = box
                                    expandedMenu = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row {
                Button(
                    onClick = {
                        if (selectedTargetBox != null) {
                            scope.launch {
                                // Move
                                db.medicineDao().updateMedicine(medicine.copy(boxId = selectedTargetBox!!.id))
                                onDismiss()
                            }
                        }
                    }
                ) {
                    Text(text = stringResource(id = R.string.action_move))
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedButton(
                    onClick = {
                        if (selectedTargetBox != null) {
                            scope.launch {
                                // Copy
                                db.medicineDao().insertMedicine(medicine.copy(id = 0, boxId = selectedTargetBox!!.id))
                                onDismiss()
                            }
                        }
                    }
                ) {
                    Text(text = stringResource(id = R.string.action_copy))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.btn_cancel))
            }
        }
    )
}