package ca.uoguelph.cmer.geogryph;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;

import android.content.Context;
import android.os.AsyncTask;
import android.view.View;
import android.widget.ProgressBar;

import com.google.android.maps.MapView;

public class AsynchronousHTTP extends AsyncTask<String, Void, String> 
{			
	private final WeakReference<MapView> mapViewReference;
	private final WeakReference<Context> mainActivityReference;
	
	private ProgressBar progressBar;
	
	public static interface Contract
	{
		public void parseJSONResponse (String result);		
	}
	
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
	protected String doInBackground (String... urls) 
	{			
		String output = null;
		try
		{
			URL url = new URL(urls[0]); // The first url is the actual (and only url)
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			
			// Read the stream
			BufferedReader br = null; 
			InputStream is = null;						
			try
			{				
				br = new BufferedReader(new InputStreamReader(con.getInputStream()));
				String line = "";
				StringBuilder result = new StringBuilder();	// StringBuilder.append performs much better than concatenating Strings			
				while ((line = br.readLine()) != null)				
					result.append(line); // Concatenate all input strings into one line								
				
				output = result.toString();
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
				
				if (is != null)
				{
					try
					{
						is.close();						
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

	@Override
	protected void onPostExecute (String output)
	{							
		if (!isCancelled())
		{			
			if (mapViewReference != null)
			{
				MapView mapView = mapViewReference.get();
				if (mapView != null)
				{																					
					String result = output.toString();
					Context context = mainActivityReference.get();
					Contract contract = (Contract) context;
					contract.parseJSONResponse(result);						
				}						
			}
		}
		progressBar.setVisibility(View.INVISIBLE);		
	}
}
