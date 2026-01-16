package com.sixdfx.svglayereditor.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.sixdfx.svglayereditor.FileExplorer
import com.sixdfx.svglayereditor.SvgFileInfo
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilePickerDialog(
    files: List<SvgFileInfo>,
    isLoading: Boolean,
    hasFullStorageAccess: Boolean,
    onFileSelected: (SvgFileInfo) -> Unit,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit,
    onUseSystemPicker: () -> Unit,
    onRequestPermission: () -> Unit
) {
    val primaryColor = Color(0xFF6B4E9B)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Estado para la previsualización
    var previewFile by remember { mutableStateOf<SvgFileInfo?>(null) }
    
    // Estado para el diálogo de renombrar
    var fileToRename by remember { mutableStateOf<SvgFileInfo?>(null) }
    
    // Estado para el diálogo de eliminar
    var fileToDelete by remember { mutableStateOf<SvgFileInfo?>(null) }
    
    // ImageLoader con soporte para SVG
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                add(SvgDecoder.Factory())
            }
            .build()
    }
    
    // Diálogo de renombrar
    fileToRename?.let { file ->
        RenameFileDialog(
            currentName = file.name,
            onConfirm = { newName ->
                scope.launch {
                    val result = FileExplorer.renameFile(context, file, newName)
                    if (result != null) {
                        Toast.makeText(context, "Archivo renombrado", Toast.LENGTH_SHORT).show()
                        onRefresh()
                    } else {
                        Toast.makeText(context, "Error al renombrar. Verifica que tienes permisos.", Toast.LENGTH_LONG).show()
                    }
                }
                fileToRename = null
            },
            onDismiss = { fileToRename = null }
        )
    }
    
    // Diálogo de confirmar eliminación
    fileToDelete?.let { file ->
        DeleteFileDialog(
            fileName = file.name,
            onConfirm = {
                scope.launch {
                    val success = FileExplorer.deleteFile(context, file)
                    if (success) {
                        Toast.makeText(context, "Archivo eliminado", Toast.LENGTH_SHORT).show()
                        onRefresh()
                    } else {
                        Toast.makeText(context, "Error al eliminar. Verifica que tienes permisos.", Toast.LENGTH_LONG).show()
                    }
                }
                fileToDelete = null
            },
            onDismiss = { fileToDelete = null }
        )
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                TopAppBar(
                    title = { 
                        Text(
                            "Seleccionar archivo SVG",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = primaryColor,
                        titleContentColor = Color.White
                    ),
                    actions = {
                        IconButton(onClick = onRefresh) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Actualizar",
                                tint = Color.White
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Cerrar",
                                tint = Color.White
                            )
                        }
                    }
                )
                
                // Banner de permisos
                if (!hasFullStorageAccess) {
                    PermissionBanner(
                        onRequestPermission = onRequestPermission
                    )
                }
                
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(color = primaryColor)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Buscando archivos SVG...")
                            }
                        }
                    }
                    
                    files.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.SearchOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "No se encontraron archivos SVG",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Coloca archivos SVG en la carpeta Descargas o Documentos",
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                    
                    else -> {
                        // Lista de archivos
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentPadding = PaddingValues(8.dp)
                        ) {
                            items(files, key = { it.path }) { file ->
                                FileItemWithPreview(
                                    file = file,
                                    imageLoader = imageLoader,
                                    isExpanded = previewFile == file,
                                    onPreviewClick = {
                                        previewFile = if (previewFile == file) null else file
                                    },
                                    onSelectClick = { onFileSelected(file) },
                                    onRenameClick = { fileToRename = file },
                                    onDeleteClick = { fileToDelete = file }
                                )
                            }
                        }
                    }
                }
                
                // Footer con botón alternativo
                Divider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${files.size} archivos encontrados",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    
                    TextButton(onClick = onUseSystemPicker) {
                        Icon(
                            Icons.Default.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Usar explorador del sistema")
                    }
                }
            }
        }
    }
}

