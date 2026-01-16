package com.sixdfx.svglayereditor

import com.sixdfx.svglayereditor.model.SvgLayer
import java.util.regex.Pattern

/**
 * Parser y generador de SVG - Versión robusta con regex
 * No usa XmlPullParser para evitar errores con SVGs mal formados
 */
object SvgProcessor {
    
    /**
     * Parsea un archivo SVG y extrae las capas (elementos hijos directos del <svg>)
     */
    fun parseSvg(content: String): Triple<String, List<SvgLayer>, String> {
        val layers = mutableListOf<SvgLayer>()
        
        // Limpiar el contenido de posibles problemas
        val cleanContent = content
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .trim()
        
        // Extraer el header (<?xml...?> y <svg ...>)
        val svgOpenPattern = Pattern.compile("^([\\s\\S]*?<svg[^>]*>)", Pattern.MULTILINE)
        val svgOpenMatcher = svgOpenPattern.matcher(cleanContent)
        
        val header = if (svgOpenMatcher.find()) {
            svgOpenMatcher.group(1) ?: ""
        } else {
            // Si no encuentra, intentar crear uno básico
            "<svg xmlns=\"http://www.w3.org/2000/svg\">"
        }
        
        // Extraer el contenido entre <svg> y </svg>
        val svgContentPattern = Pattern.compile("<svg[^>]*>([\\s\\S]*)</svg>", Pattern.MULTILINE)
        val svgContentMatcher = svgContentPattern.matcher(cleanContent)
        
        if (svgContentMatcher.find()) {
            val innerContent = svgContentMatcher.group(1) ?: ""
            
            // Extraer elementos de nivel superior
            val extractedLayers = extractTopLevelElements(innerContent)
            layers.addAll(extractedLayers)
        }
        
        return Triple(header, layers, "</svg>")
    }
    
    /**
     * Extrae elementos de nivel superior del contenido SVG
     */
    private fun extractTopLevelElements(content: String): List<SvgLayer> {
        val layers = mutableListOf<SvgLayer>()
        var layerIndex = 0
        
        // Tags SVG comunes que queremos extraer
        val tagNames = listOf("g", "path", "rect", "circle", "ellipse", "line", 
            "polyline", "polygon", "text", "image", "use", "defs", 
            "clipPath", "mask", "pattern", "linearGradient", "radialGradient",
            "symbol", "style", "metadata", "title", "desc")
        
        var position = 0
        
        while (position < content.length) {
            // Buscar el próximo tag de apertura
            val nextTagMatch = findNextTag(content, position, tagNames)
            
            if (nextTagMatch == null) {
                break
            }
            
            val (tagName, tagStart, isSelfClosing) = nextTagMatch
            
            if (isSelfClosing) {
                // Tag auto-cerrado como <path ... />
                val tagEnd = content.indexOf("/>", tagStart) + 2
                if (tagEnd > 1) {
                    val elementContent = content.substring(tagStart, tagEnd)
                    val id = extractId(elementContent) ?: "layer_$layerIndex"
                    
                    layers.add(SvgLayer(
                        originalId = id,
                        name = id,
                        tagName = tagName,
                        xmlContent = elementContent.trim(),
                        isOriginal = true
                    ))
                    layerIndex++
                    position = tagEnd
                } else {
                    position = tagStart + 1
                }
            } else {
                // Tag con cierre como <g>...</g>
                val elementEnd = findClosingTag(content, tagStart, tagName)
                if (elementEnd > tagStart) {
                    val elementContent = content.substring(tagStart, elementEnd)
                    val id = extractId(elementContent) ?: "layer_$layerIndex"
                    
                    layers.add(SvgLayer(
                        originalId = id,
                        name = id,
                        tagName = tagName,
                        xmlContent = elementContent.trim(),
                        isOriginal = true
                    ))
                    layerIndex++
                    position = elementEnd
                } else {
                    position = tagStart + 1
                }
            }
        }
        
        return layers
    }
    
    /**
     * Busca el próximo tag de la lista
     */
    private fun findNextTag(content: String, startPos: Int, tagNames: List<String>): Triple<String, Int, Boolean>? {
        var bestMatch: Triple<String, Int, Boolean>? = null
        var bestPos = Int.MAX_VALUE
        
        for (tagName in tagNames) {
            // Buscar <tagName (con espacio o >)
            val patterns = listOf("<$tagName ", "<$tagName>", "<$tagName/")
            
            for (pattern in patterns) {
                val pos = content.indexOf(pattern, startPos, ignoreCase = true)
                if (pos >= 0 && pos < bestPos) {
                    // Verificar si es auto-cerrado
                    val tagEndSearch = content.indexOf(">", pos)
                    val isSelfClosing = if (tagEndSearch > pos) {
                        content.substring(pos, tagEndSearch + 1).contains("/>")
                    } else {
                        false
                    }
                    
                    bestPos = pos
                    bestMatch = Triple(tagName, pos, isSelfClosing)
                }
            }
        }
        
        return bestMatch
    }
    
