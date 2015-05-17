/*
 * Copyright (C) 2008 ZXing authors
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

package com.Zxing.Demo.view;

import java.util.Collection;
import java.util.HashSet;

import net.app.netdatatransfer.R;
import net.app.netdatatransfer.R.color;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import com.Zxing.Demo.camera.CameraManager;
import com.google.zxing.ResultPoint;

/**
 * This view is overlaid on top of the camera preview. It adds the viewfinder
 * rectangle and partial transparency outside it, as well as the laser scanner
 * animation and result points.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class ViewfinderView extends View {

    private static final int[] SCANNER_ALPHA = { 0, 64, 128, 192, 255, 192,
            128, 64 };
    private static final long ANIMATION_DELAY = 100L;
    private static final int OPAQUE = 0xFF;

    private final Paint paint;
    private Bitmap resultBitmap;
    private final int maskColor;
    private final int resultColor;
    private final int frameColor;
    private final int laserColor;
    private final int resultPointColor;
    private int scannerAlpha;
    private Collection<ResultPoint> possibleResultPoints;
    private Collection<ResultPoint> lastPossibleResultPoints;
    private final int cornerLength;
    private boolean isFirst = true;

    private static final int CORNER_WIDTH = 6;// 四个绿色边角对应的宽度
    private int slideTop; // 中间滑动线的最顶端位置
    private int slideBottom; // 中间滑动线的最底端位置
    private static final int MIDDLE_LINE_WIDTH = 6; // 扫描框中的中间线的宽度
    private static final int MIDDLE_LINE_PADDING = 5; // 扫描框中的中间线的与扫描框左右的间隙
    private static final int SPEEN_DISTANCE = 25; // 中间那条线每次刷新移动的距离

    // This constructor is used when the class is built from an XML resource.
    public ViewfinderView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Initialize these once for performance rather than calling them every
        // time in onDraw().
        paint = new Paint();
        Resources resources = getResources();
        maskColor = resources.getColor(R.color.viewfinder_mask);
        resultColor = resources.getColor(R.color.result_view);
        frameColor = resources.getColor(R.color.viewfinder_frame);
        laserColor = resources.getColor(R.color.viewfinder_laser);
        resultPointColor = resources.getColor(R.color.possible_result_points);
        scannerAlpha = 0;
        possibleResultPoints = new HashSet<ResultPoint>(5);
        cornerLength = (int) resources.getDimension(R.dimen.corner_length);
    }

    @Override
    public void onDraw(Canvas canvas) {
        Rect frame = CameraManager.get().getFramingRect();
        if (frame == null) {
            return;
        }
        int width = canvas.getWidth();
        int height = canvas.getHeight();

        if (isFirst) {
            isFirst = false;
            slideTop = frame.top;
            slideBottom = frame.bottom;
        }

        // Draw the exterior (i.e. outside the framing rect) darkened
        paint.setColor(resultBitmap != null ? resultColor : maskColor);
        canvas.drawRect(0, 0, width, frame.top, paint);
        canvas.drawRect(0, frame.top, frame.left, frame.bottom + 1, paint);
        canvas.drawRect(frame.right + 1, frame.top, width, frame.bottom + 1,
                paint);
        canvas.drawRect(0, frame.bottom + 1, width, height, paint);

        if (resultBitmap != null) {
            // Draw the opaque result bitmap over the scanning rectangle
            paint.setAlpha(OPAQUE);
            canvas.drawBitmap(resultBitmap, frame.left, frame.top, paint);
        } else {

            // Draw a two pixel solid black border inside the framing rect
            paint.setColor(frameColor);
            canvas.drawRect(frame.left, frame.top, frame.right + 1,
                    frame.top + 2, paint);
            canvas.drawRect(frame.left, frame.top + 2, frame.left + 2,
                    frame.bottom - 1, paint);
            canvas.drawRect(frame.right - 1, frame.top, frame.right + 1,
                    frame.bottom - 1, paint);
            canvas.drawRect(frame.left, frame.bottom - 1, frame.right + 1,
                    frame.bottom + 1, paint);

            // Draw four green corner
            paint.setColor(Color.GREEN);
            canvas.drawRect(frame.left + 1, frame.top + 1, frame.left + 1
                    + cornerLength, frame.top + 1 + CORNER_WIDTH, paint);
            canvas.drawRect(frame.right - cornerLength, frame.top + 1,
                    frame.right, frame.top + 1 + CORNER_WIDTH, paint);
            canvas.drawRect(frame.left + 1, frame.bottom - CORNER_WIDTH,
                    frame.left + 1 + cornerLength, frame.bottom, paint);
            canvas.drawRect(frame.right - cornerLength, frame.bottom
                    - CORNER_WIDTH, frame.right, frame.bottom, paint);
            canvas.drawRect(frame.left + 1, frame.top + 1, frame.left + 1
                    + CORNER_WIDTH, frame.top + 1 + cornerLength, paint);
            canvas.drawRect(frame.right - CORNER_WIDTH, frame.top + 1,
                    frame.right, frame.top + 1 + cornerLength, paint);
            canvas.drawRect(frame.left + 1, frame.bottom - cornerLength,
                    frame.left + 1 + CORNER_WIDTH, frame.bottom, paint);
            canvas.drawRect(frame.right - CORNER_WIDTH, frame.bottom
                    - cornerLength, frame.right, frame.bottom, paint);

            // Draw a red "laser scanner" line through the middle to show
            // decoding is active
            paint.setColor(laserColor);
            paint.setAlpha(SCANNER_ALPHA[scannerAlpha]);
            scannerAlpha = (scannerAlpha + 1) % SCANNER_ALPHA.length;
            slideTop += SPEEN_DISTANCE;
            if (slideTop >= frame.bottom) {
                slideTop = frame.top;
            }
            canvas.drawRect(frame.left + MIDDLE_LINE_PADDING, slideTop
                    - MIDDLE_LINE_WIDTH / 2, frame.right - MIDDLE_LINE_PADDING,
                    slideTop + MIDDLE_LINE_WIDTH / 2, paint);

            Collection<ResultPoint> currentPossible = possibleResultPoints;
            Collection<ResultPoint> currentLast = lastPossibleResultPoints;
            if (currentPossible.isEmpty()) {
                lastPossibleResultPoints = null;
            } else {
                possibleResultPoints = new HashSet<ResultPoint>(5);
                lastPossibleResultPoints = currentPossible;
                paint.setAlpha(OPAQUE);
                paint.setColor(resultPointColor);
                for (ResultPoint point : currentPossible) {
                    canvas.drawCircle(frame.left + point.getX(), frame.top
                            + point.getY(), 6.0f, paint);
                }
            }
            if (currentLast != null) {
                paint.setAlpha(OPAQUE / 2);
                paint.setColor(resultPointColor);
                for (ResultPoint point : currentLast) {
                    canvas.drawCircle(frame.left + point.getX(), frame.top
                            + point.getY(), 3.0f, paint);
                }
            }

            // Request another update at the animation interval, but only
            // repaint the laser line,
            // not the entire viewfinder mask.
            postInvalidateDelayed(ANIMATION_DELAY, frame.left, frame.top,
                    frame.right, frame.bottom);
        }
    }

    public void drawViewfinder() {
        resultBitmap = null;
        invalidate();
    }

    /**
     * Draw a bitmap with the result points highlighted instead of the live
     * scanning display.
     *
     * @param barcode
     *            An image of the decoded barcode.
     */
    public void drawResultBitmap(Bitmap barcode) {
        resultBitmap = barcode;
        invalidate();
    }

    public void addPossibleResultPoint(ResultPoint point) {
        possibleResultPoints.add(point);
    }

}
