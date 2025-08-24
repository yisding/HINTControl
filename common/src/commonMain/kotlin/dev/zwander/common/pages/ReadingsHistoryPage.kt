package dev.zwander.common.pages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.icerock.moko.resources.compose.stringResource
import dev.zwander.common.components.HybridElevatedCard
import dev.zwander.common.components.InfoRow
import dev.zwander.common.data.Page
import dev.zwander.common.data.SavedReading
import dev.zwander.common.data.generateInfoList
import dev.zwander.common.data.set
import dev.zwander.common.database.getRoomDatabase
import dev.zwander.common.model.GlobalModel
import dev.zwander.compose.alertdialog.InWindowAlertDialog
import dev.zwander.resources.common.MR
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class, kotlin.time.ExperimentalTime::class)
@Composable
fun ReadingsHistoryPage(
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val database = remember { getRoomDatabase() }
    val savedReadings by database.getSavedReadingDao().getAll()
        .collectAsState(emptyList())

    var selectedReading by remember { mutableStateOf<SavedReading?>(null) }
    var readingToDelete by remember { mutableStateOf<SavedReading?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(MR.strings.readings_history)) },
                navigationIcon = {
                    IconButton(onClick = { GlobalModel.currentPage.value = Page.Main }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { paddingValues ->
        if (savedReadings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(MR.strings.no_saved_readings),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(items = savedReadings, key = { it.id }) { reading ->
                    HybridElevatedCard(
                        onClick = { selectedReading = reading },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                        ),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = reading.name,
                                style = MaterialTheme.typography.headlineSmall,
                            )
                            
                            reading.location?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                            
                            Text(
                                text = Instant.fromEpochMilliseconds(reading.timeMillis).toLocalDateTime(TimeZone.currentSystemDefault()).toString(),
                                style = MaterialTheme.typography.labelSmall,
                            )
                            
                            // Show signal strength summary
                            val signalItems = generateInfoList(reading.mainData?.signal) {
                                reading.mainData?.signal?.fourG?.let { lte ->
                                    this[MR.strings.lte] = "${lte.rsrp ?: "?"} dBm / ${lte.sinr ?: "?"} dB"
                                }
                                reading.mainData?.signal?.fiveG?.let { fiveG ->
                                    this[MR.strings.five_g] = "${fiveG.rsrp ?: "?"} dBm / ${fiveG.sinr ?: "?"} dB"
                                }
                            }
                            
                            if (signalItems.isNotEmpty()) {
                                InfoRow(
                                    items = signalItems,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                            ) {
                                TextButton(onClick = { selectedReading = reading }) {
                                    Text(stringResource(MR.strings.view_details))
                                }
                                IconButton(onClick = { readingToDelete = reading }) {
                                    Icon(Icons.Default.Delete, stringResource(MR.strings.delete))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    readingToDelete?.let { reading ->
        InWindowAlertDialog(
            showing = true,
            onDismissRequest = { readingToDelete = null },
            title = {
                Text(stringResource(MR.strings.delete_reading))
            },
            text = {
                Text(stringResource(MR.strings.delete_reading_confirmation))
            },
            buttons = {
                TextButton(onClick = { readingToDelete = null }) {
                    Text(stringResource(MR.strings.cancel))
                }
                Button(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            database.getSavedReadingDao().delete(reading.id)
                            readingToDelete = null
                        }
                    }
                ) {
                    Text(stringResource(MR.strings.delete))
                }
            }
        )
    }

    // Details dialog
    selectedReading?.let { reading ->
        ReadingDetailsDialog(
            reading = reading,
            onDismiss = { selectedReading = null }
        )
    }
}

@OptIn(kotlin.time.ExperimentalTime::class)
@Composable
fun ReadingDetailsDialog(
    reading: SavedReading,
    onDismiss: () -> Unit,
) {
    InWindowAlertDialog(
        showing = true,
        onDismissRequest = onDismiss,
        title = {
            Text(reading.name)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Basic info - using location and notes directly as strings
                reading.location?.let {
                    Text("Location: $it", style = MaterialTheme.typography.bodyMedium)
                }
                reading.notes?.let {
                    Text("Notes: $it", style = MaterialTheme.typography.bodyMedium)
                }
                Text(
                    "Time: ${Instant.fromEpochMilliseconds(reading.timeMillis).toLocalDateTime(TimeZone.currentSystemDefault())}",
                    style = MaterialTheme.typography.bodySmall
                )
                
                // Main data
                reading.mainData?.let { mainData ->
                    Text(
                        text = stringResource(MR.strings.general),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    
                    val mainInfo = generateInfoList(mainData) {
                        mainData.device?.let { device ->
                            this[MR.strings.model] = device.model
                            this[MR.strings.manufacturer] = device.manufacturer
                            device.softwareVersion?.let { this[MR.strings.software_version] = it }
                            device.hardwareVersion?.let { this[MR.strings.hardware_version] = it }
                        }
                    }
                    
                    if (mainInfo.isNotEmpty()) {
                        InfoRow(
                            items = mainInfo,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                
                // Signal data  
                reading.mainData?.signal?.let { signal ->
                    // 4G/LTE Signal
                    signal.fourG?.let { lte ->
                        Text(
                            text = stringResource(MR.strings.lte),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        
                        val lteInfo = generateInfoList(lte) {
                            this[MR.strings.rsrp] = "${lte.rsrp ?: "?"} dBm"
                            this[MR.strings.rsrq] = "${lte.rsrq ?: "?"} dB"
                            this[MR.strings.sinr] = "${lte.sinr ?: "?"} dB"
                            lte.bands?.firstOrNull()?.let { this[MR.strings.band] = it }
                        }
                        
                        if (lteInfo.isNotEmpty()) {
                            InfoRow(
                                items = lteInfo,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    
                    // 5G Signal
                    signal.fiveG?.let { fiveG ->
                        Text(
                            text = stringResource(MR.strings.five_g),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        
                        val fiveGInfo = generateInfoList(fiveG) {
                            this[MR.strings.rsrp] = "${fiveG.rsrp ?: "?"} dBm"
                            this[MR.strings.rsrq] = "${fiveG.rsrq ?: "?"} dB"
                            this[MR.strings.sinr] = "${fiveG.sinr ?: "?"} dB"
                            fiveG.bands?.firstOrNull()?.let { this[MR.strings.band] = it }
                        }
                        
                        if (fiveGInfo.isNotEmpty()) {
                            InfoRow(
                                items = fiveGInfo,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    
                    // Generic signal data
                    signal.generic?.let { generic ->
                        val genericInfo = generateInfoList(generic) {
                            generic.connectionStatus?.let { this[MR.strings.connection] = it }
                            generic.connectionText?.let { this[MR.strings.connection_text] = it }
                            generic.roaming?.let { this[MR.strings.roaming] = it.toString() }
                        }
                        
                        if (genericInfo.isNotEmpty()) {
                            InfoRow(
                                items = genericInfo,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
                
                // Cell data
                reading.cellData?.cell?.let { cell ->
                    // 4G Cell Info
                    cell.fourG?.let { lte ->
                        Text(
                            text = "${stringResource(MR.strings.lte)} Cell",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        
                        val lteCell = generateInfoList(lte) {
                            lte.cid?.let { this[MR.strings.cid] = it.toString() }
                            lte.eNBID?.let { this[MR.strings.enb_id] = it.toString() }
                            lte.pci?.let { this[MR.strings.pci] = it.toString() }
                            lte.tac?.let { this[MR.strings.tac] = it.toString() }
                            lte.earfcn?.let { this[MR.strings.earfcn] = it.toString() }
                        }
                        
                        if (lteCell.isNotEmpty()) {
                            InfoRow(
                                items = lteCell,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    
                    // 5G Cell Info
                    cell.fiveG?.let { fiveG ->
                        Text(
                            text = "${stringResource(MR.strings.five_g)} Cell",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        
                        val fiveGCell = generateInfoList(fiveG) {
                            fiveG.nci?.let { this[MR.strings.nci] = it.toString() }
                            fiveG.gNBID?.let { this[MR.strings.gnb_id] = it.toString() }
                            fiveG.pci?.let { this[MR.strings.pci] = it.toString() }
                            fiveG.tac?.let { this[MR.strings.tac] = it.toString() }
                            fiveG.nrarfcn?.let { this[MR.strings.nrarfcn] = it.toString() }
                        }
                        
                        if (fiveGCell.isNotEmpty()) {
                            InfoRow(
                                items = fiveGCell,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        },
        buttons = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(MR.strings.ok))
            }
        }
    )
}