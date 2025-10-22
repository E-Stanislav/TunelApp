package com.tunelapp.ui.tv

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tunelapp.data.ConnectionState
import com.tunelapp.data.VlessServer
import com.tunelapp.utils.toVlessServer
import com.tunelapp.ui.theme.VpnConnected
import com.tunelapp.ui.theme.VpnConnecting
import com.tunelapp.ui.theme.VpnDisconnected
import com.tunelapp.viewmodel.MainViewModel

/**
 * Main screen for Android TV
 * Optimized for D-pad navigation and large screen viewing
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TvMainScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val vpnState by viewModel.vpnState.collectAsState()
    val servers by viewModel.servers.collectAsState()
    
    // VPN permission launcher
    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            vpnState.server?.let { proxyServer ->
                viewModel.connect(context, proxyServer.toVlessServer())
            }
        }
    }
    
    // Check VPN permission
    LaunchedEffect(vpnState.connectionState) {
        if (vpnState.connectionState == ConnectionState.CONNECTING) {
            val intent = VpnService.prepare(context)
            if (intent != null) {
                // For TV, we should show a message about VPN permission
                vpnPermissionLauncher.launch(intent)
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(48.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            // Left side: Connection status
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TvConnectionCard(
                    vpnState = vpnState,
                    onToggle = { viewModel.toggleConnection(context) }
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                TvImportButton(
                    onClick = { viewModel.importFromClipboard(context) }
                )
            }
            
            // Right side: Server list
            Column(
                modifier = Modifier
                    .weight(1.5f)
                    .fillMaxHeight()
            ) {
                Text(
                    text = "TunelApp",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                if (servers.isEmpty()) {
                    TvEmptyServersView()
                } else {
                    TvServerList(
                        servers = servers,
                        currentServer = vpnState.server?.toVlessServer(),
                        onServerSelect = { server ->
                            viewModel.selectServer(server)
                        },
                        onServerDelete = { server ->
                            viewModel.deleteServer(server)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TvConnectionCard(
    vpnState: com.tunelapp.data.VpnState,
    onToggle: () -> Unit
) {
    val color = when (vpnState.connectionState) {
        ConnectionState.CONNECTED -> VpnConnected
        ConnectionState.CONNECTING, ConnectionState.DISCONNECTING -> VpnConnecting
        ConnectionState.DISCONNECTED -> VpnDisconnected
        ConnectionState.ERROR -> MaterialTheme.colorScheme.error
    }
    
    val statusText = when (vpnState.connectionState) {
        ConnectionState.CONNECTED -> "CONNECTED"
        ConnectionState.CONNECTING -> "CONNECTING..."
        ConnectionState.DISCONNECTING -> "DISCONNECTING..."
        ConnectionState.DISCONNECTED -> "DISCONNECTED"
        ConnectionState.ERROR -> "ERROR"
    }
    
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .focusRequester(focusRequester)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (isFocused) BorderStroke(4.dp, MaterialTheme.colorScheme.primary) else null,
        onClick = onToggle
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (vpnState.connectionState == ConnectionState.CONNECTED) {
                    Icons.Default.CheckCircle
                } else {
                    Icons.Default.Close
                },
                contentDescription = statusText,
                modifier = Modifier.size(80.dp),
                tint = color
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = statusText,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            
            vpnState.server?.let { server ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = server.name,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun TvImportButton(onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .then(
                if (isFocused) Modifier.border(4.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                else Modifier
            ),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Icon(
            Icons.Default.Add,
            contentDescription = null,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = "IMPORT FROM CLIPBOARD",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun TvServerList(
    servers: List<VlessServer>,
    currentServer: VlessServer?,
    onServerSelect: (VlessServer) -> Unit,
    onServerDelete: (VlessServer) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Servers",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        items(servers, key = { it.id }) { server ->
            TvServerItem(
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
fun TvServerItem(
    server: VlessServer,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isFocused) BorderStroke(4.dp, MaterialTheme.colorScheme.primary) else null,
        onClick = onSelect
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = server.name,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${server.address}:${server.port}",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun TvEmptyServersView() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "No servers configured",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Import a VLESS URL from clipboard",
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

