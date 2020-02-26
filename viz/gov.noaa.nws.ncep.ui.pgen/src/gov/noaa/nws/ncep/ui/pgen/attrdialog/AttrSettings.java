/*
 * gov.noaa.nws.ncep.ui.pgen.attrDialog.AttrSettings
 *
 * 26 March 2009
 *
 * This code has been developed by the NCEP/SIB for use in the AWIPS2 system.
 */

package gov.noaa.nws.ncep.ui.pgen.attrdialog;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.viz.core.exception.VizException;
import com.vividsolutions.jts.geom.Coordinate;

import gov.noaa.nws.ncep.ui.pgen.PgenConstant;
import gov.noaa.nws.ncep.ui.pgen.PgenStaticDataProvider;
import gov.noaa.nws.ncep.ui.pgen.contours.ContourMinmax;
import gov.noaa.nws.ncep.ui.pgen.contours.Contours;
import gov.noaa.nws.ncep.ui.pgen.elements.AbstractDrawableComponent;
import gov.noaa.nws.ncep.ui.pgen.elements.DECollection;
import gov.noaa.nws.ncep.ui.pgen.elements.DrawableElement;
import gov.noaa.nws.ncep.ui.pgen.elements.Layer;
import gov.noaa.nws.ncep.ui.pgen.elements.Line;
import gov.noaa.nws.ncep.ui.pgen.elements.Outlook;
import gov.noaa.nws.ncep.ui.pgen.elements.Product;
import gov.noaa.nws.ncep.ui.pgen.elements.Text;
import gov.noaa.nws.ncep.ui.pgen.elements.Track;
import gov.noaa.nws.ncep.ui.pgen.file.FileTools;
import gov.noaa.nws.ncep.ui.pgen.file.ProductConverter;
import gov.noaa.nws.ncep.ui.pgen.file.Products;
import gov.noaa.nws.ncep.ui.pgen.productmanage.ProductConfigureDialog;
import gov.noaa.nws.ncep.ui.pgen.tca.TCAElement;
import gov.noaa.nws.ncep.ui.pgen.tca.TropicalCycloneAdvisory;

/**
 * Singleton for the default attribute settings of PGEN DrawableElements.
 *
 * <pre>
 * SOFTWARE HISTORY
 *
 * Date          Ticket#     Engineer    Description
 * ------------- ----------- ----------- ---------------------------------------
 * 03/09         84          J. Wu       Initial Creation.
 * 04/09         89          J. Wu       Added Arc.
 * 07/10         270         B. Yin      Use AbstractDrawableComponent instead
 *                                       of IAttribute
 * 12/10         359         B. Yin      Change loadPgenSettings to public
 * 07/11         450         G. Hull     settings tbl from NcPathManager
 * 08/11         ?           B. Yin      Put current time for Storm Track
 * 11/11         ?           B. Yin      Load settings for different Activity
 *                                       Type.
 * 09/12                     B. Hebbard  Merge changes by RTS OB12.9.1 (format
 *                                       only, this file)
 * 10/13         TTR768      J. Wu       Load/set default attributes for outlook
 *                                       labels (Text).
 * 01/15         R5199/T1058 J. Wu       Load/Save settings for different
 *                                       settings tables.
 * 12/15         R12990      J. Wu       Added default spacing for contour
 *                                       symbols and their labels.
 * 12/15         R12989      P. Moyer    Prior text attribute tracking via
 *                                       pgenTypeLabels HashMap
 * Jan 27, 2016  R13166      J. Wu       Add symbol only & label only for
 *                                       Contours Min/Max.
 * Jun 20, 2016  R8305       B. Yin      Remove TCA advisory.
 * Jul 26, 2019  66393       mapeters    Add parm to contours key in settings map
 * Feb 26, 2020  75024       smanoj      Fix to have correct default Text Attributes for 
 *                                       tropical TROF front label.
 *
 * </pre>
 *
 * @author J. Wu
 */

public class AttrSettings {

    private static final String UNDERSCORE = "_";

    private static AttrSettings INSTANCE = null;

    /**
     * General Settings - each associated with a given settings_tbl_?.xml where
     * "?" is the activity type
     */
    private static HashMap<String, HashMap<String, AbstractDrawableComponent>> settingsMap = null;

