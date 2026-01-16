package com.sixdfx.svglayereditor.laser

import androidx.lifecycle.ViewModel
import com.sixdfx.svglayereditor.model.SvgLayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Estado de la pantalla de conversión
 */
data class ConversionScreenState(
    val layers: List<SvgLayer> = emptyList(),
    val layerConfigs: List<LayerLaserConfig> = emptyList(),
    val position: JobPosition = JobPosition(),
    val workAreaWidth: Float = 130f,
    val workAreaHeight: Float = 140f,
    val maxSValue: Int = 1000,
    val fileName: String = "longer_job",
    val isConverting: Boolean = false,
    val conversionResult: ConversionResult? = null,
    val showPositionDialog: Boolean = false,
    val selectedLayerForEdit: String? = null,  // ID de capa seleccionada para editar
    val errorMessage: String? = null
)

/**
 * ViewModel para la pantalla de conversión SVG → G-code
 */
class ConversionViewModel : ViewModel() {
    
    private val _state = MutableStateFlow(ConversionScreenState())
    val state: StateFlow<ConversionScreenState> = _state.asStateFlow()
    
    /**
     * Inicializa con las capas del SVG cargado
     */
    fun initialize(layers: List<SvgLayer>) {
        val configs = layers.map { layer ->
            LayerLaserConfig(
                layerId = layer.id,
                layerName = layer.name,
                enabled = true,
                mode = inferMode(layer),
                speedMmMin = 3000,
                powerPercent = 80,
                passes = 1,
                accuracy = Accuracy.FINE
            )
        }
        
        _state.update { 
            it.copy(
                layers = layers,
                layerConfigs = configs,
                conversionResult = null,
                errorMessage = null
            ) 
        }
    }
    
    /**
     * Infiere el modo de grabado basado en el contenido de la capa
     */
    private fun inferMode(layer: SvgLayer): EngraveMode {
        val content = layer.xmlContent.lowercase()
        // Si tiene fill que no sea "none", probablemente es relleno
        val hasFill = content.contains("fill=") && 
                      !content.contains("fill=\"none\"") &&
                      !content.contains("fill='none'") &&
                      !content.contains("fill:none")
        return if (hasFill) EngraveMode.FILL else EngraveMode.LINE
    }
    
    /**
     * Habilita/deshabilita una capa
     */
    fun toggleLayerEnabled(layerId: String) {
        _state.update { state ->
            val newConfigs = state.layerConfigs.map { config ->
                if (config.layerId == layerId) {
                    config.copy(enabled = !config.enabled)
                } else config
            }
            state.copy(layerConfigs = newConfigs)
        }
    }
    
    /**
     * Actualiza la configuración de una capa
     */
    fun updateLayerConfig(layerId: String, update: (LayerLaserConfig) -> LayerLaserConfig) {
        _state.update { state ->
            val newConfigs = state.layerConfigs.map { config ->
                if (config.layerId == layerId) {
                    update(config)
                } else config
            }
            state.copy(layerConfigs = newConfigs)
        }
    }
    
    /**
     * Actualiza la velocidad de una capa
     */
    fun updateSpeed(layerId: String, speed: Int) {
        val clampedSpeed = speed.coerceIn(LayerLaserConfig.MIN_SPEED, LayerLaserConfig.MAX_SPEED)
        updateLayerConfig(layerId) { it.copy(speedMmMin = clampedSpeed) }
    }
    
    /**
     * Actualiza la potencia de una capa
     */
    fun updatePower(layerId: String, power: Int) {
        val clampedPower = power.coerceIn(LayerLaserConfig.MIN_POWER, LayerLaserConfig.MAX_POWER)
        updateLayerConfig(layerId) { it.copy(powerPercent = clampedPower) }
    }
    
    /**
     * Actualiza el modo de grabado
     */
    fun updateMode(layerId: String, mode: EngraveMode) {
        updateLayerConfig(layerId) { it.copy(mode = mode) }
    }
    
    /**
     * Actualiza el método de proceso
     */
    fun updateMethod(layerId: String, method: ProcessMethod) {
        updateLayerConfig(layerId) { it.copy(method = method) }
    }
    
