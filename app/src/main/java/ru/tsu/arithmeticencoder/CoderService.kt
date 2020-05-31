package ru.tsu.arithmeticencoder

import android.app.IntentService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.io.*


private const val INPUT_TEXT = "inputText"
private const val INPUT_CODE = "inputCode"
private const val PATH = "path"

private const val ACTION_DECOMPRESS = "ACTION_DECOMPRESS"
private const val ACTION_COMPRESS = "ACTION_COMPRESS"

private const val LOG_TAG = "myLogs"

private lateinit var fileToWork: String

private var currentID = -1

private var sendTimes = 0

@kotlin.ExperimentalUnsignedTypes
class CoderService : IntentService("TextFileCoder") {

    override fun onHandleIntent(intent: Intent?) {
        createNotificationChannel()
        sendTimes = 0
        Log.d(LOG_TAG, "on handle")
        when (intent?.action) {
            ACTION_DECOMPRESS -> {
                val path = intent.getStringExtra(PATH)
                onHandleDecompress(path!!)
            }
            ACTION_COMPRESS -> {
                val path = intent.getStringExtra(PATH)
                onHandleCompress(path!!)
            }
        }
    }

    private fun onHandleCompress(path: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent, 0)

        builder = NotificationCompat.Builder(this, "my_notes")
            .setSmallIcon(R.drawable.ic_notification_24dp)
            .setContentTitle("file compressing")
            .setContentText(path)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        fileToWork = "${File(path).parent}/${File(path).name.replace(".txt", "_coded.ae")}"
        val inputText = readFileBin(path)
        Coder.compress(inputText)

        Log.d(LOG_TAG, "end of compressing")
    }

    private fun onHandleDecompress(path: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent =
            PendingIntent.getActivity(this, 0, intent, 0)

        builder = NotificationCompat.Builder(this, "CoderChannel")
            .setSmallIcon(R.drawable.ic_notification_24dp)
            .setContentTitle("file decompressing")
            .setContentText(path)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        fileToWork = "${File(path).parent}/${File(path).name.replace(".ae", "_decoded.txt")}"
        val inputText = readFileBin(path)

        println()
        println("decoder service")
        for(item in inputText){
            for( i in 1..8){
                print("${(item.toInt() shr (8-i)) and 1}")
            }
        }
        println()
        Decoder.decompress(inputText)

        Log.d(LOG_TAG, "end of compressing")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "my notifications"
            val descriptionText = "test notifications"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("CoderChannel", name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }


    private fun readFileBin(fileToRead: String): ByteArray {
        var ba = byteArrayOf()
        try {
            //открываем поток для чтения
            val stream = DataInputStream(FileInputStream(File(fileToRead)))
            ba = stream.readBytes()
            stream.close()
        } catch (e: FileNotFoundException) {
            Log.d(LOG_TAG, "not found")
            e.printStackTrace()
        } catch (e: IOException) {
            Log.d(LOG_TAG, "IOE")
            e.printStackTrace()
        }
        return ba
    }

    companion object {
        lateinit var builder: NotificationCompat.Builder

        @JvmStatic
        fun startDecompressing(context: Context, path: String, inputCode: ByteArray) {
            val intent = Intent(context, CoderService::class.java).apply {
                action = ACTION_DECOMPRESS
                putExtra(INPUT_CODE, inputCode)
                putExtra(PATH, path)
            }
            context.startService(intent)
        }

        @JvmStatic
        fun startCompressing(context: Context, path: String, inputText: ByteArray) {
            Log.d(LOG_TAG, "comp start")
            val intent = Intent(context, CoderService::class.java).apply {
                action = ACTION_COMPRESS
                putExtra(INPUT_TEXT, inputText)
                putExtra(PATH, path)
            }
            context.startService(intent)
        }

        private fun writeBinFile(path: String, data: ByteArray) {
            try {
                val f = File(path)
                f.createNewFile()
                val stream = DataOutputStream(
                    FileOutputStream(f, false)
                )
                stream.write(data)
                stream.close()
                Log.d(LOG_TAG, "Файл записан $path")
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        private fun appendBinFile(path: String, data: ByteArray) {
            try {
                val f = File(path)
                f.createNewFile()
                val stream = DataOutputStream(
                    FileOutputStream(f, true)
                )
                stream.write(data)
                stream.close()
                Log.d(LOG_TAG, "Файл обновлен $path")
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        fun updateInfo(toPrint: ByteArray, passed: Int, all: Int) {
            println()
            println("coder service")
            for(item in toPrint){
                for( i in 1..8){
                    print("${(item.toInt() shr (8-i)) and 1}")
                }
            }
            println()
            if (sendTimes == 0) {
                writeBinFile(fileToWork, toPrint)
            } else {
                appendBinFile(fileToWork, toPrint)
                with(NotificationManagerCompat.from(CoderService.builder.mContext)) {
                    CoderService.builder.setProgress(all, passed, false)
                    notify(currentID, CoderService.builder.build())
                }
            }
            sendTimes++
        }

        fun createNotification(all: Int) {
            Log.d(LOG_TAG, "createNotification $all" )
            currentID++
            with(NotificationManagerCompat.from(CoderService.builder.mContext)) {
                CoderService.builder.setProgress(all, 0, false)
                notify(currentID, CoderService.builder.build())
            }
        }

        fun sendLastDecodeNotification() {
            with(NotificationManagerCompat.from(CoderService.builder.mContext)) {
                builder.setProgress(0, 0, false)
                builder.setContentTitle("decompressing finished")
                notify(currentID, CoderService.builder.build())
            }
        }

        fun sendLastCodeNotification(ratio: Double) {
            Log.d(LOG_TAG, "last coder notification")
            with(NotificationManagerCompat.from(CoderService.builder.mContext)) {
                builder.setProgress(0, 0, false)
                builder.setContentTitle("compressing finished. ratio : $ratio")
                notify(currentID, CoderService.builder.build())
            }

        }

        fun sendHeader(header: ByteArray) {
            sendTimes++
            writeBinFile(fileToWork, header)
        }
    }

}
