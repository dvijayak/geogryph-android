package ca.uoguelph.cmer.geogryph;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;

public class AboutDialogFragment extends DialogFragment {

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState)
	{				
		// Create the about the app dialog
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		
		// Get the layout inflater
		LayoutInflater inflater = getActivity().getLayoutInflater();		
		
		// Inflate and set the layout for the dialog
		// Pass null as the parent view because its going in the dialog layout				
		builder.setView(inflater.inflate(R.layout.dialog_about, null));						
		builder.setPositiveButton(R.string.dialog_about_positive,
				new DialogInterface.OnClickListener() 
				{					
					@Override
					public void onClick(DialogInterface dialog, int which) 
					{
						dialog.dismiss();
					}
				}
		);	
		
		return builder.create();
	}	
	
}
