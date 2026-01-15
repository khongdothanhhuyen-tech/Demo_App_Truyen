package com.example.app_truyen;

import android.app.Application;
import com.cloudinary.android.MediaManager;
import java.util.HashMap;
import java.util.Map;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", "dud8btify");
        config.put("api_key", "842544571358461");
        config.put("api_secret", "pazrZ0Xy4BP0kea-EC7xJaN6Wvg");
        MediaManager.init(this, config);
    }
}
