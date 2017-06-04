package com.example.goranminov.popmovies;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
    /*
     * I have followed the examples from the Sunshine app that was provided during my Nanodegree course.
     * I have followed the AsyncTask class the documentation on android.developer Website
     * as well as the AsyncTask class from the Sunshine app.
     */
public class MainActivity extends AppCompatActivity implements MovieAdapter.MovieAdapterOnClickHandler {

    private RecyclerView mRecyclerView;
    private MovieAdapter mMovieAdapter;
    private TextView mErrorMessage;
    private ProgressBar mLoadingData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*
         * We get the reference to our RecyclerView so we can later attach the adapter.
         */
        mRecyclerView = (RecyclerView) findViewById(R.id.movies_data_recycler_view);

        /*
         * We get the reference to our Error Message TextView so we can display the
         * error message in case there is a problem to retrieve the data from
         * the Movie Database website
         */
        mErrorMessage = (TextView) findViewById(R.id.error_message);

        /*
         * We get the reference to our ProgressBar so we will indicate to the user that we are
          * loading the data.
         */
        mLoadingData = (ProgressBar) findViewById(R.id.loading_data_progress_bar);

        /*
         * We attach GridLayoutManager to our RecyclerView as we need to display our results
         * in Grid style.
         */
        GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setHasFixedSize(true);

        /*
         * The MovieAdapter is responsible to attach our data and display it.
         */
        mMovieAdapter = new MovieAdapter(this);
        mRecyclerView.setAdapter(mMovieAdapter);

        /*
         * Call our loadPopularMovies method.
         */
        loadPopularMovies();

    }

    /*
     * Method used to get the movies data. The preffered sorting is set to be popular.
     */
    private void loadPopularMovies() {
        showMovieData();
        new GetMovieData().execute("popular");
    }

    /*
     * Method used to get the movies data with the top_rated sorting.
     */
    private void loadTopRatedMovies() {
        showMovieData();
        new GetMovieData().execute("top_rated");
    }

    /*
     * Method used to set the error message as invisible and the RecyclerView
     * as visible.
     */
    private void showMovieData() {
        mErrorMessage.setVisibility(View.INVISIBLE);
        mRecyclerView.setVisibility(View.VISIBLE);
    }

    /*
     * Method used to set the error message as visible and the RecyclerView
     * as invisible.
     */
    private void showErrorData() {
        mErrorMessage.setVisibility(View.VISIBLE);
        mRecyclerView.setVisibility(View.INVISIBLE);
    }

    /**
     * Override the onClick method so we will be able to handle the RecyclerView item clicks.
     *
     * @param selectedMovie The information for the movie that was clicked.
     */
    @Override
    public void onClick(String selectedMovie) {
        Intent intent = new Intent(this, DetailActivity.class);
        intent.putExtra(Intent.EXTRA_TEXT, selectedMovie);
        startActivity(intent);
    }

    /*
     * Class used to perform network requests, extends AsyncTask
     */
    public class GetMovieData extends AsyncTask<String, Void, String[]> {

        /**
         * Take the String representing the complete details in JSON format and
         * pull out the data we need to construct the Strings needed.
         *
         * @param stringMovies String representing the complete details in JSON format.
         *
         * @return Strings needed for the wireframes.
         */
        private String[] getMovieDataFromJson(String stringMovies) throws JSONException {

            /*
             * The names of the JSON objects that we need to extract.
             */
            final String MDB_RESULTS = "results";
            final String MDB_ORIGINAL_TITLE = "original_title";
            final String MDB_POSTER_PATH = "poster_path";
            final String MDB_OVERVIEW = "overview";
            final String MDB_VOTE_AVERAGE = "vote_average";
            final String MDB_RELEASE_DATE = "release_date";

            /*
             * Object from the returned string and use that Object to create an Array from
             * the parent MDB_RESULTS.
             */
            JSONObject moviesResultJsonObject = new JSONObject(stringMovies);
            JSONArray moviesResultJsonArray = moviesResultJsonObject.getJSONArray(MDB_RESULTS);

            /*
             * The String Array to be returned.
             */
            String[] resultString = new String[20];

            for (int i = 0; i < moviesResultJsonArray.length(); i++) {
                String originalTitle;
                String posterPath;
                String overview;
                String voteAverage;
                String releaseDate;

                /*
                 * Get the JSONObject.
                 */
                JSONObject moviesResults = moviesResultJsonArray.getJSONObject(i);
                originalTitle = moviesResults.getString(MDB_ORIGINAL_TITLE);
                posterPath = moviesResults.getString(MDB_POSTER_PATH);
                overview = moviesResults.getString(MDB_OVERVIEW);
                voteAverage = moviesResults.getString(MDB_VOTE_AVERAGE);
                releaseDate = moviesResults.getString(MDB_RELEASE_DATE);

                /* This is implemented this way so that will be easy when we will need
                 * to extract the data later.
                 */
                resultString[i] = posterPath + "!" + originalTitle + "@"
                        + overview + "#" + voteAverage +
                        "£" + releaseDate;
            }
            return resultString;
        }

        /*
         * Set the ProgressBar to be visible to indicate the user that we are
         * loading the data.
         */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mLoadingData.setVisibility(View.VISIBLE);
        }

        @Override
        protected String[] doInBackground(String... params) {


             // If there is no params there is nothing to look up.
            if (params.length == 0) {
                return null;
            }

            /*
             * These two need to be declared outside the try/catch
             * so that they can be closed in the finally block.
             */
            HttpURLConnection urlConnection = null;
            BufferedReader bufferedReader = null;

            // Will contain the raw JSON response as a string.
            String moviesJsonString = null;

            try {

                /*
                 * Construct the URL for the TheMovieDB query.
                 * Possible parameters are available at TMDB's API page.
                 */
                final String MDB_BASE_URL = "http://api.themoviedb.org/3/movie/";
                final String APPID_PARAM = "api_key";
                Uri builtUri = Uri.parse(MDB_BASE_URL).buildUpon()
                        .appendPath(params[0])
                        .appendQueryParameter(APPID_PARAM, BuildConfig.MOVIE_DATABASE_API_KEY)
                        .build();

                URL url = new URL(builtUri.toString());

                // Create the request to theMovieDB, and open the connection.
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String.
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer stringBuffer = new StringBuffer();

                if (inputStream == null) {

                    // Nothing to do.
                    return null;
                }

                bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    stringBuffer.append(line + "\n");
                }
                if (stringBuffer.length() == 0) {

                    // Stream was empty.  No point in parsing.
                    return null;
                }

                moviesJsonString = stringBuffer.toString();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();

                /* If the code didn't successfully get the data, there's no point in attemping
                 * to parse it.
                 */
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            try {
                return getMovieDataFromJson(moviesJsonString);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String[] strings) {
            /*
             * Set the ProgressBar to invisible and pass the data to the Adapter.
             */
            mLoadingData.setVisibility(View.INVISIBLE);
            if (strings != null) {
                showMovieData();
                mMovieAdapter.setMovieData(strings);
            } else {
                showErrorData();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        /*
         * We inflate our menu layout to this menu and display it in the Toolbar.
         */
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        /*
         * Clear the data in the Adapter and load it again.
         */
        if (id == R.id.action_popular) {
            mMovieAdapter.setMovieData(null);
            loadPopularMovies();
            return true;
        }

        /*
         * Clear the data in the Adapter and load it again.
         */
        if (id == R.id.action_top_rated) {
            mMovieAdapter.setMovieData(null);
            loadTopRatedMovies();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
