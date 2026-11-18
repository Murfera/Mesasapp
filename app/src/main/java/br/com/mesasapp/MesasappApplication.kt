package br.com.mesasapp

import android.app.Application
import br.com.mesasapp.di.AppContainer

class MesasappApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
