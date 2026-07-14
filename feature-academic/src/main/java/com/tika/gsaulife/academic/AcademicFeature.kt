package com.tika.gsaulife.academic

import android.content.Context
import android.content.Intent
import android.webkit.CookieManager
import androidx.fragment.app.Fragment
import com.tika.gsaulife.academic.data.AcademicCache
import com.tika.gsaulife.academic.data.AcademicSettings
import com.tika.gsaulife.academic.data.SchoolSessionStore
import com.tika.gsaulife.academic.ui.AcademicRootFragment
import com.tika.gsaulife.academic.ui.LoginActivity
import com.tika.gsaulife.academic.widget.ExamScheduleWidgetProvider
import com.tika.gsaulife.academic.widget.TodayScheduleWidgetProvider
import com.tika.gsaulife.academic.widget.UpcomingScheduleWidgetProvider

object AcademicFeature {
    fun createFragment(): Fragment = AcademicRootFragment()

    fun isLoggedIn(context: Context): Boolean {
        val store = SchoolSessionStore.get(context)
        return store.isLoggedIn(SchoolSystem.ACADEMIC) && store.isLoggedIn(SchoolSystem.STUDENT_AFFAIRS)
    }

    fun loginIntent(context: Context, forceReauth: Boolean = false): Intent =
        LoginActivity.intent(context, forceReauth)

    fun logoutAll(context: Context) {
        SchoolSessionStore.get(context).clearAll()
        AcademicCache.get(context).clear()
        AcademicSettings.get(context).clear()
        CookieManager.getInstance().removeAllCookies { CookieManager.getInstance().flush() }
        refreshWidgets(context)
    }

    fun refreshWidgets(context: Context) {
        TodayScheduleWidgetProvider.refreshAll(context)
        UpcomingScheduleWidgetProvider.refreshAll(context)
        ExamScheduleWidgetProvider.refreshAll(context)
    }

    fun resetToMenu(fragment: Fragment) {
        (fragment as? AcademicRootFragment)?.resetToMenu()
    }
}
