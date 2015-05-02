package edu.wit.TTime;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MainActivity extends Activity implements android.location.LocationListener {

	private LocationManager locationManager;
	private Location location;
	private LatLng latlng;
	private float lat, lon;
	private GoogleMap map;
	private final String APIkey = "AImXNKdMcU2V9ehOdT5K0A";
	private String getStops;
	double dLat, dLon, distance;
	private HashMap <Marker, Integer> markers;
	private ConnectivityManager connectivity;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Context context = getApplicationContext();

		// initialize map with current location
		markers = new HashMap<Marker, Integer>();
		map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
		getCurrentLocation();

		// as long as the map is initialized zoom into the current location of the user
		if (map!=null){
			Toast.makeText(context,"Gathering Location Information", Toast.LENGTH_SHORT).show();
			LatLng ll = new LatLng(location.getLatitude(), location.getLongitude());
			map.moveCamera(CameraUpdateFactory.newLatLngZoom(ll, 15));
			map.animateCamera(CameraUpdateFactory.zoomIn());
			map.animateCamera(CameraUpdateFactory.zoomTo(15), 2000, null);
			map.setOnInfoWindowClickListener(new OnInfoWindowClickListener() {

				// detects when a user selects a marker on the map fragment
				@Override
				public void onInfoWindowClick(Marker m) { 
					Intent myIntent = new Intent(MainActivity.this,DisplayStopInfo.class);
					myIntent.putExtra("stop", markers.get(m));
					myIntent.putExtra("stopName", m.getTitle());
					myIntent.putExtra("stopLat", dLat);
					myIntent.putExtra("stopLon", dLon);
					myIntent.putExtra("stopDistance", m.getSnippet());
					startActivity(myIntent);
				}
			});
		}
		else {
			Toast.makeText(context,"Map was not able to initialize.", Toast.LENGTH_SHORT).show();
		}

		try {
			showStations();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	// The methods below were for checking and alerting the lack of internet or location connections
	// These continued to crash for unknown reasons but will be implemented in the future

	/*
	public boolean isConnectingToIntent() {
		NetworkInfo[] info = connectivity.getAllNetworkInfo();
		 if(info != null) {
			 for(int i = 0; i < info.length; i++) {
				 if (info[i].getState() == NetworkInfo.State.CONNECTED) {
					 return true;
				 }
			 }
		 }
		 return false;
	}


	private void buildAlertMessageNoGps() {
		final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
	    builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
	           .setCancelable(false)
	           .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
	               public void onClick(final DialogInterface dialog, final int id) {
	                   startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
	               }
	           })
	           .setNegativeButton("No", new DialogInterface.OnClickListener() {
	               public void onClick(final DialogInterface dialog, final int id) {
	                    dialog.cancel();
	               }
	           });
	    final AlertDialog alert = builder.create();
	    alert.show();
	}

	private void buildAlertMessageNoInternet() {
		final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
	    builder.setMessage("You aren't connected to the internet, would you like to connect?")
	           .setCancelable(false)
	           .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
	               public void onClick(final DialogInterface dialog, final int id) {
	                   startActivity(new Intent(android.provider.Settings.ACTION_DATA_ROAMING_SETTINGS));
	               }
	           })
	           .setNegativeButton("No", new DialogInterface.OnClickListener() {
	               public void onClick(final DialogInterface dialog, final int id) {
	                    dialog.cancel();
	               }
	           });
	    final AlertDialog alert = builder.create();
	    alert.show();
	}
	 */

	// method to get location of user

	public void getCurrentLocation() {
		map.setMyLocationEnabled(true);
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		Criteria criteria = new Criteria();
		String bestProvider = locationManager.getBestProvider(criteria, true);
		locationManager.requestLocationUpdates(bestProvider, 100, 0, this);

		location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		if (location != null) {
			onLocationChanged(location);
		}

		lat = (float) location.getLatitude();
		lon = (float) location.getLongitude();
	}

	// Accesses the MBTA API to get locations of nearby MBTA stops

	public void showStations() throws InterruptedException, ExecutionException, IOException, JSONException {
		String query = "stopsbylocation", stop_name, stop_id;
		LatLng stop_location;
		getStops = "http://realtime.mbta.com/developer/api/v2/" + query
				+ "?api_key=" + APIkey + "&lat=" + lat + "&lon=" + lon + "&format=json";
		Worker w = new Worker();
		w.execute().get();
		String jsonString = w.getJSON();
		JSONObject jsonResponse = new JSONObject(jsonString);
		JSONObject jsonChildNode;

		try {
			JSONArray jsonMainNode = jsonResponse.optJSONArray("stop");
			String parent_station = "";

			for (int i=0; i<jsonMainNode.length(); i++) {
				jsonChildNode = jsonMainNode.getJSONObject(i);
				stop_id = jsonChildNode.optString("stop_id");
				stop_name = jsonChildNode.optString("stop_name");
				parent_station = jsonChildNode.optString("parent_station");

				// stop_id containing "place" eliminates duplicate data
				if(!stop_id.contains("place")){
					int dashFinder = 0;
					dashFinder = stop_name.indexOf('-');
					if(dashFinder > 0) {
						// inbound and outbound stops are layered on each other, take one and eliminate the direction in the name of the stop
						stop_name = stop_name.substring(0, dashFinder-1);
					}
					dLat = jsonChildNode.optDouble("stop_lat");
					dLon = jsonChildNode.optDouble("stop_lon");
					distance = jsonChildNode.optDouble("distance");
					stop_location = new LatLng(dLat, dLon);
					Marker m = map.addMarker(new MarkerOptions().position(stop_location).title(stop_name).snippet("Distance:" + distance + " mi"));
					markers.put(m, Integer.parseInt(stop_id));
				}	
			}
		} catch (JSONException e) {
			e.printStackTrace();
		} 
	}

	@Override
	public void onStart() {
		getCurrentLocation();
		super.onStart();
	}

	@Override
	public void onLocationChanged(Location location) {
		latlng =  new LatLng(location.getLatitude(), location.getLongitude());
		lat = (float) latlng.latitude;
		lon = (float) latlng.longitude;
	}

	private void stopLocationServices() {
		locationManager.removeUpdates(this);
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onProviderEnabled(String provider) {
		// do nothing

	}

	@Override
	public void onProviderDisabled(String provider) {
		// do nothing
	}

	// Creates HTTP requests and accepts replies from MBTA API

	public class Worker extends AsyncTask<String, Integer, Boolean> {

		String jsonString = "";
		@Override
		protected Boolean doInBackground(String... params) {
			try {
				HttpClient httpclient = new DefaultHttpClient();
				HttpGet httpget = new HttpGet (getStops);
				HttpResponse response = httpclient.execute(httpget);
				HttpEntity entity = response.getEntity();
				jsonString = EntityUtils.toString(entity);
			}catch(IOException e){
				e.printStackTrace();
			}
			return null;
		}

		private String getJSON() {
			return jsonString;
		}
	}
}    