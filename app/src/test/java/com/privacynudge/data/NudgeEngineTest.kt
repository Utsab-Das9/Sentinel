package com.privacynudge.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Method

/**
 * Unit tests for NudgeEngine.
 * Uses Class.forName("sun.misc.Unsafe") at runtime to create an instance
 * without calling the constructor (which requires Android Context).
 */
class NudgeEngineTest {

    private lateinit var nudgeEngine: NudgeEngine

    @Before
    fun setup() {
        val unsafeClass = Class.forName("sun.misc.Unsafe")
        val theUnsafe = unsafeClass.getDeclaredField("theUnsafe")
        theUnsafe.isAccessible = true
        val unsafe = theUnsafe.get(null)
        val allocate = unsafeClass.getDeclaredMethod("allocateInstance", Class::class.java)
        nudgeEngine = allocate.invoke(unsafe, NudgeEngine::class.java) as NudgeEngine
    }

    // ── evaluatePermissionRisk ──────────────────────────────────────────

    @Test
    fun `returns null for uninstalled app`() {
        val profile = profile(isInstalled = false, riskLevel = RiskLevel.HIGH)
        assertNull(nudgeEngine.evaluatePermissionRisk(profile))
    }

    @Test
    fun `returns null for LOW risk`() {
        val profile = profile(isInstalled = true, riskLevel = RiskLevel.LOW)
        assertNull(nudgeEngine.evaluatePermissionRisk(profile))
    }

    @Test
    fun `returns event for HIGH risk with permissions listed`() {
        val perms = listOf(
            PermissionState(PermissionType.LOCATION, isGranted = true),
            PermissionState(PermissionType.CAMERA, isGranted = true),
            PermissionState(PermissionType.MICROPHONE, isGranted = true),
            PermissionState(PermissionType.CONTACTS, isGranted = true)
        )
        val event = nudgeEngine.evaluatePermissionRisk(profile(true, RiskLevel.HIGH, perms))

        assertNotNull(event)
        assertEquals("Test App", event!!.appName)
        assertTrue(event.reason.contains("high-risk"))
        assertEquals("Permissions", event.permissionType)
    }

    @Test
    fun `returns event for MEDIUM risk`() {
        val perms = listOf(
            PermissionState(PermissionType.LOCATION, isGranted = true),
            PermissionState(PermissionType.CAMERA, isGranted = true)
        )
        val event = nudgeEngine.evaluatePermissionRisk(profile(true, RiskLevel.MEDIUM, perms))

        assertNotNull(event)
        assertTrue(event!!.reason.contains("noteworthy"))
    }

    // ── generateDnsNudgeReason (private, via reflection) ────────────────

    @Test
    fun `Location plus Analytics triggers notification`() {
        val perms = listOf(PermissionState(PermissionType.LOCATION, isGranted = true))
        val (reason, notify) = callNudgeReason(profile(true, RiskLevel.MEDIUM, perms), "a.example.com", "Analytics")
        assertTrue(notify)
        assertTrue(reason.contains("Location", ignoreCase = true))
    }

    @Test
    fun `Contacts plus Social triggers notification`() {
        val perms = listOf(PermissionState(PermissionType.CONTACTS, isGranted = true))
        val (reason, notify) = callNudgeReason(profile(true, RiskLevel.MEDIUM, perms), "s.example.com", "Social")
        assertTrue(notify)
        assertTrue(reason.contains("Contact", ignoreCase = true))
    }

    @Test
    fun `multiple permissions plus Analytics triggers notification`() {
        val perms = listOf(
            PermissionState(PermissionType.STORAGE, isGranted = true),
            PermissionState(PermissionType.PHONE, isGranted = true),
            PermissionState(PermissionType.CALENDAR, isGranted = true)
        )
        val (reason, notify) = callNudgeReason(profile(true, RiskLevel.MEDIUM, perms), "t.example.com", "Analytics")
        assertTrue(notify)
        assertTrue(reason.contains("analytics", ignoreCase = true))
    }

    @Test
    fun `Advertising domain with permissions triggers notification`() {
        val perms = listOf(PermissionState(PermissionType.STORAGE, isGranted = true))
        val (reason, notify) = callNudgeReason(profile(true, RiskLevel.LOW, perms), "ads.example.com", "Advertising")
        assertTrue(notify)
        assertTrue(reason.contains("ad network", ignoreCase = true))
    }

    @Test
    fun `no permissions means no notification`() {
        val (_, notify) = callNudgeReason(profile(true, RiskLevel.LOW), "r.example.com", "Other")
        assertEquals(false, notify)
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private fun profile(
        isInstalled: Boolean, riskLevel: RiskLevel,
        permissions: List<PermissionState> = emptyList()
    ) = AppPermissionProfile(
        packageName = "com.test.app", appName = "Test App", icon = null,
        isInstalled = isInstalled, permissions = permissions, riskLevel = riskLevel
    )

    @Suppress("UNCHECKED_CAST")
    private fun callNudgeReason(
        profile: AppPermissionProfile, domain: String, category: String
    ): Pair<String, Boolean> {
        val m: Method = NudgeEngine::class.java.getDeclaredMethod(
            "generateDnsNudgeReason",
            AppPermissionProfile::class.java, String::class.java, String::class.java
        )
        m.isAccessible = true
        return m.invoke(nudgeEngine, profile, domain, category) as Pair<String, Boolean>
    }
}
