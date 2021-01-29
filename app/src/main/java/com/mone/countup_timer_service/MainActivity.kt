package com.mone.countup_timer_service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.mone.countup_timer_service.CountUpTimerService.MyBinder
import com.mone.countup_timer_service.databinding.ActivityMainBinding
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

typealias Color = R.color

class MainActivity : FragmentActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var timerService: CountUpTimerService
    var isBound = MutableLiveData<Boolean>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        isBound.postValue(false)


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

    private fun startTimerService() {
        val intent = Intent(this@MainActivity, CountUpTimerService::class.java)
        startService(intent)
        bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun stopTimerService() {
        if (isBound.value!!) {
            unbindService(mServiceConnection)
            isBound.postValue(false)
        }

        val intent = Intent(this@MainActivity, CountUpTimerService::class.java)
        stopService(intent)
    }

    private fun updateUI(isStart: Boolean) {
        if (isStart) {

            timerService.timeFlow.onEach { time ->
                binding.timer.text = time
            }.launchIn(lifecycleScope)

            binding.btnStartStop.setBackgroundColor(resources.getColor(Color.red))
            binding.btnStartStop.text = "Stop"
        } else {
            binding.btnStartStop.setBackgroundColor(resources.getColor(Color.green))
            binding.btnStartStop.text = "Start"
        }
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
            isBound.postValue(false)
        }

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val myBinder = service as MyBinder
            timerService = myBinder.service
            isBound.postValue(true)
        }
    }
}