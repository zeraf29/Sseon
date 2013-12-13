package com.sseon.gui.widget;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.sseon.R;

public class ButtonListAdapter<T> extends BaseAdapter {
	private LayoutInflater inflater;
	private ArrayList<T> items;
	private boolean buttonVisible;
	private String buttonLabel = "지우기";
	
	AdapterView.OnItemClickListener clickListener;
	
	public ButtonListAdapter(Context context, ArrayList<T> list) {
		this.items = list;
		this.inflater = (LayoutInflater) context.getSystemService(
				Context.LAYOUT_INFLATER_SERVICE);
	}
	
	public boolean getButtonVisible() {
		return buttonVisible;
	}

	public void setButtonVisible(boolean buttonVisible) {
		this.buttonVisible = buttonVisible;
		notifyDataSetChanged();
	}
	
	public void setButtonLabel(String label) {
		this.buttonLabel = label;
		notifyDataSetChanged();
	}

	/**
	 * 옆구리에 있는 버튼 눌렀을 때 알려주는 거
	 */
	public void setButtonClickListener(AdapterView.OnItemClickListener l) {
		this.clickListener = l;
	}

	@Override
	public int getCount() {
		return items.size();
	}

	@Override
	public Object getItem(int position) {
		return items.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = inflater.inflate(R.layout.removable_list_item, parent, false);
		}
		
		TextView text = (TextView) convertView.findViewById(R.id.text);
		text.setText(items.get(position).toString());
		
		Button button = (Button) convertView.findViewById(R.id.remove_button);
		button.setText(buttonLabel);
		button.setVisibility(buttonVisible ? View.VISIBLE : View.INVISIBLE);
		
		final int pos = position;
		final View view = convertView;
		button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				clickListener.onItemClick(null, view, pos, pos);
			}
		});
		return convertView;
	}
	
}