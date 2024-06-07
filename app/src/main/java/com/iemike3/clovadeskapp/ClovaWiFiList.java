package com.iemike3.clovadeskapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.iemike3.clovadeskapp.clovawifi.WifiListAdapter;
import com.iemike3.clovadeskapp.clovawifi.WifiListData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class ClovaWiFiList extends Activity {

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
            // タップしたアイテムの取得
            ListView listView = (ListView) parent;
            WifiListData item = (WifiListData) listView.getItemAtPosition(position);  // SampleListItemにキャスト

            try {
                for (int i = 0; i < clovaWiFiJson.length(); i++) {
                    JSONObject wifi = clovaWiFiJson.getJSONObject(i);
                    String ssid = wifi.getString("ssid");
                    if (item.getWifiName().equals(ssid)) {
                        LinearLayout linearLayout = null;
                        EditText editText = null;
                        AlertDialog.Builder alertDialog =  new AlertDialog.Builder(ClovaWiFiList.this);
                        alertDialog.setTitle(ssid);
                        alertDialog.setMessage(wifi.toString());
                        if (wifi.getInt("key_mgmt") != 0) {
                            linearLayout = new LinearLayout(ClovaWiFiList.this);
                            linearLayout.setOrientation(LinearLayout.VERTICAL);
                            linearLayout.setPadding(15, 0, 15 ,0);
                            editText = new EditText(ClovaWiFiList.this);
                            editText.setInputType(129);
                            linearLayout.addView(editText);
                            CheckBox checkBox = new CheckBox(ClovaWiFiList.this);
                            checkBox.setText("パスワードを表示");
                            EditText finalEditText = editText;
                            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                                @Override
                                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                    if (isChecked) {
                                        finalEditText.setTransformationMethod(null);
                                    } else {
                                        finalEditText.setTransformationMethod(new PasswordTransformationMethod());
                                    }
                                }
                            });
                            linearLayout.addView(checkBox);
                            alertDialog.setView(linearLayout);
                        }
                        EditText finalEditText1 = editText;
                        alertDialog.setPositiveButton("接続する", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                String password = null;
                                try {
                                    if (wifi.getInt("key_mgmt") != 0) {
                                        wifi.remove("psk");
                                        wifi.put("psk", finalEditText1.getText().toString());
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
