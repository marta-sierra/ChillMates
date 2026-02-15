package es.uc3m.android.chillmates

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import es.uc3m.android.chillmates.ui.theme.ChillMatesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChillMatesTheme {
                ChillMatesApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class) // Necesario para TopAppBar
@Composable
fun ChillMatesApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationSuiteScaffold(
        navigationSuiteItems = {
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
    ) {
        // --- AQUÍ AÑADIMOS EL SCAFFOLD PARA LA BARRA SUPERIOR ---
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("ChillMates") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        ) { innerPadding ->
            // El NavHost ahora vive dentro del área de contenido del Scaffold
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding), // IMPORTANTE: Para que no se solape con la TopBar
                color = MaterialTheme.colorScheme.background
            ) {
                NavHost(
                    navController = navController,
                    startDestination = NavGraph.Home.route
                ) {
                    composable(NavGraph.Home.route) { HomePage(navController) }
                    composable(NavGraph.Chores.route) { ChoresPage(navController) }
                    composable(NavGraph.Expenses.route) { ExpensesTrackerPage(navController) }
                    composable(NavGraph.Repairs.route) { RepairPage(navController) }
                    composable(NavGraph.Shopping.route) { ShoppingListPage(navController) }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AppPreview() {
    ChillMatesTheme {
        ChillMatesApp()
    }
}