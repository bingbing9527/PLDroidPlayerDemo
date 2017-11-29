package me.qbb.player.player;

/**
 * 创建时间 2017/10/31 11:18
 *
 * @author Qian Bing Bing
 *         类说明
 */

public class TCPlayerListener {

    /**
     * 播放信息监听
     */
    public interface OnTCPlayerPreparedListener {

        void onTCPlayerPrepared(TCPlayerView tcPlayerView);
    }

    /**
     * 播放信息监听
     */
    public interface OnTCPlayerInfoListener {

        boolean onTCPlayerInfo(TCPlayerView tcPlayerView, int what, int extra);
    }


    /**
     * 错误监听
     */
    public interface OnTCPlayerErrorListener {
        /**
         * 错误回调方法
         * @param tcPlayerView
         * @param errorCode
         * @param position 发生错误时的position
         */
        void onError(TCPlayerView tcPlayerView, int errorCode, long position);
    }

    /**
     * 播放完成
     */
    public interface OnTCPlayerCompletionListener{
        /**
         * 播放完成
         * @param tcPlayerView
         */
         void onCompletion(TCPlayerView tcPlayerView);
    }
    /**
     * 缓存监听
     */
    public interface OnTCPlayBufferingUpdateListener{
        /**
         * 缓冲回调
         * @param tcPlayerView
         * @param percent
         */
        void onBufferingUpdate(TCPlayerView tcPlayerView, int percent);
    }
}
