package com.held.gcm;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.held.activity.ChatActivity;
import com.held.activity.FeedActivity;
import com.held.activity.NotificationActivity;
import com.held.activity.R;
import com.held.retrofit.HeldService;
import com.held.retrofit.response.PostResponse;
import com.held.utils.AppConstants;
import com.held.utils.HeldApplication;
import com.held.utils.PreferenceHelper;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class GcmIntentService extends IntentService {


    private final String TAG = GcmIntentService.class.getSimpleName();
    private AudioManager am;
    private LocalBroadcastManager localBroadcastManager;
    private String sourceFileName;

    public GcmIntentService() {
        super("GcmIntentService");
        localBroadcastManager = LocalBroadcastManager.getInstance(HeldApplication.getAppContext());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        String messageType = gcm.getMessageType(intent);

        if (!extras.isEmpty()) {  // has effect of unparcelling Bundle
            if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
                Log.d(TAG, "Send error : " + extras.toString());

            } else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
                Log.d(TAG, "Deleted messages on server: " + extras.toString());

            } else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
                checkNotificationType(extras);
                Log.i(TAG, "Received: " + extras.toString());
            }
        } else {
            Log.i(TAG, "Received Empty push notification message");
        }
        // Release the wake lock provided by the WakefulBroadcastReceiver.
        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    private void checkNotificationType(Bundle bundleResponse) {

//        Intent intent = new Intent(this, SplashActivity.class);
//        sendNotification(intent);

        //Coming Notification format
        // "GCM": "{\"data\":{\"title\":\"push notification\",\"message\":\"You have an Event coming up!
        // Don't forget to check your schedule!\",\"notificationId\":2,\"vibrate\":1,\"sound\":1}}"
//
        String type = bundleResponse.getString("type");

        Log.i(TAG, " type: " + type);
        String message = bundleResponse.getString("gcm.notification.body");
        String title = bundleResponse.getString("gcm.notification.title");
        Log.i(TAG, " title: " + title);

        int gameId, oppToken;

        switch (type) {
            case "friend:request":
                Intent intent = new Intent(this, NotificationActivity.class);
                intent.putExtra("id", 0);
                sendNotification(intent, title, message);
                break;
            case "friend:approve":
                intent = new Intent(this, ChatActivity.class);
                sendNotification(intent, title, message);
                break;
            case "friend:message":
                String entity = bundleResponse.getString("entity");
                String str[] = message.split(":");
                if (HeldApplication.IS_CHAT_FOREGROUND) {
                    intent = new Intent("CHAT");
                    intent.putExtra("id", str[0]);
                    intent.putExtra("isOneToOne", true);
                    localBroadcastManager.sendBroadcast(intent);

                } else {
                    intent = new Intent(this, ChatActivity.class);
                    intent.putExtra("id", str[0]);
                    intent.putExtra("isOneToOne", true);
                    sendNotification(intent, title, message);
                }
                break;
            case "post:held":
                intent = new Intent(this, FeedActivity.class);
                sendNotification(intent, title, message);
                break;
            case "post:download_request":
                intent = new Intent(this, NotificationActivity.class);
                intent.putExtra("id", 1);
                sendNotification(intent, title, message);
                break;
            case "post:message":
                entity = bundleResponse.getString("entity");
                if (HeldApplication.IS_CHAT_FOREGROUND) {
                    intent = new Intent("CHAT");
                    intent.putExtra("id", entity);
                    intent.putExtra("isOneToOne", false);
                    localBroadcastManager.sendBroadcast(intent);
                }
//                else if (HeldApplication.IS_APP_FOREGROUND) {
//                    intent = new Intent(this, ChatActivity.class);
//                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                    intent.putExtra("id", entity);
//                    intent.putExtra("isOneToOne", false);
//                    startActivity(intent);
//                }
                else {
                    intent = new Intent(this, ChatActivity.class);
                    intent.putExtra("id", entity);
                    intent.putExtra("isOneToOne", false);
                    sendNotification(intent, title, message);
                }
                break;
            case "post:download_approve":
                entity = bundleResponse.getString("entity");
                callPostSearchApi(entity);
                intent = new Intent(this, NotificationActivity.class);
                sendNotification(intent, title, message);
                break;
        }

    }

    private void callPostSearchApi(String postId) {
        HeldService.getService().postSearch(PreferenceHelper.getInstance(HeldApplication.getAppContext())
                .readPreference(getString(R.string.API_session_token)), postId, new Callback<PostResponse>() {
            @Override
            public void success(PostResponse postResponse, Response response) {
                Target target = new Target() {
                    @Override
                    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                        File cameraFolder;

                        cameraFolder = new File(Environment
                                .getExternalStorageDirectory(), "/HELD");
                        if (!cameraFolder.exists())
                            cameraFolder.mkdirs();

                        sourceFileName = "/IMG_"
                                + System.currentTimeMillis() + ".jpg";

                        File photo = new File(cameraFolder, sourceFileName);

                        FileOutputStream out = null;
                        try {
                            out = new FileOutputStream(photo);
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out); // bmp is your Bitmap instance
                            // PNG is a lossless format, the compression factor (100) is ignored
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            try {
                                if (out != null) {
                                    out.close();
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    @Override
                    public void onBitmapFailed(Drawable errorDrawable) {

                    }

                    @Override
                    public void onPrepareLoad(Drawable placeHolderDrawable) {

                    }
                };
                Picasso.with(HeldApplication.getAppContext()).load(AppConstants.BASE_URL + postResponse.getImage()).into(target);
            }

            @Override
            public void failure(RetrofitError error) {

            }
        });
    }

    protected void sendNotification(Intent intent, String title, String message) {

        NotificationManager mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder mBuilder = (NotificationCompat.Builder) new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message);
        mBuilder.setAutoCancel(true);
        mBuilder.setContentIntent(contentIntent);

        mNotificationManager.notify(GCMConstants.NotificationID.getID(), mBuilder.build());
    }

    /*public void setVibrate(NotificationCompat.Builder mBuilder) {
        if (am != null) {
            switch (am.getRingerMode()) {
                case AudioManager.RINGER_MODE_SILENT:
                    mBuilder.setVibrate(new long[]{1000, 1000, 1000, 1000, 1000});
                    break;
            }
        }
    }*/
}