package app.johnhennis.obstweinrechner.ui.recipes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.johnhennis.obstweinrechner.data.FruitRecipe
import app.johnhennis.obstweinrechner.ui.AppViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeListScreen(
    factory: AppViewModelFactory,
    onBack: () -> Unit,
    onOpenTrash: () -> Unit
) {
    val viewModel: RecipeListViewModel = viewModel(factory = factory)
    val recipes by viewModel.recipes.collectAsState()

    var editingRecipe by remember { mutableStateOf<FruitRecipe?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Verhältnis-Editor") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenTrash) {
                        Icon(Icons.Filled.Delete, contentDescription = "Papierkorb öffnen")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editingRecipe = null
                showEditor = true
            }) {
                Icon(Icons.Filled.Add, contentDescription = "Neue Frucht hinzufügen")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(16.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(recipes, key = { it.id.ifEmpty { it.name } }) { recipe ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable {
                        editingRecipe = recipe
                        showEditor = true
                    }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(recipe.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "Frucht: ${recipe.fruchtKg} kg · Saft: ${recipe.saftLiter} L · Wasser: ${recipe.wasserLiter} L",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            "Zucker: ${recipe.zuckerKg} kg · Hefe: ${recipe.hefeSorte}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }

    if (showEditor) {
        RecipeEditorDialog(
            factory = factory,
            recipe = editingRecipe,
            onDismiss = { showEditor = false }
        )
    }
}
