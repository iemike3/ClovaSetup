package com.iemike3.clovadeskapp.clovawifi;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.iemike3.clovadeskapp.R;

import java.util.List;

public class WifiListAdapter extends ArrayAdapter<WifiListData> {

    private int mResource;
    private List<WifiListData> mItems;
    private LayoutInflater mInflater;

    public WifiListAdapter(Context context, int resource, List<WifiListData> items) {
        super(context, resource, items);

        mResource = resource;
        mItems = items;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;

        if (convertView != null) {
            view = convertView;
        }
        else {
            view = mInflater.inflate(mResource, null);
        }


        // リストビューに表示する要素を取得
        WifiListData item = mItems.get(position);

        // サムネイル画像を設定
        //ImageView thumbnail = (ImageView) view.findViewById(R.id.appicon);
        //thumbnail.setImageDrawable(item.getAppIcon());

        // タイトルを設定
        TextView title = view.findViewById(R.id.wifi_name);
        title.setText(item.getWifiName());

        //ProgressBar progressBar = view.findViewById(R.id.wifi_connect_progress);

        //TextView pkgname = (TextView) view.findViewById(R.id.packagename);
        //pkgname.setText(item.getPackageName());

        return view;
    }

}
