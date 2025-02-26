package com.example.strooplocker

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
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

    private lateinit var recyclerView: RecyclerView
    private lateinit var appsAdapter: AppsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_apps)

        recyclerView = findViewById(R.id.appsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        loadInstalledApps()
    }

    private fun loadInstalledApps() {
        lifecycleScope.launch {
            val apps = withContext(Dispatchers.IO) {
                // Get installed apps
                val pm = packageManager
                val flags = PackageManager.GET_META_DATA

                pm.getInstalledApplications(flags)
                    .filter { app ->
                        // Only show launchable apps with launcher activities
                        pm.getLaunchIntentForPackage(app.packageName) != null &&
                                // Filter out system apps
                                (app.flags and ApplicationInfo.FLAG_SYSTEM) == 0
                    }
                    .sortedBy { app -> app.loadLabel(pm).toString() }
            }

            // Check which apps are already locked
            val lockedApps = withContext(Dispatchers.IO) {
                LockManager.getLockedApps(this@SelectAppsActivity)
            }

            appsAdapter = AppsAdapter(apps, lockedApps.toSet()) { app, isLocked ->
                onAppSelected(app, isLocked)
            }
            recyclerView.adapter = appsAdapter
        }
    }

    private fun onAppSelected(app: ApplicationInfo, isCurrentlyLocked: Boolean) {
        lifecycleScope.launch {
            if (isCurrentlyLocked) {
                // Unlock the app
                LockManager.removeLockedApp(this@SelectAppsActivity, app.packageName)
                Toast.makeText(this@SelectAppsActivity,
                    "Unlocked: ${app.loadLabel(packageManager)}",
                    Toast.LENGTH_SHORT).show()
            } else {
                // Lock the app
                LockManager.addLockedApp(this@SelectAppsActivity, app.packageName)
                Toast.makeText(this@SelectAppsActivity,
                    "Locked: ${app.loadLabel(packageManager)}",
                    Toast.LENGTH_SHORT).show()
            }

            // Refresh the list
            val updatedLockedApps = withContext(Dispatchers.IO) {
                LockManager.getLockedApps(this@SelectAppsActivity)
            }
            appsAdapter.updateLockedApps(updatedLockedApps.toSet())
        }
    }

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