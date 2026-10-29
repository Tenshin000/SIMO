# Scalable Ingredient Management Organizer
> **Scalable Ingredient Management Organizer** (**SIMO**) è un'applicazione Android concepita per chef, maestri pasticceri, laboratori di panificazione artigianale e appassionati esigenti. Nasce dalla richiesta reale di un maestro pasticcere di disporre di uno strumento avanzato per il calcolo proporzionale delle dosi, delle rese in cottura e del food cost. SIMO modella il ricettario come un grafo matematico gerarchico ad albero (DAG), offrendo scalatura dinamica lineare e inversa (*reverse scaling*), calcolo analitico del food cost al centesimo, tracciabilità normativa rigorosa dei 14 allergeni alimentari, etichetta con dichiarazione nutrizionale europea (Regolamento UE 1169/2011), cooking mode a schermo sempre attivo con timer multipli concorrenti resilienti in background, lista della spesa intelligente con aggregazione automatica, esportazione di schede tecniche professionali in formato PDF multipagina A4 e assistenza culinaria generativa bivalente con Google Gemini AI e OpenAI ChatGPT.

---

## Indice Generale della Documentazione
1. [Guida all'Installazione](#guida-allinstallazione)
   * [Requisiti di Sistema](#requisiti-di-sistema)
   * [Installazione Diretta tramite APK (Sideloading)](#installazione-diretta-tramite-apk-sideloading)
   * [Installazione Rapida via ADB (Android Debug Bridge)](#installazione-rapida-via-adb-android-debug-bridge)
   * [Differenza tra SIMO-release.apk e SIMO-debug.apk](#differenza-tra-simo-releaseapk-e-simo-debugapk)
   * [Compilazione dal Codice Sorgente (Per Sviluppatori)](#compilazione-dal-codice-sorgente-per-sviluppatori)
2. [Manuale d'Istruzioni e Guida all'Uso (Versione 2.0)](#manuale-distruzioni-e-guida-alluso-versione-20)
   * [1. Benvenuti in SIMO](#1-benvenuti-in-simo)
   * [2. Esplorare il Ricettario e la Schermata Home](#2-esplorare-il-ricettario-e-la-schermata-home)
   * [3. Creazione e Modifica delle Ricette](#3-creazione-e-modifica-delle-ricette)
   * [4. Il Cuore di SIMO: Scaling, Cottura e Reverse Scaling](#4-il-cuore-di-simo-scaling-cottura-e-reverse-scaling)
   * [5. Dichiarazione Nutrizionale UE, Allergeni e Food Cost](#5-dichiarazione-nutrizionale-ue-allergeni-e-food-cost)
   * [6. Cooking Mode e Timer Multipli Integrati](#6-cooking-mode-e-timer-multipli-integrati)
   * [7. Lista della Spesa Intelligente](#7-lista-della-spesa-intelligente)
   * [8. Assistente Chef AI, Stampa PDF Multipagina e Backup](#8-assistente-chef-ai-stampa-pdf-multipagina-e-backup)
3. [Manuale Tecnico e Architettura di Sistema (Versione 2.0)](#manuale-tecnico-e-architettura-di-sistema-versione-20)
   * [1. Introduzione e Requisiti](#1-introduzione-e-requisiti)
   * [2. Architettura del Software e Pattern Architetturali](#2-architettura-del-software-e-pattern-architetturali)
   * [3. Domain Engine: Teoria dei Grafi, Nutrizione e Calcolo Matematico](#3-domain-engine-teoria-dei-grafi-nutrizione-e-calcolo-matematico)
   * [4. Strato Dati, Room Database e Persistenza Reattiva](#4-strato-dati-room-database-e-persistenza-reattiva)
   * [5. Integrazione Cloud AI: Dual-Engine Google Gemini & OpenAI ChatGPT](#5-integrazione-cloud-ai-dual-engine-google-gemini--openai-chatgpt)
   * [6. Rendering Grafico e Generazione PDF Multipagina](#6-rendering-grafico-e-generazione-pdf-multipagina)
   * [7. Sottosistema di Backup, Serializzazione JSON e Risoluzione Conflitti](#7-sottosistema-di-backup-serializzazione-json-e-risoluzione-conflitti)
   * [8. Testing, Validazione e Qualità del Codice](#8-testing-validazione-e-qualità-del-codice)

---

# Guida all'Installazione

In questa sezione vengono descritti i requisiti hardware e software, le modalità operative di installazione autonoma dei pacchetti precompilati APK su dispositivi Android (sia manuale via file manager sia automatizzata via ADB) e le istruzioni di compilazione per sviluppatori.

### Requisiti di Sistema
* **Sistema Operativo:** Android 8.0 (Oreo, API Level 26) o versioni successive (pienamente verificato e ottimizzato per Android 14 e Android 15 / API Level 35+).
* **Architetture Supportate:** ARM64 (`arm64-v8a`), ARMv7 (`armeabi-v7a`), x86 e x86_64.
* **Memoria RAM:** Minimo 2 GB (consigliati 3 GB o superiori per calcoli DAG estesi e rendering reattivo a 60 FPS).
* **Spazio su Disco:** Circa 75 MB per l'installazione dell'applicazione e archiviazione del database locale SQLite (Room).

### Installazione Diretta tramite APK (Sideloading)
L'applicazione è distribuita direttamente come pacchetto APK firmato, consentendo l'installazione autonoma senza necessità del Google Play Store:

1. **Scaricare il pacchetto APK:**
   * Copiare o scaricare sul dispositivo Android il file `SIMO-release.apk` (consigliato per la produzione) oppure `SIMO-debug.apk`.
2. **Abilitare l'installazione da origini sconosciute:**
   * Toccando il file APK scaricato dal browser o dal gestore file di sistema, Android presenterà un avviso di sicurezza.
   * Premere **Impostazioni** nel popup e attivare la spunta **Consenti da questa origine** per autorizzare il browser o il file manager a installare pacchetti esterni.
3. **Completare l'installazione:**
   * Tornare alla schermata precedente e toccare **Installa**.
   * Al termine dell'operazione, toccare **Apri** per avviare SIMO.

> [!NOTE]
> **Avviso Google Play Protect:**  
> Il pacchetto di produzione `SIMO-release.apk` è firmato crittograficamente con il Keystore personale dello sviluppatore (RSA 2048-bit, schemi di firma v1, v2 e v3, emesso a nome di *Francesco Panattoni, SIMO*). Trattandosi di un'installazione autonoma esterna, Play Protect potrebbe mostrare la schermata *"App di sviluppatore sconosciuto"*. È sufficiente premere **Ulteriori dettagli** e confermare con **Installa comunque**.

### Installazione Rapida via ADB (Android Debug Bridge)
Se il dispositivo Android è connesso al computer con il debug USB abilitato, o se si utilizza un emulatore, l'installazione può essere completata con un unico comando da terminale:

```powershell
# Installazione del pacchetto di produzione Release (consigliato)
adb install -r SIMO-release.apk

# Oppure installazione della variante Debug
adb install -r SIMO-debug.apk
```

Il flag `-r` consente di reinstallare l'applicazione mantenendo inalterati i dati del database locale SQLite e le preferenze dell'utente.

### Differenza tra SIMO-release.apk e SIMO-debug.apk

| Proprietà | `SIMO-release.apk` (Consigliato per l'Uso Quotidiano) | `SIMO-debug.apk` (Sviluppo e Diagnostica) |
|---|---|---|
| **Ottimizzazione R8 / ProGuard** | **Abilitata:** Codice ottimizzato, minificato, dead-code rimosso e desugaring Java 8+ attivo. | **Disabilitata:** Conserva i simboli di debug completi e le tabelle di riga. |
| **Prestazioni di Esecuzione** | Massima velocità di calcolo matematico, rendering Compose a 60 FPS fluido e privo di jank. | Minore fluidità dovuta al tracciamento di debug a runtime. |
| **Dimensione Pacchetto** | Circa **8–12 MB** (snello ed efficiente). | Circa **25–35 MB** (più voluminoso). |
| **Firma Crittografica** | Keystore ufficiale di produzione con chiave RSA-2048 certificata (*Francesco Panattoni*). | Chiave standard `androiddebugkey` generata dal toolchain Gradle locale. |
| **Destinazione d'Uso** | Dispositivo personale, banco di lavoro del laboratorio, produzione. | Test su emulatore, diagnostica Logcat, ispezione del database con Android Studio. |

### Compilazione dal Codice Sorgente (Per Sviluppatori)
Per compilare il progetto partendo dal codice sorgente:

1. **Prerequisiti:**
   * **Git** installato.
   * **JDK 17** o superiore (es. OpenJDK 17, Eclipse Temurin 17).
   * **Android Studio** (Koala, Ladybug o versioni successive) con Android SDK Platforms 26–35 installati.
2. **Clonazione della Repository:**
   ```bash
   git clone https://github.com/tuo-username/SIMO.git
   cd SIMO
   ```
3. **Esecuzione dei Test Unitari e Compilazione via Gradle:**
   * **Su Windows (PowerShell):**
     ```powershell
     # Esecuzione della suite completa di 59 test unitari
     .\gradlew.bat testDebugUnitTest

     # Compilazione pacchetti APK
     .\gradlew.bat assembleRelease
     .\gradlew.bat assembleDebug
     ```
   * **Su Linux / macOS:**
     ```bash
     chmod +x gradlew
     ./gradlew testDebugUnitTest
     ./gradlew assembleRelease
     ```
4. **Locazione dei file generati:**
   * Release APK: `app/build/outputs/apk/release/app-release.apk`
   * Debug APK: `app/build/outputs/apk/debug/app-debug.apk`

---

# Manuale d'Istruzioni e Guida all'Uso (Versione 2.0)

## 1. Benvenuti in SIMO

**SIMO** (*Scalable Ingredient Management Organizer*) nasce per rispondere in modo scientifico, elegante ed intuitivo a un'esigenza fondamentale di chiunque operi in cucina, in pasticceria o in un laboratorio artigianale: **il calcolo proporzionale e analitico delle dosi, delle rese e dei costi**.

Spesso i ricettari tradizionali presentano formulazioni calibrate su dosi fisse (ad esempio una torta per 8 persone o un impasto per 500 g di farina), ma le reali esigenze produttive sono mutevoli: un banchetto con 45 invitati, una fornitura di 80 cornetti, oppure una dispensa in cui sono rimasti solo 130 g di burro o 2 uova. Inoltre, la moderna legislazione alimentare europea (Regolamento UE 1169/2011) impone una trasparenza rigorosa sia sugli **allergeni** sia sulla **dichiarazione nutrizionale** obbligatoria dei prodotti finiti.

### Le Caratteristiche e Innovazioni di SIMO
SIMO non è un semplice taccuino di appunti, bensì un sofisticato **motore matematico e relazionale a grafo (DAG)**:
* **Ricalcolo Dinamico Diretto (Scaling):** Modifichi la resa (es. porzioni, pezzi, grammi) e tutti gli ingredienti si aggiornano istantaneamente con precisione decimale.
* **Modificatore Peso e Resa in Cottura:** Imposti la variazione di peso subita durante la cottura (calo peso negativo per evaporazione nel forno come per torte e carni, oppure aumento di massa positivo per assorbimento d'acqua come per pasta e riso).
* **Dichiarazione Nutrizionale UE Integrata (Reg. UE 1169/2011):** Tabella nutrizionale completa con i 6 valori fondamentali (Calorie, Carboidrati, Grassi, Proteine, Acqua, Sodio e Sale equivalente calcolato), espressi sia per 100 g sia per singola porzione di prodotto finito cotto.
* **Reverse Scaling dalla Dispensa:** Inserisci la quantità dell'ingrediente limitante rimasto in magazzino e SIMO riparametra l'intera ricetta a ritroso.
* **Sotto-Ricette Gerarchiche a Cascata (DAG):** Colleghi preparazioni base (es. *Crema Pasticcera*) all'interno di ricette composte (es. *Cornetto alla Crema*). La scalatura e gli allergeni si propagano automaticamente lungo l'intero albero matematico.
* **Food Cost al Centesimo ed Equivalenza Culinaria:** Calcolo in tempo reale del costo totale dell'impasto e del costo industriale per singola porzione, con conversioni metriche automatiche ($g \leftrightarrow kg$, $ml \leftrightarrow l$) ed equivalenza culinaria massa-volume 1:1.
* **Esportazione PDF Professionale A4 (Minimo 2 Pagine):** Genera schede tecniche vettoriali di laboratorio: Pagina 1 con metadati, ingredienti, food cost, allergeni e tabella nutrizionale UE; Pagina 2 con il procedimento di preparazione ad alta visibilità in nero pieno.
* **Cooking Mode Protetta:** Schermo sempre attivo (*Keep Screen On*) e timer multipli concorrenti con estrazione automatica dei tempi dalle istruzioni.
* **Assistente Chef AI Bivalente (Gemini & ChatGPT):** Generazione completa, completamento intelligente delle dosi mancanti e stima nutrizionale non-distruttiva, con preservazione dell'unità di misura desiderata (es. *torta*, *bicchieri*, *panini*).
* **Nuova Categoria Predefinita Panini:** Categoria gastronomica dedicata per la panificazione e la paninoteca artigianale.

---

## 2. Esplorare il Ricettario e la Schermata Home

All'avvio dell'applicazione verrai accolto dalla schermata principale, disegnata secondo una palette di colori pastello morbidi (*Soft Macaron*) che garantisce un contrasto ideale sia alla luce naturale del laboratorio sia in ambienti di cottura. Grazie al motore di pre-caricamento asincrono immediato, l'intero ricettario viene popolato istantaneamente, eliminando qualsiasi schermata bianca transitoria.

![Schermata Principale di SIMO](docs/img/screen_optimized_home.png)

### 2.1 Comprendere la Scheda di ciascuna Ricetta
Ogni elemento del ricettario mostra a colpo d'occhio i dati fondamentali:
* **Nome e Descrizione Culinaria:** Il titolo della ricetta e un breve riassunto delle sue caratteristiche organolettiche e procedurali.
* **Badge Categoria (`Dolci`, `Pizze`, `Panini`, `Primi`):** Segnala la tipologia di preparazione e permette il filtraggio tematico con un tocco.
* **Badge Resa Base:** Indica le dosi originali registrate (ad esempio *Resa base: 8 porzioni*, *10 cornetti* oppure *500 g*).
* **Icona Preferiti (Stella):** Con un semplice tocco sulla stella puoi contrassegnare o rimuovere qualsiasi ricetta dai tuoi preferiti.
* **Pulsanti Rapidi:** L'icona **Matita** apre la modifica della ricetta, il **Cestino** consente l'eliminazione sicura (con salvaguardia delle ricette correlate) e il pulsante **Vedi Ricetta** conduce direttamente alla console di dettaglio e ricalcolo.

### 2.2 Ricerca, Categorie Predefinite e Ordinamento
Nella barra superiore (*Top Bar*) trovi tutti gli strumenti per gestire il catalogo:
1. **Barra Scorrevole delle Categorie:** Include le categorie predefinite dell'applicazione (*Dolci, Pizze, Panetteria, Panini, Antipasti, Primi, Secondi, Contorni, Zuppe, Alcolici, Analcolici, Caffetteria*) oltre a eventuali tag personalizzati, ordinati alfabeticamente. Toccando un tag, il ricettario mostra solo le preparazioni corrispondenti.
2. **Icona Ordinamento ($\equiv$):** Permette di visualizzare le ricette secondo diversi criteri:
   * *Dalla A alla Z* / *Dalla Z alla A* (ordine alfabetico);
   * *Dal più recente* / *Dal meno recente* (ordine cronologico);
   * *Solo Preferiti* (mostra unicamente le preparazioni contrassegnate con la stella).
3. **Campo di Ricerca Testuale:** Digitando una parola, il catalogo filtra all'istante le ricette per corrispondenza nel titolo o negli ingredienti.

![Menu di Ordinamento e Disposizione](docs/img/sort_menu_open.png)

---

## 3. Creazione e Modifica delle Ricette

SIMO offre un ambiente di redazione avanzato, accessibile tramite il pulsante circolare **[+]** nella schermata principale oppure toccando la **Matita** su una ricetta esistente.

![Schermata di Modifica Ricetta](docs/img/screen_recipe_edit.png)

### 3.1 Dati Generali e Modificatore Peso in Cottura
Nella prima sezione della scheda è possibile impostare:
* **Nome della Ricetta:** Titolo formale della preparazione.
* **Categoria e Tag:** Selezione rapida tra i tag ufficiali (compreso *Panini*) o inserimento di una categoria personalizzata.
* **Resa Base e Unità di Resa:** La quantità di riferimento (es. *4 porzioni*, *1 torta*, *2 bicchieri*, *6 panini*).
* **Modificatore Peso in Cottura (%):** Campo fondamentale introdotto nella versione 2.0. Permette di registrare la percentuale di variazione della massa durante il trattamento termico:
  * **Valori Negativi (Calo Peso):** Es. `-10%` per torte e crostate, `-15%` per arrosti o `-8%` per creme da pasticceria che perdono acqua per evaporazione in forno o sul fornello.
  * **Valori Positivi (Resa/Aumento di Massa):** Es. `+120%` per pasta secca o riso che assorbono liquidi durante la bollitura raddoppiando o triplicando il proprio peso iniziale.
  * **Valore Neutro (0%):** Per preparazioni a crudo, cocktail o piatti freddi senza variazione ponderale.

### 3.2 Inserimento e Gestione dei 6 Valori Nutrizionali
Toccando ciascun ingrediente, si accede alla configurazione analitica dettagliata:
Oltre al nome, alla dose, al costo commerciale e ai 14 allergeni, per ogni ingrediente è possibile registrare i **6 parametri nutrizionali ufficiali per 100 g**:
1. **Calorie (kcal):** Valore energetico espresso in chilocalorie.
2. **Carboidrati (g):** Contenuto di glucidi per 100 g.
3. **Grassi (g):** Contenuto lipidico per 100 g.
4. **Proteine (g):** Quota proteica per 100 g.
5. **Acqua / Umidità Residua (g):** Grammi d'acqua presenti in 100 g di prodotto.
6. **Sodio (mg):** Milligrammi di sodio (da cui SIMO calcola automaticamente l'equivalente in sale: $\text{Sale} = \text{Sodio [g]} \times 2.5$).

### 3.3 Banner Assistente AI: Calcola Nutrizione vs Completa Dosi
Nella parte superiore dell'editor è collocato il banner dell'Assistente AI. Toccando **Apri Assistente AI** si apre la finestra di dialogo operativa:
* **Calcola Valori Nutrizionali:** Analizza gli ingredienti già presenti e calcola esclusivamente i 6 valori nutrizionali per 100 g basandosi su tabelle ufficiali (CREA / USDA). **Non modifica né cancella** le pesature, i costi unitari e gli allergeni precedentemente inseriti!
* **Completa Dosi Mancanti:** Perfeziona la ricetta bilanciando grammature, costi, allergeni e valori nutrizionali per gli ingredienti privi di dose, senza aggiungere o rimuovere ingredienti.
* **Genera Ricetta Completa:** Genera da zero l'intera ricetta (dosi, costi, allergeni, valori nutrizionali, calo peso e passaggi numerati).

![Opzioni dedicate dell'Assistente AI](docs/img/screen_ai_options_dialog.png)

---

## 4. Il Cuore di SIMO: Scaling, Cottura e Reverse Scaling

Nel dettaglio di ogni ricetta risiede il cruscotto di calcolo proporzionale in tempo reale.

![Dettaglio Ricetta con Ricalcolo Dinamico e Cottura](docs/img/screen_crema_detail_open.png)

### 4.1 Controller di Resa e Variazione Peso in Cottura
Il pannello **Ricalcolo Dinamico** unifica la scalatura delle porzioni con la fisica della cottura:
* I pulsanti **[–]** e **[+]** consentono di variare la resa con visualizzazione del moltiplicatore scalare (es. **1.00x**, **2.50x**).
* **Confronto Crudo vs Cotto:** Il box mostra sia il peso totale dell'impasto crudo sommato prima della lavorazione (es. *Impasto crudo: 618 g*), sia il peso del prodotto finito dopo la cottura (es. *Cotto finito: 568.56 g* con calo dell'8%).
* **Resa Cotta a Porzione:** SIMO indica il peso netto stimato di ciascuna singola porzione cotta finita (es. *~71 g a porzione*).

### 4.2 Reverse Scaling dalla Dispensa
Se disponi di una quantità limitata di una materia prima (ad esempio 120 g di burro anziché 175 g):
1. Tocca la pillola con la quantità dell'ingrediente vincolante;
2. Inserisci la quantità effettivamente disponibile e conferma;
3. L'intero albero di componenti e la resa totale si riparametreranno all'istante attorno a quel vincolo.

### 4.3 Sotto-Ricette Gerarchiche a Cascata
Nel *Cornetto alla Crema*, la preparazione richiede la *Crema Pasticcera*. SIMO la tratta come una sotto-ricetta collegata: espandendo la card è possibile visualizzare i componenti interni (latte, tuorli, amido) con le dosi ricalcolate per la quantità di crema necessaria ai cornetti.

---

## 5. Dichiarazione Nutrizionale UE, Allergeni e Food Cost

### 5.1 Scheda Nutrizionale Grafica UE (Regolamento UE 1169/2011)
In fondo alla schermata di dettaglio, sotto la lista degli ingredienti e degli allergeni, compare la nuova **Dichiarazione Nutrizionale Grafica UE**:

![Scheda Nutrizionale Grafica UE](docs/img/screen_nutrition_card.png)

* **Dicitura Ufficiale di Stima:** L'intestazione riporta chiaramente la dicitura *"DICHIARAZIONE NUTRIZIONALE (Stima – Reg. UE n. 1169/2011)"*.
* **Doppia Colonna di Riferimento:** I valori medi sono suddivisi su due colonne: **Per 100 g** e **Per Porzione** (con peso esatto della porzione cotta finita).
* **Protezione Tipografica Monoriga per l'Energia:** La riga dell'energia riporta congiuntamente i valori in **kJ** e in **kcal** (es. *984 kJ / 235 kcal*) formattati per non andare mai a capo o subire troncamenti.
* **I 7 Parametri Standard UE:** Energia, Grassi, Carboidrati, Proteine, Sale equivalente (derivato dal Sodio moltiplicato per 2.5), Sodio e Acqua (umidità residua).
* **Concentrazione Nutrienti con Modificatore Cottura:** I valori per 100 g tengono conto dell'evaporazione d'acqua in cottura: se l'impasto perde il 10% d'acqua, i macronutrienti nei 100 g di prodotto finito risulteranno proporzionalmente concentrati, riflettendo la reale chimica alimentare!

### 5.2 Allergeni Normativi UE e Food Cost al Centesimo
* **14 Allergeni UE:** Evidenziati con chip colorati e badge monoriga protetti (*Allergene #N*), ereditati ricorsivamente da tutte le sotto-ricette.
* **Food Cost in Tempo Reale:** Calcola il costo totale dell'impasto e il costo a porzione/pezzo con equivalenza culinaria 1:1 per densità unitaria ($1\,\text{g} = 1\,\text{ml}$ e $1\,\text{kg} = 1\,\text{l}$), prevenendo distorsioni di prezzo quando i liquidi (es. latte, caffè, panna) vengono dosati in millilitri ma tariffati in litri o chilogrammi.

---

## 6. Cooking Mode e Timer Multipli Integrati

La **Cooking Mode** trasforma SIMO in un cruscotto operativo da banco per la preparazione attiva.

| Cooking Mode a Schermo Attivo | Regolazione Rapida Timer |
|:---:|:---:|
| ![Cooking Mode](docs/img/screen_cooking_opened_success.png) | ![Regolazione Rapida Timer](docs/img/screen_timer_dialog_new.png) |

* **Schermo Sempre Attivo (*Keep Screen On*):** Nessun blackout del display mentre le mani sono sporche o bagnate.
* **Checklist di Pesatura e Passaggi:** Caselle di spunta rapide per segnare gli ingredienti pesati e le fasi procedurali completate.
* **Riconoscimento Automatico dei Timer nel Testo:** Frasi come *"infornare per 35 minuti"* o *"cuocere per 12 minuti"* generano pulsanti interattivi immediati con il conto alla rovescia preimpostato.
* **Resilienza in Background con Foreground Service:** Puoi cambiare app (es. guardare un tutorial su YouTube o rispondere al telefono): i timer continuano a scorrere e suoneranno con allarme acustico e vibrazione allo scadere.

![Timer Interattivi nelle Istruzioni](docs/img/screen_instructions_timers.png)

---

## 7. Lista della Spesa Intelligente

Accessibile dal menu superiore o direttamente dal dettaglio ricetta tramite il tasto verde **Aggiungi a Spesa**:

![Lista della Spesa Intelligente](docs/img/shopping_list_verified.png)

* **Aggregazione Automatica:** Se più ricette utilizzano lo stesso ingrediente con la stessa unità, le quantità vengono unificate in una sola riga (es. $200\,\text{g} + 150\,\text{g} = 350\,\text{g}$ di burro).
* **Stima del Costo Totale della Spesa:** Calcola in tempo reale la spesa economica complessiva convertendo automaticamente i prezzi registrati al chilogrammo o al litro.
* **Gestione Articoli "q.b.":** Gli articoli a dose libera adottano un quantitativo d'acquisto standard (100 g o 1 unità) per agevolare l'approvvigionamento al supermercato.

---

## 8. Assistente Chef AI, Stampa PDF Multipagina e Backup

### 8.1 Assistente Chef AI Bivalente (Gemini & ChatGPT)
SIMO integra nativamente due motori di intelligenza artificiale: **Google Gemini** e **OpenAI ChatGPT**.

![Finestra dell'Assistente AI](docs/img/screen_gemini_assistant_dialog.png)

* **Preservazione Intelligente dell'Unità di Resa:** Quando imposti un'unità personalizzata (es. *torta*, *bicchieri*, *panini*, *fette*, *crostate*), l'AI valuta la coerenza culinaria con la ricetta richiesta:
  * Se l'unità è congruente (es. *torta* per una cheesecake, *bicchieri* per un cocktail o un frappè, *panini* per burger o panini), l'AI **mantiene la tua unità** e calibra le dosi su di essa!
  * Solo se l'unità è palesemente priva di senso gastronomico (es. *bicchieri* per una pizza o un risotto), l'AI torna a *porzioni*.
* **Valori Realistici e Bilanciati:** I prompt di sistema impongono il massimo rigore gastronomico, dosaggi calibrati, prezzi di mercato italiani e valori nutrizionali fedeli.
* **Sicurezza Hardware-Backed AES-256 GCM:** Le tue chiavi API di Google ed OpenAI sono crittografate nel chip hardware sicuro (*Android KeyStore*) del dispositivo e non vengono mai esportate nei backup.

### 8.2 Esportazione PDF Professionale A4 (Minimo 2 Pagine)
Dalla scheda di dettaglio, toccando l'icona **PDF**, SIMO genera un documento A4 stampabile:
* **Minimo 2 Pagine Garantito:**
  * **Pagina 1:** Intestazione SIMO, data, resa e porzioni, tabella delle materie prime scalate, box Food Cost, tabella degli allergeni UE e la dichiarazione nutrizionale UE completa;
  * **Pagina 2 (e successive):** Interamente dedicata al **Procedimento di Preparazione**, stampato con testo in nero pieno nitido ad alto contrasto per la massima leggibilità sul banco di lavoro.
* Condivisione immediata tramite WhatsApp, Telegram, email, Google Drive o stampa diretta Wi-Fi.

### 8.3 Backup e Ripristino JSON con Risoluzione Conflitti
Dalla barra principale puoi esportare l'intero archivio in un file standard `.json` (schema versione 2) contenente tutti i valori nutrizionali, i costi e i modificatori di cottura. Al momento del ripristino su un nuovo telefono, SIMO ti consentirà di risolvere individualmente ogni eventuale conflitto tra ricette nuove ed esistenti.

---

---

# Manuale Tecnico e Architettura di Sistema (Versione 2.0)

# SIMO – Manuale Tecnico e Architettura di Sistema

**Versione Software:** 2.0.0 (Release Ufficiale v2.0)

**Document ID:** `SIMO-ENG-SPEC-2026-V2`  
**Autore:** Francesco Panattoni  
**Target Framework:** Android SDK 24–37 (Kotlin 2.0 / Jetpack Compose Material 3)  

---

## Sommario Esecutivo e Stack Tecnologico

* **Linguaggio & Runtime:** Kotlin 2.0 su JVM Android (API 24–37)

* **Presentation Layer:** Jetpack Compose (Material 3), Unidirectional Data Flow (UDF), StateFlow

* **Domain Layer:** Pure Kotlin Business Logic (`MathEngine`, `CookingTimerExtractor`)

* **Data & Persistence Layer:** Android Room (SQLite ORM, Schema v12, migrazioni atomiche)

* **Hardware Security:** AndroidKeyStore (AES-256 GCM hardware-backed)

* **Cloud AI Engine:** Architettura Dual-Engine (Google Gemini REST v1beta & OpenAI ChatGPT API)

* **Vectorial Export:** Android Vector Graphics `PdfDocument` con paginazione A4 multipagina (≥ 2 pagine)

* **Test Suite:** 59 test unitari automatizzati su JVM

---


## Introduzione


### Problem Statement & Contesto di Dominio

Nei moderni processi di preparazione culinaria professionale e di alta pasticceria, la gestione delle ricette presenta una complessità computazionale e strutturale che eccede ampiamente le capacità di un semplice archivio testuale o tabellare.

I requisiti critici individuati sul campo evidenziano le seguenti problematiche:

    * **Gerarchie Multi-Livello (Composite Recipes):** Le preparazioni complesse non sono insiemi piatti di materie prime, bensì aggregati di preparazioni intermedie (*sotto-ricette*). Ad esempio, un *Cornetto alla Crema* include una pasta sfogliata lievitata e una *Crema Pasticcera*. Qualsiasi variazione della resa o della ricetta base della crema deve propagarsi in modo deterministico e reattivo all'intero albero di composizione;
    * **Inversione di Scala da Vincolo di Dispensa (Reverse Scaling):** In ambiente di laboratorio o domestico, la quantità di resa finale è frequentemente vincolata dalla disponibilità residua di un ingrediente critico (esempio: 120 g di burro anziché 175 g). Il sistema deve essere in grado di calcolare il fattore di scala inverso $k \in R⁺$ rispetto a un generico nodo dell'albero ed estendere la proporzione all'intero grafo;
    * **Tracciabilità Normativa degli Allergeni (Regolamento UE 1169/2011):** L'obbligo di legge impone l'individuazione e la segnalazione dei 14 allergeni normativi. La presenza di un allergene in una foglia del grafo deve essere propagata ricorsivamente fino alla radice;
    * **Food Cost Contabile in Tempo Reale:** Necessità di quantificare all'istante l'incidenza economica di ciascun lotto di produzione e il costo industriale di ogni singola porzione, considerando conversioni tra unità di massa ($g, kg$), di volume ($ml, l$) e unità discrete quantizzate ($pezzi; unit\grave{a}$);
    * **Operatività Senza Soluzione di Continuità (Cooking Mode):** Esigenza di un'interfaccia esecutiva durante la cottura che mantenga il display costantemente attivo, gestisca timer asincroni multipli con allarmi acustici e vibrazione, e supporti aggiunte rapide di tempo non intrusive;
    * **Privacy e Sicurezza delle Chiavi API di Terze Parti:** Protezione crittografica hardware-backed contro attacchi di estrazione locale o perdite accidentali nei backup per le chiavi di accesso a modelli generativi remoti (Google Gemini e OpenAI ChatGPT);
    * **Variazione di Massa Termica in Cottura (Resa Termica & Calo Peso):** Nei trattamenti termici (cottura in forno, padella, bollitura, frittura) gli alimenti subiscono alterazioni fisiche rilevanti della massa: calo peso per evaporazione dell'acqua (es. torte, arrosti $-10\% \div -20\%$) oppure aumento di massa per assorbimento di liquidi idratanti (es. pasta secca e riso $+100\% \div +150\%$). L'architettura deve modellare tale fenomeno termodinamico calcolando la resa cotta finita a porzione e la conseguente variazione di densità dei nutrienti;
    * **Dichiarazione Nutrizionale Ufficiale (Regolamento UE n. 1169/2011):** Necessità di determinare in modo scientifico la concentrazione o diluizione post-cottura dei 6 nutrienti normativi (Energia in kJ/kcal, Grassi, Carboidrati, Proteine, Acqua, Sodio e Sale equivalente), fornendo la doppia rappresentazione analitica *Per 100g* e *Per Porzione* contrassegnata dalla dicitura legale di stima.


### Requisiti Non Funzionali e Vincoli Architetturali

Il sistema è stato ingegnerizzato rispettando i seguenti vincoli prestazionali e architetturali:

    * **Offline-First:** Tutte le funzioni analitiche, di scaling, di navigazione del grafo, di food cost e di cottura guidata devono operare a latenza zero anche in assenza totale di connettività di rete;
    * **Tempo di Calcolo dello Scaling:** Complessità temporale $O(|V| + |E|)$ rispetto al grafo di composizione, con tempo di re-rendering $≤ 16 \text{ms}$ a 60 FPS per evitare frame-drop nell'interfaccia utente dichiarativa;
    * **Assenza di Dipendenze Circolari:** Garanzia formale che il grafo delle ricette rimanga in ogni istante un **Grafo Diretto Aciclico** (**DAG**);
    * **Memory Footprint Controllato:** Nessuna allocazione ridondante di bitmap durante l'esportazione PDF o la gestione dei timer; rilascio immediato delle risorse audio dell'Hardware Abstraction Layer.

% ==============================================================================
% SEZIONE 2: ARCHITETTURA DEL SOFTWARE E DESIGN PATTERN
% ==============================================================================


## Architettura del Software e Pattern Architetturali


### Unidirectional Data Flow (UDF) e State Immutability

Ogni schermata applicativa è funzione pura dello stato corrente (*State-Driven UI*):

$$\text{UI} = f(\text{UiState})$$

I ViewModel non espongono variabili mutabili pubbliche: ogni transizione di stato avviene tramite istanze di data class immutabili incapsulate in `StateFlow<T>`.

```kotlin
val uiState: StateFlow<RecipeListUiState> = combine(
    repository.allRecipes,
    _filterState
) { recipes, filter ->
    // Calcolo filtri, ordinamento e ricerca su pool di background
    val sortedAvailableTags = RecipeTags.getAllSortedTags(recipes.map { it.category })
    val byTags = if (filter.selectedTags.isEmpty()) recipes 
                 else recipes.filter { r -> filter.selectedTags.any { it.equals(r.category, true) } }
    val byFavorites = if (filter.favoritesOnly) byTags.filter { it.isFavorite } else byTags
    val filtered = if (filter.query.isBlank()) byFavorites 
                   else byFavorites.filter { it.name.contains(filter.query, true) }
    
    
    
    val sorted = when (filter.sort) {
        RecipeSortOption.NAME_ASC -> filtered.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
        RecipeSortOption.NAME_DESC -> filtered.sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.name })
        RecipeSortOption.DATE_DESC -> filtered.sortedByDescending { it.createdAt }
        RecipeSortOption.DATE_ASC -> filtered.sortedBy { it.createdAt }
    }
    RecipeListUiState(recipes = sorted, availableTags = sortedAvailableTags, ...)
}.flowOn(Dispatchers.Default)
 .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RecipeListUiState())
```


### Concorrenza e Thread Safety

L'applicazione fa uso sistematico delle Kotlin Coroutines, assegnando rigorosamente i contesti di esecuzione:

    * **`Dispatchers.Main**:` Riservato esclusivamente al rendering dichiarativo di Jetpack Compose e al binding degli eventi UI;
    * **`Dispatchers.Default**:` Dedicato al calcolo analitico intensivo (espansione del DAG, determinazione dei percorsi, ordinamento, estrazione regex dei timer e ricalcolo proporzionale);
    * **`Dispatchers.IO**:` Adibito all'I/O su database Room SQLite, operazioni di crittografia hardware, chiamate REST HTTP di Gemini e stream di scrittura PDF/JSON.


### Lifecycle Optimization: Warm-up Asincrono e Pre-caching all'Avvio

Per prevenire l'anti-pattern del *Flash of Empty State* (in cui la schermata home mostra un layout vuoto per alcune centinaia di millisecondi durante l'interrogazione asincrona iniziale del database), l'architettura implementa una fase di warm-up reattivo. 
Durante lo splash iniziale di avvio, il repository esegue il pre-caricamento immediato della collezione completa delle entità `RecipeWithComponents`. L'emissione iniziale del relativo `StateFlow` avviene in modo concorrente con la composizione della UI, garantendo che al passaggio alla schermata del ricettario l'elenco sia già interamente popolato e ordinato, azzerando qualsiasi percezione di caricamento differito.

% ==============================================================================
% SEZIONE 3: DOMAIN ENGINE - GRAFI E CALCOLO MATEMATICO
% ==============================================================================


## Domain Engine: Teoria dei Grafi e Calcolo Matematico


### Formalizzazione del Ricettario come DAG Pesato

> **Definizione:** 
Sia $G = (V; E; W)$ un grafo orientato pesato finito dove:

    * $V = V_{\text{raw}} \cup V_{\text{recipe}}$ è l'insieme dei vertici, partizionato in materie prime $V_{\text{raw}}$ e preparazioni composte $V_{\text{recipe}}$.
    * $E \subseteq V_{\text{recipe}} \times V$ è l'insieme degli archi orientati $(u; v)$, indicante che la ricetta $u$ richiede l'ingrediente o sotto-ricetta $v$.
    * $W: E \to R⁺ \times \mathcal{U}$ è la funzione peso che associa ad ogni arco la tupla $(q; u)$ rappresentante la quantità base $q$ e l'unità di misura $u \in \mathcal{U}$.

Affinché il calcolo delle dosi converga in tempo finito e non generi paradossi fisici (ad esempio: la Crema Pasticcera che richiede Cornetti che a loro volta richiedono Crema Pasticcera), $G$ deve soddisfare il vincolo fondamentale di aciclicità:

$$\forall v \in V, \quad \nexists \text{ cammino orientato } v \rightsquigarrow v \quad \iff \quad G \text{ è un DAG}$$


### Algoritmo di Rilevamento Cicli Preventivo (`wouldCreateCycle

)}
Prima di consentire l'inserimento o la modifica di un arco $(u; v)$ dove $u$ è la ricetta genitore e $v$ è la ricetta figlia candidata, il sistema valuta se esiste un cammino diretto $v \rightsquigarrow u$.

> **Teorema:** 
Dato un DAG $G = (V; E)$ e un arco candidato $(u; v)$, il nuovo grafo $G' = (V; E \cup \{(u; v)\})$ è un DAG se e solo se $u$ non è raggiungibile da $v$ in $G$.

> *Dimostrazione:* 
Se $u$ fosse raggiungibile da $v$, esisterebbe un cammino orientato $p = (v; w_1; w_2; \dots; w_k; u)$ in $G$. Aggiungendo l'arco $(u; v)$, si formerebbe il ciclo $C = (u; v; w_1; \dots; w_k; u)$, violando la proprietà di DAG. Se $u$ non è raggiungibile da $v$, nessun nuovo cammino originante da $u$ potrà mai ricongiungersi a un suo predecessore, preservando l'aciclicità.

L'implementazione in `MathEngine.kt` impiega una Breadth-First Search (BFS) ottimizzata:

```kotlin
fun wouldCreateCycle(
    parentRecipeId: String,
    candidateChildId: String,
    existingEdges: List<Pair<String, String>>
): Boolean {
    if (parentRecipeId.isBlank() || candidateChildId.isBlank()) return false
    if (parentRecipeId == candidateChildId) return true // Auto-loop immediato

    // Costruzione lista di adiacenza
    val adjacency = mutableMapOf<String, MutableList<String>>()
    for ((parent, child) in existingEdges) {
        if (parent.isNotBlank() && child.isNotBlank()) {
            adjacency.getOrPut(parent) { mutableListOf() }.add(child)
        }
    }

    val queue = ArrayDeque<String>()
    queue.add(candidateChildId)
    val visited = mutableSetOf<String>()

    while (queue.isNotEmpty()) {
        val curr = queue.removeFirst()
        if (curr == parentRecipeId) return true // Ciclo rilevato!
        if (visited.add(curr)) {
            adjacency[curr]?.forEach { next ->
                if (!visited.contains(next)) queue.add(next)
            }
        }
    }
    return false
}
```

**Complessità Computazionale:**

    * **Tempo:** $O(|V| + |E|)$ nel caso peggiore (visita di tutti i vertici e archi raggiungibili).
    * **Spazio:** $O(|V|)$ per l'allocazione della coda e dell'insieme di visitati.


### Algoritmo di Espansione ad Albero Scalato (`buildScaledTree

)}
Dato un nodo radice $R \in V_{\text{recipe}}$ e un target di resa $Y_{\text{target}} \in R⁺$, il fattore di scala primario $k_R$ è calcolato come:

$$k_R = \frac{Y_{\text{target}}}{Y_{\text{base}}(R)}$$

Per ciascun componente $c = (R, i)$ con quantità base $q_{\text{base}}(c)$:

$$q_{\text{scaled}}(c) = q_{\text{base}}(c) \cdot k_R$$

Se il componente $i$ è a sua volta una ricetta $i \in V_{\text{recipe}}$, essa richiede una quantità pari a $q_{\text{scaled}}(c)$ espressa nell'unità di resa della sotto-ricetta. Di conseguenza, il fattore di scala per la sotto-ricetta $k_i$ diventa:

$$k_i = \frac{q_{\text{scaled}}(c)}{Y_{\text{base}}(i)} = \frac{q_{\text{base}}(c) \cdot k_R}{Y_{\text{base}}(i)}$$

Il processo prosegue ricorsivamente fino alle foglie del grafo ($V_{\text{raw}}$).

> **Nota Architetturale:** 
Per garantire la robustezza del runtime Android e prevenire esaurimenti di stack su grafi patologici, l'algoritmo impone una soglia massima di guardia `MAX\_RECURSION\_DEPTH = 25` e mantiene un set di controllo `visitedRecipeIds` ad ogni ramo dell'albero.


### Modello di Reverse Scaling

Il Reverse Scaling consente di calcolare il moltiplicatore $k^*$ a partire da un generico ingrediente $j$ per cui l'operatore dispone di una quantità limite $q_{\text{disp}}(j)$:

$$k^* = \frac{q_{\text{disp}}(j)}{q_{\text{base}}(j)}$$

Tutti gli altri componenti $m \neq j$ e la resa complessiva $Y$ vengono immediatamente riparametrati:

$$Y^* = Y_{\text{base}} \cdot k^*, \qquad \forall m \in \text{Componenti}, \quad q^*(m) = q_{\text{base}}(m) \cdot k^*$$


### Quantizzazione e Sistema delle Unità di Misura

In gastronomia e pasticceria non tutte le grandezze possono essere trattate come numeri reali continui in $R⁺$. L'architettura classifica le unità $\mathcal{U}$ in tre categorie ortogonali:

    * **Unità Continue Ponderali e Volumetriche ($g, kg, ml, l$):** I valori scalati mantengono fino a due cifre decimali se $q < 100$, oppure vengono approssimati al numero intero se $q ≥ 100$ per uniformità con le bilance digitali professionali.
    * **Unità Discrete Quantizzate ($pezzi; unit\grave{a**$):}
    
$$\text{roundIntegerAmount}(q) = \max(1; \lfloor q + 0.5 \rfloor)$$

    Nel caso specifico delle uova intere o dei tuorli, un valore come 2.4 unità non è utilizzabile direttamente senza creare sprechi. La quantizzazione arrotonda all'intero più prossimo salvaguardando il minimo di 1 unità.
    * **Quantificatori Qualitativi ($q.b.$):** Non sono soggetti a moltiplicazione scalare e mantengono costantemente la dicitura letterale ``q.b.''.


### Modello Analitico del Food Cost

Sia $P(i)$ il prezzo per unità di misura associato all'ingrediente $i$. Il costo elementare $C(c)$ del componente scalato $c$ con quantità $q(c)$ e unità $u_r$, dato un prezzo $p$ con unità $u_p$, è formalizzato come:

$$C(c) = q(c) \cdot \mu(u_r; u_p) \cdot p$$

Dove $\mu(u_r, u_p)$ è l'operatore di conversione metrica:

$$
    \mu(u_r; u_p) = 
    \begin{cases}
        1 & \text{se } u_r = u_p \lor (u_r \in \{g, ml\} \land u_p \in \{g, ml\}) \lor (u_r \in \{kg, l\} \land u_p \in \{kg, l\}) \\
        10^{-3} & \text{se } u_r \in \{g, ml\} \land u_p \in \{kg, l\} \\
        10^3 & \text{se } u_r \in \{kg, l\} \land u_p \in \{g, ml\} \\
        \frac{\text{gramsPerUnit}}{1000} & \text{se } u_r \in \{\text{unità, pz}\} \land u_p = kg \\
        \text{gramsPerUnit} & \text{se } u_r \in \{\text{unità, pz}\} \land u_p = g \\
        0 & \text{se } u_r = \text{q.b.}
    \end{cases}
$$

Nel contesto della ricetta, un ingrediente la cui dose è qualificata come *q.b.* (quanto basta) presenta $C(c) = 0$, trattandosi di un dosaggio non ponderale a discrezione dello chef (ad es. un pizzico di sale o spezie) che non deve alterare artificiosamente il costo base del lotto di produzione.

Il costo totale dell'impasto $C_{\text{totale}}$ e il costo unitario per porzione $C_{\text{porzione}}$ si ottengono mediante accumulazione ricorsiva bottom-up:

$$C_{\text{totale}} = \sum_{c \in \text{Foglie}} C(c) + \sum_{s \in \text{Sotto-Ricette}} C_{\text{totale}}(s)$$

$$C_{\text{porzione}} = \frac{C_{\text{totale}}}{Y_{\text{target}}}$$


### Coerenza Metrica tra Food Cost e Carrello della Spesa

L'aggregazione degli ingredienti nella lista della spesa adotta l'identico operatore di conversione metrica $\mu(u_r, u_p)$. In questo modo, un ingrediente con costo espresso al chilogrammo (es. € 1.80/kg) la cui ricetta richiede 250 g produce nel carrello della spesa una stima pari a:

$$\text{Costo}_{\text{spesa}} = 250 \cdot 10^{-3} \cdot 1.80 = 0.45 \text{€}$$

eliminando qualsiasi discrepanza tra il Food Cost stimato nel dettaglio ricetta e la spesa complessiva.

Per gli ingredienti a quantitativo qualitativo indeterminato (*q.b.*), la lista della spesa eredita l'unità di misura associata al prezzo d'acquisto ($u_p$) e adotta una quantità deterministica standard per consentire l'approvvigionamento del formato commerciale minimo:

$$
q_{\text{spesa}} = 
\begin{cases}
    100 & \text{se } u_p \in \{\text{g}, \text{ml}\} \\
    1 & \text{per unità discrete o formati commerciali (pz, unità)}
\end{cases}
$$

fornendo all'utente un punto di partenza facilmente calibrabile direttamente dalla checklist d'acquisto.


### Tipografia e Algoritmo di Line-Breaking Antifrattura

Nella presentazione delle stringhe descrittive complesse degli ingredienti (specialmente denominazioni protette quali *Formaggio Parmigiano Reggiano DOP* o *Olio Extravergine d'Oliva*), l'algoritmo di rendering di Jetpack Compose è stato vincolato per inibire la sillabazione infrasillabica (`Hyphens.None`) e forzare il wrapping esclusivamente sui delimitatori di parola (`LineBreak.Paragraph`). Ciò previene la frammentazione antiestetica dei lemmi merceologici sui display a densità variabile.


### Stabilità Tipografica dei Badge Allergeni e Prevenzione della Compressione Orizzontale

Nella scheda di riepilogo normativo degli allergeni (`RecipeAllergensCard`), ciascuna voce associa la denominazione merceologica (es. *Anidride solforosa e solfiti*) al rispettivo identificatore normativo UE (es. *Allergene \#12*). A causa del comportamento di misurazione unconstrained dei layout `Row` in Compose con allineamento `SpaceBetween`, stringhe particolarmente estese provocavano una contrazione dell'area utile assegnata al badge laterale, inducendo il wrapping anomalo della sequenza (*``Aller'' / ``gene'' / ``\#12''*).

La correzione architetturale applica due vincoli concorrenti di layout:

    * Il componente testuale della denominazione riceve il modificatore flessibile non saturante:
    

```kotlin
Text(
    text = allergen.displayName,
    modifier = Modifier.weight(1f, fill = false)
)
```

    Ciò impone a Compose di misurare prioritariamente il badge laterale alla sua dimensione intrinseca ideale, assegnando alla denominazione lo spazio residuo senza esercitare pressione di restringimento.
    * Il badge numerico è blindato con vincoli di linea singola inderogabili:
    

```kotlin
Text(
    text = "Allergene #${allergen.ordinal + 1}",
    maxLines = 1,
    softWrap = false
)
```

Tale configurazione garantisce che su qualsiasi densità dpi e con qualsivoglia fattore di scala dei caratteri di sistema, il contrassegno normativo si presenti sempre integro su un'unica riga.


### Equivalenza Culinaria Massa-Volume ($1:1$) nel Food Cost e nella Spesa

Nell'esperienza gastronomica quotidiana, molti ingredienti fluidi o semi-densi (acqua, latte, succhi, infusi, brodi, caffè, panna liquida, ecc.) vengono misurati indifferentemente in volume ($ml, l$) o in peso ($g, kg$). L'operatore di conversione $\mu(u_r; u_p)$ integra il principio di **equivalenza culinaria isotropa**:

$$1 \text{g} \equiv 1 \text{ml}, \qquad 1 \text{kg} \equiv 1 \text{l}$$

In questo modo, se un ingrediente viene dosato in millilitri (es. $300 \text{ml}$ di latte o caffè) ma il listino d'acquisto è espresso al chilogrammo (es. € $25.00/\text{kg}$), il costo elementare non genera anomalie di scala né moltiplicazioni improprie:

$$C = 300 \text{ml} \cdot 10^{-3} \cdot 25.00 = 7.50 \text{€}$$

L'aggregatore della lista della spesa applica la medesima equivalenza, garantendo coerenza assoluta tra magazzino, preventivo economico e quantità da acquistare.


### Modello Matematico della Variazione di Massa in Cottura (Resa Termica)

Durante la trasformazione termica, la massa complessiva della preparazione si modifica in funzione del parametro `cookingWeightModifier` ($\Delta M \in [-100, +\infty) $):
> **Definizione:** 
Sia $W_{\text{raw}}$ la massa totale dell'impasto a crudo, calcolata come sommatoria dei pesi di tutti gli ingredienti ponderali ($g, kg$) o convertibili ($ml, l$):

$$W_{\text{raw}} = \sum_{c \in \text{Foglie}} q_{\text{scaled}}(c) \cdot \rho(u_c)$$

dove $\rho(u_c) = 1$ per grammi e millilitri, $\rho(u_c) = 1000$ per chilogrammi e litri, e $\rho(u_c) = \text{gramsPerUnit}$ per unità discrete.
La massa cotta finita $W_{\text{cooked}}$ è definita come:

$$W_{\text{cooked}} = W_{\text{raw}} \cdot ≤ft(1 + \frac{\Delta M}{100}\right)$$

Si distinguono tre regimi termodinamici:

    * **Calo Peso Termico ($\Delta M < 0$):** Evaporazione d'acqua per torte da forno, crostate, panificati e arrosti (es. $\Delta M = -10\% ⟹ W_{\text{cooked}} = 0.90 \cdot W_{\text{raw}}$);
    * **Idratazione e Aumento di Massa ($\Delta M > 0$):** Assorbimento di brodo o acqua di cottura da parte di amidi e cereali secchi (es. pasta secca $\Delta M = +120\% ⟹ W_{\text{cooked}} = 2.20 \cdot W_{\text{raw}}$);
    * **Regime Neutro ($\Delta M = 0$):** Preparazioni fredde, salse crude, cocktail o gelati dove non vi è perdita né guadagno di solvente volatile.

Il peso finito cotto di ciascuna porzione è dato da:

$$w_{\text{portion, cooked}} = \frac{W_{\text{cooked}}}{N_{\text{portions}}}$$


### Modello di Calcolo della Dichiarazione Nutrizionale UE (Regolamento UE n. 1169/2011)

Ciascuna materia prima $i \in V_{\text{raw}}$ possiede un vettore nutrizionale intrinseco a crudo riferito a $100 \text{g}$:

$$\mathbf{Nut}_i = \begin{pmatrix} E_i \\ C_i \\ F_i \\ P_i \\ W_i \\ S_i \end{pmatrix} = \begin{pmatrix} \text{Energia (kcal)} \\ \text{Carboidrati (g)} \\ \text{Grassi (g)} \\ \text{Proteine (g)} \\ \text{Acqua/Umidità (g)} \\ \text{Sodio (mg)} \end{pmatrix}$$

L'Energia in kiloJoule viene calcolata secondo il fattore di conversione standard UE:

$$E_{\text{kJ}} = E_{\text{kcal}} \times 4.184$$

La quantità equivalente di **Sale (g)** è formalizzata dal Regolamento UE 1169/2011 moltiplicando il Sodio (espresso in grammi) per la costante stechiometrica del cloruro di sodio ($2.5$):

$$\text{Sale (g)} = \frac{Sodio_{\text{mg}}}{1000} \times 2.5$$

La quantità assoluta totale di ciascun nutriente nella ricetta prima della cottura è:

$$\mathbf{Nut}_{\text{tot}} = \sum_{c \in \text{Componenti}} \frac{q_{\text{grams}}(c)}{100} \cdot \mathbf{Nut}_c$$


### Algoritmo di Concentrazione e Diluizione Nutrizionale Post-Cottura

> **Teorema:** 
Dato che i macronutrienti (grassi, proteine, carboidrati) e i micronutrienti minerali (sodio/sale) non subiscono evaporazione a temperature di cottura ordinarie ($≤ 250^\circ\text{C}$), la massa assoluta dei nutrienti si conserva: $\mathbf{Nut}_{\text{tot, cooked}} = \mathbf{Nut}_{\text{tot, raw}}$.
Pertanto, la concentrazione di ciascun nutriente per $100 \text{g}$ di prodotto finito cotto è funzione inversa della massa cotta:

$$\mathbf{Nut}_{\text{cooked}}^{100g} = \frac{\mathbf{Nut}_{\text{tot}}}{W_{\text{cooked}}} \times 100 = \frac{\mathbf{Nut}_{\text{tot}}}{W_{\text{raw}} \cdot ≤ft(1 + \frac{\Delta M}{100}\right)} \times 100$$

> *Dimostrazione:* 
Se $\Delta M < 0$ (evaporazione dell'acqua), il denominatore decresce ($W_{\text{cooked}} < W_{\text{raw}}$), determinando un aumento della densità calorica e nutrizionale per 100 grammi (effetto di concentrazione). Viceversa, se $\Delta M > 0$ (assorbimento d'acqua), il denominatore cresce, inducendo una diluizione della densità nutrizionale per 100 grammi. In entrambi i casi, l'apporto nutrizionale **per porzione** rimane invariato rispetto al totale:

$$\mathbf{Nut}_{\text{portion}} = \frac{\mathbf{Nut}_{\text{tot}}}{N_{\text{portions}}}$$


### Regole di Conservazione Gastronomica dell'Unità di Resa

Per prevenire conversioni inappropriate operate dai modelli linguistici (ad es. forzare a ``porzioni'' preparazioni con resa unitaria intrinseca), il motore impone le seguenti regole deterministiche:

    * Se l'unità inserita dall'utente appartiene all'insieme dei descrittori ammissibili per la classe di ricetta (es. *torta, crostata* per categoria Dolci; *bicchieri, calici, tazze* per Caffetteria o Drink; *panini, burger* per la categoria Panini; *teglia, pizza* per Pizze), il generatore AI è vincolato contrattualmente a preservare fedelmente tale unità;
    * Solo in caso di incompatibilità semantica manifesta (es. *bicchieri* per un arrosto di carne o per un risotto), il sistema applica il fallback sicuro al formato generico ``porzioni''.

% ==============================================================================
% SEZIONE 4: STRATO DATI, ROOM DATABASE E PERSISTENZA
% ==============================================================================


## Strato Dati, Room Database e Persistenza Reattiva


### Schema Relazionale e Normalizzazione

Il database relazionale locale è implementato mediante **Android Room** (schema version 12), configurato senza esportazione di schema per ottimizzazione di build KSP.


### Polimorfismo Relazionale degli Item

Per supportare la composizione arbitraria del DAG, l'entità `ItemEntity` è polimorfa:

    * Se `type = "RAW\_INGREDIENT"`, l'elemento rappresenta una materia prima fisica primaria (esempio: *Latte intero*, *Farina 00*).
    * Se `type = "RECIPE"`, l'elemento funge da proxy referenziabile verso una tupla nella tabella `recipes` avente la medesima chiave primaria (`items.id = recipes.id`).


### Prevenzione dei ``Dangling Pointers'' Relazionali

Uno dei punti più delicati dell'architettura riguarda l'eliminazione di una ricetta utilizzata come ingrediente in altre preparazioni.
> **Vincolo di Sicurezza:** 
Se l'utente elimina la ricetta $R$, ma $R$ è utilizzata in $P_1, P_2, \dots, P_n$:

    * La tupla corrispondente viene cancellata dalla tabella `recipes`;
    * I suoi componenti interni come genitore vengono rimossi da `recipe\_components`;
    * **L'entità in `items** non viene eliminata`, bensì convertita atomicamente da `type = "RECIPE"` a `type = "RAW\_INGREDIENT"`.

In questo modo, le ricette genitori $P_1, \dots, P_n$ continuano a funzionare regolarmente considerando $R$ come una materia prima acquistabile/pre-composta, senza generare eccezioni di puntatore nullo o corruzioni nel grafo!

```kotlin
suspend fun deleteRecipe(recipeId: String, convertToRaw: Boolean = false): DeleteResult {
    return try {
        val referencing = recipeDao.getParentRecipeNamesUsingChild(recipeId)
        database.withTransaction {
            recipeDao.deleteComponentsForRecipe(recipeId)
            recipeDao.deleteRecipeById(recipeId)
            if (convertToRaw || referencing.isNotEmpty()) {
                itemDao.updateItemType(recipeId, "RAW_INGREDIENT")
            } else {
                itemDao.deleteItemById(recipeId)
            }
        }
        DeleteResult.Success
    } catch (e: Exception) {
        DeleteResult.Error(e.message ?: "Errore eliminazione")
    }
}
```


### Evoluzione dello Schema e Migrazioni Database

La migrazione incrementale alla versione 10 aggiunge la colonna `allergens` valorizzando retroattivamente le materie prime base, mentre l'evoluzione verso la versione 12 introduce il supporto economico e di costo unitario per gli articoli della spesa:

```kotlin
val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE items ADD COLUMN allergens TEXT NOT NULL DEFAULT ''")
        db.execSQL("UPDATE items SET allergens = 'LATTE' WHERE id = 'ing_latte'")
        db.execSQL("UPDATE items SET allergens = 'UOVA' WHERE id = 'ing_tuorli'")
        db.execSQL("UPDATE items SET allergens = 'GLUTINE' WHERE id = 'ing_farina_manitoba'")
        db.execSQL("UPDATE items SET allergens = 'UOVA,LATTE' WHERE id = 'recipe_crema_pasticcera'")
    }
}

val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Supporto nutrizionale esteso a 6 grandezze sulle materie prime
        db.execSQL("ALTER TABLE items ADD COLUMN caloriesKcal REAL NOT NULL DEFAULT 0.0")
        db.execSQL("ALTER TABLE items ADD COLUMN carbsG REAL NOT NULL DEFAULT 0.0")
        db.execSQL("ALTER TABLE items ADD COLUMN fatsG REAL NOT NULL DEFAULT 0.0")
        db.execSQL("ALTER TABLE items ADD COLUMN proteinsG REAL NOT NULL DEFAULT 0.0")
        db.execSQL("ALTER TABLE items ADD COLUMN waterG REAL NOT NULL DEFAULT 0.0")
        db.execSQL("ALTER TABLE items ADD COLUMN sodiumMg REAL NOT NULL DEFAULT 0.0")

        // Supporto termico e calo peso in cottura sulla ricetta
        db.execSQL("ALTER TABLE recipes ADD COLUMN cookingWeightModifier REAL NOT NULL DEFAULT 0.0")
    }
}
```


### Pattern di Modifica Reattiva: `EditableComponent

}
Nella gestione dello stato di editing delle preparazioni, `RecipeEditViewModel` adotta la struttura immutabile `EditableComponent`:

```kotlin
data class EditableComponent(
    val tempId: String = UUID.randomUUID().toString(),
    val itemId: String,
    val itemName: String,
    val itemType: ItemType,
    val quantity: String,
    val unit: String,
    val pricePerUnit: Double? = null,
    val priceUnit: String? = null,
    val allergens: List<Allergen> = emptyList(),
    val caloriesKcal: Double = 0.0,
    val carbsG: Double = 0.0,
    val fatsG: Double = 0.0,
    val proteinsG: Double = 0.0,
    val waterG: Double = 0.0,
    val sodiumMg: Double = 0.0
)
```

Il metodo atomico `updateComponentDetails(...)` applica una sincronizzazione bidirezionale:

    * Aggiorna in modo immutabile l'elenco dei componenti della ricetta nello stato UI;
    * Se il componente è una materia prima fisica (`itemType != RECIPE`), esegue una coroutine asincrona su `Dispatchers.IO` persistendo nel database Room le modifiche a nome, unità predefinita, costo unitario e allergeni all'interno della tabella `items`.

Ciò assicura che qualunque rettifica apportata dall'utente o dall'assistente AI Gemini persista globalmente nell'anagrafica del ricettario.


### Algoritmo di Riordinamento Reattivo Continuo (Drag & Drop) e Spostamento Discreto

Nel modulo di composizione della ricetta (`RecipeEditScreen`), l'ordinamento sequenziale degli ingredienti risponde a due modalità operative:

    * **Spostamento Discreto a Passo Singolo:** I metodi `moveComponentUp(index)` e `moveComponentDown(index)` operano scambi puntuali tra indici adiacenti su lista immutabile (`toMutableList().apply \{ Collections.swap(...) \`}).
    * **Trascinamento Continuo Multi-Posizione (Fluid Drag & Drop):** L'implementazione convenzionale dei pointer gesture detector in Compose (`detectDragGesturesAfterLongPress`) acquisisce per chiusura lessicale (*closure capture*) l'indice dell'elemento all'avvio del tocco; ciò comportava l'arresto dello scorrimento dopo un singolo scambio.

Per consentire lo scorrimento continuo dall'inizio alla fine dell'elenco senza mai sollevare il dito, è stato implementato un automa dinamico a stato reattivo:

```kotlin
var activeDragId by remember { mutableStateOf<String?>(null) }
var dragOffsetY by remember { mutableFloatStateOf(0f) }
val itemSlotHeightPx = with(LocalDensity.current) { 80.dp.toPx() }

// All'interno di pointerInput(comp.tempId):
detectDragGesturesAfterLongPress(
    onDragStart = { activeDragId = comp.tempId; dragOffsetY = 0f },
    onDrag = { _, dragAmount ->
        dragOffsetY += dragAmount.y
        // Spostamento verso il basso (multi-slot)
        while (dragOffsetY > itemSlotHeightPx) {
            val list = viewModel.uiState.value.components
            val curIdx = list.indexOfFirst { it.tempId == activeDragId }
            if (curIdx != -1 && curIdx < list.size - 1) {
                viewModel.moveComponent(curIdx, curIdx + 1)
                dragOffsetY -= itemSlotHeightPx
            } else break
        }
        // Spostamento verso l'alto (multi-slot)
        while (dragOffsetY < -itemSlotHeightPx) {
            val list = viewModel.uiState.value.components
            val curIdx = list.indexOfFirst { it.tempId == activeDragId }
            if (curIdx != -1 && curIdx > 0) {
                viewModel.moveComponent(curIdx, curIdx - 1)
                dragOffsetY += itemSlotHeightPx
            } else break
        }
    },
    onDragEnd = { activeDragId = null; dragOffsetY = 0f },
    onDragCancel = { activeDragId = null; dragOffsetY = 0f }
)
```

L'algoritmo calcola in tempo reale la posizione corrente dell'`activeDragId` nello StateFlow immutabile, decrementando o incrementando iterativamente l'accumulatore `dragOffsetY` in quanti multipli dell'altezza di slot (`itemSlotHeightPx`). L'esperienza visiva è resa continua tramite accelerazione hardware con `Modifier.graphicsLayer \{ translationY = dragOffsetY; scaleX = 1.02f; scaleY = 1.02f \`} ed elevazione su layer prioritario con `Modifier.zIndex(10f)`.

% ==============================================================================
% SEZIONE 5: SICUREZZA HARDWARE-BACKED E CRITTOGRAFIA
% ==============================================================================


## Sicurezza Hardware-Backed e Crittografia KeyStore


### Architettura del Modulo `CryptoUtils

}
SIMO implementa una soluzione di crittografia hardware-backed a livello TEE (*Trusted Execution Environment*) o StrongBox Keymaster.


### Parametri Crittografici e Modalità Operativa

* **Provider Crittografico:** `AndroidKeyStore`;
    * **Algoritmo Cifrante:** `AES` (Advanced Encryption Standard);
    * **Dimensione Chiave:** 256 bit ($32 \text{byte}$);
    * **Modalità di Blocco:** `GCM` (Galois/Counter Mode);
    * **Padding:** `NoPadding`;
    * **Autenticazione Tag:** 128 bit ($16 \text{byte}$).

> **Vincolo di Sicurezza:** 
Le master key con alias `simo\_gemini\_master\_key` e `simo\_openai\_master\_key` sono generate con il parametro:

`setRandomizedEncryptionRequired(true)`

La chiave non può mai lasciare il chip di sicurezza hardware. Se l'utente esporta un backup o se i dati dell'applicazione vengono clonati su un altro dispositivo, il token cifrato risulterà matematicamente indecifrabile (`AEADBadTagException`), garantendo l'assoluta impossibilità di esfiltrazione delle credenziali.

```kotlin
private fun getOrCreateSecretKey(): SecretKey {
    if (keyStore.containsAlias(KEY_ALIAS)) {
        val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        if (entry != null) return entry.secretKey
    }

    val keyGenerator = KeyGenerator.getInstance(
        KeyProperties.KEY_ALGORITHM_AES,
        ANDROID_KEYSTORE_PROVIDER
    )
    val keyGenSpecBuilder = KeyGenParameterSpec.Builder(
        KEY_ALIAS,
        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
    )
        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
        .setKeySize(256)
        .setRandomizedEncryptionRequired(true)

    keyGenerator.init(keyGenSpecBuilder.build())
    return keyGenerator.generateKey()
}
```

% ==============================================================================
% SEZIONE 6: INTEGRAZIONE CLOUD AI - GOOGLE GEMINI
% ==============================================================================


## Integrazione Cloud AI: Architettura Dual-Engine (Gemini & ChatGPT)


### Architettura Dual-Provider e Protocollo di Rete

Il sottosistema di intelligenza artificiale culinaria adotta un'astrazione modulare bivalente che supporta sia i modelli di **Google Gemini** (`GeminiRecipeService.kt`) sia i modelli di **OpenAI ChatGPT** (`OpenAiRecipeService.kt`):

    * **Google Gemini Endpoint (REST v1beta):**
    
    \small\ttfamily
    POST https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={apiKey}
    
    * **OpenAI ChatGPT Endpoint (Chat Completions v1):**
    
    \small\ttfamily
    POST https://api.openai.com/v1/chat/completions (Header: Authorization Bearer {apiKey})
    

L'utente può alternare a caldo il motore preferito oppure delegare all'automa interno il fallback trasparente nel caso in cui uno dei servizi sia temporaneamente saturo o privo di credito residuo.


### Selezione del Modello (Model Tiering)

Per massimizzare reattività e accuratezza culinaria, l'architettura supporta due modelli con scopi differenziati:

    * **`gemini-flash-lite-latest** (Modello Predefinito):`
    
        * Latenza media: $\sim 800 - 1400 \text{ms}$.
        * Utilizzo: Generazione rapida di piatti tradizionali, ricette standard, compendio ingredienti.
    
    * **`gemini-flash-latest** (Modello Avanzato per Preparazioni Complesse):`
    
        * Finestra di contesto e capacità di reasoning termodinamico e chimico avanzata.
        * Utilizzo: Grandi lievitati (panettoni, colombe), bilanciamento zuccheri e grassi nei gelati, gestione impasti ad alta idratazione ($≥ 80\%$).


### Modalità Operative Non Distruttive dell'Assistente AI

L'interazione con l'Intelligenza Artificiale non si limita alla generazione integrale ex-novo di una ricetta, ma supporta interventi chirurgici non distruttivi volti a preservare il lavoro già svolto dallo chef:

    * **Genera Ricetta Completa:** Crea da zero la lista di ingredienti dosati, costi al chilo/litro, allergeni e procedimento numerato;
    * **Completa Dosi Mancanti (Non Distruttivo):** Analizza l'elenco di ingredienti già inseriti e compila **esclusivamente** le pesature non definite (quantità nulle o pari a zero), i prezzi mancanti, gli allergeni e i 6 parametri nutrizionali, senza alterare gli ingredienti o i dosaggi precedentemente stabiliti;
    * **Calcola Valori Nutrizionali (Preservazione Totale):** Esegue una stima analitica focalizzata esclusivamente sui 6 macronutrienti per 100g per ogni materia prima presente. Preserva intatti pesature, costi, allergeni e modificatore di resa termica;
    * **Genera Solo Procedimento Numerato:** Crea passaggi testuali sequenziali coerenti con le materie prime definite.


### Prompt Engineering & Strutturazione JSON Forzata

Il prompt inviato al modello include vincoli contrattuali rigidi per assicurare una risposta conforme allo schema di parsing interno:

```kotlin
Tu sei un Maestro Pasticcere e Chef Professionista.
Genera una ricetta perfettamente bilanciata rispondendo ESCLUSIVAMENTE con un oggetto JSON valido.
Non aggiungere testo introduttivo, ne' spiegazioni, ne' blocchi di codice markdown.

Formato JSON richiesto:
{
  "name": "Nome ricetta",
  "category": "Dolci / Salati / Pane / Pizze / Primi / Secondi",
  "baseYieldQuantity": 10.0,
  "yieldUnit": "porzioni / cornetti / g / pizza",
  "description": "Breve descrizione",
  "instructions": "1. Primo passaggio...\n2. Secondo passaggio...",
  "components": [
    {
      "name": "Nome ingrediente",
      "quantity": 250.0,
      "unit": "g / ml / unita' / q.b.",
      "allergens": "GLUTINE,LATTE"
    }
  ]
}
```


### Sanitizzazione Output & Motore di Fallback Euristico

La risposta del provider cloud attraversa un parser difensivo:

    * **Detergente Markdown (`stripJsonMarkdown**):` Rimuove prefissi o suffissi del tipo ````json ... ````.
    * **Offline Heuristic Generator:** Se la richiesta fallisce per assenza di connettività, quota API esaurita o formato malformato, si attiva automaticamente il generatore procedurale offline. Esso consulta un dizionario euristico interno per calcolare proporzioni classiche garantendo che l'utente non rimanga mai bloccato.


### UI State Handling, Indicatori Lineari e Layout Scrollabile

Durante l'invocazione asincrona del modello generativo, lo stato è tracciato dal flag booleano `isAiLoading`. Per prevenire anomalie visive ed eccentricità di rendering riscontrabili nei circular spinner su schermi di ridotte dimensioni, l'interfaccia adotta un `LinearProgressIndicator` orizzontale a progressione vettoriale continua centrato all'interno di una card tematica. Inoltre, il contenitore modale integra il modificatore `Modifier.verticalScroll(rememberScrollState())`, garantendo la completa accessibilità dei controlli su qualsiasi diagonale di schermo senza incorrere in clipping o troncamento dei contenuti.

% ==============================================================================
% SEZIONE 7: COOKING MODE, CONCORRENZA E SOTTOSISTEMI HARDWARE
% ==============================================================================


## Cooking Mode, Concorrenza e Sottosistemi Hardware


### Integrazione Window Manager: Flag ``Keep Screen On''

Durante la sessione di preparazione, il display non deve spegnersi per timeout di inattività. L'implementazione evita permessi invasivi (`WAKE\_LOCK`) ed esegue il binding sicuro al ciclo di vita del composable tramite `DisposableEffect`:

```kotlin
val currentView = LocalView.current
DisposableEffect(Unit) {
    currentView.keepScreenOn = true
    onDispose {
        currentView.keepScreenOn = false
    }
}
```


### Motore NLP Deterministico: `CookingTimerExtractor

}
Il componente `CookingTimerExtractor.kt` scansiona le righe delle istruzioni applicando l'espressione regolare pre-compilata:

\small\ttfamily
(?i)(?:per|circa)?\textbackslash s*(\textbackslash d+(?:[.,]\textbackslash d+)?)\dots(ore|ora|h|minuti|min|m|secondi|s)\textbackslash b

L'automa estrae il valore scalare, gestisce virgole e punti decimali, converte il periodo nell'intervallo $t \in [5, 86400]$ secondi e genera il pulsante interattivo associato al passaggio.


### Macchina a Stati dei Timer Concorrenti e Architettura Persistente

Per garantire che il conto alla rovescia non subisca drift temporale o azzeramenti imprevisti quando l'applicazione viene posta in background (ad esempio durante l'ascolto o la visione di video su YouTube, o a fronte della liberazione della memoria di processo da parte dell'OS), la gestione temporale è disaccoppiata dal ciclo di refresh della UI e ancorata a timestamp assoluti di sistema.

I timer attivi sono modellati mediante la data class `ActiveKitchenTimer`:

```kotlin
data class ActiveKitchenTimer(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val totalSeconds: Int,
    val remainingSeconds: Int = totalSeconds,
    val isRunning: Boolean = true,
    val isFinished: Boolean = false,
    val endTimeMillis: Long = System.currentTimeMillis() + (totalSeconds * 1000L),
    val pausedRemainingSeconds: Int? = null
) {
    fun currentRemainingSeconds(): Int {
        if (isFinished) return 0
        return if (!isRunning) {
            pausedRemainingSeconds ?: remainingSeconds
        } else {
            val remaining = ((endTimeMillis - System.currentTimeMillis()) / 1000).toInt()
            Math.max(0, remaining)
        }
    }
}
```

La continuità operativa è presidiata dal singleton architetturale `CookingSessionManager`:

    * **Persistenza dello Stato UI e Ingredienti:** Ogni variazione (ingredienti pesati, passaggi completati, timer aggiunti o rimossi) viene serializzata atomicamente in formato JSON all'interno di `SharedPreferences`. In caso di distruzione della schermata o riavvio forzato da parte del sistema operativo, la sessione viene ricaricata istantaneamente (`hasActiveSession()`), ripristinando fedelmente l'albero scalato e lo stato di avanzamento;
    * **Foreground Service Dedicato (`CookingTimerService**):` Quando sono presenti timer in esecuzione, il sistema avvia un servizio di primo piano con notifica permanente (`startForegroundService`), che preserva il processo applicativo dallo scheduler a bassa priorità del kernel Linux e garantisce la tempestività dell'allarme sonoro anche con display bloccato.


### Interfacciamento con Hardware HAL: Audio & Vibrazione

Al raggiungimento dello zero ($t = 0$), il sistema interroga l'HAL:

    * **Audio:** Generazione deterministica tramite `ToneGenerator(AudioManager.STREAM\_ALARM, 100)`, con emissione ciclica di `TONE\_PROP\_BEEP2`.
    * **Vibrazione Multilivello:** Gestione congiunta di `VibratorManager` (per Android 12+, API $≥ 31$) e fallback su `Vibrator` per le versioni legacy (API $24-30$).


### Componente Modale Non Intrusivo: `QuickAddTimerDialog

}
Il dialogo modale compatto per l'aggiunta di intervalli temporali discreti (**+30s; +1m; +5m; +10m**) opera direttamente sulla lista mutabile dei timer senza ricomporre l'albero di rendering delle istruzioni, garantendo transizioni fluide e tempo di frame $≤ 8 \text{ms}$.


### Gestione Layout Insets e Risoluzione Occlusioni di Sistema

Nei dispositivi moderni con navigazione gestuale a schermo intero (Android 10+, API $≥ 29$), la barra inferiore dei timer (*Floating Multi-Timer Bar*) è potenzialmente soggetta a sovrapposizione con la barra di sistema (*gesture pill*). L'implementazione in `CookingModeScreen.kt` applica il padding dinamico derivato da `WindowInsets.navigationBars`, garantendo che tutti i componenti di controllo rimangano entro l'area utile (*safe area*) dello schermo. Inoltre, i pulsanti di avvio del timer applicano vincoli proporzionali per distribuire uniformemente lo spazio orizzontale su qualsiasi larghezza di display, prevenendo la compressione o l'interruzione di riga non voluta dei testi.

% ==============================================================================
% SEZIONE 8: RENDERING GRAFICO E GENERAZIONE PDF
% ==============================================================================


## Rendering Grafico e Generazione PDF


### Architettura del Modulo `RecipePdfExporter

}
L'esportazione documentale non fa affidamento su librerie esterne pesanti (come iText o Apache PDFBox), bensì sfrutta la pipeline vettoriale nativa del sistema operativo: 
  `android.graphics.pdf.PdfDocument` e la classe `Canvas`.


### Geometria della Pagina e Mappatura Tipografica

Il layout rispetta le specifiche internazionali ISO 216 per il formato **A4** con risoluzione tipografica a 72 DPI:

    * **Larghezza Pagina ($W$):** $595 \text{pt}$ ($210 \text{mm}$);
    * **Altezza Pagina ($H$):** $842 \text{pt}$ ($297 \text{mm}$);
    * **Margini Laterali ($M$):** $40 \text{pt}$ ($14.1 \text{mm}$);
    * **Larghezza Area Utile ($W_{\text{content**}$):} $W - 2M = 515 \text{pt}$.


### Architettura di Paginazione Deterministica ($\ge 2$ Pagine)

Per massimizzare la chiarezza in ambiente professionale e separare rigorosamente la distinta base dal protocollo esecutivo, l'esportatore implementa una suddivisione deterministica in almeno due pagine:

    * **Pagina 1 (Distinta Materie Prime, Food Cost e Nutrizione UE):**
    
        * Intestazione grafica SIMO, data e timestamp di compilazione;
        * Metadati di ricetta: titolo, categoria, porzioni di riferimento e modificatore resa in cottura ($\Delta M\%$);
        * Tabella ingredienti scalati con quantità, unità, costo elementare e badge allergeni;
        * Riquadro contabile Food Cost (Costo totale impasto e Costo unitario a porzione);
        * Tabella grafica conforme al Reg. UE 1169/2011 con dicitura ufficiale di Stima, colonne per 100g e per porzione, energia (kJ/kcal), grassi, carboidrati, proteine, sale equivalente, sodio e acqua.
    
    * **Pagina 2 (Procedimento di Preparazione e Istruzioni Operative):**
    
        * Riservata integralmente ai passaggi numerati di lavorazione;
        * Tipografia in nero pieno ad altissimo contrasto (`Color.BLACK`) con word-wrapping su canvas vettoriale;
        * Gestione del salto pagina automatico in caso di procedimenti straordinariamente articolati.


### Algoritmo di Paginazione e Word-Wrapping Dinamico

Un problema tipico della stampa da mobile è la rottura di riga per passaggi procedurali lunghi. `RecipePdfExporter` implementa un motore di misura del testo:

```kotlin
private fun drawWrappedText(
    canvas: Canvas,
    text: String,
    x: Float,
    startY: Float,
    maxWidth: Float,
    paint: Paint,
    lineSpacing: Float
): Float {
    var currentY = startY
    val words = text.split(" ")
    var currentLine = StringBuilder()

    for (word in words) {
        val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
        val measuredWidth = paint.measureText(testLine)
        if (measuredWidth > maxWidth && currentLine.isNotEmpty()) {
            canvas.drawText(currentLine.toString(), x, currentY, paint)
            currentY += lineSpacing
            currentLine = StringBuilder(word)
        } else {
            currentLine = StringBuilder(testLine)
        }
    }
    if (currentLine.isNotEmpty()) {
        canvas.drawText(currentLine.toString(), x, currentY, paint)
        currentY += lineSpacing
    }
    return currentY
}
```


### Sandboxing e FileProvider Android

Il file compilato viene allocato nella directory di cache privata (`context.cacheDir/pdfs`) e condiviso verso le altre applicazioni mediante un Content URI firmato generato da   `androidx.core.content.FileProvider`, conforme alle policy di sicurezza di Android 11+ (Scoped Storage). 

% ==============================================================================
% SEZIONE 9: BACKUP, SERIALIZZAZIONE E MERGE
% ==============================================================================


## Sottosistema di Backup, Serializzazione e Merge


### Schema Canonico SIMO JSON v2

Il file di esportazione è strutturato per incapsulare atomicamente l'intero grafo del ricettario (Schema versione 2 con nutrizione e calo cottura):

```kotlin
{
  "version": 2,
  "appName": "SIMO",
  "timestamp": 1774028400000,
  "items": [
    {
      "id": "ing_farina_manitoba",
      "name": "Farina Manitoba",
      "type": "RAW_INGREDIENT",
      "defaultUnit": "g",
      "pricePerUnit": 1.60,
      "priceUnit": "kg",
      "allergens": "GLUTINE",
      "caloriesKcal": 364.0,
      "carbsG": 72.0,
      "fatsG": 1.2,
      "proteinsG": 14.5,
      "waterG": 12.0,
      "sodiumMg": 2.0
    }
  ],
  "recipes": [
    {
      "id": "recipe_cornetto_crema",
      "name": "Cornetto alla Crema",
      "baseYieldQuantity": 10.0,
      "yieldUnit": "cornetti",
      "category": "Dolci",
      "cookingWeightModifier": -10.0,
      "instructions": "...",
      "components": [
        {
          "childItemId": "ing_farina_manitoba",
          "baseQuantity": 210.0,
          "unit": "g"
        }
      ]
    }
  ]
}
```


### Algoritmo di Risoluzione Conflitti a Due Fasi

Durante il ripristino (`importBackup`):

	* **Analisi Preventiva:** Il parser analizza il file senza scrivere su disco, confrontando `recipe.id` e `recipe.name` con il database locale per distinguere *Nuove Ricette* e *Conflitti*;
	* **Risoluzione e Commit Atomico:** L'utente sceglie **Sostituisci** (Replace) o **Mantieni** (Keep) per ogni conflitto. Il salvataggio avviene in una transazione SQLite isolata, garantendo l'integrità delle chiavi esterne.

% ==============================================================================
% SEZIONE 10: TESTING, VALIDAZIONE E QUALIT\`A DEL CODICE
% ==============================================================================


## Testing, Validazione e Qualità del Codice


### Pipeline di Distribuzione, Firma Crittografica e Keystore Personale

Per la distribuzione autonoma (*sideloading* e canali diretti) nel rispetto degli standard di sicurezza Android e dei controlli euristici di Google Play Protect, la compilazione del pacchetto APK di rilascio è vincolata a un Keystore crittografico dedicato (`simo-release.jks`):

    * **Identità dello Sviluppatore Certificata:** Il certificato X.509 associato alla chiave RSA a 2048 bit è rilasciato con i metadati d'identità del creatore:
    
    \small\ttfamily
    CN=Francesco Panattoni, OU=App Development, O=SIMO, C=IT
    
    con impronta crittografica SHA-256 univoca memorizzata per la verifica di autenticità;
    * **Firma Ibrida v1, v2 e v3 con `apksigner**:` Il build system Gradle (task `packageRelease`) applica sia la firma JAR classica (v1) sia la firma a blocchi dell'intero file APK (APK Signature Scheme v2 e v3). Ciò impedisce qualsiasi manomissione post-compilazione delle classi DEX o delle risorse binarie compresse;
    * **Trasparenza e Mitigazione Play Protect:** L'adozione di un Keystore personale stabile assicura che gli aggiornamenti mantengano la continuità della catena di fiducia (*signing key rotation continuity*), eliminando falsi allarmi del motore Play Protect e garantendo all'utente finale un'installazione sicura e trasparente.

% ==============================================================================
% SEZIONE 11: CONCLUSIONI E SCALABILIT\`A FUTURA
% ==============================================================================


## Conclusioni e Scalabilità Futura


### Valutazione dei Risultati

L'architettura ingegnerizzata per SIMO ha pienamente soddisfatto gli obiettivi di progetto con la release 2.0:

    * **Aderenza Architetturale:** Completa separazione tra strato di presentazione (Jetpack Compose), modello di dominio matematico (DAG Engine puro) e persistenza (Room SQLite);
    * **Reattività Runtime:** Frame rate stabile a 60 FPS con zero jank percepibile durante il riparametramento continuo delle dosi e l'esecuzione contemporanea di timer multipli;
    * **Conformità e Sicurezza:** Piena aderenza alla normativa UE 1169/2011 per gli allergeni e standard crittografico hardware-backed AES-256 GCM conforme ai requisiti OWASP Mobile Security.


### Miglioramenti Futuri

Questa applicazione può essere ulteriormente migliorata aggiungendo ulteriori feature:

	* **Versione in Inglese:** Aggiunge un'impostazione per la lingua inglese in modo che l'app sia utilizzabile da più persone possibile; 
	* **Integrazione Immagini:** Aggiunta di un supporto visivo per identificare rapidamente le ricette e implementazione dell'Intelligenza Artificiale per riconoscere ed estrarre preparazioni direttamente da una fotografia;
	* **Integrazione Comandi Vocali:** Utilizzando la voce sarà possibile inserire ricette, ma soprattutto interagire durante la Cooking Mode,
	* **Gestione Avanzata della Dispensa:** Filtro ricette realizzabili in tempo reale in base alle scorte disponibili, con modalità "svuota-frigo" assistita da AI per abbattere gli sprechi ed import automatico dagli articoli spuntati nella lista della spesa;
	* **Supporto Esteso a Nuovi Provider AI (Claude & DeepSeek):** Ulteriore arricchimento dell'architettura modulare bivalente per connettere modelli alternativi offline od on-premise;
	* **Integrazione Bilance Bluetooth Low Energy (BLE):** Interfacciamento diretto con le bilance da banco per l'acquisizione automatica del peso dell'ingrediente selezionato in Cooking Mode;
	* **Sincronizzazione Multi-Dispositivo Crittografata:** Estensione del backup verso archivi cloud end-to-end encrypted (E2EE) per la condivisione istantanea tra stazioni di lavoro e cucine multiple.

\end{document}
