package net.zaczek.webtts;

import java.io.IOException;
import java.util.ArrayList;

import net.zaczek.webtts.Data.DataManager;
import net.zaczek.webtts.Data.WebSiteRef;

import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import android.support.v4.app.NavUtils;

public class EditWebSiteActivity extends Activity {

	EditText edtName;
	EditText edtUrl;
	EditText edtLinkSelector;
	EditText edtArticleSelector;

	private WebSiteRef webSite;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_edit_web_site);

		edtName = (EditText) findViewById(R.id.edtName);
		edtUrl = (EditText) findViewById(R.id.edtUrl);
		edtLinkSelector = (EditText) findViewById(R.id.edtLinkSelector);
		edtArticleSelector = (EditText) findViewById(R.id.edtArticleSelector);

		Intent intent = getIntent();
		webSite = intent.getParcelableExtra("website");

		if (webSite != null) {
			edtName.setText(webSite.text);
			edtUrl.setText(webSite.url);
			edtLinkSelector.setText(webSite.link_selector);
			edtArticleSelector.setText(webSite.article_selector);
		}

		// Show the Up button in the action bar.
		setupActionBar();
	}

	/**
	 * Set up the {@link android.app.ActionBar}.
	 */
	private void setupActionBar() {
		getActionBar().setDisplayHomeAsUpEnabled(true);
		setTitle(R.string.title_activity_edit_web_site);
	}

	public void onSave(View view) {
		try {
			ArrayList<WebSiteRef> sites = DataManager.readWebSites();
			WebSiteRef local = null;
			if (webSite != null) {
				for (WebSiteRef item : sites) {
					if (TextUtils.equals(item.url, webSite.url)) {
						local = item;
						break;
					}
				}
			} else {
				local = new WebSiteRef();
				sites.add(local);
			}

			local.text = edtName.getText().toString();
			local.url = edtUrl.getText().toString();
			local.link_selector = edtLinkSelector.getText().toString();
			local.article_selector = edtArticleSelector.getText().toString();

			DataManager.writeWebSites(sites);

			NavUtils.navigateUpFromSameTask(this);
		} catch (IOException e) {
			Log.e("WebTTS", "Unable to save", e);
			Toast.makeText(this, "Unable to save", Toast.LENGTH_SHORT).show();
		}
	}

	public void onDelete(View view) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Confirm");
		builder.setMessage("Are you sure?");
		builder.setPositiveButton(android.R.string.yes,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						try {
							ArrayList<WebSiteRef> sites = DataManager
									.readWebSites();
							if (webSite != null) {
								for (WebSiteRef item : sites) {
									if (TextUtils.equals(item.url, webSite.url)) {
										sites.remove(item);
										DataManager.writeWebSites(sites);
										break;
									}
								}
							}
							NavUtils.navigateUpFromSameTask(EditWebSiteActivity.this);
						} catch (IOException e) {
							Log.e("WebTTS", "Unable to delete", e);
							Toast.makeText(EditWebSiteActivity.this,
									"Unable to delete", Toast.LENGTH_SHORT)
									.show();
						}
					}

				});
		builder.setNegativeButton("NO", null);

		AlertDialog alert = builder.create();
		alert.show();

	}

	public void onCancel(View view) {
		NavUtils.navigateUpFromSameTask(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.edit_web_site, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

}
