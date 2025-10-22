package com.tunelapp.ui.mobile

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tunelapp.R
import com.tunelapp.data.ConnectionState
import com.tunelapp.data.VlessServer
import com.tunelapp.ui.theme.VpnConnected
import com.tunelapp.ui.theme.VpnConnecting
import com.tunelapp.ui.theme.VpnDisconnected
import com.tunelapp.viewmodel.MainViewModel

/**
 * Main screen for mobile devices
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val vpnState by viewModel.vpnState.collectAsState()
    val servers by viewModel.servers.collectAsState()
    val importResult by viewModel.importResult.collectAsState()
    
    // Dialog state
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    
    // VPN permission launcher
    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            vpnState.server?.let { server ->
                viewModel.connect(context, server)
            }
        }
    }
    
    // Check VPN permission when needed
    LaunchedEffect(vpnState.connectionState) {
        if (vpnState.connectionState == ConnectionState.CONNECTING) {
            val intent = VpnService.prepare(context)
            if (intent != null) {
                // Show permission dialog first
                errorMessage = context.getString(R.string.vpn_permission_message)
                showErrorDialog = true
                vpnPermissionLauncher.launch(intent)
            }
        }
    }
    
    // Show import result
    LaunchedEffect(importResult) {
        importResult?.let { result ->
            if (result.isSuccess) {
                // Success handled by ViewModel
            }
            viewModel.clearImportResult()
        }
    }
    
    // Show error dialog when error message changes
    LaunchedEffect(vpnState.errorMessage) {
        vpnState.errorMessage?.let { error ->
            errorMessage = error
            showErrorDialog = true
            viewModel.clearError()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.importFromClipboard(context) },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Import")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Connection status card
            ConnectionCard(
                vpnState = vpnState,
                onToggle = { viewModel.toggleConnection(context) },
                modifier = Modifier.padding(16.dp)
            )
            
            // Server list
            if (servers.isEmpty()) {
                EmptyServersView(
                    onImport = { viewModel.importFromClipboard(context) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                )
            } else {
                ServerList(
                    servers = servers,
                    currentServer = vpnState.server,
                    onServerSelect = { server ->
                        viewModel.selectServer(server)
                    },
                    onServerDelete = { server ->
                        viewModel.deleteServer(server)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
    
    // Error dialog
    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.warning),
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { showErrorDialog = false }
                ) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }
}

@Composable
fun ConnectionCard(
    vpnState: com.tunelapp.data.VpnState,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (vpnState.connectionState == ConnectionState.CONNECTED) 1.1f else 1f,
        label = "scale"
    )
    
    val color = when (vpnState.connectionState) {
        ConnectionState.CONNECTED -> VpnConnected
        ConnectionState.CONNECTING, ConnectionState.DISCONNECTING -> VpnConnecting
        ConnectionState.DISCONNECTED -> VpnDisconnected
        ConnectionState.ERROR -> MaterialTheme.colorScheme.error
    }
    
    val statusText = when (vpnState.connectionState) {
        ConnectionState.CONNECTED -> stringResource(R.string.connected)
        ConnectionState.CONNECTING -> stringResource(R.string.connecting)
        ConnectionState.DISCONNECTING -> stringResource(R.string.disconnecting)
        ConnectionState.DISCONNECTED -> stringResource(R.string.disconnected)
        ConnectionState.ERROR -> "Error"
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Connection button
            Surface(
                modifier = Modifier
                    .size(120.dp)
                    .scale(scale),
                shape = CircleShape,
                color = color,
                onClick = onToggle
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (vpnState.connectionState == ConnectionState.CONNECTED) {
                            Icons.Default.CheckCircle
                        } else {
                            Icons.Default.Close
                        },
                        contentDescription = statusText,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                    
                    // Show warning icon when no server is selected
                    if (vpnState.connectionState == ConnectionState.DISCONNECTED && vpnState.server == null) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "No server selected",
                            modifier = Modifier
                                .size(20.dp)
                                .align(Alignment.TopEnd)
                                .offset(x = 8.dp, y = (-8).dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Status text
            Text(
                text = statusText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            // Server name
            vpnState.server?.let { server ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = server.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Traffic stats
            AnimatedVisibility(visible = vpnState.connectionState == ConnectionState.CONNECTED) {
                Column(
                    modifier = Modifier.padding(top = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        TrafficStat(
                            label = "↑",
                            value = formatBytes(vpnState.stats.uploadSpeed)
                        )
                        TrafficStat(
                            label = "↓",
                            value = formatBytes(vpnState.stats.downloadSpeed)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TrafficStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ServerList(
    servers: List<VlessServer>,
    currentServer: VlessServer?,
    onServerSelect: (VlessServer) -> Unit,
    onServerDelete: (VlessServer) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        item {
            Text(
                text = "Servers",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )
        }
        
        items(servers, key = { it.id }) { server ->
            ServerItem(
                server = server,
                isSelected = server.id == currentServer?.id,
                onSelect = { onServerSelect(server) },
                onDelete = { onServerDelete(server) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerItem(
    server: VlessServer,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        onClick = onSelect
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = server.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${server.address}:${server.port}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
    
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Server") },
            text = { Text("Are you sure you want to delete ${server.name}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun EmptyServersView(
    onImport: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = stringResource(R.string.no_servers),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(onClick = onImport) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.import_from_clipboard))
        }
    }
}

fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B/s"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB/s"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB/s"
        else -> "${bytes / (1024 * 1024 * 1024)} GB/s"
    }
}

