package com.privacynudge.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for Data Models: PermissionState and AppPermissionProfile.
 */
class DataModelTest {

    @Test
    fun `test PermissionState initialization`() {
        val state = PermissionState(
            type = PermissionType.LOCATION,
            isGranted = true,
            isDangerous = true,
            permissionName = "android.permission.ACCESS_FINE_LOCATION"
        )
        
        assertEquals(PermissionType.LOCATION, state.type)
        assertTrue(state.isGranted)
        assertTrue(state.isDangerous)
        assertEquals("android.permission.ACCESS_FINE_LOCATION", state.permissionName)
    }

    @Test
    fun `test AppPermissionProfile filtering`() {
        val permissions = listOf(
            PermissionState(PermissionType.LOCATION, isGranted = true),
            PermissionState(PermissionType.CAMERA, isGranted = false),
            PermissionState(PermissionType.MICROPHONE, isGranted = true)
        )
        
        val profile = AppPermissionProfile(
            packageName = "com.test.app",
            appName = "Test App",
            icon = null,
            isInstalled = true,
            permissions = permissions,
            riskLevel = RiskLevel.MEDIUM
        )
        
        // Test grantedPermissions
        assertEquals(2, profile.grantedPermissions.size)
        assertTrue(profile.grantedPermissions.any { it.type == PermissionType.LOCATION })
        assertTrue(profile.grantedPermissions.any { it.type == PermissionType.MICROPHONE })
        
        // Test deniedPermissions
        assertEquals(1, profile.deniedPermissions.size)
        assertEquals(PermissionType.CAMERA, profile.deniedPermissions[0].type)
    }

    @Test
    fun `test AppPermissionProfile dangerousPermissionCount`() {
        val permissions = listOf(
            PermissionState(PermissionType.LOCATION, isGranted = true, isDangerous = true),
            PermissionState(PermissionType.CAMERA, isGranted = true, isDangerous = false), // Not marked as dangerous for test
            PermissionState(PermissionType.MICROPHONE, isGranted = false, isDangerous = true),
            PermissionState(PermissionType.CONTACTS, isGranted = true, isDangerous = true)
        )
        
        val profile = AppPermissionProfile(
            packageName = "com.test.app",
            appName = "Test App",
            icon = null,
            isInstalled = true,
            permissions = permissions,
            riskLevel = RiskLevel.HIGH
        )
        
        // Only LOCATION and CONTACTS should count (isGranted AND isDangerous)
        assertEquals(2, profile.dangerousPermissionCount)
    }
}
