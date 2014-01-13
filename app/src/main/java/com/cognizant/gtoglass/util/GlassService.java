package com.cognizant.gtoglass.util;

import android.app.Service;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.glass.location.GlassLocationManager;
import com.google.glass.timeline.TimelineHelper;
import com.google.glass.timeline.TimelineProvider;
import com.google.glass.util.SettingsSecure;
import com.google.googlex.glass.common.proto.MenuItem;
import com.google.googlex.glass.common.proto.MenuValue;
import com.google.googlex.glass.common.proto.TimelineItem;

import java.util.UUID;
import java.util.concurrent.Executors;

/**
 * The main application service that manages the lifetime of the compass live card and the objects
 * that help out with orientation tracking and landmarks.
 */
public class GlassService extends Service {

    public static final String SERVICE_BROADCAST = "com.andrusiv.glass.bash.RANDOM";
    public static final String SERVICE_CARD = "home_card";
    public static final String SERVICE_COMMAND = "command";
    public static final String COMMAND_GET_RANDOM = "random";

    public static final String TAG = "UB-GS";
    private static final String SERVICE_ITEM_URL = "intent_url";
    private static final String SERVICE_ITEM_TEXT = "intent_text";

    @Override
    public int onStartCommand(Intent intent, int flags, int startid) {
        super.onStartCommand(intent, flags, startid);
        Log.d(TAG, "GlassService onStartCommand() " + intent);

        GlassLocationManager.init(this);
        String homeCardId = PreferenceManager.getDefaultSharedPreferences(this).getString(SERVICE_CARD, null);

        TimelineHelper tlHelper = new TimelineHelper();
        ContentResolver cr = getContentResolver();
        if (homeCardId != null) {
            // find and delete previous home card
            TimelineItem timelineItem = tlHelper.queryTimelineItem(cr,
                    homeCardId);
            if (timelineItem != null && !timelineItem.getIsDeleted())
                tlHelper.deleteTimelineItem(this, timelineItem);
        }

        requestAndUpdateTimeline(cr, tlHelper);

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void requestAndUpdateTimeline(final ContentResolver cr, final TimelineHelper tlHelper) {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                //UkrBashPicture picture = UkrBashPicture.NO_PICTURE;
                try {
                } catch (Exception e) {
                    Log.w(TAG, "Something bad happened when requesting/parsing: " + e);
                }

                // create new home card
                String id = UUID.randomUUID().toString();
                MenuItem delOption = MenuItem.newBuilder()
                        .setAction(MenuItem.Action.DELETE).build();
                MenuItem readAloud = MenuItem.newBuilder()
                        .setAction(MenuItem.Action.READ_ALOUD).build();
                MenuItem share = MenuItem.newBuilder()
                        .setAction(MenuItem.Action.SHARE).build();
                MenuItem customOption = MenuItem.newBuilder()
                        .addValue(MenuValue.newBuilder()
                                .setDisplayName("Random Picture")
                                .build())
                        .setAction(MenuItem.Action.BROADCAST)
                        .setBroadcastAction(SERVICE_BROADCAST).build();

                TimelineItem.Builder builder = tlHelper
                        .createTimelineItemBuilder(GlassService.this, new SettingsSecure(cr));
                TimelineItem item = builder
                        .setId(id)
                        .setHtml(generateHtml())
                        .setText("Description")
                        .setIsPinned(true)
                        .addMenuItem(customOption)
                        .addMenuItem(readAloud)
                        .addMenuItem(share)
                        .addMenuItem(delOption)
                        .build();

                cr.insert(TimelineProvider.TIMELINE_URI,
                        TimelineHelper.toContentValues(item));
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(GlassService.this);
                preferences.edit().putString(SERVICE_CARD, id).commit();
            }
        };
        Executors.newSingleThreadExecutor().execute(r);
    }

    private static String generateHtml() {
        String html = "" +
                "<article class=\"photo\">" +
                "  <img src=\"https://0.gravatar.com/avatar/a350320c1486118d0cc2cc2cbe460136%3Fd%3Didenticons.github.com/61e5542deca08b7fb3d852c14b57738e.png\" width=\"100%\" height=\"100%\" />" +
//                                "  <div class=\"photo-overlay\" />" +
                "  <section>" +
                "    <p class=\"text-auto-size\" style=\"visibility: visible\">picturetext</p>" +
                "  </section>" +
                "</article>";
        return html;
    }
}
