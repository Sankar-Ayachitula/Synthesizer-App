package edu.northeastern.synthesizer.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class WaveformView extends View {

    private final Paint paint = new Paint();
    private float[] samples = new float[0];

    public WaveformView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint.setColor(0xFFFF8800);
        paint.setStrokeWidth(3f);
        paint.setStyle(Paint.Style.STROKE);
    }


    public void updateWave(float[] newSamples) {
        this.samples = newSamples;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (samples == null || samples.length < 2) return;

        float width = getWidth();
        float height = getHeight();
        float mid = height / 2f;

        int sampleCount = samples.length;

        float xStep = width / (float) sampleCount;

        for (int i = 1; i < sampleCount; i++) {
            float x1 = (i - 1) * xStep;
            float y1 = mid - samples[i - 1] * mid;

            float x2 = i * xStep;
            float y2 = mid - samples[i] * mid;

            canvas.drawLine(x1, y1, x2, y2, paint);
        }
    }
}
