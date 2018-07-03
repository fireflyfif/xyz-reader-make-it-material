package com.example.xyzreader.ui;

// COMPLETED: Add the support library for all Fragment and Loader's classes for backward compatibility
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.SharedElementCallback;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.support.v7.widget.Toolbar;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;

import java.util.List;
import java.util.Map;

/**
 * An activity representing a single Article detail screen, letting you swipe between articles.
 */
public class ArticleDetailActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = ArticleDetailActivity.class.getSimpleName();

    private Cursor mCursor;
    private long mStartId;

    private long mSelectedItemId;
    private int mSelectedItemUpButtonFloor = Integer.MAX_VALUE;

    private ViewPager mPager;
    private MyPagerAdapter mPagerAdapter;
    private Toolbar mToolbar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }

        // COMPLETED: First postpone the enter transition, then set it
        ActivityCompat.postponeEnterTransition(this);

        setContentView(R.layout.activity_article_detail);

        getSupportLoaderManager().initLoader(0, null, this);

        // COMPLETED: Set the Toolbar first
        mToolbar = findViewById(R.id.toolbar_detail_activity);
        setSupportActionBar(mToolbar);

        // COMPLETED: Set the Up Button correctly
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            // Hide the title on the ActionBar
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // COMPLETED: Set the View Pager
        setViewPager();

        if (savedInstanceState == null) {
            if (getIntent() != null && getIntent().getData() != null) {
                mStartId = ItemsContract.Items.getItemId(getIntent().getData());
                mSelectedItemId = mStartId;
            }

            // COMPLETED: Avoid a postponeEnterTransition on orientation change
            ActivityCompat.postponeEnterTransition(this);
        }
    }

    /*
    COMPLETED: Method for setting the View Pager
     */
    private void setViewPager() {

        mPagerAdapter = new MyPagerAdapter(getSupportFragmentManager());
        mPager = findViewById(R.id.pager);
        mPager.setAdapter(mPagerAdapter);
        mPager.setPageMargin((int) TypedValue
                .applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics()));
        mPager.setPageMarginDrawable(new ColorDrawable(0x22000000));

        // COMPLETED: Remove the setOnPageChangeListener() which is now deprecated and set addOnPageChangeListener()
        mPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                // COMPLETED: Set the selectedIndex to the current position that is saved from the first Activity
                ArticleListActivity.currentPosition = position;

                if (mCursor != null) {
                    mCursor.moveToPosition(position);
                }
                mSelectedItemId = mCursor.getLong(ArticleLoader.Query._ID);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }


    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mCursor = cursor;
        mPagerAdapter.notifyDataSetChanged();

        // Select the start ID
        if (mStartId > 0) {
            mCursor.moveToFirst();
            // TODO: optimize
            while (!mCursor.isAfterLast()) {
                if (mCursor.getLong(ArticleLoader.Query._ID) == mStartId) {
                    final int position = mCursor.getPosition();
                    mPager.setCurrentItem(position, false);
                    break;
                }
                mCursor.moveToNext();
            }
            mStartId = 0;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
        mPagerAdapter.notifyDataSetChanged();
    }

    /**
     * ViewPager Adapter Class
     */
    private class MyPagerAdapter extends FragmentStatePagerAdapter {

        private MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            super.setPrimaryItem(container, position, object);
            ArticleDetailFragment fragment = (ArticleDetailFragment) object;
            if (fragment != null) {
                mSelectedItemUpButtonFloor = fragment.getUpButtonFloor();
            }
        }

        @Override
        public Fragment getItem(int position) {
            mCursor.moveToPosition(position);
            long cursorId = mCursor.getLong(ArticleLoader.Query._ID);
            String articleName = mCursor.getString(ArticleLoader.Query.TITLE);

            if (mStartId == cursorId) {
                return ArticleDetailFragment.newInstance(cursorId, articleName);
            } else {
                return ArticleDetailFragment.newInstance(cursorId, "");
            }
        }

        @Override
        public int getCount() {
            return (mCursor != null) ? mCursor.getCount() : 0;
        }

    }

    /**
     * TODO: Remove if not in use
     * Method for getting the Shared Element ready for Transition
     * Currently not in use, because I decided to go with another approach
     * source: https://android-developers.googleblog.com/2018/02/continuous-shared-element-transitions.html
     */
    private void prepareSharedElementTransition() {
        Transition transition = TransitionInflater.from(this).inflateTransition(
                R.transition.image_shared_transition);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setSharedElementEnterTransition(transition);
        }

        setEnterSharedElementCallback(
                new SharedElementCallback() {
                    @Override
                    public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
                        Fragment currentFragment = (Fragment) mPager.getAdapter()
                                .instantiateItem(mPager, ArticleListActivity.currentPosition);

                        View view = currentFragment.getView();
                        if (view == null) {
                            return;
                        }

                        sharedElements.put(names.get(0),
                                view.findViewById(R.id.photo));
                    }
                }
        );
    }

    // COMPLETED: Finish the transition
    @Override
    public void finishAfterTransition() {
        setResult(RESULT_OK);
        super.finishAfterTransition();
    }

}
