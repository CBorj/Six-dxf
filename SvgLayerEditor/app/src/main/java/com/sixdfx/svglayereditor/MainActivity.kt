package com.sixdfx.svglayereditor

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sixdfx.svglayereditor.model.EditorAction
import com.sixdfx.svglayereditor.ui.ConversionScreen
import com.sixdfx.svglayereditor.ui.EditorScreen
import com.sixdfx.svglayereditor.ui.FilePickerDialog
import com.sixdfx.svglayereditor.ui.theme.SvgLayerEditorTheme
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Pantallas de navegación de la app
 */
sealed class AppScreen {
    object Editor : AppScreen()
    object Conversion : AppScreen()
}

class MainActivity : ComponentActivity() {
    
    private fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivity(intent)
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            SvgLayerEditorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainContent()
                }
            }
        }
    }
    
    @Composable
    fun MainContent() {
        val viewModel: EditorViewModel = viewModel()
        val state by viewModel.state.collectAsState()
        val coroutineScope = rememberCoroutineScope()
        
        // Estado de navegación
        var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Editor) }
        
        // Estado para el G-code generado (para enviar a la máquina)
        var generatedGcode by remember { mutableStateOf<String?>(null) }
        
        // Estado para saber si después de cargar archivo debe ir a conversión
        var navigateToConversionAfterLoad by remember { mutableStateOf(false) }
        
        // Estado para el explorador de archivos
        var showFilePicker by remember { mutableStateOf(false) }
        var svgFiles by remember { mutableStateOf<List<SvgFileInfo>>(emptyList()) }
        var isLoadingFiles by remember { mutableStateOf(false) }
        
        // Launcher para permisos
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val allGranted = permissions.values.all { it }
            if (allGranted) {
                // Cargar archivos después de obtener permisos
                coroutineScope.launch {
                    isLoadingFiles = true
                    svgFiles = FileExplorer.findSvgFiles(this@MainActivity)
                    isLoadingFiles = false
                    showFilePicker = true
                }
            } else {
                Toast.makeText(
                    this@MainActivity,
                    "Se necesitan permisos para acceder a los archivos",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        
        // Launcher para seleccionar archivo (sistema)
        val filePickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let { selectedUri ->
                try {
                    val fileName = getFileName(selectedUri) ?: "archivo.svg"
                    val content = readFileContent(selectedUri)
                    viewModel.dispatch(EditorAction.LoadFile(fileName, content))
                    // Si debe navegar a conversión después de cargar
                    if (navigateToConversionAfterLoad) {
                        currentScreen = AppScreen.Conversion
                        navigateToConversionAfterLoad = false
                    }
                } catch (e: Exception) {
                    Toast.makeText(
                        this@MainActivity,
                        "Error al leer el archivo: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    navigateToConversionAfterLoad = false
                }
            } ?: run {
                navigateToConversionAfterLoad = false
            }
        }
        
        // Launcher para guardar archivo
        val saveFileLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("image/svg+xml")
        ) { uri: Uri? ->
            uri?.let { selectedUri ->
                try {
                    val content = viewModel.getFinalSvgContent()
                    saveFileContent(selectedUri, content)
                    Toast.makeText(
                        this@MainActivity,
                        "¡Archivo guardado correctamente!",
                        Toast.LENGTH_LONG
                    ).show()
                } catch (e: Exception) {
                    Toast.makeText(
                        this@MainActivity,
                        "Error al guardar: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
        
        // Función para abrir el explorador de archivos
        fun openFilePicker() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13+: usar READ_MEDIA_IMAGES
                if (ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.READ_MEDIA_IMAGES
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    coroutineScope.launch {
                        isLoadingFiles = true
                        svgFiles = FileExplorer.findSvgFiles(this@MainActivity)
                        isLoadingFiles = false
                        showFilePicker = true
                    }
                } else {
                    permissionLauncher.launch(arrayOf(Manifest.permission.READ_MEDIA_IMAGES))
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11-12: no necesita permisos especiales para MediaStore
                coroutineScope.launch {
                    isLoadingFiles = true
                    svgFiles = FileExplorer.findSvgFiles(this@MainActivity)
                    isLoadingFiles = false
                    showFilePicker = true
                }
            } else {
                // Android 10 y anteriores
                if (ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    coroutineScope.launch {
                        isLoadingFiles = true
                        svgFiles = FileExplorer.findSvgFiles(this@MainActivity)
                        isLoadingFiles = false
                        showFilePicker = true
                    }
                } else {
                    permissionLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
                }
            }
        }
        
        // Navegación entre pantallas
        when (currentScreen) {
            is AppScreen.Editor -> {
                EditorScreen(
                    state = state,
                    onAction = { action -> viewModel.dispatch(action) },
                    onSelectFile = { openFilePicker() },
                    onSaveFile = { _ ->
                        val baseName = state.fileName.substringBeforeLast(".")
                        saveFileLauncher.launch("${baseName}_modificado.svg")
                    },
                    onNavigateToConversion = {
                        if (state.workingLayers.isNotEmpty()) {
                            currentScreen = AppScreen.Conversion
                        } else {
                            Toast.makeText(
                                this@MainActivity,
                                "Primero carga un archivo SVG",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    onSelectFileForConversion = {
                        navigateToConversionAfterLoad = true
                        openFilePicker()
                    }
                )
            }
            
            is AppScreen.Conversion -> {
                ConversionScreen(
                    layers = state.workingLayers,
                    onBack = { currentScreen = AppScreen.Editor },
                    onConversionComplete = { gcode ->
                        generatedGcode = gcode
                        // Aquí se puede añadir la navegación a la pantalla de envío
                        Toast.makeText(
                            this@MainActivity,
                            "G-code generado: ${gcode.lines().size} líneas",
                            Toast.LENGTH_LONG
                        ).show()
                        // Por ahora volver al editor
                        currentScreen = AppScreen.Editor
                    }
                )
            }
        }
        
        // Diálogo del explorador de archivos
        if (showFilePicker) {
            FilePickerDialog(
                files = svgFiles,
                isLoading = isLoadingFiles,
                hasFullStorageAccess = hasStoragePermission(),
                onFileSelected = { fileInfo ->
                    coroutineScope.launch {
                        try {
                            val content = FileExplorer.readSvgContent(this@MainActivity, fileInfo)
                            viewModel.dispatch(EditorAction.LoadFile(fileInfo.name, content))
                            showFilePicker = false
                        } catch (e: Exception) {
                            Toast.makeText(
                                this@MainActivity,
                                "Error al leer: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                },
                onDismiss = { showFilePicker = false },
                onRefresh = {
                    coroutineScope.launch {
                        isLoadingFiles = true
                        svgFiles = FileExplorer.findSvgFiles(this@MainActivity)
                        isLoadingFiles = false
                    }
                },
                onUseSystemPicker = {
                    showFilePicker = false
                    filePickerLauncher.launch("image/svg+xml")
                },
                onRequestPermission = {
                    requestStoragePermission()
                }
            )
        }
    }
    
    private fun getFileName(uri: Uri): String? {
        var name: String? = null
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    name = it.getString(nameIndex)
                }
            }
        }
        return name
    }
    
    private fun readFileContent(uri: Uri): String {
        val inputStream = contentResolver.openInputStream(uri)
        val reader = BufferedReader(InputStreamReader(inputStream))
        val content = reader.readText()
        reader.close()
        inputStream?.close()
        return content
    }
    
    private fun saveFileContent(uri: Uri, content: String) {
        contentResolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.write(content.toByteArray(Charsets.UTF_8))
        }
    }
}