    /**
     * Current Settings associated with the current activity.
     */
    private static String settingsName = null;

    private static HashMap<String, AbstractDrawableComponent> settings = null;

    public static String settingsFileName = "settings_tbl.xml";

    /**
     * Settings for outlook.
     */
    public static String outlookSettingsFileName = "outlooksettings.xml";

    private static HashMap<String, AbstractDrawableComponent> outlookLineSettings = null;

    private static HashMap<String, AbstractDrawableComponent> outlookLabelSettings = null;

    /**
     * Settings for default spacing between contour symbols with their labels.
     */
    private HashMap<String, HashMap<String, Coordinate>> contourSymbolSpacingMap = null;

    /**
     * Settings for PGEN type text label tracking
     */
    private static HashMap<String, String[]> pgenTypeLabels = null;

    /**
     * Private constructor
     *
     * @throws VizException
     */
    private AttrSettings() throws VizException {
        super();
        settingsMap = new HashMap<>();
        settings = new HashMap<>();
        pgenTypeLabels = new HashMap<>();
        loadSettingsTable();

        loadOutlookSettings();

        generateDefaultSpacingMap();
    }

    /**
     * Creates a AttrSettings instance if it does not exist and returns the
     * instance. If it exists, return the instance.
     *
     * @return
     */
    public static AttrSettings getInstance() {

        if (INSTANCE == null) {

            try {
                INSTANCE = new AttrSettings();
            } catch (VizException e) {
                e.printStackTrace();
            }

        }

        return INSTANCE;

    }

    /**
     * Gets the default settings loaded from the settings_tbl.xml for the given
     * settings ID.
     *
     * @param id
     *            the settings ID (typically just a PGEN type, but contours use
     *            {@link #getContourSettingsKey})
     *
     * @return a drawable containing the configured attributes
     */
    public AbstractDrawableComponent getSettings(String id) {
        AbstractDrawableComponent adc = settings.get(id);
        if (adc == null && isContoursKey(id)) {
            adc = settings.get(PgenConstant.CONTOURS);
        }
        return adc;
    }

    /**
     * Gets the file name where the current settings is loaded from.
     */
    public String getSettingsName() {
        return settingsName;
    }

    /**
     * Gets the default line settings for outlook loaded from the
     * outlooksettings.xml.
     */
    public HashMap<String, AbstractDrawableComponent> getOutlookLineSettings() {
        return outlookLineSettings;
    }

    /**
     * Gets the default label (text) settings for outlook loaded from the
     * outlooksettings.xml.
     */
    public HashMap<String, AbstractDrawableComponent> getOutlookLabelSettings() {
        return outlookLabelSettings;
    }

    /**
     * Associates the component's PgenType with the component in question.
     *
     * @param de
     *            the AbstractDrawableComponent to set
     */
    public void setSettings(AbstractDrawableComponent de) {

        String pgenID = de.getPgenType();
        if (de instanceof TCAElement) {
            /*
             * Remove TCA advisories. Save attributes only.
             */
            TCAElement tca = (TCAElement) de.copy();
            tca.setAdvisories(new ArrayList<TropicalCycloneAdvisory>());
            settings.put(pgenID, tca);
        } else {
            if (de instanceof Contours) {
                String parmSpecificKey = getContoursSettingsKey(
                        ((Contours) de).getParm());
                settings.put(parmSpecificKey, de);
            }
            settings.put(pgenID, de);
        }
    }

    /**
     * Checks the pgenTypeLabel HashMap for the presence of a PgenType. If it
     * exists, returns the string associated with it. Otherwise returns null.
     *
     * @param typeString
     *            the pgenType to search for
     * @return
     */
    public String[] getPgenTypeLabel(String type) {
        String[] ret = null;
        ret = pgenTypeLabels.get(type);
        return ret;
    }

    /**
     * Inserts a new PgenType/Text pair into the pgenTypeLabel HashMap. If it
     * already exists, replaces the existing text value with the new one.
     *
     * @param type
     *            the pgenType to insert
     * @param text
     *            the text string to insert
     */
    public void setPgenTypeLabel(String type, String[] text) {
        pgenTypeLabels.put(type, text);
    }

