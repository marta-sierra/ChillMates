package es.uc3m.android.chillmates

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import es.uc3m.android.chillmates.chores.ChoresPage
import es.uc3m.android.chillmates.home.HomePage
import es.uc3m.android.chillmates.expensestracker.ExpensesTrackerPage
import es.uc3m.android.chillmates.repairs.RepairPage
import es.uc3m.android.chillmates.settings.SettingsPage
import es.uc3m.android.chillmates.shoppinglist.ShoppingListPage
import es.uc3m.android.chillmates.ui.theme.ChillMatesTheme
import es.uc3m.android.chillmates.user.UserPage

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Parse incoming URIs (e.g., chillmates.app/join/{flatId}) to handle invitations.
        val data: Uri? = intent?.data
        if (data != null && data.pathSegments.contains("join")) {
            val flatId = data.lastPathSegment
            // TODO: Pass this flatId
        }

        enableEdgeToEdge()
        setContent {
            ChillMatesTheme {
                ChillMatesApp()
            }
        }
    }
}

/**
 * Root Composable that manages the Navigation Suite (Bottom Bar / Rail)
 * and the main Navigation Graph.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChillMatesApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Check if the current route is the login screen to hide global navigation bars
    val loginRoute = stringResource(R.string.login_route)
    val isLoginScreen = currentDestination?.route == loginRoute

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            if (!isLoginScreen) {
                NavGraph.bottomBarItems.forEach { screen ->
                    item(
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                if (!isLoginScreen) {
                    TopAppBar(
                        title = { Text(stringResource(R.string.app_name)) },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        actions = {
                            SettingsTopBarButton(onClick = { navController.navigate(NavGraph.Settings.route) })
                            Spacer(modifier = Modifier.width(4.dp))
                            ProfileTopBarButton(onClick = { navController.navigate(NavGraph.User.route) })
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                    )
                }
            }
        ) { innerPadding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                color = MaterialTheme.colorScheme.background
            ) {
                NavHost(
                    navController = navController,
                    startDestination = loginRoute
                ) {
                    composable(loginRoute) { LoginPage(navController) }
                    composable(NavGraph.Home.route) { HomePage(navController) }
                    composable(NavGraph.Chores.route) { ChoresPage(navController) }
                    composable(NavGraph.Expenses.route) { ExpensesTrackerPage(navController) }
                    composable(NavGraph.Repairs.route) { RepairPage(navController) }
                    composable(NavGraph.Shopping.route) { ShoppingListPage(navController) }
                    composable(NavGraph.User.route) { UserPage(navController) }
                    composable(NavGraph.Settings.route) { SettingsPage(navController) }
                }
            }
        }
    }
}

@Composable
private fun ProfileTopBarButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Box(modifier = Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = stringResource(R.string.nav_cd_profile),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun SettingsTopBarButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Box(modifier = Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.nav_cd_settings),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}