package me.qbb.player.utils;

import android.content.Context;

/**
 * 创建时间 2017/11/29 14:59
 *
 * @author Qian Bing Bing
 *         类说明 屏幕相关工具类
 */

public class ScreenUtils {


    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }
}
