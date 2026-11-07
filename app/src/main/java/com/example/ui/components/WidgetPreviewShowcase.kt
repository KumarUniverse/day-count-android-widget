package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddHome
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CountdownTimer
import com.example.data.WidgetShapeStyle
import com.example.data.WidgetThemeColor
import kotlin.math.roundToInt

enum class WallpaperTheme(
  val displayName: String,
  val darkGradient: List<Color>,
  val lightGradient: List<Color>
) {
  SAPPHIRE(
    displayName = "Sky / Midnight",
    darkGradient = listOf(Color(0xFF0F172A), Color(0xFF1E1B4B), Color(0xFF090D16)),
    lightGradient = listOf(Color(0xFFE0F2FE), Color(0xFFDBEAFE), Color(0xFFF1F5F9))
  ),
  SUNSET(
    displayName = "Rose / Dusk",
    darkGradient = listOf(Color(0xFF4C0519), Color(0xFF831843), Color(0xFF18181B)),
    lightGradient = listOf(Color(0xFFFCE7F3), Color(0xFFF3E8FF), Color(0xFFFFF1F2))
  ),
  EMERALD(
    displayName = "Mint / Forest",
    darkGradient = listOf(Color(0xFF064E3B), Color(0xFF062D24), Color(0xFF021712)),
    lightGradient = listOf(Color(0xFFD1FAE5), Color(0xFFECFDF5), Color(0xFFF0FDF4))
  ),
  OBSIDIAN(
    displayName = "Alabaster / Obsidian",
    darkGradient = listOf(Color(0xFF18181B), Color(0xFF27272A), Color(0xFF09090B)),
    lightGradient = listOf(Color(0xFFF8FAFC), Color(0xFFE2E8F0), Color(0xFFF1F5F9))
  );

  fun gradientForTheme(isLightTheme: Boolean): List<Color> {
    return if (isLightTheme) lightGradient else darkGradient
  }
}

