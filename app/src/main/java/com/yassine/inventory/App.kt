package com.yassine.inventory

import android.app.Application
import android.content.Context
import android.content.res.Configuration

class App : Application() {
    override fun attachBaseContext(base: Context) {
        val locales = AppLocale.locales(base)
        val context = if (locales.isNotEmpty()) {
            base.createConfigurationContext(
                Configuration(base.resources.configuration).apply {
                    setLocales(android.os.LocaleList(*locales.toTypedArray()))
                }
            )
        } else {
            base
        }
        super.attachBaseContext(context)
    }
}