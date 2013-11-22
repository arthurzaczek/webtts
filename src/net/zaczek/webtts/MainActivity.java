package net.zaczek.webtts;

import java.util.ArrayList;

import net.zaczek.webtts.Data.DataManager;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;

public class MainActivity extends AbstractListActivity implements
		OnItemSelectedListener {
	private static final String TAG = "WebTTS";

	private static final int DLG_WAIT = 1;

	private ArrayAdapter<WebSiteRef> adapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		fillData();
	}

	@Override
	public void onItemSelected(AdapterView<?> adapterView, View view, int pos,
			long id) {
		try {
			// mTTS.speak(adapter.getItem(pos).text, TTSManager.QUEUE_FLUSH,
			// null);
		} catch (Exception ex) {
			Log.e(TAG, ex.toString());
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {

	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Intent i = new Intent(this, ArticleList.class);
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
		} catch (Exception ex) {
			Toast.makeText(this, ex.toString(), Toast.LENGTH_LONG).show();
		}
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
			startActivity(new Intent(this, About.class));
			return true;
		case R.id.action_sync:
			sync();
			break;
		case R.id.action_exit:
			finish();
			return true;
		}

		return super.onMenuItemSelected(featureId, item);
	}

	private class SyncTask extends AsyncTask<Void, Void, Void> {
		private String msg;

		@Override
		protected void onPreExecute() {
			showDialog(DLG_WAIT);
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
			dismissDialog(DLG_WAIT);
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

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DLG_WAIT:
			ProgressDialog pDialog = new ProgressDialog(this);
			pDialog.setMessage("Syncing WebSites");
			return pDialog;
		}
		return null;
	}
}
