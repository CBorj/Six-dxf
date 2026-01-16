package com.sixdfx.svglayereditor.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sixdfx.svglayereditor.laser.*
import com.sixdfx.svglayereditor.model.SvgLayer
import kotlin.math.roundToInt

/**
 * Pantalla de conversión SVG a G-code
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversionScreen(
    layers: List<SvgLayer>,
    onBack: () -> Unit,
    onConversionComplete: (String) -> Unit,
    viewModel: ConversionViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    
    LaunchedEffect(layers) {
        viewModel.initialize(layers)
    }
    
    // Mostrar diálogo de posición
    if (state.showPositionDialog) {
        PositionDialog(
            currentPosition = state.position,
            workAreaWidth = state.workAreaWidth,
            workAreaHeight = state.workAreaHeight,
            onDismiss = { viewModel.togglePositionDialog(false) },
            onPositionSelected = { position ->
                viewModel.updateOriginPosition(position)
                viewModel.togglePositionDialog(false)
            },
            onCustomPosition = { x, y ->
                viewModel.updateCustomPosition(x, y)
                viewModel.togglePositionDialog(false)
            }
        )
    }
    
    // Mostrar resultado de conversión
    state.conversionResult?.let { result ->
        if (result.success) {
            ConversionResultDialog(
                result = result,
                fileName = state.fileName,
                onDismiss = { viewModel.clearResult() },
                onConfirm = {
                    viewModel.getGcode()?.let { gcode ->
                        onConversionComplete(gcode)
                    }
                    viewModel.clearResult()
                }
            )
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Conversión a G-code") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.togglePositionDialog(true) }) {
                        Icon(Icons.Default.MyLocation, "Posición")
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
            // Área de trabajo y posición
            WorkAreaPreview(
                state = state,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .padding(16.dp)
            )
            
            // Lista de capas con configuración
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.layerConfigs) { config ->
                    LayerConfigCard(
                        config = config,
                        isExpanded = state.selectedLayerForEdit == config.layerId,
                        onToggleEnabled = { viewModel.toggleLayerEnabled(config.layerId) },
                        onToggleExpand = { 
                            viewModel.selectLayerForEdit(
                                if (state.selectedLayerForEdit == config.layerId) null 
                                else config.layerId
                            )
                        },
                        onSpeedChange = { viewModel.updateSpeed(config.layerId, it) },
                        onPowerChange = { viewModel.updatePower(config.layerId, it) },
                        onModeChange = { viewModel.updateMode(config.layerId, it) },
                        onMethodChange = { viewModel.updateMethod(config.layerId, it) },
                        onPassesChange = { viewModel.updatePasses(config.layerId, it) },
                        onAccuracyChange = { viewModel.updateAccuracy(config.layerId, it) },
                        onFillSpacingChange = { viewModel.updateFillSpacing(config.layerId, it) },
                        onCopyToAll = { viewModel.copyConfigToAll(config.layerId) }
                    )
                }
            }
            
            // Error message
            state.errorMessage?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            
            // Botón de conversión
            Button(
                onClick = { viewModel.convert() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(56.dp),
                enabled = !state.isConverting && state.layerConfigs.any { it.enabled }
            ) {
                if (state.isConverting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.Default.Transform, "Convertir")
                    Spacer(Modifier.width(8.dp))
                    Text("CONVERTIR A G-CODE", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Vista previa del área de trabajo con posición
 */
