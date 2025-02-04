package com.example.webchecker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.jsoup.Jsoup

class WebCheckerWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        try {
            val sharedPreferences = applicationContext.getSharedPreferences("WebCheckerPrefs", Context.MODE_PRIVATE)

            val semaforo = sharedPreferences.getString("semaforo", null) ?: return Result.failure()
            if (isStopped || semaforo == "R") return Result.failure()

            val url = sharedPreferences.getString("url", null) ?: return Result.failure()
            val word = sharedPreferences.getString("word", null) ?: return Result.failure()

            val doc = Jsoup.connect(url).get()
            if (isStopped || semaforo == "R") return Result.failure()

            if (doc.text().contains(word, ignoreCase = true)) {
                sendNotification()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            return Result.failure()
        }

        return Result.success()
    }

    private fun sendNotification() {
        val context = applicationContext

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "guestlist_channel",
                "Guestlist Notification",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Canal para notificaciones de palabras encontradas"
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(context, "guestlist_channel")
            .setContentTitle("¡Se encontró la palabra!")
            .setContentText("La palabra fue encontrada en la página web.")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1, notification)
    }
}
