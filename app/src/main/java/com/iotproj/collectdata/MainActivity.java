package com.iotproj.collectdata;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // Wifi 스캐닝, 권한 획득 관련 변수
    private PermissionSupport permission;
    WifiManager wifiManager;
    BroadcastReceiver wifiScanReceiver;

    // DB 관리 관련 변수
    SQLiteDatabase db;
    NewSQLiteOpenHelper dbHelper;

    //레이아웃 컨트롤 관련 변수
    EditText inputLocate;
    Button scanBtn,submitBtn;
    ListView wifiList;
    List<ScanResult> wifiResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 사용자에게 필요한 권한 요청
        permissionCheck();

        inputLocate = findViewById(R.id.inputLocation);
        scanBtn = findViewById(R.id.scanNow);
        submitBtn = findViewById(R.id.submitDB);

        wifiList = findViewById(R.id.wifiList);
        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        dbHelper = new NewSQLiteOpenHelper(MainActivity.this, "person.db", null, 1);

        // 시스템에서 각종 변경 정보를 인식했을 때, 그 중에서도 Wifi 스캔 값이 변경되었을 경우 동작
        wifiScanReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                boolean success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false);
                if (success) {
                    scanSuccess();
                    Log.e("wifi","scan Success!!!!!");
                    wifiAnalyzer();
                }
                else {
                    scanFailure();
                    Log.e("wifi","scan Failure.....");
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        this.registerReceiver(wifiScanReceiver, intentFilter);

        // Scan Now 버튼을 눌렀을 때 작동
        scanBtn.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onClick(View v) {
                boolean success = wifiManager.startScan();
                if (!success) {
                    scanFailure();
                }
                wifiResult = wifiManager.getScanResults();
            }
        });

        // DB 제출 버튼을 눌렀을 때 작동
        submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String targetPos = inputLocate.getText().toString();
                if (targetPos.length() == 0) {
                    Toast.makeText(MainActivity.this, "Target Position should not empty", Toast.LENGTH_LONG).show();
                    return;
                }

                //경고창을 띄울 공간
                AlertDialog.Builder myAlertBuilder = new AlertDialog.Builder(MainActivity.this);
                myAlertBuilder.setTitle("위치 확인");
                myAlertBuilder.setMessage("직전에 WiFi를 스캔한 위치가 " + targetPos + "이(가) 맞습니까? \n 이 작업은 되돌릴 수 없습니다.");
                myAlertBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // OK를 눌렀을 경우에 행동할 내용들
                        int totDelete = delete(targetPos);

                        // DB 업로드 시 스캔 결과 정렬
                        Comparator<ScanResult> comparator = new Comparator<ScanResult>() {
                            @Override
                            public int compare(ScanResult o1, ScanResult o2) {
                                return o2.level - o1.level;
                            }
                        };
                        Collections.sort(wifiResult, comparator);
                        int count = 0;
                        for (ScanResult choseWifi : wifiResult) {
                            if (count >= 7) break;
                            count += 1;
                            insert(choseWifi.BSSID, choseWifi.level, targetPos);
                        }

                        Toast.makeText(MainActivity.this, totDelete + "행 데이터 제거, " + count + "행 데이터 추가", Toast.LENGTH_SHORT).show();
                        //
                    }
                });
                myAlertBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(MainActivity.this, "DB upload canceled.", Toast.LENGTH_LONG).show();
                    }
                });
                myAlertBuilder.show();
            }
        });
    }


    //===========================================
    //=========== SQLite DB 명령어 영역 ===========
    //===========================================
    private void insert(String mac, int rss, String pos) {
        db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put("mac", mac);
        values.put("rss", rss);
        values.put("pos", pos);
        db.insert("fingerprint", null, values);
    }

    // 현재 위치에 등록된 정보를 전부 삭제 (DB 업로드 시 delete 후 insert)
    private int delete(String pos) {
        db = dbHelper.getWritableDatabase();
        int deleteRows = db.delete("fingerprint", "pos=?", new String[]{pos});
        return deleteRows;
    }


    //===========================================
    //========== WiFi 스캐닝 컨트롤 영역 ===========
    //===========================================
    //수집한 Wifi 정보를 배열에 뿌리는 역할
    private void wifiAnalyzer() {
        List<String> list = new ArrayList<>();
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, list);
        String mac, dbm, freq;
        for (ScanResult choseWifi : wifiResult) {
            mac = choseWifi.BSSID;
            dbm = Integer.toString(choseWifi.level) + "dBm";
            freq = Integer.toString(choseWifi.frequency);

            String completeInfo = mac + " | " + dbm + " | " + freq;
            list.add(completeInfo);
        }
        wifiList.setAdapter(adapter);
    }

    //Wifi 정보 스캔에 성공했을 경우에 행동할 것들
    private void scanSuccess() {
        @SuppressLint("MissingPermission") List<ScanResult> results = wifiManager.getScanResults();
        Log.e("wifi", results.toString());
        Toast.makeText(this.getApplicationContext(), "Wifi Scan Success", Toast.LENGTH_LONG).show();
    }

    //Wifi 정보 스캔에 실패했을 경우에 행동할 것들
    private void scanFailure() {
        @SuppressLint("MissingPermission") List<ScanResult> results = wifiManager.getScanResults();
        Log.e("wifi", results.toString());
        Toast.makeText(this.getApplicationContext(), "Wifi Scan Failure, Old Information may appear.", Toast.LENGTH_LONG).show();
    }

    //===========================================
    //=========== 필요한 권한 확인 영역 ============
    //===========================================
    // 필요한 권한 체크
    private void permissionCheck() {
        permission = new PermissionSupport(this, this);
        if (!permission.checkPermission()) {
            permission.requestPermission();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (!permission.permissionResult(requestCode, permissions, grantResults)) {
            permission.requestPermission();
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}