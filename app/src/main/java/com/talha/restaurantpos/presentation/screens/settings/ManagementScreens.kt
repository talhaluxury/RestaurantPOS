package com.talha.restaurantpos.presentation.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talha.restaurantpos.domain.model.*
import com.talha.restaurantpos.domain.repository.InventoryRepository
import com.talha.restaurantpos.domain.repository.StaffRepository
import com.talha.restaurantpos.domain.repository.TableRepository
import com.talha.restaurantpos.presentation.components.*
import com.talha.restaurantpos.presentation.theme.GlassTheme
import com.talha.restaurantpos.util.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ---------------- Staff ----------------

@HiltViewModel
class StaffManagementViewModel @Inject constructor(
    private val staffRepository: StaffRepository,
    private val sessionManager: SessionManager
) : ViewModel() {
    private val _staff = MutableStateFlow<List<StaffMember>>(emptyList())
    val staff: StateFlow<List<StaffMember>> = _staff.asStateFlow()
    private var restaurantId = ""

    init {
        viewModelScope.launch {
            restaurantId = sessionManager.currentRestaurantId() ?: return@launch
            staffRepository.observeStaff(restaurantId).collect { _staff.value = it }
        }
    }

    fun save(member: StaffMember) { viewModelScope.launch { staffRepository.upsertStaff(restaurantId, member) } }
    fun delete(id: String) { viewModelScope.launch { staffRepository.deleteStaff(restaurantId, id) } }
}

