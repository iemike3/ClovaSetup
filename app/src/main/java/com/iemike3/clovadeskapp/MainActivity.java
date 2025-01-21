package com.iemike3.clovadeskapp;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.iemike3.clovaconnector.ClovaSPPCallback;
import com.iemike3.clovaconnector.ClovaSPPConnector;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends Activity {

    public static ClovaSPPConnector clovaSPPConnector;;
    private BluetoothAdapter bluetoothAdapter;
    private ClovaSearchBroadcastReceiver clovaSearchBroadcastReceiver;

    public int checkSelfPermission(final String permission) {
        return ((getPackageManager().checkPermission(permission, getPackageName()) == PackageManager.PERMISSION_GRANTED) ? PackageManager.PERMISSION_GRANTED : PackageManager.PERMISSION_DENIED);
    }

    public void requestNeedPermissions() {
        String[] needPermissions;
        boolean isNeedRequestPermission = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                needPermissions = new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.ACCESS_FINE_LOCATION};
            } else {
                needPermissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
            }
        } else {
            needPermissions = new String[]{Manifest.permission.ACCESS_COARSE_LOCATION};
        }
        for (String permission : needPermissions) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                isNeedRequestPermission = true;
            }
        }
        if (isNeedRequestPermission) {
            requestPermissions(needPermissions, 1100);
        }
    }

    public boolean checkNeedPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                        checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                        checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    return true;
                }
                return false;
            } else {
                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    return true;
                }
                return false;
            }
        } else {
            if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                return true;
            }
            return false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(clovaSearchBroadcastReceiver);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        // Bluetoothが有効化されていなければ有効化するようにIntent
        if (!bluetoothAdapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, 1000);
        }

        //buildScanFilters(), buildScanSettings(),
        findViewById(R.id.connect_clova).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!bluetoothAdapter.isEnabled()) {
                    Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(intent, 1001);
                    return;
                }

                if (!checkNeedPermissions()) {
                    requestNeedPermissions();
                    return;
                }

                findViewById(R.id.connect_clova).setEnabled(false);

                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
                clovaSearchBroadcastReceiver = new ClovaSearchBroadcastReceiver();
                registerReceiver(clovaSearchBroadcastReceiver, intentFilter);

                LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

                if (bluetoothAdapter.startDiscovery()) {
                    Toast.makeText(MainActivity.this, "Clovaを検索中", Toast.LENGTH_SHORT).show();
                } else {
                    if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q && !locationManager.isLocationEnabled()) {
                        new AlertDialog.Builder(MainActivity.this)
                                .setMessage("Clovaに接続するには、位置情報を有効にしてください。")
                                .setPositiveButton("有効にする", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                        startActivity(intent);
                                    }
                                })
                                .show();
                    }
                    findViewById(R.id.connect_clova).setEnabled(true);
                    Toast.makeText(MainActivity.this, "Clovaの検索に失敗しました。", Toast.LENGTH_SHORT).show();
                }
            }
        });

        findViewById(R.id.disconnect_clova).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findViewById(R.id.connect_clova).setEnabled(true);
                findViewById(R.id.disconnect_clova).setEnabled(false);
                Toast.makeText(MainActivity.this, "切断しました", Toast.LENGTH_SHORT).show();
                clovaSPPConnector.close();
            }
        });

        findViewById(R.id.clova_checkdevice).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (clovaSPPConnector != null) {
                    clovaSPPConnector.checkMyDevice();
                } else {
                    Toast.makeText(MainActivity.this, "Clovaが接続されていません", Toast.LENGTH_SHORT).show();
                }
            }
        });

        findViewById(R.id.clova_firmware).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (clovaSPPConnector != null) {
                    clovaSPPConnector.getDetails();
                } else {
                    Toast.makeText(MainActivity.this, "Clovaが接続されていません", Toast.LENGTH_SHORT).show();
                }
            }
        });

        findViewById(R.id.clova_wifilist).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (clovaSPPConnector != null) {
                    clovaSPPConnector.getWifiList();
                } else {
                    Toast.makeText(MainActivity.this, "Clovaが接続されていません", Toast.LENGTH_SHORT).show();
                }
            }
        });

        findViewById(R.id.clova_setauth).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (clovaSPPConnector != null) {
                    clovaSPPConnector.setAuth();
                } else {
                    Toast.makeText(MainActivity.this, "Clovaが接続されていません", Toast.LENGTH_SHORT).show();
                }
            }
        });

        findViewById(R.id.clova_protocolver).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (clovaSPPConnector != null) {
                    clovaSPPConnector.getProtocolVersion();
                } else {
                    Toast.makeText(MainActivity.this, "Clovaが接続されていません", Toast.LENGTH_SHORT).show();
                }
            }
        });

        findViewById(R.id.clova_prepareinfo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (clovaSPPConnector != null) {
                    clovaSPPConnector.getPrepareInfo();
                } else {
                    Toast.makeText(MainActivity.this, "Clovaが接続されていません", Toast.LENGTH_SHORT).show();
                }
            }
        });

        findViewById(R.id.clova_enable_btpan).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (clovaSPPConnector != null) {
                    clovaSPPConnector.toggleBTPan(true);
                } else {
                    Toast.makeText(MainActivity.this, "Clovaが接続されていません", Toast.LENGTH_SHORT).show();
                }
            }
        });

        findViewById(R.id.clova_disable_btpan).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (clovaSPPConnector != null) {
                    clovaSPPConnector.toggleBTPan(false);
                } else {
                    Toast.makeText(MainActivity.this, "Clovaが接続されていません", Toast.LENGTH_SHORT).show();
                }
            }
        });

        findViewById(R.id.clova_changeserver).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (clovaSPPConnector != null) {
                    View clova_changeserver = LayoutInflater.from(MainActivity.this).inflate(R.layout.dialog_clova_changeserver, null);
                    LinearLayout clova_layout_custom_svurl = clova_changeserver.findViewById(R.id.clova_layout_custom_svurl);
                    EditText clova_edittext_svurl = clova_changeserver.findViewById(R.id.clova_edittext_svurl);

                    ArrayAdapter<String> adapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_spinner_item);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

                    adapter.add("LINE (Production)");
                    adapter.add("NAVER (Production)");
                    adapter.add("Custom");

                    Spinner spinner = (Spinner) clova_changeserver.findViewById(R.id.clova_connectto_server_spinner);
                    spinner.setAdapter(adapter);
                    spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                            switch (position) {
                                case 0: // LINE (Production)
                                    clova_layout_custom_svurl.setVisibility(View.GONE);
                                    break;
                                case 1: // NAVER (Production)
                                    clova_layout_custom_svurl.setVisibility(View.GONE);
                                    break;
                                case 2: // Custom
                                    clova_layout_custom_svurl.setVisibility(View.VISIBLE);
                                    break;
                            }
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parentView) {
                        }
                    });

                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this)
                            .setView(clova_changeserver)
                            .setNegativeButton("キャンセル", null)
                            .setPositiveButton("サーバーを変更", null);

                    AlertDialog dialog = builder.show();
                    Button button = dialog.getButton(DialogInterface.BUTTON_POSITIVE);

                    button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            switch (spinner.getSelectedItemPosition()) {
                                case 0: // LINE (Production)
                                    clovaSPPConnector.sendScheme(Uri.parse("clovadevice://server_setting/0"));
                                    dialog.dismiss();
                                    break;
                                case 1: // NAVER (Production)
                                    clovaSPPConnector.sendScheme(Uri.parse("clovadevice://server_setting/3"));
                                    dialog.dismiss();
                                    break;
                                case 2: // Custom
                                    if (!clova_edittext_svurl.getText().toString().startsWith("https://")) {
                                        Toast.makeText(MainActivity.this, "サーバーURLが無効です。", Toast.LENGTH_SHORT).show();
                                        break;
                                    }
                                    Uri.Builder uriBuilder = Uri.parse("clovadevice://server_setting/10").buildUpon();
                                    uriBuilder.appendQueryParameter("cic", clova_edittext_svurl.getText().toString());
                                    uriBuilder.appendQueryParameter("auth", clova_edittext_svurl.getText().toString());
                                    uriBuilder.appendQueryParameter("client_id", "fake_client_id");
                                    uriBuilder.appendQueryParameter("client_secret_id", "fake_secret_id");
                                    clovaSPPConnector.sendScheme(uriBuilder.build());
                                    dialog.dismiss();
                                    break;
                                default:
                                    return;
                            }
                        }
                    });
                } else {
                    Toast.makeText(MainActivity.this, "Clovaが接続されていません", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    class ClovaSearchBroadcastReceiver extends BroadcastReceiver {
        @SuppressWarnings("deprecation")
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null && intent.getAction().equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getName() != null && (device.getName().startsWith("WAVE-") || device.getName().startsWith("CLOVA-")) && device.getType() != 2) {
                    // CLOVAを発見
                    unregisterReceiver(clovaSearchBroadcastReceiver);
                    clovaSPPConnector = new ClovaSPPConnector(MainActivity.this, bluetoothAdapter, new ClovaSPPCallback() {
                        @Override
                        public void onConnected(String clovaDeviceName) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    findViewById(R.id.connect_clova).setEnabled(false);
                                    findViewById(R.id.disconnect_clova).setEnabled(true);
                                    Toast.makeText(MainActivity.this, clovaDeviceName + "に接続しました", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }

                        @Override
                        public void onDisconnected() {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    findViewById(R.id.connect_clova).setEnabled(true);
                                    findViewById(R.id.disconnect_clova).setEnabled(false);
                                }
                            });
                        }

                        @Override
                        public void onError(int errorType, String errorMessage) {
                            if (errorType == ClovaSPPConnector.ERROR_NOT_CONNECTED) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(MainActivity.this, "Clovaが接続されていません", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            } else if (errorType == ClovaSPPConnector.ERROR_FAILED_TO_CONNECT) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        findViewById(R.id.connect_clova).setEnabled(true);
                                        findViewById(R.id.disconnect_clova).setEnabled(false);

                                        new AlertDialog.Builder(MainActivity.this)
                                                .setMessage("Clovaへの接続に失敗しました")
                                                .setPositiveButton("OK", null)
                                                .show();
                                    }
                                });
                            }
                        }

                        @Override
                        public void onClovaData(BluetoothDevice clovaDevice, String type, String message) {
                            Log.d("onClovaData (" + type + ")", message);
                            if (type.contains("AMGFRQ")) {
                                try {
                                    JSONObject clova_details_json = new JSONObject(message);
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            View clova_details = LayoutInflater.from(MainActivity.this).inflate(R.layout.dialog_clova_details, null);
                                            ((TextView) clova_details.findViewById(R.id.clova_devicename_value)).setText(clovaDevice.getName());
                                            try {
                                                ((TextView) clova_details.findViewById(R.id.clova_macaddress_value)).setText(clova_details_json.getJSONObject("firmware").getString("wifimac"));
                                                ((TextView) clova_details.findViewById(R.id.clova_serial_value)).setText(clova_details_json.getJSONObject("firmware").getString("serial"));
                                                ((TextView) clova_details.findViewById(R.id.clova_firmwarever_value)).setText(clova_details_json.getJSONObject("firmware").getString("ver"));
                                                ((TextView) clova_details.findViewById(R.id.clova_hasclovatoken_value)).setText(String.valueOf(clova_details_json.getJSONObject("firmware").getBoolean("hasClovaToken")));
                                            } catch (JSONException e) {
                                                throw new RuntimeException(e);
                                            }
                                            new AlertDialog.Builder(MainActivity.this)
                                                    .setView(clova_details)
                                                    .setPositiveButton("閉じる", null)
                                                    .show();
                                        }
                                    });
                                } catch (JSONException e) {
                                    throw new RuntimeException(e);
                                }
                            } else if (type.contains("AMGNRQ")) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        startActivity(new Intent(MainActivity.this, ClovaWiFiList.class).putExtra("clovaWiFiJson", message));
                                    }
                                });
                            } else if (type.contains("AMPVRQ")) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        new AlertDialog.Builder(MainActivity.this)
                                            .setMessage(message)
                                            .setPositiveButton("閉じる", null)
                                            .show();
                                    }
                                });
                            }
                        }

                    });
                    clovaSPPConnector.connect(device);
                }
            }
        }
    }

    /*
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1000) {
            if (resultCode != RESULT_OK) {

            }
        }
    }
     */

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1100) {
            if (!checkNeedPermissions()) {
                String[] needPermissions;
                boolean isNeedOpenSettings = false;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        needPermissions = new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.ACCESS_FINE_LOCATION};
                    } else {
                        needPermissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
                    }
                } else {
                    needPermissions = new String[]{Manifest.permission.ACCESS_COARSE_LOCATION};
                }
                for (String permission : needPermissions) {
                    if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                        if (!shouldShowRequestPermissionRationale(permission)) {
                            isNeedOpenSettings = true;
                        }
                    }
                }
                if (isNeedOpenSettings) {
                    Toast.makeText(this, "一部の権限が拒否されました。\n設定画面から許可してください。", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }
            }
        }
    }
}

