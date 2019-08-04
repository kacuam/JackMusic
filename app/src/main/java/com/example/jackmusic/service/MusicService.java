package com.example.jackmusic.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.util.Log;

import com.example.jackmusic.bean.Mp3Info;
import com.example.jackmusic.util.Constants;

import java.util.ArrayList;
import java.util.List;

public class MusicService extends Service {

    private static final String TAG = "MusicService";
    public static int playMode= 2;//1.单曲循环 2.列表循环 0.随机播放
    private List<Mp3Info> mMusic_list = new ArrayList<>();
    private MusicBroadReceiver receiver;
    private int mPosition;
    private static MediaPlayer mediaPlayer;


    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG,"onCreate: musicService");
        regFilter();

        //创建音频管理器AudioManager
        AudioManager audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy: musicService");
    }

    /**
     * 注册广播
     */
    private  void regFilter(){
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.ACTION_LIST_ITEM);
        intentFilter.addAction(Constants.ACTION_PLAY);
        intentFilter.addAction(Constants.ACTION_PAUSE);
        intentFilter.addAction(Constants.ACTION_CLOSE);
        intentFilter.addAction(Constants.ACTION_PRV);
        intentFilter.addAction(Constants.ACTION_NEXT);
        if (receiver == null){
            receiver = new MusicBroadReceiver();
        }
        getApplicationContext().registerReceiver(receiver,intentFilter);
    }

    /**
     * 广播接收者
     */
    public class MusicBroadReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()){
                case Constants.ACTION_LIST_ITEM:
                    Log.i(TAG, "onReceive: ACTION_LIST_ITEM");
                    //点击左侧菜单
                    break;
                case Constants.ACTION_PLAY:
                    Log.i(TAG, "onReceive: ACTION_PLAY");
                case Constants.ACTION_PAUSE:
                    Log.i(TAG, "onReceive: ACTION_PAUSE");
                case Constants.ACTION_CLOSE:
                    Log.i(TAG, "onReceive: ACTION_CLOSE");
                case Constants.ACTION_NEXT:
                    Log.i(TAG, "onReceive: ACTION_NEXT");
                case Constants.ACTION_PRV:
                    Log.i(TAG, "onReceive: ACTION_PRV");
            }
        }
    }
}
