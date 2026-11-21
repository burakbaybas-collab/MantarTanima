package com.burakbaybas.mantartanima.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.burakbaybas.mantartanima.PhotoAngle
import com.burakbaybas.mantartanima.R
import java.io.File

const val DEVELOPER_NAME = "Burak BAYBAŞ"
const val INSTAGRAM_HANDLE = "@burak_baybas"
const val INSTAGRAM_URL = "https://www.instagram.com/burak_baybas/"

private val DeepGreen = Color(0xFF17502A)
private val FreshGreen = Color(0xFF4E9A2F)
private val Glass = Color(0xE6FBF8F2)       // buzlu cam kart rengi
private val PillGlass = Color(0xD9E6EFDC)

private fun PhotoAngle.exampleRes() = when (this) {
    PhotoAngle.TOP -> R.drawable.example_top
    PhotoAngle.SIDE -> R.drawable.example_side
    PhotoAngle.BOTTOM -> R.drawable.example_bottom
}

private fun PhotoAngle.cardTitle() = when (this) {
    PhotoAngle.TOP -> "Üstten"
    PhotoAngle.SIDE -> "Yandan"
    PhotoAngle.BOTTOM -> "Alttan"
}

private fun PhotoAngle.cardHint() = when (this) {
    PhotoAngle.TOP -> "Şapka üstü"
    PhotoAngle.SIDE -> "Şapka ve sap"
    PhotoAngle.BOTTOM -> "Lameller / gözenekler"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    photos: Map<PhotoAngle, Uri>,
    isDemo: Boolean,
    onPhotoSelected: (PhotoAngle, Uri) -> Unit,
    onPhotoRemoved: (PhotoAngle) -> Unit,
    onAnalyze: () -> Unit,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    // Kamera açıkken uygulama arka planda kapanabilir; bu yüzden saklanabilir durum.
    var pendingAngle by rememberSaveable { mutableStateOf<PhotoAngle?>(null) }
    var pendingCameraUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var sheetFor by remember { mutableStateOf<PhotoAngle?>(null) }
    var showTips by remember { mutableStateOf(false) }

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

    Box(Modifier.fillMaxSize()) {
        // Orman arka planı + okunabilirlik için açık renk perde
        Image(
            painter = painterResource(R.drawable.bg_forest),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color(0xF2FBF8F2),
                        0.55f to Color(0xCCFBF8F2),
                        1f to Color(0x66FBF8F2),
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Header()
            Spacer(Modifier.height(18.dp))

            // Bilgi şeridi
            Surface(
                shape = RoundedCornerShape(50),
                color = PillGlass,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.7f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    Modifier.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Collections, null, tint = DeepGreen, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Mantarın üç farklı açıdan fotoğrafını yükleyin",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = DeepGreen,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { showTips = true }) {
                        Icon(Icons.Outlined.Info, "İpuçları", tint = DeepGreen)
                    }
                }
            }
            Spacer(Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PhotoAngle.entries.forEach { angle ->
                    AngleCard(
                        angle = angle,
                        uri = photos[angle],
                        onPick = { sheetFor = angle },
                        onRemove = { onPhotoRemoved(angle) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(22.dp))

            AnalyzeButton(
                enabled = allReady,
                label = if (allReady) "Tanımla" else "Tanımla (${photos.size}/3)",
                onClick = onAnalyze,
            )
            if (isDemo) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Demo modu: tanıma modeli yüklenmedi.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }


            Spacer(Modifier.height(20.dp))
            Text("Geliştirici", style = MaterialTheme.typography.labelSmall, color = DeepGreen.copy(alpha = 0.7f))
            Text(
                DEVELOPER_NAME,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = DeepGreen,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { uriHandler.openUri(INSTAGRAM_URL) }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
            Spacer(Modifier.height(8.dp))
        }
    }

    // Kamera / galeri seçimi
    sheetFor?.let { angle ->
        ModalBottomSheet(onDismissRequest = { sheetFor = null }) {
            Text(
                "${angle.cardTitle()} görünüm fotoğrafı",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
            )
            Text(
                angle.hint,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Spacer(Modifier.height(8.dp))
            ListItem(
                headlineContent = { Text("Kamera ile çek") },
                leadingContent = { Icon(Icons.Filled.PhotoCamera, null, tint = DeepGreen) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable {
                    sheetFor = null
                    val uri = createCameraUri(context)
                    pendingAngle = angle
                    pendingCameraUri = uri
                    cameraLauncher.launch(uri)
                },
            )
            ListItem(
                headlineContent = { Text("Galeriden seç") },
                leadingContent = { Icon(Icons.Filled.PhotoLibrary, null, tint = DeepGreen) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable {
                    sheetFor = null
                    pendingAngle = angle
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
            )
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showTips) {
        AlertDialog(
            onDismissRequest = { showTips = false },
            confirmButton = { TextButton(onClick = { showTips = false }) { Text("Tamam") } },
            title = { Text("İyi fotoğraf için ipuçları") },
            text = {
                Text(
                    "• Mantar kadrajın büyük kısmını kaplasın.\n" +
                        "• Gün ışığında, flaşsız çekin.\n" +
                        "• Üstten: şapkanın tamamı görünsün.\n" +
                        "• Yandan: şapka, sap ve sapın tabanı (torba/halka varsa) birlikte görünsün.\n" +
                        "• Alttan: lameller veya gözenekler net görünsün.\n" +
                        "• Mantarı topraktan çıkarırken sap tabanını koparmayın; ayırt edici özellikler oradadır."
                )
            },
        )
    }
}

@Composable
private fun Header() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Image(
            painter = painterResource(R.drawable.logo_mark),
            contentDescription = "FungiLens logosu",
            modifier = Modifier.size(110.dp),
        )
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = DeepGreen)) { append("Fungi") }
                withStyle(SpanStyle(color = FreshGreen)) { append("Lens") }
            },
            fontSize = 40.sp,
            fontWeight = FontWeight.ExtraBold,
            lineHeight = 44.sp,
            textAlign = TextAlign.Center,
        )
        Text(
            "Yapay Zekâ Destekli Mantar Tanımlama Sistemi",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = DeepGreen,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .width(160.dp)
                .height(2.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(FreshGreen.copy(alpha = 0f), FreshGreen, FreshGreen.copy(alpha = 0f))
                    ),
                    RoundedCornerShape(1.dp),
                )
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Doğayı Keşfet, Mantarları Tanı.",
            style = MaterialTheme.typography.bodyMedium,
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun AngleCard(
    angle: PhotoAngle,
    uri: Uri?,
    onPick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier
            .shadow(6.dp, shape, ambientColor = DeepGreen.copy(alpha = 0.25f), spotColor = DeepGreen.copy(alpha = 0.25f))
            .clip(shape)
            .background(Glass)
            .border(1.dp, Color.White.copy(alpha = 0.8f), shape)
            .clickable(onClick = onPick)
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(15.dp)),
        ) {
            if (uri != null) {
                AsyncImage(
                    model = uri,
                    contentDescription = angle.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = "Eklendi",
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(5.dp)
                        .size(22.dp)
                        .background(FreshGreen, CircleShape),
                )
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(26.dp)
                        .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                        .clickable(onClick = onRemove),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Close, "Kaldır", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            } else {
                Image(
                    painter = painterResource(angle.exampleRes()),
                    contentDescription = "${angle.title} örnek fotoğraf",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Text(
                    "Örnek",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .background(DeepGreen.copy(alpha = 0.75f), RoundedCornerShape(50))
                        .border(1.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(50))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            angle.cardTitle(),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = DeepGreen,
        )
        Text(
            angle.cardHint(),
            style = MaterialTheme.typography.labelSmall,
            color = DeepGreen.copy(alpha = 0.75f),
            textAlign = TextAlign.Center,
            maxLines = 2,
            minLines = 2,
        )
        Spacer(Modifier.height(6.dp))
        // Kesik çizgili yükleme alanı
        Column(
            Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawRoundRect(
                        color = DeepGreen.copy(alpha = 0.35f),
                        style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))),
                        cornerRadius = CornerRadius(12.dp.toPx()),
                    )
                }
                .padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Outlined.AddAPhoto, null, tint = DeepGreen, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(2.dp))
            Text(
                if (uri == null) "Fotoğraf Yükle" else "Değiştir",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = DeepGreen,
            )
        }
    }
}

@Composable
private fun AnalyzeButton(enabled: Boolean, label: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    val brush = if (enabled) {
        Brush.verticalGradient(listOf(Color(0xFF2E7D3A), DeepGreen))
    } else {
        Brush.verticalGradient(listOf(Color(0xFF9DB39F), Color(0xFF86A08A)))
    }
    Box(
        Modifier
            .fillMaxWidth(0.85f)
            .height(60.dp)
            .shadow(if (enabled) 10.dp else 0.dp, shape, spotColor = DeepGreen)
            .clip(shape)
            .background(brush)
            .border(2.dp, Color.White.copy(alpha = 0.35f), shape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(10.dp))
            Text(label, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private fun createCameraUri(context: Context): Uri {
    val dir = File(context.cacheDir, "images").apply { mkdirs() }
    val file = File(dir, "photo_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
