package net.zaczek.webtts;

import android.app.ListActivity;
import android.os.Bundle;

public class AbstractListActivity extends ListActivity  {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
}