@Composable
fun WorkAreaPreview(
    state: ConversionScreenState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Cuadrícula visual
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Color(0xFF1A1A2E), RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Grid lines
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    repeat(5) {
                        Divider(color = Color(0xFF4ECCA3).copy(alpha = 0.2f))
                    }
                }
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    repeat(5) {
                        Divider(
                            color = Color(0xFF4ECCA3).copy(alpha = 0.2f),
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(1.dp)
                        )
                    }
                }
                
                // Indicador de posición
                val offsetX = when (state.position.originPosition) {
                    OriginPosition.TOP_LEFT, OriginPosition.CENTER_LEFT, OriginPosition.BOTTOM_LEFT -> 0f
                    OriginPosition.TOP_CENTER, OriginPosition.CENTER, OriginPosition.BOTTOM_CENTER -> 0.5f
                    OriginPosition.TOP_RIGHT, OriginPosition.CENTER_RIGHT, OriginPosition.BOTTOM_RIGHT -> 1f
                    OriginPosition.CUSTOM -> state.position.customX / state.workAreaWidth
                }
                val offsetY = when (state.position.originPosition) {
                    OriginPosition.TOP_LEFT, OriginPosition.TOP_CENTER, OriginPosition.TOP_RIGHT -> 0f
                    OriginPosition.CENTER_LEFT, OriginPosition.CENTER, OriginPosition.CENTER_RIGHT -> 0.5f
                    OriginPosition.BOTTOM_LEFT, OriginPosition.BOTTOM_CENTER, OriginPosition.BOTTOM_RIGHT -> 1f
                    OriginPosition.CUSTOM -> 1f - (state.position.customY / state.workAreaHeight)
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .align(
                                when {
                                    offsetX < 0.33f && offsetY < 0.33f -> Alignment.TopStart
                                    offsetX < 0.33f && offsetY > 0.66f -> Alignment.BottomStart
                                    offsetX < 0.33f -> Alignment.CenterStart
                                    offsetX > 0.66f && offsetY < 0.33f -> Alignment.TopEnd
                                    offsetX > 0.66f && offsetY > 0.66f -> Alignment.BottomEnd
                                    offsetX > 0.66f -> Alignment.CenterEnd
                                    offsetY < 0.33f -> Alignment.TopCenter
                                    offsetY > 0.66f -> Alignment.BottomCenter
                                    else -> Alignment.Center
                                }
                            )
                            .size(40.dp)
                            .background(Color(0xFF4ECCA3).copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                            .border(2.dp, Color(0xFF4ECCA3), RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("📐", fontSize = 16.sp)
                    }
                }
            }
            
            // Info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Área de trabajo",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${state.workAreaWidth.toInt()} × ${state.workAreaHeight.toInt()} mm",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Column {
                    Text(
                        "Posición",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        if (state.position.useCustomPosition) 
                            "X: ${state.position.customX}, Y: ${state.position.customY}"
                        else 
                            state.position.originPosition.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Column {
                    Text(
                        "Capas activas",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${state.layerConfigs.count { it.enabled }} de ${state.layerConfigs.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * Tarjeta de configuración de capa
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayerConfigCard(
    config: LayerLaserConfig,
    isExpanded: Boolean,
    onToggleEnabled: () -> Unit,
    onToggleExpand: () -> Unit,
    onSpeedChange: (Int) -> Unit,
    onPowerChange: (Int) -> Unit,
    onModeChange: (EngraveMode) -> Unit,
    @Suppress("UNUSED_PARAMETER") onMethodChange: (ProcessMethod) -> Unit,
    onPassesChange: (Int) -> Unit,
    onAccuracyChange: (Accuracy) -> Unit,
    onFillSpacingChange: (Float) -> Unit,
    onCopyToAll: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (config.enabled) 
                MaterialTheme.colorScheme.surface 
            else 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column {
            // Header de la capa
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = config.enabled,
                    onCheckedChange = { onToggleEnabled() }
                )
                
                Spacer(Modifier.width(8.dp))
                
                // Color indicator
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(
                            if (config.mode == EngraveMode.LINE) 
                                Color(0xFF4ECDC4) 
                            else 
                                Color(0xFFFF6B6B)
                        )
                )
                
                Spacer(Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = config.layerName,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${config.mode.name} • ${config.speedMmMin} mm/min • ${config.powerPercent}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Contraer" else "Expandir"
                )
            }
            
            // Configuración expandida
            AnimatedVisibility(
                visible = isExpanded && config.enabled,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Divider()
                    
                    // Modo: Line / Fill
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Modo:",
                            modifier = Modifier.width(80.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        FilterChip(
                            selected = config.mode == EngraveMode.LINE,
                            onClick = { onModeChange(EngraveMode.LINE) },
                            label = { Text("Línea") },
                            leadingIcon = {
                                if (config.mode == EngraveMode.LINE) {
                                    Icon(Icons.Default.Check, "Seleccionado", Modifier.size(16.dp))
                                }
                            }
                        )
                        FilterChip(
                            selected = config.mode == EngraveMode.FILL,
                            onClick = { onModeChange(EngraveMode.FILL) },
                            label = { Text("Relleno") },
                            leadingIcon = {
                                if (config.mode == EngraveMode.FILL) {
                                    Icon(Icons.Default.Check, "Seleccionado", Modifier.size(16.dp))
                                }
                            }
                        )
                    }
                    
                    // Velocidad
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Velocidad", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "${config.speedMmMin} mm/min",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = config.speedMmMin.toFloat(),
                            onValueChange = { onSpeedChange(it.roundToInt()) },
                            valueRange = 500f..6000f,
                            steps = 10
                        )
                    }
                    
                    // Potencia
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Potencia", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "${config.powerPercent}%",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = config.powerPercent.toFloat(),
                            onValueChange = { onPowerChange(it.roundToInt()) },
                            valueRange = 0f..100f
                        )
                    }
                    
                    // Pasadas
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Pasadas:",
                            modifier = Modifier.width(80.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        IconButton(
                            onClick = { onPassesChange(config.passes - 1) },
                            enabled = config.passes > 1
                        ) {
                            Icon(Icons.Default.Remove, "Menos")
                        }
                        
                        Text(
                            "${config.passes}",
                            modifier = Modifier.width(40.dp),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold
                        )
                        
                        IconButton(
                            onClick = { onPassesChange(config.passes + 1) },
                            enabled = config.passes < 10
                        ) {
                            Icon(Icons.Default.Add, "Más")
                        }
                    }
                    
                    // Precisión
                    Column {
                        Text("Precisión", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Accuracy.entries.forEach { accuracy ->
                                FilterChip(
                                    selected = config.accuracy == accuracy,
                                    onClick = { onAccuracyChange(accuracy) },
                                    label = { 
                                        Text(
                                            accuracy.displayName.replace(" ", "\n"),
                                            fontSize = 10.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    
                    // Espaciado de relleno (solo si modo es FILL)
                    if (config.mode == EngraveMode.FILL) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Espaciado relleno", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "${String.format("%.2f", config.fillSpacing)} mm",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Slider(
                                value = config.fillSpacing,
                                onValueChange = { onFillSpacingChange(it) },
                                valueRange = 0.05f..1f
                            )
                        }
                    }
                    
                    // Botón copiar a todas
                    OutlinedButton(
                        onClick = onCopyToAll,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.ContentCopy, "Copiar")
                        Spacer(Modifier.width(8.dp))
                        Text("Copiar config a todas las capas")
                    }
                }
            }
        }
    }
}

