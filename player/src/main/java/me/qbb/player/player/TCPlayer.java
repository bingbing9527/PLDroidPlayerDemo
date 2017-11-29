package me.qbb.player.player;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.danikula.videocache.HttpProxyCacheServer;
import com.facebook.drawee.view.SimpleDraweeView;
import com.pili.pldroid.player.IMediaController;
import com.pili.pldroid.player.PLMediaPlayer;

import java.io.File;
import java.util.Locale;

import me.qbb.player.App;
import me.qbb.player.R;
import me.qbb.player.utils.FileUtils;
import me.qbb.player.utils.ImageLoader;
import me.qbb.player.utils.LogUtil;
import me.qbb.player.utils.NetUtilss;
import rx.Observable;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

/**
 * 创建时间 2017/10/26 16:17
 *
 * @author Qian Bing Bing
 *         类说明
 */

public class TCPlayer extends FrameLayout implements IMediaController, View.OnClickListener {

    public static final int NET_NO = 0;
    public static final int NET_WIFI = 1;
    public static final int NET_4G = 2;

    public static int sDefaultTimeout = 3000;
    private static final int SEEK_TO_POST_DELAY_MILLIS = 200;
    private static final int FADE_OUT = 1;
    private static final int SHOW_PROGRESS = 2;
    @NonNull
    private final Context context;

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            long pos;
            switch (msg.what) {
                case FADE_OUT:
                    try {
                        hide();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case SHOW_PROGRESS:
                    try {
                        pos = setProgress();
                        if (!mDragging && mShowing) {
                            msg = obtainMessage(SHOW_PROGRESS);
                            sendMessageDelayed(msg, 1000 - (pos % 1000));
                            updatePausePlay();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                default:
                    break;
            }
        }
    };

    private ImageView full;
    private SeekBar mProgress;
    private TextView mEndTime;
    private ImageView noFull;
    private ImageView mPauseButton;
    private TextView mCurrentTime;
    private TextView tvVideoBack;
    private TextView tvVideoTitle;
    private ImageView ivVideoBack;

    private boolean isVip;
    public LinearLayout normalLoading;
    public LinearLayout vipLogo;
    private SimpleDraweeView sdv_video_loading;

    private Runnable mLastSeekBarRunnable;
    private boolean mDragging;
    private AudioManager mAM;
    private MediaPlayerControl mPlayer;
    private boolean mShowing;
    private LinearLayout controller;
    private LinearLayout llVideoTitle;
    private View mAnchor;
    private Activity mActivity;
    private boolean mDisableProgress = false;
    private long mDuration;
    private boolean mInstantSeeking = true;
    private RelativeLayout player_container;
    private int delayedTime;
    private SimpleDraweeView videoBg;
    private LinearLayout errorLoading;
    private LinearLayout llReconnect;
    private FrameLayout super_video;
    private TCPlayerView videoView;
    /**
     * 播放器是否准备好了
     */
    private boolean isPrepared = false;
    /**
     * 是否需要缓存
     */
    private boolean needCache;
    private long errorPosition;
    private HttpProxyCacheServer proxy;
    private String proxyUrl;
    private String url;
    private String oldUrl = "";
    private boolean isLive;
    private boolean showTitleLayout;
    private boolean isFirst = true;
    private boolean playing;
    private String videoCachePath;
    private boolean isListen;
    /**
     * 4G网络提示
     */
    private AlertDialog alertDialog;
    private boolean hasDialog;
    private boolean completion;
    private OnErrorListener mOnErrorListener;
    private OnCompletionListener mOnCompletionListener;
    private OnInfoListener mOnInfoListener;
    private OnBufferingUpdateListener mOnBufferingUpdateListener;
    private boolean showSmallSrceen;

    public TCPlayer(@NonNull Context context) {
        this(context, null);
    }

    public TCPlayer(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TCPlayer(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        mActivity = ((Activity) this.context);
        videoView = TCPlayerManager.getTcPlayerManager().initialize(context);
        initView();
    }

    private void initView() {
        LayoutInflater.from(getContext()).inflate(R.layout.view_video, this);
        //所有view容器
        player_container = (RelativeLayout) findViewById(R.id.player_container);
        //播放器容器
        super_video = (FrameLayout) findViewById(R.id.super_video);
//        videoView = (TCPlayer) findViewById(R.id.video_view);
        //标题
        llVideoTitle = (LinearLayout) findViewById(R.id.ll_video_title);
        ivVideoBack = (ImageView) findViewById(R.id.iv_video_back);
        tvVideoBack = (TextView) findViewById(R.id.tv_video_back);
        tvVideoTitle = (TextView) findViewById(R.id.tv_video_title);
        //控制器
        controller = (LinearLayout) findViewById(R.id.controller);
        full = (ImageView) findViewById(R.id.full);
        mProgress = (SeekBar) findViewById(R.id.seekBar);
        if (mProgress != null) {
            mProgress.setOnSeekBarChangeListener(mSeekListener);
            mProgress.setThumbOffset(1);
            mProgress.setMax(1000);
            mProgress.setEnabled(!mDisableProgress);
        }
        mPauseButton = (ImageView) findViewById(R.id.iv_play);
        mEndTime = (TextView) findViewById(R.id.tv_time);
        noFull = (ImageView) findViewById(R.id.no_full);
        mCurrentTime = (TextView) findViewById(R.id.tv_use_time);
        mPauseButton.requestFocus();
        mPauseButton.setImageResource(R.mipmap.vip_play);
        //loading
        sdv_video_loading = (SimpleDraweeView) findViewById(R.id.sdv_video_loading);
        vipLogo = (LinearLayout) findViewById(R.id.ll_vip_logo);
        normalLoading = (LinearLayout) findViewById(R.id.ll_loading);
        ImageLoader.loaderGIF(sdv_video_loading, Uri.parse("res://" + getContext().getPackageName() + "/" + R.drawable.video_loading));
        //错误loading
        errorLoading = (LinearLayout) findViewById(R.id.ll_loading_error);
        //重新连接
        llReconnect = (LinearLayout) findViewById(R.id.ll_reconnect);
        //背景
        videoBg = (SimpleDraweeView) findViewById(R.id.video_bg);

        try {
            initEvent();

            initVideo();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 初始化播放器
     */
    private void initVideo() {
        addPlayer(videoView);
        mAM = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        videoView.setCoverView(videoBg);
        //初始化缓存代理
        initProxy();
    }

    private void initProxy() {
        proxy = App.getProxy(getContext());
    }


    /**
     * 初始化事件
     */
    private void initEvent() {
        //控制器监听
        mPauseButton.setOnClickListener(this);
        full.setOnClickListener(this);
        noFull.setOnClickListener(this);
        llReconnect.setOnClickListener(this);
        vipLogo.setOnClickListener(this);
        videoBg.setOnClickListener(this);
        ivVideoBack.setOnClickListener(this);
        //播放器监听
        videoView
                .onPrepared(mOnTCPlayerPreparedListener)
                .onInfo(mOnTCPlayerInfoListener)
                .onBuffering(mOnTCPlayBufferingUpdateListener)
                .onError(mOnTCPlayerErrorListener)
                .onComplete(mOnTCPlayerCompletionListener);
    }

    private TCPlayerListener.OnTCPlayerPreparedListener mOnTCPlayerPreparedListener = new TCPlayerListener.OnTCPlayerPreparedListener() {
        @Override
        public void onTCPlayerPrepared(TCPlayerView tcPlayerView) {
            isPrepared = true;
        }
    };

    private TCPlayerListener.OnTCPlayerInfoListener mOnTCPlayerInfoListener = new TCPlayerListener.OnTCPlayerInfoListener() {
        @Override
        public boolean onTCPlayerInfo(TCPlayerView tcPlayerView, int what, int extra) {
            //第一帧视频已成功渲染
            if (PLMediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START == what) {
                completion = false;
                hideVipLogo();
                delayedTime = 0;
                normalLoading.setVisibility(GONE);
                videoView.setBufferingIndicator(normalLoading);
                isFirst = false;
            }
            if (mOnInfoListener != null) {
                return mOnInfoListener.onInfo(TCPlayer.this, what, extra);
            }
            return false;
        }
    };

    private TCPlayerListener.OnTCPlayBufferingUpdateListener mOnTCPlayBufferingUpdateListener = new TCPlayerListener.OnTCPlayBufferingUpdateListener() {
        @Override
        public void onBufferingUpdate(TCPlayerView tcPlayerView, int percent) {
            if (!isLive) {
                if (videoView != null) {
                    videoView.pause();
                }
            }
            if (mOnBufferingUpdateListener != null) {
                mOnBufferingUpdateListener.onBufferingUpdate(TCPlayer.this, percent);
            }
        }
    };

    private TCPlayerListener.OnTCPlayerErrorListener mOnTCPlayerErrorListener = new TCPlayerListener.OnTCPlayerErrorListener() {
        @Override
        public void onError(TCPlayerView tcPlayerView, int errorCode, long position) {
            if (TCPlayerView.UNKOWN_ERROR_CODE == errorCode) {
                if (needCache) {
                    if (videoView != null) {
                        deleteCache();
                        videoView.setVideoPath(oldUrl);
                        play(position);
                    }
                } else {
                    errorPosition = position;
                    showErrorLoading();
                }
            } else {
                errorPosition = position;
                showErrorLoading();
            }
            if (mOnErrorListener != null) {
                mOnErrorListener.onError(TCPlayer.this, errorCode);
            }
        }

    };


    private TCPlayerListener.OnTCPlayerCompletionListener mOnTCPlayerCompletionListener = new TCPlayerListener.OnTCPlayerCompletionListener() {
        @Override
        public void onCompletion(TCPlayerView tcPlayerView) {
            completion = true;
            if (mOnCompletionListener != null) {
                mOnCompletionListener.onCompletion(TCPlayer.this);
            }
        }
    };


    @Override
    public void setMediaPlayer(MediaPlayerControl player) {
        mPlayer = player;
        updatePausePlay();
    }


    /**
     * Control the action when the seekbar dragged by user
     *
     * @param seekWhenDragging True the media will seek periodically
     */
    public void setInstantSeeking(boolean seekWhenDragging) {
        mInstantSeeking = seekWhenDragging;
    }

    private void updatePausePlay() {
//        if (mPauseButton == null || mPlayer == null || !isFirstFrame) {
        if (mPauseButton == null || mPlayer == null || isFirst) {
            return;
        }
        if (mPlayer.isPlaying()) {
            mPauseButton.setImageResource(R.mipmap.vip_pause);
        } else {
            mPauseButton.setImageResource(R.mipmap.vip_play);
        }
    }

    @Override
    public void show() {
        show(sDefaultTimeout);
    }

    @Override
    public void show(int timeout) {

        if (showSmallSrceen) {
            noAnimationHideAll();
            return;
        }

        if (!mShowing) {
            if (mPauseButton != null) {
                mPauseButton.requestFocus();
            }
            //标题
            if (showTitleLayout) {
                Animation titleShowAni = AnimationUtils.loadAnimation(getContext(), R.anim.video_title_enter);
                titleShowAni.setAnimationListener(new SimpleAnimationListener() {
                    @Override
                    public void onAnimationEnd(Animation animation) {
                        super.onAnimationEnd(animation);
                        if (llVideoTitle != null) {
                            llVideoTitle.setVisibility(VISIBLE);
                        }
                    }
                });
                llVideoTitle.startAnimation(titleShowAni);
            }
            //控制器
            Animation controllerShowAni = AnimationUtils.loadAnimation(getContext(), R.anim.controller_enter);
            controllerShowAni.setAnimationListener(new SimpleAnimationListener() {

                @Override
                public void onAnimationEnd(Animation animation) {
                    super.onAnimationEnd(animation);
                    if (controller != null) {
                        controller.setVisibility(VISIBLE);
                    }
                }
            });
            controller.startAnimation(controllerShowAni);

            mShowing = true;
        }
        updatePausePlay();
        mHandler.sendEmptyMessage(SHOW_PROGRESS);

        if (timeout != 0) {
            mHandler.removeMessages(FADE_OUT);
            mHandler.sendMessageDelayed(mHandler.obtainMessage(FADE_OUT),
                    timeout);
        }
    }

    @Override
    public void hide() {
        //标题
        if (showTitleLayout) {
            Animation titleHideAni = AnimationUtils.loadAnimation(getContext(), R.anim.video_title_exit);
            titleHideAni.setAnimationListener(new SimpleAnimationListener() {
                @Override
                public void onAnimationEnd(Animation animation) {
                    super.onAnimationEnd(animation);
                    if (llVideoTitle != null) {
                        llVideoTitle.setVisibility(GONE);
                    }
                }
            });
            llVideoTitle.startAnimation(titleHideAni);
        }

        //控制器
        Animation controllerHideAni = AnimationUtils.loadAnimation(getContext(), R.anim.controller_exit);
        controllerHideAni.setAnimationListener(new SimpleAnimationListener() {

            @Override
            public void onAnimationEnd(Animation animation) {
                super.onAnimationEnd(animation);
                if (controller != null) {
                    controller.setVisibility(GONE);
                }
            }
        });
        controller.startAnimation(controllerHideAni);

        mHandler.removeMessages(SHOW_PROGRESS);
        mShowing = false;
    }

    /**
     * 直接隐藏标题和控制器
     */
    public void noAnimationHideAll() {
        if (showTitleLayout) {
            //标题
            llVideoTitle.clearAnimation();
            llVideoTitle.setVisibility(GONE);
        }
        //控制器
        controller.clearAnimation();
        controller.setVisibility(GONE);

        mHandler.removeMessages(SHOW_PROGRESS);
        mShowing = false;

    }

    /**
     * 直接显示标题和控制器
     */
    public void noAnimationShowAll() {
        noAnimationShowAll(sDefaultTimeout);

    }

    public void noAnimationShowAll(int timeout) {
        if (mPauseButton != null) {
            mPauseButton.requestFocus();
        }
        if (showTitleLayout) {
            //标题
            llVideoTitle.clearAnimation();
            llVideoTitle.setVisibility(VISIBLE);
        }
        //控制器
        controller.clearAnimation();
        controller.setVisibility(VISIBLE);

        mShowing = true;
        updatePausePlay();

        if (isPlaying()) {
            mHandler.sendEmptyMessage(SHOW_PROGRESS);
        }

        if (timeout != 0) {
            mHandler.removeMessages(FADE_OUT);
            mHandler.sendMessageDelayed(mHandler.obtainMessage(FADE_OUT),
                    timeout);
        }
    }


    @Override
    public boolean isShowing() {
        return mShowing;
    }

    @Override
    public void setAnchorView(View view) {
        mAnchor = view;
        if (mAnchor == null) {
            sDefaultTimeout = 0; // show forever
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (mPauseButton != null) {
            LogUtil.e("---------" + enabled);
//            mPauseButton.setEnabled(enabled);
        }
        if (mProgress != null && !mDisableProgress) {
            mProgress.setEnabled(enabled);
        }
        disableUnsupportedButtons();
        super.setEnabled(enabled);
    }

    private void disableUnsupportedButtons() {
        try {
            if (mPauseButton != null && mPlayer != null && !mPlayer.canPause()) {
                mPauseButton.setEnabled(false);
            }
        } catch (IncompatibleClassChangeError ex) {
            ex.printStackTrace();
        }
    }

    private void doPauseResume() {
        doPauseResume(0);
    }

    private void doPauseResume(final long position) {
        //设置视频路径
        //url更新，重新设置视频路径
        if (!oldUrl.equals(url)) {
            //需要缓存，点击播放的时候在初始化代理
            if (needCache || proxy.isCached(url)) {
                proxyUrl = proxy.getProxyUrl(url);
            }
            if (videoView != null) {
                videoView.setVideoPath((needCache || proxy.isCached(url)) ? proxyUrl : url);
            }
            oldUrl = url;
        }

        if (mPlayer == null) {
            return;
        }
        hideVipLogo();
        hideVideoBg();
        hideErrorLoading();
        if (videoView != null) {
            normalLoading.setVisibility(VISIBLE);
            videoView.setBufferingIndicator(normalLoading);
        }
        if (mPlayer.isPlaying()) {
            mPlayer.pause();
            playing = false;
        } else {

            if (position != 0) {
                mPlayer.seekTo(position);
            }
            mPlayer.start();
            playing = true;
        }
        updatePausePlay();
    }


    private SeekBar.OnSeekBarChangeListener mSeekListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onStartTrackingTouch(SeekBar bar) {
            if (mPlayer == null) {
                return;
            }
            mDragging = true;
            show(3600000);
            mHandler.removeMessages(SHOW_PROGRESS);
            if (mInstantSeeking) {
                mAM.setStreamMute(AudioManager.STREAM_MUSIC, true);
            }
        }

        @Override
        public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {
            if (!fromuser || mPlayer == null) {
                return;
            }
            final long newposition = (long) (mDuration * progress) / 1000;
            String time = generateTime(newposition);
            if (mInstantSeeking) {
                mHandler.removeCallbacks(mLastSeekBarRunnable);
                mLastSeekBarRunnable = new Runnable() {
                    @Override
                    public void run() {
                        mPlayer.seekTo(newposition);
                    }
                };
                mHandler.postDelayed(mLastSeekBarRunnable, SEEK_TO_POST_DELAY_MILLIS);
            }
            if (mCurrentTime != null) {
                mCurrentTime.setText(time);
            }
        }

        @Override
        public void onStopTrackingTouch(SeekBar bar) {
            if (!mInstantSeeking) {
                mPlayer.seekTo(mDuration * bar.getProgress() / 1000);
            }
            show(sDefaultTimeout);
            mHandler.removeMessages(SHOW_PROGRESS);
            mAM.setStreamMute(AudioManager.STREAM_MUSIC, false);
            mDragging = false;
            mHandler.sendEmptyMessageDelayed(SHOW_PROGRESS, 1000);
        }
    };

    /**
     * 设置进度条
     *
     * @return 进度信息
     */
    private long setProgress() {

        if (mPlayer == null || mDragging) {
            return 0;
        }

        long position = mPlayer.getCurrentPosition();
        long duration = mPlayer.getDuration();
        if (mProgress != null) {
            if (duration > 0) {
                long pos = 1000L * position / duration;
                mProgress.setProgress((int) pos);
            }
            int percent = mPlayer.getBufferPercentage();
            mProgress.setSecondaryProgress(percent * 10);
        }
        mDuration = duration;

        if (mEndTime != null) {
            mEndTime.setText(generateTime(mDuration));
        }
        if (mCurrentTime != null) {
            mCurrentTime.setText(generateTime(position));
//            //播放时间和结束时间一致并且进度条>900，设置进入条诶1000
//            if (mCurrentTime.getText().equals(mCurrentTime != null ? mCurrentTime.getText() : "") && mProgress.getProgress() > 900) {
//                mProgress.setProgress((int) 1000L);
//            }
        }
        return position;

    }

    /**
     * 格式化时间
     *
     * @param position 时间，单位毫秒
     * @return
     */
    private static String generateTime(long position) {
        int totalSeconds = (int) (position / 1000);

        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;

        if (hours > 0) {
            return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes,
                    seconds);
        } else {
            return String.format(Locale.US, "%02d:%02d", minutes, seconds);
        }
    }

    /**
     * 是否全屏。此方法和showSmallSrceen方法，必须调用其中一个
     *
     * @param showFull true 全屏，false 非全屏（16:9）
     */
    public TCPlayer showFull(boolean showFull) {
        showSmallSrceen = false;
        full.setVisibility(showFull ? View.GONE : VISIBLE);
        noFull.setVisibility(showFull ? View.VISIBLE : GONE);
        if (mActivity != null) {
            mActivity.setRequestedOrientation(showFull ? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        reSizeLayout16x9(player_container, showFull ? 1 : 0);
        reSizeLayout16x9(super_video, showFull ? 1 : 0);
        reSizeLayout16x9(videoBg, showFull ? 1 : 0);
        return this;
    }

    /**
     * 设置为16x9
     *
     * @param v
     * @param type 0,半屏 1全屏
     */
    public void reSizeLayout16x9(View v, int type) {
        DisplayMetrics dm2 = getResources().getDisplayMetrics();
        int width = dm2.widthPixels;
        int height = width * 9 / 16;
        ViewGroup.LayoutParams params = v.getLayoutParams();
        if (params != null) {
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            if (type == 0) {
                params.height = height;
            } else {
                params.height = dm2.heightPixels;
            }
        } else {
            if (type == 0) {
                params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height);
            } else {
                params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            }
        }
        v.setLayoutParams(params);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_video_back:
                if (mOnClickBackIconListener != null) {
                    mOnClickBackIconListener.onClickBackIcon(v, this);
                }
                break;
            case R.id.iv_play:
                if (isListen) {//如果监听网络类型
                    //没有弹过窗
                    if (!hasDialog) {
                        int netConnected = NetUtilss.isNetConnected(mActivity);
                        //判断网络类型
                        if (netConnected == NET_NO) {
                            //无网络
                        } else if (netConnected == NET_WIFI) {
                            //wifi
                            click2Play();
                        } else if (netConnected == NET_4G) {
                            //流量
                            creatMyDialog();
                        }
                    } else {
                        click2Play();
                    }
                } else {
                    click2Play();
                }
                break;
            case R.id.video_bg:
            case R.id.ll_vip_logo:
                if (isShowing()) {
                    hide();
                } else {
                    show();
                }
                break;
            case R.id.full:
                showFull(true);
                break;
            case R.id.no_full:
                showFull(false);
                break;
            case R.id.ll_reconnect:
                if (mOnClickReconnectListener != null) {
                    hideErrorLoading();
                    normalLoading.setVisibility(VISIBLE);
                    mOnClickReconnectListener.onClickReconnect(llReconnect, this, errorPosition);
                }
                break;
            case R.id.exit_cancel:
                //弹窗--取消
                alertDialog.dismiss();
                break;
            case R.id.exit_sure:
                //弹窗--确定
                click2Play();
                alertDialog.dismiss();
                break;
            default:
                break;
        }
    }

    /**
     * 点击播放
     */
    private void click2Play() {
        if (isFirst) {
            if (playing) {
                return;
            }
            mPauseButton.setImageResource(R.mipmap.vip_pause);
            hideVideoBg();
            if (isVip) {
                showVipLogo();
            } else {
                normalLoading.setVisibility(VISIBLE);
            }
            playing = !isPlaying();
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    doPauseResume();
                    show(sDefaultTimeout);
                    delayedTime = 0;
                }
            }, delayedTime);
        } else {
            doPauseResume();
            show(sDefaultTimeout);
        }
        //播放后，不在弹窗
        hasDialog = true;
    }

