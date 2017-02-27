package gov.noaa.nws.ncep.viz.tools.plotModelMngr;

import gov.noaa.nws.ncep.viz.common.ui.UserEntryDialog;
import gov.noaa.nws.ncep.viz.rsc.plotdata.plotModels.PlotModelMngr;
import gov.noaa.nws.ncep.viz.rsc.plotdata.plotModels.elements.PlotModel;
import gov.noaa.nws.ncep.viz.rsc.plotdata.rsc.EditPlotModelComposite;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;

/**
 * UI for editing Plot Models.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer       Description
 * ------------ ---------- ----------- --------------------------
 * 10/15/2009    172        M. Li           Initial creation.
 * 12/05/2009    217        Greg Hull       Use EditPlotModelComposite
 * 07/26/2010    285        Q. Zhou         modified editPlotModelComposit
 * 03/08/2011    425        Greg Hull       Add a SaveAs button
 * 03/10/2013    921        S. Russell      TTR 921. Commented out "Save" button
 * 03/11/2013    921        S. Russell      TTR 921. Append "COPY" during "Save As"
 * 06/12/2015   8051        Jonas Okwara    Add a "Save" Button and its functionality
 * 10/01/2015   R8051       Edwin Brown     Added Save button
 *                                          Clean up work
 * 03/29/2016   R7567       A. Su           Added localization info to dialog title.
 *                                          Added a new open() method to allow this dialog to access its parent
 *                                            so that this dialog can be brought to front, and
 *                                            be able to clear the selection of Plot Model Manager dialog's Plot Model list.
 *                                          Handled the event for the window close button to clear the above selection.
 * </pre>
 * 
 * @author mli
 * @version 1.0
 */
public class EditPlotModelDialog extends Dialog {

    protected Shell shell;

    protected String DIALOG_TITLE = "Edit Plot Model";

    protected String pltmdlnmsffx = "null";

    protected String SAVE_BUTTON = "Save";

    protected String CANCEL_BUTTON = "Cancel";

    protected String SAVE_AS_BUTTON = "Save As";

    protected String CONFIRM = "Confirm";

    protected String NO = "No";

    protected String YES = "Yes";

    protected String SAVE_PLOT_MODEL_AS = "Save Plot Model As";

    protected String EDIT_PLOT_MODEL = "Edit Plot Model";

    protected boolean ok = false;

    protected String PLOT_MODEL_OVERWRITE_CONFIRMATION = "'Plot Model already exists.\n\nDo you want to overwrite it?";

    private PlotModel editedPlotModel = null;

    private EditPlotModelDialogManager manager = EditPlotModelDialogManager
            .getInstance();

    /**
     * This Plot Model Manager dialog as input to initiate this Edit Plot Model
     * dialog.
     */
    private PlotModelMngrDialog parent = null;

    /**
     * This plugin name as the input to initiate this Edit Plot Model.
     */
    private String savedPluginName = null;

    /**
     * This plot model name as the input to initiate this Edit Plot Model.
     */
    private String savedPlotModelName = null;

    public EditPlotModelDialog(Shell parentShell, PlotModel pm) {
        super(parentShell);

        // Set title with the info of localization.
        String localizationInfo = manager.getLocalizationString(pm);
        DIALOG_TITLE = EDIT_PLOT_MODEL + " - " + pm.getPlugin() + "/"
                + pm.getName() + localizationInfo;

        editedPlotModel = new PlotModel(pm);
    }

