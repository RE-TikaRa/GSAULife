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

object AcademicFeature {
    fun createFragment(): Fragment = AcademicRootFragment()

    fun isLoggedIn(context: Context, system: SchoolSystem): Boolean =
        SchoolSessionStore.get(context).isLoggedIn(system)

    fun loginIntent(context: Context, system: SchoolSystem): Intent =
        LoginActivity.intent(context, system)

    fun logoutAll(context: Context) {
        SchoolSessionStore.get(context).clearAll()
        AcademicCache.get(context).clear()
        AcademicSettings.get(context).clear()
        CookieManager.getInstance().removeAllCookies { CookieManager.getInstance().flush() }
    }

    fun resetToMenu(fragment: Fragment) {
        (fragment as? AcademicRootFragment)?.resetToMenu()
    }
}
