package ca.uoguelph.cmer.geogryph;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

public class Main extends MapActivity {

	@SuppressLint({ "ParserError", "ParserError", "ParserError", "ParserError", "ParserError", "ParserError" })
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        MapView mapView = (MapView) findViewById(R.id.mapview);
        mapView.setBuiltInZoomControls(true);
        
        // Actual work begins
        List<Overlay> mapOverlays = mapView.getOverlays();
        Drawable drawable = this.getResources().getDrawable(R.drawable.me);
        GeoItemizedOverlay itemizedOverlay = new GeoItemizedOverlay(drawable, this);
        
        List<GeoPoint> points = new ArrayList<GeoPoint>(); 
        points.add(new GeoPoint(19240000, -99120000));
        points.add(new GeoPoint(35410000, 139460000));
        List<OverlayItem> overlayItems = new ArrayList<OverlayItem>();
        overlayItems.add(new OverlayItem(points.get(0), "Hola, Mundo!", "I'm in Mexico City!"));
        overlayItems.add(new OverlayItem(points.get(1), "Sekai, konichiwa!", "I'm in Japan!"));
        for (OverlayItem item : overlayItems)
        	itemizedOverlay.addOverlay(item);        
        mapOverlays.add(itemizedOverlay);
    }

	@Override
	protected boolean isRouteDisplayed() { 
		return false;
	}

    
}
