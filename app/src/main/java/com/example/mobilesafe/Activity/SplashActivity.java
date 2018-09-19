package com.example.mobilesafe.Activity;

import android.Manifest;
import android.app.Application;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mobilesafe.BuildConfig;
import com.example.mobilesafe.JavaBean.UpdateInfo;
import com.example.mobilesafe.R;
import com.example.mobilesafe.utils.GSonUtils;
import com.example.mobilesafe.utils.HttpUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONObject;
import org.xutils.common.Callback;
import org.xutils.http.RequestParams;
import org.xutils.x;

import java.io.File;
import java.io.IOException;
import java.util.List;

import okhttp3.Call;
import okhttp3.Response;

import static android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION;

public class SplashActivity extends AppCompatActivity{

    /**
     * 更新新版本的状态码
     */
    private static final int UPDATE_VERSION = 100;
    /**
     * 进入主界面的状态码
     */

    ProgressDialog mProgressDialog;

    private static final String tag = "SplashActivity";
    private static final int ENTER_HOME = 101;

    private UpdateInfo updateInfo;

    private TextView tv_version_name;

    private int mLocalVersionCode;

    private Handler mHandler = new Handler(){
        public void handleMessage(Message msg){

            switch (msg.what){
                case UPDATE_VERSION:
                    showUpdateDialog();
                    break;
                case ENTER_HOME:
                    //进入程序主界面，
                    EnterHome();
                    break;
            }

        }
    };

