package com.jeepchief.photomemorial.view

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import com.jeepchief.photomemorial.R
import com.jeepchief.photomemorial.databinding.ActivityMainBinding
import com.naver.maps.map.NaverMapSdk

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // init naver maps
        NaverMapSdk.getInstance(this).client = NaverMapSdk.NaverCloudPlatformClient("kd3ptmxe5c")


        binding.apply {
            
        }
    }
}