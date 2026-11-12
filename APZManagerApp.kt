package com.apz.manager

import android.app.Application
import com.apz.manager.data.AppRepository
import com.apz.manager.ui.AppViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module

class APZManagerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val appModule = module {
            single { AppRepository(androidContext()) }
            viewModel { AppViewModel(get()) }
        }
        startKoin {
            androidLogger()
            androidContext(this@APZManagerApp)
            modules(appModule)
        }
    }
}
