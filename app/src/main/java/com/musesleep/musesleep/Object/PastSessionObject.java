package com.musesleep.musesleep.Object;

import android.graphics.drawable.Drawable;

public class PastSessionObject {

    private String upperText;
    private String lowerText;
    private Drawable drawable;

    public PastSessionObject(String upperText, String lowerText, Drawable drawable) {
        this.upperText = upperText;
        this.lowerText = lowerText;
        this.drawable = drawable;
    }

    public String getUpperText() {
        return upperText;
    }

    public String getLowerText() {
        return lowerText;
    }

    public Drawable getDrawable() {
        return drawable;
    }
}
