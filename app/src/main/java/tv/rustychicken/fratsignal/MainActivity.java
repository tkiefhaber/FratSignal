package tv.rustychicken.fratsignal;

import android.location.Location;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.ui.ParseLoginBuilder;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends ActionBarActivity implements GoogleApiClient.ConnectionCallbacks {

    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    List<ParseUser> mNearbyUsers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (ParseUser.getCurrentUser() == null) {
            ParseLoginBuilder builder = new ParseLoginBuilder(MainActivity.this);
            startActivityForResult(builder.build(), 0);
        }
        buildGoogleApiClient();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.logout) {
            ParseUser.logOut();
            recreate();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        GoogleMap map = mapFragment.getMap();
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            pinMap(map);
            saveCurrentLocation();
            getNearbyUsers(map);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    private void pinMap(GoogleMap map) {
        LatLng location =
                new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
        map.setMyLocationEnabled(true);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 16));
        map.addMarker(new MarkerOptions()
                .position(location));
    }

    private void pinNearbyUsers(GoogleMap map) {
        if (mNearbyUsers.size() > 0) {
            LatLngBounds.Builder boundaries = new LatLngBounds.Builder();

            for (int i = 0; i < mNearbyUsers.size(); i++) {
                ParseUser u = mNearbyUsers.get(i);
                ParseGeoPoint lastLocation = (ParseGeoPoint) u.get("lastLocation");
                LatLng location = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
                boundaries.include(location);

                map.addMarker(new MarkerOptions().position(location));
            }
            map.moveCamera(CameraUpdateFactory.newLatLngBounds(boundaries.build(), 100));
        }
    }

    private void saveCurrentLocation() {
        ParseUser currentUser = ParseUser.getCurrentUser();
        if (currentUser != null) {
            ParseGeoPoint point =
                    new ParseGeoPoint(mLastLocation.getLatitude(), mLastLocation.getLongitude());
            currentUser.put("lastLocation", point);
            currentUser.saveInBackground();
        }
    }

    private void getNearbyUsers(final GoogleMap map) {
        ParseUser currentUser = ParseUser.getCurrentUser();
        if (currentUser != null) {
            ArrayList<String> userList = new ArrayList<>();
            final ArrayAdapter<String> listAdapter = new ArrayAdapter<>(getApplicationContext(), R.layout.user_list_item, userList);
            final ListView userListView = (ListView) findViewById(R.id.user_list);
            userListView.setAdapter(listAdapter);

            ParseGeoPoint point =
                    new ParseGeoPoint(mLastLocation.getLatitude(), mLastLocation.getLongitude());
            ParseQuery<ParseUser> query = ParseUser.getQuery();
            query.whereNotEqualTo("username", currentUser.get("username"));
            query.whereNear("lastLocation", point);
            query.findInBackground(new FindCallback<ParseUser>() {

                @Override
                public void done(List<ParseUser> parseObjects, ParseException e) {
                    if (e == null) {
                        mNearbyUsers = parseObjects;
                        for (int i = 0; i < parseObjects.size(); i++) {
                            ParseUser u = parseObjects.get(i);
                            listAdapter.add(u.getString("name"));
                        }
                        pinNearbyUsers(map);
                    }
                }
            });
        }
    }
}
