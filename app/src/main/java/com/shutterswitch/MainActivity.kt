package com.shutterswitch

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.os.Environment
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.material.icons.Icons
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    // Permission launcher for POST_NOTIFICATIONS (Android 13+)
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Notification will show if granted; silently ignored if denied */ }

    private var downloadId: Long = -1

    private val downloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (id == downloadId && downloadId != -1L) {
                val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                val uri = downloadManager.getUriForDownloadedFile(downloadId)
                if (uri != null) {
                    val installIntent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/vnd.android.package-archive")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                    }
                    startActivity(installIntent)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(downloadReceiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(downloadReceiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        }

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Request SYSTEM_ALERT_WINDOW permission
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Please allow 'Display over other apps' to keep the screen on", Toast.LENGTH_LONG).show()
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }

        setContent {
            ShutterSwitchTheme {
                ShutterSwitchScreen(
                    onSwitchOn = { startWakeLockService() },
                    onSwitchOff = { stopWakeLockService() },
                    packageManager = packageManager,
                    packageName = packageName,
                    onDownloadRequest = { url -> downloadAndInstallUpdate(url) }
                )
            }
        }
    }

    private fun startWakeLockService() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Permission required: Display over other apps", Toast.LENGTH_LONG).show()
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
            return
        }
        val intent = Intent(this, WakeLockService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }

    private fun stopWakeLockService() {
        val intent = Intent(this, WakeLockService::class.java)
        stopService(intent)
    }

    private fun downloadAndInstallUpdate(url: String) {
        val request = DownloadManager.Request(Uri.parse(url)).apply {
            setTitle("App Update")
            setDescription("Downloading latest version...")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "app-update.apk")
        }
        val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadId = downloadManager.enqueue(request)
        Toast.makeText(this, "Downloading update...", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(downloadReceiver)
        } catch (e: Exception) {
            // Ignore if not registered
        }
        // Stop the service when activity is destroyed to avoid orphaned wake locks
        stopWakeLockService()
    }
}

// ─────────────────────────────────────────────
// Compose UI
// ─────────────────────────────────────────────

