package net.zaczek.webtts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import net.zaczek.webtts.Data.DataManager;
import org.jsoup.Connection.Response;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.R.integer;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;

public class ArticleList extends AbstractListActivity implements
		OnItemSelectedListener {
	private static final String TAG = "webtts";

	private static final int DLG_WAIT = 1;

	private static final int ABOUT_ID = 1;
	private static final int EXIT_ID = 2;
	private ArrayAdapter<ArticleRef> adapter;

	private WebSiteRef webSite;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.articlelist);
		getActionBar().setDisplayHomeAsUpEnabled(true);

		Intent intent = getIntent();
		webSite = intent.getParcelableExtra("website");
		setTitle(webSite.text);

		getListView().setOnItemSelectedListener(this);
		fillData();
	}

	@Override
	public void onItemSelected(AdapterView<?> adapterView, View view, int pos,
			long id) {
		try {
			ArticleRef a = adapter.getItem(pos);
			// mTTS.speak(a.text, TTSManager.QUEUE_FLUSH, null);
		} catch (Exception ex) {
			Log.e(TAG, ex.toString());
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {

	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Intent i = new Intent(this, Article.class);
		ArticleRef a = adapter.getItem(position);
		i.putExtra("article", a);
		i.putExtra("website", webSite);
		startActivity(i);
	}

	private void fillData() {
		if (task == null) {
			task = new FillDataTask(webSite.url);
			task.execute();
		}
	}

	private FillDataTask task;

	@SuppressLint("UseSparseArrays")
	private class FillDataTask extends AsyncTask<Void, Void, Void> {
		private String msg;
		private String url;
		private ArrayList<ArticleRef> articles;
		private HashMap<Integer, ArrayList<ArticleRef>> articlesMap;
		private boolean useMap = true;

		public FillDataTask(String url) {
			this.url = url;
		}

		@Override
		protected void onPreExecute() {
			showDialog(DLG_WAIT);
			super.onPreExecute();
		}

		@Override
		protected Void doInBackground(Void... params) {
			try {
				articles = new ArrayList<ArticleRef>();
				articlesMap = new HashMap<Integer, ArrayList<ArticleRef>>();
				useMap = true; // TextUtils.isEmpty(webSite.link_selector);

				Log.i(TAG, "Downloading article list from " + url);
				Response response = DataManager.jsoupConnect(url).execute();
				int status = response.statusCode();
				if (status == 200) {
					Log.i(TAG, "Start parsing");
					Document doc = response.parse();
					Elements links;
					if (useMap) {
						links = doc.select("a");
					} else {
						links = doc.select(webSite.link_selector);
					}
					Log.i(TAG, "Parsed " + links.size() + " links");
					String lnkText;
					String href;
					ArticleRef aRef;
					int idx = 0;
					int segmentCount;
					for (Element lnk : links) {
						href = lnk.attr("abs:href");
						lnkText = lnk.text();
						if (!TextUtils.isEmpty(lnkText)) {
							Log.d(TAG, href);
							aRef = new ArticleRef(href, lnkText, idx);

							if (useMap) {
								segmentCount = href.split("/").length;
								if (articlesMap.containsKey(segmentCount)) {
									articlesMap.get(segmentCount).add(aRef);
								} else {
									ArrayList<ArticleRef> tmpList = new ArrayList<ArticleRef>();
									tmpList.add(aRef);
									articlesMap.put(segmentCount, tmpList);
								}
							} else {
								articles.add(aRef);
							}
							idx++;
						}
					}
				} else {
					msg = response.statusMessage();
					Log.w(TAG, "Error (" + status + "): " + msg);
				}
			} catch (Exception ex) {
				Log.e(TAG, "Error reading article list", ex);
				msg = ex.getMessage();
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			dismissDialog(DLG_WAIT);

			if (!TextUtils.isEmpty(msg)) {
				Toast.makeText(ArticleList.this, msg, Toast.LENGTH_SHORT)
						.show();
			}

			task = null;
			if (useMap && articlesMap.size() > 0) {
				// articles = articlesMap.Values.OrderBy(lst => lst.Count()).Last();
				// vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
				articles = articlesMap.values().iterator().next();
				for(ArrayList<ArticleRef> item : articlesMap.values()) {
					if(item.size() > articles.size())
						articles = item;
				}
				// ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
				// articles = articlesMap.Values.OrderBy(lst => lst.Count()).Last();
			}
			DataManager.setCurrentArticles(articles);
			adapter = new ArrayAdapter<ArticleRef>(ArticleList.this,
					android.R.layout.simple_list_item_1, articles);
			setListAdapter(adapter);

			super.onPostExecute(result);
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		ProgressDialog dialog;
		switch (id) {
		case DLG_WAIT:
			dialog = new ProgressDialog(this);
			dialog.setMessage("Loading");
			break;
		default:
			dialog = null;
		}
		return dialog;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, ABOUT_ID, 0, "About");
		menu.add(0, EXIT_ID, 0, "Exit");
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		int itemId = item.getItemId();
		switch (itemId) {
		case ABOUT_ID:
			startActivity(new Intent(this, About.class));
			return true;
		case EXIT_ID:
			finish();
			return true;
			// Respond to the action bar's Up/Home button
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}

		return super.onMenuItemSelected(featureId, item);
	}
}
