package ca.uoguelph.cmer.geogryph;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Point;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;

public class CircleOverlay extends Overlay {
	
	private final GeoPoint campus_center = new GeoPoint(43529201, -80228713);

	public CircleOverlay ()
	{
//		super();
	}
	
	@Override
	public void draw (Canvas canvas, MapView mapView, boolean shadow)
	{
		super.draw(canvas, mapView, shadow);
		
		Projection projection = mapView.getProjection();
		Point point = projection.toPixels(campus_center, null);
		
		Paint paint = new Paint();
		paint.setAntiAlias(true);
		paint.setARGB(60, 255, 0, 0);
		paint.setStyle(Style.STROKE);
		canvas.drawCircle(point.x, point.y, 1000, paint);		
	}
}
