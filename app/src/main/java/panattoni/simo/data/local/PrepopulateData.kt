package panattoni.simo.data.local

import panattoni.simo.data.local.entity.ItemEntity
import panattoni.simo.data.local.entity.RecipeComponentEntity
import panattoni.simo.data.local.entity.RecipeEntity
import panattoni.simo.domain.Allergen

/**
 * Fornisce il dataset iniziale predefinito (materie prime, ricette dimostrative ed archi del DAG)
 * inserito al primo avvio dell'applicazione.
 */
object PrepopulateData{

    val ID_RECIPE_CREMA = "recipe_crema_pasticcera"
    val ID_RECIPE_CORNETTO = "recipe_cornetto_crema"
    val ID_RECIPE_PIZZA = "recipe_pizza_margherita"

    val DEFAULT_ITEMS: List<ItemEntity> = listOf(
        // Materie prime per Crema Pasticcera con 6 valori nutrizionali per 100g
        ItemEntity(
            id = "ing_latte", name = "Latte intero", type = "RAW_INGREDIENT", defaultUnit = "ml",
            pricePerUnit = 1.40, priceUnit = "l", allergens = Allergen.LATTE.id,
            caloriesKcal = 64.0, carbohydratesG = 4.8, fatsG = 3.6, proteinsG = 3.3, waterG = 87.5, sodiumMg = 44.0
        ),
        ItemEntity(
            id = "ing_tuorli", name = "Tuorli d'uovo", type = "RAW_INGREDIENT", defaultUnit = "unità",
            gramsPerUnit = 20.0, pricePerUnit = 0.35, priceUnit = "unità", allergens = Allergen.UOVA.id,
            caloriesKcal = 322.0, carbohydratesG = 3.6, fatsG = 26.5, proteinsG = 15.9, waterG = 51.0, sodiumMg = 48.0
        ),
        ItemEntity(
            id = "ing_zucchero", name = "Zucchero semolato", type = "RAW_INGREDIENT", defaultUnit = "g",
            pricePerUnit = 1.20, priceUnit = "kg",
            caloriesKcal = 387.0, carbohydratesG = 99.9, fatsG = 0.0, proteinsG = 0.0, waterG = 0.1, sodiumMg = 1.0
        ),
        ItemEntity(
            id = "ing_amido_mais", name = "Amido di mais (maizena)", type = "RAW_INGREDIENT", defaultUnit = "g",
            pricePerUnit = 2.50, priceUnit = "kg",
            caloriesKcal = 381.0, carbohydratesG = 91.0, fatsG = 0.1, proteinsG = 0.3, waterG = 8.5, sodiumMg = 9.0
        ),
        ItemEntity(
            id = "ing_scorza_limone", name = "Scorza di limone", type = "RAW_INGREDIENT", defaultUnit = "unità",
            pricePerUnit = 0.50, priceUnit = "unità",
            caloriesKcal = 47.0, carbohydratesG = 16.0, fatsG = 0.3, proteinsG = 1.5, waterG = 81.6, sodiumMg = 6.0
        ),
        ItemEntity(
            id = "ing_vaniglia", name = "Baccello di vaniglia", type = "RAW_INGREDIENT", defaultUnit = "unità",
            pricePerUnit = 2.00, priceUnit = "unità",
            caloriesKcal = 288.0, carbohydratesG = 12.6, fatsG = 0.1, proteinsG = 0.1, waterG = 52.6, sodiumMg = 9.0
        ),

        // Materie prime aggiuntive per Cornetti
        ItemEntity(
            id = "ing_farina_manitoba", name = "Farina Manitoba", type = "RAW_INGREDIENT", defaultUnit = "g",
            pricePerUnit = 1.60, priceUnit = "kg", allergens = Allergen.GLUTINE.id,
            caloriesKcal = 364.0, carbohydratesG = 72.0, fatsG = 1.5, proteinsG = 14.0, waterG = 12.0, sodiumMg = 2.0
        ),
        ItemEntity(
            id = "ing_farina_00", name = "Farina 00", type = "RAW_INGREDIENT", defaultUnit = "g",
            pricePerUnit = 1.10, priceUnit = "kg", allergens = Allergen.GLUTINE.id,
            caloriesKcal = 364.0, carbohydratesG = 76.3, fatsG = 1.0, proteinsG = 10.3, waterG = 14.0, sodiumMg = 2.0
        ),
        ItemEntity(
            id = "ing_uova", name = "Uova intere", type = "RAW_INGREDIENT", defaultUnit = "unità",
            gramsPerUnit = 50.0, pricePerUnit = 0.30, priceUnit = "unità", allergens = Allergen.UOVA.id,
            caloriesKcal = 143.0, carbohydratesG = 0.7, fatsG = 9.5, proteinsG = 12.6, waterG = 76.0, sodiumMg = 142.0
        ),
        ItemEntity(
            id = "ing_burro", name = "Burro da laminazione", type = "RAW_INGREDIENT", defaultUnit = "g",
            pricePerUnit = 9.00, priceUnit = "kg", allergens = Allergen.LATTE.id,
            caloriesKcal = 717.0, carbohydratesG = 0.1, fatsG = 81.1, proteinsG = 0.9, waterG = 16.0, sodiumMg = 11.0
        ),
        ItemEntity(
            id = "ing_lievito_birra", name = "Lievito di birra fresco", type = "RAW_INGREDIENT", defaultUnit = "g",
            pricePerUnit = 0.40, priceUnit = "pz",
            caloriesKcal = 105.0, carbohydratesG = 18.0, fatsG = 1.9, proteinsG = 8.4, waterG = 69.0, sodiumMg = 30.0
        ),
        ItemEntity(
            id = "ing_sale", name = "Sale fino", type = "RAW_INGREDIENT", defaultUnit = "g",
            pricePerUnit = 0.50, priceUnit = "kg",
            caloriesKcal = 0.0, carbohydratesG = 0.0, fatsG = 0.0, proteinsG = 0.0, waterG = 0.2, sodiumMg = 38758.0
        ),

        // La Crema come RECIPE Item
        ItemEntity(id = ID_RECIPE_CREMA, name = "Crema Pasticcera Classica", type = "RECIPE", defaultUnit = "g", allergens = "${Allergen.UOVA.id},${Allergen.LATTE.id}"),

        // Materie prime per Pizza Margherita
        ItemEntity(
            id = "ing_farina_0", name = "Farina tipo 0 (media forza)", type = "RAW_INGREDIENT", defaultUnit = "g",
            pricePerUnit = 1.20, priceUnit = "kg", allergens = Allergen.GLUTINE.id,
            caloriesKcal = 364.0, carbohydratesG = 74.0, fatsG = 1.2, proteinsG = 11.5, waterG = 13.0, sodiumMg = 2.0
        ),
        ItemEntity(
            id = "ing_acqua", name = "Acqua naturale tiepida", type = "RAW_INGREDIENT", defaultUnit = "ml",
            pricePerUnit = 0.10, priceUnit = "l",
            caloriesKcal = 0.0, carbohydratesG = 0.0, fatsG = 0.0, proteinsG = 0.0, waterG = 100.0, sodiumMg = 5.0
        ),
        ItemEntity(
            id = "ing_olio_evo", name = "Olio extravergine d'oliva", type = "RAW_INGREDIENT", defaultUnit = "ml",
            pricePerUnit = 9.50, priceUnit = "l",
            caloriesKcal = 884.0, carbohydratesG = 0.0, fatsG = 100.0, proteinsG = 0.0, waterG = 0.0, sodiumMg = 2.0
        ),
        ItemEntity(
            id = "ing_pomodori_pelati", name = "Pomodori pelati San Marzano", type = "RAW_INGREDIENT", defaultUnit = "g",
            pricePerUnit = 2.00, priceUnit = "kg",
            caloriesKcal = 22.0, carbohydratesG = 3.5, fatsG = 0.2, proteinsG = 1.2, waterG = 93.5, sodiumMg = 12.0
        ),
        ItemEntity(
            id = "ing_mozzarella", name = "Mozzarella fiordilatte", type = "RAW_INGREDIENT", defaultUnit = "g",
            pricePerUnit = 8.50, priceUnit = "kg", allergens = Allergen.LATTE.id,
            caloriesKcal = 280.0, carbohydratesG = 2.2, fatsG = 22.0, proteinsG = 18.0, waterG = 55.0, sodiumMg = 400.0
        ),
        ItemEntity(
            id = "ing_basilico", name = "Foglie di basilico fresco", type = "RAW_INGREDIENT", defaultUnit = "unità",
            pricePerUnit = 0.05, priceUnit = "unità",
            caloriesKcal = 23.0, carbohydratesG = 2.7, fatsG = 0.6, proteinsG = 3.2, waterG = 92.0, sodiumMg = 4.0
        )
    )

