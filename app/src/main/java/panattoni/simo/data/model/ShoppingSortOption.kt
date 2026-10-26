package panattoni.simo.data.model

enum class ShoppingSortOption(val displayName: String){
    NAME_ASC("Dalla A alla Z"),
    NAME_DESC("Dalla Z alla A"),
    DATE_DESC("Dal più recente al meno recente"),
    DATE_ASC("Dal meno recente al più recente")
}
