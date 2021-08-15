package com.udacity

import android.animation.ObjectAnimator
import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.coroutines.InternalCoroutinesApi

private val NOTIFICATION_ID = 0

class MainActivity : AppCompatActivity() {

    private var downloadID: Long = 0

    private lateinit var notificationManager: NotificationManager
    private lateinit var pendingIntent: PendingIntent
    //for actions like snooze
    private lateinit var action: NotificationCompat.Action
    private lateinit var viewModel : ViewModel
    private var fileName = ""
    private var fileStatus : Int? = 0
    private var status = ""

    /*private var i = 0
    private val handler = Handler(Looper.getMainLooper()).postDelayed({
        // Your Code
        progressBar!!.progress = i
    }, 3000)

    *//*    val binding: ContentMainBinding = DataBindingUtil.inflate(
        layoutInflater, R.layout.content_main, C, false)*//*


    private var progressBar: ProgressBar? = null*/


    @InternalCoroutinesApi
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        custom_button.setState(ButtonState.Clicked)

        registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        custom_button.setOnClickListener {

                if (viewModel.currentUrl.value != null)
                {
                    download()
                    //Log.i("mainStateBefore", custom_button.getState())
                    //set state
                    //custom_button.setMyButtonState(ButtonState.Clicked)
                    custom_button.setState(ButtonState.Loading)
                    //Log.i("mainStateAfter",custom_button.getState())
                }
                else
                {
                    Toast.makeText(applicationContext, "Select downloadable item", Toast.LENGTH_SHORT).show()
                }
        }
         notificationManager = this.getSystemService(
            NotificationManager::class.java
        ) as NotificationManager

    createChannel(
        //since the channel and send notification uses the same channel_id, the notification will be passed through said channel
        getString(R.string.notification_id),
        getString(R.string.notification_name))

        viewModel = ViewModelProvider(this).get(ViewModel::class.java)
    }
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            //"EXTRA_DOWNLOAD_ID" - Intent extra included with ACTION_DOWNLOAD_COMPLETE intents, indicating the ID
            // (as a long) of the download that just completed.
            val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)

            //from: https://stackoverflow.com/questions/58083140/how-to-download-file-using-downloadmanager-in-api-29-or-android-q
            val downloadManager =
                getSystemService(DOWNLOAD_SERVICE) as DownloadManager

            //finds the file downloaded based on the files "id"; returned object is of type "DownloadManager.Query"
            val fileDownloaded = id?.let { DownloadManager.Query().setFilterById(it) }
            //The Cursor interface provides random read-write access to the result set returned by a database query.
            val cursor: Cursor = downloadManager.query(fileDownloaded)


            //Move the cursor to the first row. This method will return false if the cursor is empty.
            if (cursor.moveToFirst()) {
                //getInt - Returns the value of the requested column as an int.
                //getColumnIndex - Returns the zero-based index for the given column name, or -1 if the column doesn't exist.
                //overall, the line below is saying to get the integer value of the column that contains the download status
                //so it looks like each column just has one row since we are always returning the zero-based index
                fileStatus = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
            }

            when (fileStatus) {
                DownloadManager.STATUS_SUCCESSFUL -> { //"DownloadManager.STATUS_SUCCESSFUL" has a constant value of 8
                    status = "Success"
                }
                DownloadManager.STATUS_FAILED -> {
                    status = "Fail"
                }
            }

            if (context != null) {
                notificationManager.sendNotification(
                    context.getText(R.string.app_description).toString(),
                    context, fileName, status
                )
            }
            custom_button.setState(ButtonState.Completed)
        }
    }


    fun onRadioButtonClicked(view: View) {
        if (view is RadioButton) {
            // Is the button now checked?
            val checked = view.isChecked

            // Check which radio button was clicked
            when (view.getId()) {
                R.id.appLoad ->
                    if (checked) {
                        viewModel.setUrl(AppUrl)
                        fileName = getString(R.string.App)
                    }
                R.id.glide ->
                    if (checked) {
                        viewModel.setUrl(GlideUrl)
                        fileName = getString(R.string.Glide)
                    }
                R.id.retrofit ->
                    if (checked)
                    {
                        viewModel.setUrl(RetrofitUrl)
                        fileName = getString(R.string.Retrofit)
                    }
            }
        }
    }


    //when to call this function, in a broadcast class?

        @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannel(channelId: String, channelName: String) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            )

            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.RED
            notificationChannel.enableVibration(true)
            notificationChannel.description = "Download complete"

            notificationManager.createNotificationChannel(notificationChannel)
        }
    }
//The download manager is a system service that handles long-running HTTP downloads.
//Clients may request that a URI be downloaded to a particular destination file.
    private fun download() {
        val request =
            DownloadManager.Request(Uri.parse(viewModel.currentUrl.value))
                .setTitle(getString(R.string.app_name))
                .setDescription(getString(R.string.app_description))
                .setRequiresCharging(false)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

        val downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        //an ID for the download, unique across the system. This ID is used to make future calls related to this download.
        downloadID =
            downloadManager.enqueue(request)// enqueue puts the download request in the queue.
    }

    companion object {
        private const val AppUrl =
            "https://github.com/udacity/nd940-c3-advanced-android-programming-project-starter/archive/master.zip"

        private const val GlideUrl = "https://github.com/bumptech/glide/archive/refs/heads/master.zip"

        private const val RetrofitUrl = "https://github.com/square/retrofit/archive/refs/heads/master.zip"

        private const val CHANNEL_ID = "channelId"
    }
    //Note: Each scope function adds context to the one that already exists (context of our class or outer function).
//So since we want to use this extension function outside of our MainActivity class, we define the extension function inside of
//said class

    fun NotificationManager.sendNotification(messageBody: String, applicationContext: Context, fileName : String, fileStatus : String) {

        //note that the Intent is created inside the function (as opposed to BroadcastReceiver class)
        val detailIntent = Intent(applicationContext, DetailActivity::class.java)
        detailIntent.putExtra("fileName",fileName)
        detailIntent.putExtra("fileStatus",fileStatus)

        pendingIntent = PendingIntent.getActivity(
            applicationContext,
            NOTIFICATION_ID,
            detailIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        action = NotificationCompat.Action(null,"check",pendingIntent)


        val snoozeIntent = Intent(applicationContext, SnoozeReceiver::class.java)
        val REQUEST_CODE = 1
        val FLAGS = 1

        val snoozePendingIntent: PendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            REQUEST_CODE,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        val snoozeAction = NotificationCompat.Action(null,"snooze",snoozePendingIntent)

        val builder = NotificationCompat.Builder(
            applicationContext,
            applicationContext.getString(R.string.notification_id)
        )
            .setSmallIcon(R.drawable.ic_assistant_black_24dp)
            .setContentTitle(applicationContext
                .getString(R.string.notification_title)) //R
            .setContentIntent(pendingIntent)
            .addAction(action)
            .addAction(snoozeAction)
            .setAutoCancel(true)
            .setContentText(messageBody)
            .build()

        notify(NOTIFICATION_ID, builder)
    }

    fun NotificationManager.cancelNotifications() {
        cancelAll()
    }
}
