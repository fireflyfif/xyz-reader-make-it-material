package com.example.xyzreader.ui;

// COMPLETED: Add the support library for all Fragment and Loader's classes for backward compatibility

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.SharedElementCallback;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import com.example.xyzreader.R;
import com.example.xyzreader.adapters.ArticlesAdapter;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.UpdaterService;

import java.util.List;
import java.util.Map;

/**
 * An activity representing a list of Articles. This activity has different presentations for
 * handset and tablet-size devices. On handsets, the activity presents a list of items, which when
 * touched, lead to a {@link ArticleDetailActivity} representing item details. On tablets, the
 * activity presents a grid of items as cards.
 */
public class ArticleListActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = ArticleListActivity.class.getSimpleName();

    // COMPLETED: Save this position on SavedInstanceState
    // Holds the current item position to be shared between the grid Layout and the Pager Activity
    public static int currentPosition;
    public static final String KEY_CURRENT_POSITION = "current_position";

    private Toolbar mToolbar;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;
    private ArticlesAdapter mArticlesAdapter;
    private CoordinatorLayout mCoordinatorLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_article_list);

        // COMPLETED: Set the Toolbar
        mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        mSwipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        // COMPLETED: Call the refresh listener here
        swipeToRefresh();

        mRecyclerView = findViewById(R.id.recycler_view);
        getSupportLoaderManager().initLoader(0, null, this);

        if (savedInstanceState == null) {
            refresh();
        } else {
            // COMPLETED: Save the current position upon rotation
            currentPosition = savedInstanceState.getInt(KEY_CURRENT_POSITION, 0);
        }

        Log.d(TAG, "Current position clicked: " + currentPosition);
    }

    private void refresh() {
        startService(new Intent(this, UpdaterService.class));
    }

    /**
     * COMPLETED: Set a listener to the Swipe Refresh Layout
     */
    private void swipeToRefresh() {

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(mRefreshingReceiver,
                new IntentFilter(UpdaterService.BROADCAST_ACTION_STATE_CHANGE));
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mRefreshingReceiver);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_CURRENT_POSITION, currentPosition);
    }

    private boolean mIsRefreshing = false;

    private BroadcastReceiver mRefreshingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (UpdaterService.BROADCAST_ACTION_STATE_CHANGE.equals(intent.getAction())) {
                mIsRefreshing = intent.getBooleanExtra(UpdaterService.EXTRA_REFRESHING, false);
                updateRefreshingUI();
            }
        }
    };

    private void updateRefreshingUI() {
        // COMPLETED: Add a Snackbar to show the user a message
        mCoordinatorLayout = findViewById(R.id.coordinator_layout);
        Snackbar.make(mCoordinatorLayout, "Your list is freshly updated!", Snackbar.LENGTH_SHORT).show();

        mSwipeRefreshLayout.setRefreshing(mIsRefreshing);
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> cursorLoader, Cursor cursor) {
        mArticlesAdapter = new ArticlesAdapter(this, cursor);
        mArticlesAdapter.setHasStableIds(true);
        mRecyclerView.setAdapter(mArticlesAdapter);
        int columnCount = getResources().getInteger(R.integer.list_column_count);

        StaggeredGridLayoutManager sglm =
                new StaggeredGridLayoutManager(columnCount, StaggeredGridLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(sglm);

        // COMPLETED: Swap the cursor here
        mArticlesAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        mRecyclerView.setAdapter(null);

        // COMPLETED:
        mArticlesAdapter.swapCursor(null);
    }


    /**
     * TODO: Remove if not in use
     * COMPLETED: Add this shared element for exit transition
     * source: https://android-developers.googleblog.com/2018/02/continuous-shared-element-transitions.html
     * Currently not in use, because I decided to go with another approach
     */
    private void prepareExitTransitions() {

        setExitSharedElementCallback(
                new SharedElementCallback() {
                    @Override
                    public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
                        RecyclerView.ViewHolder selectedViewHolder =
                                mRecyclerView.findViewHolderForAdapterPosition(currentPosition);

                        if (selectedViewHolder == null || selectedViewHolder.itemView == null) {
                            return;
                        }

                        // When transitioning back in, use the thumbnail at index the user had swiped
                        // to in the pager activity
                        sharedElements.put(names.get(0),
                                selectedViewHolder.itemView.findViewById(R.id.thumbnail));
                    }
                }
        );
    }

}