    val DEFAULT_RECIPES: List<RecipeEntity> = listOf(
        RecipeEntity(
            id = ID_RECIPE_CREMA,
            name = "Crema Pasticcera",
            description = "Crema densa e vellutata a base di latte, tuorli e amido, aromatizzata con scorza di limone e bacca di vaniglia.",
            baseYieldQuantity = 500.0,
            yieldUnit = "g",
            instructions = "1. Scaldare il latte con la scorza di limone e i semi del baccello di vaniglia.\n2. In una ciotola, sbattere i tuorli con lo zucchero e incorporare l'amido di mais setacciato.\n3. Filtrare il latte caldo versandolo a filo sul composto di tuorli, mescolando continuamente.\n4. Cuocere a fuoco dolce fino al raggiungimento della corretta densità.\n5. Coprire con pellicola a contatto e lasciar raffreddare.",
            isDefault = true,
            isFavorite = false,
            category = "Dolci",
            createdAt = 1000000L,
            cookingWeightModifier = -8.0 // Calo dell'8% per evaporazione acqua a fuoco dolce
        ),
        RecipeEntity(
            id = ID_RECIPE_CORNETTO,
            name = "Cornetto alla Crema",
            description = "Preparazione lievitata e sfogliata a strati sottili con burro da laminazione, farcita con crema pasticcera.",
            baseYieldQuantity = 10.0,
            yieldUnit = "cornetti",
            instructions = "1. Preparare l'impasto base con le farine, lievito, latte, zucchero e uova fino a sviluppo del glutine.\n2. Far riposare l'impasto in frigorifero a +4°C.\n3. Procedere all'incasso del burro ed eseguire le tre pieghe canoniche intervallate da riposi.\n4. Stendere, sagomare a triangoli e arrotolare.\n5. Lievitare fino al raddoppio, cuocere a 180°C per 18 minuti e farcire con la crema pasticcera.",
            isDefault = true,
            isFavorite = false,
            category = "Dolci",
            createdAt = 2000000L,
            cookingWeightModifier = -12.0 // Calo del 12% in cottura in forno
        ),
        RecipeEntity(
            id = ID_RECIPE_PIZZA,
            name = "Pizza Margherita",
            description = "Classica preparazione tradizionale a lenta lievitazione naturale, con farcitura di pomodoro pelato, mozzarella fiordilatte e basilico fresco.",
            baseYieldQuantity = 1.0,
            yieldUnit = "pizza",
            instructions = "1. Sciogliere il lievito nell'acqua, aggiungere gradualmente la farina setacciata.\n2. Incorporare il sale e l'olio, lavorando fino ad ottenere una massa liscia ed elastica.\n3. Lasciar maturare e lievitare l'impasto per almeno 8 ore a temperatura controllata.\n4. Stendere delicatamente a disco dal centro verso il cornicione.\n5. Guarnire con pomodoro pelato schiacciato, mozzarella ben sgocciolata e infornare alla massima temperatura consentita.\n6. Rifinire con foglie di basilico fresco a crudo.",
            isDefault = true,
            isFavorite = false,
            category = "Pizze",
            createdAt = 3000000L,
            cookingWeightModifier = -10.0 // Calo del 10% in cottura forno pizza ad alta temperatura
        )
    )

