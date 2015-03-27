package com.example.ttimes;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import android.os.Bundle;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MainActivity extends Activity implements android.location.LocationListener {

	private LocationManager locationManager;
	private Location location;
	//private double lat, lng;
	private LatLng latlng;
	private float lat, lon;
	private GoogleMap map;
	private final String APIkey = "AImXNKdMcU2V9ehOdT5K0A";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
		getCurrentLocation();
		
		if (map!=null){
	    	LatLng ll = new LatLng(location.getLatitude(), location.getLongitude());
			map.addMarker(new MarkerOptions().position(ll).title("You Are Here"));
			map.moveCamera(CameraUpdateFactory.newLatLngZoom(ll, 15));
			map.animateCamera(CameraUpdateFactory.zoomIn());
			map.animateCamera(CameraUpdateFactory.zoomTo(15), 2000, null);
			
		}
		else {
			Context context = getApplicationContext();
			CharSequence text = "fucking null brah";
			int duration = Toast.LENGTH_LONG;
			
			Toast toast = Toast.makeText(context, text, duration);
			toast.show();
		}
		
		showStations();
	}



	public void getCurrentLocation() {
		map.setMyLocationEnabled(true);
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        String bestProvider = locationManager.getBestProvider(criteria, true);
        locationManager.requestLocationUpdates(bestProvider, 100, 0, this);
        
        location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        if (location != null) {
            onLocationChanged(location);
    		Log.v("here", "in location not null");
        }
	}
	
	public void showStations() {
		String query = "stopsbylocation";
		String HTTPquery = "http://realtime.mbta.com/developer/api/v2/<" + query
		+ ">?api_key=<" + APIkey + ">&format=json&<parameter>=<"
				+ lat + ',' + lon +'>';
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
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub
		
	}
}    
   