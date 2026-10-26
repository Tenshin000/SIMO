package panattoni.simo.domain

enum class UnitCategory{
    WEIGHT,
    VOLUME,
    DISCRETE,
    ARBITRARY
}

data class MeasurementUnit(
    val code: String,
    val displayName: String,
    val category: UnitCategory,
    val isDiscrete: Boolean = false,
    val defaultStep: Double = 1.0
){
    companion object{
        val GRAM = MeasurementUnit("g", "Grammi (g)", UnitCategory.WEIGHT, defaultStep = 10.0)
        val MILLILITER = MeasurementUnit("ml", "Millilitri (ml)", UnitCategory.VOLUME, defaultStep = 10.0)
        val UNIT = MeasurementUnit("unità", "Unità", UnitCategory.DISCRETE, isDiscrete = true, defaultStep = 1.0)
        val PIECE = MeasurementUnit("pz", "Pezzi (pz)", UnitCategory.DISCRETE, isDiscrete = true, defaultStep = 1.0)
        val KILOGRAM = MeasurementUnit("kg", "Chilogrammi (kg)", UnitCategory.WEIGHT, defaultStep = 0.1)
        val LITER = MeasurementUnit("l", "Litri (l)", UnitCategory.VOLUME, defaultStep = 0.1)
        val TABLESPOON = MeasurementUnit("cucchiaio", "Cucchiai", UnitCategory.DISCRETE, isDiscrete = true, defaultStep = 1.0)
        val TEASPOON = MeasurementUnit("cucchiaino", "Cucchiaini", UnitCategory.DISCRETE, isDiscrete = true, defaultStep = 1.0)
        val PINCH = MeasurementUnit("pizzico", "Pizzico", UnitCategory.DISCRETE, isDiscrete = true, defaultStep = 1.0)
        val QB = MeasurementUnit("q.b.", "Quanto basta (q.b.)", UnitCategory.ARBITRARY, defaultStep = 1.0)

        val ALL_UNITS = listOf(
            GRAM,
            MILLILITER,
            UNIT,
            PIECE,
            KILOGRAM,
            LITER,
            TABLESPOON,
            TEASPOON,
            PINCH,
            QB
        )

        fun fromCode(code: String): MeasurementUnit{
            return ALL_UNITS.firstOrNull{ it.code.equals(code.trim(), ignoreCase = true) }
                ?: MeasurementUnit(code, code, UnitCategory.DISCRETE, isDiscrete = isIntegerUnit(code))
        }

        private val QB_CODES = setOf("q.b.", "q.b", "qb", "quanto basta")
        private val INTEGER_UNIT_CODES = setOf(
            "unità", "unita", "unit",
            "pz", "pezzi", "pezzo",
            "cucchiaio", "cucchiai",
            "cucchiaino", "cucchiaini",
            "pizzico", "pizzichi"
        )

        fun isQb(unitCode: String?): Boolean{
            if (unitCode.isNullOrBlank()) return false
            return unitCode.trim().lowercase() in QB_CODES
        }

        fun isIntegerUnit(unitCode: String?): Boolean{
            if (unitCode.isNullOrBlank()) return false
            return unitCode.trim().lowercase() in INTEGER_UNIT_CODES
        }

        /**
         * Approssimazione per unità intere (unità, pezzi, cucchiai, cucchiaini):
         * - Se i decimali sono < .5 si approssima per difetto
         * - Se i decimali sono >= .5 si approssima per eccesso
         * - Mai messi con la virgola
         */
        fun roundIntegerAmount(amount: Double): Long{
            if (!amount.isFinite() || amount.isNaN()) return 0L
            val floorVal = kotlin.math.floor(amount)
            val decimal = amount - floorVal
            return if (decimal < 0.5){
                floorVal.toLong()
            } else{
                kotlin.math.ceil(amount).toLong()
            }
        }

        fun getDefaultQuantityForUnit(unitCode: String): String{
            if (isQb(unitCode)) return ""
            val normalized = unitCode.trim().lowercase()
            return if (normalized == "g" || normalized == "ml"){
                "100"
            } else{
                "1"
            }
        }

        fun formatAmount(amount: Double, unitCode: String): String{
            if (isQb(unitCode)){
                return "q.b."
            }
            if (isIntegerUnit(unitCode)){
                val rounded = roundIntegerAmount(amount)
                return "$rounded $unitCode"
            }
            val rounded = Math.round(amount * 10.0) / 10.0
            return if (Math.abs(rounded - Math.round(rounded)) < 0.01){
                "${Math.round(rounded).toInt()} $unitCode"
            } else{
                String.format(java.util.Locale.US, "%.1f %s", rounded, unitCode)
            }
        }
    }
}
