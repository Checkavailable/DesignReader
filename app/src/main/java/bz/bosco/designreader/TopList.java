package bz.bosco.designreader;

import android.app.AlertDialog;
import android.app.ListFragment;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Random;


public class TopList extends ListFragment {

    public static final String TAG = TopList.class.getSimpleName();
    protected JSONObject mBlogData;
    protected ProgressBar mProgressbar;
    private final String KEY_TITLE= "title";
    private final String KEY_AUTHOR= "user_display_name";
    private final String KEY_ID= "id";
    private final String KEY_COMMENT_COUNT= "comment_count";
    private final String KEY_VOTE_COUNT= "vote_count";
    private final String KEY_DATE= "created_at";
    private final String KEY_URL= "url";
    private final String KEY_SITE_URL= "site_url";
    private final String KEY_USER_ID= "user_id";
    private final String KEY_PORTRAIT= "user_portrait_url";
    private final String KEY_USER_JOB= "user_job";
    private final String KEY_USER_URL= "user_url";
    private final String KEY_COMMENTS= "comments";
    private final String KEY_BADGE= "badge";
    private ArrayList <BlogPost> blogPosts = new ArrayList<BlogPost>();
    private SwipeRefreshLayout swipeview;
    private ListAdapter adapter;

    private ListView ls;
    private int currentPage = 1;