    /**
     * Load default settings from settings_tbl.xml from localization.
     */
    private void loadSettingsTable() {

        /*
         * Get the settings table file from localization
         */
        File settingsFile = PgenStaticDataProvider.getProvider().getFile(
                PgenStaticDataProvider.getProvider().getPgenLocalizationRoot()
                        + settingsFileName);

        if (settingsFile == null) {
            System.out.println("Unable to fing pgen settings table");
        }

        loadPgenSettings(settingsFile.getAbsolutePath());
    }

    /**
     * Load default settings defined for an PGEN activity.
     */
    public void loadProdSettings(String prodName) {
        if (prodName == null || prodName.isEmpty()) {
            loadSettingsTable();
        } else {
            try {

                String pt = ProductConfigureDialog.getProductTypes()
                        .get(prodName).getType();
                String pt1 = pt.replaceAll(" ", "_");

                LocalizationFile lFile = PgenStaticDataProvider.getProvider()
                        .getStaticLocalizationFile(
                                ProductConfigureDialog.getSettingFullPath(pt1));

                String filePath = lFile.getFile().getAbsolutePath();
                if (!new File(filePath).canRead()) {
                    loadSettingsTable();
                } else {
                    loadPgenSettings(filePath);
                }
            } catch (Exception e) {
                loadSettingsTable();
            }
        }

        generateDefaultSpacingMap();
    }

    /**
     * Read in a settings file from the input file.
     */
    private boolean loadPgenSettings(String fileName) {

        boolean ret = false;

        // Find if the table has been loaded.
        if (settingsMap.get(fileName) != null) {
            settings = settingsMap.get(fileName);
            settingsName = new String(fileName);
            return true;
        }

        // Load the table if never loaded before.
        HashMap<String, AbstractDrawableComponent> newSettings = new HashMap<>();

        File sFile = new File(fileName);

        try {
            if (sFile.canRead()) {
                Products products = FileTools.read(fileName);

                List<gov.noaa.nws.ncep.ui.pgen.elements.Product> prds;

                prds = ProductConverter.convert(products);

                for (gov.noaa.nws.ncep.ui.pgen.elements.Product p : prds) {

                    for (gov.noaa.nws.ncep.ui.pgen.elements.Layer layer : p
                            .getLayers()) {

                        for (gov.noaa.nws.ncep.ui.pgen.elements.AbstractDrawableComponent de : layer
                                .getDrawables()) {

                            String pgenID = de.getPgenType();

                            if (pgenID != null) {
                                if (de instanceof Contours) {
                                    /*
                                     * Load contours under parm-specific key,
                                     * and under non-parm-specific key
                                     * (non-parm-specific key is just a fallback
                                     * to ensure backwards compatibility)
                                     */
                                    String parmSpecificId = getContoursSettingsKey(
                                            ((Contours) de).getParm());
                                    newSettings.put(parmSpecificId, de);
                                }
                                newSettings.put(pgenID, de);
                            }

                            if (pgenID.equalsIgnoreCase("General Text")) {
                                ((Text) de).setText(new String[] { "" });
                            } else if (pgenID.equalsIgnoreCase("STORM_TRACK")) {
                                // set Track time to current time
                                Calendar cal1 = Calendar.getInstance(
                                        TimeZone.getTimeZone("GMT"));
                                Calendar cal2 = (Calendar) cal1.clone();
                                cal2.add(Calendar.HOUR_OF_DAY, 1);

                                ((Track) de).setFirstTimeCalendar(cal1);
                                ((Track) de).setSecondTimeCalendar(cal2);

                            } else if (PgenConstant.TROP_TROF_TEXT
                                    .equalsIgnoreCase(pgenID)) {
                                ((Text) de).setPgenType(PgenConstant.TROP_TROF_TEXT);
                                newSettings.put(pgenID, de);
                            }
                        }
                    }
                }

                if (newSettings.size() > 0) {
                    settingsMap.put(fileName, newSettings);
                    settings = newSettings;
                    settingsName = new String(fileName);
                    ret = true;
                }

            }
        } catch (Exception e) {
            ret = false;
        }

        return ret;
    }

