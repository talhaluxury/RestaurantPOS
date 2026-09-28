package com.talha.restaurantpos.presentation.navigation

import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.talha.restaurantpos.domain.model.Order
import com.talha.restaurantpos.presentation.screens.auth.*
import com.talha.restaurantpos.presentation.screens.billing.BillingViewModel
import com.talha.restaurantpos.presentation.screens.billing.NewBillScreen
import com.talha.restaurantpos.presentation.screens.dashboard.DashboardScreen
import com.talha.restaurantpos.presentation.screens.kitchen.KitchenScreen
import com.talha.restaurantpos.presentation.screens.menu.AddEditProductScreen
import com.talha.restaurantpos.presentation.screens.menu.MenuManagementScreen
import com.talha.restaurantpos.presentation.screens.orders.OrderDetailScreen
import com.talha.restaurantpos.presentation.screens.orders.OrdersScreen
import com.talha.restaurantpos.presentation.screens.payment.PaymentScreen
import com.talha.restaurantpos.presentation.screens.payment.PaymentSuccessScreen
import com.talha.restaurantpos.presentation.screens.reports.ReportsScreen
import com.talha.restaurantpos.presentation.screens.settings.*
import com.talha.restaurantpos.presentation.screens.setup.RestaurantSetupScreen

@Composable
fun PosNavGraph(navController: NavHostController = rememberNavController()) {
    val sessionViewModel: AppSessionViewModel = hiltViewModel()
    val restaurant by sessionViewModel.restaurant.collectAsState()

    // Held across the billing -> payment -> receipt flow; simpler and more robust than
    // serializing a full Order object through string nav routes.
    var completedOrder by remember { mutableStateOf<Order?>(null) }

    NavHost(navController = navController, startDestination = Routes.SPLASH) {

        composable(Routes.SPLASH) {
            val authViewModel: AuthViewModel = hiltViewModel()
            val destination by authViewModel.destination.collectAsState()
            LaunchedEffect(Unit) { authViewModel.checkSession() }
            LaunchedEffect(destination) {
                when (destination) {
                    is SessionDestination.Welcome -> navController.navigateSingleTop(Routes.WELCOME)
                    is SessionDestination.NeedsRestaurantSetup -> navController.navigateSingleTop(Routes.RESTAURANT_SETUP)
                    is SessionDestination.Dashboard -> navController.navigateSingleTop(Routes.DASHBOARD)
                    is SessionDestination.Splash -> Unit
                }
            }
            SplashScreen(onReady = {})
        }

        composable(Routes.WELCOME) {
            WelcomeScreen(
                onLogin = { navController.navigate(Routes.LOGIN) },
                onRegister = { navController.navigate(Routes.REGISTER) }
            )
        }

        composable(Routes.LOGIN) {
            LoginScreen(
                onLoggedIn = { dest ->
                    val target = if (dest is SessionDestination.NeedsRestaurantSetup) Routes.RESTAURANT_SETUP else Routes.DASHBOARD
                    navController.navigateSingleTop(target)
                },
                onGoToRegister = { navController.navigate(Routes.REGISTER) },
                onForgotPassword = { navController.navigate(Routes.FORGOT_PASSWORD) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegistered = { navController.navigateSingleTop(Routes.RESTAURANT_SETUP) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.FORGOT_PASSWORD) {
            ForgotPasswordScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.RESTAURANT_SETUP) {
            RestaurantSetupScreen(onDone = { navController.navigateSingleTop(Routes.DASHBOARD) })
        }

        composable(Routes.DASHBOARD) {
            DashboardScreen(
                onNewBill = { navController.navigate(Routes.NEW_BILL) },
                onOrders = { navController.navigate(Routes.ORDERS) },
                onMenu = { navController.navigate(Routes.MENU) },
                onReports = { navController.navigate(Routes.REPORTS) },
                onSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(Routes.NEW_BILL) {
            NewBillScreen(
                onBack = { navController.popBackStack() },
                onProceedToPayment = { navController.navigate(Routes.PAYMENT) }
            )
        }

        composable(Routes.PAYMENT) {
            val billingEntry = remember(it) { navController.getBackStackEntry(Routes.NEW_BILL) }
            val billingViewModel: BillingViewModel = hiltViewModel(billingEntry)
            val billingState by billingViewModel.uiState.collectAsState()
            PaymentScreen(
                pendingOrder = billingViewModel.buildPendingOrder(),
                currency = billingState.currency,
                onBack = { navController.popBackStack() },
                onPaymentComplete = { placed ->
                    completedOrder = placed
                    billingViewModel.clearCart()
                    navController.navigate(Routes.PAYMENT_SUCCESS) {
                        popUpTo(Routes.NEW_BILL) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.PAYMENT_SUCCESS) {
            val order = completedOrder
            if (order != null) {
                PaymentSuccessScreen(
                    order = order,
                    restaurant = restaurant,
                    onDone = { navController.navigateSingleTop(Routes.DASHBOARD) },
                    onNewBill = {
                        navController.navigate(Routes.NEW_BILL) { popUpTo(Routes.DASHBOARD) }
                    }
                )
            }
        }

        composable(Routes.ORDERS) {
            OrdersScreen(
                onBack = { navController.popBackStack() },
                onOpenOrder = { orderId -> navController.navigate(Routes.orderDetail(orderId)) }
            )
        }

        composable(
            route = Routes.ORDER_DETAIL,
            arguments = listOf(navArgument("orderId") { type = NavType.StringType })
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId").orEmpty()
            OrderDetailScreen(
                orderId = orderId,
                restaurant = restaurant,
                onBack = { navController.popBackStack() },
                onDuplicateToNewBill = { _ -> navController.navigate(Routes.NEW_BILL) { popUpTo(Routes.DASHBOARD) } }
            )
        }

        composable(Routes.MENU) {
            MenuManagementScreen(
                onBack = { navController.popBackStack() },
                onAddProduct = { navController.navigate(Routes.addEditProduct()) },
                onEditProduct = { productId -> navController.navigate(Routes.addEditProduct(productId)) }
            )
        }

        composable(
            route = Routes.ADD_EDIT_PRODUCT,
            arguments = listOf(navArgument("productId") { type = NavType.StringType; defaultValue = "" })
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId").takeUnless { it.isNullOrBlank() }
            AddEditProductScreen(productId = productId, onBack = { navController.popBackStack() })
        }

        composable(Routes.REPORTS) {
            ReportsScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onStaff = { navController.navigate(Routes.STAFF) },
                onInventory = { navController.navigate(Routes.INVENTORY) },
                onTables = { navController.navigate(Routes.TABLES) },
                onLoggedOut = { navController.navigate(Routes.WELCOME) { popUpTo(0) } }
            )
        }

        composable(Routes.STAFF) { StaffScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.INVENTORY) { InventoryScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.TABLES) { TablesScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.KITCHEN) { KitchenScreen(onBack = { navController.popBackStack() }) }
    }
}

private fun androidx.navigation.NavController.navigateSingleTop(route: String) {
    navigate(route) {
        launchSingleTop = true
        popUpTo(route) { inclusive = false }
    }
}
