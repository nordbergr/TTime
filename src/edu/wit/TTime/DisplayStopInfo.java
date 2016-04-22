package edu.wit.TTime;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class DisplayStopInfo extends ActionBarActivity {

	private GoogleMap map;
	private ListView lvRt1,lvRt2, lvRt3;
	private TextView tvRt1, tvRt2, tvRt3;
	private final String APIkey = "AImXNKdMcU2V9ehOdT5K0A";
	private double stopLat, stopLon, distance, travelTime;
	private LatLng stopLatLng;
	private String stopName, queryPredictions, stopID;
	private Context context;
	private List<edu.wit.TTime.ListItem> subwayList = new ArrayList<edu.wit.TTime.ListItem>();
	private List<edu.wit.TTime.ListItem> crList = new ArrayList<edu.wit.TTime.ListItem>();
	private List<edu.wit.TTime.ListItem> busList = new ArrayList<edu.wit.TTime.ListItem>();
	private AlarmManager almMgr;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.display_stop_info, menu);
		return super.onCreateOptionsMenu(menu);
	}
	
	
	// All initializations needed after activity starts.
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_display_stop_info);	
		context = getApplicationContext();
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		// initialize map, list views, and text views
		map = ((MapFragment)getFragmentManager().findFragmentById(R.id.mapSnippet)).getMap();
		lvRt1 = (ListView) findViewById(R.id.lvRt1);
		lvRt2 = (ListView) findViewById(R.id.lvRt2);
		lvRt3 = (ListView) findViewById(R.id.lvRt3);
		tvRt1 = (TextView) findViewById(R.id.tvRt1);
		tvRt2 = (TextView) findViewById(R.id.tvRt2);
		tvRt3 = (TextView) findViewById(R.id.tvRt3);

		// Get information passed to this activity
		Intent intent = getIntent();
		stopID = intent.getStringExtra("stopID");
		stopName = intent.getStringExtra("stopName");
		stopLat = intent.getDoubleExtra("stopLat", 42.3583333);
		stopLon = intent.getDoubleExtra("stopLon", -71.0602778);
		String strDist = intent.getStringExtra("stopDistance");
		int index = strDist.indexOf(" ");
		distance = Double.parseDouble(strDist.substring(0, index));

		getSupportActionBar().setTitle(stopName);

		// set up map fragment
		if(map!=null) {
			stopLatLng = new LatLng(stopLat, stopLon);
			map.addMarker(new MarkerOptions().position(stopLatLng).title(stopName));
			map.moveCamera(CameraUpdateFactory.newLatLngZoom(stopLatLng, 15));
			map.animateCamera(CameraUpdateFactory.zoomIn());
			map.animateCamera(CameraUpdateFactory.zoomTo(15), 2000, null);
		}

		getStopPredictions();					// Calls Asynchronous method to call MBTA API 
	}

	public String getTravelTime() {
		//.05 mi per minute walking that is 3.0 mi per hour
		travelTime = distance / .05;

		// add time for users to be slightly early to the train
		if(travelTime <= 1) {
			travelTime = 1;
		}
		else{
			travelTime += 1;
		}
		return Double.toString(Math.floor(travelTime)); 
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@SuppressLint("SimpleDateFormat") 
	public void getStopPredictions() {
		String jsonString;
		queryPredictions = "http://realtime.mbta.com/developer/api/v2/predictionsbystop?api_key="
				+ APIkey + "&stop=" + stopID + "&format=json";

		Worker w = new Worker();
		w.execute();

		// Force wait until JSON response is received
		do {
			jsonString=w.getJSON();
		} while(jsonString.isEmpty());

		try {
			JSONObject jsonResponse = new JSONObject(jsonString);
			JSONObject jsonRouteType, jsonDirectionNode, jsonTripNode, jsonTripIt; 
			JSONArray jsonMainNode = jsonResponse.optJSONArray("mode");
			JSONArray jsonRouteInfo, jsonDirection, jsonTrip;
			@SuppressWarnings("unused")
			String headsign, modeName;
			if(!jsonString.contains("error")&& !jsonString.contains("Error")){

				for(int i=0;i<jsonMainNode.length(); i++) {
					jsonRouteType=jsonMainNode.getJSONObject(i); 			// Get route List
					
					if(i+1 != Integer.parseInt(jsonRouteType.getString("route_type"))) {
						i+= 1;
					}
					modeName = jsonRouteType.getString("mode_name");

					jsonRouteInfo = jsonRouteType.getJSONArray("route");

					for(int j=0;j<jsonRouteInfo.length();j++){				// Get direction List
						jsonDirectionNode=jsonRouteInfo.getJSONObject(j);
						jsonDirection = jsonDirectionNode.getJSONArray("direction");

						for(int k=0;k<jsonDirection.length();k++){			// Get trip List
							jsonTripNode = jsonDirection.getJSONObject(k);
							jsonTrip = jsonTripNode.getJSONArray("trip");

							for(int l=0;l<jsonTrip.length();l++){  			// Iterate through trip List
								jsonTripIt = jsonTrip.getJSONObject(l);

								if(l==0)
								{
									headsign = jsonTripIt.getString("trip_headsign"); 
								}
								String tripDirection = (String) jsonTripIt.get("trip_headsign");
								String epochArrival = (String) jsonTripIt.get("sch_arr_dt");
								String arrival = new java.text.SimpleDateFormat("hh:mm aa").format(new java.util.Date (Long.parseLong(epochArrival)*1000));
								if(arrival.toCharArray()[0] == '0'){
									arrival = arrival.substring(1);
								}
								switch(modeName) {
								case "Subway": 						// 1st form of transportation
									edu.wit.TTime.ListItem subway = new edu.wit.TTime.ListItem();
									subway.headsign = "To: " + tripDirection;
									subway.dateTime = "At " + arrival;
									subwayList.add(subway);
									break;
								case "Commuter Rail": 						// 2nd form of transportation
									edu.wit.TTime.ListItem CR = new edu.wit.TTime.ListItem();
									CR.headsign="To: " + tripDirection;
									CR.dateTime="At " + arrival;
									crList.add(CR);
									break;
								case "Bus":							// 3rd form of transportation
									edu.wit.TTime.ListItem bus = new edu.wit.TTime.ListItem();
									bus.headsign="To: " + tripDirection;
									bus.dateTime="At " + arrival;
									busList.add(bus);
									break;
								}
							}
						}
					}
				} // end main node
			}
		} // end try block
		catch(JSONException e) {
			// showTimes handles error
		}
		showTimes();
	} // end get stop predictions

	public void showTimes() {
		ListItemAdapter adapter;
		if(!subwayList.isEmpty()) {
			tvRt1.setText("Subway");
			adapter = new ListItemAdapter(this,0,subwayList);
			lvRt1.setAdapter(adapter);
			lvRt1.setOnItemClickListener(new AdapterView.OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> parent, View view,
						int position, long id) {
					buildAlertMessageAlarm(subwayList.get(position));
				}
			});
		}
		
		if(!crList.isEmpty()){
			tvRt2.setText("Commuter Rail");
			adapter = new ListItemAdapter(this,0,crList);
			lvRt2.setAdapter(adapter);
			lvRt2.setOnItemClickListener(new AdapterView.OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					buildAlertMessageAlarm(crList.get(position));
				}
			});
		}
		
		if(!busList.isEmpty()){
			tvRt3.setText("Bus");
			adapter = new ListItemAdapter(this,0,busList);
			lvRt3.setAdapter(adapter);
			lvRt3.setOnItemClickListener(new AdapterView.OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> parent, View view,
						int position, long id) {
					buildAlertMessageAlarm(busList.get(position));
				}
			});
		}
		if(subwayList.isEmpty() && crList.isEmpty() && busList.isEmpty()){
			
			final AlertDialog.Builder builder = new AlertDialog.Builder(DisplayStopInfo.this);
			builder.setMessage("No information is available for this station.")
			.setCancelable(false)
			.setNegativeButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(final DialogInterface dialog, final int id) {
					dialog.cancel();
				}
			});
			final AlertDialog alert = builder.create();
			alert.show();
		}
	}

	private void buildAlertMessageAlarm(final edu.wit.TTime.ListItem listItem) {

		final AlertDialog.Builder builder = new AlertDialog.Builder(DisplayStopInfo.this);
		builder.setMessage("Set a notification to alert you when to travel to this station?\n\nExpected travel time about " + getTravelTime().substring(0, getTravelTime().indexOf('.')) + " minutes.")
		.setCancelable(false)
		.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			public void onClick(final DialogInterface dialog, final int id) {
				String time = (String)listItem.dateTime.subSequence(3, listItem.dateTime.length());
				Toast.makeText(context, "An alarm set for " + time + " has been created for you.", Toast.LENGTH_LONG).show();
				setAlarm(time); 
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

	private void setAlarm(String dateTime) {
		int hour = 0, minute = 0;
		if(dateTime.contains(" PM")) {
			hour += 12;
		}

		dateTime = dateTime.substring(0, dateTime.indexOf(':') + 3);
		int split = dateTime.indexOf(':');
		hour = Integer.parseInt(dateTime.substring(0, split));
		minute = Integer.parseInt(dateTime.substring(split+1));

		if(minute + Double.parseDouble(getTravelTime()) >= 60){
			hour += 1;
			minute = minute % 60;
			if(hour >= 24){
				hour = hour % 24;
			}
		}

		Calendar cal = Calendar.getInstance();
		long calHrs = cal.get(Calendar.HOUR_OF_DAY);
		long calMin = cal.get(Calendar.MINUTE);
		calMin *= 60000;
		calHrs *= 3600000;
		long calTime = calMin + calHrs;
		String travelTimer = getTravelTime(); // current time in milliseconds
		travelTimer = (String) travelTimer.subSequence(0, travelTimer.indexOf('.'));
		double travelTime = Long.parseLong(travelTimer);
		long totalTravelTime = (long) travelTime * 60000; // travel time in milliseconds

		hour *= 3600000; 				// 60*60 minutes in an hour * seconds in a minute
		minute *= 60000; 				// 60 seconds in a minute
		long time = minute + hour; 		// arrival time in milliseconds

		time -= totalTravelTime;
		time -= calTime; 

		almMgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		Intent i = new Intent(context, DisplayStopInfo.class);
		PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
		cal.setTimeInMillis(cal.getTimeInMillis()+time);
		almMgr.set(AlarmManager.RTC_WAKEUP,cal.getTimeInMillis(),pi);
	}

	public class Worker extends AsyncTask<String, Integer, Boolean> {
		String jsonString = "";
		@Override
		protected Boolean doInBackground(String... params) {
			try {
				HttpClient httpclient = new DefaultHttpClient();
				HttpGet httpget = new HttpGet(queryPredictions);
				HttpResponse response = httpclient.execute(httpget);
				HttpEntity entity = response.getEntity();
				jsonString = EntityUtils.toString(entity);
			}catch(IOException e) {
				e.printStackTrace();
			}
			return null;
		}
		private String getJSON() {
			return jsonString;
		}
	}
}