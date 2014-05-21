package net.zaczek.webtts.mpr;

import java.security.InvalidParameterException;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.view.KeyEvent;

//http://stackoverflow.com/questions/5584605/is-it-possible-to-react-to-headphone-volume-up-down-keys-in-an-android-applicati
// http://developer.android.com/training/managing-audio/volume-playback.html
public class MediaPlayerReceiver extends BroadcastReceiver {
	private static final String TAG = "mpr";
	private final RemoteControlListener listener;

	public MediaPlayerReceiver(RemoteControlListener listener) {
		super();
		if(listener == null) throw new InvalidParameterException("listener must not be null");
		
		this.listener = listener;
	}
	
	public void registerReceiver(Context ctx) {
		IntentFilter mediaFilter = new IntentFilter(Intent.ACTION_MEDIA_BUTTON);
		mediaFilter.setPriority(9999);
		ctx.registerReceiver(this, mediaFilter);
	}
	
	public void unregisterReceiver(Context ctx) {
		ctx.unregisterReceiver(this);
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(TAG, "MPR called");
		final String intentAction = intent.getAction();
		if (!Intent.ACTION_MEDIA_BUTTON.equals(intentAction)) {
			Log.d(TAG, "Not ACTION_MEDIA_BUTTON, intent was " + intentAction);
			return;
		}
		final KeyEvent event = (KeyEvent) intent
				.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
		if (event == null) {
			Log.d(TAG, "No event");
			return;
		}
		try {
			final int action = event.getAction();
			final int keyCode = event.getKeyCode();
			Log.d(TAG, "Event.action = " + action + "; key = " + keyCode);
			if (action == KeyEvent.ACTION_DOWN) {
				switch (keyCode) {
				case KeyEvent.KEYCODE_MEDIA_PLAY:
					listener.onMediaPlay();
					break;
				case KeyEvent.KEYCODE_MEDIA_NEXT:
					listener.onMediaNext();
					break;
				case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
					listener.onMediaPrev();
					break;
				}
			}
		} catch (Exception e) {
			Log.e(TAG, "Error in BroadcastReceiver", e);
		}
		abortBroadcast();
	}
}
