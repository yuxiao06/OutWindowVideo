package com.demo.outwindowvideo;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 视频播放器状态
 *
 * @since 2022/8/11
 */
@IntDef({
        PlayerState.END,
        PlayerState.ERROR,
        PlayerState.IDLE,
        PlayerState.PREPARING,
        PlayerState.PREPARED,
        PlayerState.PLAYING,
        PlayerState.PAUSED,
        PlayerState.STOPPED,
        PlayerState.PLAYBACK_COMPLETED
})
@Retention(RetentionPolicy.SOURCE)
public @interface PlayerState {
    int END = -2;
    int ERROR = -1;
    int IDLE = 0;
    int PREPARING = 1;
    int PREPARED = 2;
    int PLAYING = 3;
    int PAUSED = 4;
    int STOPPED = 5;
    int PLAYBACK_COMPLETED = 6;
}