package com.example.onefifty.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.onefifty.MainActivity
import com.example.onefifty.R
import org.json.JSONArray

class OneFiftyWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val currentMinutes = loadCurrentMinutesFromPrefs(context)
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, currentMinutes, 150)
        }
    }

    private fun loadCurrentMinutesFromPrefs(context: Context): Int {
        return try {
            val prefs = context.getSharedPreferences("tempo150_local_prefs", Context.MODE_PRIVATE)
            val jsonStr = prefs.getString("key_weekly_sessions", null) ?: return 0
            val array = JSONArray(jsonStr)
            var totalSecs = 0
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                totalSecs += if (obj.has("durationSeconds")) {
                    obj.getInt("durationSeconds")
                } else {
                    obj.optInt("durationMinutes", 0) * 60
                }
            }
            totalSecs / 60
        } catch (e: Exception) {
            0
        }
    }

    companion object {
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            currentMinutes: Int,
            targetMinutes: Int = 150
        ) {
            val percentage = if (targetMinutes > 0) (currentMinutes * 100) / targetMinutes else 0
            val views = RemoteViews(context.packageName, R.layout.widget_onefifty_1x1)

            views.setTextViewText(R.id.widget_percentage, "$percentage%")
            views.setTextViewText(R.id.widget_subtext, "$currentMinutes/${targetMinutes}m")

            // Tap Widget -> Open App
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun updateAllWidgets(context: Context, currentMinutes: Int, targetMinutes: Int = 150) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, OneFiftyWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                for (appWidgetId in appWidgetIds) {
                    updateAppWidget(context, appWidgetManager, appWidgetId, currentMinutes, targetMinutes)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
