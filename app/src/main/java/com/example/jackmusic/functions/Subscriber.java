package com.example.jackmusic.functions;

public interface Subscriber<T> {
    void onComplete(T t);
    void onError(Exception e);
}