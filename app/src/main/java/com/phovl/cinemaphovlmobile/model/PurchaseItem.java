package com.phovl.cinemaphovlmobile.model;

import android.net.Uri;

public class PurchaseItem {
    public final String filename;
    public final Uri uri;
    public final long sizeBytes;
    public final long timestamp;
    public final String funcionId;

    public PurchaseItem(String filename, Uri uri, long sizeBytes, long timestamp, String funcionId) {
        this.filename = filename;
        this.uri = uri;
        this.sizeBytes = sizeBytes;
        this.timestamp = timestamp;
        this.funcionId = funcionId;
    }
}
