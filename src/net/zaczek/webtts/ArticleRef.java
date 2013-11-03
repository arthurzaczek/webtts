package net.zaczek.webtts;

import android.os.Parcel;
import android.os.Parcelable;

public class ArticleRef implements Parcelable {
	public ArticleRef() {
	}
	
	public ArticleRef(String url, String text) {
		this.url = url;
		this.text = text;
	}

	public String url;
	public String text;

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
	}
	
    public static final Parcelable.Creator<ArticleRef> CREATOR = new Parcelable.Creator<ArticleRef>() {
        public ArticleRef createFromParcel(Parcel in) {
            return new ArticleRef(in);
        }

        public ArticleRef[] newArray(int size) {
            return new ArticleRef[size];
        }
    };

    // example constructor that takes a Parcel and gives you an object populated with it's values
    private ArticleRef(Parcel in) {
        url = in.readString();
        text = in.readString();
    }
}
