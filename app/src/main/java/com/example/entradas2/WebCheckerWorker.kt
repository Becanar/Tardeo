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

/**
 * WebCheckerWorker es una clase que extiende Worker para realizar tareas en segundo plano.
 * En este caso, se utiliza para comprobar si una palabra específica se encuentra en una página web
 * y, si es así, enviar una notificación al usuario.
 *
 * @param appContext Contexto de la aplicación
 * @param workerParams Parámetros de trabajo proporcionados por el sistema
 */
class WebCheckerWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {
    /**
     * Realiza la tarea de verificar si la palabra buscada está presente en el contenido de la página web.
     * Si la palabra se encuentra, envía una notificación al usuario.
     * Si ocurre algún error o la tarea es detenida, el resultado es un fallo.
     *
     * @return El resultado de la tarea, puede ser éxito o fallo
     */
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
    /**
     * Envía una notificación al usuario indicando que la palabra ha sido encontrada en la página web.
     */
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
