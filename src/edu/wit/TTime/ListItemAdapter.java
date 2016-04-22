package edu.wit.TTime;

import java.util.ArrayList;
import java.util.List;

import android.app.LauncherActivity.ListItem;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class ListItemAdapter extends ArrayAdapter<edu.wit.TTime.ListItem> {
	
	private LayoutInflater mInflater;

	
	public ListItemAdapter(Context context, int resource, List<edu.wit.TTime.ListItem> objects) {
		super(context, resource, objects);
		mInflater = (LayoutInflater) context.getSystemService(context.LAYOUT_INFLATER_SERVICE);
	}
	
	public View getView(int position, View convertView, ViewGroup parent) {
		
		edu.wit.TTime.ListItem item;
		item = getItem(position);
		
		View view = mInflater.inflate(R.layout.list_item, parent, false);
		
		TextView headsign;
		headsign = (TextView)view.findViewById(R.id.headsign);
		headsign.setText(item.headsign);
		
		TextView dateTime;
		dateTime = (TextView)view.findViewById(R.id.dateTime);
		dateTime.setText(item.dateTime);
		
		return view;
	}

	



	

	

}
