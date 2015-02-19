package gov.noaa.nws.ncep.ui.nsharp.view;
/**
 * 
 * gov.noaa.nws.ncep.ui.nsharp.palette.NsharpParcelDialog
 * 
 * 
 * This code has been developed by the NCEP-SIB for use in the AWIPS2 system.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    	Engineer    Description
 * -------		------- 	-------- 	-----------
 * 03/23/2010	229			Chin Chen	Initial coding
 *
 * </pre>
 * 
 * @author Chin Chen
 * @version 1.0
 */


import gov.noaa.nws.ncep.ui.nsharp.display.NsharpEditor;
import gov.noaa.nws.ncep.ui.nsharp.display.rsc.NsharpResourceHandler;
import gov.noaa.nws.ncep.ui.nsharp.natives.NsharpNativeConstants;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.raytheon.uf.viz.core.exception.VizException;

public class NsharpParcelDialog extends Dialog {
	private static NsharpParcelDialog thisDialog=null;
	private String CUR_SFC = "Current Surface"; 
	private String FRCST_SFC = "Forecast Surface";
	private String MML= "Mean Mixing Layer";
	private String MUP = "Most Unstable Parcel";
	private String UDL=  "User Defined Level";
	private String EFF = "Mean Effective Layer";
	private static int userDefdParcelMb = 850; //default value
	private int btnWidth = 300;
	private int btnHeight = 20;
	private int labelGap = 20;
	private int btnGapX = 5;
	private int btnGapY = 5;
	private short curParcelType;
	private Button curSfcBtn,frcstBtn,effBtn,mmlBtn, mupBtn,udlBtn;
	private Text userDefdMbtext;	
	public static int getUserDefdParcelMb() {
		return userDefdParcelMb;
	}
	
	
	public void resetUserDefParcel(){
		userDefdParcelMb = 850;
	}
	public void reset(){
		userDefdParcelMb = 850;
		curParcelType = NsharpNativeConstants.PARCELTYPE_MOST_UNSTABLE;
	}
	public static NsharpParcelDialog getInstance( Shell parShell){

		if ( thisDialog == null ){
			try {
				thisDialog = new NsharpParcelDialog( parShell );
				//System.out.println("new parcel dialog INSTANCE created");
			} catch (VizException e) {
				e.printStackTrace();
			}

		}
		else {
			//System.out.println("current load dialog INSTANCE returned!");
		}

		return thisDialog;

	}

	public static NsharpParcelDialog getAccess() {
		return thisDialog;
	}
	protected NsharpParcelDialog(Shell parentShell) throws VizException {
		super(parentShell);
		thisDialog = this;
	}
	
