package ca.uoguelph.cmer.geogryph;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.graphics.drawable.Drawable;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;

public class GeoItemizedOverlay extends ItemizedOverlay<OverlayItem> 
{

	private ArrayList<OverlayItem> overlays = new ArrayList<OverlayItem>();
	private Context context;
	private MapView mapView;
	private MapController mapController;	
	
	public GeoItemizedOverlay (Drawable defaultMarker, Context context, MapView mapView) 
	{
		super(boundCenterBottom(defaultMarker));		
		this.context = context;
		this.mapView = mapView;
		this.mapController = mapView.getController();
		populate();
	}
	
	public void addOverlay (OverlayItem overlay)
	{
		GeoPoint overlayPoint = overlay.getPoint();
		int newLat = overlayPoint.getLatitudeE6();
		int newLon = overlayPoint.getLongitudeE6();
		Editor editor = Main.persistentPrimitives.edit();
		editor.putInt("lat", newLat);
		editor.putInt("lon", newLon);
		editor.commit(); // Do not forget to commit the edits!
		
		mapController.animateTo(overlayPoint);		
		// Only create the new overlay if it does not exist
		if (!overlays.contains(overlay))
		{
			overlays.add(overlay);		
			populate();
		}
	}	
	
	@Override
	public int size () 
	{
		return overlays.size();
	}		

	@Override
	protected OverlayItem createItem (int i) 
	{		
		return overlays.get(i);
	}
	
	@Override
	protected boolean onTap (int index)
	{
		OverlayItem overlay = overlays.get(index);		
		GeoPoint currentCenter = mapView.getMapCenter();
		GeoPoint newCenter = overlay.getPoint();
		int newLat = newCenter.getLatitudeE6();
		int newLon = newCenter.getLongitudeE6();
		Editor editor = Main.persistentPrimitives.edit();
		editor.putInt("lat", newLat);
		editor.putInt("lon", newLon);		
		editor.commit(); // Do not forget to commit the edits!
		
		// If already centered, pop up dialog
		if (currentCenter.equals(newCenter))
		{
			AlertDialog.Builder dialog = new AlertDialog.Builder(context);
			dialog.setTitle(overlay.getTitle());
			dialog.setMessage(overlay.getSnippet());
			dialog.show();
		}
		// else, center on the marker
		else
			mapController.animateTo(newCenter); // Pans smoothly to the point and sets it as the map center
		return true;
	}
	
	public void clear ()
	{
		overlays.clear();
		populate();
	}
}
