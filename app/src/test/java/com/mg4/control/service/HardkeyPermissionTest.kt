package com.mg4.control.service

import android.content.pm.PermissionInfo
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [T-902] Le broadcast hardkey pilote des écritures véhicule. La seule barrière contre
 * un émetteur tiers est le niveau de protection de la permission exigée : si elle
 * retombe en "normal", n'importe quelle app peut la demander et forger l'action.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HardkeyPermissionTest {

    @Test
    fun `la permission hardkey est declaree en signature`() {
        val pm = ApplicationProvider.getApplicationContext<android.content.Context>().packageManager
        val info = pm.getPermissionInfo(MG4ControlService.HARDKEY_PERMISSION, 0)
        assertEquals(
            "La permission hardkey doit rester signature-only",
            PermissionInfo.PROTECTION_SIGNATURE,
            info.protection
        )
    }

    @Test
    fun `le nom de la permission correspond a celui du Manifest`() {
        assertEquals("com.mg4.control.permission.RECEIVE_HARDKEY",
            MG4ControlService.HARDKEY_PERMISSION)
    }
}
