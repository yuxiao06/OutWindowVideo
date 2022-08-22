package com.demo.outwindowvideo;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @since 2022/8/9
 */
@IntDef({
        AlphaModel.VIDEO_TRANS_TOP_ALPHA,
        AlphaModel.VIDEO_TRANS_BOTTOM_ALPHA,
        AlphaModel.VIDEO_TRANS_LEFT_ALPHA,
        AlphaModel.VIDEO_TRANS_RIGHT_ALPHA
})
@Retention(RetentionPolicy.SOURCE)
public @interface AlphaModel {
    // 上面透明，下面彩色
    int VIDEO_TRANS_TOP_ALPHA = 0;
    // 上面是彩色，下面是透明通道
    int VIDEO_TRANS_BOTTOM_ALPHA = 1;
    // 左边是透明通道，右边是彩色
    int VIDEO_TRANS_LEFT_ALPHA = 2;
    // 左边是彩色，右边是透明通道
    int VIDEO_TRANS_RIGHT_ALPHA = 3;
}
