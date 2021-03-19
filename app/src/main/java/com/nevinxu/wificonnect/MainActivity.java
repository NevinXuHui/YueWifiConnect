package com.nevinxu.wificonnect;

import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.nevinxu.wifiscan.Utils.YueWifiHelper;
import com.nevinxu.wifiscan.listener.ScanResultListener;
import com.lee.yuewificonnect.R;
import com.nevinxu.wificonnect.adapter.WifiAdapter;

import java.util.List;

public class MainActivity extends AppCompatActivity implements ScanResultListener  {

//    public static final String HOTPOINT_NBO = "CMCC-WiFi6";
    public static final String HOTPOINT_NBO = "CMCC-AP1-5G";
    public static final String HOTPOINT_PASSWORD = "12345678";
    private RecyclerView rec;
    private TextView tvCurrentWifi;
    private ImageView ivScan;

    private WifiAdapter adapter;
    private YueWifiHelper helper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_wifi);
        rec = findViewById(R.id.rec);

        rec.setLayoutManager(new LinearLayoutManager(this));

        adapter = new WifiAdapter();
        rec.setAdapter(adapter);

        tvCurrentWifi = findViewById(R.id.tv_wifi_name);

        helper = new YueWifiHelper(this,this);

        ivScan = findViewById(R.id.iv_add);
        ivScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                helper.startScan();   //按键触发搜索
//                WifiUtil.checkMIwifiPermission(MainActivity.this);
            }
        });



    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onStop() {
        super.onStop();
        helper.stop();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        helper.destroy();
    }


    @Override
    public void resultSuc(final List<ScanResult> list, boolean isLastTime) {
        if(list != null && list.size() > 0){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    adapter.setItems(list);
                    adapter.notifyDataSetChanged();
                }
            });
            helper.filterAndConnectTargetWifi(list,HOTPOINT_NBO,HOTPOINT_PASSWORD,isLastTime);

        }
    }

    @Override
    public void filterFailure() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvCurrentWifi.setText(">>>>> Filter Failure!");
            }
        });
    }

    @Override
    public void connectedWifiCallback(final WifiInfo info) {
        final boolean isConnect = helper.isConnected(info);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
               if(isConnect){
                   tvCurrentWifi.setText(info.getSSID());
               }else {
                   tvCurrentWifi.setText(info.getSSID()+"  >>> Connect Failure!");
               }
            }
        });
    }
}
