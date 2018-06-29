package com.example.xyzreader.ui;

// COMPLETED: Add the support library for all Fragment and Loader's classes for backward compatibility
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.SharedElementCallback;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.transition.TransitionInflater;
import android.view.View;
import android.view.Window;

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

    // TODO: Save this position on SavedInstanceState
    public static int currentPosition;
    private static final String KEY_CURRENT_POSITION = "current_position";

    private Toolbar mToolbar;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;
    private ArticlesAdapter mArticlesAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // COMPLETED: Set the Exit Shared Element with the callback
        // It should be added before adding content!!!
        //ActivityCompat.setExitSharedElementCallback(this, exitTransitionCallback);
        //ActivityCompat.startPostponedEnterTransition(this);

        if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.LOLLIPOP) {
            prepareExitTransitions();
            //postponeEnterTransition();
        }

        setContentView(R.layout.activity_article_list);

        if (savedInstanceState != null) {
            currentPosition = savedInstanceState.getInt(KEY_CURRENT_POSITION, 0);
            return;
        }


        mToolbar = findViewById(R.id.toolbar);

        final View toolbarContainerView = findViewById(R.id.toolbar_container);

        mSwipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);

        mRecyclerView = findViewById(R.id.recycler_view);

        getSupportLoaderManager().initLoader(0, null, this);

        if (savedInstanceState == null) {
            refresh();
        }


    }

    private void refresh() {
        startService(new Intent(this, UpdaterService.class));
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
        mSwipeRefreshLayout.setRefreshing(mIsRefreshing);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mArticlesAdapter = new ArticlesAdapter(this, cursor);
        mArticlesAdapter.setHasStableIds(true);
        mRecyclerView.setAdapter(mArticlesAdapter);
        int columnCount = getResources().getInteger(R.integer.list_column_count);

        // TODO: Decide if I will use this layout manager
        StaggeredGridLayoutManager sglm =
                new StaggeredGridLayoutManager(columnCount, StaggeredGridLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(sglm);

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mRecyclerView.setAdapter(null);
    }


    /**
     * COMPLETED: Add this shared element for exit transition
     * source: https://android-developers.googleblog.com/2018/02/continuous-shared-element-transitions.html
     */
    private void prepareExitTransitions() {
        if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
            getWindow().setExitTransition(TransitionInflater.from(this)
                    .inflateTransition(R.transition.grid_exit_transition));

            setExitSharedElementCallback(
                    new SharedElementCallback() {
                        @Override
                        public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
                            // TODO: Need to adjust that adapter's current position
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

    // TODO: Remove if I don't need it
    private final SharedElementCallback exitTransitionCallback =
            new SharedElementCallback() {
                @Override
                public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
                    // TODO: Need to adjust that adapter's current position
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
            };

}
