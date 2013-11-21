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

import org.jsoup.Jsoup;

import net.zaczek.webtts.ArticleRef;
import net.zaczek.webtts.WebSiteRef;

import android.os.Environment;
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
		final ArrayList<WebSiteRef> result = new ArrayList<WebSiteRef>();
		final FileReader sr = openRead("websites.csv");
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
				result.add(url);
			}
		} finally {
			sr.close();
		}
		return result;
	}

	public static void downloadWebSites() throws IOException {
		final StringBuffer urls = downloadText(new URL(
				"https://docs.google.com/spreadsheet/pub?key=0Au6e93kxiTMhdGdUVmZvdEdZcHdvaVBZUlp0WFpYU2c&single=true&gid=0&output=csv"));
		final OutputStreamWriter sw = openWrite("websites.csv", false);
		try {
			sw.write(urls.toString());
		} finally {
			sw.flush();
			sw.close();
		}
	}

	private static ArrayList<ArticleRef> globalArticles;

	public static ArrayList<ArticleRef> getCurrentArticles() {
		return globalArticles;
	}

	public static void setCurrentArticles(ArrayList<ArticleRef> value) {
		globalArticles = value;
	}
}
