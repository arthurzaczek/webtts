package net.zaczek.webtts;

import java.util.ArrayList;

import net.zaczek.webtts.Data.DataManager;
import net.zaczek.webtts.Data.WebSiteRef;
import net.zaczek.webtts.mpr.MediaPlayerReceiver;
import net.zaczek.webtts.mpr.RemoteControlListener;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
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

public class MainActivity extends ListActivity implements OnInitListener,
		RemoteControlListener {
	private static final String TAG = "WebTTS";

	private ArrayAdapter<WebSiteRef> adapter;
	private TextToSpeech tts;
	private boolean ttsInitialized = false;
	private MediaPlayerReceiver mediaPlayerReceiver = new MediaPlayerReceiver(
			this);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(TAG, "Starting WebTTS main activity");
		setContentView(R.layout.activity_main);

		tts = new TextToSpeech(this, this);

		registerForContextMenu(getListView());

		getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		fillData();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		mediaPlayerReceiver.registerReceiver(this);
	}
	
	@Override
	protected void onPause() {
		mediaPlayerReceiver.unregisterReceiver(this);
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (tts != null) {
			tts.stop();
			tts.shutdown();
		}
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		play(position);
	}

	private void play(int position) {
		if (position < 0 || position >= getListAdapter().getCount())
			return;

		Intent i = new Intent(this, ArticleListActivity.class);
		WebSiteRef website = adapter.getItem(position);
		Log.i(TAG, "Opening website " + website.text);
		i.putExtra("website", website);
		startActivity(i);
	}

	private void fillData() {
		try {
			ArrayList<WebSiteRef> data = DataManager.readWebSites();
			if (data.size() == 0) {
				data.add(new WebSiteRef("Please sync..."));
			}
			adapter = new ArrayAdapter<WebSiteRef>(this,
					android.R.layout.simple_list_item_activated_1, data);
			setListAdapter(adapter);
			select(0);
		} catch (Exception ex) {
			Toast.makeText(this, ex.toString(), Toast.LENGTH_LONG).show();
		}
	}

	private void select(int idx) {
		if (idx >= getListAdapter().getCount())
			return;
		setSelection(idx);
		getListView().setItemChecked(idx, true);
		if (ttsInitialized) {
			final WebSiteRef ws = adapter.getItem(idx);
			tts.speak(ws.text, TextToSpeech.QUEUE_FLUSH, null);
		}
	}

	private void next() {
		if (getListAdapter().getCount() == 0)
			return;
		final int idx = getListView().getCheckedItemPosition();
		if (idx + 1 < getListAdapter().getCount()) {
			select(idx + 1);
		} else {
			select(0);
		}
	}

	private void prev() {
		final int count = getListAdapter().getCount();
		if (count == 0)
			return;
		final int idx = getListView().getCheckedItemPosition();
		if (idx > 0) {
			select(idx - 1);
		} else {
			select(count - 1);
		}
	}

	public void onPlay(View v) {
		final int idx = getListView().getCheckedItemPosition();
		play(idx);
	}
	
	public void onNext(View v) {
		next();
	}

	public void onPrev(View v) {
		prev();
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
			AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item
					.getMenuInfo();
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

	@Override
	public void onInit(int status) {
		if (status == TextToSpeech.SUCCESS) {
			// tts.setLanguage(Locale.GERMAN);
			ttsInitialized = true;
			select(getListView().getCheckedItemPosition()); // reselect to
															// anounce current
															// item
		} else {
			Log.e(TAG, "Initilization Failed");
		}
	}
	
	@Override
	public void onMediaPlay() {
	}

	@Override
	public void onMediaNext() {
		next();
	}

	@Override
	public void onMediaPrev() {
		prev();
	}
}