	private void createDialogContents(Composite parent){
		
		final Group  btnGp = new Group(parent, SWT.SHADOW_ETCHED_IN );

		Listener radioGpLsner = new Listener() {
			public void handleEvent(Event event) { 
				Control [] children = btnGp.getChildren();
				for (int j=0; j<children.length; j++) {
					Control child = children [j];
					if (child instanceof Button) {
						Button button = (Button) child;
						if (button.getSelection()) {
							curParcelType = Short.parseShort(button.getData().toString());
							
							NsharpEditor editor = NsharpEditor.getActiveNsharpEditor();
							if(editor != null){
								NsharpResourceHandler skewtRsc = NsharpEditor.getActiveNsharpEditor().getRscHandler();
								if(skewtRsc!=null){
									skewtRsc.setCurrentParcel(curParcelType);
									//skewtRsc.setParcelList(parcelList);
									editor.refresh();
								}
							}
							break;// only one button should be selected, get out of here
						}
					}
				}
			}          		
		};
		curSfcBtn = new Button(btnGp, SWT.RADIO | SWT.BORDER);
		curSfcBtn.setText(CUR_SFC);
		curSfcBtn.setEnabled( true );
		curSfcBtn.setBounds(btnGp.getBounds().x+ btnGapX, btnGp.getBounds().y + labelGap, btnWidth,btnHeight);
		curSfcBtn.setData(NsharpNativeConstants.PARCELTYPE_OBS_SFC);
		curSfcBtn.addListener( SWT.MouseUp,radioGpLsner);
		
		frcstBtn = new Button(btnGp, SWT.RADIO | SWT.BORDER);
		frcstBtn.setText(FRCST_SFC);
		frcstBtn.setEnabled( true );
		frcstBtn.setBounds(btnGp.getBounds().x+ btnGapX, curSfcBtn.getBounds().y + curSfcBtn.getBounds().height+ btnGapY, btnWidth,btnHeight);
		frcstBtn.setData(NsharpNativeConstants.PARCELTYPE_FCST_SFC);
		frcstBtn.addListener( SWT.MouseUp,radioGpLsner);
		
		mmlBtn = new Button(btnGp, SWT.RADIO | SWT.BORDER);
		mmlBtn.setText(MML);
		mmlBtn.setEnabled( true );
		mmlBtn.setBounds(btnGp.getBounds().x+ btnGapX, frcstBtn.getBounds().y + frcstBtn.getBounds().height+ btnGapY, btnWidth,btnHeight);
		mmlBtn.setData(NsharpNativeConstants.PARCELTYPE_MEAN_MIXING);
		mmlBtn.addListener( SWT.MouseUp,radioGpLsner);
		mupBtn = new Button(btnGp, SWT.RADIO | SWT.BORDER);
		mupBtn.setText(MUP);
		mupBtn.setEnabled( true );
		mupBtn.setBounds(btnGp.getBounds().x+ btnGapX, mmlBtn.getBounds().y + mmlBtn.getBounds().height+ btnGapY, btnWidth,btnHeight);
		mupBtn.setData(NsharpNativeConstants.PARCELTYPE_MOST_UNSTABLE);
		mupBtn.addListener( SWT.MouseUp,radioGpLsner);
		effBtn = new Button(btnGp, SWT.RADIO | SWT.BORDER);
		effBtn.setText(EFF);
		effBtn.setEnabled( true );
		effBtn.setBounds(btnGp.getBounds().x+ btnGapX, mupBtn.getBounds().y + mupBtn.getBounds().height+ btnGapY, btnWidth,btnHeight);
		effBtn.setData(NsharpNativeConstants.PARCELTYPE_EFF);
		effBtn.addListener( SWT.MouseUp,radioGpLsner);
		
		udlBtn = new Button(btnGp, SWT.RADIO | SWT.BORDER);
		udlBtn.setText(UDL);
		udlBtn.setEnabled( true );
		udlBtn.setBounds(btnGp.getBounds().x+ btnGapX, effBtn.getBounds().y + effBtn.getBounds().height+ btnGapY, btnWidth,btnHeight);
		udlBtn.setData(NsharpNativeConstants.PARCELTYPE_USER_DEFINED);
		udlBtn.addListener( SWT.MouseUp,radioGpLsner);
		
		udlBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				//when CR is entered, this fcn is called. 
				//do nothing here. 
				//system will call okPressed() next. 
				//System.out.println("UDL selectied");
			}
		});
		
		userDefdMbtext = new Text (btnGp, SWT.BORDER | SWT.SINGLE);
		userDefdMbtext.setBounds (btnGp.getBounds().x+ btnGapX, udlBtn.getBounds().y + udlBtn.getBounds().height+ btnGapY, btnWidth/4,btnHeight);
		userDefdMbtext.setText(Integer.toString(userDefdParcelMb));
		userDefdMbtext.setEnabled(true);
		userDefdMbtext.setVisible(true);

		//to make sure user enter digits only
		userDefdMbtext.addListener (SWT.Verify, new Listener () {
			public void handleEvent (Event e) {
				String string = e.text;
				char [] chars = new char [string.length ()];
				string.getChars (0, chars.length, chars, 0);
				
				for (int i=0; i<chars.length; i++) {
					if (!('0' <= chars [i] && chars [i] <= '9')) {
						e.doit = false;
						return;
					}
				}
				
			}
		});
		NsharpResourceHandler skewtRsc = NsharpEditor.getActiveNsharpEditor().getRscHandler();
		if(skewtRsc!=null){
			curParcelType = skewtRsc.getCurrentParcel();
			switch(curParcelType){
			case NsharpNativeConstants.PARCELTYPE_OBS_SFC:
				curSfcBtn.setSelection(true);
				break;
			case NsharpNativeConstants.PARCELTYPE_EFF:
				effBtn.setSelection(true);
				break;
			case NsharpNativeConstants.PARCELTYPE_FCST_SFC:
				frcstBtn.setSelection(true);
				break;
			case NsharpNativeConstants.PARCELTYPE_MEAN_MIXING:
				mmlBtn.setSelection(true);
				break;
			case NsharpNativeConstants.PARCELTYPE_MOST_UNSTABLE:
				mupBtn.setSelection(true);
				break;
			case NsharpNativeConstants.PARCELTYPE_USER_DEFINED:
				udlBtn.setSelection(true);
				break;
			default:
				break;
			}
		}

	}
	@Override
	public void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		Button okBtn = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,
				true);
		okBtn.addListener( SWT.MouseUp, new Listener() {
			public void handleEvent(Event event) {  
				//System.out.println("OK listener is called");
				String textStr = userDefdMbtext.getText();
				if((textStr != null) && !(textStr.isEmpty())){
					userDefdParcelMb = Integer.decode(textStr);
					//System.out.println(userDefdParcelMb);
				}
				NsharpResourceHandler skewtRsc = NsharpEditor.getActiveNsharpEditor().getRscHandler();
				skewtRsc.setCurrentParcel(curParcelType);
				close();
			}          		            	 	
		} );  

		Button canBtn = createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CLOSE_LABEL, false);
		canBtn.addListener( SWT.MouseUp, new Listener() {
			public void handleEvent(Event event) {    
				close();
			}          		            	 	
		} );  
	}


	//@Override
	//This function name is miss leading....
	//This function is called when CR is preseed, but NOT "ok" button.
	//Override this and move close() from here to OK button Listener 
	//So, we only close when "OK" is pressed, not "CR". 
	public void okPressed() {
		//System.out.println("CR is pressed");
		setReturnCode(OK);
		//close();
	}
	
	/*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets.Shell)
       */
    @Override   
    protected void configureShell( Shell shell ) {
        super.configureShell( shell );       
        shell.setText( "Parcels Display Configuration" );
        
    }
	@Override
	public Control createDialogArea(Composite parent) {
		Composite top;
		top = (Composite) super.createDialogArea(parent);

		// Create the main layout for the shell.
		GridLayout mainLayout = new GridLayout(1, false);
		mainLayout.marginHeight = 3;
		mainLayout.marginWidth = 3;
		top.setLayout(mainLayout);

		// Initialize all of the menus, controls, and layouts
		createDialogContents(top);

		return top;
	}   

	
	@Override
    public int open( ) {
        //System.out.println("parcel dialog opened");
        
        if ( this.getShell() == null ){
			this.create();
		}
   	    this.getShell().setLocation(this.getShell().getParent().getLocation().x+1100,
   	    		this.getShell().getParent().getLocation().y+200);
   	    return super.open();
    	
    }
	@Override
	public boolean close() {
		//System.out.println("parcel close called");
		return (super.close());
    }

}
