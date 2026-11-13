package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.CountdownDateUtils
import com.example.data.CountdownMode
import com.example.data.CountdownTimer
import com.example.data.DateDisplayMode
import com.example.data.WidgetShapeStyle
import com.example.data.WidgetThemeColor
import com.example.widget.DayCountWidgetProvider
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

private val QUICK_DAYS = listOf(7, 14, 21, 30, 60, 90, 100, 365)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditTimerDialog(
  initialTimer: CountdownTimer?,
  onDismiss: () -> Unit,
  onSave: (CountdownTimer) -> Unit
) {
  val isEditing = initialTimer != null
  val now = System.currentTimeMillis()
  val cs = MaterialTheme.colorScheme
  val focusManager = LocalFocusManager.current

  var title by remember {
    mutableStateOf(CountdownDateUtils.sanitizeTitleInput(initialTimer?.title ?: ""))
  }
  var subtitle by remember { mutableStateOf(initialTimer?.subtitle ?: "") }
  var mode by remember { mutableStateOf(initialTimer?.getResolvedMode() ?: CountdownMode.TARGET_DATE) }

  var targetDateEpochMs by remember {
    mutableLongStateOf(
      initialTimer?.targetDateEpochMs ?: CountdownDateUtils.addLocalCalendarDays(now, 30)
    )
  }
  // Starting Date (optional) for "Until Specific Date" mode so the user can adjust the progress bar
  var targetModeStartDateEpochMs by remember {
    mutableStateOf<Long?>(
      if (initialTimer != null && initialTimer.getResolvedMode() == CountdownMode.TARGET_DATE) {
        initialTimer.startDateEpochMs
      } else {
        null
      }
    )
  }

  var targetDaysTotal by remember {
    mutableIntStateOf((initialTimer?.targetDaysTotal ?: 30).coerceIn(1, 36500))
  }
  var isEditingDaysWithNumpad by remember { mutableStateOf(false) }
  var daysTextFieldValue by remember {
    val initStr = ((initialTimer?.targetDaysTotal ?: 30).coerceIn(1, 36500)).toString()
    mutableStateOf(TextFieldValue(text = initStr, selection = TextRange(initStr.length)))
  }
  val daysFocusRequester = remember { FocusRequester() }

  // Required Starting Date for "Specified Days" mode
  var startDateEpochMs by remember {
    mutableStateOf<Long?>(initialTimer?.startDateEpochMs)
  }

  var selectedColorId by remember {
    mutableStateOf(initialTimer?.colorSchemeId ?: WidgetThemeColor.DYNAMIC.id)
  }
  var selectedShapeStyle by remember {
    mutableStateOf(initialTimer?.shapeStyle ?: WidgetShapeStyle.SQUIRCLE.name)
  }
  var showTargetDate by remember {
    mutableStateOf(initialTimer?.showTargetDate ?: true)
  }
  var dateDisplayMode by remember {
    mutableStateOf(initialTimer?.getResolvedDateDisplayMode() ?: DateDisplayMode.ENDING_DATE)
  }
  var opacityPercent by remember {
    mutableIntStateOf(initialTimer?.opacityPercent ?: 100)
  }

  var showTargetDatePickerDialog by remember { mutableStateOf(false) }
  var showTargetModeStartDatePickerDialog by remember { mutableStateOf(false) }
  var showStartDatePickerDialog by remember { mutableStateOf(false) }
  var titleError by remember { mutableStateOf(false) }
  var startDateError by remember { mutableStateOf(false) }

  val commitNumpadDaysInput = {
    val sanitizedInt = CountdownDateUtils.parseSanitizedDays(
      rawInput = daysTextFieldValue.text,
      fallback = targetDaysTotal
    )
    targetDaysTotal = sanitizedInt
    val normalizedText = sanitizedInt.toString()
    daysTextFieldValue = TextFieldValue(
      text = normalizedText,
      selection = TextRange(normalizedText.length)
    )
    isEditingDaysWithNumpad = false
    focusManager.clearFocus()
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth(0.95f)
        .padding(vertical = 24.dp)
        .testTag("create_edit_timer_dialog"),
      shape = RoundedCornerShape(28.dp),
      color = cs.surface,
      border = BorderStroke(1.dp, cs.outline.copy(alpha = 0.4f)),
      tonalElevation = 8.dp
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(20.dp)
      ) {
        // Dialog Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = if (isEditing) "Edit Countdown Widget" else "New Countdown Widget",
              fontSize = 20.sp,
              fontWeight = FontWeight.Bold,
              color = cs.onSurface
            )
            Text(
              text = "Custom label, countdown mode, shape & Material color",
              fontSize = 12.sp,
              color = cs.onSurfaceVariant
            )
          }

          IconButton(onClick = onDismiss) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Close",
              tint = cs.onSurfaceVariant
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Countdown Mode Selector (Date vs Days)
        Text(
          text = "COUNTDOWN TYPE",
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          color = cs.primary,
          letterSpacing = 1.sp
        )

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          ModeCard(
            title = "Until Specific Date",
            subtitle = "Choose a calendar date",
            isSelected = mode == CountdownMode.TARGET_DATE,
            onClick = {
              if (isEditingDaysWithNumpad) commitNumpadDaysInput()
              mode = CountdownMode.TARGET_DATE
              startDateError = false
            },
            modifier = Modifier
              .weight(1f)
              .testTag("mode_target_date_card")
          )

          ModeCard(
            title = "Specified Days",
            subtitle = "e.g. 30 / 100 days streak",
            isSelected = mode == CountdownMode.DAY_COUNT,
            onClick = { mode = CountdownMode.DAY_COUNT },
            modifier = Modifier
              .weight(1f)
              .testTag("mode_specified_days_card")
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Custom Label / Title Input (Max 32 characters)
        OutlinedTextField(
          value = title,
          onValueChange = {
            val sanitized = CountdownDateUtils.sanitizeTitleInput(it)
            title = sanitized
            titleError = sanitized.isBlank()
          },
          label = { Text("Custom Widget Label (Required)") },
          placeholder = { Text("e.g. Summer Vacation, 100 Days of Code") },
          isError = titleError,
          singleLine = true,
          supportingText = {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(
                text = if (titleError) {
                  "Please enter a custom label for this timer"
                } else {
                  "Maximum 32 characters"
                },
                color = if (titleError) cs.error else cs.onSurfaceVariant,
                fontSize = 11.sp
              )
              Text(
                text = "${title.length} / ${CountdownDateUtils.MAX_TITLE_LENGTH}",
                color = cs.onSurfaceVariant,
                fontSize = 11.sp,
                modifier = Modifier.testTag("title_char_counter")
              )
            }
          },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("timer_title_input"),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = cs.primary,
            unfocusedBorderColor = cs.outline,
            focusedTextColor = cs.onSurface,
            unfocusedTextColor = cs.onSurface,
            focusedContainerColor = cs.surfaceVariant,
            unfocusedContainerColor = cs.surfaceVariant
          )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Subtitle / Note (left blank by default)
        OutlinedTextField(
          value = subtitle,
          onValueChange = { subtitle = it },
          label = { Text("Optional Subtitle") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = cs.primary,
            unfocusedBorderColor = cs.outline,
            focusedTextColor = cs.onSurface,
            unfocusedTextColor = cs.onSurface,
            focusedContainerColor = cs.surfaceVariant,
            unfocusedContainerColor = cs.surfaceVariant
          )
        )

        Spacer(modifier = Modifier.height(16.dp))

        val dateFormatter = remember { SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()) }

        // Date Pickers or Days Stepper
        if (mode == CountdownMode.TARGET_DATE) {
          // 1. Starting Date (optional) BEFORE the required Ending Date so the user can adjust the progress bar
          val effectiveTargetModeStartMs =
            targetModeStartDateEpochMs ?: CountdownDateUtils.addLocalCalendarDays(now, 0)
          val totalSpanDays = max(
            1L,
            CountdownDateUtils.calendarDaysBetween(effectiveTargetModeStartMs, targetDateEpochMs)
          )
          val elapsedDays = max(
            0L,
            CountdownDateUtils.calendarDaysBetween(effectiveTargetModeStartMs, now)
          ).coerceAtMost(totalSpanDays)
          val previewProgressFraction =
            (elapsedDays.toFloat() / totalSpanDays.toFloat()).coerceIn(0f, 1f)
          val previewProgressPercent = (previewProgressFraction * 100).roundToInt()

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Starting Date (optional)",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = cs.primary,
              modifier = Modifier.testTag("optional_starting_date_label")
            )

            if (targetModeStartDateEpochMs != null) {
              TextButton(
                onClick = { targetModeStartDateEpochMs = null },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                modifier = Modifier.height(26.dp)
              ) {
                Text("Reset to Today", fontSize = 11.sp, color = cs.primary)
              }
            }
          }

          val optionalStartDisplay = targetModeStartDateEpochMs?.let {
            dateFormatter.format(Date(it))
          } ?: "Today (${SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(now))}) — Default"

          Surface(
            shape = RoundedCornerShape(16.dp),
            color = cs.surfaceVariant,
            border = BorderStroke(1.dp, cs.outline.copy(alpha = 0.5f)),
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 8.dp)
              .clickable { showTargetModeStartDatePickerDialog = true }
              .testTag("target_mode_start_date_picker_trigger")
          ) {
            Column(modifier = Modifier.padding(16.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = optionalStartDisplay,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = cs.onSurface,
                    modifier = Modifier.testTag("selected_optional_starting_date_text")
                  )
                  Text(
                    text = "Adjusts progress bar: $previewProgressPercent% completed ($elapsedDays / $totalSpanDays days)",
                    fontSize = 11.sp,
                    color = cs.primary,
                    modifier = Modifier.padding(top = 2.dp)
                  )
                }
                Icon(
                  imageVector = Icons.Default.CalendarMonth,
                  contentDescription = "Pick Optional Starting Date",
                  tint = cs.primary
                )
              }

              Spacer(modifier = Modifier.height(8.dp))

              LinearProgressIndicator(
                progress = { previewProgressFraction },
                modifier = Modifier
                  .fillMaxWidth()
                  .height(5.dp)
                  .clip(RoundedCornerShape(3.dp)),
                color = cs.primary,
                trackColor = cs.onSurface.copy(alpha = 0.15f)
              )
            }
          }

          // Quick preset buttons for Optional Starting Date
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            listOf(
              "Today" to 0,
              "-7 Days" to -7,
              "-14 Days" to -14,
              "-30 Days" to -30,
              "-90 Days" to -90
            ).forEach { (label, dayOffset) ->
              OutlinedButton(
                onClick = {
                  targetModeStartDateEpochMs = CountdownDateUtils.addLocalCalendarDays(now, dayOffset)
                },
                modifier = Modifier
                  .height(34.dp)
                  .testTag("optional_start_preset_${label.lowercase().replace(" ", "_")}")
              ) {
                Text(label, fontSize = 11.sp)
              }
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // 2. Ending Date (required)
          val formattedDate = dateFormatter.format(Date(targetDateEpochMs))
          val daysLeft = CountdownDateUtils.calendarDaysBetween(now, targetDateEpochMs)

          Text(
            text = "Ending Date (required)",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = cs.primary,
            modifier = Modifier.testTag("ending_date_required_label")
          )

          Surface(
            shape = RoundedCornerShape(16.dp),
            color = cs.surfaceVariant,
            border = BorderStroke(1.dp, cs.outline.copy(alpha = 0.5f)),
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 8.dp)
              .clickable { showTargetDatePickerDialog = true }
              .testTag("target_date_picker_trigger")
          ) {
            Row(
              modifier = Modifier.padding(16.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(
                  text = formattedDate,
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Bold,
                  color = cs.onSurface,
                  modifier = Modifier.testTag("selected_ending_date_text")
                )
                Text(
                  text = if (daysLeft >= 0) "$daysLeft days remaining from now" else "${-daysLeft} days overdue",
                  fontSize = 12.sp,
                  color = cs.primary
                )
              }
              Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = "Pick Date",
                tint = cs.primary
              )
            }
          }

          // Quick preset buttons for ending date
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            listOf(
              "+7 Days" to 7,
              "+14 Days" to 14,
              "+30 Days" to 30,
              "+60 Days" to 60,
              "+100 Days" to 100
            ).forEach { (label, days) ->
              OutlinedButton(
                onClick = {
                  targetDateEpochMs = CountdownDateUtils.addLocalCalendarDays(now, days)
                },
                modifier = Modifier.height(34.dp)
              ) {
                Text(label, fontSize = 11.sp)
              }
            }
          }
        } else {
          // Specified Days Mode: Required Starting Date + Specified Number of Days (Tap-to-Edit Numpad)
          Text(
            text = "Starting Date (required)",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (startDateError) cs.error else cs.primary,
            modifier = Modifier.testTag("starting_date_required_label")
          )

          val currentStartMs = startDateEpochMs
          val formattedStart = currentStartMs?.let { dateFormatter.format(Date(it)) }
            ?: "Select Starting Date (Required)"

          val calculatedEndDateText = if (currentStartMs != null) {
            val endMs = CountdownDateUtils.addLocalCalendarDays(currentStartMs, targetDaysTotal)
            val endDateStr = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(endMs))
            "Ends on $endDateStr ($targetDaysTotal days total)"
          } else {
            "Tap to choose when this $targetDaysTotal-day countdown starts"
          }

          Surface(
            shape = RoundedCornerShape(16.dp),
            color = cs.surfaceVariant,
            border = BorderStroke(
              width = if (startDateError) 1.5.dp else 1.dp,
              color = if (startDateError) cs.error else cs.outline.copy(alpha = 0.5f)
            ),
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 8.dp)
              .clickable {
                if (isEditingDaysWithNumpad) commitNumpadDaysInput()
                showStartDatePickerDialog = true
              }
              .testTag("start_date_picker_trigger")
          ) {
            Row(
              modifier = Modifier.padding(16.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = formattedStart,
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (currentStartMs != null) cs.onSurface else cs.onSurfaceVariant,
                  modifier = Modifier.testTag("selected_starting_date_text")
                )
                Text(
                  text = calculatedEndDateText,
                  fontSize = 12.sp,
                  color = if (startDateError) cs.error else cs.primary
                )
              }
              Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = "Pick Starting Date",
                tint = if (startDateError) cs.error else cs.primary
              )
            }
          }

          if (startDateError) {
            Text(
              text = "Please select a starting date for this countdown",
              color = cs.error,
              fontSize = 12.sp,
              modifier = Modifier
                .padding(start = 8.dp, bottom = 6.dp)
                .testTag("start_date_error_text")
            )
          }

          // Quick preset buttons for starting date
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            listOf(
              "Today" to 0,
              "Yesterday" to -1,
              "-3 Days" to -3,
              "-7 Days" to -7,
              "-14 Days" to -14
            ).forEach { (label, dayOffset) ->
              OutlinedButton(
                onClick = {
                  startDateEpochMs = CountdownDateUtils.addLocalCalendarDays(now, dayOffset)
                  startDateError = false
                },
                modifier = Modifier
                  .height(34.dp)
                  .testTag("start_date_preset_${label.lowercase().replace(" ", "_")}")
              ) {
                Text(label, fontSize = 11.sp)
              }
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "SPECIFIED NUMBER OF DAYS",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = cs.primary,
              letterSpacing = 1.sp
            )

            Text(
              text = if (isEditingDaysWithNumpad) "Done" else "Tap number for numpad",
              fontSize = 11.sp,
              fontWeight = if (isEditingDaysWithNumpad) FontWeight.Bold else FontWeight.Normal,
              color = if (isEditingDaysWithNumpad) cs.primary else cs.onSurfaceVariant,
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable {
                  if (isEditingDaysWithNumpad) {
                    commitNumpadDaysInput()
                  } else {
                    val str = targetDaysTotal.toString()
                    daysTextFieldValue = TextFieldValue(text = str, selection = TextRange(0, str.length))
                    isEditingDaysWithNumpad = true
                  }
                }
                .padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            IconButton(
              onClick = {
                val base = if (isEditingDaysWithNumpad) {
                  CountdownDateUtils.parseSanitizedDays(daysTextFieldValue.text, targetDaysTotal)
                } else {
                  targetDaysTotal
                }
                if (base > CountdownDateUtils.MIN_SPECIFIED_DAYS) {
                  val next = base - 1
                  targetDaysTotal = next
                  val nextStr = next.toString()
                  daysTextFieldValue = TextFieldValue(text = nextStr, selection = TextRange(nextStr.length))
                }
              },
              modifier = Modifier
                .size(44.dp)
                .background(cs.surfaceVariant, CircleShape)
            ) {
              Icon(Icons.Default.Remove, contentDescription = "Decrease Days", tint = cs.onSurface)
            }

            if (isEditingDaysWithNumpad) {
              LaunchedEffect(Unit) {
                daysFocusRequester.requestFocus()
              }

              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                OutlinedTextField(
                  value = daysTextFieldValue,
                  onValueChange = { newVal ->
                    // Input sanitation: only allow digits, strip leading zeros, clamp max to 36500
                    val sanitized = CountdownDateUtils.sanitizeDaysInput(newVal.text)
                    daysTextFieldValue = TextFieldValue(
                      text = sanitized,
                      selection = TextRange(sanitized.length)
                    )
                    val parsed = sanitized.toIntOrNull()
                    if (parsed != null && parsed >= CountdownDateUtils.MIN_SPECIFIED_DAYS) {
                      targetDaysTotal = parsed.coerceIn(
                        CountdownDateUtils.MIN_SPECIFIED_DAYS,
                        CountdownDateUtils.MAX_SPECIFIED_DAYS
                      )
                    }
                  },
                  singleLine = true,
                  textStyle = TextStyle(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = cs.primary,
                    textAlign = TextAlign.Center
                  ),
                  keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                  ),
                  keyboardActions = KeyboardActions(
                    onDone = { commitNumpadDaysInput() }
                  ),
                  shape = RoundedCornerShape(16.dp),
                  colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = cs.primary,
                    unfocusedBorderColor = cs.primary,
                    focusedContainerColor = cs.surfaceVariant,
                    unfocusedContainerColor = cs.surfaceVariant
                  ),
                  modifier = Modifier
                    .width(148.dp)
                    .focusRequester(daysFocusRequester)
                    .testTag("specified_days_numpad_input")
                )

                Text(
                  text = "ENTER 1 – 36,500 DAYS",
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  color = cs.onSurfaceVariant,
                  modifier = Modifier.padding(top = 4.dp)
                )
              }
            } else {
              // Tappable Days Number Box that opens the numpad on tap
              Surface(
                shape = RoundedCornerShape(16.dp),
                color = cs.surfaceVariant.copy(alpha = 0.6f),
                border = BorderStroke(1.dp, cs.primary.copy(alpha = 0.35f)),
                modifier = Modifier
                  .clip(RoundedCornerShape(16.dp))
                  .clickable {
                    val str = targetDaysTotal.toString()
                    daysTextFieldValue = TextFieldValue(
                      text = str,
                      selection = TextRange(0, str.length)
                    )
                    isEditingDaysWithNumpad = true
                  }
                  .testTag("specified_days_number_display")
              ) {
                Column(
                  horizontalAlignment = Alignment.CenterHorizontally,
                  modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    Text(
                      text = "$targetDaysTotal",
                      fontSize = 34.sp,
                      fontWeight = FontWeight.ExtraBold,
                      color = cs.primary
                    )
                    Icon(
                      imageVector = Icons.Default.Edit,
                      contentDescription = "Tap to type exact days",
                      tint = cs.primary.copy(alpha = 0.7f),
                      modifier = Modifier.size(16.dp)
                    )
                  }
                  Text(
                    text = "TAP TO TYPE DAYS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = cs.onSurfaceVariant
                  )
                }
              }
            }

            IconButton(
              onClick = {
                val base = if (isEditingDaysWithNumpad) {
                  CountdownDateUtils.parseSanitizedDays(daysTextFieldValue.text, targetDaysTotal)
                } else {
                  targetDaysTotal
                }
                if (base < CountdownDateUtils.MAX_SPECIFIED_DAYS) {
                  val next = base + 1
                  targetDaysTotal = next
                  val nextStr = next.toString()
                  daysTextFieldValue = TextFieldValue(text = nextStr, selection = TextRange(nextStr.length))
                }
              },
              modifier = Modifier
                .size(44.dp)
                .background(cs.surfaceVariant, CircleShape)
            ) {
              Icon(Icons.Default.Add, contentDescription = "Increase Days", tint = cs.onSurface)
            }
          }

          // Preset quick buttons for day count
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            QUICK_DAYS.forEach { days ->
              FilterChip(
                selected = targetDaysTotal == days && !isEditingDaysWithNumpad,
                onClick = {
                  targetDaysTotal = days
                  val str = days.toString()
                  daysTextFieldValue = TextFieldValue(text = str, selection = TextRange(str.length))
                  isEditingDaysWithNumpad = false
                  focusManager.clearFocus()
                },
                label = { Text("$days Days", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = cs.primary,
                  selectedLabelColor = cs.onPrimary,
                  containerColor = cs.surfaceVariant,
                  labelColor = cs.onSurfaceVariant
                )
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Android Widget Shape Selector
        Text(
          text = "ANDROID WIDGET SHAPE",
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          color = cs.primary,
          letterSpacing = 1.sp
        )

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          WidgetShapeStyle.entries.forEach { shape ->
            FilterChip(
              selected = selectedShapeStyle == shape.name,
              onClick = { selectedShapeStyle = shape.name },
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

        Spacer(modifier = Modifier.height(12.dp))

        // Theme Color Palette
        Text(
          text = "WIDGET COLOR THEME",
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          color = cs.primary,
          letterSpacing = 1.sp
        )

        val context = LocalContext.current
        val isDarkAppTheme = cs.background.luminance() < 0.5f

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          WidgetThemeColor.entries.forEach { themeColor ->
            val isSelected = selectedColorId == themeColor.id
            val swatchColor = if (themeColor == WidgetThemeColor.DYNAMIC) {
              Color(
                DayCountWidgetProvider.resolveCanvasWidgetColors(
                  context = context,
                  themeColor = WidgetThemeColor.DYNAMIC,
                  overrideIsDark = isDarkAppTheme
                ).surface
              )
            } else {
              Color(themeColor.surfaceHex)
            }

            FilterChip(
              selected = isSelected,
              onClick = { selectedColorId = themeColor.id },
              leadingIcon = {
                Box(
                  modifier = Modifier
                    .size(16.dp)
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

        Spacer(modifier = Modifier.height(12.dp))

        // Widget Opacity Slider (0% to 100%)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "WIDGET OPACITY",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = cs.primary,
            letterSpacing = 1.sp
          )
          Text(
            text = "$opacityPercent%",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = cs.onSurface
          )
        }

        Slider(
          value = opacityPercent.toFloat(),
          onValueChange = { opacityPercent = it.roundToInt().coerceIn(0, 100) },
          valueRange = 0f..100f,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("dialog_opacity_slider"),
          colors = SliderDefaults.colors(
            thumbColor = cs.primary,
            activeTrackColor = cs.primary,
            inactiveTrackColor = cs.outline.copy(alpha = 0.3f)
          )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Toggle: Display Countdown Date on Widget (High-contrast Switch with Check Icon)
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Display countdown date on widget",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = cs.onSurface,
            modifier = Modifier.weight(1f)
          )

          Switch(
            checked = showTargetDate,
            onCheckedChange = { showTargetDate = it },
            modifier = Modifier.testTag("toggle_show_target_date"),
            thumbContent = if (showTargetDate) {
              {
                Icon(
                  imageVector = Icons.Default.Check,
                  contentDescription = null,
                  modifier = Modifier.size(SwitchDefaults.IconSize)
                )
              }
            } else {
              null
            },
            colors = SwitchDefaults.colors(
              checkedThumbColor = Color.White,
              checkedTrackColor = cs.primary,
              checkedBorderColor = cs.primary,
              checkedIconColor = cs.primary,
              uncheckedThumbColor = cs.outline,
              uncheckedTrackColor = cs.surfaceVariant,
              uncheckedBorderColor = cs.outline
            )
          )
        }

        // 2 Radio Button Options underneath "Display countdown date on widget"
        // Works for EITHER countdown type; grayed out and unselectable when showTargetDate is false.
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 2.dp),
          verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
          DateDisplayMode.entries.forEach { option ->
            val isSelected = dateDisplayMode == option
            val radioTextColor = if (showTargetDate) {
              cs.onSurface
            } else {
              cs.onSurface.copy(alpha = 0.38f)
            }

            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(
                  enabled = showTargetDate,
                  role = Role.RadioButton,
                  onClick = { dateDisplayMode = option }
                )
                .padding(vertical = 4.dp, horizontal = 4.dp)
                .testTag("radio_row_${option.name.lowercase()}"),
              verticalAlignment = Alignment.CenterVertically
            ) {
              RadioButton(
                selected = isSelected,
                onClick = if (showTargetDate) {
                  { dateDisplayMode = option }
                } else {
                  null
                },
                enabled = showTargetDate,
                colors = RadioButtonDefaults.colors(
                  selectedColor = cs.primary,
                  unselectedColor = cs.onSurfaceVariant,
                  disabledSelectedColor = cs.onSurface.copy(alpha = 0.38f),
                  disabledUnselectedColor = cs.onSurface.copy(alpha = 0.38f)
                ),
                modifier = Modifier.testTag("radio_${option.name.lowercase()}")
              )
              Text(
                text = option.displayName,
                fontSize = 13.sp,
                fontWeight = if (isSelected && showTargetDate) FontWeight.SemiBold else FontWeight.Normal,
                color = radioTextColor,
                modifier = Modifier.padding(start = 6.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Action Buttons: Save & Cancel
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = cs.onSurface)
          ) {
            Text("Cancel")
          }

          Button(
            onClick = {
              if (isEditingDaysWithNumpad) {
                commitNumpadDaysInput()
              }

              var hasError = false
              if (title.isBlank()) {
                titleError = true
                hasError = true
              }
              if (mode == CountdownMode.DAY_COUNT && startDateEpochMs == null) {
                startDateError = true
                hasError = true
              }
              if (hasError) {
                return@Button
              }

              val sanitizedDays = CountdownDateUtils.parseSanitizedDays(
                rawInput = if (isEditingDaysWithNumpad) daysTextFieldValue.text else targetDaysTotal.toString(),
                fallback = targetDaysTotal
              )

              val resolvedStartMs = if (mode == CountdownMode.TARGET_DATE) {
                targetModeStartDateEpochMs ?: CountdownDateUtils.addLocalCalendarDays(now, 0)
              } else {
                startDateEpochMs
                  ?: initialTimer?.startDateEpochMs
                  ?: CountdownDateUtils.addLocalCalendarDays(now, 0)
              }

              val timer = CountdownTimer(
                id = initialTimer?.id ?: 0L,
                title = CountdownDateUtils.sanitizeTitleInput(title.trim()),
                subtitle = subtitle.trim(),
                mode = mode.name,
                targetDateEpochMs = targetDateEpochMs,
                targetDaysTotal = sanitizedDays,
                startDateEpochMs = resolvedStartMs,
                colorSchemeId = selectedColorId,
                shapeStyle = selectedShapeStyle,
                showTargetDate = showTargetDate,
                dateDisplayMode = dateDisplayMode.name,
                opacityPercent = opacityPercent,
                createdAt = initialTimer?.createdAt ?: now
              )
              onSave(timer)
            },
            modifier = Modifier
              .weight(1f)
              .testTag("save_timer_button"),
            colors = ButtonDefaults.buttonColors(
              containerColor = cs.primary,
              contentColor = cs.onPrimary
            )
          ) {
            Text(if (isEditing) "Save Changes" else "Create Widget", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }

  // Material 3 DatePickerDialog for Starting Date (optional) ("Until Specific Date" mode)
  if (showTargetModeStartDatePickerDialog) {
    val targetStartPickerState = rememberDatePickerState(
      initialSelectedDateMillis = CountdownDateUtils.localMillisToUtcPickerMillis(
        targetModeStartDateEpochMs ?: now
      )
    )

    DatePickerDialog(
      onDismissRequest = { showTargetModeStartDatePickerDialog = false },
      confirmButton = {
        TextButton(
          onClick = {
            targetStartPickerState.selectedDateMillis?.let { utcPicked ->
              targetModeStartDateEpochMs = CountdownDateUtils.utcPickerMillisToLocalMillis(utcPicked)
            }
            showTargetModeStartDatePickerDialog = false
          }
        ) {
          Text("Select Starting Date")
        }
      },
      dismissButton = {
        TextButton(onClick = { showTargetModeStartDatePickerDialog = false }) {
          Text("Cancel")
        }
      }
    ) {
      DatePicker(state = targetStartPickerState)
    }
  }

  // Material 3 DatePickerDialog for Ending Date ("Until Specific Date" mode)
  if (showTargetDatePickerDialog) {
    val datePickerState = rememberDatePickerState(
      initialSelectedDateMillis = CountdownDateUtils.localMillisToUtcPickerMillis(targetDateEpochMs)
    )

    DatePickerDialog(
      onDismissRequest = { showTargetDatePickerDialog = false },
      confirmButton = {
        TextButton(
          onClick = {
            datePickerState.selectedDateMillis?.let { utcPicked ->
              targetDateEpochMs = CountdownDateUtils.utcPickerMillisToLocalMillis(utcPicked)
            }
            showTargetDatePickerDialog = false
          }
        ) {
          Text("Select Date")
        }
      },
      dismissButton = {
        TextButton(onClick = { showTargetDatePickerDialog = false }) {
          Text("Cancel")
        }
      }
    ) {
      DatePicker(state = datePickerState)
    }
  }

  // Material 3 DatePickerDialog for Required Starting Date ("Specified Days" mode)
  if (showStartDatePickerDialog) {
    val startPickerState = rememberDatePickerState(
      initialSelectedDateMillis = CountdownDateUtils.localMillisToUtcPickerMillis(
        startDateEpochMs ?: now
      )
    )

    DatePickerDialog(
      onDismissRequest = { showStartDatePickerDialog = false },
      confirmButton = {
        TextButton(
          onClick = {
            startPickerState.selectedDateMillis?.let { utcPicked ->
              startDateEpochMs = CountdownDateUtils.utcPickerMillisToLocalMillis(utcPicked)
              startDateError = false
            }
            showStartDatePickerDialog = false
          }
        ) {
          Text("Select Starting Date")
        }
      },
      dismissButton = {
        TextButton(onClick = { showStartDatePickerDialog = false }) {
          Text("Cancel")
        }
      }
    ) {
      DatePicker(state = startPickerState)
    }
  }
}

@Composable
private fun ModeCard(
  title: String,
  subtitle: String,
  isSelected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val cs = MaterialTheme.colorScheme
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = if (isSelected) cs.primaryContainer else cs.surfaceVariant,
    border = BorderStroke(
      width = if (isSelected) 2.dp else 1.dp,
      color = if (isSelected) cs.primary else cs.outline.copy(alpha = 0.5f)
    ),
    modifier = modifier.clickable { onClick() }
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = if (isSelected) cs.onPrimaryContainer else cs.onSurface
      )
      Text(
        text = subtitle,
        fontSize = 11.sp,
        color = cs.onSurfaceVariant,
        modifier = Modifier.padding(top = 2.dp)
      )
    }
  }
}
