package me.qbb.player.player;

import android.content.Context;

/**
 * 创建时间 2017/10/27 11:16
 *
 * @author Qian Bing Bing
 *         类说明 播放器管理器
 */

public class TCPlayerManager {
    /**
     * 播放器管理器对象
     */
    public static TCPlayerManager tcPlayerManager;
    /**
     * 播放器对象
     */
    private TCPlayerView tcPlayerView;


    private TCPlayerManager() {
    }

    /**
     * 获取播放器管理器对象
     * @return 播放器管理器对象
     */
    public static TCPlayerManager getTcPlayerManager() {
        if (tcPlayerManager == null) {
            synchronized (TCPlayerManager.class) {
                if (tcPlayerManager == null) {
                    tcPlayerManager = new TCPlayerManager();
                }
            }
        }
        return tcPlayerManager;
    }

    /**
     * 初始化播放器
     * @param context 上下文
     * @return 播放器对象
     */
    public TCPlayerView initialize(Context context) {
        if (tcPlayerView == null) {
            tcPlayerView = new TCPlayerView(context);
        }
        return tcPlayerView;
    }

}
