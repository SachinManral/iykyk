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

        // 2. Outer Orbital Track Canvas with delicate guide ticks and floating light sparks
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

            // Dynamic floating neon sparks along the orbit
            val sparkAngles = listOf(20f, 95f, 185f, 280f)
            val sparkColors = listOf(Color(0xFFF43F5E), Color(0xFF06B6D4), Color(0xFF8B5CF6), Color(0xFFF59E0B))
            sparkAngles.forEachIndexed { i, baseDeg ->
                val rad = Math.toRadians((baseDeg + orbitAngle).toDouble())
                val x = center.x + (orbitRadius * cos(rad)).toFloat()
                val y = center.y + (orbitRadius * sin(rad)).toFloat()
                
                // Spark aura
                drawCircle(
                    color = sparkColors[i % sparkColors.size].copy(alpha = 0.4f),
                    radius = 5.dp.toPx(),
                    center = Offset(x, y)
                )
                // Spark core
                drawCircle(
                    color = Color.White,
                    radius = 2.dp.toPx(),
                    center = Offset(x, y)
                )
            }
        }

        // 3. Orbiting Face Avatars: ONLY displayed after distinct faces are discovered
        val orbitRadiusDp = 138.dp
        val avatarCount = discoveredAvatars.size

        if (avatarCount > 0) {
            discoveredAvatars.forEachIndexed { index, avatarBmp ->
                val baseAngle = 270f + (index * (360f / avatarCount))
                val totalAngleRad = Math.toRadians((baseAngle + orbitAngle * 0.20f).toDouble())
                val offsetX = (orbitRadiusDp.value * cos(totalAngleRad)).dp
                val offsetY = (orbitRadiusDp.value * sin(totalAngleRad)).dp
                val borderColor = haloColors[index % haloColors.size]

                Box(
                    modifier = Modifier
                        .offset { IntOffset(offsetX.roundToPx(), offsetY.roundToPx()) }
                        .size(56.dp)
                        .shadow(16.dp, CircleShape, ambientColor = borderColor, spotColor = borderColor)
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
                    Image(
                        bitmap = avatarBmp.asImageBitmap(),
                        contentDescription = "Detected Face ${index + 1}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }

        // 4. Central Radial Gauge: Uncompleted Track + Animated Active Arc + Leading Bead
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

        // 5. Central Glass Dial Disc: Frosted Dark Badge with Percentage & Clean Stage Chip
        Box(
            modifier = Modifier
                .size(136.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF20163F),
                            Color(0xFF110B24)
                        )
                    )
                )
                .border(1.2.dp, Color(0x3D8B5CF6), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 10.dp)
            ) {
                Text(
                    text = "$progressPercent%",
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = (-0.5).sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Choosing best moments...",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFC084FC),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
