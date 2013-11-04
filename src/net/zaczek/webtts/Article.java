package net.zaczek.webtts;

import java.util.ArrayList;
import java.util.Locale;

import net.zaczek.webtts.Data.DataManager;

import org.jsoup.Connection.Response;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.support.v4.app.NavUtils;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

public class Article extends Activity implements OnInitListener {
	private static final String TAG = "webtts";

	private static final int DLG_WAIT = 1;

	private static final int ABOUT_ID = 1;
	private static final int SHOW_TEXT_ID = 2;
	private static final int EXIT_ID = 3;

	private TextView txtArticle;
	private WakeLock wl;
	
	TextToSpeech tts;
	boolean ttsInitialized = false;
	boolean isPlaying = false;

	private StringBuilder text;
	private ArrayList<ArticleRef> moreArticles;
	private WebSiteRef webSite;
	private ArticleRef article;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.article);
		getActionBar().setDisplayHomeAsUpEnabled(true);

		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
				"ListenToPageAndStayAwake");
		tts = new TextToSpeech(this, this);

		txtArticle = (TextView) findViewById(R.id.txtArticle);

		Intent intent = getIntent();
		article = intent.getParcelableExtra("article");
		webSite = intent.getParcelableExtra("website");
		super.setTitle(article.text);
		fillData();
	}
	
	@Override
	public void onInit(int status) {
		if (status == TextToSpeech.SUCCESS) {
			tts.setLanguage(Locale.GERMAN);
			ttsInitialized = true;
			play();
	    } else {
	        Log.e(TAG, "Initilization Failed");
	    }
	}

	@Override
	protected void onResume() {
		wl.acquire();
		super.onResume();
	}

	@Override
	protected void onPause() {
		wl.release();
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

	private void fillData() {
		if (task == null) {
			task = new FillDataTask();
			task.execute();
		}
	}

	private FillDataTask task;

	private class FillDataTask extends AsyncTask<Void, Void, Void> {
		private String msg;
		private String url;

		public FillDataTask() {
			this.url = article.url;
			text = new StringBuilder();
			moreArticles = new ArrayList<ArticleRef>();
			txtArticle.setText("Loading " + url);
		}

		@Override
		protected void onPreExecute() {
			showDialog(DLG_WAIT);
			super.onPreExecute();
		}

		@Override
		protected Void doInBackground(Void... params) {
			try {
				Response response = DataManager.jsoupConnect(url).execute();
				int status = response.statusCode();
				if (status == 200) {
					Document doc = response.parse();
					Elements elements = doc.select(webSite.article_selector);
					for (Element e : elements) {
						text.append(e.text());
						if (text.charAt(text.length() - 1) != '.') {
							text.append(".");
						}
						text.append("\n");
					}

					// More Articles
					if (!TextUtils.isEmpty(webSite.readmore_selector)) {
						Elements links = doc.select(webSite.readmore_selector);
						for (Element lnk : links) {
							moreArticles.add(new ArticleRef(lnk
									.attr("abs:href"), lnk.text()));
						}
					}
				} else {
					msg = response.statusMessage();
				}
			} catch (Exception ex) {
				Log.e(TAG, "Error reading article", ex);
				msg = ex.getMessage();
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			dismissDialog(DLG_WAIT);

			if (!TextUtils.isEmpty(msg)) {
				Toast.makeText(Article.this, msg, Toast.LENGTH_SHORT).show();
				txtArticle.setText(msg);
			} else {
				txtArticle.setText(String.format("%d chars", text.length()));
				play();
			}
			task = null;
			super.onPostExecute(result);
		}
	}

	private void play() {
		if (text != null && ttsInitialized && !isPlaying) {
			tts.speak(text.toString(), TextToSpeech.QUEUE_FLUSH, null);
			isPlaying = true;
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
		menu.add(0, SHOW_TEXT_ID, 0, "Show Text");
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
		case SHOW_TEXT_ID:
			txtArticle.setText(text);
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
