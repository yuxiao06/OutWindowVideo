package com.demo.outwindowvideo;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

/**
 * @since 2022/8/9
 */
public class VideoView extends SurfaceView
        implements SurfaceHolder.Callback, MediaPlayer.OnInfoListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener {

    private static final String TAG = "VideoView";

    /**
     * 当前播放器状态
     */
    private int currentState = PlayerState.IDLE;
    /**
     * 标记播放器是否准备就绪
     */
    private boolean isSurfaceCreated;
    /**
     * mediaPlayer
     */
    private MediaPlayer mediaPlayer = null;
    /**
     * 视频源
     */
    private String assetsFileName;

    private VideoListener.OnBufferingListener bufferingListener;
    private VideoListener.OnFrameListener frameListener;
    private VideoListener.OnErrorListener errorListener;

    // region 构造
    public VideoView(Context context) {
        this(context, null);
    }

    public VideoView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VideoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initSurfaceHolder();
    }
    // endregion

    private void initSurfaceHolder() {
        getHolder().addCallback(this);
        currentState = PlayerState.IDLE;
    }

    // region surface回调
    @Override
    public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
        this.isSurfaceCreated = true;
        openVideo();
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2) {
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {
        this.isSurfaceCreated = false;
        release();
    }
    // endregion


    public void setVideoFromAssets(String assetsFileName) {
        this.assetsFileName = assetsFileName;
        openVideo();
    }

    private void openVideo() {
        if (TextUtils.isEmpty(assetsFileName) || !isSurfaceCreated) {
            return;
        }
        stop();

        initMediaPlayer();
        setVideoDataSourceFromAssets();
        currentState = PlayerState.PREPARING;
        prepareAsync();
    }

    private void initMediaPlayer() {
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDisplay(getHolder());
        }
        mediaPlayer.setOnInfoListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnCompletionListener(this);
        currentState = PlayerState.IDLE;
    }

    private void setVideoDataSourceFromAssets() {
        try {
            AssetFileDescriptor assetFileDescriptor = getContext().getAssets().openFd
                    (assetsFileName);
            mediaPlayer.setDataSource(assetFileDescriptor.getFileDescriptor(),
                    assetFileDescriptor.getStartOffset(), assetFileDescriptor.getLength());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // region 播放器状态
    public void start() {
        if (mediaPlayer == null) {
            return;
        }
        switch (currentState) {
            case PlayerState.PREPARED:
            case PlayerState.PAUSED:
            case PlayerState.PLAYBACK_COMPLETED:
                mediaPlayer.start();
                currentState = PlayerState.PLAYING;
                break;
            case PlayerState.STOPPED:
                prepareAsync(mediaPlayer -> {
                    mediaPlayer.start();
                    currentState = PlayerState.PLAYING;
                });
                break;
        }
    }

    public void pause() {
        if (mediaPlayer == null) {
            return;
        }
        if (currentState == PlayerState.PLAYING) {
            mediaPlayer.pause();
            currentState = PlayerState.PAUSED;
        }
    }

    public void stop() {
        if (mediaPlayer == null) {
            return;
        }
        switch (currentState) {
            case PlayerState.PREPARED:
            case PlayerState.PLAYING:
            case PlayerState.PAUSED:
            case PlayerState.PLAYBACK_COMPLETED:
                mediaPlayer.stop();
                currentState = PlayerState.STOPPED;
                break;
        }
    }

    public void release() {
        if (mediaPlayer != null) {
            mediaPlayer.reset();
            mediaPlayer.release();
            mediaPlayer.setOnInfoListener(null);
            mediaPlayer.setOnErrorListener(null);
            mediaPlayer.setOnCompletionListener(null);
            mediaPlayer = null;
            currentState = PlayerState.END;
        }
    }

    private void prepareAsync() {
        prepareAsync(null);
    }

    private void prepareAsync(final MediaPlayer.OnPreparedListener onPreparedListener) {
        if (mediaPlayer == null) {
            return;
        }
        if (currentState == PlayerState.PREPARING || currentState == PlayerState.STOPPED) {
            mediaPlayer.setOnPreparedListener(mp -> {
                currentState = PlayerState.PREPARED;
                if (onPreparedListener != null) {
                    onPreparedListener.onPrepared(mp);
                }
            });
            mediaPlayer.prepareAsync();
        }
    }
    // endregion

    // region 播放器回调
    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        currentState = PlayerState.PLAYBACK_COMPLETED;
    }

    @Override
    public boolean onError(MediaPlayer mp, int frameworkErr, int implErr) {
        currentState = PlayerState.ERROR;
        if (errorListener != null) {
            errorListener.onVideoError(mp, frameworkErr, implErr);
        }
        Log.w(TAG, "Error: " + frameworkErr + "," + implErr);
        return true;
    }

    @Override
    public boolean onInfo(MediaPlayer mediaPlayer, int what, int extra) {
        switch (what) {
            // 开始卡顿
            case MediaPlayer.MEDIA_INFO_BUFFERING_START:
                if (bufferingListener != null) {
                    bufferingListener.onStartBuffering();
                }
                break;
            // 卡顿结束
            case MediaPlayer.MEDIA_INFO_BUFFERING_END:
                if (bufferingListener != null) {
                    bufferingListener.onEndBuffering();
                }
                break;
            // 首帧渲染
            case MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START:
                if (frameListener != null) {
                    frameListener.onVideoShowFrame();
                }
                break;
        }
        return false;
    }
    // endregion

    // region 视频状态监听
    public void setOnBufferingListener(VideoListener.OnBufferingListener listener) {
        this.bufferingListener = listener;
    }

    public void setOnFrameListener(VideoListener.OnFrameListener listener) {
        this.frameListener = listener;
    }

    public void setOnErrorListener(VideoListener.OnErrorListener listener) {
        this.errorListener = listener;
    }
    // endregion

    /**
     * 获取首帧图
     */
    public Bitmap getFirstFrame() {
        if (TextUtils.isEmpty(assetsFileName)) {
            return null;
        }
        Bitmap firstFrame = null;
        try {
            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            AssetFileDescriptor assetFileDescriptor = getContext().getAssets().openFd(assetsFileName);
            mmr.setDataSource(assetFileDescriptor.getFileDescriptor(),
                    assetFileDescriptor.getStartOffset(), assetFileDescriptor.getLength());
            firstFrame = mmr.getFrameAtTime();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return firstFrame;
    }
}