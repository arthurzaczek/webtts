package net.zaczek.webtts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import net.zaczek.webtts.Data.DataManager;

import org.jsoup.Connection.Response;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;
import android.support.v4.app.NavUtils;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class Article extends Activity implements OnInitListener {
	private static final String TAG = "webtts";

	private static final int DLG_WAIT = 1;

	private static final int ABOUT_ID = 1;
	private static final int SHOW_TEXT_ID = 2;
	private static final int EXIT_ID = 3;

	private TextView txtArticle;
	private ProgressBar progBar;
	private WakeLock wl;

	TextToSpeech tts;
	boolean ttsInitialized = false;
	boolean isPlaying = false;

	private StringBuilder text;
	private String[] sentences;
	private int currentSentenceIdx = 0;

	private ArrayList<ArticleRef> moreArticles;
	private WebSiteRef webSite;
	private ArticleRef article;

	private HashMap<String, String> ttsParams = new HashMap<String, String>();

	private MPR mediaPlayerReceiver = new MPR();

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
				if(!isPlaying) return;
				final int localIdx = currentSentenceIdx;
				progBar.setProgress(localIdx);
				currentSentenceIdx++;
				if (sentences != null && localIdx < sentences.length) {
					tts.speak(sentences[localIdx], TextToSpeech.QUEUE_ADD,
							ttsParams);
				}
			}
		});

		IntentFilter mediaFilter = new IntentFilter(Intent.ACTION_MEDIA_BUTTON);
		mediaFilter.setPriority(9999);
		this.registerReceiver(mediaPlayerReceiver, mediaFilter);

		txtArticle = (TextView) findViewById(R.id.txtArticle);
		progBar = (ProgressBar) findViewById(R.id.progBar);
		

		final Intent intent = getIntent();
		article = intent.getParcelableExtra("article");
		webSite = intent.getParcelableExtra("website");
		fillData();
	}

	private void play() {
		if (sentences != null && sentences.length > 0 && ttsInitialized
				&& !isPlaying) {
			final int localIdx = currentSentenceIdx;
			currentSentenceIdx++;
			tts.speak(sentences[localIdx], TextToSpeech.QUEUE_FLUSH, ttsParams);
			isPlaying = true;
		}
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
		super.setTitle(article.text);
		progBar.setProgress(0);
		progBar.setMax(0);

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
			url = article.url;
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
						int idx =0;
						for (Element lnk : links) {
							moreArticles.add(new ArticleRef(lnk
									.attr("abs:href"), lnk.text(), idx));
							idx++;
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
				sentences = text.toString().split("\\.");
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

	class MPR extends BroadcastReceiver {
		public MPR() {
			super();
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			String intentAction = intent.getAction();
			if (!Intent.ACTION_MEDIA_BUTTON.equals(intentAction)) {
				return;
			}
			KeyEvent event = (KeyEvent) intent
					.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
			if (event == null) {
				return;
			}
			try {
				int action = event.getAction();
				switch (action) {
				case KeyEvent.KEYCODE_MEDIA_PLAY:
					if(isPlaying) {
						stop();
					} else {
						play();
					}
					break;
				case KeyEvent.KEYCODE_MEDIA_NEXT:
					if(article.index < DataManager.getCurrentArticles().size()) {
						stop();
						article = DataManager.getCurrentArticles().get(article.index + 1);
						fillData();
					}
					break;
				case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
					if(article.index > 0) {
						stop();
						article = DataManager.getCurrentArticles().get(article.index - 1);
						fillData();
					}
					break;
				}
			} catch (Exception e) {
				Log.e(TAG, "Error in BroadcastReceiver", e);
			}
			abortBroadcast();
		}

		private void stop() {
			isPlaying = false;
			tts.stop();
		}
	}
}
