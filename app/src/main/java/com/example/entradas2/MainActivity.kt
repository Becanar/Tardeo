package com.example.entradas2

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.webchecker.WebCheckerWorker
import java.util.concurrent.TimeUnit
import java.util.UUID
/**
 * MainActivity es la actividad principal de la aplicación.
 * Esta actividad se encarga de configurar la interfaz de usuario,
 * gestionar permisos de notificación y controlar el trabajo de verificación web
 * con el uso de WorkManager.
 */
class MainActivity : AppCompatActivity() {

    private val workTag = "WebCheckerWork"
    private var workId = UUID.randomUUID()
    private var semaforo = "R"

    /**
     * Método de inicio de la actividad donde se configura la interfaz,
     * se gestionan los permisos y se inicializan los elementos de la UI.
     *
     * @param savedInstanceState El estado guardado de la actividad (si existe)
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }

        val urlField = findViewById<EditText>(R.id.url)
        val wordField = findViewById<EditText>(R.id.palabra)
        val btnPlay = findViewById<Button>(R.id.btnPlay)
        val btnStop = findViewById<Button>(R.id.btnStop)
        val spinner = findViewById<Spinner>(R.id.spinnerFrequency)

        val options = arrayOf("5 minutos", "10 minutos", "30 minutos", "1 hora")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, options)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        val sharedPreferences = getSharedPreferences("WebCheckerPrefs", MODE_PRIVATE)

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            /**
             * Método llamado cuando se selecciona una opción en el Spinner.
             * Guarda el intervalo seleccionado en las preferencias.
             */
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedInterval = when (position) {
                    0 -> 5L
                    1 -> 10L
                    2 -> 30L
                    else -> 60L
                }
                sharedPreferences.edit().putLong("interval", selectedInterval).apply()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        btnPlay.setOnClickListener {
            semaforo = "V"
            sharedPreferences.edit().putString("semaforo", semaforo).apply()

            WorkManager.getInstance(this).cancelAllWorkByTag(workTag)

            val interval = sharedPreferences.getLong("interval", 15L) // Default 15 minutos
            val workRequest = PeriodicWorkRequestBuilder<WebCheckerWorker>(
                interval, TimeUnit.MINUTES
            ).addTag(workTag).build()

            WorkManager.getInstance(this).enqueue(workRequest)
            workId = workRequest.id
        }

        btnStop.setOnClickListener {
            semaforo = "R"
            sharedPreferences.edit().putString("semaforo", semaforo).apply()
            stopWork()
        }

    }
    /**
     * Detiene el trabajo en curso y limpia cualquier trabajo pendiente.
     */
    private fun stopWork() {
        WorkManager.getInstance(this).cancelWorkById(workId)
        WorkManager.getInstance(this).pruneWork()
    }
}

