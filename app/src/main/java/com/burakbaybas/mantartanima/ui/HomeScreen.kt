package com.burakbaybas.mantartanima.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.burakbaybas.mantartanima.PhotoAngle
import java.io.File

const val DEVELOPER_NAME = "Burak BAYBAŞ"

@Composable
fun HomeScreen(
    photos: Map<PhotoAngle, Uri>,
    isDemo: Boolean,
    onPhotoSelected: (PhotoAngle, Uri) -> Unit,
    onPhotoRemoved: (PhotoAngle) -> Unit,
    onAnalyze: () -> Unit,
) {
    val context = LocalContext.current
    // Kamera açıkken uygulama arka planda kapanabilir; bu yüzden saklanabilir durum.
    var pendingAngle by rememberSaveable { mutableStateOf<PhotoAngle?>(null) }
    var pendingCameraUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val angle = pendingAngle
        if (uri != null && angle != null) onPhotoSelected(angle, uri)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val angle = pendingAngle
        val uri = pendingCameraUri
        if (success && angle != null && uri != null) onPhotoSelected(angle, uri)
    }

    val allReady = PhotoAngle.entries.all { it in photos }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Mantar Tanıma", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(6.dp))
            Text(
                "Mantarı üç açıdan fotoğraflayın; üç fotoğraf birlikte veri tabanıyla karşılaştırılır.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))

            PhotoAngle.entries.forEach { angle ->
                PhotoSlot(
                    angle = angle,
                    uri = photos[angle],
                    onCamera = {
                        val uri = createCameraUri(context)
                        pendingAngle = angle
                        pendingCameraUri = uri
                        cameraLauncher.launch(uri)
                    },
                    onGallery = {
                        pendingAngle = angle
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onRemove = { onPhotoRemoved(angle) },
                )
                Spacer(Modifier.height(10.dp))
            }

            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onAnalyze,
                enabled = allReady,
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Icon(Icons.Filled.Search, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (allReady) "Tanımla" else "Tanımla (${photos.size}/3 fotoğraf)")
            }

            Spacer(Modifier.height(20.dp))
            SafetyNotice()
            if (isDemo) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "Demo modu: tanıma modeli henüz yüklenmedi.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }

            Spacer(Modifier.height(28.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))
            Text("Geliştirici", style = MaterialTheme.typography.labelMedium)
            Text(
                DEVELOPER_NAME,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun PhotoSlot(
    angle: PhotoAngle,
    uri: Uri?,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onRemove: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                if (uri != null) {
                    AsyncImage(
                        model = uri,
                        contentDescription = angle.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        Icons.Filled.AddAPhoto,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(angle.title, style = MaterialTheme.typography.titleMedium)
                    if (uri != null) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "✓",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Text(angle.hint, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilledTonalIconButton(onClick = onCamera) {
                        Icon(Icons.Filled.PhotoCamera, contentDescription = "${angle.title} fotoğraf çek")
                    }
                    FilledTonalIconButton(onClick = onGallery) {
                        Icon(Icons.Filled.PhotoLibrary, contentDescription = "${angle.title} galeriden seç")
                    }
                    if (uri != null) {
                        IconButton(onClick = onRemove) {
                            Icon(Icons.Filled.Close, contentDescription = "${angle.title} fotoğrafını kaldır")
                        }
                    }
                }
            }
        }
    }
}

private fun createCameraUri(context: Context): Uri {
    val dir = File(context.cacheDir, "images").apply { mkdirs() }
    val file = File(dir, "photo_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
