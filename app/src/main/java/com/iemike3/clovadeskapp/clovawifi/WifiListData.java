package com.iemike3.clovadeskapp.clovawifi;

public class WifiListData {
    private String mwifiName = null;

    public WifiListData() {}

    public WifiListData(String wifiName) {
        mwifiName = wifiName;
    }

    public void setWifiName(String wifiName) {
        mwifiName = wifiName;
    }

    public String getWifiName() {
        return mwifiName;
    }

}
