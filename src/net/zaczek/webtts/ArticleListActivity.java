package net.zaczek.webtts;

import java.util.ArrayList;
import java.util.HashMap;
import net.zaczek.webtts.Data.ArticleRef;
import net.zaczek.webtts.Data.DataManager;
import net.zaczek.webtts.Data.WebSiteRef;

import org.jsoup.Connection.Response;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
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

public class ArticleListActivity extends AbstractListActivity implements
		OnItemSelectedListener {
	private static final String TAG = "webtts";

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
			// ArticleRef a = adapter.getItem(pos);
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
		Intent i = new Intent(this, ArticleActivity.class);
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
		private ProgressDialog dialog;

		public FillDataTask(String url) {
			dialog = new ProgressDialog(ArticleListActivity.this);
			dialog.setMessage("Loading");
			this.url = url;
		}

		@Override
		protected void onPreExecute() {
			dialog.show();
			super.onPreExecute();
		}

		@Override
		protected Void doInBackground(Void... params) {
			try {
				articles = new ArrayList<ArticleRef>();
				articlesMap = new HashMap<Integer, ArrayList<ArticleRef>>();
				String link_selector = DataManager.getLinkSelector(webSite);
				useMap = TextUtils.isEmpty(link_selector);
				if (TextUtils.isEmpty(link_selector)) {
					link_selector = "a";
				}
				
				Log.i(TAG, "Downloading article list from " + url);
				final Response response = DataManager.jsoupConnect(url).execute();
				final int status = response.statusCode();
				if (status == 200) {
					Log.i(TAG, "Start parsing");
					final Document doc = response.parse();
					final Elements links = doc.select(link_selector);
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
			dialog.dismiss();

			if (!TextUtils.isEmpty(msg)) {
				Toast.makeText(ArticleListActivity.this, msg, Toast.LENGTH_SHORT)
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
			adapter = new ArrayAdapter<ArticleRef>(ArticleListActivity.this,
					android.R.layout.simple_list_item_1, articles);
			setListAdapter(adapter);

			super.onPostExecute(result);
		}
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
			startActivity(new Intent(this, AboutActivity.class));
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
