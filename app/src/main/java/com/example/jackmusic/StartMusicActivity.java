package com.example.jackmusic;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import com.example.jackmusic.bean.Mp3Info;
import com.example.jackmusic.functions.Subscriber;
import com.example.jackmusic.service.MusicService;
import com.example.jackmusic.util.Constants;
import com.example.jackmusic.util.LrcUtil;
import com.example.jackmusic.util.MediaUtil;
import com.example.jackmusic.util.SpTools;
import com.example.jackmusic.util.StatusBarUtil;
import com.example.jackmusic.view.LrcView;
import com.example.jackmusic.view.MusicPlayerView;
import com.example.jackmusic.view.RoundImageView;
import com.example.jackmusic.view.SlidingMenu;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

public class StartMusicActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "MusicActivity";
    private RelativeLayout mStartView;
    private SlidingMenu mSlidMenu;
    private ListView mListView;
    private RoundImageView mBack;
    private TextView mViewSong,mViewSinger;
    private LrcView mLrc;
    private MusicPlayerView mPv;
    private ImageView mPlayMode,mNext,mPrevious;
    private LrcView mCurrentLrc;
    private SlidingMenu mSlidingMenu;
    private List<Mp3Info> mMusicList = new ArrayList<>();
    private ListView mLeftView;
    private Mp3Info mMp3Info;
    private TextView mSong;
    private TextView mSinger;

    private RemoteViews remoteViews;

    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder mBuilder;
    private int mPosition;
    private boolean mIsPlaying = false;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == Constants.MSG_PROGRESS) {
                int currentPosition = msg.arg1;
                int totalDuration = msg.arg2;
                mPv.setProgress(currentPosition);
                mPv.setMax(totalDuration);
                mCurrentLrc.updateTime(currentPosition);
            }
            if (msg.what == Constants.MSG_PREPARED) {
                mPosition = msg.arg1;
                mIsPlaying = (boolean) msg.obj;
                switchSongUI(mPosition, mIsPlaying);
            }
            if (msg.what == Constants.MSG_PLAY_STATE) {
                mIsPlaying = (boolean) msg.obj;
                refreshPlayStateUI(mIsPlaying);
            }
            if (msg.what == Constants.MSG_CANCEL) {
                mIsPlaying = false;
                finish();
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();
        initPermission();
    }

    private void initView(){
        //去掉标题栏
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        StatusBarUtil.enableTranslucentStatusbar(StartMusicActivity.this);
        setContentView(R.layout.activity_start_music);
        //startmusic
        mStartView = (RelativeLayout) findViewById(R.id.start);
        mSlidMenu = (SlidingMenu) findViewById(R.id.sm);
        //left
        mListView = (ListView) findViewById(R.id.listview);
        //content
        mBack = (RoundImageView) findViewById(R.id.back);//切换左右控件
        mViewSong = (TextView) findViewById(R.id.textViewSong);//歌名
        mViewSinger = (TextView) findViewById(R.id.textViewSinger);//歌手
        mLrc = (LrcView) findViewById(R.id.lrc);//当前歌词
        mPv = (MusicPlayerView) findViewById(R.id.mpv);//自定义播放控件
        mPlayMode = (ImageView) findViewById(R.id.play_mode);//播放模式
        mNext = (ImageView) findViewById(R.id.next);//下一首
        mPrevious = (ImageView) findViewById(R.id.previous);//上一首
        //通知栏布局,加载自定义布局
        remoteViews = new RemoteViews(getPackageName(),R.layout.customnotice);
        //创建通知栏
        createNotification();
    }

    /**
     * 创建通知栏
     */
    private void createNotification() {
        mBuilder = new NotificationCompat.Builder(this,"default");
        // 点击跳转到主界面
        Intent intent_main = new Intent(this,StartMusicActivity.class);
        //4个参数context, requestCode, intent, flags
        PendingIntent pendingIntent_go = PendingIntent.getActivity(this,1,intent_main,PendingIntent.FLAG_UPDATE_CURRENT);
        //添加点击事件
        remoteViews.setOnClickPendingIntent(R.id.notice,pendingIntent_go);

        //点击关闭
        Intent intent_cancel = new Intent();
        intent_cancel.setAction(Constants.ACTION_CLOSE);
        PendingIntent pendingIntent_close = PendingIntent.getBroadcast(this,2,intent_cancel,PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.widget_close,pendingIntent_close);

        //上一首
        Intent intent_prv = new Intent();
        intent_prv.setAction(Constants.ACTION_PRV);
        PendingIntent pendingIntent_prv = PendingIntent.getBroadcast(this,3,intent_prv,PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.widget_prev,pendingIntent_prv);

        //设置播放暂停
        Intent intent_play_pause;
        if (mIsPlaying){ //正在播放-->暂停
            intent_play_pause = new Intent();
            intent_play_pause.setAction(Constants.ACTION_PAUSE);
            PendingIntent pendingIntent_play = PendingIntent.getBroadcast(this,4,intent_play_pause,PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViews.setOnClickPendingIntent(R.id.widget_play,pendingIntent_play);
        }
        if (!mIsPlaying){ //暂停中-->播放
            intent_play_pause = new Intent();
            intent_play_pause.setAction(Constants.ACTION_PLAY);
            PendingIntent pendingIntent_play = PendingIntent.getBroadcast(this,5,intent_play_pause,PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViews.setOnClickPendingIntent(R.id.widget_play,pendingIntent_play);
        }

        //下一首
        Intent intent_next = new Intent();
        intent_next.setAction(Constants.ACTION_NEXT);
        PendingIntent pendingIntent_next = PendingIntent.getBroadcast(this,6,intent_next,PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.widget_prev,pendingIntent_next);

        mBuilder.setSmallIcon(R.mipmap.music);// 设置状态栏顶部图标
        mBuilder.setContent(remoteViews);//传入通知栏布局和意图
        mBuilder.setOngoing(true);//表示正在进行的通知
    }

    /**
     * 获取权限
     */
    private void initPermission(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            //没有权限
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.READ_EXTERNAL_STORAGE)){
                // 用户拒绝过这个权限了，应该提示用户，为什么需要这个权限。
                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
                // 申请授权。
            } else {
                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
                // 申请授权。
            }
        } else {
            init();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    init();
                } else {
                    Toast.makeText(this,"授权失败，无法启动",Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void init(){
        initData();
        initEvent();
    }

    private void initData(){
        //音乐列表
        mMusicList = MediaUtil.getMp3Infos(this);
        //启动音乐服务
        startMusicService();
        mLeftView.setAdapter(new MediaListAdapter());
        //消息管理
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        //初始化控件UI，默认显示历史播放歌曲
        mPosition = SpTools.getInt(getApplicationContext(), "music_current_position", 0);
        mIsPlaying = MusicService.isPlaying();
        switchSongUI(mPosition, mIsPlaying);
    }

    /**
     * 开始音乐服务并传输数据
     */
    private void startMusicService(){
        Intent musicService = new Intent();
        musicService.setClass(getApplicationContext(), MusicService.class);
        musicService.putParcelableArrayListExtra("music_list", (ArrayList<? extends Parcelable>) mMusicList);
        musicService.putExtra("messenger", new Messenger(handler));
        startService(musicService);
    }

    /**
     * 刷新播放控件的歌名，歌手，图片，按钮的形状
     */
    private void switchSongUI(int position,boolean isPlaying){
        if (mMusicList.size() > 0 && position < mMusicList.size()) {
            // 1.获取播放数据
            mMp3Info = mMusicList.get(position);
            // 2.设置歌曲名，歌手
            String mSongTitle = mMp3Info.getTitle();
            String mSingerArtist = mMp3Info.getArtist();
            mSong.setText(mSongTitle);
            mSinger.setText(mSingerArtist);
            // 3.更新notification通知栏和播放控件UI
            Bitmap mBitmap = MediaUtil.getArtwork(StartMusicActivity.this, mMp3Info.getId(), mMp3Info.getAlbumId(), true, false);
            remoteViews.setImageViewBitmap(R.id.widget_album, mBitmap);
            remoteViews.setTextViewText(R.id.widget_title, mMp3Info.getTitle());
            remoteViews.setTextViewText(R.id.widget_artist, mMp3Info.getArtist());
            refreshPlayStateUI(isPlaying);
            mPv.setCoverBitmap(mBitmap);
            // 4.更换音乐背景
            assert mBitmap != null;
            Palette.from(mBitmap).generate(new Palette.PaletteAsyncListener() {
                @Override
                public void onGenerated(Palette p) {
                    int mutedColor = p.getMutedColor(Color.BLACK);
                    Palette.Swatch darkMutedSwatch = p.getDarkMutedSwatch();      //获取柔和的黑
                    mStartView.setBackgroundColor(darkMutedSwatch != null ? darkMutedSwatch.getRgb() : mutedColor);
                    mLeftView.setBackgroundColor(darkMutedSwatch != null ? darkMutedSwatch.getRgb() : mutedColor);
                }
            });
            // 5.设置歌词
            File mFile = MediaUtil.getLrcFile(mMp3Info.getUrl());
            if (mFile != null) {
                Log.i(TAG, "switchSongUI: mFile != null");
                try {
                    BufferedReader inputStreamReader = new BufferedReader(new InputStreamReader(new FileInputStream(mFile)));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = inputStreamReader.readLine()) != null) {
                        sb.append(line).append('\n');
                    }
                    mCurrentLrc.loadLrc(sb.toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                LrcUtil.getMusicLrc(mMp3Info.getTitle(), mMp3Info.getArtist(), new Subscriber<String>() {
                    @Override
                    public void onComplete(String s) {
                        mCurrentLrc.loadLrc(s);
                        //保存歌词到本地
                        File file = new File(mMp3Info.getUrl().replace(".mp3", ".lrc"));
                        FileOutputStream fileOutputStream;
                        try {
                            fileOutputStream = new FileOutputStream(file);
                            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);
                            outputStreamWriter.write(s);
                            outputStreamWriter.close();
                            fileOutputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }

                    @Override
                    public void onError(Exception e) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mCurrentLrc.reset();
                            }
                        });
                    }
                });
            }
            // 6.选中左侧播放中的歌曲颜色
            changeColorNormalPrv();
            changeColorSelected();
        } else {
            Toast.makeText(this, "当前没有音乐，记得去下载再来。", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 刷新播放控件及通知
     */
    private void refreshPlayStateUI(boolean isPlaying) {
        updateMpv(isPlaying);
        updateNotification();
    }

    /**
     * 更新播放控件
     */
    private void updateMpv(boolean isPlaying) {
        // content播放控件
        if (isPlaying) {
            mPv.start();
        } else {
            mPv.stop();
        }
    }

    private void initEvent(){
        mBack.setOnClickListener(this);
        mPv.setOnClickListener(this);
        mPrevious.setOnClickListener(this);
        mPlayMode.setOnClickListener(this);
        mNext.setOnClickListener(this);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //点击左侧菜单

                sendBroadcast(Constants.ACTION_LIST_ITEM,position);
            }
        });
    }

    /**
     * 更新通知栏UI
     */
    private void updateNotification() {
        Intent intent_play_pause;
        // 创建并设置通知栏
        if (mIsPlaying) {
            remoteViews.setImageViewResource(R.id.widget_play, R.drawable.widget_play);
        } else {
            remoteViews.setImageViewResource(R.id.widget_play, R.drawable.widget_pause);
        }
        // 设置播放
        if (mIsPlaying) {//如果正在播放——》暂停
            intent_play_pause = new Intent();
            intent_play_pause.setAction(Constants.ACTION_PAUSE);
            PendingIntent pending_intent_play = PendingIntent.getBroadcast(this, 4, intent_play_pause, PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViews.setOnClickPendingIntent(R.id.widget_play, pending_intent_play);
        }
        if (!mIsPlaying) {//如果暂停——》播放
            intent_play_pause = new Intent();
            intent_play_pause.setAction(Constants.ACTION_PLAY);
            PendingIntent pending_intent_play = PendingIntent.getBroadcast(this, 5, intent_play_pause, PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViews.setOnClickPendingIntent(R.id.widget_play, pending_intent_play);
        }
        mNotificationManager.notify(Constants.NOTIFICATION_CEDE, mBuilder.build());
    }
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.back:
                //切换左右布局
                mSlidingMenu.toggle();
                break;
            case R.id.mpv:
                //自定义播放控件，点击播放或暂停
                if (mIsPlaying){
                    sendBroadcast(Constants.ACTION_PAUSE);
                } else {
                    sendBroadcast(Constants.ACTION_PLAY);
                }
                break;
            case R.id.previous://上一首
                sendBroadcast(Constants.ACTION_PRV);
                break;
            case R.id.play_mode://q切换播放模式
                MusicService.playMode++;
                switch(MusicService.playMode % 3){
                    case 0:
                        mPlayMode.setImageResource(R.drawable.player_btn_mode_shuffle_normal);
                        break;
                    case 1:
                        mPlayMode.setImageResource(R.drawable.player_btn_mode_loopsingle_normal);
                        break;
                    case 2:
                        mPlayMode.setImageResource(R.drawable.player_btn_mode_playall_normal);
                        break;
                }
                break;
            case R.id.next://下一首
                sendBroadcast(Constants.ACTION_NEXT);
                break;
        }
    }

    private void sendBroadcast(String action){
        Intent intent = new Intent();
        intent.setAction(action);
        sendBroadcast(intent);
    }

    private void sendBroadcast(String action,int postion){
        Intent intent = new Intent();
        intent.setAction(action);
        intent.putExtra("postion",postion);
        sendBroadcast(intent);
    }

    /**
     * 左侧音乐列表适配器
     */
    private class MediaListAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return mMusicList.size();
        }

        @Override
        public Object getItem(int position) {
            return mMusicList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                holder = new ViewHolder();
                convertView = View.inflate(StartMusicActivity.this,R.layout.music_listtem, null);
                holder.mImgAlbum = (ImageView) convertView.findViewById(R.id.img_album);
                holder.mTvTitle = (TextView) convertView.findViewById(R.id.tv_title);
                holder.mTvArtist = (TextView) convertView.findViewById(R.id.tv_artist);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.mImgAlbum.setImageBitmap(MediaUtil.getArtwork(StartMusicActivity.this, mMusicList.get(position).getId(), mMusicList.get(position).getAlbumId(), true, true));
            holder.mTvTitle.setText(mMusicList.get(position).getTitle());
            holder.mTvArtist.setText(mMusicList.get(position).getArtist());

            if (mPosition == position) {
                holder.mTvTitle.setTextColor(getResources().getColor(R.color.colorAccent));
            } else {
                holder.mTvTitle.setTextColor(getResources().getColor(R.color.colorNormal));
            }
            holder.mTvTitle.setTag(position);

            return convertView;
        }
    }

    private static class ViewHolder {
        ImageView mImgAlbum;
        TextView mTvTitle;
        TextView mTvArtist;
    }

    public void changeColorNormal() {
        TextView tv = (TextView) mLeftView.findViewWithTag(mPosition);
        if (tv != null) {
            tv.setTextColor(getResources().getColor(R.color.colorNormal));
        }
    }

    public void changeColorNormalPrv() {
        TextView tv = (TextView) mLeftView.findViewWithTag(MusicService.prv_position);
        if (tv != null) {
            tv.setTextColor(getResources().getColor(R.color.colorNormal));
        }
    }

    public void changeColorSelected() {
        TextView tv = (TextView) mLeftView.findViewWithTag(mPosition);
        if (tv != null) {
            tv.setTextColor(getResources().getColor(R.color.colorAccent));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SpTools.setInt(getApplicationContext(), "music_current_position", mPosition);
    }
}
