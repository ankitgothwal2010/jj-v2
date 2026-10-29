/*
 * Copyright ©️ 2026 Ankit Gothwal.
 * All rights reserved, subject to applicable third-party license terms.
 *
 * Project: JARVIS AI
 * Developer: Ankit Gothwal
 * Application ID: com.ankit.jarvis
 */
package com.ankit.jarvis

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Custom original JARVIS-inspired futuristic circular HUD Voice Visualizer View.
 * Created and owned by Ankit Gothwal.
 *
 * Renders concentric electric cyan (#00E5FF) and deep cobalt blue (#0077FF) segmented tech rings,
 * a pulsing core reactor ring that responds to microphone RMS audio levels or TTS speaking state,
 * and smooth 60fps hardware-accelerated canvas rotation.
 */
class HudVoiceVisualizerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    enum class HudMode { IDLE, LISTENING, SPEAKING }

    private var hudMode: HudMode = HudMode.IDLE
    private var audioAmplitude: Float = 0.1f
    private var rotationAngle: Float = 0f

    private var primaryColor: Int = Color.parseColor("#00E5FF")
    private var secondaryColor: Int = Color.parseColor("#0077FF")
    private var coreGlowColor: Int = Color.parseColor("#3300E5FF")

    private val outerRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        color = primaryColor
        pathEffect = DashPathEffect(floatArrayOf(28f, 14f, 8f, 14f), 0f)
    }

    private val middleArcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 8f
        strokeCap = Paint.Cap.ROUND
        color = secondaryColor
    }

    private val innerTickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = primaryColor
        alpha = 180
    }

    private val coreFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = coreGlowColor
    }

    private val coreBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
        color = primaryColor
    }

    private val arcRect = RectF()

    private val animator = ValueAnimator.ofFloat(0f, 360f).apply {
        duration = 12000L
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            rotationAngle = it.animatedValue as Float
            invalidate()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!animator.isStarted) {
            animator.start()
        }
    }

    override fun onDetachedFromWindow() {
        animator.cancel()
        super.onDetachedFromWindow()
    }

    fun setHudMode(mode: HudMode) {
        hudMode = mode
        if (mode == HudMode.IDLE) {
            audioAmplitude = 0.08f
        }
        invalidate()
    }

    fun updateAudioLevel(normalizedLevel: Float) {
        audioAmplitude = normalizedLevel.coerceIn(0.05f, 1.0f)
        invalidate()
    }

    fun setAccentPalette(accentType: String) {
        when (accentType) {
            "BLUE" -> {
                primaryColor = Color.parseColor("#38BDF8")
                secondaryColor = Color.parseColor("#2563EB")
                coreGlowColor = Color.parseColor("#3338BDF8")
            }
            "HIGH_CONTRAST" -> {
                primaryColor = Color.parseColor("#00FFFF")
                secondaryColor = Color.parseColor("#FFFFFF")
                coreGlowColor = Color.parseColor("#4400FFFF")
            }
            else -> {
                primaryColor = Color.parseColor("#00E5FF")
                secondaryColor = Color.parseColor("#0077FF")
                coreGlowColor = Color.parseColor("#3300E5FF")
            }
        }
        outerRingPaint.color = primaryColor
        middleArcPaint.color = secondaryColor
        innerTickPaint.color = primaryColor
        coreFillPaint.color = coreGlowColor
        coreBorderPaint.color = primaryColor
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val cy = height / 2f
        val maxRadius = min(cx, cy) * 0.88f

        val speedMultiplier = when (hudMode) {
            HudMode.IDLE -> 1.0f
            HudMode.LISTENING -> 2.4f
            HudMode.SPEAKING -> 1.8f
        }

        // 1. Outer Rotating Dashed Telemetry Ring
        canvas.save()
        canvas.rotate(rotationAngle * speedMultiplier, cx, cy)
        canvas.drawCircle(cx, cy, maxRadius, outerRingPaint)
        canvas.restore()

        // 2. Counter-Rotating Segmented HUD Arcs
        val midRadius = maxRadius * 0.76f
        arcRect.set(cx - midRadius, cy - midRadius, cx + midRadius, cy + midRadius)
        canvas.save()
        canvas.rotate(-rotationAngle * 1.4f * speedMultiplier, cx, cy)
        for (i in 0 until 4) {
            canvas.drawArc(arcRect, i * 90f + 12f, 56f, false, middleArcPaint)
        }
        canvas.restore()

        // 3. Radial Tick Marks
        val tickInner = maxRadius * 0.58f
        val tickOuter = maxRadius * 0.65f
        for (deg in 0 until 360 step 15) {
            val rad = Math.toRadians((deg + rotationAngle * 0.5f).toDouble())
            val startX = cx + (tickInner * cos(rad)).toFloat()
            val startY = cy + (tickInner * sin(rad)).toFloat()
            val stopX = cx + (tickOuter * cos(rad)).toFloat()
            val stopY = cy + (tickOuter * sin(rad)).toFloat()
            canvas.drawLine(startX, startY, stopX, stopY, innerTickPaint)
        }

        // 4. Dynamic Voice-Reactive Core Pulse
        val pulseBoost = when (hudMode) {
            HudMode.IDLE -> 0.04f * sin(Math.toRadians((rotationAngle * 4).toDouble())).toFloat()
            HudMode.LISTENING -> audioAmplitude * 0.22f
            HudMode.SPEAKING -> 0.14f * kotlin.math.abs(sin(Math.toRadians((rotationAngle * 8).toDouble())).toFloat())
        }

        val coreRadius = maxRadius * (0.34f + pulseBoost)
        canvas.drawCircle(cx, cy, coreRadius * 1.18f, coreFillPaint)
        canvas.drawCircle(cx, cy, coreRadius, coreBorderPaint)
    }
}