@Composable
fun RenameFileDialog(
    currentName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val primaryColor = Color(0xFF6B4E9B)
    
    // Quitar la extensión .svg para editar solo el nombre
    val nameWithoutExtension = currentName.removeSuffix(".svg").removeSuffix(".SVG")
    var newName by remember { mutableStateOf(nameWithoutExtension) }
    var isError by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.DriveFileRenameOutline,
                    contentDescription = null,
                    tint = primaryColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Renombrar archivo")
            }
        },
        text = {
            Column {
                Text(
                    "Introduce el nuevo nombre para el archivo:",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = newName,
                    onValueChange = { 
                        newName = it
                        isError = it.isBlank() || it.contains("/") || it.contains("\\")
                    },
                    label = { Text("Nombre") },
                    suffix = { Text(".svg") },
                    isError = isError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primaryColor,
                        cursorColor = primaryColor
                    )
                )
                if (isError) {
                    Text(
                        "Nombre no válido",
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(newName) },
                enabled = newName.isNotBlank() && !isError,
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
            ) {
                Text("Renombrar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun DeleteFileDialog(
    fileName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = Color.Red
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Eliminar archivo")
            }
        },
        text = {
            Column {
                Text("¿Estás seguro de que quieres eliminar este archivo?")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    fileName,
                    fontWeight = FontWeight.Bold,
                    color = Color.Red
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Esta acción no se puede deshacer.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text("Eliminar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun FileItemWithPreview(
    file: SvgFileInfo,
    imageLoader: ImageLoader,
    isExpanded: Boolean,
    onPreviewClick: () -> Unit,
    onSelectClick: () -> Unit,
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val primaryColor = Color(0xFF6B4E9B)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded) Color(0xFFF5F0FF) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onPreviewClick)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Miniatura SVG
                SvgThumbnail(
                    file = file,
                    imageLoader = imageLoader,
                    size = 52
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Info del archivo
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = file.name,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(2.dp))
                    
                    Row {
                        Text(
                            text = formatFileSize(file.size),
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = " • ",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = formatDate(file.lastModified),
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
                
                // Icono de expansión
                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Ocultar" else "Ver preview",
                    tint = primaryColor
                )
            }
            
            // Preview expandible
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                ) {
                    // Preview grande
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White)
                            .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        SvgPreview(
                            file = file,
                            imageLoader = imageLoader
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Ruta del archivo
                    Text(
                        text = file.path,
                        fontSize = 11.sp,
                        color = Color.Gray,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Botones de acción
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Botón Renombrar
                        OutlinedButton(
                            onClick = onRenameClick,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                Icons.Default.DriveFileRenameOutline,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Renombrar", fontSize = 12.sp)
                        }
                        
                        // Botón Eliminar
                        OutlinedButton(
                            onClick = onDeleteClick,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.Red
                            )
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Eliminar", fontSize = 12.sp)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Botón para seleccionar
                    Button(
                        onClick = onSelectClick,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Editar este archivo")
                    }
                }
            }
        }
    }
}

@Composable
fun SvgThumbnail(
    file: SvgFileInfo,
    imageLoader: ImageLoader,
    size: Int = 48
) {
    val context = LocalContext.current
    
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFF5F5F5),
        modifier = Modifier
            .size(size.dp)
            .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            if (file.uri != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(file.uri)
                        .crossfade(true)
                        .build(),
                    contentDescription = file.name,
                    imageLoader = imageLoader,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                    contentScale = ContentScale.Fit
                )
            } else if (file.path.isNotEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(File(file.path))
                        .crossfade(true)
                        .build(),
                    contentDescription = file.name,
                    imageLoader = imageLoader,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                    contentScale = ContentScale.Fit
                )
            } else {
                // Fallback
                Text(
                    text = "SVG",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color(0xFF1976D2)
                )
            }
        }
    }
}

@Composable
fun SvgPreview(
    file: SvgFileInfo,
    imageLoader: ImageLoader
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading && !hasError) {
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                color = Color(0xFF6B4E9B)
            )
        }
        
        if (file.uri != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(file.uri)
                    .crossfade(true)
                    .listener(
                        onSuccess = { _, _ -> isLoading = false },
                        onError = { _, _ -> 
                            isLoading = false
                            hasError = true
                        }
                    )
                    .build(),
                contentDescription = file.name,
                imageLoader = imageLoader,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                contentScale = ContentScale.Fit
            )
        } else if (file.path.isNotEmpty()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(File(file.path))
                    .crossfade(true)
                    .listener(
                        onSuccess = { _, _ -> isLoading = false },
                        onError = { _, _ -> 
                            isLoading = false
                            hasError = true
                        }
                    )
                    .build(),
                contentDescription = file.name,
                imageLoader = imageLoader,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                contentScale = ContentScale.Fit
            )
        }
        
        if (hasError) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.BrokenImage,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "No se pudo cargar la vista previa",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
    }
}

private fun formatDate(timestamp: Long): String {
    return if (timestamp > 0) {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(timestamp * 1000))
    } else {
        ""
    }
}

@Composable
fun PermissionBanner(
    onRequestPermission: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF3E0) // Naranja claro
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono de advertencia
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFE65100), // Naranja oscuro
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Texto
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Permisos limitados",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFFE65100)
                )
                Text(
                    text = "Para renombrar o eliminar archivos, activa el acceso completo a archivos",
                    fontSize = 12.sp,
                    color = Color(0xFF5D4037)
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Botón para ir a ajustes
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE65100)
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "Ajustes",
                    fontSize = 12.sp
                )
            }
        }
    }
}
