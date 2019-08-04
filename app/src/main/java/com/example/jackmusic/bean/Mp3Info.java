package com.example.jackmusic.bean;

import android.os.Parcel;
import android.os.Parcelable;

public class Mp3Info implements Parcelable {
    protected Mp3Info(Parcel in) {
    }

    public static final Creator<Mp3Info> CREATOR = new Creator<Mp3Info>() {
        @Override
        public Mp3Info createFromParcel(Parcel in) {
            return new Mp3Info(in);
        }

        @Override
        public Mp3Info[] newArray(int size) {
            return new Mp3Info[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
    }
}
