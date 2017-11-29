package me.qbb.player.utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.text.TextUtils;

import com.facebook.binaryresource.BinaryResource;
import com.facebook.binaryresource.FileBinaryResource;
import com.facebook.cache.common.CacheKey;
import com.facebook.common.executors.CallerThreadExecutor;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.controller.AbstractDraweeController;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.cache.DefaultCacheKeyFactory;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.imagepipeline.core.ImagePipelineFactory;
import com.facebook.imagepipeline.datasource.BaseBitmapDataSubscriber;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.postprocessors.IterativeBoxBlurPostProcessor;
import com.facebook.imagepipeline.request.BasePostprocessor;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

import java.io.File;

import me.qbb.player.utils.ScreenUtils;

/**
 * 创建日期：2017/7/18 14:52
 *
 * @author Qian Bing Bing
 *         类说明：图片加载
 */

public class ImageLoader {

    /**
     * 加载图片(带裁剪)
     *
     * @param uri
     * @param draweeView
     * @param wDP        宽 单位dp
     * @param hDP        高 单位dp
     */
    public static void displayImageDP(Context context,SimpleDraweeView draweeView, String uri, int wDP, int hDP) {
        uri = TextUtils.isEmpty(uri) ? "" : uri;
        displayImage(Uri.parse(uri), draweeView, ScreenUtils.dip2px(context, wDP), ScreenUtils.dip2px(context, hDP));
    }

    /**
     * 加载图片
     *
     * @param uri
     * @param draweeView
     * @param wPx
     * @param hPx
     */
    public static void displayImage(Uri uri, SimpleDraweeView draweeView, int wPx, int hPx) {
        if (draweeView == null) {
            return;
        }
        Object tag = draweeView.getTag();
        if (tag != null && uri.toString().equals(tag)) {
            return;
        }
        draweeView.setTag(uri.toString());
        ImageRequest request = ImageRequestBuilder.newBuilderWithSource(uri)
                .setResizeOptions(new ResizeOptions(wPx, hPx))
                .build();

        DraweeController controller = Fresco.newDraweeControllerBuilder()
                .setImageRequest(request)
                .setOldController(draweeView.getController())
                .build();
        draweeView.setController(controller);
    }

    /**
     * 加载GIF。
     * 传过来的view，不要加tag
     *
     * @param view
     * @param uri
     */
    public static void loaderGIF(SimpleDraweeView view, Uri uri, int wPx, int hPx) {
        if (view == null) {
            return;
        }
        Object tag = view.getTag();
        if (tag != null && uri.toString().equals(tag)) {
            return;
        }
        view.setTag(uri.toString());
        ImageRequest request = ImageRequestBuilder
                .newBuilderWithSource(uri)
                .setResizeOptions(new ResizeOptions(wPx, hPx))
                .build();
        AbstractDraweeController mDraweeController = Fresco.newDraweeControllerBuilder()
                .setAutoPlayAnimations(true)
                .setUri(uri)//设置uri
                .setImageRequest(request)
                .setOldController(view.getController())
                .build();
        //设置Controller
        view.setController(mDraweeController);
    }

    /**
     * 加载GIF。
     * 传过来的view，不要加tag
     *
     * @param view
     * @param uri
     */
    public static void loaderGIF(SimpleDraweeView view, Uri uri) {
        if (view == null) {
            return;
        }
        Object tag = view.getTag();
        if (tag != null && uri.toString().equals(tag)) {
            return;
        }
        view.setTag(uri.toString());
        AbstractDraweeController mDraweeController = Fresco.newDraweeControllerBuilder()
                .setAutoPlayAnimations(true)
                .setUri(uri)//设置uri
                .setOldController(view.getController())
                .build();
        //设置Controller
        view.setController(mDraweeController);
    }

    /**
     * 默认加载图片。
     * 传过来的view，不要加tag
     *
     * @param view
     * @param url
     */
    public static void loaderImgDef(SimpleDraweeView view, String url) {
        if (view == null) {
            return;
        }
        Object tag = view.getTag();
        if (tag != null && url.equals(tag)) {
            return;
        }
        Uri uri = Uri.parse(url);
        view.setImageURI(uri);
    }

    /**
     * 以高斯模糊显示。
     *
     * @param draweeView View。
     * @param url        url.
     */
    public static void showUrlBlur(SimpleDraweeView draweeView, String url) {
        showUrlBlur(draweeView, url, 60, 10);
    }

