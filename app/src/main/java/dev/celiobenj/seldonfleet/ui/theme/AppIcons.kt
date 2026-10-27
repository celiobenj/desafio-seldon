package dev.celiobenj.seldonfleet.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Ícones próprios do app, desenhados como vetores em grade de 24 dp com traço de 2 dp e
 * pontas arredondadas. A cor final vem do `tint` do componente Icon.
 */
object AppIcons {

    val CheckCircle: ImageVector by lazy {
        strokeIcon("CheckCircle") {
            circle()
            moveTo(8f, 12.5f)
            lineTo(11f, 15.5f)
            lineTo(16.5f, 9f)
        }
    }

    val AlertTriangle: ImageVector by lazy {
        strokeIcon("AlertTriangle") {
            moveTo(12f, 3.5f)
            lineTo(21.5f, 20f)
            lineTo(2.5f, 20f)
            close()
            moveTo(12f, 10f)
            lineTo(12f, 14f)
            dot(12f, 17f)
        }
    }

    val AlertCircle: ImageVector by lazy {
        strokeIcon("AlertCircle") {
            circle()
            moveTo(12f, 7.5f)
            lineTo(12f, 13f)
            dot(12f, 16.5f)
        }
    }

    val HelpCircle: ImageVector by lazy {
        strokeIcon("HelpCircle") {
            circle()
            moveTo(9.5f, 9.5f)
            arcTo(2.5f, 2.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 13f, y1 = 11.8f)
            quadTo(12f, 12.3f, 12f, 13.5f)
            lineTo(12f, 14f)
            dot(12f, 17f)
        }
    }

    val StarFilled: ImageVector by lazy { starIcon("StarFilled", filled = true) }

    val StarOutline: ImageVector by lazy { starIcon("StarOutline", filled = false) }

    val ArrowBack: ImageVector by lazy {
        strokeIcon("ArrowBack") {
            moveTo(19f, 12f)
            lineTo(5f, 12f)
            moveTo(11f, 6f)
            lineTo(5f, 12f)
            lineTo(11f, 18f)
        }
    }

    val Check: ImageVector by lazy {
        strokeIcon("Check") {
            moveTo(5f, 12.5f)
            lineTo(10f, 17.5f)
            lineTo(19f, 7f)
        }
    }

    val Refresh: ImageVector by lazy {
        strokeIcon("Refresh") {
            moveTo(19.5f, 12f)
            arcTo(7.5f, 7.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 17.3f, y1 = 6.7f)
            moveTo(19.5f, 4.5f)
            lineTo(19.5f, 9f)
            lineTo(15f, 9f)
        }
    }

    // ---------- construção ----------

    private fun strokeIcon(name: String, block: PathBuilder.() -> Unit): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                pathBuilder = block,
            )
        }.build()

    /** Estrela de 5 pontas calculada (raio externo 9,5; interno 4,2), cheia ou só contorno. */
    private fun starIcon(name: String, filled: Boolean): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            path(
                fill = if (filled) SolidColor(Color.Black) else null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                for (i in 0 until 10) {
                    val radius = if (i % 2 == 0) 9.5f else 4.2f
                    val angle = -PI / 2 + i * PI / 5
                    val x = 12f + radius * cos(angle).toFloat()
                    val y = 12.8f + radius * sin(angle).toFloat()
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }
        }.build()

    /** Círculo de raio 9 centrado na grade. */
    private fun PathBuilder.circle() {
        moveTo(12f, 3f)
        arcTo(9f, 9f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 12f, y1 = 21f)
        arcTo(9f, 9f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 12f, y1 = 3f)
        close()
    }

    /** Ponto: segmento mínimo que a ponta arredondada transforma em círculo. */
    private fun PathBuilder.dot(x: Float, y: Float) {
        moveTo(x, y)
        lineTo(x + 0.01f, y)
    }
}
