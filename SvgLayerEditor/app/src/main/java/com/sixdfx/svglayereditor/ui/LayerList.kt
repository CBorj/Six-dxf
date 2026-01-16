package com.sixdfx.svglayereditor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sixdfx.svglayereditor.model.SvgLayer
import org.burnoutcrew.reorderable.*

@Composable
fun LayerList(
    layers: List<SvgLayer>,
    onDuplicate: (SvgLayer) -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onReorder: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val reorderState = rememberReorderableLazyListState(
        onMove = { from, to ->
            onReorder(from.index, to.index)
        }
    )
    
    LazyColumn(
        state = reorderState.listState,
        modifier = modifier
            .reorderable(reorderState)
            .fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(
            items = layers,
            key = { _, layer -> layer.id }
        ) { _, layer ->
            ReorderableItem(reorderState, key = layer.id) { isDragging ->
                LayerCard(
                    layer = layer,
                    isDragging = isDragging,
                    onDuplicate = { onDuplicate(layer) },
                    onRename = { newName -> onRename(layer.id, newName) },
                    onDelete = { onDelete(layer.id) },
                    dragModifier = Modifier.detectReorderAfterLongPress(reorderState)
                )
            }
        }
    }
}

@Composable
fun LayerCard(
    layer: SvgLayer,
    isDragging: Boolean,
    onDuplicate: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    dragModifier: Modifier = Modifier
) {
    var showRenameDialog by remember { mutableStateOf(false) }
    var newName by remember(layer.name) { mutableStateOf(layer.name) }
    
    val backgroundColor = if (layer.isOriginal) {
        Color(0xFFE3F2FD) // Azul claro para originales
    } else {
        Color(0xFFFFF3E0) // Naranja claro para duplicadas
    }
    
    val elevation = if (isDragging) 8.dp else 2.dp
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(dragModifier),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono de arrastre
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = "Arrastrar",
                modifier = Modifier
                    .size(24.dp)
                    .padding(end = 8.dp),
                tint = Color.Gray
            )
            
            // Información de la capa
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = layer.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Badge para tipo
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF6B4E9B))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = layer.tagName,
                            color = Color.White,
                            fontSize = 10.sp
                        )
                    }
                }
                
                if (!layer.isOriginal) {
                    Text(
                        text = "Duplicado de: ${layer.parentOriginalId}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                } else {
                    Text(
                        text = "Original",
                        fontSize = 12.sp,
                        color = Color(0xFF1976D2)
                    )
                }
            }
            
            // Botones de acción
            Row {
                // Duplicar
                IconButton(
                    onClick = onDuplicate,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Duplicar",
                        tint = Color(0xFF4CAF50)
                    )
                }
                
                // Renombrar
                IconButton(
                    onClick = { showRenameDialog = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Renombrar",
                        tint = Color(0xFF2196F3)
                    )
                }
                
                // Eliminar (solo para no originales)
                IconButton(
                    onClick = onDelete,
                    enabled = !layer.isOriginal,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = if (layer.isOriginal) Color.LightGray else Color(0xFFF44336)
                    )
                }
            }
        }
    }
    
    // Diálogo de renombrar
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Renombrar capa") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Nuevo nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newName.isNotBlank()) {
                            onRename(newName)
                        }
                        showRenameDialog = false
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    newName = layer.name
                    showRenameDialog = false 
                }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
