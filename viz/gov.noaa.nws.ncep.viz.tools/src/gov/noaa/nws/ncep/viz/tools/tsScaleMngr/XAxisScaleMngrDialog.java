/**
 * This code has unlimited rights, and is provided "as is" by the National Centers 
 * for Environmental Prediction, without warranty of any kind, either expressed or implied, 
 * including but not limited to the implied warranties of merchantability and/or fitness 
 * for a particular purpose.
 * 
 * This code has been developed by the NCEP-SIB for use in the AWIPS2 system.
 * 
 **/
package gov.noaa.nws.ncep.viz.tools.tsScaleMngr;

import gov.noaa.nws.ncep.viz.common.tsScaleMngr.EditXAxisScaleDialog;
import gov.noaa.nws.ncep.viz.common.tsScaleMngr.XAxisScale;
import gov.noaa.nws.ncep.viz.common.tsScaleMngr.XAxisScaleElement;
import gov.noaa.nws.ncep.viz.common.tsScaleMngr.XAxisScaleMngr;
import gov.noaa.nws.ncep.viz.common.ui.UserEntryDialog;
import gov.noaa.nws.ncep.viz.resources.attributes.ResourceExtPointMngr;
import gov.noaa.nws.ncep.viz.ui.display.NcDisplayMngr;

import java.util.HashMap;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.viz.core.exception.VizException;

/**
 * X-axis Scale Manager dialog.
 * 
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#         Engineer        Description
 * ------------ ----------      -----------     --------------------------
 * 09/2014      R4875           sgurung         Initial creation
 * May 16, 2016 5647            tgurney         Remove minimize/maximize buttons
 * 
 * </pre>
 * 
 * @author sgurung
 * @version 1
 */
public class XAxisScaleMngrDialog extends Dialog {

    private Shell shell;

    private Font font;

    private List xAxisScaleList = null;

    private Button newxAxisScaleBtn = null;

    private Button copyxAxisScaleBtn = null;

    private Button editxAxisScaleBtn = null;

    private Button deletexAxisScaleBtn = null;

    private HashMap<String, XAxisScale> xAxisScales = null;

    private EditXAxisScaleDialog editXAxisScaleDlg = null;

    // the resource ext point mngr is used to get a list of all the
    // resources that have a XAxisScaleName attribute and then we will
    // get the plugin from the resource name
    protected ResourceExtPointMngr rscExtPointMngr = null;

    public XAxisScaleMngrDialog(Shell parent) {
        super(parent);
        rscExtPointMngr = ResourceExtPointMngr.getInstance();
    }

    public Object open() {
        Shell parent = getParent();
        Display display = parent.getDisplay();
        shell = new Shell(parent,
                SWT.CLOSE | SWT.TITLE | SWT.RESIZE | SWT.MODELESS);
        shell.setText("Time-series X-axis Scale Manager");

        // Create the main layout for the shell.
        FormLayout mainLayout = new FormLayout();
        shell.setLayout(mainLayout);
        shell.setLocation(parent.getLocation().x + 10,
                parent.getLocation().y + 10);

        font = new Font(shell.getDisplay(), "Monospace", 10, SWT.BOLD);

        // create the controls and layouts
        createMainWindow();
        init();

        shell.setMinimumSize(150, 410);
        shell.pack();

        shell.open();

        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }

        font.dispose();

