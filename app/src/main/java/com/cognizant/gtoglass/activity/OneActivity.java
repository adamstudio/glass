package com.cognizant.gtoglass.activity;

import android.os.Bundle;
import android.util.Log;

import com.cognizant.gtoglass.model.Target;

/**
 * Created by devarajns on 10/01/14.
 */
public class OneActivity extends TargetFinderActivity {

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        Log.i(LOG_TAG, "onCreate");
        mTargetListIndex =1;

        super.onCreate(savedInstanceState);
    }
}