/**
 * Diálogo de selección de posición
 */
@Composable
fun PositionDialog(
    currentPosition: JobPosition,
    workAreaWidth: Float,
    workAreaHeight: Float,
    onDismiss: () -> Unit,
    onPositionSelected: (OriginPosition) -> Unit,
    onCustomPosition: (Float, Float) -> Unit
) {
    var customX by remember { mutableStateOf(currentPosition.customX.toString()) }
    var customY by remember { mutableStateOf(currentPosition.customY.toString()) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    "Posición del diseño",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(Modifier.height(16.dp))
                
                Text(
                    "Selecciona dónde colocar el origen (0,0) de tu diseño en el área de trabajo:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(Modifier.height(16.dp))
                
                // Grid de posiciones predefinidas
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Fila superior
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        PositionButton(
                            position = OriginPosition.TOP_LEFT,
                            isSelected = currentPosition.originPosition == OriginPosition.TOP_LEFT && !currentPosition.useCustomPosition,
                            onClick = { onPositionSelected(OriginPosition.TOP_LEFT) },
                            modifier = Modifier.weight(1f)
                        )
                        PositionButton(
                            position = OriginPosition.TOP_CENTER,
                            isSelected = currentPosition.originPosition == OriginPosition.TOP_CENTER && !currentPosition.useCustomPosition,
                            onClick = { onPositionSelected(OriginPosition.TOP_CENTER) },
                            modifier = Modifier.weight(1f)
                        )
                        PositionButton(
                            position = OriginPosition.TOP_RIGHT,
                            isSelected = currentPosition.originPosition == OriginPosition.TOP_RIGHT && !currentPosition.useCustomPosition,
                            onClick = { onPositionSelected(OriginPosition.TOP_RIGHT) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    // Fila central
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        PositionButton(
                            position = OriginPosition.CENTER_LEFT,
                            isSelected = currentPosition.originPosition == OriginPosition.CENTER_LEFT && !currentPosition.useCustomPosition,
                            onClick = { onPositionSelected(OriginPosition.CENTER_LEFT) },
                            modifier = Modifier.weight(1f)
                        )
                        PositionButton(
                            position = OriginPosition.CENTER,
                            isSelected = currentPosition.originPosition == OriginPosition.CENTER && !currentPosition.useCustomPosition,
                            onClick = { onPositionSelected(OriginPosition.CENTER) },
                            modifier = Modifier.weight(1f)
                        )
                        PositionButton(
                            position = OriginPosition.CENTER_RIGHT,
                            isSelected = currentPosition.originPosition == OriginPosition.CENTER_RIGHT && !currentPosition.useCustomPosition,
                            onClick = { onPositionSelected(OriginPosition.CENTER_RIGHT) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    // Fila inferior
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        PositionButton(
                            position = OriginPosition.BOTTOM_LEFT,
                            isSelected = currentPosition.originPosition == OriginPosition.BOTTOM_LEFT && !currentPosition.useCustomPosition,
                            onClick = { onPositionSelected(OriginPosition.BOTTOM_LEFT) },
                            modifier = Modifier.weight(1f)
                        )
                        PositionButton(
                            position = OriginPosition.BOTTOM_CENTER,
                            isSelected = currentPosition.originPosition == OriginPosition.BOTTOM_CENTER && !currentPosition.useCustomPosition,
                            onClick = { onPositionSelected(OriginPosition.BOTTOM_CENTER) },
                            modifier = Modifier.weight(1f)
                        )
                        PositionButton(
                            position = OriginPosition.BOTTOM_RIGHT,
                            isSelected = currentPosition.originPosition == OriginPosition.BOTTOM_RIGHT && !currentPosition.useCustomPosition,
                            onClick = { onPositionSelected(OriginPosition.BOTTOM_RIGHT) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                Divider()
                
                Spacer(Modifier.height(16.dp))
                
                // Posición personalizada
                Text(
                    "O introduce posición personalizada (mm):",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customX,
                        onValueChange = { customX = it },
                        label = { Text("X") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = customY,
                        onValueChange = { customY = it },
                        label = { Text("Y") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                }
                
                Text(
                    "Área: ${workAreaWidth.toInt()} × ${workAreaHeight.toInt()} mm",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar")
                    }
                    
                    Button(
                        onClick = {
                            val x = customX.toFloatOrNull() ?: 0f
                            val y = customY.toFloatOrNull() ?: 0f
                            onCustomPosition(x, y)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Aplicar")
                    }
                }
            }
        }
    }
}

/**
 * Botón de selección de posición
 */
@Composable
fun PositionButton(
    position: OriginPosition,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon = when (position) {
        OriginPosition.TOP_LEFT -> "↖"
        OriginPosition.TOP_CENTER -> "↑"
        OriginPosition.TOP_RIGHT -> "↗"
        OriginPosition.CENTER_LEFT -> "←"
        OriginPosition.CENTER -> "●"
        OriginPosition.CENTER_RIGHT -> "→"
        OriginPosition.BOTTOM_LEFT -> "↙"
        OriginPosition.BOTTOM_CENTER -> "↓"
        OriginPosition.BOTTOM_RIGHT -> "↘"
        OriginPosition.CUSTOM -> "?"
    }
    
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primary 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Text(
            icon,
            color = if (isSelected) 
                MaterialTheme.colorScheme.onPrimary 
            else 
                MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 18.sp
        )
    }
}

/**
 * Diálogo de resultado de conversión
 */
@Composable
fun ConversionResultDialog(
    result: ConversionResult,
    fileName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Éxito",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(Modifier.height(16.dp))
                
                Text(
                    "¡Conversión exitosa!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(Modifier.height(16.dp))
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        ResultRow("Archivo", fileName)
                        ResultRow("Líneas G-code", "${result.lineCount}")
                        ResultRow("Tiempo estimado", "${String.format("%.1f", result.estimatedTimeMinutes)} min")
                        result.boundingBox?.let { box ->
                            ResultRow("Tamaño", "${String.format("%.1f", box.width)} × ${String.format("%.1f", box.height)} mm")
                            ResultRow("Posición", "X:${String.format("%.1f", box.minX)}, Y:${String.format("%.1f", box.minY)}")
                        }
                    }
                }
                
                Spacer(Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cerrar")
                    }
                    
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Send, "Enviar")
                        Spacer(Modifier.width(8.dp))
                        Text("Continuar")
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}
