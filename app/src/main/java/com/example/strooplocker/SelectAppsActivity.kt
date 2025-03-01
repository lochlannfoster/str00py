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

class SelectAppsActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SelectAppsActivity_DEBUG"
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var appsAdapter: AppsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_apps)

        Log.d(TAG, "onCreate: Setting up apps selection activity")

        recyclerView = findViewById(R.id.appsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        loadInstalledApps()
    }

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

                // Display results or message if no apps found
                if (apps.isEmpty()) {
                    Toast.makeText(this@SelectAppsActivity,
                        "No lockable apps found on device",
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
                    "Error loading apps: ${e.message}",
                    Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun onAppSelected(app: ApplicationInfo, isCurrentlyLocked: Boolean) {
        lifecycleScope.launch {
            Log.d(TAG, "App selection changed - Package: ${app.packageName}, Currently Locked: $isCurrentlyLocked")

            try {
                if (isCurrentlyLocked) {
                    // Unlock the app
                    LockManager.removeLockedApp(this@SelectAppsActivity, app.packageName)
                    Toast.makeText(this@SelectAppsActivity,
                        "Unlocked: ${app.loadLabel(packageManager)}",
                        Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "Successfully UNLOCKED: ${app.packageName}")
                } else {
                    // Lock the app
                    LockManager.addLockedApp(this@SelectAppsActivity, app.packageName)
                    Toast.makeText(this@SelectAppsActivity,
                        "Locked: ${app.loadLabel(packageManager)}",
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
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    // AppsAdapter inner class
    inner class AppsAdapter(
        private val apps: List<ApplicationInfo>,
        private var lockedApps: Set<String>,
        private val onAppClick: (ApplicationInfo, Boolean) -> Unit
    ) : RecyclerView.Adapter<AppsAdapter.AppViewHolder>() {

        inner class AppViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val appIcon: ImageView = view.findViewById(R.id.appIcon)
            val appName: TextView = view.findViewById(R.id.appName)
            val lockStatus: TextView = view.findViewById(R.id.lockStatus)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_app, parent, false)
            return AppViewHolder(view)
        }

        override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
            val app = apps[position]
            val isLocked = lockedApps.contains(app.packageName)

            holder.appName.text = app.loadLabel(packageManager)
            holder.appIcon.setImageDrawable(app.loadIcon(packageManager))
            holder.lockStatus.text = if (isLocked) "Locked" else "Unlocked"

            holder.itemView.setOnClickListener {
                onAppClick(app, isLocked)
            }
        }

        override fun getItemCount() = apps.size

        fun updateLockedApps(newLockedApps: Set<String>) {
            lockedApps = newLockedApps
            notifyDataSetChanged()
        }
    }
}