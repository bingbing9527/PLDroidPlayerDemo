package me.qbb.player.simple;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import me.qbb.player.R;
import me.qbb.player.player.TCPlayer;
import me.qbb.player.utils.LogUtil;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recycler;
    private int orientation;
    private LinearLayoutManager layoutManager;
    private View action;
    private MainListAdapter knowVideoAdapter;
    private TCPlayer tcPlayer;
    private boolean isVip;
    private RelativeLayout fullScreen;
    private FrameLayout adapterPlayerContainer;
    /**
     * 正在播放，因退出后台而暂停为ture
     */
    private boolean playToPause;
    private FrameLayout fl_small_screen;
    private RelativeLayout small_screen;
    private ImageView close;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recycler = (RecyclerView) findViewById(R.id.recycler);

        close = (ImageView) findViewById(R.id.close);
        fullScreen = (RelativeLayout) findViewById(R.id.full_screen);
        fl_small_screen = (FrameLayout) findViewById(R.id.fl_small_screen);
        small_screen = (RelativeLayout) findViewById(R.id.small_screen);

        layoutManager = new LinearLayoutManager(this);
        recycler.setLayoutManager(layoutManager);

        knowVideoAdapter = new MainListAdapter(this);
        recycler.setAdapter(knowVideoAdapter);

        isVip = true;
        initVideo();

        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fl_small_screen.setVisibility(View.GONE);
                tcPlayer.pause();
            }
        });

    }

    /**
     * 初始化视频
     */
    private void initVideo() {
        /**
         * 初始化播放器
         */
        tcPlayer = new TCPlayer(this);
        tcPlayer.setIsVip(isVip)
                .setStartDelayed(isVip ? 1000 : 0)
                .setNeedCache(false)
                .showTitleLayout(false)
                .setVideoDuration(70 * 1000)
                .setOnClickBackIconListener(new TCPlayer.OnClickBackIconListener() {
                    @Override
                    public void onClickBackIcon(View v, TCPlayer tcPlayer) {
                        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                        }
                    }
                })
                .setOnClickReconnectListener(new TCPlayer.OnClickReconnectListener() {
                    @Override
                    public void onClickReconnect(View v, TCPlayer tcPlayer, long currenPosition) {
                        tcPlayer.setVideoPath("http://ors68wv1p.bkt.clouddn.com/video/mp4/MV.mp4");
                        tcPlayer.play(currenPosition);
                    }
                });
        recycler.addOnChildAttachStateChangeListener(new RecyclerView.OnChildAttachStateChangeListener() {
            @Override
            public void onChildViewAttachedToWindow(View view) {
                RecyclerView.ViewHolder childViewHolder = recycler.getChildViewHolder(view);
                if (childViewHolder instanceof MainListAdapter.VideoViewHolder) {
                    fl_small_screen.setVisibility(View.GONE);
                    adapterPlayerContainer = (FrameLayout) view.findViewById(R.id.adapter_player_container);
                    if (tcPlayer == null) {
                        return;
                    }
                    ViewGroup group = (ViewGroup) tcPlayer.getParent();
                    if (group != null) {
                        group.removeAllViews();
                    }
                    adapterPlayerContainer.removeAllViews();
                    adapterPlayerContainer.addView(tcPlayer);
                    if (!tcPlayer.getTCVideoView().isPlaying()) {
                        tcPlayer.showVideoBg();
                    }
                    tcPlayer.showFull(false)
                            .setVideoBgUrl("https://ss1.bdstatic.com/70cFvXSh_Q1YnxGkpoWK1HF6hhy/it/u=2291319651,1418676676&fm=27&gp=0.jpg")
                            .setVideoPath("http://ors68wv1p.bkt.clouddn.com/video/mp4/MV.mp4")
                            .noAnimationShowAll(0);
                }
            }

            @Override
            public void onChildViewDetachedFromWindow(View view) {
                RecyclerView.ViewHolder childViewHolder = recycler.getChildViewHolder(view);
                if (childViewHolder instanceof MainListAdapter.VideoViewHolder) {
                    if (tcPlayer != null) {
                        //消失，展示小视图
                        fullScreen.setVisibility(View.GONE);
                        ViewGroup group = (ViewGroup) tcPlayer.getParent();
                        if (group != null) {
                            group.removeAllViews();
                        }
                        small_screen.addView(tcPlayer);
                        fl_small_screen.setVisibility(View.VISIBLE);
                        tcPlayer.showSmallSrceen(true)
                                .noAnimationHideAll();
                    }

                }

            }

        });
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (tcPlayer != null) {
            orientation = newConfig.orientation;
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                //横屏
                ViewGroup group = (ViewGroup) tcPlayer.getParent();
                if (group == null) {
                    return;
                }
                group.removeAllViews();
                tcPlayer.showFull(true)
                        .showTitleLayout(true)
                        .showTitle(false)
                        .showBackIcon(true);
                fullScreen.addView(tcPlayer);
                fullScreen.setVisibility(View.VISIBLE);
                int mHideFlags =
                        View.SYSTEM_UI_FLAG_LOW_PROFILE
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_IMMERSIVE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
                fullScreen.setSystemUiVisibility(mHideFlags);
            } else {
                //竖屏
                fullScreen.setVisibility(View.GONE);
                ViewGroup group = (ViewGroup) tcPlayer.getParent();
                if (group != null) {
                    group.removeAllViews();
                }
                if (adapterPlayerContainer != null) {
                    adapterPlayerContainer.removeAllViews();
                    tcPlayer.showFull(false)
                            .showTitleLayout(false);
                    adapterPlayerContainer.addView(tcPlayer);
                    recycler.setVisibility(View.VISIBLE);
                }
            }
            tcPlayer.noAnimationHideAll();
            tcPlayer.noAnimationShowAll();
        } else {
            fullScreen.setVisibility(View.GONE);
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (tcPlayer != null) {
            if (tcPlayer.isPlaying()) {
                playToPause = true;
                tcPlayer.onPause();
            }
            tcPlayer.setActivityIslive(false);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (tcPlayer != null) {
            if (playToPause) {
                //播放后，退到后台
                tcPlayer.play();
                tcPlayer.show();
                playToPause = false;
                LogUtil.e("播放后，退到后台");
            } else {
                //未播放，退到后台
                LogUtil.e("未播放，退到后台");
                tcPlayer.noAnimationShowAll(0);
            }
            tcPlayer.setActivityIslive(true);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tcPlayer != null) {
            tcPlayer.onDestroy();
        }
    }

    @Override
    public void onBackPressed() {
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            finish();
        }
    }
}
