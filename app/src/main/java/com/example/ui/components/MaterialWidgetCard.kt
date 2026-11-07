package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddHome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CountdownTimer
import com.example.data.WidgetShapeStyle
import com.example.data.WidgetThemeColor
import kotlin.math.abs

data class ResolvedWidgetColors(
  val primary: Color,
  val surface: Color,
  val container: Color,
  val onSurface: Color
)

@Composable
fun resolveWidgetColors(themeColor: WidgetThemeColor): ResolvedWidgetColors {
  return if (themeColor == WidgetThemeColor.DYNAMIC) {
    val cs = MaterialTheme.colorScheme
    ResolvedWidgetColors(
      primary = cs.primary,
      surface = cs.surfaceVariant,
      container = cs.primaryContainer,
      onSurface = cs.onPrimaryContainer
    )
  } else {
    ResolvedWidgetColors(
      primary = Color(themeColor.primaryHex),
      surface = Color(themeColor.surfaceHex),
      container = Color(themeColor.containerHex),
      onSurface = Color(themeColor.onSurfaceHex)
    )
  }
}

@Composable
fun MaterialWidgetCard(
  timer: CountdownTimer,
  modifier: Modifier = Modifier,
  currentTimeMs: Long = System.currentTimeMillis(),
  onCardClick: () -> Unit = {},
  onEditClick: () -> Unit = {},
  onDeleteClick: () -> Unit = {},
  onAddToHomeScreenClick: () -> Unit = {},
  showActions: Boolean = true
) {
  val shapeStyle = timer.getResolvedShape()
  val shape = WidgetCustomShapes.getShape(shapeStyle)
  val colors = resolveWidgetColors(timer.getResolvedTheme())
  val cs = MaterialTheme.colorScheme

  val daysRemaining = timer.daysRemaining(currentTimeMs)
  val isCompleted = timer.isCompleted(currentTimeMs)
  val progress = timer.progressFraction(currentTimeMs)

  val isSymmetricShape = shapeStyle == WidgetShapeStyle.CIRCLE ||
    shapeStyle == WidgetShapeStyle.FLOWER ||
    shapeStyle == WidgetShapeStyle.HEXAGON

  // Compact 196dp 1:1 square for symmetric radial shapes so the vertical bounding box is tight
  // while still keeping all text safely inside the curved perimeter.
  val shapedCardModifier = if (isSymmetricShape) {
    Modifier.size(196.dp)
  } else {
    Modifier.fillMaxWidth()
  }

  // Snug interior padding tailored to each silhouette without excess vertical dead space
  val contentPadding = when (shapeStyle) {
    WidgetShapeStyle.CIRCLE -> PaddingValues(horizontal = 30.dp, vertical = 18.dp)
    WidgetShapeStyle.FLOWER -> PaddingValues(horizontal = 32.dp, vertical = 20.dp)
    WidgetShapeStyle.HEXAGON -> PaddingValues(horizontal = 28.dp, vertical = 18.dp)
    WidgetShapeStyle.PILL -> PaddingValues(horizontal = 42.dp, vertical = 14.dp)
    WidgetShapeStyle.SQUIRCLE -> PaddingValues(horizontal = 22.dp, vertical = 14.dp)
    WidgetShapeStyle.ROUNDED -> PaddingValues(horizontal = 20.dp, vertical = 13.dp)
  }

  val opacityAlpha = (timer.opacityPercent.coerceIn(0, 100) / 100f)

  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    // 1. Pure Shaped Widget Container (Contains ONLY countdown data, never action buttons)
    Card(
      modifier = shapedCardModifier
        .testTag("widget_card_${timer.id}")
        .clip(shape)
        .clickable { onCardClick() },
      shape = shape,
      elevation = CardDefaults.cardElevation(
        defaultElevation = (8f * opacityAlpha).dp,
        pressedElevation = (12f * opacityAlpha).dp,
        hoveredElevation = (10f * opacityAlpha).dp
      ),
      colors = CardDefaults.cardColors(
        containerColor = Color.Transparent
      ),
      border = BorderStroke(
        width = 1.5.dp,
        color = colors.primary.copy(alpha = (0.18f + 0.45f * opacityAlpha).coerceIn(0f, 1f))
      )
    ) {
      val boxModifier = if (isSymmetricShape) {
        Modifier.fillMaxSize()
      } else {
        Modifier.fillMaxWidth()
      }

      Box(
        modifier = boxModifier
          .background(
            brush = Brush.linearGradient(
              colors = listOf(
                colors.container.copy(alpha = 0.88f * opacityAlpha),
                colors.surface.copy(alpha = 0.96f * opacityAlpha)
              )
            )
          )
          .padding(contentPadding),
        contentAlignment = Alignment.Center
      ) {
        if (isSymmetricShape) {
          SymmetricShapeLayout(
            timer = timer,
            currentTimeMs = currentTimeMs,
            daysRemaining = daysRemaining,
            isCompleted = isCompleted,
            progress = progress,
            colors = colors
          )
        } else {
          StandardWidgetLayout(
            timer = timer,
            currentTimeMs = currentTimeMs,
            daysRemaining = daysRemaining,
            isCompleted = isCompleted,
            progress = progress,
            colors = colors,
            isPillShape = shapeStyle == WidgetShapeStyle.PILL
          )
        }
      }
    }

    // 2. External Action Bar — Strictly OUTSIDE and UNDERNEATH the widget shape
    if (showActions) {
      Spacer(modifier = Modifier.height(6.dp))

      val buttonTextColor = if (colors.primary.luminance() > 0.45f) {
        Color(0xFF090D16)
      } else {
        Color.White
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Button(
          onClick = onAddToHomeScreenClick,
          modifier = Modifier
            .weight(1f)
            .height(40.dp)
            .testTag("add_to_home_screen_${timer.id}"),
          shape = RoundedCornerShape(14.dp),
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = colors.primary,
            contentColor = buttonTextColor
          )
        ) {
          Icon(
            imageVector = Icons.Default.AddHome,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Add to Home Screen",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
          )
        }

        IconButton(
          onClick = onEditClick,
          modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(cs.surfaceVariant)
            .border(1.dp, cs.outline.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .testTag("edit_timer_${timer.id}")
        ) {
          Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = "Edit timer",
            tint = cs.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
          )
        }

        IconButton(
          onClick = onDeleteClick,
          modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(cs.surfaceVariant)
            .border(1.dp, cs.outline.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .testTag("delete_timer_${timer.id}")
        ) {
          Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = "Delete timer",
            tint = cs.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
          )
        }
      }
    }
  }
}

