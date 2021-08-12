package gov.noaa.nws.ncep.ui.pgen;

/**
 * A central place for constants used throughout PGen, include constants here
 * that are likely to be used in more than one file within PGen.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Jun 26, 2015  8198     srussell  Initial creation
 * Nov 18, 2015  12829    J. Wu     Add more common constants.
 * Dec 17, 2015  12990    J. Wu     Add more constants.
 * Jan 14, 2016  12989    P. Moyer  Constants used in PgenTextDrawingTool that
 *                                  can be shared.
 * Jan 27, 2016  13166    J. Wu     Add CIRCLE.
 * May 02, 2016  16076    J. Wu     Add more constants.
 * May 16, 2016  18388    J. Wu     Add more constants.
 * May 25, 2016  17940    J. Wu     Add more constants.
 * Dec 12, 2016  17469    W. Kwock  Added CWA Formatter
 * Jan 06, 2020  71971    smanoj    Added constants for SIGMET Types
 * Feb 26, 2020  75024    smanoj    Added constants for Front Types and Labels
 * Mar 13, 2020  76151    tjensen   Added TYPE_TROPICAL_CYCLONE and
 *                                  TYPE_VOLCANIC_ASH
 * Dec 09, 2020  85217    smanoj    Added ADD_NEW_LABEL
 *
 * Aug 17, 2021  93036      omoncayo     PGEN INTL SIGMET:QC Check Attributes
 *
 * </pre>
 *
 * @author srussell
 *
 */

public final class PgenConstant {
    public static final String LABELED_SYMBOL = "labeledSymbol";

    public static final String LABELED_FRONT = "labeledFront";

    public static final String DELETE_LABEL = "Delete Label";

    public static final String ADD_LABEL = "Add Label";

    public static final String ADD_NEW_LABEL = "Add New Label";

    public static final String EDIT_LABEL = "Edit Label";

    public static final String ALWAYS_VISIBLE = "alwaysVisible";

    public static final String TRUE = "true";

    public static final String FALSE = "false";

    public static final String LABEL = "label";

    public static final String ICON = "icon";

    public static final String NAME = "name";

    public static final String ACTIONSXTRA = "actionsxtra";

    public static final String SYMBOL = "Symbol";

    public static final String ACTIONS = "actions";

    public static final String CLASSNAME = "className";

    public static final String PLUGINXML_ATTRIBUTE_DELIMETER = "\\s*,\\s*";

    /*
     * Pgen actions as defined in pgen plugin.xml <action name="Connect", ...>
     */
    public static final String ACTION_CONNECT = "Connect";

    public static final String ACTION_COPY = "Copy";

    public static final String ACTION_EXTRAP = "Extrap";

    public static final String ACTION_FLIP = "Flip";

    public static final String ACTION_INTERP = "Interp";

    public static final String ACTION_MODIFY = "Modify";

    public static final String ACTION_MOVE = "Move";

    public static final String ACTION_MULTISELECT = "MultiSelect";

    public static final String ACTION_ROTATE = "Rotate";

    public static final String ACTION_SELECT = "Select";

    public static final String UNDO = "Undo";

    public static final String REDO = "Redo";

    /*
     * Pgen categories (aka, classes) as defined in pgen plugin.xml <class
     * name="Front", ...>
     */
    public static final String CATEGORY_ARC = "Arc";

    public static final String CATEGORY_COMBO = "Combo";

    public static final String CATEGORY_FRONT = "Front";

    public static final String CATEGORY_LINES = "Lines";

    public static final String CATEGORY_MET = "MET";

    public static final String CATEGORY_SIGMET = "SIGMET";

    public static final String CATEGORY_SYMBOLS = "Symbols";

    public static final String CATEGORY_TEXT = "Text";

    public static final String CATEGORY_ANY = "Any";

    /*
     * Pgen objects (aka, elements) as defined in pgen plugin.xml <object
     * name="General Text", ...>
     */
    public static final String TYPE_GENERAL_TEXT = "General Text";

