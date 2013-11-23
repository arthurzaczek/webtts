package net.zaczek.webtts.Data;

import java.util.ArrayList;

import net.zaczek.webtts.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class WebSiteRefAdapter extends ArrayAdapter<WebSiteRef> {
	private ArrayList<WebSiteRef> websites;

	public WebSiteRefAdapter(Context context,
			ArrayList<WebSiteRef> websites) {
		super(context, R.layout.list_item_websites, websites);
		this.websites = websites;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View v = convertView;
		if (v == null) {
			
			LayoutInflater vi = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			v = vi.inflate(R.layout.list_item_websites, null);
		}

		WebSiteRef site = websites.get(position);
		if (site != null) {
			TextView text = (TextView) v.findViewById(R.id.text_row);
			ImageView image = (ImageView) v.findViewById(R.id.image_row);

			if (text != null) {
				text.setText(site.text);
			}

			if (image != null) {
				
			}
		}
		return v;
	}
}