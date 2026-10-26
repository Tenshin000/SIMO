package panattoni.simo

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import panattoni.simo.service.CookingSessionManager
import panattoni.simo.ui.navigation.SimoNavHost
import panattoni.simo.ui.theme.SIMOTheme

// Classe principale dell'applicazione. Eredita da ComponentActivity,
// che è la classe base standard per le app Android che utilizzano Jetpack Compose.
class MainActivity : ComponentActivity(){
    // Il metodo onCreate viene eseguito quando l'Activity viene creata.
    // È il punto di partenza per l'inizializzazione e la configurazione dell'interfaccia utente.
    override fun onCreate(savedInstanceState: Bundle?){
        // Richiama il metodo della classe padre per le operazioni di setup fondamentali dell'Activity
        super.onCreate(savedInstanceState)

        // Inizializza la gestione persistente delle sessioni di cottura e timer
        CookingSessionManager.init(applicationContext)

        // Abilita il layout "edge-to-edge", permettendo all'interfaccia dell'app di
        // estendersi dietro le barre di sistema (status bar in alto e navigation bar in basso)
        enableEdgeToEdge()

        // setContent definisce l'interfaccia utente dell'Activity utilizzando Jetpack Compose
        // invece dei classici file di layout XML.
        setContent{
            // Applica il tema visivo dell'applicazione (colori, tipografia, forme)
            SIMOTheme{
                // Richiama il componente che gestisce la navigazione dell'app.
                // Si occupa di mostrare le diverse schermate in base alle azioni dell'utente.
                SimoNavHost()
            }
        }
    }
}