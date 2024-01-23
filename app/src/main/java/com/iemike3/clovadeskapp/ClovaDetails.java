package com.iemike3.clovadeskapp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

public class ClovaDetails extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clova_details);
        getActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#ffffff")));
        getActionBar().setElevation(0);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        Intent fromIntent = getIntent();
        Bundle extras = fromIntent.getExtras();

        try {
            JSONObject jsonObject = new JSONObject(extras.getString("clova_return_json"));

            ((TextView) findViewById(R.id.device_name_value)).setText(extras.getString("clova_device_name"));
            ((TextView) findViewById(R.id.device_wifi_mac_value)).setText(jsonObject.getJSONObject("firmware").getString("wifimac"));
            ((TextView) findViewById(R.id.device_serial_value)).setText(jsonObject.getJSONObject("firmware").getString("serial"));
            ((TextView) findViewById(R.id.device_firmware_version_value)).setText(jsonObject.getJSONObject("firmware").getString("ver"));
            ((TextView) findViewById(R.id.device_hasclovatoken_value)).setText(String.valueOf(jsonObject.getJSONObject("firmware").getBoolean("hasClovaToken")));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        /*
        findViewById(R.id.btn_device_check).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.clovaDeskConnector.checkMyDevice();
            }
        });
         */

    }

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
