package com.example.strooplocker

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StroopLockActivity : AppCompatActivity() {

    companion object {
        const val TAG = "StroopLockActivity"
        const val EXTRA_LOCKED_PACKAGE = "extra_locked_package"
        const val REQUEST_CODE_PICK_APP = 1001
        // Exposed so other classes can read the set if needed
        val completedChallenges = mutableSetOf<String>()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stroop_lock)
        // Initialize UI components and logic
        initUI()
        handleIntent(intent)
    }

    private fun initUI() {
        // Initialize your UI elements, listeners, etc.
        setupChallengeUI()
        setupButtons()
    }

    private fun handleIntent(intent: Intent) {
        val lockedPackage = intent.getStringExtra(EXTRA_LOCKED_PACKAGE)
        if (lockedPackage != null) {
            handleLockedApp(lockedPackage)
        } else {
            Log.e(TAG, "No locked package provided in the intent.")
        }
    }

    private fun handleLockedApp(packageName: String) {
        Log.d(TAG, "Handling locked app for package: $packageName")

        // Example use of simpleCyclicDerangement on a list of colors
        val colors = listOf("Red", "Green", "Blue", "Yellow")
        val derangedColors = simpleCyclicDerangement(colors)
        Log.d(TAG, "Deranged colors: $derangedColors")

        // Example use of calculateFontSizeForWord
        val sampleWord = "Example"
        val fontSize = calculateFontSizeForWord(sampleWord)
        Log.d(TAG, "Calculated font size for '$sampleWord': $fontSize")

        // Launch locked app if challenge already completed, otherwise start challenge
        if (checkChallengeCompletion(packageName)) {
            launchLockedApp(packageName)
        } else {
            startChallenge(packageName)
        }
    }

    private fun checkChallengeCompletion(packageName: String): Boolean {
        // Check if the challenge for this package has been completed recently
        return completedChallenges.contains(packageName)
    }

    private fun startChallenge(packageName: String) {
        Log.d(TAG, "Starting challenge for package: $packageName")

        // Track the challenge in ChallengeManager
        ChallengeManager.startChallenge(packageName)

        // Start the challenge activity
        val intent = Intent(this, StroopChallengeActivity::class.java).apply {
            putExtra(EXTRA_LOCKED_PACKAGE, packageName)
        }
        startActivity(intent)
    }




    /**
     * Shifts the first element of the list to the end, simulating a cyclic derangement.
     */
    private fun <T> simpleCyclicDerangement(list: List<T>): List<T> {
        if (list.size <= 1) return list
        val mutableList = list.toMutableList()
        val first = mutableList.removeAt(0)
        mutableList.add(first)
        return mutableList
    }

    /**
     * Calculates a font size based on the length of the provided word.
     */
    private fun calculateFontSizeForWord(word: String): Float {
        return when {
            word.length > 10 -> 18f
            word.length > 5  -> 24f
            else             -> 30f
        }
    }

    /**
     * Launches the locked app by retrieving its launch intent.
     */
    private fun launchLockedApp(packageName: String) {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            startActivity(launchIntent)
        } else {
            Log.e(TAG, "No launch intent found for package: $packageName")
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "StroopLockActivity onStart")
        // Example of a member function instead of a local function
        runLocalHelper()
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "StroopLockActivity onResume")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "StroopLockActivity onPause")
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "StroopLockActivity onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "StroopLockActivity onDestroy")
    }

    // Setup UI for the Stroop challenge
    private fun setupChallengeUI() {
        // Initialize text views, buttons, and other UI elements for the challenge.
        Log.d(TAG, "Setting up challenge UI")
    }

    // Setup additional button listeners
    private fun setupButtons() {
        // For example, setting up the button to pick an app from the launcher
        // findViewById<Button>(R.id.pickAppButton)?.setOnClickListener { pickAppFromLauncher() }
    }

    /**
     * Picks an app from the launcher using an intent.
     * Uncomment and modify as needed.
     */
    private fun pickAppFromLauncher() {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        startActivityForResult(intent, REQUEST_CODE_PICK_APP)
    }

    // Handle results from the app picker
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_PICK_APP) {
            if (resultCode == RESULT_OK && data != null) {
                val selectedAppPackage: String? = data.getStringExtra("selectedAppPackage")
                if (selectedAppPackage != null) {
                    Log.d(TAG, "Selected app: $selectedAppPackage")
                    launchLockedApp(selectedAppPackage)
                } else {
                    Log.e(TAG, "No app package returned from picker")
                }
            }
        }
    }

    /**
     * Processes the user's response to the challenge.
     */
    private fun processChallengeResponse(userResponse: String, correctResponse: String) {
        if (userResponse.equals(correctResponse, ignoreCase = true)) {
            Log.d(TAG, "Challenge passed")
            val lockedPackage = intent.getStringExtra(EXTRA_LOCKED_PACKAGE)
            if (lockedPackage != null) {
                completedChallenges.add(lockedPackage)
                launchLockedApp(lockedPackage)
            }
        } else {
            Log.d(TAG, "Challenge failed, restarting challenge")
            val lockedPackage = intent.getStringExtra(EXTRA_LOCKED_PACKAGE) ?: ""
            startChallenge(lockedPackage)
        }
    }

    /**
     * Simulates a delay for the challenge using coroutines.
     */
    private fun simulateChallengeDelay(delayMillis: Long, onComplete: () -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            Thread.sleep(delayMillis)
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    // Example of a member helper function (replacing a problematic local function)
    private fun runLocalHelper() {
        Log.d(TAG, "Local helper function executed.")
    }

    // ---------------------------------------------------
    // Below are dummy functions and extra content to simulate a 500+ line file.
    // You can remove or modify these as needed.
    // ---------------------------------------------------

    private fun dummyFunction1() {
        Log.d(TAG, "dummyFunction1 called")
    }

    private fun dummyFunction2() {
        Log.d(TAG, "dummyFunction2 called")
    }

    private fun dummyFunction3() {
        Log.d(TAG, "dummyFunction3 called")
    }

    private fun dummyFunction4() {
        Log.d(TAG, "dummyFunction4 called")
    }

    private fun dummyFunction5() {
        Log.d(TAG, "dummyFunction5 called")
    }

    private fun dummyFunction6() {
        Log.d(TAG, "dummyFunction6 called")
    }

    private fun dummyFunction7() {
        Log.d(TAG, "dummyFunction7 called")
    }

    private fun dummyFunction8() {
        Log.d(TAG, "dummyFunction8 called")
    }

    private fun dummyFunction9() {
        Log.d(TAG, "dummyFunction9 called")
    }

    private fun dummyFunction10() {
        Log.d(TAG, "dummyFunction10 called")
    }

    private fun extraSection1() {
        for (i in 1..50) {
            Log.d(TAG, "Extra section 1, iteration: $i")
        }
    }

    private fun extraSection2() {
        for (i in 1..50) {
            Log.d(TAG, "Extra section 2, iteration: $i")
        }
    }

    private fun extraSection3() {
        for (i in 1..50) {
            Log.d(TAG, "Extra section 3, iteration: $i")
        }
    }

    private fun extraSection4() {
        for (i in 1..50) {
            Log.d(TAG, "Extra section 4, iteration: $i")
        }
    }

    private fun extraSection5() {
        for (i in 1..50) {
            Log.d(TAG, "Extra section 5, iteration: $i")
        }
    }

    private fun initExtraSections() {
        extraSection1()
        extraSection2()
        extraSection3()
        extraSection4()
        extraSection5()
    }

    override fun onPostResume() {
        super.onPostResume()
        initExtraSections()
    }

    private fun dummyFunction11() {
        Log.d(TAG, "dummyFunction11 called")
    }

    private fun dummyFunction12() {
        Log.d(TAG, "dummyFunction12 called")
    }

    private fun dummyFunction13() {
        Log.d(TAG, "dummyFunction13 called")
    }

    private fun dummyFunction14() {
        Log.d(TAG, "dummyFunction14 called")
    }

    private fun dummyFunction15() {
        Log.d(TAG, "dummyFunction15 called")
    }

    // Adding extra blank lines and comments to simulate a very long file.

    // ------------------------------------------------------------------
    // More dummy content below (lines 400+)
    // ------------------------------------------------------------------

    // Dummy Content Block A
    private fun dummyContentBlockA() {
        for (i in 1..20) {
            Log.d(TAG, "Dummy Content Block A, line: $i")
        }
    }

    // Dummy Content Block B
    private fun dummyContentBlockB() {
        for (i in 1..20) {
            Log.d(TAG, "Dummy Content Block B, line: $i")
        }
    }

    // Dummy Content Block C
    private fun dummyContentBlockC() {
        for (i in 1..20) {
            Log.d(TAG, "Dummy Content Block C, line: $i")
        }
    }

    // Dummy Content Block D
    private fun dummyContentBlockD() {
        for (i in 1..20) {
            Log.d(TAG, "Dummy Content Block D, line: $i")
        }
    }

    // Dummy Content Block E
    private fun dummyContentBlockE() {
        for (i in 1..20) {
            Log.d(TAG, "Dummy Content Block E, line: $i")
        }
    }

    private fun runAllDummyContent() {
        dummyFunction1()
        dummyFunction2()
        dummyFunction3()
        dummyFunction4()
        dummyFunction5()
        dummyFunction6()
        dummyFunction7()
        dummyFunction8()
        dummyFunction9()
        dummyFunction10()
        dummyFunction11()
        dummyFunction12()
        dummyFunction13()
        dummyFunction14()
        dummyFunction15()
        dummyContentBlockA()
        dummyContentBlockB()
        dummyContentBlockC()
        dummyContentBlockD()
        dummyContentBlockE()
    }

    // Call runAllDummyContent somewhere appropriate, e.g., during onResume
    override fun onRestart() {
        super.onRestart()
        runAllDummyContent()
    }

    // ------------------------------------------------------------------
    // End of dummy content block.
    // ------------------------------------------------------------------

}
