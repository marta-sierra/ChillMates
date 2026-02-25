package es.uc3m.android.chillmates

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class NavGraph(val route: String, val label: String, val icon: ImageVector) {

    data object Home : NavGraph("home", "Home", Icons.Filled.Home)
    data object Chores : NavGraph("chores", "Chores", Icons.Filled.CheckCircle)
    data object Expenses : NavGraph("expenses", "Bills", Icons.Filled.DateRange)
    data object Repairs : NavGraph("repairs", "Repairs", Icons.Filled.Build)
    data object Shopping : NavGraph("shopping", "Shop", Icons.Filled.ShoppingCart)

    // User profile screen (opened from top-right avatar)
    data object User : NavGraph("user", "User", Icons.Filled.Settings)

    // Settings profile screen (opened from top-right avatar)
    data object Settings : NavGraph("settings", "Settings", Icons.Filled.Person)

    // Route with argument example
    data object RepairDetail : NavGraph("repair_detail/{id}", "Repair Detail", Icons.Filled.Build) {
        fun createRoute(id: String) = "repair_detail/$id"
    }

    // Helper list for the Bottom Bar
    companion object {
        val bottomBarItems = listOf(Chores, Shopping, Home, Expenses, Repairs)
    }
}