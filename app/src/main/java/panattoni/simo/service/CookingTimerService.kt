package panattoni.simo.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import panattoni.simo.MainActivity
import panattoni.simo.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Foreground Service per il conto alla rovescia continuo dei timer da cucina in background.
 * Mantiene l'app viva contro il Low Memory Killer (LMK) di Android anche mentre l'utente
 * usa YouTube o altre app pesanti, suonando l'allarme tempestivamente al termine del tempo.
 */
class CookingTimerService : Service(){

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var tickerJob: Job? = null
    private var alarmTone: ToneGenerator? = null
    private var isAlarmRinging = false

    companion object{
        const val CHANNEL_ID = "simo_cooking_timers_channel"
        const val NOTIFICATION_ID = 8881
        const val ACTION_STOP_ALARM = "panattoni.simo.ACTION_STOP_ALARM"

        fun start(context: Context){
            try{
                val intent = Intent(context, CookingTimerService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                    context.startForegroundService(intent)
                } else{
                    context.startService(intent)
                }
            } catch (_: Exception){}
        }

        fun stop(context: Context){
            try{
                val intent = Intent(context, CookingTimerService::class.java)
                context.stopService(intent)
            } catch (_: Exception){}
        }

        fun stopAlarmSound(context: Context){
            try{
                val intent = Intent(context, CookingTimerService::class.java).apply{
                    action = ACTION_STOP_ALARM
                }
                context.startService(intent)
            } catch (_: Exception){}
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate(){
        super.onCreate()
        createNotificationChannel()
        startForegroundServiceNotification("Cooking Mode attiva", "Timer in attesa...")
        startTickerLoop()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int{
        if (intent?.action == ACTION_STOP_ALARM){
            stopAlarm()
        }
        return START_STICKY
    }

    private fun createNotificationChannel(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Timer Cucina SIMO",
                NotificationManager.IMPORTANCE_HIGH
            ).apply{
                description = "Notifiche attive per i timer in corso e allarme di cottura"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                enableVibration(true)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun startForegroundServiceNotification(title: String, content: String){
        val notification = buildNotification(title, content)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE){
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else{
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(title: String, content: String): Notification{
        val openAppIntent = Intent(this, MainActivity::class.java).apply{
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_COOKING_MODE", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val iconRes = android.R.drawable.ic_lock_idle_alarm

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(iconRes)
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSilent(true)
            .build()
    }

    private fun updateNotification(title: String, content: String){
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(title, content))
    }

    private fun startTickerLoop(){
        tickerJob?.cancel()
        tickerJob = serviceScope.launch{
            while (isActive){
                delay(1000L)
                val justFinished = CookingSessionManager.tick(applicationContext)
                val session = CookingSessionManager.sessionState.value

                if (session == null){
                    stopSelf()
                    break
                }

                val runningTimers = session.activeTimers.filter{ it.isRunning && !it.isFinished }
                val finishedTimers = session.activeTimers.filter{ it.isFinished }

                if (justFinished || finishedTimers.isNotEmpty()){
                    if (!isAlarmRinging){
                        startAlarm()
                    }
                    val names = finishedTimers.joinToString{ it.label }
                    updateNotification("🔔 Timer completato!", "È pronto: $names")
                } else if (runningTimers.isNotEmpty()){
                    // Trova il timer con il tempo rimanente minore per mostrarlo nella notifica
                    val nextTimer = runningTimers.minByOrNull{ it.currentRemainingSeconds() }
                    if (nextTimer != null){
                        val rem = nextTimer.currentRemainingSeconds()
                        val m = rem / 60
                        val s = rem % 60
                        val formattedTime = String.format("%02d:%02d", m, s)
                        updateNotification(
                            "⏱️ SIMO - Cooking Mode",
                            "${nextTimer.label}: $formattedTime rimanenti"
                        )
                    }
                } else{
                    // Nessun timer in corso e nessun timer che squilla: ferma il service per risparmiare batteria
                    stopAlarm()
                    stopSelf()
                    break
                }
            }
        }
    }

    private fun startAlarm(){
        isAlarmRinging = true
        serviceScope.launch{
            try{
                alarmTone = ToneGenerator(AudioManager.STREAM_ALARM, 100)
                while (isActive && isAlarmRinging){
                    try{
                        alarmTone?.startTone(ToneGenerator.TONE_PROP_BEEP2, 900)
                    } catch (_: Exception){}
                    vibrate()
                    delay(1500L)
                }
            } catch (_: Exception){
            } finally{
                stopAlarm()
            }
        }
    }

    private fun stopAlarm(){
        isAlarmRinging = false
        try{
            alarmTone?.stopTone()
            alarmTone?.release()
            alarmTone = null
        } catch (_: Exception){}
    }

    private fun vibrate(){
        try{
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
                val manager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
            } else{
                @Suppress("DEPRECATION")
                val v = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                    v?.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
                } else{
                    @Suppress("DEPRECATION")
                    v?.vibrate(500)
                }
            }
        } catch (_: Exception){}
    }

    override fun onDestroy(){
        super.onDestroy()
        stopAlarm()
        tickerJob?.cancel()
        serviceScope.cancel()
    }
}