    /**
     * Actualiza las pasadas
     */
    fun updatePasses(layerId: String, passes: Int) {
        val clampedPasses = passes.coerceIn(LayerLaserConfig.MIN_PASSES, LayerLaserConfig.MAX_PASSES)
        updateLayerConfig(layerId) { it.copy(passes = clampedPasses) }
    }
    
    /**
     * Actualiza la precisión
     */
    fun updateAccuracy(layerId: String, accuracy: Accuracy) {
        updateLayerConfig(layerId) { it.copy(accuracy = accuracy) }
    }
    
    /**
     * Actualiza el espaciado de relleno
     */
    fun updateFillSpacing(layerId: String, spacing: Float) {
        updateLayerConfig(layerId) { it.copy(fillSpacing = spacing.coerceAtLeast(0.05f)) }
    }
    
    /**
     * Actualiza el ángulo de relleno
     */
    fun updateFillAngle(layerId: String, angle: Int) {
        updateLayerConfig(layerId) { it.copy(fillAngle = angle.coerceIn(0, 180)) }
    }
    
    /**
     * Muestra/oculta el diálogo de posición
     */
    fun togglePositionDialog(show: Boolean) {
        _state.update { it.copy(showPositionDialog = show) }
    }
    
    /**
     * Actualiza la posición del origen
     */
    fun updateOriginPosition(position: OriginPosition) {
        _state.update { state ->
            state.copy(
                position = state.position.copy(
                    originPosition = position,
                    useCustomPosition = position == OriginPosition.CUSTOM
                )
            )
        }
    }
    
    /**
     * Actualiza la posición personalizada
     */
    fun updateCustomPosition(x: Float, y: Float) {
        _state.update { state ->
            state.copy(
                position = state.position.copy(
                    customX = x.coerceIn(0f, state.workAreaWidth),
                    customY = y.coerceIn(0f, state.workAreaHeight),
                    useCustomPosition = true,
                    originPosition = OriginPosition.CUSTOM
                )
            )
        }
    }
    
    /**
     * Actualiza el nombre del archivo
     */
    fun updateFileName(name: String) {
        _state.update { it.copy(fileName = name) }
    }
    
    /**
     * Selecciona una capa para editar
     */
    fun selectLayerForEdit(layerId: String?) {
        _state.update { it.copy(selectedLayerForEdit = layerId) }
    }
    
    /**
     * Ejecuta la conversión
     */
    fun convert() {
        _state.update { it.copy(isConverting = true, errorMessage = null) }
        
        val currentState = _state.value
        val jobConfig = LaserJobConfig(
            layerConfigs = currentState.layerConfigs,
            position = currentState.position,
            workAreaWidth = currentState.workAreaWidth,
            workAreaHeight = currentState.workAreaHeight,
            maxSValue = currentState.maxSValue,
            fileName = currentState.fileName
        )
        
        val result = SvgToGcode.convert(currentState.layers, jobConfig)
        
        _state.update { 
            it.copy(
                isConverting = false,
                conversionResult = result,
                errorMessage = if (!result.success) result.errorMessage else null
            ) 
        }
    }
    
    /**
     * Limpia el resultado de conversión
     */
    fun clearResult() {
        _state.update { it.copy(conversionResult = null, errorMessage = null) }
    }
    
    /**
     * Obtiene el G-code generado
     */
    fun getGcode(): String? {
        return _state.value.conversionResult?.gcode
    }
    
    /**
     * Copia configuración de una capa a todas las demás
     */
    fun copyConfigToAll(sourceLayerId: String) {
        _state.update { state ->
            val sourceConfig = state.layerConfigs.find { it.layerId == sourceLayerId } ?: return@update state
            
            val newConfigs = state.layerConfigs.map { config ->
                if (config.layerId != sourceLayerId) {
                    config.copy(
                        mode = sourceConfig.mode,
                        method = sourceConfig.method,
                        speedMmMin = sourceConfig.speedMmMin,
                        powerPercent = sourceConfig.powerPercent,
                        passes = sourceConfig.passes,
                        accuracy = sourceConfig.accuracy,
                        fillAngle = sourceConfig.fillAngle,
                        fillSpacing = sourceConfig.fillSpacing
                    )
                } else config
            }
            state.copy(layerConfigs = newConfigs)
        }
    }
}
