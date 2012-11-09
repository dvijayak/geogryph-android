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
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

public class Main extends MapActivity implements CampusBuildingsDialogFragment.Contract, AsynchronousHTTP.Contract, SharedPreferences, Runnable
{
	// Map objects & parameters
	private GeoMyLocationOverlay me;
	private static MapView mapView;
	private GeoItemizedOverlay campusBuildingsOverlay;
//	private GeoItemizedOverlay destinationOverlay;
	private List<Overlay> directionsPolyline;
	private static MapController mapController;
	private final int minZoom = 12;
	private final int desiredZoom = 18;
	
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
        
        campusBuildingsOverlay = new GeoItemizedOverlay(getResources().getDrawable(R.drawable.university_resized), this, mapView);       
        mapOverlays.add(campusBuildingsOverlay);
        directionsPolyline = new ArrayList<Overlay>();
        
        // Other objects
        persistentPrimitives = getPreferences(MODE_PRIVATE); // used to store and read saved location
    }

	@Override
	protected boolean isRouteDisplayed() 
	{ 
		return false;
	}
		
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
	
    @TargetApi(14)
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.main, menu);
	    this.getActionBar().setHomeButtonEnabled(true); // Ensures that the app icon is clickable	    
	    return true;
	}	

	@Override
	public void run() 
	{
		mapController.animateTo(me.getMyLocation());
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
			
			AlertDialog.Builder dialog = new AlertDialog.Builder(this);
			dialog.setTitle("Saved");
			dialog.setMessage("Your current location has been saved!");
			dialog.show();
			return true;
		case R.id.menu_clear:
			campusBuildingsOverlay.clear();
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

	// Implements the contract defined by CampusBuildingsDialogFragment
	@Override
	public void addOverlay(int which) 
	{
		campusBuildingsOverlay.addOverlay(buildings[which], null);
		campusBuildingsOverlay.commit();
	}			
	
	// Construct a valid HTTP request for using the Directions REST Web Service
	private String buildHTTPRequest (GeoPoint origin, GeoPoint destination, boolean sensor)
	{
		String request = "http://maps.googleapis.com/maps/api/directions/";
		request += "json?"; // Output format (json or xml)
		request += "mode=walking"; // Mode
		
		double latOrigin = origin.getLatitudeE6() / 1e6;
		double lonOrigin = origin.getLongitudeE6() / 1e6;
		double latDestination = destination.getLatitudeE6() / 1e6;
		double lonDestination = destination.getLongitudeE6() / 1e6;
		
		request += "&origin=" + latOrigin + "," + lonOrigin + "&destination=" + latDestination + "," + lonDestination;
		
		request += "&sensor=" + sensor;	
		return request;
	}
	
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
	// Code obtained November 9th 2012 2:54 AM from http://jeffreysambells.com/2010/05/27/decoding-polylines-from-google-maps-direction-api-with-java
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
	public void parseJSONResponse (String result)
	{
        // build JSON object
        try
        {
        	List<Overlay> mapOverlays = mapView.getOverlays();        	
        	JSONObject response = new JSONObject(result);        	
        	if (response.get("status").equals("OK"))
        	{
        		JSONArray routes = response.getJSONArray("routes");
        		for (int r = 0; r < routes.length(); r++)
        		{
        			JSONObject route = routes.getJSONObject(r);
        			JSONArray legs = route.getJSONArray("legs");
        			for (int l = 0; l < legs.length(); l++)
        			{
        				JSONObject leg = legs.getJSONObject(l);
        				JSONArray steps = leg.getJSONArray("steps");
        				for (int s = 0; s < steps.length(); s++)
        				{        					        					
        					// Render each polyline/path (includes smoothed/curved paths)
        					JSONObject step = steps.getJSONObject(s);        					        					      					        					         					        					
        					JSONObject polyline = step.getJSONObject("polyline");        					
        					List<GeoPoint> smoothedPath = decodePolyline(polyline.getString("points"));        					
        					int length = smoothedPath.size();        					
        					if (length > 1)
        					{
        						// Draw a line between two points (i.e. a step; the atomic unit of a route)
        						Overlay path;
        						int point;
        						for (point = 1; point < length; point++)
        						{
        							GeoPoint A = smoothedPath.get(point - 1), B = smoothedPath.get(point);
        							path = new PathOverlay(A, B);
        							directionsPolyline.add(path);
        							mapOverlays.add(path);          							
        						}
        						
        						// Scroll to the last point (destination point)
            					if (s == steps.length() - 1) 
            					{
            						// Create the destination marker and add it to the map
            						GeoPoint destination = smoothedPath.get(point - 1);
            						OverlayItem overlay = new OverlayItem(destination, "Destination", "You want to go here");			            						
            						campusBuildingsOverlay.addOverlay(overlay, getResources().getDrawable(R.drawable.blue_marker_resized));
            						campusBuildingsOverlay.commit();
            						mapOverlays.add(campusBuildingsOverlay);
            						mapController.animateTo(destination);
            					}
        					}        					        					        						      					        					        					
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
			
			// Build HTTP request for Google Directions
			String request = buildHTTPRequest(origin, destination, true);
			
	        // Set up asynchronous http client     
	        if (isNetworkAvailable())        
	        	new AsynchronousHTTP(mapView, this).execute(request);	// execute Request in background task        
	        else
	        {
				AlertDialog.Builder dialog = new AlertDialog.Builder(this);
				dialog.setTitle("Error!");
				dialog.setMessage("You are not connected to the Internet!");
				dialog.show();
	        }   						
		}
		else
		{
			AlertDialog.Builder dialog = new AlertDialog.Builder(this);
			dialog.setTitle("Nothing Saved");
			dialog.setMessage("You have not saved a location saved yet!");
			dialog.show();
		}			
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
