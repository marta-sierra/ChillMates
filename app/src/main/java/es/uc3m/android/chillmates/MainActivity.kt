package es.uc3m.android.chillmates

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
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
import es.uc3m.android.chillmates.viewmodel.AuthState
import es.uc3m.android.chillmates.viewmodel.AuthViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val data: Uri? = intent?.data
        if (data != null && data.pathSegments.contains("join")) {
            val flatId = data.lastPathSegment
        }

        enableEdgeToEdge()
        setContent {
            ChillMatesTheme {
                ChillMatesApp()
            }
        }
    }
}

@Composable
fun WelcomeBackScreen(
    userName: String,
    flatName: String?,
    onContinue: () -> Unit,
    onChangeAccount: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.secondaryContainer
                            )
                        ),
                        shape = RoundedCornerShape(50)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = null,
                    modifier = Modifier.size(50.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Welcome back,",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = userName.ifEmpty { "mate" },
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "to",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = flatName ?: "your flat",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.welcome_back_continue),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = onChangeAccount,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.welcome_back_change_account),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChillMatesApp(authViewModel: AuthViewModel = viewModel()) {
    val navController = rememberNavController()
    val authState by authViewModel.uiState.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val loginRoute = stringResource(R.string.login_route)
    val welcomeBackRoute = "welcome-back"

    if (authState.authState is AuthState.Loading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    if (authState.authState is AuthState.NeedsFlatSetup) {
        FlatSetupPage(
            authViewModel = authViewModel,
            onComplete = {
                navController.navigate(welcomeBackRoute) {
                    popUpTo(loginRoute) { inclusive = true }
                }
            }
        )
        return
    }

    val isAuthenticated = authState.authState is AuthState.Authenticated
    val isUnauthenticated = authState.authState is AuthState.Unauthenticated

    LaunchedEffect(isUnauthenticated) {
        if (isUnauthenticated) {
            navController.navigate(loginRoute) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    val startDestination = if (isAuthenticated) welcomeBackRoute else loginRoute

    val showBottomBar = currentRoute != null && currentRoute != loginRoute && currentRoute != welcomeBackRoute

    val bottomNavItems = NavGraph.bottomBarItems

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            if (showBottomBar) {
                bottomNavItems.forEach { screen ->
                    item(
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = currentRoute == screen.route,
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
                if (showBottomBar) {
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
                    startDestination = startDestination
                ) {
                    composable(loginRoute) { LoginPage(navController, authViewModel) }
                    composable(welcomeBackRoute) {
                        WelcomeBackScreen(
                            userName = currentUser?.displayName ?: "",
                            flatName = currentUser?.flatName,
                            onContinue = {
                                navController.navigate(NavGraph.Home.route) {
                                    popUpTo(welcomeBackRoute) { inclusive = true }
                                }
                            },
                            onChangeAccount = {
                                authViewModel.logout()
                            }
                        )
                    }
                    composable(NavGraph.Home.route) { HomePage(navController) }
                    composable(NavGraph.Chores.route) { ChoresPage(navController) }
                    composable(NavGraph.Expenses.route) { ExpensesTrackerPage(navController) }
                    composable(NavGraph.Repairs.route) { RepairPage(navController) }
                    composable(NavGraph.Shopping.route) { ShoppingListPage(navController) }
                    composable(NavGraph.User.route) { UserPage(navController) }
                    composable(NavGraph.Settings.route) { SettingsPage(navController, authViewModel) }
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