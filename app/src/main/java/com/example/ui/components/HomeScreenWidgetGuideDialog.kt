package com.example.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddHome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.widget.WidgetUpdateHelper

@Composable
fun HomeScreenWidgetGuideDialog(
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val cs = MaterialTheme.colorScheme

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(28.dp),
      color = cs.surface,
      border = BorderStroke(1.dp, cs.outline.copy(alpha = 0.4f)),
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      Column(
        modifier = Modifier.padding(22.dp)
      ) {
        // Title Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Surface(
              shape = CircleShape,
              color = cs.primary.copy(alpha = 0.15f),
              modifier = Modifier.size(36.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Default.Widgets,
                  contentDescription = null,
                  tint = cs.primary,
                  modifier = Modifier.size(20.dp)
                )
              }
            }
            Text(
              text = "Add to Home Screen",
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold,
              color = cs.onSurface
            )
          }

          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = cs.onSurfaceVariant)
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
          text = "Your countdown widgets look just like default Android widgets with smooth rounded corners, elevation shadows, and Material You styling.",
          fontSize = 13.sp,
          color = cs.onSurfaceVariant,
          lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Steps
        GuideStep(
          stepNumber = "1",
          title = "Tap 'Add to Home Screen'",
          desc = "Use the 'Add to Home Screen' button on any countdown widget for direct placement.",
          icon = Icons.Default.AddHome
        )

        Spacer(modifier = Modifier.height(10.dp))

        GuideStep(
          stepNumber = "2",
          title = "Or Long-Press Home Screen",
          desc = "Touch and hold any empty area on your phone's home screen and choose 'Widgets'.",
          icon = Icons.Default.TouchApp
        )

        Spacer(modifier = Modifier.height(10.dp))

        GuideStep(
          stepNumber = "3",
          title = "Select DayCount",
          desc = "Drag the DayCount widget to your desired spot on your home screen.",
          icon = Icons.Default.Widgets
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = cs.onSurface)
          ) {
            Text("Got It")
          }

          Button(
            onClick = {
              WidgetUpdateHelper.requestAddWidgetToHomeScreen(context)
              onDismiss()
            },
            modifier = Modifier.weight(1.3f),
            colors = ButtonDefaults.buttonColors(
              containerColor = cs.primary,
              contentColor = cs.onPrimary
            )
          ) {
            Icon(Icons.Default.AddHome, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Add Now", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

@Composable
private fun GuideStep(
  stepNumber: String,
  title: String,
  desc: String,
  icon: ImageVector
) {
  val cs = MaterialTheme.colorScheme
  Row(
    verticalAlignment = Alignment.Top,
    horizontalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Surface(
      shape = CircleShape,
      color = cs.surfaceVariant,
      border = BorderStroke(1.dp, cs.outline.copy(alpha = 0.4f)),
      modifier = Modifier.size(28.dp)
    ) {
      Box(contentAlignment = Alignment.Center) {
        Text(text = stepNumber, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = cs.onSurface)
      }
    }

    Column {
      Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = cs.onSurface)
      Text(text = desc, fontSize = 11.sp, color = cs.onSurfaceVariant)
    }
  }
}
