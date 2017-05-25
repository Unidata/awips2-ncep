/*
 * EditAsdiAttrDialog
 * 
 * Date created (November 08, 2010)
 * 
 *  This code has been developed by the SIB for use in the AWIPS2 system. 
 */
package gov.noaa.nws.ncep.viz.rsc.asdi.rsc;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Shell;

import com.raytheon.uf.viz.core.rsc.capabilities.Capabilities;

import gov.noaa.nws.ncep.viz.resources.INatlCntrsResourceData;
import gov.noaa.nws.ncep.viz.resources.attributes.AbstractEditResourceAttrsDialog;
import gov.noaa.nws.ncep.viz.resources.attributes.ResourceAttrSet.RscAttrValue;
import gov.noaa.nws.ncep.viz.resources.colorBar.ColorBarEditor;
import gov.noaa.nws.ncep.viz.ui.display.ColorBar;
import gov.noaa.nws.ncep.viz.ui.display.NcDisplayMngr;

//import gov.noaa.nws.ncep.edex.plugin.aww.util;

/**
 * Creates a dialog box to edit the ASDI resource
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 04/24/2017   R28579     R.Reynolds  Initial coding.
 * 
 * </pre>
 */

public class EditAsdiAttrsDialog extends AbstractEditResourceAttrsDialog {

    private static final String DEPART_ARRIVE = "departArrive";

    private static final String ALL_AIRPORTS = "allAirports";

    private static final String COLORBAR_HEIGHT = "colorBarHeight";

    private static final String COLORBAR_TIME = "colorBarTime";

    private static final String TIME_LIMIT_VALUE = "timeLimitValue";

    ColorBarEditor colorBarEditor = null;

    Button toggleDepartures = null;

    Button toggleArrivals = null;

    Button toggleBoth = null;

    Label label = null;

    private List allAirportsList = null;

    private Button allAirportsBtn = null;

    private Button arrivalsBtn = null;

    private Button departuresBtn = null;

    private Button bothBtn = null;

    private RscAttrValue allAirportsAttr = null;

    private RscAttrValue departArriveAttr = null;

    private RscAttrValue timeLimitValueAttr = null;

    RscAttrValue colorBarHeightAttr;

    RscAttrValue colorBarTimeAttr;

    private String[] selectedAirportNames = null;

    Boolean check = false;

    AsdiResourceData rscData = null;

    public EditAsdiAttrsDialog(Shell parentShell, INatlCntrsResourceData r,
            Capabilities capabilities, Boolean apply) {
        super(parentShell, r, capabilities, apply);
        rscData = (AsdiResourceData) r;
    }

