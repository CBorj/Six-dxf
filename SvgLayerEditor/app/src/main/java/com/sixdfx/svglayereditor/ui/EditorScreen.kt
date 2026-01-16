package com.sixdfx.svglayereditor.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sixdfx.svglayereditor.R
import com.sixdfx.svglayereditor.model.EditorAction
import com.sixdfx.svglayereditor.model.EditorState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    state: EditorState,
    onAction: (EditorAction) -> Unit,
    onSelectFile: () -> Unit,
    onSaveFile: (String) -> Unit,
    onNavigateToConversion: () -> Unit = {},
    onSelectFileForConversion: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val primaryColor = Color(0xFF6B4E9B)
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_logo),
                            contentDescription = "Logo",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "SVG Layer Editor",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = primaryColor,
                    titleContentColor = Color.White
                ),
                actions = {
                    if (state.fileName.isNotEmpty()) {
                        IconButton(onClick = { onAction(EditorAction.Reset) }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar",
                                tint = Color.White
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (state.workingLayers.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 8.dp,
                    color = Color.White
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Estadísticas
                        Column {
                            Text(
                                text = "Total: ${state.workingLayers.size} capas",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = "${state.workingLayers.count { !it.isOriginal }} añadidas",
                                fontSize = 12.sp,
                                color = Color(0xFFFF7043)
                            )
                        }
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Botón láser
                            OutlinedButton(
                                onClick = onNavigateToConversion,
                                shape = RoundedCornerShape(24.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = Color(0xFFFF9800)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Láser", color = Color(0xFFFF9800))
                            }
                            
                            // Botón confirmar
                            Button(
                                onClick = {
                                    onAction(EditorAction.ConfirmChanges)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = primaryColor
                                ),
                                shape = RoundedCornerShape(24.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Guardar")
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF5F5F5))
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = primaryColor
                    )
                }
                
                state.fileName.isEmpty() -> {
                    // Pantalla inicial
                    WelcomeScreen(
                        onEditSvg = onSelectFile,
                        onConvertToGcode = onSelectFileForConversion,
                        primaryColor = primaryColor
                    )
                }
                
                else -> {
                    // Editor de capas
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Header con nombre de archivo
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.InsertDriveFile,
                                    contentDescription = null,
                                    tint = primaryColor,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = state.fileName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        text = "${state.originalLayers.size} capas originales",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                        
                        // Instrucciones
                        Text(
                            text = "📌 Mantén presionado para arrastrar • 🔵 Original • 🟠 Duplicado",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Lista de capas
                        LayerList(
                            layers = state.workingLayers,
                            onDuplicate = { layer ->
                                onAction(EditorAction.DuplicateLayer(layer))
                            },
                            onRename = { layerId, newName ->
                                onAction(EditorAction.RenameLayer(layerId, newName))
                            },
                            onDelete = { layerId ->
                                onAction(EditorAction.DeleteLayer(layerId))
                            },
                            onReorder = { from, to ->
                                onAction(EditorAction.ReorderLayers(from, to))
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            
            // Manejo de mensajes de éxito
            state.successMessage?.let { svgContent ->
                LaunchedEffect(svgContent) {
                    onSaveFile(svgContent)
                    onAction(EditorAction.ClearMessages)
                }
            }
            
            // Manejo de errores
            state.errorMessage?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { onAction(EditorAction.ClearMessages) }) {
                            Text("OK", color = Color.White)
                        }
                    },
                    containerColor = Color(0xFFF44336)
                ) {
                    Text(error, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun WelcomeScreen(
    onEditSvg: () -> Unit,
    onConvertToGcode: () -> Unit,
    primaryColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_logo),
            contentDescription = "Logo",
            modifier = Modifier
                .size(150.dp)
                .clip(RoundedCornerShape(24.dp)),
            contentScale = ContentScale.Crop
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "SVG Layer Editor",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = primaryColor
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Edita capas SVG y convierte a G-code\npara grabado láser",
            fontSize = 16.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Botón Editar SVG
        Button(
            onClick = onEditSvg,
            colors = ButtonDefaults.buttonColors(
                containerColor = primaryColor
            ),
            shape = RoundedCornerShape(24.dp),
            contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Editar SVG",
                fontSize = 16.sp
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Botón Convertir a G-code
        OutlinedButton(
            onClick = onConvertToGcode,
            shape = RoundedCornerShape(24.dp),
            contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color(0xFFFF9800)
            ),
            border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFF9800))
        ) {
            Icon(
                imageVector = Icons.Default.Bolt,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Convertir a G-code",
                fontSize = 16.sp
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFE8F5E9)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "✨ Funcionalidades",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("• Editar capas SVG (duplicar, renombrar, reordenar)", fontSize = 14.sp)
                Text("• Convertir SVG a G-code para láser", fontSize = 14.sp)
                Text("• Configurar velocidad, potencia y pasadas", fontSize = 14.sp)
                Text("• Modo vectorial (LINE) o relleno (FILL)", fontSize = 14.sp)
                Text("• Posicionar imagen en área de trabajo", fontSize = 14.sp)
            }
        }
    }
}
