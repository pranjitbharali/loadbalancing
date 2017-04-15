package com.example.jit.multicast_demo;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;

import java.io.File;
import java.io.IOException;

import static java.security.AccessController.getContext;

public class Play extends AppCompatActivity implements SurfaceHolder.Callback,
        MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener,
        View.OnClickListener {
    final String path = Environment.getExternalStorageDirectory().getPath()+"/video";
    private final String TAG = "VIDEOPLAYER";
    private MediaPlayer mMediaPlayer;
    private SurfaceView mPreview;
    private SurfaceHolder holder;
    private ImageView pause;
    private int currentVid;
    private int noofseeders;
    private boolean buffering;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_play);

        System.out.println("PLAYING");

        currentVid = 0;
        buffering = true;

        Bundle bun = getIntent().getExtras();
        noofseeders=-1;
        if(bun!=null)
            noofseeders = bun.getInt("noofseeders");

        pause = (ImageView) findViewById(R.id.iv);
        pause.setVisibility(View.VISIBLE);
//        if (mMediaPlayer != null) {
//            if (!mMediaPlayer.isPlaying()) {
//                pause.setVisibility(View.VISIBLE);
//            }
//        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions( this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},1);
        }

        mPreview = (SurfaceView) findViewById(R.id.sv);
        mPreview.setOnClickListener(this);

        holder = mPreview.getHolder();
        holder.addCallback(this);

        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnCompletionListener(this);
    }

    protected void playVideo() {
        System.out.println("playVideo called for "+currentVid);
        if(currentVid >= noofseeders)
            return;
        try {
            mMediaPlayer.setDisplay(holder);
            Display.semarr[currentVid].acquire();
//            while(true) {
//                Display.sema.acquire();
//                try {
//                    if (Display.check.containsKey(currentVid))
//                        break;
//                    System.out.println("Checked Unsuccessfully");
//                }catch(Exception e) {
//                    e.printStackTrace();
//                } finally {
//                    Display.sema.release();
//                }
//                Thread.sleep(1000);
//            }
            System.out.println("playing "+currentVid);
            buffering = false;
            mMediaPlayer.setDataSource(path +"/"+ "playTempVid" + currentVid + ".mp4");
            currentVid++;
            mMediaPlayer.prepare();
        } catch (Exception e) {
            Log.i(TAG, "Error");
            e.printStackTrace();
        }
    }

    public void onCompletion(MediaPlayer arg0) {
        Log.d(TAG, "onCompletion called");
        new Thread(new Runnable() {
            @Override
            public void run() {
                buffering = true;
                mMediaPlayer.reset();
                mMediaPlayer.setOnCompletionListener(Play.this);
                mMediaPlayer.setOnPreparedListener(Play.this);
                File fl = new File(path +"/"+ "playTempVid" + (currentVid-1) + ".mp4");
                fl.delete();
                playVideo();
            }
        }).start();
    }

    public void onPrepared(MediaPlayer mediaplayer) {
        Log.d(TAG, "onPrepared called");
        int mVideoWidth = mMediaPlayer.getVideoWidth();
        int mVideoHeight = mMediaPlayer.getVideoHeight();
        float videoProportion = (float) mVideoWidth / (float)mVideoHeight;
        int screenWidth = ((Activity)this).getWindowManager().getDefaultDisplay().getWidth();
        int screenHeight = ((Activity)this).getWindowManager().getDefaultDisplay().getHeight();
        float screenProportion = (float) screenWidth / (float) screenHeight;
        android.view.ViewGroup.LayoutParams lp = mPreview.getLayoutParams();
        System.out.println("ScreenWidth: "+screenWidth + " ScreenHeight: "+screenHeight);
        if (videoProportion > screenProportion) {
            //System.out.println("VP>SP");
            lp.width = screenWidth;
            lp.height = (int) ((float) screenWidth / videoProportion);
        } else {
            //System.out.println("SP>VP");
            lp.width = (int) (videoProportion * (float) screenHeight);
            lp.height = screenHeight;
        }
        if(!mMediaPlayer.isPlaying()) {
            mMediaPlayer.start();
            pause.setVisibility(View.GONE);
        }
        mPreview.setLayoutParams(lp);
        mPreview.setClickable(true);
    }

    public void onClick(View v) {
        if(buffering)
            return;
        if (v.getId() == R.id.sv) {
            if (mMediaPlayer != null) {
                if (mMediaPlayer.isPlaying()) {
                    mMediaPlayer.pause();
                    pause.setVisibility(View.VISIBLE);
                } else {
                    mMediaPlayer.start();
                    pause.setVisibility(View.GONE);
                }
            }
        }
    }

    public void surfaceChanged(SurfaceHolder surfaceholder, int i, int j, int k) {
        Log.d(TAG, "surfaceChanged called");
    }
    public void surfaceDestroyed(SurfaceHolder surfaceholder) {
        Log.d(TAG, "surfaceDestroyed called");
    }
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "surfaceCreated called");
        playVideo();
    }
}
