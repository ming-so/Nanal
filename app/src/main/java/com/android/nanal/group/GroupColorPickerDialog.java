package com.android.nanal.group;

import android.app.Dialog;
import android.os.Bundle;

import com.android.nanal.R;
import com.android.nanal.color.ColorPickerDialog;

public class GroupColorPickerDialog extends ColorPickerDialog {

    private static final int NUM_COLUMNS = 4;
    private static final String KEY_GROUP_COLOR = "group_color";

    private int mGroupColor;

    public GroupColorPickerDialog() {
        // Empty constructor required for dialog fragment.
    }

    public static GroupColorPickerDialog newInstance(int[] colors, int selectedColor, boolean isTablet) {
        GroupColorPickerDialog ret = new GroupColorPickerDialog();
        ret.initialize(R.string.group_color_picker_dialog_title, colors, selectedColor, NUM_COLUMNS,
                isTablet ? SIZE_LARGE : SIZE_SMALL);
        return ret;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mGroupColor = savedInstanceState.getInt(KEY_GROUP_COLOR);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_GROUP_COLOR, mGroupColor);
    }

    public void setGroupColor(int color) {
        mGroupColor = color;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        return dialog;
    }
}
