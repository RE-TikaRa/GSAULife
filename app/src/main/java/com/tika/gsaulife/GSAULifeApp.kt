package com.tika.gsaulife

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.tika.gsaulife.card.CardFeature

class GSAULifeApp : Application(), DefaultLifecycleObserver {
    override fun onCreate() {
        super<Application>.onCreate()
        AppearanceManager.apply(AppearanceManager.getMode(this))
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        CardFeature.restoreContinuousRefresh(this)
    }
}
