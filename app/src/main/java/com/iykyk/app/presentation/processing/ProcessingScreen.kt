package com.iykyk.app.presentation.processing

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iykyk.app.data.model.PipelineStage
import com.iykyk.app.presentation.MainViewModel
import com.iykyk.app.presentation.theme.AccentCyan
import com.iykyk.app.presentation.theme.BackgroundGradient
import com.iykyk.app.presentation.theme.BgDark
import com.iykyk.app.presentation.theme.PrimaryPink
import com.iykyk.app.presentation.theme.PrimaryPurple
import com.iykyk.app.presentation.theme.SurfaceCard
import com.iykyk.app.presentation.theme.SurfaceDark
import com.iykyk.app.presentation.theme.SurfaceDarkStroke
import com.iykyk.app.presentation.theme.SurfaceElevated
import com.iykyk.app.presentation.theme.TextMuted
import com.iykyk.app.presentation.theme.TextPrimary
import com.iykyk.app.presentation.theme.TextSecondary

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
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            viewModel.cancelProcessing()
                            onCancelClick()
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Cancel",
                                tint = TextPrimary
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Processing",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Sit tight, magic is happening ✨",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                        TextButton(onClick = {
                            viewModel.cancelProcessing()
                            onCancelClick()
                        }) {
                            Text("Cancel", color = TextMuted)
                        }
                    }
                    Spacer(modifier = Modifier.height(28.dp))
                }

                // Central Circular Progress Gauge with Orbiting Avatars
                item {
                    Box(
                        modifier = Modifier.size(230.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Outer Circular Track & Progress
                        CircularProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier.size(196.dp),
                            color = PrimaryPink,
                            trackColor = SurfaceElevated,
                            strokeWidth = 10.dp
                        )

                        // Central Numeric Display
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "${progress.progressPercent}%",
                                fontSize = 38.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (progress.progressPercent >= 90) "Almost there" else "Analyzing...",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        }

                        // Orbiting Discovered Avatars matching Sample UI
                        val avatars = progress.discoveredAvatars
                        if (avatars.isNotEmpty()) {
                            // Top avatar
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, PrimaryPurple, CircleShape)
                            ) {
                                Image(
                                    bitmap = avatars[0].asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        if (avatars.size > 1) {
                            // Top-right avatar
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, AccentCyan, CircleShape)
                            ) {
                                Image(
                                    bitmap = avatars[1].asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        if (avatars.size > 2) {
                            // Bottom-left avatar
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, PrimaryPink, CircleShape)
                            ) {
                                Image(
                                    bitmap = avatars[2].asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        if (avatars.size > 3) {
                            // Bottom-center avatar
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, PrimaryPurple, CircleShape)
                            ) {
                                Image(
                                    bitmap = avatars[3].asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(28.dp))
                }

                // 6-Stage Checklist Card
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(SurfaceCard)
                            .border(1.dp, SurfaceDarkStroke, RoundedCornerShape(22.dp))
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        val stages = listOf(
                            PipelineStage.READING_VIDEO to "Reading video",
                            PipelineStage.DETECTING_FACES to "Detecting faces",
                            PipelineStage.GROUPING_IDENTITIES to "Grouping identities",
                            PipelineStage.COUNTING_APPEARANCES to "Counting appearances",
                            PipelineStage.CHOOSING_BEST_MOMENTS to "Choosing best moments",
                            PipelineStage.COMPOSING_COLLAGE to "Composing collage"
                        )

                        stages.forEach { (stage, label) ->
                            val currentStageOrdinal = progress.stage.ordinal
                            val itemStageOrdinal = stage.ordinal

                            val isCompleted = currentStageOrdinal > itemStageOrdinal
                            val isInProgress = currentStageOrdinal == itemStageOrdinal

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    isCompleted -> PrimaryPurple
                                                    isInProgress -> PrimaryPink
                                                    else -> Color.Transparent
                                                }
                                            )
                                            .border(
                                                1.5.dp,
                                                if (isCompleted || isInProgress) Color.Transparent else TextMuted,
                                                CircleShape
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
                                            Icon(
                                                imageVector = Icons.Default.Circle,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(8.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = label,
                                        fontSize = 14.sp,
                                        fontWeight = if (isInProgress) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isInProgress || isCompleted) TextPrimary else TextMuted
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = when {
                                            isCompleted -> "Completed"
                                            isInProgress -> "In progress..."
                                            else -> "Waiting"
                                        },
                                        fontSize = 12.sp,
                                        color = when {
                                            isCompleted -> AccentCyan
                                            isInProgress -> PrimaryPink
                                            else -> TextMuted
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = TextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Tip Card with Sparkle Accent
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(SurfaceElevated)
                            .border(1.dp, SurfaceDarkStroke, RoundedCornerShape(20.dp))
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = PrimaryPink,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Tip",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "We pick the clearest, most natural moments for the best results.",
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
