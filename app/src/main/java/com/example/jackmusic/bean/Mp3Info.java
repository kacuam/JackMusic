package com.example.jackmusic.bean;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 序列化bean对象，使之能在activity和service中传递信息
 * 使用Parcel类存储序列化数据,包装需要传输的数据，然后在Binder中传输，用于跨进程传输数据。
 * 可以将序列化之后的数据写入一个共享内存中，其他进程通过Parcel可以从这块共享内存读出字节流，并反序列化成对象
 */
public class Mp3Info implements Parcelable {

    private long id;//音乐的id
    private String title;//音乐标题
    private String artist;//作者
    private String album;//专辑名
    private long albumId;//专辑id
    private long duration;//时长（毫秒）
    private long size;//文件大小
    private String url;
    private String songId;
    private String songName;
    private String picUrl;
    private String audio;

    public Mp3Info() {

    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public long getAlbumId() {
        return albumId;
    }

    public void setAlbumId(long albumId) {
        this.albumId = albumId;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getSongId() {
        return songId;
    }

    public void setSongId(String songId) {
        this.songId = songId;
    }

    public String getSongName() {
        return songName;
    }

    public void setSongName(String songName) {
        this.songName = songName;
    }

    public String getPicUrl() {
        return picUrl;
    }

    public void setPicUrl(String picUrl) {
        this.picUrl = picUrl;
    }

    public String getAudio() {
        return audio;
    }

    public void setAudio(String audio) {
        this.audio = audio;
    }


    public Mp3Info(Parcel in) {
        id = in.readLong();
        title = in.readString();
        artist = in.readString();
        album = in.readString();
        albumId = in.readLong();
        duration = in.readLong();
        size = in.readLong();
        url = in.readString();
        songId = in.readString();
        songName = in.readString();
        picUrl = in.readString();
        audio = in.readString();
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
        dest.writeLong(id);
        dest.writeString(title);
        dest.writeString(artist);
        dest.writeString(album);
        dest.writeLong(albumId);
        dest.writeLong(duration);
        dest.writeLong(size);
        dest.writeString(url);
        dest.writeString(songId);
        dest.writeString(songName);
        dest.writeString(picUrl);
        dest.writeString(audio);
    }

    @Override
    public String toString() {
        return "Mp3Info{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", artist='" + artist + '\'' +
                ", album='" + album + '\'' +
                ", albumId=" + albumId +
                ", duration=" + duration +
                ", size=" + size +
                ", url='" + url + '\'' +
                ", songId='" + songId + '\'' +
                ", songName='" + songName + '\'' +
                ", picUrl='" + picUrl + '\'' +
                ", audio='" + audio + '\'' +
                '}';
    }
}
