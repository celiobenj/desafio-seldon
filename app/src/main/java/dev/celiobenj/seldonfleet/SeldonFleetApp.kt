package dev.celiobenj.seldonfleet

import android.app.Application

/** Application do app: cria o [AppContainer] uma única vez, no início do processo. */
class SeldonFleetApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
