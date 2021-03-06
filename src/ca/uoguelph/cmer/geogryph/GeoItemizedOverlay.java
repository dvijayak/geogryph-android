package ca.uoguelph.cmer.geogryph;

import java.util.HashMap;
import java.util.Map;

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
	private Map<String, OverlayItem> overlays = new HashMap<String, OverlayItem>();
	private Context context;
	private MapView mapView;
	private MapController mapController;	
	
	public GeoItemizedOverlay (Drawable defaultMarker, Context context, MapView mapView) 
	{
		super(boundCenterBottom(defaultMarker));		
		this.context = context; // Activity that this belongs to
		this.mapView = mapView;
		this.mapController = mapView.getController();	
		populate();
	}
	
	// Obtain the key of the overlay item (concatenation of title and snippet)
	private static String getKey (OverlayItem overlay)
	{
		if (overlay != null)		
			return overlay.getTitle() + overlay.getSnippet();		
		else
			return null;
	}
	
	public void addOverlay (OverlayItem overlay, Drawable marker)
	{		
		// Assign an alternate marker if provided; else the default is used
		if (marker != null)
			overlay.setMarker(boundCenterBottom(marker));				
		overlays.put(getKey(overlay), overlay);
		
		// Restrict the map zoom level to the specified minimum/desired zoom level
		int currentZoom = mapView.getZoomLevel();
		if (currentZoom < Main.desiredZoom)				
			mapController.setZoom(Main.desiredZoom);
		
		snapToMarker(overlay, false);
	}	
	
	public void addPOIOverlay (OverlayItem overlay, Drawable marker, String key)
	{										
		// Assign an alternate marker if provided; else the default is used
		if (marker != null)
			overlay.setMarker(boundCenterBottom(marker));				
		overlays.put(key, overlay);
		snapToMarker(overlay, false);
	}
	
	public void changeOverlayMarker (String key, Drawable marker)
	{
		if (containsOverlay(key))
		{
			OverlayItem overlay = overlays.get(key);
			if (marker != null)
				overlay.setMarker(boundCenterBottom(marker));
			else
				overlay.setMarker(null);
			mapView.invalidate();
		}
	}
	
	public void changeOverlaySnippet (String key, String snippet)
	{
		if (containsOverlay(key))
		{			
			// First, remove the overlay
			OverlayItem overlay = overlays.get(key);
			GeoPoint point = overlay.getPoint();
			String title = overlay.getTitle();
			String oldSnippet = overlay.getSnippet();				
			removeOverlay(overlay);			
			
			// Recreate the overlay with the new snippet
			if (snippet == null) snippet = new String();
			if (oldSnippet.length() > 0)
				snippet += "\n\n" + oldSnippet;			
			OverlayItem newOverlay = new OverlayItem(point, title, snippet);
			addPOIOverlay(newOverlay, null, key);			
		}
	}
	
	public boolean containsOverlay (OverlayItem overlay)
	{		
		return (overlay != null) ? overlays.containsKey(getKey(overlay)) : null;
	}
	
	public boolean containsOverlay(String key)
	{
		return (key != null) ? overlays.containsKey(key) : null;
	}
	
	public void removeOverlay (OverlayItem overlay)
	{
		if (overlay != null)		
			if (containsOverlay(overlay))
				overlays.remove(getKey(overlay));	
	}
	
	@Override
	public int size () 
	{
		return overlays.size();
	}		

	@Override
	protected OverlayItem createItem (int i) 
	{		
		Object[] array = overlays.values().toArray();
		OverlayItem overlay = (OverlayItem) array[i];
		return overlay;
	}
	
	@Override
	protected boolean onTap (int index)
	{
		Object[] array = overlays.values().toArray();
		OverlayItem overlay = (OverlayItem) array[index];		
		snapToMarker(overlay, true);
		return true;
	}		
	
	public void clear ()
	{
		overlays.clear();		
		populate();
		mapView.invalidate();
	}
	
	// Populate only when all insertions/deletions in a batch have been performed 
	public void commit ()
	{
		populate();
	}

	private void snapToMarker (OverlayItem overlay, boolean showInfo) 
	{
		GeoPoint currentCenter = mapView.getMapCenter();
		GeoPoint newCenter = overlay.getPoint();
		int newLat = newCenter.getLatitudeE6();
		int newLon = newCenter.getLongitudeE6();
		Editor editor = Main.persistentPrimitives.edit();
		editor.putInt("lat", newLat);
		editor.putInt("lon", newLon);		
		editor.commit(); // Do not forget to commit the edits!
		
		// Produce dialog with information on the location
		if (showInfo)
			Main.produceAlertDialog(context, overlay.getTitle(), overlay.getSnippet());
		// Center on marker
		if (!currentCenter.equals(newCenter))		
			mapController.animateTo(newCenter); // Pans smoothly to the point and sets it as the map center					
	}		
}
