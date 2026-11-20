package com.example.medical_shelf.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.medical_shelf.R
import com.example.medical_shelf.alarm.AlarmReceiver
import com.example.medical_shelf.data.local.AppDatabase
import com.example.medical_shelf.data.local.entity.IntakeLogEntity
import com.example.medical_shelf.data.local.entity.IntakeTimeSection
import com.example.medical_shelf.data.local.entity.ScheduleEntity
import com.example.medical_shelf.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntakeScreen(
    onOpenPaywall: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context) }
    val prefs = remember { UserPreferencesRepository(context) }

    val isPro by prefs.isProVersion.collectAsState(initial = false)
    val schedules by db.scheduleDao().getEnabledSchedules().collectAsState(initial = emptyList())

    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    val logs by db.intakeLogDao().getLogsForDate(todayStr).collectAsState(initial = emptyList())

    var showAddScheduleDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.nav_intake)) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddScheduleDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val sections = listOf(
                IntakeTimeSection.MORNING,
                IntakeTimeSection.AFTERNOON,
                IntakeTimeSection.EVENING,
                IntakeTimeSection.NIGHT
            )

            sections.forEach { section ->
                val sectionSchedules = schedules.filter { it.section == section }
                if (sectionSchedules.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(id = section.titleResId),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }

                    items(sectionSchedules, key = { it.id }) { schedule ->
                        val log = logs.find { it.scheduleId == schedule.id }
                        IntakeCard(
                            schedule = schedule,
                            log = log,
                            isPro = isPro,
                            onTookIt = { foodMins ->
                                val takeIntent = Intent(context, AlarmReceiver::class.java).apply {
                                    action = AlarmReceiver.ACTION_TAKE_MEDICINE
                                    putExtra(AlarmReceiver.EXTRA_SCHEDULE_ID, schedule.id)
                                    putExtra(AlarmReceiver.EXTRA_MEDICINE_ID, schedule.medicineId)
                                    putExtra(AlarmReceiver.EXTRA_MEDICINE_NAME, schedule.medicineName)
                                    putExtra(AlarmReceiver.EXTRA_BEFORE_FOOD_MINS, foodMins)
                                }
                                context.sendBroadcast(takeIntent)
                            },
                            onUndo = {
                                if (log != null) {
                                    scope.launch {
                                        db.intakeLogDao().deleteLog(log)
                                    }
                                }
                            },
                            onOpenPaywall = onOpenPaywall
                        )
                    }
                }
            }

            if (schedules.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 60.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Text(
                            text = stringResource(id = R.string.nav_intake),
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun IntakeCard(
    schedule: ScheduleEntity,
    log: IntakeLogEntity?,
    isPro: Boolean,
    onTookIt: (beforeFoodMins: Int) -> Unit,
    onUndo: () -> Unit,
    onOpenPaywall: () -> Unit
) {
    val isTaken = log != null
    val containerColor = if (isTaken) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = schedule.medicineName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!schedule.dosage.isNullOrBlank()) {
                        Text(
                            text = schedule.dosage,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = String.format(Locale.getDefault(), "%02d:%02d", schedule.timeHour, schedule.timeMinute),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (isTaken) {
                val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(log.intakeTimestamp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(id = R.string.took_today_at, timeStr),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF2E7D32)
                        )
                    }

                    TextButton(onClick = onUndo) {
                        Text(text = stringResource(id = R.string.btn_undo), color = MaterialTheme.colorScheme.error)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { onTookIt(0) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2E7D32),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = stringResource(id = R.string.btn_took_it), fontWeight = FontWeight.Bold)
                    }

                    // Before-food timer button (Pro)
                    OutlinedButton(
                        onClick = {
                            if (isPro) {
                                onTookIt(30) // 30 min before food
                            } else {
                                onOpenPaywall()
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = if (isPro) Icons.Default.Timer else Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (isPro) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "30m")
                    }
                }
            }
        }
    }
}