    /**
     * 以高斯模糊显示。
     *
     * @param draweeView View。
     * @param url        url.
     * @param iterations 迭代次数，越大越魔化。
     * @param blurRadius 模糊图半径，必须大于0，越大越模糊。
     */
    public static void showUrlBlur(SimpleDraweeView draweeView, String url, int iterations, int blurRadius) {
        try {
            Object tag = draweeView.getTag();
            if (tag != null && url.equals(tag)) {
                return;
            }
            draweeView.setTag(url);
            Uri uri = Uri.parse(url);
            ImageRequest request = ImageRequestBuilder.newBuilderWithSource(uri)
                    .setPostprocessor(new IterativeBoxBlurPostProcessor(iterations, blurRadius))
                    .build();
            AbstractDraweeController controller = Fresco.newDraweeControllerBuilder()
                    .setOldController(draweeView.getController())
                    .setImageRequest(request)
                    .build();
            draweeView.setController(controller);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 图片是否已经存在了
     *
     * @param context
     * @param uri
     * @return
     */
    public static boolean isCached(Context context, Uri uri) {
        ImagePipeline imagePipeline = Fresco.getImagePipeline();
        DataSource<Boolean> dataSource = imagePipeline.isInDiskCache(uri);
        if (dataSource == null) {
            return false;
        }
        ImageRequest imageRequest = ImageRequest.fromUri(uri);
        CacheKey cacheKey = DefaultCacheKeyFactory.getInstance()
                .getEncodedCacheKey(imageRequest, context);
        BinaryResource resource = ImagePipelineFactory.getInstance()
                .getMainFileCache().getResource(cacheKey);
        return resource != null && dataSource.getResult() != null && dataSource.getResult();
    }

    /**
     * 本地缓存文件
     *
     * @param context
     * @param uri
     * @return
     */
    public static File getCache(Context context, Uri uri) {
        if (!isCached(context, uri)){
            return null;
        }
        ImageRequest imageRequest = ImageRequest.fromUri(uri);
        CacheKey cacheKey = DefaultCacheKeyFactory.getInstance()
                .getEncodedCacheKey(imageRequest, context);
        BinaryResource resource = ImagePipelineFactory.getInstance()
                .getMainFileCache().getResource(cacheKey);
        File file = ((FileBinaryResource) resource).getFile();
        return file;
    }

    /**
     * 返回bitmap,也可以用来监听下载，bitmap会被fresco自动回收
     *
     * @param context  上下文
     * @param url      网络地址
     * @param width    宽度 可以为0 px
     * @param height   高度 可以为0 px
     * @param listener 回调
     */
    public static void getFrescoImg(Context context, String url, int width, int height, final LoadFrescoListener listener) {
        getFrescoImgProcessor(context, url, width, height, null, listener);
    }

    /**
     * 返回bitmap,也可以用来监听下载，bitmap会被fresco自动回收
     *
     * @param context   上下文
     * @param url       网络地址
     * @param width     宽度 px
     * @param height    高度 px
     * @param processor 处理图片
     * @param listener  回调
     */
    public static void getFrescoImgProcessor(Context context, final String url, final int width, final int height,
                                             BasePostprocessor processor, final LoadFrescoListener listener) {

        ResizeOptions resizeOptions = null;
        if (width != 0 && height != 0) {
            resizeOptions = new ResizeOptions(width, height);
        }
        ImageRequest imageRequest = ImageRequestBuilder.newBuilderWithSource(Uri.parse(url))
                .setProgressiveRenderingEnabled(false)  //不渲染
                .setPostprocessor(processor)
                .setResizeOptions(resizeOptions)
                .build();
        ImagePipeline imagePipeline = Fresco.getImagePipeline();
        DataSource<CloseableReference<CloseableImage>> dataSource = imagePipeline.fetchDecodedImage(imageRequest, context);
        dataSource.subscribe(new BaseBitmapDataSubscriber() {
            @Override
            protected void onNewResultImpl(Bitmap bitmap) {
                //图片不能是GIF
                listener.onSuccess(bitmap);
            }

            @Override
            protected void onFailureImpl(DataSource<CloseableReference<CloseableImage>> dataSource) {
                listener.onFail();
            }
        }, CallerThreadExecutor.getInstance());

    }

    public interface LoadFrescoListener {
        void onSuccess(Bitmap bitmap);

        void onFail();
    }

}
