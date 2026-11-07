package com.example.ui.components

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.data.WidgetShapeStyle
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object WidgetCustomShapes {

  fun getShape(style: WidgetShapeStyle): Shape {
    return when (style) {
      WidgetShapeStyle.SQUIRCLE -> RoundedCornerShape(28.dp)
      WidgetShapeStyle.ROUNDED -> RoundedCornerShape(16.dp)
      WidgetShapeStyle.PILL -> RoundedCornerShape(50)
      WidgetShapeStyle.CIRCLE -> CircleShape
      WidgetShapeStyle.HEXAGON -> HexagonWidgetShape(cornerSmoothing = 0.18f)
      WidgetShapeStyle.FLOWER -> ScallopedFlowerWidgetShape(petals = 12)
    }
  }
}

/**
 * Rounded Hexagon Shape inspired by Android Material 3 geometric shapes.
 * Adapts gracefully to square or rectangular bounds.
 */
class HexagonWidgetShape(private val cornerSmoothing: Float = 0.18f) : Shape {
  override fun createOutline(
    size: Size,
    layoutDirection: LayoutDirection,
    density: Density
  ): Outline {
    val path = Path()
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h / 2f
    val rx = w / 2f
    val ry = h / 2f

    // 6 vertices of a hexagon stretched to the bounding box
    val points = (0 until 6).map { i ->
      val angle = (PI / 3.0) * i - (PI / 6.0)
      Pair(
        cx + (rx * cos(angle)).toFloat(),
        cy + (ry * sin(angle)).toFloat()
      )
    }

    for (i in 0 until 6) {
      val prev = points[(i + 5) % 6]
      val curr = points[i]
      val next = points[(i + 1) % 6]

      val startX = curr.first + (prev.first - curr.first) * cornerSmoothing
      val startY = curr.second + (prev.second - curr.second) * cornerSmoothing
      val endX = curr.first + (next.first - curr.first) * cornerSmoothing
      val endY = curr.second + (next.second - curr.second) * cornerSmoothing

      if (i == 0) {
        path.moveTo(startX, startY)
      } else {
        path.lineTo(startX, startY)
      }
      path.quadraticTo(curr.first, curr.second, endX, endY)
    }
    path.close()
    return Outline.Generic(path)
  }
}

/**
 * Android Material You Scalloped Flower / Cookie Widget Shape.
 * Creates smooth outward-curving petals around the perimeter.
 */
class ScallopedFlowerWidgetShape(private val petals: Int = 12) : Shape {
  override fun createOutline(
    size: Size,
    layoutDirection: LayoutDirection,
    density: Density
  ): Outline {
    val path = Path()
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h / 2f
    val baseRx = w * 0.45f
    val baseRy = h * 0.45f
    val ampX = w * 0.05f
    val ampY = h * 0.05f

    val steps = 180
    for (step in 0..steps) {
      val theta = (2.0 * PI * step) / steps
      val wave = (1.0 + cos(petals * theta)) * 0.5 // 0..1 smooth wave
      val rx = baseRx + ampX * wave.toFloat()
      val ry = baseRy + ampY * wave.toFloat()

      val x = cx + rx * cos(theta).toFloat()
      val y = cy + ry * sin(theta).toFloat()

      if (step == 0) {
        path.moveTo(x, y)
      } else {
        path.lineTo(x, y)
      }
    }
    path.close()
    return Outline.Generic(path)
  }
}
