package com.jeepchief.photomemorial

import android.app.Application
import com.kakao.sdk.common.KakaoSdk

class PmApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Kakao SDK 초기화
        KakaoSdk.init(this, "811a46bfeb6a6941e7a6342a780d23a5")
    }
}