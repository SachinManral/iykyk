package com.iykyk.app.presentation.result

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iykyk.app.presentation.MainViewModel
import com.iykyk.app.presentation.theme.AccentCyan
import com.iykyk.app.presentation.theme.BackgroundGradient
import com.iykyk.app.presentation.theme.BgDark
import com.iykyk.app.presentation.theme.PrimaryPink
import com.iykyk.app.presentation.theme.PrimaryPurple
import com.iykyk.app.presentation.theme.SuccessGreen
import com.iykyk.app.presentation.theme.SurfaceCard
import com.iykyk.app.presentation.theme.SurfaceDark
import com.iykyk.app.presentation.theme.SurfaceDarkStroke
import com.iykyk.app.presentation.theme.SurfaceElevated
import com.iykyk.app.presentation.theme.TextMuted
import com.iykyk.app.presentation.theme.TextPrimary
import com.iykyk.app.presentation.theme.TextSecondary
import kotlinx.coroutines.delay

import com.iykyk.app.graphics.CollageTheme
import com.iykyk.app.presentation.theme.PrimaryGradient

@Composable
fun CollageResultScreen(
    viewModel: MainViewModel,
    onBackClick: () -> Unit,
    onHomeClick: () -> Unit,
    onViewPeopleClick: () -> Unit,
    onShareClick: () -> Unit
) {
    val context = LocalContext.current
    val currentCollage by viewModel.currentCollage.collectAsState()
    val activeTheme by viewModel.selectedCollageTheme.collectAsState()
    val isSaving by viewModel.isSavingToGallery.collectAsState()
    val toastMessage by viewModel.saveToastMessage.collectAsState()

    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(2500)
            viewModel.clearToastMessage()
        }
    }

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
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextPrimary
                            )
                        }
                        Text(
                            text = "Your Collage",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        IconButton(onClick = onHomeClick) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = "Home",
                                tint = TextPrimary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Celebratory Headline matching Sample UI
                item {
                    val peopleCount = currentCollage?.people?.size ?: 0
                    val appearancesCount = currentCollage?.totalAppearances ?: 0

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Everyone's here! 🎉",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$peopleCount people · $appearancesCount appearances",
                            fontSize = 14.sp,
                            color = AccentCyan,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Aesthetic Dynamic Template & Remix Bar
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(SurfaceDark.copy(alpha = 0.85f))
                            .border(1.dp, SurfaceDarkStroke, RoundedCornerShape(16.dp))
                            .clickable {
                                viewModel.remixCollageStyle()
                            }
                            .padding(vertical = 10.dp, horizontal = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "✨",
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Auto-Designed Template",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Tap to roll a new creative style",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(PrimaryGradient)
                                .shadow(6.dp, RoundedCornerShape(12.dp))
                                .padding(vertical = 7.dp, horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🎲 Remix",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                }

                // Collage Canvas Image View Frame
                item {
                    val collageBitmap = currentCollage?.collageBitmap
                    if (collageBitmap != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.92f)
                                .aspectRatio(9f / 16f)
                                .shadow(28.dp, RoundedCornerShape(24.dp), ambientColor = PrimaryPurple, spotColor = PrimaryPink)
                                .clip(RoundedCornerShape(24.dp))
                                .border(1.5.dp, Color(0x33FFFFFF), RoundedCornerShape(24.dp))
                        ) {
                            Image(
                                bitmap = collageBitmap.asImageBitmap(),
                                contentDescription = "Rendered Collage",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.92f)
                                .aspectRatio(9f / 16f)
                                .clip(RoundedCornerShape(24.dp))
                                .background(SurfaceDark),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = PrimaryPink)
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Toast Feedback banner if saved
                if (toastMessage != null) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF064E3B))
                                .border(1.dp, SuccessGreen.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = SuccessGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = toastMessage ?: "",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // Bottom Action Buttons: Save to Gallery, Share, View People
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Save to Gallery
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(88.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(SurfaceCard)
                                .border(1.dp, SurfaceDarkStroke, RoundedCornerShape(20.dp))
                                .clickable(enabled = !isSaving) {
                                    viewModel.saveCurrentCollageToGallery(context)
                                }
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                if (isSaving) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = PrimaryPink,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = null,
                                        tint = AccentCyan,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Save to Gallery",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                )
                            }
                        }

                        // Share
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(88.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(SurfaceCard)
                                .border(1.dp, SurfaceDarkStroke, RoundedCornerShape(20.dp))
                                .clickable {
                                    onShareClick()
                                }
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = null,
                                    tint = PrimaryPink,
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Share",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                )
                            }
                        }

                        // View People
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(88.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(SurfaceCard)
                                .border(1.dp, SurfaceDarkStroke, RoundedCornerShape(20.dp))
                                .clickable {
                                    onViewPeopleClick()
                                }
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Group,
                                    contentDescription = null,
                                    tint = PrimaryPurple,
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "View People",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(28.dp))
                }
            }
        }
    }
}
