package tk.zwander.overlaymanager.util

import android.content.Context
import android.os.Handler
import android.os.Looper
import tk.zwander.overlaymanager.App

val Context.app: App
    get() = applicationContext as App

val mainHandler = Handler(Looper.getMainLooper())