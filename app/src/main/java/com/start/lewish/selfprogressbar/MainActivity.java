package com.start.lewish.selfprogressbar;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;

public class MainActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener, View.OnClickListener {

    private EditText mEditText;
    private TextProgressBar mTextProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextProgressBar = (TextProgressBar) findViewById(R.id.mTextProgressBar);

        ((SeekBar) findViewById(R.id.progress_slider)).setOnSeekBarChangeListener(this);
        findViewById(R.id.button_animate).setOnClickListener(this);

        mEditText = (EditText) findViewById(R.id.progress_jump);
    }

    @Override
    public void onProgressChanged(final SeekBar seekBar, final int progress, final boolean user) {
        mTextProgressBar.setProgress(progress);
    }

    @Override
    public void onStartTrackingTouch(final SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(final SeekBar seekBar) {

    }

    @Override
    public void onClick(final View v) {
        final Integer progress = Integer.parseInt(mEditText.getText().toString());
        mTextProgressBar.animateProgress(progress);
    }
}