    /**
     * 弹出对话框提示用户更新
     */
    private void showUpdateDialog() {
        //对话框是依赖Activity的
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        //设置左上角图标
        builder.setIcon(R.drawable.ic_launcher_foreground);
        builder.setTitle("版本更新");
        //设置文本
        builder.setMessage(updateInfo.getVersionDes());
        builder.setPositiveButton("立即更新", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                //下载APK，downloadURL
                downloadAPK();
                //EnterHome();
            }
        });
        builder.setNegativeButton("稍后更新", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                //取消对话框，进入主界面
                EnterHome();
            }
        });
        builder.show();
    }

    private void installApk(File file){
        Uri uri;
        Intent intent = new Intent("android.intent.action.VIEW");
        /*intent.setData(Uri.fromFile(file));
        intent.setType("application/vnd.android.package-archive");*/
        //安卓7.0之后,不能在Intent之中直接传递如file://的路径，要通过fileProvider传递并且临时授权uri
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            uri = FileProvider.getUriForFile(getApplicationContext(), "com.example.mobilesafe.fileProvider", file);
            // 给目标应用一个临时授权
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            uri = Uri.fromFile(file);
        }
        intent.addCategory("android.intent.category.DEFAULT");
        intent.setDataAndType(uri,"application/vnd.android.package-archive");
        intent.setFlags(FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
    }
    private void downloadAPK() {
        //APK下载地址，放置APK的所在路径
        //先判断SD卡是否可用hello
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            //2获取SD卡路径

            String path = Environment.getExternalStorageDirectory().getAbsoluteFile() + File.separator + "mobilesafe74.apk";
            Log.d("path",path);
            //3发送请求，获取apk并且放置到指定的路径
            RequestParams params = new RequestParams(updateInfo.getDownloadUrl());
            params.setSaveFilePath(path);
            params.setAutoRename(true);
            mProgressDialog = new ProgressDialog(SplashActivity.this);
            x.http().get(params, new Callback.ProgressCallback<File>() {

                @Override
                public void onSuccess(File result) {
                    Log.d(tag, "下载成功");
                    mProgressDialog.dismiss();
                    installApk(result);
                }

                @Override
                public void onError(Throwable ex, boolean isOnCallback) {
                    Log.d(tag, "下载失败");
                    ex.printStackTrace();
                    mProgressDialog.dismiss();
                }

                @Override
                public void onCancelled(CancelledException cex) {
                    Log.d(tag, "取消下载");
                    cex.printStackTrace();
                    mProgressDialog.dismiss();
                }

                @Override
                public void onFinished() {
                    Log.d(tag, "结束下载");
                    mProgressDialog.dismiss();
                }

                @Override
                public void onWaiting() {
                    // 网络请求开始的时候调用
                    Log.d(tag, "等待下载");
                }

                @Override
                public void onStarted() {
                    // 下载的时候不断回调的方法
                    Log.d(tag, "开始下载");
                }

                @Override
                public void onLoading(long total, long current, boolean isDownloading) {
                    // 当前的下载进度和文件总大小
                    Log.i(tag, "正在下载中......");
                    mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    mProgressDialog.setMessage("正在下载中......");
                    mProgressDialog.show();
                    mProgressDialog.setMax((int) total);
                    mProgressDialog.setProgress((int) current);
                }
            });

        }

    }

    private void EnterHome() {
        Intent intent = new Intent(this,HomeActivity.class);
        startActivity(intent);
        //在开启新届面后将导航界面关闭
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //去除Activity的头
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_splash);
        x.Ext.init(this.getApplication());
        x.Ext.setDebug(BuildConfig.DEBUG);
        //申请权限
        if(ContextCompat.checkSelfPermission(SplashActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(SplashActivity.this, new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
        }
        //初始化UI
        initUi();
        Log.d("SplashActivity","finish initUi");
        //初始化数据
        initData();
        Log.d("SplashActivity","finish initData");

    }

    /**
     * 初始化数据
     */
    private void initData() {
        // 应用版本名称
        tv_version_name.setText("版本名称：" + getVersionName());
        //检测是否有更新，如果有提示用户下载
        getVersionCode();
    }

    /**
     * 初始化UI
     */
    private void initUi() {
        tv_version_name = (TextView)findViewById(R.id.tv_version_name);
        //获取本地版本号
        mLocalVersionCode = getVersionCode();
        //获取服务器版本号（客户端给请求，服务端给响应（json,xml））
        //json解析
        //字段1：版本名称
        //字段2：新版本描述
        //字段3：服务端版本号
        //字段4：下载地址
        //检测版本号
        checkVersion();

   }

    private void checkVersion() {
          HttpUtils.sendOkHttpRequest("http://172.16.35.92:8080/update74.json", new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                Log.d("onFailure",e.toString());
                Log.d("onFailure","failed");
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Long startTime = System.currentTimeMillis();
                Message msg = Message.obtain();
                Log.d("Splash Activity","get response");
                String responseText = response.body().string();
                Log.d("response",responseText);
                updateInfo = GSonUtils.handlerUpdateInfoResponse(responseText);
                Log.d("updateInfo",updateInfo.getDownloadUrl());
                Log.d("updateInfo",updateInfo.getVersionCode());
                Log.d("updateInfo",String.valueOf(mLocalVersionCode));
               //比对版本号
                try{
                    if(mLocalVersionCode < Integer.parseInt(updateInfo.getVersionCode())){
                        //提示用户更新，弹出对话框，在子线程，用消息机制
                        //Log.d("updateInfo",updateInfo.getVersionCode());
                        //Log.d("updateInfo",String.valueOf(mLocalVersionCode));
                        msg.what = UPDATE_VERSION;
                    }else{
                        //进入应用程序主界面
                        msg.what = ENTER_HOME;
                    }
                }finally{
                    long endTime = System.currentTimeMillis();
                    if(endTime-startTime < 4000){
                        try {
                            Thread.sleep(4000-(endTime-startTime));
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    mHandler.sendMessage(msg);
                }
            }
        });
    }

    /**
     * 获取版本名称
     * @return 应用版本名称，返回null代表有异常
     */

    private String getVersionName() {
        //包管理者PackageManager
        PackageManager pm = getPackageManager();
        /* 从包管理者对象中获取指定包名的对应信息：版本名称版本号,传0代表获取基本信息 */
        try {
            PackageInfo packageInfo = pm.getPackageInfo(getPackageName(),0);
            return packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取版本号
     * @return 应用版本号，返回0代表有异常
     */
    public int getVersionCode() {
        PackageManager pm = getPackageManager();
        /* 从包管理者对象中获取指定包名的对应信息：版本名称版本号,传0代表获取基本信息 */
        try {
            PackageInfo packageInfo = pm.getPackageInfo(getPackageName(),0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return 0;
    }
}
