package com.example.xyzreader.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.ui.ArticleDetailActivity;
import com.example.xyzreader.ui.ArticleListActivity;
import com.squareup.picasso.Picasso;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

// COMPLETED: Created a separate Adapter class so that the code be decoupled
// Help from this tutorial: http://innodroid.com/blog/post/a-complex-activity-transition-part-2
public class ArticlesAdapter extends RecyclerView.Adapter<ArticlesAdapter.ArticlesViewHolder> {

    private static final String TAG = ArticlesAdapter.class.getSimpleName();

    public static final String EXTRA_ARTICLE_ITEM = "article_image_url";
    public static final String EXTRA_ARTICLE_IMAGE_TRANSITION = "article_transition_name";

    public SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    public SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    public GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2,1,1);

    private Cursor mCursor;
    private Context mContext;


    public ArticlesAdapter(Context context, Cursor cursor) {
        mContext = context;
        mCursor = cursor;
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
    public void onBindViewHolder(@NonNull  ArticlesViewHolder holder, int position) {

        mCursor.moveToPosition(position);
        holder.titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));

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
                .into(holder.thumbnailView);

        Log.d(TAG, "Image url: " + mCursor.getString(ArticleLoader.Query.THUMB_URL));
    }


    @Override
    public int getItemCount() {
        return mCursor.getCount();
    }

    // COMPLETED: Moved the Intent to this method
    private void showViewPagerActivity(View view, int position) {
        Uri itemIdUri = ItemsContract.Items.buildItemUri(getItemId(position));

        Intent intent = new Intent(Intent.ACTION_VIEW, itemIdUri);
        intent.putExtra(EXTRA_ARTICLE_ITEM, position);

        Activity activity = (Activity) mContext;
        ActivityOptionsCompat options = ActivityOptionsCompat
                .makeSceneTransitionAnimation(activity, view, EXTRA_ARTICLE_ITEM);
        ActivityCompat.startActivity(activity, intent, options.toBundle());
    }

    public class ArticlesViewHolder extends RecyclerView.ViewHolder {

        private ImageView thumbnailView;
        private TextView titleView;
        private TextView subtitleView;

        private ArticlesViewHolder(View itemView) {
            super(itemView);

            thumbnailView = itemView.findViewById(R.id.thumbnail);
            titleView = itemView.findViewById(R.id.article_title);
            subtitleView = itemView.findViewById(R.id.article_subtitle);

            itemView.setOnClickListener(ClickItemListener);
        }

        private View.OnClickListener ClickItemListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showViewPagerActivity(v, getLayoutPosition());
            }
        };
    }
}
