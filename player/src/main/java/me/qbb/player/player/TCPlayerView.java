package me.qbb.player.player;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.AttributeSet;

import com.pili.pldroid.player.AVOptions;
import com.pili.pldroid.player.PLMediaPlayer;
import com.pili.pldroid.player.widget.PLVideoTextureView;

import me.qbb.player.utils.LogUtil;

/**
 * 创建时间 2017/10/26 14:34
 *
 * @author Qian Bing Bing
 *         类说明
 */

public class TCPlayerView extends PLVideoTextureView {

    /**
     * 播放出错
     */
    private int STATUS_ERROR = -1;
    /**
     * 需要重新连接
     */
    private int STATUS_NEEDRECONNECT = 0;
    /**
     * 正在加载
     */
    private int STATUS_LOADING = 1;
    /**
     * 正在播放
     */
    private int STATUS_PLAYING = 2;
    /**
     * 停止播放
     */
    private int STATUS_PAUSE = 3;
    /**
     * 播放结束
     */
    private int STATUS_COMPLETED = 4;

    /**
     * 异步获取当前播放位置
     */
    private static final int MSG_CURRENT_POSITION = 0xa1;

    /**
     * 最大重连次数
     */
    private int maxReconnectCount = 3;
    /**
     * 已重连的次数
     */
    private int reconnectCount;
    /**
     * 是否需要重连
     */
    private boolean isNeedReconnect;
    /**
     * 重连时，播放到的Position
     */
    private long currentPosition;
    private long oldCurrentPosition;

    /**
     * 未知错误码
     */
    public static final int UNKOWN_ERROR_CODE = 20171106;
    /**
     * 播放器未知错误码
     */
    public static final int MEDIA_ERROR_UNKNOWN = PLMediaPlayer.MEDIA_ERROR_UNKNOWN;

    /**
     * 不是错误码
     */
    public static final int NOT_ERROR_CODE = 200;

