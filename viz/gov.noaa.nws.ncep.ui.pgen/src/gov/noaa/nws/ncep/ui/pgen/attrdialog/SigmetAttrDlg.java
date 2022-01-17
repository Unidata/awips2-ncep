/*
 * gov.noaa.nws.ncep.ui.pgen.attrDialog.SigmetAttrDlg
 *
 * September 2009
 *
 * This code has been developed by the NCEP/SIB for use in the AWIPS2 system.
 */

package gov.noaa.nws.ncep.ui.pgen.attrdialog;

import java.awt.Color;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.regex.Pattern;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.time.util.TimeUtil;
import com.raytheon.uf.common.util.StringUtil;
import com.raytheon.uf.viz.core.map.IMapDescriptor;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Polygon;

import gov.noaa.nws.ncep.edex.common.stationTables.Station;
import gov.noaa.nws.ncep.ui.pgen.Activator;
import gov.noaa.nws.ncep.ui.pgen.PgenConstant;
import gov.noaa.nws.ncep.ui.pgen.PgenSession;
import gov.noaa.nws.ncep.ui.pgen.PgenStaticDataProvider;
import gov.noaa.nws.ncep.ui.pgen.PgenUtil;
import gov.noaa.nws.ncep.ui.pgen.attrdialog.vaadialog.SpinnerSlider;
import gov.noaa.nws.ncep.ui.pgen.display.FillPatternList.FillPattern;
import gov.noaa.nws.ncep.ui.pgen.display.IAttribute;
import gov.noaa.nws.ncep.ui.pgen.display.ILine;
import gov.noaa.nws.ncep.ui.pgen.elements.AbstractDrawableComponent;
import gov.noaa.nws.ncep.ui.pgen.elements.Layer;
import gov.noaa.nws.ncep.ui.pgen.elements.Product;
import gov.noaa.nws.ncep.ui.pgen.elements.ProductTime;
import gov.noaa.nws.ncep.ui.pgen.sigmet.CarSamBackupWmoHeader;
import gov.noaa.nws.ncep.ui.pgen.sigmet.ISigmet;
import gov.noaa.nws.ncep.ui.pgen.sigmet.Sigmet;
import gov.noaa.nws.ncep.ui.pgen.sigmet.SigmetInfo;
import gov.noaa.nws.ncep.ui.pgen.store.PgenStorageException;
import gov.noaa.nws.ncep.ui.pgen.store.StorageUtils;
import gov.noaa.nws.ncep.viz.common.SnapUtil;
import gov.noaa.nws.ncep.viz.common.ui.color.ColorButtonSelector;

/**
 * Singleton attribute dialog for sigmet.
 *
 * <pre>
 * SOFTWARE HISTORY
 *
 * Date          Ticket#    Engineer     Description
 * ------------- ---------- ------------ ---------------------------------------
 * 09/09         160        Gang Zhang   Initial Creation.
 * 03/10         231        Archana      Altered the dialog for sigmet to
 *                                       display only a button showing the
 *                                       selected color instead of displaying
 *                                       the complete color matrix.
 * 03/10         223        M.Laryukhin  Refactored getVOR method to be used
 *                                       with gfa.
 * 04/11                    B. Yin       Re-factor IAttribute
 * 07/11         450        G. Hull      NcPathManager
 * 12/11         526        B. Yin       Close dialog after text is saved.
 * 02/12         597        S. Gurung    Moved snap functionalities to SnapUtil
 *                                       from SigmetInfo.
 * 03/12         #612,#613  S. Gurung    Accept phenom Lat/Lon and convert them
 *                                       to prepended format. Change KZOA to
 *                                       KZAK.
 * 03/12         611        S. Gurung    Fixed ability to change SIGMET type
 *                                       (from Area to Line/Isolated and back
 *                                       and forth)
 * 03/12         676        Q. Zhou      Added Issue Office dropdown list.
 * 08/12         612        S. Gurung    Fixed issue related to conversion of
 *                                       phenom Lat/Lon to prepended format
 * 03/13         928        B. Yin       Made the button bar smaller.
 * 04/13         977        S. Gilbert   PGEN Database support
 * 09/13         TTR656     J. Wu        Display for INTL_SIGMET converted from
 *                                       VGF.
 * 09/14         TTR974     J. Wu        update "editableAttrFromLine" in
 *                                       "setSigmet()".
 * 10/14         TTR433     J. Wu        Set input verification/output format
 *                                       for Phenom Lat/Lon.
 * 10/14         TTR722     J. Wu        Display TC center/Movement/FL level for
 *                                       ISOLATED TC.
 * Mar 20, 2019  7572       dgilling     Code cleanup.
 * Jan 07, 2020  71971      smanoj       Code fix to Store and Retrieve
 *                                       INTL_SIGMET.
 * Jan 31, 2020  73863      smanoj       Added check to validate lat/lon values.
 * Mar 13, 2020  76151      tjensen      Code cleanup and added null checks
 * Apr 21, 2020  76155      ksunil       ID values for INTL is dependent on WMO value selected
 * Apr 21, 2020  77994      ksunil       Tropical Cyclone and Volcanic Ash should default to 6 hours end time.
 *               78000                   New fcst widget items for TC
 * May 12, 2020  77473      ksunil       INtl Sigmet input validation framework
 * Apr 28, 2020  77667      smanoj       Flight Information Region (FIR) update.
 * May 04, 2020  77670      smanoj       Format changes for Thunderstorm.
 * May 06, 2020  77691      smanoj       Format changes for Volcanic Ash.
 * May 14, 2020  77691      smanoj       Additional format changes for VA ERUPTION.
 * May 18, 2020  77690      smanoj       Tropical Cyclone format changes.
 * May 22, 2020  78470      smanoj       INTL Sigmet Save ID Bug Fix.
 * May 22, 2020  78000      ksunil       New Tropical Cyclone UI components for Fcst
 * Jun 4,  2020  79256      ksunil       Series ID is now a function of Issuing Office
 * Jun 03, 2020  78215      smanoj       INTL Sigmet Cancel functionality changes.
 * Jun 11, 2020  79243      smanoj       Added Caribbean and South American FIRs.
 * Jul 01, 2020  79980      smanoj       Tropical Cyclone FCST Center enhancement.
 * Aug 19, 2020  81314      smanoj       INTL Sigmet Volcanic Ash GUI enhancement.
 * Jan 28, 2021  86821      achalla      Refactored width attribute in International SIGMET Edit GUI,
 *                                       SIGMET Save output and International SIGMET message to show Integer.
 * Feb 05, 2021  87538      smanoj       Added FCST Lat/Lon for Tropical Cyclone, also fixed some issues.
 * Feb 18, 2021  86828      achalla      Created updateFirBtn() method to check the correct FIR Region buttons
 * Feb 24, 2021  86827      srussell     Updated createDialogAreaGeneral():
 *                                       Removed the setWidthStr() call in the
 *                                       set and reset area, it was preventing
 *                                       user entered values from being used.
 *                                       Updated getFirString() removed the
 *                                       unneeded call to JTS covers() method
 * Feb 26, 2021  87541      achalla      Updated getFirString() to identify and select AWC Backup FIR Regions
 *                                       and drop AWC AOR FIR Regions if  the sigmet polygon crosses over
 *                                       or partially extends into those regions.
 * Mar 09, 2021  88219      achalla      Added a duplicate set of Level Info attributes within
 *                                       the Forecast Section of the Edit Attributes area in the
 *                                       Int'l SIGMET Edit GUI for the VOLCANIC_ASH phenomenon and
 *                                       Removed the existing Altitudes attributes
 * Mar 09, 2021  87540      srussell     Added createLineWidthControls() to
 *                                       install a slider & spinner to control
 *                                       the line width of polygons.
 *                                       Updated okPressed() to support the new
 *                                       line width controls when the "Apply"
 *                                       button is pressed.
 *                                       Updated setAttrForDlg() to support the
 *                                       new line width GUI controls.
 * Mar 15, 2021  88217     smanoj        SIGMET CANCEL SAVE Enhancement.
 * Mar 25, 2021  86828     achalla       Updated getFirs() to check FIR Region buttons instantly
 *                                       when the area polygon is moved into new region
 * Apr 09, 2021  90325     smanoj        CARSAM Backup WMO headers update.
 * Apr 28, 2021  90556     smanoj        Drop unneeded trailing attributes from
 *                                       final Cancellation SIGMET Save.
 * May 10, 2021  91845     smanoj        Save CARSAM Backupmode flag to the SIGMET xml.
 * May 28, 2021  91845     smanoj        Drawing SIGMET spanning multiple FIRs in Backupmode.
 * Jun 04, 2021  91845     smanoj        Fixing some issues with Backupmode and CANCEL.
 * Jun 09, 2021  90732     mroos         Correcting Level Info locations, drop-down box, and observed info
 * Jun 29, 2021  93036     smanoj        Changes for QC alerts for Int'l SIGMETS.
 * Jun 30, 2021  93038     mroos         Change default Trend attribute and allow default attributes to change
 * Jul 26, 2021  93964      omoncayo     Eliminate gosh volcanos menu for Cyclone
 *                                       populate Observed Phenom Lat and Lon
 * Aug 20, 2021  93036      omoncayo     PGEN INTL SIGMET:QC Check Attributes
 *
 * </pre>
 *
 * @author gzhang
 */

public class SigmetAttrDlg extends AttrDlg implements ISigmet {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(SigmetAttrDlg.class);

    private static final long SIX_HR_MS = 6 * TimeUtil.MILLIS_PER_HOUR;

    private static final int MAX_FLIGHT_LEVEL = 600;

    private static final String NONE = "-none-";

    private static final String STNRY = "STNRY";

    private static final String EDITABLE_ATTR_PHENOM_NAME = "editableAttrPhenomName";

    private static final String EDITABLE_ATTR_STATUS = "editableAttrStatus";

    private static final String EDITABLE_ATTR_FROM_LINE = "editableAttrFromLine";

    private static final String LINE_TYPE = "lineType";

    private static final String EDITABLE_ATTR_FREE_TEXT = "editableAttrFreeText";

    private static final String EDITABLE_ATTR_LEVEL_TEXT2 = "editableAttrLevelText2";

    private static final String EDITABLE_ATTR_LEVEL_INFO2 = "editableAttrLevelInfo2";

    private static final String EDITABLE_ATTR_LEVEL_TEXT1 = "editableAttrLevelText1";

    private static final String EDITABLE_ATTR_LEVEL_INFO1 = "editableAttrLevelInfo1";

    private static final String EDITABLE_ATTR_LEVEL = "editableAttrLevel";

    private static final String EDITABLE_ATTR_ALT_LEVEL_TEXT2 = "editableAttrAltLevelText2";

    private static final String EDITABLE_ATTR_ALT_LEVEL_INFO2 = "editableAttrAltLevelInfo2";

    private static final String EDITABLE_ATTR_ALT_LEVEL_TEXT1 = "editableAttrAltLevelText1";

    private static final String EDITABLE_ATTR_ALT_LEVEL_INFO1 = "editableAttrAltLevelInfo1";

    private static final String EDITABLE_ATTR_ALT_LEVEL = "editableAttrAltLevel";

    private static final String EDITABLE_ATTR_PHENOM2 = "editableAttrPhenom2";

    private static final String EDITABLE_ATTR_TREND = "editableAttrTrend";

    private static final String EDITABLE_ATTR_PHENOM_DIRECTION = "editableAttrPhenomDirection";

    private static final String EDITABLE_ATTR_PHENOM_SPEED = "editableAttrPhenomSpeed";

    private static final String EDITABLE_ATTR_MOVEMENT = "editableAttrMovement";

    private static final String EDITABLE_ATTR_FCST_AVAIL = "editableAttrFcstAvail";

    private static final String EDITABLE_ATTR_FCST_TIME = "editableAttrFcstTime";

    private static final String EDITABLE_ATTR_FCST_CENTER = "editableAttrFcstCntr";

    private static final String EDITABLE_ATTR_FCST_VADESC = "editableAttrFcstVADesc";

    private static final String EDITABLE_ATTR_RAL_SELECTION = "editableAttrRALSelection";

    private static final String EDITABLE_ATTR_ALTITUDE_SELECTION = "editableAttrAltitudeSelection";

    private static final String EDITABLE_ATTR_POLYGON_LINE_WIDTH = "editableAttrPolygonLineWidth";

    private static final String STATUS_NEW = "0";

    private static final String STATUS_AMEND = "1";

    private static final String STATUS_CANCEL = "2";

    private static final int APPLY_ID = IDialogConstants.CLIENT_ID + 1;

    private static final int SAVE_ID = IDialogConstants.CLIENT_ID + 2;

    private static SigmetAttrDlg INSTANCE = null;

    public static final String AREA = "Area";

    public static final String LINE = "Line";

    public static final String RADIUS = "Radius";

    public static final String ISOLATED = "Isolated";

    // default
    private String lineType = AREA;

    private String origLineType = lineType;

    private static final String WIDTH = "10.00";

    // default, nautical miles
    private String widthStr = WIDTH;

    private static final String[] LINE_SIDES = new String[] { "ESOL", "NOF",
            "SOF", "EOF", "WOF" };

    // default
    private String sideOfLine = LINE_SIDES[0];

    private gov.noaa.nws.ncep.ui.pgen.elements.DrawableElement sigmet = null;

    protected Composite top = null;

    protected ColorButtonSelector cs = null;

    private Combo comboISU, comboMWO, comboID = null;

    private boolean withExpandedArea = false;

    private boolean copiedToSigmet = false;

    private boolean comboPhenomCalled = false;

    private boolean tropCycFlag = false;

    private Text txtInfo, txtTo, txtFreeText;

    private Text txtPheLat, txtPheLon;

    private Control detailsArea;

    private Point cachedWindowSize;

    // _fmtflag in C without text
    private String latLonFormatFlagAndText;

    // MWO
    private String editableAttrArea;

    // IssueOffice
    private String editableAttrIssueOffice;

    // 0=new,1=amend,2=cancel...
    private String editableAttrStatus;

    // alfa,brave...
    private String editableAttrId;

    // 1,2,...,300
    private String editableAttrSeqNum;

    // start valid
    private String editableAttrStartTime;

    // end valid
    private String editableAttrEndTime;

    private String editableAttrRemarks;

    private String editableAttrPhenom;

    private String editableAttrPhenom2;

    private String editableAttrPhenomName;

    private String editableAttrPhenomLat;

    private String editableAttrPhenomLon;

    private String editableAttrPhenomPressure;

    private String editableAttrPhenomMaxWind;

    private String editableAttrFreeText;

    private String editableAttrFcstAvail;

    private String editableAttrFcstTime;

    private String editableAttrFcstCntr;

    private String editableAttrFcstPhenomLat;

    private String editableAttrFcstPhenomLon;

    private String editableAttrFcstVADesc;

    private String editableAttrTrend;

    private String editableAttrMovement;

    private String editableAttrPhenomSpeed;

    private String editableAttrPhenomDirection;

    private String editableAttrLevel;

    private String editableAttrLevelInfo1;

    private String editableAttrLevelInfo2;

    private String editableAttrLevelText1;

    private String editableAttrLevelText2;

    private String editableAttrAltLevel;

    private String editableAttrAltLevelInfo1;

    private String editableAttrAltLevelInfo2;

    private String editableAttrAltLevelText1;

    private String editableAttrAltLevelText2;

    private String editableAttrAltLevelText;

    private String editableAttrFromLine;

    private String editableAttrFir;

    private String editableAttrCarSamBackupMode;

    private final Map<String, Control> attrControlMap = new HashMap<>();

    private final Map<String, Button[]> attrButtonMap = new HashMap<>();

    private final Map<String, Button[]> firButtonMap = new HashMap<>();

    /**
     * Colors to indicate if Phenom lat/lon input is in correct format.
     */
    private static final Color wrongFormatColor = Color.red;

    private static final Color rightFormatColor = Color.green;

    private Combo comboPhenom;

    private boolean firCalledForSecondLine = false;

    private String editableFirID;

    private String editableAttrRALSelection;

    private String editableAttrAltitudeSelection;

    private SigmetCancelDlg sigmetCnlDlg = null;

    // Fir Regions selection buttons
    private Button btnPacific;

    private Button btnAtlantic;

    private Button btnMexico;

    private Button btnOther;

    private Button btnCarSamBackUp;

    private boolean isCarSamBackup = false;

    // Line Width Controls
    private Button chkBoxLineWidth;

    private Label lineWidthLabel;

    private SpinnerSlider lineWidthSpinSlide;

    /**
     * Constructor.
     */
    protected SigmetAttrDlg(Shell parShell) {
        super(parShell);
    }

    public static synchronized SigmetAttrDlg getInstance(Shell parShell) {
        if (INSTANCE == null) {
            INSTANCE = new SigmetAttrDlg(parShell);
        }
        return INSTANCE;
    }

    private void resetText(String coorsLatLon, Text txtInfo) {

        StringBuilder sb = new StringBuilder();
        String[] strings = coorsLatLon.split(SigmetInfo.LINE_SEPERATER);
        for (int i = 0; i < strings.length; i++) {
            if (i != 0 && (i % 6 == 0)) {
                sb.append("\n");
            }
            if (!((i == strings.length - 1) && ("New".equals(strings[i])
                    || "Old".equals(strings[i]) || "VOR".equals(strings[i])))) {
                sb.append(strings[i] + "  ");
            }
        }
        txtInfo.setText(sb.toString());

    }

    @Override
    public void okPressed() {
        List<AbstractDrawableComponent> adcList = null;
        List<AbstractDrawableComponent> newList = new ArrayList<>();

        // get the list of selected tracks
        if (drawingLayer != null) {
            adcList = drawingLayer.getAllSelected();

            if (adcList != null && !adcList.isEmpty()) {

                // loop through the list and update attributes
                for (AbstractDrawableComponent adc : adcList) {

                    Sigmet el = (Sigmet) adc.getPrimaryDE();

                    if (el != null) {
                        // Create a copy of the currently selected element
                        Sigmet newEl = (Sigmet) el.copy();

                        // Update the new Element with these current
                        // attributes
                        copyEditableAttrToSigmet(newEl);// 20100115

                        // Change type and update From line
                        newEl = convertType(newEl);
                        newEl.update(this);

                        newList.add(newEl);

                    }
                }

                List<AbstractDrawableComponent> oldList = new ArrayList<>(
                        adcList);
                drawingLayer.replaceElements(oldList, newList);
            }

            // set the new elements as selected.
            drawingLayer.removeSelected();
            for (AbstractDrawableComponent adc : newList) {
                drawingLayer.addSelected(adc);
            }
        }

        if (mapEditor != null) {
            mapEditor.refresh();
        }

    }

    /**
     * Return color from the color picker of the dialog
     */
    @Override
    public Color[] getColors() {
        // IAttribute requires to return an array of colors
        // Only the first color is used at this time.
        Color[] colors = new Color[2];
        colors[0] = new java.awt.Color(cs.getColorValue().red,
                cs.getColorValue().green, cs.getColorValue().blue);
        colors[1] = Color.green;

        return colors;
    }

    private void setColor(Color clr) {
        cs.setColorValue(new RGB(clr.getRed(), clr.getGreen(), clr.getBlue()));
    }

    @Override
    public String getLineType() {
        return this.lineType;
    }

    public void setLineType(String lType) {
        setOrigLineType(getLineType());
        this.lineType = lType;
    }

    public String getOrigLineType() {
        return this.origLineType;
    }

    public void setOrigLineType(String lType) {
        this.origLineType = lType;
    }

    public String getSideOfLine() {
        return this.sideOfLine;
    }

    public void setSideOfLine(String lineSideString) {
        this.sideOfLine = lineSideString;
    }

    public String getEditableAttrAltitudeSelection() {
        return editableAttrAltitudeSelection;
    }

    public void setEditableAttrAltitudeSelection(
            String editableAttrAltitudeSelection) {
        this.editableAttrAltitudeSelection = editableAttrAltitudeSelection;
    }

    @Override
    public double getWidth() {
        return Double.parseDouble(this.widthStr);
    }

    public String getWidthStr() {
        return this.widthStr;
    }

    public void setWidthStr(String widthString) {
        this.widthStr = widthString;
    }

    @Override
    public void setAttrForDlg(IAttribute attr) {
        Color clr = attr.getColors()[0];
        if (clr != null) {
            this.setColor(clr);
        }

        if (attr instanceof ILine) {
            float lw = attr.getLineWidth();
            if (lw > 0) {
                this.setLineWidth(lw);
            }

        }
    }

    @Override
    public int getSmoothFactor() {
        return 0;
    }

    @Override
    public FillPattern getFillPattern() {
        return FillPattern.FILL_PATTERN_0;
    }

    public boolean isClosed() {
        return false;
    }

    @Override
    public Boolean isFilled() {
        return false;
    }

    @Override
    public float getLineWidth() {
        return this.lineWidthSpinSlide.getSelection();
    }

    public void setLineWidth(float lineWidth) {
        int lw = 0;
        lw = (int) lineWidth;
        this.lineWidthSpinSlide.setSelection(lw);
    }

    @Override
    public double getSizeScale() {
        return 2;
    }

    @Override
    public void createButtonsForButtonBar(Composite parent) {
        if ("Pgen Select".equals(mouseHandlerName) || withExpandedArea) {
            Button saveBtn = createButton(parent, SAVE_ID, "Save", false);
            saveBtn.setEnabled(true);

            Button applyBtn = createButton(parent, APPLY_ID, "Apply", false);
            applyBtn.setEnabled(true);
        } else {
            createButton(parent, IDialogConstants.OK_ID,
                    IDialogConstants.OK_LABEL, true);
        }

        createButton(parent, IDialogConstants.CANCEL_ID,
                IDialogConstants.CANCEL_LABEL, false);

        // reset ???
        setMouseHandlerName(null);
        withExpandedArea = false;
    }

    @Override
    protected void buttonPressed(int buttonId) {
        super.buttonPressed(buttonId);

        switch (buttonId) {
        case SAVE_ID:
            String inValid = this.validateSigmetEntries();
            if (!StringUtils.isEmpty(inValid)) {
                (new SigmetAttrValidateDlg(getShell(), inValid)).open();
                break;
            }

            setEditableAttrId(comboID.getText());
            okPressed();

            if (STATUS_CANCEL
                    .equals(SigmetAttrDlg.this.getEditableAttrStatus())) {
                if ((sigmetCnlDlg == null)) {
                    Sigmet sigmet = (Sigmet) getSigmet();
                    try {
                        sigmetCnlDlg = new SigmetCancelDlg(
                                getInstance(getShell()), getShell(), sigmet);
                    } catch (Exception ee) {
                        statusHandler
                                .warn("Unable to create SIGMET Cancellation Dialog: "
                                        + ee.getLocalizedMessage(), ee);
                    }
                    if (sigmetCnlDlg != null) {
                        sigmetCnlDlg.open();
                    }
                }
            }

            SigmetAttrDlgSaveMsgDlg md = null;
            try {
                md = new SigmetAttrDlgSaveMsgDlg(getShell());
            } catch (Exception ee) {
                statusHandler.debug(ee.getLocalizedMessage(), ee);
            }

            if (md != null) {
                md.open();
            }
            break;

        case APPLY_ID:
            setEditableAttrId(comboID.getText());
            Sigmet sigmet = (Sigmet) this.getSigmet();
            sigmet.setEditableAttrId(comboID.getText());
            okPressed();
            break;

        default:
            break;
        }
    }