@Composable
fun WidgetPreviewShowcase(
  timer: CountdownTimer?,
  currentTimeMs: Long,
  onUpdateTimerQuickly: (CountdownTimer) -> Unit,
  onAddToHomeScreen: (CountdownTimer) -> Unit,
  onEditTimer: (CountdownTimer) -> Unit,
  onOpenHomeGuide: () -> Unit,
  modifier: Modifier = Modifier
) {
  var selectedWallpaper by remember { mutableStateOf(WallpaperTheme.SAPPHIRE) }
  val cs = MaterialTheme.colorScheme
  val isLightAppTheme = cs.background.luminance() > 0.5f

  if (timer == null) {
    Card(
      modifier = modifier.fillMaxWidth(),
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = cs.surfaceVariant)
    ) {
      Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text("No timer selected for preview", color = cs.onSurfaceVariant)
      }
    }
    return
  }

  val currentShape = timer.getResolvedShape()
  val currentColor = timer.getResolvedTheme()
  val activeWallpaperGradient = selectedWallpaper.gradientForTheme(isLightAppTheme)
  val statusBarTextColor = if (isLightAppTheme) Color(0xFF0F172A) else Color.White

  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("widget_preview_canvas_card"),
    shape = RoundedCornerShape(28.dp),
    colors = CardDefaults.cardColors(containerColor = cs.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    border = BorderStroke(1.dp, cs.outline.copy(alpha = 0.4f))
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      // Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Surface(
            shape = CircleShape,
            color = cs.primary.copy(alpha = 0.15f),
            modifier = Modifier.size(32.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Default.AddHome,
                contentDescription = null,
                tint = cs.primary,
                modifier = Modifier.size(18.dp)
              )
            }
          }
          Column {
            Text(
              text = "Live Android Widget Preview",
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              color = cs.onSurface
            )
            Text(
              text = "Tap any shape, color, or opacity to style '${timer.title}'",
              fontSize = 11.sp,
              color = cs.onSurfaceVariant
            )
          }
        }

        IconButton(
          onClick = onOpenHomeGuide,
          modifier = Modifier.testTag("home_screen_guide_button")
        ) {
          Icon(
            imageVector = Icons.Default.Info,
            contentDescription = "Widget installation guide",
            tint = cs.onSurfaceVariant
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Home Screen Mockup Canvas (Adapts to Light Theme / Dark Theme automatically)
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("live_widget_preview_backdrop")
          .clip(RoundedCornerShape(24.dp))
          .background(
            brush = Brush.radialGradient(
              colors = activeWallpaperGradient,
              radius = 900f
            )
          )
          .border(
            width = 1.dp,
            color = cs.outline.copy(alpha = 0.35f),
            shape = RoundedCornerShape(24.dp)
          )
          .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.fillMaxWidth()
        ) {
          // Mock status bar / launcher clock
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 8.dp, start = 8.dp, end = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "9:41",
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold,
              color = statusBarTextColor.copy(alpha = 0.75f)
            )
            Text(
              text = "5G • 100%",
              fontSize = 10.sp,
              color = statusBarTextColor.copy(alpha = 0.65f)
            )
          }

          // The Widget Card itself - automatically adapts when shapeStyle or colorSchemeId changes
          AnimatedContent(
            targetState = Pair(timer.shapeStyle, timer.colorSchemeId),
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "widgetShapeAnim"
          ) { _ ->
            MaterialWidgetCard(
              timer = timer,
              currentTimeMs = currentTimeMs,
              showActions = false,
              onCardClick = { onEditTimer(timer) }
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Quick Shape Selector
      Text(
        text = "WIDGET SHAPE",
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = cs.onSurfaceVariant,
        letterSpacing = 0.8.sp
      )

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState())
          .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        WidgetShapeStyle.entries.forEach { shape ->
          FilterChip(
            selected = currentShape == shape,
            onClick = {
              onUpdateTimerQuickly(timer.copy(shapeStyle = shape.name))
            },
            label = { Text(shape.displayName, fontSize = 12.sp) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = cs.primary,
              selectedLabelColor = cs.onPrimary,
              containerColor = cs.surfaceVariant,
              labelColor = cs.onSurfaceVariant
            )
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Quick Color Theme Switcher (including Device Dynamic, Pure White, Grayish-Black)
      Text(
        text = "WIDGET COLOR THEME",
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = cs.onSurfaceVariant,
        letterSpacing = 0.8.sp
      )

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState())
          .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        WidgetThemeColor.entries.forEach { themeColor ->
          val isSelected = currentColor == themeColor
          val swatchColor = when (themeColor) {
            WidgetThemeColor.DYNAMIC -> cs.primary
            WidgetThemeColor.WHITE -> Color.White
            WidgetThemeColor.CHARCOAL -> Color(0xFF27272A)
            else -> Color(themeColor.primaryHex)
          }

          FilterChip(
            selected = isSelected,
            onClick = {
              onUpdateTimerQuickly(timer.copy(colorSchemeId = themeColor.id))
            },
            leadingIcon = {
              Box(
                modifier = Modifier
                  .size(14.dp)
                  .clip(CircleShape)
                  .background(swatchColor)
                  .border(0.8.dp, cs.outline, CircleShape)
              )
            },
            label = { Text(themeColor.displayName, fontSize = 12.sp) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = cs.primaryContainer,
              selectedLabelColor = cs.onPrimaryContainer,
              containerColor = cs.surfaceVariant,
              labelColor = cs.onSurfaceVariant
            ),
            border = FilterChipDefaults.filterChipBorder(
              enabled = true,
              selected = isSelected,
              selectedBorderColor = cs.primary,
              selectedBorderWidth = 1.5.dp,
              borderColor = cs.outline.copy(alpha = 0.5f)
            )
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Opacity Slider (0% to 100%)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "WIDGET OPACITY",
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          color = cs.onSurfaceVariant,
          letterSpacing = 0.8.sp
        )
        Text(
          text = "${timer.opacityPercent}%",
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          color = cs.primary
        )
      }

      Slider(
        value = timer.opacityPercent.toFloat(),
        onValueChange = { newVal ->
          val clamped = newVal.roundToInt().coerceIn(0, 100)
          if (clamped != timer.opacityPercent) {
            onUpdateTimerQuickly(timer.copy(opacityPercent = clamped))
          }
        },
        valueRange = 0f..100f,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("showcase_opacity_slider"),
        colors = SliderDefaults.colors(
          thumbColor = cs.primary,
          activeTrackColor = cs.primary,
          inactiveTrackColor = cs.outline.copy(alpha = 0.3f)
        )
      )

      Spacer(modifier = Modifier.height(6.dp))

      // Wallpaper Backdrop Switcher
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "PREVIEW WALLPAPER",
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          color = cs.onSurfaceVariant,
          letterSpacing = 0.8.sp
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          WallpaperTheme.entries.forEach { wp ->
            val swatchPreviewColor = wp.gradientForTheme(isLightAppTheme).first()
            Box(
              modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (selectedWallpaper == wp) cs.primary else swatchPreviewColor)
                .border(1.dp, cs.outline.copy(alpha = 0.5f), CircleShape)
                .clickable { selectedWallpaper = wp },
              contentAlignment = Alignment.Center
            ) {
              Box(
                modifier = Modifier
                  .size(if (selectedWallpaper == wp) 18.dp else 24.dp)
                  .clip(CircleShape)
                  .background(swatchPreviewColor)
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Single Add to Home Screen & Edit buttons
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Button(
          onClick = { onAddToHomeScreen(timer) },
          modifier = Modifier
            .weight(1f)
            .testTag("showcase_add_to_home_button"),
          colors = ButtonDefaults.buttonColors(
            containerColor = cs.primary,
            contentColor = cs.onPrimary
          )
        ) {
          Icon(
            imageVector = Icons.Default.AddHome,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Add to Home Screen",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
          )
        }

        OutlinedButton(
          onClick = { onEditTimer(timer) },
          colors = ButtonDefaults.outlinedButtonColors(contentColor = cs.onSurface),
          border = BorderStroke(1.dp, cs.outline)
        ) {
          Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Edit", fontSize = 13.sp)
        }
      }
    }
  }
}
