package dev.zwander.common.util

import dev.zwander.common.model.adapters.BaseAdvancedData
import dev.zwander.common.model.adapters.BaseCellData

object CellMapperUtils {
    private const val CELLMAPPER_BASE_URL = "https://www.cellmapper.net/map"
    
    fun generateCellMapperUrl(
        cellData: BaseCellData?,
        advancedData: BaseAdvancedData?,
        lat: Double? = null,
        lon: Double? = null
    ): String? {
        val mcc = advancedData?.mcc?.toIntOrNull() ?: return null
        val mnc = advancedData?.mnc?.toIntOrNull() ?: return null
        val tac = advancedData?.tac?.toIntOrNull() ?: return null
        val pci = advancedData?.pci?.toIntOrNull()
        val earfcn = advancedData?.earfcn?.toIntOrNull()
        
        val networkType = when {
            cellData?.bands?.any { it.startsWith("n") } == true -> "NR"
            else -> "LTE"
        }
        
        return buildString {
            append(CELLMAPPER_BASE_URL)
            append("?MCC=$mcc")
            append("&MNC=$mnc")
            append("&type=$networkType")
            append("&latitude=${lat ?: 0}")
            append("&longitude=${lon ?: 0}")
            append("&zoom=13")
            append("&showTowers=true")
            append("&showIcons=true")
            append("&showData=true")
            append("&showNeighbors=false")
            append("&showLines=true")
            append("&showLabels=true")
            
            pci?.let { append("&PCI=$it") }
            earfcn?.let { append("&EARFCN=$it") }
            tac?.let { append("&TAC=$it") }
        }
    }
    
    fun generateTowerSearchUrl(
        advancedData: BaseAdvancedData?,
        cellData: BaseCellData?
    ): String? {
        val mcc = advancedData?.mcc?.toIntOrNull() ?: return null
        val mnc = advancedData?.mnc?.toIntOrNull() ?: return null
        val ecgi = advancedData?.ecgi ?: return null
        
        val networkType = when {
            cellData?.bands?.any { it.startsWith("n") } == true -> "NR"
            else -> "LTE"
        }
        
        return buildString {
            append("https://www.cellmapper.net/")
            append("$networkType/")
            append("$mcc/$mnc/")
            append("search?cell=$ecgi")
        }
    }
}