@Composable
fun ShutterSwitchScreen(
    onSwitchOn: () -> Unit,
    onSwitchOff: () -> Unit,
    packageManager: PackageManager,
    packageName: String,
    onDownloadRequest: (String) -> Unit
) {
    val isOn by WakeLockService.isServiceRunning.collectAsState()

    val currentVersion = remember {
        try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0"
        } catch (e: Exception) {
            "1.0"
        }
    }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var newVersion by remember { mutableStateOf("") }
    var apkUrl by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        thread {
            try {
                val url = URL("https://api.github.com/repos/saheermk/no-sleep/releases/latest")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val json = JSONObject(response)
                    val tagName = json.getString("tag_name")
                    val cleanTagName = tagName.removePrefix("v")
                    
                    if (cleanTagName != currentVersion) {
                        val assets = json.getJSONArray("assets")
                        for (i in 0 until assets.length()) {
                            val asset = assets.getJSONObject(i)
                            if (asset.getString("name").endsWith(".apk")) {
                                newVersion = cleanTagName
                                apkUrl = asset.getString("browser_download_url")
                                showUpdateDialog = true
                                break
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Animate background gradient
    val bgColorTop by animateColorAsState(
        targetValue = if (isOn) Color(0xFF0A1628) else Color(0xFF1A1A2E),
        animationSpec = tween(600), label = "bgTop"
    )
    val bgColorBottom by animateColorAsState(
        targetValue = if (isOn) Color(0xFF001F4D) else Color(0xFF16213E),
        animationSpec = tween(600), label = "bgBottom"
    )

    // Glow scale animation
    val glowScale by animateFloatAsState(
        targetValue = if (isOn) 1.4f else 0.6f,
        animationSpec = spring(stiffness = Spring.StiffnessLow), label = "glow"
    )

    if (showUpdateDialog) {
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            title = { Text("Update Available") },
            text = { Text("A new version (v$newVersion) is available. Would you like to update?") },
            confirmButton = {
                TextButton(onClick = {
                    showUpdateDialog = false
                    onDownloadRequest(apkUrl)
                }) {
                    Text("Update")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateDialog = false }) {
                    Text("Later")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(bgColorTop, bgColorBottom))),
        contentAlignment = Alignment.Center
    ) {
        // Ambient glow blob behind the switch
        Box(
            modifier = Modifier
                .size(320.dp)
                .scale(glowScale)
                .blur(80.dp)
                .background(
                    color = if (isOn) Color(0x6600CFFF) else Color(0x22334455),
                    shape = CircleShape
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // App title
            Text(
                text = "NO\nSLEEP",
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 8.sp,
                lineHeight = 42.sp,
                color = Color(0xFFE0F4FF),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Status subtitle
            val statusText by remember(isOn) {
                derivedStateOf { if (isOn) "NO SLEEP ACTIVE" else "DEVICE CAN SLEEP" }
            }
            val statusColor by animateColorAsState(
                targetValue = if (isOn) Color(0xFF00CFFF) else Color(0xFF667788),
                animationSpec = tween(400), label = "statusColor"
            )
            Text(
                text = statusText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 3.sp,
                color = statusColor
            )

            Spacer(modifier = Modifier.height(72.dp))

            // The large Shutter Switch toggle
            LargeShutterToggle(
                isOn = isOn,
                onToggle = { newState ->
                    if (newState) onSwitchOn() else onSwitchOff()
                }
            )

            Spacer(modifier = Modifier.height(56.dp))

            // Info card
            InfoCard(isOn = isOn)

            Spacer(modifier = Modifier.height(32.dp))

            // Developer Info
            DeveloperInfo()
        }
    }
}

@Composable
fun LargeShutterToggle(
    isOn: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val trackColor by animateColorAsState(
        targetValue = if (isOn) Color(0xFF005F7A) else Color(0xFF1E2A38),
        animationSpec = tween(400), label = "track"
    )
    val thumbColor by animateColorAsState(
        targetValue = if (isOn) Color(0xFF00CFFF) else Color(0xFF445566),
        animationSpec = tween(400), label = "thumb"
    )
    val thumbOffsetDp by animateDpAsState(
        targetValue = if (isOn) 68.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "thumbOffset"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Track
        Box(
            modifier = Modifier
                .width(160.dp)
                .height(88.dp)
                .clip(RoundedCornerShape(44.dp))
                .background(trackColor)
                .padding(8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            // Thumb
            Box(
                modifier = Modifier
                    .offset(x = thumbOffsetDp)
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(thumbColor),
                contentAlignment = Alignment.Center
            ) {
                // Power icon indicator
                Icon(
                    painter = painterResource(id = if (isOn) R.drawable.ic_sun else R.drawable.ic_moon),
                    contentDescription = "Toggle Icon",
                    modifier = Modifier.size(32.dp),
                    tint = if (isOn) Color(0xFF001A22) else Color(0xFF223344)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ON / OFF tap button
        Button(
            onClick = { onToggle(!isOn) },
            modifier = Modifier
                .width(160.dp)
                .height(52.dp),
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isOn) Color(0xFF00CFFF) else Color(0xFF2A3A4A),
                contentColor = if (isOn) Color(0xFF001A22) else Color(0xFF99BBCC)
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = if (isOn) 8.dp else 2.dp
            )
        ) {
            Text(
                text = if (isOn) "TURN OFF" else "TURN ON",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                letterSpacing = 2.sp
            )
        }
    }
}

@Composable
fun InfoCard(isOn: Boolean) {
    val cardBg by animateColorAsState(
        targetValue = if (isOn) Color(0x2200CFFF) else Color(0x111E2A38),
        animationSpec = tween(500), label = "cardBg"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        color = cardBg,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(id = if (isOn) R.drawable.ic_sun else R.drawable.ic_lightbulb),
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = if (isOn) Color(0xFFAAEEFF) else Color(0xFF88AABB)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isOn)
                    "Screen is forced ON\nYour device will not sleep while this is active."
                else
                    "Quick Tip:\nPull down your Quick Settings (notification panel), tap the edit icon, and add the 'No Sleep' tile for easy access!",
                fontSize = 14.sp,
                color = if (isOn) Color(0xFFAAEEFF) else Color(0xFF88AABB),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
        }
    }
}

@Composable
fun DeveloperInfo() {
    val uriHandler = LocalUriHandler.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Developed by saheermk",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00CFFF)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Creative Developer blending design\nand engineering to create immersive apps.",
                fontSize = 12.sp,
                color = Color(0xFF667788),
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Socials Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Website
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { uriHandler.openUri("https://saheermk.pages.dev") }.padding(4.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_website),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color(0xFFAAEEFF)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Website",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFAAEEFF)
                    )
                }

                // LinkedIn
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { uriHandler.openUri("https://in.linkedin.com/in/saheermk") }.padding(4.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_linkedin),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color(0xFFAAEEFF)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "LinkedIn",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFAAEEFF)
                    )
                }

                // GitHub
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { uriHandler.openUri("https://github.com/saheermk/") }.padding(4.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_github),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color(0xFFAAEEFF)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "GitHub",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFAAEEFF)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// Theme
// ─────────────────────────────────────────────

@Composable
fun ShutterSwitchTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF00CFFF),
            background = Color(0xFF0A1628),
            surface = Color(0xFF16213E)
        ),
        content = content
    )
}
