package com.cognizant.gtoglass.activity;

import android.os.Bundle;
import android.util.Log;

/**
 * Created by devarajns on 10/01/14.
 */
public class OneActivity extends TargetFinderActivity {

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        Log.i(LOG_TAG, "onCreate");
        super.onCreate(savedInstanceState);
        mTargetListIndex =1;
    }
    @Override
    protected void onStop() {
        super.onStop();
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
    }

}

