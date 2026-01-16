package com.sixdfx.svglayereditor.laser

/**
 * Modo de grabado para cada capa
 */
enum class EngraveMode {
    LINE,       // Vectorial - sigue los contornos
    FILL        // Relleno - rellena áreas cerradas con líneas
}

/**
 * Método de procesamiento
 */
enum class ProcessMethod {
    ENGRAVE,    // Grabar (menor potencia)
    CUT         // Cortar (mayor potencia, múltiples pasadas)
}

/**
 * Precisión/Resolución del grabado
 */
enum class Accuracy(val stepMm: Float, val displayName: String) {
    ULTRA_FINE(0.05f, "Ultra Fine"),   // 0.05mm paso
    FINE(0.1f, "Fine"),                 // 0.1mm paso
    FAST(0.2f, "Fast"),                 // 0.2mm paso
    ULTRA_FAST(0.3f, "Ultra Fast")      // 0.3mm paso
}

/**
 * Configuración de grabado para una capa individual
 */
data class LayerLaserConfig(
    val layerId: String,
    val layerName: String,
    val enabled: Boolean = true,                    // Si esta capa se incluye en el grabado
    val mode: EngraveMode = EngraveMode.LINE,       // Vectorial o relleno
    val method: ProcessMethod = ProcessMethod.ENGRAVE,
    val speedMmMin: Int = 3000,                     // Velocidad en mm/min (500-6000)
    val powerPercent: Int = 80,                     // Potencia 0-100%
    val passes: Int = 1,                            // Número de pasadas
    val accuracy: Accuracy = Accuracy.FINE,         // Precisión/resolución
    val fillAngle: Int = 0,                         // Ángulo de relleno (0-180) solo para FILL
    val fillSpacing: Float = 0.1f                   // Espaciado de líneas de relleno en mm
) {
    /**
     * Calcula el S-value para GRBL (0-1000)
     */
    fun sValue(maxSValue: Int = 1000): Int {
        return (powerPercent * maxSValue) / 100
    }
    
    companion object {
        const val MIN_SPEED = 500
        const val MAX_SPEED = 6000
        const val MIN_POWER = 0
        const val MAX_POWER = 100
        const val MIN_PASSES = 1
        const val MAX_PASSES = 10
    }
}

/**
 * Posición de origen para el grabado
 */
enum class OriginPosition(val displayName: String) {
    TOP_LEFT("Superior Izquierda"),
    TOP_CENTER("Superior Centro"),
    TOP_RIGHT("Superior Derecha"),
    CENTER_LEFT("Centro Izquierda"),
    CENTER("Centro"),
    CENTER_RIGHT("Centro Derecha"),
    BOTTOM_LEFT("Inferior Izquierda"),
    BOTTOM_CENTER("Inferior Centro"),
    BOTTOM_RIGHT("Inferior Derecha"),
    CUSTOM("Personalizado")
}

/**
 * Configuración de posicionamiento del trabajo
 */
data class JobPosition(
    val originPosition: OriginPosition = OriginPosition.BOTTOM_LEFT,
    val customX: Float = 0f,            // Posición X personalizada en mm
    val customY: Float = 0f,            // Posición Y personalizada en mm
    val useCustomPosition: Boolean = false
) {
    /**
     * Calcula el offset real basado en el tamaño de la imagen y área de trabajo
     */
    fun calculateOffset(
        imageWidth: Float,
        imageHeight: Float,
        workAreaWidth: Float = 130f,
        workAreaHeight: Float = 140f
    ): Pair<Float, Float> {
        if (useCustomPosition) {
            return Pair(customX, customY)
        }
        
        return when (originPosition) {
            OriginPosition.TOP_LEFT -> Pair(0f, workAreaHeight - imageHeight)
            OriginPosition.TOP_CENTER -> Pair((workAreaWidth - imageWidth) / 2, workAreaHeight - imageHeight)
            OriginPosition.TOP_RIGHT -> Pair(workAreaWidth - imageWidth, workAreaHeight - imageHeight)
            OriginPosition.CENTER_LEFT -> Pair(0f, (workAreaHeight - imageHeight) / 2)
            OriginPosition.CENTER -> Pair((workAreaWidth - imageWidth) / 2, (workAreaHeight - imageHeight) / 2)
            OriginPosition.CENTER_RIGHT -> Pair(workAreaWidth - imageWidth, (workAreaHeight - imageHeight) / 2)
            OriginPosition.BOTTOM_LEFT -> Pair(0f, 0f)
            OriginPosition.BOTTOM_CENTER -> Pair((workAreaWidth - imageWidth) / 2, 0f)
            OriginPosition.BOTTOM_RIGHT -> Pair(workAreaWidth - imageWidth, 0f)
            OriginPosition.CUSTOM -> Pair(customX, customY)
        }
    }
}

/**
 * Configuración global del trabajo de grabado
 */
data class LaserJobConfig(
    val layerConfigs: List<LayerLaserConfig> = emptyList(),
    val position: JobPosition = JobPosition(),
    val workAreaWidth: Float = 130f,        // Área de trabajo en mm
    val workAreaHeight: Float = 140f,
    val maxSValue: Int = 1000,              // S-value máximo del láser
    val safeHeight: Float = 0f,             // Altura segura Z (para láser siempre 0)
    val fileName: String = "longer_job"     // Nombre del archivo a guardar en la máquina
)

/**
 * Resultado de la conversión
 */
data class ConversionResult(
    val success: Boolean,
    val gcode: String = "",
    val lineCount: Int = 0,
    val estimatedTimeMinutes: Float = 0f,
    val boundingBox: BoundingBox? = null,
    val errorMessage: String? = null
)

/**
 * Caja delimitadora del diseño
 */
data class BoundingBox(
    val minX: Float,
    val minY: Float,
    val maxX: Float,
    val maxY: Float
) {
    val width: Float get() = maxX - minX
    val height: Float get() = maxY - minY
    val centerX: Float get() = minX + width / 2
    val centerY: Float get() = minY + height / 2
}
