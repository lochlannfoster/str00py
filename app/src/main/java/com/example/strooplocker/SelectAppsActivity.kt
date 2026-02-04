package com.example.strooplocker

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SearchView
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

/**
 * Data class to hold app information for display and filtering.
 */
data class AppInfo(
    val appName: String,
    val packageName: String,
    val icon: Drawable,
    val applicationInfo: ApplicationInfo,
    var isLocked: Boolean
)


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
    private var allApps: List<AppInfo> = emptyList()
    private var filteredApps: List<AppInfo> = emptyList()

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

        // Set up search functionality
        val searchView = findViewById<SearchView>(R.id.searchView)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                filterApps(newText ?: "")
                return true
            }
        })

        loadInstalledApps()
    }

    /**
     * Filters the app list based on search query.
     * Matches against app name and package name.
     * Results are sorted with locked apps first, then alphabetically.
     */
    private fun filterApps(query: String) {
        filteredApps = if (query.isEmpty()) {
            allApps
        } else {
            allApps.filter {
                it.appName.contains(query, ignoreCase = true) ||
                it.packageName.contains(query, ignoreCase = true)
            }
        }
        // Sort: locked apps first, then alphabetically
        filteredApps = filteredApps.sortedWith(compareBy({ !it.isLocked }, { it.appName.lowercase() }))
        if (::appsAdapter.isInitialized) {
            appsAdapter.updateList(filteredApps)
        }
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
                val pm = packageManager
                val apps = withContext(Dispatchers.IO) {
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
                    userApps
                }

                // Check which apps are already locked
                val lockedAppsSet = withContext(Dispatchers.IO) {
                    Log.d(TAG, "Retrieving currently locked apps")
                    LockManager.getLockedApps(this@SelectAppsActivity).toSet()
                }

                Log.d(TAG, "Currently locked apps: $lockedAppsSet")

                // Display results or message if no apps found
                if (apps.isEmpty()) {
                    Toast.makeText(this@SelectAppsActivity,
                        getString(R.string.toast_no_apps),
                        Toast.LENGTH_LONG).show()
                    Log.w(TAG, "No apps available to lock - adapter not created")
                } else {
                    // Convert to AppInfo list with locked status
                    val appInfoList = apps.map { app ->
                        AppInfo(
                            appName = app.loadLabel(pm).toString(),
                            packageName = app.packageName,
                            icon = app.loadIcon(pm),
                            applicationInfo = app,
                            isLocked = lockedAppsSet.contains(app.packageName)
                        )
                    }

                    // Sort: locked apps first, then alphabetically
                    allApps = appInfoList.sortedWith(compareBy({ !it.isLocked }, { it.appName.lowercase() }))
                    filteredApps = allApps

                    appsAdapter = AppsAdapter(filteredApps) { appInfo ->
                        onAppSelected(appInfo)
                    }
                    recyclerView.adapter = appsAdapter
                    Log.d(TAG, "Adapter set with ${allApps.size} apps")
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
     * @param appInfo The AppInfo of the selected app
     */
    private fun onAppSelected(appInfo: AppInfo) {
        lifecycleScope.launch {
            Log.d(TAG, "App selection changed - Package: ${appInfo.packageName}, Currently Locked: ${appInfo.isLocked}")

            try {
                if (appInfo.isLocked) {
                    // Unlock the app
                    LockManager.removeLockedApp(this@SelectAppsActivity, appInfo.packageName)
                    Toast.makeText(this@SelectAppsActivity,
                        getString(R.string.toast_app_unlocked, appInfo.appName),
                        Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "Successfully UNLOCKED: ${appInfo.packageName}")
                } else {
                    // Lock the app
                    LockManager.addLockedApp(this@SelectAppsActivity, appInfo.packageName)
                    Toast.makeText(this@SelectAppsActivity,
                        getString(R.string.toast_app_locked, appInfo.appName),
                        Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "Successfully LOCKED: ${appInfo.packageName}")
                }

                // Update locked status in our lists
                val updatedLockedApps = withContext(Dispatchers.IO) {
                    LockManager.getLockedApps(this@SelectAppsActivity).toSet()
                }

                Log.d(TAG, "Current locked apps: ${updatedLockedApps.joinToString()}")

                // Update isLocked status in allApps
                allApps = allApps.map { app ->
                    app.copy(isLocked = updatedLockedApps.contains(app.packageName))
                }

                // Re-sort: locked apps first, then alphabetically
                allApps = allApps.sortedWith(compareBy({ !it.isLocked }, { it.appName.lowercase() }))

                // Re-apply filter with current search query
                val searchView = findViewById<SearchView>(R.id.searchView)
                filterApps(searchView.query?.toString() ?: "")
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
     * @property apps List of AppInfo objects for installed apps
     * @property onAppClick Callback function when an app is clicked
     */
    inner class AppsAdapter(
        private var apps: List<AppInfo>,
        private val onAppClick: (AppInfo) -> Unit
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
            val appInfo = apps[position]

            val context = holder.itemView.context
            holder.appName.text = appInfo.appName
            holder.appIcon.setImageDrawable(appInfo.icon)
            holder.lockStatus.text = context.getString(
                if (appInfo.isLocked) R.string.app_status_locked else R.string.app_status_unlocked
            )

            // Set text color based on lock status (use resource colors)
            holder.lockStatus.setTextColor(
                ContextCompat.getColor(
                    context,
                    if (appInfo.isLocked) R.color.locked_green else R.color.unlocked_gray
                )
            )

            holder.itemView.setOnClickListener {
                onAppClick(appInfo)
            }
        }

        /**
         * Returns the total number of items in the adapter.
         */
        override fun getItemCount() = apps.size

        /**
         * Updates the list of apps and refreshes the UI.
         *
         * @param newList Updated list of AppInfo objects
         */
        fun updateList(newList: List<AppInfo>) {
            apps = newList
            notifyDataSetChanged()
        }
    }
}