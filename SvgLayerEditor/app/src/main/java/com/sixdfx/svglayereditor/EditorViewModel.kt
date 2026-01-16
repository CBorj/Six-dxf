package com.sixdfx.svglayereditor

import androidx.lifecycle.ViewModel
import com.sixdfx.svglayereditor.model.EditorAction
import com.sixdfx.svglayereditor.model.EditorState
import com.sixdfx.svglayereditor.model.SvgLayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class EditorViewModel : ViewModel() {
    
    private val _state = MutableStateFlow(EditorState())
    val state: StateFlow<EditorState> = _state.asStateFlow()
    
    fun dispatch(action: EditorAction) {
        when (action) {
            is EditorAction.LoadFile -> loadFile(action.fileName, action.content)
            is EditorAction.DuplicateLayer -> duplicateLayer(action.layer)
            is EditorAction.RenameLayer -> renameLayer(action.layerId, action.newName)
            is EditorAction.DeleteLayer -> deleteLayer(action.layerId)
            is EditorAction.ReorderLayers -> reorderLayers(action.fromIndex, action.toIndex)
            is EditorAction.ConfirmChanges -> confirmChanges()
            is EditorAction.ClearMessages -> clearMessages()
            is EditorAction.Reset -> reset()
        }
    }
    
    private fun loadFile(fileName: String, content: String) {
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        
        try {
            val (header, layers, footer) = SvgProcessor.parseSvg(content)
            
            _state.update { state ->
                state.copy(
                    fileName = fileName,
                    svgHeader = header,
                    svgFooter = footer,
                    originalLayers = layers,
                    workingLayers = layers.toList(),
                    isLoading = false
                )
            }
        } catch (e: Exception) {
            _state.update { 
                it.copy(
                    isLoading = false, 
                    errorMessage = "Error al cargar el archivo: ${e.message}"
                ) 
            }
        }
    }
    
    private fun duplicateLayer(layer: SvgLayer) {
        val currentLayers = _state.value.workingLayers.toMutableList()
        val baseName = layer.name
        var counter = 1
        var newName = "${baseName}_copia"
        
        // Asegurarse de que el nombre sea único
        while (currentLayers.any { it.name == newName }) {
            counter++
            newName = "${baseName}_copia$counter"
        }
        
        val duplicatedLayer = SvgProcessor.duplicateLayer(layer, newName)
        
        // Insertar después de la capa original
        val index = currentLayers.indexOfFirst { it.id == layer.id }
        if (index >= 0) {
            currentLayers.add(index + 1, duplicatedLayer)
        } else {
            currentLayers.add(duplicatedLayer)
        }
        
        _state.update { it.copy(workingLayers = currentLayers) }
    }
    
    private fun renameLayer(layerId: String, newName: String) {
        val currentLayers = _state.value.workingLayers.toMutableList()
        val index = currentLayers.indexOfFirst { it.id == layerId }
        
        if (index >= 0) {
            val layer = currentLayers[index]
            // Solo permitir renombrar si no es original o si el nuevo nombre es válido
            currentLayers[index] = layer.copy(name = newName)
            _state.update { it.copy(workingLayers = currentLayers) }
        }
    }
    
    private fun deleteLayer(layerId: String) {
        val currentLayers = _state.value.workingLayers.toMutableList()
        val layerToDelete = currentLayers.find { it.id == layerId }
        
        // Solo eliminar si NO es una capa original
        if (layerToDelete != null && !layerToDelete.isOriginal) {
            currentLayers.removeIf { it.id == layerId }
            _state.update { it.copy(workingLayers = currentLayers) }
        }
    }
    
    private fun reorderLayers(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        
        val currentLayers = _state.value.workingLayers.toMutableList()
        val item = currentLayers.removeAt(fromIndex)
        currentLayers.add(toIndex, item)
        
        _state.update { it.copy(workingLayers = currentLayers) }
    }
    
    private fun confirmChanges() {
        _state.update { it.copy(isLoading = true) }
        
        try {
            val currentState = _state.value
            val finalSvg = SvgProcessor.generateSvg(
                currentState.svgHeader,
                currentState.workingLayers,
                currentState.svgFooter
            )
            
            _state.update { 
                it.copy(
                    isLoading = false,
                    successMessage = finalSvg
                ) 
            }
        } catch (e: Exception) {
            _state.update { 
                it.copy(
                    isLoading = false, 
                    errorMessage = "Error al generar el archivo: ${e.message}"
                ) 
            }
        }
    }
    
    private fun clearMessages() {
        _state.update { it.copy(errorMessage = null, successMessage = null) }
    }
    
    private fun reset() {
        _state.update { EditorState() }
    }
    
    /**
     * Obtiene el contenido SVG final para guardar
     */
    fun getFinalSvgContent(): String {
        val currentState = _state.value
        return SvgProcessor.generateSvg(
            currentState.svgHeader,
            currentState.workingLayers,
            currentState.svgFooter
        )
    }
}
