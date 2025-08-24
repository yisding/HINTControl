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
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(MR.strings.back))
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
                                text = reading.time.toLocalDateTime(TimeZone.currentSystemDefault()).toString(),
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
                // Basic info
                val basicInfo = generateInfoList(reading) {
                    this[MR.strings.timestamp] = reading.time.toLocalDateTime(TimeZone.currentSystemDefault()).toString()
                    reading.location?.let { this[MR.strings.location] = it }
                    reading.notes?.let { this[MR.strings.notes] = it }
                }
                
                if (basicInfo.isNotEmpty()) {
                    InfoRow(
                        items = basicInfo,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                
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
                            this[MR.strings.software_version] = device.softwareVersion
                            this[MR.strings.hardware_version] = device.hardwareVersion
                        }
                        mainData.generic?.let { generic ->
                            this[MR.strings.connection] = generic.connectionStatus
                            this[MR.strings.connection_text] = generic.connectionText
                            this[MR.strings.has_sim] = generic.hasSim?.toString()
                            this[MR.strings.roaming] = generic.roaming?.toString()
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
                    Text(
                        text = stringResource(MR.strings.signal),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    
                    val signalInfo = generateInfoList(signal) {
                        signal.fourG?.let { lte ->
                            this[MR.strings.rsrp] = "${lte.rsrp ?: "?"} dBm"
                            this[MR.strings.rsrq] = "${lte.rsrq ?: "?"} dB"
                            this[MR.strings.sinr] = "${lte.sinr ?: "?"} dB"
                            this[MR.strings.band] = lte.band
                            this[MR.strings.bandwidth] = lte.bandwidth
                        }
                        signal.fiveG?.let { fiveG ->
                            this[MR.strings.rsrp] = "${fiveG.rsrp ?: "?"} dBm"
                            this[MR.strings.rsrq] = "${fiveG.rsrq ?: "?"} dB"
                            this[MR.strings.sinr] = "${fiveG.sinr ?: "?"} dB"
                            this[MR.strings.band] = fiveG.band
                            this[MR.strings.bandwidth] = fiveG.bandwidth
                        }
                    }
                    
                    if (signalInfo.isNotEmpty()) {
                        InfoRow(
                            items = signalInfo,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                
                // Cell data
                reading.cellData?.cell?.let { cell ->
                    Text(
                        text = stringResource(MR.strings.cell_info),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    
                    val cellInfo = generateInfoList(cell) {
                        cell.fourG?.let { lte ->
                            this[MR.strings.cell_id] = lte.cid?.toString()
                            this[MR.strings.enb_id] = lte.eNBID?.toString()
                            this[MR.strings.pci] = lte.pci?.toString()
                            this[MR.strings.tac] = lte.tac?.toString()
                            this[MR.strings.earfcn] = lte.earfcn?.toString()
                        }
                        cell.fiveG?.let { fiveG ->
                            this[MR.strings.nci] = fiveG.nci?.toString()
                            this[MR.strings.gnb_id] = fiveG.gNBID?.toString()
                            this[MR.strings.pci] = fiveG.pci?.toString()
                            this[MR.strings.tac] = fiveG.tac?.toString()
                            this[MR.strings.nrarfcn] = fiveG.nrarfcn?.toString()
                        }
                    }
                    
                    if (cellInfo.isNotEmpty()) {
                        InfoRow(
                            items = cellInfo,
                            modifier = Modifier.fillMaxWidth(),
                        )
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