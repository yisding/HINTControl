package dev.zwander.common.pages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.icerock.moko.resources.compose.stringResource
import dev.zwander.common.components.HybridElevatedCard
import dev.zwander.common.components.InfoRow
import dev.zwander.common.data.SavedReading
import dev.zwander.common.database.getRoomDatabase
import dev.zwander.compose.alertdialog.InWindowAlertDialog
import dev.zwander.resources.common.MR
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun ReadingsHistoryPage(
    modifier: Modifier = Modifier,
) {
    val database = remember { getRoomDatabase() }
    val savedReadingsFlow = remember { database.getSavedReadingDao().getAll() }
    val savedReadings by savedReadingsFlow.collectAsState(initial = emptyList())
    
    var selectedReading by remember { mutableStateOf<SavedReading?>(null) }
    var readingToDelete by remember { mutableStateOf<SavedReading?>(null) }
    val scope = rememberCoroutineScope()
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(
            text = stringResource(MR.strings.readings_history),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp),
        )
        
        if (savedReadings.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(MR.strings.no_saved_readings),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(savedReadings) { reading ->
                    SavedReadingCard(
                        reading = reading,
                        onViewDetails = { selectedReading = reading },
                        onDelete = { readingToDelete = reading },
                    )
                }
            }
        }
    }
    
    // Details Dialog
    selectedReading?.let { reading ->
        ReadingDetailsDialog(
            reading = reading,
            onDismiss = { selectedReading = null },
        )
    }
    
    // Delete Confirmation Dialog
    readingToDelete?.let { reading ->
        InWindowAlertDialog(
            onDismissRequest = { readingToDelete = null },
            title = {
                Text(stringResource(MR.strings.delete_reading))
            },
            text = {
                Text(stringResource(MR.strings.delete_reading_confirmation))
            },
            buttons = {
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    TextButton(
                        onClick = { readingToDelete = null },
                    ) {
                        Text(stringResource(MR.strings.cancel))
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    TextButton(
                        onClick = {
                            scope.launch {
                                database.getSavedReadingDao().delete(reading.id)
                                readingToDelete = null
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                    ) {
                        Text(stringResource(MR.strings.delete))
                    }
                }
            },
        )
    }
}

@Composable
private fun SavedReadingCard(
    reading: SavedReading,
    onViewDetails: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    HybridElevatedCard(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = reading.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    
                    reading.location?.let { location ->
                        Text(
                            text = location,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    
                    val instant = Instant.fromEpochMilliseconds(reading.timeMillis)
                    val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
                    Text(
                        text = "${localDateTime.date} ${localDateTime.hour.toString().padStart(2, '0')}:${localDateTime.minute.toString().padStart(2, '0')}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                
                Row {
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(MR.strings.delete),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
            
            // Display key signal metrics
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                reading.mainData?.signal?.fourG?.let { lte ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "LTE",
                            style = MaterialTheme.typography.labelSmall,
                        )
                        Text(
                            text = "${lte.rsrp ?: "--"} dBm",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = "RSRP",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                
                reading.mainData?.signal?.fiveG?.let { fiveG ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "5G",
                            style = MaterialTheme.typography.labelSmall,
                        )
                        Text(
                            text = "${fiveG.rsrp ?: "--"} dBm",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = "RSRP",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                
                reading.mainData?.signal?.fourG?.let { lte ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "LTE",
                            style = MaterialTheme.typography.labelSmall,
                        )
                        Text(
                            text = "${lte.sinr ?: "--"} dB",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = "SINR",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                
                reading.mainData?.signal?.fiveG?.let { fiveG ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "5G",
                            style = MaterialTheme.typography.labelSmall,
                        )
                        Text(
                            text = "${fiveG.sinr ?: "--"} dB",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = "SINR",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = onViewDetails,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(MR.strings.view_details))
            }
        }
    }
}

@Composable
private fun ReadingDetailsDialog(
    reading: SavedReading,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    InWindowAlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(reading.name)
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        reading.location?.let { location ->
                            InfoRow(
                                label = stringResource(MR.strings.location_optional).replace(" (optional)", ""),
                                value = location,
                            )
                        }
                        
                        reading.notes?.let { notes ->
                            InfoRow(
                                label = stringResource(MR.strings.notes_optional).replace(" (optional)", ""),
                                value = notes,
                            )
                        }
                        
                        val instant = Instant.fromEpochMilliseconds(reading.timeMillis)
                        val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
                        InfoRow(
                            label = "Time",
                            value = "${localDateTime.date} ${localDateTime.hour.toString().padStart(2, '0')}:${localDateTime.minute.toString().padStart(2, '0')}",
                        )
                    }
                }
                
                // LTE Data
                reading.mainData?.signal?.fourG?.let { lte ->
                    item {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = stringResource(MR.strings.lte),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            lte.bands?.let { InfoRow(label = stringResource(MR.strings.bands), value = it.joinToString(", ")) }
                            lte.rsrp?.let { InfoRow(label = stringResource(MR.strings.rsrp), value = "$it dBm") }
                            lte.rsrq?.let { InfoRow(label = stringResource(MR.strings.rsrq), value = "$it dB") }
                            lte.rssi?.let { InfoRow(label = stringResource(MR.strings.rssi), value = "$it dBm") }
                            lte.sinr?.let { InfoRow(label = stringResource(MR.strings.sinr), value = "$it dB") }
                            lte.cid?.let { InfoRow(label = stringResource(MR.strings.cid), value = it.toString()) }
                            lte.nbid?.let { InfoRow(label = stringResource(MR.strings.enbid), value = it.toString()) }
                        }
                    }
                }
                
                // 5G Data
                reading.mainData?.signal?.fiveG?.let { fiveG ->
                    item {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = stringResource(MR.strings.five_g),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            fiveG.bands?.let { InfoRow(label = stringResource(MR.strings.bands), value = it.joinToString(", ")) }
                            fiveG.rsrp?.let { InfoRow(label = stringResource(MR.strings.rsrp), value = "$it dBm") }
                            fiveG.rsrq?.let { InfoRow(label = stringResource(MR.strings.rsrq), value = "$it dB") }
                            fiveG.rssi?.let { InfoRow(label = stringResource(MR.strings.rssi), value = "$it dBm") }
                            fiveG.sinr?.let { InfoRow(label = stringResource(MR.strings.sinr), value = "$it dB") }
                            fiveG.cid?.let { InfoRow(label = stringResource(MR.strings.cid), value = it.toString()) }
                            fiveG.nbid?.let { InfoRow(label = stringResource(MR.strings.gnbid), value = it.toString()) }
                        }
                    }
                }
                
                // Device Data
                reading.mainData?.device?.let { device ->
                    item {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = stringResource(MR.strings.device),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            device.friendlyName?.let { InfoRow(label = "Name", value = it) }
                            device.model?.let { InfoRow(label = "Model", value = it) }
                            device.softwareVersion?.let { InfoRow(label = "Software", value = it) }
                        }
                    }
                }
            }
        },
        buttons = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(MR.strings.ok))
            }
        },
        modifier = modifier,
    )
}