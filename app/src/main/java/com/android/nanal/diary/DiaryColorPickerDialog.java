package com.android.nanal.diary;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import com.android.nanal.R;
import com.android.nanal.color.ColorPickerDialog;

public class DiaryColorPickerDialog extends ColorPickerDialog {

    private static final int NUM_COLUMNS = 4;
    private static final String KEY_CALENDAR_COLOR = "calendar_color";

    private int mCalendarColor;

    public DiaryColorPickerDialog() {
        // Empty constructor required for dialog fragment.
    }

    public static DiaryColorPickerDialog newInstance(int[] colors, int selectedColor, boolean isTablet) {
        DiaryColorPickerDialog ret = new DiaryColorPickerDialog();
        ret.initialize(R.string.diary_color_picker_dialog_title, colors, selectedColor, NUM_COLUMNS,
                isTablet ? SIZE_LARGE : SIZE_SMALL);
        return ret;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mCalendarColor = savedInstanceState.getInt(KEY_CALENDAR_COLOR);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_CALENDAR_COLOR, mCalendarColor);
    }

    public void setCalendarColor(int color) {
        mCalendarColor = color;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        mAlertDialog.setButton(DialogInterface.BUTTON_NEUTRAL,
                getActivity().getString(R.string.diary_color_set_to_default),
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        onColorSelected(mCalendarColor);
                    }
                }
        );
        return dialog;
    }
}
