package com.example.proc7;

import android.net.Uri;

public class Cloth {
    private String title;
    private String producer;
    private Uri imageUri;

    public Cloth(String title, String producer, Uri imageUri) {
        this.title = title;
        this.producer = producer;
        this.imageUri = imageUri;
    }

    public String getTitle() {
        return title;
    }

    public String getProducer() {
        return producer;
    }

    public Uri getImageUri() {
        return imageUri;
    }

}