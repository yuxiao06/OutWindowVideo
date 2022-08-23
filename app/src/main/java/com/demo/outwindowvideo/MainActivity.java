package com.demo.outwindowvideo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

public class MainActivity extends AppCompatActivity {

    private VideoView backVideoView;
    private ImageView backVideoCover;
    private AlphaVideoView frontVideoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_pyq_item);
        init();
    }

    private void init() {
        initView();
        initVideoListener();
    }

    private void initView() {
        backVideoView = findViewById(R.id.back_video);
        frontVideoView = findViewById(R.id.front_video);
        backVideoCover = findViewById(R.id.back_video_cover);

        // 添加评论假数据
        FakeCommentUtil.addFakeComment(this, findViewById(R.id.comment_container));
        // 设置播放资源
        backVideoView.setVideoFromAssets("backVideo.mp4");
        frontVideoView.setVideoFromAssets("frontVideo.mp4");
        // 设置封面
        backVideoCover.setImageBitmap(backVideoView.getFirstFrame());
        // 播放视频
        frontVideoView.postDelayed(this::startAllVideo, 500);
    }

    private void initVideoListener() {
        // 监听首帧出现
        backVideoView.setOnFrameListener(() -> backVideoCover.setVisibility(View.GONE));
        // 播放错误监听
        frontVideoView.setOnErrorListener((mp, frameworkErr, implErr) -> stopAllVideo());
        backVideoView.setOnErrorListener((mp, frameworkErr, implErr) -> stopAllVideo());
        // 缓存状态监听
        frontVideoView.setOnBufferingListener(new VideoListener.OnBufferingListener() {
            @Override
            public void onStartBuffering() {
                pauseAllVideo();
            }

            @Override
            public void onEndBuffering() {
                startAllVideo();
            }
        });
        backVideoView.setOnBufferingListener(new VideoListener.OnBufferingListener() {
            @Override
            public void onStartBuffering() {
                pauseAllVideo();
            }

            @Override
            public void onEndBuffering() {
                startAllVideo();
            }
        });
    }

    /**
     * 播放视频
     */
    private void startAllVideo() {
        backVideoView.start();
        frontVideoView.start();
    }

    /**
     * 暂停播放视频
     */
    private void pauseAllVideo() {
        backVideoView.pause();
        frontVideoView.pause();
    }

    /**
     * 停止播放视频
     */
    private void stopAllVideo() {
        backVideoView.stop();
        frontVideoView.stop();
    }

    /**
     * 释放视频
     */
    private void releaseAllVideo() {
        backVideoView.release();
        frontVideoView.release();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseAllVideo();
    }
}