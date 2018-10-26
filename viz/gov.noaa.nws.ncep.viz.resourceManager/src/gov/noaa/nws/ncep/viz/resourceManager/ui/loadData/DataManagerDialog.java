package gov.noaa.nws.ncep.viz.resourceManager.ui.loadData;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.ShellListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.viz.ui.editor.AbstractEditor;

import gov.noaa.nws.ncep.viz.common.display.NcDisplayType;
import gov.noaa.nws.ncep.viz.resourceManager.ui.loadRbd.LoadRbdControl;
import gov.noaa.nws.ncep.viz.resourceManager.ui.manageResources.ManageResourceControl;
import gov.noaa.nws.ncep.viz.resourceManager.ui.manageSpf.ManageSpfControl;
import gov.noaa.nws.ncep.viz.resources.manager.AbstractRBD;
import gov.noaa.nws.ncep.viz.resources.manager.RscBundleDisplayMngr;
import gov.noaa.nws.ncep.viz.ui.display.NcDisplayMngr;
import gov.noaa.nws.ncep.viz.ui.display.NcEditorUtil;

/**
 * Main Dialog to manage and load RBDs.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date          Ticket#    Engineer     Description
 * ------------	----------	-----------	--------------------------
 * 01/25/10       #226      Greg Hull    Initial creation
 * 06/10/10       #273      Greg Hull    Added ManageResource tab
 * 08/18/10       #273      Greg Hull    use mode to set the initial tab
 * 12/17/10       #365      Greg Hull    update Manage Control
 * 01/26/11                 Greg Hull    don't set the dialog size.
 * 02/16/11       #408      Greg Hull    Change shell to Modeless and have hotkey 
 *                                       bring to the front.
 * 11/07/11                 Chin Chen    fixed a null pointer exception bug       
 * 06/19/12       #624      Greg Hull    clone imported RBD and set size based 
 *                                       on prev width.
 * 02/22/13       #972      Greg Hull    AbstractEditor
 * May 16, 2016 5655        bsteffen     Close shell when parent is closed.
 * 04/04/2016     R17317    A. Su        Made the method isOpen() static.
 * May 16, 2016 5655        bsteffen     Close shell when parent is closed.
 * 
 * </pre>
 * 
 * @author
 * @version 1
 */
public class DataManagerDialog extends Dialog {

    private RscBundleDisplayMngr rbd_mngr;

    private static Shell shell;

    private String dlgTitle;

    private static boolean isOpen = false;

    private TabFolder mngrTabFolder = null;

    protected Control activeMngr = null;

    protected DataLoadControl createRbdCntrl = null;

    protected ManageResourceControl manageRscCntrl = null;

    private static int prevHeight = 0;

    private Point prevLocation = new Point(0, 0);

