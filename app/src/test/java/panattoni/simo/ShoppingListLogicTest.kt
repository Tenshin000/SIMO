package panattoni.simo

import panattoni.simo.data.model.Item
import panattoni.simo.data.model.ItemType
import panattoni.simo.data.model.ShoppingItem
import panattoni.simo.data.model.ShoppingSortOption
import panattoni.simo.domain.MathEngine
import panattoni.simo.ui.viewmodel.ShoppingItemUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ShoppingListLogicTest {

    @Test
    fun testShoppingCostCalculation(){
        // Farina a €1.20/kg
        val flourItem = Item(
            id = "flour",
            name = "Farina 00",
            type = ItemType.RAW_INGREDIENT,
            pricePerUnit = 1.20,
            priceUnit = "kg"
        )

        // 500g di Farina 00 -> 0.5kg * 1.20 = €0.60
        val costFlour = MathEngine.calculateComponentCost(
            quantity = 500.0,
            recipeUnit = "g",
            pricePerUnit = flourItem.pricePerUnit,
            priceUnit = flourItem.priceUnit,
            gramsPerUnit = flourItem.gramsPerUnit
        )
        assertNotNull(costFlour)
        assertEquals(0.60, costFlour!!, 0.001)

        // Burro a €8.00/kg, 250g -> €2.00
        val costButter = MathEngine.calculateComponentCost(
            quantity = 250.0,
            recipeUnit = "g",
            pricePerUnit = 8.00,
            priceUnit = "kg"
        )
        assertNotNull(costButter)
        assertEquals(2.00, costButter!!, 0.001)

        // Uova a €0.30 / pz, 4 pz -> €1.20
        val costEggs = MathEngine.calculateComponentCost(
            quantity = 4.0,
            recipeUnit = "pz",
            pricePerUnit = 0.30,
            priceUnit = "pz"
        )
        assertNotNull(costEggs)
        assertEquals(1.20, costEggs!!, 0.001)

        // Articolo senza prezzo
        val costNull = MathEngine.calculateComponentCost(
            quantity = 100.0,
            recipeUnit = "g",
            pricePerUnit = null,
            priceUnit = null
        )
        assertNull(costNull)
    }

    @Test
    fun testShoppingListSorting(){
        val itemA = ShoppingItemUiModel(
            item = ShoppingItem(id = 1, itemName = "Burro", quantity = 250.0, unit = "g", isChecked = false, createdAt = 1000L),
            estimatedCost = 2.0
        )
        val itemB = ShoppingItemUiModel(
            item = ShoppingItem(id = 2, itemName = "Zucchero", quantity = 200.0, unit = "g", isChecked = false, createdAt = 2000L),
            estimatedCost = 0.3
        )
        val itemC = ShoppingItemUiModel(
            item = ShoppingItem(id = 3, itemName = "Farina 00", quantity = 500.0, unit = "g", isChecked = false, createdAt = 3000L),
            estimatedCost = 0.6
        )
        val itemChecked = ShoppingItemUiModel(
            item = ShoppingItem(id = 4, itemName = "Acqua", quantity = 1000.0, unit = "ml", isChecked = true, createdAt = 4000L),
            estimatedCost = 0.0
        )

        val list = listOf(itemB, itemChecked, itemA, itemC)

        fun sort(sortOption: ShoppingSortOption): List<ShoppingItemUiModel> {
            return list.sortedWith { a, b ->
                val checkedCompare = a.item.isChecked.compareTo(b.item.isChecked)
                if (checkedCompare != 0){
                    checkedCompare
                } else {
                    when (sortOption){
                        ShoppingSortOption.NAME_ASC ->
                            a.item.itemName.compareTo(b.item.itemName, ignoreCase = true)
                        ShoppingSortOption.NAME_DESC ->
                            b.item.itemName.compareTo(a.item.itemName, ignoreCase = true)
                        ShoppingSortOption.DATE_DESC ->
                            b.item.createdAt.compareTo(a.item.createdAt).let {
                                if (it != 0) it else b.item.id.compareTo(a.item.id)
                            }
                        ShoppingSortOption.DATE_ASC ->
                            a.item.createdAt.compareTo(b.item.createdAt).let {
                                if (it != 0) it else a.item.id.compareTo(b.item.id)
                            }
                    }
                }
            }
        }

        // Test NAME_ASC (A-Z): Burro, Farina 00, Zucchero, then checked Acqua
        val sortedAsc = sort(ShoppingSortOption.NAME_ASC)
        assertEquals(listOf("Burro", "Farina 00", "Zucchero", "Acqua"), sortedAsc.map { it.item.itemName })

        // Test NAME_DESC (Z-A): Zucchero, Farina 00, Burro, then checked Acqua
        val sortedDesc = sort(ShoppingSortOption.NAME_DESC)
        assertEquals(listOf("Zucchero", "Farina 00", "Burro", "Acqua"), sortedDesc.map { it.item.itemName })

        // Test DATE_DESC (Più recente al meno recente): Farina 00 (3000), Zucchero (2000), Burro (1000), then checked Acqua
        val sortedDateDesc = sort(ShoppingSortOption.DATE_DESC)
        assertEquals(listOf("Farina 00", "Zucchero", "Burro", "Acqua"), sortedDateDesc.map { it.item.itemName })

        // Test DATE_ASC (Dal meno recente al più recente): Burro (1000), Zucchero (2000), Farina 00 (3000), then checked Acqua
        val sortedDateAsc = sort(ShoppingSortOption.DATE_ASC)
        assertEquals(listOf("Burro", "Zucchero", "Farina 00", "Acqua"), sortedDateAsc.map { it.item.itemName })
    }

    @Test
    fun testQbShoppingListResolution(){
        fun resolveQb(rawUnit: String, itemPriceUnit: String?, itemDefaultUnit: String): Pair<String, Double> {
            val isQb = panattoni.simo.domain.MeasurementUnit.isQb(rawUnit)
            val effectiveUnit = if (isQb){
                itemPriceUnit?.trim()?.ifEmpty { null }
                    ?: itemDefaultUnit.trim().ifEmpty { null }
                    ?: "pz"
            } else {
                rawUnit
            }

            val effectiveQty = if (isQb){
                when (effectiveUnit.lowercase()){
                    "g", "ml" -> 100.0
                    else -> 1.0
                }
            } else {
                1.0
            }
            return effectiveUnit to effectiveQty
        }

        // Sale: q.b. con priceUnit "g" -> unità "g", default dose 100.0
        val (saleUnit, saleQty) = resolveQb("q.b.", "g", "g")
        assertEquals("g", saleUnit)
        assertEquals(100.0, saleQty, 0.001)

        // Latte: q.b. con priceUnit "ml" -> unità "ml", default dose 100.0
        val (latteUnit, latteQty) = resolveQb("q.b.", "ml", "ml")
        assertEquals("ml", latteUnit)
        assertEquals(100.0, latteQty, 0.001)

        // Limone: q.b. con priceUnit "pz" -> unità "pz", default dose 1.0
        val (limoneUnit, limoneQty) = resolveQb("q.b.", "pz", "pz")
        assertEquals("pz", limoneUnit)
        assertEquals(1.0, limoneQty, 0.001)

        // Olio: q.b. con priceUnit "l" -> unità "l", default dose 1.0
        val (olioUnit, olioQty) = resolveQb("q.b.", "l", "l")
        assertEquals("l", olioUnit)
        assertEquals(1.0, olioQty, 0.001)

        // Zucchero: q.b. con priceUnit "kg" -> unità "kg", default dose 1.0
        val (zuccheroUnit, zuccheroQty) = resolveQb("q.b.", "kg", "g")
        assertEquals("kg", zuccheroUnit)
        assertEquals(1.0, zuccheroQty, 0.001)
    }

    @Test
    fun testShoppingCostUnitConversion(){
        // Parmigiano: 50 g con prezzo €18.00/kg -> deve risultare €0.90 (NON €900.00!)
        val costParmigiano = MathEngine.calculateComponentCost(
            quantity = 50.0,
            recipeUnit = "g",
            pricePerUnit = 18.00,
            priceUnit = "kg"
        )
        assertNotNull(costParmigiano)
        assertEquals(0.90, costParmigiano!!, 0.001)

        // Olio: 40 ml con prezzo €10.00/l -> deve risultare €0.40 (NON €400.00!)
        val costOlio = MathEngine.calculateComponentCost(
            quantity = 40.0,
            recipeUnit = "ml",
            pricePerUnit = 10.00,
            priceUnit = "l"
        )
        assertNotNull(costOlio)
        assertEquals(0.40, costOlio!!, 0.001)

        // Cipolle: 400 g con prezzo €1.50/kg -> deve risultare €0.60 (NON €600.00!)
        val costCipolle = MathEngine.calculateComponentCost(
            quantity = 400.0,
            recipeUnit = "g",
            pricePerUnit = 1.50,
            priceUnit = "kg"
        )
        assertNotNull(costCipolle)
        assertEquals(0.60, costCipolle!!, 0.001)

        // Uova: 6 unità con prezzo €0.30/unità -> €1.80
        val costUova = MathEngine.calculateComponentCost(
            quantity = 6.0,
            recipeUnit = "unità",
            pricePerUnit = 0.30,
            priceUnit = "unità"
        )
        assertNotNull(costUova)
        assertEquals(1.80, costUova!!, 0.001)
    }
}
