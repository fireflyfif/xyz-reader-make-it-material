package com.example.xyzreader.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.ui.ArticleListActivity;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.concurrent.atomic.AtomicBoolean;

// COMPLETED: Created a separate Adapter class so that the code be more readable
// Help from this tutorial: http://innodroid.com/blog/post/a-complex-activity-transition-part-2
public class ArticlesAdapter extends RecyclerView.Adapter<ArticlesAdapter.ArticlesViewHolder> {

    private static final String TAG = ArticlesAdapter.class.getSimpleName();

    private static final String TRANSITION_NAME = "transition";

    private int mMutedColor = 0xFF333333;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2,1,1);

    private Cursor mCursor;
    private Context mContext;
    private int mLastPosition = -1;

    /**
     * COMPLETED: Add a listener that is attached to all ViewHolders to handle image loading events and clicks
     */
    private interface ViewHolderListener {

        void onLoadCompleted(ImageView imageView, int adapterPosition);

        void onItemClicked(View view, int adapterPosition);
    }

    // COMPLETED: Added those too
    private final Picasso mRequestManager;
    private final ViewHolderListener mViewHolderListener;


    public ArticlesAdapter(Context context, Cursor cursor) {
        mContext = context;
        mCursor = cursor;

        // COMPLETED: Added
        mRequestManager = Picasso.get();
        mViewHolderListener = new ViewHolderListenerImplementation((AppCompatActivity) mContext);
    }

    @Override
    public long getItemId(int position) {
        mCursor.moveToPosition(position);
        return mCursor.getLong(ArticleLoader.Query._ID);
    }

    private Date parsePublishedDate() {
        try {
            String date = mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
            return dateFormat.parse(date);
        } catch (ParseException ex) {
            Log.e(TAG, ex.getMessage());
            Log.i(TAG, "passing today's date");
            return new Date();
        }
    }

    @NonNull
    @Override
    public ArticlesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(
                R.layout.list_item_article, parent, false);

        return new ArticlesViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ArticlesViewHolder holder, final int position) {

        mCursor.moveToPosition(position);
        String title = mCursor.getString(ArticleLoader.Query.TITLE);

        holder.titleView.setText(title);

        Date publishedDate = parsePublishedDate();
        if (!publishedDate.before(START_OF_EPOCH.getTime())) {

            holder.subtitleView.setText(Html.fromHtml(
                    DateUtils.getRelativeTimeSpanString(
                            publishedDate.getTime(),
                            System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_ALL).toString()
                            + "<br/>" + " by "
                            + mCursor.getString(ArticleLoader.Query.AUTHOR)));

        } else {
            holder.subtitleView.setText(Html.fromHtml(
                    outputFormat.format(publishedDate)
                            + "<br/>" + " by "
                            + mCursor.getString(ArticleLoader.Query.AUTHOR)));
        }

        // COMPLETED: Add Picasso library for smoothly loading images from internet
        Picasso.get()
                .load(Uri.parse(mCursor.getString(ArticleLoader.Query.THUMB_URL)))
                .placeholder(R.drawable.empty_detail)
                .error(R.drawable.empty_detail)
                .into(holder.thumbnailView, new Callback() {
                    @Override
                    public void onSuccess() {
                       mViewHolderListener.onLoadCompleted(holder.thumbnailView, position);

                       // COMPLETED: Generate background color on each item's card view from the grid
                       Bitmap bitmap = ((BitmapDrawable)
                                holder.thumbnailView.getDrawable()).getBitmap();
                       holder.thumbnailView.setImageBitmap(bitmap);

                       Palette palette = Palette.from(bitmap).generate();
                       int generatedColor = palette.getMutedColor(mMutedColor);

                       holder.cardView.setCardBackgroundColor(generatedColor);

                       // COMPLETED: Set the Item Animator here
                        setItemAnimator(holder.itemView, position);
                    }

                    @Override
                    public void onError(Exception e) {
                        mViewHolderListener.onLoadCompleted(holder.thumbnailView, position);
                    }
                });

        // COMPLETED: Set the Transition name to the title of the article
        ViewCompat.setTransitionName(holder.thumbnailView, title);

        Log.d(TAG, "Transition name: " + title);
        Log.d(TAG, "Image url: " + mCursor.getString(ArticleLoader.Query.THUMB_URL));
    }

    /**
     * COMPLETED: Method for animating the RecyclerView Items
     * resource: https://stackoverflow.com/a/26748274/8132331
     * @param view that is being animated
     * @param position get the position of the item
     */
    private void setItemAnimator(View view, int position) {
        if (position > mLastPosition) {
            Animation animation = AnimationUtils.loadAnimation(mContext,
                    android.R.anim.slide_in_left);
            view.startAnimation(animation);
            mLastPosition = position;
        }
    }

    @Override
    public int getItemCount() {
        return mCursor.getCount();
    }

    // COMPLETED: Moved the Intent to this method
    private void showViewPagerActivity(int position, ImageView thumbnail) {
        Uri itemIdUri = ItemsContract.Items.buildItemUri(getItemId(position));
        String photoUrl = mCursor.getString(ArticleLoader.Query.PHOTO_URL);
        Log.d(TAG, "Photo url: " + photoUrl);

        Intent intent = new Intent(Intent.ACTION_VIEW, itemIdUri);
        intent.putExtra(TRANSITION_NAME, ViewCompat.getTransitionName(thumbnail));

        Activity activity = (Activity) mContext;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ActivityOptionsCompat options = ActivityOptionsCompat
                    .makeSceneTransitionAnimation(
                            activity,
                            thumbnail,
                            ViewCompat.getTransitionName(thumbnail));

            Log.d(TAG, "Transition name: " + ViewCompat.getTransitionName(thumbnail));

            ActivityCompat.startActivity(activity, intent, options.toBundle());
        } else {
            activity.startActivity(intent);
        }
    }

    /**
     * COMPLETED:
     * Clear the animation on detach to avoid problems with fast scrolling
     *
     * @param holder of the current custom Adapter
     */
    @Override
    public void onViewDetachedFromWindow(@NonNull ArticlesViewHolder holder) {
        (holder).clearAnimation();
    }

    /**
     * Default {@Link ViewHolderListener} implementation
     * source: https://github.com/google/android-transition-examples/blob/master/GridToPager/app/src/main/java/com/google/samples/gridtopager/adapter/GridAdapter.java
     */
    private class ViewHolderListenerImplementation implements ViewHolderListener {

        private AppCompatActivity mActivity;
        private AtomicBoolean mEnterTransitionStarted;

        ViewHolderListenerImplementation(AppCompatActivity activity) {
            mActivity = activity;
            mEnterTransitionStarted = new AtomicBoolean();
        }


        @Override
        public void onLoadCompleted(ImageView imageView, int adapterPosition) {
            // Call startPostponedEnterTransition only when the 'selected' image loading is completed.
            if (ArticleListActivity.currentPosition != adapterPosition) {
                return;
            }

            if (mEnterTransitionStarted.getAndSet(true)) {
                return;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mActivity.startPostponedEnterTransition();
            }
        }

        @Override
        public void onItemClicked(View view, int adapterPosition) {
            // Update the position
            ArticleListActivity.currentPosition = adapterPosition;

            ImageView transitioningView = view.findViewById(R.id.thumbnail);

            Log.d(TAG, "Current position at: " + ArticleListActivity.currentPosition);

            // COMPLETED: Call the Intent with the options method
            showViewPagerActivity(adapterPosition, transitioningView);
        }
    }

    public class ArticlesViewHolder extends RecyclerView.ViewHolder implements
            View.OnClickListener {

        private ImageView thumbnailView;
        private TextView titleView;
        private TextView subtitleView;
        private CardView cardView;

        private ViewHolderListener viewHolderListener;

        private ArticlesViewHolder(View itemView) {
            super(itemView);

            thumbnailView = itemView.findViewById(R.id.thumbnail);
            titleView = itemView.findViewById(R.id.article_title);
            subtitleView = itemView.findViewById(R.id.article_subtitle);
            cardView = itemView.findViewById(R.id.card_view);

            viewHolderListener = mViewHolderListener;

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            viewHolderListener.onItemClicked(v, getAdapterPosition());
        }

        public void clearAnimation() {
            itemView.clearAnimation();
        }
    }

    // COMPLETED: Swap the cursor
    public void swapCursor(Cursor cursor) {
        mCursor = cursor;
        notifyDataSetChanged();
    }
}
