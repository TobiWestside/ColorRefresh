package de.tobiasfraenzel.colorrefresh

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.android.synthetic.main.activity_fullscreen.*
import kotlin.random.Random

/**
 * Shows a random color in a full-screen activity that shows and
 * hides the system UI (i.e. status bar and navigation/system bar)
 * with user interaction.
 */
class FullscreenActivity : AppCompatActivity() {
    private val LOG_TAG = "ColorRefresh"
    private val mHideHandler = Handler()
    private val mHidePart2Runnable = Runnable {
        // Delayed removal of status and navigation bar

        // Some of these constants are new as of API 16 (Jelly Bean)
        // and API 19 (KitKat). It is safe to use them, as they are inlined
        // at compile-time and do nothing on earlier devices.
        fullscreen_content.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LOW_PROFILE or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
    }
    private val mShowPart2Runnable = Runnable {
        // Delayed display of UI elements
        fullscreen_content_controls.visibility = View.VISIBLE
    }
    private var textVisible: Boolean = false
    private val mHideRunnable = Runnable { hide() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set layout
        setContentView(R.layout.activity_fullscreen)

        // Don't show color codes at the start
        hideText()

        // Set new background color
        val refreshLayout: SwipeRefreshLayout = findViewById(R.id.swipeContainer)
        updateBackgroundColor(refreshLayout)

         // Invoked when the user performs a swipe-to-refresh gesture.
        refreshLayout.setOnRefreshListener {
            Log.d(LOG_TAG, "onRefresh called from SwipeRefreshLayout")
            updateBackgroundColor(refreshLayout)
        }

        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent?): Boolean {
                if (textVisible) {
                    hideText()
                } else {
                    showText()
                }
                Log.d(LOG_TAG, "RefreshLayout tapped")
                return super.onSingleTapUp(e)
            }
        })
        refreshLayout.setOnTouchListener { layout, event -> gestureDetector.onTouchEvent(event) }

    }

    /**
     * Sets a new random color as the background color of the SwipeRefreshLayout
     * and adapts the text color to the brightness of the color
     */
    private fun updateBackgroundColor(refreshLayout: SwipeRefreshLayout) {
        val colorValues = getRandomColor()
        val hexColor = String.format("#%06X", 0xFFFFFF and colorValues[0])
        val rgbColor = "${colorValues[1]}, ${colorValues[2]}, ${colorValues[3]}"

        // Threshold for toggling text color
        val threshold = 127 // medium luminance

        // Calculate luminance based on approximation of formula according to ITU BT.601
        // https://www.itu.int/rec/R-REC-BT.601
        // Approximation: luminance = 0.3 R + 0.5 G + 0.16 B
        val isDark:Boolean = (colorValues[1] + colorValues[1]
                            + colorValues[2] + colorValues[2] + colorValues[2]
                            + colorValues[3]) / 6 < threshold

        // Set the background color and update the text
        refreshLayout.setBackgroundColor(colorValues[0])
        val hexColorView: TextView = findViewById(R.id.hexColor)
        hexColorView.text = hexColor
        val rgbColorView: TextView = findViewById(R.id.rgbColor)
        rgbColorView.text = rgbColor

        // Set text color to white on dark colors (low luminance)
        // and to black on bright colors (high luminance)
        if (isDark) {
            hexColorView.setTextColor(Color.WHITE)
            rgbColorView.setTextColor(Color.WHITE)
        } else {
            hexColorView.setTextColor(Color.BLACK)
            rgbColorView.setTextColor(Color.BLACK)
        }

        refreshLayout.isRefreshing = false
    }

    /**
     * Get a new random color
     * @return Int array, where [0] is the Color value and
     * the R, G, and B values are in [1], [2], [3]
     */
    fun getRandomColor(): IntArray {
        val red:Int = Random.nextInt(256)
        val green:Int = Random.nextInt(256)
        val blue:Int = Random.nextInt(256)
        return intArrayOf(Color.argb(255, red, green, blue), red, green, blue)

    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100)
    }

    /**
     * Hides the TextViews that contain the color codes
     */
    private fun hideText() {
        val rgbView: TextView = findViewById(R.id.rgbColor)
        val hexView: TextView = findViewById(R.id.hexColor)
        rgbView.visibility = View.INVISIBLE
        hexView.visibility = View.INVISIBLE
        textVisible = false
    }

    /**
     * Shows the TextViews that contain the color codes
     */
    private fun showText() {
        val rgbView: TextView = findViewById(R.id.rgbColor)
        val hexView: TextView = findViewById(R.id.hexColor)
        rgbView.visibility = View.VISIBLE
        hexView.visibility = View.VISIBLE
        textVisible = true
    }

    private fun hide() {
        Log.i(LOG_TAG, "hide called")
        // Hide UI first
        fullscreen_content_controls.visibility = View.GONE

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable)
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY.toLong())
    }

    /**
     * Schedules a call to hide() in [delayMillis], canceling any
     * previously scheduled calls.
     */
    private fun delayedHide(delayMillis: Int) {
        Log.i(LOG_TAG, "delayedHide called")
        mHideHandler.removeCallbacks(mHideRunnable)
        mHideHandler.postDelayed(mHideRunnable, delayMillis.toLong())
    }

    companion object {
        /**
         * Whether or not the system UI should be auto-hidden after
         * [AUTO_HIDE_DELAY_MILLIS] milliseconds.
         */
        private val AUTO_HIDE = true

        /**
         * If [AUTO_HIDE] is set, the number of milliseconds to wait after
         * user interaction before hiding the system UI.
         */
        private val AUTO_HIDE_DELAY_MILLIS = 3000

        /**
         * Some older devices needs a small delay between UI widget updates
         * and a change of the status and navigation bar.
         */
        private val UI_ANIMATION_DELAY = 300
    }
}
