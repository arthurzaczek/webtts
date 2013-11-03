package net.zaczek.webtts;

import android.os.Parcel;
import android.os.Parcelable;

public class WebSiteRef implements Parcelable {
	public WebSiteRef() {
	}
	
	public WebSiteRef(String text) {
		this.text = text;
	}

	public String url;
	public String text;
	public String link_selector;
	public String article_selector;
	public String readmore_selector;

	@Override
	public String toString() {
		return text;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(url);
		dest.writeString(text);
		dest.writeString(link_selector);
		dest.writeString(article_selector);
		dest.writeString(readmore_selector);
	}
	
    public static final Parcelable.Creator<WebSiteRef> CREATOR = new Parcelable.Creator<WebSiteRef>() {
        public WebSiteRef createFromParcel(Parcel in) {
            return new WebSiteRef(in);
        }

        public WebSiteRef[] newArray(int size) {
            return new WebSiteRef[size];
        }
    };

    // example constructor that takes a Parcel and gives you an object populated with it's values
    private WebSiteRef(Parcel in) {
        url = in.readString();
        text = in.readString();
        link_selector = in.readString();
        article_selector = in.readString();
        readmore_selector = in.readString();
    }
}