    String[] imageId = {
            "dn_blue",
            "dn_green",
            "dn_red",
            "dn_yellow",
            "dn_icon",
    };



    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View contentView  = inflater.inflate(R.layout.activity_list, null);
        mProgressbar = (ProgressBar) contentView.findViewById(R.id.progressBar);
        final Context context = getActivity().getApplicationContext();
        ls = (ListView) contentView.findViewById(android.R.id.list);
        swipeview =  (SwipeRefreshLayout) contentView.findViewById(R.id.swipe_container);
        swipeview.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        swipeview.setRefreshing(false);
                    }
                }, 5000);
            }
        });

        // Attach the listener to the AdapterView onCreate
        ls.setOnScrollListener(new EndlessScrollListener() {
            @Override
            public void onScroll(AbsListView view,int firstVisibleItem,int visibleItemCount,int totalItemCount)
            {
                // If the total item count is zero and the previous isn't, assume the
                // list is invalidated and should be reset back to initial state


                if (totalItemCount < getPreviousTotalItemCount()) {
                    setCurrentPage(getStartingPageIndex());
                    setPreviousTotalItemCount(totalItemCount);
                    if (totalItemCount == 0) { setLoading(true); }
                }

                // If it’s still loading, we check to see if the dataset count has
                // changed, if so we conclude it has finished loading and update the current page
                // number and total item count.
                if (isLoading() && (totalItemCount > getPreviousTotalItemCount())) {
                    setLoading (false);
                    setPreviousTotalItemCount(totalItemCount);
                    incrementCurrentPage();
                }

                // If it isn’t currently loading, we check to see if we have breached
                // the visibleThreshold and need to reload more data.
                // If we do need to reload some more data, we execute onLoadMore to fetch the data.
                if (!isLoading() && (totalItemCount - visibleItemCount)<=(firstVisibleItem + getVisibleThreshold())) {
                    onLoadMore(incrementCurrentPage(), totalItemCount);
                    setLoading(true);
                }



            }
            @Override
            public void onLoadMore(int page, int totalItemsCount) {
                // Triggered only when new data needs to be appended to the list
                // Add whatever code is needed to append new items to your AdapterView
                //Toast.makeText(getActivity(), "Hello", Toast.LENGTH_LONG).show();
                //customLoadMoreDataFromApi(page);
                if (isNetworkAvailable(context)){
                    mProgressbar.setVisibility(View.VISIBLE);
                    GetBlogPostTask getBlogPostTask = new GetBlogPostTask();
                    getBlogPostTask.execute();
                }
                // or customLoadMoreDataFromApi(totalItemsCount);
            }
        });


        if (isNetworkAvailable(context)){
            mProgressbar.setVisibility(View.VISIBLE);
            GetBlogPostTask getBlogPostTask = new GetBlogPostTask();
            getBlogPostTask.execute();
        }

        return contentView;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        try {
            JSONArray jsonPosts = mBlogData.getJSONArray("stories");
            JSONObject jsonPost = jsonPosts.getJSONObject(position);
            String blogUrl = jsonPost.getString(KEY_URL);

            Intent intent = new Intent(getActivity(), BlogWebViewActivity.class);
            intent.setData(Uri.parse(blogUrl));
            startActivity(intent);

        } catch (JSONException e) {
            logException(e);
        }
    }

    public void setSwipeTrue(){
        swipeview.setEnabled(true);
    }

    public void setSwipeFalse(){
        swipeview.setEnabled(false);
    }


    private void handleBlogResponse() {
        Random randomGenerator = new Random();
        int len = imageId.length;
        mProgressbar.setVisibility(View.INVISIBLE);
        if (mBlogData == null){
            updateDisplayForError();
        }
        else{
            try {
                JSONArray jsonPosts = mBlogData.getJSONArray("stories");
                //ArrayList <BlogPost> blogPosts = new ArrayList<BlogPost>();
                for (int i = 0; i < jsonPosts.length();i++){
                    JSONObject post = jsonPosts.getJSONObject(i);
                    String title = post.getString(KEY_TITLE);
                    title = Html.fromHtml(title).toString();
                    String author = post.getString(KEY_AUTHOR);
                    author = Html.fromHtml(author).toString();
                    String badge= post.getString(KEY_BADGE);
                    badge = Html.fromHtml(badge).toString();
                    String commentcount = post.getString(KEY_COMMENT_COUNT);
                    commentcount = Html.fromHtml(commentcount).toString();
                    String votes = post.getString(KEY_VOTE_COUNT);
                    votes = Html.fromHtml(votes).toString();
                    String date = post.getString(KEY_DATE);
                    date = Html.fromHtml(date).toString();
                    String job = post.getString(KEY_USER_JOB);
                    job = Html.fromHtml(job).toString();

                    BlogPost blogPost = new BlogPost();
                    blogPost.setTitle(title);
                    blogPost.setUser_display_name(author);
                    blogPost.setComment_count(commentcount);
                    blogPost.setVote_count(votes);
                    blogPost.setDate(date);
                    blogPost.setUser_job(job);
                    blogPost.setBadge(badge);

                    /**if (post.isNull(KEY_BADGE)) {
                        int randomNumber = randomGenerator.nextInt(len);
                        blogPost.setBadge(imageId[randomNumber]);
                    }else{
                        blogPost.setBadge(badge);
                    }**/
                    blogPosts.add(blogPost);

                }

                //String[] from = {KEY_TITLE, KEY_AUTHOR};
                //int[] to = {android.R.id.text1, android.R.id.text2};

                if(ls.getAdapter()==null){ //Adapter not set yet.
                    adapter = new ListAdapter(getActivity(), blogPosts);
                    setListAdapter(adapter);
                } else{
                    adapter.notifyDataSetChanged();
                };
            } catch (JSONException e) {
                logException(e);
            }
        }
    }

    private void logException(Exception e) {
        Log.e(TAG, "Exception caught!", e);
    }

    private boolean isNetworkAvailable(Context context) {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        boolean isAvailable = false;

        if (networkInfo != null && networkInfo.isConnected()) {
            isAvailable = true;
        }else{
            Toast.makeText(getActivity(), "Network is unavailable.", Toast.LENGTH_LONG).show();
            isAvailable = true;
        }

        return isAvailable;
    }

    private void updateDisplayForError() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.error_title));
        builder.setMessage(getString(R.string.error_message));
        builder.setPositiveButton(android.R.string.ok,null);
        AlertDialog dialog = builder.create();
        dialog.show();

        TextView emptyTextView = (TextView) getListView().getEmptyView();
        emptyTextView.setText(getString(R.string.no_items));
    }

    private class GetBlogPostTask extends AsyncTask<Object, Void, JSONObject>{

        @Override
        protected JSONObject doInBackground(Object... params) {
            int responseCode = -1;
            JSONObject jsonResponse = null;
            StringBuilder builder = new StringBuilder();
            HttpClient client = new DefaultHttpClient();
            HttpGet httpget = new HttpGet("https://api-news.layervault.com/api/v1/stories?client_id=dcc2b9a92a4fc65e2a8c09b74f8f15621c8969543f8c334a5d67135cf9fc78a5&page="+currentPage);
            currentPage++;

            try {
                HttpResponse response = client.execute(httpget);
                StatusLine statusLine = response.getStatusLine();
                responseCode = statusLine.getStatusCode();

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    HttpEntity entity = response.getEntity();
                    InputStream content = entity.getContent();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(content));
                    String line;
                    while((line = reader.readLine()) != null){
                        builder.append(line);
                    }

                    jsonResponse = new JSONObject(builder.toString());
                }
                else {
                    Log.i(TAG, String.format("Unsuccessful HTTP response code: %d", responseCode));
                }
            }
            catch (JSONException e) {
                logException(e);
            }
            catch (Exception e) {
                logException(e);
            }

            return jsonResponse;
        }

        @Override
        protected void onPostExecute(JSONObject result){
            mBlogData = result;
            handleBlogResponse();

        }

    }

}
