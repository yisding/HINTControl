package dev.zwander.common.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.icerock.moko.resources.compose.stringResource
import dev.zwander.common.data.SavedReading
import dev.zwander.common.database.getRoomDatabase
import dev.zwander.common.model.MainModel
import dev.zwander.compose.alertdialog.InWindowAlertDialog
import dev.zwander.resources.common.MR
import kotlinx.coroutines.launch

@Composable
fun SaveReadingDialog(
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    InWindowAlertDialog(
        showing = true,
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(MR.strings.save_reading))
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(MR.strings.reading_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text(stringResource(MR.strings.location_optional)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(MR.strings.notes_optional)) },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        buttons = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                }
                
                TextButton(
                    onClick = onDismiss,
                    enabled = !isSaving,
                ) {
                    Text(stringResource(MR.strings.cancel))
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                TextButton(
                    onClick = {
                        if (name.isNotBlank()) {
                            isSaving = true
                            scope.launch {
                                try {
                                    val database = getRoomDatabase()
                                    val reading = SavedReading(
                                        name = name.trim(),
                                        location = location.trim().takeIf { it.isNotBlank() },
                                        notes = notes.trim().takeIf { it.isNotBlank() },
                                        timeMillis = System.currentTimeMillis(),
                                        mainData = MainModel.currentMainData.value,
                                        cellData = MainModel.currentCellData.value,
                                        clientData = MainModel.currentClientData.value,
                                        simData = MainModel.currentSimData.value,
                                    )
                                    database.getSavedReadingDao().insert(reading)
                                    onSaved()
                                    onDismiss()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                } finally {
                                    isSaving = false
                                }
                            }
                        }
                    },
                    enabled = name.isNotBlank() && !isSaving,
                ) {
                    Text(stringResource(MR.strings.save))
                }
            }
        },
        modifier = modifier,
    )
}