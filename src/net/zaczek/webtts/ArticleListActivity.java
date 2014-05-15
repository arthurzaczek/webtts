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
import android.app.ListActivity;
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
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class ArticleListActivity extends ListActivity {
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
		if (webSite != null) {
			setTitle(webSite.text);
		}
		
		getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		fillData();
	}

	public void onPlayAll(View v) {
		if (adapter.getCount() > 0) {
			play(0);
		}
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		play(position);
	}

	private void play(int position) {
		final ArticleRef a = adapter.getItem(position);
		final Intent i = new Intent(this, ArticleActivity.class);
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
				final String link_selector = "a";

				Log.i(TAG, "Downloading article list from " + url);
				final Response response = DataManager.jsoupConnect(url)
						.execute();
				final int status = response.statusCode();
				if (status == 200) {
					Log.i(TAG, "Start parsing");
					final Document doc = response.parse();
					final Elements links = doc.select(link_selector);
					Log.i(TAG, "Parsed " + links.size() + " links");
					String lnkText;
					String href;
					ArticleRef aRef;
					int segmentCount;
					for (Element lnk : links) {
						href = lnk.attr("abs:href");
						lnkText = lnk.text();
						if (!TextUtils.isEmpty(lnkText)) {
							Log.d(TAG, href);
							aRef = new ArticleRef(href, lnkText, -1);

							segmentCount = href.split("/").length;
							if (articlesMap.containsKey(segmentCount)) {
								final ArrayList<ArticleRef> tmpList = articlesMap.get(segmentCount);
								aRef.setIndex(tmpList.size());
								tmpList.add(aRef);
							} else {
								final ArrayList<ArticleRef> tmpList = new ArrayList<ArticleRef>();
								aRef.setIndex(0);
								tmpList.add(aRef);
								articlesMap.put(segmentCount, tmpList);
							}
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
				Toast.makeText(ArticleListActivity.this, msg,
						Toast.LENGTH_SHORT).show();
			}

			task = null;
			if (articlesMap.size() > 0) {
				// articles = articlesMap.Values.OrderBy(lst =>
				// lst.Count()).Last();
				// vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
				articles = articlesMap.values().iterator().next();
				for (ArrayList<ArticleRef> item : articlesMap.values()) {
					if (item.size() > articles.size())
						articles = item;
				}
				// ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
				// articles = articlesMap.Values.OrderBy(lst =>
				// lst.Count()).Last();
			}
			DataManager.setCurrentArticles(articles);
			adapter = new ArrayAdapter<ArticleRef>(ArticleListActivity.this,
					android.R.layout.simple_list_item_activated_1, articles);
			setListAdapter(adapter);
			setSelection(0);
			getListView().setItemChecked(0, true);

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
