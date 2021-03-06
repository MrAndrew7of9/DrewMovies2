package com.example.android.drewmovies2;

import android.annotation.SuppressLint;
import android.support.design.widget.FloatingActionButton;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.Toast;

import com.example.android.drewmovies2.data.FavoriteMoviesContract;
import com.example.android.drewmovies2.models.MovieParcelable;
import com.example.android.drewmovies2.utils.BuildUrlUtils;
import com.example.android.drewmovies2.utils.LoadAndParseUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MovieGridActivity extends AppCompatActivity implements View.OnClickListener {

//    final static private String TAG = MovieGridActivity.class.getSimpleName();
    @BindView(R.id.movie_posters_gv)
    GridView moviesGridView;
    @BindView(R.id.fab_prime)
    FloatingActionButton fabPrime;
    @BindView(R.id.btn_top)
    Button btnTop;
    @BindView(R.id.btn_pop)
    Button btnPop;
    @BindView(R.id.btn_fav)
    Button btnFav;

    private URL movieListUrl;
    private boolean isFav;
    private String title;
    private int gvIndex;

    private Boolean isFabOpen = false;
    private Animation fab_open, fab_close, rotate_forward, rotate_backward;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_grid);
        ButterKnife.bind(this);

//        //set animation resources
        fab_open = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_open);
        fab_close = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.fab_close);
        rotate_forward = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.rotate_forward);
        rotate_backward = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.rotate_backward);
        //set click listeners
        fabPrime.setOnClickListener(this);
        btnFav.setOnClickListener(this);
        btnPop.setOnClickListener(this);
        btnTop.setOnClickListener(this);

        if (savedInstanceState == null) {
            //loads popular movies on first start
            title = getResources().getString(R.string.movie_list_pop_title);
            setTitle(title);
            movieListUrl = BuildUrlUtils.buildMovieListPopRequestUrl();
            isFav = false;
            if (isConnected()) {
                loadVideoList(movieListUrl, isFav);
            } else {
                Toast.makeText(MovieGridActivity.this, "No Internet Connection Detected",
                        Toast.LENGTH_SHORT).show();
                moviesGridView.setAdapter(null);
            }

        }
    }

    //I found a tutorial and used in a previous practice project before starting this course and I
    //couldn't find the link again for this project submission, but I think this functions better for
    //the user than multiple clicks to a shared preference screen and I've significantly changed the
    //implementation to be far from the original source
    @Override
    public void onClick(View v) {
        int id = v.getId();
//        URL movieListUrl;
//        boolean favoriteSelection;
        switch (id){
            case R.id.fab_prime:
                //closes or opens fab once user selects plus icon
                animateFAB();
                break;
            case R.id.btn_pop:
                // sets view to and loads popular movies
                title = getResources().getString(R.string.movie_list_pop_title);
                setTitle(title);
                movieListUrl = BuildUrlUtils.buildMovieListPopRequestUrl();
                isFav = false;
                loadVideoList(movieListUrl, isFav);
                //closes fab once user selects an option
                animateFAB();
                break;
            case R.id.btn_top:
                // sets view to and loads top rated movies
                title = getResources().getString(R.string.movie_list_top_title);
                setTitle(title);
                movieListUrl = BuildUrlUtils.buildMovieListRatedRequestUrl();
                isFav = false;
                loadVideoList(movieListUrl, isFav);
                //closes fab once user selects an option
                animateFAB();
                break;
            case R.id.btn_fav:
                // sets view to and loads user's favorite movies
                title = getResources().getString(R.string.movie_list_fav_title);
                setTitle(title);
                movieListUrl = null;
                isFav = true;
                loadVideoList(movieListUrl, isFav);
                //closes fab once user selects an option
                animateFAB();
                break;
        }
    }

    public void animateFAB(){
    //animates opening/showing of fab button choices
        if(isFabOpen){
            fabPrime.startAnimation(rotate_backward);
            btnTop.startAnimation(fab_close);
            btnPop.startAnimation(fab_close);
            btnFav.startAnimation(fab_close);
            btnTop.setClickable(false);
            btnPop.setClickable(false);
            btnFav.setClickable(false);
            isFabOpen = false;

        } else {
            fabPrime.startAnimation(rotate_forward);
            btnTop.startAnimation(fab_open);
            btnPop.startAnimation(fab_open);
            btnFav.startAnimation(fab_open);
            btnTop.setClickable(true);
            btnPop.setClickable(true);
            btnFav.setClickable(true);
            isFabOpen = true;
        }
    }

    private void loadVideoList(URL movieListUrl, boolean favoriteSelection) {
        //reloads movie list as soon as the user selects a different search/sort option
        if (movieListUrl != null && !favoriteSelection) {
            //only starts async task if url exists and user didn't select to view favorites
            boolean isConnected = isConnected();
            if(isConnected) {
                new MovieDbQueryTask().execute(movieListUrl);
            } else {
                Toast.makeText(MovieGridActivity.this, "No Internet Connection Detected",
                        Toast.LENGTH_SHORT).show();
                moviesGridView.setAdapter(null);
            }
        } else if ( movieListUrl == null && favoriteSelection) {
            //loads movies from db save and recreates the objects to mimic same UI functionality
            final ArrayList<MovieParcelable> favMovies = new ArrayList<>();
            Uri uri = FavoriteMoviesContract.FavoriteEntry.CONTENT_URI;
            Cursor moviesCursor = getContentResolver().query(uri, null, null, null, null);
            //sets movies into new array list from content resolver returned cursor
            try {
                for (int i = 0; i < Objects.requireNonNull(moviesCursor).getCount(); i++) {
                    MovieParcelable movie = new MovieParcelable();
                    // Indices for the _id, description, and priority columns
//                    int idIndex = moviesCursor.getColumnIndex(FavoriteMoviesContract.FavoriteEntry._ID);
                    int movieIdIndex = moviesCursor.getColumnIndex(FavoriteMoviesContract.FavoriteEntry.COLUMN_MOVIE_ID);
                    int titleIndex = moviesCursor.getColumnIndex(FavoriteMoviesContract.FavoriteEntry.COLUMN_TITLE);
                    int imageUrlIndex = moviesCursor.getColumnIndex(FavoriteMoviesContract.FavoriteEntry.COLUMN_IMAGE_URL);
                    int plotIndex = moviesCursor.getColumnIndex(FavoriteMoviesContract.FavoriteEntry.COLUMN_PLOT);
                    int releaseDateIndex = moviesCursor.getColumnIndex(FavoriteMoviesContract.FavoriteEntry.COLUMN_RELEASE_DATE);
                    int ratingIndex = moviesCursor.getColumnIndex(FavoriteMoviesContract.FavoriteEntry.COLUMN_RATING);

                    moviesCursor.moveToPosition(i); // get to the right location in the cursor

                    // Determine the values of the wanted data
                    String title = moviesCursor.getString(titleIndex);
                    Integer movieId = moviesCursor.getInt(movieIdIndex);
                    String movieImagePath = moviesCursor.getString(imageUrlIndex);
                    String plot = moviesCursor.getString(plotIndex);
                    String releaseDate = moviesCursor.getString(releaseDateIndex);
                    Double userRating = moviesCursor.getDouble(ratingIndex);

                    //Set values
                    movie.setMovieTitle(title);
                    movie.setMovieId(movieId);
                    movie.setImageUrlPath(movieImagePath);
                    movie.setAbout(plot);
                    movie.setReleaseDate(releaseDate);
                    movie.setUserRating(userRating);

                    favMovies.add(movie);
                }
            } finally {
                Objects.requireNonNull(moviesCursor).close();
            }
            //same as AsyncTask to replicate UI
            if (favMovies != null) {
                MoviePostersAdapter adapter = new MoviePostersAdapter(getApplicationContext(), favMovies);
                moviesGridView.setAdapter(adapter);
                moviesGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view,
                                            int position, long id) {
                        Toast.makeText(MovieGridActivity.this, "" + favMovies.get(position).getMovieTitle(),
                                Toast.LENGTH_SHORT).show();
                        Intent startMovieDetailIntent = new Intent(MovieGridActivity.this, MovieDetailActivity.class);
                        startMovieDetailIntent.putExtra("movie_object", favMovies.get(position));
                        startActivity(startMovieDetailIntent);
                    }
                });
            }
        } else if (movieListUrl == null && !favoriteSelection) {
            Toast.makeText(MovieGridActivity.this, "No valid preference selected!!!",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isConnected() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = Objects.requireNonNull(cm).getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        //saves variables when rotation changes or activity is paused, etc.
        title = savedInstanceState.getString("TITLE");
        isFav = savedInstanceState.getBoolean("IS_FAV");
        String movieListUrlString = savedInstanceState.getString("QUERY_URL");
        try {
            movieListUrl = new URL(movieListUrlString);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        gvIndex = savedInstanceState.getInt("GRID_VIEW_POSITION");
        moviesGridView.setSelection(gvIndex);
        setTitle(title);
        loadVideoList(movieListUrl, isFav);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //restores saved instances between lifecyle events
        outState.putBoolean("IS_FAV", isFav);
        outState.putString("TITLE", title);
        if (movieListUrl != null){
            outState.putString("QUERY_URL", movieListUrl.toString());
        }
        //solution to keep grid view position on rotation found here:
        // "https://stackoverflow.com/questions/8619794/maintain-scroll-position-of-gridview-through-screen-rotation"
        gvIndex = moviesGridView.getFirstVisiblePosition();
        outState.putInt("GRID_VIEW_POSITION", gvIndex);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        //makes removal of a favorite item appear as soon as the user returns from the detail view
        //while also remembering the position of the view and setting it accordingly
        if(this.getTitle() == getResources().getText(R.string.movie_list_fav_title)) {
            URL movieListUrl = null;
            boolean favoriteSelection = true;
            loadVideoList(movieListUrl, favoriteSelection);
            if(gvIndex != 0) {
                moviesGridView.setSelection(gvIndex);
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    //inner class to async load movie list (suppress to get lint error to go away, cannot make it static)
    @SuppressLint("StaticFieldLeak")
    class MovieDbQueryTask extends AsyncTask<URL, Void, ArrayList<MovieParcelable>> {

        @Override
        protected ArrayList<MovieParcelable> doInBackground(URL... urls) {
            URL loadMoviesUrl = urls[0];
            if (loadMoviesUrl == null) {
                this.cancel(true);
            }
//            Log.v(TAG, "loadMoviesUrl: "+ urls[0]);
            ArrayList<MovieParcelable> movieDbResults = null;
            try {
                movieDbResults = LoadAndParseUtils.loadMoviesJsonFromUrl(loadMoviesUrl);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return movieDbResults;
        }

        @Override
        protected void onCancelled() {
            Toast.makeText(MovieGridActivity.this, getString(R.string.network_error),
                    Toast.LENGTH_SHORT).show();
        }

        @Override
        protected void onPostExecute(final ArrayList<MovieParcelable> movies) {
//            Log.d(TAG, "ArrayList<Movie>: " + movies);
            if (movies != null) {
                MoviePostersAdapter adapter = new MoviePostersAdapter(getApplicationContext(), movies);
                moviesGridView.setAdapter(adapter);
                moviesGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view,
                                            int position, long id) {
                        Toast.makeText(MovieGridActivity.this, "" + movies.get(position).getMovieTitle(),
                                Toast.LENGTH_SHORT).show();
                        Intent startMovieDetailIntent = new Intent(MovieGridActivity.this, MovieDetailActivity.class);
                        startMovieDetailIntent.putExtra("movie_object", movies.get(position));
                        startActivity(startMovieDetailIntent);
                    }
                });
            } else {
                Toast.makeText(MovieGridActivity.this, getString(R.string.network_error),
                        Toast.LENGTH_SHORT).show();
            }
        }

    } //end movielist query Async task

}