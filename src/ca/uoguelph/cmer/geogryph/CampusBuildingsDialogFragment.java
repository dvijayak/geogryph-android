package ca.uoguelph.cmer.geogryph;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

public class CampusBuildingsDialogFragment extends DialogFragment {
	
	// Activities that wish to use this dialog must implement this callback method
	public static interface Contract
	{
		public void addOverlay(int which);
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState)
	{		
		// Create the campus buildings list dialog
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.dialog_buildings_title);
		builder.setItems(R.array.buildings_snippet, 
				new DialogInterface.OnClickListener() 
				{				
					@Override
					public void onClick(DialogInterface dialog, int which) 
					{
						Contract contract = (Contract) getActivity();
						contract.addOverlay(which);						
					}
				}
		);		
		return builder.create();
	}		
}