    private String validateSigmetEntries() {
        // Validation for NPE
        String phenomType = SigmetAttrDlg.this.getEditableAttrPhenom() == null
                ? "" : SigmetAttrDlg.this.getEditableAttrPhenom();
        String levelInfo1 = editableAttrLevelInfo1 == null ? ""
                : editableAttrLevelInfo1;
        String level = getEditableAttrLevel() == null ? ""
                : getEditableAttrLevel();

        StringBuffer errors = new StringBuffer();

        // SIGMET should intersect at least One FIR.
        errors.append(validateFIRSigmetEntries(
                SigmetAttrDlg.this.getEditableAttrFir()));

        // Validations by Phenom Type
        switch (phenomType) {
        case PgenConstant.TYPE_FRQ_TS:
        case PgenConstant.TYPE_OBSC_TS:
        case PgenConstant.TYPE_EMBD_TS:
        case PgenConstant.TYPE_SQL_TS:
            if (!getEditableAttrLevel().equals(PgenConstant.LEVEL_TOPS)) {
                errors.append(
                        "Level Info first widget combo box should be set to “TOPS”.\n\n");
            }
            // validate level information (Max value is 600)
            errors.append(validateLevelInfoSigmetEntries(
                    SigmetAttrDlg.this.getEditableAttrLevelText1(), 0, 600));
            break;
        case PgenConstant.TYPE_SEV_TURB:
        case PgenConstant.TYPE_SEV_ICE:
            if (!getEditableAttrLevel().equals(PgenConstant.LEVEL_FCST)) {
                errors.append(
                        "Level Info first widget combo box should be set to “FCST”.\n\n");
            }
            switch (levelInfo1) {
            case PgenConstant.LEVEL_INFO_ABV:
            case PgenConstant.LEVEL_INFO_BLW:
                // validate level information (Max value is 600)
                errors.append(validateLevelInfoSigmetEntries(
                        SigmetAttrDlg.this.getEditableAttrLevelText1(), 0,
                        600));
                break;
            case PgenConstant.LEVEL_INFO_BTN:
                // validate level information (Min 250, Max 350
                errors.append(validateLevelInfoSigmetEntries(
                        SigmetAttrDlg.this.getEditableAttrLevelText1(), 250,
                        350));
                if (getEditableAttrLevelInfo2()
                        .equals(PgenConstant.LEVEL_INFO2_AND)) {
                    // validate level information (Max value is 600)
                    errors.append(validateLevelInfoSigmetEntries(
                            SigmetAttrDlg.this.getEditableAttrLevelText2(),
                            Integer.parseInt(SigmetAttrDlg.this
                                    .getEditableAttrLevelText1()),
                            600));
                } else {
                    errors.append(
                            "Ensure the Level Info 4th widget is set to AND\n\n");
                }
                break;
            default:
                errors.append(
                        "Level Info second widget combo box should be set to “ABV/BLW/BTN”.\n\n");
            }
            break;
        case PgenConstant.TYPE_RDOACT_CLD:
            if (!getEditableAttrLevel().equals(PgenConstant.LEVEL_FCST)) {
                errors.append(
                        "Level Info first widget combo box should be set to “FCST”.\n\n");
            }
            switch (levelInfo1) {
            case PgenConstant.LEVEL_INFO_ABV:
            case PgenConstant.LEVEL_INFO_BLW:
                // validate level information (Max value is 600)
                errors.append(validateLevelInfoSigmetEntries(
                        SigmetAttrDlg.this.getEditableAttrLevelText1(), 0,
                        600));
                break;
            case PgenConstant.LEVEL_INFO_BTN:
                // validate level information (Max value is 600)
                errors.append(validateLevelInfoSigmetEntries(
                        SigmetAttrDlg.this.getEditableAttrLevelText1(), 0,
                        600));
                if (getEditableAttrLevelInfo2()
                        .equals(PgenConstant.LEVEL_INFO2_AND)) {
                    // validate level information (Max value is 600)
                    errors.append(validateLevelInfoSigmetEntries(
                            SigmetAttrDlg.this.getEditableAttrLevelText2(),
                            Integer.parseInt(SigmetAttrDlg.this
                                    .getEditableAttrLevelText1()),
                            600));
                } else {
                    errors.append(
                            "Ensure the Level Info 4th widget is set to AND\n\n");
                }
                break;
            default:
                errors.append(
                        "Level Info second widget combo box should be set to “ABV/BLW/BTN”.\n\n");

            }
            break;
        case PgenConstant.TYPE_TROPICAL_CYCLONE:

            if (StringUtils
                    .isEmpty(SigmetAttrDlg.this.getEditableAttrPhenomName())) {
                errors.append(
                        "Phenom Name can't be null or empty. Please Enter Phenom Name.\n\n");
            }
            if (StringUtils
                    .isEmpty(SigmetAttrDlg.this.getEditableAttrFcstCntr())
                    && ("true".equals(getEditableAttrFcstAvail()))) {
                errors.append("Fcst Center can't be null or empty.\n\n");
            }
            if (!getEditableAttrLevel().equals(PgenConstant.LEVEL_TOPS)) {
                errors.append(
                        "Level Info first widget combo box should be set to “TOPS”.\n\n");
            }
            // validate level information (Max value is 600)
            errors.append(validateLevelInfoSigmetEntries(
                    SigmetAttrDlg.this.getEditableAttrLevelText1(), 0, 600));

            errors.append(validateFcstAvailSigmetEntries(phenomType));

            break;
        case PgenConstant.TYPE_VOLCANIC_ASH:

            if (StringUtils
                    .isEmpty(SigmetAttrDlg.this.getEditableAttrPhenomName())) {
                errors.append(
                        "Phenom Name can't be null or empty. Please Select/Enter Phenom Name.\n\n");
            }
            // Validate Radical/Area/Line Description Lat/Lon
            if (!StringUtils
                    .isEmpty(SigmetAttrDlg.this.getEditableAttrFcstVADesc())
                    && (SigmetAttrDlg.this.getEditableAttrFcstVADesc().trim()
                            .length() > 0)) {
                String[] locPair = SigmetAttrDlg.this
                        .getEditableAttrFcstVADesc().split("-");
                for (int i = 0; i < locPair.length; i++) {
                    String locTemp = locPair[i];
                    locTemp = locTemp.trim();
                    String[] latlonPair = locTemp.split(" ");
                    if (latlonPair.length > 1) {
                        String lat = latlonPair[0];
                        String lon = latlonPair[1];

                        if (lat.length() < 5) {
                            errors.append(
                                    "Entered an Invalid Latitude value for the Radial/Area/Line Description :  "
                                            + lat
                                            + ". Example of Valid Latitude: N2330.\n\n");
                        }
                        if (lon.length() < 6) {
                            errors.append(
                                    "Entered an Invalid Longitude value for the Radial/Area/Line Description :  "
                                            + lon
                                            + ". Example of Valid Longitude: W07500.\n\n");
                        }
                    } else {
                        errors.append(
                                "Both Latitude and Longitude coordinates need to be entered for the "
                                        + "Radial/Area/Line Description. Example N2330 W07500. \n\n");
                    }

                }
            } // End Switch - Validation of Radial/Area/Line Description is on
              // validateCommonSigmetEntries

            switch (level) {
            case PgenConstant.LEVEL_FCST:
                // validate level information (Max value is 600)
                errors.append(validateLevelInfoSigmetEntries(
                        SigmetAttrDlg.this.getEditableAttrLevelText1(), 0,
                        600));

                switch (levelInfo1) {
                case PgenConstant.LEVEL_INFO_ABV:
                case PgenConstant.LEVEL_INFO_BLW:
                case PgenConstant.LEVEL_INFO_BTN:
                    if (getEditableAttrLevelInfo2()
                            .equals(PgenConstant.LEVEL_INFO2_AND)) {
                        // validate level information (Max value is 600)
                        errors.append(validateLevelInfoSigmetEntries(
                                SigmetAttrDlg.this.getEditableAttrLevelText2(),
                                0, 600));
                    } else {
                        errors.append(
                                "Ensure the Level Info 4th widget is set to AND\n\n");
                    }
                    break;

                default:
                    errors.append(
                            "Level Info second widget combo box should be set to “ABV/BLW/BTN”.\n\n");

                }

                break;

            case PgenConstant.LEVEL_TOPS:

                switch (levelInfo1) {
                case PgenConstant.LEVEL_INFO_ABV:
                case PgenConstant.LEVEL_INFO_BLW:
                    // validate level information (Max value is 600)
                    errors.append(validateLevelInfoSigmetEntries(
                            SigmetAttrDlg.this.getEditableAttrLevelText1(), 0,
                            600));
                    break;
                default:
                    errors.append(
                            "Level Info second widget combo box should be set to “ABV/BLW”.\n\n");
                }

                break;

            default:
                errors.append(
                        "Level Info first widget combo box should be set to “FCST” or “TOPS” .\n\n");

            }

            errors.append(validateFcstAvailSigmetEntries(phenomType));

            break;
        } // End switch

        return errors.toString();
    }

    private String validateFIRSigmetEntries(String firID) {
        StringBuffer errors = new StringBuffer();
        if (firID == null || firID.isEmpty()) {
            if (SigmetAttrDlg.AREA.equals(lineType)) {
                errors.append(
                        "SIGMET polygon does not intersect any FIRs for the selected MWO.\n\n");
            } else if (SigmetAttrDlg.ISOLATED.equals(lineType)) {
                errors.append(
                        "Isolated SIGMET does not intersect any FIRs for the selected MWO.\n\n");
            } else {
                errors.append(
                        "Line SIGMET does not intersect any FIRs for the selected MWO.\n\n");
            }
        }
        return errors.toString();
    }

    private String validateLevelInfoSigmetEntries(String levelInfo,
            int minFlightLevel, int maxFlightLevel) {
        StringBuffer errors = new StringBuffer();
        if (levelInfo == null || levelInfo.isEmpty()) {
            errors.append(
                    "The Flight Level Info should be a 3-digit value. No Value entered.\n\n");
        } else {
            int levelVal = Integer.parseInt(levelInfo.trim());
            if (!levelInfo.trim().matches("^\\d{3}$")) {
                errors.append(String.format(
                        "The Flight Level Info should be a 3-digit value. Value entered is %s.%n%n",
                        levelInfo));
            } else {
                if (levelVal < minFlightLevel) {
                    errors.append(String.format(
                            "Lower Limit for the Flight Level is %s. Value entered is %s.%n%n",
                            minFlightLevel, levelVal));
                }
                if (levelVal > maxFlightLevel) {
                    errors.append(String.format(
                            "Upper Limit for the Flight Level is %s. Value entered is %s.%n%n",
                            maxFlightLevel, levelVal));
                }
            }
        }
        return errors.toString();
    }

    private String validateFcstAvailSigmetEntries(String phenomType) {

        StringBuffer errors = new StringBuffer();

        // check if the fcstAvail check box is set and make sure time and
        // center are correct and Radial/Area/Line Description is not empty
        if ("true".equals(getEditableAttrFcstAvail())) {

            if (!validateFcstTimeString(
                    SigmetAttrDlg.this.getEditableAttrFcstTime())) {
                errors.append(
                        "Incorrect format for fcst Time. Use HHMM zulu time.\n");
            }

            if (!validateTimeDifference(this.getEditableAttrStartTime(),
                    this.getEditableAttrEndTime(), SIX_HR_MS)) {
                errors.append("Valid from/to can't be more than 6 Hrs.\n");
            }

            switch (phenomType) {
            case PgenConstant.TYPE_TROPICAL_CYCLONE:
                if (StringUtils.isEmpty(
                        SigmetAttrDlg.this.getEditableAttrFcstPhenomLat())) {
                    errors.append(
                            "Forecast Phenom Latitude can't be null or empty. Please Enter valid Latitude.\n\n");
                }
                if (StringUtils.isEmpty(
                        SigmetAttrDlg.this.getEditableAttrFcstPhenomLon())) {
                    errors.append(
                            "Forecast Phenom Longitude can't be null or empty. Please Enter valid Longitude.\n\n");
                }
                break;
            case PgenConstant.TYPE_VOLCANIC_ASH:
                if (StringUtils.isEmpty(
                        SigmetAttrDlg.this.getEditableAttrFcstVADesc())) {
                    errors.append(
                            "Both Latitude and Longitude coordinates need to be entered for the "
                                    + "Radial/Area/Line Description. Example N2330 W07500. \n\n");
                }
                break;
            }

        }

        return errors.toString();
    }

    @Override
    public void enableButtons() {
        getButton(IDialogConstants.CANCEL_ID).setEnabled(true);
    }

    private Control createDetailsArea(Composite parent) {
        Composite detailsComposite = (Composite) super.createDialogArea(parent);
        GridData gdText = new GridData();
        gdText.widthHint = 66;
        GridLayout mainLayout5 = new GridLayout(8, false);
        mainLayout5.marginHeight = 3;
        mainLayout5.marginWidth = 3;
        detailsComposite.setLayout(mainLayout5);

        createDetailsAreaGeneral(detailsComposite);

        if (PgenConstant.TYPE_TROPICAL_CYCLONE.equals(editableAttrPhenom)) {
            createDetailsAreaPhenomDetailsTropCyclone(detailsComposite, gdText);
        }

        if (PgenConstant.TYPE_VOLCANIC_ASH.equals(editableAttrPhenom)) {
            createDetailsAreaPhenomDetailsVolc(detailsComposite, gdText);
            createDetailsAreaLevel(detailsComposite);
        }

        // ------------------------ Phenom Attributes
        createDetailsAreaPhenomAttr(detailsComposite);

        if (PgenConstant.TYPE_TROPICAL_CYCLONE.equals(editableAttrPhenom)) {
            createDetailsAreaSecondPhenom(detailsComposite);
            createDetailsAreaForecastSection(detailsComposite);
        }

        if (PgenConstant.TYPE_VOLCANIC_ASH.equals(editableAttrPhenom)) {
            createDetailsAreaForecastSectionVolcanic(detailsComposite);
        }

        if (!PgenConstant.TYPE_VOLCANIC_ASH.equals(editableAttrPhenom)) {
            // ------------------------------ Level Info:
            createDetailsAreaLevel(detailsComposite);
        }

        // ------------------------------- Remarks
        createDetailsAreaRemarks(detailsComposite);

        // ------------------------------- buttons
        Label lblDummy = new Label(detailsComposite, SWT.CENTER);
        lblDummy.setLayoutData(
                new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));

        createButtonsForButtonBar(detailsComposite);

        if (comboPhenomCalled) {
            withExpandedArea = true;
            comboPhenomCalled = false;
        }

        Label lblDummy1 = new Label(detailsComposite, SWT.CENTER);
        lblDummy1.setLayoutData(
                new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

        if (copiedToSigmet) {
            init();
            copiedToSigmet = false;
        }

        return detailsComposite;

    }

