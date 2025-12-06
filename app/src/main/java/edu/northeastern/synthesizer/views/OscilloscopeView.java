package edu.northeastern.synthesizer.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class OscilloscopeView extends View {

    // Buffer we draw from
    private float[] waveform = new float[512];

    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Object lock = new Object();

    public OscilloscopeView(Context context) {
        super(context);
        init();
    }

    public OscilloscopeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public OscilloscopeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        linePaint.setStrokeWidth(3f);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setColor(0xFF00FF00); // green

        axisPaint.setStrokeWidth(1f);
        axisPaint.setStyle(Paint.Style.STROKE);
        axisPaint.setColor(0x55FFFFFF); // semi-white
    }

    /**
     * Called from MainActivity to push new samples in.
     * The array is assumed to be mono, in range [-1, 1].
     */
    public void updateWaveform(float[] samples) {
        if (samples == null || samples.length == 0) return;
        synchronized (lock) {
            if (waveform.length != samples.length) {
                waveform = new float[samples.length];
            }
            System.arraycopy(samples, 0, waveform, 0, samples.length);
        }
        invalidate(); // schedule redraw
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float width = getWidth();
        float height = getHeight();
        if (width <= 0 || height <= 0) return;

        float centerY = height / 2f;
        float lastX = 0;
        float lastY = centerY;

        // Draw mid axis
        canvas.drawLine(0, centerY, width, centerY, axisPaint);

        float[] localBuf;
        synchronized (lock) {
            localBuf = waveform.clone();
        }

        int n = localBuf.length;
        if (n < 2) return;

        float xStep = width / (float) (n - 1);

        for (int i = 0; i < n; i++) {
            float x = i * xStep;
            // Convert [-1, 1] to screen coordinates (top=0, bottom=height)
            float sample = localBuf[i];
            if (sample > 1f) sample = 1f;
            if (sample < -1f) sample = -1f;
            float y = centerY - sample * (height * 0.45f); // leave some padding

            if (i > 0) {
                canvas.drawLine(lastX, lastY, x, y, linePaint);
            }
            lastX = x;
            lastY = y;
        }
    }
}