    /**
     * Load default settings from outlooksettings.xml
     *
     * Setting is a hash map with outlook type and label string as keys.
     */
    private static void loadOutlookSettings() {

        if (outlookLineSettings == null) {
            outlookLineSettings = new HashMap<>();
        }

        if (outlookLabelSettings == null) {
            outlookLabelSettings = new HashMap<>();
        }

        String settingFile = PgenStaticDataProvider.getProvider()
                .getFileAbsolutePath(PgenStaticDataProvider.getProvider()
                        .getPgenLocalizationRoot() + outlookSettingsFileName);

        gov.noaa.nws.ncep.ui.pgen.file.Products products = FileTools
                .read(settingFile);

        if (products != null) {
            List<Product> prds;

            prds = ProductConverter.convert(products);

            for (Product p : prds) {

                for (Layer layer : p.getLayers()) {

                    Iterator<AbstractDrawableComponent> it = layer
                            .getComponentIterator();
                    while (it.hasNext()) {
                        AbstractDrawableComponent adc = it.next();
                        if (adc.getName().equalsIgnoreCase("OUTLOOK")) {
                            Iterator<AbstractDrawableComponent> itLn = ((Outlook) adc)
                                    .getComponentIterator();
                            while (itLn.hasNext()) {
                                AbstractDrawableComponent lnGrp = itLn.next();
                                if (lnGrp.getName().equalsIgnoreCase(
                                        Outlook.OUTLOOK_LABELED_LINE)) {
                                    String key = null;
                                    Line ln = null;
                                    Text txt = null;
                                    Iterator<DrawableElement> itDe = ((DECollection) lnGrp)
                                            .createDEIterator();
                                    while (itDe.hasNext()) {
                                        DrawableElement de = itDe.next();
                                        if (de instanceof Text) {
                                            txt = (Text) de;
                                            key = ((Outlook) adc)
                                                    .getOutlookType()
                                                    + txt.getText()[0];
                                        } else if (de instanceof Line) {
                                            ln = (Line) de;
                                        }
                                    }

                                    if (key != null && ln != null) {
                                        outlookLineSettings.put(key, ln);
                                        outlookLabelSettings.put(key, txt);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /*
     * Create default spacing map between contour symbols and their labels.
     */
    private void generateDefaultSpacingMap() {

        if (contourSymbolSpacingMap == null) {
            contourSymbolSpacingMap = new HashMap<>();
        }

        if (contourSymbolSpacingMap.get(settingsName) == null) {
            HashMap<String, Coordinate> contourSymbolSpacing = new HashMap<>();
            if (settings != null) {
                for (AbstractDrawableComponent adc : settings.values()) {
                    if (adc instanceof Contours) {
                        Contours contours = (Contours) adc;
                        List<ContourMinmax> csymbols = contours
                                .getContourMinmaxs();
                        if (csymbols != null) {
                            for (ContourMinmax cmx : csymbols) {
                                Text label = cmx.getLabel();
                                DrawableElement symbol = cmx.getSymbol();
                                if (label != null && symbol != null) {
                                    contourSymbolSpacing.put(
                                            symbol.getPgenType(),
                                            new Coordinate(label.getXOffset(),
                                                    label.getYOffset()));
                                }
                            }
                        }
                    }
                }
            }

            contourSymbolSpacingMap.put(settingsName, contourSymbolSpacing);
        }
    }

    /**
     * Retrieve default spacing between a contour symbol and its label in
     * current activity.
     *
     * @param symbolType
     *            pgenType for the contour symbol
     *
     * @return Coordinate A Coordinate with default spacing value in x and y
     *         direction.
     */
    public Coordinate getContourSymbolDefaultSpacing(String symbolType) {
        return contourSymbolSpacingMap.get(settingsName).get(symbolType);
    }

    /**
     * Get the key for retrieving contours settings via {@link #getSettings}.
     *
     * @param parm
     * @return the settings key
     */
    public static String getContoursSettingsKey(String parm) {
        return PgenConstant.CONTOURS + UNDERSCORE + parm;
    }

    private static boolean isContoursKey(String settingsKey) {
        return settingsKey != null
                && settingsKey.startsWith(PgenConstant.CONTOURS + UNDERSCORE);
    }
}