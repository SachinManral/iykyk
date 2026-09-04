package com.iykyk.app.presentation.processing

import android.graphics.Bitmap
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iykyk.app.data.model.PipelineStage
import com.iykyk.app.presentation.MainViewModel
import com.iykyk.app.presentation.theme.AccentCyan
import com.iykyk.app.presentation.theme.AccentGold
import com.iykyk.app.presentation.theme.BackgroundGradient
import com.iykyk.app.presentation.theme.BgDark
import com.iykyk.app.presentation.theme.PrimaryPink
import com.iykyk.app.presentation.theme.PrimaryPurple
import com.iykyk.app.presentation.theme.SuccessGreen
import com.iykyk.app.presentation.theme.SurfaceDarkStroke
import com.iykyk.app.presentation.theme.TextMuted
import com.iykyk.app.presentation.theme.TextPrimary
import com.iykyk.app.presentation.theme.TextSecondary
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

data class StageInfo(
    val stage: PipelineStage,
    val title: String,
    val description: String
)

@Composable
fun ProcessingScreen(
    viewModel: MainViewModel,
    onProcessingFinished: () -> Unit,
    onCancelClick: () -> Unit
) {
    val progress by viewModel.pipelineProgress.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.startProcessing(
            onComplete = {
                onProcessingFinished()
            }
        )
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progress.progressPercent / 100f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "progress"
    )

    val stagesList = listOf(
        StageInfo(
            PipelineStage.READING_VIDEO,
            "Reading video",
            "Extracting frames and audio metadata"
        ),
        StageInfo(
            PipelineStage.DETECTING_FACES,
            "Detecting faces",
            "Finding and tracking faces in each frame"
        ),
        StageInfo(
            PipelineStage.GROUPING_IDENTITIES,
            "Grouping identities",
            "Matching same people across frames"
        ),
        StageInfo(
            PipelineStage.COUNTING_APPEARANCES,
            "Counting appearances",
            "Identifying unique appearances"
        ),
        StageInfo(
            PipelineStage.CHOOSING_BEST_MOMENTS,
            "Choosing best moments",
            "Selecting the clearest and best shots"
        ),
        StageInfo(
            PipelineStage.COMPOSING_COLLAGE,
            "Composing collage",
            "Putting everything together"
        )
    )

    Scaffold(
        containerColor = BgDark
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundGradient)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header Bar: Back Arrow, Centered Title & Cancel Pill Button
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = {
                                viewModel.cancelProcessing()
                                onCancelClick()
                            },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Cancel",
                                tint = TextPrimary
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "Processing",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Turning your video into memories",
                                fontSize = 13.sp,
                                color = TextMuted
                            )
                        }

                        // Cancel Pill Button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0x1AFF3B81))
                                .border(1.dp, PrimaryPink.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                                .clickable {
                                    viewModel.cancelProcessing()
                                    onCancelClick()
                                }
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Cancel",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryPink
                            )
                        }
                    }
                }

                // Central Radial Gauge with Orbiting Detected Faces
                item {
                    RadialProgressWithOrbit(
                        progressFraction = animatedProgress,
                        progressPercent = progress.progressPercent,
                        statusText = progress.statusMessage,
                        discoveredAvatars = progress.discoveredAvatars
                    )
                }

                // Pipeline Stages Checklist Card with vertical timeline connector
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0xFF191333),
                                        Color(0xFF130E26)
                                    )
                                )
                            )
                            .border(1.dp, Color(0x334B367C), RoundedCornerShape(24.dp))
                            .padding(18.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            stagesList.forEachIndexed { index, stageInfo ->
                                val currentOrdinal = progress.stage.ordinal
                                val itemOrdinal = stageInfo.stage.ordinal

                                val isCompleted = currentOrdinal > itemOrdinal || progress.stage == PipelineStage.COMPLETED
                                val isInProgress = currentOrdinal == itemOrdinal && progress.stage != PipelineStage.COMPLETED
                                val isLast = index == stagesList.lastIndex
                                val recordedDurationSecs = progress.stageDurationsSeconds[stageInfo.stage]

                                PipelineStageRow(
                                    stageInfo = stageInfo,
                                    isCompleted = isCompleted,
                                    isInProgress = isInProgress,
                                    isLast = isLast,
                                    realDurationSecs = recordedDurationSecs
                                )
                            }
                        }
                    }
                }

                // Bottom Tip Box
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF16102D))
                            .border(1.dp, Color(0x2B4B367C), RoundedCornerShape(20.dp))
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF26194A)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Lightbulb,
                                    contentDescription = "Tip",
                                    tint = Color(0xFFD6B0FF),
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Tip",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "We pick the clearest, most natural moments for the best results.",
                                    fontSize = 12.sp,
                                    color = TextMuted,
                                    lineHeight = 17.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * High-end Futuristic Radial Progress Indicator with Glowing Dual-Tone Sweep & Studio-Quality Orbiting Avatars
 */
