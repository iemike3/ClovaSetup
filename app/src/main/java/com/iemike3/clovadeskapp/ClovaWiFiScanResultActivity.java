package com.iemike3.clovadeskapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.iemike3.clovadeskapp.clovawifi.WifiListAdapter;
import com.iemike3.clovadeskapp.clovawifi.WifiListData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class ClovaWiFiScanResultActivity extends Activity {

    private JSONArray clovaWiFiJson;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clova_wifiscanlist);

        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setTitle("Wi-Fiに接続");

        ListView listView = findViewById(R.id.ui_clova_wifi_scanlist);

        // リストビューに表示する要素を設定
        ArrayList<WifiListData> listItems = new ArrayList<>();

        Intent fromIntent = getIntent();
        Bundle extras = fromIntent.getExtras();
        getActionBar().setSubtitle(extras.getString("clova_device_name"));

        try {
            clovaWiFiJson = new JSONObject(extras.getString("clovaWiFiJson")).getJSONArray("wifi");
            for (int i = 0;i < clovaWiFiJson.length(); i++) {
                JSONObject wifi = clovaWiFiJson.getJSONObject(i);
                String ssid = wifi.getString("ssid");
                //int signal_level = wifi.getInt("signal_level");

                listView.setOnItemClickListener(onItemClickListener);
                listItems.add(new WifiListData(ssid));

                //WifiListData item2 = new WifiListData("testWifiName");
                //listView.setOnItemClickListener(onItemClickListener);
                //listItems.add(new WifiListData("testWifiName341"));

                WifiListAdapter adapter = new WifiListAdapter(this, R.layout.recyclerview_clova_wifi_scanlist, listItems);
                listView.setAdapter(adapter);
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
    private AdapterView.OnItemClickListener onItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            ListView listView = (ListView) parent;
            WifiListData item = (WifiListData) listView.getItemAtPosition(position);

            try {
                for (int i = 0; i < clovaWiFiJson.length(); i++) {
                    JSONObject wifi = clovaWiFiJson.getJSONObject(i);
                    String ssid = wifi.getString("ssid");

                    if (item.getWifiName().equals(ssid)) {

                        String encryptTypeString = "不明";

                        switch (wifi.getInt("key_mgmt")) {
                            case 0:
                                encryptTypeString = "なし";
                                break;
                            case 2:
                                encryptTypeString = "EAP";
                                break;
                            case 4:
                                encryptTypeString = "PSK";
                                break;
                            case 8:
                                encryptTypeString = "WEP";
                                break;
                        }

                        View clova_details = LayoutInflater.from(ClovaWiFiScanResultActivity.this).inflate(R.layout.dialog_clova_connectwifi, null);
                        EditText editText_WiFi_Password = clova_details.findViewById(R.id.editText_wifi_password);

                        AlertDialog.Builder alertDialog =  new AlertDialog.Builder(ClovaWiFiScanResultActivity.this);
                        alertDialog.setTitle(ssid);
                        alertDialog.setView(clova_details);

                        ((TextView) clova_details.findViewById(R.id.textview_wifi_rssi)).setText("RSSI: " + String.valueOf(wifi.getInt("signal_level")));
                        ((TextView) clova_details.findViewById(R.id.textview_wifi_encrypt_type)).setText("暗号化: " + encryptTypeString);

                        if (wifi.getInt("key_mgmt") != 0) {
                            clova_details.findViewById(R.id.linearlayout_wifi_password).setVisibility(View.VISIBLE);
                            ((CheckBox) clova_details.findViewById(R.id.checkBox_show_wifi_password)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                                @Override
                                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                    int currentCursorPosition = editText_WiFi_Password.getSelectionStart();
                                    if (isChecked) {
                                        editText_WiFi_Password.setTransformationMethod(null);
                                        editText_WiFi_Password.setSelection(currentCursorPosition);
                                    } else {
                                        editText_WiFi_Password.setTransformationMethod(new PasswordTransformationMethod());
                                        editText_WiFi_Password.setSelection(currentCursorPosition);
                                    }
                                }
                            });
                        }

                        alertDialog.setPositiveButton("接続", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                try {
                                    if (wifi.getInt("key_mgmt") != 0) {
                                        wifi.remove("psk");
                                        wifi.put("psk", editText_WiFi_Password.getText().toString());
                                    }
                                 } catch (JSONException e) {
                                    throw new RuntimeException(e);
                                 }
                                 MainActivity.clovaSPPConnector.connectWifi(wifi);
                            }
                        });
                        alertDialog.show();
                    }
                }
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            //Toast.makeText(ClovaWiFiList.this, item.getWifiName(), Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }
}
