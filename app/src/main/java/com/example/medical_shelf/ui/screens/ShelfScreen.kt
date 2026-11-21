package com.example.medical_shelf.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.medical_shelf.R
import com.example.medical_shelf.data.local.AppDatabase
import com.example.medical_shelf.data.local.entity.BoxEntity
import com.example.medical_shelf.data.local.entity.BoxTemplate
import com.example.medical_shelf.data.local.entity.MedicineEntity
import com.example.medical_shelf.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShelfScreen(
    onNavigateToAddMed: (medId: Long?, boxId: Long?) -> Unit,
    onNavigateToChecklist: (Long) -> Unit,
    onOpenPaywall: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context) }
    val prefs = remember { UserPreferencesRepository(context) }

    val isPro by prefs.isProVersion.collectAsState(initial = false)
    val boxes by db.boxDao().getAllBoxes().collectAsState(initial = emptyList())

    var selectedBoxId by remember { mutableStateOf<Long?>(null) } // null = All
    var searchQuery by remember { mutableStateOf("") }
    var showNewBoxDialog by remember { mutableStateOf(false) }

    val medicinesFlow = remember(selectedBoxId, searchQuery) {
        when {
            searchQuery.isNotBlank() -> db.medicineDao().searchMedicines(searchQuery)
            selectedBoxId != null && selectedBoxId!! > 0 -> db.medicineDao().getMedicinesByBox(selectedBoxId!!)
            else -> db.medicineDao().getAllMedicines()
        }
    }
    val medicines by medicinesFlow.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.nav_shelf)) },
                actions = {
                    if (selectedBoxId != null && selectedBoxId!! > 0) {
                        IconButton(onClick = { onNavigateToChecklist(selectedBoxId!!) }) {
                            Icon(
                                imageVector = Icons.Default.Checklist,
                                contentDescription = stringResource(id = R.string.btn_checklist)
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Horizontal Carousel of Boxes
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    BoxChip(
                        name = stringResource(id = R.string.box_all_medicines),
                        isSelected = selectedBoxId == null,
                        isLocked = false,
                        onClick = { selectedBoxId = null }
                    )
                }

                items(boxes, key = { it.id }) { box ->
                    val boxTitle = if (box.template == BoxTemplate.CUSTOM && !box.customName.isNullOrBlank()) {
                        box.customName
                    } else {
                        stringResource(id = box.template.titleResId)
                    }

                    val isLocked = !isPro && box.template != BoxTemplate.HOME
                    BoxChip(
                        name = boxTitle,
                        isSelected = selectedBoxId == box.id,
                        isLocked = isLocked,
                        onClick = {
                            if (isLocked) {
                                onOpenPaywall()
                            } else {
                                selectedBoxId = box.id
                            }
                        }
                    )
                }

                item {
                    BoxChip(
                        name = stringResource(id = R.string.box_add_new),
                        isSelected = false,
                        isLocked = false,
                        onClick = {
                            if (isPro) {
                                showNewBoxDialog = true
                            } else {
                                onOpenPaywall()
                            }
                        }
                    )
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                placeholder = { Text(text = stringResource(id = R.string.search_hint)) },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            // Medicines List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(medicines, key = { it.id }) { medicine ->
                    MedicineCard(
                        medicine = medicine,
                        onDecrementQuantity = {
                            if (medicine.quantity > 0) {
                                scope.launch {
                                    db.medicineDao().updateMedicine(medicine.copy(quantity = medicine.quantity - 1))
                                }
                            }
                        },
                        onEdit = { onNavigateToAddMed(medicine.id, selectedBoxId) },
                        onDelete = {
                            scope.launch {
                                db.medicineDao().deleteMedicine(medicine)
                            }
                        }
                    )
                }
            }
        }
    }

    if (showNewBoxDialog) {
        var newBoxName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewBoxDialog = false },
            title = { Text(text = stringResource(id = R.string.dialog_new_box_title)) },
            text = {
                OutlinedTextField(
                    value = newBoxName,
                    onValueChange = { newBoxName = it },
                    label = { Text(text = stringResource(id = R.string.dialog_new_box_hint)) },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newBoxName.isNotBlank()) {
                            scope.launch {
                                val box = BoxEntity(
                                    template = BoxTemplate.CUSTOM,
                                    customName = newBoxName.trim(),
                                    isSystem = false
                                )
                                db.boxDao().insertBox(box)
                                showNewBoxDialog = false
                            }
                        }
                    }
                ) {
                    Text(text = stringResource(id = R.string.btn_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewBoxDialog = false }) {
                    Text(text = stringResource(id = R.string.btn_cancel))
                }
            }
        )
    }
}

@Composable
fun BoxChip(
    name: String,
    isSelected: Boolean,
    isLocked: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                color = textColor,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 14.sp
            )

            if (isLocked) {
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun MedicineCard(
    medicine: MedicineEntity,
    onDecrementQuantity: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    val effectiveStatus = remember(medicine) { getExpiryStatus(medicine) }
    val statusColor = when (effectiveStatus) {
        ExpiryStatus.EXPIRED -> Color(0xFFD32F2F)
        ExpiryStatus.EXPIRING_SOON -> Color(0xFFF57C00)
        ExpiryStatus.GOOD -> Color(0xFF388E3C)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Package Photo / Default Icon
            if (!medicine.photoPath.isNullOrBlank()) {
                AsyncImage(
                    model = Uri.parse(medicine.photoPath),
                    contentDescription = null,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MedicalServices,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = medicine.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (!medicine.comment.isNullOrBlank()) {
                    Text(
                        text = medicine.comment,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Expiry Badge
                if (!medicine.expirationDate.isNullOrBlank()) {
                    Text(
                        text = stringResource(id = R.string.effective_expiry, medicine.expirationDate),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }

                // Quantity & Decrement
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.quantity_left, medicine.quantity),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = onDecrementQuantity,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(imageVector = Icons.Default.MoreVert, contentDescription = null)
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(text = stringResource(id = R.string.btn_edit)) },
                        onClick = {
                            showMenu = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(text = stringResource(id = R.string.btn_delete)) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

enum class ExpiryStatus { EXPIRED, EXPIRING_SOON, GOOD }

fun getExpiryStatus(medicine: MedicineEntity): ExpiryStatus {
    val expTime = medicine.expirationTimestamp ?: return ExpiryStatus.GOOD
    val now = System.currentTimeMillis()
    val warningTime = now + (14L * 24 * 60 * 60 * 1000)

    return when {
        expTime < now -> ExpiryStatus.EXPIRED
        expTime <= warningTime -> ExpiryStatus.EXPIRING_SOON
        else -> ExpiryStatus.GOOD
    }
}