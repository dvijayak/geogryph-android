package ca.uoguelph.cmer.geogryph;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

public class Main extends MapActivity implements CampusBuildingsDialogFragment.Contract, AsynchronousHTTP.Contract, SharedPreferences, Runnable //, OnLongClickListener, OnTouchListener
{
	// UI objects
	private SearchView searchView;
	/*private long startTime = 0;
	private int screenX, screenY;*/	
	
	// Map objects & parameters
	private MyLocationOverlay me;
	private static MapView mapView;
	protected GeoItemizedOverlay markersOverlay;
	private List<Overlay> directionsPolyline;
	private static MapController mapController;
	protected static final int minZoom = 14;
	protected static final int desiredZoom = 19;
	
	// Directions
	public String directionsResult;

	// Pre-defined locations
	private final GeoPoint stone_gordon = new GeoPoint(43526643, -80224733);
	private final GeoPoint campus_center = new GeoPoint(43529201, -80228713);

	// Campus buildings	
	protected OverlayItem[] buildings;
	private CampusBuildingsDialogFragment buildingsDialog; // Dialog for displaying list
	
	// Other objects		
	protected static GeoPoint savedLocation;
	protected static SharedPreferences persistentPrimitives;
	
	@Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);  
        buildingsDialog = new CampusBuildingsDialogFragment();        
        
        // Initializing the map        
        mapView = (MapView) findViewById(R.id.mapview);
        mapView.preLoad();
        mapView.setBuiltInZoomControls(true);
        mapController = mapView.getController(); // Controller allows to set the map zoom and map center        
         
        // Overlays and center map initially
        List<Overlay> mapOverlays = mapView.getOverlays();
        me = new GeoMyLocationOverlay(this, mapView);
        mapView.postInvalidate();
        mapOverlays.add(me);
		try
		{
			mapController.animateTo(me.getMyLocation());
		}
		catch (java.lang.NullPointerException e)
		{
			me.runOnFirstFix(new Thread(this));
			mapController.animateTo(stone_gordon);
		}
        mapController.setZoom(desiredZoom);                             
                               
        // Campus buildings        
        String title[] = getResources().getStringArray(R.array.buildings_title);
        String snippet[] = getResources().getStringArray(R.array.buildings_snippet);
        int lat[] = getResources().getIntArray(R.array.buildings_lat);
        int lon[] = getResources().getIntArray(R.array.buildings_lon);
        int totalBuildings = title.length;
        buildings = new OverlayItem[totalBuildings];    ;   
        for (int i = 0; i < totalBuildings; i++)
        	buildings[i] = new OverlayItem(new GeoPoint(lat[i], lon[i]), title[i], snippet[i]);
        
        markersOverlay = new GeoItemizedOverlay(getResources().getDrawable(R.drawable.university_resized), this, mapView);       
        mapOverlays.add(markersOverlay);
        directionsPolyline = new ArrayList<Overlay>();
        
        // Other objects
        persistentPrimitives = getPreferences(MODE_PRIVATE); // used to store and read saved location
        
        // Searchable: get the intent, verify the action and get the query        
        handleSearchIntent(getIntent());

    }
	
	@Override
	public void onNewIntent (Intent intent)
	{
		setIntent(intent);
		handleSearchIntent(intent);
	}
	
	private void handleSearchIntent (Intent intent)
	{
        if (Intent.ACTION_SEARCH.equals(intent.getAction()))
        {        	
        	String query = intent.getStringExtra(SearchManager.QUERY);        	
        	searchPlaces(query, campus_center);
        }
	}
	
    @TargetApi(14)
	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
    {    	    	    	
	    getMenuInflater().inflate(R.menu.main, menu);
	    getActionBar().setHomeButtonEnabled(true); // Ensures that the app icon is clickable
	    
	    // Obtain a reference to the search action view				
		searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
		
		// Set the searchable configuration
		SearchManager sm = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
		searchView.setSearchableInfo(sm.getSearchableInfo(getComponentName()));
		searchView.setOnQueryTextListener(new OnQueryTextListener()
			{
				@Override
				public boolean onQueryTextSubmit(String query) {
					Log.v("Query", "Query: " + query);
					if (query.equalsIgnoreCase("any") || query.equalsIgnoreCase("all") || query.equalsIgnoreCase("anything"))
					{
						searchPlaces(null, campus_center);
						return true;
					}
					else
						return false;
				}
				
				// Perform default behaviour
				@Override
				public boolean onQueryTextChange(String newText) {
					return false;
				}				
			}
		);				
	    return super.onCreateOptionsMenu(menu);
	}	
    
	@Override
	public boolean onOptionsItemSelected (MenuItem item)
	{
		switch (item.getItemId())
		{
		case android.R.id.home:
			// App icon in action bar clicked; go to user's current location				
			try
			{
				mapController.animateTo(me.getMyLocation());				
			}
			catch (java.lang.NullPointerException e)
			{
				mapController.animateTo(stone_gordon);
			}
			return true;
		case R.id.menu_list:
			buildingsDialog.show(this.getFragmentManager(), "tag_buildings");
			return true;
		case R.id.menu_plot:						
			int lat = persistentPrimitives.getInt("lat", stone_gordon.getLatitudeE6());
			int lon = persistentPrimitives.getInt("lon", stone_gordon.getLongitudeE6());
			GeoPoint destination = new GeoPoint(lat, lon);			
			plotDirections(me.getMyLocation(), destination);
			return true;
		case R.id.menu_save:
			// Create an object for storing persistent key-value pairs of primitive data types			
			Editor storageEditor = persistentPrimitives.edit();
			
			// Write to persistent storage			
			GeoPoint currentLocation = me.getMyLocation();
			storageEditor.putInt("lat", currentLocation.getLatitudeE6());
			storageEditor.putInt("lon", currentLocation.getLongitudeE6());
			storageEditor.commit();
			
			produceAlertDialog(this, "Saved", "Your current location has been saved");			
			return true;
		case R.id.menu_clear:
			markersOverlay.clear();			
			List<Overlay> mapOverlays = mapView.getOverlays();
			for (Overlay path : directionsPolyline)
				mapOverlays.remove(path);			
			directionsPolyline.clear();			
			return true;
		case R.id.menu_about:
			new AboutDialogFragment().show(this.getFragmentManager(), "tag_about");
		default:
			return super.onOptionsItemSelected(item);
		}
	}	
	
	// Ensures that location service updates pause/resume upon pausing/resuming the application
	// (Tentative): might need to disable this feature to allow a smoother app-switching experience but at the cost of Internet usage
	@Override
	public void onResume ()
	{
		super.onResume();
		me.enableMyLocation();
	}
	
	@Override
	public void onPause ()
	{
		super.onPause();
		me.disableMyLocation();
	}
		
	/*
	 * Description: Mark a location on the map with a long-press gesture 
	 * Leave commented - Unstable but working code. Can be completed in the future.
	 * @Override
	public boolean dispatchTouchEvent (MotionEvent event)
	{
		int threshold = 800; // In milliseconds		;
		
		int actionType = event.getAction();
		
		switch (actionType)
		{
		case MotionEvent.ACTION_DOWN:
			startTime = event.getEventTime();
			screenX = (int) event.getX();
			screenY = (int) event.getY();
		case MotionEvent.ACTION_MOVE:
			
		case MotionEvent.ACTION_UP:
			long eventTime = event.getEventTime();
			long downTime = event.getDownTime();
			
			if (startTime == downTime)
			{
				if ((eventTime - startTime) > threshold)
				{
					long totalTime = eventTime - startTime;
					Log.v("LongPress", Long.toString(totalTime));
					
					// Convert screen pixels to a GeoPoint
					Projection projection = mapView.getProjection();
					GeoPoint location = projection.fromPixels(screenX, screenY);					
					
					// Create an overlay on this location and add to the map
					// TODO USE GOOGLE GEOCODE SERVICE TO DISPLAY ADDRESS AND POSSIBLY NAME OF LOCATION
					OverlayItem item = new OverlayItem(location, "Marked Location", location.getLatitudeE6()/1e6 + ", " + location.getLongitudeE6()/1e6);
					markersOverlay.addOverlay(item, getResources().getDrawable(R.drawable.yellow_dot));
					markersOverlay.commit();					
				}
			}
		}
		
		return super.dispatchTouchEvent(event);
	}*/	
	
	// General function for generating a standard info alert dialog
	protected static void produceAlertDialog(Context context, String title, String message)
	{
		AlertDialog.Builder dialog = new AlertDialog.Builder(context);
		dialog.setTitle(title);
		dialog.setMessage(message);
		dialog.show();
	}

    // Scroll to the user's current location upon successfully receiving a valid fix
	@Override
	public void run() 
	{
		mapController.animateTo(me.getMyLocation());
	}    

	// Implements the contract defined by CampusBuildingsDialogFragment
	@Override
	public void addOverlay(int which) 
	{
		markersOverlay.addOverlay(buildings[which], null);
		markersOverlay.commit();
	}			
	
	// Construct a valid HTTP request for using the Directions REST API
	private String buildHTTPRequest (GeoPoint origin, GeoPoint destination, boolean sensor)
	{
		String request = getResources().getString(R.string.maps_domain) + "directions/";
		request += getResources().getString(R.string.maps_output); // Output format (json or xml)
		request += "mode=walking"; // Mode
		
		double latOrigin = origin.getLatitudeE6() / 1e6;
		double lonOrigin = origin.getLongitudeE6() / 1e6;
		double latDestination = destination.getLatitudeE6() / 1e6;
		double lonDestination = destination.getLongitudeE6() / 1e6;		
		request += "&origin=" + latOrigin + "," + lonOrigin + "&destination=" + latDestination + "," + lonDestination;
		
		request += "&sensor=" + sensor;	
		
		return request;
	}
	
	// Construct a valid HTTP request for using the Places REST API
	private String buildHTTPRequest (String query, GeoPoint location, boolean sensor)
	{
		String request = getResources().getString(R.string.maps_domain) + "place/nearbysearch/";
		request += getResources().getString(R.string.maps_output);
		
		// Replace white-space characters with "+"
		// Adds a specific keyword search parameter if and only if provided
//		if (!query.equalsIgnoreCase("any") && !query.equalsIgnoreCase("all") && !query.equalsIgnoreCase("anything"))
		if (query != null)
		{
			query = query.replaceAll("\\s", "+");
			request += "keyword=" + query;						
		}		
		
		double lat = location.getLatitudeE6() / 1e6;
		double lon = location.getLongitudeE6() / 1e6;
		request += "&location=" + lat + "," + lon;
		request += "&radius=" + getResources().getInteger(R.integer.places_textsearch_radius);
		
		request += "&sensor=" + sensor;
		request += "&key=" + getResources().getString(R.string.maps_api_key);
		Log.v("buildHTTPRequest", request);
		return request;
	}
	
	// 
	
	// Check if the device has access to the Internet
	private boolean isNetworkAvailable ()
	{
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo ni = cm.getActiveNetworkInfo();
				
		if (ni != null && ni.isConnected())
			return true;
		return false;
	}
	
	// Decode the overview_polyline object into an ordered list of GeoPoints (used for smoothing paths)
	// (Follows the polyline encoding algorithm provided by Google at https://developers.google.com/maps/documentation/utilities/polylinealgorithm) 
	// The code for this method was obtained November 9th 2012 2:54 AM from http://jeffreysambells.com/2010/05/27/decoding-polylines-from-google-maps-direction-api-with-java
	private List<GeoPoint> decodePolyline(String encoded) 
	{
		List<GeoPoint> smoothPath = new ArrayList<GeoPoint>();
		int index = 0, length = encoded.length();
		int lat = 0, lon = 0;
		
		while (index < length)
		{
			// Decode latitude
			int b, shift = 0, result = 0;
			do
			{
				b = encoded.charAt(index++) - 63;
				result |= (b & 0x1f) << shift;
				shift += 5;
			}
			while (b >= 0x20);
			int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
			lat += dlat;
			
			// Decode longitude
			shift = 0;
			result = 0;
			do
			{
				b = encoded.charAt(index++) - 63;
				result |= (b & 0x1f) << shift;
				shift += 5;				
			}
			while (b >= 0x20);
			int dlon = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
			lon += dlon;
			
			// Create a GeoPoint from the decoded lat/lon and add this to the path
			GeoPoint point = new GeoPoint((int) (((double) lat / 1e5) * 1e6), (int) (((double) lon / 1e5) * 1e6));
			smoothPath.add(point);
		}
		return smoothPath;
	}		
	
	@Override
	public void parseJSONResponse (String queryResult)
	{
        // build JSON object
        try
        {        	
        	JSONObject response = new JSONObject(queryResult);        	
        	if (response.has("status") && response.getString("status").equals("OK"))
        	{        		            	        	
        		if (response.has("routes")) 
        		{        			
        			JSONArray routes = response.getJSONArray("routes");
        			List<GeoPoint> pathPoints = new ArrayList<GeoPoint>();
        			for (int r = 0; r < routes.length(); r++)
            		{
            			JSONObject route = routes.getJSONObject(r);            			
            			if (route.has("legs"))
            			{
            				JSONArray legs = route.getJSONArray("legs");
            				for (int l = 0; l < legs.length(); l++)
                			{
                				JSONObject leg = legs.getJSONObject(l);                				
                				if (leg.has("steps"))
                				{
                					JSONArray steps = leg.getJSONArray("steps");
                					for (int s = 0; s < steps.length(); s++)
                    				{        					        					
                    					JSONObject step = steps.getJSONObject(s);                    					
                    					if (step.has("polyline"))
                    					{
                        					// Render each polyline/path (includes smoothed/curved paths)
                    						JSONObject polyline = step.getJSONObject("polyline");     
                    						List<GeoPoint> smoothedPath = decodePolyline(polyline.getString("points"));        					
                        					int length = smoothedPath.size();        					
                        					if (length > 1)
                        					{
                        						// Create a line between two points (i.e. a step; the atomic unit of a route)
                        						int point;
                        						for (point = 1; point < length; point++)
                        						{
                        							GeoPoint A = smoothedPath.get(point - 1), B = smoothedPath.get(point);
                        							pathPoints.add(A);
                        							pathPoints.add(B);
                        						}
                        						
                        						// Scroll to the last point (destination point)
                            					if (s == steps.length() - 1) 
                            					{
                            						// Create the destination marker and add it to the map
                            						GeoPoint destination = smoothedPath.get(point - 1);
                            						OverlayItem overlay = new OverlayItem(destination, "Destination", "You want to go here");			            						
                            						markersOverlay.addOverlay(overlay, getResources().getDrawable(R.drawable.blue_marker_resized));
                            						markersOverlay.commit();            						
                            						mapController.animateTo(destination);
                            					}
                        					} 
                    					}                    					       					        					        						      					        					        					
                    				}
                				}                				
                			}
            			}            			
            		}
            		// Render the complete route polyline
            		Overlay path = new PathOverlay(pathPoints.toArray(new GeoPoint[pathPoints.size()]), mapView);
            		directionsPolyline.add(path);
            		mapView.getOverlays().add(path);
        		}
        		else if (response.has("results"))
        		{
        			if (response.has("html_attributions"))
        			{
        				// Present any necessary attributions to the user, if any (required by Google)
        				JSONArray htmlAttributions = response.getJSONArray("html_attributions");        				
        				if (htmlAttributions.length() > 0)
        				{
        					StringBuilder attributions = new StringBuilder();
            				for (int a = 0; a < htmlAttributions.length(); a++)
            					attributions.append(htmlAttributions.getString(a));
            				produceAlertDialog(this, "Legal Attributions", attributions.toString());	
        				}        				
        			}
        			
        			if (response.has("results"))
        			{
        				JSONArray results = response.getJSONArray("results");
        				int totalResults = results.length();
        				if (totalResults <= 0)
        					produceAlertDialog(this, "No Results", "No results were found!");
        				else
        				{
        					for (int r = 0; r < results.length(); r++)
            				{
            					JSONObject result = results.getJSONObject(r);
            					
            					// Retrieve the location of the object on the map
            					int lat = 0, lon = 0;
            					if (result.has("geometry"))
            					{
            						JSONObject geometry = result.getJSONObject("geometry");
            						if (geometry.has("location"))
            						{
            							JSONObject location = geometry.getJSONObject("location");
            							if (location.has("lat") && location.has("lng"))
            							{
            								lat = (int)(location.getDouble("lat") * 1e6);
            								lon = (int)(location.getDouble("lng") * 1e6);
            							}
            								
            						}
            					}
            					
            					// Create the snippet of the overlay
            					String id = null, name = null, address = null;
            					String iconURL = null, type = null, rating = null, hours = null;        					
            					if (result.has("id"))
            						id = result.getString("id");
            					if (result.has("name"))
            						name = result.getString("name");
            					if (result.has("formatted_address"))
            						address = "Address: " + result.getString("formatted_address");
            					if (result.has("icon"))
            						iconURL = result.getString("icon");
            					if (result.has("types"))
            					{
            						type = "Type: ";
            						JSONArray types = result.getJSONArray("types");
            						int length = types.length();
            						for (int t = 0; t < length; t++)
            						{
            							type += types.getString(t);
            							if (t != length - 1)
            								type += ", ";            							
            						}            							
            					}
            					if (result.has("rating"))
            						rating = "Rating: " + result.getString("rating");
            					if (result.has("opening_hours"))
            					{
            						JSONObject openHours = result.getJSONObject("opening_hours");
            						if (openHours.has("open_now"))   
            						{
            							if (openHours.getBoolean("open_now"))
            								hours = "(OPEN)";
            							else
            								hours = "(CLOSED)";
            							name += " " + hours;
            						}
            					}            						            					            					
            					
            					
            					String[] properties = {address, type, rating};        					
            					StringBuilder snippet = new StringBuilder();		        					
            					for (int i = 0; i < properties.length; i++) {		
            						if (properties[i] != null)			
            							snippet.append(properties[i] + "\n");            						
            					}
            					
            					if (id != null)
            					{            						            					
	            					if (iconURL != null)
	            					{
	            						String request = id + "|" + iconURL;
	            						Log.v("Places Icon", "Request: " + request);
	            						if (isNetworkAvailable())
	            						{
	            							new AsynchronousHTTP(mapView, this).execute(request);
	            						}
	            						else
	            							produceAlertDialog(this, "Error!", "You are not connected to the Internet!");
	            					}
	            					            				
	        						markersOverlay.addPOIOverlay(new OverlayItem(new GeoPoint(lat, lon), name, snippet.toString()), null, id);
            					}
            				}
            				markersOverlay.commit();
        				}
        				
        			}
        			
        		}
        		
        	}            
        }
        catch (JSONException je)
        {
        	je.printStackTrace();
        }        		
	}
	
	private void plotDirections (GeoPoint origin, GeoPoint destination)
	{
		if (destination != null)
		{	
			// Clear existing polyline
			List<Overlay> mapOverlays = mapView.getOverlays();
			for (Overlay path : directionsPolyline)
				mapOverlays.remove(path);
			directionsPolyline.clear();
			
			// Create origin marker
			markersOverlay.addOverlay(new OverlayItem(me.getMyLocation(), "Origin", "You started from here"), getResources().getDrawable(R.drawable.me_resized));
			markersOverlay.commit();
			
			// Build HTTP request for Google Directions
			String request = buildHTTPRequest(origin, destination, true);
			
	        // Set up asynchronous http client and fire request    
	        if (isNetworkAvailable())        
	        	new AsynchronousHTTP(mapView, this).execute(request);	// execute Request in background task        
	        else	        
	        	produceAlertDialog(this, "Error!", "You are not connected to the Internet!");					        			
		}
		else		
			produceAlertDialog(this, "Nothing Saved", "You have not saved a location yet!");						
	}	
	
	private boolean searchPlaces (String query, GeoPoint boundingCircleCenter)
	{	
		// Build HTTP request for Google Places		
		String request = buildHTTPRequest(query, boundingCircleCenter, true);
		
		// Set up asynchronous http client and fire request
		if (isNetworkAvailable())
			new AsynchronousHTTP(mapView, this).execute(request);
		else
		{
			produceAlertDialog(this, "Error!", "You are not connected to the Internet!");
			return false;
		}
		
		return true;
	}	
	
	@Override
	protected boolean isRouteDisplayed() 
	{ 
		return false;
	}
	
	@Override
	public boolean contains(String key) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Editor edit() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, ?> getAll() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean getBoolean(String key, boolean defValue) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public float getFloat(String key, float defValue) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getInt(String key, int defValue) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getLong(String key, long defValue) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getString(String key, String defValue) {
		// TODO Auto-generated method stub		
		return null;
	}

	@Override
	public Set<String> getStringSet(String arg0, Set<String> arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void registerOnSharedPreferenceChangeListener(
			OnSharedPreferenceChangeListener listener) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void unregisterOnSharedPreferenceChangeListener(
			OnSharedPreferenceChangeListener listener) {
		// TODO Auto-generated method stub
		
	}
}
