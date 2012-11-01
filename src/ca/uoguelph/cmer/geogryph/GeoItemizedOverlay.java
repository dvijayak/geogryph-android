package ca.uoguelph.cmer.geogryph;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.Drawable;

import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.OverlayItem;

public class GeoItemizedOverlay extends ItemizedOverlay 
{

	private ArrayList<OverlayItem> overlays = new ArrayList<OverlayItem>();
	private Context context;
	
	@SuppressLint("ParserError")
	public GeoItemizedOverlay (Drawable defaultMarker, Context newContext) 
	{
		super(boundCenterBottom(defaultMarker));		
		context = newContext;
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
		
		AlertDialog.Builder dialog = new AlertDialog.Builder(context);
		dialog.setTitle(overlay.getTitle());
		dialog.setMessage(overlay.getSnippet());
		dialog.show();
		return true;
	}

	public void addOverlay (OverlayItem overlay)
	{
		overlays.add(overlay);
		populate();
	}

}