@Composable
fun StaffScreen(onBack: () -> Unit, viewModel: StaffManagementViewModel = hiltViewModel()) {
    val colors = GlassTheme.colors
    val staff by viewModel.staff.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(StaffRole.CASHIER) }
    var pin by remember { mutableStateOf("") }

    GlassBackground {
        Column(Modifier.fillMaxSize()) {
            GlassTopBar(title = "Staff", subtitle = "${staff.size} members", onBack = onBack, actions = {
                GlassIconButton(Icons.Default.Add, "Add Staff", { showAdd = true }, accented = true)
            })
            if (staff.isEmpty()) {
                GlassEmptyState(Icons.Default.People, "No Staff Yet", "Add your team so they can log in and take orders.", "ADD STAFF") { showAdd = true }
            } else {
                LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(staff, key = { it.id }) { member ->
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(member.name, color = colors.textPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    Text(member.role.name, color = colors.accent, style = MaterialTheme.typography.labelMedium)
                                }
                                GlassIconButton(Icons.Default.Delete, "Delete", { viewModel.delete(member.id) }, size = 36.dp)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showAdd = false }) {
            GlassCard(elevated = true, accentBorder = true) {
                Column(Modifier.padding(24.dp).widthIn(min = 280.dp)) {
                    Text("Add Staff Member", color = colors.textPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(14.dp))
                    GlassTextField(name, { name = it }, label = "Name")
                    Spacer(Modifier.height(10.dp))
                    GlassTextField(phone, { phone = it }, label = "Phone", keyboardType = KeyboardType.Phone)
                    Spacer(Modifier.height(10.dp))
                    GlassTextField(pin, { pin = it.filter(Char::isDigit).take(4) }, label = "4-digit PIN", keyboardType = KeyboardType.NumberPassword)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StaffRole.entries.filter { it != StaffRole.OWNER }.forEach { r ->
                            GlassChip(r.name, role == r, { role = r })
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GlassButton("Cancel", { showAdd = false }, style = GlassButtonStyle.SECONDARY, modifier = Modifier.weight(1f))
                        GlassButton("Add", {
                            viewModel.save(StaffMember(name = name, phone = phone, role = role, pin = pin))
                            name = ""; phone = ""; pin = ""; showAdd = false
                        }, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

// ---------------- Inventory ----------------

@HiltViewModel
class InventoryManagementViewModel @Inject constructor(
    private val inventoryRepository: InventoryRepository,
    private val sessionManager: SessionManager
) : ViewModel() {
    private val _items = MutableStateFlow<List<InventoryItem>>(emptyList())
    val items: StateFlow<List<InventoryItem>> = _items.asStateFlow()
    private var restaurantId = ""

    init {
        viewModelScope.launch {
            restaurantId = sessionManager.currentRestaurantId() ?: return@launch
            inventoryRepository.observeInventory(restaurantId).collect { _items.value = it }
        }
    }

    fun save(item: InventoryItem) { viewModelScope.launch { inventoryRepository.upsertItem(restaurantId, item) } }
}

@Composable
fun InventoryScreen(onBack: () -> Unit, viewModel: InventoryManagementViewModel = hiltViewModel()) {
    val colors = GlassTheme.colors
    val items by viewModel.items.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("") }
    var threshold by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("kg") }

    GlassBackground {
        Column(Modifier.fillMaxSize()) {
            GlassTopBar(title = "Inventory", subtitle = "${items.count { it.isLowStock }} low stock", onBack = onBack, actions = {
                GlassIconButton(Icons.Default.Add, "Add Item", { showAdd = true }, accented = true)
            })
            if (items.isEmpty()) {
                GlassEmptyState(Icons.Default.Inventory, "No Inventory Items", "Track ingredient stock to get low-stock alerts.", "ADD ITEM") { showAdd = true }
            } else {
                LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(items, key = { it.id }) { item ->
                        GlassCard(modifier = Modifier.fillMaxWidth(), accentBorder = item.isLowStock) {
                            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(item.name, color = colors.textPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    Text("${item.currentStock} ${item.unit}", color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
                                }
                                if (item.isLowStock) {
                                    Text("LOW STOCK", color = colors.warning, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showAdd = false }) {
            GlassCard(elevated = true, accentBorder = true) {
                Column(Modifier.padding(24.dp).widthIn(min = 280.dp)) {
                    Text("Add Inventory Item", color = colors.textPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(14.dp))
                    GlassTextField(name, { name = it }, label = "Item Name", placeholder = "Chicken")
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GlassTextField(stock, { stock = it }, label = "Current Stock", keyboardType = KeyboardType.Decimal, modifier = Modifier.weight(1f))
                        GlassTextField(unit, { unit = it }, label = "Unit", placeholder = "kg", modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                    GlassTextField(threshold, { threshold = it }, label = "Low Stock Threshold", keyboardType = KeyboardType.Decimal)
                    Spacer(Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GlassButton("Cancel", { showAdd = false }, style = GlassButtonStyle.SECONDARY, modifier = Modifier.weight(1f))
                        GlassButton("Add", {
                            viewModel.save(InventoryItem(name = name, currentStock = stock.toDoubleOrNull() ?: 0.0, lowStockThreshold = threshold.toDoubleOrNull() ?: 0.0, unit = unit))
                            name = ""; stock = ""; threshold = ""; showAdd = false
                        }, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

// ---------------- Tables ----------------

@HiltViewModel
class TablesManagementViewModel @Inject constructor(
    private val tableRepository: TableRepository,
    private val sessionManager: SessionManager
) : ViewModel() {
    private val _tables = MutableStateFlow<List<RestaurantTable>>(emptyList())
    val tables: StateFlow<List<RestaurantTable>> = _tables.asStateFlow()
    private var restaurantId = ""

    init {
        viewModelScope.launch {
            restaurantId = sessionManager.currentRestaurantId() ?: return@launch
            tableRepository.observeTables(restaurantId).collect { _tables.value = it }
        }
    }

    fun addTable(label: String) { viewModelScope.launch { tableRepository.upsertTable(restaurantId, RestaurantTable(label = label)) } }
    fun setStatus(id: String, status: TableStatus) { viewModelScope.launch { tableRepository.setTableStatus(restaurantId, id, status) } }
}

@Composable
fun TablesScreen(onBack: () -> Unit, viewModel: TablesManagementViewModel = hiltViewModel()) {
    val colors = GlassTheme.colors
    val tables by viewModel.tables.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var label by remember { mutableStateOf("") }

    GlassBackground {
        Column(Modifier.fillMaxSize()) {
            GlassTopBar(title = "Tables", subtitle = "${tables.size} tables", onBack = onBack, actions = {
                GlassIconButton(Icons.Default.Add, "Add Table", { showAdd = true }, accented = true)
            })
            if (tables.isEmpty()) {
                GlassEmptyState(Icons.Default.TableRestaurant, "No Tables Yet", "Add tables to enable dine-in billing.", "ADD TABLE") { showAdd = true }
            } else {
                LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(tables, key = { it.id }) { table ->
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(table.label, color = colors.textPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                TableStatus.entries.forEach { s ->
                                    Spacer(Modifier.width(6.dp))
                                    GlassChip(s.name.take(3), table.status == s, { viewModel.setStatus(table.id, s) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showAdd = false }) {
            GlassCard(elevated = true, accentBorder = true) {
                Column(Modifier.padding(24.dp).widthIn(min = 260.dp)) {
                    Text("Add Table", color = colors.textPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(14.dp))
                    GlassTextField(label, { label = it }, placeholder = "Table 05")
                    Spacer(Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GlassButton("Cancel", { showAdd = false }, style = GlassButtonStyle.SECONDARY, modifier = Modifier.weight(1f))
                        GlassButton("Add", { viewModel.addTable(label); label = ""; showAdd = false }, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
