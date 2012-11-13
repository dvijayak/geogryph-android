package ca.uoguelph.cmer.geogryph;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.view.View;
import android.widget.ProgressBar;

import com.google.android.maps.MapView;

public class AsynchronousHTTP extends AsyncTask<String, Void, Object> 
{			
	private final WeakReference<MapView> mapViewReference;
	private final WeakReference<Context> mainActivityReference;
	
	private ProgressBar progressBar;
	
	private String overlayKey = null;
	
	public AsynchronousHTTP (MapView mapView, Context context)
	{
		mapViewReference = new WeakReference<MapView>(mapView);
		mainActivityReference = new WeakReference<Context>(context);
		progressBar = (ProgressBar) ((Main) context).findViewById(R.id.progressbar);
	}

	@Override
	protected void onPreExecute ()
	{ 		
		progressBar.setVisibility(View.VISIBLE);
	}
	
	// Query the HTTP server and receive response; perform this in background
	@Override	
	protected Object doInBackground (String... urls) 
	{
		// Note: The first url (urls[0]) is the actual (and only url)		
		
		Object output = null;
		try
		{			
			String trueUrl;
			
			// Check if the request url contains an overlay key prefix 
			String[] parsedUrl = urls[0].split("\\|"); 
			if (parsedUrl.length > 1)
			{
				overlayKey = parsedUrl[0];
				trueUrl = parsedUrl[1];
			}
			else
				trueUrl = urls[0];
			
			URL url = new URL(trueUrl); 
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			
			// Read the stream
			BufferedReader br = null; 
			BufferedInputStream bis = null;						
			try
			{			
				// Expecting an image (PNG) download
				if (trueUrl.endsWith(".png"))
				{
//					bis = new BufferedInputStream(con.getInputStream());
//					byte[] response =
					Bitmap bitmap = BitmapFactory.decodeStream((InputStream)con.getInputStream());
					output = bitmap;
//					br = new BufferedReader(new InputStreamReader(con.getInputStream()));
//					String line = "";
//					StringBuilder result = new StringBuilder();	// StringBuilder.append performs much better than concatenating Strings			
//					while ((line = br.readLine()) != null)				
//						result.append(line); // Concatenate all input strings into one line								
//					
//					output = result.toString();	
				}
				// ...else, the request is expected to return a String response
				else
				{
					br = new BufferedReader(new InputStreamReader(con.getInputStream()));
					String line = "";
					StringBuilder result = new StringBuilder();	// StringBuilder.append performs much better than concatenating Strings			
					while ((line = br.readLine()) != null)				
						result.append(line); // Concatenate all input strings into one line								
					
					output = result.toString();	
				}				
			}
			catch (Exception ioe)
			{
				ioe.printStackTrace();
			}
			finally
			{
				if (br != null)
				{
					try
					{
						br.close();
					}
					catch (IOException ioe)
					{
						ioe.printStackTrace();
					}
				}
				
				if (bis != null)
				{
					try
					{
						bis.close();						
					}
					catch (IOException ioe)
					{
						ioe.printStackTrace();
					}
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}		
		return output;		
	}
	
	public static interface Contract
	{
		public void parseJSONResponse (String result);		
	}

	@Override
	protected void onPostExecute (Object output)
	{							
		if (!isCancelled())
		{			
			if (mapViewReference != null && mainActivityReference != null && output != null)
			{
				MapView mapView = mapViewReference.get();
				Context context = mainActivityReference.get();
				if (mapView != null)
				{		
					Class<? extends Object> dataType = output.getClass();
					Contract contract = (Contract) context;
							
					// Handle the output separately depending on the type returned
					if (dataType.equals(String.class))
					{															;
						contract.parseJSONResponse(output.toString());	
					}										
					else if (dataType.equals(Bitmap.class))
					{						
						((Main) context).markersOverlay.changeOverlayMarker(overlayKey, new BitmapDrawable((Bitmap)output));
					}						
				}						
			}
		}
		progressBar.setVisibility(View.INVISIBLE);		
	}
}
