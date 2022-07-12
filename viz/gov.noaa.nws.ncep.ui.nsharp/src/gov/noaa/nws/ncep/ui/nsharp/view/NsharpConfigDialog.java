package gov.noaa.nws.ncep.ui.nsharp.view;

/**
 * 
 * gov.noaa.nws.ncep.ui.nsharp.palette.NsharpConfigDialog
 * 
 * 
 * This code has been developed by the NCEP-SIB for use in the AWIPS2 system.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    	Engineer    Description
 * -------		------- 	-------- 	-----------
 * 03/21/2012	229			Chin Chen	Initial coding
 * 03/09/2015   RM#6674     Chin Chen   support model sounding query data interpolation and nearest point option                       
 * 09/16/2015   RM#10188    Chin Chen   Model selection upgrades - use grid resource definition name for model type display
 * 07/12/2022             srcarter@ucar Make this dialog resizable 
 *
 * </pre>
 * 
 * @author Chin Chen
 * @version 1.0
 */

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

public class NsharpConfigDialog extends Dialog {
    private Button parameterBtn, dataDisplayBtn, dataPageBtn, timeLineBtn,
            stnBtn, sndBtn, paneCfgBtn, mdlDataBtn;

    private static NsharpConfigDialog thisDialog = null;

    private static NsharpParametersSelectionConfigDialog parameterSelDialog = null;

    private static NsharpDataDisplayConfigDialog dataDislpayDialog = null;

    private static NsharpDataPageConfigDialog dataPageDialog = null;

    private static NsharpTimeLineConfigDialog timelineDialog = null;

    private static NsharpStnConfigDialog stnDialog = null;

    private static NsharpSndConfigDialog sndDialog = null;

    private static NsharpPaneConfigDialog paneCfgDialog = null;

    private static NsharpGridDataConfigDialog mdlDataDialog = null;

    public NsharpConfigDialog(Shell parentShell) {
        super(parentShell);
    }

    public NsharpConfigDialog(IShellProvider parentShell) {
        super(parentShell);
    }

    public static NsharpConfigDialog getInstance(Shell parShell) {

        if (thisDialog == null) {
            thisDialog = new NsharpConfigDialog(parShell);
        }

        return thisDialog;

    }

