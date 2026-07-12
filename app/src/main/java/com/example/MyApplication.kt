package com.example

import android.app.Application
import android.content.Context

class MyApplication : Application() {
    private var attributionContext: Context? = null
    private var isCreating = false

    override fun getApplicationContext(): Context {
        val defaultAppCtx = super.getApplicationContext()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            if (attributionContext == null && !isCreating) {
                isCreating = true
                try {
                    attributionContext = defaultAppCtx.createAttributionContext("default")
                } catch (e: Exception) {
                    // Fallback to default application context if creation fails
                } finally {
                    isCreating = false
                }
            }
            return attributionContext ?: defaultAppCtx
        }
        return defaultAppCtx
    }
}