    private AVOptions options;
    private int status;
    private String url;

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_CURRENT_POSITION:
                    currentPosition = getCurrentPosition();
                    mHandler.sendEmptyMessageDelayed(MSG_CURRENT_POSITION, 500);
//                    LogUtil.e("MSG_CURRENT_POSITIONMSG_CURRENT_POSITION--" + currentPosition);
                    if (currentPosition != 0) {
                        oldCurrentPosition = currentPosition;
                    }
                    break;
                default:
                    break;
            }
        }
    };


    public TCPlayerView(Context context) {
        super(context);
        init();
    }

    public TCPlayerView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init();
    }

    public TCPlayerView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        init();
    }

    public TCPlayerView(Context context, AttributeSet attributeSet, int i, int i1) {
        super(context, attributeSet, i, i1);
        init();
    }

    /**
     * 初始化
     */
    private void init() {
        //软解
        setOptions(AVOptions.MEDIA_CODEC_SW_DECODE);
        //平铺
        setDisplayAspectRatio(PLVideoTextureView.ASPECT_RATIO_FIT_PARENT);
        //播放监听
        initEvent();

    }


    /**
     * 初始化监听
     */
    private void initEvent() {
        setOnErrorListener(onErrorListener);
        setOnCompletionListener(onCompletionListener);
        setOnInfoListener(onInfoListener);
        setOnBufferingUpdateListener(onBufferingUpdateListener);
        setOnPreparedListener(onPreparedListener);
    }

    @Override
    public void setVideoPath(String url) {
        this.url = url;
        super.setVideoPath(url);
    }

    @Override
    public void start() {

        mHandler.sendEmptyMessage(MSG_CURRENT_POSITION);
        super.start();
    }

    @Override
    public void pause() {
        super.pause();
        mHandler.removeMessages(MSG_CURRENT_POSITION);
    }

    @Override
    public void stopPlayback() {
        super.stopPlayback();
        mHandler.removeMessages(MSG_CURRENT_POSITION);
    }

    private void setOptions(int codecType) {
        options = new AVOptions();

        // 准备超时时间，包括创建资源、建立连接、请求码流等，单位是 ms
        // 默认值是：无
        options.setInteger(AVOptions.KEY_PREPARE_TIMEOUT, 10 * 1000);
        // 读取视频流超时时间，单位是 ms
        // 默认值是：10 * 1000
        options.setInteger(AVOptions.KEY_GET_AV_FRAME_TIMEOUT, 10 * 1000);
        // 播放前最大探测流的字节数，单位是 byte
        // 默认值是：128 * 1024
        options.setInteger(AVOptions.KEY_PROBESIZE, 128 * 1024);
        // 解码方式:
        // codec＝AVOptions.MEDIA_CODEC_HW_DECODE，硬解
        // codec=AVOptions.MEDIA_CODEC_SW_DECODE, 软解
        // codec=AVOptions.MEDIA_CODEC_AUTO, 硬解优先，失败后自动切换到软解
        // 默认值是：MEDIA_CODEC_SW_DECODE
        options.setInteger(AVOptions.KEY_MEDIACODEC, codecType);
        // 是否自动启动播放，如果设置为 1，则在调用 `prepareAsync` 或者 `setVideoPath` 之后自动启动播放，无需调用 `start()`
        // 默认值是：1
        int keyStartOnPrepared = 0;
        options.setInteger(AVOptions.KEY_START_ON_PREPARED, keyStartOnPrepared);
        setAVOptions(options);
    }


    private PLMediaPlayer.OnPreparedListener onPreparedListener = new PLMediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(PLMediaPlayer plMediaPlayer) {
            if (mOnTCPlayerPreparedListener != null) {
                mOnTCPlayerPreparedListener.onTCPlayerPrepared(TCPlayerView.this);
            }
        }
    };


    /**
     * 错误监听
     */
    private PLMediaPlayer.OnErrorListener onErrorListener = new PLMediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(PLMediaPlayer plMediaPlayer, int errorCode) {

            switch (errorCode) {
                case PLMediaPlayer.ERROR_CODE_INVALID_URI:
                    LogUtil.e("Invalid URL !");
                    break;
                case PLMediaPlayer.ERROR_CODE_404_NOT_FOUND:
                    LogUtil.e("404 resource not found !");
                    isNeedReconnect = true;
                    break;
                case PLMediaPlayer.ERROR_CODE_CONNECTION_REFUSED:
                    LogUtil.e("Connection refused !");
                    isNeedReconnect = true;
                    break;
                case PLMediaPlayer.ERROR_CODE_CONNECTION_TIMEOUT:
                    LogUtil.e("Connection timeout !");
                    isNeedReconnect = true;
                    break;
                case PLMediaPlayer.ERROR_CODE_EMPTY_PLAYLIST:
                    LogUtil.e("Empty playlist !");
                    isNeedReconnect = true;
                    break;
                case PLMediaPlayer.ERROR_CODE_STREAM_DISCONNECTED:
                    LogUtil.e("Stream disconnected !");
                    isNeedReconnect = true;
                    break;
                case PLMediaPlayer.ERROR_CODE_IO_ERROR:
                    LogUtil.e("Network IO Error !");
                    isNeedReconnect = true;
                    break;
                case PLMediaPlayer.ERROR_CODE_UNAUTHORIZED:
                    LogUtil.e("Unauthorized Error !");
                    break;
                case PLMediaPlayer.ERROR_CODE_PREPARE_TIMEOUT:
                    LogUtil.e("Prepare timeout !");
                    isNeedReconnect = true;
                    break;
                case PLMediaPlayer.ERROR_CODE_READ_FRAME_TIMEOUT:
                    LogUtil.e("Read frame timeout !");
                    isNeedReconnect = true;
                    break;
                case PLMediaPlayer.ERROR_CODE_HW_DECODE_FAILURE:
                    setOptions(AVOptions.MEDIA_CODEC_SW_DECODE);
                    isNeedReconnect = true;
                    break;
                case PLMediaPlayer.MEDIA_ERROR_UNKNOWN:
                    errorCode = MEDIA_ERROR_UNKNOWN;
                    LogUtil.e("media error unknown !");
                    break;
                default:
                    errorCode = UNKOWN_ERROR_CODE;
                    LogUtil.e("unknown error !");
                    break;

            }
//            LogUtil.e("currentPositioncurrentPosition=====" + currentPosition + "--oldCurrentPositionoldCurrentPosition--" + oldCurrentPosition);
            mHandler.removeMessages(MSG_CURRENT_POSITION);
            if (isNeedReconnect) {
                statusChange(STATUS_NEEDRECONNECT);
            } else {
                statusChange(STATUS_ERROR,errorCode);
            }

            // Return true means the error has been handled
            // If return false, then `onCompletion` will be called
            return true;
        }

    };

    /**
     * 播放结束监听
     */
    private PLMediaPlayer.OnCompletionListener onCompletionListener = new PLMediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(PLMediaPlayer plMediaPlayer) {
            statusChange(STATUS_COMPLETED);
            if (mOnTCPlayerCompletionListener != null) {
                mOnTCPlayerCompletionListener.onCompletion(TCPlayerView.this);
            }
        }
    };

    /**
     * 播放信息监听
     */
    private PLMediaPlayer.OnInfoListener onInfoListener = new PLMediaPlayer.OnInfoListener() {
        @Override
        public boolean onInfo(PLMediaPlayer mp, int what, int extra) {
            LogUtil.e("onInfo!");

            if (mOnTCPlayerInfoListener != null) {
               return mOnTCPlayerInfoListener.onTCPlayerInfo(TCPlayerView.this, what, extra);
            }
            return false;
        }
    };
    /**
     * 缓冲监听
     */
    private PLMediaPlayer.OnBufferingUpdateListener onBufferingUpdateListener = new PLMediaPlayer.OnBufferingUpdateListener() {
        @Override
        public void onBufferingUpdate(PLMediaPlayer plMediaPlayer, int percent) {
            if (mOnTCPlayBufferingUpdateListener != null) {
                mOnTCPlayBufferingUpdateListener.onBufferingUpdate(TCPlayerView.this, percent);
            }
        }
    };

    /**
     * 重新连接
     */
    private void sendReconnectMessage() {
        if (TextUtils.isEmpty(url)) {
            return;
        }
        if (reconnectCount > maxReconnectCount) {
            statusChange(STATUS_ERROR);
            return;
        }
        reconnectCount++;
        setVideoPath(url);
        if (!isPlaying()) {
            start();
        }
        seekTo(currentPosition == 0 ? oldCurrentPosition : currentPosition);
    }

    /**
     * 视频播放状态的改变
     *
     * @param newStatus
     */
    private void statusChange(int newStatus) {
        statusChange(newStatus,NOT_ERROR_CODE);
    }
    /**
     * 视频播放状态的改变
     *
     * @param newStatus
     * @param errorCode
     */
    private void statusChange(int newStatus,int errorCode) {
        status = newStatus;
        if (STATUS_NEEDRECONNECT == newStatus) {
            //重新连接
            sendReconnectMessage();
        } else if (STATUS_ERROR == newStatus) {
            if (mOnTCPlayerErrorListener != null) {
                mOnTCPlayerErrorListener.onError(TCPlayerView.this, errorCode,currentPosition == 0 ? oldCurrentPosition : currentPosition);
            }
        } else if (STATUS_COMPLETED == newStatus) {
            mHandler.removeMessages(MSG_CURRENT_POSITION);
        } else {

        }
    }

    /**
     * 内部重连最大次数
     *
     * @param maxReconnectCount
     * @return
     */
    public TCPlayerView setMaxReconnectCount(int maxReconnectCount) {
        this.maxReconnectCount = maxReconnectCount;
        return this;
    }

    private TCPlayerListener.OnTCPlayerPreparedListener mOnTCPlayerPreparedListener;
    private TCPlayerListener.OnTCPlayerErrorListener mOnTCPlayerErrorListener;
    private TCPlayerListener.OnTCPlayerCompletionListener mOnTCPlayerCompletionListener;
    private TCPlayerListener.OnTCPlayerInfoListener mOnTCPlayerInfoListener;
    private TCPlayerListener.OnTCPlayBufferingUpdateListener mOnTCPlayBufferingUpdateListener;

    public TCPlayerView onPrepared(TCPlayerListener.OnTCPlayerPreparedListener mOnTCPlayerPreparedListener) {
        this.mOnTCPlayerPreparedListener = mOnTCPlayerPreparedListener;
        return this;
    }

    public TCPlayerView onError(TCPlayerListener.OnTCPlayerErrorListener mOnTCPlayerErrorListener) {
        this.mOnTCPlayerErrorListener = mOnTCPlayerErrorListener;
        return this;
    }

    public TCPlayerView onComplete(TCPlayerListener.OnTCPlayerCompletionListener mOnTCPlayerCompletionListener) {
        this.mOnTCPlayerCompletionListener = mOnTCPlayerCompletionListener;
        return this;
    }

    public TCPlayerView onInfo(TCPlayerListener.OnTCPlayerInfoListener mOnTCPlayerInfoListener) {
        this.mOnTCPlayerInfoListener = mOnTCPlayerInfoListener;
        return this;
    }

    public TCPlayerView onBuffering(TCPlayerListener.OnTCPlayBufferingUpdateListener mOnTCPlayBufferingUpdateListener) {
        this.mOnTCPlayBufferingUpdateListener = mOnTCPlayBufferingUpdateListener;
        return this;
    }


}
