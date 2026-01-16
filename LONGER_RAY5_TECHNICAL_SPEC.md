# Longer Ray5 Mini - Especificación Técnica para Desarrollo

## 1. CONEXIÓN

### 1.1 Modos WiFi

| Modo | SSID | Password | IP | Puerto |
|------|------|----------|----|----|
| **AP** (Access Point) | `LongerLaser_XXXX` | `12345678` | `192.168.0.1` | `8847` |
| **STA** (Cliente Router) | Tu red WiFi | Tu password | DHCP (ej: 192.168.1.xxx) | `8847` |

### 1.2 Protocolo
- **Tipo**: TCP Socket (texto plano)
- **Firmware**: GRBL
- **Baud USB**: 115200
- **Transfer Mode**: Buffered

### 1.3 Comandos WiFi (via consola GRBL)
```
$radio/mode=sta
$sta/ssid=Your_SSID
$sta/password=Your_PassWord
$wifi/begin
```
Respuesta exitosa: `status = connected` + IP asignada

### 1.4 Indicadores LED
- **Naranja (respirando)**: Conectando
- **Verde**: Conectado exitosamente

---

## 2. ESPECIFICACIONES DE MÁQUINA

| Parámetro | Valor |
|-----------|-------|
| Área de trabajo | 130mm x 140mm |
| Origen | Front Left (esquina frontal izquierda) |
| S-value máximo | 1000 (para potencia láser) |
| Tipos de láser | 2.5W / 3.5W 450nm Blue light |
| Memoria interna | ~4.30 MB |
| Formato archivos | `longer_XXX` (numerados) |

---

## 3. PARÁMETROS DE GRABADO

### 3.1 Por Capa
| Parámetro | Descripción | Valores típicos |
|-----------|-------------|-----------------|
| **Mode** | Tipo de trazo | `Line` / `Fill` |
| **Image mode** | Algoritmo dithering | `Stucki` |
| **Processing method** | Método de procesado | `Engrave` / `Cut` |
| **Laser power** | Potencia del láser | 0-100% |
| **Speed** | Velocidad de movimiento | 500-10000 mm/min |
| **Times** | Número de pasadas | 1, 2, 3... |
| **Accuracy** | Resolución/precisión | `Ultra Fine`, `Fine`, `Fast`, `Ultra Fast` |

### 3.2 Intervalos de línea (Fill)
| Calidad | Line Interval |
|---------|---------------|
| Alta precisión | 0.05mm |
| Alta eficiencia | 0.10mm |

### 3.3 Border Setting (Preview)
| Parámetro | Valor típico |
|-----------|--------------|
| Speed | 1000 mm/min |
| Laser power | 2% |

---

## 4. FLUJO DE TRABAJO (LaserBurn App)

```
1. Conectar WiFi (AP o STA)
2. Añadir gráfico (Draw/Image/Text/QR)
3. Edit → Ajustar Size, Position, Rotate
4. Layer → Configurar parámetros por capa
5. Next → Preview
6. Border setting → Frame (preview del borde)
7. Start → Input file name
8. Upload → Sube archivo a memoria de la máquina
9. Confirm → Inicia grabado
10. Pause/Stop durante ejecución
```

---

## 5. CAPAS (Layers)

- Hasta **10 capas** (00-09) con colores diferentes
- Cada capa tiene sus propios parámetros
- Se pueden activar/desactivar (Output toggle)
- Columnas: Layer | Mode | Spd/Pwr | Output | Show | More

Ejemplo de configuración:
```
Layer 00: Line  6000/10.0  [ON] [ON]
Layer 01: Line  6000/10.0  [ON] [ON]
```

---

## 6. COMANDOS GRBL RELEVANTES

### 6.1 Consulta de estado
```
?                    → Estado actual
$$                   → Configuración GRBL
$X                   → Desbloquear (unlock)
$H                   → Homing
```

### 6.2 Control tiempo real
| Acción | Comando |
|--------|---------|
| Pausa | `!` |
| Reanudar | `~` |
| Reset suave | `0x18` (byte) |

### 6.3 Movimiento y láser
```
G90                  → Posicionamiento absoluto
G91                  → Posicionamiento relativo
G0 X10 Y10           → Movimiento rápido
G1 X10 Y10 F3000     → Movimiento lineal con velocidad
M4 S500              → Láser ON (modo dinámico, 50%)
M5                   → Láser OFF
```

### 6.4 Macros predefinidos en consola
- **IP**: Muestra IP actual
- **AP**: Cambia a modo Access Point
- **STA**: Cambia a modo Station (cliente)
- **Macro3, Macro4, Macro5**: Personalizables

---

## 7. CONVERSIÓN POTENCIA

```
Power % → S-value:
0%   → S0
10%  → S100
50%  → S500
100% → S1000

Fórmula: S = (Power% / 100) * 1000
```

---

## 8. ARCHIVOS EN MEMORIA

- Ubicación: Memoria interna de la máquina
- Espacio: ~4.30 MB total
- Formato nombre: `longer_001`, `longer_002`, etc.
- Se pueden ver/eliminar desde la app (File list)
- Se pueden ejecutar desde el menú de la máquina

---

## 9. FLUJO DE COMUNICACIÓN TCP

```
1. Abrir socket TCP a IP:8847
2. (Opcional) Leer banner de bienvenida
3. Enviar comando + "\n"
4. Esperar respuesta "ok" o "error:X"
5. Repetir para cada línea de G-code
6. Cerrar socket
```

### Ejemplo de streaming:
```python
for line in gcode_lines:
    socket.send(line + "\n")
    response = socket.recv()  # Esperar "ok"
    if "error" in response:
        handle_error()
```

---

## 10. NOTAS DE SEGURIDAD

⚠️ **Sin autenticación**: Cualquier cliente en la red puede enviar comandos
⚠️ **Validar siempre**: El trabajo antes de enviar
⚠️ **No exponer**: Puerto 8847 fuera de la LAN
⚠️ **Gafas de protección**: Obligatorias durante grabado
⚠️ **Verificar foco**: Antes de cada trabajo

---

## INFORMACIÓN PENDIENTE

Las siguientes páginas podrían contener información adicional útil:

1. **Página 7**: Product Specifications (specs técnicos exactos)
2. **Página 9**: Product Parts Overview (diagrama de componentes)
3. **Páginas 16-19**: Connect RAY5 mini to LaserGRBL via Wi-Fi (detalles adicionales)
4. **Páginas 20-21**: Create a Project in LaserGRBL (flujo en LaserGRBL)

---

*Documento generado para desarrollo de app Android - SVG Layer Editor*
*Fecha: Enero 2026*
