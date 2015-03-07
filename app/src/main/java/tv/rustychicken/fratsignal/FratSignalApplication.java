package tv.rustychicken.fratsignal;


import android.app.Application;

import com.parse.Parse;
import com.parse.ParseFacebookUtils;
import com.parse.ParseTwitterUtils;

public class FratSignalApplication extends Application {
    public void onCreate() {
        Parse.initialize(this, getString(R.string.parse_app_id), getString(R.string.parse_client_key));
        ParseTwitterUtils.initialize(getString(R.string.twitter_key), getString(R.string.twitter_secret));
        ParseFacebookUtils.initialize(getString(R.string.facebook_app_id));
    }
}
