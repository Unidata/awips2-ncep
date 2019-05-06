/**
 * This software was developed and / or modified by Raytheon Company,
 * pursuant to Contract DG133W-05-CQ-1067 with the US Government.
 *
 * U.S. EXPORT CONTROLLED TECHNICAL DATA
 * This software product contains export-restricted data whose
 * export/transfer/disclosure is restricted by U.S. law. Dissemination
 * to non-U.S. persons whether in the United States or abroad requires
 * an export license or other authorization.
 *
 * Contractor Name:        Raytheon Company
 * Contractor Address:     6825 Pine Street, Suite 340
 *                         Mail Stop B8
 *                         Omaha, NE 68106
 *                         402.291.0100
 *
 * See the AWIPS II Master Rights File ("Master Rights File.pdf") for
 * further licensing information.
 **/
package gov.noaa.nws.ncep.ui.nsharp.view;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.raytheon.viz.ui.dialogs.CaveJFACEDialog;

import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingLayer;
import gov.noaa.nws.ncep.ui.nsharp.display.NsharpEditor;
import gov.noaa.nws.ncep.ui.nsharp.display.rsc.NsharpResourceHandler;

/**
 * gov.noaa.nws.ncep.ui.nsharp.palette.NsharpEditDataDialog
 *
 * This java class performs the NSHARP NsharpLoadDialog functions. This code has
 * been developed by the NCEP-SIB for use in the AWIPS2 system.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 04/26/2011    229       Chin Chen    Initial coding
 * 07/16/2014    TTR828    Chin Chen    swapped wind direction and wind speed lines at edit dialog
 * 08/06/2014    TTR828    Chin Chen    Set "add new level" as default selection
 * Aug 20, 2018  #7081     dgilling     Refactor based on CaveJFACEDialog.
 * Oct 01, 2018  7478      bsteffen     Allow 360 as a valid wind direction.
 *
 * </pre>
 *
 * @author Chin Chen
 */

public class NsharpEditDataDialog extends CaveJFACEDialog {

    private static enum EditType {
        SELECTED_LEVEL, NEW_LEVEL
    }

    private static final String MISSING_VALUE = " N/A ";

    private static final int APPLY_ID = IDialogConstants.CLIENT_ID + 1;

    private java.util.List<NcSoundingLayer> curSoundingLayerList;

    private final Color colorBlue;

    private final Color colorLightGrey;

    private final VerifyListener postiveFloatVerifyListener = (e) -> {
        String string = e.text;
        for (int i = 0; i < string.length(); i++) {
            if (!('0' <= string.charAt(i) && string.charAt(i) <= '9')
                    && string.charAt(i) != '.') {
                e.doit = false;
                return;
            }
        }
    };

    private final VerifyListener floatVerifyListener = (e) -> {
        String string = e.text;
        for (int i = 0; i < string.length(); i++) {
            if (!('0' <= string.charAt(i) && string.charAt(i) <= '9')
                    && string.charAt(i) != '.' && string.charAt(i) != '-') {
                e.doit = false;
                return;
            }
        }
    };

    private List pressureList;

    private Text curPressText;

    private Text newPressText;

    private Text curTempText;

    private Text newTempText;

    private Text curWDirText;

    private Text curDewText;

    private Text newDewText;

    private Text newWDirText;

    private Text curWSpText;

    private Text newWSpText;

    private EditType currentEditType;

    public NsharpEditDataDialog(Shell parentShell) {
        super(parentShell);

        this.colorBlue = new Color(parentShell.getDisplay(), 135, 206, 235);
        this.colorLightGrey = new Color(parentShell.getDisplay(), 211, 211,
                211);
    }