    @Override
    public Composite createDialog(Composite topComp) {

        checkAttributeValues();

        topComp.setLayout(new FormLayout());

        // Color Bar - Main
        Group colorBarGroup = new Group(topComp, SWT.NONE);
        // colorBarGroup.setText("Edit Main Color Bar");
        FormData fd = new FormData();
        fd.left = new FormAttachment(0, 15);
        fd.right = new FormAttachment(100, -15);
        fd.top = new FormAttachment(0, 32);
        colorBarGroup.setLayoutData(fd);
        colorBarGroup.setLayout(new FormLayout());
        ColorBar editedColorBar = null;

        if (rscData.plotByHeight()) {
            editedColorBar = (ColorBar) colorBarHeightAttr.getAttrValue();
            colorBarGroup.setText(" Edit Main Color Bar - ASDI_H ");

        } else {
            editedColorBar = (ColorBar) colorBarTimeAttr.getAttrValue();
            colorBarGroup.setText(" Edit Main Color Bar - ASDI_T ");
        }
        colorBarEditor = new ColorBarEditor(colorBarGroup, editedColorBar,
                true);

        // scale for time length
        Group scaleGroup = new Group(topComp, SWT.NONE);
        fd = new FormData();
        fd.left = new FormAttachment(0, 15);
        fd.right = new FormAttachment(50, 15);
        fd.top = new FormAttachment(colorBarGroup, 10, SWT.BOTTOM);
        scaleGroup.setLayoutData(fd);
        scaleGroup.setLayout(new FormLayout());
        Scale scale = new Scale(scaleGroup, SWT.BORDER);
        fd = new FormData();
        fd.left = new FormAttachment(1, 0);
        fd.right = new FormAttachment(100, 0);
        fd.top = new FormAttachment(scaleGroup, 1, SWT.TOP);
        scale.setLayoutData(fd);
        scale.setMaximum(15);
        scale.setMinimum(1);
        scale.setIncrement(1);
        scale.setPageIncrement(1);
        scale.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event event) {

                scaleGroup.setText(

                        "Time Limit (Min) " + scale.getSelection() + "");
                timeLimitValueAttr.setAttrValue(scale.getSelection() + 0);
            }
        });

        Integer tLimit = (Integer) timeLimitValueAttr.getAttrValue();
        if (tLimit > scale.getMaximum()) {
            tLimit = (Integer) scale.getMaximum();
        }
        scale.setSelection((Integer) timeLimitValueAttr.getAttrValue());
        scale.notifyListeners(SWT.Selection, new Event());

        // depart/arrive
        Group departArriveGroup = new Group(topComp, SWT.NONE);
        fd = new FormData();
        fd.left = new FormAttachment(0, 15);
        fd.right = new FormAttachment(50, 15);
        fd.top = new FormAttachment(scaleGroup, 15, SWT.BOTTOM);
        departArriveGroup.setLayoutData(fd);
        departArriveGroup.setLayout(new RowLayout(SWT.HORIZONTAL));

        departuresBtn = new Button(departArriveGroup, SWT.RADIO);
        // departuresBtn.setSelection(true);
        this.departuresBtn.setText("Departures");
        this.departuresBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                Integer value = 0;
                departArriveAttr.setAttrValue(value);
                allAirportsBtn.setFocus();
            }
        });

        arrivalsBtn = new Button(departArriveGroup, SWT.RADIO);

        arrivalsBtn.setText("Arrivals");
        arrivalsBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                Integer value = 1;
                departArriveAttr.setAttrValue(value);
                allAirportsBtn.setFocus();
            }
        });

        bothBtn = new Button(departArriveGroup, SWT.RADIO);
        bothBtn.setText("Both");
        bothBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                Integer value = 2;
                departArriveAttr.setAttrValue(value);
                allAirportsBtn.setFocus();
            }
        });
        int value = (Integer) departArriveAttr.getAttrValue();
        if (value == 0) {
            departuresBtn.setSelection(true);
        } else if (value == 1) {
            arrivalsBtn.setSelection(true);
        } else {
            bothBtn.setSelection(true);
        }

        // airport label and select-all check box
        Composite compositeAirportsSelectAll = new Composite(topComp, SWT.NONE);
        fd = new FormData();
        fd.left = new FormAttachment(0, 15);
        fd.right = new FormAttachment(50, 15);
        fd.top = new FormAttachment(departArriveGroup, 10, SWT.BOTTOM);
        compositeAirportsSelectAll.setLayoutData(fd);
        compositeAirportsSelectAll.setLayout(new RowLayout(SWT.HORIZONTAL));

        label = new Label(compositeAirportsSelectAll, SWT.NONE);
        label.setText("Airports");

        allAirportsBtn = new Button(compositeAirportsSelectAll, SWT.CHECK);
        allAirportsBtn.setText("Select All");
        allAirportsBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                allAirportsAttr.setAttrValue(allAirportsBtn.getSelection());

                if (allAirportsBtn.getSelection()) {
                    allAirportsList.selectAll();
                } else {
                    allAirportsList.deselectAll();
                    allAirportsList.select(0);
                }
                allAirportsList.notifyListeners(SWT.Selection, new Event());

            }
        });

        allAirportsBtn.setSelection((Boolean) allAirportsAttr.getAttrValue());

        // list of all airports
        allAirportsList = new List(topComp,
                SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);

        fd = new FormData();
        fd.left = new FormAttachment(0, 15);
        fd.right = new FormAttachment(30, 15);
        fd.top = new FormAttachment(compositeAirportsSelectAll, 10, SWT.BOTTOM);
        fd.bottom = new FormAttachment(100, -30);
        allAirportsList.setLayoutData(fd);

        java.util.List<String> apts = AirportNames.getAirports(); // rscData.getAirportNames();
        for (int index = 0; index < apts.size(); index++) {
            allAirportsList.add(apts.get(index));
        }

        java.util.List<String> initialSelectedAirports = rscData
                .getSelectedAirportNames();

        allAirportsList
                .setSelection(initialSelectedAirports.toArray(new String[0]));

        selectedAirportNames = allAirportsList.getSelection();

        allAirportsList.addSelectionListener(new SelectionListener() {

            public void widgetSelected(SelectionEvent event) {

                if (allAirportsBtn.getSelection()) {
                    allAirportsList.selectAll();
                }

                selectedAirportNames = allAirportsList.getSelection();

            }

            public void widgetDefaultSelected(SelectionEvent event) {

                if (allAirportsBtn.getSelection()) {
                    allAirportsList.selectAll();
                }
                selectedAirportNames = allAirportsList.getSelection();

            }
        });

        Boolean bvalue = (Boolean) allAirportsAttr.getAttrValue();
        if (bvalue) {
            if (allAirportsBtn.getSelection()) {
                allAirportsList.selectAll();
            } else {
                allAirportsList.deselectAll();
                allAirportsList.select(0);
            }
            allAirportsList.notifyListeners(SWT.Selection, new Event());
            rscData.setSelectedAirportNames(selectedAirportNames);
        }

        return topComp;
    }

    /**
     * sets attributes values
     * 
     */
    private void checkAttributeValues() {

        departArriveAttr = editedRscAttrSet.getRscAttr(DEPART_ARRIVE);

        allAirportsAttr = editedRscAttrSet.getRscAttr(ALL_AIRPORTS);

        timeLimitValueAttr = editedRscAttrSet.getRscAttr(TIME_LIMIT_VALUE);

        colorBarHeightAttr = editedRscAttrSet.getRscAttr(COLORBAR_HEIGHT);

        colorBarTimeAttr = editedRscAttrSet.getRscAttr(COLORBAR_TIME);

    }

    /*
     * (non-Javadoc)
     * 
     * @see gov.noaa.nws.ncep.viz.resources.attributes.
     * AbstractEditResourceAttrsDialog #initWidgets()
     */
    @Override
    public void initWidgets() {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc) override of cancel, OK buttons on Attribute dialog so
     * changes can be stored in resourceData
     * 
     * @see gov.noaa.nws.ncep.viz.resources.attributes.
     * AbstractEditResourceAttrsDialog#createShell()
     */
    @Override
    public void createShell() {
        int style = SWT.DIALOG_TRIM | SWT.RESIZE;
        if (!hasApplyBtn) {
            style |= SWT.APPLICATION_MODAL;
        }

        shell = new Shell(getParent(), style);
        shell.setText(dlgTitle);

        GridLayout mainLayout = new GridLayout(1, true);
        mainLayout.marginHeight = 1;
        mainLayout.marginWidth = 1;
        shell.setLayout(mainLayout);

        Composite topComp = new Composite(shell, SWT.NONE);
        topComp.setLayout(new GridLayout());
        GridData gd = new GridData();
        gd.grabExcessHorizontalSpace = true;
        gd.grabExcessVerticalSpace = true;
        gd.horizontalAlignment = SWT.FILL;
        gd.verticalAlignment = SWT.FILL;
        topComp.setLayoutData(gd);

        createDialog(topComp);
        Label sep = new Label(shell, SWT.SEPARATOR | SWT.HORIZONTAL);
        gd = new GridData();
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalAlignment = SWT.FILL;
        sep.setLayoutData(gd);

        Composite okCanComp = new Composite(shell, SWT.NONE);
        gd = new GridData();
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalAlignment = SWT.CENTER;
        okCanComp.setLayoutData(gd);

        okCanComp.setLayout(new GridLayout((hasApplyBtn ? 3 : 2), true));

        Button canBtn = new Button(okCanComp, SWT.PUSH);
        canBtn.setText(" Cancel ");

        canBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                rscData.setCancelBtnPressed(true);
                ok = false;
                shell.dispose();
            }
        });

        if (hasApplyBtn) {
            Button applyBtn = new Button(okCanComp, SWT.PUSH);
            applyBtn.setText(" Apply ");

            applyBtn.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    rscData.setSelectedAirportNames(selectedAirportNames);
                    rscData.setCancelBtnPressed(false);
                    rscData.setRscAttrSet(editedRscAttrSet);
                    rscData.setIsEdited(true);
                    NcDisplayMngr.getActiveNatlCntrsEditor().refresh();
                }
            });
        }

        Button okBtn = new Button(okCanComp, SWT.PUSH);
        okBtn.setText("    OK    ");

        okBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                rscData.setSelectedAirportNames(selectedAirportNames);
                rscData.setCancelBtnPressed(false);
                rscData.setIsEdited(true);
                handleOK();
            }
        });
    }

    @Override
    protected void dispose() {
        super.dispose();
        colorBarEditor.dispose();
    }

}
