package ca.uoguelph.cmer.geogryph;

import java.util.List;

import android.annotation.TargetApi;
import android.graphics.drawable.Drawable;
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

public class Main extends MapActivity 
{
	// Map objects & parameters
	private GeoMyLocationOverlay me;
	private MapController mapController;
	private final int minZoom = 12;
	private final int desiredZoom = 18;

	// Pre-defined locations
	private final GeoPoint stone_gordon = new GeoPoint(43526643, -80224733);
	private final GeoPoint campus_center = new GeoPoint(43529201, -80228713);

	// Campus buildings	
	private final OverlayItem[] buildings = 
		{
			new OverlayItem(new GeoPoint(43533294, -80224637), "AC", "Athletics Centre"),
			new OverlayItem(new GeoPoint(43529492,-80229779), "ANNU", "Animal Science & Nutrition"),
			new OverlayItem(new GeoPoint(43529420,-80227644), "ALEX", "Alexander Hall"),
			new OverlayItem(new GeoPoint(43528690,-80226075), "AXEL", "Axelrod Building"),
			new OverlayItem(new GeoPoint(43528200,-80229038), "BIO", "Biodiversity Institute of Ontario")
		};
	    
	@Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
//        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE); // location services        
        
        // Initializing the map
        MapView mapView = (MapView) findViewById(R.id.mapview);
        mapView.preLoad();
        mapView.setBuiltInZoomControls(true);
        mapController = mapView.getController(); // Controller allows to set the map zoom and map center
        mapController.setCenter(stone_gordon);
        mapController.setZoom(desiredZoom);               
        List<Overlay> mapOverlays = mapView.getOverlays();
        
        // Overlays
        me = new GeoMyLocationOverlay(this, mapView);       
        mapView.postInvalidate();
        mapOverlays.add(me);
                       
        
        // Campus buildings
        Drawable red_university = this.getResources().getDrawable(R.drawable.university);
        GeoItemizedOverlay campusBuildingsOverlay = new GeoItemizedOverlay(red_university, this, mapController);
        for (OverlayItem item : buildings)
        	campusBuildingsOverlay.addOverlay(item);        
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
			mapController.setCenter(me.getMyLocation());			
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

    
}
