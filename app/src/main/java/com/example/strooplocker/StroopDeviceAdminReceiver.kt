package com.example.strooplocker

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

/**
 * Device Administrator Receiver for the str00py app.
 *
 * This receiver handles device administrator status changes, such as when
 * the app is granted or revoked device admin privileges. Device admin
 * capabilities allow the app to perform certain system-level operations
 * like locking the device.
 *
 * To use device admin features, the app needs to be explicitly granted
 * these permissions by the user through the system settings.
 */
class StroopDeviceAdminReceiver : DeviceAdminReceiver() {

    /**
     * Called when the app is granted device administrator privileges.
     *
     * @param context The context in which the receiver is running
     * @param intent The intent being received
     */
    override fun onEnabled(context: Context, intent: Intent) {
        Toast.makeText(context, "Device Admin: enabled", Toast.LENGTH_SHORT).show()
    }

    /**
     * Called when the app's device administrator privileges are revoked.
     *
     * @param context The context in which the receiver is running
     * @param intent The intent being received
     */
    override fun onDisabled(context: Context, intent: Intent) {
        Toast.makeText(context, "Device Admin: disabled", Toast.LENGTH_SHORT).show()
    }
}