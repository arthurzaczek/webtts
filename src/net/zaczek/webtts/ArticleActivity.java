package net.zaczek.webtts;

import java.util.HashMap;

import net.zaczek.webtts.Data.ArticleRef;
import net.zaczek.webtts.Data.DataManager;
import net.zaczek.webtts.mpr.MediaPlayerReceiver;
import net.zaczek.webtts.mpr.RemoteControlListener;

import org.jsoup.Connection.Response;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;
import android.support.v4.app.NavUtils;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class ArticleActivity extends Activity implements OnInitListener,
		RemoteControlListener {
	private static final String TAG = "webtts";

	private static final int ABOUT_ID = 1;
	private static final int SHOW_TEXT_ID = 2;
	private static final int EXIT_ID = 3;

	private TextView txtArticle;
	private ProgressBar progBar;
	private ImageButton btnNext;
	private ImageButton btnPrev;
	private ImageButton btnPlayPause;

	private WakeLock wl;
	private MediaPlayerReceiver mediaPlayerReceiver = new MediaPlayerReceiver(
			this);

	private TextToSpeech tts;
	private HashMap<String, String> ttsParams = new HashMap<String, String>();
	private boolean ttsInitialized = false;
	private boolean isPlaying = false;

	private StringBuilder text;
	private String[] sentences;
	private int currentSentenceIdx = 0;

	// private ArrayList<ArticleRef> moreArticles;
	private ArticleRef article;

	/** Called when the activity is first created. */
	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.article);
		getActionBar().setDisplayHomeAsUpEnabled(true);

		final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
				"ListenToPageAndStayAwake");

		ttsParams.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "next");

		tts = new TextToSpeech(this, this);
		tts.setOnUtteranceCompletedListener(new OnUtteranceCompletedListener() {
			@Override
			public void onUtteranceCompleted(String utteranceId) {
				if (!isPlaying)
					return;
				final int localIdx = currentSentenceIdx;
				progBar.setProgress(localIdx);
				currentSentenceIdx++;
				if (sentences != null && localIdx < sentences.length) {
					tts.speak(sentences[localIdx], TextToSpeech.QUEUE_ADD,
							ttsParams);
				}
			}
		});

		// Register/Unregister MPR here to enable navigation in background
		mediaPlayerReceiver.registerReceiver(this);

		txtArticle = (TextView) findViewById(R.id.txtArticle);
		progBar = (ProgressBar) findViewById(R.id.progBar);
		btnNext = (ImageButton) findViewById(R.id.btnNext);
		btnPrev = (ImageButton) findViewById(R.id.btnPrev);
		btnPlayPause = (ImageButton)findViewById(R.id.btnPlayPause);

		final Intent intent = getIntent();
		article = intent.getParcelableExtra("article");
		if (article == null) {
			final String dataStr = intent.getStringExtra(Intent.EXTRA_TEXT);
			if (!TextUtils.isEmpty(dataStr)) {
				article = new ArticleRef();
				article.index = -1;
				article.url = dataStr;
				article.text = dataStr;
			}
		}
		fillData();
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

		// Register/Unregister MPR here to enable navigation in background
		mediaPlayerReceiver.unregisterReceiver(this);
	}

	private void play() {
		if (sentences != null && sentences.length > 0 && ttsInitialized
				&& !isPlaying) {
			final int localIdx = currentSentenceIdx;
			currentSentenceIdx++;
			tts.speak(sentences[localIdx], TextToSpeech.QUEUE_FLUSH, ttsParams);
			isPlaying = true;
			btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
		}
	}

	private void stop() {
		isPlaying = false;
		btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
		tts.stop();
	}

	private void prev() {
		if (article == null || article.index < 0)
			return;

		if (article.index > 0) {
			stop();
			article = DataManager.getCurrentArticles().get(article.index - 1);
			fillData();
		}
	}

	private void next() {
		if (article == null || article.index < 0)
			return;

		if (article.index < DataManager.getCurrentArticles().size()) {
			stop();
			article = DataManager.getCurrentArticles().get(article.index + 1);
			fillData();
		}
	}

	@Override
	public void onInit(int status) {
		if (status == TextToSpeech.SUCCESS) {
			// tts.setLanguage(Locale.GERMAN);
			ttsInitialized = true;
			play();
		} else {
			Log.e(TAG, "Initilization Failed");
		}
	}

	private void fillData() {
		if (article == null) {
			Log.e(TAG, "Nothing to read receivced");
			Toast.makeText(this, "Nothing to read receivced", Toast.LENGTH_LONG)
					.show();
			finish();
		}
		super.setTitle(article.text);
		progBar.setProgress(0);
		progBar.setMax(0);

		if (article.index < 0) {
			btnNext.setEnabled(false);
			btnPrev.setEnabled(false);
		} else {
			btnNext.setEnabled(true);
			btnPrev.setEnabled(true);
		}

		if (task == null) {
			task = new FillDataTask();
			task.execute();
		}
	}

	private FillDataTask task;

	private class FillDataTask extends AsyncTask<Void, Void, Void> {
		private String msg;
		private String url;
		private String[] defaultSelectors;
		private ProgressDialog dialog;

		public FillDataTask() {

			dialog = new ProgressDialog(ArticleActivity.this);
			dialog.setMessage("Loading");

			defaultSelectors = new String[] { "article", "main",
					"div[id=article], div.article", "div[id=story], div.story",
					"div[id=content], div.content", "div[id=main], div.main", };
			url = article.url;
			text = new StringBuilder();
			// moreArticles = new ArrayList<ArticleRef>();
			txtArticle.setText("Loading " + url);
		}

		@Override
		protected void onPreExecute() {
			dialog.show();
			super.onPreExecute();
		}

		@Override
		protected Void doInBackground(Void... params) {
			try {
				final Response response = DataManager.jsoupConnect(url)
						.execute();
				final int status = response.statusCode();
				if (status == 200) {
					final Document doc = response.parse();
					Elements elements = null;

					for (String selector : defaultSelectors) {
						Log.d(TAG, "Trying selector " + selector);
						elements = doc.select(selector);
						if (elements != null && elements.size() > 0) {
							Log.d(TAG, "  -> found " + elements.size()
									+ " elements");
							break;
						}
					}

					if (elements == null) {
						msg = "Article text could not be extracted, no suitable root element found";
						Log.w(TAG, msg);
						return null;
					}

					for (Element e : elements) {
						final String innerText = e.text();
						if (!TextUtils.isEmpty(innerText)) {
							text.append(innerText);
							if (text.charAt(text.length() - 1) != '.') {
								text.append(".");
							}
							text.append("\n");
						}
					}

					// More Articles
					// Elements links = doc.select("a");
					// int idx = 0;
					// for (Element lnk : links) {
					// moreArticles.add(new ArticleRef(lnk.attr("abs:href"),
					// lnk.text(), idx));
					// idx++;
					// }

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
			dialog.dismiss();

			if (!TextUtils.isEmpty(msg)) {
				Toast.makeText(ArticleActivity.this, msg, Toast.LENGTH_SHORT)
						.show();
				txtArticle.setText(msg);
			} else {
				sentences = text.toString().split("(?<=[a-z])\\.\\s+");
				txtArticle.setText(String.format("%d chars, %d sentences",
						text.length(), sentences.length));
				progBar.setProgress(0);
				progBar.setMax(sentences.length);
				currentSentenceIdx = 0;
				play();
			}
			task = null;
			super.onPostExecute(result);
		}
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
			startActivity(new Intent(this, AboutActivity.class));
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

	public void onPlayPause(View v) {
		if (isPlaying) {
			stop();
		} else {
			play();
		}
	}

	public void onNext(View v) {
		next();
	}

	public void onPrev(View v) {
		prev();
	}

	@Override
	public void onMediaPlay() {
		if (isPlaying) {
			stop();
		} else {
			play();
		}
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
