package gov.noaa.nws.ncep.ui.nsharp.view;
/**
 * 
 * gov.noaa.nws.ncep.ui.nsharp.view.NsharpGridDataConfigDialog
 * 
 * 
 * This code has been developed by the NCEP-SIB for use in the AWIPS2 system.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    	Engineer    Description
 * -------		------- 	-------- 	-----------
 * 03/09/2015   RM#6674     Chin Chen   Initial coding. support model sounding query data interpolation and nearest point option                       
 *
 * </pre>
 * 
 * @author Chin Chen
 * @version 1.0
 */


import gov.noaa.nws.ncep.ui.nsharp.NsharpConfigManager;
import gov.noaa.nws.ncep.ui.nsharp.NsharpConfigStore;
import gov.noaa.nws.ncep.ui.nsharp.NsharpConstants;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import com.raytheon.uf.viz.core.exception.VizException;

public class NsharpGridDataConfigDialog extends Dialog {
	private static NsharpGridDataConfigDialog thisDialog=null;
	private NsharpConfigStore configStore=null;
	private NsharpConfigManager mgr;
	private boolean gridInterpolation;

	public static NsharpGridDataConfigDialog getInstance( Shell parShell){

		if ( thisDialog == null ){
			try {
				thisDialog = new NsharpGridDataConfigDialog( parShell );
				
			} catch (VizException e) {
				e.printStackTrace();
			}

		}

		return thisDialog;

	}
	public NsharpGridDataConfigDialog(Shell parentShell) throws VizException {
		super(parentShell);
		thisDialog = this;
		mgr =NsharpConfigManager.getInstance();
		configStore = mgr.retrieveNsharpConfigStoreFromFs();
		if(configStore != null){
			gridInterpolation = configStore.getGraphProperty().isGridInterpolation();
		}
		else
			gridInterpolation = true; //by default
	}
	
	private void updateCfgStore() throws VizException{
		if(configStore != null){
			configStore.getGraphProperty().setGridInterpolation(gridInterpolation);
		}
	}
	private void saveCfgStore() throws VizException{
		if(configStore != null){
			configStore.getGraphProperty().setGridInterpolation(gridInterpolation);
			mgr.saveConfigStoreToFs(configStore);
		}
	}
	@Override
	public void createButtonsForButtonBar(Composite parent) {
		Button saveBtn = createButton(parent, IDialogConstants.INTERNAL_ID,
				"Save",false);
		saveBtn.addListener( SWT.MouseUp, new Listener() {
			public void handleEvent(Event event) {  
				try {
					saveCfgStore();
				} catch (VizException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}          		            	 	
		} );  

		Button canBtn = createButton(parent, IDialogConstants.CLOSE_ID,
				IDialogConstants.CLOSE_LABEL, false);
		canBtn.addListener( SWT.MouseUp, new Listener() {
			public void handleEvent(Event event) {    
				//System.out.println("close listener is called");
				close();
			}          		            	 	
		} );  
	}

	/*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets.Shell)
       */
    @Override   
    protected void configureShell( Shell shell ) {
        super.configureShell( shell );       
        shell.setText( "Nsharp Grid Data Interpolation" );
        
    }
    
    private void createDialogContents(Composite parent) {

        Group btnGp = new Group(parent, SWT.SHADOW_ETCHED_IN);
        Button intpBtn = new Button(btnGp, SWT.RADIO | SWT.BORDER);
        intpBtn.setText("interpolation");
        intpBtn.setEnabled(true);
        intpBtn.setBounds(btnGp.getBounds().x + NsharpConstants.btnGapX,
                btnGp.getBounds().y + NsharpConstants.labelGap,
                NsharpConstants.btnWidth, NsharpConstants.btnHeight);
        intpBtn.addListener(SWT.MouseUp, new Listener() {
            public void handleEvent(Event event) {
            	gridInterpolation = true;
            	try {
					updateCfgStore();
				} catch (VizException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }
        });

        Button nearptBtn = new Button(btnGp, SWT.RADIO | SWT.BORDER);
        nearptBtn.setText("nearest point");
        nearptBtn.setEnabled(true);
        nearptBtn.setBounds(btnGp.getBounds().x + NsharpConstants.btnGapX,
                intpBtn.getBounds().y + intpBtn.getBounds().height
                        + NsharpConstants.btnGapY, NsharpConstants.btnWidth,
                NsharpConstants.btnHeight);
        nearptBtn.addListener(SWT.MouseUp, new Listener() {
            public void handleEvent(Event event) {
            	gridInterpolation = false;
            	try {
					updateCfgStore();
				} catch (VizException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }
        });

        if(gridInterpolation){
        	intpBtn.setSelection(true);
        }
        else {
        	nearptBtn.setSelection(true);
        }
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
        if ( this.getShell() == null ){
			this.create();
		}
   	    this.getShell().setLocation(this.getShell().getParent().getLocation().x+1100,
   	    		this.getShell().getParent().getLocation().y+200);
   	    return super.open();
    	
    }
	@Override
	public boolean close() {
		return (super.close());
    }

}
