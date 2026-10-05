/*
 * ----------------------------------------------------------------------------
 *
 * Copyright 2015 Michael Buchfink (buchfink@web.de)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * ----------------------------------------------------------------------------
 *
 * 03.10.2015 - Creation date
 *
 * ----------------------------------------------------------------------------
 */
package de.bsc.buwx

import android.util.Log

/**
 * Application constants
 */
object Wx {
    /** Enables debug logging. Must be false in commits and releases. */
    const val DEV = false
    const val JSON_URL = "https://ws.buwx.de/api/wxdata.json"
    const val WEB_URL = "https://buwx.de"
}

/** Logs a debug message when [Wx.DEV] is enabled. */
inline fun logDebug(tag: String, message: () -> String) {
    if (Wx.DEV) Log.d(tag, message())
}
