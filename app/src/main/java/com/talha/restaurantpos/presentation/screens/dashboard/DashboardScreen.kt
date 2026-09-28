package com.talha.restaurantpos.presentation.screens.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.talha.restaurantpos.presentation.components.*
import com.talha.restaurantpos.presentation.theme.GlassTheme
import com.talha.restaurantpos.util.CurrencyFormatter

data class QuickAction(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val onClick: () -> Unit)

@Composable
fun DashboardScreen(
    onNewBill: () -> Unit,
    onOrders: () -> Unit,
    onMenu: () -> Unit,
    onReports: () -> Unit,
    onSettings: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val colors = GlassTheme.colors
    val state by viewModel.uiState.collectAsState()
    val currency = state.restaurant.currency

    GlassBackground {
        Column(Modifier.fillMaxSize()) {
            GlassTopBar(
                title = state.restaurant.name.ifBlank { "Dashboard" },
                subtitle = "Welcome back, ${state.cashierName}",
                actions = {
                    OnlineStatusPill(state.isOnline)
                    Spacer(Modifier.width(10.dp))
                    GlassIconButton(Icons.Default.Settings, "Settings", onSettings)
                }
            )

            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)
            ) {
                val (delta, positive) = percentDelta(state.todaySales.totalRevenue, state.yesterdaySales.totalRevenue)
                GlassHeroStatCard(
                    title = "Today's Sales",
                    valueText = CurrencyFormatter.format(state.todaySales.totalRevenue, currency),
                    deltaText = "$delta vs yesterday",
                    positive = positive
                )

                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GlassStatCard("Orders", state.todaySales.orderCount.toString(), modifier = Modifier.weight(1f))
                    GlassStatCard("Avg Order", CurrencyFormatter.format(state.todaySales.averageOrderValue, currency), modifier = Modifier.weight(1f))
                    GlassStatCard("Items Sold", state.todaySales.itemsSold.toString(), modifier = Modifier.weight(1f))
                }

                Spacer(Modifier.height(24.dp))
                Text("Quick Actions", color = colors.textPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))

                val actions = listOf(
                    QuickAction("New Bill", Icons.Default.AddCircle, onNewBill),
                    QuickAction("Orders", Icons.Default.Receipt, onOrders),
                    QuickAction("Menu", Icons.Default.RestaurantMenu, onMenu),
                    QuickAction("Reports", Icons.Default.BarChart, onReports)
                )
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 150.dp),
                    modifier = Modifier.heightIn(max = 220.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(actions) { action ->
                        GlassCard(onClick = action.onClick, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(18.dp)) {
                                Box(
                                    Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                                ) {
                                    Icon(action.icon, contentDescription = action.label, tint = colors.accent, modifier = Modifier.size(28.dp))
                                }
                                Spacer(Modifier.height(10.dp))
                                Text(action.label.uppercase(), color = colors.textPrimary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            val navItems = listOf(
                NavItem("Home", Icons.Default.Home, "home"),
                NavItem("Bill", Icons.Default.PointOfSale, "bill"),
                NavItem("Orders", Icons.Default.Receipt, "orders"),
                NavItem("Menu", Icons.Default.RestaurantMenu, "menu"),
                NavItem("More", Icons.Default.MoreHoriz, "more")
            )
            GlassNavigationBar(
                items = navItems,
                selectedRoute = "home",
                onSelect = { route ->
                    when (route) {
                        "bill" -> onNewBill()
                        "orders" -> onOrders()
                        "menu" -> onMenu()
                        "more" -> onSettings()
                    }
                },
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
