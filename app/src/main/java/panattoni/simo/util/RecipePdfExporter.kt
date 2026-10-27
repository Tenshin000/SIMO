package panattoni.simo.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import panattoni.simo.data.model.ScaledComponent
import panattoni.simo.data.model.ScaledRecipeTree
import panattoni.simo.domain.Allergen
import panattoni.simo.domain.MeasurementUnit
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Esportatore PDF professionale conforme allo standard A4 (72 dpi).
 * Genera schede ricetta complete con logo SIMO, tabella dosi scalata,
 * tabella allergeni (Reg. UE 1169/2011), costo porzione e passaggi numerati.
 */
object RecipePdfExporter{

    private const val PAGE_WIDTH = 595  // Standard A4 width in points at 72 dpi
    private const val PAGE_HEIGHT = 842 // Standard A4 height in points at 72 dpi
    private const val MARGIN = 40f
    private const val CONTENT_WIDTH = PAGE_WIDTH - (MARGIN * 2)

    /**
     * Genera un PDF professionale a colori per la ricetta scalata corrente
     * e apre il chooser nativo Android per condividerlo o stamparlo.
     */
    fun shareRecipePdf(context: Context, tree: ScaledRecipeTree){
        try{
            val pdfFile = generatePdfFile(context, tree)
            if (pdfFile == null || !pdfFile.exists() || pdfFile.length() == 0L){
                android.widget.Toast.makeText(context, "Errore nella generazione del file PDF.", android.widget.Toast.LENGTH_SHORT).show()
                return
            }
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply{
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Scheda Ricetta: ${tree.recipe.name}")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Scheda tecnica e ingredienti per \"${tree.recipe.name}\" esportata da SIMO."
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Stampa o Condividi Scheda Ricetta")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception){
            e.printStackTrace()
            android.widget.Toast.makeText(
                context,
                "Impossibile condividere il PDF: ${e.localizedMessage ?: "errore imprevisto"}",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }

    fun generatePdfFile(context: Context, tree: ScaledRecipeTree): File?{
        var document: PdfDocument? = null
        return try{
            val recipesDir = File(context.cacheDir, "recipes").apply{ mkdirs() }
            val sanitizedName = tree.recipe.name.replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank{ "ricetta" }
            val file = File(recipesDir, "Ricetta_$sanitizedName.pdf")

            document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            drawRecipeContent(canvas, tree)

            document.finishPage(page)
            FileOutputStream(file).use{ out ->
                document.writeTo(out)
                out.flush()
            }
            file
        } catch (e: Exception){
            e.printStackTrace()
            null
        } finally{
            try{
                document?.close()
            } catch (_: Exception){}
        }
    }

    private fun drawRecipeContent(canvas: Canvas, tree: ScaledRecipeTree){
        var currentY = MARGIN

        // Background / Palette Colors
        val primaryColor = Color.rgb(186, 104, 200)      // Soft Berry / Purple
        val primaryDark = Color.rgb(106, 27, 154)        // Deep Purple
        val headerBgColor = Color.rgb(243, 229, 245)     // Light Pastel Purple
        val tableHeaderBg = Color.rgb(248, 237, 245)     // Very light pastel
        val textColor = Color.rgb(40, 40, 40)
        val subtleText = Color.rgb(110, 110, 110)
        val allergenBg = Color.rgb(255, 243, 224)       // Warm allergen chip
        val allergenText = Color.rgb(230, 81, 0)
        val dividerColor = Color.rgb(230, 230, 230)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 1. Header Card Background
        val headerHeight = 76f
        paint.color = headerBgColor
        canvas.drawRoundRect(
            RectF(MARGIN, currentY, MARGIN + CONTENT_WIDTH, currentY + headerHeight),
            12f, 12f, paint
        )

        // Logo / App Badge "SIMO"
        paint.color = primaryDark
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("SIMO", MARGIN + 14f, currentY + 30f, paint)

        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = subtleText
        canvas.drawText("Scalable Ingredient Management Organizer", MARGIN + 14f, currentY + 44f, paint)

        // Data e Resa sul lato destro dell'header
        val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.ITALIAN).format(Date())
        paint.textAlign = Paint.Align.RIGHT
        paint.textSize = 9f
        paint.color = subtleText
        canvas.drawText("Data: $dateStr", MARGIN + CONTENT_WIDTH - 14f, currentY + 28f, paint)

        val yieldFormatted = ItalianGrammarUtils.formatQuantityWithUnit(tree.targetYieldQuantity, tree.recipe.yieldUnit)
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = primaryDark
        canvas.drawText("Resa: $yieldFormatted", MARGIN + CONTENT_WIDTH - 14f, currentY + 48f, paint)

        paint.textAlign = Paint.Align.LEFT
        currentY += headerHeight + 16f

        // 2. Titolo Ricetta & Descrizione
        paint.color = primaryDark
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(tree.recipe.name, MARGIN, currentY + 18f, paint)
        currentY += 28f

        if (tree.recipe.description.isNotBlank()){
            paint.color = subtleText
            paint.textSize = 10f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            val descLines = wrapText(tree.recipe.description, CONTENT_WIDTH, paint)
            for (line in descLines.take(2)){
                canvas.drawText(line, MARGIN, currentY + 10f, paint)
                currentY += 14f
            }
            currentY += 6f
        }

        // Costo ricetta se presente
        if (tree.hasCostData && tree.totalCost > 0.0){
            val costText = String.format(
                Locale.ITALIAN,
                "Food Cost: €%.2f totali  |  €%.2f (%s)",
                tree.totalCost,
                tree.costPerPortion,
                ItalianGrammarUtils.formatYieldCostLabel(tree.recipe.yieldUnit)
            )
            paint.color = Color.rgb(46, 125, 50)
            paint.textSize = 10.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(costText, MARGIN, currentY + 10f, paint)
            currentY += 20f
        }

        // 3. Tabella Ingredienti
        paint.color = primaryDark
        paint.textSize = 13f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("LISTA INGREDIENTI (Dosi Scalate)", MARGIN, currentY + 12f, paint)
        currentY += 20f

        // Header Tabella
        paint.color = tableHeaderBg
        canvas.drawRoundRect(
            RectF(MARGIN, currentY, MARGIN + CONTENT_WIDTH, currentY + 22f),
            6f, 6f, paint
        )

        paint.color = primaryDark
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Ingrediente", MARGIN + 8f, currentY + 15f, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Quantità", MARGIN + CONTENT_WIDTH - 8f, currentY + 15f, paint)
        paint.textAlign = Paint.Align.LEFT
        currentY += 24f

        // Righe Ingredienti
        val allComps = mutableListOf<ScaledComponent>()
        fun collectComps(node: ScaledRecipeTree){
            for (c in node.components){
                allComps.add(c)
                c.subRecipeTree?.let{ collectComps(it) }
            }
        }
        collectComps(tree)

        paint.textSize = 10f
        for (comp in allComps){
            if (currentY > PAGE_HEIGHT - 120f) break // Limite pagina

            paint.color = textColor
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            val item = comp.componentDetail.item
            val unit = comp.componentDetail.component.unit
            val nameText = if (item.type.name == "RECIPE") "★ ${item.name} (sotto-ricetta)" else item.name
            canvas.drawText(nameText, MARGIN + 8f, currentY + 13f, paint)

            val qtyText = if (MeasurementUnit.isQb(unit)){
                "q.b."
            } else{
                val qtyVal = if (MeasurementUnit.isIntegerUnit(unit)){
                    MeasurementUnit.roundIntegerAmount(comp.scaledQuantity).toDouble()
                } else{
                    comp.scaledQuantity
                }
                val displayUnit = ItalianGrammarUtils.formatUnitByQuantity(unit, qtyVal)
                "${NumberUtils.formatQuantityForUnit(comp.scaledQuantity, unit)} $displayUnit"
            }
            paint.textAlign = Paint.Align.RIGHT
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(qtyText, MARGIN + CONTENT_WIDTH - 8f, currentY + 13f, paint)
            paint.textAlign = Paint.Align.LEFT

            // Linea divisoria sottile
            paint.color = dividerColor
            paint.strokeWidth = 0.8f
            canvas.drawLine(MARGIN + 8f, currentY + 19f, MARGIN + CONTENT_WIDTH - 8f, currentY + 19f, paint)
            currentY += 21f
        }
        currentY += 10f

        // 4. Allergeni Ufficiali UE Evidenziati a Norma di Legge
        val allAllergens = tree.allergens
        if (allAllergens.isNotEmpty() && currentY < PAGE_HEIGHT - 120f){
            paint.color = primaryDark
            paint.textSize = 12f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("ALLERGENI PRESENTI (Regolamento UE 1169/2011)", MARGIN, currentY + 12f, paint)
            currentY += 18f

            // Box Allergeni
            paint.color = allergenBg
            val allergenStr = allAllergens.joinToString(", "){ it.displayName }
            val allergenLines = wrapText(allergenStr, CONTENT_WIDTH - 20f, paint)
            val boxHeight = (allergenLines.size * 14f) + 16f

            canvas.drawRoundRect(
                RectF(MARGIN, currentY, MARGIN + CONTENT_WIDTH, currentY + boxHeight),
                8f, 8f, paint
            )

            paint.color = allergenText
            paint.textSize = 9.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            var textY = currentY + 14f
            for (line in allergenLines){
                canvas.drawText("⚠️  $line", MARGIN + 10f, textY, paint)
                textY += 14f
            }
            currentY += boxHeight + 16f
        }

        // 5. Procedimento di Preparazione Numerato
        if (tree.recipe.instructions.isNotBlank() && currentY < PAGE_HEIGHT - 80f){
            paint.color = primaryDark
            paint.textSize = 12f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("PROCEDIMENTO DI PREPARAZIONE", MARGIN, currentY + 12f, paint)
            currentY += 20f

            val lines = tree.recipe.instructions.lines().map{ it.trim() }.filter{ it.isNotEmpty() }
            paint.color = textColor
            paint.textSize = 9.5f
            for ((index, step) in lines.withIndex()){
                if (currentY > PAGE_HEIGHT - 40f) break
                val cleanStep = if (step.matches(Regex("""^\d+[.)\-].*"""))) step else "${index + 1}. $step"
                val wrapped = wrapText(cleanStep, CONTENT_WIDTH - 10f, paint)
                for (wLine in wrapped){
                    if (currentY > PAGE_HEIGHT - 35f) break
                    canvas.drawText(wLine, MARGIN + 6f, currentY + 11f, paint)
                    currentY += 14f
                }
                currentY += 4f
            }
        }

        // Footer di chiusura
        paint.color = subtleText
        paint.textSize = 8f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(
            "Documento generato dall'app SIMO - Scalable Ingredient Management Organizer",
            PAGE_WIDTH / 2f,
            PAGE_HEIGHT - 16f,
            paint
        )
    }

    private fun wrapText(text: String, maxWidth: Float, paint: Paint): List<String>{
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""

        for (word in words){
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            val width = paint.measureText(testLine)
            if (width > maxWidth){
                if (currentLine.isNotEmpty()) lines.add(currentLine)
                currentLine = word
            } else{
                currentLine = testLine
            }
        }
        if (currentLine.isNotEmpty()){
            lines.add(currentLine)
        }
        return lines
    }
}
