/*
Copyright 2017 Lunarflint

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package zone.lunar.pianoview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.View;

import java.util.HashSet;
import java.util.Set;

public class PianoView extends View {

    //const
    final float WHITE_KEY_WIDTH = 100f;
    final float BLACK_KEY_WIDTH = 60f;
    final float WB_H_RATIO_L = 0.66f;
    final float WB_H_RATIO_M = 0.5f;
    final float WB_H_RATIO_R = 1f - WB_H_RATIO_L;
    final float WB_V_RATIO = 0.4f;
    final float L_SHIFT_1 = WHITE_KEY_WIDTH - WB_H_RATIO_L * BLACK_KEY_WIDTH;   // C -> C# or F -> F#
    final float L_SHIFT_2 = WHITE_KEY_WIDTH - L_SHIFT_1;                        // C# -> D or F# -> G
    final float M_SHIFT_1 = WHITE_KEY_WIDTH - WB_H_RATIO_M * BLACK_KEY_WIDTH;   // G -> G#
    final float M_SHIFT_2 = WHITE_KEY_WIDTH - M_SHIFT_1;                        // G# -> A
    final float R_SHIFT_1 = WHITE_KEY_WIDTH - WB_H_RATIO_R * BLACK_KEY_WIDTH;   // D -> D# or A -> A#
    final float R_SHIFT_2 = WHITE_KEY_WIDTH - R_SHIFT_1;                        // D# -> E or A# -> B
    final float W_SHIFT = WHITE_KEY_WIDTH;                                      // E -> F or B -> C

    //drawing related
    Paint strokePaint;
    Paint onTouchPaint;

    Paint blackKeyPaint;
    RectF blackKeyRect;

    Paint whiteKeyPaint;
    Path whiteKeyLPath; // C or F
    Path whiteKeyRPath; // E or B
    Path whiteKeyGPath;
    Path whiteKeyAPath;
    Path whiteKeyDPath;
    Path whiteKeyPath; // this is the one being use for painting (dst param of Path.offset(x, y))

    // data
    private float position;
    private float scale;

    // touch
    private Set<Integer> touches;
    private SparseIntArray pointerMap;

    // misc
    private float width;
    private float height;

    private int pointerId;
    private float pointerX;
    private float pointerY;


    private PianoViewTouchEventListener listener;


    public PianoView(Context context) {
        super(context);
        init(context);
    }

    public PianoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public PianoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    public PianoView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }


    private void init(Context context) {
        position = 5 * 7 * WHITE_KEY_WIDTH; // Middle C / C5
        scale = 1f;

        listener = null;
        pointerId = -1;

        strokePaint = new Paint();
        onTouchPaint = new Paint();

        blackKeyPaint = new Paint();
        blackKeyRect = new RectF();

        whiteKeyPaint = new Paint();
        whiteKeyLPath = new Path();
        whiteKeyRPath = new Path();
        whiteKeyGPath = new Path();
        whiteKeyAPath = new Path();
        whiteKeyDPath = new Path();
        whiteKeyPath = new Path();

        //todo: styling
        strokePaint.setAntiAlias(true);
        strokePaint.setColor(Color.rgb(0x45, 0x45, 0x45));
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(1);

        onTouchPaint.setAntiAlias(true);
        onTouchPaint.setColor(Color.rgb(0x85, 0x85, 0xb8));
        onTouchPaint.setStyle(Paint.Style.FILL);

        blackKeyPaint.setAntiAlias(true);
        blackKeyPaint.setColor(Color.rgb(0x11, 0x11, 0x11));
        blackKeyPaint.setStyle(Paint.Style.FILL);

        whiteKeyPaint.setAntiAlias(true);
        whiteKeyPaint.setColor(Color.rgb(0xee, 0xee, 0xee));
        whiteKeyPaint.setStyle(Paint.Style.FILL);

        touches = new HashSet<>();
        pointerMap = new SparseIntArray();
    }

    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
        this.scale = scale;
        calculateDrawCoordinates();
        postInvalidate();
    }

    public float getPosition() {
        return position;
    }

    public void setPosition(float position) {
        position = position < 0 ? 0 : position;
        position = position > 75 * WHITE_KEY_WIDTH ? 75 * WHITE_KEY_WIDTH : position; //75 white keys in 127 midi notes
        this.position = position;
        postInvalidate();
    }

    public void setTouchEventListener(PianoViewTouchEventListener listener) {
        this.listener = listener;
    }

    private void calculateDrawCoordinates() {
        blackKeyRect.set(0f, 0f, BLACK_KEY_WIDTH * scale, height * (1 - WB_V_RATIO));


        final float y = height * (1 - WB_V_RATIO);
        final float ww = WHITE_KEY_WIDTH * scale;
        final float bw = BLACK_KEY_WIDTH * scale;

        whiteKeyLPath.rewind();
        whiteKeyLPath.moveTo(0, 0);
        whiteKeyLPath.lineTo(0, height);
        whiteKeyLPath.lineTo(ww, height);
        whiteKeyLPath.lineTo(ww, y);
        whiteKeyLPath.lineTo(ww - bw * WB_H_RATIO_L, y);
        whiteKeyLPath.lineTo(ww - bw * WB_H_RATIO_L, 0);
        whiteKeyLPath.close();


        whiteKeyRPath.rewind();
        whiteKeyRPath.moveTo(0, y);
        whiteKeyRPath.lineTo(0, height);
        whiteKeyRPath.lineTo(ww, height);
        whiteKeyRPath.lineTo(ww, 0);
        whiteKeyRPath.lineTo(bw * (1 - WB_H_RATIO_R), 0);
        whiteKeyRPath.lineTo(bw * (1 - WB_H_RATIO_R), y);
        whiteKeyRPath.close();

        whiteKeyGPath.rewind();
        whiteKeyGPath.moveTo(0, y);
        whiteKeyGPath.lineTo(0, height);
        whiteKeyGPath.lineTo(ww, height);
        whiteKeyGPath.lineTo(ww, y);
        whiteKeyGPath.lineTo(ww - bw * WB_H_RATIO_M, y);
        whiteKeyGPath.lineTo(ww - bw * WB_H_RATIO_M, 0);
        whiteKeyGPath.lineTo(bw * (1 - WB_H_RATIO_L), 0);
        whiteKeyGPath.lineTo(bw * (1 - WB_H_RATIO_L), y);
        whiteKeyGPath.close();

        whiteKeyAPath.rewind();
        whiteKeyAPath.moveTo(0, y);
        whiteKeyAPath.lineTo(0, height);
        whiteKeyAPath.lineTo(ww, height);
        whiteKeyAPath.lineTo(ww, y);
        whiteKeyAPath.lineTo(ww - bw * WB_H_RATIO_R, y);
        whiteKeyAPath.lineTo(ww - bw * WB_H_RATIO_R, 0);
        whiteKeyAPath.lineTo(bw * (1 - WB_H_RATIO_M), 0);
        whiteKeyAPath.lineTo(bw * (1 - WB_H_RATIO_M), y);
        whiteKeyAPath.close();

        whiteKeyDPath.rewind();
        whiteKeyDPath.moveTo(0, y);
        whiteKeyDPath.lineTo(0, height);
        whiteKeyDPath.lineTo(ww, height);
        whiteKeyDPath.lineTo(ww, y);
        whiteKeyDPath.lineTo(ww - bw * WB_H_RATIO_R, y);
        whiteKeyDPath.lineTo(ww - bw * WB_H_RATIO_R, 0);
        whiteKeyDPath.lineTo(bw * (1 - WB_H_RATIO_L), 0);
        whiteKeyDPath.lineTo(bw * (1 - WB_H_RATIO_L), y);
        whiteKeyDPath.close();
    }

    public int pixelToMidiNote(float x, float y) {
        float pos = x / scale + position;
        int octave = (int) (pos / WHITE_KEY_WIDTH) / 7; // 7 white keys in total
        float pos2 = pos - octave * WHITE_KEY_WIDTH * 7;

        int key;

        if (y > height * (1 - WB_V_RATIO)) { // lower half of the keyboard, must be white key
            key = (int) (pos / WHITE_KEY_WIDTH) % 7;
            switch (key) {
                //case 0: key = 0; break;   // C
                case 1: key = 2; break;     // D
                case 2: key = 4; break;     // E
                case 3: key = 5; break;     // F
                case 4: key = 7; break;     // G
                case 5: key = 9; break;     // A
                case 6: key = 11; break;    // B
            }
        }
        else { // upper half, need to check for black keys
            if(pos2 >= WHITE_KEY_WIDTH * 3) { // F - B
                if (pos2 < WHITE_KEY_WIDTH * 3 + L_SHIFT_1)
                    key = 5; // F
                else if (pos2 < WHITE_KEY_WIDTH * 3 + L_SHIFT_1 + BLACK_KEY_WIDTH)
                    key = 6; // F#
                else if (pos2 < WHITE_KEY_WIDTH * 3 + L_SHIFT_1 + L_SHIFT_2 + M_SHIFT_1)
                    key = 7; // G
                else if (pos2 < WHITE_KEY_WIDTH * 3 + L_SHIFT_1 + L_SHIFT_2 + M_SHIFT_1 + BLACK_KEY_WIDTH)
                    key = 8; // G#
                else if (pos2 < WHITE_KEY_WIDTH * 3 + L_SHIFT_1 + L_SHIFT_2 + M_SHIFT_1 + M_SHIFT_2 + R_SHIFT_1)
                    key = 9; // A
                else if (pos2 < WHITE_KEY_WIDTH * 3 + L_SHIFT_1 + L_SHIFT_2 + M_SHIFT_1 + M_SHIFT_2 + R_SHIFT_1 + BLACK_KEY_WIDTH)
                    key = 10; // A#
                else
                    key = 11; // B

            }
            else { // C - E
                if (pos2 < L_SHIFT_1)
                    key = 0; // C
                else if (pos2 < L_SHIFT_1 + BLACK_KEY_WIDTH)
                    key = 1; // C#
                else if (pos2 < L_SHIFT_1 + L_SHIFT_2 + R_SHIFT_1)
                    key = 2; // D
                else if (pos2 < L_SHIFT_1 + L_SHIFT_2 + R_SHIFT_1 + BLACK_KEY_WIDTH)
                    key = 3; // D#
                else
                    key = 4; // E
            }
        }

        return octave * 12 + key;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        width = w;
        height = h;
        calculateDrawCoordinates();
    }

    protected void drawWhiteKeyL(Canvas canvas, float x, boolean isTouching) { // C or F
        whiteKeyLPath.offset(x, 0, whiteKeyPath);
        canvas.drawPath(whiteKeyPath, isTouching ? onTouchPaint : whiteKeyPaint);
        canvas.drawPath(whiteKeyPath, strokePaint);
    }

    protected void drawWhiteKeyR(Canvas canvas, float x, boolean isTouching) { // E or B
        whiteKeyRPath.offset(x, 0, whiteKeyPath);
        canvas.drawPath(whiteKeyPath, isTouching ? onTouchPaint : whiteKeyPaint);
        canvas.drawPath(whiteKeyPath, strokePaint);
    }

    protected void drawWhiteKeyG(Canvas canvas, float x, boolean isTouching) {
        whiteKeyGPath.offset(x, 0, whiteKeyPath);
        canvas.drawPath(whiteKeyPath, isTouching ? onTouchPaint : whiteKeyPaint);
        canvas.drawPath(whiteKeyPath, strokePaint);
    }

    protected void drawWhiteKeyA(Canvas canvas, float x, boolean isTouching) {
        whiteKeyAPath.offset(x, 0, whiteKeyPath);
        canvas.drawPath(whiteKeyPath, isTouching ? onTouchPaint : whiteKeyPaint);
        canvas.drawPath(whiteKeyPath, strokePaint);
    }

    protected void drawWhiteKeyD(Canvas canvas, float x, boolean isTouching) {
        whiteKeyDPath.offset(x, 0, whiteKeyPath);
        canvas.drawPath(whiteKeyPath, isTouching ? onTouchPaint : whiteKeyPaint);
        canvas.drawPath(whiteKeyPath, strokePaint);
    }

    protected void drawBlackKey(Canvas canvas, float x, boolean isTouching) {
        blackKeyRect.offsetTo(x, 0f);
        canvas.drawRect(blackKeyRect, isTouching ? onTouchPaint : blackKeyPaint);
        canvas.drawRect(blackKeyRect, strokePaint);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        final float left = position;
        final float right = position + width / scale;

        //fun fact: you can unroll this loop to something similar to a Duff's device, not that i would do in Java...
        float pos = left - (left % WHITE_KEY_WIDTH) - WHITE_KEY_WIDTH;
        pos = pos < 0 ? 0 : pos;

        int note = pixelToMidiNote((pos - left) * scale, height); //get the white note at start

        while (pos < right) {
            float x = (pos - left) * scale;
            boolean isTouching = touches.contains(note);
            switch (note % 12) {
                case 0: //C
                    drawWhiteKeyL(canvas, x, isTouching);
                    pos += L_SHIFT_1;
                    ++note;
                    break;

                case 1: //C#
                    drawBlackKey(canvas, x, isTouching);
                    pos += L_SHIFT_2;
                    ++note;
                    break;

                case 2: //D
                    drawWhiteKeyD(canvas, x, isTouching);
                    pos += R_SHIFT_1;
                    ++note;
                    break;

                case 3: //D#
                    drawBlackKey(canvas, x, isTouching);
                    pos += R_SHIFT_2;
                    ++note;
                    break;

                case 4: //E
                    drawWhiteKeyR(canvas, x, isTouching);
                    pos += W_SHIFT;
                    ++note;
                    break;

                case 5: //F
                    drawWhiteKeyL(canvas, x, isTouching);
                    pos += L_SHIFT_1;
                    ++note;
                    break;

                case 6: //F#
                    drawBlackKey(canvas, x, isTouching);
                    pos += L_SHIFT_2;
                    ++note;
                    break;

                case 7: //G
                    drawWhiteKeyG(canvas, x, isTouching);
                    pos += M_SHIFT_1;
                    ++note;
                    break;

                case 8: //G#
                    drawBlackKey(canvas, x, isTouching);
                    pos += M_SHIFT_2;
                    ++note;
                    break;

                case 9: //A
                    drawWhiteKeyA(canvas, x, isTouching);
                    pos += R_SHIFT_1;
                    ++note;
                    break;

                case 10: //A#
                    drawBlackKey(canvas, x, isTouching);
                    pos += R_SHIFT_2;
                    ++note;
                    break;

                case 11: //B
                    drawWhiteKeyR(canvas, x, isTouching);
                    pos += W_SHIFT;
                    ++note;
                    break;
            }
        }
    }

    // input

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN: {
                int idx = event.getActionIndex();
                float x = event.getX(idx);
                float y = event.getY(idx);
                int id = event.getPointerId(idx);

                if(pointerId == -1) {
                    pointerId = id;
                    pointerX = x;
                    pointerY = y;
                }

                int note = pixelToMidiNote(x, y);
                int velocity = (int) (0.5f + y * 127f / (height * (1 - WB_V_RATIO)));
                velocity = velocity > 127 ? 127 : velocity;

                touches.add(note);
                pointerMap.append(id, note);
                postInvalidate();
                if(listener != null)
                    listener.onKeyDown(note, velocity);
                return true;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP: {
                int idx = event.getActionIndex();
                int id = event.getPointerId(idx);

                if(id == pointerId)
                    pointerId = -1;

                int note = pointerMap.get(id, Integer.MIN_VALUE);
                if(note != Integer.MIN_VALUE) {
                    touches.remove(note);
                    pointerMap.delete(id);
                }
                postInvalidate();
                if(listener != null)
                    listener.onKeyUp(note);
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                int idx = event.getActionIndex();
                int id = event.getPointerId(idx);

                if(id == pointerId) {
                    float newX = event.getX();
                    float newY = event.getY();
                    float dx = newX - pointerX;
                    float pos = getPosition();
                    setPosition(pos - dx / scale);
                    pointerX = newX;
                    pointerY = newY;
                    return true;
                }
                else {
                    return super.onTouchEvent(event);
                }
            }
            default:
                return super.onTouchEvent(event);
        }
    }

}
