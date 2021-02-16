package com.mone.countup_timer_service

import android.content.*
import android.os.Bundle
import android.os.IBinder
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.mone.countup_timer_service.CountUpTimerService.TimerBinder
import com.mone.countup_timer_service.databinding.ActivityMainBinding
import com.mone.countup_timer_service.utils.Constants
import com.mone.countup_timer_service.utils.isServiceRunningInForeground
import kotlinx.coroutines.launch


typealias Color = R.color

class MainActivity : FragmentActivity() {

    private lateinit var binding: ActivityMainBinding
    private val intentToService by lazy {
        Intent(this@MainActivity, CountUpTimerService::class.java)
    }
    private lateinit var timerService: CountUpTimerService
    private var isBound = MutableLiveData(false)
    private val receiver: TimerStatusReceiver by lazy {
        TimerStatusReceiver()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        isBound.postValue(
            isServiceRunningInForeground(CountUpTimerService::class.java)
        )

        isBound.observe(this) { isActive ->
            lifecycleScope.launch {
                updateUI(isActive)
            }
        }


        binding.btnStartStop.setOnClickListener {
            if (isBound.value!!)
                stopTimerService()
            else
                startTimerService()
        }
    }

    override fun onResume() {
        super.onResume()
        isBound.postValue(isServiceRunningInForeground(CountUpTimerService::class.java))
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(receiver, IntentFilter(Constants.ACTION_TIME_KEY))
    }

    private fun startTimerService() {
        startService(intentToService)
        bindService(intentToService, mServiceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun stopTimerService() {
        if (isBound.value!!) {
            unbindService(mServiceConnection)
            isBound.postValue(false)
        }
        stopService(intentToService)
    }

    private fun updateUI(isStart: Boolean) {
        if (isStart) {
            // when the activity going to be Destroyed, the service will be Unbind from activity,
            // But is still running in foreground. So when you start the app again, you should
            // bind the activity to service again.
            bindService(intentToService, mServiceConnection, Context.BIND_AUTO_CREATE)

            /*timerService.timeFlow.onEach { time ->
                binding.timer.text = time
            }.launchIn(lifecycleScope)*/

            binding.btnStartStop.setBackgroundColor(resources.getColor(Color.red))
            binding.btnStartStop.text = getString(R.string.btn_stop)
        } else {
            binding.btnStartStop.setBackgroundColor(resources.getColor(Color.green))
            binding.btnStartStop.text = getString(R.string.btn_start)
        }
    }


    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
    }

    override fun onStop() {
        super.onStop()
        if (isBound.value!!) {
            unbindService(mServiceConnection)
            isBound.postValue(false)
        }
    }

    private val mServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName) {
            //isBound.postValue(false)
        }

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val myBinder = service as TimerBinder
            timerService = myBinder.service
            isBound.postValue(true)
        }
    }

    /**
     * used to get events from foreground service
     */
    inner class TimerStatusReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Constants.ACTION_TIME_KEY -> {
                    if (intent.hasExtra(Constants.ACTION_TIME_VALUE)) {
                        val intentExtra = intent.getStringExtra(Constants.ACTION_TIME_VALUE)

                        if (intentExtra == Constants.ACTION_TIMER_STOP) {
                            stopTimerService()
                        } else {
                            binding.timer.text = intent.getStringExtra(Constants.ACTION_TIME_VALUE)
                        }
                    }
                }
            }
        }
    }
}