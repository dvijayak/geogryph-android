package ca.uoguelph.cmer.geogryph;

import java.util.HashMap;
import java.util.Map;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.graphics.drawable.Drawable;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;

public class GeoItemizedOverlay extends ItemizedOverlay<OverlayItem> implements Centerable
{

//	private ArrayList<OverlayItem> overlays = new ArrayList<OverlayItem>();
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
	
	private static String getKey (OverlayItem overlay)
	{
		if (overlay != null)
		{
			return overlay.getTitle() + overlay.getSnippet();
		}
		else
			return null;
	}
	
	public void addOverlay (OverlayItem overlay, Drawable marker)
	{		
		// Assign an alternate marker if provided; else the default is used
		if (marker != null)
			overlay.setMarker(boundCenterBottom(marker));
		overlays.put(getKey(overlay), overlay);		
		snapToMarker(overlay);
	}	
	
	public boolean containsOverlay (OverlayItem overlay)
	{		
		return (overlay != null) ? overlays.containsKey(getKey(overlay)) : null;
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
		snapToMarker(overlay);
		return true;
	}		
	
	public void clear ()
	{
		overlays.clear();
		populate();
	}
	
	// Populate only when all insertions/deletions in a batch have been performed 
	public void commit ()
	{
		populate();
	}

	@Override
	public void snapToMarker(OverlayItem overlay) 
	{
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
	}
}