    private void deleteCache() {
        if (TextUtils.isEmpty(proxyUrl)) {
            return;
        }
        Observable.just(proxyUrl)
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .subscribe(new Action1<String>() {
                    @Override
                    public void call(String s) {
                        try {
                            if (s.length() > 7) {
                                videoCachePath = s.substring(7);
                                File videoCache = new File(videoCachePath);
                                File videoCacheDownload = new File(videoCachePath + ".download");
                                if (videoCache.exists()) {
                                    FileUtils.RecursionDeleteFile(new File(videoCachePath));
                                }
                                if (videoCacheDownload.exists()) {
                                    FileUtils.RecursionDeleteFile(videoCacheDownload);
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });

    }

    protected void creatMyDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity, R.style.MyDialog);
        View view = LayoutInflater.from(mActivity).inflate(R.layout.dialog_exit, null);//加载自定义布局
        ((TextView) view.findViewById(R.id.dialog_exit_title)).setText("观看视频");
        ((TextView) view.findViewById(R.id.dialog_exit_content)).setText("您正在使用流量观看视频，是否继续？");
        builder.setView(view);
        alertDialog = builder.create();
        alertDialog.show();
        view.findViewById(R.id.exit_cancel).setOnClickListener(this);
        view.findViewById(R.id.exit_sure).setOnClickListener(this);
        alertDialog.setCanceledOnTouchOutside(false);// 设置点击屏幕Dialog不消失
        hasDialog = true;
    }

    /**
     * 添加播放器
     *
     * @param tcPlayerView 播放器
     */
    private void addPlayer(TCPlayerView tcPlayerView) {
        if (super_video == null || tcPlayerView == null) {
            return;
        }
        super_video.removeAllViews();
        ViewGroup group = (ViewGroup) tcPlayerView.getParent();
        if (group != null) {
            group.removeAllViews();
        }
        tcPlayerView.setMediaController(this);
        tcPlayerView.setCoverView(videoBg);
        super_video.addView(tcPlayerView);
    }


    /**
     * 是否是小屏幕。此方法和showFull方法必须调用一个
     * @param showSmallSrceen
     * @return
     */
    public TCPlayer showSmallSrceen(boolean showSmallSrceen) {
        this.showSmallSrceen = showSmallSrceen;
        return this;
    }

    /**
     * 设置标题
     *
     * @param title 标题文本
     * @return TCPlayer
     */
    public TCPlayer setTitle(CharSequence title) {
        if (tvVideoTitle == null) {
            return this;
        }
        tvVideoTitle.setText(TextUtils.isEmpty(title) ? "" : title);
        return this;
    }

    /**
     * 是否需要标题栏
     *
     * @param showTitleLayout
     * @return
     */
    public TCPlayer showTitleLayout(boolean showTitleLayout) {
        this.showTitleLayout = showTitleLayout;
        if (llVideoTitle == null) {
            return this;
        }
        llVideoTitle.setVisibility(showTitleLayout ? VISIBLE : GONE);
        return this;
    }

    /**
     * 是否显示标题
     *
     * @param show true显示
     * @return TCPlayer
     */
    public TCPlayer showTitle(boolean show) {
        if (tvVideoTitle == null) {
            return this;
        }
        tvVideoTitle.setVisibility(show ? (showTitleLayout ? VISIBLE : GONE) : GONE);
        return this;
    }

    /**
     * 是否显示返回图片
     *
     * @param show true显示
     * @return TCPlayer
     */
    public TCPlayer showBackIcon(boolean show) {
        if (ivVideoBack == null) {
            return this;
        }
        ivVideoBack.setVisibility(show ? (showTitleLayout ? VISIBLE : GONE) : GONE);
        return this;
    }

    /**
     * 设置背景图片
     *
     * @param imgeUrl
     */
    public TCPlayer setVideoBgUrl(String imgeUrl) {
        if (videoBg != null) {
            videoBg.setImageURI(imgeUrl);
        }
        return this;
    }

    /**
     * 显示背景图
     *
     * @return
     */
    public void showVideoBg() {
        if (videoBg != null) {
            videoBg.setVisibility(VISIBLE);
        }
    }

    /**
     * 隐藏背景图片
     */
    public void hideVideoBg() {
        if (videoBg != null) {
            videoBg.setVisibility(GONE);
        }
    }

    /**
     * 设置视频总时间
     *
     * @param duration 总时间 毫秒
     * @return
     */
    public TCPlayer setVideoDuration(long duration) {
        if (videoView == null) {
            return this;
        }
        mEndTime.setText(generateTime(duration));
        return this;
    }

    /**
     * 显示Vip标识
     */
    public void showVipLogo() {
        if (vipLogo != null) {
            vipLogo.setVisibility(VISIBLE);
        }
    }

    /**
     * 隐藏Vip标识
     */
    public void hideVipLogo() {
        if (vipLogo != null) {
            vipLogo.setVisibility(GONE);
        }
    }

    public void showErrorLoading() {
        if (errorLoading != null) {
            errorLoading.setVisibility(VISIBLE);
        }
    }

    public void hideErrorLoading() {
        if (errorLoading != null) {
            errorLoading.setVisibility(GONE);
        }
    }

    /**
     * 视频url
     *
     * @param url
     * @return
     */
    public TCPlayer setVideoPath(String url) {
        this.url = url;
        if (videoView != null) {
            videoView.setVideoPath(url);
            videoView.isPressed();
        }

        return this;
    }

    /**
     * 开始播放
     *
     * @return
     */
    public TCPlayer play() {
        doPauseResume();
        return this;
    }

    /**
     * 开始播放
     *
     * @return
     */
    public TCPlayer play(long position) {
        doPauseResume(position);
        return this;
    }

    /**
     * 暂停
     *
     * @return
     */
    public TCPlayer pause() {
        videoView.pause();
        return this;
    }


    /**
     * 获取播放进度
     *
     * @return
     */
    public long getCurrentPosition() {
        if (videoView != null) {
            return videoView.getCurrentPosition();
        }
        return 0;
    }

    /**
     * 获取视频总长度
     *
     * @return
     */
    public long getDuration() {
        if (videoView != null) {
            return videoView.getDuration();
        }
        return 0;
    }

    /**
     * 返回播放器View
     *
     * @return
     */
    public TCPlayerView getTCVideoView() {
        return videoView;
    }

    /**
     * 返回是否播放
     *
     * @return
     */
    public boolean isPlaying() {
        return playing;
    }

    /**
     * 返回是否播放结束
     *
     * @return
     */
    public boolean isCompletion() {
        return completion;
    }

    /**
     * 延迟播放时间
     *
     * @param delayedTime
     * @return
     */
    public TCPlayer setStartDelayed(int delayedTime) {
        this.delayedTime = delayedTime;
        return this;
    }

    /**
     * 获取是否Vip
     *
     * @param isVip
     * @return
     */
    public TCPlayer setIsVip(boolean isVip) {
        this.isVip = isVip;
        return this;
    }

    /**
     * 是否需要缓存
     *
     * @param needCache true需要缓存
     * @return
     */
    public TCPlayer setNeedCache(boolean needCache) {
        this.needCache = needCache;
        return this;
    }

    /**
     * 设置Activity是活动
     *
     * @param isLive
     * @return
     */
    public TCPlayer setActivityIslive(boolean isLive) {
        this.isLive = isLive;
        return this;
    }

    /**
     * 设置是否监听当前网络状态
     *
     * @param isListen
     * @return
     */
    public TCPlayer setIsListenNetwork(boolean isListen) {
        this.isListen = isListen;
        return this;
    }


    public void onPause() {
        videoView.pause();
    }

    public void onDestroy() {
        videoView.stopPlayback();
    }

    private OnClickBackIconListener mOnClickBackIconListener;

    public TCPlayer setOnClickBackIconListener(OnClickBackIconListener mOnClickBackIconListener) {
        this.mOnClickBackIconListener = mOnClickBackIconListener;
        return this;
    }

    /**
     * 点击返回图标
     */
    public interface OnClickBackIconListener {
        void onClickBackIcon(View v, TCPlayer tcPlayer);
    }

    private OnClickReconnectListener mOnClickReconnectListener;

    public TCPlayer setOnClickReconnectListener(OnClickReconnectListener mOnClickReconnectListener) {
        this.mOnClickReconnectListener = mOnClickReconnectListener;
        return this;
    }

    /**
     * 点击重新连接监听
     */
    public interface OnClickReconnectListener {
        void onClickReconnect(View v, TCPlayer tcPlayer, long currenPosition);
    }

    public void setOnErrorListener(OnErrorListener mOnErrorListener) {
        this.mOnErrorListener = mOnErrorListener;
    }

    /**
     * 错误监听
     */
    public interface OnErrorListener {
        boolean onError(TCPlayer tcPlayer, int errorCode);
    }

    /**
     * 结束监听
     */
    public interface OnCompletionListener {
        void onCompletion(TCPlayer tcPlayer);
    }

    public void setOnCompletionListener(OnCompletionListener mOnCompletionListener) {

        this.mOnCompletionListener = mOnCompletionListener;
    }

    /**
     * 信息监听
     */
    public interface OnInfoListener {
        boolean onInfo(TCPlayer tcPlayer, int what, int extra);
    }

    public void setOnInfoListener(OnInfoListener mOnInfoListener) {

        this.mOnInfoListener = mOnInfoListener;
    }

    /**
     * 缓冲监听
     */
    public interface OnBufferingUpdateListener {
        void onBufferingUpdate(TCPlayer tcPlayer, int percent);
    }

    public void setOnBufferingUpdateListener(OnBufferingUpdateListener mOnBufferingUpdateListener) {
        this.mOnBufferingUpdateListener = mOnBufferingUpdateListener;
    }

    private class SimpleAnimationListener implements Animation.AnimationListener {

        @Override
        public void onAnimationStart(Animation animation) {

        }

        @Override
        public void onAnimationEnd(Animation animation) {

        }

        @Override
        public void onAnimationRepeat(Animation animation) {

        }
    }
}