@Composable
private fun RadialProgressWithOrbit(
    progressFraction: Float,
    progressPercent: Int,
    statusText: String,
    discoveredAvatars: List<Bitmap>
) {
    val infiniteTransition = rememberInfiniteTransition(label = "processingAnimation")
    
    // Smooth orbit rotation for detected faces
    val orbitAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(26000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit"
    )

    // Dynamic rotating gradient for liquid neon energy effect
    val gradientSpinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gradientSpin"
    )

    // Subtle pulsing ambient breath
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val haloColors = listOf(
        Color(0xFF8B5CF6), // Purple
        Color(0xFF06B6D4), // Cyan
        Color(0xFFF43F5E), // Pink
        Color(0xFFF59E0B), // Amber
        Color(0xFF3B82F6)  // Blue
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(340.dp),
        contentAlignment = Alignment.Center
    ) {
        // 1. Ambient soft pulsing radial aura behind progress circle
        Canvas(modifier = Modifier.size(260.dp)) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF8B5CF6).copy(alpha = pulseAlpha * 0.25f),
                        Color(0xFF06B6D4).copy(alpha = pulseAlpha * 0.10f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = size.minDimension / 2f
                )
            )
        }

        // 2. Outer Orbital Track Canvas
        Canvas(modifier = Modifier.size(290.dp)) {
            val orbitRadius = size.minDimension / 2f

            // Sleek dashed orbital path
            drawCircle(
                color = Color(0x226D4CAD),
                radius = orbitRadius,
                style = Stroke(
                    width = 1.2.dp.toPx(),
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8f, 12f), 0f)
                )
            )
        }

        // 3. Central Radial Gauge: Uncompleted Track + Animated Active Arc + Leading Bead
        Canvas(modifier = Modifier.size(184.dp)) {
            val strokeWidth = 12.dp.toPx()
            val arcSize = size.minDimension - strokeWidth
            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

            // Deep violet uncompleted background track
            drawArc(
                color = Color(0xFF1E153B),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = Size(arcSize, arcSize),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Active neon gradient sweep arc
            val sweepAngle = (progressFraction * 360f).coerceIn(2f, 360f)
            val gradientBrush = Brush.sweepGradient(
                listOf(
                    Color(0xFF8B5CF6),
                    Color(0xFF06B6D4),
                    Color(0xFFF43F5E),
                    Color(0xFFF59E0B),
                    Color(0xFF8B5CF6)
                )
            )

            drawArc(
                brush = gradientBrush,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = Size(arcSize, arcSize),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Glowing neon bead at the tip of the active progress arc
            if (progressFraction > 0.01f) {
                val tipAngleDeg = -90f + sweepAngle
                val tipAngleRad = Math.toRadians(tipAngleDeg.toDouble())
                val tipRadius = arcSize / 2f
                val tipX = center.x + (tipRadius * cos(tipAngleRad)).toFloat()
                val tipY = center.y + (tipRadius * sin(tipAngleRad)).toFloat()

                // Outer flare
                drawCircle(
                    color = Color(0xFF06B6D4).copy(alpha = 0.6f),
                    radius = 9.dp.toPx(),
                    center = Offset(tipX, tipY)
                )
                // Core light
                drawCircle(
                    color = Color.White,
                    radius = 4.dp.toPx(),
                    center = Offset(tipX, tipY)
                )
            }
        }

        // 4. Center Content (No inner black circle): Clean Percentage & 2-Line Status
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Text(
                text = "$progressPercent%",
                fontSize = 42.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Choosing best\nmoments...",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFC084FC),
                textAlign = TextAlign.Center,
                lineHeight = 17.sp
            )
        }

        // 5. Free-Floating Orbiting Face Avatars: Floating randomly and organically (Rendered above the circle)
        val avatarCount = discoveredAvatars.size

        if (avatarCount > 0) {
            val baseRadiiDp = listOf(136.dp, 144.dp, 134.dp, 146.dp, 140.dp)

            discoveredAvatars.forEachIndexed { index, avatarBmp ->
                // Distribute evenly in distinct sectors (360 / N) to strictly prevent overlapping
                val sectorBaseAngle = (index * (360f / avatarCount))
                val baseRadius = baseRadiiDp[index % baseRadiiDp.size]
                val phase = index * 1.45f

                // Random organic 2D floating physics
                val radialDrift = (sin(Math.toRadians((orbitAngle * 1.8f + phase * 60f).toDouble())).toFloat() * 7f) +
                        (cos(Math.toRadians((orbitAngle * 1.1f + index * 40f).toDouble())).toFloat() * 4f)
                val effectiveRadiusDp = (baseRadius.value + radialDrift).dp

                val angularDrift = (sin(Math.toRadians((orbitAngle * 0.9f + phase * 50f).toDouble())).toFloat() * 9f) +
                        (cos(Math.toRadians((orbitAngle * 0.5f + index * 80f).toDouble())).toFloat() * 5f)
                val totalAngleRad = Math.toRadians((sectorBaseAngle + orbitAngle * 0.18f + angularDrift).toDouble())

                val offsetX = (effectiveRadiusDp.value * cos(totalAngleRad)).dp
                val offsetY = (effectiveRadiusDp.value * sin(totalAngleRad)).dp
                val borderColor = haloColors[index % haloColors.size]

                Box(
                    modifier = Modifier
                        .offset { IntOffset(offsetX.roundToPx(), offsetY.roundToPx()) }
                        .size(56.dp)
                        .shadow(18.dp, CircleShape, ambientColor = borderColor, spotColor = borderColor)
                        .clip(CircleShape)
                        .background(Color(0xFF1E1538))
                        .border(
                            width = 2.5.dp,
                            brush = Brush.linearGradient(
                                listOf(borderColor, borderColor.copy(alpha = 0.6f))
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (!avatarBmp.isRecycled) {
                        Image(
                            bitmap = avatarBmp.asImageBitmap(),
                            contentDescription = "Detected Face ${index + 1}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }

        // 6. Animated Glowing Shining Particles with Light Tails
        Canvas(modifier = Modifier.size(330.dp)) {
            val centerOffset = Offset(size.width / 2f, size.height / 2f)
            val particleSeeds = listOf(
                Triple(25f, 80.dp, true) to Color(0xFFFBBF24),   // Gold with tail
                Triple(65f, 155.dp, false) to Color(0xFF06B6D4), // Cyan spark
                Triple(110f, 95.dp, true) to Color.White,        // Diamond white with tail
                Triple(155f, 150.dp, false) to Color(0xFFF43F5E),// Pink spark
                Triple(205f, 85.dp, true) to Color(0xFFA855F7),  // Purple with tail
                Triple(250f, 158.dp, false) to Color(0xFF38BDF8),// Sky blue spark
                Triple(295f, 105.dp, true) to Color(0xFFFBBF24), // Gold with tail
                Triple(340f, 150.dp, false) to Color.White       // Diamond white spark
            )

            particleSeeds.forEachIndexed { i, pair ->
                val (baseAngle, baseRad, hasTail) = pair.first
                val color = pair.second

                // Dynamic drifting float
                val movingAngle = baseAngle + orbitAngle * (0.16f + i * 0.025f)
                val movingRad = baseRad.toPx() + sin(Math.toRadians((orbitAngle * 2f + i * 50f).toDouble())).toFloat() * 9f
                val radMath = Math.toRadians(movingAngle.toDouble())

                val gx = centerOffset.x + (movingRad * cos(radMath)).toFloat()
                val gy = centerOffset.y + (movingRad * sin(radMath)).toFloat()
                val centerPt = Offset(gx, gy)

                // Twinkling scale & glow
                val twinkle = 0.40f + 0.60f * abs(sin(Math.toRadians((orbitAngle * 2.8f + i * 55f).toDouble())).toFloat())
                val auraSize = 5.5.dp.toPx() * twinkle
                val coreSize = 2.2.dp.toPx() * twinkle

                // Light trail / tail for particles with tails
                if (hasTail) {
                    val tailLength = 14.dp.toPx() * twinkle
                    val tailAngleRad = Math.toRadians((movingAngle - 90f).toDouble())
                    val tailEndX = gx - (tailLength * cos(tailAngleRad)).toFloat()
                    val tailEndY = gy - (tailLength * sin(tailAngleRad)).toFloat()

                    drawLine(
                        brush = Brush.linearGradient(
                            colors = listOf(color.copy(alpha = 0.65f * twinkle), Color.Transparent),
                            start = centerPt,
                            end = Offset(tailEndX, tailEndY)
                        ),
                        start = centerPt,
                        end = Offset(tailEndX, tailEndY),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // Outer glowing aura
                drawCircle(
                    color = color.copy(alpha = 0.45f * twinkle),
                    radius = auraSize,
                    center = centerPt
                )

                // Bright glowing core point
                drawCircle(
                    color = Color.White,
                    radius = coreSize,
                    center = centerPt
                )
            }
        }
    }
}

/**
 * Individual row in the Pipeline Stages checklist with real dynamic duration and connected vertical line
 */
@Composable
private fun PipelineStageRow(
    stageInfo: StageInfo,
    isCompleted: Boolean,
    isInProgress: Boolean,
    isLast: Boolean,
    realDurationSecs: Int? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left Column: Node Icon + Vertical Line & Content
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Node Icon with connecting vertical line
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(26.dp)
            ) {
                // Circle Node
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isCompleted -> Color(0xFF6C3CE9)
                                isInProgress -> Color(0x336C3CE9)
                                else -> Color.Transparent
                            }
                        )
                        .border(
                            width = if (isInProgress) 2.dp else 1.2.dp,
                            color = when {
                                isCompleted -> Color.Transparent
                                isInProgress -> Color(0xFF8B5CF6)
                                else -> Color(0x44FFFFFF)
                            },
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCompleted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    } else if (isInProgress) {
                        // Glowing white core dot
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }
                }

                // Vertical Connector Line to next node
                if (!isLast) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(28.dp)
                            .background(
                                if (isCompleted) Color(0xFF6C3CE9) else Color(0x1FFFFFFF)
                            )
                    )
                }
            }

            // Title and Description
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stageInfo.title,
                    fontSize = 15.sp,
                    fontWeight = if (isInProgress) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isCompleted || isInProgress) TextPrimary else TextSecondary
                )
                Text(
                    text = stageInfo.description,
                    fontSize = 12.sp,
                    color = TextMuted,
                    lineHeight = 16.sp
                )
            }
        }

        // Right Column: Status & Real Elapsed Stage Duration (NO placeholder times)
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = when {
                    isCompleted -> "Completed"
                    isInProgress -> "In progress..."
                    else -> "Waiting"
                },
                fontSize = 13.sp,
                fontWeight = if (isInProgress) FontWeight.Bold else FontWeight.Medium,
                color = when {
                    isCompleted -> SuccessGreen
                    isInProgress -> PrimaryPink
                    else -> TextMuted
                }
            )

            // Only display real elapsed duration when recorded or in progress
            if (isCompleted && realDurationSecs != null) {
                Text(
                    text = "${realDurationSecs}s",
                    fontSize = 12.sp,
                    color = TextMuted
                )
            } else if (isInProgress && realDurationSecs != null) {
                Text(
                    text = "${realDurationSecs}s",
                    fontSize = 12.sp,
                    color = TextMuted
                )
            }
        }
    }
}