    public void createDialogContents(Composite parent) {
        parameterBtn = new Button(parent, SWT.PUSH);
        parameterBtn.setText("Parameters Selection");
        parameterBtn.setEnabled(true);

        parameterBtn.addListener(SWT.MouseUp, new Listener() {
            public void handleEvent(Event event) {
                Shell shell = PlatformUI.getWorkbench()
                        .getActiveWorkbenchWindow().getShell();
                parameterSelDialog = NsharpParametersSelectionConfigDialog
                        .getInstance(shell);
                if (parameterSelDialog != null) {
                    parameterSelDialog.open();
                }
            }
        });
        dataDisplayBtn = new Button(parent, SWT.PUSH);
        dataDisplayBtn.setText("Data Display Configuration");
        dataDisplayBtn.setEnabled(true);

        dataDisplayBtn.addListener(SWT.MouseUp, new Listener() {
            public void handleEvent(Event event) {
                Shell shell = PlatformUI.getWorkbench()
                        .getActiveWorkbenchWindow().getShell();
                dataDislpayDialog = NsharpDataDisplayConfigDialog
                        .getInstance(shell);
                if (dataDislpayDialog != null) {
                    dataDislpayDialog.open();
                }
            }
        });

        dataPageBtn = new Button(parent, SWT.PUSH);
        dataPageBtn.setText("Data Page Configuration");
        dataPageBtn.setEnabled(true);

        dataPageBtn.addListener(SWT.MouseUp, new Listener() {
            public void handleEvent(Event event) {
                Shell shell = PlatformUI.getWorkbench()
                        .getActiveWorkbenchWindow().getShell();
                dataPageDialog = NsharpDataPageConfigDialog.getInstance(shell);
                if (dataPageDialog != null) {
                    dataPageDialog.open();
                }
            }
        });
        timeLineBtn = new Button(parent, SWT.PUSH);
        timeLineBtn.setText("Time Line Activation");

        timeLineBtn.setEnabled(true);

        timeLineBtn.addListener(SWT.MouseUp, new Listener() {
            public void handleEvent(Event event) {
                Shell shell = PlatformUI.getWorkbench()
                        .getActiveWorkbenchWindow().getShell();
                timelineDialog = NsharpTimeLineConfigDialog.getInstance(shell);
                if (timelineDialog != null) {
                    timelineDialog.open();
                }
            }
        });
        stnBtn = new Button(parent, SWT.PUSH);
        stnBtn.setText("Station Activation");
        stnBtn.setEnabled(true);

        stnBtn.addListener(SWT.MouseUp, new Listener() {
            public void handleEvent(Event event) {
                Shell shell = PlatformUI.getWorkbench()
                        .getActiveWorkbenchWindow().getShell();
                stnDialog = NsharpStnConfigDialog.getInstance(shell);
                if (stnDialog != null) {
                    stnDialog.open();
                }
            }
        });
        sndBtn = new Button(parent, SWT.PUSH);
        sndBtn.setText("Sounding Source Activation");
        sndBtn.setEnabled(true);

        sndBtn.addListener(SWT.MouseUp, new Listener() {
            public void handleEvent(Event event) {
                Shell shell = PlatformUI.getWorkbench()
                        .getActiveWorkbenchWindow().getShell();
                sndDialog = NsharpSndConfigDialog.getInstance(shell);
                if (sndDialog != null) {
                    sndDialog.open();
                }
            }
        });

        paneCfgBtn = new Button(parent, SWT.PUSH);
        paneCfgBtn.setText("Display Pane Configuration");
        paneCfgBtn.setEnabled(true);

        paneCfgBtn.addListener(SWT.MouseUp, new Listener() {
            public void handleEvent(Event event) {
                Shell shell = PlatformUI.getWorkbench()
                        .getActiveWorkbenchWindow().getShell();
                paneCfgDialog = NsharpPaneConfigDialog.getInstance(shell);
                if (paneCfgDialog != null) {
                    paneCfgDialog.open();
                }
            }
        });
        mdlDataBtn = new Button(parent, SWT.PUSH);
        mdlDataBtn.setText("Grid Data Interpolation");
        mdlDataBtn.setEnabled(true);

        mdlDataBtn.addListener(SWT.MouseUp, new Listener() {
            public void handleEvent(Event event) {
                Shell shell = PlatformUI.getWorkbench()
                        .getActiveWorkbenchWindow().getShell();
                mdlDataDialog = NsharpGridDataConfigDialog.getInstance(shell);
                if (mdlDataDialog != null) {
                    mdlDataDialog.open();
                }
            }
        });

        Button windbarbCfgBtn = new Button(parent, SWT.PUSH);
        windbarbCfgBtn.setText("Wind Barb Configuration");
        windbarbCfgBtn.setEnabled(true);

        windbarbCfgBtn.addListener(SWT.MouseUp, new Listener() {
            public void handleEvent(Event event) {
                Shell shell = PlatformUI.getWorkbench()
                        .getActiveWorkbenchWindow().getShell();
                NsharpWindBarbConfigDialog windBarbDlg = NsharpWindBarbConfigDialog
                        .getInstance(shell);

                if (windBarbDlg != null) {
                    windBarbDlg.open();
                }
            }
        });
    }

    @Override
    public Control createDialogArea(Composite parent) {
        Composite top;
        top = (Composite) super.createDialogArea(parent);

        // Create the main layout for the shell.
        GridLayout mainLayout = new GridLayout(1, true);
        mainLayout.marginHeight = 3;
        mainLayout.marginWidth = 3;
        top.setLayout(mainLayout);

        // Initialize all of the menus, controls, and layouts
        createDialogContents(top);

        return top;
    }

    @Override
    public void createButtonsForButtonBar(Composite parent) {
        Button closeBtn = createButton(parent, IDialogConstants.CLOSE_ID,
                IDialogConstants.CLOSE_LABEL, true);
        closeBtn.addListener(SWT.MouseUp, new Listener() {
            public void handleEvent(Event event) {
                close();
            }
        });

    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText("Nsharp Configuration");
        shell.setSize(250, 450);
    }

    @Override
    public int open() {
        if (this.getShell() == null) {
            this.create();
        }
        this.getShell().setLocation(
                this.getShell().getParent().getLocation().x + 1100,
                this.getShell().getParent().getLocation().y + 200);
        return super.open();

    }
    
    
    /* (non-Javadoc)
     * @see org.eclipse.jface.dialogs.Dialog#isResizable()
     * Allow this dialog to be resizable, because sometimes it is not
     * large enough to display all the options on monitors with 
     * certain resolutions.
     */
    @Override
    protected boolean isResizable(){
    	return true;
    }
}
