/**
 * This code has unlimited rights, and is provided "as is" by the National Centers 
 * for Environmental Prediction, without warranty of any kind, either expressed or implied, 
 * including but not limited to the implied warranties of merchantability and/or fitness 
 * for a particular purpose.
 * 
 * This code has been developed by the NCEP-SIB for use in the AWIPS2 system.
 * 
 **/
package gov.noaa.nws.ncep.viz.common.tsScaleMngr;

import gov.noaa.nws.ncep.viz.localization.NcPathManager;
import gov.noaa.nws.ncep.viz.localization.NcPathManager.NcPathConstants;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

/**
 * UI for editing XAxis Scale and XAxis Scale Elements.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#    Engineer    Description
 * ------------  ---------- ----------- --------------------------
 * Sep 22, 2014  R4875      sgurung     Initial creation
 * 
 * </pre>
 * 
 * @author sgurung
 * @version 1.0
 */

public class EditXAxisScaleComposite extends Composite {

    private XAxisScale editedXAxisScale = null;

    private ArrayList<Combo> durationComboList = null;

    private ArrayList<Combo> labelIntervalComboList = null;

    private ArrayList<Combo> majorTickIntervalComboList = null;

    private ArrayList<Combo> minorTickIntervalComboList = null;

    private ArrayList<Combo> labelFormatComboList = null;

    private ArrayList<Button> delBtnList = null;

    private Composite topComposite = null;

    private Group scConditionsGrp = null;

    private File minusImageFile = NcPathManager.getInstance().getStaticFile(
            NcPathConstants.CONDITIONAL_FILTER_MINUS_IMG);

    private File plusImageFile = NcPathManager.getInstance().getStaticFile(
            NcPathConstants.CONDITIONAL_FILTER_PLUS_IMG);

    private GridData gd = null;

    private String availDurationStrings[] = { "1 hr", "2 hrs", "3 hrs",
            "6 hrs", "12 hrs", "24 hrs", "3 days", "7 days", "30 days" };

    private int availDurationHours[] = { 1, 2, 3, 6, 12, 24, 72, 168, 720 };

    private String availLabelIntervalStrings[] = { "1 min", "2 mins", "5 mins",
            "10 mins", "15 mins", "20 mins", "30 mins", "1 hr", "90 mins",
            "2 hrs", "3 hrs", "6 hrs", "12 hrs", "24 hrs", "3 days" };

    private int availLabelIntervalMins[] = { 1, 2, 5, 10, 15, 20, 30, 60, 90,
            120, 180, 360, 720, 1440, 4320 };

    private String availMajorTickIntervalStrings[] = { "1 min", "2 mins",
            "5 mins", "10 mins", "15 mins", "20 mins", "30 mins", "1 hr",
            "90 mins", "2 hrs", "3 hrs", "6 hrs", "12 hrs", "24 hrs", "3 days" };

    private int availMajorTickIntervalMins[] = { 1, 2, 5, 10, 15, 20, 30, 60,
            90, 120, 180, 360, 720, 1440, 4320 };

    private String availMinorTickIntervalStrings[] = { "1 min", "2 mins",
            "5 mins", "10 mins", "15 mins", "20 mins", "30 mins", "1 hr",
            "90 mins", "2 hrs", "3 hrs", "6 hrs", "12 hrs", "24 hrs", "3 days" };

    private int availMinorTickIntervalMins[] = { 1, 2, 5, 10, 15, 20, 30, 60,
            90, 120, 180, 360, 720, 1440, 4320 };

    private String availLabelFormatStrings[] = { "mm", "HH", "HHmm", "HH:mm",
            "MM-dd-HH", "MM-dd HHmm", "MM-dd HH:mm", "MMM dd", "MMM dd HH:mm" };

    private Set<Integer> selectedDurations = new HashSet<Integer>();

    public EditXAxisScaleComposite(Composite parent, int style, XAxisScale cf) {
        super(parent, style);
        editedXAxisScale = cf;

        topComposite = this;

        GridLayout mainLayout = new GridLayout(1, true);
        mainLayout.marginHeight = 1;
        mainLayout.marginWidth = 1;
        mainLayout.verticalSpacing = 5;
        topComposite.setLayout(mainLayout);

        createControls();

        initWidgets();
    }

