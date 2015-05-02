package edu.wit.TTime;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class DisplayStopInfo extends Activity {
	
	private GoogleMap map;
	private ListView lvInbound, lvOutbound;
	private TextView tvInbound, tvOutbound, tvStopName;
	private final String APIkey = "AImXNKdMcU2V9ehOdT5K0A";
	private double stopLat, stopLon, distance, travelTime;
	private LatLng stopLatLng;
	private String stopName, queryPredictions;
	private int stopID;
	private List <Map<String, String>> inboundTimes, outboundTimes, arrivalTimes;
	private String[] Arrivals = new String[2];
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_display_stop_info);	
		
		map = ((MapFragment)getFragmentManager().findFragmentById(R.id.mapSnippet)).getMap();
		lvInbound = (ListView) findViewById(R.id.lvInbound);
		lvOutbound = (ListView) findViewById(R.id.lvOutbound);
		tvInbound = (TextView) findViewById(R.id.tvInbound);
		tvOutbound = (TextView) findViewById(R.id.tvOutbound);
		tvStopName = (TextView) findViewById(R.id.tvStopName);
		
		Intent intent = getIntent();
		stopID = intent.getIntExtra("stop", -1);
		stopName = intent.getStringExtra("stopName");
		stopLat = intent.getDoubleExtra("stopLat", 42.3583333);
		stopLon = intent.getDoubleExtra("stopLon", -71.0602778);
		String strDist = intent.getStringExtra("stopDistance");
		int index = strDist.indexOf(" ");
		distance = Double.parseDouble(strDist.substring(9, index));
		tvStopName.setText(stopName);
		
		//inboundTimes = new ArrayList<Map<String, String>>();  will use with live data
		//outboundTimes = new ArrayList<Map<String, String>>(); will use with live data
		arrivalTimes = new ArrayList<Map<String, String>>();
		
		// set up map fragment
		if(map!=null) {
			stopLatLng = new LatLng(stopLat, stopLon);
			map.addMarker(new MarkerOptions().position(stopLatLng).title(stopName));
			map.moveCamera(CameraUpdateFactory.newLatLngZoom(stopLatLng, 15));
			map.animateCamera(CameraUpdateFactory.zoomIn());
			map.animateCamera(CameraUpdateFactory.zoomTo(15), 2000, null);
		}
		
		//getStopPredictions();  With live data use this
		
		/*
		 * More work with live data
		if(inboundTimes.isEmpty()) {
			tvOutbound.setText("Outbound Arrivals");
			SimpleAdapter adapterOut = new SimpleAdapter(DisplayStopInfo.this, outboundTimes, android.R.layout.simple_list_item_2, new String[] {"time", "date"}, new int[] {android.R.id.text1, android.R.id.text2});
			lvOutbound.setAdapter(adapterOut);
		}
		else if(outboundTimes.isEmpty()) {
			tvInbound.setText("InboundArrivals");
			SimpleAdapter adapterIn = new SimpleAdapter(DisplayStopInfo.this, inboundTimes, android.R.layout.simple_list_item_2, new String[] {"time", "date"}, new int[] {android.R.id.text1, android.R.id.text2});
			lvOutbound.setAdapter(adapterIn);
		}
		else {
			tvOutbound.setText("Outbound Arrivals");
			tvInbound.setText("InboundArrivals");
			
			SimpleAdapter adapterOut = new SimpleAdapter(DisplayStopInfo.this, outboundTimes, android.R.layout.simple_list_item_2, new String[] {"time", "date"}, new int[] {android.R.id.text1, android.R.id.text2});
			lvOutbound.setAdapter(adapterOut);
			
			SimpleAdapter adapterIn = new SimpleAdapter(DisplayStopInfo.this, inboundTimes, android.R.layout.simple_list_item_2, new String[] {"time", "date"}, new int[] {android.R.id.text1, android.R.id.text2});
			lvOutbound.setAdapter(adapterIn);
			
		}
		*/
		
		// Fake data for presentation
		fakeData();
		
	}
	
	// Static data methods
	
	public void fakeData() {
		//get current date and time
		
		Calendar c = Calendar.getInstance();
		String am_pm = "AM";
		int Hour = c.get(Calendar.HOUR_OF_DAY);
		int Minute = c.get(Calendar.MINUTE);
		String arrivalTime1, arrivalTime2;
	//	if(Hour >= 12) {
	//		am_pm = "PM";
	//		if(Hour > 12) {
	//			Hour -= 12;
	//		}
	//	}
		if(Minute < 10) {
			String min = null;
			min=String.format("%02d", Minute); // leading 0 for minutes less than 2 digits
		}
		
		SimpleDateFormat df = new SimpleDateFormat("MM-dd-yyy");
		String arrivalDate = df.format(c.getTime());
		String date = null;
		
		 // Checks if next arriving train rolls minutes past 60
		if(Minute + 3 >= 60) {
			Hour += 1;
			am_pm = amORpm(Hour);
			Hour = TwelveHrTime(Hour);
			Minute = (Minute+3) % 60;
			date=String.format("%02d:%02d", Hour, Minute);
			arrivalTime1 = date + ' ' + am_pm;
			Arrivals[0] = arrivalTime1;
			if(Minute + 7 >= 60) {
				Minute = (Minute + 7) % 60;
				Hour += 1;
				am_pm = amORpm(Hour);
				Hour = TwelveHrTime(Hour);
				date=String.format("%02d:%02d", Hour, Minute);
				arrivalTime2 = date + ' ' + am_pm;
				Arrivals[1] = arrivalTime2;
			} else {
				Minute =+ 7;
				String.format("%02d:%02d", Hour, Minute);
				arrivalTime2 = date + ' ' + am_pm;
				Arrivals[1] = arrivalTime2;
			}
		}
		else if(Minute + 7 >= 60) {
			Minute += 3;
			date = String.format("%02d:%02d", Hour, Minute);
			arrivalTime1 = date + ' ' + am_pm;
			Arrivals[0] = arrivalTime1;
			Minute = (Minute + 7) % 60;
			Hour += 1;
			am_pm = amORpm(Hour);
			Hour = TwelveHrTime(Hour);
			date = String.format("%02d:%02d", Hour, Minute);
			arrivalTime2 = date + ' ' + am_pm;
			Arrivals[1] = arrivalTime2;
		}
		else {
			Minute += 3;
			am_pm = amORpm(Hour);
			Hour = TwelveHrTime(Hour);
			date = String.format("%02d:%02d", Hour, Minute);
			arrivalTime1 = date + ' ' + am_pm;
			Arrivals[0] = arrivalTime1;
			Minute += 7;
			date=String.format("%02d:%02d",Hour, Minute);
			arrivalTime2 = date + ' ' + am_pm;
			Arrivals[1] = arrivalTime2;
		}
		
		displayFakeData(arrivalDate);
	}
	
	// AM or PM for 12 hour time
	public String amORpm(int hr) {
		if(hr >= 12) {
			return "PM";
		}
		else {
			return "AM";
		}
	}
	
	// Changes 24 hour time to 12 hour time
	public int TwelveHrTime(int Hr) {
		if(Hr > 12) {
			return Hr -= 12;
		}
		return Hr;
	}
	
	public void displayFakeData(String date) {
		// formating for list view
		for(int i=0; i<Arrivals.length;i++) {
			Map<String, String> stopStuff = new HashMap<String, String>();
			stopStuff.put("time", Arrivals[i]);
			stopStuff.put("date", date.toString());
			arrivalTimes.add(stopStuff);
		}
		
		tvOutbound.setText("Outbound Arrivals");
		tvInbound.setText("Inbound Arrivals");
		
		SimpleAdapter adapterOut = new SimpleAdapter(DisplayStopInfo.this, arrivalTimes, android.R.layout.simple_list_item_2, new String[] {"time", "date"}, new int[] {android.R.id.text1, android.R.id.text2});
		lvOutbound.setAdapter(adapterOut);
		
		SimpleAdapter adapterIn = new SimpleAdapter(DisplayStopInfo.this, arrivalTimes, android.R.layout.simple_list_item_2, new String[] {"time", "date"}, new int[] {android.R.id.text1, android.R.id.text2});
		lvInbound.setAdapter(adapterIn);
		
		// list view on click
		lvOutbound.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				
				// Alert Dialog for setting an alarm
				final AlertDialog.Builder builder = new AlertDialog.Builder(DisplayStopInfo.this);
			    builder.setMessage("Set a notification to alert you when to go to the stop?\n expected travel time is: " + getTravelTime() + " minutes")
			           .setCancelable(false)
			           .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			        	   public void onClick(final DialogInterface dialog, final int id) {
			        		   Calendar cal = Calendar.getInstance();
			        		   
			                   if(lvOutbound.getSelectedItemPosition() == 0 || lvOutbound.getSelectedItemPosition() == 0) {
				                	double time = 3.0 - getTravelTime();  
				                  }
			                   else if(lvOutbound.getSelectedItemPosition() == 1 || lvInbound.getSelectedItemPosition() == 1) {
			                	   double time = 10.0 - getTravelTime();
			                   }
			                  //Intent myIntent = new Intent(DisplayStopInfo.this, myService.class);
			                  //AlarmManager alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
			                  //PendingIntent pendingIntent = PendingIntent.getService(DisplayStopInfo.this, 0, myIntent, 0);
			                  int hour = cal.get(Calendar.HOUR_OF_DAY);
			                  int min = cal.get(Calendar.MINUTE);			             
			                  //cal.set(Calendar.HOUR_OF_DAY, );
			                  Toast.makeText(getApplicationContext(), "A notification has been set for you", Toast.LENGTH_SHORT).show();
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
		});
		
		lvInbound.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				
				final AlertDialog.Builder builder = new AlertDialog.Builder(DisplayStopInfo.this);
			    builder.setMessage("Set a notification to alert you when to go to the stop?\n expected travel time is: " + getTravelTime() + " minutes")
			           .setCancelable(false)
			           .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			        	   public void onClick(final DialogInterface dialog, final int id) {
			        		   /*Calendar cal = Calendar.getInstance();
			        		   int time = 0;
			        		   
			                   if(lvOutbound.getSelectedItemPosition() == 0 || lvOutbound.getSelectedItemPosition() == 0) {
				                	time = (int) (3.0 - getTravelTime());  
				                  }
			                   else if(lvOutbound.getSelectedItemPosition() == 1 || lvInbound.getSelectedItemPosition() == 1) {
			                	   time = (int) (10.0 - getTravelTime());
			                   }
			                  int hour = cal.get(Calendar.HOUR_OF_DAY);
			                  int min = cal.get(Calendar.MINUTE)+time;
			     
			                  Intent alarmIntent = new Intent(DisplayStopInfo.this, myService.class);
			                  long scTime = 60 * (time * 1000);
			                  PendingIntent pendingIntent = PendingIntent.getBroadcast(DisplayStopInfo.this, 0, alarmIntent, 0);
			                  AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
			                  alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + scTime, pendingIntent);
			                  cal.set(Calendar.HOUR_OF_DAY, ); */
			                  Toast.makeText(getApplicationContext(), "A notification has been set for you", Toast.LENGTH_SHORT).show();
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
		});
	}
	
	public double getTravelTime() {
		//.05 mi per minute walking that is 3.0 mi per hour
		travelTime = distance / .05;
		
		// add time for users to be slightly early to the train
		if(travelTime <= 1) {
			travelTime = 1;
		}
		else{
			travelTime += 1;
		}
		
		return Math.floor(travelTime); 
	}
	
	// End static data work

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.display_stop_info, menu);
		return true;
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
	
	public void getStopPredictions() {
		String jsonString;
		queryPredictions = "http://realtime.mbta.com/developer/api/v2/predictionsbystop?api_key="
				+ APIkey + "&stop=" + stopName + "&format=json";
		
		Worker w = new Worker();
		w.execute();
		jsonString = w.getJSON();
		
		try {
			JSONObject jsonResponse = new JSONObject(jsonString);
			JSONObject jsonChildNode;
			JSONArray jsonMainNode = jsonResponse.optJSONArray("mode");
			
			for(int i=0;i<jsonMainNode.length(); i++) {
				jsonChildNode=jsonMainNode.getJSONObject(i);
				if(jsonChildNode.optString("direction_name").equals("Inbound")) {
					double epochArrivalIn = Double.parseDouble(jsonChildNode.optString("sch_arr_dt"));
					String date = new java.text.SimpleDateFormat("MM/dd/yyyy HH:MM").format(new java.util.Date((long) (epochArrivalIn*1000)));
					int indexOfSpace = date.indexOf(" ");
					String showDay = date.substring(0, indexOfSpace);
					String showTime = date.substring(indexOfSpace + 1);
					Map<String, String> stopStuff = new HashMap<String, String>();
					stopStuff.put("time", showTime);
					stopStuff.put("date", showDay);
					inboundTimes.add(stopStuff);
				}
				else if(jsonChildNode.optString("direction_name").equals("Outbound")) {
					double epochArrivalOut = Double.parseDouble(jsonChildNode.optString("sch_arr_dt"));
					String date = new java.text.SimpleDateFormat("MM/dd/yyyy HH:MM").format(new java.util.Date((long) (epochArrivalOut*1000)));
					int indexOfSpace = date.indexOf(" ");
					String showDay = date.substring(0, indexOfSpace);
					String showTime = date.substring(indexOfSpace + 1);
					Map<String, String> stopStuff = new HashMap<String, String>();
					stopStuff.put("time", showTime);
					stopStuff.put("date", showDay);
					outboundTimes.add(stopStuff);
				}
				else {
					tvInbound.setText("No data for station: " + stopName);
					break;
				}
			}
			//if(!inboundTimes.isEmpty()) {
			//}
			//if(!outboundTimes.isEmpty()) {
			//}
		} catch (JSONException e) {
			e.printStackTrace();
		}
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