    public DataManagerDialog(Shell parShell, String title,
            RscBundleDisplayMngr mngr, String mode) throws VizException {
        super(parShell);
        rbd_mngr = mngr;
        this.dlgTitle = title;

        shell = new Shell(SWT.SHELL_TRIM | SWT.MODELESS);
        shell.setText(dlgTitle);

        final Shell parentShell = parShell;
        final Shell currentShell = shell;
        ShellListener closeListener = new ShellAdapter() {

            @Override
            public void shellClosed(ShellEvent e) {
                if (e.widget != currentShell && !currentShell.isDisposed()) {
                    currentShell.close();
                }
                if (!parentShell.isDisposed()) {
                    parentShell.removeShellListener(this);
                }
            }

        };
        /*
         * Add the close listener to parentShell so when the parent is closed
         * the dialog will close.
         */
        parentShell.addShellListener(closeListener);
        /*
         * Add the close listener to the currentShell so when the dialog is
         * closed it can remove the shell listener from the parent.
         */
        currentShell.addShellListener(closeListener);

        GridLayout mainLayout = new GridLayout(1, true);
        mainLayout.marginHeight = 1;
        mainLayout.marginWidth = 1;
        shell.setLayout(mainLayout);
        prevLocation = new Point(parShell.getLocation().x, 0);
        // shell.setLocation( parShell.getLocation().x, 0);

        mngrTabFolder = new TabFolder(shell, SWT.NONE);
        GridData gd = new GridData();
        gd.grabExcessHorizontalSpace = true;
        gd.grabExcessVerticalSpace = true;
        gd.horizontalAlignment = SWT.FILL;
        gd.verticalAlignment = SWT.FILL;
        mngrTabFolder.setLayoutData(gd);

        final TabItem mngrTabItem = new TabItem(mngrTabFolder, SWT.NONE);
        mngrTabItem.setText(" Load Resource ");

        // get the active Display and set the rbd_mngr with it
        AbstractEditor currEditor = NcDisplayMngr.getActiveNatlCntrsEditor();

        if (currEditor != null) {

            try {
                // check that t a type of display that can be imported
                AbstractRBD<?> rbdBndl = AbstractRBD
                        .createRbdFromEditor(currEditor);

                rbdBndl = AbstractRBD.clone(rbdBndl);

                rbd_mngr.initFromRbdBundle(rbdBndl);
            } catch (VizException e) {
                MessageDialog errDlg = new MessageDialog(shell, "Error", null,
                        "Error importing Rbd from display "
                                + NcEditorUtil.getDisplayName(currEditor)
                                + ".\n" + e.getMessage(),
                        MessageDialog.ERROR, new String[] { "OK" }, 0);
                errDlg.open();

                rbd_mngr.init(NcDisplayType.NMAP_DISPLAY);
            }
        }

        createRbdCntrl = new DataLoadControl(mngrTabFolder, rbd_mngr);

        final TabItem cnfgTabItem = new TabItem(mngrTabFolder, SWT.NONE);
        cnfgTabItem.setText(" Manage Resources ");

        manageRscCntrl = new ManageResourceControl(mngrTabFolder);

        mngrTabItem.setControl(createRbdCntrl);
        cnfgTabItem.setControl(manageRscCntrl);

        Button closeBtn = new Button(shell, SWT.PUSH);
        closeBtn.setText("    Close    ");
        gd = new GridData();
        gd.horizontalAlignment = SWT.END;
        gd.verticalAlignment = SWT.END;
        closeBtn.setLayoutData(gd);
        closeBtn.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent ev) {
                close();
            }
        });

        mngrTabFolder.layout();

        if (mode == null || mode.equals("CREATE_RBD")) {
            mngrTabFolder.setSelection(0);
            createRbdCntrl.updateDialog();
        } else if (mode.equals("MANAGE_RESOURCES")) {
            mngrTabFolder.setSelection(1);
            manageRscCntrl.updateDialog();
        } else {
            mngrTabFolder.setSelection(0);
        }

        //
        mngrTabFolder.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent ev) {
                TabItem[] seldTab = mngrTabFolder.getSelection();

                if (seldTab[0].getControl() instanceof LoadRbdControl) {
                    ((LoadRbdControl) seldTab[0].getControl()).updateDialog();
                } else if (seldTab[0]
                        .getControl() instanceof DataLoadControl) {
                    ((DataLoadControl) seldTab[0].getControl()).updateDialog();
                } else if (seldTab[0]
                        .getControl() instanceof ManageSpfControl) {
                    ((ManageSpfControl) seldTab[0].getControl()).updateDialog();
                } else if (seldTab[0]
                        .getControl() instanceof ManageResourceControl) {
                    ((ManageResourceControl) seldTab[0].getControl())
                            .updateDialog();
                }
            }
        });
    }

    public static boolean isOpen() {
        return isOpen;
    }

    public Object open() {
        Shell parent = getParent();
        Display display = parent.getDisplay();

        shell.setSize(new Point(shell.getSize().x,
                (prevHeight == 0 ? shell.getSize().y : prevHeight)));
        shell.setLocation(prevLocation);
        shell.open();

        isOpen = true;

        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }

        isOpen = false;

        return null;
    }

    public static void close() {
        // if there is a preview editor up then close it
        if (shell != null) {
            if (!shell.isDisposed()) {
                prevHeight = shell.getSize().y;
                shell.dispose();
            }
        }
        isOpen = false;
    }

    public void setActiveTab(String mode) {
        shell.setFocus();
        shell.setActive();
        shell.setMaximized(false); // will cause the dialog to be brought to the
                                   // front.

        if (mode == null) {
            return;
        }
        if (mode.equals("LOAD")) {
            mngrTabFolder.setSelection(0);
        } else if (mode.equals("CREATE")) {
            mngrTabFolder.setSelection(1);
        }
    }
}