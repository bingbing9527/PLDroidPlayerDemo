package me.qbb.player;

import android.app.Application;
import android.content.Context;
import android.os.Environment;

import com.danikula.videocache.HttpProxyCacheServer;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.core.ImagePipelineConfig;

import java.io.File;

import me.qbb.player.player.MyFileNameGenerator;

/**
 * 创建时间 2017/11/29 15:02
 *
 * @author Qian Bing Bing
 *         类说明
 */

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ImagePipelineConfig config = ImagePipelineConfig.newBuilder(this)
                .setDownsampleEnabled(true).build();
        Fresco.initialize(this, config);
    }

    private HttpProxyCacheServer proxy;

    public static HttpProxyCacheServer getProxy(Context context) {
        App app = (App) context.getApplicationContext();
        return app.proxy == null ? (app.proxy = app.newProxy()) : app.proxy;
    }
    private HttpProxyCacheServer newProxy() {
       File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "0playerDemo","video");
        return new HttpProxyCacheServer
                .Builder(this)
                .maxCacheSize(2L * 1024L * 1024L * 1024L)//缓存2GB
                .fileNameGenerator(new MyFileNameGenerator())
                .cacheDirectory(file)
                .build();
    }
}
