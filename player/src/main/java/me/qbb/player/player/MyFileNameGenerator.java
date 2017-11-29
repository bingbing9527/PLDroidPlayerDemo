package me.qbb.player.player;

import android.text.TextUtils;

import com.danikula.videocache.ProxyCacheUtils;
import com.danikula.videocache.file.FileNameGenerator;

/**
 * 创建日期：2017/4/25 17:09
 *
 * @author Qian Bing Bing
 * @mail bingbing.qian@quanmintaiji.cn
 * @phone 15201460236
 * 类说明：视频缓存命名策略,已去除.mp4及后面的参数的链接的MD5值
 */

public class MyFileNameGenerator implements FileNameGenerator {
    @Override
    public String generate(String url) {
        String newUrl = url;
        try {
            if (url.contains("?")) {
                newUrl = url.substring(0, url.indexOf("?"));
            } else {
                newUrl = url;
            }
        } catch (Exception e) {
            e.printStackTrace();
            newUrl = url;
        }
        StringBuffer cacheName = new StringBuffer();
        boolean contains = newUrl.contains(".mp4");
        //链接中包含.mp4，就去除.mp4及后面的参数
        if (contains) {
            String[] sourceStrArray = newUrl.split("\\.");
            for (int i = 0; i < sourceStrArray.length; i++) {
                if (i != sourceStrArray.length - 1) {
                    cacheName.append(TextUtils.isEmpty(sourceStrArray[i]) ? "" : sourceStrArray[i]);
                }
            }
        } else {
            //否则直接使用url
            cacheName.append(newUrl);
        }
        return ProxyCacheUtils.computeMD5(TextUtils.isEmpty(cacheName) ? newUrl : cacheName.toString());
    }
}
