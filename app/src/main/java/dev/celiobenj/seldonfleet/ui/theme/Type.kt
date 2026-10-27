package dev.celiobenj.seldonfleet.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Default = Typography()

/**
 * Tipografia padrão do Material 3 com a fonte do sistema. Única mudança: rótulos pequenos
 * sobem de 11 para 12 sp (mínimo para rótulos); o corpo de texto usa no mínimo 14 sp.
 */
val Typography = Default.copy(
    labelSmall = Default.labelSmall.copy(fontSize = 12.sp, lineHeight = 16.sp),
)

/** IDs de dispositivo: monoespaçada do sistema, com ênfase. */
val DeviceIdStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Bold,
    fontSize = 16.sp,
    lineHeight = 22.sp,
)

/** Rótulos técnicos em caixa alta (chips, subtítulos, títulos de atributo). */
val MonoLabelStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Medium,
    fontSize = 12.sp,
    lineHeight = 16.sp,
    letterSpacing = 0.5.sp,
)
