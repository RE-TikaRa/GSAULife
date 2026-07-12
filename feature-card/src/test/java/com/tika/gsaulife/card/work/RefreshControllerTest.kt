package com.tika.gsaulife.card.work

import com.tika.gsaulife.card.RefreshMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RefreshControllerTest {
    @Test
    fun `只有持续刷新且存在校园卡时运行服务`() {
        assertFalse(shouldRunRefreshService(RefreshMode.ON_DEMAND, false))
        assertFalse(shouldRunRefreshService(RefreshMode.ON_DEMAND, true))
        assertFalse(shouldRunRefreshService(RefreshMode.CONTINUOUS, false))
        assertTrue(shouldRunRefreshService(RefreshMode.CONTINUOUS, true))
    }
}
