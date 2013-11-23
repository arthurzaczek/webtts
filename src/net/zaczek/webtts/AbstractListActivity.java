package net.zaczek.webtts;

import java.util.HashMap;
import java.util.Locale;

import android.app.ListActivity;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.Log;

public class AbstractListActivity extends ListActivity implements OnInitListener {
	private static final String TAG = "webtts";

	private TextToSpeech tts;
	private HashMap<String, String> ttsParams = new HashMap<String, String>();
	private boolean ttsInitialized = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		tts = new TextToSpeech(this, this);
	}
	
	@Override
	public void onInit(int status) {
		if (status == TextToSpeech.SUCCESS) {
			tts.setLanguage(Locale.GERMAN);
			ttsInitialized = true;
		} else {
			Log.e(TAG, "Initilization Failed");
		}
	}
	
	protected void speak(String text) {
		if (ttsInitialized) {
			tts.speak(text, TextToSpeech.QUEUE_FLUSH, ttsParams);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
}
