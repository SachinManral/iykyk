package com.iykyk.app.presentation.history

import android.text.format.DateFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iykyk.app.R
import com.iykyk.app.data.model.CollageResult
import com.iykyk.app.data.model.PersonIdentity
import com.iykyk.app.presentation.MainViewModel
import com.iykyk.app.presentation.theme.BackgroundGradient
import com.iykyk.app.presentation.theme.BgDark
import com.iykyk.app.presentation.theme.PrimaryPink
import com.iykyk.app.presentation.theme.PrimaryPurple
import com.iykyk.app.presentation.theme.SurfaceDark
import com.iykyk.app.presentation.theme.SurfaceDarkStroke
import com.iykyk.app.presentation.theme.TextMuted
import com.iykyk.app.presentation.theme.TextPrimary
import com.iykyk.app.presentation.theme.TextSecondary
import java.util.Date

enum class HistorySortOption(val displayName: String) {
    NEWEST("Newest"),
    OLDEST("Oldest"),
    MOST_PEOPLE("Most People"),
    DURATION("Duration")
}

enum class HistoryFilterTab(val displayName: String) {
    RECENT("Recent"),
    FAVORITES("Favorites"),
    ALL("All")
}

@Composable
fun HistoryContent(
    viewModel: MainViewModel,
    onCreateCollageClick: () -> Unit,
    onOpenCollageClick: (CollageResult) -> Unit,
    modifier: Modifier = Modifier
) {
    val recentCollages by viewModel.recentCollages.collectAsState()
    val favoriteCollageIds by viewModel.favoriteCollageIds.collectAsState()
    var currentSortOption by remember { mutableStateOf(HistorySortOption.NEWEST) }
    var selectedFilterTab by remember { mutableStateOf(HistoryFilterTab.RECENT) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var collageToDelete by remember { mutableStateOf<CollageResult?>(null) }
    val focusManager = LocalFocusManager.current

    // Filter by tab and search
    val filteredCollages = remember(recentCollages, selectedFilterTab, searchQuery, favoriteCollageIds) {
        val tabFiltered = when (selectedFilterTab) {
            HistoryFilterTab.RECENT -> recentCollages
            HistoryFilterTab.FAVORITES -> recentCollages.filter { favoriteCollageIds.contains(it.creationTimestampMs) }
            HistoryFilterTab.ALL -> recentCollages
        }

        if (searchQuery.isBlank()) {
            tabFiltered
        } else {
            tabFiltered.filter { collage ->
                collage.videoTitle.contains(searchQuery, ignoreCase = true) ||
                        collage.people.any { it.label.contains(searchQuery, ignoreCase = true) }
            }
        }
    }

    // Sort collages according to selected option
    val sortedCollages = remember(filteredCollages, currentSortOption) {
        when (currentSortOption) {
            HistorySortOption.NEWEST -> filteredCollages.sortedByDescending { it.creationTimestampMs }
            HistorySortOption.OLDEST -> filteredCollages.sortedBy { it.creationTimestampMs }
            HistorySortOption.MOST_PEOPLE -> filteredCollages.sortedByDescending { it.people.size }
            HistorySortOption.DURATION -> filteredCollages.sortedByDescending { it.videoDurationMs }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundGradient)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Area matching reference image
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Top Row: Title & Subtitle on Left + Circular Action Buttons (Search & Tune) on Right
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = "History",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )

                                Text(
                                    text = "Your saved video collages & detected moments",
                                    fontSize = 13.sp,
                                    color = TextMuted,
                                    fontWeight = FontWeight.Normal
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Action Icons: Search & Tune
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Search Button
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF1B1435))
                                        .border(0.8.dp, Color(0x2BFFFFFF), CircleShape)
                                        .clickable { isSearchActive = !isSearchActive },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search",
                                        tint = if (isSearchActive) PrimaryPink else TextPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Tune / Filter Settings Button
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF1B1435))
                                        .border(0.8.dp, Color(0x2BFFFFFF), CircleShape)
                                        .clickable { sortMenuExpanded = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = "Filter",
                                        tint = TextPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // Animated Expandable Search Field
                        AnimatedVisibility(
                            visible = isSearchActive,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF16102D))
                                    .border(0.8.dp, Color(0x33FFFFFF), RoundedCornerShape(14.dp))
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = TextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )

                                    Box(modifier = Modifier.weight(1f)) {
                                        if (searchQuery.isEmpty()) {
                                            Text(
                                                text = "Search collages & people...",
                                                color = TextMuted,
                                                fontSize = 13.sp
                                            )
                                        }
                                        BasicTextField(
                                            value = searchQuery,
                                            onValueChange = { searchQuery = it },
                                            singleLine = true,
                                            textStyle = TextStyle(
                                                color = TextPrimary,
                                                fontSize = 13.sp
                                            ),
                                            cursorBrush = SolidColor(PrimaryPink),
                                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }

                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(
                                            onClick = { searchQuery = "" },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Clear",
                                                tint = TextMuted,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Filter Pills (Recent, Favorites, All) & Sort By Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Filter Pills
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                HistoryFilterTab.entries.forEach { tab ->
                                    val isSelected = selectedFilterTab == tab
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(
                                                if (isSelected) Color(0xFF6C3CE9) else Color(0x14FFFFFF)
                                            )
                                            .border(
                                                width = if (isSelected) 0.dp else 0.8.dp,
                                                color = if (isSelected) Color.Transparent else Color(0x26FFFFFF),
                                                shape = RoundedCornerShape(20.dp)
                                            )
                                            .clickable { selectedFilterTab = tab }
                                            .padding(horizontal = 14.dp, vertical = 7.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = tab.displayName,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else TextSecondary
                                        )
                                    }
                                }
                            }

                            // Sort by trigger
                            Box {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { sortMenuExpanded = true }
                                        .padding(horizontal = 4.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "Sort by",
                                        fontSize = 12.sp,
                                        color = TextMuted,
                                        fontWeight = FontWeight.Normal
                                    )
                                    Text(
                                        text = currentSortOption.displayName,
                                        fontSize = 12.sp,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Sort Options",
                                        tint = TextMuted,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = sortMenuExpanded,
                                    onDismissRequest = { sortMenuExpanded = false },
                                    modifier = Modifier
                                        .background(Color(0xFF1E1738))
                                        .border(1.dp, SurfaceDarkStroke, RoundedCornerShape(12.dp))
                                ) {
                                    HistorySortOption.entries.forEach { option ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = option.displayName,
                                                    color = if (option == currentSortOption) PrimaryPink else TextPrimary,
                                                    fontWeight = if (option == currentSortOption) FontWeight.Bold else FontWeight.Normal,
                                                    fontSize = 14.sp
                                                )
                                            },
                                            onClick = {
                                                currentSortOption = option
                                                sortMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Collages List
                items(sortedCollages, key = { it.creationTimestampMs }) { collage ->
                    val isFav = favoriteCollageIds.contains(collage.creationTimestampMs)
                    ModernHistoryCollageCard(
                        collage = collage,
                        isFavorite = isFav,
                        onToggleFavorite = { viewModel.toggleFavorite(collage.creationTimestampMs) },
                        onClick = {
                            viewModel.openCollage(collage)
                            onOpenCollageClick(collage)
                        },
                        onDeleteClick = {
                            collageToDelete = collage
                        }
                    )
                }

                // "No more collages yet" / Empty End Section
                item {
                    NoMoreCollagesFooter(
                        hasCollages = sortedCollages.isNotEmpty(),
                        emptyMessage = if (selectedFilterTab == HistoryFilterTab.FAVORITES && recentCollages.isNotEmpty()) {
                            "No favorite collages yet.\nTap the heart icon on any collage to save it here."
                        } else {
                            "Process more videos to see your\nmemories here."
                        },
                        onCreateCollageClick = onCreateCollageClick
                    )
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
 * Modern Full-Width History Collage Card inspired by the sample visual design.
 * Features an asymmetric photo mosaic tile grid with remaining face counter badge, favorite heart button, and overflow menu.
 */
@Composable
private fun ModernHistoryCollageCard(
    collage: CollageResult,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val formattedDate = remember(collage.creationTimestampMs) {
        try {
            DateFormat.format("MMM d, yyyy", Date(collage.creationTimestampMs)).toString()
        } catch (e: Exception) {
            "Recent"
        }
    }

    val formattedDuration = remember(collage.videoDurationMs) {
        val totalSeconds = (collage.videoDurationMs / 1000).coerceAtLeast(1)
        if (totalSeconds < 60) {
            "${totalSeconds}s video"
        } else {
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            "${minutes}m ${seconds}s video"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF191333),
                        Color(0xFF130E26)
                    )
                )
            )
            .border(1.dp, Color(0x334B367C), RoundedCornerShape(24.dp))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Mosaic Grid Header
            CollageMosaicPreview(
                collage = collage,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(16.dp))
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Title & Action Buttons (Favorite Heart + 3-Dot Menu)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = collage.videoTitle.ifBlank { "Sample Collage" },
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Small Heart / Favorite Button
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = if (isFavorite) "Remove favorite" else "Add to favorites",
                            tint = if (isFavorite) PrimaryPink else Color(0xFF8B85AC),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // 3-Dot Overflow Menu
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Options",
                                tint = Color(0xFF8B85AC),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            modifier = Modifier
                                .background(Color(0xFF1E1738))
                                .border(1.dp, SurfaceDarkStroke, RoundedCornerShape(10.dp))
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "Open Collage",
                                        color = TextPrimary,
                                        fontSize = 14.sp
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                        contentDescription = null,
                                        tint = PrimaryPink,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onClick()
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "Delete",
                                        color = Color(0xFFFF5252),
                                        fontSize = 14.sp
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = null,
                                        tint = Color(0xFFFF5252),
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onDeleteClick()
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle Row 1: People & Appearances count
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Group,
                    contentDescription = null,
                    tint = Color(0xFFA59BC8),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "${collage.people.size} people  ·  ${collage.totalAppearances} appearances",
                    fontSize = 13.sp,
                    color = Color(0xFFA59BC8),
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Subtitle Row 2: Date & Video Duration
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.CalendarToday,
                    contentDescription = null,
                    tint = Color(0xFFA59BC8),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "$formattedDate  ·  $formattedDuration",
                    fontSize = 13.sp,
                    color = Color(0xFFA59BC8),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Visual Asymmetric Mosaic grid preview matching the sample design:
 * Left side: Large primary hero face.
 * Right side: 2x2 grid containing additional faces, with "+N" badge on the last cell if extra people exist.
 */
@Composable
private fun CollageMosaicPreview(
    collage: CollageResult,
    modifier: Modifier = Modifier
) {
    val people = collage.people
    val totalPeople = people.size

    Box(
        modifier = modifier.background(Color(0xFF100B20))
    ) {
        if (people.isEmpty()) {
            // Fallback to collage Bitmap if no people list
            Image(
                bitmap = collage.collageBitmap.asImageBitmap(),
                contentDescription = collage.videoTitle,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else if (totalPeople == 1) {
            // Single person: Full width
            SinglePersonTile(
                person = people[0],
                collageFallback = collage.collageBitmap,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Asymmetric Mosaic Layout (Left hero + Right 2x2 grid)
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Left Column: Large Hero Face
                Box(
                    modifier = Modifier
                        .weight(1.15f)
                        .fillMaxHeight()
                ) {
                    SinglePersonTile(
                        person = people[0],
                        collageFallback = collage.collageBitmap,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Right Column: Up to 4 smaller faces arranged in 2 rows x 2 cols
                val remainingPeople = people.drop(1)
                val extraCount = (totalPeople - 5).coerceAtLeast(0)

                Column(
                    modifier = Modifier
                        .weight(1.85f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Top Row of Right side (2 images)
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        val p1 = remainingPeople.getOrNull(0)
                        val p2 = remainingPeople.getOrNull(1)

                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            if (p1 != null) {
                                SinglePersonTile(person = p1, collageFallback = collage.collageBitmap, modifier = Modifier.fillMaxSize())
                            } else {
                                Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1C1635)))
                            }
                        }

                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            if (p2 != null) {
                                SinglePersonTile(person = p2, collageFallback = collage.collageBitmap, modifier = Modifier.fillMaxSize())
                            } else {
                                Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1C1635)))
                            }
                        }
                    }

                    // Bottom Row of Right side (2 images)
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        val p3 = remainingPeople.getOrNull(2)
                        val p4 = remainingPeople.getOrNull(3)

                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            if (p3 != null) {
                                SinglePersonTile(person = p3, collageFallback = collage.collageBitmap, modifier = Modifier.fillMaxSize())
                            } else {
                                Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1C1635)))
                            }
                        }

                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            if (p4 != null) {
                                SinglePersonTile(person = p4, collageFallback = collage.collageBitmap, modifier = Modifier.fillMaxSize())
                            } else if (p3 != null) {
                                SinglePersonTile(person = p3, collageFallback = collage.collageBitmap, modifier = Modifier.fillMaxSize())
                            } else {
                                Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1C1635)))
                            }

                            // Show badge on bottom right tile if more people exist
                            if (extraCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(4.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xD9100B22))
                                        .border(0.6.dp, Color(0x33FFFFFF), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 3.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Collections,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Text(
                                            text = "+$extraCount",
                                            fontSize = 11.sp,
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
}

/**
 * Individual Person Tile for mosaic grid
 */
@Composable
private fun SinglePersonTile(
    person: PersonIdentity,
    collageFallback: android.graphics.Bitmap?,
    modifier: Modifier = Modifier
) {
    val bitmap = person.representativePortraitBitmap
        ?: person.avatarThumbnailBitmap
        ?: collageFallback

    Box(
        modifier = modifier.background(Color(0xFF191330))
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = person.label,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PrimaryPurple.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = person.label.take(2).uppercase(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * "No more collages yet" Footer State inspired directly by the reference image
 */
@Composable
private fun NoMoreCollagesFooter(
    hasCollages: Boolean,
    emptyMessage: String = "Process more videos to see your\nmemories here.",
    onCreateCollageClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = if (hasCollages) 20.dp else 50.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Decorative Icon with subtle sparkles
        Box(
            modifier = Modifier
                .size(72.dp),
            contentAlignment = Alignment.Center
        ) {
            // Sparkles on top-right
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Color(0xFFC070FF),
                modifier = Modifier
                    .size(16.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = (-4).dp, y = 2.dp)
            )

            // Overlapping Card Frames
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .rotate(-10f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x33513C7A))
                    .border(1.dp, Color(0x667A50BE), RoundedCornerShape(10.dp))
            )
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .rotate(6f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x6637255E))
                    .border(1.2.dp, Color(0x99A66BFF), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Collections,
                    contentDescription = null,
                    tint = Color(0xFFD6B0FF),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Headline
        Text(
            text = "No more collages yet",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Subtitle
        Text(
            text = emptyMessage,
            fontSize = 13.sp,
            color = Color(0xFF8E88AC),
            lineHeight = 18.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Gradient Pill CTA Button
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(26.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF4C27B8),
                            Color(0xFF8B25A2)
                        )
                    )
                )
                .clickable { onCreateCollageClick() }
                .padding(horizontal = 24.dp, vertical = 13.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Create New Collage",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}
