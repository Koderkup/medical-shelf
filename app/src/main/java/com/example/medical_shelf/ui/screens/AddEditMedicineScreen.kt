package com.example.medical_shelf.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.medical_shelf.R
import com.example.medical_shelf.data.local.AppDatabase
import com.example.medical_shelf.data.local.entity.BoxEntity
import com.example.medical_shelf.data.local.entity.BoxTemplate
import com.example.medical_shelf.data.local.entity.MedicineEntity
import com.example.medical_shelf.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditMedicineScreen(
    medicineId: Long? = null,
    defaultBoxId: Long? = null,
    scannedBarcodeName: String? = null,
    scannedDate: String? = null,
    onNavigateToScanner: (mode: String) -> Unit,
    onOpenPaywall: () -> Unit,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context) }
    val prefs = remember { UserPreferencesRepository(context) }

    val isPro by prefs.isProVersion.collectAsState(initial = false)
    val boxes by db.boxDao().getAllBoxes().collectAsState(initial = emptyList())
    val medicineCount by db.medicineDao().getMedicineCount().collectAsState(initial = 0)

    var name by remember { mutableStateOf("") }
    var barcode by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }
    var selectedBox by remember { mutableStateOf<BoxEntity?>(null) }
    var quantity by remember { mutableIntStateOf(1) }
    var dosage by remember { mutableStateOf("") }
    var expirationDate by remember { mutableStateOf("") }
    var photoUriString by remember { mutableStateOf<String?>(null) }

    var expandedBoxMenu by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            photoUriString = uri.toString()
        }
    }

    LaunchedEffect(scannedBarcodeName) {
        if (!scannedBarcodeName.isNullOrBlank()) {
            // Check if scannedBarcodeName is pure digits (barcode) or medicine name
            if (scannedBarcodeName.all { it.isDigit() }) {
                barcode = scannedBarcodeName
                // Lookup if dictionary/cache has a name for this barcode
                val cached = db.userBarcodeCacheDao().findNameByBarcode(scannedBarcodeName)
                if (cached != null) {
                    name = cached
                }
            } else {
                name = scannedBarcodeName
            }
        }
    }

    LaunchedEffect(scannedDate) {
        if (!scannedDate.isNullOrBlank()) {
            expirationDate = scannedDate
        }
    }

    LaunchedEffect(medicineId) {
        if (medicineId != null && medicineId > 0) {
            val existing = db.medicineDao().getMedicineByIdSync(medicineId)
            if (existing != null) {
                name = existing.name
                barcode = existing.barcode ?: ""
                comment = existing.comment ?: ""
                quantity = existing.quantity
                dosage = existing.dosage ?: ""
                expirationDate = existing.expirationDate ?: ""
                photoUriString = existing.photoPath
                selectedBox = boxes.find { it.id == existing.boxId }
            }
        }
    }

    LaunchedEffect(boxes, defaultBoxId) {
        if (selectedBox == null && boxes.isNotEmpty()) {
            selectedBox = if (defaultBoxId != null && defaultBoxId > 0) {
                boxes.find { it.id == defaultBoxId } ?: boxes.first()
            } else {
                boxes.find { it.template == BoxTemplate.HOME } ?: boxes.first()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (medicineId != null && medicineId > 0)
                            stringResource(id = R.string.edit_med_title)
                        else
                            stringResource(id = R.string.add_med_title)
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Scanner Launch Buttons (Height 68dp for two-line text comfort)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { onNavigateToScanner("barcode") },
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 68.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = stringResource(id = R.string.scanner_mode_barcode), fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = { onNavigateToScanner("ocr") },
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 68.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = stringResource(id = R.string.scanner_mode_ocr), fontSize = 12.sp)
                }
            }

            // Name (Mandatory)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(text = stringResource(id = R.string.field_name_label)) },
                placeholder = { Text(text = stringResource(id = R.string.field_name_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Optional Barcode field
            OutlinedTextField(
                value = barcode,
                onValueChange = { barcode = it },
                label = { Text(text = "Штрих-код (необязательно)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Comment / Purpose
            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it },
                label = { Text(text = stringResource(id = R.string.field_comment_label)) },
                placeholder = { Text(text = stringResource(id = R.string.field_comment_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Box Dropdown Selector
            ExposedDropdownMenuBox(
                expanded = expandedBoxMenu,
                onExpandedChange = { expandedBoxMenu = !expandedBoxMenu }
            ) {
                val boxTitle = selectedBox?.let { box ->
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
                    label = { Text(text = stringResource(id = R.string.field_box_label)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedBoxMenu) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(12.dp)
                )

                ExposedDropdownMenu(
                    expanded = expandedBoxMenu,
                    onDismissRequest = { expandedBoxMenu = false }
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
                                selectedBox = box
                                expandedBoxMenu = false
                            }
                        )
                    }
                }
            }

            // Dosage & Quantity
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = dosage,
                    onValueChange = { dosage = it },
                    label = { Text(text = stringResource(id = R.string.field_dosage_label)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = quantity.toString(),
                    onValueChange = { quantity = it.toIntOrNull() ?: 1 },
                    label = { Text(text = stringResource(id = R.string.field_quantity_label)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Expiration Date
            OutlinedTextField(
                value = expirationDate,
                onValueChange = { expirationDate = it },
                label = { Text(text = stringResource(id = R.string.expiration_date)) },
                placeholder = { Text(text = "MM.YYYY") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Save Button with Freemium check
            Button(
                onClick = {
                    if (name.isNotBlank() && selectedBox != null) {
                        // Check Freemium limit: max 8 medicines for Free users
                        if (!isPro && medicineId == null && medicineCount >= 8) {
                            onOpenPaywall()
                        } else {
                            scope.launch {
                                val medicine = MedicineEntity(
                                    id = medicineId ?: 0,
                                    boxId = selectedBox!!.id,
                                    name = name.trim(),
                                    comment = comment.trim().ifBlank { null },
                                    quantity = quantity,
                                    dosage = dosage.trim().ifBlank { null },
                                    expirationDate = expirationDate.trim().ifBlank { null },
                                    barcode = barcode.trim().ifBlank { null },
                                    photoPath = photoUriString
                                )
                                db.medicineDao().insertMedicine(medicine)

                                // If user typed a barcode and name, save to cache
                                if (barcode.isNotBlank() && name.isNotBlank()) {
                                    db.userBarcodeCacheDao().insertOrReplace(
                                        com.example.medical_shelf.data.local.entity.UserBarcodeCacheEntity(
                                            barcode = barcode.trim(),
                                            medicineName = name.trim()
                                        )
                                    )
                                }

                                onDone()
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.btn_save),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}