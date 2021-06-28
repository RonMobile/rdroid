package com.termux.app;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.termux.R;

/**
 * TODO: document your custom view class.
 */
public class MainButtonView extends LinearLayout {

    private ImageView mImageView;
    private TextView mTextView;

    public MainButtonView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    private void init(Context context, AttributeSet attrs, int defStyle) {
        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(
            attrs, R.styleable.MainButtonView, defStyle, 0);

        // todo: change defValue
        String text = a.getString(R.styleable.MainButtonView_text);
        Drawable drawable = a.getDrawable(R.styleable.MainButtonView_src);

        a.recycle();

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT,
            .5f
        );

        setLayoutParams(params);
        setOrientation(LinearLayout.VERTICAL);
        setGravity(Gravity.CENTER_VERTICAL);

        LayoutInflater inflater = (LayoutInflater) context
            .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.main_button_view, this, true);

        mImageView =  (ImageView) getChildAt(0);
        mImageView.setImageDrawable(drawable);

        mTextView = (TextView) getChildAt(1);
        mTextView.setText(text);

        // Set listener
        setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                ImageView mImageViewLocal = (ImageView) ((LinearLayout) view).getChildAt(0);

                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    mImageViewLocal.setImageTintList(
                        ColorStateList.valueOf(
                            ContextCompat.getColor(context, R.color.light_blue_400)
                        ));
                } else if (motionEvent.getAction() == MotionEvent.ACTION_UP){
                    mImageViewLocal.setImageTintList(
                        ColorStateList.valueOf(
                            ContextCompat.getColor(context, R.color.gray_400)
                        ));
                }
                return true;
            }
        });


    }


}
