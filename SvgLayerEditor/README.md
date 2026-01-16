# SVG Layer Editor 🎨

Aplicación Android para editar capas de archivos SVG.

## Funcionalidades

- ✅ **Cargar archivos SVG** desde el dispositivo
- ✅ **Listar todas las capas** del archivo
- ✅ **Duplicar capas** existentes
- ✅ **Renombrar capas** (modificar ID)
- ✅ **Reordenar capas** con arrastrar y soltar (drag & drop)
- ✅ **Eliminar capas** duplicadas (las originales están protegidas)
- ✅ **Exportar SVG modificado**

## Capturas de pantalla

| Pantalla inicial | Editor de capas |
|------------------|-----------------|
| Logo + botón para seleccionar archivo | Lista de capas con acciones |

## Cómo compilar

### Requisitos
- Android Studio Hedgehog (2023.1.1) o superior
- JDK 17
- Android SDK 34

### Pasos

1. **Abrir en Android Studio**
   ```
   File > Open > Seleccionar carpeta SvgLayerEditor
   ```

2. **Sincronizar Gradle**
   - Android Studio detectará automáticamente el proyecto
   - Click en "Sync Now" si aparece el mensaje

3. **Compilar APK**
   ```
   Build > Build Bundle(s) / APK(s) > Build APK(s)
   ```
   
   O desde terminal:
   ```bash
   ./gradlew assembleDebug
   ```

4. **La APK estará en:**
   ```
   app/build/outputs/apk/debug/app-debug.apk
   ```

## Estructura del proyecto

```
SvgLayerEditor/
├── app/
│   ├── src/main/
│   │   ├── java/com/sixdfx/svglayereditor/
│   │   │   ├── MainActivity.kt          # Activity principal
│   │   │   ├── EditorViewModel.kt        # ViewModel con lógica
│   │   │   ├── SvgProcessor.kt           # Parser y generador SVG
│   │   │   ├── model/
│   │   │   │   └── SvgLayer.kt           # Modelos de datos
│   │   │   └── ui/
│   │   │       ├── EditorScreen.kt       # Pantalla principal
│   │   │       ├── LayerList.kt          # Lista de capas
│   │   │       └── theme/                # Tema Material 3
│   │   ├── res/
│   │   │   ├── drawable/                 # Iconos y logo
│   │   │   ├── values/                   # Strings, colores, temas
│   │   │   └── xml/                      # Configuración backup
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

## Tecnologías utilizadas

- **Kotlin** - Lenguaje principal
- **Jetpack Compose** - UI declarativa
- **Material Design 3** - Diseño moderno
- **Compose Reorderable** - Drag & drop para listas
- **ViewModel** - Arquitectura MVVM

## Uso

1. Abre la app
2. Pulsa "Seleccionar archivo SVG"
3. Elige un archivo SVG de tu dispositivo
4. Verás la lista de capas:
   - 🔵 **Azul** = Capa original (no se puede eliminar)
   - 🟠 **Naranja** = Capa duplicada
5. Acciones disponibles:
   - 📋 **Duplicar** - Crea una copia de la capa
   - ✏️ **Renombrar** - Cambia el ID de la capa
   - 🗑️ **Eliminar** - Solo para capas duplicadas
   - ↕️ **Arrastrar** - Mantén presionado para reordenar
6. Pulsa "Confirmar y Guardar" para exportar el SVG modificado

## Licencia

MIT License
