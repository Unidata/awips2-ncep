package gov.noaa.nws.ncep.viz.resourceManager.ui.manageResources;

import gov.noaa.nws.ncep.viz.resourceManager.ui.manageResources.ManageResourceControl.IEditResourceComposite;
import gov.noaa.nws.ncep.viz.resources.manager.AttrSetGroup;
import gov.noaa.nws.ncep.viz.resources.manager.AttributeSet;
import gov.noaa.nws.ncep.viz.resources.manager.ResourceDefinition;
import gov.noaa.nws.ncep.viz.resources.manager.ResourceDefnsMngr;
import gov.noaa.nws.ncep.viz.resources.manager.ResourceName;
import gov.noaa.nws.ncep.viz.ui.display.NcDisplayMngr;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.common.localization.exception.LocalizationException;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.viz.core.exception.VizException;

/**
 * 
 * 
 * <pre>
 *  SOFTWARE HISTORY
 *  Date         Ticket#     Engineer    Description
 *  ------------ ----------  ----------- --------------------------
 *  06/09/2010       #273      Greg Hull    Created
 *  12/22/2011       #365      Greg Hull    Changes to support dynamic resources
 *  07/22/2011       #450      Greg Hull    Save to User Localization
 *  06/10/2012       #816      Greg Hull    allow user to add to multiple attrSetGroups
 *  02/22/2013       #972      Greg Hull    NcDisplayMngr
 *  05/23/2016      R17711     J. Beck      Removed UI code and UI backing code that displays group list
 *                                          and associates list of groups with attribute sets..
 *                                          Copy makes a new attribute set using the existing group.
 *                                          Changed listener code, dialog text, dialog severity.
 *                                          Added code to prevent errors (like empty UI text fields).
 *                                          Removed calls to the deprecated getFile() method (Raytheon).
 *                                          Added statusHandler for some events.
 *  09/26/2016      R20482     K.Bugenhagen In setSelectedResource, only check 
 *                                          if file exists if it's not a 
 *                                          user-level file.  Also, cleanup.
 * 
 * </pre>
 * 
 * @author Greg Hull
 * @version 1
 */
class EditAttrSetComp extends Composite implements IEditResourceComposite {

    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(EditAttrSetComp.class);

    private ResourceDefnsMngr resourceDefnsManager;

    private ManageResourceControl mngrControl;

    private ResourceName selectedResourceName = null;

    private ResourceDefinition selectedResourceDefn = null;

    private AttributeSet selectedAttributeSet = null;

    private Text attrNameTextField;

    private Text attrSetTextArea;

    private Button saveButton;

    private Button createButton;

    private Button cancelButton;

    private static final int BUTTON_WIDTH = 100;

    private static final int PERCENT_POSITION = 100;

    /**
     * 
     * Constructor
     * 
     * @param parent
     *            the parent Composite
     * @param style
     *            the style of widget to construct
     * @param mgrCtl
     *            the ManageResourceControl
     */
    public EditAttrSetComp(Composite parent, int style,
            ManageResourceControl mgrCtl) {

        super(parent, style);
        init(mgrCtl);

    }

