package ca.uoguelph.cmer.geogryph;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;

public class PathOverlay extends Overlay {
				
	private GeoPoint[] pathGeoPoints; 
		
	private Point pathPoints[];
	private float projectedPoints[];
	
	private Paint paint;	
	
	public PathOverlay(GeoPoint[] geoPoints, MapView mapView) 
	{
		pathGeoPoints = geoPoints;
		pathPoints = new Point[geoPoints.length];
		
		paint = new Paint();
		paint.setDither(true);
		paint.setARGB(180, 200, 0, 200);
		paint.setStyle(Paint.Style.FILL_AND_STROKE);
		paint.setStrokeWidth(8);			
	}

	public void draw(Canvas canvas, MapView mapView, boolean shadow)
	{					
		if (shadow == false)			
		{								
			Projection projection = mapView.getProjection();
			
			int length = pathGeoPoints.length;
			projectedPoints = new float[length*4]; // Make room for from.x, from.y, to.x, to.y
			for (int g = 0, p = 1; g < length; g++, p += 2)		
			{
				pathPoints[g] = projection.toPixels(pathGeoPoints[g], null);
				
				if (g % 2 != 0)
				{					
					projectedPoints[p] = pathPoints[g].y;
					projectedPoints[p-1] = pathPoints[g].x;
					projectedPoints[p-2] = pathPoints[g-1].y;
					projectedPoints[p-3] = pathPoints[g-1].x;	
				}
			}	
			
			canvas.drawLines(projectedPoints, paint);			
		}
	}
}
