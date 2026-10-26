package panattoni.simo.data.model

enum class RecipeSortOption(val displayName: String){
    NAME_ASC("Dalla A alla Z"),
    NAME_DESC("Dalla Z alla A"),
    DATE_DESC("Dal più recente"),
    DATE_ASC("Dal meno recente")
}
