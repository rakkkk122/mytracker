package com.example.tracker.ui.checkin

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tracker.data.DailyCheckIn
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Klik dengan efek "menekan" (scale spring) + ripple yang ikut bentuk. */
@Composable
fun Modifier.bouncyClickable(
    shape: Shape = RoundedCornerShape(0.dp),
    onClick: () -> Unit
): Modifier {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "pressScale"
    )
    return this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clip(shape)
        .clickable(interactionSource = source, indication = LocalIndication.current, onClick = onClick)
}

/** Kilau cahaya yang lewat berkala di atas komponen (taruh setelah .clip(shape)). */
@Composable
fun Modifier.shine(): Modifier {
    val transition = rememberInfiniteTransition(label = "shine")
    val x by transition.animateFloat(
        initialValue = -0.4f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, delayMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shineX"
    )
    return this.drawWithContent {
        drawContent()
        val w = size.width
        val cx = x * w
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.30f), Color.Transparent),
                start = Offset(cx - w * 0.2f, 0f),
                end = Offset(cx + w * 0.2f, size.height)
            )
        )
    }
}

/** Muncul bertahap (fade + naik) tanpa menggeser layout. */
@Composable
fun StaggeredReveal(index: Int, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index * 90L)
        progress.animateTo(1f, tween(480, easing = FastOutSlowInEasing))
    }
    Box(
        modifier = modifier.graphicsLayer {
            alpha = progress.value
            translationY = (1f - progress.value) * 36.dp.toPx()
        }
    ) { content() }
}

/** Pill streak: angka menghitung naik + api berdenyut. */
@Composable
fun StreakPill(count: Int, modifier: Modifier = Modifier, suffix: String = "Hari") {
    val orange = Color(0xFFFF9800)
    var target by remember { mutableStateOf(0) }
    LaunchedEffect(count) { target = count }
    val shown by animateIntAsState(target, tween(900, easing = FastOutSlowInEasing), label = "streakCount")

    val flame = rememberInfiniteTransition(label = "flame")
    val flameScale by flame.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "flameScale"
    )

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = orange.copy(alpha = 0.18f),
        border = BorderStroke(1.dp, orange.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.LocalFireDepartment,
                contentDescription = null,
                tint = orange,
                modifier = Modifier
                    .size(16.dp)
                    .graphicsLayer { scaleX = flameScale; scaleY = flameScale }
            )
            Text(
                text = "$shown $suffix",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/** Indikator 7 hari terakhir; titik yang sudah check-in "pop" satu per satu. */
@Composable
fun WeekStreakRow(
    allCheckIns: Map<LocalDate, DailyCheckIn>,
    todayCheckedIn: Boolean,
    modifier: Modifier = Modifier,
    labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    val today = remember { LocalDate.now() }
    val days = remember { (6 downTo 0).map { today.minusDays(it.toLong()) } }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        days.forEachIndexed { index, date ->
            val isToday = date == today
            val done = if (isToday) todayCheckedIn else allCheckIns[date]?.isCheckedIn == true
            DayDot(
                label = date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale("id", "ID")),
                done = done,
                isToday = isToday,
                index = index,
                labelColor = labelColor
            )
        }
    }
}

@Composable
private fun DayDot(label: String, done: Boolean, isToday: Boolean, index: Int, labelColor: Color) {
    val colors = MaterialTheme.colorScheme
    val pop = remember { Animatable(if (done) 0f else 1f) }
    LaunchedEffect(done) {
        if (done) {
            delay(150L + index * 70L)
            pop.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow))
        } else pop.snapTo(1f)
    }

    val pulse = rememberInfiniteTransition(label = "todayPulse")
    val pulseAlpha by pulse.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulseAlpha"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .graphicsLayer {
                    val s = if (done) pop.value else 1f
                    scaleX = s; scaleY = s
                }
                .clip(CircleShape)
                .background(if (done) colors.primary else labelColor.copy(alpha = 0.08f))
                .then(
                    if (!done) Modifier.border(
                        1.5.dp,
                        if (isToday) colors.primary.copy(alpha = pulseAlpha) else labelColor.copy(alpha = 0.25f),
                        CircleShape
                    ) else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            if (done) {
                Icon(Icons.Default.Check, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(16.dp))
            }
        }
        Spacer(Modifier.height(3.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.Medium,
            color = if (isToday) colors.primary else labelColor
        )
    }
}

private data class Particle(val angleDeg: Float, val speed: Float, val color: Color, val size: Float)

/** Ledakan konfeti sederhana (sekali jalan). */
@Composable
fun ConfettiBurst(modifier: Modifier = Modifier, palette: List<Color>, count: Int = 22) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(1300, easing = LinearOutSlowInEasing)) }
    val particles = remember {
        List(count) { i ->
            Particle(
                angleDeg = i * 360f / count + Random.nextFloat() * 14f,
                speed = 0.55f + Random.nextFloat() * 0.5f,
                color = palette[i % palette.size],
                size = 4f + Random.nextFloat() * 4f
            )
        }
    }
    Canvas(modifier) {
        val p = progress.value
        val maxR = size.minDimension / 2f
        particles.forEach { pt ->
            val rad = Math.toRadians(pt.angleDeg.toDouble())
            val r = maxR * pt.speed * p
            val gravity = 40.dp.toPx() * p * p
            drawCircle(
                color = pt.color.copy(alpha = (1f - p).coerceIn(0f, 1f)),
                radius = pt.size.dp.toPx() * (1f - 0.5f * p),
                center = Offset(center.x + cos(rad).toFloat() * r, center.y + sin(rad).toFloat() * r + gravity)
            )
        }
    }
}
