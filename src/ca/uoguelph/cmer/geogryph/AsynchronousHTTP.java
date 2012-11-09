package ca.uoguelph.cmer.geogryph;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.maps.MapView;

public class AsynchronousHTTP extends AsyncTask<String, Void, String> 
{
	
	protected String result;
	
	private final WeakReference<MapView> mapViewReference;
	private final WeakReference<Context> mainActivityReference;
	
	public static interface Contract
	{
		public void parseJSONResponse(String result);
	}
	
	public AsynchronousHTTP (MapView mapView, Context context)
	{
		this.mapViewReference = new WeakReference<MapView>(mapView);
		this.mainActivityReference = new WeakReference<Context>(context);
	}
	
	// Actual download method; run in the background task thread
	@Override	
	protected String doInBackground(String... urls) 
	{			
		return queryHTTPServer(urls[0]); // The first url is the actual (and only url)
	}

	@Override
	protected void onPostExecute (String result)
	{
		if (isCancelled())
		{
			result = null;
		}
		
		if (mapViewReference != null)
		{
			MapView mapView = mapViewReference.get();
			if (mapView != null)
			{
//				Log.v("Asynchro", result);
				Context context = mainActivityReference.get();
				Contract contract = (Contract) context;
				contract.parseJSONResponse(result);				
			}
					
		}
	}

	private String queryHTTPServer (String urlString)
	{					
		try
		{
			URL url = new URL(urlString);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			
			// Read the stream
			BufferedReader br = null;			
			try
			{
				br = new BufferedReader(new InputStreamReader(con.getInputStream()));
				String line = "";
				StringBuilder result = new StringBuilder();	// StringBuilder.append performs much better than concatenating Strings			
				while ((line = br.readLine()) != null)				
					result.append(line); // Concatenate all input strings into one line									
				return result.toString();
			}
			catch (IOException ioe)
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
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}		
		return null;
	}
}