    /**
     * Encuentra el tag de cierre correspondiente
     */
    private fun findClosingTag(content: String, startPos: Int, tagName: String): Int {
        var depth = 0
        var pos = startPos
        
        // Primero pasar el tag de apertura
        val firstClose = content.indexOf(">", pos)
        if (firstClose < 0) return content.length
        pos = firstClose + 1
        
        while (pos < content.length) {
            // Buscar próximo < 
            val nextBracket = content.indexOf("<", pos)
            if (nextBracket < 0) break
            
            // Verificar si es tag de cierre
            val closeTag = "</$tagName>"
            val closeTagAlt = "</$tagName "
            
            if (content.regionMatches(nextBracket, closeTag, 0, closeTag.length, ignoreCase = true) ||
                content.regionMatches(nextBracket, closeTagAlt, 0, closeTagAlt.length, ignoreCase = true)) {
                // Es un tag de cierre
                if (depth == 0) {
                    // Encontrar el final del tag de cierre
                    val tagEnd = content.indexOf(">", nextBracket)
                    return if (tagEnd > 0) tagEnd + 1 else nextBracket
                }
                depth--
                pos = nextBracket + tagName.length + 3
            } else {
                // Verificar si es tag de apertura del mismo tipo
                val openTag = "<$tagName "
                val openTag2 = "<$tagName>"
                
                if (content.regionMatches(nextBracket, openTag, 0, openTag.length, ignoreCase = true) ||
                    content.regionMatches(nextBracket, openTag2, 0, openTag2.length, ignoreCase = true)) {
                    // Verificar si no es auto-cerrado
                    val tagEndPos = content.indexOf(">", nextBracket)
                    if (tagEndPos > 0) {
                        val tagContent = content.substring(nextBracket, tagEndPos + 1)
                        if (!tagContent.contains("/>")) {
                            depth++
                        }
                    }
                    pos = tagEndPos + 1
                } else {
                    // Otro tag, saltar
                    pos = nextBracket + 1
                }
            }
        }
        
        // No encontrado, devolver fin del contenido
        return content.length
    }
    
    /**
     * Extrae el ID de un elemento
     */
    private fun extractId(elementContent: String): String? {
        val idPattern = Pattern.compile("\\bid\\s*=\\s*[\"']([^\"']*)[\"']", Pattern.CASE_INSENSITIVE)
        val matcher = idPattern.matcher(elementContent)
        return if (matcher.find()) matcher.group(1) else null
    }
    
    /**
     * Genera el SVG final con las capas modificadas
     */
    fun generateSvg(header: String, layers: List<SvgLayer>, footer: String): String {
        val builder = StringBuilder()
        builder.append(header)
        builder.append("\n")
        
        layers.forEach { layer ->
            // Actualizar el ID en el contenido XML si el nombre cambió
            var xmlContent = layer.xmlContent
            if (layer.name != layer.originalId) {
                // Reemplazar el ID en el contenido
                val idPattern = Pattern.compile("\\bid\\s*=\\s*[\"'][^\"']*[\"']", Pattern.CASE_INSENSITIVE)
                val matcher = idPattern.matcher(xmlContent)
                xmlContent = if (matcher.find()) {
                    matcher.replaceFirst("id=\"${layer.name}\"")
                } else {
                    // Añadir ID si no existe
                    xmlContent.replaceFirst("<${layer.tagName}", "<${layer.tagName} id=\"${layer.name}\"")
                }
            }
            builder.append("  ")
            builder.append(xmlContent)
            builder.append("\n")
        }
        
        builder.append(footer)
        return builder.toString()
    }
    
    /**
     * Duplica una capa con un nuevo ID
     */
    fun duplicateLayer(layer: SvgLayer, newName: String): SvgLayer {
        var newXmlContent = layer.xmlContent
        
        // Actualizar el ID en el XML
        val idPattern = Pattern.compile("\\bid\\s*=\\s*[\"'][^\"']*[\"']", Pattern.CASE_INSENSITIVE)
        val matcher = idPattern.matcher(newXmlContent)
        newXmlContent = if (matcher.find()) {
            matcher.replaceFirst("id=\"$newName\"")
        } else {
            newXmlContent.replaceFirst("<${layer.tagName}", "<${layer.tagName} id=\"$newName\"")
        }
        
        return SvgLayer(
            originalId = newName,
            name = newName,
            tagName = layer.tagName,
            xmlContent = newXmlContent,
            isOriginal = false,
            parentOriginalId = layer.originalId
        )
    }
}
