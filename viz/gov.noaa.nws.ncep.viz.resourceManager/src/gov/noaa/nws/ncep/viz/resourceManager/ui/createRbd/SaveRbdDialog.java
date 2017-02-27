package gov.noaa.nws.ncep.viz.resourceManager.ui.createRbd;

import gov.noaa.nws.ncep.viz.resources.manager.SpfsManager;
import gov.noaa.nws.ncep.viz.ui.display.NcDisplayMngr;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * Dialog displayed from RBD Mngr window when the 'Import SPF...' is selected.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * 02/16/10      #226       Greg Hull    Initial Creation
 * 03/08/10      #228       Archana      Added logic to sort files in the
 *                                       list viewer by name and date.
 *                                       Altered the labels for the display_by_name
 *                                       and display_by_date radio buttons and altered
 *                                       their layout.
 * 02/04/11      #408       Greg Hull    add flag to save the reference time.    
 * 08/01/11      #450       Greg Hull    Change spf name combo to a Text widget
 * 08/01/11      #450       Greg Hull    Use SpfsManager with new NcPathManager 
 * 	                                     Localization for USER-level save/delete.
 * 11/03/11      #???       B. Hebbard   Add "Save Source Timestamp As:" Constant / Latest  * 
 * 05/02/12      #585       S. Gurung    Commented out unwanted options ("Sort Alphabetically" and "Sort By Date")
 * 11/18/15      #12852     bkowal       Ensure that the dialog does not recursively reset its state
 *                                       when nothing has actually changed.
 * 10/11/2016    R17032     A. Su        Disallowed some chars in naming SPF Group, SPF Name and RBD Name.
 * 10/20/2016    R17365     K.Bugenhagen Update selectionListener for rbd_combo
 *                                       to remember selected rbd name at save.
 * 
 * </pre>
 * 
 * @author ghull
 * @version 1
 */

public class SaveRbdDialog extends Dialog {

    private Shell shell;

    private String dlgTitle = "Save Resource Bundle Display";

    private Combo spf_group_combo = null;

    private Text spf_name_txt = null;

    private Combo rbd_name_combo = null;

    private Button save_time_as_constant = null;

    private Button save_time_as_latest = null;

    private Button save_ref_time_btn = null;

    private ListViewer spf_name_lviewer = null;

    private Button save_btn = null;

    private String seldSpfGroup = null;

    private String seldSpfName = null;

    private String seldRbdName = null;

    // true if entering a new SPF or rbd name
    private boolean newRbd = false;

    private Boolean saveRbdOkd = false;

    private boolean saveTimeAsConstant;

    private boolean saveRefTime;

    public String getSeldSpfGroup() {
        return seldSpfGroup;
    }

    public String getSeldSpfName() {
        return seldSpfName;
    }

    public String getSeldRbdName() {
        return seldRbdName;
    }

    public boolean getSaveTimeAsConstant() {
        return saveTimeAsConstant;
    }

    public boolean getSaveRefTime() {
        return saveRefTime;
    }

    public SaveRbdDialog(Shell parShell, String spf_group, String spf_name,
            String rbd_name, boolean refTime, boolean saveTimeAsConstant) {
        super(parShell);
        seldSpfGroup = spf_group;
        seldSpfName = spf_name;
        seldRbdName = rbd_name;
        saveRefTime = refTime;
        this.saveTimeAsConstant = saveTimeAsConstant;
    }

    public Object open() {
        Shell parent = getParent();
        Display display = parent.getDisplay();

        shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MODELESS);
        shell.setText(dlgTitle);
        shell.setSize(540, 500); // pack later

        shell.setLayout(new FormLayout());

        FormData fd = new FormData();

        spf_group_combo = new Combo(shell, SWT.DROP_DOWN);
        fd = new FormData();
        fd.top = new FormAttachment(5, 10);
        fd.left = new FormAttachment(9, 10);
        fd.right = new FormAttachment(85, 0);
        spf_group_combo.setLayoutData(fd);