    val DEFAULT_COMPONENTS: List<RecipeComponentEntity> = listOf(
        // Componenti Crema Pasticcera (Totale resa 500g)
        RecipeComponentEntity(parentRecipeId = ID_RECIPE_CREMA, childItemId = "ing_latte", baseQuantity = 415.0, unit = "ml", sortOrder = 1),
        RecipeComponentEntity(parentRecipeId = ID_RECIPE_CREMA, childItemId = "ing_tuorli", baseQuantity = 3.0, unit = "unità", sortOrder = 2),
        RecipeComponentEntity(parentRecipeId = ID_RECIPE_CREMA, childItemId = "ing_zucchero", baseQuantity = 100.0, unit = "g", sortOrder = 3),
        RecipeComponentEntity(parentRecipeId = ID_RECIPE_CREMA, childItemId = "ing_amido_mais", baseQuantity = 35.0, unit = "g", sortOrder = 4),
        RecipeComponentEntity(parentRecipeId = ID_RECIPE_CREMA, childItemId = "ing_scorza_limone", baseQuantity = 1.0, unit = "unità", sortOrder = 5),
        RecipeComponentEntity(parentRecipeId = ID_RECIPE_CREMA, childItemId = "ing_vaniglia", baseQuantity = 1.0, unit = "unità", sortOrder = 6),

        // Componenti Cornetto alla Crema (Dosi base per 10 cornetti)
        RecipeComponentEntity(parentRecipeId = ID_RECIPE_CORNETTO, childItemId = "ing_farina_manitoba", baseQuantity = 210.0, unit = "g", sortOrder = 1),
        RecipeComponentEntity(parentRecipeId = ID_RECIPE_CORNETTO, childItemId = "ing_farina_00", baseQuantity = 210.0, unit = "g", sortOrder = 2),
        RecipeComponentEntity(parentRecipeId = ID_RECIPE_CORNETTO, childItemId = "ing_latte", baseQuantity = 150.0, unit = "ml", sortOrder = 3),
        RecipeComponentEntity(parentRecipeId = ID_RECIPE_CORNETTO, childItemId = "ing_uova", baseQuantity = 2.0, unit = "unità", sortOrder = 4),
        RecipeComponentEntity(parentRecipeId = ID_RECIPE_CORNETTO, childItemId = "ing_zucchero", baseQuantity = 75.0, unit = "g", sortOrder = 5),
        RecipeComponentEntity(parentRecipeId = ID_RECIPE_CORNETTO, childItemId = "ing_burro", baseQuantity = 175.0, unit = "g", sortOrder = 6),
        RecipeComponentEntity(parentRecipeId = ID_RECIPE_CORNETTO, childItemId = "ing_lievito_birra", baseQuantity = 15.0, unit = "g", sortOrder = 7),
        RecipeComponentEntity(parentRecipeId = ID_RECIPE_CORNETTO, childItemId = "ing_sale", baseQuantity = 5.0, unit = "g", sortOrder = 8),
        // SOTTO-RICETTA Crema Pasticcera (330g per 10 cornetti)
        RecipeComponentEntity(parentRecipeId = ID_RECIPE_CORNETTO, childItemId = ID_RECIPE_CREMA, baseQuantity = 330.0, unit = "g", sortOrder = 9),

        // Componenti Pizza Margherita (Dosi base per 1 singola pizza)
        RecipeComponentEntity(parentRecipeId = ID_RECIPE_PIZZA, childItemId = "ing_farina_0", baseQuantity = 250.0, unit = "g", sortOrder = 1),
        RecipeComponentEntity(parentRecipeId = ID_RECIPE_PIZZA, childItemId = "ing_acqua", baseQuantity = 175.0, unit = "ml", sortOrder = 2),
        RecipeComponentEntity(parentRecipeId = ID_RECIPE_PIZZA, childItemId = "ing_lievito_birra", baseQuantity = 5.0, unit = "g", sortOrder = 3),
        RecipeComponentEntity(parentRecipeId = ID_RECIPE_PIZZA, childItemId = "ing_olio_evo", baseQuantity = 10.0, unit = "ml", sortOrder = 4),
        RecipeComponentEntity(parentRecipeId = ID_RECIPE_PIZZA, childItemId = "ing_sale", baseQuantity = 5.0, unit = "g", sortOrder = 5),
        RecipeComponentEntity(parentRecipeId = ID_RECIPE_PIZZA, childItemId = "ing_pomodori_pelati", baseQuantity = 200.0, unit = "g", sortOrder = 6),
        RecipeComponentEntity(parentRecipeId = ID_RECIPE_PIZZA, childItemId = "ing_mozzarella", baseQuantity = 150.0, unit = "g", sortOrder = 7),
        RecipeComponentEntity(parentRecipeId = ID_RECIPE_PIZZA, childItemId = "ing_basilico", baseQuantity = 3.0, unit = "unità", sortOrder = 8)
    )
}
