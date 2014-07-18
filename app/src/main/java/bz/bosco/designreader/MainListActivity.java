package bz.bosco.designreader;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
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


public class MainListActivity extends ListActivity {

    public static final String TAG = MainListActivity.class.getSimpleName();
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

    String[] imageId = {
            "dn_blue",
            "dn_green",
            "dn_red",
            "dn_yellow",
            "dn_icon",
    };


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_list);

        mProgressbar = (ProgressBar) findViewById(R.id.progressBar);


        if (isNetworkAvailable()){
            mProgressbar.setVisibility(View.VISIBLE);
            GetBlogPostTask getBlogPostTask = new GetBlogPostTask();
            getBlogPostTask.execute();
        }
    }


    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        try {
            JSONArray jsonPosts = mBlogData.getJSONArray("stories");
            JSONObject jsonPost = jsonPosts.getJSONObject(position);
            String blogUrl = jsonPost.getString(KEY_URL);

            Intent intent = new Intent(this, BlogWebViewActivity.class);
            intent.setData(Uri.parse(blogUrl));
            startActivity(intent);

        } catch (JSONException e) {
            logException(e);
        }
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
                ArrayList <BlogPost> blogPosts = new ArrayList<BlogPost>();
                for (int i = 0; i < jsonPosts.length();i++){
                    JSONObject post = jsonPosts.getJSONObject(i);
                    String title = post.getString(KEY_TITLE);
                    title = Html.fromHtml(title).toString();
                    String author = post.getString(KEY_AUTHOR);
                    author = Html.fromHtml(author).toString();
                    String badge= post.getString(KEY_BADGE);
                    badge = Html.fromHtml(badge).toString();
                    BlogPost blogPost = new BlogPost();
                    blogPost.setTitle(title);
                    blogPost.setUser_display_name(author);

                    if (post.isNull(KEY_BADGE)) {
                        int randomNumber = randomGenerator.nextInt(len);
                        blogPost.setBadge(imageId[randomNumber]);
                    }else{
                        blogPost.setBadge(badge);
                    }
                    blogPosts.add(blogPost);

                }

                //String[] from = {KEY_TITLE, KEY_AUTHOR};
                //int[] to = {android.R.id.text1, android.R.id.text2};
                ListAdapter adapter = new ListAdapter(this, blogPosts);
                setListAdapter(adapter);
            } catch (JSONException e) {
                logException(e);
            }
        }
    }

    private void logException(Exception e) {
        Log.e(TAG, "Exception caught!", e);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        boolean isAvailable = false;

        if (networkInfo != null && networkInfo.isConnected()) {
            isAvailable = true;
        }else{
            Toast.makeText(this, "Network is unavailable.", Toast.LENGTH_LONG).show();
            isAvailable = true;
        }

        return isAvailable;
    }

    private void updateDisplayForError() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
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
            HttpGet httpget = new HttpGet("https://api-news.layervault.com/api/v1/stories?client_id=dcc2b9a92a4fc65e2a8c09b74f8f15621c8969543f8c334a5d67135cf9fc78a5");

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
