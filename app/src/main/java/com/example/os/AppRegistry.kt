package com.example.os

import android.Manifest
import com.example.R
import com.example.model.AppDefinition

object AppRegistry {
    val APPS: List<AppDefinition> = listOf(
        AppDefinition(
            id = "phone",
            titleRes = R.string.app_phone,
            iconKey = "phone"
        ),
        AppDefinition(
            id = "messages",
            titleRes = R.string.app_messages,
            iconKey = "messages"
        ),
        AppDefinition(
            id = "camera",
            titleRes = R.string.app_camera,
            iconKey = "camera",
            requiredPermission = Manifest.permission.CAMERA,
            permissionRationaleRes = R.string.camera_permission_rationale
        ),
        AppDefinition(
            id = "clock",
            titleRes = R.string.app_clock,
            iconKey = "clock"
        ),
        AppDefinition(
            id = "calculator",
            titleRes = R.string.app_calculator,
            iconKey = "calculator"
        ),
        AppDefinition(
            id = "notes",
            titleRes = R.string.app_notes,
            iconKey = "notes"
        ),
        AppDefinition(
            id = "gallery",
            titleRes = R.string.app_gallery,
            iconKey = "gallery"
        ),
        AppDefinition(
            id = "settings",
            titleRes = R.string.app_settings,
            iconKey = "settings"
        ),
        AppDefinition(
            id = "files",
            titleRes = R.string.app_files,
            iconKey = "files"
        ),
        AppDefinition(
            id = "contacts",
            titleRes = R.string.app_contacts,
            iconKey = "contacts",
            requiredPermission = Manifest.permission.READ_CONTACTS,
            permissionRationaleRes = R.string.contacts_permission_rationale
        ),
        AppDefinition(
            id = "calendar",
            titleRes = R.string.app_calendar,
            iconKey = "calendar",
            requiredPermission = Manifest.permission.READ_CALENDAR,
            permissionRationaleRes = R.string.calendar_permission_rationale
        )
    )

    val DEFAULT_DOCK_APP_IDS = listOf("phone", "messages", "camera", "settings")
    val DEFAULT_GRID_APP_IDS = listOf("clock", "calculator", "notes", "gallery", "calendar", "contacts", "files")

    fun getApp(id: String): AppDefinition? {
        return APPS.find { it.id == id }
    }
}
