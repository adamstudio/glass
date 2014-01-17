
package com.cognizant.gtoglass.activity;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;

import com.google.android.glass.app.Card;
import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;

import java.util.ArrayList;
import java.util.Arrays;

public class ScreenSlideActivity extends Activity implements GestureDetector.BaseListener {

    public static final String EXTRA_SELECTED_VALUE = "selected_value";
    private GestureDetector mDetector;
    private static final String LOG_TAG = "GTOGlass";

    private ArrayList<Card> mlcCards = new ArrayList<Card>();
    private ArrayList<String> mlsText = new ArrayList<String>(Arrays.asList(TargetFinderActivity.TARGET_NAMES));
    private CardScrollView csvCardsView;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        for (int i = 0; i < mlsText.size(); i++)
        {
            Card newCard = new Card(this);
          //  newCard.setFullScreenImages(true);
            newCard.setText(mlsText.get(i));
            mlcCards.add(newCard);
        }

        csvCardsView = new CardScrollView(this) {
            @Override
            public final boolean dispatchGenericFocusedEvent(MotionEvent event) {
                if (mDetector.onMotionEvent(event)) {
                    return true;
                }
                return super.dispatchGenericFocusedEvent(event);
            }
        };
        csaAdapter cvAdapter = new csaAdapter();
        csvCardsView.setAdapter(cvAdapter);
        csvCardsView.activate();
        setContentView(csvCardsView);
        mDetector = new GestureDetector(this).setBaseListener(this);
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        return mDetector.onMotionEvent(event);
    }

    @Override
    public boolean onGesture(Gesture gesture) {
        if (gesture == Gesture.TAP) {
            Intent intent = new Intent(this, TargetFinderActivity.class);
            intent.putExtra(TargetFinderActivity.TARGET_INDEX_EXTRA, csvCardsView.getSelectedItemPosition());
            startActivity(intent);
            return true;
        }
        return false;
    }

    private class csaAdapter extends CardScrollAdapter
    {
        @Override
        public int findIdPosition(Object id)
        {
            return -1;
        }

        @Override
        public int findItemPosition(Object item)
        {
            return mlcCards.indexOf(item);
        }

        @Override
        public int getCount()
        {
            return mlcCards.size();
        }

        @Override
        public Object getItem(int position)
        {
            return mlcCards.get(position);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            return mlcCards.get(position).toView();
        }
    }
}
