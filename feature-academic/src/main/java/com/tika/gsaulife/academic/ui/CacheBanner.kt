package com.tika.gsaulife.academic.ui

import android.view.View
import com.tika.gsaulife.academic.R
import com.tika.gsaulife.academic.databinding.AcademicViewCacheBannerBinding
import java.text.DateFormat
import java.util.Date

internal fun AcademicViewCacheBannerBinding.show(
    fetchedAt: Long,
    needsLogin: Boolean,
    login: () -> Unit,
) {
    root.visibility = View.VISIBLE
    academicCacheMessage.text = root.context.getString(
        if (needsLogin) R.string.academic_cache_logged_out else R.string.academic_cache_offline,
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(fetchedAt)),
    )
    academicCacheLogin.visibility = if (needsLogin) View.VISIBLE else View.GONE
    academicCacheLogin.setOnClickListener { login() }
}

internal fun AcademicViewCacheBannerBinding.hide() {
    root.visibility = View.GONE
}
