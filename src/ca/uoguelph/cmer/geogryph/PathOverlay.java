package ca.uoguelph.cmer.geogryph;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;

public class PathOverlay extends Overlay {
	
	private GeoPoint A;
	private GeoPoint B;
	
	private Point pA;
	private Point pB;
	
	private Paint paint;	
	
	public PathOverlay (GeoPoint A, GeoPoint B) 
	{
		this.A = A;
		this.B = B;	
		
		paint = new Paint();
		paint.setDither(true);
		paint.setARGB(180, 200, 0, 200);
		paint.setStyle(Paint.Style.FILL_AND_STROKE);
		paint.setStrokeWidth(8);
	}
	
	public void draw(Canvas canvas, MapView mapView, boolean shadow)
	{					
		if (shadow != false)			
		{								
			Projection projection = mapView.getProjection();
			pA = projection.toPixels(A, null);
			pB = projection.toPixels(B, null);

			canvas.drawLine(pA.x, pA.y, pB.x, pB.y, paint);
		}
	}
}
