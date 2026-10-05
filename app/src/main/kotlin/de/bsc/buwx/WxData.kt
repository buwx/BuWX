package de.bsc.buwx

import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.text.NumberFormat
import kotlin.math.round

/**
 * One set of measurements from the weather station API.
 */
data class WxData(
    /** Time of measurement in unix seconds. */
    val time: Long,
    val outTemp: Double,
    val outHumidity: Double,
    val dailyRain: Double,
    val windSpeed: Double,
    val windDir: String,
) {
    /** Data older than [MAX_AGE_SEC] is not shown, the widget displays dashes instead. */
    fun isCurrent(nowSec: Long = System.currentTimeMillis() / 1000): Boolean =
        nowSec - time < MAX_AGE_SEC

    /** Temperature band used to pick the widget background color. */
    val tempBand: TempBand
        get() = when {
            outTemp < 0.0 -> TempBand.FROST
            outTemp < 10.0 -> TempBand.COLD
            outTemp < 20.0 -> TempBand.MILD
            outTemp < 28.0 -> TempBand.WARM
            else -> TempBand.HOT
        }

    // Formatting and rounding as in the original widget

    fun formatTemp(): String = numberFormat().format(outTemp) + "°C"

    fun formatHumidity(): String = numberFormat().format(outHumidity) + "%"

    fun formatWind(): String {
        val speed = numberFormat().format(round(windSpeed)) + " km/h"
        return if (windDir.isBlank() || windDir == "-") speed else "$speed $windDir"
    }

    /** Daily rain rounded to liters, or null when it rounds to zero. */
    fun formatRain(): String? {
        val rain = round(dailyRain)
        return if (rain > 0.0) numberFormat().format(rain) + "l" else null
    }

    enum class TempBand { FROST, COLD, MILD, WARM, HOT }

    companion object {
        const val MAX_AGE_SEC = 600L
        private const val TIMEOUT_MS = 10_000

        @Throws(IOException::class, JSONException::class)
        fun load(url: String = Wx.JSON_URL): WxData {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            try {
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                return fromJson(JSONObject(body))
            } finally {
                connection.disconnect()
            }
        }

        /** All values are delivered as strings, optDouble/optLong convert them. */
        fun fromJson(json: JSONObject) = WxData(
            time = json.optLong("time", 0L),
            outTemp = json.optDouble("outTemp", 0.0),
            outHumidity = json.optDouble("outHumidity", 0.0),
            dailyRain = json.optDouble("dailyRain", 0.0),
            windSpeed = json.optDouble("windSpeed", 0.0),
            windDir = json.optString("windDir", ""),
        )

        private fun numberFormat(): NumberFormat = NumberFormat.getInstance()
    }
}
