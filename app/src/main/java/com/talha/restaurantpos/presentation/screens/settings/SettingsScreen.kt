package com.talha.restaurantpos.presentation.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.talha.restaurantpos.printer.PaperWidth
import com.talha.restaurantpos.presentation.components.*
import com.talha.restaurantpos.presentation.theme.GlassTheme
import com.talha.restaurantpos.util.ThemePref

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onStaff: () -> Unit,
    onInventory: () -> Unit,
    onTables: () -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val colors = GlassTheme.colors
    val state by viewModel.uiState.collectAsState()
    var showLogoutConfirm by remember { mutableStateOf(false) }

    var name by remember(state.restaurant.id) { mutableStateOf(state.restaurant.name) }
    var phone by remember(state.restaurant.id) { mutableStateOf(state.restaurant.phone) }
    var address by remember(state.restaurant.id) { mutableStateOf(state.restaurant.address) }
    var footer by remember(state.restaurant.id) { mutableStateOf(state.restaurant.receiptFooter) }
    var taxPercent by remember(state.restaurant.id) { mutableStateOf(state.restaurant.taxPercent.toString()) }
    var servicePercent by remember(state.restaurant.id) { mutableStateOf(state.restaurant.serviceChargePercent.toString()) }

    LaunchedEffect(Unit) { viewModel.refreshPairedPrinters() }

    GlassBackground {
        Column(Modifier.fillMaxSize()) {
            GlassTopBar(title = "Settings", onBack = onBack)

            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {

                SectionTitle("Restaurant Profile")
                GlassCard(elevated = true, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp)) {
                        GlassTextField(name, { name = it }, label = "Restaurant Name")
                        Spacer(Modifier.height(12.dp))
                        GlassTextField(phone, { phone = it }, label = "Phone", keyboardType = KeyboardType.Phone)
                        Spacer(Modifier.height(12.dp))
                        GlassTextField(address, { address = it }, label = "Address")
                        Spacer(Modifier.height(12.dp))
                        GlassTextField(footer, { footer = it }, label = "Receipt Footer")
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            GlassTextField(taxPercent, { taxPercent = it }, label = "Tax %", keyboardType = KeyboardType.Decimal, modifier = Modifier.weight(1f))
                            GlassTextField(servicePercent, { servicePercent = it }, label = "Service Charge %", keyboardType = KeyboardType.Decimal, modifier = Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(16.dp))
                        GlassButton(
                            "SAVE CHANGES", loading = state.saving,
                            onClick = {
                                viewModel.saveRestaurant(
                                    state.restaurant.copy(
                                        name = name, phone = phone, address = address, receiptFooter = footer,
                                        taxPercent = taxPercent.toDoubleOrNull() ?: 0.0,
                                        serviceChargePercent = servicePercent.toDoubleOrNull() ?: 0.0
                                    )
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                SectionTitle("Management")
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        SettingsRow(Icons.Default.People, "Staff", onStaff)
                        SettingsRow(Icons.Default.Inventory, "Inventory", onInventory)
                        SettingsRow(Icons.Default.TableRestaurant, "Tables", onTables)
                    }
                }

                SectionTitle("Website")
                GlassCard(elevated = true, modifier = Modifier.fillMaxWidth()) {
                    val clipboard = LocalClipboardManager.current
                    var copied by remember { mutableStateOf(false) }
                    Column(Modifier.padding(18.dp)) {
                        Text("Restaurant ID", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
                        Text(
                            "Paste this into the ordering website's config so it connects to your menu and orders.",
                            color = colors.textTertiary, style = MaterialTheme.typography.labelSmall
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                state.restaurant.id, color = colors.textPrimary, style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f)
                            )
                            Icon(
                                if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                                contentDescription = "Copy",
                                tint = if (copied) colors.success else colors.accent,
                                modifier = Modifier.clickable {
                                    clipboard.setText(AnnotatedString(state.restaurant.id))
                                    copied = true
                                }
                            )
                        }
                    }
                }

                SectionTitle("Printer")
                GlassCard(elevated = true, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp)) {
                        Text("Paper Width", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(8.dp))
                        GlassSegmentedControl(
                            options = listOf("58mm", "80mm"),
                            selectedIndex = if (state.paperWidth == PaperWidth.MM_58) 0 else 1,
                            onSelect = { viewModel.setPaperWidth(if (it == 0) PaperWidth.MM_58 else PaperWidth.MM_80) }
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("Paired Bluetooth Printers", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(8.dp))
                        if (state.pairedPrinters.isEmpty()) {
                            Text("No paired printers found. Pair your thermal printer in Android Bluetooth settings first.", color = colors.textTertiary, style = MaterialTheme.typography.bodyMedium)
                        } else {
                            state.pairedPrinters.forEach { printer ->
                                Row(
                                    Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(printer.name, color = colors.textPrimary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                    GlassButton(
                                        if (state.connectedPrinterAddress == printer.address) "Connected" else "Connect",
                                        { viewModel.connectPrinter(printer.address) },
                                        style = if (state.connectedPrinterAddress == printer.address) GlassButtonStyle.SUCCESS else GlassButtonStyle.SECONDARY
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            GlassButton("Refresh", viewModel::refreshPairedPrinters, style = GlassButtonStyle.SECONDARY, modifier = Modifier.weight(1f))
                            GlassButton("Test Print", viewModel::testPrint, style = GlassButtonStyle.SECONDARY, modifier = Modifier.weight(1f), enabled = state.connectedPrinterAddress != null)
                        }
                        if (state.printerMessage != null) {
                            Spacer(Modifier.height(8.dp))
                            Text(state.printerMessage!!, color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                SectionTitle("Theme")
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlassChip("Light", state.theme == ThemePref.LIGHT, { viewModel.setTheme(ThemePref.LIGHT) })
                        GlassChip("Dark", state.theme == ThemePref.DARK, { viewModel.setTheme(ThemePref.DARK) })
                        GlassChip("System", state.theme == ThemePref.SYSTEM, { viewModel.setTheme(ThemePref.SYSTEM) })
                    }
                }

                Spacer(Modifier.height(20.dp))
                GlassButton("LOG OUT", { showLogoutConfirm = true }, style = GlassButtonStyle.DANGER, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showLogoutConfirm) {
        GlassDialog(
            title = "Log out?",
            message = "You can log back in anytime. Any unsynced bills will sync once you're back online.",
            onDismiss = { showLogoutConfirm = false },
            confirmText = "Log Out",
            dismissText = "Cancel",
            isError = true,
            onConfirm = { viewModel.logout(onLoggedOut) }
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    val colors = GlassTheme.colors
    Spacer(Modifier.height(20.dp))
    Text(text, color = colors.textPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun SettingsRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    val colors = GlassTheme.colors
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Text(label, color = colors.textPrimary, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = colors.textTertiary)
    }
}
