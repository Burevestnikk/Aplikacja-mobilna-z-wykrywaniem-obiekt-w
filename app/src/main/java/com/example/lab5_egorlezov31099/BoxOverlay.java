package com.example.lab5_egorlezov31099;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import com.google.mlkit.vision.objects.DetectedObject;

public class BoxOverlay extends View {
    private RectF rect;
    private Paint paint;

    private TextView textView;

    private int objectId;
    private DetectedObject detectedObject;

    public BoxOverlay(Context context, RectF rect) {
        super(context);
        this.rect = rect;
        paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4f);
        objectId = 0;

        textView = new TextView(context);
        textView.setTextColor(Color.WHITE);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
    }

    public void setDetectedObject(DetectedObject detectedObject) {
        this.detectedObject = detectedObject;
    }

    public int getObjectId() {
        return objectId;
    }

    public void setObjectId(int objectId) {
        this.objectId = objectId;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawRect(rect, paint);

        float textOffset = textView.getTextSize() / 2;
        float x = rect.left + rect.width() / 2;
        float y = rect.top - textOffset;
        textView.setText(String.valueOf(objectId));
        textView.setX(x);
        textView.setY(y);
        textView.draw(canvas);
    }
}
