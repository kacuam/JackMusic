package com.example.jackmusic;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import com.example.jackmusic.service.MusicService;
import com.example.jackmusic.util.Constants;
import com.example.jackmusic.util.StatusBarUtil;
import com.example.jackmusic.view.LrcView;
import com.example.jackmusic.view.MusicPlayerView;
import com.example.jackmusic.view.RoundImageView;
import com.example.jackmusic.view.SlidingMenu;

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

    private RemoteViews remoteViews;

    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder mBuilder;
    private boolean mIsPlaying = false;

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

        //启动音乐服务

        //消息管理

        //初始化控件UI，默认显示历史播放歌曲

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

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.back:
                //切换左右布局

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
}
