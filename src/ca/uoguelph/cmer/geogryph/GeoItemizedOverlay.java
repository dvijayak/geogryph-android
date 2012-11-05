package ca.uoguelph.cmer.geogryph;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.Drawable;

import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapController;
import com.google.android.maps.OverlayItem;

public class GeoItemizedOverlay extends ItemizedOverlay 
{

	private ArrayList<OverlayItem> overlays = new ArrayList<OverlayItem>();
	private Context context;
	private MapController mapController;	
	
	public GeoItemizedOverlay (Drawable defaultMarker, Context newContext, MapController newMapController) 
	{
		super(boundCenterBottom(defaultMarker));		
		context = newContext;
		mapController = newMapController;
	}

	public void addOverlay (OverlayItem overlay)
	{
		overlays.add(overlay);
		populate();
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
		
		// Center on the marker
		mapController.setCenter(overlay.getPoint());		
		AlertDialog.Builder dialog = new AlertDialog.Builder(context);
		dialog.setTitle(overlay.getTitle());
		dialog.setMessage(overlay.getSnippet());
		dialog.show();
		return true;
	}
}
