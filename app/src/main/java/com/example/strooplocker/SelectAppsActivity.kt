package com.example.strooplocker

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
            val apps = withContext(Dispatchers.IO) {
                // Get installed apps
                val pm = packageManager
                val flags = PackageManager.GET_META_DATA

                val installedApps = pm.getInstalledApplications(flags)
                    .filter { app ->
                        // Only show launchable apps with launcher activities
                        val launchIntent = pm.getLaunchIntentForPackage(app.packageName)
                        val isSystemApp = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0

                        Log.d(TAG, "App: ${app.packageName}")
                        Log.d(TAG, "Launchable: ${launchIntent != null}")
                        Log.d(TAG, "Is System App: $isSystemApp")

                        launchIntent != null && !isSystemApp
                    }
                    .sortedBy { app -> app.loadLabel(pm).toString() }

                Log.d(TAG, "Total launchable non-system apps: ${installedApps.size}")
                installedApps
            }

            // Check which apps are already locked
            val lockedApps = withContext(Dispatchers.IO) {
                Log.d(TAG, "Retrieving currently locked apps")
                LockManager.getLockedApps(this@SelectAppsActivity)
            }

            Log.d(TAG, "Currently locked apps:")
            lockedApps.forEachIndexed { index, app ->
                Log.d(TAG, "Locked App #$index: $app")
            }

            appsAdapter = AppsAdapter(apps, lockedApps.toSet()) { app, isLocked ->
                onAppSelected(app, isLocked)
            }
            recyclerView.adapter = appsAdapter
        }
    }

    private fun onAppSelected(app: ApplicationInfo, isCurrentlyLocked: Boolean) {
        lifecycleScope.launch {
            Log.d(TAG, "App selection changed")
            Log.d(TAG, "Package: ${app.packageName}")
            Log.d(TAG, "App Name: ${app.loadLabel(packageManager)}")
            Log.d(TAG, "Currently Locked: $isCurrentlyLocked")

            if (isCurrentlyLocked) {
                // Unlock the app
                LockManager.removeLockedApp(this@SelectAppsActivity, app.packageName)
                Log.i(TAG, "App unlocked: ${app.packageName}")
                Toast.makeText(this@SelectAppsActivity,
                    "Unlocked: ${app.loadLabel(packageManager)}",
                    Toast.LENGTH_SHORT).show()
            } else {
                // Lock the app
                LockManager.addLockedApp(this@SelectAppsActivity, app.packageName)
                Log.i(TAG, "App locked: ${app.packageName}")
                Toast.makeText(this@SelectAppsActivity,
                    "Locked: ${app.loadLabel(packageManager)}",
                    Toast.LENGTH_SHORT).show()
            }

            // Refresh the list
            val updatedLockedApps = withContext(Dispatchers.IO) {
                LockManager.getLockedApps(this@SelectAppsActivity)
            }

            Log.d(TAG, "Updated locked apps:")
            updatedLockedApps.forEachIndexed { index, app ->
                Log.d(TAG, "Locked App #$index: $app")
            }

            appsAdapter.updateLockedApps(updatedLockedApps.toSet())
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