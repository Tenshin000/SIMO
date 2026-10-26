package panattoni.simo.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import panattoni.simo.data.model.ScaledComponent
import panattoni.simo.domain.MeasurementUnit
import panattoni.simo.ui.theme.MacaronCard
import panattoni.simo.ui.theme.MacaronPrimary
import panattoni.simo.ui.theme.MacaronSubtleText
import panattoni.simo.ui.theme.MacaronText
import panattoni.simo.util.NumberUtils

/**
 * Finestra di dialogo per il Reverse Scaling: calcola proporzionalmente
 * tutte le dosi della ricetta a partire dalla quantità di ingrediente disponibile.
 */
@Composable
fun ReverseScaleDialog(
    scaledComponent: ScaledComponent,
    onDismiss: () -> Unit,
    onConfirm: (newAmount: Double) -> Unit
){
    val comp = scaledComponent.componentDetail.component
    val item = scaledComponent.componentDetail.item
    val isInteger = MeasurementUnit.isIntegerUnit(comp.unit)

    var inputAmount by remember{
        mutableStateOf(
            if (isInteger){
                MeasurementUnit.roundIntegerAmount(scaledComponent.scaledQuantity).toString()
            } else{
                NumberUtils.formatAmount(scaledComponent.scaledQuantity)
            }
        )
    }

    var isError by remember{ mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MacaronCard,
        shape = RoundedCornerShape(20.dp),
        title ={
            Column{
                Text(
                    text = "Reverse Scaling",
                    color = MacaronPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = item.name,
                    color = MacaronText,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }
        },
        text ={
            Column(modifier = Modifier.fillMaxWidth()){
                Text(
                    text = "Inserisci la quantità che hai a disposizione. Tutti gli altri ingredienti e le altre ricette verranno riproporzionati istantaneamente per mantenere intatte le proporzioni.",
                    color = MacaronSubtleText,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = inputAmount,
                    onValueChange ={ input ->
                        val sanitized = if (isInteger) input.filter{ it.isDigit() } else input
                        inputAmount = sanitized
                        isError = !NumberUtils.isValidPositiveNumber(sanitized)
                    },
                    label ={ Text("Quantità disponibile (${comp.unit})") },
                    isError = isError,
                    keyboardOptions = KeyboardOptions(keyboardType = if (isInteger) KeyboardType.Number else KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                if (isError){
                    Text(
                        text = if (isInteger) "Inserisci un numero intero positivo (es. 2 o 5)" else "Inserisci un numero valido maggiore di zero (es. 250 o 1,5)",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton ={
            Button(
                onClick ={
                    val parsed = NumberUtils.parseDoubleOrNull(inputAmount)
                    if (parsed != null && parsed > 0.0){
                        val finalAmount = if (isInteger) MeasurementUnit.roundIntegerAmount(parsed).toDouble() else parsed
                        onConfirm(finalAmount)
                    } else{
                        isError = true
                    }
                },
                enabled = NumberUtils.isValidPositiveNumber(inputAmount),
                colors = ButtonDefaults.buttonColors(containerColor = MacaronPrimary),
                shape = RoundedCornerShape(10.dp)
            ){
                Text("Ricalcola tutto", color = MacaronCard, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton ={
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ){
                Text("Annulla", color = MacaronSubtleText)
            }
        }
    )
}
