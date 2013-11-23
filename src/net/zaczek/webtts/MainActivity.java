package net.zaczek.webtts;

import java.util.ArrayList;

import net.zaczek.webtts.Data.DataManager;
import net.zaczek.webtts.Data.WebSiteRef;
import net.zaczek.webtts.Data.WebSiteRefAdapter;
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
import android.widget.AdapterView.OnItemSelectedListener;

public class MainActivity extends AbstractListActivity implements
		OnItemSelectedListener {
	private static final String TAG = "WebTTS";

	private ArrayAdapter<WebSiteRef> adapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		registerForContextMenu(getListView());
		fillData();
	}

	@Override
	public void onItemSelected(AdapterView<?> adapterView, View view, int pos,
			long id) {
		try {
			speak(adapter.getItem(pos).text);
		} catch (Exception ex) {
			Log.e(TAG, ex.toString());
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {

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
			adapter = new WebSiteRefAdapter(this, data);
			setListAdapter(adapter);
			setSelection(0);
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
		case R.id.action_sync:
			sync();
			return true;
		case R.id.action_exit:
			finish();
			return true;
		}

		return super.onMenuItemSelected(featureId, item);
	}

	private class SyncTask extends AsyncTask<Void, Void, Void> {
		private String msg;
		ProgressDialog dialog;
		
		public SyncTask()
		{
			dialog = new ProgressDialog(MainActivity.this);
			dialog.setMessage("Syncing WebSites");
		}

		@Override
		protected void onPreExecute() {
			dialog.show();
			super.onPreExecute();
		}

		@Override
		protected Void doInBackground(Void... params) {
			try {
				DataManager.downloadWebSitesSettings();
			} catch (Exception ex) {
				msg = ex.toString();
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			dialog.dismiss();
			if (!TextUtils.isEmpty(msg)) {
				Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT)
						.show();
			}
			fillData();
			syncTask = null;
			super.onPostExecute(result);
		}
	}

	private SyncTask syncTask;

	private void sync() {
		Log.d(TAG, "Syncing websites");
		if (syncTask == null) {
			syncTask = new SyncTask();
			syncTask.execute();
		}
	}
}
