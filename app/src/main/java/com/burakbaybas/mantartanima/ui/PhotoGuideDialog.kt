package com.burakbaybas.mantartanima.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.burakbaybas.mantartanima.R

private val GuideGreen = Color(0xFF1B5E2E)
private val GoodGreen = Color(0xFF1F8F4E)
private val BadRed = Color(0xFFC62828)

private data class GuideStep(
    val number: Int,
    val title: String,
    @DrawableRes val image: Int,
    val points: List<String>,
)

private val steps = listOf(
    GuideStep(
        1, "Üstten", R.drawable.example_top,
        listOf("Şapkanın tamamı", "Renk ve pullar net"),
    ),
    GuideStep(
        2, "Yandan", R.drawable.example_side,
        listOf("Şapka, sap, taban", "Halka ve torba"),
    ),
    GuideStep(
        3, "Alttan", R.drawable.example_bottom,
        listOf("Lamel / gözenek", "Sap bağlantısı"),
    ),
)

/** Ana sayfadaki ⓘ düğmesiyle açılan, örnek görselli fotoğraf çekim rehberi. */
@Composable
fun PhotoGuideDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            modifier = Modifier.fillMaxWidth(0.94f),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.5.dp, Color(0xFFB9E2C6)),
        ) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(38.dp).background(Color(0xFFEAF7EE), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Outlined.PhotoCamera, null, tint = GuideGreen) }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Fotoğraf Rehberi", fontWeight = FontWeight.Bold, fontSize = 19.sp, color = GuideGreen)
                        Text("Aynı mantarı üç açıdan çekin", style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF5B6B5E))
                    }
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, "Kapat") }
                }

                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    steps.forEach { StepCard(it, Modifier.weight(1f)) }
                }

                Spacer(Modifier.height(12.dp))
                Text("Doğru ve yanlış çekim", fontWeight = FontWeight.Bold, color = GuideGreen,
                    style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ExampleTile("Doğru", "Yakın, net", good = true, Modifier.weight(1f)) {
                        GuideImage(R.drawable.example_side)
                    }
                    ExampleTile("Uzak", "Mantar küçük", good = false, Modifier.weight(1f)) {
                        Box(Modifier.fillMaxSize().background(Color(0xFF6E7B5B)), contentAlignment = Alignment.Center) {
                            Image(
                                painterResource(R.drawable.example_side), null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(0.38f).clip(RoundedCornerShape(4.dp)),
                            )
                        }
                    }
                    ExampleTile("Bulanık", "Titrek çekim", good = false, Modifier.weight(1f)) {
                        GuideImage(R.drawable.example_side, Modifier.blur(6.dp))
                    }
                    ExampleTile("Karanlık", "Az ışık", good = false, Modifier.weight(1f)) {
                        GuideImage(
                            R.drawable.example_side,
                            colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToScale(0.3f, 0.3f, 0.3f, 1f) }),
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                TipRow(Icons.Filled.CheckCircle, GoodGreen, "Üç fotoğraf da aynı mantarın olsun; tek mantar kadrajı doldursun.")
                TipRow(Icons.Filled.CheckCircle, GoodGreen, "Gün ışığında, flaşsız çekin.")

                Spacer(Modifier.height(6.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFF1E4), RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFFF5C99A), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Warning, null, tint = Color(0xFFD97706), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Sap tabanını koparmayın: ölümcül Amanita'ların torbası (volva) toprağın içindedir.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF7A3E00),
                    )
                }

                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GuideGreen),
                    shape = RoundedCornerShape(14.dp),
                ) { Text("Anladım", fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

@Composable
private fun StepCard(step: GuideStep, modifier: Modifier) {
    Column(
        modifier
            .background(Color(0xFFF4F9F1), RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFFD5E8CC), RoundedCornerShape(12.dp))
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            Image(
                painterResource(step.image), step.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(9.dp)),
            )
            Box(
                Modifier
                    .padding(4.dp)
                    .size(22.dp)
                    .background(GuideGreen, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Text("${step.number}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
        }
        Spacer(Modifier.height(4.dp))
        Text(step.title, fontWeight = FontWeight.Bold, color = GuideGreen, fontSize = 15.sp)
        step.points.forEach { p ->
            Text(p, fontSize = 11.sp, lineHeight = 13.sp, color = Color(0xFF33373D), textAlign = TextAlign.Center, maxLines = 1)
        }
    }
}

@Composable
private fun GuideImage(
    @DrawableRes res: Int,
    modifier: Modifier = Modifier,
    colorFilter: ColorFilter? = null,
) {
    Image(
        painterResource(res), null,
        contentScale = ContentScale.Crop,
        colorFilter = colorFilter,
        modifier = modifier.fillMaxSize(),
    )
}

@Composable
private fun ExampleTile(
    title: String,
    subtitle: String,
    good: Boolean,
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    val color = if (good) GoodGreen else BadRed
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(10.dp))
                .border(2.dp, color, RoundedCornerShape(10.dp)),
        ) {
            content()
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(3.dp)
                    .background(Color.White, CircleShape),
            ) {
                Icon(
                    if (good) Icons.Filled.CheckCircle else Icons.Filled.Cancel, null,
                    tint = color, modifier = Modifier.size(18.dp),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(title, fontWeight = FontWeight.Bold, color = color, fontSize = 12.sp, textAlign = TextAlign.Center)
        Text(subtitle, fontSize = 10.sp, color = Color(0xFF5B6B5E), textAlign = TextAlign.Center, maxLines = 1)
    }
}

@Composable
private fun TipRow(icon: ImageVector, tint: Color, text: String) {
    Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = Color(0xFF33373D))
    }
}
