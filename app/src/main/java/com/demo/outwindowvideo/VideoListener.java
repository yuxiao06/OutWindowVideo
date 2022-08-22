package com.demo.outwindowvideo;

import android.media.MediaPlayer;

/**
 * @since 2022/8/15
 */
public class VideoListener {

    /**
     * 缓冲监听
     */
    public interface OnBufferingListener {
        /**
         * 开始缓冲
         */
        void onStartBuffering();

        /**
         * 缓冲结束
         */
        void onEndBuffering();
    }

    /**
     * 视频 播放画面 回调
     */
    public interface OnFrameListener {
        /**
         * 出现首帧画面
         */
        void onVideoShowFrame();
    }

    /**
     * 播放错误监听
     */
    public interface OnErrorListener {
        void onVideoError(MediaPlayer mp, int frameworkErr, int implErr);
    }
}