    private void handleListSelection() {
        if (currentEditType == EditType.SELECTED_LEVEL) {
            NcSoundingLayer selLevelSounding = curSoundingLayerList
                    .get(pressureList.getSelectionIndex());
            curTempText
                    .setText(Float.toString(selLevelSounding.getTemperature()));
            curDewText.setText(Float.toString(selLevelSounding.getDewpoint()));
            curWSpText.setText(Float.toString(selLevelSounding.getWindSpeed()));
            curWDirText.setText(
                    Float.toString(selLevelSounding.getWindDirection()));
            curPressText
                    .setText(Float.toString(selLevelSounding.getPressure()));
            newTempText
                    .setText(Float.toString(selLevelSounding.getTemperature()));
            newDewText.setText(Float.toString(selLevelSounding.getDewpoint()));
            newWSpText.setText(Float.toString(selLevelSounding.getWindSpeed()));
            newWDirText.setText(
                    Float.toString(selLevelSounding.getWindDirection()));
            newPressText
                    .setText(Float.toString(selLevelSounding.getPressure()));
        }
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);
        composite.setLayout(new GridLayout());

        Composite topComposite = new Composite(composite, SWT.NONE);
        topComposite.setLayout(new GridLayout(2, false));
        topComposite
                .setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        Group pressureListGp = new Group(topComposite, SWT.DEFAULT);
        pressureListGp.setText("Pressure Level:");
        pressureListGp.setLayout(new GridLayout());
        pressureListGp
                .setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        pressureList = new List(pressureListGp, SWT.BORDER | SWT.V_SCROLL);
        if (NsharpEditor.getActiveNsharpEditor() != null) {
            NsharpResourceHandler rsc = NsharpEditor.getActiveNsharpEditor()
                    .getRscHandler();
            if (rsc != null && rsc.getSoundingLys() != null) {
                curSoundingLayerList = rsc.getSoundingLys();
                for (NcSoundingLayer layer : curSoundingLayerList) {
                    pressureList.add(Float.toString(layer.getPressure()));
                }
            }
        }
        pressureList.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                handleListSelection();
            }
        });
        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        Rectangle trim = pressureList.computeTrim(0, 0, 1,
                pressureList.getItemHeight() * 5);
        gd.heightHint = trim.height;
        pressureList.setLayoutData(gd);

        currentEditType = EditType.NEW_LEVEL;

        Group editButtonGroup = new Group(topComposite, SWT.DEFAULT);
        editButtonGroup.setText("Edit Option:");
        editButtonGroup.setLayout(new GridLayout());
        editButtonGroup
                .setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));

        Button editLevelButton = new Button(editButtonGroup, SWT.PUSH);
        editLevelButton.setText("Edit Selected Level");
        editLevelButton.setBackground(colorLightGrey);
        editLevelButton.setLayoutData(
                new GridData(SWT.FILL, SWT.DEFAULT, true, false));

        Button addLevelButton = new Button(editButtonGroup, SWT.PUSH);
        addLevelButton.setText("Add New Level");
        addLevelButton.setBackground(colorBlue);
        addLevelButton.setLayoutData(
                new GridData(SWT.FILL, SWT.DEFAULT, true, false));

        editLevelButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                currentEditType = EditType.SELECTED_LEVEL;
                editLevelButton.setBackground(colorBlue);
                addLevelButton.setBackground(colorLightGrey);
                handleListSelection();
            }
        });
        addLevelButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                currentEditType = EditType.NEW_LEVEL;
                editLevelButton.setBackground(colorLightGrey);
                addLevelButton.setBackground(colorBlue);
                curTempText.setText(MISSING_VALUE);
                curDewText.setText(MISSING_VALUE);
                curWSpText.setText(MISSING_VALUE);
                curWDirText.setText(MISSING_VALUE);
                curPressText.setText(MISSING_VALUE);
                newTempText.setText(StringUtils.EMPTY);
                newDewText.setText(StringUtils.EMPTY);
                newWSpText.setText(StringUtils.EMPTY);
                newWDirText.setText(StringUtils.EMPTY);
                newPressText.setText(StringUtils.EMPTY);
            }
        });

        Composite entryComposite = new Composite(composite, SWT.NONE);
        entryComposite.setLayout(new GridLayout(3, false));
        entryComposite
                .setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        Label placeholderLbl = new Label(entryComposite, SWT.NONE);
        placeholderLbl.setLayoutData(
                new GridData(SWT.FILL, SWT.DEFAULT, true, false));

        Label curlbl = new Label(entryComposite, SWT.NONE);
        curlbl.setText("Current Value");
        curlbl.setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT, true, false));

        Label newlbl = new Label(entryComposite, SWT.NONE);
        newlbl.setText("New Value");
        newlbl.setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT, true, false));

        Label presslbl = new Label(entryComposite, SWT.RIGHT);
        presslbl.setText("Pressure (mb)");
        presslbl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        curPressText = new Text(entryComposite,
                SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
        curPressText.setText(MISSING_VALUE);
        curPressText.setBackground(colorLightGrey);

        GC gc = new GC(curPressText);
        int charWidth = gc.textExtent("9").x;
        gc.dispose();
        Rectangle textFieldBounds = curPressText.computeTrim(0, 0,
                charWidth * 12, 1);

        GridData layoutData = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        layoutData.widthHint = textFieldBounds.width;
        curPressText.setLayoutData(layoutData);

        newPressText = new Text(entryComposite, SWT.SINGLE | SWT.BORDER);
        newPressText.setText(StringUtils.EMPTY);
        newPressText.addVerifyListener(postiveFloatVerifyListener);
        layoutData = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        layoutData.widthHint = textFieldBounds.width;
        newPressText.setLayoutData(layoutData);

        Label templbl = new Label(entryComposite, SWT.RIGHT);
        templbl.setText("Temperature (C)");
        templbl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        curTempText = new Text(entryComposite,
                SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
        curTempText.setText(MISSING_VALUE);
        curTempText.setBackground(colorLightGrey);
        layoutData = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        layoutData.widthHint = textFieldBounds.width;
        curTempText.setLayoutData(layoutData);

        newTempText = new Text(entryComposite, SWT.SINGLE | SWT.BORDER);
        newTempText.setText(StringUtils.EMPTY);
        newTempText.addVerifyListener(floatVerifyListener);
        layoutData = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        layoutData.widthHint = textFieldBounds.width;
        newTempText.setLayoutData(layoutData);

        Label dewlbl = new Label(entryComposite, SWT.RIGHT);
        dewlbl.setText("DewPoint (C)");
        dewlbl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        curDewText = new Text(entryComposite,
                SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
        curDewText.setText(MISSING_VALUE);
        curDewText.setBackground(colorLightGrey);
        layoutData = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        layoutData.widthHint = textFieldBounds.width;
        curDewText.setLayoutData(layoutData);

        newDewText = new Text(entryComposite, SWT.SINGLE | SWT.BORDER);
        newDewText.setText(StringUtils.EMPTY);
        newDewText.addVerifyListener(floatVerifyListener);
        layoutData = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        layoutData.widthHint = textFieldBounds.width;
        newDewText.setLayoutData(layoutData);

        Label wdirlbl = new Label(entryComposite, SWT.RIGHT);
        wdirlbl.setText("Wind Direction");
        wdirlbl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        curWDirText = new Text(entryComposite,
                SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
        curWDirText.setText(MISSING_VALUE);
        curWDirText.setBackground(colorLightGrey);
        layoutData = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        layoutData.widthHint = textFieldBounds.width;
        curWDirText.setLayoutData(layoutData);

        newWDirText = new Text(entryComposite, SWT.SINGLE | SWT.BORDER);
        newWDirText.setText(StringUtils.EMPTY);
        newWDirText.addVerifyListener(postiveFloatVerifyListener);
        layoutData = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        layoutData.widthHint = textFieldBounds.width;
        newWDirText.setLayoutData(layoutData);

        Label wsplbl = new Label(entryComposite, SWT.RIGHT);
        wsplbl.setText("Wind Speed (Knot)");
        wdirlbl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        curWSpText = new Text(entryComposite,
                SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
        curWSpText.setText(MISSING_VALUE);
        curWSpText.setBackground(colorLightGrey);
        layoutData = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        layoutData.widthHint = textFieldBounds.width;
        curWSpText.setLayoutData(layoutData);

        newWSpText = new Text(entryComposite, SWT.SINGLE | SWT.BORDER);
        newWSpText.setText(StringUtils.EMPTY);
        newWSpText.addVerifyListener(postiveFloatVerifyListener);
        layoutData = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        layoutData.widthHint = textFieldBounds.width;
        newWSpText.setLayoutData(layoutData);
        return composite;
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText("Sounding Data Editor");

        newShell.addDisposeListener((e) -> {
            colorBlue.dispose();
            colorLightGrey.dispose();
        });
    }

    private void verifyInput() {
        float pressure = 0;
        try {
            String textVal = (StringUtils.isNotBlank(newPressText.getText()))
                    ? newPressText.getText() : curPressText.getText();
            pressure = Float.valueOf(textVal);
        } catch (NumberFormatException e) {
            MessageDialog.openWarning(getShell(), "Invalid Data",
                    "Invalid value entered for pressure.");
            return;
        }

        float temperature = 0;
        try {
            String textVal = (StringUtils.isNotBlank(newTempText.getText()))
                    ? newTempText.getText() : curTempText.getText();
            temperature = Float.valueOf(textVal);
        } catch (NumberFormatException e) {
            MessageDialog.openWarning(getShell(), "Invalid Data",
                    "Invalid value entered for temperature.");
            return;
        }

        float dewPoint = 0;
        try {
            String textVal = (StringUtils.isNotBlank(newDewText.getText()))
                    ? newDewText.getText() : curDewText.getText();
            dewPoint = Float.valueOf(textVal);
        } catch (NumberFormatException e) {
            MessageDialog.openWarning(getShell(), "Invalid Data",
                    "Invalid value entered for dew point.");
            return;
        }

        if (dewPoint > temperature) {
            MessageDialog.openWarning(getShell(), "Invalid Data",
                    "Dew point is higher than temperature.");
            return;
        }

        float windDirection = 0;
        try {
            String textVal = (StringUtils.isNotBlank(newWDirText.getText()))
                    ? newWDirText.getText() : curWDirText.getText();
            windDirection = Float.valueOf(textVal);
        } catch (NumberFormatException e) {
            MessageDialog.openWarning(getShell(), "Invalid Data",
                    "Invalid value entered for wind direction.");
            return;
        }
        if (windDirection > 360f) {
            MessageDialog.openWarning(getShell(), "Invalid Data",
                    "Wind direction is greater than 360 degrees.");
            return;
        }

        float windSpeed = 0;
        try {
            String textVal = (StringUtils.isNotBlank(newWSpText.getText()))
                    ? newWSpText.getText() : curWSpText.getText();
            windSpeed = Float.valueOf(textVal);
        } catch (NumberFormatException e) {
            MessageDialog.openWarning(getShell(), "Invalid Data",
                    "Invalid value entered for wind speed.");
            return;
        }

        NsharpEditor editor = NsharpEditor.getActiveNsharpEditor();
        if (editor != null) {
            NsharpResourceHandler rscHandler = editor.getRscHandler();
            switch (currentEditType) {
            case NEW_LEVEL:
                rscHandler.addNewLayer(temperature, dewPoint, windSpeed,
                        windDirection, pressure);
                break;
            case SELECTED_LEVEL:
                rscHandler.updateLayer(pressureList.getSelectionIndex(),
                        temperature, dewPoint, windSpeed, windDirection,
                        pressure);
                break;
            default:
                return;
            }
            editor.refresh();
        }
    }

    private boolean isNoEmptyInput() {
        return StringUtils.isNoneBlank(newPressText.getText(),
                newTempText.getText(), newDewText.getText(),
                newWDirText.getText(), newWSpText.getText());
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, APPLY_ID, "Apply", true);
        createButton(parent, IDialogConstants.CLOSE_ID,
                IDialogConstants.CLOSE_LABEL, false);
    }

    @Override
    protected void buttonPressed(int buttonId) {
        switch (buttonId) {
        case APPLY_ID:
            if ((EditType.NEW_LEVEL == currentEditType) && !isNoEmptyInput()) {
                MessageDialog.openWarning(getShell(), StringUtils.EMPTY,
                        "Missing input data! Should fill up all 5 entries!");
                return;
            }
            verifyInput();
            break;
        case IDialogConstants.CLOSE_ID:
            close();
            break;
        }
    }
}