    public void createShell(int x, int y) {
        shell = new Shell(getParent(), SWT.DIALOG_TRIM | SWT.RESIZE);
        shell.setText(DIALOG_TITLE);
        GridLayout mainLayout = new GridLayout(1, true);
        mainLayout.marginHeight = 1;
        mainLayout.marginWidth = 1;
        shell.setLayout(mainLayout);
        shell.setLocation(x, y);

        // Handle the event for the window close button.
        shell.addListener(SWT.Close, new Listener() {
            public void handleEvent(Event event) {
                handleDialogClosing();
            }
        });

        Composite editPlotModelComp = new EditPlotModelComposite(shell,
                SWT.NONE, editedPlotModel, null);

        GridData gd = new GridData();

        Composite okCanComp = new Composite(shell, SWT.NONE);
        gd = new GridData();
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalAlignment = SWT.FILL;
        okCanComp.setLayoutData(gd);

        okCanComp.setLayout(new FormLayout());

        Button canBtn = new Button(okCanComp, SWT.PUSH);
        canBtn.setText(CANCEL_BUTTON);
        FormData fd = new FormData();
        fd.width = 80;
        fd.bottom = new FormAttachment(100, -5);
        fd.left = new FormAttachment(25, -40);
        canBtn.setLayoutData(fd);

        canBtn.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                handleDialogClosing();
                ok = false;
                shell.dispose();
            }
        });

        // Set and Enable "Save" Button

        Button saveBtn = new Button(okCanComp, SWT.PUSH);
        saveBtn.setText(SAVE_BUTTON);
        fd = new FormData();
        fd.width = 80;
        fd.bottom = new FormAttachment(100, -5);
        fd.left = new FormAttachment(50, -40);
        saveBtn.setLayoutData(fd);
        saveBtn.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                handleDialogClosing();

                String newPltMdlName = editedPlotModel.getName();
                LocalizationLevel lLevel = editedPlotModel
                        .getLocalizationFile().getContext()
                        .getLocalizationLevel();
                if (lLevel == LocalizationLevel.USER) {
                    editedPlotModel.setName(newPltMdlName);
                    ok = true;
                    shell.dispose();
                } else // Trying to save and a BASE level of the file exists.
                       // Open confirmation dialog
                {
                    String TemplateOverrideConfirmation = newPltMdlName
                            + " (BASE) exists. Please confirm that you want to create a USER level version, "
                            + newPltMdlName + " (USER), that will override "
                            + newPltMdlName + " (BASE)";
                    boolean result = MessageDialog.openConfirm(shell, CONFIRM,
                            TemplateOverrideConfirmation);

                    if (result) {
                        editedPlotModel.setName(newPltMdlName);
                        ok = true;
                        shell.dispose();
                    } else {
                        return;
                    }
                }
            }
        });

        // Enable "Save As" Button
        Button saveAsBtn = new Button(okCanComp, SWT.PUSH);
        saveAsBtn.setText(SAVE_AS_BUTTON);
        fd = new FormData();
        fd.width = 90;
        fd.bottom = new FormAttachment(100, -5);
        fd.left = new FormAttachment(75, -40);
        saveAsBtn.setLayoutData(fd);

        saveAsBtn.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                handleDialogClosing();

                pltmdlnmsffx = "COPY";
                String pmname = editedPlotModel.getName() + pltmdlnmsffx;

                // pop up a dialog to prompt for the new name
                UserEntryDialog entryDlg = new UserEntryDialog(shell,
                        SAVE_AS_BUTTON, SAVE_PLOT_MODEL_AS, pmname);
                String newPltMdlName = entryDlg.open();

                if (newPltMdlName == null || newPltMdlName.isEmpty()) {
                    return;
                }

                // if this plotModel already exists, prompt to overwrite
                if (PlotModelMngr.getInstance().getPlotModel(
                        editedPlotModel.getPlugin(), newPltMdlName) != null) {
                    MessageDialog confirmDlg = new MessageDialog(shell,
                            CONFIRM, null, "A '" + newPltMdlName
                                    + PLOT_MODEL_OVERWRITE_CONFIRMATION,
                            MessageDialog.QUESTION, new String[] { YES, NO }, 0);
                    confirmDlg.open();
                    if (confirmDlg.getReturnCode() == MessageDialog.CANCEL) {
                        return;
                    }
                }

                editedPlotModel.setName(newPltMdlName);
                ok = true;
                shell.dispose();
            }
        });

    }

    public void open() {
        open(getParent().getLocation().x + 10, getParent().getLocation().y + 10);
    }

    public Object open(int x, int y) {
        Display display = getParent().getDisplay();

        createShell(x, y);

        initWidgets();

        manager.addToActivelyEditedPlotModelList(savedPluginName,
                savedPlotModelName, shell);

        shell.pack();
        shell.open();

        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }

        manager.removeFromActivelyEditedPlotModelList(savedPluginName,
                savedPlotModelName);

        return (ok ? editedPlotModel : null);
    }

    /**
     * This open() method records its parent composite widget, and plot model
     * before calling open(x, y).
     * 
     * @param parent
     *            the parent dialog who initiates this dialog.
     * @param pluginName
     *            the plugin name associated with this dialog.
     * @param plotModelName
     *            the plot model name associated with this dialog.
     * @param x
     *            the x coordinate to open this dialog
     * @param y
     *            the y coordinate to open this dialog.
     * @return the plot model to be edited.
     */
    public Object open(PlotModelMngrDialog parent, String pluginName,
            String plotModelName, int x, int y) {

        this.parent = parent;
        savedPluginName = pluginName;
        savedPlotModelName = plotModelName;

        return open(x, y);
    }

    public void initWidgets() {
    }

    /**
     * Clean up work when this dialog is closing.
     */
    private void handleDialogClosing() {
        if (parent != null) {
            parent.clearPlotModelListSelections();
        }
    }
}
