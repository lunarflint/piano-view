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

package zone.lunar.pianoviewdemo;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Toast;

import org.billthefarmer.mididriver.MidiDriver;

import zone.lunar.pianoview.PianoView;
import zone.lunar.pianoview.PianoViewTouchEventListener;

public class MainActivity extends AppCompatActivity implements PianoViewTouchEventListener {

    private MidiDriver midiDriver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        midiDriver = new MidiDriver();

        final PianoView pv = (PianoView) findViewById(R.id.pianoview);
        pv.setTouchEventListener(this);


        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setCustomView(R.layout.actionbar);
            actionBar.setDisplayShowCustomEnabled(true);

            View v = actionBar.getCustomView();
            SeekBar sb = (SeekBar) v.findViewById(R.id.scaleBar);
            sb.setProgress(50);

            sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onStartTrackingTouch(SeekBar seekBar) { }
                @Override public void onStopTrackingTouch(SeekBar seekBar) { }
                @Override
                public void onProgressChanged(SeekBar seekBar, int value, boolean fromUser) {
                    float scale = value >= 50f ? value / 50f : .5f + value / 100f;
                    pv.setScale(scale);
                }
            });
        }
        else {
            Toast.makeText(getApplicationContext(), "Warning: Action bar not found!", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        midiDriver.start();
        byte[] ev = new byte[2];
        ev[0] = (byte) 0xc0;
        ev[1] = (byte) 2;
        midiDriver.write(ev);
    }

    @Override
    protected void onPause() {
        midiDriver.stop();
        super.onPause();
    }

    @Override
    public void onKeyDown(int midiNote, int velocity) {
        byte[] ev = new byte[3];
        ev[0] = (byte) 0x90;
        ev[1] = (byte) midiNote;
        ev[2] = (byte) velocity;
        midiDriver.write(ev);
    }

    @Override
    public void onKeyUp(int midiNote) {
        byte[] ev = new byte[3];
        ev[0] = (byte) 0x80;
        ev[1] = (byte) midiNote;
        ev[2] = (byte) 0x00;
        midiDriver.write(ev);
    }
}
