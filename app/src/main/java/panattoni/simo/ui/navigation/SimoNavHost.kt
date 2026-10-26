package panattoni.simo.ui.navigation

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import panattoni.simo.service.CookingSessionManager
import panattoni.simo.ui.screens.RecipeDetailScreen
import panattoni.simo.ui.screens.RecipeEditScreen
import panattoni.simo.ui.screens.RecipeListScreen
import panattoni.simo.ui.screens.SplashScreen
import panattoni.simo.ui.viewmodel.RecipeDetailViewModel
import panattoni.simo.ui.viewmodel.RecipeEditViewModel
import panattoni.simo.ui.viewmodel.RecipeListViewModel

@Composable
fun SimoNavHost(
    navController: NavHostController = rememberNavController()
){
    val context = LocalContext.current
    val application = context.applicationContext as Application

    // Pre-carichiamo il ViewModel delle ricette durante la splash screen:
    // in questo modo il database termina query e ordinamento PRIMA dell'arrivo al ricettario!
    val listViewModel: RecipeListViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = "splash"
    ){
        // Schermata di caricamento animata con fragola e Bubble Font
        composable("splash"){
            SplashScreen(
                isReady = { !listViewModel.uiState.value.isLoading },
                onSplashFinished ={
                    val activeSession = CookingSessionManager.sessionState.value
                    if (activeSession != null){
                        // Naviga prima alla lista ricette e poi al dettaglio:
                        // in questo modo il back stack contiene sempre la schermata principale
                        // ed evita che l'utente rimanga bloccato al ritorno dall'app in background!
                        navController.navigate("recipe_list"){
                            popUpTo("splash"){ inclusive = true }
                        }
                        navController.navigate("recipe_detail/${activeSession.recipeId}")
                    } else{
                        navController.navigate("recipe_list"){
                            popUpTo("splash"){ inclusive = true }
                        }
                    }
                }
            )
        }

        // Schermata principale lista ricette
        composable("recipe_list"){
            RecipeListScreen(
                viewModel = listViewModel,
                onRecipeClick ={ recipeId ->
                    navController.navigate("recipe_detail/$recipeId")
                },
                onAddRecipeClick ={
                    navController.navigate("recipe_create")
                },
                onEditRecipeClick ={ recipeId ->
                    navController.navigate("recipe_edit/$recipeId")
                },
                onShoppingClick ={
                    navController.navigate("shopping_list")
                }
            )
        }

        // Schermata Lista della Spesa
        composable("shopping_list"){
            val shoppingViewModel: panattoni.simo.ui.viewmodel.ShoppingListViewModel = viewModel()
            val safeBack: () -> Unit = {
                if (!navController.popBackStack()){
                    navController.navigate("recipe_list"){
                        popUpTo(0){ inclusive = true }
                    }
                }
            }
            BackHandler{
                safeBack()
            }
            panattoni.simo.ui.screens.ShoppingListScreen(
                viewModel = shoppingViewModel,
                onBackClick = safeBack
            )
        }

        // Schermata dettaglio ricetta (Ricalcolo dinamico, Reverse scaling, Sotto-ricette)
        composable(
            route = "recipe_detail/{recipeId}",
            arguments = listOf(navArgument("recipeId"){ type = NavType.StringType })
        ){ backStackEntry ->
            val recipeId = backStackEntry.arguments?.getString("recipeId") ?: return@composable
            val detailViewModel: RecipeDetailViewModel = viewModel(
                key = "detail_$recipeId",
                factory = object : androidx.lifecycle.ViewModelProvider.Factory{
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T{
                        return RecipeDetailViewModel(application, recipeId) as T
                    }
                }
            )

            val safeBack: () -> Unit = {
                if (!navController.popBackStack()){
                    navController.navigate("recipe_list"){
                        popUpTo(0){ inclusive = true }
                    }
                }
            }
            BackHandler{
                safeBack()
            }

            RecipeDetailScreen(
                viewModel = detailViewModel,
                onBackClick = safeBack,
                onDrillDownClick ={ subRecipeId ->
                    navController.navigate("recipe_detail/$subRecipeId")
                }
            )
        }

        // Schermata creazione nuova ricetta
        composable("recipe_create"){
            val editViewModel: RecipeEditViewModel = viewModel(
                factory = object : androidx.lifecycle.ViewModelProvider.Factory{
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T{
                        return RecipeEditViewModel(application, null) as T
                    }
                }
            )
            val safeBack: () -> Unit = {
                if (!navController.popBackStack()){
                    navController.navigate("recipe_list"){
                        popUpTo(0){ inclusive = true }
                    }
                }
            }
            BackHandler{
                safeBack()
            }
            RecipeEditScreen(
                viewModel = editViewModel,
                onBackClick = safeBack,
                onSaveSuccess = safeBack
            )
        }

        // Schermata modifica ricetta esistente
        composable(
            route = "recipe_edit/{recipeId}",
            arguments = listOf(navArgument("recipeId"){ type = NavType.StringType })
        ){ backStackEntry ->
            val recipeId = backStackEntry.arguments?.getString("recipeId") ?: return@composable
            val editViewModel: RecipeEditViewModel = viewModel(
                key = "edit_$recipeId",
                factory = object : androidx.lifecycle.ViewModelProvider.Factory{
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T{
                        return RecipeEditViewModel(application, recipeId) as T
                    }
                }
            )
            val safeBack: () -> Unit = {
                if (!navController.popBackStack()){
                    navController.navigate("recipe_list"){
                        popUpTo(0){ inclusive = true }
                    }
                }
            }
            BackHandler{
                safeBack()
            }
            RecipeEditScreen(
                viewModel = editViewModel,
                onBackClick = safeBack,
                onSaveSuccess = safeBack
            )
        }
    }
}
