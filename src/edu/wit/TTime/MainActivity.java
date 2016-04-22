package edu.wit.TTime;

import java.io.IOException;
import java.text.DecimalFormat;
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
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
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
	private double dLat, dLon, distance, oldLat=0.0, oldLon=0.0;
	private HashMap<Marker, String> markers;
	private ConnectivityManager connectivity;
	private Context context;
	private boolean internetConnectionFound = false, locationFound = false;
	
	/*
	 * Check internet and location connections
	 * initialize map
	 */
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		context = getApplicationContext();

		connectivity = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
		isConnectingToInternet();
		locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		if(!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			buildAlertMessageNoGps();
		}
		else {
			locationFound = true;
		}

		// initialize map with current location
		markers = new HashMap<Marker, String>(); // was <Marker>
		map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
		getCurrentLocation();

		// as long as the map is initialized zoom into the current location of the user
		if (map!=null){
			Toast.makeText(context,"Gathering MBTA Vehicle Information", Toast.LENGTH_SHORT).show();
			LatLng ll = new LatLng(location.getLatitude(), location.getLongitude());
			map.moveCamera(CameraUpdateFactory.newLatLngZoom(ll, 15));
			map.animateCamera(CameraUpdateFactory.zoomIn());
			map.animateCamera(CameraUpdateFactory.zoomTo(15), 2000, null);
			map.setOnInfoWindowClickListener(new OnInfoWindowClickListener() {

				// detects when a user selects a marker on the map fragment
				// need to add stop Id to this for query
				@Override
				public void onInfoWindowClick(Marker m) { 
					Intent myIntent = new Intent(MainActivity.this,DisplayStopInfo.class);
					myIntent.putExtra("stopID", markers.get(m));
					myIntent.putExtra("stopName", m.getTitle());
					myIntent.putExtra("stopLat", m.getPosition().latitude);
					myIntent.putExtra("stopLon", m.getPosition().longitude);
					myIntent.putExtra("stopDistance", m.getSnippet());
					startActivity(myIntent);
				}
			});
		}
		else {
			Toast.makeText(context,"Map was not able to initialize.", Toast.LENGTH_SHORT).show();
			finish();
		}
		
		int connectionTimeOut = 0;
		// waits up to 5 seconds for internet and locaiton to be detected
		while(internetConnectionFound == false && locationFound == false && connectionTimeOut <= 10){
			try {
				// wait half a second to ensure location and Internet are available
				Thread.sleep(500); 
			} catch(InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
			connectionTimeOut++;
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

	// These methods are for checking and alerting the lack of Internet or location connections

	public void isConnectingToInternet() {

		boolean hasWifi = false, hasMobile = false;
		NetworkInfo[] info = connectivity.getAllNetworkInfo();
		if(info != null) {
			for(NetworkInfo ni : info){
				if(ni.getTypeName().equalsIgnoreCase("WIFI") && ni.isConnected()) {
					hasWifi = true;
				}
				else if(ni.getTypeName().equalsIgnoreCase("MOBILE") && ni.isConnected()) {
					hasMobile = true;
				}
			}
		}
		// this means all Internet connections are closed
		if(!hasMobile && !hasWifi) {			
			buildAlertMessageNoInternet(); 
		}
		else {
			internetConnectionFound = true;
		}
	}

	private void buildAlertMessageNoGps() {
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
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
		AlertDialog alert = builder.create();
		alert.show();
	}

	private void buildAlertMessageNoInternet() {
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
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
		AlertDialog alert = builder.create();
		alert.show();
	}

	private void buildAlertMessageNoStations() {
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("We couldn't find any MBTA Stations within a mile of your current location :(")
		.setCancelable(false)
		.setPositiveButton("Get Help Online", new DialogInterface.OnClickListener() {
			public void onClick(final DialogInterface dialog, final int id) {
				startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.mbta.com")));
			}
		})
		.setNegativeButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(final DialogInterface dialog, final int id) {
				dialog.cancel();
			}
		});
		AlertDialog alert = builder.create();
		alert.show();
	}

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
		String query = "stopsbylocation", stop_name, stop_id, parent_station;
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

			if(jsonMainNode.length()==0) {
				buildAlertMessageNoStations();
			}
			else {

				for (int i=0; i<jsonMainNode.length(); i++) {

					jsonChildNode = jsonMainNode.getJSONObject(i);
					stop_id = jsonChildNode.optString("stop_id");
					stop_name = jsonChildNode.optString("stop_name");
					parent_station = jsonChildNode.optString("parent_station");

					if(stop_name.contains(" @ ")|| (!stop_name.contains(" - ")&& !parent_station.contains("place-"))){
						dLat = jsonChildNode.optDouble("stop_lat");
						dLon = jsonChildNode.optDouble("stop_lon");
						if(dLat == oldLat && dLon == oldLon){
							dLat += 0.000003;
							dLon += 0.000060;
						}
						// prevent same position from appearing
						else { 					
							oldLat = dLat;
							oldLon = dLon;
						}
						distance = jsonChildNode.optDouble("distance");
						DecimalFormat df = new DecimalFormat("#.00");
						distance = Double.parseDouble(df.format(distance));
						stop_location = new LatLng(dLat, dLon);
						Marker m = map.addMarker(new MarkerOptions().position(stop_location).title(stop_name).snippet(distance + " mi"));
						markers.put(m, stop_id); 
					} 		// end outer if block
				} 			// end for loop
			} 				// end else block
		} catch (JSONException e) {
			e.printStackTrace();
		} 
	}

	@Override
	public void onStart() {
		getCurrentLocation();
		super.onStart();
	}
	
	// hopefully prevent reloading data when navigating back to map
	public void onResume() {
		super.onResume();
	}

	@Override
	public void onLocationChanged(Location location) {
		latlng =  new LatLng(location.getLatitude(), location.getLongitude());
		lat = (float) latlng.latitude;
		lon = (float) latlng.longitude;
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

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub
		
	}
}    

