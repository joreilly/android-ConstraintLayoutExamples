/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.androidstudio.motionlayoutexample

import android.os.Build
import android.os.Bundle
import android.support.annotation.IdRes
import android.support.annotation.RequiresApi
import android.support.constraint.ConstraintSet
import android.support.constraint.motion.MotionLayout
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.ImageView

@RequiresApi(Build.VERSION_CODES.LOLLIPOP) // for View#clipToOutline
class DemoActivity: AppCompatActivity() {

    private lateinit var container: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = intent.getIntExtra("layout_file_id", R.layout.motion_01_basic)
        setContentView(layout)
        container = findViewById(R.id.motionLayout)

        if (layout == R.layout.motion_11_coordinatorlayout) {
            val icon = findViewById<ImageView>(R.id.icon)
            icon?.clipToOutline = true
        }

        val debugMode = if (intent.getBooleanExtra("showPaths", false)) {
            MotionLayout.DEBUG_SHOW_PATH
        } else {
            MotionLayout.DEBUG_SHOW_NONE
        }
        val motionLayout = container as MotionLayout
        motionLayout.setDebugMode(MotionLayout.DEBUG_SHOW_PROGRESS)

        val accumulativeListener = AccumulativeTransitionListener()
        motionLayout.setTransitionListener(accumulativeListener)


    }

    fun changeState(v: View?) {
        val motionLayout = container as? MotionLayout ?: return
        if (motionLayout.progress > 0.5f) {
            motionLayout.transitionToStart()
        } else {
            motionLayout.transitionToEnd()
        }
    }
}


open class TransitionListener: MotionLayout.TransitionListener {

    /**
     * Called when a transition has been completed
     */
    override fun onTransitionCompleted(view: MotionLayout, @IdRes constraintSetId: Int) = Unit

    /**
     * Called when a [KeyTrigger] is triggered
     */
    override fun onTransitionTrigger(view: MotionLayout, @IdRes triggerId: Int, isPositive: Boolean, progress: Float) = Unit

    /**
     * Called when a transition has been started
     */
    @Deprecated("This is not being called due to a bug in MotionLayout. This will be fixed in Alpha 4.")
    override fun onTransitionStarted(view: MotionLayout, @IdRes startConstraintSetId: Int, @IdRes endConstraintSetId: Int) = Unit

    /**
     * Called when the transition or its progress changes
     */
    override fun onTransitionChange(view: MotionLayout, @IdRes startConstraintSetId: Int, @IdRes endConstraintSetId: Int, progress: Float) = Unit
}


class AccumulativeTransitionListener: TransitionListener() {

    var didApplyConstraintSet = false

    override fun onTransitionChange(view: MotionLayout, @IdRes startConstraintSetId: Int, @IdRes endConstraintSetId: Int, progress: Float) {
        if (!didApplyConstraintSet) {
            // Let's retrieve our ConstraintSets first
            val startConstraintSet = view.getConstraintSet(startConstraintSetId)
            val endConstraintSet = view.getConstraintSet(endConstraintSetId)
            // Merge them (using an extension function)
            val mergedConstraintSet = startConstraintSet + endConstraintSet
            // Clear + Set them
            endConstraintSet.setConstraints(mergedConstraintSet)
            didApplyConstraintSet = true
        }
    }

    override fun onTransitionCompleted(view: MotionLayout, @IdRes constraintSetId: Int) {
        didApplyConstraintSet = false
    }

}


/**
 * Merges two [ConstraintSet]s. If a Constraint exists in the first [ConstraintSet] and the second one, the original value will be replaced by the new value.
 * This doesn't mutate the original ConstraintSet, but rather creates a copy.
 */
operator fun ConstraintSet.plus(other: ConstraintSet): ConstraintSet {
    return this.copy().apply { updateWith(other) }
}

/**
 * Clears the existing constraints and sets them to those of the [constraintSet]
 */
fun ConstraintSet.setConstraints(constraintSet: ConstraintSet) {
    val field = ConstraintSet::class.java.getDeclaredField("mConstraints")
    field.isAccessible = true

    val theseConstraints: HashMap<Int, Any> = field.get(this) as HashMap<Int, Any>
    val newConstraints: HashMap<Int, Any> = field.get(constraintSet) as HashMap<Int, Any>

    theseConstraints.clear()
    theseConstraints.putAll(newConstraints)
}

/**
 * Copies the [ConstraintSet] into a new [ConstraintSet]
 */
fun ConstraintSet.copy(): ConstraintSet = ConstraintSet().apply { clone(this@copy) }

/**
 * Clears the constraints of this [ConstraintSet] using reflection
 */
@Suppress("UNCHECKED_CAST")
fun ConstraintSet.clearConstraints() {
    val field = ConstraintSet::class.java.getDeclaredField("mConstraints")
    field.isAccessible = true

    val theseConstraints: HashMap<Int, Any> = field.get(this) as HashMap<Int, Any>
    theseConstraints.clear()
}

/**
 * Mutates the [ConstraintSet] with the values from the [other] [ConstraintSet]
 * @see https://github.com/tristanvda/ConstraintSet-UpdateWith
 */
@Suppress("UNCHECKED_CAST")
fun ConstraintSet.updateWith(other: ConstraintSet) {

    val field = ConstraintSet::class.java.getDeclaredField("mConstraints")
    field.isAccessible = true

    val theseConstraints: HashMap<Int, Any> = field.get(this) as HashMap<Int, Any>
    val newConstraints: HashMap<Int, Any> = field.get(other) as HashMap<Int, Any>

    theseConstraints.putAll(newConstraints)
}


