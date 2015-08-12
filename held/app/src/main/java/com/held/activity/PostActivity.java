package com.held.activity;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.held.fragment.ChatFragment;
import com.held.fragment.FeedFragment;
import com.held.fragment.NotificationFragment;
import com.held.fragment.PostFragment;
import com.held.fragment.SendFriendRequestFragment;
import com.held.utils.PreferenceHelper;

/**
 * Created by jay on 5/8/15.
 */
public class PostActivity extends ParentActivity {

    public static boolean isPostVisible;
    private Fragment mDisplayFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);

        if (PreferenceHelper.getInstance(getApplicationContext()).readPreference("isFirstPostCreated", false)) {
            launchFeedScreen();
        } else {
            launchCreatePostScreen();
        }
    }

    private void launchFeedScreen() {
        updateToolbar(true, false, true, false, true, true, false, "");
        addFragment(FeedFragment.newInstance(), FeedFragment.TAG, true);
        mDisplayFragment = FeedFragment.newInstance();
    }

    private void launchCreatePostScreen() {
        updateToolbar(false, true, false, false, false, false, true, "");
        replaceFragment(PostFragment.newInstance(), PostFragment.TAG, false);
        mDisplayFragment = PostFragment.newInstance();
    }

    private void launchNotificationScreen() {
        updateToolbar(true, false, true, false, true, true, false, "");
        addFragment(NotificationFragment.newInstance(), NotificationFragment.TAG, true);
        mDisplayFragment = NotificationFragment.newInstance();
    }

    private void launchCreatePostFragmentFromFeed() {
        updateToolbar(false, true, false, false, false, false, true, "");
        addFragment(PostFragment.newInstance(), PostFragment.TAG, true);
        mDisplayFragment = PostFragment.newInstance();
    }

    private void launchChatScreen(String postid) {
        addFragment(ChatFragment.newInstance(postid), ChatFragment.TAG, true);
        mDisplayFragment = PostFragment.newInstance();
    }

    @Override
    public void onBackPressed() {
        if (mDisplayFragment instanceof FeedFragment) {
            finish();
        } else if (mDisplayFragment instanceof PostFragment) {
            updateToolbar(true, false, true, false, true, true, false, "");
            mDisplayFragment = FeedFragment.newInstance();
        } else if (mDisplayFragment instanceof ChatFragment) {
            updateToolbar(true, false, true, false, true, true, false, "");
            mDisplayFragment = FeedFragment.newInstance();
        } else if (mDisplayFragment instanceof NotificationFragment) {
            mDisplayFragment = FeedFragment.newInstance();
        } else if (mDisplayFragment instanceof SendFriendRequestFragment) {
            updateToolbar(true, false, true, false, true, true, false, "");
            mDisplayFragment = FeedFragment.newInstance();
        }
        if (!isPostVisible) {
            super.onBackPressed();
        }
    }

    @Override
    public void perform(int id, Bundle bundle) {
        super.perform(id, bundle);
        switch (id) {
            case 0:
                launchCreatePostScreen();
                break;
            case 1:
                launchFeedScreen();
                break;
            case 2:
                if (bundle != null)
                    launchChatScreen(bundle.getString("postid"));
                break;
            case 3:
                launchCreatePostFragmentFromFeed();
                break;
            case 4:
                launchNotificationScreen();
                break;
            case 5:
                if (bundle != null)
                    launchRequestFriendScreen(bundle.getString("name"), bundle.getString("image"));
                break;

        }
    }

    private void launchRequestFriendScreen(String name, String image) {
        updateToolbar(false, false, false, false, false, false, false, "");
        addFragment(SendFriendRequestFragment.newInstance(name, "http://139.162.1.137/api" + image), SendFriendRequestFragment.TAG, true);
        mDisplayFragment = SendFriendRequestFragment.newInstance(name, "http://139.162.1.137/api" + image);
    }
}
