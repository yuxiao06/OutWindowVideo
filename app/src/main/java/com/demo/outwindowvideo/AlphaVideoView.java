package com.demo.outwindowvideo;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;

/**
 * 可播放带透明度视频的播放器
 */
public class AlphaVideoView extends GLTextureView
        implements MediaPlayer.OnInfoListener, MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {

    private static final String TAG = "AlphaVideoView";
    private static final int GL_CONTEXT_VERSION = 2;

    /**
     * 当前播放器状态
     */
    private int currentState = PlayerState.IDLE;
    /**
     * 透明视频的模式
     */
    private @AlphaModel
    int alphaModel = AlphaModel.VIDEO_TRANS_LEFT_ALPHA;

    private AlphaVideoRenderer renderer;
    /**
     * mediaPlayer
     */
    private MediaPlayer mediaPlayer;
    private Surface surfaceTexture;
    /**
     * 视频源
     */
    private String assetsFileName;
    /**
     * 标记播放器是否准备就绪
     */
    private boolean isSurfaceCreated;
    private boolean isDataSourceSet;

    private VideoListener.OnBufferingListener bufferingListener;
    private VideoListener.OnErrorListener errorListener;

    // region 构造
    public AlphaVideoView(Context context) {
        this(context, null);
    }

    public AlphaVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        obtainRendererOptions(attrs);
        init();
    }

    /**
     * 获取xml配置的渲染属性
     */
    private void obtainRendererOptions(AttributeSet attrs) {
        if (attrs != null) {
            TypedArray ta = getContext().obtainStyledAttributes(attrs, R.styleable.AlphaVideoView);
            String mode = ta.getString(R.styleable.AlphaVideoView_alphaModel);
            if (!TextUtils.isEmpty(mode)) {
                switch (mode) {
                    case "0":
                        alphaModel = AlphaModel.VIDEO_TRANS_TOP_ALPHA;
                        break;
                    case "1":
                        alphaModel = AlphaModel.VIDEO_TRANS_BOTTOM_ALPHA;
                        break;
                    case "2":
                        alphaModel = AlphaModel.VIDEO_TRANS_LEFT_ALPHA;
                        break;
                    case "3":
                        alphaModel = AlphaModel.VIDEO_TRANS_RIGHT_ALPHA;
                        break;
                    default:
                        break;
                }
            }
            ta.recycle();
        }
    }
    // endregion

    private void init() {
        // 设置OpenGL ES 2.0
        setEGLContextClientVersion(GL_CONTEXT_VERSION);
        // 安装一个配置器
        setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        renderer = new AlphaVideoRenderer(alphaModel);
        renderer.setOnSurfacePrepareListener(surface -> {
            surfaceTexture = surface;
            isSurfaceCreated = true;
            openVideo();
        });
        // 设置渲染器，有些方法必须在此方法之前调用，有些必须在之后调用
        setRenderer(renderer);
        // 控制在暂停和恢复GLTextureView时是否保留EGL上下文
        setPreserveEGLContextOnPause(true);
        // 设置成透明
        setOpaque(false);
        currentState = PlayerState.IDLE;
    }

    public void setVideoFromAssets(String assetsFileName) {
        this.assetsFileName = assetsFileName;
        isDataSourceSet = true;
        openVideo();
    }

    private void openVideo() {
        // 未准备就绪
        if (!isSurfaceCreated || !isDataSourceSet) {
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
            // 设置播放的时候一直让屏幕变亮
            mediaPlayer.setScreenOnWhilePlaying(true);
            mediaPlayer.setSurface(surfaceTexture);
            surfaceTexture.release();
        }
        mediaPlayer.setOnInfoListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnCompletionListener(this);
        currentState = PlayerState.IDLE;
    }

    public void setVideoDataSourceFromAssets() {
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
        }
        return false;
    }
    // endregion

    // region 视频状态监听
    public void setOnBufferingListener(VideoListener.OnBufferingListener listener) {
        this.bufferingListener = listener;
    }

    public void setOnErrorListener(VideoListener.OnErrorListener listener) {
        this.errorListener = listener;
    }
    // endregion
}
