package com.iykyk.app.presentation.history

import android.text.format.DateFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iykyk.app.R
import com.iykyk.app.data.model.CollageResult
import com.iykyk.app.presentation.MainViewModel
import com.iykyk.app.presentation.theme.AccentCyan
import com.iykyk.app.presentation.theme.AccentGold
import com.iykyk.app.presentation.theme.BackgroundGradient
import com.iykyk.app.presentation.theme.BgDark
import com.iykyk.app.presentation.theme.PrimaryGradient
import com.iykyk.app.presentation.theme.PrimaryPink
import com.iykyk.app.presentation.theme.PrimaryPurple
import com.iykyk.app.presentation.theme.SurfaceCard
import com.iykyk.app.presentation.theme.SurfaceDark
import com.iykyk.app.presentation.theme.SurfaceDarkStroke
import com.iykyk.app.presentation.theme.SurfaceElevated
import com.iykyk.app.presentation.theme.TextMuted
import com.iykyk.app.presentation.theme.TextPrimary
import com.iykyk.app.presentation.theme.TextSecondary
import java.util.Date

@Composable
fun HistoryScreen(
    viewModel: MainViewModel,
    onBackClick: () -> Unit,
    onHomeClick: () -> Unit,
    onCreateCollageClick: () -> Unit,
    onOpenCollageClick: (CollageResult) -> Unit
) {
    val recentCollages by viewModel.recentCollages.collectAsState()
    var selectedNavIndex by remember { mutableIntStateOf(1) } // Active tab = History (index 1)
    var collageToDelete by remember { mutableStateOf<CollageResult?>(null) }

    // Aggregate statistics
    val totalCollages = recentCollages.size
    val totalPeopleDiscovered = recentCollages.sumOf { it.people.size }
    val totalAppearances = recentCollages.sumOf { it.totalAppearances }

    Scaffold(
        containerColor = BgDark,
        bottomBar = {
            // Floating Bottom Navigation Bar matching Home screen
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 42.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .shadow(22.dp, RoundedCornerShape(36.dp), ambientColor = Color.Black, spotColor = Color(0x33000000))
                        .clip(RoundedCornerShape(36.dp))
                        .background(Color(0xCC150E28))
                        .border(0.8.dp, Color(0x1FFFFFFF), RoundedCornerShape(36.dp))
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val navItems = listOf(
                            Triple("Home", R.drawable.ic_nav_home, 0),
                            Triple("History", R.drawable.ic_nav_history, 1),
                            Triple("Profile", R.drawable.ic_nav_profile, 2)
                        )

                        navItems.forEach { (label, iconRes, index) ->
                            val isSelected = selectedNavIndex == index

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        selectedNavIndex = index
                                        if (index == 0) {
                                            onHomeClick()
                                        }
                                    }
                                    .padding(vertical = 2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) Color(0x33FF3B81) else Color.Transparent
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = iconRes),
                                        contentDescription = label,
                                        tint = if (isSelected) PrimaryPink else Color(0x8CFFFFFF),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) PrimaryPink else Color(0x73FFFFFF)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundGradient)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(
                                text = "Collage Vault",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = if (totalCollages > 0) "$totalCollages saved memories" else "No saved collages yet",
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                        }
                    }

                    if (recentCollages.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(PrimaryPurple.copy(alpha = 0.2f))
                                .border(1.dp, PrimaryPurple.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                .clickable { onCreateCollageClick() }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "New",
                                    tint = PrimaryPink,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "New",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }

                if (recentCollages.isNotEmpty()) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(bottom = 20.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Top Stats Strip
                        item(span = { GridItemSpan(2) }) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                Color(0xFF221A40),
                                                Color(0xFF150E28)
                                            )
                                        )
                                    )
                                    .border(1.dp, SurfaceDarkStroke, RoundedCornerShape(20.dp))
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StatChip(
                                    label = "Collages",
                                    value = "$totalCollages",
                                    accentColor = PrimaryPurple,
                                    icon = Icons.Default.Movie
                                )
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(32.dp)
                                        .background(SurfaceDarkStroke)
                                )
                                StatChip(
                                    label = "People",
                                    value = "$totalPeopleDiscovered",
                                    accentColor = AccentCyan,
                                    icon = Icons.Default.Face
                                )
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(32.dp)
                                        .background(SurfaceDarkStroke)
                                )
                                StatChip(
                                    label = "Moments",
                                    value = "$totalAppearances",
                                    accentColor = AccentGold,
                                    icon = Icons.Default.AutoAwesome
                                )
                            }
                        }

                        // Grid of Collages
                        items(recentCollages, key = { it.creationTimestampMs }) { collage ->
                            HistoryCollageCard(
                                collage = collage,
                                onClick = {
                                    viewModel.openCollage(collage)
                                    onOpenCollageClick(collage)
                                },
                                onDeleteClick = {
                                    collageToDelete = collage
                                }
                            )
                        }
                    }
                } else {
                    // Premium Empty State
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .shadow(24.dp, CircleShape, ambientColor = PrimaryPurple, spotColor = PrimaryPink)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(
                                                Color(0x667A4DF2),
                                                Color(0x22FF3B81),
                                                Color(0x00000000)
                                            )
                                        )
                                    )
                                    .border(1.5.dp, PrimaryPurple.copy(alpha = 0.6f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_nav_history),
                                    contentDescription = null,
                                    tint = PrimaryPink,
                                    modifier = Modifier.size(38.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = "Your Vault is Empty",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Create unforgettable collages from your videos and discover every person inside.",
                                fontSize = 13.sp,
                                color = TextMuted,
                                lineHeight = 19.sp,
                                modifier = Modifier.padding(horizontal = 16.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(28.dp))

                            Button(
                                onClick = onCreateCollageClick,
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .height(50.dp)
                                    .shadow(16.dp, RoundedCornerShape(25.dp), ambientColor = PrimaryPurple, spotColor = PrimaryPink),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                contentPadding = PaddingValues(0.dp),
                                shape = RoundedCornerShape(25.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(PrimaryGradient),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Create First Collage",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (collageToDelete != null) {
        val target = collageToDelete!!
        AlertDialog(
            onDismissRequest = { collageToDelete = null },
            containerColor = SurfaceDark,
            title = {
                Text(
                    text = "Delete Collage?",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to remove \"${target.videoTitle}\" from your history?",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSavedCollage(target)
                        collageToDelete = null
                    }
                ) {
                    Text(
                        text = "Delete",
                        color = PrimaryPink,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { collageToDelete = null }
                ) {
                    Text(
                        text = "Cancel",
                        color = TextMuted
                    )
                }
            }
        )
    }
}

/**
 * Metric summary chip for top banner
 */
@Composable
private fun StatChip(
    label: String,
    value: String,
    accentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Modern 2-column Collage Card with avatar bubbles, badges & delete trigger
 */
@Composable
private fun HistoryCollageCard(
    collage: CollageResult,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val dateString = remember(collage.creationTimestampMs) {
        try {
            DateFormat.format("MMM d, yyyy", Date(collage.creationTimestampMs)).toString()
        } catch (e: Exception) {
            "Recent"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceCard)
            .border(1.dp, SurfaceDarkStroke, RoundedCornerShape(20.dp))
            .clickable { onClick() }
    ) {
        Column {
            // Collage Image Frame
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(175.dp)
                    .background(Color(0xFF130E26))
            ) {
                Image(
                    bitmap = collage.collageBitmap.asImageBitmap(),
                    contentDescription = collage.videoTitle,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Top gradient scrim
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0x99000000), Color.Transparent)
                            )
                        )
                )

                // Top badges row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // People count badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xCC1A1433))
                            .border(0.6.dp, AccentCyan.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "${collage.people.size} people",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentCyan
                        )
                    }

                    // Appearances count badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xCC1A1433))
                            .border(0.6.dp, Color(0x33FFFFFF), RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "${collage.totalAppearances} apps.",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Bottom gradient scrim with Avatar Bubble Stack
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color(0xDD090714)),
                                startY = 0f
                            )
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    // Overlapping avatar face bubbles
                    val maxAvatarsToShow = 3
                    val previewPeople = collage.people.take(maxAvatarsToShow)
                    val totalExtra = collage.people.size - maxAvatarsToShow

                    Box(
                        modifier = Modifier
                            .height(26.dp)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        previewPeople.forEachIndexed { index, person ->
                            val bmp = person.avatarThumbnailBitmap ?: person.representativePortraitBitmap
                            Box(
                                modifier = Modifier
                                    .offset(x = (index * 16).dp)
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(SurfaceDark)
                                    .border(1.5.dp, Color(0xFF090714), CircleShape)
                            ) {
                                if (bmp != null) {
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(PrimaryPurple.copy(alpha = 0.4f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${person.id}",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        if (totalExtra > 0) {
                            Box(
                                modifier = Modifier
                                    .offset(x = (previewPeople.size * 16).dp)
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryPurple)
                                    .border(1.5.dp, Color(0xFF090714), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "+$totalExtra",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // Card Metadata Footer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = collage.videoTitle,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(22.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete",
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = dateString,
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
        }
    }
}

