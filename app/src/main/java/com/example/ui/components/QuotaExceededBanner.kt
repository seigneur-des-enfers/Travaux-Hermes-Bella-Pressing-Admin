package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.di.Graph
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Bannière visuelle informant les administrateurs et livreurs de l'état du mode dégradé :
 * - Quota Firestore Spark Plan saturé (RESOURCE_EXHAUSTED) ou réseau indisponible (UNAVAILABLE).
 * - Single Source of Truth locale (Room) préservée et fonctionnelle.
 * - Nombre d'actions d'écriture en attente de synchronisation idempotente via WorkManager.
 */
@Composable
fun QuotaExceededBanner(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val isQuotaExceeded by Graph.adminOrderRepository.isQuotaExceeded.collectAsStateWithLifecycle()
    val pendingCount by Graph.adminOrderRepository.pendingActionsCount.collectAsStateWithLifecycle(initialValue = 0)

    var isRetrying by remember { mutableStateOf(false) }

    val shouldShow = isQuotaExceeded || pendingCount > 0

    AnimatedVisibility(
        visible = shouldShow,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isQuotaExceeded) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.tertiaryContainer
                }
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = if (isQuotaExceeded) Icons.Default.CloudOff else Icons.Default.Warning,
                        contentDescription = "Statut Quota / Réseau",
                        tint = if (isQuotaExceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(26.dp)
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isQuotaExceeded) "Mode Dégradé Résilient (Quota / Hors-Ligne)" else "Synchronisation en attente",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isQuotaExceeded) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = if (isQuotaExceeded) {
                                "Les quotas Firestore sont saturés ou le réseau est coupé. Les flux Room restent actifs et vos modifications sont conservées localement."
                            } else {
                                "Des modifications locales sont en attente de synchronisation avec les serveurs."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isQuotaExceeded) MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.9f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Badge compteur d'actions
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$pendingCount action(s) en file WorkManager",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Bouton Réessayer la synchronisation
                    Button(
                        onClick = {
                            scope.launch {
                                isRetrying = true
                                Graph.adminOrderRepository.retryConnection(context)
                                Graph.firestoreSyncManager.retrySync()
                                delay(1500)
                                isRetrying = false
                            }
                        },
                        enabled = !isRetrying,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isQuotaExceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        ),
                        contentPadding = ButtonDefaults.ContentPadding
                    ) {
                        if (isRetrying) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Synchronisation...", style = MaterialTheme.typography.labelSmall)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Réessayer",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Synchroniser", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}