        return null;
    }

    /**
     * closes the Dialog
     */
    public void close() {
        if (shell != null)
            shell.dispose();
    }

    /**
     * Create the dialog components.
     */
    private void createMainWindow() {
        Composite topComp = new Composite(shell, SWT.NONE);
        FormData fd = new FormData();
        fd.top = new FormAttachment(0, 0);
        fd.left = new FormAttachment(0, 0);
        fd.right = new FormAttachment(100, 0);
        fd.bottom = new FormAttachment(100, 0);
        topComp.setLayoutData(fd);

        topComp.setLayout(new FormLayout());

        xAxisScaleList = new List(topComp, SWT.BORDER | SWT.SINGLE
                | SWT.V_SCROLL | SWT.H_SCROLL);
        fd = new FormData();
        fd.height = 230;
        fd.width = 250;
        fd.top = new FormAttachment(0, 35);
        fd.left = new FormAttachment(0, 15);
        fd.right = new FormAttachment(100, -15);
        xAxisScaleList.setLayoutData(fd);

        xAxisScaleList.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {

                if (xAxisScaleList.getSelectionCount() == 1) {
                    copyxAxisScaleBtn.setEnabled(true);
                    editxAxisScaleBtn.setEnabled(true);

                    String xAxisScaleName = xAxisScaleList.getSelection()[0];

                    if (xAxisScaleName.equals(XAxisScaleMngr.NullScaleName)) {
                        editxAxisScaleBtn.setEnabled(false);
                        copyxAxisScaleBtn.setEnabled(false);
                    } else {
                        copyxAxisScaleBtn.setEnabled(true);
                        editxAxisScaleBtn.setEnabled(true);
                    }

                    // if this xAxisScale is in the USER context
                    // then allow the user to delete it.
                    XAxisScale cf = XAxisScaleMngr.getInstance().getXAxisScale(
                            xAxisScaleName);

                    if (cf != null
                            && cf.getLocalizationFile().getContext()
                                    .getLocalizationLevel() == LocalizationLevel.USER) {
                        deletexAxisScaleBtn.setEnabled(true);
                    } else {
                        deletexAxisScaleBtn.setEnabled(false);
                    }
                } else {
                    copyxAxisScaleBtn.setEnabled(false);
                    editxAxisScaleBtn.setEnabled(false);
                    deletexAxisScaleBtn.setEnabled(false);
                }
            }
        });

        xAxisScaleList.addListener(SWT.MouseDoubleClick, new Listener() {
            public void handleEvent(Event event) {
                editXAxisScale(false);
            }
        });

        Label pmLbl = new Label(topComp, SWT.NONE);
        pmLbl.setText("X-axis Scales");
        fd = new FormData();
        fd.left = new FormAttachment(xAxisScaleList, 0, SWT.LEFT);
        fd.bottom = new FormAttachment(xAxisScaleList, -3, SWT.TOP);
        pmLbl.setLayoutData(fd);

        newxAxisScaleBtn = new Button(topComp, SWT.PUSH);
        newxAxisScaleBtn.setText(" New... ");
        fd = new FormData();
        fd.width = 60;
        fd.left = new FormAttachment(20, -30);
        fd.top = new FormAttachment(xAxisScaleList, 10, SWT.BOTTOM);
        newxAxisScaleBtn.setLayoutData(fd);

        newxAxisScaleBtn.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {

                editXAxisScale(true);
            }
        });

        copyxAxisScaleBtn = new Button(topComp, SWT.PUSH);
        copyxAxisScaleBtn.setText(" Copy... ");
        fd = new FormData();
        fd.width = 60;
        fd.left = new FormAttachment(40, -30);
        fd.top = new FormAttachment(xAxisScaleList, 10, SWT.BOTTOM);
        copyxAxisScaleBtn.setLayoutData(fd);

        copyxAxisScaleBtn.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {

                String fromxAxisScaleName = xAxisScaleList.getSelection()[0];
                // pop up a dialog to prompt for the new name
                UserEntryDialog entryDlg = new UserEntryDialog(shell, "Copy",
                        "X-axis Scale Name:", "CopyOf" + fromxAxisScaleName);
                String newxAxisScaleName = entryDlg.open();

                if (newxAxisScaleName == null || // cancel pressed
                        newxAxisScaleName.isEmpty()) {
                    return;
                }

                // if this X-axis Scale already exists, display a message
                if (xAxisScales.containsKey(newxAxisScaleName)) {

                    MessageDialog infoDlg = new MessageDialog(
                            shell,
                            "Message",
                            null,
                            "A '"
                                    + newxAxisScaleName
                                    + "' X-axis Scale with this name already exists.",
                            MessageDialog.INFORMATION, new String[] { " OK " },
                            0);
                    infoDlg.open();

                    return;
                }

                copyXAxisScale(fromxAxisScaleName, newxAxisScaleName);
            }
        });

        editxAxisScaleBtn = new Button(topComp, SWT.PUSH);
        editxAxisScaleBtn.setText(" Edit... ");
        fd = new FormData();
        fd.width = 60;
        fd.left = new FormAttachment(60, -30);
        fd.top = new FormAttachment(xAxisScaleList, 10, SWT.BOTTOM);
        editxAxisScaleBtn.setLayoutData(fd);

        editxAxisScaleBtn.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                editXAxisScale(false);
            }
        });

        deletexAxisScaleBtn = new Button(topComp, SWT.PUSH);
        deletexAxisScaleBtn.setText(" Delete ");
        fd = new FormData();
        fd.width = 60;
        fd.left = new FormAttachment(80, -30);
        fd.top = new FormAttachment(xAxisScaleList, 10, SWT.BOTTOM);
        deletexAxisScaleBtn.setLayoutData(fd);

        deletexAxisScaleBtn.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                deleteXAxisScale();
            }
        });

        Label sepLbl = new Label(topComp, SWT.SEPARATOR | SWT.HORIZONTAL);
        fd = new FormData();
        fd.left = new FormAttachment(0, 2);
        fd.right = new FormAttachment(100, -2);
        fd.bottom = new FormAttachment(100, -45);
        sepLbl.setLayoutData(fd);

        Button closeBtn = new Button(topComp, SWT.PUSH);
        closeBtn.setText(" Close  ");
        fd = new FormData();
        fd.right = new FormAttachment(100, -20);
        fd.top = new FormAttachment(sepLbl, 10, SWT.BOTTOM);
        closeBtn.setLayoutData(fd);

        closeBtn.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                shell.dispose();
            }
        });
    }

    private void copyXAxisScale(String fromxAxisScaleName,
            String toxAxisScaleName) {
        if (fromxAxisScaleName == null || toxAxisScaleName == null) {
            return; // nothing selected; sanity check
        }

        XAxisScale cf = null;

        if (xAxisScales.containsKey(fromxAxisScaleName)) {
            cf = new XAxisScale(xAxisScales.get(fromxAxisScaleName));
            cf.setName(toxAxisScaleName);

            // create a LocalizationFile
            try {
                XAxisScaleMngr.getInstance().saveXAxisScale(cf);

                xAxisScales.put(fromxAxisScaleName, cf);

                loadXAxisScalesList();
            } catch (VizException ve) {
                MessageDialog errDlg = new MessageDialog(
                        NcDisplayMngr.getCaveShell(), "Error", null,
                        "Error Saving X-axis Scale " + fromxAxisScaleName
                                + ".\n\n" + ve.getMessage(),
                        MessageDialog.ERROR, new String[] { "OK" }, 0);
                errDlg.open();
            }
        }
    }

    private void editXAxisScale(boolean isNew) {
        String xAxisScaleName = "";
        if (!isNew) {
            xAxisScaleName = xAxisScaleList.getSelection()[0];

            if (xAxisScaleName.equals(XAxisScaleMngr.NullScaleName)) {
                MessageDialog errDlg = new MessageDialog(
                        NcDisplayMngr.getCaveShell(), "Error", null,
                        "Can't edit the Null X-axis Scale " + ".\n\n",
                        MessageDialog.ERROR, new String[] { "OK" }, 0);
                errDlg.open();
                return;
            }
        }

        XAxisScale sc = null;

        if (!isNew && xAxisScales.containsKey(xAxisScaleName)) {
            sc = new XAxisScale(xAxisScales.get(xAxisScaleName));
        } else {
            sc = new XAxisScale();
            sc.setName(xAxisScaleName);
            sc.getXAxisScaleElements().add(new XAxisScaleElement()); // create
                                                                     // the list
                                                                     // of
                                                                     // elements
        }

        editXAxisScaleDlg = new EditXAxisScaleDialog(shell, sc);

        XAxisScale newXAxisScale = (XAxisScale) editXAxisScaleDlg.open(
                shell.getLocation().x + shell.getSize().x / 2,
                shell.getLocation().y);

        if (newXAxisScale != null) {
            // create a LocalizationFile
            try {
                XAxisScaleMngr.getInstance().saveXAxisScale(newXAxisScale);

                xAxisScales.put(xAxisScaleName, newXAxisScale);

                loadXAxisScalesList();
            } catch (VizException ve) {
                MessageDialog errDlg = new MessageDialog(
                        NcDisplayMngr.getCaveShell(), "Error", null,
                        "Error Saving X-axis Scale " + xAxisScaleName + ".\n\n"
                                + ve.getMessage(), MessageDialog.ERROR,
                        new String[] { "OK" }, 0);
                errDlg.open();
            }
        }
    }

    private void deleteXAxisScale() {
        String xAxisScaleName = xAxisScaleList.getSelection()[0];
        if (xAxisScaleName == null) {
            return; // nothing selected; sanity check
        }

        // TODO : get a list of all the attribute sets that refer to this
        // xAxisScale
        // and tell the user to edit attribute sets
        try {

            // don't delete BASE/SITE/DESK level X-axis Scale
            XAxisScale cf = XAxisScaleMngr.getInstance().getXAxisScale(
                    xAxisScaleName);
            if (cf == null) {
                throw new VizException();
            }

            MessageDialog confirmDlg = new MessageDialog(shell,
                    "Confirm Delete", null, "Are you sure you want to delete "
                            + xAxisScaleName + "?\n", MessageDialog.QUESTION,
                    new String[] { "Yes", "No" }, 0);
            confirmDlg.open();

            if (confirmDlg.getReturnCode() == MessageDialog.CANCEL) {
                return;
            }

            XAxisScaleMngr.getInstance().deleteXAxisScale(xAxisScaleName);
        } catch (VizException e) {
            MessageDialog errDlg = new MessageDialog(
                    NcDisplayMngr.getCaveShell(), "Error", null,
                    "Error Deleting X-axis Scale " + xAxisScaleName + ".\n\n"
                            + e.getMessage(), MessageDialog.ERROR,
                    new String[] { "OK" }, 0);
            errDlg.open();
        }

        loadXAxisScalesList();
    }

    private void init() {
        loadXAxisScalesList();

        copyxAxisScaleBtn.setEnabled(false);
        editxAxisScaleBtn.setEnabled(false);
        deletexAxisScaleBtn.setEnabled(false);
    }

    // this will load all of the X-axis Scales files
    private void loadXAxisScalesList() {
        xAxisScales = XAxisScaleMngr.getInstance().getXAxisScales();

        xAxisScaleList.removeAll();

        if (xAxisScales != null) {
            for (XAxisScale cf : xAxisScales.values()) {
                xAxisScaleList.add(cf.getName());
            }
        }

        copyxAxisScaleBtn.setEnabled(false);
        editxAxisScaleBtn.setEnabled(false);
        deletexAxisScaleBtn.setEnabled(false);

    }

    public boolean isOpen() {
        return shell != null && !shell.isDisposed();
    }
}