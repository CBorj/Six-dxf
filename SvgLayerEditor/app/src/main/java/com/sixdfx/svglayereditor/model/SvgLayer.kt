package com.sixdfx.svglayereditor.model

import java.util.UUID

/**
 * Representa una capa SVG con su información
 */
data class SvgLayer(
    val id: String = UUID.randomUUID().toString(),
    val originalId: String,           // ID original del elemento en el SVG
    val name: String,                   // Nombre mostrado (puede ser modificado)
    val tagName: String,               // Tipo de elemento (g, path, rect, etc.)
    val xmlContent: String,            // Contenido XML del elemento
    val isOriginal: Boolean = true,    // Si es una capa original o duplicada
    val parentOriginalId: String? = null // Si es duplicada, ID de la capa original de la que proviene
)

/**
 * Estado de la aplicación
 */
data class EditorState(
    val fileName: String = "",
    val svgHeader: String = "",        // Parte inicial del SVG (<?xml...> y <svg ...>)
    val svgFooter: String = "</svg>",  // Cierre del SVG
    val originalLayers: List<SvgLayer> = emptyList(),
    val workingLayers: List<SvgLayer> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

/**
 * Acciones del editor
 */
sealed class EditorAction {
    data class LoadFile(val fileName: String, val content: String) : EditorAction()
    data class DuplicateLayer(val layer: SvgLayer) : EditorAction()
    data class RenameLayer(val layerId: String, val newName: String) : EditorAction()
    data class DeleteLayer(val layerId: String) : EditorAction()
    data class ReorderLayers(val fromIndex: Int, val toIndex: Int) : EditorAction()
    object ConfirmChanges : EditorAction()
    object ClearMessages : EditorAction()
    object Reset : EditorAction()
}
