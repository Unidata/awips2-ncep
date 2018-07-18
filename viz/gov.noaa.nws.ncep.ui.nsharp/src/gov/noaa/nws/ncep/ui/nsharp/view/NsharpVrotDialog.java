package gov.noaa.nws.ncep.ui.nsharp.view;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class NsharpVrotDialog extends Dialog {
	private Text vrotText;
	private static float vrotValue;
	private MessageBox mb;
	private static NsharpVrotDialog thisDialog = null;
	private class InEditListener implements Listener {
		Object inType;
		public void handleEvent (Event e) {
			String string = e.text;
			char [] chars = new char [string.length ()];
			string.getChars (0, chars.length, chars, 0);
			if(inType instanceof Float){
				for (int i=0; i<chars.length; i++) {
					if (!('0' <= chars [i] && chars [i] <= '9') && ('.'!= chars [i])) {
						e.doit = false;
						return;
					}
				}
			} else if(inType instanceof Integer){
				for (int i=0; i<chars.length; i++) {
					if (!('0' <= chars [i] && chars [i] <= '9') ) {
						e.doit = false;
						return;
					}
				}
			}
			
		}

		public InEditListener( Object type) {
			super();
			inType = type;
		}
		
	}
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Enter Vrot Value");
		 mb = new MessageBox(newShell, SWT.ICON_WARNING
					| SWT.OK);

			mb.setMessage( "Input Error! ");
	}
	private boolean applyChanges(){
		String textStr = vrotText.getText();
		
		if((textStr != null) && !(textStr.isEmpty())){
			try{
				vrotValue = Float.parseFloat(textStr);
			}
			catch (NumberFormatException e){		
				mb.open();
				return false;
			}
			
		}
        return true;
	}
	@Override
	public void createButtonsForButtonBar(Composite parent) {
		Button appBtn = createButton(parent, IDialogConstants.INTERNAL_ID,
				"Apply", false);
		appBtn.addListener( SWT.MouseUp, new Listener() {
			public void handleEvent(Event event) {    
				applyChanges();
				close();
			}          		            	 	
		} );
		
		Button closeBtn = createButton(parent, IDialogConstants.CLOSE_ID, IDialogConstants.CLOSE_LABEL,
				true);
		closeBtn.addListener( SWT.MouseUp, new Listener() {
			public void handleEvent(Event event) {  
				close();
			}          		            	 	
		} );  
		
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite top;
		top = (Composite) super.createDialogArea(parent);
		vrotText = new Text(top, SWT.BORDER );
		vrotText.setEditable(true);
		vrotText.setVisible(true); 
		String vrotStr = Float.toString(vrotValue);
		vrotText.setText(vrotStr);
		//listen to Float data type, use 0f as input
		vrotText.addListener (SWT.Verify, new InEditListener (0f) );
		return null;
	}
	protected NsharpVrotDialog(Shell parShell) {
		super(parShell);
	}
	public static NsharpVrotDialog getInstance(Shell parShell) {

        if (thisDialog == null) {
            thisDialog = new NsharpVrotDialog(parShell);
        }
        return thisDialog;

    }
	public float getVrotValue() {
		return vrotValue;
	}
	
}
