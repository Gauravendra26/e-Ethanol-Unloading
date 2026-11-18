package com.visibleinfotech.e_ethanolUnloading.gateEntryNew

import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.widget.AppCompatImageView

class ZoomableImageView(context: Context, attrs: AttributeSet? = null) : AppCompatImageView(context, attrs) {

    private val matrix = Matrix()
    private val savedMatrix = Matrix()
    private val startPoint = PointF()
    private val midPoint = PointF()
    private var oldDist = 1f
    private val matrixValues = FloatArray(9)

    private var mode = NONE
    private var isZoomEnabled = true
    private var minScale = 1f
    private var maxScale = 5f
    private var currentScale = 1f

    private val scaleDetector = ScaleGestureDetector(context, ScaleListener())
    private var imageClickListener: OnImageClickListener? = null

    private val imageRect = RectF()
    private var imageWidth = 0f
    private var imageHeight = 0f

    interface OnImageClickListener {
        fun onImageClick()
    }

    init {
        setScaleType(ScaleType.MATRIX)
        setImageMatrix(matrix)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateImageRect()
    }

    private fun updateImageRect() {
        drawable?.let { drawable ->
            val drawableWidth = drawable.intrinsicWidth
            val drawableHeight = drawable.intrinsicHeight
            val viewWidth = width
            val viewHeight = height

            if (drawableWidth <= 0 || drawableHeight <= 0 || viewWidth <= 0 || viewHeight <= 0) {
                return
            }

            val scaleX = viewWidth.toFloat() / drawableWidth
            val scaleY = viewHeight.toFloat() / drawableHeight
            val scale = minOf(scaleX, scaleY)

            imageWidth = drawableWidth * scale
            imageHeight = drawableHeight * scale

            val dx = (viewWidth - imageWidth) / 2
            val dy = (viewHeight - imageHeight) / 2

            imageRect.set(dx, dy, dx + imageWidth, dy + imageHeight)
        }
    }

    private fun isPointOnImage(x: Float, y: Float): Boolean {
        return imageRect.contains(x, y)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isZoomEnabled) {
            return super.onTouchEvent(event)
        }

        scaleDetector.onTouchEvent(event)

        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                savedMatrix.set(matrix)
                startPoint.set(event.x, event.y)
                mode = DRAG
            }

            MotionEvent.ACTION_UP -> {
                if (mode == NONE || mode == DRAG) {
                    val dx = event.x - startPoint.x
                    val dy = event.y - startPoint.y
                    val distance = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()

                    if (distance < 10) {
                        val imageCoords = getImageCoordinates(event.x, event.y)
                        if (imageCoords != null && imageClickListener != null) {
                            if (currentScale <= 1.1f) {
                                imageClickListener?.onImageClick()
                            } else {
                                resetZoom()
                            }
                        }
                    }
                }
                mode = NONE
            }

            MotionEvent.ACTION_POINTER_UP -> {
                mode = NONE
            }

            MotionEvent.ACTION_MOVE -> {
                if (mode == DRAG) {
                    val dx = event.x - startPoint.x
                    val dy = event.y - startPoint.y
                    matrix.set(savedMatrix)
                    matrix.postTranslate(dx, dy)
                    setImageMatrix(matrix)
                }
            }
        }

        return true
    }

    private fun getImageCoordinates(viewX: Float, viewY: Float): FloatArray? {
        drawable ?: return null

        val inverseMatrix = Matrix()
        if (!matrix.invert(inverseMatrix)) {
            return null
        }

        val imageCoords = floatArrayOf(viewX, viewY)
        inverseMatrix.mapPoints(imageCoords)

        val drawableWidth = drawable.intrinsicWidth
        val drawableHeight = drawable.intrinsicHeight

        if (imageCoords[0] < 0 || imageCoords[0] > drawableWidth ||
            imageCoords[1] < 0 || imageCoords[1] > drawableHeight) {
            return null
        }

        return imageCoords
    }

    inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            var scaleFactor = detector.scaleFactor
            val newScale = currentScale * scaleFactor

            when {
                newScale < minScale -> {
                    scaleFactor = minScale / currentScale
                }
                newScale > maxScale -> {
                    scaleFactor = maxScale / currentScale
                }
            }

            currentScale *= scaleFactor
            matrix.set(savedMatrix)
            matrix.postScale(scaleFactor, scaleFactor, detector.focusX, detector.focusY)
            setImageMatrix(matrix)
            savedMatrix.set(matrix)

            return true
        }

        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            savedMatrix.set(matrix)
            return true
        }
    }

    fun setZoomEnabled(enabled: Boolean) {
        isZoomEnabled = enabled
    }

    fun setMinScale(scale: Float) {
        minScale = scale
    }

    fun setMaxScale(scale: Float) {
        maxScale = scale
    }

    fun resetZoom() {
        matrix.reset()
        currentScale = 1f
        setImageMatrix(matrix)
        savedMatrix.set(matrix)
        updateImageRect()
    }

    fun getCurrentScale(): Float = currentScale

    fun setOnImageClickListener(listener: OnImageClickListener) {
        imageClickListener = listener
    }

    override fun setImageMatrix(matrix: Matrix) {
        super.setImageMatrix(matrix)
        updateImageRect()
    }

    companion object {
        private const val NONE = 0
        private const val DRAG = 1
        private const val ZOOM = 2
    }
}