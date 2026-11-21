package com.burakbaybas.mantartanima.ui

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.burakbaybas.mantartanima.R

private const val PREFS = "fungilens"
private const val KEY_ACCEPTED = "uyari_onaylandi_v1"

object Disclaimer {
    fun isAccepted(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ACCEPTED, false)

    fun accept(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_ACCEPTED, true).apply()
}

private val DeepGreen = Color(0xFF17502A)
private val WarnRed = Color(0xFFB3261E)

/** Uygulama ilk açıldığında gösterilen, onaylanması gereken güvenlik uyarısı. */
@Composable
fun DisclaimerScreen(onAccept: () -> Unit) {
    var checked by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.bg_forest),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(Modifier.fillMaxSize().background(Color(0xD9FBF8F2)))

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.logo_mark),
                contentDescription = "FungiLens logosu",
                modifier = Modifier.size(90.dp),
            )
            Spacer(Modifier.height(12.dp))

            val shape = RoundedCornerShape(22.dp)
            Column(
                Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, shape, spotColor = DeepGreen.copy(alpha = 0.3f))
                    .clip(shape)
                    .background(Color(0xF2FFFFFF))
                    .border(1.dp, Color.White, shape)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(Icons.Filled.Warning, null, tint = WarnRed, modifier = Modifier.size(40.dp))
                Spacer(Modifier.height(8.dp))
                Text(
                    "Önemli Güvenlik Uyarısı",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = WarnRed,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(14.dp))
                Bullet("FungiLens yapay zekâ ile tahmin yapan bir yardımcı araçtır; sonuçları hatalı olabilir.")
                Bullet("Bir mantarın yenilebilir olduğuna asla yalnızca bu uygulamaya bakarak karar vermeyin.")
                Bullet("Bazı ölümcül zehirli türler, yenen türlere çok benzer. Köygöçüren gibi türlerde tek bir mantar ölüme yol açabilir.")
                Bullet("Toplanan mantarları tüketmeden önce mutlaka bir mantar uzmanına gösterin.")
                Bullet("Zehirlenme şüphesinde vakit kaybetmeden 112'yi veya Ulusal Zehir Danışma Merkezi'ni (114) arayın.")

                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { checked = !checked }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = { checked = it },
                        colors = CheckboxDefaults.colors(checkedColor = DeepGreen),
                    )
                    Text(
                        "Uyarıyı okudum ve anladım.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = DeepGreen,
                    )
                }
                Spacer(Modifier.height(10.dp))

                val btnShape = RoundedCornerShape(50)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(btnShape)
                        .background(
                            if (checked) Brush.verticalGradient(listOf(Color(0xFF2E7D3A), DeepGreen))
                            else Brush.verticalGradient(listOf(Color(0xFFA9BCAB), Color(0xFF93A996)))
                        )
                        .clickable(enabled = checked, onClick = onAccept),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Onaylıyorum, devam et", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun Bullet(text: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text("•", color = DeepGreen, fontWeight = FontWeight.Bold, modifier = Modifier.width(16.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF2B2B2B))
    }
}
