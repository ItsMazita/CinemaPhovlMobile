package com.phovl.cinemaphovlmobile.model;

import com.google.gson.annotations.SerializedName;

public class RegisterResponse {
    @SerializedName("message")
    private String message;

    public String getMessage() {
        return message;
    }
}