    /**
     * 
     * Initial components during object creation
     */
    private void init(ManageResourceControl mgrCtl) {

        // Create the top level Composite (like a container)
        // that will hold the child UI components
        Composite editAttrComposite = this;
        FormData fd = new FormData();
        fd.top = new FormAttachment(0, 12);
        fd.left = new FormAttachment(0, 0);
        fd.right = new FormAttachment(PERCENT_POSITION, 0);
        fd.bottom = new FormAttachment(PERCENT_POSITION, 0);
        editAttrComposite.setLayoutData(fd);
        setLayoutData(fd);
        editAttrComposite.setLayout(new FormLayout());

        mngrControl = mgrCtl;
        resourceDefnsManager = mngrControl.getRscDefnMngr();

        // Create the labels, buttons, and text fields
        createUiComponents(editAttrComposite);

        /*
         * Listener for the attribute set name text field. Conditionally sets
         * the create button to be enabled or disabled if the name is empty or
         * not.
         */
        attrNameTextField.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {

                String newTextStr = attrNameTextField.getText().trim();

                if (newTextStr.isEmpty()) {
                    createButton.setEnabled(false);
                } else {
                    if (!attrSetTextArea.getText().trim().isEmpty())
                        createButton.setEnabled(true);
                }
            }
        });

        /*
         * Listener for the attribute set text area. Conditionally sets the
         * save/create button to be enabled or disabled if the name is empty or
         * not.
         */
        attrSetTextArea.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {

                String newAreaTextStr = attrSetTextArea.getText().trim();

                if (newAreaTextStr.isEmpty()) {
                    saveButton.setEnabled(false);
                    createButton.setEnabled(false);

                } else {
                    if (!attrNameTextField.getText().trim().isEmpty()) {
                        saveButton.setEnabled(true);
                        createButton.setEnabled(true);
                    }
                }
            }
        });

        /*
         * Button listeners
         */
        saveButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent ev) {
                saveAttrSet(false);
            }
        });

        createButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent ev) {
                saveAttrSet(true);
            }
        });

        cancelButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent ev) {
                mngrControl.editActionCanceled();
            }
        });
    }

    /**
     * 
     * Create all the buttons and text areas used in this UI.
     * 
     * @param parent
     *            the parent Composite (parent container)
     */
    private void createUiComponents(Composite parent) {

        /*
         * Text input areas
         */

        // Create attribute set name Text field, editable (when copying)
        attrNameTextField = new Text(parent, SWT.SINGLE | SWT.BORDER);
        attrNameTextField.setText("");
        FormData fd = new FormData();
        fd.width = 200;
        fd.top = new FormAttachment(0, 30);
        fd.left = new FormAttachment(0, 15);
        attrNameTextField.setLayoutData(fd);

        // Create attribute set Text area
        attrSetTextArea = new Text(parent, SWT.MULTI | SWT.BORDER
                | SWT.V_SCROLL | SWT.H_SCROLL);
        attrSetTextArea.setText("");
        fd = new FormData();
        fd.top = new FormAttachment(attrNameTextField, 0, SWT.TOP);
        fd.left = new FormAttachment(40, 0);
        fd.right = new FormAttachment(PERCENT_POSITION, -10);
        fd.bottom = new FormAttachment(PERCENT_POSITION, -60);
        attrSetTextArea.setLayoutData(fd);

        // The attribute set name label (Left, top)
        createLabel(parent, "Attribute Set Name", attrNameTextField);

        // The attribute set content label (Right, top)
        createLabel(parent, "Attribute Set Values", attrSetTextArea);

        saveButton = createButton(parent, "Save");
        createButton = createButton(parent, "Create");
        cancelButton = createButton(parent, "Cancel");

    }

    /**
     * Create buttons
     * 
     * @param parent
     *            this button's Composite (parent container)
     * @param buttonText
     *            this button's text
     * @return a Button
     */
    private Button createButton(Composite parent, String buttonText) {
        Button b = new Button(parent, SWT.PUSH);
        b.setText(buttonText);
        FormData data = new FormData();
        data.width = BUTTON_WIDTH;
        data.bottom = new FormAttachment(100, -10);

        if (b.getText().equalsIgnoreCase("cancel")) {
            data.right = new FormAttachment(saveButton, -20, SWT.LEFT);
        } else {
            data.right = new FormAttachment(PERCENT_POSITION, -30);
        }

        b.setLayoutData(data);
        return b;
    }

    /**
     * Create a label
     * 
     * @param parent
     *            this label's Composite (parent container)
     * @param labelText
     *            this labels's text
     * @param textField
     *            this label's associated Text (holds text)
     * @return
     */
    private Label createLabel(Composite parent, String labelText, Text textField) {
        Label l = new Label(parent, SWT.NONE);
        l.setText(labelText);
        FormData data = new FormData();
        data.bottom = new FormAttachment(textField, -3, SWT.TOP);
        data.left = new FormAttachment(textField, 0, SWT.LEFT);
        l.setLayoutData(data);
        return l;
    }

    /*
     * (non-Javadoc)
     * 
     * @see gov.noaa.nws.ncep.viz.resourceManager.ui.manageResources.
     * ManageResourceControl.IEditResourceComposite#getSelectedResourceName()
     */

    @Override
    public ResourceName getSelectedResourceName() {
        return selectedResourceName;
    }

    /*
     * (non-Javadoc)
     * 
     * @see gov.noaa.nws.ncep.viz.resourceManager.ui.manageResources.
     * ManageResourceControl.IEditResourceComposite#isModified()
     */
    @Override
    public boolean isModified() {
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see gov.noaa.nws.ncep.viz.resourceManager.ui.manageResources.
     * ManageResourceControl.IEditResourceComposite#activate()
     */
    @Override
    public void activate() {
        setVisible(true);
        if (getParent() instanceof Group) {
            ((Group) getParent()).setText(getTitle());
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see gov.noaa.nws.ncep.viz.resourceManager.ui.manageResources.
     * ManageResourceControl
     * .IEditResourceComposite#copySelectedResource(gov.noaa
     * .nws.ncep.viz.resources.manager.ResourceName)
     * 
     * Override superclass method for Copy button clicked
     */
    @Override
    public void copySelectedResource(ResourceName rscName) {
        setSelectedResource(rscName);
        attrNameTextField.setText("CopyOf" + attrNameTextField.getText());
        attrNameTextField.setSelection(0, attrNameTextField.getText().length());
        attrNameTextField.setEditable(true);
        attrNameTextField.setEnabled(true);
        attrNameTextField.setBackground(attrSetTextArea.getBackground());
        createButton.setVisible(true);
        saveButton.setVisible(false);
        attrNameTextField.setFocus();
    }

    /*
     * (non-Javadoc)
     * 
     * @see gov.noaa.nws.ncep.viz.resourceManager.ui.manageResources.
     * ManageResourceControl
     * .IEditResourceComposite#editSelectedResource(gov.noaa
     * .nws.ncep.viz.resources.manager.ResourceName)
     * 
     * Override superclass method for Edit button clicked
     */
    @Override
    public void editSelectedResource(ResourceName rscName) {
        setSelectedResource(rscName);
        attrNameTextField.setEditable(false);
        attrNameTextField.setEnabled(false);
        createButton.setVisible(false);
        saveButton.setVisible(true);
        attrSetTextArea.setFocus();
    }

    /**
     * Called by copy and edit button to set and validate content
     * 
     * @param resourceName
     */
    public void setSelectedResource(ResourceName resourceName) {
        selectedResourceName = resourceName;

        if (resourceName.getRscAttrSetName().isEmpty()) {
            attrNameTextField.setText("");
            return;
        }

        // Grab the selected attribute set for this resource
        selectedAttributeSet = resourceDefnsManager.getAttrSet(resourceName);

        boolean isUserLevelFile = selectedAttributeSet.getFile().getContext()
                .getLocalizationLevel() == LocalizationLevel.USER;
        if (!isUserLevelFile) {
            if (selectedAttributeSet == null
                    || !selectedAttributeSet.getFile().exists()) {
                attrNameTextField.setText("");
                return;
            }
        }

        selectedResourceDefn = resourceDefnsManager
                .getResourceDefinition(resourceName);

        // Set the name label text for the selected attribute set
        attrNameTextField.setText(resourceName.getRscAttrSetName());

        // Read in the text for the selected attribute set
        InputStream inputStream;
        BufferedInputStream bis;
        ByteArrayOutputStream outStream = null;

        try {

            inputStream = selectedAttributeSet.getFile().openInputStream();
            bis = new BufferedInputStream(inputStream);
            outStream = new ByteArrayOutputStream();
            int result = bis.read();

            while (result != -1) {
                outStream.write((byte) result);
                result = bis.read();
            }

            attrSetTextArea.setText(outStream.toString());

        } catch (LocalizationException e) {
            statusHandler
                    .handle(Priority.PROBLEM,
                            "Localiztion Exception encountered getting attribute set values",
                            e);

        }

        catch (FileNotFoundException fnf) {
            statusHandler.handle(Priority.PROBLEM,
                    "File not found error getting attribute set values", fnf);

        } catch (IOException ioe) {
            statusHandler.handle(Priority.PROBLEM,
                    "IO error getting attribute set values", ioe);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see gov.noaa.nws.ncep.viz.resourceManager.ui.manageResources.
     * ManageResourceControl.IEditResourceComposite#deactivate()
     */
    @Override
    public void deactivate() {
        setVisible(false);
    }

    /*
     * The title for the entire window (non-Javadoc)
     * 
     * @see gov.noaa.nws.ncep.viz.resourceManager.ui.manageResources.
     * ManageResourceControl.IEditResourceComposite#getTitle()
     */
    @Override
    public String getTitle() {
        return "Edit Attribute Set";
    }

    /**
     * 
     * Save the attribute set text we are "copying" or "editing". Save and
     * create buttons invoke this method.
     * 
     * @param isCopy
     *            true if copying an attribute set, false if editing
     */
    private void saveAttrSet(boolean isCopy) {

        // Validate the attribute set
        MessageDialog md = validateAttributeSet(attrSetTextArea.getText());

        // Not null means something went wrong during validation
        if (md != null) {
            md.open();
            return;
        }

        String attrSetName = attrNameTextField.getText().trim();

        // With 'copy', does the new attribute set name
        // already exist ?
        // if it does, display a warning dialog

        if (isCopy) {
            boolean attrSetAlreadyExists;

            if (selectedResourceDefn.applyAttrSetGroups()) {
                List<String> availAttrSetsList = resourceDefnsManager
                        .getAvailAttrSetsForRscImpl(selectedResourceDefn
                                .getRscImplementation());
                attrSetAlreadyExists = availAttrSetsList.contains(attrSetName);

            } else {
                ResourceName newRscName = new ResourceName(selectedResourceName);
                newRscName.setRscAttrSetName(attrSetName);
                attrSetAlreadyExists = (resourceDefnsManager
                        .getAttrSet(newRscName) != null);
            }

            if (attrSetAlreadyExists) {

                md = createMessageDialog(
                        getShell(),
                        "Save Error",
                        "Warning: The Attribute Set "
                                + attrSetName
                                + " already exists! \n Choose a different name? ",
                        MessageDialog.WARNING);
                md.open();
                return;
            }
        }

        // Try to save the attribute set
        try {
            // may throw a VizException
            resourceDefnsManager.saveAttrSet(selectedResourceDefn, attrSetName,
                    attrSetTextArea.getText());

        } catch (VizException e) {

            md = createMessageDialog(NcDisplayMngr.getCaveShell(),
                    "Save Error", "Error Saving Attribute Set, " + attrSetName,
                    MessageDialog.ERROR);
            md.open();
            return;
        }

        if (selectedResourceDefn.applyAttrSetGroups()) {

            AttrSetGroup attrSetGroup = resourceDefnsManager
                    .getAttrSetGroupForResource(selectedResourceName);

            try {
                if (attrSetGroup == null) {
                    throw new VizException("Can't find group for "
                            + selectedResourceName.toString());
                }

                // if the attrSet is not already in the group then add it
                if (!attrSetGroup.getAttrSetNames().contains(attrSetName)) {

                    attrSetGroup.addAttrSetName(attrSetName);

                    // may throw a VizException
                    resourceDefnsManager.saveAttrSetGroup(attrSetGroup);
                }
            } catch (VizException e) {

                md = createMessageDialog(NcDisplayMngr.getCaveShell(),
                        "Save Error", "Error Saving Attribute Set "
                                + attrSetName + " to AttrSetGroup "
                                + attrSetGroup, MessageDialog.ERROR);
                md.open();
            }

        }

        md = createMessageDialog(getShell(), "Info", "The Attribute Set  "
                + attrSetName + "  has been "
                + (isCopy ? "Created!" : "Saved!"), MessageDialog.INFORMATION);
        md.open();

        // After a save, set the column selections to what is currently selected
        ResourceName newSeldRscName = new ResourceName();
        newSeldRscName.setRscCategory(selectedResourceName.getRscCategory());
        newSeldRscName.setRscType(selectedResourceName.getRscType());
        newSeldRscName.setRscGroup(selectedResourceName.getRscGroup());
        newSeldRscName.setRscAttrSetName(attrSetName);
        mngrControl.updateResourceSelections(newSeldRscName);

    }

    /**
     * 
     * Validate an attribute set by writing to a tmp file and parsing the file
     * using a method in ResourceDefnsMngr.
     * 
     * Will throw a VizException if the new attribute set format is not correct.
     * 
     * @param attributeSetText
     *            the attribute set text to validate
     * @throws IOException
     *             on file read/write error
     * @throws VizException
     *             on attribute set parse error
     */
    private MessageDialog validateAttributeSet(String attributeSetText) {

        MessageDialog md;

        try {

            File tmpAttrSetFile = File.createTempFile("tempAttrSet-", ".attr");
            FileWriter fwriter = new FileWriter(tmpAttrSetFile);
            fwriter.write(attributeSetText);
            fwriter.close();

            // Invoke our static attribute set parser method which operates on a
            // File, hence the createTempFile() above
            ResourceDefnsMngr.readAttrSetFile(tmpAttrSetFile);
            tmpAttrSetFile.delete();

        } catch (IOException e) {

            md = createMessageDialog(getShell(), "IO Error",
                    "IO Error Validating Attribute Set", MessageDialog.ERROR);
            return md;

        } catch (VizException v) {

            md = createMessageDialog(
                    getShell(),
                    "Parse Error",
                    "Warning: Format/Parse Error while Validating Attribute Set \n Check format of Attribute Set ?",
                    MessageDialog.WARNING);
            return md;
        }

        // this means "OK"
        return null;
    }

    private MessageDialog createMessageDialog(Shell parentShell, String title,
            String dialogMessage, int dialogImageType) {
        return new MessageDialog(parentShell, title, null, dialogMessage,
                dialogImageType, new String[] { "OK" }, 0);
    }

}
