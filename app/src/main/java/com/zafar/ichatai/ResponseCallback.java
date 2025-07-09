package com.zafar.ichatai;

public interface ResponseCallback {
    void onResponse(String response);
    void onError(Throwable throwable);
}