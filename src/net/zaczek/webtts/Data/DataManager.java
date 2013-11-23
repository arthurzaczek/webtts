package net.zaczek.webtts.Data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import org.jsoup.Jsoup;

import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import au.com.bytecode.opencsv.CSVReader;

public class DataManager {
	private static final int TIMEOUT = 20000;
	private static final int BUFFER_SIZE = 8 * 1024;

	public static FileReader openRead(String name) throws IOException {
		File root = Environment.getExternalStorageDirectory();
		File dir = new File(root, "webtts");
		dir.mkdir();
		File file = new File(dir, name);
		if (!file.exists()) {
			file.createNewFile();
		}
		return new FileReader(file);
	}

	public static OutputStreamWriter openWrite(String name, boolean append)
			throws IOException {
		File root = Environment.getExternalStorageDirectory();
		File dir = new File(root, "webtts");
		dir.mkdir();
		File file = new File(dir, name);
		if (!file.exists()) {
			file.createNewFile();
		}
		OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(
				file, append), "UTF-8");
		if (append == false)
			out.write('\ufeff');
		return out;
	}

	public static String readLine(BufferedReader in) throws IOException {
		String line = in.readLine();
		if (line == null)
			return null;
		line = line.trim();
		return line;
	}

	public static StringBuffer downloadText(URL url) throws IOException {
		final URLConnection c = url.openConnection();

		// Setup timeouts
		c.setConnectTimeout(TIMEOUT);
		c.setReadTimeout(TIMEOUT);

		final BufferedReader rd = new BufferedReader(new InputStreamReader(
				c.getInputStream()), BUFFER_SIZE);
		final StringBuffer result = new StringBuffer("");
		for (String line; (line = rd.readLine()) != null;) {
			result.append(line).append("\n");
		}
		rd.close();
		return result;
	}

	public static org.jsoup.Connection jsoupConnect(String url) {
		return Jsoup
				.connect(url)
				.timeout(TIMEOUT)
				.followRedirects(true)
				.header("Connection", "close")
				.userAgent(
						"Mozilla/5.0 (Linux; U; Android 1.5; AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 Mobile Safari/533.1");
	}

	public static ArrayList<WebSiteRef> readWebSites() throws IOException {
		return readWebSitesInternal("websites.csv");
	}

	private static HashMap<String, WebSiteRef> website_configs_cache = null;

	private static HashMap<String, WebSiteRef> ensureWebSitesConfigs()
			throws IOException {
		if (website_configs_cache == null) {
			ArrayList<WebSiteRef> lst = readWebSitesInternal("websites_configs.csv");
			website_configs_cache = new HashMap<String, WebSiteRef>();
			for (WebSiteRef item : lst) {
				website_configs_cache.put(item.uri.getHost(), item);
			}
		}
		return website_configs_cache;
	}

	private static ArrayList<WebSiteRef> readWebSitesInternal(String file)
			throws IOException {
		final ArrayList<WebSiteRef> result = new ArrayList<WebSiteRef>();
		final FileReader sr = openRead(file);
		try {
			final CSVReader reader = new CSVReader(sr);
			String[] line;
			reader.readNext(); // skip first line
			while ((line = reader.readNext()) != null) {
				final WebSiteRef url = new WebSiteRef();
				url.text = line[0];
				url.url = line[1];
				url.link_selector = line[2];
				url.article_selector = line[3];
				if (line.length > 4) {
					url.readmore_selector = line[4];
				}

				url.uri = Uri.parse(url.url.toLowerCase(Locale.getDefault()));

				result.add(url);
			}
		} finally {
			sr.close();
		}
		return result;
	}

	public static void writeWebSites(ArrayList<WebSiteRef> sites)
			throws IOException {
		final OutputStreamWriter sw = openWrite("websites.csv", false);
		try {
			sw.write("name,url,link_selector,article_selector,readmore_selector\n");
			for (WebSiteRef url : sites) {
				sw.write("\"" + url.text + "\"");
				sw.write(",\"" + url.url + "\"");
				sw.write(",\"" + url.link_selector + "\"");
				sw.write(",\"" + url.article_selector + "\"");
				if (!TextUtils.isEmpty(url.readmore_selector)) {
					sw.write(",\"" + url.readmore_selector + "\"");
				}
				sw.write("\n");
			}
		} finally {
			sw.flush();
			sw.close();
		}
	}

	public static void downloadWebSitesSettings() throws IOException {
		final StringBuffer urls = downloadText(new URL(
				"https://docs.google.com/spreadsheet/pub?key=0Au6e93kxiTMhdGdUVmZvdEdZcHdvaVBZUlp0WFpYU2c&single=true&gid=0&output=csv"));
		final OutputStreamWriter sw = openWrite("websites_configs.csv", false);
		try {
			sw.write(urls.toString());
		} finally {
			sw.flush();
			sw.close();
		}
		website_configs_cache = null;
	}

	private static ArrayList<ArticleRef> globalArticles;

	public static ArrayList<ArticleRef> getCurrentArticles() {
		return globalArticles;
	}

	public static void setCurrentArticles(ArrayList<ArticleRef> value) {
		globalArticles = value;
	}

	public static String getLinkSelector(WebSiteRef webSite) {
		if (!TextUtils.isEmpty(webSite.link_selector)) {
			return webSite.link_selector;
		}
		try {
			ensureWebSitesConfigs();
			WebSiteRef cfg = website_configs_cache.get(webSite.uri.getHost());
			if(cfg != null && !TextUtils.isEmpty(cfg.link_selector)) {
				return cfg.link_selector;
			}
		} catch (IOException e) {
			Log.e("WebTTS", "Error reading link selector", e);
		}
		return null;
	}

	public static String getArticleSelector(WebSiteRef webSite) {
		if (!TextUtils.isEmpty(webSite.article_selector)) {
			return webSite.article_selector;
		}
		try {
			ensureWebSitesConfigs();
			WebSiteRef cfg = website_configs_cache.get(webSite.uri.getHost());
			if(cfg != null && !TextUtils.isEmpty(cfg.article_selector)) {
				return cfg.article_selector;
			}
		} catch (IOException e) {
			Log.e("WebTTS", "Error reading article selector", e);
		}
		return null;
	}
}