    /*
     * Create XAxis Scale elements with attributes: plotParamName,
     * constraintType and value
     */
    private void createControls() {
        if (editedXAxisScale == null) {
            System.out.println("XAxis Scale to Edit is not set???");
            return;
        }

        scConditionsGrp = new Group(topComposite, SWT.SHADOW_NONE);

        GridLayout gl2 = new GridLayout(6, false);
        gl2.marginTop = 7;
        gl2.marginBottom = 8;
        gl2.marginRight = 8;
        gl2.horizontalSpacing = 30;
        scConditionsGrp.setLayout(gl2);
        scConditionsGrp.setText("X-axis Scale Values");

        gd = new GridData();
        gd.heightHint = 25;
        gd.widthHint = 25;
        Button addBtn = new Button(scConditionsGrp, SWT.TOGGLE);
        addBtn.setToolTipText("Add New Scale");
        addBtn.setLayoutData(gd);

        if (plusImageFile != null && plusImageFile.exists()) {
            addBtn.setImage(new Image(Display.getCurrent(), plusImageFile
                    .getAbsolutePath()));
        } else {
            addBtn.setText("+");
        }

        addBtn.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                addxAxisScale();
            }
        });

        new Label(scConditionsGrp, SWT.NONE).setText("Duration");
        new Label(scConditionsGrp, SWT.NONE).setText("Label Interval");
        new Label(scConditionsGrp, SWT.NONE).setText("Major Tick Interval");
        new Label(scConditionsGrp, SWT.NONE).setText("Minor Tick Interval");
        new Label(scConditionsGrp, SWT.NONE).setText("Label Date/Time Format");

        delBtnList = new ArrayList<Button>();
        durationComboList = new ArrayList<Combo>();
        labelIntervalComboList = new ArrayList<Combo>();
        majorTickIntervalComboList = new ArrayList<Combo>();
        minorTickIntervalComboList = new ArrayList<Combo>();
        labelFormatComboList = new ArrayList<Combo>();

        Button delBtn = null;
        Combo durationCombo = null;
        Combo labelIntervalCombo = null;
        Combo majorTickIntervalCombo = null;
        Combo minorTickIntervalCombo = null;
        Combo labelFormatCombo = null;

        for (int i = 0; i < editedXAxisScale.getSize(); i++) {

            delBtn = createDeleteBtn();
            delBtnList.add(delBtn);

            durationCombo = new Combo(scConditionsGrp, SWT.DROP_DOWN
                    | SWT.READ_ONLY);
            durationComboList.add(durationCombo);

            labelIntervalCombo = new Combo(scConditionsGrp, SWT.DROP_DOWN
                    | SWT.READ_ONLY);
            labelIntervalComboList.add(labelIntervalCombo);

            majorTickIntervalCombo = new Combo(scConditionsGrp, SWT.DROP_DOWN
                    | SWT.READ_ONLY);
            majorTickIntervalComboList.add(majorTickIntervalCombo);

            minorTickIntervalCombo = new Combo(scConditionsGrp, SWT.DROP_DOWN
                    | SWT.READ_ONLY);
            minorTickIntervalComboList.add(minorTickIntervalCombo);

            labelFormatCombo = new Combo(scConditionsGrp, SWT.DROP_DOWN
                    | SWT.READ_ONLY);
            labelFormatComboList.add(labelFormatCombo);
        }

    }

    public void initWidgets() {

        for (int i = 0; i < editedXAxisScale.getSize(); i++) {
            durationComboList.get(i).setItems(availDurationStrings);

            labelIntervalComboList.get(i).setItems(availLabelIntervalStrings);

            majorTickIntervalComboList.get(i).setItems(
                    availMajorTickIntervalStrings);

            minorTickIntervalComboList.get(i).setItems(
                    availMinorTickIntervalStrings);

            labelFormatComboList.get(i).setItems(availLabelFormatStrings);

            if (editedXAxisScale.getXAxisScaleElement(i) != null) {

                for (int j = 0; j < availDurationStrings.length; j++) {
                    if (editedXAxisScale.getXAxisScaleElement(i).getDuration() == availDurationHours[j]) {
                        durationComboList.get(i).select(j);
                        break;
                    }
                }

                for (int j = 0; j < availLabelIntervalStrings.length; j++) {
                    if (editedXAxisScale.getXAxisScaleElement(i)
                            .getLabelInterval() == availLabelIntervalMins[j]) {
                        labelIntervalComboList.get(i).select(j);
                        break;
                    }
                }

                for (int j = 0; j < availMajorTickIntervalStrings.length; j++) {
                    if (editedXAxisScale.getXAxisScaleElement(i)
                            .getMajorTickInterval() == availMajorTickIntervalMins[j]) {
                        majorTickIntervalComboList.get(i).select(j);
                        break;
                    }
                }

                for (int j = 0; j < availMinorTickIntervalStrings.length; j++) {
                    if (editedXAxisScale.getXAxisScaleElement(i)
                            .getMinorTickInterval() == availMinorTickIntervalMins[j]) {
                        minorTickIntervalComboList.get(i).select(j);
                        break;
                    }
                }

                for (int j = 0; j < availLabelFormatStrings.length; j++) {
                    String ct = editedXAxisScale.getXAxisScaleElement(i)
                            .getLabelFormat();
                    if (availLabelFormatStrings[j].equals(ct)) {
                        labelFormatComboList.get(i).select(j);
                        break;
                    }
                }
            }
        }

        for (int i = 0; i < delBtnList.size(); i++) {

            delBtnList.get(i).addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent event) {
                    deletexAxisScale();
                }
            });
        }

        for (int i = 0; i < durationComboList.size(); i++) {

            durationComboList.get(i).addSelectionListener(
                    new SelectionAdapter() {
                        public void widgetSelected(SelectionEvent event) {
                            updateDuration();
                        }
                    });
        }

        for (int i = 0; i < labelIntervalComboList.size(); i++) {

            labelIntervalComboList.get(i).addSelectionListener(
                    new SelectionAdapter() {
                        public void widgetSelected(SelectionEvent event) {
                            updateLabelInterval();
                        }
                    });
        }

        for (int i = 0; i < majorTickIntervalComboList.size(); i++) {

            majorTickIntervalComboList.get(i).addSelectionListener(
                    new SelectionAdapter() {
                        public void widgetSelected(SelectionEvent event) {
                            updateMajorTickInterval();
                        }
                    });
        }

        for (int i = 0; i < minorTickIntervalComboList.size(); i++) {

            minorTickIntervalComboList.get(i).addSelectionListener(
                    new SelectionAdapter() {
                        public void widgetSelected(SelectionEvent event) {
                            updateMinorTickInterval();
                        }
                    });
        }

        for (int i = 0; i < labelFormatComboList.size(); i++) {

            labelFormatComboList.get(i).addSelectionListener(
                    new SelectionAdapter() {
                        public void widgetSelected(SelectionEvent event) {
                            updateLabelFormat();
                        }
                    });
        }

    }

    public void addxAxisScale() {

        Button delBtn = createDeleteBtn();
        delBtnList.add(delBtn);
        delBtn.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                deletexAxisScale();
            }
        });

        Combo durationCombo = new Combo(scConditionsGrp, SWT.DROP_DOWN
                | SWT.READ_ONLY);
        durationCombo.setItems(availDurationStrings);
        durationComboList.add(durationCombo);
        durationCombo.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                updateDuration();
            }
        });

        Combo labelIntervalCombo = new Combo(scConditionsGrp, SWT.DROP_DOWN
                | SWT.READ_ONLY);
        labelIntervalCombo.setItems(availLabelIntervalStrings);
        labelIntervalComboList.add(labelIntervalCombo);
        labelIntervalCombo.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                updateLabelInterval();
            }
        });

        Combo majorTickIntervalCombo = new Combo(scConditionsGrp, SWT.DROP_DOWN
                | SWT.READ_ONLY);
        majorTickIntervalCombo.setItems(availMajorTickIntervalStrings);
        majorTickIntervalComboList.add(majorTickIntervalCombo);
        majorTickIntervalCombo.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                updateMajorTickInterval();
            }
        });

        Combo minorTickIntervalCombo = new Combo(scConditionsGrp, SWT.DROP_DOWN
                | SWT.READ_ONLY);
        minorTickIntervalCombo.setItems(availMinorTickIntervalStrings);
        minorTickIntervalComboList.add(minorTickIntervalCombo);
        minorTickIntervalCombo.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                updateMinorTickInterval();
            }
        });

        Combo labelFormatCombo = new Combo(scConditionsGrp, SWT.DROP_DOWN
                | SWT.READ_ONLY);
        labelFormatCombo.setItems(availLabelFormatStrings);
        labelFormatComboList.add(labelFormatCombo);
        labelFormatCombo.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                updateLabelFormat();
            }
        });

        editedXAxisScale.getXAxisScaleElements().add(new XAxisScaleElement());

        this.getShell().pack();
    }

    private Button createDeleteBtn() {
        gd = new GridData();
        gd.heightHint = 25;
        gd.widthHint = 25;
        Button delBtn = new Button(scConditionsGrp, SWT.TOGGLE);

        if (minusImageFile != null && minusImageFile.exists()) {
            delBtn.setImage(new Image(Display.getCurrent(), minusImageFile
                    .getAbsolutePath()));
        } else {
            delBtn.setText("-");
        }

        delBtn.setToolTipText("Delete Scale");
        delBtn.setLayoutData(gd);

        return delBtn;
    }

    public void deletexAxisScale() {

        for (int i = 0; i < editedXAxisScale.getSize(); i++) {

            if (delBtnList.get(i).getSelection()) {

                editedXAxisScale.getXAxisScaleElements().remove(
                        editedXAxisScale.getXAxisScaleElement(i));

                delBtnList.get(i).dispose();
                durationComboList.get(i).dispose();
                labelIntervalComboList.get(i).dispose();
                majorTickIntervalComboList.get(i).dispose();
                minorTickIntervalComboList.get(i).dispose();
                labelFormatComboList.get(i).dispose();

                delBtnList.remove(i);
                durationComboList.remove(i);
                labelIntervalComboList.remove(i);
                majorTickIntervalComboList.remove(i);
                minorTickIntervalComboList.remove(i);
                labelFormatComboList.remove(i);
            }
        }

        this.getShell().pack();
    }

    public void updateDuration() {

        for (int i = 0; i < editedXAxisScale.getSize(); i++) {

            for (int j = 0; j < availDurationStrings.length; j++) {

                if (availDurationStrings[j].equals(durationComboList.get(i)
                        .getText())) {

                    if (durationComboList.get(i).isFocusControl()
                            && selectedDurations
                                    .contains(availDurationHours[j])) {

                        MessageDialog infoDlg = new MessageDialog(
                                this.getShell(), "Information", null,
                                "The selected duration '"
                                        + availDurationStrings[j]
                                        + "' already exists.\n",
                                MessageDialog.INFORMATION,
                                new String[] { "OK" }, 0);
                        infoDlg.open();

                        if (infoDlg.getReturnCode() == MessageDialog.CANCEL) {
                            return;
                        }

                        break;
                    }

                    editedXAxisScale.getXAxisScaleElement(i).setDuration(
                            availDurationHours[j]);
                    selectedDurations.add(availDurationHours[j]);
                    break;

                }
            }
        }
    }

    public void updateLabelInterval() {

        for (int i = 0; i < editedXAxisScale.getSize(); i++) {

            for (int j = 0; j < availLabelIntervalStrings.length; j++) {
                if (availLabelIntervalStrings[j].equals(labelIntervalComboList
                        .get(i).getText())) {
                    editedXAxisScale.getXAxisScaleElement(i).setLabelInterval(
                            availLabelIntervalMins[j]);
                    break;
                }
            }
        }
    }

    public void updateMajorTickInterval() {

        for (int i = 0; i < editedXAxisScale.getSize(); i++) {
            for (int j = 0; j < availMajorTickIntervalStrings.length; j++) {
                if (availMajorTickIntervalStrings[j]
                        .equals(majorTickIntervalComboList.get(i).getText())) {
                    editedXAxisScale
                            .getXAxisScaleElement(i)
                            .setMajorTickInterval(availMajorTickIntervalMins[j]);
                    break;
                }
            }
        }
    }

    public void updateMinorTickInterval() {

        for (int i = 0; i < editedXAxisScale.getSize(); i++) {
            for (int j = 0; j < availMinorTickIntervalStrings.length; j++) {
                if (availMinorTickIntervalStrings[j]
                        .equals(minorTickIntervalComboList.get(i).getText())) {
                    editedXAxisScale
                            .getXAxisScaleElement(i)
                            .setMinorTickInterval(availMinorTickIntervalMins[j]);
                    break;
                }
            }
        }
    }

    public void updateLabelFormat() {

        for (int i = 0; i < editedXAxisScale.getSize(); i++) {
            editedXAxisScale.getXAxisScaleElement(i).setLabelFormat(
                    labelFormatComboList.get(i).getText());
        }
    }

    XAxisScale getEditedXAxisScale() {
        return editedXAxisScale;
    }

}
