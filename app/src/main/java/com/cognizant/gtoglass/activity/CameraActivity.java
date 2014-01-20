package com.cognizant.gtoglass.activity;

/**
 * Created by devarajns on 17/01/14.
 */
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.FileObserver;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.cognizant.gtoglass.view.CameraView;
import com.google.android.glass.media.CameraManager;
import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.FileAsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.apache.http.Header;


public class CameraActivity extends Activity
{

    public static final String LOG_TAG = "GTOGlass";
    private static final int TAKE_PICTURE_REQUEST = 1;
    private static final int TAKE_VIDEO_REQUEST = 2;
    private GestureDetector mGestureDetector = null;
    private CameraView cameraView = null;

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Initiate CameraView
        cameraView = new CameraView(this);

        // Turn on Gestures
        mGestureDetector = createGestureDetector(this);

        // Set the view
        this.setContentView(cameraView);
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onResume()
     */
    @Override
    protected void onResume()
    {
        super.onResume();

        // Do not hold the camera during onResume
        if (cameraView != null)
        {
            cameraView.releaseCamera();
        }

        // Set the view
        this.setContentView(cameraView);
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onPause()
     */
    @Override
    protected void onPause()
    {
        super.onPause();

        // Do not hold the camera during onPause
        if (cameraView != null)
        {
            cameraView.releaseCamera();
        }
    }

    private void postImage(File file) {
        Log.i(LOG_TAG, "upload" + file.exists() + " " + file.length());
        RequestParams params = new RequestParams();
        try {
            params.put("file", file);
        } catch (FileNotFoundException e) {
            Log.i(LOG_TAG, "error");
        }
        AsyncHttpClient client = new AsyncHttpClient();
        client.post("http://10.237.77.163:9000/images", params, getResponseHandler());
    }

    protected AsyncHttpResponseHandler getResponseHandler() {
        return new FileAsyncHttpResponseHandler(this) {
            @Override
            public void onStart() {
                Log.i(LOG_TAG, "start");
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, File response) {
                Log.i(LOG_TAG, "success"+response.length());
                FileInputStream inputStream = null;
                try {
                    inputStream = new FileInputStream(response.getAbsolutePath());
                    BufferedReader r = new BufferedReader(new InputStreamReader(inputStream));
                    StringBuilder total = new StringBuilder();
                    String line;
                    try {
                        while ((line = r.readLine()) != null) {
                            total.append(line);
                        }
                        Log.i(LOG_TAG,"total:"+total);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }      finally {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                debugFile(response);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, File file) {
                Log.i(LOG_TAG, "fail" +throwable.getMessage());
                debugFile(file);
            }

            private void debugFile(File file) {
                if (file == null || !file.exists()) {
                    Log.i(LOG_TAG, "Response is null");
                    return;
                }
                try {
                    Log.i(LOG_TAG, file.getAbsolutePath() + "\r\n\r\n");
                } catch (Throwable t) {
                    Log.i(LOG_TAG, "Cannot debug file contents", t);
                }
            }
        };
    }
    /**
     * Gesture detection for fingers on the Glass
     * @param context
     * @return
     */
    private GestureDetector createGestureDetector(Context context)
    {
        GestureDetector gestureDetector = new GestureDetector(context);

        //Create a base listener for generic gestures
        gestureDetector.setBaseListener( new GestureDetector.BaseListener()
        {
            @Override
            public boolean onGesture(Gesture gesture)
            {
                // Make sure view is initiated
                if (cameraView != null)
                {
                    // Tap with a single finger for photo
                    if (gesture == Gesture.TAP)
                    {
                        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        if (intent != null)
                        {
                            startActivityForResult(intent, TAKE_PICTURE_REQUEST);
                        }

                        return true;
                    }
                }

                return false;
            }
        });

        return gestureDetector;
    }

    /*
     * Send generic motion events to the gesture detector
     */
    @Override
    public boolean onGenericMotionEvent(MotionEvent event)
    {
        if (mGestureDetector != null)
        {
            return mGestureDetector.onMotionEvent(event);
        }

        return false;
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        // Handle photos
        if (requestCode == TAKE_PICTURE_REQUEST && resultCode == RESULT_OK)
        {
            String picturePath = data.getStringExtra(CameraManager.EXTRA_PICTURE_FILE_PATH);
           // processPictureWhenReady(picturePath);
            final File pictureFile = new File(picturePath);
            final Handler handler = new Handler();
            Thread th = new Thread() {
                public void run() {
                    if(pictureFile.exists()){
                        Log.i(LOG_TAG, pictureFile.length()+" here "+pictureFile.getAbsolutePath());
                        postImage(pictureFile);
                    handler.removeCallbacks(this);
                    }else{

                        Log.i(LOG_TAG, pictureFile.length()+" not here "+pictureFile.getAbsolutePath());
                    handler.postDelayed(this, 2000);
                    }
                }
            };
            th.start();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Process picture - from example GDK
     * @param picturePath
     */
    private void processPictureWhenReady(final String picturePath)
    {
        final File pictureFile = new File(picturePath);

        if (pictureFile.exists())
        {
            // The picture is ready; process it.
        }
        else
        {
            // The file does not exist yet. Before starting the file observer, you
            // can update your UI to let the user know that the application is
            // waiting for the picture (for example, by displaying the thumbnail
            // image and a progress indicator).

            final File parentDirectory = pictureFile.getParentFile();
            FileObserver observer = new FileObserver(parentDirectory.getPath())
            {
                // Protect against additional pending events after CLOSE_WRITE is
                // handled.
                private boolean isFileWritten;

                @Override
                public void onEvent(int event, String path)
                {
                    if (! isFileWritten)
                    {
                        // For safety, make sure that the file that was created in
                        // the directory is actually the one that we're expecting.
                        File affectedFile = new File(parentDirectory, path);
                        isFileWritten = (event == FileObserver.CLOSE_WRITE && affectedFile.equals(pictureFile));

                        if (isFileWritten)
                        {
                            stopWatching();
                            Log.i(LOG_TAG, pictureFile.length()+" here "+pictureFile.getAbsolutePath());
                            postImage(pictureFile);
                            // Now that the file is ready, recursively call
                            // processPictureWhenReady again (on the UI thread).
                            runOnUiThread(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    processPictureWhenReady(picturePath);
                                }
                            });
                        }
                    }
                }
            };
            observer.startWatching();
        }
    }

    /**
     * Added but irrelevant
     */
        /*
         * (non-Javadoc)
         * @see android.app.Activity#onKeyDown(int, android.view.KeyEvent)
         */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if (keyCode == KeyEvent.KEYCODE_CAMERA)
        {
            // Stop the preview and release the camera.
            // Execute your logic as quickly as possible
            // so the capture happens quickly.
            return false;
        }
        else
        {
            return super.onKeyDown(keyCode, event);
        }
    }
}