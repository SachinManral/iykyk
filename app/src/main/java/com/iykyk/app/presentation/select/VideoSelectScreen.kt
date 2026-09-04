package com.iykyk.app.presentation.select

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iykyk.app.presentation.MainViewModel
import com.iykyk.app.presentation.VideoItem
import com.iykyk.app.presentation.theme.BackgroundGradient
import com.iykyk.app.presentation.theme.BgDark
import com.iykyk.app.presentation.theme.PrimaryGradient
import com.iykyk.app.presentation.theme.PrimaryPink
import com.iykyk.app.presentation.theme.PrimaryPurple
import com.iykyk.app.presentation.theme.TextMuted
import com.iykyk.app.presentation.theme.TextPrimary
import com.iykyk.app.presentation.theme.TextSecondary

@Composable
fun VideoSelectScreen(
    viewModel: MainViewModel,
    onBackClick: () -> Unit,
    onNextClick: () -> Unit
) {
    val context = LocalContext.current
    val videoList by viewModel.videoList.collectAsState()
    val selectedVideo by viewModel.selectedVideo.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    // 1. System Photo/Video Gallery Picker (PhotoPicker / Media gallery)
    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.selectCustomVideoUri(uri)
        }
    }

    // 2. System File Manager / Folder Browser (DocumentsUI / Storage Access Framework)
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                // Ignore if not persistable
            }
            viewModel.selectCustomVideoUri(uri)
        }
    }

    Scaffold(
        containerColor = BgDark,
        bottomBar = {
            // Floating Selected Video Bar at bottom
            if (selectedVideo != null) {
                val video = selectedVideo!!
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(68.dp)
                            .shadow(20.dp, RoundedCornerShape(34.dp), ambientColor = Color.Black, spotColor = Color(0x66000000))
                            .clip(RoundedCornerShape(34.dp))
                            .background(Color(0xF0150E28))
                            .border(0.8.dp, Color(0x33FFFFFF), RoundedCornerShape(34.dp))
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left: Thumbnail preview & selection info
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF1E1738))
                                        .border(0.6.dp, Color(0x33FFFFFF), RoundedCornerShape(10.dp))
                                ) {
                                    if (video.thumbnailBitmap != null) {
                                        Image(
                                            bitmap = video.thumbnailBitmap.asImageBitmap(),
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
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "1 video selected",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "${video.durationText} · ${video.resolutionText}",
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                }
                            }

                            // Right: Next Button with gradient pill
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(PrimaryGradient)
                                    .clickable { onNextClick() }
                                    .padding(horizontal = 22.dp, vertical = 11.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Next",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header: Back Button and Centered Title
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextPrimary
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Select Video",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Choose a video to analyze",
                                fontSize = 13.sp,
                                color = TextMuted
                            )
                        }

                        Spacer(modifier = Modifier.size(40.dp))
                    }
                }

                // Segmented Tabs: Videos, Gallery, Folders
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(26.dp))
                            .background(Color(0xFF140F27))
                            .border(0.8.dp, Color(0x22FFFFFF), RoundedCornerShape(26.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Videos Tab
                        TabItem(
                            label = "Videos",
                            icon = Icons.Filled.PlayArrow,
                            isSelected = selectedTab == 0,
                            modifier = Modifier.weight(1f)
                        ) {
                            selectedTab = 0
                        }

                        // Gallery Tab (Native Gallery / Photos Picker)
                        TabItem(
                            label = "Gallery",
                            icon = Icons.Outlined.Collections,
                            isSelected = selectedTab == 1,
                            modifier = Modifier.weight(1f)
                        ) {
                            selectedTab = 1
                            galleryPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                        }

                        // Folders Tab (Native Storage / Document Browser)
                        TabItem(
                            label = "Folders",
                            icon = Icons.Outlined.Folder,
                            isSelected = selectedTab == 2,
                            modifier = Modifier.weight(1f)
                        ) {
                            selectedTab = 2
                            documentPickerLauncher.launch(arrayOf("video/*", "*/*"))
                        }
                    }
                }

                // Browse Phone Storage Banner (Directly opens device file/folder storage)
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0xFF1B1435),
                                        Color(0xFF140E2A)
                                    )
                                )
                            )
                            .border(1.dp, Color(0x334B367C), RoundedCornerShape(20.dp))
                            .clickable { documentPickerLauncher.launch(arrayOf("video/*", "*/*")) }
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0xFF5A2CE2)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Folder,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "Browse Phone Storage",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Pick any video from your device",
                                        fontSize = 12.sp,
                                        color = TextMuted
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = Color(0xFFA59BC8),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                // Recent Videos Section Header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Videos",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Text(
                            text = "${videoList.size} videos",
                            fontSize = 13.sp,
                            color = TextMuted,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }

                // Video Cards List
                items(videoList, key = { it.uri.toString() }) { video ->
                    val isSelected = selectedVideo?.uri == video.uri
                    SelectableVideoCard(
                        video = video,
                        isSelected = isSelected,
                        onClick = { viewModel.selectVideo(video) }
                    )
                }
            }
        }
    }
}

/**
 * Clean Tab item for Segmented control
 */
@Composable
private fun TabItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(if (isSelected) Color(0xFF6C3CE9) else Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else TextMuted,
                modifier = Modifier.size(17.dp)
            )
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else TextMuted
            )
        }
    }
}

/**
 * Modern Selectable Video Item Card with Real Video Thumbnail and Radio Checkbox
 */
@Composable
private fun SelectableVideoCard(
    video: VideoItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isSelected) Color(0xFF1C1538) else Color(0xFF16102D)
            )
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) Color(0xFF6C3CE9) else Color(0x224B367C),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(12.dp)
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
                // Video Thumbnail Frame with duration badge
                Box(
                    modifier = Modifier
                        .size(width = 100.dp, height = 64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF100B20))
                ) {
                    if (video.thumbnailBitmap != null) {
                        Image(
                            bitmap = video.thumbnailBitmap.asImageBitmap(),
                            contentDescription = video.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF2A1C50), Color(0xFF16102E))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Duration overlay on bottom-right of thumbnail
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color(0xCC000000))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = video.durationText,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Video Metadata Column
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = video.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Duration & Resolution Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = null,
                                tint = Color(0xFFA59BC8),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = video.durationText,
                                fontSize = 11.sp,
                                color = Color(0xFFA59BC8)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AspectRatio,
                                contentDescription = null,
                                tint = Color(0xFFA59BC8),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = video.resolutionText,
                                fontSize = 11.sp,
                                color = Color(0xFFA59BC8)
                            )
                        }
                    }

                    // Date Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarToday,
                            contentDescription = null,
                            tint = Color(0xFFA59BC8),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = video.dateText,
                            fontSize = 11.sp,
                            color = Color(0xFFA59BC8)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Selection Checkbox / Radio
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) Color(0xFF6C3CE9) else Color.Transparent)
                    .border(
                        width = if (isSelected) 0.dp else 1.5.dp,
                        color = if (isSelected) Color.Transparent else Color(0x44FFFFFF),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}