@Composable
private fun SymmetricShapeLayout(
  timer: CountdownTimer,
  currentTimeMs: Long,
  daysRemaining: Long,
  isCompleted: Boolean,
  progress: Float,
  colors: ResolvedWidgetColors
) {
  val displayDays = if (isCompleted) abs(daysRemaining) else daysRemaining

  Column(
    modifier = Modifier.fillMaxSize(),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Text(
      text = timer.title,
      fontSize = 12.sp,
      fontWeight = FontWeight.Bold,
      color = colors.onSurface,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
      textAlign = TextAlign.Center,
      lineHeight = 14.sp,
      modifier = Modifier.fillMaxWidth(0.92f)
    )

    if (timer.subtitle.isNotBlank()) {
      Text(
        text = timer.subtitle,
        fontSize = 10.sp,
        color = colors.onSurface.copy(alpha = 0.72f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        modifier = Modifier
          .fillMaxWidth(0.88f)
          .padding(top = 1.dp)
      )
    }

    Spacer(modifier = Modifier.height(2.dp))

    Text(
      text = displayDays.toString(),
      fontSize = 34.sp,
      fontWeight = FontWeight.Black,
      color = colors.primary,
      lineHeight = 36.sp,
      modifier = Modifier.testTag("days_remaining_text_${timer.id}")
    )

    Text(
      text = when {
        isCompleted && displayDays == 0L -> "TODAY!"
        isCompleted -> "DAYS AGO"
        displayDays == 1L -> "DAY LEFT"
        else -> "DAYS LEFT"
      },
      fontSize = 9.sp,
      fontWeight = FontWeight.Bold,
      color = if (isCompleted) Color(0xFFF87171) else colors.onSurface.copy(alpha = 0.85f),
      letterSpacing = 0.8.sp
    )

    Spacer(modifier = Modifier.height(4.dp))

    LinearProgressIndicator(
      progress = { progress },
      modifier = Modifier
        .fillMaxWidth(0.68f)
        .height(4.dp)
        .clip(RoundedCornerShape(2.dp)),
      color = colors.primary,
      trackColor = colors.onSurface.copy(alpha = 0.18f)
    )

    val footerText = timer.formattedCountdownFooter(nowMs = currentTimeMs, compact = true)

    if (footerText.isNotBlank()) {
      Spacer(modifier = Modifier.height(3.dp))
      Text(
        text = footerText,
        fontSize = 9.sp,
        color = colors.onSurface.copy(alpha = 0.78f),
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
          .fillMaxWidth(0.85f)
          .testTag("widget_footer_text_${timer.id}")
      )
    }
  }
}

@Composable
private fun StandardWidgetLayout(
  timer: CountdownTimer,
  currentTimeMs: Long,
  daysRemaining: Long,
  isCompleted: Boolean,
  progress: Float,
  colors: ResolvedWidgetColors,
  isPillShape: Boolean
) {
  Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = if (isPillShape) Alignment.CenterHorizontally else Alignment.Start
  ) {
    // Top Title & Optional Subtitle
    Text(
      text = timer.title,
      fontSize = 15.sp,
      fontWeight = FontWeight.Bold,
      color = colors.onSurface,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      textAlign = if (isPillShape) TextAlign.Center else TextAlign.Start,
      modifier = Modifier.fillMaxWidth()
    )

    if (timer.subtitle.isNotBlank()) {
      Text(
        text = timer.subtitle,
        fontSize = 11.sp,
        color = colors.onSurface.copy(alpha = 0.72f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = if (isPillShape) TextAlign.Center else TextAlign.Start,
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 1.dp)
      )
    }

    Spacer(modifier = Modifier.height(4.dp))

    // Main Counter Display
    val displayDays = if (isCompleted) abs(daysRemaining) else daysRemaining
    Row(
      verticalAlignment = Alignment.Bottom,
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Text(
        text = displayDays.toString(),
        fontSize = 40.sp,
        fontWeight = FontWeight.ExtraBold,
        color = colors.primary,
        lineHeight = 40.sp,
        modifier = Modifier.testTag("days_remaining_text_${timer.id}")
      )

      Text(
        text = when {
          isCompleted && displayDays == 0L -> "TODAY!"
          isCompleted -> "DAYS AGO"
          displayDays == 1L -> "DAY LEFT"
          else -> "DAYS LEFT"
        },
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = if (isCompleted) Color(0xFFF87171) else colors.onSurface.copy(alpha = 0.9f),
        letterSpacing = 1.sp,
        modifier = Modifier.padding(bottom = 5.dp)
      )
    }

    Spacer(modifier = Modifier.height(6.dp))

    // Progress Bar (narrower inside Pill so semicircular caps never clip it)
    LinearProgressIndicator(
      progress = { progress },
      modifier = Modifier
        .fillMaxWidth(if (isPillShape) 0.86f else 1f)
        .height(5.dp)
        .clip(RoundedCornerShape(3.dp)),
      color = colors.primary,
      trackColor = colors.onSurface.copy(alpha = 0.18f)
    )

    Spacer(modifier = Modifier.height(4.dp))

    // Bottom info row: Optional countdown date / fraction of days completed & Progress %
    val bottomDateText = timer.formattedCountdownFooter(nowMs = currentTimeMs, compact = false)

    Row(
      modifier = Modifier.fillMaxWidth(if (isPillShape) 0.86f else 1f),
      horizontalArrangement = if (bottomDateText.isNotBlank()) {
        Arrangement.SpaceBetween
      } else if (isPillShape) {
        Arrangement.Center
      } else {
        Arrangement.End
      },
      verticalAlignment = Alignment.CenterVertically
    ) {
      if (bottomDateText.isNotBlank()) {
        Text(
          text = bottomDateText,
          fontSize = 11.sp,
          color = colors.onSurface.copy(alpha = 0.75f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier
            .weight(1f)
            .padding(end = 8.dp)
            .testTag("widget_footer_text_${timer.id}")
        )
      }

      Text(
        text = "${(progress * 100).toInt()}%",
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = colors.primary
      )
    }
  }
}