    private void createDetailsAreaGeneral(Composite detailsComposite) {
        Group top3 = new Group(detailsComposite, SWT.LEFT);
        top3.setLayoutData(
                new GridData(SWT.FILL, SWT.CENTER, true, true, 8, 1));
        top3.setLayout(new GridLayout(8, false));

        final Button btnNewUpdate = new Button(top3, SWT.RADIO);
        btnNewUpdate.setText("New/Update");
        btnNewUpdate.setSelection(true);
        this.setEditableAttrStatus("0");
        btnNewUpdate.setLayoutData(
                new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1));
        btnNewUpdate.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event e) {
                setEditableAttrStatus(STATUS_NEW);
            }
        });

        final Button btnAmend = new Button(top3, SWT.RADIO);
        btnAmend.setText("Amend");
        btnAmend.setLayoutData(
                new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1));
        btnAmend.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event e) {
                setEditableAttrStatus(STATUS_AMEND);
            }
        });

        final Button btnCancel = new Button(top3, SWT.RADIO);
        btnCancel.setText("Cancel");
        btnCancel.setLayoutData(
                new GridData(SWT.LEFT, SWT.CENTER, true, false, 4, 1));
        btnCancel.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event e) {
                setEditableAttrStatus(STATUS_CANCEL);

                if (btnCancel.getSelection()) {
                    if (SigmetAttrDlg.this.drawingLayer.getActiveProduct()
                            .getInputFile() != null) {
                        Sigmet sigmet = (Sigmet) getSigmet();
                        // User can CANCEL a SIGMET if it is active
                        // or Open the Cancel Dialog if it is already a
                        // cancelled Sigmet (ie. Cancelled Sigmet *.xml exist
                        if (isSigmetActive() || (sigmet != null && STATUS_CANCEL
                                .equals(sigmet.getEditableAttrStatus()))) {
                            try {
                                sigmetCnlDlg = new SigmetCancelDlg(
                                        getInstance(getShell()), getShell(),
                                        sigmet);
                            } catch (Exception ee) {
                                statusHandler
                                        .warn("Unable to create SIGMET Cancellation Dialog: "
                                                + ee.getLocalizedMessage(), ee);
                            }
                            if (sigmetCnlDlg != null) {
                                sigmetCnlDlg.open();
                            }
                        } else {
                            statusHandler.warn(
                                    "Unable to cancel SIGMET product: SIGMET is not active.");
                            // Can't Cancel but update
                            btnCancel.setSelection(false);
                            btnNewUpdate.setSelection(true);
                            setEditableAttrStatus(STATUS_NEW);
                        }
                    } else {
                        statusHandler.warn(
                                "Unable to cancel SIGMET product: SIGMET is not Saved.");
                        // Can't Cancel but update
                        btnCancel.setSelection(false);
                        btnNewUpdate.setSelection(true);
                        setEditableAttrStatus(STATUS_NEW);
                    }
                }
            }
        });

        attrButtonMap.put(EDITABLE_ATTR_STATUS,
                new Button[] { btnNewUpdate, btnAmend, btnCancel });

        Label lblValidFrom = new Label(top3, SWT.LEFT);
        lblValidFrom.setText("Valid from:");

        final Text txtValidFrom = new Text(top3, SWT.LEFT | SWT.BORDER);
        attrControlMap.put("editableAttrStartTime", txtValidFrom);
        String startTime = StringUtil.isEmptyString(editableAttrStartTime)
                ? this.getTimeStringPlusHourInHMS(0)
                : this.editableAttrStartTime;
        txtValidFrom.setText(startTime);
        setEditableAttrStartTime(txtValidFrom.getText());

        txtValidFrom.addListener(SWT.Verify, new Listener() {
            @Override
            public void handleEvent(Event e) {
                e.doit = validateHMSInput(e, txtValidFrom);
                if (!e.doit) {
                    return;
                }
                SigmetAttrDlg.this
                        .setEditableAttrStartTime(txtValidFrom.getText());
            }
        });
        txtValidFrom.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(org.eclipse.swt.events.FocusEvent e) {
                // No op
            }

            @Override
            public void focusLost(org.eclipse.swt.events.FocusEvent e) {
                String timeString = txtValidFrom.getText();
                if (timeString == null || timeString.length() != 6
                        || !validateTimeStringInDHM(timeString)) {
                    txtValidFrom.setText(getTimeStringPlusHourInHMS(0));
                }
                setEditableAttrStartTime(txtValidFrom.getText());
            }
        });
        txtValidFrom.addKeyListener(new org.eclipse.swt.events.KeyListener() {
            @Override
            public void keyPressed(org.eclipse.swt.events.KeyEvent e) {
                if (e.keyCode == SWT.KEYPAD_CR || e.keyCode == SWT.CR) {
                    String timeString = txtValidFrom.getText();
                    if (timeString == null || timeString.length() != 6
                            || !validateTimeStringInDHM(timeString)) {
                        txtValidFrom.setText(getTimeStringPlusHourInHMS(0));
                    }
                }
                SigmetAttrDlg.this
                        .setEditableAttrStartTime(txtValidFrom.getText());
            }

            @Override
            public void keyReleased(org.eclipse.swt.events.KeyEvent e) {
                // No Op
            }
        });

        Label lblTo = new Label(top3, SWT.LEFT);
        lblTo.setText("To:");

        txtTo = new Text(top3, SWT.LEFT | SWT.BORDER);
        attrControlMap.put("editableAttrEndTime", txtTo);
        String endTime;
        endTime = StringUtil.isEmptyString(editableAttrEndTime)
                ? this.getTimeStringPlusHourInHMS(4) : this.editableAttrEndTime;
        txtTo.setText(endTime);
        setEditableAttrEndTime(txtTo.getText());

        txtTo.addListener(SWT.Verify, new Listener() {
            @Override
            public void handleEvent(Event e) {
                e.doit = validateHMSInput(e, txtTo);
                if (!e.doit) {
                    return;
                }
                setEditableAttrEndTime(txtTo.getText());
            }
        });
        txtTo.addFocusListener(new org.eclipse.swt.events.FocusListener() {
            @Override
            public void focusGained(org.eclipse.swt.events.FocusEvent e) {
                // No Op
            }

            @Override
            public void focusLost(org.eclipse.swt.events.FocusEvent e) {
                String timeString = txtValidFrom.getText();
                if (timeString == null || timeString.length() != 6
                        || !validateTimeStringInDHM(timeString)) {
                    txtValidFrom.setText(getTimeStringPlusHourInHMS(4));
                }
                setEditableAttrEndTime(txtTo.getText());
            }
        });
        txtTo.addKeyListener(new org.eclipse.swt.events.KeyListener() {
            @Override
            public void keyPressed(org.eclipse.swt.events.KeyEvent e) {
                if (e.keyCode == SWT.KEYPAD_CR || e.keyCode == SWT.CR) {
                    String timeString = txtValidFrom.getText();
                    if (timeString == null || timeString.length() != 6
                            || !validateTimeStringInDHM(timeString)) {
                        txtValidFrom.setText(getTimeStringPlusHourInHMS(4));
                    }
                }
                setEditableAttrEndTime(txtTo.getText());
            }

            @Override
            public void keyReleased(org.eclipse.swt.events.KeyEvent e) {
                // No Op
            }
        });

        Label lblStartPlus = new Label(top3, SWT.LEFT);
        lblStartPlus.setText("Start plus:");
        final Button btnStartPlus4hrs = new Button(top3, SWT.PUSH);
        btnStartPlus4hrs.setText("4hrs");
        btnStartPlus4hrs.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event e) {
                if (txtValidFrom.getText().length() != 6) {
                    return;
                }
                txtTo.setText(convertTimeStringPlusHourInHMS(
                        txtValidFrom.getText(), 4, true));
                setEditableAttrEndTime(txtTo.getText());
            }
        });

        final Button btnStartPlus6hrs = new Button(top3, SWT.PUSH);
        btnStartPlus6hrs.setText("6hrs");
        btnStartPlus6hrs.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event e) {
                if (txtValidFrom.getText().length() != 6) {
                    return;
                }
                txtTo.setText(convertTimeStringPlusHourInHMS(
                        txtValidFrom.getText(), 6, true));
                setEditableAttrEndTime(txtTo.getText());
            }
        });

        Label lblPhenom = new Label(top3, SWT.LEFT);
        lblPhenom.setText("Phenom:");
        comboPhenom = new Combo(top3, SWT.LEFT | SWT.READ_ONLY);
        attrControlMap.put("editableAttrPhenom", comboPhenom);
        comboPhenom.setItems(
                getPhenomenons(SigmetInfo.getSigmetTypeString(pgenType)));
        setControl(comboPhenom, "editableAttrPhenom");
        comboPhenom.setLayoutData(
                new GridData(SWT.LEFT, SWT.CENTER, true, true, 7, 1));

        comboPhenom.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event e) {
                setEditableAttrPhenom(comboPhenom.getText());
                String endTime;
                switch (comboPhenom.getText()) {
                case PgenConstant.TYPE_TROPICAL_CYCLONE:
                case PgenConstant.TYPE_VOLCANIC_ASH:
                    endTime = convertTimeStringPlusHourInHMS(
                            txtValidFrom.getText(), 6, true);
                    break;
                default:
                    endTime = convertTimeStringPlusHourInHMS(
                            txtValidFrom.getText(), 4, true);

                }

                txtTo.setText(endTime);
                setEditableAttrEndTime(txtTo.getText());
                withExpandedArea = true;
                tropCycFlag = PgenConstant.TYPE_TROPICAL_CYCLONE
                        .equals(editableAttrPhenom);
                copyEditableAttrToSigmet((Sigmet) getSigmet());
            }
        });
    }

    private void createDetailsAreaPhenomDetailsTropCyclone(
            Composite detailsComposite, GridData gdText) {
        Group topPhenom = new Group(detailsComposite, SWT.LEFT);
        topPhenom.setLayoutData(
                new GridData(SWT.FILL, SWT.CENTER, true, true, 8, 1));
        topPhenom.setLayout(new GridLayout(8, false));

        Shell shell = getShell();
        Label lblSEPhenomName = new Label(topPhenom, SWT.LEFT);
        lblSEPhenomName.setText("Select / Enter\nPhenom Name: ");

        final Text txtSEPhenomName = new Text(topPhenom, SWT.LEFT | SWT.BORDER);
        attrControlMap.put(EDITABLE_ATTR_PHENOM_NAME, txtSEPhenomName);
        txtSEPhenomName.setLayoutData(
                new GridData(SWT.FILL, SWT.CENTER, true, true, 7, 1));

        txtSEPhenomName.addListener(SWT.Modify, new Listener() {
            @Override
            public void handleEvent(Event e) {
                setEditableAttrPhenomName(txtSEPhenomName.getText());
            }
        });

        Label lblPheLat = new Label(topPhenom, SWT.LEFT);
        lblPheLat.setText("Observed Phenom\nLat: ");
        txtPheLat = new Text(topPhenom, SWT.LEFT | SWT.BORDER);
        attrControlMap.put("editableAttrPhenomLat", txtPheLat);
        txtPheLat.setText(txtInfo.getText().split(" ")[0]);
        setEditableAttrPhenomLat(
                getPhenomLatLon(txtPheLat.getText().trim(), true));
        txtPheLat.setLayoutData(gdText);
        txtPheLat.addListener(SWT.Modify, new Listener() {
            @Override
            public void handleEvent(Event e) {
                String phenomLat = getPhenomLatLon(txtPheLat.getText().trim(),
                        true);
                if (!"".equals(phenomLat)) {
                    setEditableAttrPhenomLat(phenomLat);
                } else {
                    setEditableAttrPhenomLat(null);
                }
            }
        });
        txtPheLat.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                // No Op
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (getEditableAttrPhenomLat() != null) {
                    txtPheLat.setText(getEditableAttrPhenomLat());
                    setBackgroundColor(txtPheLat, rightFormatColor);
                } else {
                    /*
                     * "???" causes inconvenience for copy/paste. Instead, use
                     * Color as hint.
                     */
                    txtPheLat.setText("");
                    setBackgroundColor(txtPheLat, wrongFormatColor);
                }
            }
        });

        Label lblPheLon = new Label(topPhenom, SWT.LEFT);
        lblPheLon.setText("Observed Phenom\nLon: ");
        txtPheLon = new Text(topPhenom, SWT.LEFT | SWT.BORDER);
        attrControlMap.put("editableAttrPhenomLon", txtPheLon);
        txtPheLon.setText(txtInfo.getText().split(" ")[1]);
        setEditableAttrPhenomLon(
                getPhenomLatLon(txtPheLon.getText().trim(), false));
        txtPheLon.setLayoutData(gdText);
        txtPheLon.addListener(SWT.Modify, new Listener() {
            @Override
            public void handleEvent(Event e) {
                String phenomLon = getPhenomLatLon(txtPheLon.getText().trim(),
                        false);
                if (!"".equals(phenomLon)) {
                    setEditableAttrPhenomLon(phenomLon);
                } else {
                    setEditableAttrPhenomLon(null);
                }
            }
        });
        txtPheLon.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                // No Op
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (getEditableAttrPhenomLon() != null) {
                    txtPheLon.setText(getEditableAttrPhenomLon());
                    setBackgroundColor(txtPheLon, rightFormatColor);
                } else {
                    /*
                     * "???" causes inconvenience for copy/paste. Instead, use
                     * Color as hint.
                     */
                    txtPheLon.setText("");
                    setBackgroundColor(txtPheLon, wrongFormatColor);
                }
            }
        });

        Label lblPressure = new Label(topPhenom, SWT.LEFT);
        lblPressure.setEnabled(tropCycFlag);
        lblPressure.setText("Pressure\nHPA: ");
        Text txtPressure = new Text(topPhenom, SWT.LEFT | SWT.BORDER);
        txtPressure.setEnabled(tropCycFlag);

        txtPressure.addListener(SWT.Modify, new Listener() {
            @Override
            public void handleEvent(Event e) {
                if (validateNumInput(txtPressure.getText())) {
                    setEditableAttrPhenomPressure(txtPressure.getText());
                }
            }
        });
        txtPressure.setLayoutData(gdText);
        attrControlMap.put("editableAttrPhenomPressure", txtPressure);

        Label lblMaxWinds = new Label(topPhenom, SWT.LEFT);
        lblMaxWinds.setEnabled(tropCycFlag);
        lblMaxWinds.setText("Max\nWinds: ");
        Text txtMaxWinds = new Text(topPhenom, SWT.LEFT | SWT.BORDER);
        txtMaxWinds.setEnabled(tropCycFlag);
        this.setEditableAttrPhenomMaxWind(txtMaxWinds.getText());

        txtMaxWinds.addListener(SWT.Modify, new Listener() {
            @Override
            public void handleEvent(Event e) {
                if (validateNumInput(txtMaxWinds.getText())) {
                    setEditableAttrPhenomMaxWind(txtMaxWinds.getText());
                }
            }
        });
        txtMaxWinds.setLayoutData(gdText);
        attrControlMap.put("editableAttrPhenomMaxWind", txtMaxWinds);
    }

    private void createDetailsAreaPhenomDetailsVolc(Composite detailsComposite,
            GridData gdText) {
        Group topPhenom = new Group(detailsComposite, SWT.LEFT);
        topPhenom.setLayoutData(
                new GridData(SWT.FILL, SWT.CENTER, true, true, 8, 1));
        topPhenom.setLayout(new GridLayout(8, false));

        Shell shell = getShell();
        Label lblSEPhenomName = new Label(topPhenom, SWT.LEFT);
        lblSEPhenomName.setText("Select / Enter\nPhenom Name: ");

        final Text txtSEPhenomName = new Text(topPhenom, SWT.LEFT | SWT.BORDER);
        attrControlMap.put(EDITABLE_ATTR_PHENOM_NAME, txtSEPhenomName);
        txtSEPhenomName.setLayoutData(
                new GridData(SWT.FILL, SWT.CENTER, true, false, 6, 1));
        txtSEPhenomName.addListener(SWT.Modify, new Listener() {
            @Override
            public void handleEvent(Event e) {
                setEditableAttrPhenomName(txtSEPhenomName.getText());
                setPhenomLatLon(txtSEPhenomName.getText());
            }
        });

        final ToolBar tb = new ToolBar(topPhenom, SWT.HORIZONTAL);
        final ToolItem ti = new ToolItem(tb, SWT.PUSH);
        ImageDescriptor id = Activator.imageDescriptorFromPlugin(
                Activator.PLUGIN_ID, "icons/dropdown_arrow.gif");
        Image icon = null;
        if (id != null) {
            icon = id.createImage();
        }
        ti.setImage(icon);
        final Menu mu = new Menu(shell, SWT.POP_UP);

        for (int i = 0; i < SigmetInfo.VOL_NAME_BUCKET_ARRAY.length; i++) {
            // first option is entering name
            if (i == 0) {
                MenuItem mi1 = new MenuItem(mu, SWT.PUSH);
                mi1.setText(SigmetInfo.VOL_NAME_BUCKET_ARRAY[i]);
            } else {
                MenuItem mi1 = new MenuItem(mu, SWT.CASCADE);
                mi1.setText(SigmetInfo.VOL_NAME_BUCKET_ARRAY[i]);
                Menu mi1Menu = new Menu(shell, SWT.DROP_DOWN);
                mi1.setMenu(mi1Menu);

                List<String> list = SigmetInfo.VOLCANO_BUCKET_MAP
                        .get(SigmetInfo.VOL_NAME_BUCKET_ARRAY[i]);
                int size = list.size();
                for (int j = 0; j < size; j++) {
                    final MenuItem mi1MenuMi1 = new MenuItem(mi1Menu, SWT.PUSH);
                    mi1MenuMi1.setText(list.get(j));
                    mi1MenuMi1.addListener(SWT.Selection, new Listener() {
                        @Override
                        public void handleEvent(Event e) {
                            txtSEPhenomName.setText(mi1MenuMi1.getText());
                            setPhenomLatLon(mi1MenuMi1.getText());
                        }
                    });
                }
            }
        }

        ti.addListener(SWT.Selection, new Listener() {
            /*
             * Main button clicked: Pop up the menu showing all the symbols.
             */
            @Override
            public void handleEvent(Event event) {
                Rectangle bounds = ti.getBounds();
                Point point = tb.toDisplay(bounds.x, bounds.y + bounds.height);
                mu.setLocation(point);
                mu.setVisible(true);
            }
        });

        Label lblPheLat = new Label(topPhenom, SWT.LEFT);
        lblPheLat.setText("Phenom\nLat: ");
        txtPheLat = new Text(topPhenom, SWT.LEFT | SWT.BORDER);
        attrControlMap.put("editableAttrPhenomLat", txtPheLat);

        txtPheLat.addListener(SWT.Modify, new Listener() {
            @Override
            public void handleEvent(Event e) {
                String phenomLat = getPhenomLatLon(txtPheLat.getText().trim(),
                        true);
                if (!"".equals(phenomLat)) {
                    setEditableAttrPhenomLat(phenomLat);
                } else {
                    setEditableAttrPhenomLat(null);
                }
            }
        });
        txtPheLat.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                // No Op
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (getEditableAttrPhenomLat() != null) {
                    txtPheLat.setText(getEditableAttrPhenomLat());
                    setBackgroundColor(txtPheLat, rightFormatColor);
                } else {
                    /*
                     * "???" causes inconvenience for copy/paste. Instead, use
                     * Color as hint.
                     */
                    txtPheLat.setText("");
                    setBackgroundColor(txtPheLat, wrongFormatColor);
                }
            }
        });

        txtPheLat.setLayoutData(gdText);

        Label lblPheLon = new Label(topPhenom, SWT.LEFT);
        lblPheLon.setText("Phenom\nLon: ");
        txtPheLon = new Text(topPhenom, SWT.LEFT | SWT.BORDER);
        attrControlMap.put("editableAttrPhenomLon", txtPheLon);

        txtPheLon.addListener(SWT.Modify, new Listener() {
            @Override
            public void handleEvent(Event e) {
                String phenomLon = getPhenomLatLon(txtPheLon.getText().trim(),
                        false);
                if (!"".equals(phenomLon)) {
                    setEditableAttrPhenomLon(phenomLon);
                } else {
                    setEditableAttrPhenomLon(null);
                }
            }
        });
        txtPheLon.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                // No Op
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (getEditableAttrPhenomLon() != null) {
                    txtPheLon.setText(getEditableAttrPhenomLon());
                    setBackgroundColor(txtPheLon, rightFormatColor);
                } else {
                    /*
                     * "???" causes inconvenience for copy/paste. Instead, use
                     * Color as hint.
                     */
                    txtPheLon.setText("");
                    setBackgroundColor(txtPheLon, wrongFormatColor);
                }
            }
        });

        txtPheLon.setLayoutData(gdText);

        Label lblPressure = new Label(topPhenom, SWT.LEFT);
        lblPressure.setEnabled(tropCycFlag);
        lblPressure.setText("Pressure\nHPA: ");
        Text txtPressure = new Text(topPhenom, SWT.LEFT | SWT.BORDER);
        txtPressure.setEnabled(tropCycFlag);

        txtPressure.addListener(SWT.Modify, new Listener() {
            @Override
            public void handleEvent(Event e) {
                if (validateNumInput(txtPressure.getText())) {
                    setEditableAttrPhenomPressure(txtPressure.getText());
                }
            }
        });
        txtPressure.setLayoutData(gdText);
        attrControlMap.put("editableAttrPhenomPressure", txtPressure);

        Label lblMaxWinds = new Label(topPhenom, SWT.LEFT);
        lblMaxWinds.setEnabled(tropCycFlag);
        lblMaxWinds.setText("Max\nWinds: ");
        Text txtMaxWinds = new Text(topPhenom, SWT.LEFT | SWT.BORDER);
        txtMaxWinds.setEnabled(tropCycFlag);
        this.setEditableAttrPhenomMaxWind(txtMaxWinds.getText());

        txtMaxWinds.addListener(SWT.Modify, new Listener() {
            @Override
            public void handleEvent(Event e) {
                if (validateNumInput(txtMaxWinds.getText())) {
                    setEditableAttrPhenomMaxWind(txtMaxWinds.getText());
                }
            }
        });
        txtMaxWinds.setLayoutData(gdText);
        attrControlMap.put("editableAttrPhenomMaxWind", txtMaxWinds);
    }

    private void createDetailsAreaPhenomAttr(Composite detailsComposite) {
        Group top4 = new Group(detailsComposite, SWT.LEFT);
        top4.setLayoutData(
                new GridData(SWT.FILL, SWT.CENTER, true, true, 8, 1));
        top4.setLayout(new GridLayout(8, false));
        top4.setText("".equals(editableAttrPhenom) ? comboPhenom.getText()
                : editableAttrPhenom + " Attributes: ");

        comboPhenom.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event event) {
                editableAttrPhenom = comboPhenom.getText().trim();
                comboPhenomCalled = true;
                top4.setText(editableAttrPhenom + " Attributes: ");
                copyEditableAttrToSigmet((Sigmet) getSigmet());
                showDetailsArea();

            }
        });

        Label lblMovement = new Label(top4, SWT.LEFT);
        lblMovement.setText("Movement: ");

        final Button btnSTNRY = new Button(top4, SWT.RADIO);
        btnSTNRY.setText(STNRY);
        btnSTNRY.setSelection(true);
        this.setEditableAttrMovement(STNRY);

        btnSTNRY.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event e) {
                setEditableAttrMovement(STNRY);
            }
        });

        final Button btnMVG = new Button(top4, SWT.RADIO);
        btnMVG.setText("MVG      ");
        btnMVG.setLayoutData(
                new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1));

        btnMVG.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event e) {
                setEditableAttrMovement("MVG");
            }
        });
        attrButtonMap.put(EDITABLE_ATTR_MOVEMENT,
                new Button[] { btnSTNRY, btnMVG });

        Label lblSpeed = new Label(top4, SWT.LEFT);
        lblSpeed.setText("Speed: ");
        final Combo comboSpeed = new Combo(top4, SWT.READ_ONLY);
        attrControlMap.put(EDITABLE_ATTR_PHENOM_SPEED, comboSpeed);
        comboSpeed.setItems(SigmetInfo.SPEED_ARRAY);
        if (editableAttrPhenomSpeed == null) {
            comboSpeed.select(0);
            this.setEditableAttrPhenomSpeed(comboSpeed.getText());
        }
        copyEditableAttrToSigmet((Sigmet) getSigmet());
        comboSpeed.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event e) {
                SigmetAttrDlg.this
                        .setEditableAttrPhenomSpeed(comboSpeed.getText());
                copyEditableAttrToSigmet((Sigmet) getSigmet());

            }
        });

        Label lblDirection = new Label(top4, SWT.LEFT);
        lblDirection.setText("Direction toward:");
        final Combo comboDirection = new Combo(top4, SWT.READ_ONLY);
        attrControlMap.put(EDITABLE_ATTR_PHENOM_DIRECTION, comboDirection);
        comboDirection.setItems(SigmetInfo.DIRECT_ARRAY);
        if (editableAttrPhenomDirection == null) {
            comboDirection.select(0);
            this.setEditableAttrPhenomDirection(comboDirection.getText());
        }
        copyEditableAttrToSigmet((Sigmet) getSigmet());
        comboDirection.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event e) {
                setEditableAttrPhenomDirection(comboDirection.getText());
                copyEditableAttrToSigmet((Sigmet) getSigmet());
            }
        });

        Label lblTrend = new Label(top4, SWT.LEFT);
        lblTrend.setText("Trend: ");
        final Combo comboTrend = new Combo(top4, SWT.READ_ONLY);
        attrControlMap.put(EDITABLE_ATTR_TREND, comboTrend);
        comboTrend.setItems(SigmetInfo.TREND_ARRAY);
        if (editableAttrTrend == null) {
            comboTrend.select(1);
            this.setEditableAttrTrend(comboTrend.getText());
        }
        copyEditableAttrToSigmet((Sigmet) getSigmet());
        comboTrend.setLayoutData(
                new GridData(SWT.LEFT, SWT.CENTER, false, false, 7, 1));
        comboTrend.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event e) {
                setEditableAttrTrend(comboTrend.getText());
                copyEditableAttrToSigmet((Sigmet) getSigmet());
            }
        });
    }

    private void createDetailsAreaLevel(Composite detailsComposite) {
        Group top5 = new Group(detailsComposite, SWT.LEFT);
        top5.setLayoutData(
                new GridData(SWT.FILL, SWT.CENTER, true, true, 8, 1));
        top5.setLayout(new GridLayout(8, false));

        Label lblLevelInfo = new Label(top5, SWT.LEFT);
        lblLevelInfo.setText("Level Info: ");

        final Combo comboLevel = new Combo(top5, SWT.READ_ONLY);
        comboLevel.setItems(SigmetInfo.LEVEL_ARRAY);
        switch (comboPhenom.getText()) {
        case PgenConstant.TYPE_SEV_TURB:
        case PgenConstant.TYPE_SEV_ICE:
        case PgenConstant.TYPE_RDOACT_CLD:
        case PgenConstant.TYPE_VOLCANIC_ASH:
            comboLevel.setText(comboLevel.getItem(1));// FCST
            break;
        default:
            comboLevel.setText(comboLevel.getItem(0));// TOPS

        }
        setEditableAttrLevel(comboLevel.getText());
        setControl(comboLevel, EDITABLE_ATTR_LEVEL);
        attrControlMap.put(EDITABLE_ATTR_LEVEL, comboLevel);

        final Combo comboLevelInfo1 = new Combo(top5, SWT.READ_ONLY);
        attrControlMap.put(EDITABLE_ATTR_LEVEL_INFO1, comboLevelInfo1);
        comboLevelInfo1.setItems(SigmetInfo.LEVEL_INFO_ARRAY);
        setControl(comboLevelInfo1, EDITABLE_ATTR_LEVEL_INFO1);

        final Text txtLevelInfo1 = new Text(top5, SWT.SINGLE | SWT.BORDER);
        attrControlMap.put(EDITABLE_ATTR_LEVEL_TEXT1, txtLevelInfo1);
        txtLevelInfo1.setLayoutData(
                new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1));

        txtLevelInfo1.addListener(SWT.Verify, new Listener() {
            @Override
            public void handleEvent(Event e) {
                e.doit = validateNumInput(e);
            }
        });

        GridData gdText1 = new GridData();
        gdText1.widthHint = 66;
        gdText1.grabExcessHorizontalSpace = true;

        txtLevelInfo1.setLayoutData(gdText1);

        final Combo comboLevelInfo2 = new Combo(top5, SWT.READ_ONLY);
        attrControlMap.put(EDITABLE_ATTR_LEVEL_INFO2, comboLevelInfo2);
        comboLevelInfo2.setItems(SigmetInfo.LEVEL_INFO_2_ARRAY);
        setControl(comboLevelInfo2, EDITABLE_ATTR_LEVEL_INFO2);

        comboLevelInfo2.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event e) {
                SigmetAttrDlg.this
                        .setEditableAttrLevelInfo2(comboLevelInfo2.getText());
            }
        });

        final Text txtLevelInfo2 = new Text(top5, SWT.SINGLE | SWT.BORDER);
        attrControlMap.put(EDITABLE_ATTR_LEVEL_TEXT2, txtLevelInfo2);
        txtLevelInfo2.setLayoutData(
                new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1));

        txtLevelInfo2.addListener(SWT.Verify, new Listener() {
            @Override
            public void handleEvent(Event e) {
                e.doit = validateNumInput(e);
            }
        });
        txtLevelInfo2.setLayoutData(gdText1);

        txtLevelInfo2.addListener(SWT.Modify, new Listener() {
            @Override
            public void handleEvent(Event e) {
                qualityCheckForLevelInfo(txtLevelInfo2.getText());
                SigmetAttrDlg.this
                        .setEditableAttrLevelText2(txtLevelInfo2.getText());
            }
        });

        comboLevel.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event e) {
                setEditableAttrLevel(comboLevel.getText());
                if (NONE.equals(comboLevel.getText())) {
                    txtLevelInfo1.setText("");
                    txtLevelInfo2.setText("");
                    txtFreeText.setText("");
                    comboLevelInfo2.select(0);
                }
            }
        });

        comboLevelInfo1.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event e) {
                SigmetAttrDlg.this
                        .setEditableAttrLevelInfo1(comboLevelInfo1.getText());
            }
        });

        txtLevelInfo1.addListener(SWT.Modify, new Listener() {
            @Override
            public void handleEvent(Event e) {
                qualityCheckForLevelInfo(txtLevelInfo1.getText());
                SigmetAttrDlg.this
                        .setEditableAttrLevelText1(txtLevelInfo1.getText());
            }
        });
    }

    private void createDetailsAreaRemarks(Composite detailsComposite) {
        Group top6 = new Group(detailsComposite, SWT.LEFT);
        top6.setLayoutData(
                new GridData(SWT.FILL, SWT.CENTER, true, true, 8, 1));
        top6.setLayout(new GridLayout(8, false));

        Label lblFreeText = new Label(top6, SWT.LEFT);
        lblFreeText.setText("Free Text:   ");
        lblFreeText
                .setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
        txtFreeText = new Text(top6, SWT.MULTI | SWT.BORDER);
        attrControlMap.put(EDITABLE_ATTR_FREE_TEXT, txtFreeText);
        GridData gData = new GridData(SWT.FILL, SWT.CENTER, true, true, 7, 1);
        gData.heightHint = 36;
        txtFreeText.setLayoutData(gData);
        txtFreeText.addListener(SWT.Modify, new Listener() {
            @Override
            public void handleEvent(Event e) {
                SigmetAttrDlg.this
                        .setEditableAttrFreeText(txtFreeText.getText());
            }
        });
    }

    public void setNewFirID(Button btn) {
        editableFirID = getFirs();
        String newFir = btn.getText();
        if (btn.getSelection()) {
            if (!editableFirID.contains(newFir)) {
                if (editableFirID.length() == 0) {
                    editableFirID = editableFirID.concat(newFir);
                } else {
                    editableFirID = editableFirID.concat(" " + newFir);
                }
            }
        } else {
            if (editableFirID.contains(newFir)) {
                editableFirID = editableFirID.replaceFirst(newFir, "");
            }
        }
        editableFirID.trim();
    }

    private void createFirRegion(Composite detailsComposite) {
        editableFirID = getFirs();
        setEditableAttrFir(editableFirID);
        Group top7 = new Group(detailsComposite, SWT.LEFT);
        top7.setLayoutData(
                new GridData(SWT.FILL, SWT.CENTER, true, true, 8, 1));
        top7.setLayout(new GridLayout(8, false));
        top7.setText(SigmetConstant.FIR_REGION);

        Group top8 = new Group(top7, SWT.LEFT);
        top8.setLayoutData(
                new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1));
        top8.setLayout(new GridLayout(8, false));
        top8.setText(SigmetConstant.PACIFIC);
        for (String s : SigmetInfo.FIR_PACIFIC) {
            btnPacific = new Button(top8, SWT.CHECK);
            btnPacific.setText(s);
            // map all fir buttons with their name as keys
            firButtonMap.put(s, new Button[] { btnPacific });
            btnPacific.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent event) {
                    setNewFirID(btnPacific);
                }
            });

            if (editableFirID != null) {
                if (editableFirID.contains(s)) {
                    btnPacific.setSelection(true);
                }
            }

        }

        Group top9 = new Group(top7, SWT.TOP);
        top9.setLayoutData(
                new GridData(SWT.LEFT, SWT.CENTER, true, false, 4, 1));
        top9.setLayout(new GridLayout(8, false));
        top9.setText(SigmetConstant.ATLANTIC);
        for (String s : SigmetInfo.FIR_ATLANTIC) {
            btnAtlantic = new Button(top9, SWT.CHECK);
            btnAtlantic.setText(s);
            // map all fir buttons with their name as keys
            firButtonMap.put(s, new Button[] { btnAtlantic });
            btnAtlantic.addSelectionListener(new SelectionAdapter() {

                @Override
                public void widgetSelected(SelectionEvent event) {
                    setNewFirID(btnAtlantic);
                }

            });

            if (editableFirID != null) {
                if (editableFirID.contains(s)) {
                    btnAtlantic.setSelection(true);
                }
            }
        }

        Group firCarSAmericanGrp = new Group(top7, SWT.LEFT);
        firCarSAmericanGrp.setLayoutData(
                new GridData(SWT.FILL, SWT.CENTER, true, true, 8, 1));
        firCarSAmericanGrp.setLayout(new GridLayout(8, false));

        Group firMexicoGrp = new Group(firCarSAmericanGrp, SWT.LEFT);
        firMexicoGrp.setLayoutData(
                new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1));
        firMexicoGrp.setLayout(new GridLayout(8, false));
        firMexicoGrp.setText(SigmetConstant.MEXICO);
        for (String s : SigmetInfo.FIR_MEXICO) {
            btnMexico = new Button(firMexicoGrp, SWT.CHECK);
            btnMexico.setText(s);
            // map all fir buttons with their name as keys
            firButtonMap.put(s, new Button[] { btnMexico });
            btnMexico.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent event) {
                    setNewFirID(btnMexico);
                }
            });

            if (editableFirID != null) {
                if (editableFirID.contains(s)) {
                    btnMexico.setSelection(true);
                }
            }
        }

        Group firOtherGrp = new Group(firCarSAmericanGrp, SWT.TOP);
        firOtherGrp.setLayoutData(
                new GridData(SWT.LEFT, SWT.CENTER, true, false, 4, 1));
        firOtherGrp.setLayout(new GridLayout(8, false));
        firOtherGrp.setText(SigmetConstant.OTHER_SITES);
        for (String s : SigmetInfo.FIR_OTHER) {
            btnOther = new Button(firOtherGrp, SWT.RADIO);
            btnOther.setText(s);
            // map all fir buttons with their name as keys
            firButtonMap.put(s, new Button[] { btnOther });
            btnOther.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent event) {
                    setNewFirID(btnOther);
                }
            });

            if (editableFirID != null) {
                if (editableFirID.contains(s)) {
                    btnOther.setSelection(true);
                }
            }
        }

        // CARSAM Backup Mode
        Group backupGrp = new Group(firCarSAmericanGrp, SWT.TOP);
        backupGrp.setLayoutData(
                new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
        backupGrp.setLayout(new GridLayout(8, false));
        btnCarSamBackUp = new Button(backupGrp, SWT.CHECK);
        btnCarSamBackUp.setText("CARSAM Backup Mode");
        btnCarSamBackUp.setEnabled(false);
        // CARSAM back mode only editable if Fir Region checked is
        // one of the CARSAM sites
        if (editableFirID != null) {
            for (CarSamBackupWmoHeader carsamWmo : SigmetInfo.awcBackupCarSamWmoHeaders
                    .getCarSamBackupWmoHeader()) {
                if (editableFirID.contains(carsamWmo.getFirID())) {
                    btnCarSamBackUp.setEnabled(true);
                    if ((SigmetAttrDlg.this
                            .getEditableAttrCarSamBackupMode() != null)
                            && (SigmetAttrDlg.this
                                    .getEditableAttrCarSamBackupMode()
                                    .contains("true"))) {
                        btnCarSamBackUp.setSelection(true);
                        SigmetAttrDlg.this.isCarSamBackup = true;
                    } else {
                        SigmetAttrDlg.this
                                .setEditableAttrCarSamBackupMode("false");
                        SigmetAttrDlg.this.isCarSamBackup = false;
                    }

                    break;
                }
            }
        }
        btnCarSamBackUp.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent event) {
                Button btn = (Button) event.getSource();
                isCarSamBackup = btn.getSelection();
                SigmetAttrDlg.this.setEditableAttrCarSamBackupMode(
                        Boolean.toString(btn.getSelection()));
                if (isCarSamBackup) {
                    editableFirID = getFirs();
                    String[] firValues = editableFirID.split(" ");
                    for (String firVal : firValues) {
                        for (String other : SigmetInfo.FIR_OTHER) {
                            if (other.contains(firVal)) {
                                // Multiple FIRs not allowed in Backup Mode
                                // uncheck FIR_MEXICO if FIR_OTHER selected.
                                editableFirID = firVal;
                                for (String str : SigmetInfo.FIR_MEXICO) {
                                    Button[] firButt = firButtonMap.get(str);
                                    for (int i = 0; firButt != null
                                            && i < firButt.length; i++) {
                                        if (firButt[i].getText()
                                                .contains(str)) {
                                            firButt[i].setSelection(false);
                                        }
                                    }
                                }
                                break;
                            }
                        }
                    }
                }
            }

        });
    }

    public String getFirs() {
        if (!(editableFirID == null || editableFirID.length() == 0)) {
            return editableFirID;
        }

        if (editableAttrFir != null) {
            editableFirID = editableAttrFir;
        }

        if (SigmetAttrDlg.this.drawingLayer == null) {
            return editableFirID;
        }

        StringBuilder fir = new StringBuilder();

        AbstractDrawableComponent elSelected = SigmetAttrDlg.this.drawingLayer
                .getSelectedComp();
        Coordinate[] coors = (elSelected == null) ? null
                : elSelected.getPoints().toArray(new Coordinate[] {});

        String lineType = null;
        if (SigmetAttrDlg.this.drawingLayer.getSelectedDE() != null) {
            lineType = ((Sigmet) SigmetAttrDlg.this.drawingLayer
                    .getSelectedDE()).getType();
        }

        if (coors == null || lineType == null) {
            return editableFirID;
        }

        if (coors != null && lineType != null) {
            IMapDescriptor mapDescriptor = SigmetAttrDlg.this.drawingLayer
                    .getDescriptor();

            double width = Double.parseDouble(this.widthStr);

            if (SigmetAttrDlg.AREA.equals(lineType)) {

                // get the latest coordinate of the area polygon
                Coordinate[] coorsNew = ((Sigmet) sigmet).getLinePoints();
                Coordinate[] coorsP = new Coordinate[coorsNew.length + 1];

                coorsP = Arrays.copyOf(coorsNew, coorsP.length);
                coorsP[coorsP.length - 1] = coorsNew[0];

                Polygon areaP = SigmetInfo.getPolygon(coorsP, mapDescriptor);
                fir.append(getFirString(areaP));

            } else if (SigmetAttrDlg.ISOLATED.equals(lineType)) {

                Polygon areaP = SigmetInfo.getIsolatedPolygon(coors[0], width,
                        mapDescriptor);
                fir.append(getFirString(areaP));

            } else {// Lines
                String subLineType = lineType
                        .split(SigmetInfo.LINE_SEPERATER)[1];
                Polygon areaP = SigmetInfo.getSOLPolygon(coors, subLineType,
                        width, mapDescriptor);
                fir.append(getFirString(areaP));
            }
        }
        editableFirID = fir.toString().trim();
        return editableFirID;
    }

    public String getFirString(Polygon areaP) {
        StringBuilder fir = new StringBuilder();
        Map<String, Polygon> firPolygonMap = SigmetInfo
                .initFirPolygonMapFromShapfile();

        for (Entry<String, Polygon> entry : firPolygonMap.entrySet()) {
            String aFir = entry.getKey();
            Polygon firP = entry.getValue();

            if (firP.intersects(areaP)
                    && !(fir.toString().contains(aFir.substring(0, 4)))) {
                fir.append(aFir.substring(0, 4)).append(" ");
            }

        }

        String firId = fir.toString();

        String[] firIdArray = firId.split(" ");

        // AWC Backup FIR Regions
        List<String> firIdAWCBackup = new ArrayList<>();

        Collections.addAll(firIdAWCBackup, SigmetInfo.FIR_MEXICO);
        Collections.addAll(firIdAWCBackup, SigmetInfo.FIR_OTHER);

        List<String> newFirID = new ArrayList<>();

        if (firIdArray != null && firIdArray.length > 1) {

            for (String element : firIdArray) {

                for (int i = 0; i < firIdAWCBackup.size(); i++) {

                    if (element.equals(firIdAWCBackup.get(i))) {
                        newFirID.add(element);
                    }

                }

            }
        }

        if (newFirID.size() > 0) {

            firIdArray = newFirID.stream().toArray(String[]::new);

            firId = " ";
            for (String element : firIdArray) {
                firId = firId.concat(element.toString() + " ");

            }
        }

        StringBuilder firNameBuilder = new StringBuilder();
        for (String id : firIdArray) {
            String firName = "";
            for (String s : SigmetInfo.FIR_ARRAY) {
                if (id.equals(s.substring(0, 4))) {
                    firName = s.substring(5, s.length());
                }
            }

            String[] ss = firName.split("_");
            for (String element : ss) {
                firNameBuilder.append(element).append(" ");
            }
            if (!firId.trim().isEmpty()) {
                firNameBuilder.append(" FIR ");
            }
        }

        return firCalledForSecondLine ? firNameBuilder.toString() : firId;
    }

    private void createDetailsAreaSecondPhenom(Composite detailsComposite) {
        Group topSecPhenom = new Group(detailsComposite, SWT.LEFT);
        topSecPhenom.setLayoutData(
                new GridData(SWT.FILL, SWT.CENTER, true, true, 8, 1));
        topSecPhenom.setLayout(new GridLayout(8, false));

        Label lblSecPhenom = new Label(topSecPhenom, SWT.LEFT);
        lblSecPhenom.setText("Second Phenom: ");
        final Combo comboSecPhenom = new Combo(topSecPhenom, SWT.READ_ONLY);
        attrControlMap.put(EDITABLE_ATTR_PHENOM2, comboSecPhenom);
        comboSecPhenom.setItems(SigmetInfo.PHEN_MAP
                .get(SigmetInfo.getSigmetTypeString(pgenType)));
        setControl(comboSecPhenom, EDITABLE_ATTR_PHENOM2);
        comboSecPhenom.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event e) {
                SigmetAttrDlg.this
                        .setEditableAttrPhenom2(comboSecPhenom.getText());
            }
        });
    }

    private void createDetailsAreaForecastSection(Composite detailsComposite) {

        Group topSecPhenom = new Group(detailsComposite, SWT.LEFT);
        topSecPhenom.setLayoutData(
                new GridData(SWT.FILL, SWT.CENTER, true, true, 8, 1));
        topSecPhenom.setLayout(new GridLayout(8, false));
        topSecPhenom.setText("Forecast Section:");

        Label fcstAvailable = new Label(topSecPhenom, SWT.LEFT);
        fcstAvailable.setText("Fcst Available: ");

        Button fcstCheckBox = new Button(topSecPhenom, SWT.CHECK);
        attrControlMap.put(EDITABLE_ATTR_FCST_AVAIL, fcstCheckBox);
        fcstCheckBox.setSelection(true);
        SigmetAttrDlg.this.setEditableAttrFcstAvail(SigmetConstant.TRUE);
        fcstCheckBox.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent event) {
                Button btn = (Button) event.getSource();
                SigmetAttrDlg.this.setEditableAttrFcstAvail(
                        Boolean.toString(btn.getSelection()));
            }
        });

        Label fTimeLabel = new Label(topSecPhenom, SWT.RIGHT);
        fTimeLabel.setText("Fcst Time (Z):");
        Text fcstTimeText = new Text(topSecPhenom, SWT.LEFT);
        attrControlMap.put(EDITABLE_ATTR_FCST_TIME, fcstTimeText);

        // Forecast Time is in HHMM format
        fcstTimeText.setText(getEditableAttrEndTime().substring(2, 6));
        SigmetAttrDlg.this.setEditableAttrFcstTime(fcstTimeText.getText());
        fcstTimeText.addListener(SWT.Modify, new Listener() {
            @Override
            public void handleEvent(Event e) {
                SigmetAttrDlg.this
                        .setEditableAttrFcstTime(fcstTimeText.getText());
            }
        });

        Label fcstCenter = new Label(topSecPhenom, SWT.LEFT);
        fcstCenter.setText("Fcst Center: ");
        Text fcstCenterText = new Text(topSecPhenom, SWT.LEFT);
        fcstCenterText.setLayoutData(
                new GridData(SWT.FILL, SWT.LEFT, true, true, 1, 1));
        attrControlMap.put(EDITABLE_ATTR_FCST_CENTER, fcstCenterText);

        Group topSecPhenomlatLon = new Group(detailsComposite, SWT.LEFT);
        topSecPhenomlatLon.setLayoutData(
                new GridData(SWT.FILL, SWT.CENTER, true, true, 8, 1));
        topSecPhenomlatLon.setLayout(new GridLayout(8, false));

        Label lblFcstPheLat = new Label(topSecPhenomlatLon, SWT.LEFT);
        lblFcstPheLat.setText("Fcst Phenom\nLat: ");
        Text txtFcstPheLat = new Text(topSecPhenomlatLon,
                SWT.LEFT | SWT.BORDER);
        if (SigmetAttrDlg.this.getEditableAttrFcstPhenomLat() != null) {
            txtFcstPheLat
                    .setText(SigmetAttrDlg.this.getEditableAttrFcstPhenomLat());
        }

        txtFcstPheLat.addListener(SWT.Modify, new Listener() {
            @Override
            public void handleEvent(Event e) {
                String fcstPhenomLat = getPhenomLatLon(
                        txtFcstPheLat.getText().trim(), true);
                if (!"".equals(fcstPhenomLat)) {
                    SigmetAttrDlg.this
                            .setEditableAttrFcstPhenomLat(fcstPhenomLat);
                } else {
                    SigmetAttrDlg.this.setEditableAttrFcstPhenomLat(null);
                }
            }
        });

        txtFcstPheLat.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                // No Op
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (getEditableAttrFcstPhenomLat() != null) {
                    txtFcstPheLat.setText(getEditableAttrFcstPhenomLat());
                    setBackgroundColor(txtFcstPheLat, rightFormatColor);
                    if (StringUtils.isEmpty(
                            SigmetAttrDlg.this.getEditableAttrFcstCntr())) {
                        SigmetAttrDlg.this
                                .setEditableAttrFcstCntr(String.format("%s %s",
                                        StringUtils.isEmpty(SigmetAttrDlg.this
                                                .getEditableAttrFcstPhenomLat())
                                                        ? ""
                                                        : SigmetAttrDlg.this
                                                                .getEditableAttrFcstPhenomLat(),
                                        StringUtils.isEmpty(SigmetAttrDlg.this
                                                .getEditableAttrFcstPhenomLon())
                                                        ? ""
                                                        : SigmetAttrDlg.this
                                                                .getEditableAttrFcstPhenomLon()));
                        fcstCenterText.setText(
                                SigmetAttrDlg.this.getEditableAttrFcstCntr());
                    }
                } else {
                    /*
                     * "???" causes inconvenience for copy/paste. Instead, use
                     * Color as hint.
                     */
                    txtFcstPheLat.setText("");
                    setBackgroundColor(txtFcstPheLat, wrongFormatColor);
                }
            }
        });

        Label lblFcstPheLon = new Label(topSecPhenomlatLon, SWT.LEFT);
        lblFcstPheLon.setText("Fcst Phenom\nLon: ");
        Text txtFcstPheLon = new Text(topSecPhenomlatLon,
                SWT.LEFT | SWT.BORDER);
        if (SigmetAttrDlg.this.getEditableAttrFcstPhenomLon() != null) {
            txtFcstPheLon
                    .setText(SigmetAttrDlg.this.getEditableAttrFcstPhenomLon());
        }

        txtFcstPheLon.addListener(SWT.Modify, new Listener() {
            @Override
            public void handleEvent(Event e) {
                String fcstPhenomLon = getPhenomLatLon(
                        txtFcstPheLon.getText().trim(), false);
                if (!"".equals(fcstPhenomLon)) {
                    SigmetAttrDlg.this
                            .setEditableAttrFcstPhenomLon(fcstPhenomLon);
                } else {
                    SigmetAttrDlg.this.setEditableAttrFcstPhenomLon(null);
                }
            }
        });

        txtFcstPheLon.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                // No Op
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (getEditableAttrFcstPhenomLon() != null) {
                    txtFcstPheLon.setText(getEditableAttrFcstPhenomLon());
                    setBackgroundColor(txtFcstPheLon, rightFormatColor);

                    SigmetAttrDlg.this
                            .setEditableAttrFcstCntr(String.format("%s %s",
                                    StringUtils.isEmpty(SigmetAttrDlg.this
                                            .getEditableAttrFcstPhenomLat())
                                                    ? ""
                                                    : SigmetAttrDlg.this
                                                            .getEditableAttrFcstPhenomLat(),
                                    StringUtils.isEmpty(SigmetAttrDlg.this
                                            .getEditableAttrFcstPhenomLon())
                                                    ? ""
                                                    : SigmetAttrDlg.this
                                                            .getEditableAttrFcstPhenomLon()));
                    fcstCenterText.setText(
                            SigmetAttrDlg.this.getEditableAttrFcstCntr());
                    setBackgroundColor(fcstCenterText, rightFormatColor);

                } else {
                    /*
                     * "???" causes inconvenience for copy/paste. Instead, use
                     * Color as hint.
                     */
                    txtFcstPheLon.setText("");
                    setBackgroundColor(txtFcstPheLon, wrongFormatColor);
                }
            }
        });

        fcstCenterText.addListener(SWT.Modify, new Listener() {
            @Override
            public void handleEvent(Event e) {
                SigmetAttrDlg.this.setEditableAttrFcstCntr(
                        (fcstCenterText.getText()).trim());
                for (String coord : SigmetAttrDlg.this.getEditableAttrFcstCntr()
                        .split(" ")) {
                    String valid = getPhenomLatLon(coord, false);
                    if ("".equals(valid)) {
                        SigmetAttrDlg.this.setEditableAttrFcstCntr(null);
                    }
                }
            }
        });

        fcstCenterText.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                // No Op
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (getEditableAttrFcstCntr() != null) {
                    fcstCenterText.setText(getEditableAttrFcstCntr());
                    setBackgroundColor(fcstCenterText, rightFormatColor);

                    if (!StringUtils.isEmpty(
                            SigmetAttrDlg.this.getEditableAttrFcstCntr())) {
                        if (StringUtils.isEmpty(SigmetAttrDlg.this
                                .getEditableAttrFcstPhenomLat())) {
                            SigmetAttrDlg.this.setEditableAttrFcstPhenomLat(
                                    SigmetAttrDlg.this.getEditableAttrFcstCntr()
                                            .split(" ")[0]);
                            txtFcstPheLat.setText(SigmetAttrDlg.this
                                    .getEditableAttrFcstPhenomLat());
                            setBackgroundColor(txtFcstPheLat, rightFormatColor);
                        }
                        if (StringUtils.isEmpty(SigmetAttrDlg.this
                                .getEditableAttrFcstPhenomLon())) {
                            SigmetAttrDlg.this.setEditableAttrFcstPhenomLon(
                                    SigmetAttrDlg.this.getEditableAttrFcstCntr()
                                            .split(" ")[1]);
                            txtFcstPheLon.setText(SigmetAttrDlg.this
                                    .getEditableAttrFcstPhenomLon());
                            setBackgroundColor(txtFcstPheLon, rightFormatColor);
                        }
                    }

                } else {
                    /*
                     * "???" causes inconvenience for copy/paste. Instead, use
                     * Color as hint.
                     */
                    fcstCenterText.setText("");
                    setBackgroundColor(fcstCenterText, wrongFormatColor);
                }
            }
        });

    }

    public void createLevelAltitudes(Group topLbl) {

        Label lblLevelInfo = new Label(topLbl, SWT.LEFT);
        lblLevelInfo.setText("Level Info: ");

        final Combo comboLevel = new Combo(topLbl, SWT.READ_ONLY);
        comboLevel.setItems(SigmetInfo.LEVEL_ARRAY);
        switch (comboPhenom.getText()) {
        case PgenConstant.TYPE_SEV_TURB:
        case PgenConstant.TYPE_SEV_ICE:
        case PgenConstant.TYPE_RDOACT_CLD:
        case PgenConstant.TYPE_VOLCANIC_ASH:
            comboLevel.setText(comboLevel.getItem(1));// FCST
            break;
        default:
            comboLevel.setText(comboLevel.getItem(0));// TOPS

        }
        setEditableAttrLevel(comboLevel.getText());
        attrControlMap.put(EDITABLE_ATTR_ALT_LEVEL, comboLevel);
        setControl(comboLevel, EDITABLE_ATTR_ALT_LEVEL);

        final Combo comboLevelInfo1 = new Combo(topLbl, SWT.READ_ONLY);
        attrControlMap.put(EDITABLE_ATTR_ALT_LEVEL_INFO1, comboLevelInfo1);
        comboLevelInfo1.setItems(SigmetInfo.LEVEL_INFO_ARRAY);
        setControl(comboLevelInfo1, EDITABLE_ATTR_ALT_LEVEL_INFO1);

        final Text txtLevelInfo1 = new Text(topLbl, SWT.SINGLE | SWT.BORDER);
        attrControlMap.put(EDITABLE_ATTR_ALT_LEVEL_TEXT1, txtLevelInfo1);
        txtLevelInfo1.setLayoutData(
                new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1));

        txtLevelInfo1.addListener(SWT.Verify, new Listener() {
            @Override
            public void handleEvent(Event e) {
                e.doit = validateNumInput(e);
            }
        });

        GridData gdText1 = new GridData();
        gdText1.widthHint = 66;
        gdText1.grabExcessHorizontalSpace = true;

        txtLevelInfo1.setLayoutData(gdText1);

        final Combo comboLevelInfo2 = new Combo(topLbl, SWT.READ_ONLY);
        attrControlMap.put(EDITABLE_ATTR_ALT_LEVEL_INFO2, comboLevelInfo2);
        comboLevelInfo2.setItems(SigmetInfo.LEVEL_INFO_2_ARRAY);
        setControl(comboLevelInfo2, EDITABLE_ATTR_ALT_LEVEL_INFO2);

        comboLevelInfo2.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event e) {
                SigmetAttrDlg.this.setEditableAttrAltLevelInfo2(
                        comboLevelInfo2.getText());
            }
        });

        final Text txtLevelInfo2 = new Text(topLbl, SWT.SINGLE | SWT.BORDER);
        attrControlMap.put(EDITABLE_ATTR_ALT_LEVEL_TEXT2, txtLevelInfo2);
        txtLevelInfo2.setLayoutData(
                new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1));

        txtLevelInfo2.addListener(SWT.Verify, new Listener() {
            @Override
            public void handleEvent(Event e) {
                e.doit = validateNumInput(e);
            }
        });
        txtLevelInfo2.setLayoutData(gdText1);

        txtLevelInfo2.addListener(SWT.Modify, new Listener() {
            @Override
            public void handleEvent(Event e) {
                qualityCheckForLevelInfo(txtLevelInfo2.getText());
                SigmetAttrDlg.this
                        .setEditableAttrAltLevelText2(txtLevelInfo2.getText());
            }
        });

        comboLevel.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event e) {
                setEditableAttrAltLevel(comboLevel.getText());
                if (NONE.equals(comboLevel.getText())) {
                    txtLevelInfo1.setText("");
                    txtLevelInfo2.setText("");
                    txtFreeText.setText("");
                    comboLevelInfo2.select(0);
                }
            }
        });

        comboLevelInfo1.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event e) {
                SigmetAttrDlg.this.setEditableAttrAltLevelInfo1(
                        comboLevelInfo1.getText());
            }
        });

        txtLevelInfo1.addListener(SWT.Modify, new Listener() {
            @Override
            public void handleEvent(Event e) {
                qualityCheckForLevelInfo(txtLevelInfo1.getText());
                SigmetAttrDlg.this
                        .setEditableAttrAltLevelText1(txtLevelInfo1.getText());
            }
        });

    }

    private void createDetailsAreaForecastSectionVolcanic(
            Composite detailsComposite) {

        Group topSecPhenom = new Group(detailsComposite, SWT.LEFT);
        topSecPhenom.setLayoutData(
                new GridData(SWT.FILL, SWT.CENTER, true, true, 8, 2));
        topSecPhenom.setLayout(new GridLayout(8, false));
        topSecPhenom.setText("Forecast Section:");

        Label fcstAvailable = new Label(topSecPhenom, SWT.LEFT);
        fcstAvailable.setText("Fcst Available:");

        Button fcstCheckBox = new Button(topSecPhenom, SWT.CHECK);
        attrControlMap.put(EDITABLE_ATTR_FCST_AVAIL, fcstCheckBox);
        fcstCheckBox.setSelection(true);
        SigmetAttrDlg.this.setEditableAttrFcstAvail(SigmetConstant.TRUE);
        fcstCheckBox.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent event) {
                Button btn = (Button) event.getSource();
                SigmetAttrDlg.this.setEditableAttrFcstAvail(
                        Boolean.toString(btn.getSelection()));
            }
        });

        Label fTimeLabel = new Label(topSecPhenom, SWT.RIGHT);
        fTimeLabel.setText("Fcst Time (Z):");
        Text fcstTimeText = new Text(topSecPhenom, SWT.LEFT);
        attrControlMap.put(EDITABLE_ATTR_FCST_TIME, fcstTimeText);

        // Forecast Time is in HHMM format
        fcstTimeText.setText(getEditableAttrEndTime().substring(2, 6));
        SigmetAttrDlg.this.setEditableAttrFcstTime(fcstTimeText.getText());
        fcstTimeText.addListener(SWT.Modify, new Listener() {
            @Override
            public void handleEvent(Event e) {
                SigmetAttrDlg.this
                        .setEditableAttrFcstTime(fcstTimeText.getText());
            }
        });

        Group altitudesGrp = new Group(topSecPhenom, SWT.LEFT);
        altitudesGrp.setLayoutData(
                new GridData(SWT.RIGHT, SWT.CENTER, true, false, 4, 1));
        altitudesGrp.setLayout(new GridLayout(8, false));
        altitudesGrp.setText("Altitudes");

        // Level Info attributes within the Forecast Section of
        // VOLCANIC_ASH phenomenon
        this.createLevelAltitudes(altitudesGrp);

        Group ralGrp = new Group(topSecPhenom, SWT.LEFT);
        ralGrp.setLayoutData(
                new GridData(SWT.LEFT, SWT.CENTER, false, false, 8, 1));
        ralGrp.setLayout(new GridLayout(8, false));
        ralGrp.setText("Fcst Radial/Area/Line Description");

        final Button rbtn = new Button(ralGrp, SWT.RADIO);
        rbtn.setText(RADIUS);

        rbtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                SigmetAttrDlg.this.setEditableAttrRALSelection(RADIUS);
            }
        });

        final Button abtn = new Button(ralGrp, SWT.RADIO);
        abtn.setText(AREA);
        abtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                SigmetAttrDlg.this.setEditableAttrRALSelection(AREA);
            }
        });

        final Button lbtn = new Button(ralGrp, SWT.RADIO);
        lbtn.setText(LINE);
        lbtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                SigmetAttrDlg.this.setEditableAttrRALSelection(LINE);
            }
        });

        attrButtonMap.put(EDITABLE_ATTR_RAL_SELECTION,
                new Button[] { rbtn, abtn, lbtn });

        Label lbl2 = new Label(ralGrp, SWT.LEFT);
        lbl2.setText("  WI     ");

        final Button check = new Button(ralGrp, SWT.CHECK | SWT.RIGHT);
        check.setText("VOR");
        check.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {

            }
        });

        // Forecast coordinates Text not populated by default
        Group coordGrp = new Group(topSecPhenom, SWT.LEFT);
        coordGrp.setLayoutData(
                new GridData(SWT.LEFT, SWT.CENTER, false, false, 8, 1));
        coordGrp.setLayout(new GridLayout(8, false));
        Group desTextGrp = new Group(coordGrp, SWT.LEFT);
        desTextGrp.setLayout(new GridLayout(8, false));

        int style = SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL;
        Text descText = new Text(desTextGrp, style);

        GC gc = new GC(descText);
        int charWidth = gc.getFontMetrics().getAverageCharWidth();
        int charHeight = 5;
        Rectangle size = descText.computeTrim(0, 0, charWidth * 100,
                charHeight);
        gc.dispose();
        descText.setLayoutData(GridDataFactory.defaultsFor(descText).span(8, 1)
                .hint(size.width, size.height).create());

        attrControlMap.put(EDITABLE_ATTR_FCST_VADESC, descText);
        descText.addListener(SWT.Modify, new Listener() {
            @Override
            public void handleEvent(Event e) {
                SigmetAttrDlg.this
                        .setEditableAttrFcstVADesc(descText.getText());
                for (String coord : SigmetAttrDlg.this
                        .getEditableAttrFcstVADesc().split(" ")) {
                    String valid = getPhenomLatLon(coord, false);
                    if ("".equals(valid)) {
                        SigmetAttrDlg.this.setEditableAttrFcstVADesc(null);
                    }
                }
            }
        });

        descText.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                // No Op
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (getEditableAttrFcstVADesc() != null) {
                    descText.setText(getEditableAttrFcstVADesc());
                    setBackgroundColor(descText, rightFormatColor);
                } else {
                    /*
                     * "???" causes inconvenience for copy/paste. Instead, use
                     * Color as hint.
                     */
                    descText.setText("");
                    setBackgroundColor(descText, wrongFormatColor);
                }
            }
        });

        Label lblRound = new Label(coordGrp, SWT.RIGHT);
        lblRound.setText("Round To: ");
        final Combo comboRound = new Combo(coordGrp, SWT.READ_ONLY);
        comboRound.setItems(SigmetInfo.ROUND_TO_ARRAY);
        comboRound.select(2);

    }

    protected final void showDetailsArea() {
        // for save, apply buttons
        withExpandedArea = true;

        Point oldWindowSize = getShell().getSize();
        Point newWindowSize = cachedWindowSize;

        if (detailsArea == null) {
            detailsArea = createDetailsArea((Composite) getContents());

        } else {
            detailsArea.dispose();
            detailsArea = createDetailsArea((Composite) getContents());
        }

        Point oldSize = getContents().getSize();
        Point newSize = getContents().computeSize(SWT.DEFAULT, SWT.DEFAULT);

        if (newWindowSize == null) {
            newWindowSize = new Point(oldWindowSize.x,
                    oldWindowSize.y + (newSize.y - oldSize.y));
        }

        Point windowLoc = getShell().getLocation();
        Rectangle screenArea = getContents().getDisplay().getClientArea();

        if (newWindowSize.y > screenArea.height
                - (windowLoc.y - screenArea.y)) {
            newWindowSize.y = screenArea.height - (windowLoc.y - screenArea.y);
        }

        getShell().setSize(newWindowSize);
        ((Composite) getContents()).layout(true, true);
    }

    private String getVOR(Coordinate[] coors) {
        return SnapUtil.getVORText(coors, "-", this.lineType, 6, false);
    }

    private void setPhenomLatLon(String phenomName) {
        // set Phenom Latitude
        Float lat = getLatitudeForPhenomName(phenomName);
        String latitude = getPhenomLatLon(lat.toString().trim(), true);
        setEditableAttrPhenomLat(latitude);
        txtPheLat.setText(getEditableAttrPhenomLat());

        // set Phenom Longitude
        Float lon = getLongitudeForPhenomName(phenomName);
        String longitude = getPhenomLatLon(lon.toString().trim(), false);
        setEditableAttrPhenomLon(longitude);
        txtPheLon.setText(getEditableAttrPhenomLon());
    }

    private float getLatitudeForPhenomName(String phenomName) {
        float lat = (float) 0.0;
        List<Station> volcanoStnList = SigmetInfo.VOLCANO_STATION_LIST;
        for (Station st : volcanoStnList) {
            if (st.getStnname().equalsIgnoreCase(phenomName)) {
                lat = st.getLatitude();
                return lat;
            }
        }
        return lat;
    }

    private float getLongitudeForPhenomName(String phenomName) {
        float lon = (float) 0.0;
        List<Station> volcanoStnList = SigmetInfo.VOLCANO_STATION_LIST;
        for (Station st : volcanoStnList) {
            if (st.getStnname().equalsIgnoreCase(phenomName)) {
                lon = st.getLongitude();
                return lon;
            }
        }
        return lon;
    }

    @Override
    public Control createDialogArea(Composite parent) {

        top = (Composite) super.createDialogArea(parent);

        GridLayout mainLayout = new GridLayout(12, false);
        mainLayout.marginHeight = 3;
        mainLayout.marginWidth = 3;
        top.setLayout(mainLayout);

        this.getShell().setText("International SIGMET Edit");
        this.setShellStyle(SWT.RESIZE | SWT.CLOSE);

        // Only the portion of the dialog that has the polygon menu
        createDialogAreaGeneral();

        if ("Pgen Select".equals(mouseHandlerName) || withExpandedArea) {
            // The full dialog box
            createDialogAreaSelect(parent);
        }
        init();
        addSeparator(top.getParent());

        return top;
    }

    private void createDialogAreaGeneral() {
        final Button btnArea = new Button(top, SWT.RADIO);
        btnArea.setSelection(true);
        btnArea.setText(AREA);

        final Button btnLine = new Button(top, SWT.RADIO);
        btnLine.setText(LINE);

        final Combo comboLine = new Combo(top, SWT.READ_ONLY);
        // sideOfLine
        attrControlMap.put(LINE_TYPE, comboLine);
        comboLine.setItems(LINE_SIDES);
        attrControlMap.put("sideOfLine", comboLine);
        // default: ESOL
        comboLine.select(0);
        comboLine.setEnabled(false);

        final Button btnIsolated = new Button(top, SWT.RADIO);
        btnIsolated.setText("Isolated  ");

        Label lblText = new Label(top, SWT.LEFT);
        lblText.setText("Width: ");
        final Text txtWidth = new Text(top, SWT.SINGLE | SWT.BORDER);
        attrControlMap.put("widthStr", txtWidth);
        txtWidth.setText(WIDTH);
        txtWidth.setEnabled(false);
        attrButtonMap.put(LINE_TYPE,
                new Button[] { btnArea, btnLine, btnIsolated });

        btnArea.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                comboLine.setEnabled(false);
                txtWidth.setEnabled(false);

                setLineType(AREA);
            }
        });

        btnLine.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                comboLine.setEnabled(true);
                txtWidth.setEnabled(true);

                setLineType(LINE + SigmetInfo.LINE_SEPERATER + getSideOfLine());
            }
        });

        btnIsolated.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                comboLine.setEnabled(false);
                txtWidth.setEnabled(true);

                setLineType(ISOLATED);
            }
        });

        comboLine.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                setSideOfLine(comboLine.getText());
                setLineType(LINE + SigmetInfo.LINE_SEPERATER + getSideOfLine());
            }
        });

        txtWidth.addModifyListener(e -> setWidthStr(txtWidth.getText()));

        Label colorLbl = new Label(top, SWT.LEFT);
        colorLbl.setText("Color:");

        cs = new ColorButtonSelector(top);
        Color clr = Color.cyan;
        cs.setColorValue(new RGB(clr.getRed(), clr.getGreen(), clr.getBlue()));

        // Line Width GUI
        this.createLineWidthControls();

        // set and reset
        setLineType(AREA);
        setSideOfLine(comboLine.getText());

        if (!PgenConstant.TYPE_INTL_SIGMET.equalsIgnoreCase(pgenType)
                && !PgenConstant.TYPE_CONV_SIGMET.equalsIgnoreCase(pgenType)) {
            btnLine.setEnabled(false);
            btnIsolated.setEnabled(false);
            comboLine.setEnabled(false);
            txtWidth.setEnabled(false);
        }
    }

    private void createDialogAreaSelect(Composite parent) {
        String[] mwoItems = SigmetInfo.AREA_MAP
                .get(SigmetInfo.getSigmetTypeString(pgenType));

        Composite topSelect = (Composite) super.createDialogArea(parent);

        GridLayout mainLayout2 = new GridLayout(8, false);
        mainLayout2.marginHeight = 3;
        mainLayout2.marginWidth = 4; // qu
        topSelect.setLayout(mainLayout2);

        Group top2 = new Group(topSelect, SWT.LEFT);
        top2.setLayoutData(
                new GridData(SWT.FILL, SWT.CENTER, true, true, 8, 1));
        top2.setLayout(new GridLayout(8, false));

        Label lblISU = new Label(top2, SWT.LEFT);
        lblISU.setText("ISSUE: ");
        comboISU = new Combo(top2, SWT.READ_ONLY);
        attrControlMap.put("editableAttrIssueOffice", comboISU);
        comboISU.setItems(mwoItems);
        comboISU.select(0);
        setEditableAttrIssueOffice(comboISU.getText());
        comboISU.setLayoutData(
                new GridData(SWT.LEFT, SWT.CENTER, true, true, 1, 1));

        comboISU.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event e) {
                setEditableAttrIssueOffice(comboISU.getText());
                populateIdList(comboISU.getText());
            }
        });

        Label lblMWO = new Label(top2, SWT.LEFT);
        lblMWO.setText(" MWO: ");
        comboMWO = new Combo(top2, SWT.READ_ONLY);
        attrControlMap.put("editableAttrArea", comboMWO);
        comboMWO.setItems(mwoItems);
        comboMWO.select(0);
        setEditableAttrArea(comboMWO.getText());
        comboMWO.setLayoutData(
                new GridData(SWT.LEFT, SWT.CENTER, true, true, 1, 1));

        comboMWO.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event e) {
                setEditableAttrArea(comboMWO.getText());
            }
        });

        Label lblID = new Label(top2, SWT.LEFT);
        lblID.setText("ID: ");
        comboID = new Combo(top2, SWT.READ_ONLY);
        attrControlMap.put("editableAttrId", comboID);
        populateIdList(comboISU.getText());

        setEditableAttrId(comboID.getText());
        comboID.setLayoutData(
                new GridData(SWT.FILL, SWT.CENTER, true, true, 1, 1));

        comboID.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event e) {
                setEditableAttrId(comboID.getText());
            }
        });

        Label lblSequence = new Label(top2, SWT.LEFT);
        lblSequence.setText("Sequence: ");
        Spinner spiSeq = new Spinner(top2, SWT.BORDER);
        attrControlMap.put("editableAttrSeqNum", spiSeq);
        spiSeq.setMinimum(1);
        spiSeq.setMaximum(300);
        setEditableAttrSeqNum("" + spiSeq.getSelection());
        spiSeq.setLayoutData(
                new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

        spiSeq.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event e) {
                setEditableAttrSeqNum("" + spiSeq.getSelection());
            }
        });

        // ------------------------------ FIR Region Info:
        editableFirID = "";
        createFirRegion(top2);

        final Button btnNew = new Button(top2, SWT.RADIO);
        btnNew.setSelection(true);
        btnNew.setText("LATLON");
        btnNew.setLayoutData(
                new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1));

        final Button btnVor = new Button(top2, SWT.RADIO);
        btnVor.setText("VOR");
        btnVor.setLayoutData(
                new GridData(SWT.LEFT, SWT.CENTER, true, false, 4, 1));

        int style = SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL
                | SWT.READ_ONLY;
        txtInfo = new Text(top2, style);
        attrControlMap.put(EDITABLE_ATTR_FROM_LINE, txtInfo);

        GC gc = new GC(txtInfo);
        int charWidth = gc.getFontMetrics().getAverageCharWidth();
        int charHeight = txtInfo.getLineHeight();
        Rectangle size = txtInfo.computeTrim(0, 0, charWidth * 125, 5);
        gc.dispose();
        txtInfo.setLayoutData(GridDataFactory.defaultsFor(txtInfo).span(8, 1)
                .hint(size.width, size.height).create());

        attrButtonMap.put(EDITABLE_ATTR_FROM_LINE,
                new Button[] { btnNew, btnVor });

        final StringBuilder coorsLatLon = new StringBuilder();
        final AbstractDrawableComponent elSelected = PgenSession.getInstance()
                .getPgenResource().getSelectedComp();
        final Coordinate[] coors = (elSelected == null) ? null
                : elSelected.getPoints().toArray(new Coordinate[] {});

        /*
         * Added "trim()" since SIGMETs VGFs has no "editableAttrFromLine" and
         * it is defaulted as " " when converted into XML - (J. Wu).
         */
        if (coors != null && StringUtils.isBlank(editableAttrFromLine)) {
            coorsLatLon.append(getLatLonStringPrepend2(coors,
                    AREA.equals(((Sigmet) elSelected).getType())));
            resetText(coorsLatLon.toString(), txtInfo);
            // for Sigment element use later
            coorsLatLon.append(SigmetInfo.LINE_SEPERATER);
            String latLonFmtText = coorsLatLon.append("New").toString();
            setLatLonFormatFlagAndText(latLonFmtText);
            setEditableAttrFromLine(latLonFmtText);
        }

        btnNew.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event e) {
                StringBuilder sb = new StringBuilder();
                sb.append(getLatLonStringPrepend2(coors,
                        AREA.equals(((Sigmet) elSelected).getType())));
                resetText(sb.toString(), txtInfo);
                // for Sigment element use later
                sb.append(SigmetInfo.LINE_SEPERATER);
                String latLonFmtText = sb.append("New").toString();
                setLatLonFormatFlagAndText(latLonFmtText);
                setEditableAttrFromLine(latLonFmtText);
            }
        });

        btnVor.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event e) {
                StringBuilder sb = new StringBuilder();
                sb.append(getVOR(coors));
                resetText(sb.toString(), txtInfo);
                // for Sigment element use later
                sb.append(SigmetInfo.LINE_SEPERATER);
                String latLonFmtText = sb.append("VOR").toString();
                setLatLonFormatFlagAndText(latLonFmtText);
                setEditableAttrFromLine(latLonFmtText);
            }
        });

        if (!withExpandedArea) {

            final Button btnEdit = new Button(top2, SWT.PUSH);
            btnEdit.setText("Edit Attributes");
            btnEdit.setLayoutData(
                    new GridData(SWT.FILL, SWT.CENTER, true, true, 8, 1));

            btnEdit.addListener(SWT.Selection, new Listener() {
                @Override
                public void handleEvent(Event e) {
                    withExpandedArea = true;
                    btnEdit.dispose();
                    getButton(SAVE_ID).dispose();
                    getButton(APPLY_ID).dispose();
                    getButton(IDialogConstants.CANCEL_ID).dispose();
                    copyEditableAttrToSigmet((Sigmet) getSigmet());
                    showDetailsArea();
                    withExpandedArea = true;
                    // extra since createDetailArea() inside calls init()
                    init();
                    withExpandedArea = false;
                }
            });
        }
    }

    /*
     * Create widgets for the Line Width attribute
     */
    private void createLineWidthControls() {

        // Line Width Label
        lineWidthLabel = new Label(top, SWT.LEFT);
        lineWidthLabel.setText("Line Width:");

        // Line Width Spinner and Slider
        lineWidthSpinSlide = new SpinnerSlider(top, SWT.HORIZONTAL, 1);
        GridData gd = new GridData(150, 42);
        gd.horizontalIndent = 6;
        lineWidthSpinSlide.setLayoutData(gd);
        lineWidthSpinSlide.setMinimum(1);
        lineWidthSpinSlide.setMaximum(10);
        lineWidthSpinSlide.setIncrement(1);
        lineWidthSpinSlide.setPageIncrement(1);
        lineWidthSpinSlide.setDigits(0);
        attrControlMap.put(EDITABLE_ATTR_POLYGON_LINE_WIDTH,
                lineWidthSpinSlide);
    }

    /**
     * Sets the line width value of the dialog.
     *
     * @param lw
     */
    protected void setSpinnerLineWidth(float lw) {
        lineWidthSpinSlide.setSelection((int) lw);
    }

    /**
     * Returns the line width from the dialog.
     */
    public float getSpinnerLineWidth() {
        if (chkBoxLineWidth.getSelection()) {

            return lineWidthSpinSlide.getSelection();
        } else {
            return java.lang.Float.NaN;
        }

    }

    private void populateIdList(String issueOffice) {

        // IF "INTL", the ID values are different for different Issue.
        if (SigmetInfo.SIGMET_TYPES[0]
                .equals(SigmetInfo.getSigmetTypeString(pgenType))) {
            comboID.setItems(SigmetInfo.getSeriesIDs(issueOffice));
        } else {
            comboID.setItems(SigmetInfo.ID_MAP
                    .get(SigmetInfo.getSigmetTypeString(pgenType)));
        }
        comboID.select(0);
    }

    private boolean validateFcstTimeString(String time) {
        // Use HHMM zulu time
        if (time == null || time.trim().length() != 4) {
            return false;
        }

        boolean result = true;
        final String HHMM_TIME = "([01]?[0-9]|2[0-3])[0-5][0-9]";
        final Pattern timePattern = Pattern.compile(HHMM_TIME);
        if (!timePattern.matcher(time).matches()) {
            result = false;
        }
        return result;
    }

    private String getTimeStringPlusHourInHMS(int plusHour) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, c.get(Calendar.HOUR_OF_DAY) + plusHour);

        // round to 5 min
        c.set(Calendar.MINUTE, (c.get(Calendar.MINUTE) / 5) * 5);

        DateFormat dateFormat = new SimpleDateFormat("ddHHmm");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return dateFormat.format(c.getTime());
    }

    private String convertTimeStringPlusHourInHMS(String timeString,
            int plusHour, boolean dayNeeded) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_MONTH,
                Integer.parseInt(timeString.substring(0, 2)));
        c.set(Calendar.HOUR_OF_DAY,
                Integer.parseInt(timeString.substring(2, 4)) + plusHour);
        c.set(Calendar.MINUTE, Integer.parseInt(timeString.substring(4, 6)));
        String day = c.get(Calendar.DAY_OF_MONTH) > 9
                ? Integer.toString(c.get(Calendar.DAY_OF_MONTH))
                : "0" + c.get(Calendar.DAY_OF_MONTH);
        String hour = c.get(Calendar.HOUR_OF_DAY) > 9
                ? Integer.toString(c.get(Calendar.HOUR_OF_DAY))
                : "0" + c.get(Calendar.HOUR_OF_DAY);
        String minute = c.get(Calendar.MINUTE) > 9
                ? Integer.toString(c.get(Calendar.MINUTE))
                : "0" + c.get(Calendar.MINUTE);
        return dayNeeded
                ? new StringBuilder().append(day).append(hour).append(minute)
                        .toString()
                : new StringBuilder().append(hour).append(minute).toString();
    }

    private boolean validateTimeStringInDHM(String time) {
        if (time == null || time.trim().length() != 6) {
            return false;
        }
        String txt = time.trim();
        char[] chars = txt.toCharArray();
        boolean result = true;
        if (chars[0] < '0' || chars[0] > '3') {
            result = false;
        }
        if (chars[1] < '0' || chars[1] > '9'
                || (chars[0] == '3' && chars[1] > '1')
                || (chars[0] == '0' && chars[1] < '1')) {
            result = false;
        }
        if (chars[2] < '0' || chars[2] > '2') {
            result = false;
        }
        if (chars[3] < '0' || chars[3] > '9'
                || (chars[2] == '2' && chars[3] > '3')) {
            result = false;
        }
        if (chars[4] < '0' || chars[4] > '5') {
            result = false;
        }
        if (chars[5] < '0' || chars[5] > '9') {
            result = false;
        }

        return result;
    }

    private boolean validateTimeDifference(String from, String to,
            long maxAllowedMillis) {

        SimpleDateFormat format = new SimpleDateFormat("ddHHmm");
        try {
            Date toDate = format.parse(to);
            Date fromDate = format.parse(from);

            long diff = toDate.getTime() - fromDate.getTime();
            if (diff <= 0 || diff > maxAllowedMillis) {
                return false;
            }

            return true;
        } catch (ParseException e) {
            return false;
        }

    }

    private boolean validateHMSInput(Event e, Text txt) {

        boolean result = true;

        String string = e.text.trim();
        char[] chars = new char[string.length()];
        string.getChars(0, chars.length, chars, 0);

        int i = e.start;

        if (i > 5) {
            result = false;
        }

        if (chars.length > 0) {

            if (i == 0 && (chars[0] < '0' || chars[0] > '3')) {
                result = false;
            }

            if (i == 1 && (chars[0] < '0' || chars[0] > '9'
                    || (txt.getText().charAt(0) == '3' && chars[0] > '1')
                    || (txt.getText().charAt(0) == '0' && chars[0] < '1'))) {
                result = false;
            }

            if (i == 2 && (chars[0] < '0' || chars[0] > '2')) {
                result = false;
            }

            if (i == 3 && (chars[0] < '0' || chars[0] > '9'
                    || (txt.getText().charAt(2) == '2' && chars[0] > '3'))) {
                result = false;
            }

            if (i == 4 && (chars[0] < '0' || chars[0] > '5')) {
                result = false;
            }

            if (i == 5 && (chars[0] < '0' || chars[0] > '9')) {
                result = false;
            }
        }
        return result;
    }

    private boolean isSigmetActive() {
        boolean isActive = false;
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, c.get(Calendar.HOUR_OF_DAY));

        // round to 5 min
        c.set(Calendar.MINUTE, (c.get(Calendar.MINUTE) / 5) * 5);

        DateFormat dateFormat = new SimpleDateFormat("ddHHmm");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        int currentTime = Integer.parseInt(dateFormat.format(c.getTime()));
        if (editableAttrEndTime != null) {
            if (currentTime < Integer.parseInt(editableAttrEndTime)
                    && currentTime >= Integer.parseInt(editableAttrStartTime)) {
                isActive = true;
            }
        }
        return isActive;
    }

    private boolean validateNumInput(Event e) {

        boolean result = true;
        String string = e.text;
        char[] chars = new char[string.length()];
        string.getChars(0, chars.length, chars, 0);
        for (int i = 0; i < chars.length; i++) {
            if (!('0' <= chars[i] && chars[i] <= '9')) {
                result = false;
            }
        }
        return result;
    }

    private void qualityCheckForLevelInfo(String levelInfo) {
        if (!levelInfo.isEmpty()) {
            int levelVal = Integer.parseInt(levelInfo);
            if (levelVal > MAX_FLIGHT_LEVEL) {
                String inValid = "Upper Limit for the Flight Level is 600. Value entered is "
                        + levelVal;
                if (!StringUtils.isEmpty(inValid)) {
                    (new SigmetAttrValidateDlg(getShell(), inValid)).open();
                }
            }
        }
    }

    public void copyEditableAttrToSigmet(Sigmet ba) {
        Field[] ff = this.getClass().getDeclaredFields();
        for (Field f : ff) {
            try {
                if (f.getName().contains("editableAttr")) {
                    BeanUtils.copyProperty(ba, f.getName(), f.get(this));
                }
            } catch (Exception e) {
                statusHandler.debug(e.getLocalizedMessage(), e);
            }
        }
        ba.setType(this.getLineType());
        ba.setWidth(this.getWidth());
        ba.setEditableAttrFromLine(this.latLonFormatFlagAndText);// 20091203

        ba.setColors(this.getColors());// 20100115
        // this.okPressed();//20100115

        copiedToSigmet = true;
    }

    public String getEditableAttrIssueOffice() {
        return (editableAttrIssueOffice == null
                || editableAttrIssueOffice.length() == 0)
                        ? SigmetInfo.AREA_MAP.get(SigmetInfo.SIGMET_TYPES[0])[0]
                        : editableAttrIssueOffice;
    }

    public void setEditableAttrIssueOffice(String editableAttrIssueOffice) {
        this.editableAttrIssueOffice = editableAttrIssueOffice;
    }

    public String getEditableAttrArea() {
        return (editableAttrArea == null || editableAttrArea.length() == 0)
                ? SigmetInfo.AREA_MAP.get(SigmetInfo.SIGMET_TYPES[0])[0]
                : editableAttrArea;
    }

    public void setEditableAttrArea(String editableAttrArea) {
        this.editableAttrArea = editableAttrArea;
    }

    public String getEditableAttrStatus() {
        return editableAttrStatus;
    }

    public void setEditableAttrStatus(String editableAttrStatus) {
        this.editableAttrStatus = editableAttrStatus;
    }

    public String getEditableAttrId() {
        return (editableAttrId == null || editableAttrId.length() == 0)
                ? SigmetInfo.ID_MAP.get(SigmetInfo.SIGMET_TYPES[0])[0]
                : editableAttrId;
    }

    public void setEditableAttrId(String editableAttrId) {
        this.editableAttrId = editableAttrId;
    }

    public String getEditableAttrSeqNum() {
        return (editableAttrSeqNum == null || editableAttrSeqNum.length() == 0)
                ? "1" : editableAttrSeqNum;
    }

    public void setEditableAttrSeqNum(String editableAttrSeqNum) {
        this.editableAttrSeqNum = editableAttrSeqNum;
    }

    public String getEditableAttrStartTime() {
        return editableAttrStartTime;
    }

    public void setEditableAttrStartTime(String editableAttrStartTime) {
        this.editableAttrStartTime = editableAttrStartTime;
        ((Sigmet) this.getSigmet())
                .setEditableAttrStartTime(editableAttrStartTime);
    }

    public String getEditableAttrEndTime() {
        return editableAttrEndTime;
    }

    public void setEditableAttrEndTime(String editableAttrEndTime) {
        this.editableAttrEndTime = editableAttrEndTime;
        ((Sigmet) this.getSigmet()).setEditableAttrEndTime(editableAttrEndTime);
    }

    public String getEditableAttrRemarks() {
        return editableAttrRemarks;
    }

    public void setEditableAttrRemarks(String editableAttrRemarks) {
        this.editableAttrRemarks = editableAttrRemarks;
    }

    public String getEditableAttrPhenom() {
        return editableAttrPhenom;
    }

    public void setEditableAttrPhenom(String editableAttrPhenom) {
        this.editableAttrPhenom = editableAttrPhenom;
        ((Sigmet) this.getSigmet()).setEditableAttrPhenom(editableAttrPhenom);
    }

    public String getEditableAttrPhenom2() {
        return editableAttrPhenom2;
    }

    public void setEditableAttrPhenom2(String editableAttrPhenom2) {
        this.editableAttrPhenom2 = editableAttrPhenom2;
    }

    public String getEditableAttrPhenomName() {
        return editableAttrPhenomName;
    }

    public void setEditableAttrPhenomName(String editableAttrPhenomName) {
        this.editableAttrPhenomName = editableAttrPhenomName;
    }

    public String getEditableAttrPhenomLat() {
        return editableAttrPhenomLat;
    }

    public void setEditableAttrPhenomLat(String editableAttrPhenomLat) {
        this.editableAttrPhenomLat = editableAttrPhenomLat;
    }

    public String getEditableAttrPhenomLon() {
        return editableAttrPhenomLon;
    }

    public void setEditableAttrPhenomLon(String editableAttrPhenomLon) {
        this.editableAttrPhenomLon = editableAttrPhenomLon;
    }

    public String getEditableAttrPhenomPressure() {
        return editableAttrPhenomPressure;
    }

    public void setEditableAttrPhenomPressure(
            String editableAttrPhenomPressure) {
        this.editableAttrPhenomPressure = editableAttrPhenomPressure;
    }

    public String getEditableAttrPhenomMaxWind() {
        return editableAttrPhenomMaxWind;
    }

    public void setEditableAttrPhenomMaxWind(String editableAttrPhenomMaxWind) {
        this.editableAttrPhenomMaxWind = editableAttrPhenomMaxWind;
    }

    public String getEditableAttrFreeText() {
        return editableAttrFreeText;
    }

    public void setEditableAttrFreeText(String editableAttrFreeText) {
        this.editableAttrFreeText = editableAttrFreeText;
    }

    public String getEditableAttrFcstAvail() {
        return editableAttrFcstAvail;
    }

    public void setEditableAttrFcstAvail(String editableAttrFcstAvail) {
        this.editableAttrFcstAvail = editableAttrFcstAvail;
        ((Sigmet) this.getSigmet())
                .setEditableAttrFcstAvail(editableAttrFcstAvail);
    }

    public String getEditableAttrFcstTime() {
        return editableAttrFcstTime;
    }

    public void setEditableAttrFcstTime(String editableAttrFcstTime) {
        this.editableAttrFcstTime = editableAttrFcstTime;
        ((Sigmet) this.getSigmet())
                .setEditableAttrFcstTime(editableAttrFcstTime);
    }

    public String getEditableAttrFcstCntr() {
        return editableAttrFcstCntr;
    }

    public void setEditableAttrFcstCntr(String editableAttrFcstCntr) {
        this.editableAttrFcstCntr = editableAttrFcstCntr;
    }

    public String getEditableAttrFcstPhenomLat() {
        return editableAttrFcstPhenomLat;
    }

    public void setEditableAttrFcstPhenomLat(String editableAttrFcstPhenomLat) {
        this.editableAttrFcstPhenomLat = editableAttrFcstPhenomLat;
    }

    public String getEditableAttrFcstPhenomLon() {
        return editableAttrFcstPhenomLon;
    }

    public void setEditableAttrFcstPhenomLon(String editableAttrFcstPhenomLon) {
        this.editableAttrFcstPhenomLon = editableAttrFcstPhenomLon;
    }

    public String getEditableAttrFcstVADesc() {
        return editableAttrFcstVADesc;
    }

    public void setEditableAttrFcstVADesc(String editableAttrFcstVADesc) {
        this.editableAttrFcstVADesc = editableAttrFcstVADesc;
    }

    public String getEditableAttrTrend() {
        return editableAttrTrend;
    }

    public void setEditableAttrTrend(String editableAttrTrend) {
        this.editableAttrTrend = editableAttrTrend;
    }

    public String getEditableAttrMovement() {
        return editableAttrMovement;
    }

    public void setEditableAttrMovement(String editableAttrMovement) {
        this.editableAttrMovement = editableAttrMovement;
    }

    public String getEditableAltLevelText() {
        return editableAttrAltLevelText;
    }

    public void setEditableAltLevelText(String editableAltLevelText) {
        this.editableAttrAltLevelText = editableAltLevelText;
    }

    public String getEditableAttrRALSelection() {
        return editableAttrRALSelection;
    }

    public void setEditableAttrRALSelection(String ralButtonSelection) {
        this.editableAttrRALSelection = ralButtonSelection;
    }

    public String getEditableAttrPhenomSpeed() {
        return editableAttrPhenomSpeed;
    }

    public void setEditableAttrPhenomSpeed(String editableAttrPhenomSpeed) {
        this.editableAttrPhenomSpeed = editableAttrPhenomSpeed;
    }

    public String getEditableAttrPhenomDirection() {
        return editableAttrPhenomDirection;
    }

    public void setEditableAttrPhenomDirection(
            String editableAttrPhenomDirection) {
        this.editableAttrPhenomDirection = editableAttrPhenomDirection;
    }

    public String getEditableAttrLevel() {
        return editableAttrLevel;
    }

    public void setEditableAttrLevel(String editableAttrLevel) {
        this.editableAttrLevel = editableAttrLevel;
    }

    public String getEditableAttrLevelInfo1() {
        return editableAttrLevelInfo1;
    }

    public void setEditableAttrLevelInfo1(String editableAttrLevelInfo1) {
        this.editableAttrLevelInfo1 = editableAttrLevelInfo1;
        ((Sigmet) this.getSigmet())
                .setEditableAttrLevelInfo1(editableAttrLevelInfo1);
    }

    public String getEditableAttrLevelInfo2() {
        return editableAttrLevelInfo2;
    }

    public void setEditableAttrLevelInfo2(String editableAttrLevelInfo2) {
        this.editableAttrLevelInfo2 = editableAttrLevelInfo2;
        ((Sigmet) this.getSigmet())
                .setEditableAttrLevelInfo2(editableAttrLevelInfo2);
    }

    public String getEditableAttrLevelText1() {
        return editableAttrLevelText1;
    }

    public void setEditableAttrLevelText1(String editableAttrLevelText1) {
        this.editableAttrLevelText1 = editableAttrLevelText1;
        ((Sigmet) this.getSigmet())
                .setEditableAttrLevelText1(editableAttrLevelText1);
    }

    public String getEditableAttrLevelText2() {
        return editableAttrLevelText2;
    }

    public void setEditableAttrLevelText2(String editableAttrLevelText2) {
        this.editableAttrLevelText2 = editableAttrLevelText2;
        ((Sigmet) this.getSigmet())
                .setEditableAttrLevelText2(editableAttrLevelText2);
    }

    public String getEditableAttrAltLevel() {
        return editableAttrAltLevel;
    }

    public void setEditableAttrAltLevel(String editableAttrLevel) {
        this.editableAttrAltLevel = editableAttrLevel;
        ((Sigmet) this.getSigmet()).setEditableAttrAltLevel(editableAttrLevel);
    }

    public String getEditableAttrAltLevelInfo1() {
        return editableAttrAltLevelInfo1;
    }

    public void setEditableAttrAltLevelInfo1(String editableAttrLevelInfo1) {
        this.editableAttrAltLevelInfo1 = editableAttrLevelInfo1;
        ((Sigmet) this.getSigmet())
                .setEditableAttrAltLevelInfo1(editableAttrLevelInfo1);
    }

    public String getEditableAttrAltLevelInfo2() {
        return editableAttrAltLevelInfo2;
    }

    public void setEditableAttrAltLevelInfo2(String editableAttrLevelInfo2) {
        this.editableAttrAltLevelInfo2 = editableAttrLevelInfo2;
        ((Sigmet) this.getSigmet())
                .setEditableAttrAltLevelInfo2(editableAttrLevelInfo2);
    }

    public String getEditableAttrAltLevelText1() {
        return editableAttrAltLevelText1;
    }

    public void setEditableAttrAltLevelText1(String editableAttrLevelText1) {
        this.editableAttrAltLevelText1 = editableAttrLevelText1;
        ((Sigmet) this.getSigmet())
                .setEditableAttrAltLevelText1(editableAttrLevelText1);
    }

    public String getEditableAttrAltLevelText2() {
        return editableAttrAltLevelText2;
    }

    public void setEditableAttrAltLevelText2(String editableAttrLevelText2) {
        this.editableAttrAltLevelText2 = editableAttrLevelText2;
        ((Sigmet) this.getSigmet())
                .setEditableAttrAltLevelText2(editableAttrLevelText2);
    }

    public String getEditableAttrFromLine() {
        return editableAttrFromLine;
    }

    public void setEditableAttrFromLine(String editableAttrFromLine) {
        this.editableAttrFromLine = editableAttrFromLine;
        ((Sigmet) this.getSigmet())
                .setEditableAttrFromLine(editableAttrFromLine);
    }

    public String getLatLonFormatFlagAndText() {
        return latLonFormatFlagAndText;
    }

    public void setLatLonFormatFlagAndText(String latLonFormatFlagAndText) {
        this.latLonFormatFlagAndText = latLonFormatFlagAndText;
    }

    public String getEditableAttrFir() {
        return editableAttrFir;
    }

    public void setEditableAttrFir(String editableAttrFir) {
        this.editableAttrFir = editableAttrFir;
        ((Sigmet) this.getSigmet()).setEditableAttrFir(editableAttrFir);
    }

    public String getEditableAttrCarSamBackupMode() {
        return editableAttrCarSamBackupMode;
    }

    public void setEditableAttrCarSamBackupMode(
            String editableAttrCarSamBackupMode) {
        this.editableAttrCarSamBackupMode = editableAttrCarSamBackupMode;
        ((Sigmet) this.getSigmet())
                .setEditableAttrCarSamBackupMode(editableAttrCarSamBackupMode);
    }

    public boolean isCarSamBackupMode() {
        return isCarSamBackup;
    }

    private void setControl(Control cont, String prop) {
        PropertyDescriptor pd;
        Method pdReadMethod;
        Method pdWriteMethod;
        String propValue;
        try {
            pd = new PropertyDescriptor(prop, this.getClass());
            pdReadMethod = pd.getReadMethod();
            pdWriteMethod = pd.getWriteMethod();

            if (pdReadMethod != null) {
                propValue = (String) pdReadMethod.invoke(this, null);

                // Text Controls NO needs ???
                if (propValue == null) {
                    if (cont instanceof Combo) {
                        Combo contCombo = (Combo) cont;
                        contCombo.select(0);
                        pdWriteMethod.invoke(this, contCombo.getText());
                    }

                    if (cont instanceof Spinner) {
                        Spinner contSpinner = (Spinner) cont;
                        contSpinner.setSelection(0);
                        pdWriteMethod.invoke(this, contSpinner.getText());
                    }
                } else {
                    if (cont instanceof Combo) {
                        Combo c = (Combo) cont;
                        c.setText(propValue);

                        if (c.getText().contains("CYCLONE")) {
                            tropCycFlag = true;
                        }
                    }
                    if (cont instanceof Text) {
                        ((Text) cont).setText(propValue);
                    }
                    if (cont instanceof Spinner) {
                        ((Spinner) cont)
                                .setSelection(Integer.parseInt(propValue));
                    }
                }
            }

        } catch (Exception e) {
            statusHandler.debug("--- inside setControl(): " + e.getMessage(),
                    e);
        }
    }

    private class SigmetAttrValidateDlg extends AttrDlg {

        private Text txtError;

        private final String errorInfo;

        SigmetAttrValidateDlg(Shell parShell, String error) {
            super(parShell);
            this.errorInfo = error;
        }

        @Override
        public Control createDialogArea(Composite parent) {
            Composite top = (Composite) super.createDialogArea(parent);
            new Label(top, SWT.LEFT)
                    .setText("Please correct the following errors");

            GridLayout mainLayout = new GridLayout(1, false);
            mainLayout.marginHeight = 3;
            mainLayout.marginWidth = 3;
            top.setLayout(mainLayout);

            this.getShell().setText("Invalid SIGMET Entries");

            txtError = new Text(top,
                    SWT.MULTI | SWT.BORDER | SWT.READ_ONLY | SWT.WRAP);
            txtError.setText(errorInfo);

            GC gc = new GC(txtError);
            int charWidth = gc.getFontMetrics().getAverageCharWidth();
            int charHeight = txtError.getLineHeight();
            Rectangle size = txtError.computeTrim(0, 0, charWidth * 70,
                    charHeight * 20);
            gc.dispose();
            txtError.setLayoutData(GridDataFactory.defaultsFor(txtError)
                    .span(3, 1).hint(size.width, size.height).create());

            return top;
        }

        @Override
        public void createButtonsForButtonBar(Composite parent) {
            createButton(parent, IDialogConstants.OK_ID, "Close", true);

        }

        @Override
        public void enableButtons() {
            this.getButton(IDialogConstants.OK_ID).setEnabled(true);
        }

        // listener function of Save Button
        @Override
        public void okPressed() {
            setReturnCode(OK);
            close();

        }

        @Override
        public void handleShellCloseEvent() {
            close();
        }

        @Override
        public void setAttrForDlg(IAttribute ia) {
        }

    }

    private class SigmetAttrDlgSaveMsgDlg extends AttrDlg {

        private Text txtInfo;

        private Text txtSave;

        private boolean cnlSigmet = false;

        SigmetAttrDlgSaveMsgDlg(Shell parShell) {
            super(parShell);
            if (STATUS_CANCEL
                    .equals(SigmetAttrDlg.this.getEditableAttrStatus())) {
                cnlSigmet = true;
            }
        }

        @Override
        public Control createDialogArea(Composite parent) {
            Composite top = (Composite) super.createDialogArea(parent);

            GridLayout mainLayout = new GridLayout(3, false);
            mainLayout.marginHeight = 3;
            mainLayout.marginWidth = 3;
            top.setLayout(mainLayout);

            this.getShell().setText("SIGMET Save");

            txtInfo = new Text(top,
                    SWT.MULTI | SWT.BORDER | SWT.READ_ONLY | SWT.WRAP);
            txtInfo.setText(getFileContent());

            GC gc = new GC(txtInfo);
            int charWidth = gc.getFontMetrics().getAverageCharWidth();
            int charHeight = txtInfo.getLineHeight();
            Rectangle size = txtInfo.computeTrim(0, 0, charWidth * 70,
                    charHeight * 20);
            gc.dispose();
            txtInfo.setLayoutData(GridDataFactory.defaultsFor(txtInfo)
                    .span(3, 1).hint(size.width, size.height).create());

            txtSave = new Text(top, SWT.BORDER | SWT.READ_ONLY);
            txtSave.setLayoutData(
                    new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
            txtSave.setText(getFileName());

            return top;
        }

        @Override
        public void createButtonsForButtonBar(Composite parent) {
            createButton(parent, IDialogConstants.OK_ID, "Save", true);
            createButton(parent, IDialogConstants.CANCEL_ID,
                    IDialogConstants.CANCEL_LABEL, false);
        }

        @Override
        public void enableButtons() {
            this.getButton(IDialogConstants.CANCEL_ID).setEnabled(true);
            this.getButton(IDialogConstants.OK_ID).setEnabled(true);
        }

        @Override
        public void handleShellCloseEvent() {
            close();
        }

        @Override
        public void cancelPressed() {
            setReturnCode(CANCEL);
            close();
        }

        // listener function of Save Button
        @Override
        public void okPressed() {
            String dataURI = storeActivity();

            if (dataURI != null) {
                try {
                    StorageUtils.storeDerivedProduct(dataURI, txtSave.getText(),
                            "TEXT", txtInfo.getText());
                } catch (PgenStorageException e) {
                    StorageUtils.showError(e);
                }
            }

            setReturnCode(OK);
            close();
            SigmetAttrDlg.this.drawingLayer.removeSelected();
            SigmetAttrDlg.this.close();
            PgenUtil.setSelectingMode();

        }

        private String storeActivity() {
            String dataURI;

            Layer defaultLayer = new Layer();
            defaultLayer.addElement(
                    SigmetAttrDlg.this.drawingLayer.getSelectedDE());
            ArrayList<Layer> layerList = new ArrayList<>();
            layerList.add(defaultLayer);

            String forecaster = System.getProperty("user.name");
            ProductTime refTime = new ProductTime();

            // Use (hardcode) pgenType as the name and type of a new Product.
            Product defaultProduct = new Product(SigmetAttrDlg.this.pgenType,
                    SigmetAttrDlg.this.pgenType, forecaster, null, refTime,
                    layerList);

            String plabel = SigmetAttrDlg.this.drawingLayer.getActiveProduct()
                    .getOutputFile();
            if (plabel == null) {
                plabel = SigmetAttrDlg.this.drawingLayer
                        .buildActivityLabel(defaultProduct);
            }

            /*
             * Construct a new label name and activity xml filename by using (1)
             * pgenType as the prefix, and (2) its tag name inserted before the
             * filename extension "xml" with a dot connecting each field in the
             * filename, e.g., "INTL_SIGMET.07012020.10.KKCI_BRAVO_5.xml".
             */
            String prefix = SigmetAttrDlg.this.pgenType.replaceAll("\\s", "");
            String fromFileName = getFileName();
            String tagName = fromFileName.substring(0,
                    fromFileName.indexOf('.'));
            int insertionPoint = plabel.lastIndexOf('.');
            String filename = prefix
                    + plabel.substring(plabel.indexOf('.'), insertionPoint + 1)
                    + tagName + plabel.substring(insertionPoint);

            defaultProduct.setOutputFile(filename);

            defaultProduct.setCenter(PgenUtil.getCurrentOffice());

            try {
                dataURI = StorageUtils.storeProduct(defaultProduct);
            } catch (PgenStorageException e) {
                StorageUtils.showError(e);
                return null;
            }
            return dataURI;
        }

        private String getFileContent() {
            StringBuilder sb = new StringBuilder();

            // Check Fir to see it belong to CAR/SAM
            boolean isCarSamFir = false;
            if (SigmetAttrDlg.this.getFirs() != null) {
                for (CarSamBackupWmoHeader carsamWmo : SigmetInfo.awcBackupCarSamWmoHeaders
                        .getCarSamBackupWmoHeader()) {
                    if ((SigmetAttrDlg.this.getFirs())
                            .contains(carsamWmo.getFirID())) {
                        isCarSamFir = true;
                        break;
                    }
                }
            }

            // C code: @4146, @5377
            if (SigmetInfo.getAFOSflg()) {
                sb.append("ZCZC ");
                sb.append(getIdnode());
                sb.append(getAfospil());
                sb.append("\n").append(getWmo());
                sb.append("\n").append(getFirstLine());
                sb.append("\n").append(getSecondLine());
                sb.append("\n").append("NNNN");
                sb.append("\n");
            } else {
                if (isCarSamFir && (SigmetAttrDlg.this.isCarSamBackup)) {
                    // CARSAM Fir and it is in backup mode; WMO header but NO
                    // AWIPS PILL
                    sb.append(getWmoForCarSamBackup());
                } else {
                    // Not CARSAM Backup Mode; Both WMO header and AWIPS PILL
                    sb.append(getWmo());
                    sb.append("\n").append(getAfospil());
                }
                sb.append("\n").append(getFirstLine());
                sb.append("\n").append(getSecondLine());
            }
            return sb.toString();
        }

        private String getFileName() {
            StringBuilder sb = new StringBuilder();
            sb.append(SigmetAttrDlg.this.getEditableAttrArea());
            sb.append("_").append(SigmetAttrDlg.this.getEditableAttrId());

            if (cnlSigmet) {
                sb.append("_").append(sigmetCnlDlg.getSeriesNumber());
            } else {
                sb.append("_")
                        .append(SigmetAttrDlg.this.getEditableAttrSeqNum());
            }

            sb.append(".sigintl");
            return sb.toString();
        }

        private String getWmoForCarSamBackup() {
            StringBuilder sb = new StringBuilder();
            String firStr = SigmetAttrDlg.this.getFirs();
            String phen = SigmetAttrDlg.this.getEditableAttrPhenom();

            for (CarSamBackupWmoHeader carsamWmo : SigmetInfo.awcBackupCarSamWmoHeaders
                    .getCarSamBackupWmoHeader()) {
                if (firStr.contains(carsamWmo.getFirID())) {
                    if (PgenConstant.TYPE_VOLCANIC_ASH.equals(phen)) {
                        sb.append(carsamWmo.getWmoHeaderForVA());
                    } else if (PgenConstant.TYPE_TROPICAL_CYCLONE
                            .equals(phen)) {
                        sb.append(carsamWmo.getWmoHeaderForTC());
                    } else {
                        sb.append(carsamWmo.getWmoHeaderForOther());
                    }
                    sb.append(" ").append(carsamWmo.getWmoID()).append(" ");
                    break;
                }
            }
            sb.append(getTimeStringPlusHourInHMS(0));
            return sb.toString();
        }

        private String getWmo() {
            StringBuilder sb = new StringBuilder();
            sb.append("W");
            sb.append(getWmoPhen());
            sb.append(getOcnWmoAwpHeaders()[1]);
            sb.append(getInum());
            sb.append(" ").append(SigmetAttrDlg.this.getEditableAttrArea());
            sb.append(" ").append(getTimeStringPlusHourInHMS(0));

            return sb.toString();
        }

        private String getAfospil() {
            StringBuilder sb = new StringBuilder();

            if ("PAWU".equals(SigmetAttrDlg.this.getEditableAttrArea())) {
                sb.append(getAwpPhen());
                sb.append(getOcnWmoAwpHeaders()[2]);
                sb.append(getInum());
                sb.append("\n").append(getIdnode());
                sb.append(SigmetAttrDlg.this.getEditableAttrId().charAt(0));
                sb.append(" WS ").append(getTimeStringPlusHourInHMS(0));
            } else {
                sb.append(getAwpPhen());
                sb.append(getOcnWmoAwpHeaders()[2]);
                sb.append(
                        SigmetAttrDlg.this.getEditableAttrId().substring(0, 1));
            }

            return sb.toString();
        }

        private String getFirstLine() {
            boolean csIntlSigm = false;
            StringBuilder sb = new StringBuilder();
            String startTime = getTimeStringPlusHourInHMS(0);
            String endTime = getTimeStringPlusHourInHMS(4);

            sb.append(SigmetAttrDlg.this.getFirs());
            sb.append(" ").append(SigmetConstant.SIGMET);

            for (String s : SigmetInfo.CAR_SAM_WMO) {
                if (s.contains(SigmetAttrDlg.this.getEditableAttrArea())) {
                    csIntlSigm = true;
                    break;
                }
            }
            if (csIntlSigm) {
                if ((SigmetConstant.ZULU
                        .contains(SigmetAttrDlg.this.getEditableAttrId()))) {
                    sb.append(" ");
                } else {
                    sb.append(" ").append(SigmetAttrDlg.this.getEditableAttrId()
                            .substring(0, 1));
                }
            } else {
                sb.append(" ").append(SigmetAttrDlg.this.getEditableAttrId());
            }

            if (cnlSigmet) {
                sb.append(" ").append(sigmetCnlDlg.getSeriesNumber());
            } else if (csIntlSigm) {
                sb.append(SigmetAttrDlg.this.getEditableAttrSeqNum());
            } else {
                sb.append(" ")
                        .append(SigmetAttrDlg.this.getEditableAttrSeqNum());
            }

            sb.append(" ").append(SigmetConstant.VALID);
            if (cnlSigmet) {
                sb.append(" ").append(getTimeStringPlusHourInHMS(0));
            } else {
                sb.append(" ").append(
                        SigmetAttrDlg.this.getEditableAttrStartTime() == null
                                ? startTime
                                : SigmetAttrDlg.this
                                        .getEditableAttrStartTime());
            }
            sb.append("/")
                    .append(SigmetAttrDlg.this.getEditableAttrEndTime() == null
                            ? endTime
                            : SigmetAttrDlg.this.getEditableAttrEndTime());
            sb.append(" ")
                    .append(SigmetAttrDlg.this.getEditableAttrIssueOffice())
                    .append("-");

            return sb.toString();
        }

        private String getSecondLine() {
            StringBuilder sb = new StringBuilder();
            // in C code: entvname,tc
            boolean isPhenNameEntered = false;
            boolean isTropCyc = false;

            String phen = SigmetAttrDlg.this.getEditableAttrPhenom();
            String phenName = SigmetAttrDlg.this.getEditableAttrPhenomName();
            phenName = phenName == null ? phenName : phenName.toUpperCase();
            if (PgenConstant.TYPE_VOLCANIC_ASH.equals(phen)) {
                isPhenNameEntered = SigmetInfo.isVolcanoNameEntered(phenName);
            }

            // ---------------------FIR

            firCalledForSecondLine = true;
            String id = SigmetAttrDlg.this.getFirs();
            String firName = "";
            for (String s : SigmetInfo.FIR_ARRAY) {
                if (id.contains(s.substring(0, 4))) {
                    firName = s.substring(5, s.length());
                    sb.append(firName.replace('_', ' ')).append(" ")
                            .append(SigmetConstant.FIR).append(" ");
                }
            }

            firCalledForSecondLine = false;

            // ---------------------CANCEL
            if (cnlSigmet) {
                sb.append(SigmetConstant.CNL);
                sb.append(" ").append(SigmetConstant.SIGMET);
                sb.append(" ").append(SigmetAttrDlg.this.getEditableAttrId());
                sb.append(" ")
                        .append(SigmetAttrDlg.this.getEditableAttrSeqNum());
                sb.append(" ")
                        .append(SigmetAttrDlg.this.getEditableAttrStartTime());
                sb.append("/")
                        .append(SigmetAttrDlg.this.getEditableAttrEndTime());
                sb.append(".");
                return sb.toString();
            }

            // ---------------------phenomnon

            if (!(PgenConstant.TYPE_TROPICAL_CYCLONE.equals(phen))) {
                String pString = phen == null
                        ? SigmetInfo.PHEN_MAP.get(SigmetInfo.SIGMET_TYPES[0])[0]
                        : phen;
                if (PgenConstant.TYPE_VOLCANIC_ASH.equals(phen)) {
                    sb.append(SigmetConstant.VA_ERUPTION).append(" ");
                } else {
                    SigmetAttrDlg.this.setEditableAttrPhenom(pString);
                    sb.append(pString.replace('_', ' '));
                }
            } else {
                isTropCyc = true;
                isPhenNameEntered = true;

                sb.append(SigmetConstant.TC);

                if (phenName != null) {
                    sb.append(" ").append(phenName.trim());
                }

                sb.append(" ").append(SigmetConstant.OBS_AT).append(" ");
                // C code: loctim/local time
                sb.append(getTimeStringPlusHourInHMS(0).substring(2, 6));
                sb.append(SigmetConstant.Z);

                if (SigmetAttrDlg.this.getEditableAttrPhenomLat() != null
                        && SigmetAttrDlg.this
                                .getEditableAttrPhenomLon() != null) {
                    sb.append(" ").append(SigmetConstant.NR);
                    sb.append(" ").append(
                            SigmetAttrDlg.this.getEditableAttrPhenomLat());
                    sb.append(" ").append(
                            SigmetAttrDlg.this.getEditableAttrPhenomLon());
                    sb.append(".");
                }

                String presHPA = SigmetAttrDlg.this
                        .getEditableAttrPhenomPressure();
                if (presHPA != null) {
                    sb.append(" ").append(presHPA.trim())
                            .append(SigmetConstant.HPA).append(".");
                }

                // --------------- max winds

                String maxWinds = SigmetAttrDlg.this
                        .getEditableAttrPhenomMaxWind();
                if (maxWinds != null && !"".equals(maxWinds.trim())) {
                    sb.append(" ").append(SigmetConstant.MAX_WINDS).append(" ");
                    sb.append(maxWinds).append(SigmetConstant.KT).append(".");
                }

                // --------------- movement

                String movement = SigmetAttrDlg.this.getEditableAttrMovement();

                if (movement == null) {
                    movement = STNRY;
                }

                if (STNRY.equals(movement)) {
                    sb.append(" ").append(SigmetConstant.STNR).append(".");
                } else if (SigmetConstant.MVG.equals(movement)) {
                    sb.append(" ").append(SigmetConstant.MOV);
                    sb.append(" ").append(SigmetAttrDlg.this
                            .getEditableAttrPhenomDirection());
                    sb.append(" ").append(
                            SigmetAttrDlg.this.getEditableAttrPhenomSpeed());
                    sb.append(SigmetConstant.KT).append(".");
                }

                // ---------------- trend

                String trend = getEditableAttrTrend();
                if (!NONE.equals(trend)) {
                    sb.append(" ").append(trend).append(".");
                }

                // ---------------- second phenom

                String phen2 = SigmetAttrDlg.this.getEditableAttrPhenom2();
                if (phen2 != null && !"".equals(phen2.trim())) {
                    sb.append(" ").append(phen2.replace('_', ' '));
                }
            }

            // ------------ VOLCANIC_ASH
            String altLevelInfo = null;
            if (PgenConstant.TYPE_VOLCANIC_ASH.equals(phen)) {
                // phenName in C code: volcn
                sb.append(phenName == null ? "" : phenName);
                sb.append(" ").append(SigmetConstant.VA_CLD);
                // phenlat,phenlon in C code
                String phenLat = SigmetAttrDlg.this.getEditableAttrPhenomLat();
                String phenLon = SigmetAttrDlg.this.getEditableAttrPhenomLon();

                if (isPhenNameEntered && phenLat != null
                        && !"".equals(phenLat.trim()) && phenLon != null
                        && !"".equals(phenLon.trim())) {

                    int lat = getIntValueOfLat(phenLat);
                    int lon = getIntValueOfLon(phenLon);
                    lat = getValRoundedToNearest15Min(lat);
                    lon = getValRoundedToNearest15Min(lon);

                    sb.append(" ").append(SigmetConstant.PSN).append(" ");
                    sb.append(phenLat.substring(0, phenLat.length() - 4));
                    sb.append(Integer.toString(lat));
                    sb.append(" ")
                            .append(phenLon.substring(0, phenLon.length() - 5));
                    String strLon = Integer.toString(lon);
                    if ((strLon.length()) < 5) {
                        sb.append("0").append(strLon);
                    } else {
                        sb.append(strLon);
                    }
                    altLevelInfo = getEditableAttrAltLevel();
                }
            }

            // ----------------tops

            String tops = getEditableAttrLevel();
            if (tops != null && tops.contains(SigmetConstant.FCST)) {
                if (PgenConstant.TYPE_VOLCANIC_ASH.equals(phen)) {
                    sb.append(" ").append(SigmetConstant.FCST);
                } else {
                    sb.append(NONE.equals(tops) ? "" : " " + tops);
                }
            }

            String lineType = ((Sigmet) SigmetAttrDlg.this.drawingLayer
                    .getSelectedDE()).getType();
            // from_line without format in C
            String fromLineWithFormat = SigmetAttrDlg.this
                    .getLatLonFormatFlagAndText();
            String[] lineArray = fromLineWithFormat
                    .split(SigmetInfo.LINE_SEPERATER);

            String locationDesc = "";
            if (fromLineWithFormat.contains(SigmetConstant.VOR)) {
                StringBuilder vorLocation = new StringBuilder();
                for (int i = 0; i < lineArray.length - 1; i++) {
                    vorLocation.append(" ").append(lineArray[i]);
                }
                locationDesc = vorLocation.toString();
            } else {
                StringBuilder latLonLocation = new StringBuilder();
                String lineInfo = lineArray[0];
                String[] locPair = lineInfo.split("-");

                for (int i = 0; i < locPair.length; i++) {
                    String locTemp = locPair[i];
                    locTemp = locTemp.trim();
                    String[] latlonPair = locTemp.split(" ");
                    if (latlonPair.length > 1) {
                        String lat = latlonPair[0];
                        String lon = latlonPair[1];

                        int latval = getIntValueOfLat(lat);
                        int lonval = getIntValueOfLon(lon);
                        latval = getValRoundedToNearest15Min(latval);
                        lonval = getValRoundedToNearest15Min(lonval);
                        latLonLocation.append((i > 0) ? " " : "")
                                .append(lat.substring(0, lat.length() - 4));
                        latLonLocation.append(Integer.toString(latval))
                                .append(" ");
                        latLonLocation
                                .append(lon.substring(0, lon.length() - 5));
                        String strLon = Integer.toString(lonval);
                        if ((strLon.length()) < 5) {
                            latLonLocation.append("0").append(strLon);
                        } else {
                            latLonLocation.append(strLon);
                        }

                        if (i < locPair.length - 1) {
                            latLonLocation.append(" ").append("-");
                        }
                    }
                }
                locationDesc = latLonLocation.toString();
            }

            // in C: switch(_subType)nmap_pgsigw.c@4008
            if (SigmetAttrDlg.ISOLATED.equals(lineType)) {
                if (isTropCyc) {
                    sb.append(" ").append(SigmetConstant.WITHIN).append(" ");
                    sb.append((int) SigmetAttrDlg.this.getWidth());
                    sb.append(" ").append(SigmetConstant.NM_CENTER).append(".");
                } else {
                    sb.append(" ").append(SigmetConstant.WI).append(" ");
                    sb.append((int) SigmetAttrDlg.this.getWidth());
                    sb.append(" ").append(SigmetConstant.NM_OF).append(" ");

                    sb.append(locationDesc).append(".");
                }
            } else if (SigmetAttrDlg.AREA.equals(lineType)) {
                if (sb.toString().contains(SigmetConstant.TS)) {
                    if (isTropCyc) {

                        // ------ TOPS
                        if (tops != null && (!NONE.equals(tops))) {
                            String text1 = SigmetAttrDlg.this
                                    .getEditableAttrLevelText1();
                            if (text1 != null && text1.length() > 0) {
                                if (SigmetConstant.TOPS.equals(tops)) {
                                    sb.append(" ").append(SigmetConstant.TOP);
                                }
                                if ((SigmetAttrDlg.this
                                        .getEditableAttrLevelInfo1()
                                        .equalsIgnoreCase(SigmetConstant.ABV))
                                        || (SigmetAttrDlg.this
                                                .getEditableAttrLevelInfo1()
                                                .equalsIgnoreCase(
                                                        SigmetConstant.BLW))) {
                                    sb.append(" ").append(SigmetAttrDlg.this
                                            .getEditableAttrLevelInfo1());
                                }
                                sb.append(" ").append(SigmetConstant.FL);
                                sb.append(text1 == null ? "" : text1);

                                String levelInfo2 = SigmetAttrDlg.this
                                        .getEditableAttrLevelInfo2();
                                if (!NONE.equals(levelInfo2)) {
                                    sb.append("/");
                                    String text2 = SigmetAttrDlg.this
                                            .getEditableAttrLevelText2();
                                    sb.append(text2 == null ? "" : text2);
                                }
                            }
                        }

                    } else {
                        sb.append(" ").append(SigmetConstant.OBS_AT).append(" ")
                                .append(getTimeStringPlusHourInHMS(0)
                                        .substring(2, 6))
                                .append(SigmetConstant.Z);
                    }
                    sb.append(" ").append(SigmetConstant.WI).append(" ");
                } else {
                    if (!fromLineWithFormat.contains(SigmetConstant.VOR)) {
                        sb.append(" ").append(SigmetConstant.WI).append(" ");
                    } else {
                        sb.append(" ").append(
                                SigmetConstant.WI_AREA_BOUNDED_BY_LINE_FM);
                    }
                }
                sb.append(locationDesc).append(".");
            } else {
                // line with LINE_SEPERATER
                sb.append(" ").append(SigmetConstant.WI).append(" ");
                sb.append((int) SigmetAttrDlg.this.getWidth());
                sb.append(" ").append(SigmetConstant.NM).append(" ");
                sb.append(getLineTypeForSOL(lineType));

                if (!fromLineWithFormat.contains(SigmetConstant.VOR)) {
                    sb.append(" ").append(SigmetConstant.LINE);
                } else {
                    sb.append(" ").append(SigmetConstant.LINE_FM);
                }
                sb.append(locationDesc).append(".");
            }

            // in C: if( ! tc )nmap_pgsigw.c@4062
            if (!isTropCyc) {

                // ------ TOPS
                if (tops != null && (!NONE.equals(tops))) {
                    if (SigmetConstant.TOPS.equals(tops)) {
                        sb.append(" ").append(SigmetConstant.TOP);
                    }
                    String levelInfo1 = SigmetAttrDlg.this
                            .getEditableAttrLevelText1();
                    if (levelInfo1 != null) {
                        if ((SigmetAttrDlg.this.getEditableAttrLevelInfo1()
                                .equalsIgnoreCase(SigmetConstant.ABV))
                                || (SigmetAttrDlg.this
                                        .getEditableAttrLevelInfo1()
                                        .equalsIgnoreCase(
                                                SigmetConstant.BLW))) {
                            sb.append(" ").append(SigmetAttrDlg.this
                                    .getEditableAttrLevelInfo1());
                        }
                    }
                    sb.append(" ").append(SigmetConstant.FL);
                    String text1 = SigmetAttrDlg.this
                            .getEditableAttrLevelText1();
                    sb.append(text1 == null ? "" : text1);

                    String levelInfo2 = SigmetAttrDlg.this
                            .getEditableAttrLevelInfo2();
                    if (!NONE.equals(levelInfo2)) {
                        sb.append("/");
                        String text2 = SigmetAttrDlg.this
                                .getEditableAttrLevelText2();
                        sb.append(text2 == null ? "" : text2);

                    }
                    sb.append(".");
                }

                // ------ movement
                String movement = SigmetAttrDlg.this.getEditableAttrMovement();
                if (STNRY.equals(movement) || movement == null) {
                    sb.append(" ").append(SigmetConstant.STNR).append(".");
                } else if (SigmetConstant.MVG.equals(movement)) {
                    sb.append(" ").append(SigmetConstant.MOV);
                    sb.append(" ").append(SigmetAttrDlg.this
                            .getEditableAttrPhenomDirection());
                    sb.append(" ").append(
                            SigmetAttrDlg.this.getEditableAttrPhenomSpeed());
                    sb.append(SigmetConstant.KT).append(". ");
                }

                // ------ trend
                String trend = SigmetAttrDlg.this.getEditableAttrTrend();
                if (!NONE.equals(trend) && trend != null) {
                    sb.append(" ").append(trend).append(".");
                }
            }

            // ------ remarks
            String remarks = SigmetAttrDlg.this.getEditableAttrRemarks();
            if (!NONE.equals(remarks) && remarks != null) {
                sb.append(" ").append(remarks).append(".");
            }

            // ------ outlook if volcano ash
            if (PgenConstant.TYPE_VOLCANIC_ASH.equals(phen)) {
                sb.append("\n").append(SigmetConstant.FCST).append(" ");
                if (SigmetConstant.TRUE.equals(
                        SigmetAttrDlg.this.getEditableAttrFcstAvail())) {
                    sb.append(SigmetAttrDlg.this.getEditableAttrFcstTime())
                            .append("Z");
                    sb.append(getAltLevelInfo(altLevelInfo).toString());
                    String ral = SigmetAttrDlg.this
                            .getEditableAttrRALSelection();
                    if (ral != null) {
                        sb.append(" ").append(SigmetConstant.VA_CLD_WI)
                                .append(" ");

                        if (LINE.equals(ral)) {
                            sb.append(SigmetConstant.EITHER_SIDE_OF_LINE)
                                    .append(" ");
                        } else if (RADIUS.equals(ral)) {
                            sb.append(SigmetConstant.OF).append(" ");
                        } else if (AREA.equals(ral)) {
                            sb.append(SigmetConstant.AREA_BOUNDED_BY)
                                    .append(" ");
                        }
                    }

                    if (SigmetAttrDlg.this
                            .getEditableAttrFcstVADesc() != null) {
                        StringBuilder fcstLatLonLoc = new StringBuilder();
                        String lineInfo = SigmetAttrDlg.this
                                .getEditableAttrFcstVADesc();
                        fcstLatLonLoc = getfcstLatLonLoc(lineInfo);
                        sb.append(fcstLatLonLoc.toString());
                    }

                    sb.append(". ");

                    if (SigmetAttrDlg.this
                            .getEditableAttrAltitudeSelection() != null) {
                        if ((SigmetConstant.ABV.equals(SigmetAttrDlg.this
                                .getEditableAttrAltitudeSelection()))
                                || (SigmetConstant.BLW.equals(SigmetAttrDlg.this
                                        .getEditableAttrAltitudeSelection()))) {
                            sb.append(" ").append(SigmetAttrDlg.this
                                    .getEditableAttrAltitudeSelection());
                        }

                        sb.append(" ").append(SigmetConstant.FL);
                        String text1 = SigmetAttrDlg.this
                                .getEditableAltLevelText();
                        sb.append(text1 == null ? "" : text1);
                        sb.append(".");
                    }

                } else {
                    sb.append("NA").append(".");
                }
            }

            // ------ outlook if tropical cyclone
            if (PgenConstant.TYPE_TROPICAL_CYCLONE.equals(phen)) {
                sb.append("\n").append(SigmetConstant.FCST).append(" ");
                if (SigmetConstant.TRUE.equals(
                        SigmetAttrDlg.this.getEditableAttrFcstAvail())) {

                    sb.append(SigmetAttrDlg.this.getEditableAttrFcstTime())
                            .append(SigmetConstant.Z).append(" ");
                    sb.append(SigmetConstant.TC_CENTER);
                    sb.append(" " + SigmetAttrDlg.this.getEditableAttrFcstCntr()
                            .toUpperCase());
                    if (!StringUtils.isEmpty(SigmetAttrDlg.this
                            .getEditableAttrFcstPhenomLat())) {
                        sb.append(" " + SigmetAttrDlg.this
                                .getEditableAttrFcstPhenomLat().toUpperCase());
                    }
                    if (!StringUtils.isEmpty(SigmetAttrDlg.this
                            .getEditableAttrFcstPhenomLon())) {
                        sb.append(" " + SigmetAttrDlg.this
                                .getEditableAttrFcstPhenomLon().toUpperCase());
                    }
                    sb.append(".");
                } else {
                    sb.append(" ").append("NA").append(".");
                }
            }

            // ------ CANCEL Correction Info
            if (cnlSigmet) {
                if (sigmetCnlDlg.getCorrInfo() != null) {
                    sb.append("\n");
                    sb.append("W");
                    sb.append(getWmoPhen());
                    sb.append(getOcnWmoAwpHeaders()[1]);
                    sb.append(getInum());
                    sb.append(" ").append(sigmetCnlDlg.getCorrInfo());
                }
            }

            // ------ Free Text
            String freeText = SigmetAttrDlg.this.getEditableAttrFreeText();
            if (freeText != null && freeText.length() > 0) {
                sb.append("\n").append(freeText.toUpperCase());
            }

            return sb.toString();
        }

                String text1 = SigmetAttrDlg.this.getEditableAttrLevelText1();
                if (text1 != null && text1.length() > 0) {
            } else {
                levelTxt = null;
            }

        private StringBuilder getAltLevelInfo(String tops) {
            StringBuilder levelTxt = new StringBuilder();

            // ------ TOPS
            if (tops != null && (!NONE.equals(tops))) {
                String text1 = SigmetAttrDlg.this
                        .getEditableAttrAltLevelText1();
                if (text1 != null && text1.length() > 0) {
                    if (SigmetConstant.FCST.equals(tops)) {
                        levelTxt.append(" ").append(SigmetConstant.FCST);
                    }

                    if ((SigmetAttrDlg.this.getEditableAttrAltLevelInfo1()
                            .equalsIgnoreCase(SigmetConstant.ABV))
                            || (SigmetAttrDlg.this
                                    .getEditableAttrAltLevelInfo1()
                                    .equalsIgnoreCase(SigmetConstant.BLW))) {
                        levelTxt.append(" ").append(SigmetAttrDlg.this
                                .getEditableAttrAltLevelInfo1());
                    }

                    levelTxt.append(" ").append(SigmetConstant.FL);
                    levelTxt.append(text1 == null ? "" : text1);

                    String levelInfo2 = SigmetAttrDlg.this
                            .getEditableAttrAltLevelInfo2();
                    if (!NONE.equals(levelInfo2)) {
                        levelTxt.append("/");
                        String text2 = SigmetAttrDlg.this
                                .getEditableAttrAltLevelText2();
                        levelTxt.append(text2 == null ? "" : text2);
                    }
                }
            } else {
                levelTxt = null;
            }
            return levelTxt;
        }

        private StringBuilder getfcstLatLonLoc(String lineInfo) {
            StringBuilder fcstLatLonLoc = new StringBuilder();
            String[] locPair = lineInfo.split("-");

            for (int i = 0; i < locPair.length; i++) {
                String locTemp = locPair[i];
                locTemp = locTemp.trim();
                String[] latlonPair = locTemp.split(" ");
                if (latlonPair.length > 1) {
                    String lat = latlonPair[0];
                    String lon = latlonPair[1];

                    int latval = getIntValueOfLat(lat);
                    int lonval = getIntValueOfLon(lon);
                    latval = getValRoundedToNearest15Min(latval);
                    lonval = getValRoundedToNearest15Min(lonval);

                    fcstLatLonLoc.append(" ")
                            .append(lat.substring(0, lat.length() - 4));
                    fcstLatLonLoc.append(Integer.toString(latval)).append(" ");
                    fcstLatLonLoc.append(lon.substring(0, lon.length() - 5));
                    String strLon = Integer.toString(lonval);
                    if ((strLon.length()) < 5) {
                        fcstLatLonLoc.append("0").append(strLon);
                    } else {
                        fcstLatLonLoc.append(strLon);
                    }

                    if (i < locPair.length - 1) {
                        fcstLatLonLoc.append(" ").append("-");
                    }
                }
            }
            return fcstLatLonLoc;
        }

        private int getIntValueOfLat(String val) {
            int intVal = 0;
            String tempVal = val.substring(val.length() - 4, val.length());
            intVal = Integer.parseInt(tempVal);
            return intVal;
        }

        private int getIntValueOfLon(String val) {
            int intVal = 0;
            String tempVal = val.substring(val.length() - 5, val.length());
            intVal = Integer.parseInt(tempVal);
            return intVal;
        }

        private int getValRoundedToNearest15Min(int val) {
            int adjVal = val;
            int hour = val / 100;
            int min = val % 100;
            int mod = min % 15;
            int res = 0;

            if (mod >= 8) {
                res = min + (15 - mod);
            } else {
                res = min - mod;
            }

            if (res > 59) {
                adjVal = (hour + 1) * 100;
            } else {
                adjVal = hour * 100 + res;
            }

            return adjVal;
        }

        private String getWmoPhen() {
            String phen = SigmetAttrDlg.this.getEditableAttrPhenom();
            if (phen != null) {
                phen = phen.trim();
            }
            if (PgenConstant.TYPE_VOLCANIC_ASH.equals(phen)) {
                return "V";
            }
            if (PgenConstant.TYPE_TROPICAL_CYCLONE.equals(phen)) {
                return "C";
            }
            return "S";
        }

        private String getAwpPhen() {
            String wmophen = getWmoPhen();
            if ("S".equals(wmophen)) {
                return "SIG";
            }
            if ("V".equals(wmophen)) {
                return "WSV";
            }
            return "WST";
        }

        private String getInum() {
            char firstIdChar = SigmetAttrDlg.this.getEditableAttrId().charAt(0);

            if ("PHFO".equals(SigmetAttrDlg.this.getEditableAttrArea())) {
                int inum = firstIdChar - 77;
                return inum < 0 || inum > 9 ? Integer.toString(inum)
                        : "0" + inum;
            } else if ("PAWU"
                    .equals(SigmetAttrDlg.this.getEditableAttrArea())) {
                int inum = firstIdChar - 72;
                return inum < 0 || inum > 9 ? Integer.toString(inum)
                        : "0" + inum;
            }
            int inum = firstIdChar - 64;
            return inum < 0 || inum > 9 ? Integer.toString(inum) : "0" + inum;
        }

        private String getIdnode() {
            String area = SigmetAttrDlg.this.getEditableAttrArea();

            if ("KKCI".equals(area)) {
                return "MKC";
            }
            if ("KNHC".equals(area)) {
                return "NHC";
            }
            if ("PHFO".equals(area)) {
                return "HFO";
            }
            // PAWU
            return "ANC";
        }

        private String[] getOcnWmoAwpHeaders() {
            String area = SigmetAttrDlg.this.getEditableAttrArea();
            // 0:hdrocn, 1:hdrwmo, 2:hdrawp
            String[] headers = new String[3];
            headers[0] = headers[1] = headers[2] = "";

            if ("PAWU".equals(area)) {
                headers[1] = "AK";
                headers[2] = "AK";
            } else if ("PHFO".equals(area)) {
                headers[1] = "PA";
                headers[2] = "PA";
            } else {
                // area is KKCI or KNHC
                String fir = SigmetAttrDlg.this.getFirs();
                if (!(fir == null || fir.length() == 0)) {
                    if (fir.contains("KZHU") || fir.contains("KZMA")
                            || fir.contains("KZWY") || fir.contains("TJZS")) {
                        headers[0] = "NT";
                        headers[1] = "NT";
                        headers[2] = "A0";
                    } else if (fir.contains("KZAK") || fir.contains("PAZA")) {
                        headers[0] = "PN";
                        headers[1] = "PN";
                        headers[2] = "P0";
                    }
                }
            }

            return headers;
        }

        @Override
        public void setAttrForDlg(IAttribute ia) {
            // No op
        }

        private String getLineTypeForSOL(String typeString) {
            String[] lineTypes = new String[] { "EITHER SIDE OF", "NORTH OF",
                    "SOUTH OF", "EAST OF", "WEST OF" };

            String type = typeString.split(SigmetInfo.LINE_SEPERATER)[1];
            int index = 0;
            for (int i = 0; i < LINE_SIDES.length; i++) {
                if (LINE_SIDES[i].equals(type)) {
                    index = i;
                }
            }
            return lineTypes[index];
        }

    }

    public gov.noaa.nws.ncep.ui.pgen.elements.DrawableElement getSigmet() {
        return sigmet;
    }

    public void setSigmet(
            gov.noaa.nws.ncep.ui.pgen.elements.DrawableElement sigmet) {
        this.sigmet = sigmet;

        Button[] buttons = attrButtonMap.get(EDITABLE_ATTR_FROM_LINE);
        Coordinate[] coors = ((Sigmet) sigmet).getLinePoints();
        String s = "";

        for (int i = 0; buttons != null && i < buttons.length; i++) {
            Button btn = buttons[i];
            if (btn != null && (!btn.isDisposed()) && btn.getSelection()
                    && btn.getText() != null && btn.getText().length() > 0) {
                if (btn.getText().contains("VOR")) {
                    s = this.getVOR(coors);
                } else {
                    s = getLatLonStringPrepend2(coors,
                            AREA.equals(((Sigmet) sigmet).getType()));
                }
            }
        }
        if (txtInfo != null && !txtInfo.isDisposed() && s != null) {
            // Update FIR region buttons
            this.updateFirBtn();
            this.resetText(s, txtInfo);
        }
        // TTR 974 - "editableAttrFromLine" needs update as well.
        if (s != null) {
            ((Sigmet) sigmet).setEditableAttrFromLine(s);
        }

    }

    private void init() {

        Sigmet sigmet = (Sigmet) this.getSigmet();
        if (sigmet != null) {
            copyEditableAttrToSigmetAttrDlg(sigmet);
            if (sigmet.getEditableAttrIssueOffice() != null) {
                if (!comboID.isDisposed()) {
                    populateIdList(sigmet.getEditableAttrIssueOffice());
                }
            }
        }

        Field[] fields = this.getClass().getDeclaredFields();
        for (Field f : fields) {
            String attr = f.getName();
            String typeValue = "";
            try {
                typeValue = String.valueOf(f.get(this));
            } catch (Exception e) {
                statusHandler.debug(e.getLocalizedMessage(), e);
            }
            Control cont = attrControlMap.get(attr);
            if (cont instanceof Combo || cont instanceof Spinner
                    || cont instanceof Text) {
                if (!cont.isDisposed()) {
                    if (EDITABLE_ATTR_FROM_LINE.equals(attr)) {
                        this.resetText(typeValue, (Text) cont);
                    } else {
                        setControl(cont, attr);
                    }
                }

                if (EDITABLE_ATTR_FROM_LINE.equals(attr) && (!cont.isDisposed())
                        && typeValue != null) {
                    // New, Old, VOR Buttons
                    Button[] butts = attrButtonMap.get(attr);

                    String[] words = typeValue.split(SigmetInfo.LINE_SEPERATER);
                    String lastPart = words[words.length - 1];

                    if (butts != null) {
                        if ("New".equals(lastPart)) {
                            butts[0].setSelection(true);
                            butts[1].setSelection(false);
                        } else {
                            butts[0].setSelection(false);
                            butts[1].setSelection(true);
                        }
                    }
                }

                // Area, Line, Isolated
                if (LINE_TYPE.equals(attr)) {
                    Button[] butts = attrButtonMap.get(attr);
                    if (butts != null) {
                        for (Button butt : butts) {
                            if (butt != null && typeValue
                                    .contains(butt.getText().trim())) {
                                butt.setSelection(true);
                                butt.notifyListeners(SWT.Selection,
                                        new Event());
                                for (Button butt2 : butts) {
                                    if (butt2 != butt) {
                                        butt2.setSelection(false);
                                    }
                                }
                                break;
                            }
                        }
                    }
                }
            }

            // Forecast Section of the GUI for VOLCANIC ASH and
            // TROPICAL CYCLONE
            if (PgenConstant.TYPE_TROPICAL_CYCLONE.equals(editableAttrPhenom)
                    || PgenConstant.TYPE_VOLCANIC_ASH
                            .equals(editableAttrPhenom)) {
                setForecastSectionGUI(attr, typeValue);
            }

            // STNRY,MVG
            if ((EDITABLE_ATTR_MOVEMENT.equals(attr)
                    && this.withExpandedArea)) {
                Button[] butts = null;
                butts = attrButtonMap.get(attr);
                if (butts != null) {
                    for (Button butt : butts) {
                        if (butt != null && !butt.isDisposed()
                                && typeValue != null
                                && typeValue.contains(butt.getText().trim())) {
                            butt.setSelection(true);
                            for (Button butt2 : butts) {
                                if (butt2 != butt) {
                                    butt2.setSelection(false);
                                }
                            }
                            break;
                        }
                    }
                }
                continue;
            }

            // New/Update,Amend,Cancel
            if (EDITABLE_ATTR_STATUS.equals(attr) && this.withExpandedArea) {
                Button[] butts = null;
                butts = attrButtonMap.get(attr);
                if (butts != null && typeValue != null) {
                    char status = typeValue.charAt(0);
                    switch (status) {
                    case '0':
                        butts[0].setSelection(true);
                        butts[1].setSelection(false);
                        butts[2].setSelection(false);
                        break;
                    case '1':
                        butts[1].setSelection(true);
                        butts[0].setSelection(false);
                        butts[2].setSelection(false);
                        break;
                    case '2':
                        butts[2].setSelection(true);
                        butts[1].setSelection(false);
                        butts[0].setSelection(false);
                        break;
                    default:
                        butts[0].setSelection(true);
                    }
                }
            }
        }
        tropCycFlag = false;
        withExpandedArea = false;
    }

    private void setForecastSectionGUI(String attr, String typeValue) {
        if (EDITABLE_ATTR_FCST_AVAIL.equals(attr) && this.withExpandedArea
                && !StringUtils.isEmpty(typeValue)) {
            Button checkBox = (Button) attrControlMap
                    .get(EDITABLE_ATTR_FCST_AVAIL);
            checkBox.setSelection(Boolean.valueOf(typeValue));
        }

        if (EDITABLE_ATTR_RAL_SELECTION.equals(attr)
                || EDITABLE_ATTR_ALTITUDE_SELECTION.equals(attr)) {
            Button[] butts = null;
            if (EDITABLE_ATTR_RAL_SELECTION.equals(attr)
                    || EDITABLE_ATTR_ALTITUDE_SELECTION.equals(attr)) {
                butts = attrButtonMap.get(attr);
                if (butts != null) {
                    for (Button butt : butts) {
                        if (butt != null && !butt.isDisposed()
                                && typeValue != null
                                && typeValue.equals(butt.getText())) {
                            butt.setSelection(true);
                            for (Button butt2 : butts) {
                                if (butt2 != butt) {
                                    butt2.setSelection(false);
                                }
                            }
                            break;
                        }
                    }
                }
            }
        }
    }

    /*
     * This method validates the input lat or lon are in the specified formats
     * as below since AWC receives info in various formats from TACs around the
     * world) and be convertyed later into specified formats.
     *
     * This a new requirement from AWC as described in AWC Sept 2014 FIT test
     * report for TTR 433 - J. Wu, Oct. 7th, 2014.
     *
     * "N", "S", and "–" are literal; "dd" or "ddd" is degree; "mm" is minute;
     * "p" is decimal degree; "(d)" is an optional degree longitude digit for
     * longitudes with absolute value >= 100.0):
     *
     * A) "Phenom Lat" text box accepts a latitude north: Nddmm, ddmmN, dd.pN,
     * Ndd.p, dd.p; Output is Nddmm.
     *
     * B)"Phenom Lat" text box accepts a latitude south: Sddmm, ddmmS, dd.pS,
     * Sdd.p, dd.p; Output is Sddmm.
     *
     * C)"Phenom Lon" text box accepts a longitude west: W(d)ddmm, (d)ddmmW,
     * (d)dd.pW, W(d)dd.p, -(d)dd.p; Output is W[0|1]ddmm.
     *
     * D)"Phenom Lon" text box accepts a longitude east: E(d)ddmm, (d)ddmmE,
     * (d)dd.pE, E(d)dd.p, (d)dd.p; Output is E[0|1]ddmm.
     *
     * Note:
     *
     * (1) "N", "S", "E", "W" is not case-sensitive.
     *
     * (2) "N"/"S"/"E"/"W" should have been stripped of before sending in here.
     *
     * (3) A "-" should be placed at the beginning if "S" or "W" found in the
     * input.
     */
    private boolean validateLatLon(String coor, boolean isLat) {
        String regexLat = "(-?[0-8]?[0-9](\\.)?(\\d*)?)|-?90(\\.)?([0]*)?";
        String regexLon = "(-?([1]?[0-7][0-9]|[0]?[0-9]?[0-9])?(\\.)?(\\d*)?)|-?180(\\.)?([0]*)?";

        java.util.regex.Matcher m;
        if (isLat) {
            m = java.util.regex.Pattern.compile(regexLat).matcher(coor);
        } else {
            m = java.util.regex.Pattern.compile(regexLon).matcher(coor);
        }

        return m.matches();
    }

    private boolean validateNumInput(String num) {
        try {
            Double.parseDouble(num);
        } catch (NumberFormatException e) {
            statusHandler.debug(e.getLocalizedMessage(), e);
            return false;
        }
        return true;
    }

    public static String latlonWithSpace(String s) {
        if (s == null || s.length() < 1) {
            return "";
        }

        String rr = "";
        if (s.length() > 0) {
            char r = s.charAt(0);

            if (r == 'N' || r == 'S') {
                rr = latlonAddSpace(s, true);
            } else {
                char e = s.charAt(s.length() - 1);
                if (e == 'W' || e == 'E') {
                    rr = latlonAddSpace(s, false);
                }
            }
        }

        return rr;
    }

    public static String latlonAddSpace(String s, boolean isHeadNS) {
        if (isHeadNS) {
            if (s.contains("W")) {
                return s.replace("W", " W");
            } else if (s.contains("E")) {
                return s.replace("E", " E");
            }
        } else {
            if (s.contains("N")) {
                return s.replace("N", "N ");
            } else if (s.contains("S")) {
                return s.replace("S", "S ");
            }
        }

        return "";
    }

    public static final String getLatLonStringPrepend2(Coordinate[] coors,
            boolean isLineTypeArea) {

        String paddedDash = " - ";

        String fourZeros = "0000";
        String fiveZeros = "00000";

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < coors.length; i++) {
            Coordinate coor = coors[i];

            result.append(coor.y >= 0 ? "N" : "S");
            int latDeg = ((int) Math.abs(coor.y) * 100);
            int latMin = (int) Math
                    .round(Math.abs(coor.y - (int) (coor.y)) * 60);
            long y = 0;

            // Coordinates are specified in degrees and minutes.
            // The expected minutes range from 0 to 59.
            if (latMin >= 60) {
                latMin = latMin - 60;
                latDeg = ((latDeg / 100) + 1) * 100;
                y = (long) latDeg + latMin;
            } else {
                y = ((int) Math.abs(coor.y) * 100)
                        + Math.round(Math.abs(coor.y - (int) (coor.y)) * 60);
            }
            result.append(new DecimalFormat(fourZeros).format(y));

            result.append(coor.x >= 0 ? " E" : " W");
            int lonDeg = ((int) Math.abs(coor.x)) * 100;
            int lonMin = (int) Math
                    .round(Math.abs(coor.x - (int) (coor.x)) * 60);
            long x = 0;

            // Coordinates are specified in degrees and minutes.
            // The expected minutes range from 0 to 59.
            if (lonMin >= 60) {
                lonMin = lonMin - 60;
                lonDeg = ((lonDeg / 100) + 1) * 100;
                x = (long) lonDeg + lonMin;
            } else {
                x = ((int) Math.abs(coor.x)) * 100
                        + Math.round(Math.abs(coor.x - (int) (coor.x)) * 60);
            }
            result.append(new DecimalFormat(fiveZeros).format(x));

            if (i < (coors.length - 1)) {
                result.append(paddedDash);
            }
        }

        if (isLineTypeArea) {
            result.append(paddedDash)
                    .append(result.toString().split(paddedDash)[0]);
        }

        return result.toString();
    }

    public static String[] getPhenomenons(String type) {
        java.util.Set<String> set = new java.util.LinkedHashSet<>();

        if (type == null || type.isEmpty()) {
            return set.toArray(new String[] {});
        }

        org.w3c.dom.NodeList nlist = null;

        File file = PgenStaticDataProvider.getProvider().getStaticFile(
                PgenStaticDataProvider.getProvider().getPgenLocalizationRoot()
                        + "phenomenons.xml");

        try {
            org.w3c.dom.Document doc = javax.xml.parsers.DocumentBuilderFactory
                    .newInstance().newDocumentBuilder()
                    .parse(file.getAbsoluteFile());
            nlist = doc.getElementsByTagName(type);

            if (nlist != null && nlist.getLength() > 0) {
                nlist = nlist.item(0).getChildNodes();

                for (int i = 0; nlist != null && i < nlist.getLength(); i++) {
                    String phenom = nlist.item(i).getTextContent();

                    if (phenom != null && (phenom.trim().length() > 0)) {
                        set.add(phenom.trim());
                    }
                }
            }
        } catch (Exception e) {
            statusHandler.error("___SigmetAttrDlg: getPhenomenons(): "
                    + e.getLocalizedMessage(), e);
        }

        if (set.isEmpty()) {
            set.add("--");
        }

        return set.toArray(new String[0]);
    }

    @Override
    public Coordinate[] getLinePoints() {
        return Collections.emptyList().toArray(new Coordinate[0]);
    }

    @Override
    public String getPatternName() {
        return null;
    }

    @Override
    public Boolean isClosedLine() {
        return false;
    }

    /**
     * This method validates the input lat or lon are in the specified formats
     * and format it into a specfied form as described below:
     *
     * This a new requirement from AWC as described in AWC Sept 2014 FIT test
     * report for TTR 433 - J. Wu, Oct. 7th, 2014.
     *
     * "N", "S", and "–" are literal; "dd" or "ddd" is degree; "mm" is minute;
     * "p" is decimal degree; "(d)" is an optional degree longitude digit for
     * longitudes with absolute value >= 100.0):
     *
     * A) "Phenom Lat" text box accepts a latitude north: Nddmm, ddmmN, dd.pN,
     * Ndd.p, dd.p; Output is Nddmm.
     *
     * B)"Phenom Lat" text box accepts a latitude south: Sddmm, ddmmS, dd.pS,
     * Sdd.p, dd.p; Output is Sddmm.
     *
     * C)"Phenom Lon" text box accepts a longitude west: W(d)ddmm, (d)ddmmW,
     * (d)dd.pW, W(d)dd.p, -(d)dd.p; Output is W[0|1]ddmm.
     *
     * D)"Phenom Lon" text box accepts a longitude east: E(d)ddmm, (d)ddmmE,
     * (d)dd.pE, E(d)dd.p, (d)dd.p; Output is E[0|1]ddmm.
     *
     * Note: "N", "S", "E", "W" is not case-sensitive here.
     */
    private String getPhenomLatLon(String input, boolean isLat) {

        input = input.toUpperCase();
        if ((isLat && (input.startsWith("S") || input.endsWith("S")))
                || (!isLat && (input.startsWith("W") || input.endsWith("W")))) {
            input = "-" + input;
        }

        /*
         * remove characters that is not a digit, -, or decimal point.
         */
        input = input.replaceAll("[^-0-9.]", "");

        /*
         * Format the output to the desired form.
         */
        StringBuilder result = new StringBuilder();
        if (!"".equals(input) && !"-".equals(input)
                && validateLatLon(input, isLat)) {

            if (!input.contains(".")) {

                /*
                 * Padding to make lat as "ddmm" and lon as "dddmm".
                 */
                String istr = input.replaceAll("-", "");
                int len = istr.length();
                String ostr = "";
                int val = Integer.parseInt(istr);
                if (isLat) {
                    if (len == 1) {
                        ostr += ("0" + istr + "00");
                    } else if (len == 2) {
                        if (val <= 90) {
                            ostr += (istr + "00");
                        }
                    } else if (len == 3) {
                        if (val <= 900) {
                            ostr += (istr + "0");
                        }
                    } else {
                        String tmp = istr.substring(0, 4);
                        if (Integer.parseInt(tmp) <= 9000) {
                            ostr += tmp;
                        }
                    }
                } else {
                    if (len == 1) {
                        ostr += ("00" + istr + "00");
                    } else if (len == 2) {
                        ostr += ("0" + istr + "00");
                    } else if (len == 3) {
                        if (val <= 180) {
                            ostr += (istr + "00");
                        } else {
                            ostr += ("0" + istr + "0");
                        }
                    } else if (len == 4) {
                        if (val > 180) {
                            ostr += ("0" + istr);
                        } else if (val >= 100) {
                            ostr += (val + "00");
                        } else if (val >= 10) {
                            ostr += ("0" + val + "00");
                        } else {
                            ostr += ("00" + val + "00");
                        }

                    } else {
                        String tmp = istr.substring(0, 5);
                        if (Integer.parseInt(tmp) <= 180_00) {
                            ostr += tmp;
                        }
                    }
                }

                if (ostr.length() > 0) {
                    if (isLat) {
                        result.append(input.startsWith("-") ? "S" : "N");
                    } else {
                        result.append(input.startsWith("-") ? "W" : "E");
                    }
                }

                result.append(ostr);

            } else {
                /*
                 * Convert to degree and minutes and then padding to make lat as
                 * "ddmm" and lon as "dddmm".
                 */
                Double value = Double.parseDouble(input);
                int deg = value.intValue();

                Double minute = (Math.abs(value - deg)) * 60.0;
                int mm = (int) Math.round(minute);

                deg = Math.abs(deg);

                if (isLat) {
                    result.append(value >= 0.0 ? "N" : "S");
                    String dstr = (deg < 10) ? ("0" + deg)
                            : Integer.toString(deg);
                    result.append(dstr);
                    String mstr = (mm < 10) ? ("0" + mm) : Integer.toString(mm);
                    result.append(mstr);

                } else {
                    result.append(value >= 0.0 ? "E" : "W");
                    String dstr = (deg < 10) ? ("00" + deg)
                            : ((deg < 100) ? ("0" + deg)
                                    : Integer.toString(deg));
                    result.append(dstr);
                    String mstr = (mm < 10) ? ("0" + mm) : Integer.toString(mm);
                    result.append(mstr);
                }
            }

            return result.toString().trim();
        } else {
            return "";
        }
    }

    public Sigmet convertType(Sigmet newEl) {

        String origLineType = this.getOrigLineType();
        String newLineType = this.getLineType();

        if (!newLineType.equals(origLineType)) {

            float p45 = 45.0F;
            float p135 = 135.0F;
            float p225 = 225.0F;
            float p315 = 315.0F;

            ArrayList<Coordinate> ptsCopy = newEl.getPoints();
            ArrayList<Coordinate> newPtsCopy = new ArrayList<>();

            /*
             * converting from a point (Isolated)
             */
            if (ISOLATED.equals(origLineType)) {

                Coordinate centerCoor = ptsCopy.get(0);

                /* to a Line */
                if (newLineType.startsWith(LINE)) {
                    /*
                     * 3 point diagonal with original point as middle point
                     */
                    newPtsCopy.add(PgenUtil.computePoint(centerCoor,
                            Float.parseFloat(widthStr), p315));
                    newPtsCopy.add(centerCoor);
                    newPtsCopy.add(PgenUtil.computePoint(centerCoor,
                            Float.parseFloat(widthStr), p135));
                } else {
                    /* to an Area */
                    /*
                     * square centered around original point
                     */
                    newPtsCopy.add(PgenUtil.computePoint(centerCoor,
                            Float.parseFloat(widthStr), p45));
                    newPtsCopy.add(PgenUtil.computePoint(centerCoor,
                            Float.parseFloat(widthStr), p135));
                    newPtsCopy.add(PgenUtil.computePoint(centerCoor,
                            Float.parseFloat(widthStr), p225));
                    newPtsCopy.add(PgenUtil.computePoint(centerCoor,
                            Float.parseFloat(widthStr), p315));
                }

                newEl.setPoints(newPtsCopy);
            } else if (ISOLATED.equals(newLineType)) {
                /*
                 * converting to a point (Isolated)
                 */
                newPtsCopy.add(ptsCopy.get(0));
                newEl.setPoints(newPtsCopy);
            }
        }

        setSigmetFromLine(newEl);
        return newEl;
    }

    public void setSigmetFromLine(
            gov.noaa.nws.ncep.ui.pgen.elements.DrawableElement sigmet) {
        this.sigmet = sigmet;
        Button[] buttons = attrButtonMap.get(EDITABLE_ATTR_FROM_LINE);
        Coordinate[] coors = ((Sigmet) sigmet).getLinePoints();
        StringBuilder s = new StringBuilder();

        for (int i = 0; buttons != null && i < buttons.length; i++) {
            Button btn = buttons[i];
            if (btn != null && (!btn.isDisposed()) && btn.getSelection()
                    && btn.getText() != null && btn.getText().length() > 0) {
                if (btn.getText().contains("VOR")) {
                    s.append(this.getVOR(coors));
                    s.append(SigmetInfo.LINE_SEPERATER);
                    String latLonFmtText = s.append("VOR").toString();
                    SigmetAttrDlg.this
                            .setLatLonFormatFlagAndText(latLonFmtText);
                    setEditableAttrFromLine(latLonFmtText);
                } else {
                    s.append(getLatLonStringPrepend2(coors,
                            AREA.equals(((Sigmet) sigmet).getType())));
                    s.append(SigmetInfo.LINE_SEPERATER);
                    String latLonFmtText = s.append("New").toString();
                    SigmetAttrDlg.this
                            .setLatLonFormatFlagAndText(latLonFmtText);
                    setEditableAttrFromLine(latLonFmtText);
                }
            }
        }
        if (txtInfo != null && !txtInfo.isDisposed()) {
            this.resetText(s.toString(), txtInfo);
        }

    }

    public void updateFirBtn() {

        // save the previous FIR Region ID
        String oldEditableFirID = editableAttrFir;
        if (oldEditableFirID.equals(null) || oldEditableFirID.equals("")) {
            return;
        }

        String[] copyFir = SigmetInfo.FIR_ARRAY.clone();
        String[] loopFIR = new String[copyFir.length];

        for (int i = 0; copyFir != null && i < copyFir.length; i++) {
            loopFIR[i] = copyFir[i].substring(0, 4);
        }

        editableFirID = "";
        String newEditableFirID = SigmetAttrDlg.this.getFirs();
        /*
         * need to update the current products Fir-Id otherwise xml file
         * output's editableAttrFir will hold the initial value
         */
        setEditableAttrFir(newEditableFirID);
        Button[] firButt = null;

        for (String str : loopFIR) {

            firButt = firButtonMap.get(str);

            for (int i = 0; firButt != null && i < firButt.length; i++) {

                if (newEditableFirID.contains(str)) {

                    firButt[i].setSelection(true);

                } else {
                    firButt[i].setSelection(false);
                }
            }

        }
    }

    public void copyEditableAttrToSigmetAttrDlg(Sigmet sig) {
        this.setLineType(sig.getType());
        // legacy properties
        this.setWidthStr(Double.toString(sig.getWidth()));

        this.setEditableAttrFreeText(sig.getEditableAttrFreeText());
        this.setEditableAttrStatus(sig.getEditableAttrStatus());
        this.setEditableAttrStartTime(sig.getEditableAttrStartTime());
        this.setEditableAttrEndTime(sig.getEditableAttrEndTime());
        this.setEditableAttrPhenom(sig.getEditableAttrPhenom());
        this.setEditableAttrPhenom2(sig.getEditableAttrPhenom2());
        this.setEditableAttrPhenomLat(sig.getEditableAttrPhenomLat());
        this.setEditableAttrPhenomLon(sig.getEditableAttrPhenomLon());
        this.setEditableAttrPhenomSpeed(sig.getEditableAttrPhenomSpeed());
        this.setEditableAttrPhenomDirection(
                sig.getEditableAttrPhenomDirection());

        this.setEditableAttrFcstTime(sig.getEditableAttrFcstTime());
        this.setEditableAttrFcstCntr(sig.getEditableAttrFcstCntr());
        this.setEditableAttrFcstPhenomLat(sig.getEditableAttrFcstPhenomLat());
        this.setEditableAttrFcstPhenomLon(sig.getEditableAttrFcstPhenomLon());
        this.setEditableAttrFcstVADesc(sig.getEditableAttrFcstVADesc());
        this.setEditableAttrFcstAvail(sig.getEditableAttrFcstAvail());

        this.setEditableAltLevelText(sig.getEditableAttrAltLevelText());
        this.setEditableAttrRALSelection(sig.getEditableAttrRALSelection());
        this.setEditableAttrAltitudeSelection(
                sig.getEditableAttrAltitudeSelection());
        this.setEditableAttrRemarks(sig.getEditableAttrRemarks());
        this.setEditableAttrPhenomName(sig.getEditableAttrPhenomName());
        this.setEditableAttrPhenomPressure(sig.getEditableAttrPhenomPressure());
        this.setEditableAttrPhenomMaxWind(sig.getEditableAttrPhenomMaxWind());
        this.setEditableAttrTrend(sig.getEditableAttrTrend());
        this.setEditableAttrMovement(sig.getEditableAttrMovement());
        this.setEditableAttrLevel(sig.getEditableAttrLevel());
        this.setEditableAttrLevelInfo1(sig.getEditableAttrLevelInfo1());
        this.setEditableAttrLevelInfo2(sig.getEditableAttrLevelInfo2());
        this.setEditableAttrLevelText1(sig.getEditableAttrLevelText1());
        this.setEditableAttrLevelText2(sig.getEditableAttrLevelText2());
        this.setEditableAttrAltLevel(sig.getEditableAttrAltLevel());
        this.setEditableAttrAltLevelInfo1(sig.getEditableAttrAltLevelInfo1());
        this.setEditableAttrAltLevelInfo2(sig.getEditableAttrAltLevelInfo2());
        this.setEditableAttrAltLevelText1(sig.getEditableAttrAltLevelText1());
        this.setEditableAttrAltLevelText2(sig.getEditableAttrAltLevelText2());
        this.setEditableAttrFir(sig.getEditableAttrFir());
        this.setEditableAttrCarSamBackupMode(
                sig.getEditableAttrCarSamBackupMode());

        String lineType = this.getType();
        if (lineType != null && lineType.contains(SigmetInfo.LINE_SEPERATER)) {
            this.setSideOfLine(lineType.split(SigmetInfo.LINE_SEPERATER)[1]);
        }

        this.setWidthStr("" + (Math.round(sig.getWidth())));// NM
        this.setLatLonFormatFlagAndText(sig.getEditableAttrFromLine());

        /*
         * from AbstractSigmet: Class.getDeclaredFields() excludes inherited
         * fields. if sigmet has null for these, don't copy
         */
        if (sig.getEditableAttrArea() != null) {
            this.setEditableAttrArea(sig.getEditableAttrArea());
        }
        if (sig.getEditableAttrIssueOffice() != null) {
            this.setEditableAttrIssueOffice(sig.getEditableAttrIssueOffice());
        }
        if (sig.getEditableAttrId() != null) {
            this.setEditableAttrId(sig.getEditableAttrId());
        }
        if (sig.getEditableAttrSeqNum() != null) {
            this.setEditableAttrSeqNum(sig.getEditableAttrSeqNum());
        }
        this.setEditableAttrFromLine(sig.getEditableAttrFromLine());
    }

    /*
     * Sets the background color for a SWT control.
     */
    private void setBackgroundColor(Control ww, Color color) {
        org.eclipse.swt.graphics.Color clr = new org.eclipse.swt.graphics.Color(
                ww.getDisplay(), color.getRed(), color.getGreen(),
                color.getBlue());
        ww.setBackground(clr);
        clr.dispose();
    }

}
