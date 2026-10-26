package panattoni.simo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import panattoni.simo.ui.theme.MacaronBackground
import panattoni.simo.ui.theme.MacaronSubtleText
import panattoni.simo.ui.theme.MacaronText
import panattoni.simo.ui.theme.MacaronTitleSimo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimoTopBar(
    title: String = "SIMO",
    subtitle: String? = null,
    showBackButton: Boolean = false,
    onBackClick: () -> Unit ={},
    actions: @Composable RowScope.() -> Unit ={}
){
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MacaronBackground)
    ){
        TopAppBar(
            title ={
                Column{
                    if (title == "SIMO"){
                        Text(
                            text = "SIMO",
                            color = MacaronTitleSimo,
                            fontFamily = panattoni.simo.ui.theme.BubbleFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp,
                            letterSpacing = 2.sp
                        )
                    } else{
                        Text(
                            text = title,
                            color = MacaronText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (!subtitle.isNullOrBlank() && title != "SIMO"){
                        Text(
                            text = subtitle,
                            color = MacaronSubtleText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            },
            navigationIcon ={
                if (showBackButton){
                    IconButton(onClick = onBackClick){
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Indietro",
                            tint = MacaronText
                        )
                    }
                }
            },
            actions = actions,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MacaronBackground
            )
        )
        if (!subtitle.isNullOrBlank() && title == "SIMO"){
            Text(
                text = subtitle,
                color = MacaronSubtleText,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 6.dp)
            )
        }
    }
}
