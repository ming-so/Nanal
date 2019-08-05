package com.android.nanal.chips.recipientchip;


import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.style.ReplacementSpan;

import com.android.nanal.chips.RecipientEntry;

/**
 * RecipientChip defines a span that contains information relevant to a
 * particular recipient.
 */
public class InvisibleRecipientChip extends ReplacementSpan implements DrawableRecipientChip {
    private final SimpleRecipientChip mDelegate;
    private static final Rect NULL_RECTANGLE = new Rect(0, 0, 0, 0);

    public InvisibleRecipientChip(final RecipientEntry entry) {
        super();

        mDelegate = new SimpleRecipientChip(entry);
    }

    @Override
    public void setSelected(final boolean selected) {
        mDelegate.setSelected(selected);
    }

    @Override
    public boolean isSelected() {
        return mDelegate.isSelected();
    }

    @Override
    public CharSequence getDisplay() {
        return mDelegate.getDisplay();
    }

    @Override
    public CharSequence getValue() {
        return mDelegate.getValue();
    }

    @Override
    public long getContactId() {
        return mDelegate.getContactId();
    }

    @Override
    public Long getDirectoryId() {
        return mDelegate.getDirectoryId();
    }

    @Override
    public String getLookupKey() {
        return mDelegate.getLookupKey();
    }

    @Override
    public long getDataId() {
        return mDelegate.getDataId();
    }

    @Override
    public RecipientEntry getEntry() {
        return mDelegate.getEntry();
    }

    @Override
    public void setOriginalText(final String text) {
        mDelegate.setOriginalText(text);
    }

    @Override
    public CharSequence getOriginalText() {
        return mDelegate.getOriginalText();
    }

    @Override
    public void draw(final Canvas canvas, final CharSequence text, final int start, final int end,
                     final float x, final int top, final int y, final int bottom, final Paint paint) {
        // Do nothing.
    }

    @Override
    public int getSize(final Paint paint, final CharSequence text, final int start, final int end,
                       final Paint.FontMetricsInt fm) {
        return 0;
    }

    @Override
    public Rect getBounds() {
        return NULL_RECTANGLE;
    }

    @Override
    public Rect getWarningIconBounds() {
        return NULL_RECTANGLE;
    }

    @Override
    public void draw(final Canvas canvas) {
        // do nothing.
    }
}
