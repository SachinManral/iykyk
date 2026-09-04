package com.iykyk.app.presentation.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iykyk.app.R
import com.iykyk.app.data.model.CollageResult
import com.iykyk.app.presentation.MainViewModel
import com.iykyk.app.presentation.history.HistoryContent
import com.iykyk.app.presentation.theme.BackgroundGradient
import com.iykyk.app.presentation.theme.BgDark
import com.iykyk.app.presentation.theme.PrimaryGradient
import com.iykyk.app.presentation.theme.PrimaryPink
import com.iykyk.app.presentation.theme.PrimaryPurple
import com.iykyk.app.presentation.theme.SurfaceCard
import com.iykyk.app.presentation.theme.SurfaceDark
import com.iykyk.app.presentation.theme.SurfaceDarkStroke
import com.iykyk.app.presentation.theme.SurfaceElevated
import com.iykyk.app.presentation.theme.TextAccentPink
import com.iykyk.app.presentation.theme.TextMuted
import com.iykyk.app.presentation.theme.TextPrimary
import com.iykyk.app.presentation.theme.TextSecondary

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onCreateCollageClick: () -> Unit,
    onOpenCollageClick: (CollageResult) -> Unit
) {
    val recentCollages by viewModel.recentCollages.collectAsState()
    var selectedNavIndex by remember { mutableIntStateOf(0) }

    // Intercept back button on History (index 1) or Profile (index 2) to return to Home (index 0)
    BackHandler(enabled = selectedNavIndex != 0) {
        selectedNavIndex = 0
    }

    Scaffold(
        containerColor = BgDark,
        bottomBar = {
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
            Crossfade(targetState = selectedNavIndex, label = "TabCrossfade") { currentTab ->
                when (currentTab) {
                    0 -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                                .padding(horizontal = 20.dp),
                            contentPadding = PaddingValues(vertical = 14.dp)
                        ) {
                            // 1. Header: Logo & Settings
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_iykyk_logo),
                                        contentDescription = "iykyk logo",
                                        modifier = Modifier
                                            .width(95.dp)
                                            .height(45.dp),
                                        contentScale = ContentScale.Fit
                                    )

                                    IconButton(onClick = { selectedNavIndex = 2 }) {
                                        Icon(
                                            imageVector = Icons.Default.Settings,
                                            contentDescription = "Settings",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                            }

                            // 2. Hero Typography
                            item {
                                Column {
                                    Text(
                                        text = "Moments fade.",
                                        fontSize = 34.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        lineHeight = 38.sp
                                    )
                                    Text(
                                        text = "People don't.",
                                        fontSize = 34.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = TextAccentPink,
                                        lineHeight = 40.sp
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "We find everyone in your videos and create a beautiful collage you can keep.",
                                        fontSize = 14.sp,
                                        color = TextSecondary,
                                        lineHeight = 20.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                            }

                            // 3. Static Sample Showcase Card (Hardcoded sample.png, non-clickable)
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(22.dp))
                                        .background(SurfaceCard)
                                        .border(1.dp, SurfaceDarkStroke, RoundedCornerShape(22.dp))
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.sample_collage_card),
                                        contentDescription = "Sample Collage Preview",
                                        modifier = Modifier.fillMaxWidth(),
                                        contentScale = ContentScale.FillWidth
                                    )
                                }
                                Spacer(modifier = Modifier.height(26.dp))
                            }

                            // 4. "Recent Collages" Section Header with "See all"
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Recent Collages",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    if (recentCollages.isNotEmpty()) {
                                        Text(
                                            text = "See all",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFFA78BFA),
                                            modifier = Modifier.clickable { selectedNavIndex = 1 }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                            }

                            // 5. Horizontal Scrolling Row of Real Generated Dual-Segment Cards
                            item {
                                if (recentCollages.isNotEmpty()) {
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(recentCollages) { collage ->
                                            DualSegmentCollageCard(
                                                collage = collage,
                                                onClick = {
                                                    viewModel.openCollage(collage)
                                                    onOpenCollageClick(collage)
                                                }
                                            )
                                        }
                                    }
                                } else {
                                    // Clean empty placeholder (No fake numbers)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(18.dp))
                                            .background(SurfaceCard.copy(alpha = 0.5f))
                                            .border(1.dp, SurfaceDarkStroke, RoundedCornerShape(18.dp))
                                            .padding(vertical = 22.dp, horizontal = 16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Your created collages will appear here",
                                            fontSize = 13.sp,
                                            color = TextMuted,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(26.dp))
                            }

                            // 6. "+ Create New Collage" CTA Button
                            item {
                                Button(
                                    onClick = onCreateCollageClick,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .shadow(16.dp, RoundedCornerShape(28.dp), ambientColor = PrimaryPurple, spotColor = PrimaryPink),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                    contentPadding = PaddingValues(0.dp),
                                    shape = RoundedCornerShape(28.dp)
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
                                                modifier = Modifier.size(22.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Create New Collage",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }
                    }
                    1 -> {
                        HistoryContent(
                            viewModel = viewModel,
                            onCreateCollageClick = onCreateCollageClick,
                            onOpenCollageClick = { collage ->
                                viewModel.openCollage(collage)
                                onOpenCollageClick(collage)
                            },
                            modifier = Modifier.padding(paddingValues)
                        )
                    }
                    2 -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Coming Soon",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}



/**
 * Real Dual-Segment Collage Card (Left: N people, Right: N apps.)
 */
@Composable
private fun DualSegmentCollageCard(
    collage: CollageResult,
    onClick: () -> Unit
) {
    val people = collage.people
    val person1Bitmap = people.getOrNull(0)?.avatarThumbnailBitmap ?: people.getOrNull(0)?.representativePortraitBitmap
    val person2Bitmap = people.getOrNull(1)?.avatarThumbnailBitmap ?: people.getOrNull(1)?.representativePortraitBitmap ?: person1Bitmap

    Box(
        modifier = Modifier
            .width(170.dp)
            .height(105.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceCard)
            .border(1.dp, SurfaceDarkStroke, RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Left Half: Photo + "N people"
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Color(0xFF221A40))
            ) {
                if (person1Bitmap != null) {
                    Image(
                        bitmap = person1Bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                // Bottom gradient scrim
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color(0xCC000000)),
                                startY = 30f
                            )
                        )
                )
                Text(
                    text = "${collage.people.size} people",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.width(1.5.dp))

            // Right Half: Photo + "N apps."
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Color(0xFF1B1533))
            ) {
                if (person2Bitmap != null) {
                    Image(
                        bitmap = person2Bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                // Bottom gradient scrim
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color(0xCC000000)),
                                startY = 30f
                            )
                        )
                )
                Text(
                    text = "${collage.totalAppearances} apps.",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }
        }
    }
}


