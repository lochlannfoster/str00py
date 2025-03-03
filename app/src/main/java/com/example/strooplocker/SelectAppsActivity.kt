package com.example.strooplocker

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.content.ContextCompat
import android.graphics.Color
import android.os.Handler
import android.os.Looper


/**
 * SelectAppsActivity displays a list of installed apps and allows the user
 * to select which ones should be locked behind the Stroop challenge.
 *
 * This activity:
 * 1. Loads all installed apps that have a launcher icon
 * 2. Displays them in a scrollable list with their icons
 * 3. Allows the user to toggle their locked status
 * 4. Persists these selections to the database
 */
class SelectAppsActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SelectAppsActivity_DEBUG"
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var appsAdapter: AppsAdapter

    /**
     * Initializes the activity, sets up the RecyclerView, and
     * loads the list of installed apps.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_apps)

        Log.d(TAG, "onCreate: Setting up apps selection activity")

        recyclerView = findViewById(R.id.appsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        loadInstalledApps()
    }

    /**
     * Loads all installed apps that have a launcher icon.
     *
     * This method:
     * 1. Queries for all apps with launcher activities
     * 2. Filters out system utilities and our own app
     * 3. Checks which apps are already locked
     * 4. Displays the results in the RecyclerView
     */
    private fun loadInstalledApps() {
        lifecycleScope.launch {
            Log.d(TAG, "Loading installed apps")

            try {
                val apps = withContext(Dispatchers.IO) {
                    val pm = packageManager
                    val ourPackageName = packageName

                    Log.d(TAG, "Our package name: $ourPackageName")

                    // Get all apps with launcher activities
                    val launcherIntent = Intent(Intent.ACTION_MAIN)
                    launcherIntent.addCategory(Intent.CATEGORY_LAUNCHER)

                    val resolveInfoList = pm.queryIntentActivities(launcherIntent, 0)
                    Log.d(TAG, "Found ${resolveInfoList.size} launchable apps")

                    // Convert to ApplicationInfo, skip our own app
                    val userApps = resolveInfoList
                        .map { it.activityInfo.applicationInfo }
                        .filter { app ->
                            // Only minimal filtering - just exclude our own app
                            val packageName = app.packageName
                            val isOurApp = packageName == ourPackageName

                            // Skip very obvious system utilities
                            val isSystemUtility = packageName.contains("settings") ||
                                    packageName.contains("launcher") ||
                                    packageName.contains("systemui")

                            val isSystemApp = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0

                            Log.d(TAG, "Package: $packageName, System: $isSystemApp, " +
                                    "Include: ${!isOurApp && !isSystemUtility}")

                            // Include everything except our app and obvious system utilities
                            !isOurApp && !isSystemUtility
                        }
                        .distinctBy { it.packageName }

                    Log.d(TAG, "Final filtered apps count: ${userApps.size}")
                    userApps.sortedBy { app -> app.loadLabel(pm).toString() }
                }

                // Check which apps are already locked
                val lockedApps = withContext(Dispatchers.IO) {
                    Log.d(TAG, "Retrieving currently locked apps")
                    LockManager.getLockedApps(this@SelectAppsActivity)
                }

                Log.d(TAG, "Currently locked apps: $lockedApps")

                // Add a delay to ensure UI is fully ready before setting adapter
                Handler(Looper.getMainLooper()).post {
                    if (apps.isEmpty()) {
                        Toast.makeText(this@SelectAppsActivity,
                            getString(R.string.toast_no_apps),
                            Toast.LENGTH_LONG).show()
                        Log.w(TAG, "No apps available to lock - adapter not created")
                    } else {
                        appsAdapter = AppsAdapter(apps, lockedApps.toSet()) { app, isLocked ->
                            onAppSelected(app, isLocked)
                        }
                        recyclerView.adapter = appsAdapter
                        Log.d(TAG, "Adapter set with ${apps.size} apps")
                    }
                }

                // Display results or message if no apps found
                if (apps.isEmpty()) {
                    Toast.makeText(this@SelectAppsActivity,
                        getString(R.string.toast_no_apps),
                        Toast.LENGTH_LONG).show()
                    Log.w(TAG, "No apps available to lock - adapter not created")
                } else {
                    appsAdapter = AppsAdapter(apps, lockedApps.toSet()) { app, isLocked ->
                        onAppSelected(app, isLocked)
                    }
                    recyclerView.adapter = appsAdapter
                    Log.d(TAG, "Adapter set with ${apps.size} apps")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading apps", e)
                Toast.makeText(this@SelectAppsActivity,
                    getString(R.string.toast_error_loading_apps, e.message),
                    Toast.LENGTH_LONG).show()
            }
        }
    }


    /**
     * Handles app selection events.
     * Toggles the locked state of the selected app and updates the UI.
     *
     * @param app The ApplicationInfo of the selected app
     * @param isCurrentlyLocked Whether the app is currently locked
     */
    private fun onAppSelected(app: ApplicationInfo, isCurrentlyLocked: Boolean) {
        lifecycleScope.launch {
            Log.d(TAG, "App selection changed - Package: ${app.packageName}, Currently Locked: $isCurrentlyLocked")

            try {
                if (isCurrentlyLocked) {
                    // Unlock the app
                    LockManager.removeLockedApp(this@SelectAppsActivity, app.packageName)
                    Toast.makeText(this@SelectAppsActivity,
                        getString(R.string.toast_app_unlocked, app.loadLabel(packageManager)),
                        Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "Successfully UNLOCKED: ${app.packageName}")
                } else {
                    // Lock the app
                    LockManager.addLockedApp(this@SelectAppsActivity, app.packageName)
                    Toast.makeText(this@SelectAppsActivity,
                        getString(R.string.toast_app_locked, app.loadLabel(packageManager)),
                        Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "Successfully LOCKED: ${app.packageName}")
                }

                // Refresh the list
                val updatedLockedApps = withContext(Dispatchers.IO) {
                    LockManager.getLockedApps(this@SelectAppsActivity)
                }

                Log.d(TAG, "Current locked apps: ${updatedLockedApps.joinToString()}")
                appsAdapter.updateLockedApps(updatedLockedApps.toSet())
            } catch (e: Exception) {
                Log.e(TAG, "Error updating app lock state", e)
                Toast.makeText(this@SelectAppsActivity,
                    getString(R.string.toast_error_generic, e.message),
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * RecyclerView adapter for displaying the list of apps.
     * Handles the UI for each app item and its locked status.
     *
     * @property apps List of ApplicationInfo objects for installed apps
     * @property lockedApps Set of package names for apps that are locked
     * @property onAppClick Callback function when an app is clicked
     */
    inner class AppsAdapter(
        private val apps: List<ApplicationInfo>,
        private var lockedApps: Set<String>,
        private val onAppClick: (ApplicationInfo, Boolean) -> Unit
    ) : RecyclerView.Adapter<AppsAdapter.AppViewHolder>() {

        /**
         * ViewHolder for app items in the RecyclerView.
         * Binds app information to the item view.
         */
        inner class AppViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val appIcon: ImageView = view.findViewById(R.id.appIcon)
            val appName: TextView = view.findViewById(R.id.appName)
            val lockStatus: TextView = view.findViewById(R.id.lockStatus)
        }

        /**
         * Creates a new ViewHolder for app items.
         */
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_app, parent, false)
            return AppViewHolder(view)
        }

        /**
         * Binds data to a ViewHolder.
         * Displays app name, icon, and lock status.
         */
        override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
            val app = apps[position]
            val isLocked = lockedApps.contains(app.packageName)

            holder.appName.text = app.loadLabel(packageManager)
            holder.appIcon.setImageDrawable(app.loadIcon(packageManager))
            holder.lockStatus.text = if (isLocked) "Locked" else "Unlocked"

            // Set text color based on lock status
            holder.lockStatus.setTextColor(if (isLocked) Color.GREEN else Color.RED)

            holder.itemView.setOnClickListener {
                onAppClick(app, isLocked)
            }
        }

        /**
         * Returns the total number of items in the adapter.
         */
        override fun getItemCount() = apps.size

        /**
         * Updates the set of locked apps and refreshes the UI.
         *
         * @param newLockedApps Updated set of package names for locked apps
         */
        fun updateLockedApps(newLockedApps: Set<String>) {
            lockedApps = newLockedApps
            notifyDataSetChanged()
        }
    }
}