    public static final String TYPE_OUTLOOK = "Outlook";

    public static final String TYPE_VOLCANO = "Volcano";

    public static final String TYPE_WATCH = "Watch";

    public static final String TYPE_INTL_SIGMET = "INTL_SIGMET";

    public static final String TYPE_VOLC_SIGMET = "VOLC_SIGMET";

    public static final String TYPE_VACL_SIGMET = "VACL_SIGMET";

    public static final String TYPE_CCFP_SIGMET = "CCFP_SIGMET";

    public static final String TYPE_CONV_SIGMET = "CONV_SIGMET";

    public static final String TYPE_NCON_SIGMET = "NCON_SIGMET";

    public static final String TYPE_AIRM_SIGMET = "AIRM_SIGMET";

    public static final String TYPE_OUTL_SIGMET = "OUTL_SIGMET";

    public static final String CIRCLE = "Circle";

    public static final String PARM = "Parm";

    public static final String LEVEL = "Level";

    public static final String LEVEL_TOPS = "TOPS";

    public static final String LEVEL_FCST = "FCST";

    public static final String LEVEL_NONE = "-none-";

    public static final String LEVEL_INFO_ABV = "ABV";

    public static final String LEVEL_INFO_BLW = "BLW";

    public static final String LEVEL_INFO_BTN = "BTN";

    public static final String LEVEL_INFO2_AND = "AND";

    public static final String FORECAST_HOUR = "ForecastHour";

    public static final String NONE = "None";

    public static final String DEFAULT_ACTIVITY_TYPE = "Default";

    public static final String DEFAULT_ACTIVITY_LABEL = "Default.DDMMYYYY.HH.xml";

    public static final String DEFAULT_SUBTYPE = "None";

    public static final String CONTOURS = "Contours";

    public static final String EVENT_DEFAULT_TEXT = "defaultTxt";

    public static final String EVENT_LABEL = "addLabel";

    public static final String EVENT_PREV_COLOR = "usePrevColor";

    public static final String EVENT_OTHER = "Other";

    public static final String GENERAL_DEFAULT = "Default";

    public static final String OPTION_ALL = "All";

    public static final String DESK = "DESK";

    public static final String G2G_BOUND_MARK = "9999";

    public static final String SIGMET = "SIGMET";

    public static final String CWA_SIGMET = "CWA_SIGMET";

    public static final String CWA_FORMATTER = "CWA_FORMATTER";

    public static final String TYPE_TROPICAL_TROF = "TROPICAL_TROF";

    public static final String TROP_TROF_TEXT = "TROP_TROF_TEXT";

    public static final String LABEL_TRPCL_WAVE = "TRPCL WAVE";

    public static final String TYPE_TROF = "TROF";

    public static final String LABEL_TROF = "TROF";

    public static final String TYPE_DRY_LINE = "DRY_LINE";

    public static final String LABEL_DRYLINE = "DRYLINE";

    public static final String TYPE_INSTABILITY = "INSTABILITY";

    public static final String LABEL_SQUALL_LINE = "SQUALL LINE";

    public static final String TYPE_SHEAR_LINE = "SHEAR_LINE";

    public static final String LABEL_SHEARLINE = "SHEARLINE";

    public static final String TYPE_FRQ_TS = "FRQ_TS";

    public static final String TYPE_OBSC_TS = "OBSC_TS";

    public static final String TYPE_EMBD_TS = "EMBD_TS";

    public static final String TYPE_SQL_TS = "SQL_TS";

    public static final String TYPE_SEV_TURB = "SEV_TURB";

    public static final String TYPE_SEV_ICE = "SEV_ICE";

    public static final String TYPE_RDOACT_CLD = "RDOACT_CLD";

    public static final String TYPE_TROPICAL_CYCLONE = "TROPICAL_CYCLONE";

    public static final String TYPE_VOLCANIC_ASH = "VOLCANIC_ASH";
}