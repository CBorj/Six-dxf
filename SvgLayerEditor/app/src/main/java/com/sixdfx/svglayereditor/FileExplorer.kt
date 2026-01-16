package com.sixdfx.svglayereditor

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class SvgFileInfo(
    val name: String,
    val path: String,
    val uri: Uri?,
    val size: Long,
    val lastModified: Long
)

object FileExplorer {
    
    /**
     * Busca archivos SVG en el almacenamiento del dispositivo
     */
    suspend fun findSvgFiles(context: Context): List<SvgFileInfo> = withContext(Dispatchers.IO) {
        val svgFiles = mutableListOf<SvgFileInfo>()
        
        // Método 1: Buscar usando MediaStore (para Android 10+)
        try {
            val mediaStoreSvgs = searchMediaStore(context)
            svgFiles.addAll(mediaStoreSvgs)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // Método 2: Buscar en carpetas comunes
        try {
            val commonFoldersSvgs = searchCommonFolders()
            // Añadir solo los que no están ya en la lista
            commonFoldersSvgs.forEach { newFile ->
                if (svgFiles.none { it.path == newFile.path }) {
                    svgFiles.add(newFile)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // Ordenar por nombre
        svgFiles.sortedBy { it.name.lowercase() }
    }
    
    private fun searchMediaStore(context: Context): List<SvgFileInfo> {
        val svgFiles = mutableListOf<SvgFileInfo>()
        
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_MODIFIED
        )
        
        val selection = "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%.svg")
        
        val cursor = context.contentResolver.query(
            MediaStore.Files.getContentUri("external"),
            projection,
            selection,
            selectionArgs,
            "${MediaStore.Files.FileColumns.DISPLAY_NAME} ASC"
        )
        
        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val nameColumn = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val pathColumn = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
            val sizeColumn = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            val modifiedColumn = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
            
            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val name = it.getString(nameColumn) ?: "unknown.svg"
                val path = it.getString(pathColumn) ?: ""
                val size = it.getLong(sizeColumn)
                val modified = it.getLong(modifiedColumn)
                
                val uri = Uri.withAppendedPath(
                    MediaStore.Files.getContentUri("external"),
                    id.toString()
                )
                
                svgFiles.add(SvgFileInfo(name, path, uri, size, modified))
            }
        }
        
        return svgFiles
    }
    
    private fun searchCommonFolders(): List<SvgFileInfo> {
        val svgFiles = mutableListOf<SvgFileInfo>()
        
        val foldersToSearch = listOf(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            File(Environment.getExternalStorageDirectory(), "SVG"),
            File(Environment.getExternalStorageDirectory(), "svg")
        )
        
        foldersToSearch.forEach { folder ->
            if (folder.exists() && folder.isDirectory) {
                searchFolderRecursively(folder, svgFiles, maxDepth = 3)
            }
        }
        
        return svgFiles
    }
    
    private fun searchFolderRecursively(folder: File, results: MutableList<SvgFileInfo>, currentDepth: Int = 0, maxDepth: Int = 3) {
        if (currentDepth > maxDepth) return
        
        folder.listFiles()?.forEach { file ->
            when {
                file.isFile && file.name.endsWith(".svg", ignoreCase = true) -> {
                    results.add(
                        SvgFileInfo(
                            name = file.name,
                            path = file.absolutePath,
                            uri = Uri.fromFile(file),
                            size = file.length(),
                            lastModified = file.lastModified()
                        )
                    )
                }
                file.isDirectory && !file.name.startsWith(".") -> {
                    searchFolderRecursively(file, results, currentDepth + 1, maxDepth)
                }
            }
        }
    }
    
    /**
     * Lee el contenido de un archivo SVG
     */
    suspend fun readSvgContent(context: Context, fileInfo: SvgFileInfo): String = withContext(Dispatchers.IO) {
        // Intentar primero por URI
        fileInfo.uri?.let { uri ->
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    return@withContext inputStream.bufferedReader().readText()
                }
            } catch (e: Exception) {
                // Si falla, intentar por path
            }
        }
        
        // Intentar por path directo
        if (fileInfo.path.isNotEmpty()) {
            val file = File(fileInfo.path)
            if (file.exists()) {
                return@withContext file.readText()
            }
        }
        
        throw Exception("No se pudo leer el archivo: ${fileInfo.name}")
    }
    
    /**
     * Elimina un archivo SVG
     * @return true si se eliminó correctamente
     */
    suspend fun deleteFile(context: Context, fileInfo: SvgFileInfo): Boolean = withContext(Dispatchers.IO) {
        try {
            // Método 1: Intentar eliminar usando ContentResolver (para archivos de MediaStore)
            fileInfo.uri?.let { uri ->
                try {
                    val rowsDeleted = context.contentResolver.delete(uri, null, null)
                    if (rowsDeleted > 0) {
                        return@withContext true
                    }
                } catch (e: SecurityException) {
                    // No tenemos permiso, intentar método alternativo
                    e.printStackTrace()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            // Método 2: Intentar eliminar directamente el archivo
            if (fileInfo.path.isNotEmpty()) {
                val file = File(fileInfo.path)
                if (file.exists() && file.canWrite()) {
                    val deleted = file.delete()
                    if (deleted) {
                        // Notificar al MediaStore que el archivo fue eliminado
                        try {
                            context.contentResolver.delete(
                                MediaStore.Files.getContentUri("external"),
                                "${MediaStore.Files.FileColumns.DATA}=?",
                                arrayOf(fileInfo.path)
                            )
                        } catch (e: Exception) {
                            // Ignorar errores de MediaStore
                        }
                        return@withContext true
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext false
    }
    
    /**
     * Renombra un archivo SVG usando múltiples métodos
     * @return El nuevo SvgFileInfo con el nombre actualizado, o null si falla
     */
    suspend fun renameFile(context: Context, fileInfo: SvgFileInfo, newName: String): SvgFileInfo? = withContext(Dispatchers.IO) {
        // Asegurarnos de que el nuevo nombre termine en .svg
        val finalName = if (newName.endsWith(".svg", ignoreCase = true)) {
            newName
        } else {
            "$newName.svg"
        }
        
        try {
            if (fileInfo.path.isNotEmpty()) {
                val oldFile = File(fileInfo.path)
                if (oldFile.exists()) {
                    val newFile = File(oldFile.parent, finalName)
                    
                    // Verificar que no exista un archivo con ese nombre
                    if (newFile.exists()) {
                        return@withContext null
                    }
                    
                    val success = oldFile.renameTo(newFile)
                    if (success) {
                        // Actualizar MediaStore
                        try {
                            context.contentResolver.delete(
                                MediaStore.Files.getContentUri("external"),
                                "${MediaStore.Files.FileColumns.DATA}=?",
                                arrayOf(fileInfo.path)
                            )
                        } catch (e: Exception) {
                            // Ignorar
                        }
                        
                        return@withContext SvgFileInfo(
                            name = finalName,
                            path = newFile.absolutePath,
                            uri = Uri.fromFile(newFile),
                            size = newFile.length(),
                            lastModified = newFile.lastModified()
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return@withContext null
    }
}
