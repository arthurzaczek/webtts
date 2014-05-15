package net.zaczek.webtts;

import java.util.ArrayList;

import net.zaczek.webtts.Data.DataManager;
import net.zaczek.webtts.Data.WebSiteRef;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class MainActivity extends ListActivity {
	private static final String TAG = "WebTTS";

	private ArrayAdapter<WebSiteRef> adapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		registerForContextMenu(getListView());
		getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		fillData();
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Intent i = new Intent(this, ArticleListActivity.class);
		WebSiteRef website = adapter.getItem(position);
		i.putExtra("website", website);
		startActivity(i);
	}

	private void fillData() {
		try {
			ArrayList<WebSiteRef> data = DataManager.readWebSites();
			if (data.size() == 0) {
				data.add(new WebSiteRef("Please sync..."));
			}
			adapter = new ArrayAdapter<WebSiteRef>(this, android.R.layout.simple_list_item_activated_1, data);
			setListAdapter(adapter);
			setSelection(0);
			getListView().setItemChecked(0, true);
		} catch (Exception ex) {
			Toast.makeText(this, ex.toString(), Toast.LENGTH_LONG).show();
		}
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		getMenuInflater().inflate(R.menu.main_list_item, menu);
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		switch (itemId) {
		case R.id.action_edit:
			AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
			Intent i = new Intent(this, EditWebSiteActivity.class);
			WebSiteRef website = adapter.getItem(info.position);
			i.putExtra("website", website);
			startActivity(i);
			return true;
		}
		return super.onContextItemSelected(item);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		int itemId = item.getItemId();
		switch (itemId) {
		case R.id.action_add_website:
			startActivity(new Intent(this, EditWebSiteActivity.class));
			return true;
		case R.id.action_about:
			startActivity(new Intent(this, AboutActivity.class));
			return true;
		case R.id.action_exit:
			finish();
			return true;
		}

		return super.onMenuItemSelected(featureId, item);
	}
}
