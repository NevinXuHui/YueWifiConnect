package com.nevinxu.wifiscan.Utils;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;

import com.lee.wifiscan.R;
import com.nevinxu.wifiscan.bean.SimpleWifiBean;
import com.nevinxu.wifiscan.bean.WifiCipherType;
import com.nevinxu.wifiscan.delegate.WifiDelegate;
import com.nevinxu.wifiscan.listener.ScanResultListener;

import java.util.List;

public class WifiUtil {
    public static final String SP_NAME = "WIFI_INFO_SAVE";
    public static final String SSID_KEY = "TARGET_SSID_KEY";
    public static final String MI_WIFI = "MI_WIFI";
    public static final int MI_REQUEST_CODE = 0x111;
    public static final int BEST_RECORD_TIME = 400;


    public static IntentFilter initFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        return filter;
    }


    public static boolean ensureConnectSuc(Context context, WifiInfo info) {
        if (info != null && !TextUtils.isEmpty(info.getSSID())) {
            String targetSSID = getTargetSSID(context);
            if (!TextUtils.isEmpty(targetSSID) && info.getSSID().contains(targetSSID)) {
                Log.i("WIFI_LIST", "5 connected success,  wifi  = " + info.getSSID());
                return true;
            }
        }
        Log.i("WIFI_LIST", "5 connected fail ");

        return false;
    }

    /**
     * 先过滤出要连接的WiFi，然后再连接.
     * 这个方法会被反复调用，直到获取到目标WiFi或者满20次
     */
    public static void filterAndConnectTargetWifi(Context context, WifiDelegate delegate, List<ScanResult> list, String targetWifiName, String targetWifiPassword, boolean isLastTime, ScanResultListener listener) {
        SimpleWifiBean wifiBean = FilterTargetWifi(context, delegate, list, targetWifiName,targetWifiPassword);
        Log.i("targetWifiName:", targetWifiName);
        if (wifiBean != null) {
            Log.i("WIFI_LIST", "3 filter, success");
            Log.i("wifiBean WifiName:", wifiBean.getWifiName());
            Log.i("wifiBean WifiPassword:", wifiBean.getWifiPassword());
            Log.i("wifiBean apabilities:", wifiBean.getCapabilities());
            connectTargetWifi(context, wifiBean);
        }
        if (isLastTime && wifiBean == null) {
            Log.i("WIFI_LIST", "3 filter, fail");
            if (listener != null) {
                listener.filterFailure();
            }
        }

    }

    /**
     * 连接到目标WiFi
     */
    public static void connectTargetWifi(Context context, SimpleWifiBean wifiBean) {
        String capabilities = wifiBean.getCapabilities();
        Log.i("WifiCipherType:",getWifiCipher(capabilities).toString());
//        if (getWifiCipher(capabilities) == WifiCipherType.WIFICIPHER_NOPASS) {  //无需密码
            WifiConfiguration tempConfig = isExsits(wifiBean.getWifiName(), context);
            if (tempConfig == null) {
                Log.i("WIFI_LIST", "4 connect, tempConfig = null");
                WifiConfiguration exsits = createWifiConfig(wifiBean.getWifiName(), wifiBean.getWifiPassword(), getWifiCipher(capabilities));
                addNetWork(exsits, context);
            } else {
                Log.i("WIFI_LIST", "4 connect, tempConfig.SSID = " + tempConfig.SSID);
                addNetWork(tempConfig, context);
            }

//        } else {
//
//            //todo 需要密码的场景
//            Log.i("WIFI_LIST","1111111111111111111");
//        }
    }

    /**
     * 过滤出想要连接的WiFi，
     * 为了获取信息方便，装饰成SimpleWifiBean实例
     */
    public static SimpleWifiBean FilterTargetWifi(Context context, WifiDelegate
            delegate, List<ScanResult> list, String targetWifiName,String targetWifiPassword) {
        SimpleWifiBean simpleWifiBean = null;
        for (ScanResult result : list) {
            if (result.SSID.contains(targetWifiName)) {
                delegate.stopScan();
                saveTargetSSID(context, targetWifiName);
                simpleWifiBean = new SimpleWifiBean();
                simpleWifiBean.setWifiName(result.SSID);
                simpleWifiBean.setWifiPassword(targetWifiPassword);
                simpleWifiBean.setCapabilities(result.capabilities);
                simpleWifiBean.setLevel(getLevel(result.level) + "");
            }
        }

        return simpleWifiBean;
    }


    public static void saveTargetSSID(Context context, String targetSSID) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
