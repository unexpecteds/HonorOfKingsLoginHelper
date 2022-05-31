package com.unexpected.tmgp.sgame;

import android.annotation.NonNull;
import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.XModuleResources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.ImageView;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import org.json.JSONObject;


public class MainHook implements IXposedHookLoadPackage, IXposedHookZygoteInit {
    private static String MODULE_PATH;
    private static XModuleResources mResources;
    private static ClassLoader classLoader;
    private XC_MethodHook.Unhook hookQQ;
    private Bitmap bitmap;
    private WindowManager windowManager;
    private WindowManager.LayoutParams windowParams;
    private ImageView imageView;

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if(lpparam.packageName.equals("com.tencent.tmgp.sgame")) {
            classLoader = lpparam.classLoader;
            XposedHelpers.findAndHookMethod("com.tencent.tmgp.sgame.SGameActivity", lpparam.classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                        Activity a = (Activity) param.thisObject;
                        if(canDrawOverlays(a)) {
                            bitmap = MainHook.getBitmapFromAssets(a, "yyb_qq.png");
                            windowManager = a.getWindowManager();
                            windowParams = new WindowManager.LayoutParams();
                            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                windowParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
                            } else {
                                windowParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
                            }
                            windowParams.format = PixelFormat.RGBA_8888;
                            windowParams.gravity = Gravity.TOP | Gravity.START;
                            windowParams.flags = 262440;
                            windowParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
                            windowParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
                            //设置悬浮窗 位置,这个受layoutParams.gravity 影响，它提供了从给定边缘的偏移量。
                            // 也就是说 这个悬浮窗的实际x，y位置。是这里x，y 加上偏移量后的。
                            windowParams.x = 0;
                            windowParams.y = 270;
                            imageView = new ImageView(a);
                            imageView.setLayoutParams(new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT));
                            imageView.setImageBitmap(bitmap);
                            imageView.setOnTouchListener(new WindowMoveOnTouchListener());
                            windowManager.addView(imageView, windowParams);
                        }
                    }
                });
            XposedHelpers.findAndHookMethod("com.tencent.msdk.sdkwrapper.qq.QQSdk$LoginListener", lpparam.classLoader, "onComplete", Object.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                        if(windowManager != null) {
                            if(param.args[0] instanceof JSONObject) {
                                windowManager.removeView(imageView);
                            }
                        }
                    }
                });
            XposedHelpers.findAndHookConstructor("com.tencent.connect.auth.AuthDialog",
                lpparam.classLoader,
                Context.class,
                String.class,
                String.class,
                XposedHelpers.findClass("com.tencent.tauth.IUiListener", lpparam.classLoader),
                XposedHelpers.findClass("com.tencent.connect.auth.QQToken", lpparam.classLoader),
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                        param.args[2] = "https://xui.ptlogin2.qq.com/cgi-bin/xlogin?appid=716027609&pt_3rd_aid=1104466820&daid=381&pt_skey_valid=0&style=35&s_url=http://connect.qq.com&refer_cgi=m_authorize&ucheck=1&fall_to_wv=1&status_os=10&redirect_uri=auth://www.qq.com&client_id=1104466820&response_type=token&scope=all&sdkp=i&sdkv=3.3.8_full&state=test&status_machine=iPhone12,1&switch=1&h5sig=jT9DUw5Sx6DB52AW7EpCAYbuT_3s-v_idjDWdrffzKM&loginty=1";
                    }
                });
            XposedHelpers.findAndHookMethod("com.tencent.connect.auth.AuthDialog$LoginWebViewClient", lpparam.classLoader, "shouldOverrideUrlLoading", WebView.class, String.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                        param.args[1] = ((String) param.args[1]).replaceFirst("www.qq.com", "tauth.qq.com/");
                    }
                });
            XposedHelpers.findAndHookMethod("com.tencent.smtt.sdk.WebView", lpparam.classLoader, "loadUrl", String.class, new XC_MethodHook(){
                    @Override
                    protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                        String url = (String) param.args[0];
                        if(url.startsWith("https://jiazhang.qq.com")) {
                            String html = inputStream2String(mResources.getAssets().open("jiazhang.html"), "utf-8");
                            XposedHelpers.callMethod(param.thisObject, "loadDataWithBaseURL", param.args[0], html, "text/html", "utf-8", null);
                        }
                    }
                });
        }
    }

    public XC_MethodHook.Unhook startHookQQ(ClassLoader classLoader) {
        return XposedHelpers.findAndHookMethod("com.tencent.connect.common.BaseApi", classLoader, "getTargetActivityIntent", String.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                    param.setResult(null);
                }
            });
    }

    @Override
    public void initZygote(IXposedHookZygoteInit.StartupParam startupParam) throws Throwable {
        MODULE_PATH = startupParam.modulePath;
        mResources = XModuleResources.createInstance(startupParam.modulePath, null);
    }

    private static boolean canDrawOverlays(@NonNull Context context) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(context);
        }
        return true;
    }

    /**
     * 从Assets中读取图片
     * @param fileName 文件名
     */
    @NonNull
    private static Bitmap getBitmapFromAssets(@NonNull Context context, @NonNull String name) throws IOException {
        AssetManager am = context.getResources().getAssets();
        try(InputStream is = am.open(name)) {
            return BitmapFactory.decodeStream(is);
        }
    }

    /**
     * Input stream to output stream.
     *
     * @param is The input stream.
     * @return output stream
     */
    private static ByteArrayOutputStream input2OutputStream(final InputStream is) {
        if(is == null) return null;
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            byte[] b = new byte[1024];
            int len;
            while((len = is.read(b, 0, 1024)) != -1) {
                os.write(b, 0, len);
            }
            return os;
        } catch(IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                is.close();
            } catch(IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Input stream to string.
     *
     * @param is          The input stream.
     * @param charsetName The name of charset.
     * @return string
     */
    private static String inputStream2String(final InputStream is, final String charsetName) {
        if(is == null || isSpace(charsetName)) return "";
        try {
            ByteArrayOutputStream baos = input2OutputStream(is);
            if(baos == null) return "";
            return baos.toString(charsetName);
        } catch(UnsupportedEncodingException e) {
            e.printStackTrace();
            return "";
        }
    }

    private static boolean isSpace(final String s) {
        if(s == null) return true;
        for(int i = 0, len = s.length(); i < len; ++i) {
            if(!Character.isWhitespace(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public class WindowMoveOnTouchListener implements android.view.View.OnTouchListener {
        private static final int MOVE_THRESHOLD = 5;
        private final int moveThreshold;
        private long startDownTime;
        private float mStartRawX;
        private float mStartRawY;
        private float mStartViewX;
        private float mStartViewY;

        public WindowMoveOnTouchListener() {
            this(MOVE_THRESHOLD);
        }

        public WindowMoveOnTouchListener(int moveThreshold) {
            this.moveThreshold = moveThreshold;
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int action = event.getAction();
            if(action == MotionEvent.ACTION_DOWN) {
                startDownTime = SystemClock.elapsedRealtime();
                mStartRawX = event.getRawX();
                mStartRawY = event.getRawY();
                mStartViewX = event.getX();
                mStartViewY = event.getY();
            } else if(action != MotionEvent.ACTION_UP) {
                if(action == MotionEvent.ACTION_MOVE && !isClick(event)) {
                    windowParams.x = (int) ((event.getRawX() - mStartViewX) - v.getX());
                    windowParams.y = (int) ((event.getRawY() - mStartViewY) - v.getY());
                    windowManager.updateViewLayout(imageView, windowParams);
                }
            } else if(isClick(event)) {
                if(SystemClock.elapsedRealtime() - startDownTime >= ViewConfiguration.getLongPressTimeout() * 0.75f) {
                    windowManager.removeView(imageView);
                } else {
                    if(hookQQ == null) {
                        imageView.setImageDrawable(mResources.getDrawable(R.drawable.ic_qq));
                        hookQQ = startHookQQ(classLoader);
                    } else {
                        imageView.setImageBitmap(bitmap);
                        hookQQ.unhook();
                        hookQQ = null;
                    }
                }
            }
            return true;
        }

        private boolean isClick(MotionEvent event) {
            return Math.abs(mStartRawX - event.getRawX()) <= moveThreshold && Math.abs(mStartRawY - event.getRawY()) <= moveThreshold;
        }
    }
}
