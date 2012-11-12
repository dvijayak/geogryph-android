package ca.uoguelph.cmer.geogryph;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.location.Location;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;

public class GeoMyLocationOverlay extends MyLocationOverlay {
	
    private Drawable marker;       
    private Point point = new Point();
    private Rect rect = new Rect();
	
	public GeoMyLocationOverlay(Context context, MapView mapView) 
	{
		super(context, mapView);		
		marker = context.getResources().getDrawable((R.drawable.me_resized));
		point = new Point();
		rect = new Rect();
        // Set the bounds of the drawn marker
        rect.left = -(marker.getIntrinsicWidth() / 2);
        rect.top = -(marker.getIntrinsicHeight());
        rect.right = marker.getIntrinsicWidth() / 2;
        rect.bottom = 0;       
        marker.setBounds(rect);
	}
	
	@Override
	public synchronized boolean draw (Canvas canvas, MapView mapView, boolean shadow, long when)
	{
		if (getLastFix() == null)
			return super.draw(canvas, mapView, shadow, when);
		
		if (marker != null)
		{		
			GeoPoint lastFix = new GeoPoint((int)(getLastFix().getLatitude() * 1e6), (int)(getLastFix().getLongitude() * 1e6));
	 		// Translate the GeoPoint of the user's location to screen pixels
			mapView.getProjection().toPixels(lastFix, point);
			drawAt(canvas, marker, point.x, point.y, shadow);
		}
		else if (getMyLocation() != null);		
			drawMyLocation(canvas, mapView, getLastFix(), getMyLocation(), when);
				
		return super.draw(canvas, mapView, shadow, when);
	}

 	@Override
    protected void drawMyLocation(Canvas canvas, MapView mapView, Location lastFix, GeoPoint myLocation, long when) 
 	{  		         	
        mapView.getProjection().toPixels(myLocation, point);       
        
        // Draw the marker on the canvas
        marker.draw(canvas);         
    } 

}