        Label spf_grp_lbl = new Label(shell, SWT.NONE);
        spf_grp_lbl.setText("SPF Group");
        fd = new FormData();
        fd.bottom = new FormAttachment(spf_group_combo, -3, SWT.TOP);
        fd.left = new FormAttachment(spf_group_combo, 0, SWT.LEFT);
        spf_grp_lbl.setLayoutData(fd);

        Group spf_name_grp = new Group(shell, SWT.SHADOW_NONE);
        spf_name_grp.setText("Available SPFs ");
        spf_name_grp.setLayout(new FormLayout());
        fd = new FormData(260, 300);
        fd.top = new FormAttachment(spf_group_combo, 20, SWT.BOTTOM);
        fd.left = new FormAttachment(0, 10);
        fd.right = new FormAttachment(100, -10);
        spf_name_grp.setLayoutData(fd);

        spf_name_lviewer = new ListViewer(spf_name_grp, SWT.SINGLE | SWT.BORDER
                | SWT.V_SCROLL | SWT.H_SCROLL);
        fd = new FormData();
        fd.top = new FormAttachment(0, 5);
        fd.left = new FormAttachment(0, 10);
        fd.right = new FormAttachment(100, -10);
        fd.bottom = new FormAttachment(100, -65);
        spf_name_lviewer.getList().setLayoutData(fd);

        spf_name_txt = new Text(spf_name_grp, SWT.SINGLE | SWT.BORDER);
        fd = new FormData();
        fd.top = new FormAttachment(spf_name_lviewer.getList(), 30, SWT.BOTTOM);
        fd.left = new FormAttachment(spf_name_lviewer.getList(), 0, SWT.LEFT);
        fd.right = new FormAttachment(spf_name_lviewer.getList(), 0, SWT.RIGHT);
        spf_name_txt.setLayoutData(fd);

        Label spf_name_lbl = new Label(spf_name_grp, SWT.NONE);
        spf_name_lbl.setText("SPF Name");
        fd = new FormData();
        fd.bottom = new FormAttachment(spf_name_txt, -3, SWT.TOP);
        fd.left = new FormAttachment(spf_name_txt, 0, SWT.LEFT);
        spf_name_lbl.setLayoutData(fd);

        rbd_name_combo = new Combo(shell, SWT.DROP_DOWN);
        fd = new FormData();
        fd.top = new FormAttachment(spf_name_grp, 30, SWT.BOTTOM);
        fd.left = new FormAttachment(spf_name_grp, 0, SWT.LEFT);
        fd.right = new FormAttachment(spf_name_grp, 0, SWT.RIGHT);
        rbd_name_combo.setLayoutData(fd);

        Label rbd_name_lbl = new Label(shell, SWT.NONE);
        rbd_name_lbl.setText("RBD Name");
        fd = new FormData();
        fd.bottom = new FormAttachment(rbd_name_combo, -3, SWT.TOP);
        fd.left = new FormAttachment(rbd_name_combo, 0, SWT.LEFT);
        rbd_name_lbl.setLayoutData(fd);

        Label sep0 = new Label(shell, SWT.SEPARATOR | SWT.HORIZONTAL);
        fd = new FormData();
        fd.top = new FormAttachment(rbd_name_combo, 15, SWT.BOTTOM);
        fd.left = new FormAttachment(0, 5);
        fd.right = new FormAttachment(100, -5);
        sep0.setLayoutData(fd);

        Composite save_time_as_grp = new Composite(shell, SWT.SHADOW_NONE);
        save_time_as_grp.setLayout(new FormLayout());
        fd = new FormData();
        fd.top = new FormAttachment(sep0, 10, SWT.BOTTOM);
        fd.left = new FormAttachment(0, 10);
        fd.right = new FormAttachment(100, -10);
        save_time_as_grp.setLayoutData(fd);

