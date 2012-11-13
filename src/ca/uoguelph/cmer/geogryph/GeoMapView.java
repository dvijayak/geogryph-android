package ca.uoguelph.cmer.geogryph;

import android.graphics.Canvas;

import com.google.android.maps.MapView;

public class GeoMapView extends MapView {
	
	public GeoMapView(android.content.Context context, android.util.AttributeSet attrs) 
	{
		super(context, attrs);
	}

	public GeoMapView(android.content.Context context, android.util.AttributeSet attrs, int defStyle) 
	{
		super(context, attrs, defStyle);
	}

	public GeoMapView(android.content.Context context, java.lang.String apiKey) 
	{
		super(context, apiKey);
	}
	
	@Override
	public void dispatchDraw (Canvas canvas)
	{
		super.dispatchDraw(canvas);
		
		int currentZoom = getZoomLevel();
		
		
		if (currentZoom < Main.minZoom)
		{
			getController().setZoom(Main.minZoom);
		}
	}
}