//        targetSSID = "\"" + targetSSID + "\""; //refer WifiInfo#getSSID()
        editor.putString(SSID_KEY, targetSSID);
        editor.commit();

    }

    public static String getTargetSSID(Context context) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        return sp.getString(SSID_KEY, "");
    }


    public static int getLevel(int level) {
        if (Math.abs(level) < 50) {
            return 1;
        } else if (Math.abs(level) < 75) {
            return 2;
        } else if (Math.abs(level) < 90) {
            return 3;
        } else {
            return 4;
        }
    }

    //查看以前是否也配置过这个网络
    public static WifiConfiguration isExsits(String SSID, Context context) {
        WifiManager wifimanager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        List<WifiConfiguration> existingConfigs = wifimanager.getConfiguredNetworks();
        for (WifiConfiguration existingConfig : existingConfigs) {
            if (existingConfig.SSID.contains(SSID)) {
                return existingConfig;
            }
        }
        return null;
    }

    /**
     * 判断wifi热点支持的加密方式
     */
    public static WifiCipherType getWifiCipher(String s) {

        if (s.isEmpty()) {
            return WifiCipherType.WIFICIPHER_INVALID;
        } else if (s.contains("WEP")) {
            return WifiCipherType.WIFICIPHER_WEP;
        } else if (s.contains("WPA") || s.contains("WPA2")) {
            return WifiCipherType.WIFICIPHER_WPA;
        } else {
            return WifiCipherType.WIFICIPHER_NOPASS;
        }
    }

    public static WifiConfiguration createWifiConfig(String SSID, String password, WifiCipherType type) {

        WifiConfiguration config = new WifiConfiguration();
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();
        config.SSID = "\"" + SSID + "\"";

        if (type == WifiCipherType.WIFICIPHER_NOPASS) {
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        }

        if (type == WifiCipherType.WIFICIPHER_WEP) {
            config.preSharedKey = "\"" + password + "\"";
            config.hiddenSSID = true;
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
        }

        if (type == WifiCipherType.WIFICIPHER_WPA) {
            config.preSharedKey = "\"" + password + "\"";
            config.hiddenSSID = true;
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            config.status = WifiConfiguration.Status.ENABLED;

        }

        return config;

    }

    /**
     * 接入某个wifi热点
     * connect to wifi
     */
    public static boolean addNetWork(WifiConfiguration config, Context context) {

        WifiManager wifimanager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        WifiInfo wifiinfo = wifimanager.getConnectionInfo();

        if (null != wifiinfo) {
            wifimanager.disableNetwork(wifiinfo.getNetworkId());
        }

        boolean result = false;

        if (config.networkId > 0) {
            result = wifimanager.enableNetwork(config.networkId, true);
            wifimanager.updateNetwork(config);
        } else {

            int i = wifimanager.addNetwork(config);
            result = false;

            if (i > 0) {

                wifimanager.saveConfiguration();
                return wifimanager.enableNetwork(i, true);
            }
        }

        return result;

    }

    /**
     * 接入某个wifi热点
     * connect to wifi
     */
    public static boolean DisConnectNetWork( Context context) {

        boolean result = false;
        WifiManager wifimanager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        WifiInfo wifiinfo = wifimanager.getConnectionInfo();

        if (null != wifiinfo) {
            result = wifimanager.disableNetwork(wifiinfo.getNetworkId());
        }

        return result;

    }

    /**
     * 是否 xiaomi 手机
     * is xiaomi phone
     */
    public static boolean isMIUI() {
        String manufacturer = Build.MANUFACTURER;
        if (!TextUtils.isEmpty(manufacturer) && "xiaomi".equalsIgnoreCase(manufacturer)) {
            return true;
        }
        return false;
    }

    /**
     * xiaomi 手动设置权限
     * xiaomi need to set wifi permission manually
     */
    public static void requestWifiPermision(AppCompatActivity activity){
        showDialo(activity);
    }

    public static void gotoAuthorize(AppCompatActivity activity){
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
        intent.setData(uri);
        activity.startActivityForResult(intent, MI_REQUEST_CODE);
    }

    public static void showDialo(final AppCompatActivity context){
       AlertDialog.Builder builder = new AlertDialog.Builder(context);
       builder.setTitle(context.getString(R.string.permission_request)).
               setMessage(context.getString(R.string.Wifi_permission)).setPositiveButton(context.getString(R.string.wifi_allow), new DialogInterface.OnClickListener() {
           @Override
           public void onClick(DialogInterface dialog, int which) {
               gotoAuthorize(context);
           }
       }).setNegativeButton(context.getString(R.string.wifi_denial), new DialogInterface.OnClickListener() {
           @Override
           public void onClick(DialogInterface dialog, int which) {

           }
       }).show();
    }


    public static boolean checkMIwifiPermission(AppCompatActivity activity){
        WifiManager wifimanager = (WifiManager) activity.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        long clickMill = System.currentTimeMillis();
        boolean enable = wifimanager.setWifiEnabled(true);
        long printMill = System.currentTimeMillis() - clickMill;
        boolean needAuthorize = printMill - BEST_RECORD_TIME > 0;
        Log.e("MI_J","needAuthorize = "+needAuthorize+" printMill = "+printMill);
        return needAuthorize ;
    }


}
