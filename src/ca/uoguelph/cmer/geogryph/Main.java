package ca.uoguelph.cmer.geogryph;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

public class Main extends MapActivity implements CampusBuildingsDialogFragment.Host
{
	// Map objects & parameters
	private GeoMyLocationOverlay me;
	private static MapView mapView;
	private GeoItemizedOverlay campusBuildingsOverlay;
	private GeoItemizedOverlay destinationOverlay; 
	private static MapController mapController;
	private final int minZoom = 12;
	private final int desiredZoom = 18;
	
	// Directions
	public String directionsResult;

	// Pre-defined locations
	private final GeoPoint stone_gordon = new GeoPoint(43526643, -80224733);
	private final GeoPoint campus_center = new GeoPoint(43529201, -80228713);

	// Campus buildings	
	protected final OverlayItem[] buildings = 
		{
			new OverlayItem(new GeoPoint(43533294, -80224637), "AC", "Athletics Centre"),
			new OverlayItem(new GeoPoint(43529492,-80229779), "ANNU", "Animal Science & Nutrition"),
			new OverlayItem(new GeoPoint(43529420,-80227644), "ALEX", "Alexander Hall"),
			new OverlayItem(new GeoPoint(43528690,-80226075), "AXEL", "Axelrod Building"),
			new OverlayItem(new GeoPoint(43528200,-80229038), "BIO", "Biodiversity Institute of Ontario")
		};
	private CampusBuildingsDialogFragment buildingsDialog; // Dialog for displaying list
	
	// Other objects	
	protected static GeoPoint savedLocation;
	
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
		try
		{
			mapController.animateTo(me.getMyLocation());
		}
		catch (java.lang.NullPointerException e)
		{
			mapController.animateTo(stone_gordon);
		}
        mapController.setZoom(desiredZoom);               
        List<Overlay> mapOverlays = mapView.getOverlays();
        
        // Overlays
        me = new GeoMyLocationOverlay(this, mapView);       
        mapView.postInvalidate();
        mapOverlays.add(me);
                       
        
        // Campus buildings        
        campusBuildingsOverlay = new GeoItemizedOverlay(this.getResources().getDrawable(R.drawable.university_resized), this, mapView);       
        mapOverlays.add(campusBuildingsOverlay);          
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
			plotDirections(me.getMyLocation(), savedLocation);
			return true;
		case R.id.menu_save:
			savedLocation = me.getMyLocation();
			AlertDialog.Builder dialog = new AlertDialog.Builder(this);
			dialog.setTitle("Saved");
			dialog.setMessage("Your current location has been saved!");
			dialog.show();
			return true;
		case R.id.menu_clear:
			if (!(mapView.getOverlays().isEmpty()))
			{
				mapView.getOverlays().clear();
				mapView.invalidate();
			}
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	// Implements the Host interface
	@Override
	public void addOverlay(int which) 
	{
		campusBuildingsOverlay.addOverlay(buildings[which]);
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
	
	protected static void parseJSONResponse (String result)
	{
        // build JSON object
        try
        {
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
        					JSONObject step = steps.getJSONObject(s);        					
        					System.out.println(step.toString());
        					
        					JSONObject startLocation = step.getJSONObject("start_location");
        					JSONObject endLocation = step.getJSONObject("end_location");
        					double startLat = startLocation.getDouble("lat")*1e6;
        					double startLon = startLocation.getDouble("lng")*1e6;
        					double endLat = endLocation.getDouble("lat")*1e6;
        					double endLon = endLocation.getDouble("lng")*1e6;
        					        					
        					GeoPoint A = new GeoPoint((int) startLat, (int) startLon);
        					GeoPoint B = new GeoPoint((int) endLat, (int) endLon);
        					Overlay path = new PathOverlay(A, B);
        					Main.mapView.getOverlays().add(path);     
//        					mapController.animateTo(B); // Guide through the path for added visual.
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
			// Scroll to user's current location
			mapController.animateTo(origin);
			// Create the destination marker and add it to the map
			OverlayItem overlay = new OverlayItem(destination, "Destination", "You want to go here");			
			destinationOverlay = new GeoItemizedOverlay(getResources().getDrawable(R.drawable.blue_marker_resized), this, mapView);
			destinationOverlay.addOverlay(overlay);
			mapView.getOverlays().add(destinationOverlay);
			
			// Build HTTP request for Google Directions
			String request = buildHTTPRequest(origin, destination, true);
			
	        // Set up asynchronous http client     
	        if (isNetworkAvailable())        
	        	new AsynchronousHTTP(mapView).execute(request);	// execute Request in background task        
	        else
	        {
				AlertDialog.Builder dialog = new AlertDialog.Builder(this);
				dialog.setTitle("Error!");
				dialog.setMessage("You are not connected to the Internet!");
				dialog.show();
	        }   						
		}
		else
			Log.v("GeoGryph app", "No saved location!");
	}
}
