package com.example.jackmusic.service;

import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.example.jackmusic.bean.Mp3Info;
import com.example.jackmusic.util.Constants;
import com.example.jackmusic.util.SpTools;
import com.example.jackmusic.util.ThreadPoolUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

//MusicService用于处理音乐播放的逻辑
public class MusicService extends Service implements MediaPlayer.OnErrorListener,MediaPlayer.OnPreparedListener,MediaPlayer.OnCompletionListener, AudioManager.OnAudioFocusChangeListener {

    private static final String TAG = "MusicService";
    public static int playMode= 2;//1.单曲循环 2.列表循环 0.随机播放
    private List<Mp3Info> mMusic_list = new ArrayList<>();
    private MusicBroadReceiver receiver;
    private int mPosition;
    private static MediaPlayer mediaPlayer;
    private NotificationManager notificationManager;
    private int mCurrentPosition;
    private Message mMessage;
    private Messenger mMessenger;
    private Random mRandom;
    public static int prv_position;
    private static boolean isLoseFocus;

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        Log.i(TAG,"onCreate: musicService");
        regFilter();

        //创建音频管理器AudioManager
        AudioManager audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        //获取音频的焦点,AUDIOFOCUS_GAIN 重新获取到音频焦点时触发的状态。
        int result = audioManager.requestAudioFocus(this,AudioManager.STREAM_MUSIC,AudioManager.AUDIOFOCUS_GAIN);
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mRandom = new Random();
        super.onCreate();
    }

    private void initPlayer(){
        if (mediaPlayer == null){
            mediaPlayer = new MediaPlayer();
        }
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setOnErrorListener(this);//资源出错
        mediaPlayer.setOnPreparedListener(this);//资源准备好的时候
        mediaPlayer.setOnCompletionListener(this);//播放完成的时候
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null){
            mMusic_list = intent.getParcelableArrayListExtra("music_list");
            mMessenger = (Messenger) intent.getExtras().get("messenger");
            mPosition = SpTools.getInt(getApplicationContext(),"music_currunt_positioin",0);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy: musicService");
        cancelMusic();
        if (receiver != null) {
            getApplicationContext().unregisterReceiver(receiver);
        }
        //stopSelf();
    }

    private void cancelMusic() {
        notificationManager.cancel(Constants.NOTIFICATION_CEDE);
        mMessage = Message.obtain();
        mMessage.what = Constants.MSG_CANCEL;
        try {
            mMessenger.send(mMessage);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    //为MediaPlayer的播放错误事件绑定监听器
    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        System.out.println("service : OnError");
        Intent intent = new Intent();
        intent.setAction(Constants.ACTION_NEXT);
        sendBroadcast(intent);
        return false;
    }

    //设置监听事件setOnPreparedListener()来通知MediaPlayer资源已经获取到
    @Override
    public void onPrepared(MediaPlayer mp) {
        mediaPlayer.start();//开始播放

        if (mMessenger != null){
            sentPreparedMessageToMain();
            sentPositionToMainByTimer();
        }
    }

    //为MediaPlayer的播放完成事件绑定监听
    @Override
    public void onCompletion(MediaPlayer mp) {
        Intent intent = new Intent();
        intent.setAction(Constants.ACTION_NEXT);
        sendBroadcast(intent);
    }

    /**
     * 播放
     */
    private void play(int position){
        if (mediaPlayer != null && mMusic_list.size() > 0){
            mediaPlayer.reset();//重置到刚刚创建的状态
            try {
                mediaPlayer.setDataSource(mMusic_list.get(position).getUrl());//设置音频的文件位置
                mediaPlayer.prepareAsync();//异步加载
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 暂停
     */
    private void pause(){
        if (mediaPlayer != null && mediaPlayer.isPlaying()){
            mCurrentPosition = mediaPlayer.getCurrentPosition();
            mediaPlayer.pause();

        }
    }

    private void sentPreparedMessageToMain() {
        Message mMessage = new Message();
        mMessage.what = Constants.MSG_PREPARED;
        mMessage.arg1 = mPosition;
        mMessage.obj = mediaPlayer.isPlaying();
        try {
            //发送播放位置
            mMessenger.send(mMessage);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void sentPlayStateToMain() {
        mMessage = Message.obtain();
        mMessage.what = Constants.MSG_PLAY_STATE;
        mMessage.obj = mediaPlayer.isPlaying();
        try {
            //发送播放状态
            mMessenger.send(mMessage);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void sentPositionToMainByTimer() {
        ThreadPoolUtil.getScheduledExecutor().scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mediaPlayer.isPlaying()) {
                        //1.准备好的时候.告诉activity,当前歌曲的总时长
                        int currentPosition = mediaPlayer.getCurrentPosition();
                        int totalDuration = mediaPlayer.getDuration();
                        mMessage = Message.obtain();
                        mMessage.what = Constants.MSG_PROGRESS;
                        mMessage.arg1 = currentPosition;
                        mMessage.arg2 = totalDuration;
                        //2.发送消息
                        mMessenger.send(mMessage);
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }, 0, 1000, TimeUnit.MILLISECONDS);
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
                    mPosition = intent.getIntExtra("position", 0);
                    play(mPosition);
                    break;
                case Constants.ACTION_PAUSE:
                    Log.i(TAG, "onReceive: ACTION_PAUSE");
                    //暂停播放
                    pause();
                    break;
                case Constants.ACTION_PLAY:
                    Log.i(TAG, "onReceive: ACTION_PLAY");
                    //开始播放
                    if (mediaPlayer != null) {
                        //mediaPlayer.seekTo(mCurrentPosition);
                        mediaPlayer.start();
                        //通知是否在播放
                        sentPlayStateToMain();
                    }else {
                        initPlayer();
                        play(mPosition);
                    }
                    break;
                case Constants.ACTION_NEXT:
                    Log.i(TAG, "onReceive: ACTION_NEXT");
                    //下一首
                    prv_position = mPosition;
                    if (playMode % 3 == 1) {//1.单曲循环
                        play(mPosition);
                    } else if (playMode % 3 == 2) {//2.列表播放
                        mPosition++;
                        if (mPosition <= mMusic_list.size() - 1) {
                            play(mPosition);
                        } else {
                            mPosition = 0;
                            play(mPosition);
                        }
                    } else if (playMode % 3 == 0) {// 0.随机播放
                        play(getRandom());
                    }
                    break;
                case Constants.ACTION_PRV:
                    Log.i(TAG, "onReceive: ACTION_PRV");
                    //上一首
                    prv_position = mPosition;
                    if (playMode % 3 == 1) {//1.单曲循环
                        play(mPosition);
                    } else if (playMode % 3 == 2) {//2.列表播放
                        mPosition--;
                        if (mPosition < 0) {
                            mPosition = mMusic_list.size() - 1;
                            play(mPosition);
                        } else {
                            play(mPosition);
                        }
                    } else if (playMode % 3 == 0) {// 0.随机播放
                        play(getRandom());
                    }
                    break;
                case Constants.ACTION_CLOSE:
                    Log.i(TAG, "onReceive: ACTION_CLOSE");
                    cancelMusic();
                    break;
                case AudioManager.ACTION_AUDIO_BECOMING_NOISY:
                    Log.i(TAG, "onReceive: ACTION_AUDIO_BECOMING_NOISY");
                    //如果耳机拨出时暂停播放
                    Intent intent_pause = new Intent();
                    intent_pause.setAction(Constants.ACTION_PAUSE);
                    sendBroadcast(intent_pause);
                    break;
            }
        }
    }

    private int getRandom() {
        mPosition = mRandom.nextInt(mMusic_list.size());
        return mPosition;
    }

    public static boolean isPlaying(){
        if(mediaPlayer != null){
            return mediaPlayer.isPlaying();
        }
        return false;
    }

    /**
     * ---------------音频焦点处理相关的方法---------------
     **/
    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN://你已经得到了音频焦点。
                Log.i(TAG, "onAudioFocusChange: -------------AUDIOFOCUS_GAIN---------------");
                // resume playback
                if (isLoseFocus) {
                    isLoseFocus = false;
                    mediaPlayer.start();
                    mediaPlayer.setVolume(1.0f, 1.0f);
                    sentPlayStateToMain();
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS://你已经失去了音频焦点很长时间了。你必须停止所有的音频播放
                Log.i(TAG, "onAudioFocusChange: -------------AUDIOFOCUS_LOSS---------------");
                // Lost focus for an unbounded amount of time: stop playback and release media player
                isLoseFocus = false;
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                    sentPlayStateToMain();
                }
                mediaPlayer.release();
                mediaPlayer = null;
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT://你暂时失去了音频焦点
                Log.i(TAG, "onAudioFocusChange: -------------AUDIOFOCUS_LOSS_TRANSIENT---------------");
                // Lost focus for a short time, but we have to stop
                // playback. We don't release the media player because playback
                // is likely to resume
                if (mediaPlayer.isPlaying()) {
                    isLoseFocus = true;
                    mediaPlayer.pause();
                    sentPlayStateToMain();
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK://你暂时失去了音频焦点，但你可以小声地继续播放音频（低音量）而不是完全扼杀音频。
                Log.i(TAG, "onAudioFocusChange: -------------AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK---------------");
                // Lost focus for a short time, but it's ok to keep playing
                // at an attenuated level
                if (mediaPlayer.isPlaying()) {
                    isLoseFocus = true;
                    mediaPlayer.setVolume(0.1f, 0.1f);
                    sentPlayStateToMain();
                }
                break;
        }

    }
}
