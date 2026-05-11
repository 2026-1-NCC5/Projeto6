// ui/screens/camera/CameraScreen.kt
package com.AlimempatIA.stockai.ui.screens.camera

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import com.AlimempatIA.stockai.ui.viewmodel.SessionUiState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.AlimempatIA.stockai.ui.components.AppBottomNavBar
import com.AlimempatIA.stockai.ui.theme.*
import com.AlimempatIA.stockai.ui.viewmodel.SessionViewModel
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

// ─────────────────────────────────────────────────────────────────────────────
// Root screen — trata permissão e direciona para o conteúdo
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun CameraScreen(
    navController: NavController,
    userId: String = "default_user",
    userName: String = "Usuário"
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    Scaffold(
        containerColor = BackgroundDark,
        bottomBar = { AppBottomNavBar(navController = navController) }
    ) { paddingValues ->
        if (hasPermission) {
            CameraContent(
                modifier = Modifier.padding(paddingValues),
                userId = userId,
                userName = userName
            )
        } else {
            PermissionRequestView(
                modifier = Modifier.padding(paddingValues),
                onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Tela de solicitação de permissão
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PermissionRequestView(modifier: Modifier, onRequest: () -> Unit) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier.size(88.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(AIGlow.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.CameraAlt, null, tint = AIGlowLight, modifier = Modifier.size(44.dp))
            }
            Text(
                "Permissao de Camera",
                fontSize = 22.sp, fontWeight = FontWeight.ExtraBold,
                color = TextPrimary, textAlign = TextAlign.Center
            )
            Text(
                "Para escanear QR Codes das maquinas, o aplicativo precisa de acesso a camera.",
                fontSize = 14.sp, color = TextSecondary, textAlign = TextAlign.Center
            )
            Button(
                onClick = onRequest,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.CameraAlt, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Permitir Camera", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Conteúdo principal — preview + overlay + resultado
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun CameraContent(modifier: Modifier, userId: String, userName: String) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val sessionViewModel: SessionViewModel = viewModel()

    // Estado
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var scannedValue by remember { mutableStateOf<String?>(null) }
    var isScanning by remember { mutableStateOf(true) }
    var flashEnabled by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var cameraError by remember { mutableStateOf(false) }

    val sessionState by sessionViewModel.sessionState.collectAsState()
    val isLoading by sessionViewModel.isLoading.collectAsState()

    val previewView = remember { PreviewView(context) }

    LaunchedEffect(scannedValue) {
        if (scannedValue != null && isScanning) {
            sessionViewModel.startSession(
                qrCode = scannedValue!!,
                userId = userId,
                userName = userName,
                sessionType = "WORK"
            )
        }
    }

    LaunchedEffect(sessionState) {
        when (sessionState) {
            is SessionUiState.Completed -> {
                android.util.Log.d("CameraScreen", "Session completed successfully")
            }
            is SessionUiState.Error -> {
                android.util.Log.e("CameraScreen", "Session error: ${(sessionState as SessionUiState.Error).message}")
                kotlinx.coroutines.delay(3000)
                scannedValue = null
                isScanning = true
            }
            else -> {}
        }
    }

    // Liga a câmera sempre que o lensFacing mudar
    LaunchedEffect(lensFacing) {
        cameraError = false
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            try {
                val provider = future.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val selector = CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build()
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { ia ->
                        ia.setAnalyzer(executor, QrAnalyzer { result ->
                            if (isScanning) {
                                scannedValue = result
                                isScanning = false
                            }
                        })
                    }
                provider.unbindAll()
                camera = provider.bindToLifecycle(lifecycleOwner, selector, preview, analysis)
            } catch (e: Exception) {
                cameraError = true
            }
        }, ContextCompat.getMainExecutor(context))
    }

    // Sincroniza flash
    LaunchedEffect(flashEnabled) {
        camera?.cameraControl?.enableTorch(flashEnabled)
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {

        // ── Preview da câmera ──────────────────────────────────────────────
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        // ── Overlay escuro com "buraco" no quadro de scan ──────────────────
        if (!cameraError && isScanning) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val frameSize = size.width * 0.68f
                val cx = size.width / 2f
                val cy = size.height / 2f - size.height * 0.04f
                val left = cx - frameSize / 2f
                val top = cy - frameSize / 2f

                val frameColor = if (scannedValue != null) StatusGreen else PrimaryBlue

                val outer = Path().apply {
                    addRect(Rect(0f, 0f, size.width, size.height))
                }
                val hole = Path().apply {
                    addRoundRect(
                        RoundRect(
                            left = left,
                            top = top,
                            right = left + frameSize,
                            bottom = top + frameSize,
                            cornerRadius = CornerRadius(24f, 24f)
                        )
                    )
                }
                val overlay = Path()
                overlay.op(outer, hole, PathOperation.Difference)
                drawPath(overlay, Color.Black.copy(alpha = 0.58f))

                val sw = 4.dp.toPx()
                val cl = 38.dp.toPx()
                val cr = 20.dp.toPx()
                val r = left + frameSize
                val b = top + frameSize

                val corners = listOf(
                    Offset(left, top + cr) to Offset(left, top + cl),
                    Offset(left + cr, top) to Offset(left + cl, top),
                    Offset(r, top + cr) to Offset(r, top + cl),
                    Offset(r - cr, top) to Offset(r - cl, top),
                    Offset(left, b - cr) to Offset(left, b - cl),
                    Offset(left + cr, b) to Offset(left + cl, b),
                    Offset(r, b - cr) to Offset(r, b - cl),
                    Offset(r - cr, b) to Offset(r - cl, b)
                )
                corners.forEach { (s, e) ->
                    drawLine(frameColor, s, e, sw, cap = StrokeCap.Round)
                }
            }
        }

        // ── Loading overlay ────────────────────────────────────────────────
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = PrimaryBlue)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Iniciando sessão...",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // ── Barra superior (título + flash + flip câmera) ──────────────────
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
                        .background(AIGlow.copy(0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.QrCodeScanner, null, tint = AIGlowLight, modifier = Modifier.size(20.dp))
                }
                Column {
                    Text("Scanner QR Code", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Aponte para o QR da maquina", fontSize = 11.sp, color = Color.White.copy(0.55f))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                    IconButton(
                        onClick = { flashEnabled = !flashEnabled },
                        modifier = Modifier.size(40.dp).clip(CircleShape)
                            .background(if (flashEnabled) StatusYellow.copy(0.3f) else Color.White.copy(0.1f))
                    ) {
                        Icon(
                            if (flashEnabled) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                            null,
                            tint = if (flashEnabled) StatusYellow else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                IconButton(
                    onClick = {
                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
                        flashEnabled = false
                    },
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(0.12f))
                ) {
                    Icon(Icons.Filled.FlipCameraAndroid, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }

        // ── Label central (instrução) ──────────────────────────────────────
        if (isScanning && !cameraError) {
            Text(
                "Posicione o QR Code dentro do quadro",
                fontSize = 12.sp,
                color = Color.White.copy(0.85f),
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (160).dp)
                    .background(Color.Black.copy(0.45f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }

        // ── Erro de câmera ─────────────────────────────────────────────────
        if (cameraError) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardDark)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Filled.VideocamOff, null, tint = StatusRed, modifier = Modifier.size(40.dp))
                    Text("Camera nao disponivel", fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                    Text("Tente alternar entre frontal e traseira", fontSize = 12.sp, color = TextSecondary, textAlign = TextAlign.Center)
                    OutlinedButton(
                        onClick = {
                            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.FlipCameraAndroid, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Alternar camera")
                    }
                }
            }
        }

        // ── Card de resultado do scan ──────────────────────────────────────
        AnimatedVisibility(
            visible = scannedValue != null && !isLoading,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut()
        ) {
            scannedValue?.let { value ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(0.82f))
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp))
                                    .background(StatusGreen.copy(0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.CheckCircle, null, tint = StatusGreen, modifier = Modifier.size(22.dp))
                            }
                            Column {
                                Text("QR Code detectado!", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = StatusGreen)
                                Text("Iniciando sessão...", fontSize = 11.sp, color = Color.White.copy(0.55f))
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(SurfaceDark)
                                .padding(12.dp)
                        ) {
                            Text(value, fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Analisador de QR Code usando ML Kit
// ─────────────────────────────────────────────────────────────────────────────
@SuppressLint("UnsafeOptInUsageError")
private class QrAnalyzer(private val onResult: (String) -> Unit) : ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient()

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                barcodes
                    .filter { it.valueType == Barcode.TYPE_TEXT || it.rawValue != null }
                    .mapNotNull { it.rawValue }
                    .firstOrNull()
                    ?.let { onResult(it) }
            }
            .addOnCompleteListener { imageProxy.close() }
    }
}