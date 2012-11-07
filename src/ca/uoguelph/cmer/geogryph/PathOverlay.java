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
	
	public PathOverlay (GeoPoint A, GeoPoint B) 
	{
		this.A = A;
		this.B = B;
	}
	
	public void draw(Canvas canvas, MapView mapView, boolean shadow)
	{
		super.draw(canvas, mapView, shadow);
						
		if (shadow != true)			
		{
			Paint paint = new Paint();
			paint.setDither(true);
			paint.setARGB(153, 0, 0, 205);
			paint.setStyle(Paint.Style.FILL_AND_STROKE);
			paint.setStrokeJoin(Paint.Join.ROUND);
			paint.setStrokeCap(Paint.Cap.ROUND);
			paint.setStrokeWidth(20);
			
			Projection projection = mapView.getProjection();
			pA = projection.toPixels(A, null);
			pB = projection.toPixels(B, null);
			
			Path path = new Path();
			path.moveTo(pB.x, pB.y);
			path.lineTo(pA.x, pA.y);
			
			canvas.drawPath(path, paint);
		}
	}
}
