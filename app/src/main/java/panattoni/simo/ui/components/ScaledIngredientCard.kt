package panattoni.simo.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import panattoni.simo.data.model.ItemType
import panattoni.simo.data.model.ScaledComponent
import panattoni.simo.domain.Allergen
import panattoni.simo.domain.MeasurementUnit
import panattoni.simo.ui.theme.MacaronBackground
import panattoni.simo.ui.theme.MacaronCard
import panattoni.simo.ui.theme.MacaronCardBorder
import panattoni.simo.ui.theme.MacaronPrimary
import panattoni.simo.ui.theme.MacaronRecipeBadge
import panattoni.simo.ui.theme.MacaronSubtleText
import panattoni.simo.ui.theme.MacaronSurfaceVariant
import panattoni.simo.ui.theme.MacaronText

/**
 * Scheda interattiva per visualizzare un ingrediente scalato,
 * con supporto per sotto-ricette annidate, badge allergeni, food cost e reverse scaling.
 */
@Composable
fun ScaledIngredientCard(
    scaledComponent: ScaledComponent,
    onReverseScaleClick: () -> Unit,
    onDrillDownClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
){
    val item = scaledComponent.componentDetail.item
    val isRecipe = item.type == ItemType.RECIPE
    var isExpanded by remember{ mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MacaronCard),
        border = BorderStroke(1.dp, if (isRecipe) MacaronPrimary.copy(alpha = 0.5f) else MacaronCardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ){
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ){
            // Riga principale (Info ingrediente/ricetta + Quantità/Reverse Scale + Espansione)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ){
                // Info ingrediente / ricetta (Sinistra)
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ){
                    if (isRecipe){
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MacaronSurfaceVariant,
                            modifier = Modifier
                                .size(36.dp)
                                .padding(top = 2.dp)
                        ){
                            Box(contentAlignment = Alignment.Center){
                                Icon(
                                    imageVector = Icons.Outlined.RestaurantMenu,
                                    contentDescription = "Ricetta",
                                    tint = MacaronPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))
                    }

                    Column{
                        if (isRecipe){
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MacaronRecipeBadge.copy(alpha = 0.12f),
                                modifier = Modifier.padding(bottom = 2.dp)
                            ){
                                Text(
                                    text = "RICETTA",
                                    color = MacaronRecipeBadge,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = item.name,
                            color = if (isRecipe) Color(0xFF1E88E5) else MacaronText,
                            fontWeight = if (isRecipe) FontWeight.Bold else FontWeight.SemiBold,
                            fontSize = 16.sp,
                            textDecoration = if (isRecipe) TextDecoration.Underline else null,
                            style = LocalTextStyle.current.copy(
                                hyphens = Hyphens.None,
                                lineBreak = LineBreak.Paragraph
                            ),
                            modifier = if (isRecipe && onDrillDownClick != null){
                                Modifier.clickable{ onDrillDownClick(item.id) }
                            } else Modifier
                        )
                        val isQb = MeasurementUnit.isQb(scaledComponent.componentDetail.component.unit)
                        if (isQb){
                            val unitPriceDesc = if (item.pricePerUnit != null && item.pricePerUnit > 0.0){
                                " (€${String.format(java.util.Locale.US, "%.2f", item.pricePerUnit)}/${item.priceUnit ?: item.defaultUnit})"
                            } else ""
                            Text(
                                text = "€0.00$unitPriceDesc",
                                color = Color(0xFF2E7D32),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        } else if (scaledComponent.componentCost != null && scaledComponent.componentCost > 0.0){
                            val costFormatted = String.format(java.util.Locale.US, "€%.2f", scaledComponent.componentCost)
                            val unitPriceDesc = if (item.pricePerUnit != null){
                                " (€${String.format(java.util.Locale.US, "%.2f", item.pricePerUnit)}/${item.priceUnit ?: item.defaultUnit})"
                            } else ""
                            Text(
                                text = "$costFormatted$unitPriceDesc",
                                color = Color(0xFF2E7D32),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }

                // Quantità calcolata e pulsante Reverse Scale + Chevron (Destra)
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ){
                    val isQb = MeasurementUnit.isQb(scaledComponent.componentDetail.component.unit)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MacaronSurfaceVariant,
                        modifier = (if (!isQb) Modifier.clickable{ onReverseScaleClick() } else Modifier)
                            .padding(end = 4.dp)
                    ){
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ){
                            if (!isQb){
                                Icon(
                                    imageVector = Icons.Default.Scale,
                                    contentDescription = "Reverse scale",
                                    tint = MacaronPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = scaledComponent.formattedScaledQuantity,
                                color = MacaronText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }

                    if (isRecipe && scaledComponent.subRecipeTree != null){
                        IconButton(
                            onClick ={ isExpanded = !isExpanded },
                            modifier = Modifier.size(32.dp)
                        ){
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isExpanded) "Comprimi" else "Espandi",
                                tint = MacaronPrimary
                            )
                        }
                    }
                }
            }

            // Allergeni a tutta larghezza (FlowRow pulito per evitare overflow o allungamenti verticali)
            val compAllergens = scaledComponent.subRecipeTree?.allergens ?: item.allergensList
            if (compAllergens.isNotEmpty()){
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ){
                    compAllergens.forEach{ allergen ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFFFF3E0),
                            border = BorderStroke(0.5.dp, Color(0xFFFFB74D))
                        ){
                            Text(
                                text = "${allergen.emoji} ${allergen.displayName}",
                                fontSize = 10.sp,
                                color = Color(0xFFE65100),
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Sezione espandibile sotto-ricetta (Drill-down inline)
            if (isRecipe && scaledComponent.subRecipeTree != null){
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ){
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .background(MacaronBackground.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ){
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ){
                            Text(
                                text = "Dosi per ${scaledComponent.formattedScaledQuantity} di ${item.name}:",
                                color = MacaronPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f, fill = false)
                            )

                            if (onDrillDownClick != null){
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    onClick = { onDrillDownClick(item.id) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = MacaronRecipeBadge.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, MacaronRecipeBadge.copy(alpha = 0.25f))
                                ){
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ){
                                        Text(
                                            text = "Vedi ricetta",
                                            color = MacaronRecipeBadge,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Icon(
                                            imageVector = Icons.Default.ChevronRight,
                                            contentDescription = null,
                                            tint = MacaronRecipeBadge,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Sub-ingredienti già ricalcolati
                        scaledComponent.subRecipeTree.components.forEach{ subComp ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ){
                                Text(
                                    text = "• ${subComp.componentDetail.item.name}",
                                    color = MacaronText,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = subComp.formattedScaledQuantity,
                                    color = MacaronText,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
