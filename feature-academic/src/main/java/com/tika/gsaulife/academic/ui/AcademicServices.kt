package com.tika.gsaulife.academic.ui

import android.content.Context
import com.tika.gsaulife.academic.data.AcademicCache
import com.tika.gsaulife.academic.data.JwxtRepository
import com.tika.gsaulife.academic.data.SchoolSessionStore
import com.tika.gsaulife.academic.data.XgfwRepository

internal fun jwxtRepository(context: Context): JwxtRepository = JwxtRepository(
    SchoolSessionStore.get(context),
    AcademicCache.get(context),
)

internal fun xgfwRepository(context: Context): XgfwRepository = XgfwRepository(
    SchoolSessionStore.get(context),
    AcademicCache.get(context),
)
