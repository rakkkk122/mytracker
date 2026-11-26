package com.example.tracker.ui.checkin

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tracker.data.DailyCheckIn
import java.time.LocalDate

/**
 * Menghitung jumlah streak hari berturut-turut yang telah dilakukan check-in.
 */
fun calculateStreak(checkIns: Map<LocalDate, DailyCheckIn>): Int {
    if (checkIns.isEmpty()) return 0
    val today = LocalDate.now()
    var streak = 0
    var date = if (checkIns[today]?.isCheckedIn == true) today else today.minusDays(1)
    while (checkIns[date]?.isCheckedIn == true) {
        streak++
        date = date.minusDays(1)
    }
    return streak
}

@Composable
fun SmartDailyCheckInBanner(
    checkIn: DailyCheckIn?,
    allCheckIns: Map<LocalDate, DailyCheckIn> = emptyMap(),
    onOpenCheckIn: () -> Unit,
    onQuickNoSpend: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isCheckedIn = checkIn?.isCheckedIn == true
    var dismissedManually by remember { mutableStateOf(false) }
    val streakCount = remember(allCheckIns, checkIn) { calculateStreak(allCheckIns) }

    AnimatedVisibility(
        visible = !dismissedManually,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        AnimatedContent(
            targetState = isCheckedIn && checkIn != null,
            transitionSpec = {
                (fadeIn(tween(350)) + scaleIn(initialScale = 0.94f, animationSpec = tween(350))) togetherWith
                        fadeOut(tween(200))
            },
            label = "checkInBannerState"
        ) { done ->
            if (done && checkIn != null) {
                DoneCard(
                    checkIn = checkIn,
                    streakCount = streakCount,
                    allCheckIns = allCheckIns,
                    onOpenCheckIn = onOpenCheckIn,
                    onDismiss = { dismissedManually = true },
                    modifier = modifier
                )
            } else {
                PendingCard(
                    streakCount = streakCount,
                    allCheckIns = allCheckIns,
                    onOpenCheckIn = onOpenCheckIn,
                    onQuickNoSpend = onQuickNoSpend,
                    modifier = modifier
                )
            }
        }
    }
}

// ───────────────────────── BELUM CHECK-IN ─────────────────────────

@Composable
private fun PendingCard(
    streakCount: Int,
    allCheckIns: Map<LocalDate, DailyCheckIn>,
    onOpenCheckIn: () -> Unit,
    onQuickNoSpend: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(26.dp)

    val primary = colors.primary
    val tertiary = colors.tertiary
    val transition = rememberInfiniteTransition(label = "pending")

    // Blob dekoratif yang melayang pelan
    val drift by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(6000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "drift"
    )
    // Cincin berdenyut di belakang ikon
    val ring by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1900, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "ring"
    )
    // Ikon sparkle bergoyang halus
    val wobble by transition.animateFloat(
        initialValue = -10f, targetValue = 10f,
        animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "wobble"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .bouncyClickable(shape, onOpenCheckIn),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.25f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            colors.primaryContainer,
                            colors.tertiaryContainer.copy(alpha = 0.85f),
                            colors.secondaryContainer.copy(alpha = 0.7f)
                        )
                    )
                )
                .drawBehind {
                    drawCircle(
                        color = primary.copy(alpha = 0.14f),
                        radius = size.height * 0.75f,
                        center = Offset(size.width * (0.94f - 0.06f * drift), size.height * (0.0f + 0.18f * drift))
                    )
                    drawCircle(
                        color = tertiary.copy(alpha = 0.12f),
                        radius = size.height * 0.5f,
                        center = Offset(size.width * (0.08f + 0.07f * drift), size.height * (1.0f - 0.12f * drift))
                    )
                }
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .graphicsLayer {
                                    val s = 1f + 0.55f * ring
                                    scaleX = s; scaleY = s
                                    alpha = (1f - ring) * 0.55f
                                }
                                .background(colors.primary, CircleShape)
                        )
                        Surface(
                            shape = CircleShape,
                            color = colors.primary,
                            modifier = Modifier.size(48.dp),
                            shadowElevation = 4.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = colors.onPrimary,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .graphicsLayer { rotationZ = wobble }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Daily Check-in",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.onPrimaryContainer
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = if (streakCount > 0)
                                "Jangan putus streak-mu hari ini! 🔥"
                            else
                                "Cukup < 15 detik ⚡ untuk jaga momentum harianmu.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                    }

                    if (streakCount > 0) {
                        Spacer(Modifier.width(8.dp))
                        StreakPill(count = streakCount)
                    }
                }

                WeekStreakRow(
                    allCheckIns = allCheckIns,
                    todayCheckedIn = false,
                    labelColor = colors.onPrimaryContainer.copy(alpha = 0.7f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val buttonShape = RoundedCornerShape(14.dp)
                    Button(
                        onClick = onOpenCheckIn,
                        modifier = Modifier
                            .weight(1f)
                            .clip(buttonShape)
                            .shine(),
                        shape = buttonShape,
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "Mulai Check-in",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge
                        )
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (onQuickNoSpend != null) {
                        OutlinedButton(
                            onClick = onQuickNoSpend,
                            shape = buttonShape,
                            border = BorderStroke(1.5.dp, colors.primary),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "No-Spend 1-Klik",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

// ───────────────────────── SUDAH CHECK-IN ─────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DoneCard(
    checkIn: DailyCheckIn,
    streakCount: Int,
    allCheckIns: Map<LocalDate, DailyCheckIn>,
    onOpenCheckIn: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(22.dp)

    // Ikon centang "pop" saat kartu muncul
    val pop = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        pop.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .bouncyClickable(shape, onOpenCheckIn),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow),
        border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = colors.primary,
                        modifier = Modifier
                            .size(40.dp)
                            .graphicsLayer { scaleX = pop.value; scaleY = pop.value }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = colors.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "Check-in Hari Ini Selesai! ✨",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface
                        )
                        Text(
                            text = "Ketuk untuk melihat atau memperbarui refleksimu.",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onOpenCheckIn, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit check-in",
                            modifier = Modifier.size(18.dp),
                            tint = colors.primary
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup banner",
                            modifier = Modifier.size(18.dp),
                            tint = colors.onSurfaceVariant
                        )
                    }
                }
            }

            WeekStreakRow(allCheckIns = allCheckIns, todayCheckedIn = true)

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (streakCount > 0) {
                    StreakPill(count = streakCount, suffix = "Hari Streak")
                }

                if (checkIn.isNoSpendDay) {
                    InfoChip("🛡️ No-Spend Day", colors.primaryContainer, colors.onPrimaryContainer)
                } else {
                    InfoChip("💸 Ada Pengeluaran", colors.errorContainer.copy(alpha = 0.6f), colors.onErrorContainer)
                }

                if (!checkIn.mood.isNullOrBlank()) {
                    InfoChip(checkIn.mood, colors.surfaceContainerHigh, colors.onSurface)
                }

                if (!checkIn.note.isNullOrBlank()) {
                    InfoChip("📝 \"${checkIn.note}\"", colors.surfaceContainerHigh, colors.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun InfoChip(text: String, container: Color, content: Color) {
    Surface(shape = RoundedCornerShape(8.dp), color = container) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = content,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}