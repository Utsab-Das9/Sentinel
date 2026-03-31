package com.privacynudge.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for PrivacyRiskCalculator.
 * Uses Class.forName("sun.misc.Unsafe") at runtime to create an instance
 * without calling the constructor (which requires Android Context).
 */
class PrivacyRiskCalculatorTest {

    private lateinit var calculator: PrivacyRiskCalculator

    @Before
    fun setup() {
        val unsafeClass = Class.forName("sun.misc.Unsafe")
        val theUnsafe = unsafeClass.getDeclaredField("theUnsafe")
        theUnsafe.isAccessible = true
        val unsafe = theUnsafe.get(null)
        val allocate = unsafeClass.getDeclaredMethod("allocateInstance", Class::class.java)
        calculator = allocate.invoke(unsafe, PrivacyRiskCalculator::class.java) as PrivacyRiskCalculator
    }

    // ── getRiskLevel ────────────────────────────────────────────────────

    @Test
    fun `getRiskLevel returns HIGH for score at or above 0_70`() {
        assertEquals(RiskLevel.HIGH, calculator.getRiskLevel(0.75f))
        assertEquals(RiskLevel.HIGH, calculator.getRiskLevel(0.70f))
        assertEquals(RiskLevel.HIGH, calculator.getRiskLevel(1.0f))
    }

    @Test
    fun `getRiskLevel returns MEDIUM for score between 0_35 and 0_70`() {
        assertEquals(RiskLevel.MEDIUM, calculator.getRiskLevel(0.69f))
        assertEquals(RiskLevel.MEDIUM, calculator.getRiskLevel(0.50f))
        assertEquals(RiskLevel.MEDIUM, calculator.getRiskLevel(0.35f))
    }

    @Test
    fun `getRiskLevel returns LOW for score below 0_35`() {
        assertEquals(RiskLevel.LOW, calculator.getRiskLevel(0.34f))
        assertEquals(RiskLevel.LOW, calculator.getRiskLevel(0.10f))
        assertEquals(RiskLevel.LOW, calculator.getRiskLevel(0.0f))
    }

    @Test
    fun `getRiskLevel overrides to LOW for Gemini package`() {
        assertEquals(RiskLevel.LOW, calculator.getRiskLevel(0.95f, packageName = "com.google.android.apps.bard"))
    }

    @Test
    fun `getRiskLevel overrides to LOW for Gemini app name`() {
        assertEquals(RiskLevel.LOW, calculator.getRiskLevel(0.95f, appName = "Google Gemini"))
        assertEquals(RiskLevel.LOW, calculator.getRiskLevel(0.80f, appName = "gemini"))
    }

    @Test
    fun `getRiskLevel does not override for non-Gemini apps`() {
        assertEquals(RiskLevel.HIGH, calculator.getRiskLevel(0.95f, packageName = "com.other.app"))
    }

    // ── calculateSL (reflection) ────────────────────────────────────────

    @Test
    fun `calculateSL returns 0 for empty permissions`() {
        assertEquals(0f, callSL(emptyList()), 0.001f)
    }

    @Test
    fun `calculateSL returns 0 for non-granted permissions`() {
        val perms = listOf(
            perm("android.permission.ACCESS_FINE_LOCATION", false),
            perm("android.permission.CAMERA", false)
        )
        assertEquals(0f, callSL(perms), 0.001f)
    }

    @Test
    fun `calculateSL returns positive for granted dangerous permissions`() {
        val perms = listOf(
            perm("android.permission.ACCESS_FINE_LOCATION", true),
            perm("android.permission.CAMERA", true)
        )
        val sl = callSL(perms)
        assertTrue("SL > 0 for granted dangerous", sl > 0f)
        assertTrue("SL <= 1", sl <= 1f)
    }

    @Test
    fun `calculateSL increases with more sensitive permissions`() {
        val few = listOf(perm("android.permission.ACCESS_FINE_LOCATION", true))
        val many = listOf(
            perm("android.permission.ACCESS_FINE_LOCATION", true),
            perm("android.permission.CAMERA", true),
            perm("android.permission.RECORD_AUDIO", true),
            perm("android.permission.READ_CONTACTS", true),
            perm("android.permission.READ_SMS", true)
        )
        assertTrue("More perms = higher SL", callSL(many) > callSL(few))
    }

    // ── calculateUI (reflection) ────────────────────────────────────────

    @Test
    fun `calculateUI returns 1_0 for recent update`() {
        val pi = android.content.pm.PackageInfo().apply {
            lastUpdateTime = System.currentTimeMillis() - (10L * 86400000)
        }
        assertEquals(1.0f, callUI(pi), 0.001f)
    }

    @Test
    fun `calculateUI returns 0_1 for very old update`() {
        val pi = android.content.pm.PackageInfo().apply {
            lastUpdateTime = System.currentTimeMillis() - (400L * 86400000)
        }
        assertEquals(0.1f, callUI(pi), 0.001f)
    }

    @Test
    fun `calculateUI returns intermediate for mid-age update`() {
        val pi = android.content.pm.PackageInfo().apply {
            lastUpdateTime = System.currentTimeMillis() - (180L * 86400000)
        }
        val ui = callUI(pi)
        assertTrue("UI between 0.1 and 1.0", ui > 0.1f && ui < 1.0f)
    }

    // ── calculateNB (reflection) ────────────────────────────────────────

    @Test
    fun `calculateNB returns 0 without INTERNET`() {
        val pi = android.content.pm.PackageInfo().apply {
            requestedPermissions = arrayOf("android.permission.READ_CONTACTS")
        }
        assertEquals(0f, callNB(pi), 0.001f)
    }

    @Test
    fun `calculateNB returns 0_5 with INTERNET only`() {
        val pi = android.content.pm.PackageInfo().apply {
            requestedPermissions = arrayOf("android.permission.INTERNET")
        }
        assertEquals(0.5f, callNB(pi), 0.001f)
    }

    @Test
    fun `calculateNB returns 0_7 with INTERNET and NETWORK_STATE`() {
        val pi = android.content.pm.PackageInfo().apply {
            requestedPermissions = arrayOf(
                "android.permission.INTERNET",
                "android.permission.ACCESS_NETWORK_STATE"
            )
        }
        assertEquals(0.7f, callNB(pi), 0.001f)
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private fun perm(name: String, granted: Boolean) = DetailedPermission(
        name = name, shortName = name.substringAfterLast('.'),
        label = name.substringAfterLast('.'),
        category = PermissionCategory.DANGEROUS, isGranted = granted
    )

    @Suppress("UNCHECKED_CAST")
    private fun callSL(perms: List<DetailedPermission>): Float {
        val m = PrivacyRiskCalculator::class.java.getDeclaredMethod("calculateSL", List::class.java)
        m.isAccessible = true
        return m.invoke(calculator, perms) as Float
    }

    private fun callUI(pi: android.content.pm.PackageInfo): Float {
        val m = PrivacyRiskCalculator::class.java.getDeclaredMethod("calculateUI", android.content.pm.PackageInfo::class.java)
        m.isAccessible = true
        return m.invoke(calculator, pi) as Float
    }

    private fun callNB(pi: android.content.pm.PackageInfo): Float {
        val m = PrivacyRiskCalculator::class.java.getDeclaredMethod("calculateNB", android.content.pm.PackageInfo::class.java)
        m.isAccessible = true
        return m.invoke(calculator, pi) as Float
    }
}
