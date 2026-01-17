package com.phovl.cinemaphovlmobile.model;

import android.net.Uri;

public class PurchaseItem {
    public final String name;
    public final Uri uri;
    public final long size;
    public final long dateMillis;
    public final String funcionId;

    public PurchaseItem(String name, Uri uri, long size, long dateMillis, String funcionId) {
        this.name = name;
        this.uri = uri;
        this.size = size;
        this.dateMillis = dateMillis;
        this.funcionId = funcionId;
    }
}
