package com.example.sensus.presentation.buyer

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.sensus.theme.SensusBackground
import com.example.sensus.theme.SensusPurple
import com.example.sensus.theme.SensusTextPrimary
import com.example.sensus.theme.SensusTextSecondary
import kotlin.random.Random

private const val QR_GRID_SIZE = 19
private const val FINDER_SIZE = 5
private const val FINDER_EDGE = 6
private const val FINDER_CENTER = 2
private const val CENTER_EMBLEM_MIN = 7
private const val CENTER_EMBLEM_MAX = 11
private const val QR_CELL_RATIO = 0.92f
private const val QR_DARK_HEX = 0xFF1E1B4B

@Composable
internal fun QrInviteDialog(inviteCode: String, onDismiss: () -> Unit, onCopyCode: () -> Unit, onShareCode: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 12.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                QrDialogHeader(onDismiss = onDismiss)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Muestra este código a tus amigos para que lo escaneen desde la app y se agreguen a tu red.",
                    fontSize = 12.sp,
                    color = SensusTextSecondary,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(18.dp))
                QrPreviewBox(inviteCode = inviteCode)
                Spacer(modifier = Modifier.height(14.dp))
                Surface(shape = RoundedCornerShape(8.dp), color = SensusBackground) {
                    Text(
                        text = inviteCode,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = SensusPurple,
                        letterSpacing = 2.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
                Spacer(modifier = Modifier.height(18.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onCopyCode, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f)) {
                        Text("Copiar código", fontSize = 12.sp, color = SensusPurple)
                    }
                    Button(
                        onClick = onShareCode,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SensusPurple),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Compartir", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun QrDialogHeader(onDismiss: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(text = "Tu Código QR de Usuario", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary)
        IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
            Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar", tint = SensusTextSecondary)
        }
    }
}

@Composable
private fun QrPreviewBox(inviteCode: String) {
    Box(
        modifier = Modifier
            .size(200.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        StylizedQrCanvas(seed = inviteCode)
        Surface(shape = CircleShape, color = SensusPurple, modifier = Modifier.size(36.dp), shadowElevation = 4.dp) {
            Box(contentAlignment = Alignment.Center) {
                Icon(imageVector = Icons.Default.Group, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun StylizedQrCanvas(seed: String) {
    val darkCells = remember(seed) { buildQrMatrix(seed) }
    Canvas(modifier = Modifier.fillMaxSize()) {
        val cellWidth = size.width / QR_GRID_SIZE
        val cellHeight = size.height / QR_GRID_SIZE
        val darkColor = Color(QR_DARK_HEX)
        for (r in 0 until QR_GRID_SIZE) {
            for (c in 0 until QR_GRID_SIZE) {
                if (darkCells[r][c]) {
                    drawRect(
                        color = darkColor,
                        topLeft = Offset(c * cellWidth, r * cellHeight),
                        size = Size(cellWidth * QR_CELL_RATIO, cellHeight * QR_CELL_RATIO)
                    )
                }
            }
        }
    }
}

private fun buildQrMatrix(seed: String): Array<BooleanArray> {
    val rng = Random(seed.hashCode())
    val cells = Array(QR_GRID_SIZE) { BooleanArray(QR_GRID_SIZE) }
    stampFinderPatterns(cells)
    for (r in 0 until QR_GRID_SIZE) {
        for (c in 0 until QR_GRID_SIZE) {
            if (!isReservedQrZone(r, c)) cells[r][c] = rng.nextBoolean()
        }
    }
    return cells
}

private fun stampFinderPatterns(cells: Array<BooleanArray>) {
    for (i in 0 until FINDER_SIZE) {
        for (j in 0 until FINDER_SIZE) {
            val isEdge = i == 0 || i == FINDER_SIZE - 1 || j == 0 || j == FINDER_SIZE - 1
            val isCenter = i == FINDER_CENTER && j == FINDER_CENTER
            val active = isEdge || isCenter
            cells[i][j] = active
            cells[i][QR_GRID_SIZE - FINDER_SIZE + j] = active
            cells[QR_GRID_SIZE - FINDER_SIZE + i][j] = active
        }
    }
}

private fun isReservedQrZone(row: Int, col: Int): Boolean {
    val inCornerFinder = (row < FINDER_EDGE && col < FINDER_EDGE) ||
        (row < FINDER_EDGE && col >= QR_GRID_SIZE - FINDER_EDGE) ||
        (row >= QR_GRID_SIZE - FINDER_EDGE && col < FINDER_EDGE)
    val inCenterEmblem = row in CENTER_EMBLEM_MIN..CENTER_EMBLEM_MAX && col in CENTER_EMBLEM_MIN..CENTER_EMBLEM_MAX
    return inCornerFinder || inCenterEmblem
}