        save_time_as_constant = new Button(save_time_as_grp, SWT.RADIO);
        save_time_as_constant.setText("Constant");
        fd = new FormData();
        fd.top = new FormAttachment(sep0, 19, SWT.BOTTOM);
        fd.left = new FormAttachment(save_time_as_grp, 10);
        save_time_as_constant.setLayoutData(fd);
        save_time_as_constant.setSelection(saveTimeAsConstant);
        save_time_as_constant.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent ev) {
                saveTimeAsConstant = save_time_as_constant.getSelection();
            }
        });

        save_time_as_latest = new Button(save_time_as_grp, SWT.RADIO);
        save_time_as_latest.setText("Latest");
        fd = new FormData();
        fd.top = new FormAttachment(sep0, 19, SWT.BOTTOM);
        fd.left = new FormAttachment(save_time_as_constant, 10, SWT.RIGHT);
        save_time_as_latest.setLayoutData(fd);
        save_time_as_latest.setSelection(!saveTimeAsConstant);
        save_time_as_latest.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent ev) {
                saveTimeAsConstant = !save_time_as_latest.getSelection();

            }
        });

        Label save_time_as_lbl = new Label(save_time_as_grp, SWT.NONE);
        save_time_as_lbl.setText("Save Source Timestamp As:");
        fd = new FormData();
        fd.bottom = new FormAttachment(save_time_as_constant, -3, SWT.TOP);
        fd.left = new FormAttachment(save_time_as_constant, -3, SWT.LEFT);
        save_time_as_lbl.setLayoutData(fd);

        Label sep1 = new Label(shell, SWT.SEPARATOR | SWT.HORIZONTAL);
        fd = new FormData();
        fd.top = new FormAttachment(save_time_as_grp, 12, SWT.BOTTOM);
        fd.left = new FormAttachment(0, 5);
        fd.right = new FormAttachment(100, -5);
        sep1.setLayoutData(fd);

        save_ref_time_btn = new Button(shell, SWT.CHECK);
        fd = new FormData();
        save_ref_time_btn.setText("Save Reference Time");
        fd.top = new FormAttachment(sep1, 15, SWT.BOTTOM);
        fd.left = new FormAttachment(rbd_name_combo, 0, SWT.LEFT);
        save_ref_time_btn.setLayoutData(fd);

        save_ref_time_btn.setSelection(saveRefTime);

        save_ref_time_btn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent ev) {
                saveRefTime = save_ref_time_btn.getSelection();
            }
        });

        Label sep2 = new Label(shell, SWT.SEPARATOR | SWT.HORIZONTAL);
        fd = new FormData();
        fd.top = new FormAttachment(save_ref_time_btn, 15, SWT.BOTTOM);
        fd.left = new FormAttachment(0, 5);
        fd.right = new FormAttachment(100, -5);
        fd.bottom = new FormAttachment(100, -60);
        sep2.setLayoutData(fd);

        Button can_btn = new Button(shell, SWT.PUSH);
        fd = new FormData();
        can_btn.setText(" Cancel ");
        fd.bottom = new FormAttachment(100, -10);
        fd.right = new FormAttachment(100, -20);
        can_btn.setLayoutData(fd);

        can_btn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent ev) {
                saveRbdOkd = false;
                shell.dispose();
            }
        });

        save_btn = new Button(shell, SWT.PUSH);
        fd = new FormData();
        save_btn.setText("  Save RBD  ");
        fd.bottom = new FormAttachment(100, -10);
        fd.right = new FormAttachment(can_btn, -20, SWT.LEFT);
        save_btn.setLayoutData(fd);

        save_btn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent ev) {

                SpfsManager manager = SpfsManager.getInstance();

                // If not a valid name, pop up an MessageDialog to alert the
                // user.
                seldSpfGroup = spf_group_combo.getText();
                if (!manager.validateName(shell, seldSpfGroup, "Saving RBD",
                        "SPF Group")) {
                    return;
                }

                // If not a valid name, pop up an MessageDialog to alert the
                // user.
                seldSpfName = spf_name_txt.getText();
                if (!manager.validateName(shell, seldSpfName, "Saving RBD",
                        "SPF Name")) {
                    return;
                }

                // If not a valid name, pop up an MessageDialog to alert the
                // user.
                seldRbdName = rbd_name_combo.getText();
                if (!manager.validateName(shell, seldRbdName, "Saving RBD",
                        "RBD Name")) {
                    return;
                }

                if (seldRbdName == null || seldRbdName.isEmpty()) {
                    System.out.println("RBD name is not selected.");
                    return;
                }

                if (!newRbd) {
                    MessageDialog confirmDlg = new MessageDialog(NcDisplayMngr
                            .getCaveShell(), "Create RBD", null, "RBD "
                            + seldRbdName + " already exists in this SPF.\n\n"
                            + "Do you want to overwrite it?",
                            MessageDialog.QUESTION,
                            new String[] { "Yes", "No" }, 0);
                    confirmDlg.open();

                    if (confirmDlg.getReturnCode() == MessageDialog.CANCEL) {
                        return;
                    }
                }

                saveRbdOkd = true;

                shell.dispose();
            }
        });

        initWidgets();

        shell.setLocation(parent.getLocation().x + 100,
                parent.getLocation().y + 100);
        shell.setMinimumSize(100, 100);

        shell.pack();
        shell.open();

        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }

        return (Boolean) saveRbdOkd;
    }

    private void initWidgets() {

        spf_group_combo.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (seldSpfGroup != null
                        && seldSpfGroup.equals(spf_group_combo.getText())) {
                    /*
                     * Do not alter anything if the selection has not actually
                     * changed because the form would no longer be in sync with
                     * what is actually selected.
                     */
                    return;
                }
                setSeldSpfGroup(spf_group_combo.getText());
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                if (seldSpfGroup != null
                        && seldSpfGroup.equals(spf_group_combo.getText())) {
                    /*
                     * Do not alter anything if the selection has not actually
                     * changed because the form would no longer be in sync with
                     * what is actually selected.
                     */
                    return;
                }
                setSeldSpfGroup(spf_group_combo.getText());
            }
        });

        spf_group_combo.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                save_btn.setEnabled(!rbd_name_combo.getText().isEmpty()
                        && !spf_name_txt.getText().isEmpty()
                        && !spf_group_combo.getText().isEmpty());
                if (seldSpfGroup != null
                        && seldSpfGroup.equals(spf_group_combo.getText())) {
                    /*
                     * Do not alter anything if the selection has not actually
                     * changed because the form would no longer be in sync with
                     * what is actually selected.
                     */
                    return;
                }
                seldSpfGroup = spf_group_combo.getText();
                spf_name_lviewer.setInput(seldSpfGroup);

                newRbd = true;
            }
        });

        spf_name_txt.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                save_btn.setEnabled(!rbd_name_combo.getText().isEmpty()
                        && !spf_name_txt.getText().isEmpty()
                        && !spf_group_combo.getText().isEmpty());

                seldSpfName = spf_name_txt.getText();
                final int spfIndex = spf_name_lviewer.getList().indexOf(
                        seldSpfName);
                if (spfIndex == -1) {
                    /*
                     * if entering a new SPF name, do not show anything selected
                     * in the list.
                     */
                    spf_name_lviewer.getList().deselectAll();
                    newRbd = true;
                } else {
                    /*
                     * The user has entered text that matches an existing SPF
                     * name. So, it should no longer be considered new.
                     */
                    spf_name_lviewer.getList().select(spfIndex);
                    newRbd = false;
                }

                newRbd = true;
            }
        });

        rbd_name_combo.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                save_btn.setEnabled(!rbd_name_combo.getText().isEmpty()
                        && !spf_name_txt.getText().isEmpty()
                        && !spf_group_combo.getText().isEmpty());

                newRbd = true;
            }
        });

        spf_name_lviewer.setContentProvider(new IStructuredContentProvider() {
            @Override
            public Object[] getElements(Object inputElement) {
                return SpfsManager.getInstance().getSpfNamesForGroup(
                        (String) inputElement);
            }

            @Override
            public void dispose() {
            }

            @Override
            public void inputChanged(Viewer viewer, Object oldInput,
                    Object newInput) {
            }
        });

        spf_name_lviewer
                .addSelectionChangedListener(new ISelectionChangedListener() {
                    @Override
                    public void selectionChanged(SelectionChangedEvent event) {
                        StructuredSelection seldSpfs = (StructuredSelection) spf_name_lviewer
                                .getSelection();
                        final String selection = (String) seldSpfs
                                .getFirstElement();
                        if (selection.equals(seldSpfs)) {
                            /*
                             * The selection has not changed.
                             */
                            return;
                        }
                        setSeldSpfName((String) seldSpfs.getFirstElement());
                    }
                });

        spf_group_combo.setItems(SpfsManager.getInstance().getAvailSPFGroups());

        rbd_name_combo.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (seldRbdName != null
                        && seldRbdName.equals(rbd_name_combo.getText())) {
                    /*
                     * Do not alter anything if the selection has not actually
                     * changed because the form would no longer be in sync with
                     * what is actually selected.
                     */
                    return;
                }
                setSelectedRbd(rbd_name_combo.getText());
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                if (seldRbdName != null
                        && seldRbdName.equals(rbd_name_combo.getText())) {
                    /*
                     * Do not alter anything if the selection has not actually
                     * changed because the form would no longer be in sync with
                     * what is actually selected.
                     */
                    return;
                }
                setSelectedRbd(rbd_name_combo.getText());
            }

        });

        // if the user has pre selected a group then select it
        //
        if (seldSpfGroup != null) {
            int spfIndex = spf_group_combo.indexOf(seldSpfGroup);
            if (spfIndex != -1) {
                spf_group_combo.select(spfIndex);
                setSeldSpfGroup(seldSpfGroup);
            }
        } else if (spf_group_combo.getItemCount() > 0) {
            spf_group_combo.select(0);
            setSeldSpfGroup(spf_group_combo.getText());

        }

        if (seldSpfName != null) {
            setSeldSpfName(seldSpfName);
        } else if (spf_name_lviewer.getList().getItemCount() > 0) {
            setSeldSpfName(spf_name_lviewer.getList().getItem(0));
        }

        if (StringUtils.isNotBlank(seldRbdName)) {
            int rbdIndex = rbd_name_combo.indexOf(seldRbdName);
            if (rbdIndex != -1) {
                rbd_name_combo.select(rbdIndex);
                setSelectedRbd(seldRbdName);
            }
        } else if (rbd_name_combo.getItemCount() > 0) {
            rbd_name_combo.select(0);
            setSelectedRbd(seldRbdName);
        }
    }

    private void setSelectedRbd(String rbdName) {
        seldRbdName = rbdName;
    }

    private void setSeldSpfGroup(String spfGroup) {
        newRbd = false;

        seldSpfGroup = spfGroup;

        spf_name_lviewer.setInput(seldSpfGroup);

        spf_name_txt.clearSelection();
    }

    private void setSeldSpfName(String spfName) {
        newRbd = false;

        seldSpfName = spfName;

        spf_name_txt.setText(seldSpfName);

        updateRbdNames(seldSpfGroup, seldSpfName);
    }

    public void updateRbdNames(String spfGroup, String spfName) {

        String savedRbdName = rbd_name_combo.getText();

        rbd_name_combo.setItems(SpfsManager.getInstance().getRbdNamesForSPF(
                spfGroup, spfName));

        if (StringUtils.isNotBlank(seldRbdName)) {
            int rbdIndex = rbd_name_combo.indexOf(seldRbdName);
            if (rbdIndex != -1) {
                rbd_name_combo.select(rbdIndex);
                setSelectedRbd(seldRbdName);
            }
        } else if (rbd_name_combo.getItemCount() > 0) {
            rbd_name_combo.select(0);
            setSelectedRbd(savedRbdName);
        }

    }

    public boolean isOpen() {
        return shell != null && !shell.isDisposed();
    }
}
