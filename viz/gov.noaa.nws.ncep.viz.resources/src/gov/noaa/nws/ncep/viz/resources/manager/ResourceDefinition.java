package gov.noaa.nws.ncep.viz.resources.manager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.raytheon.uf.common.dataquery.requests.DbQueryRequest;
import com.raytheon.uf.common.dataquery.requests.RequestConstraint;
import com.raytheon.uf.common.dataquery.requests.RequestConstraint.ConstraintType;
import com.raytheon.uf.common.dataquery.responses.DbQueryResponse;
import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.common.util.CollectionUtil;
import com.raytheon.uf.viz.core.catalog.CatalogQuery;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.requests.ThriftClient;

import gov.noaa.nws.ncep.common.dataplugin.mcidas.McidasConstants;
import gov.noaa.nws.ncep.common.dataplugin.pgen.PgenRecord;
import gov.noaa.nws.ncep.viz.common.display.NcDisplayType;
import gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsRequestableResourceData;
import gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsRequestableResourceData.DayReference;
import gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsRequestableResourceData.TimeMatchMethod;
import gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsRequestableResourceData.TimelineGenMethod;
import gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsResourceData;
import gov.noaa.nws.ncep.viz.resources.attributes.ResourceExtPointMngr;
import gov.noaa.nws.ncep.viz.resources.attributes.ResourceExtPointMngr.ResourceParamInfo;
import gov.noaa.nws.ncep.viz.resources.attributes.ResourceExtPointMngr.ResourceParamType;

/**
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 *                            Greg Hull   Created
 *  02/26/11      #408        Greg Hull   add filterable labels, rm isForecast, isEvent
 *  03/02/11      #408        Greg Hull   add definitionIndex
 *  07/18/11      #450        Greg Hull   marshal rscParameters.
 *  07/24/11      #450        Greg Hull   save LocalizationFile
 *  11/15/11                  Greg Hull   rm attrSetGroupNames
 *  01/20/12      #606        Greg Hull   query from NcInventory, rm types/sub-types lists
 *  04/23/12      #606        Greg Hull   allow NcInventory to be disabled and query/update
 *                                        for generated sub/types.
 *  05/27/12      #606        Greg Hull   createNcInventoryDefinition()
 *  05/31/12      #606        Greg Hull   save the name/alias of the inventory to query 
 *  06/05/12      #816        Greg Hull   rm definitionIndex
 *  08/29/12      #556        Greg Hull   check isRequestable() before adding as an AlertObserver (for PGEN)
 *  09/01/12      #860        Greg Hull   Add smarter caching (for all constraints) of available times.
 *  09/05/12      #860        Greg Hull   Add this to the URICatalog for storing the latest time.
 *  09/13/12      #860        Greg Hull   set default for inventoryEnabled to false.
 *  11/2012       #885        T. Lee      Set unmapped satellite projection resolution to "native"
 *  01/2013                   Greg Hull   Don't create wildcard inventory constraint
 *  02/2013       #972        Greg Hull   ResourceCategory class and supported display types
 *  03/2013       #972        Greg Hull   AttrSetsOrganization
 *  04/2013       #864        Greg Hull   mv filters and isEnabled
 *  04/2013       #838        B. Hebbard  Add special handling (like satellite) for NTRANS compound subType
 *  08/2013       #1031       Greg Hull   modified inventory query 
 *  11/2013       #1074       Greg Hull   fix bug generating native satellite sub-type
 *  05/15/2014    #1131       Quan Zhou   Added resource category GraphRscCategory. Added dfltGraphRange, dfltHourSnap
 *  06/2014                   B. Hebbard  Force getInventoryEnabled() to return false except for GRID & ENSEMBLE
 *                                        resources, and make all internal read accesses via getter
 *  07/2014       TTR1034+    J. Wu       Always query data time from DB, not from cache.
 *  07/2014       R4644       Y. Song     Added VIIRS as implementation
 *  11/2004       R4644       S. Gilbert  Removed dependency on specific PDOs and
 *                                        changed uEngine request to ThriftClinet
 *  12/24/14      R4508      S. Gurung    Add special handling for GHCD (generic high-cadence data) compound subType
 *  01/21/15      R4646      B. Yin       Handle PGEN resource group (subtype)
 *  05/14/15      R7656      A. Su        Retrieved the resource definitions of LocalRadar from an xml file.
 *  06/04/15      R7656      A. Su        Removed a debugging message when adding radar stations to generatedTypesList.
 *  08/17/15      R7755      J. Lopez     Moved isEnabled" flag  is moved to Resource Definitions
 *  08/21/2015    R7190      R. Reynolds  Modifications to handle ordering of GUI text associated with Mcidas data
 *  01/22/2016    R14142     R. Reynolds  Moved in mcidas specific aliasing code
 *  04/05/2016    RM10435    rjpeter      Removed Inventory usage.
 *  04/12/2016    R15945     R. Reynolds  Added code to build initial custom Mcidas legend string
 *  06/02/2016    RM18743    rjpeter      Fix Radar generatedTypes.
 *  06/06/2016    R15945     RCReynolds   Using McidasConstants instead of SatelliteConstants
 *  07/29/2016    R17936     mkean        null in legendString for unaliased satellite.
 *  11/08/16      5976       bsteffen     Update deprecated method calls.
 *  11/25/2016    R17956     E. Brown     Facilitate multiple subtype generators. Modification to queryGeneratedTypes(), and addition
 *                                        of methods getSubTypesForPgenRec(PgenRecord pgenRec) and 
 *                                        ResourceDefinition.appendSubType(StringBuilder builtString, String subType)
 * </pre>
 * 
 * @author ghull
 * @version 1
 */
@XmlRootElement(name = "ResourceDefinition")
@XmlAccessorType(XmlAccessType.NONE)
public class ResourceDefinition implements Comparable<ResourceDefinition> {

    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(ResourceDefinition.class);

    @XmlElement
    private String resourceDefnName;

    @XmlElement
    @XmlJavaTypeAdapter(ResourceCategory.ResourceCategoryAdapter.class)
    private ResourceCategory resourceCategory;

    private String localizationName; // the path

    private LocalizationFile localizationFile;

    /*
     * must match the name in an extension point which defines the java class
     * that implements the resource.
     */
    @XmlElement
    private String rscImplementation;

    // the name of a column in the DB used to generated dynamic Resources.
    // (Note: this must be defined as a parameter for the resource
    // implementation.)
    @XmlElement
    private String subTypeGenerator;

    // in the GUI this is edited in the rscType text widget but it is stored
    // here. (Note: this must be defined as a parameter for the resource
    // implementation.)
    @XmlElement
    private String rscTypeGenerator;

    // the resource types generated by rscTypeGenerator from the DB.
    private final ArrayList<String> generatedTypesList;

    private final ArrayList<String> generatedSubTypesList;

    // Time of last query
    private long lastTypeQueryCacheExpireTime;

    @XmlElement
    private TimeMatchMethod timeMatchMethod;

    @XmlElement
    private int frameSpan; // if 0 then the intervals are data-driven

    @XmlElement
    private TimelineGenMethod timelineGenMethod;

    private static final long TYPE_QUERY_CACHE_TIME = 60000;

    private static final int DEFAULT_FRAME_COUNT = 10;

    private static final int DEFAULT_GRAPH_RANGE = 12; // in hours quan

    private static final int DEFAULT_HOUR_SNAP = 3;

    private static final int DEFAULT_TIME_RANGE = 24; // in hours

    // define constant strings
    private static final String DELIMITER_COMMA = ",";

    private static final String DELIMITER_UNDERSCORE = "_";

    private static final String DELIMITER_COLON = ":";

    private static final String KILOMETER_UNIT = "km";

    private static final String BLANK_SPACE = " ";

    private static final String EMPTY_STRING = "";

    @XmlElement
    private int dfltFrameCount;

    // the default number of hours of data to make available in the timeline
    @XmlElement
    private int dfltTimeRange;

    @XmlElement
    private String dfltFrameTimes;

    @XmlElement
    private String cycleReference;

    @XmlElement
    private DayReference dayReference;

    @XmlElement
    private Boolean isEnabled = true;

    // the hours range in graph display.
    @XmlElement
    private int dfltGraphRange;

    // the default hour selection snap number. For example, 3 means hours must
    // be select in 0,3,6,9...
    @XmlElement
    private int dfltHourSnap;

    private AttributeSet attributeSet;

    // where/how are the attribute sets located/organized
    //

    public static enum AttrSetsOrganization {
        // GRIDS, RADAR... use ATTR SET GROUP
        BY_ATTR_SET_GROUP, BY_RSC_DEFN, DETERMINE_BY_RSC_CATEGORY
    }

    // Most will be the default which means we will use the resource
    // category to determine whether to use attr set groups, but this will
    // let a RD override the behaviour for a category if needed.
    @XmlElement
    private AttrSetsOrganization attrSetOrg = AttrSetsOrganization.DETERMINE_BY_RSC_CATEGORY;

    private List<NcDisplayType> applicableDisplayTypes;

    @XmlElement
    private String dfltGeogArea;

    // This does not include any parameters associated with subtypes or any in
    // the attributes file.
    // NOTE : Comments are kept in this map so that they can be preserved when
    // the user edits the params in the GUI. This are not returned by
    // getResourceParameters but it would be nice to have a cleaner way to
    // store the comments for a parameter.

    @XmlElement
    @XmlJavaTypeAdapter(RscParamsJaxBAdapter.class)
    private HashMap<String, String> resourceParameters;

    private boolean resourceParametersModified;

    private Boolean isRequestable = null; // based on class of the
                                          // implementations

    // Default to disabled so it must be explicitly enabled.

    // A map from the resource Constraints to a cache of the availableTimes and
    // the latest time. The availableTimes may come from an inventory query or
    // a DB query and the latest time may be determined from the latest time or
    // it may be updated via Raytheon's MenuUpdater via the URICatalog by
    // processing alert notifications.
    //
    private final ConcurrentMap<Map<String, RequestConstraint>, DataTimesCacheEntry> availTimesCache = new ConcurrentHashMap<>();

    // This variable is specific for the resource definition of LocalRadar.
    private static boolean isLocalRadarProcessed = false;

    // The name of resource definition for LocalRadar.
    private static String localRadarResourceDefnName = LocalRadarStationManager.ResourceDefnName;

    // Set this to true and store the latestTimes in the URICatalog.
    // Some RscDefns have a lot of possible constraints (i.e. radar and some
    // satellites...) which means that the inventory queries can get hit all
    // slight (1 second?) delay. In this case we will leverage Raytheon's
    // for the URI Notifications and stores the latest times.
    // The code can override this flag if false and still add the RD to the
    // Catalog if for example the DB query is too slow (i.e. > 2 seconds.)
    // This is currently only used for the latestTimes in the attr set list.
    // The actual times are still coming from the NcInventory or the DB.
    //

    @XmlElement
    private Boolean addToURICatalog = false;

    private String mcidasAliasedValues = EMPTY_STRING;

    public ResourceDefinition() {
        resourceDefnName = EMPTY_STRING;
        resourceCategory = ResourceCategory.NullCategory;
        subTypeGenerator = EMPTY_STRING;
        rscTypeGenerator = EMPTY_STRING;
        resourceParameters = new HashMap<>();
        resourceParametersModified = false;
        frameSpan = 0;
        dfltFrameCount = DEFAULT_FRAME_COUNT;
        dfltTimeRange = DEFAULT_TIME_RANGE;
        dfltGraphRange = DEFAULT_GRAPH_RANGE;
        dfltHourSnap = DEFAULT_HOUR_SNAP;
        dfltGeogArea = EMPTY_STRING;
        timeMatchMethod = TimeMatchMethod.CLOSEST_BEFORE_OR_AFTER;
        timelineGenMethod = TimelineGenMethod.USE_DATA_TIMES;
        isEnabled = true;

        generatedTypesList = new ArrayList<>();

        generatedSubTypesList = new ArrayList<>();
    }

    // shallow copy of the attrSetGroups and subTypes lists
    public ResourceDefinition(ResourceDefinition rscDefn) {

        resourceDefnName = rscDefn.getResourceDefnName();
        resourceCategory = rscDefn.resourceCategory;
        subTypeGenerator = rscDefn.getSubTypeGenerator();

        rscTypeGenerator = rscDefn.getRscTypeGenerator();

        resourceParameters = new HashMap<>(rscDefn.resourceParameters);
        resourceParametersModified = rscDefn.resourceParametersModified;

        frameSpan = rscDefn.frameSpan;
        dfltFrameCount = rscDefn.dfltFrameCount;
        dfltTimeRange = rscDefn.dfltTimeRange;
        dfltGraphRange = rscDefn.dfltGraphRange;
        dfltHourSnap = rscDefn.dfltHourSnap;
        timeMatchMethod = rscDefn.timeMatchMethod;
        timelineGenMethod = rscDefn.timelineGenMethod;

        dfltGeogArea = rscDefn.dfltGeogArea;
        rscImplementation = rscDefn.rscImplementation;

        setLocalizationFile(rscDefn.getLocalizationFile());
        isEnabled = rscDefn.isEnabled;

        generatedTypesList = new ArrayList<>();

        generatedSubTypesList = new ArrayList<>();
    }

    public String getLocalizationName() {
        return localizationName;
    }

    public void setLocalizationName(String name) {
        this.localizationName = name;
    }

    public LocalizationFile getLocalizationFile() {
        return localizationFile;
    }

    public void setLocalizationFile(LocalizationFile lFile) {
        this.localizationFile = lFile;
        if (lFile != null) {
            setLocalizationName(lFile.getName());

        } else {
            localizationName = EMPTY_STRING;
        }
    }

    public String getSubTypeGenerator() {
        return subTypeGenerator;
    }

    public void setSubTypeGenerator(String subTypeGenerator) {
        this.subTypeGenerator = subTypeGenerator;
    }

    public String[] getSubTypeGenParamsList() {

        if ((subTypeGenerator == null) || subTypeGenerator.trim().isEmpty()) {
            return new String[0];
        }
        String prmsList[] = subTypeGenerator.split(DELIMITER_COMMA);
        for (int i = 0; i < prmsList.length; i++) {
            prmsList[i] = prmsList[i].trim();
        }
        return prmsList;
    }

    // TODO : Need to change this to return the constraint field instead of
    // the generator parameter. Till then all parameters that generate a type
    // or sub type must be the same name as the request constraint.

    public String getRscTypeGenerator() {
        return (rscTypeGenerator == null ? EMPTY_STRING : rscTypeGenerator);
    }

    public void setRscTypeGenerator(String rscTypeGenerator) {
        this.rscTypeGenerator = rscTypeGenerator;
    }

    protected String getConstraintNameForParam(String field) {
        String rval = field;

        HashMap<String, ResourceParamInfo> rscImplParams = ResourceExtPointMngr
                .getInstance().getResourceParameters(rscImplementation);
        ResourceParamInfo paramInfo = rscImplParams.get(field);
        if (paramInfo != null) {
            if (paramInfo.getConstraintName() != null) {
                rval = paramInfo.getConstraintName();
            }
        }
        return rval;
    }

    public String getResourceDefnName() {
        return resourceDefnName;
    }

    public Boolean getAddToURICatalog() {
        return addToURICatalog;
    }

    public void setAddToURICatalog(Boolean addToURICatalog) {
        this.addToURICatalog = addToURICatalog;
    }

    // resourceParameters includes the comments but don't return them.
    public HashMap<String, String> getResourceParameters(
            boolean includeDefaults) {
        HashMap<String, String> prmsWithoutComments = new HashMap<>();

        for (String prmName : resourceParameters.keySet()) {

            if (!prmName.trim().startsWith("!")) {
                prmsWithoutComments.put(prmName,
                        resourceParameters.get(prmName));
            }
        }
        if (includeDefaults) {
            // the default values specified in the extension point
            HashMap<String, String> dfltParamValues = getDefaultParameterValues();

            for (String dfltPrm : dfltParamValues.keySet()) {
                if (!prmsWithoutComments.containsKey(dfltPrm)) {
                    prmsWithoutComments.put(dfltPrm,
                            dfltParamValues.get(dfltPrm));
                }
            }
        }
        //
        return prmsWithoutComments;
    }

    // TODO : we might want to insert a 'blank' place holder for IMPLEMENTATION
    // or REQUEST_CONSTRAINT parameters that are defined by the implementation
    // but not specified here. This may happen to existing user RDs after a new
    // parameter is defined. (except we'd need to figure out what to do in the
    // case of parameters like imageType's that are normally specified in the
    // attribute sets.
    public String getResourceParametersAsString() {
        if (resourceParameters.isEmpty()) {
            return EMPTY_STRING;
        }

        StringBuffer strBuf = new StringBuffer();

        for (String prmName : resourceParameters.keySet()) {
            if (prmName.startsWith("!")) {
                strBuf.append(prmName + "\n");
            } else {
                strBuf.append(
                        prmName + "=" + resourceParameters.get(prmName) + "\n");
            }
        }
        return strBuf.toString();

    }

    public void setResourceParameters(HashMap<String, String> typeParams) {
        if (resourceParameters.isEmpty()
                || !resourceParameters.equals(typeParams)) {
            resourceParametersModified = true;
        }

        resourceParameters = new HashMap<>(typeParams);
    }

    public void setResourceParametersFromString(String prmsStr) {

        // sanity check that the keys are all the same.
        String[] prmStrs = prmsStr.split("\n");

        for (String prmStr : prmStrs) {
            if (prmStr.startsWith("!")) {
                resourceParameters.put(prmStr, prmStr);
            } else {
                int equalsIndx = prmStr.indexOf("=");
                String prmName = prmStr.substring(0, equalsIndx);
                String prmVal = prmStr.substring(equalsIndx + 1,
                        prmStr.length());
                resourceParameters.put(prmName.trim(), prmVal.trim());
            }
        }
    }

    public boolean getResourceParamsModified() {
        return resourceParametersModified;
    }

    public void setResourceParamsModified(boolean rscTypeParamsModified) {
        this.resourceParametersModified = rscTypeParamsModified;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setIsEnabled(Boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    public Boolean isDisplayTypeSupported(NcDisplayType dispType) {
        for (NcDisplayType dt : getSupportedDisplayTypes()) {
            if (dt == dispType) {
                return true;
            }
        }
        return false;
    }

    public NcDisplayType[] getSupportedDisplayTypes() {
        if (applicableDisplayTypes == null) {
            applicableDisplayTypes = new ArrayList<>();
        }

        Class<?> implClass = ResourceExtPointMngr.getInstance()
                .getResourceDataClass(rscImplementation);

        try {
            Object rscData = implClass.newInstance();
            if (rscData instanceof AbstractNatlCntrsRequestableResourceData) {
                return ((AbstractNatlCntrsRequestableResourceData) rscData)
                        .getSupportedDisplayTypes();
            } else if (rscData instanceof AbstractNatlCntrsResourceData) {
                return ((AbstractNatlCntrsResourceData) rscData)
                        .getSupportedDisplayTypes();
            } else {
                statusHandler.handle(Priority.PROBLEM, ("Rsc Impl "
                        + rscImplementation + "has non-NC resource class"));
            }
        } catch (InstantiationException e) {
            statusHandler
                    .handle(Priority.PROBLEM,
                            ("Error instantiating class (" + implClass.getName()
                                    + ") for resource: " + rscImplementation),
                            e);
        } catch (IllegalAccessException e) {
            statusHandler
                    .handle(Priority.PROBLEM,
                            ("Error instantiating class (" + implClass.getName()
                                    + ") for resource: " + rscImplementation),
                            e);
        }

        return new NcDisplayType[0];
    }

    public boolean isForecast() {
        // @formatter:off
        return ((timelineGenMethod == TimelineGenMethod.USE_CYCLE_TIME_FCST_HOURS)
                || (timelineGenMethod == TimelineGenMethod.USE_FCST_FRAME_INTERVAL_FROM_REF_TIME)
                || (timelineGenMethod == TimelineGenMethod.DETERMINE_FROM_RSC_IMPLEMENTATION));
        // @formatter:on

    }

    public ResourceCategory getResourceCategory() {
        return resourceCategory;
    }

    public void setResourceCategory(ResourceCategory rCat) {
        this.resourceCategory = rCat;
    }

    public void setResourceCategory(String rCatStr) {
        this.resourceCategory = ResourceCategory.getCategory(rCatStr);
    }

    public String getRscImplementation() {
        return rscImplementation;
    }

    public void setRscImplementation(String rscImplementation) {
        this.rscImplementation = rscImplementation;
    }

    public TimeMatchMethod getTimeMatchMethod() {
        return timeMatchMethod;
    }

    public void setTimeMatchMethod(TimeMatchMethod timeMatchMethod) {
        this.timeMatchMethod = timeMatchMethod;
    }

    public TimelineGenMethod getTimelineGenMethod() {
        return timelineGenMethod;
    }

    public void setTimelineGenMethod(TimelineGenMethod timelineGenMethod) {
        this.timelineGenMethod = timelineGenMethod;
    }

    public int getFrameSpan() {
        return frameSpan;
    }

    public void setFrameSpan(int frameSpan) {
        this.frameSpan = frameSpan;
    }

    public int getDfltFrameCount() {
        return dfltFrameCount;
    }

    public void setDfltFrameCount(int dfltFrameCount) {
        this.dfltFrameCount = dfltFrameCount;
    }

    public String getDfltFrameTimes() {
        return dfltFrameTimes;
    }

    public void setDfltFrameTimes(String dfltFrameTimes) {
        this.dfltFrameTimes = dfltFrameTimes;
    }

    public String getCycleReference() {
        return cycleReference;
    }

    public void setCycleReference(String cycleReference) {
        this.cycleReference = cycleReference;
    }

    public DayReference getDayReference() {
        return dayReference;
    }

    public void setDayReference(DayReference dayReference) {
        this.dayReference = dayReference;
    }

    public int getDfltGraphRange() {
        return dfltGraphRange;
    }

    public void setDfltGraphRange(int dfltGraphRange) {
        this.dfltGraphRange = dfltGraphRange;
    }

    public int getDfltHourSnap() {
        return dfltHourSnap;
    }

    public void setDfltHourSnap(int dfltHourSnap) {
        this.dfltHourSnap = dfltHourSnap;
    }

    public int getDfltTimeRange() {
        return dfltTimeRange;
    }

    public void setDfltTimeRange(int dfltTimeRange) {
        this.dfltTimeRange = dfltTimeRange;
    }

    public String getDfltGeogArea() {
        return dfltGeogArea;
    }

    public void setDfltGeogArea(String dfltGeogArea) {
        this.dfltGeogArea = dfltGeogArea;
    }

    public void setResourceDefnName(String rName) {
        this.resourceDefnName = rName;
    }

    public boolean isPgenResource() {
        return resourceCategory == ResourceCategory.PGENRscCategory;
    }

    public AttrSetsOrganization getAttrSetOrg() {
        return attrSetOrg;
    }

    public void setAttrSetOrg(AttrSetsOrganization attrSetOrg) {
        this.attrSetOrg = attrSetOrg;
    }

    public boolean applyAttrSetGroups() {

        if (attrSetOrg == AttrSetsOrganization.DETERMINE_BY_RSC_CATEGORY) {
            return (resourceCategory == ResourceCategory.GridRscCategory)
                    || (resourceCategory == ResourceCategory.RadarRscCategory)
                    || (resourceCategory == ResourceCategory.GraphRscCategory)
                    || (resourceCategory == ResourceCategory.EnsembleRscCategory)
                    || (resourceCategory == ResourceCategory.SpaceRscCategory);
        } else if (attrSetOrg == AttrSetsOrganization.BY_ATTR_SET_GROUP) {
            return true;
        } else {
            return false;
        }
    }

    // the plugin must be given as a resource parameter
    public String getPluginName() {
        return getResourceParameters(false).get("pluginName");
    }

    public List<String> getUnconstrainedParameters() {
        HashMap<String, RequestConstraint> constrMap = getConstraintsFromParameters(
                getResourceParameters(true), true);
        List<String> unconstPrms = new ArrayList<>();

        for (String prm : constrMap.keySet()) {
            if (constrMap.get(prm) == RequestConstraint.WILDCARD) {
                unconstPrms.add(prm);
            }
        }
        // TODO : add dataTime here?
        return unconstPrms;
    }

    public HashMap<String, RequestConstraint> getConstraintsFromParameters(
            HashMap<String, String> paramValues) {
        return getConstraintsFromParameters(paramValues, false);
    }

    public HashMap<String, RequestConstraint> getConstraintsFromParameters(
            HashMap<String, String> paramValues, Boolean includeWildcard) {

        HashMap<String, RequestConstraint> constraints = new HashMap<>();

        constraints.put("pluginName", new RequestConstraint(getPluginName()));

        HashMap<String, ResourceParamInfo> rscImplParams = ResourceExtPointMngr
                .getInstance().getResourceParameters(rscImplementation);

        // Override the constraints in the inventory with those from the params.
        // If there is no param value for the constraint then change to a
        // wildcard.
        //
        // (Most of the time the paramName and constraint name are the same,
        // except for GDFILE where the constraint is 'modelName')
        // So for this case we need to [...?]
        //
        for (ResourceParamInfo prmInfo : rscImplParams.values()) {

            // if this parameter is defined as a request constraint and
            // if it has a non-wildcard value, then create a inventory
            // constraint for it
            //
            if (prmInfo
                    .getParamType() == ResourceParamType.REQUEST_CONSTRAINT) {

                String prmName = prmInfo.getParamName();
                String cnstrName = prmInfo.getConstraintName();

                if (paramValues.containsKey(prmName)) {
                    // if the constraint value is a wildcard then don't add this
                    // to the list since it will fail in the case where the
                    // DB value is a null.
                    RequestConstraint reqConstr = getConstraintFromParamValue(
                            paramValues.get(prmName));

                    if (includeWildcard
                            || (reqConstr != RequestConstraint.WILDCARD)) {

                        constraints.put(cnstrName, reqConstr);
                    }
                }
            }
        }
        return constraints;
    }

    public RequestConstraint getConstraintFromParamValue(String paramVal) {

        if (paramVal == null) {
            statusHandler.handle(Priority.INFO, "BAD paramVal");

            paramVal = BLANK_SPACE;
            return null;
        }

        if (paramVal.equals("%")) {
            return RequestConstraint.WILDCARD;
        } else if (paramVal.indexOf("%") != -1) {
            return new RequestConstraint(paramVal, ConstraintType.LIKE);
        } else if (paramVal.indexOf(DELIMITER_COMMA) == -1) {
            return new RequestConstraint(paramVal, ConstraintType.EQUALS);
        } else {
            return new RequestConstraint(paramVal, ConstraintType.IN);
        }
        // TODO : do we need a syntax for 'BETWEEN'
    }

    // based of the base class of the resource implementation
    public boolean isRequestable() {
        if ((isRequestable == null) && (rscImplementation != null)) {
            isRequestable = false;

            Class<?> implClass = ResourceExtPointMngr.getInstance()
                    .getResourceDataClass(rscImplementation);

            try {
                Object rscData = implClass.newInstance();
                isRequestable = (rscData instanceof AbstractNatlCntrsRequestableResourceData);

            } catch (InstantiationException e) {
                statusHandler.handle(Priority.PROBLEM,
                        ("Error instantiating class (" + implClass.getName()
                                + ") for resource: " + rscImplementation),
                        e);

            } catch (IllegalAccessException e) {
                statusHandler.handle(Priority.PROBLEM,
                        ("Error instantiating class (" + implClass.getName()
                                + ") for resource: " + rscImplementation),
                        e);
            }
        }
        return (isRequestable == null ? false : isRequestable);
    }

    public HashMap<String, String> getDefaultParameterValues() {
        HashMap<String, String> dfltParamValues = new HashMap<>();
        HashMap<String, ResourceParamInfo> rscImplParams = ResourceExtPointMngr
                .getInstance().getResourceParameters(getRscImplementation());

        for (ResourceParamInfo prmInfo : rscImplParams.values()) {
            String dfltVal = prmInfo.getDefaultValue();

            if ((dfltVal != null) && !dfltVal.isEmpty()) {
                dfltParamValues.put(prmInfo.getParamName(), dfltVal);
            }
        }

        return dfltParamValues;
    }

    public void validateResourceParameters() throws VizException {
        HashMap<String, ResourceParamInfo> rscImplParams = ResourceExtPointMngr
                .getInstance().getResourceParameters(getRscImplementation());

        // the parameters defined by the resource definition and use the
        // defaults specified by the implementation is not given.
        //
        HashMap<String, String> paramValues = getResourceParameters(true);

        // sanity check on timelineGen
        if ((rscImplementation == "ModelFcstGridContours")
                && (timelineGenMethod != TimelineGenMethod.USE_CYCLE_TIME_FCST_HOURS)) {
            statusHandler.handle(Priority.PROBLEM,
                    ("Sanity Check: GRID rsc has a non-forecast timelineGenMethod???"));
        }

        // check that all of the paramValues are specified for the rsc
        // implementation

        for (String prm : paramValues.keySet()) {

            if (!prm.equals("pluginName") && !rscImplParams.containsKey(prm)) {

                throw new VizException(
                        getResourceDefnName() + " has a parameter, " + prm
                                + ", that is not recognized by its implementation: "
                                + getRscImplementation());
            }
        }

        // a list of all the generated parameters
        List<String> genParamsList = new ArrayList<>(
                Arrays.asList(getSubTypeGenParamsList()));
        if (!getRscTypeGenerator().isEmpty()) {
            genParamsList.add(getRscTypeGenerator());
        }

        // check that all the parameters defined for the implementation either
        // have a value given in the rsc params, will be generated, or have a
        // default value
        for (ResourceParamInfo implPrmInfo : rscImplParams.values()) {
            String implPrm = implPrmInfo.getParamName();
            String constraintName = implPrmInfo.getConstraintName();

            if ((implPrmInfo
                    .getParamType() == ResourceParamType.EDITABLE_ATTRIBUTE)
                    || (implPrmInfo
                            .getParamType() == ResourceParamType.NON_EDITABLE_ATTRIBUTE)) {

                // if checking for attributes...
                continue;
            } else if (implPrmInfo
                    .getParamType() == ResourceParamType.REQUEST_CONSTRAINT) {

                // if this param will be generated.
                if (genParamsList.contains(constraintName)) {

                    continue;
                } else {

                    // if the needed param is not set in the resource defn or is
                    // set to empty

                    String paramValue = paramValues.get(implPrm);

                    if ((paramValue == null) || paramValue.isEmpty()) {

                        // paramValue = dfltParamValues.get( implPrm );
                        // if there is no default value specified by the
                        // implementation

                        throw new VizException(getResourceDefnName()
                                + " is missing a value for the parameter "
                                + implPrm + ".");

                    }
                }
            }
        }
    }

    public ArrayList<String> getGeneratedTypesList() throws VizException {
        queryGeneratedTypes();
        return generatedTypesList;
    }

    //
    public ArrayList<String> generatedSubTypesList() throws VizException {
        queryGeneratedTypes();
        return generatedSubTypesList;
    }

    public List<DataTime> getNormalizedDataTimes(ResourceName rscName,
            int intervalMins) throws VizException {
        List<DataTime> dataTimes = getDataTimes(rscName);
        ArrayList<DataTime> normalizedDataTimes = new ArrayList<>();

        for (DataTime dt : dataTimes) {
            long intervalMillis = intervalMins * 1000 * 60; // minutes to
                                                            // Milliseconds
            long millis = dt.getValidTime().getTimeInMillis()
                    + (intervalMillis / 2);
            millis = ((millis / intervalMillis) * intervalMillis);
            DataTime normalizedTime = new DataTime(new Date(millis));

            if (!normalizedDataTimes.contains(normalizedTime)) {
                normalizedDataTimes.add(normalizedTime);
            }
        }

        return normalizedDataTimes;
    }

    private DataTimesCacheEntry addTimesCacheEntry(
            Map<String, RequestConstraint> resourceConstraints) {
        DataTimesCacheEntry cacheEntry = availTimesCache
                .get(resourceConstraints);
        if (cacheEntry == null) {
            availTimesCache.putIfAbsent(resourceConstraints,
                    new DataTimesCacheEntry());
            cacheEntry = availTimesCache.get(resourceConstraints);
        }

        return cacheEntry;
    }

    // Return the latest time or if there either is NoData or if the time
    // hasn't been set yet, return a Null DataTime.

    public DataTime getLatestDataTime(ResourceName rscName)
            throws VizException {
        if (!isRequestable()) {
            return null;
        }

        Map<String, RequestConstraint> resourceConstraints = getConstraintsFromParameters(
                ResourceDefnsMngr.getInstance()
                        .getAllResourceParameters(rscName));

        /*
         * if times are cached for these constraints, and if the times haven't
         * expired, then just return the cached times.
         */
        DataTimesCacheEntry cacheEntry = addTimesCacheEntry(
                resourceConstraints);
        DataTime latestTime = cacheEntry.getLatestTime();

        if (latestTime == null) {
            /* Get just the latest time only */
            DataTime[] dataTimeArr = CatalogQuery
                    .performTimeQuery(resourceConstraints, true, null);
            if (!CollectionUtil.isNullOrEmpty(dataTimeArr)) {
                latestTime = dataTimeArr[0];
                cacheEntry.setLatestTime(latestTime);
            }
        }

        // if 'not set' still return a 'Null' dataTime.
        return (latestTime == null ? new DataTime(new Date(0)) : latestTime);
    }

    // update this to optionally either return all times or only matching cycle
    // times
    public List<DataTime> getDataTimes(ResourceName rscName)
            throws VizException {
        Map<String, RequestConstraint> resourceConstraints = getConstraintsFromParameters(
                ResourceDefnsMngr.getInstance()
                        .getAllResourceParameters(rscName));

        DataTimesCacheEntry cacheEntry = addTimesCacheEntry(
                resourceConstraints);
        List<DataTime> rval = cacheEntry.getAvailableTimes();
        if (rval == null) {
            try {
                DataTime[] dataTimeArr = CatalogQuery
                        .performTimeQuery(resourceConstraints, false, null);
                Arrays.sort(dataTimeArr);
                rval = Arrays.asList(dataTimeArr);
                cacheEntry.setAvailableTimes(rval);
            } catch (VizException e) {
                throw new VizException("Inventory DataTime Query Failed for "
                        + resourceDefnName, e);
            }
        }
        return rval;
    }

    public void dispose() {

        generatedSubTypesList.clear();
        generatedTypesList.clear();

        // clean up the availTimesCache
        // (note that if the cache is being updated with latest times from
        // the URI catalog, the DataTimesCacheEntry objects may stick around and
        // still get updated. But we can't remove them and it won't hurt
        // anything
        for (Map<String, RequestConstraint> rCon : availTimesCache.keySet()) {
            availTimesCache.get(rCon).clearCache();
        }
        availTimesCache.clear();

    }

    protected void queryGeneratedTypes() throws VizException {

        // Determine whether or not to use cache and bail out of this method
        if (!isRequestable() || (System
                .currentTimeMillis() < lastTypeQueryCacheExpireTime)) {
            return;
        }

        // R7656: Special case for LocalRadar:
        // Retrieve the station IDs of radar stations from an XML file,
        // bypassing its original CatalogQuery.performQuery.

        if (localRadarResourceDefnName.equals(getResourceDefnName())) {
            // LocalRadar - looping through the station IDs
            if (!isLocalRadarProcessed) {
                generatedTypesList.clear();
                List<String> stationIDs = LocalRadarStationManager.getInstance()
                        .getStationIDs();

                if (stationIDs != null) {
                    generatedTypesList.addAll(stationIDs);
                }

                isLocalRadarProcessed = true;
            }
            return;
        }

        generatedSubTypesList.clear();
        generatedTypesList.clear();

        try {
            // the parameters used to define the data that gets stored in the
            // inventory. this will be all the request constraint params
            // that don't have a specified value for this resource. This
            // will be any type or sub-type generating params and any
            // parameters that are specified in an attribute set. (ex. radar
            // product codes and satellite imageTypes.)

            HashMap<String, RequestConstraint> requestConstraints = getConstraintsFromParameters(
                    getResourceParameters(false));
            String rscTypeGenerator = getRscTypeGenerator();

            if (!rscTypeGenerator.isEmpty()) {

                String genTypesRslts[] = CatalogQuery.performQuery(
                        getConstraintNameForParam(rscTypeGenerator),
                        requestConstraints);

                for (String genType : genTypesRslts) {
                    genType = getResourceDefnName() + DELIMITER_COLON + genType;

                    if (!generatedTypesList.contains(genType)) {
                        generatedTypesList.add(genType);
                    }
                }
            } else if (getSubTypeGenParamsList().length > 0) {
                String[] subParams = getSubTypeGenParamsList();
                String areaId = EMPTY_STRING;
                Integer resolution = 0;
                String projection = EMPTY_STRING;
                String satelliteId = EMPTY_STRING;

                DbQueryRequest request = new DbQueryRequest(requestConstraints);

                if (!(getResourceCategory() == ResourceCategory.PGENRscCategory)) {
                    request.addFields(subParams);
                    request.setDistinct(true);
                }

                DbQueryResponse response = (DbQueryResponse) ThriftClient
                        .sendRequest(request);

                for (Map<String, Object> result : response.getResults()) {

                    String subType = null;
                    StringBuilder sb = new StringBuilder();

                    if (getRscImplementation()
                            .equals(McidasConstants.MCIDAS_SATELLITE)) {

                        SatelliteAreaManager.getInstance()
                                .setSubParams(subParams);

                        areaId = (String) result.get("areaId");
                        // if (areaId.contains("335"))
                        // continue; // water vapor
                        resolution = (Integer) result
                                .get(McidasConstants.RESOLUTION);
                        projection = (String) result
                                .get(McidasConstants.PROJECTION);
                        satelliteId = (String) result
                                .get(McidasConstants.SATELLITE_ID);

                        SatelliteNameManager.getInstance()
                                .setSatelliteId(satelliteId);

                        SatelliteAreaManager
                                .getInstance().satelliteId = satelliteId;

                        SatelliteAreaManager
                                .getInstance().satelliteName = SatelliteNameManager
                                        .getInstance()
                                        .getDisplayedNameByID(satelliteId);

                        for (String subParam : subParams) {

                            if (subParam.equalsIgnoreCase(
                                    McidasConstants.RESOLUTION)) {
                                if (resolution == 0) {
                                    sb.append("native");
                                } else {
                                    sb.append(resolution.toString());

                                }
                                sb.append('_');

                            } else if (subParam.equalsIgnoreCase(
                                    McidasConstants.AREA_ID)) {
                                sb.append(areaId);
                                sb.append('_');
                            } else if (subParam.equalsIgnoreCase(
                                    McidasConstants.PROJECTION)) {
                                sb.append(projection);
                                sb.append('_');
                            } else if (subParam.equalsIgnoreCase(
                                    McidasConstants.SATELLITE_ID)) {
                                sb.append(satelliteId);
                                sb.append('_');
                            }
                        }
                        sb.delete(sb.length() - 1, sb.length());

                    } else {

                        for (String param : subParams) {
                            if (sb.length() == 0) {
                                sb.append(result.get(param));
                            } else {
                                sb.append('_');
                                sb.append(result.get(param));
                            }

                        }
                    }

                    // While looping through DB records in queryGenTypes found
                    // that resource category is PGEN
                    if (getResourceCategory() == ResourceCategory.PGENRscCategory) {

                        // Build the subTypes string using the PgenRecord
                        for (Object pdo : result.values()) {
                            PgenRecord pgenRec = (PgenRecord) pdo;

                            // Get the subType string using the pgenRec
                            subType = getSubTypesForPgenRec(pgenRec);
                        }

                        if ((subType != null) && !subType.trim().isEmpty()) {
                            if (!generatedSubTypesList.contains(subType)) {
                                generatedSubTypesList.add(subType);
                            }
                        }
                    } else {

                        subType = sb.toString();
                        if ((subType != null) && !subType.trim().isEmpty()) {
                            if (!generatedSubTypesList.contains(subType)) {
                                generatedSubTypesList.add(subType);
                            }

                        }
                    }

                }

            }
        } catch (VizException e) {
            // if this is a stack trace we don't need to display the whole stack
            if (e.getMessage().length() > 200) {
                statusHandler.error("Error in queryGeneratedTypes", e);
                e = new VizException(e.getClass().getName()
                        + " exception in queryGeneratedTypes ");
            }

            throw e;
        } catch (Exception e) {
            throw new VizException(e);
        }

        lastTypeQueryCacheExpireTime = System.currentTimeMillis()
                + TYPE_QUERY_CACHE_TIME;
    }

    // Using the PgenRecord passed in, use the subTypeGenerator (e.g.
    // "activityLabel, site, forecaster") to build a subType string (e.g.
    // 10-Volcano.21112016.08.xml OAX jsmith) and return it
    private String getSubTypesForPgenRec(PgenRecord pgenRec) {
        StringBuilder subType = new StringBuilder();
        String stringToAppend = "";

        // Breaks up the subTypeGenerator string into an array
        String[] subTypeGenerators = getSubTypeGenParamsList();

        // Looping through the generators and and building the subType string
        // using the PgenRec i.e. the subTypeGenerators are activityLabel, site,
        // forecaster and it
        // builds the subType string "14-CCFP.21112016.08.xml OAX jsmith"
        // Note: I could catch if there's a blank parameter below
        for (String str : subTypeGenerators) {

            // Note: DataTime is not a field of Pgen, will cause errors, so skip
            // it
            stringToAppend = "";
            if (str.indexOf("activitySubtype") > -1) {
                stringToAppend = pgenRec.getActivitySubtype();
            } else if (str.indexOf("activityLabel") > -1) {
                stringToAppend = pgenRec.getActivityLabel();
            } else if (str.indexOf("activityName") > -1) {
                stringToAppend = pgenRec.getActivityName();
            } else if (str.indexOf("activityType") > -1) {
                stringToAppend = pgenRec.getActivityType();
            } else if (str.indexOf("site") > -1) {
                stringToAppend = pgenRec.getSite();
            } else if (str.indexOf("desk") > -1) {
                stringToAppend = pgenRec.getDesk();
            } else if (str.indexOf("forecaster") > -1) {
                stringToAppend = pgenRec.getForecaster();
            } else { // If none of the above were found in the subTypeGenerator
                     // then:
                statusHandler.handle(Priority.INFO,
                        ("Unrecognized PGEN rsc generating subType"
                                + subTypeGenerator));
            }
            subType = appendSubType(subType, stringToAppend);
        }

        return subType.toString();
    }

    // Used by getSubTypesForPgenRec(). Appends a string to the end of the
    // current subType string
    private StringBuilder appendSubType(StringBuilder builtString,
            String subType) {

        if (builtString.length() > 0) {
            // builtString already has at least one subType in it, put a space
            // between it and the next one
            builtString.append(" ");
        }

        builtString.append(subType);

        return builtString;
    }

    @Override
    public int compareTo(ResourceDefinition o) {
        return this.getResourceDefnName()
                .compareToIgnoreCase(o.getResourceDefnName());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((resourceDefnName == null) ? 0
                : resourceDefnName.hashCode());
        result = (prime * result) + ((resourceParameters == null) ? 0
                : resourceParameters.hashCode());
        result = (prime * result) + ((rscImplementation == null) ? 0
                : rscImplementation.hashCode());
        result = (prime * result) + ((rscTypeGenerator == null) ? 0
                : rscTypeGenerator.hashCode());
        result = (prime * result) + ((subTypeGenerator == null) ? 0
                : subTypeGenerator.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ResourceDefinition other = (ResourceDefinition) obj;
        if (applicableDisplayTypes == null) {
            if (other.applicableDisplayTypes != null) {
                return false;
            }
        } else if (!applicableDisplayTypes
                .equals(other.applicableDisplayTypes)) {
            return false;
        }
        if (attrSetOrg != other.attrSetOrg) {
            return false;
        }
        if (dfltFrameCount != other.dfltFrameCount) {
            return false;
        }
        if (dfltTimeRange != other.dfltTimeRange) {
            return false;
        }
        if (dfltGraphRange != other.dfltGraphRange) {
            return false;
        }
        if (dfltHourSnap != other.dfltHourSnap) {
            return false;
        }
        if (frameSpan != other.frameSpan) {
            return false;
        }
        if (resourceCategory.getCategoryName() == null) {
            if (other.resourceCategory.getCategoryName() != null) {
                return false;
            }
        } else if (!resourceCategory.getCategoryName()
                .equals(other.resourceCategory.getCategoryName())) {
            return false;
        }
        if (resourceDefnName == null) {
            if (other.resourceDefnName != null) {
                return false;
            }
        } else if (!resourceDefnName.equals(other.resourceDefnName)) {
            return false;
        }
        if (resourceParameters == null) {
            if (other.resourceParameters != null) {
                return false;
            }
        } else if (!resourceParameters.equals(other.resourceParameters)) {
            return false;
        }
        if (rscImplementation == null) {
            if (other.rscImplementation != null) {
                return false;
            }
        } else if (!rscImplementation.equals(other.rscImplementation)) {
            return false;
        }
        if (rscTypeGenerator == null) {
            if (other.rscTypeGenerator != null) {
                return false;
            }
        } else if (!rscTypeGenerator.equals(other.rscTypeGenerator)) {
            return false;
        }
        if (subTypeGenerator == null) {
            if (other.subTypeGenerator != null) {
                return false;
            }
        } else if (!subTypeGenerator.equals(other.subTypeGenerator)) {
            return false;
        }
        if (timeMatchMethod != other.timeMatchMethod) {
            return false;
        }
        if (timelineGenMethod != other.timelineGenMethod) {
            return false;
        }
        return true;
    }

    /**
     * This is a MciDAS specific method to build the aliased Resource Group
     * string by appending from 2 to 4 subTypes as specified in a Satellite
     * Resource Definition XML file
     */
    public String getRscGroupDisplayName(String originalDisplayName) {

        String displayName = originalDisplayName;

        if (getRscImplementation().equals(McidasConstants.MCIDAS_SATELLITE)) {

            mcidasAliasedValues = EMPTY_STRING;

            String[] subParams = getSubTypeGenerator().split(DELIMITER_COMMA);
            String[] str = originalDisplayName.split(DELIMITER_UNDERSCORE);

            try {

                /*
                 * iterate through corresponding subTypes, only if the number of
                 * given values are equal
                 */
                if (subParams.length == str.length) {
                    displayName = iterateSubTypes(subParams, str);
                }

            } catch (Exception ex) {

                /**
                 * TODO: Temporary DEBUG for error that only happens on NTBN
                 * Remove System.out.println's if this error goes away and
                 * replace with statusHandler/ex.getStackTrace(). System.out
                 * will go to log, currently.
                 */
                ex.printStackTrace();
                System.out.println("*subParms[] length =" + subParams.length);
                for (int k = 0; k < subParams.length; k++) {
                    System.out.println("subParams[" + k + "] = "
                            + subParams[k].toString());
                }
                System.out.println("*displayName.length =" + str.length);
                for (int k = 0; k < str.length; k++) {
                    System.out.println(
                            "displayName[" + k + "] = " + str[k].toString());
                }
                displayName = originalDisplayName;
            }
        }

        return displayName.trim();
    }

    /**
     * This is a MciDAS specific method to construct a subType String from the
     * given parameter array and subType value array.
     */
    public String iterateSubTypes(String[] subParams, String[] str) {

        String displayName = EMPTY_STRING;
        for (int k = 0; k < subParams.length; k++) {

            if (subParams[k].toString()
                    .equalsIgnoreCase(McidasConstants.RESOLUTION)) {

                if (!subParams[k].contains(KILOMETER_UNIT)) {
                    str[k] += KILOMETER_UNIT;
                }
                displayName = displayName + BLANK_SPACE + str[k];
                mcidasAliasedValues = mcidasAliasedValues
                        + McidasConstants.RESOLUTION + DELIMITER_COLON + str[k]
                        + DELIMITER_COMMA;

            } else if (subParams[k].toString()
                    .equalsIgnoreCase(McidasConstants.PROJECTION)) {

                displayName = displayName + BLANK_SPACE + str[k];

            } else if (subParams[k].toString()
                    .equalsIgnoreCase(McidasConstants.AREA_ID)) {

                SatelliteAreaManager satAreaMgr = SatelliteAreaManager
                        .getInstance();

                String areaIdName = satAreaMgr
                        .getDisplayedName(SatelliteAreaManager.ResourceDefnName
                                + SatelliteAreaManager.delimiter
                                + str[k].toString());

                if (areaIdName == null) {
                    areaIdName = str[k].toString();
                }

                displayName = displayName + BLANK_SPACE + areaIdName;
                mcidasAliasedValues = mcidasAliasedValues + McidasConstants.AREA
                        + DELIMITER_COLON + areaIdName + DELIMITER_COMMA;

            } else if (subParams[k].toString()
                    .equalsIgnoreCase(McidasConstants.SATELLITE_ID)) {

                String value = SatelliteNameManager.getInstance()
                        .getDisplayedNameByID(str[k]);

                // prevent nulls in legend string
                if (value == null) {

                    // alias mapping missing
                    value = "unknown";

                    statusHandler.handle(Priority.INFO,
                            "subType value for SatelliteName is null");
                }
                displayName += BLANK_SPACE + value;

                mcidasAliasedValues = mcidasAliasedValues
                        + McidasConstants.SATELLLITE + DELIMITER_COLON + value
                        + DELIMITER_COMMA;
            }
        }

        mcidasAliasedValues = mcidasAliasedValues.substring(0,
                mcidasAliasedValues.length() - 1);

        return displayName;
    }

    /**
     * This is a MciDAS specific method to alias an attribute. The alias will be
     * displayed in the Resource Attribute column and appended to the Resource
     * Group name (DisplayName) which appears in the legend and Dialog Box
     * invoked by the Edit Button on CreateRBD tab.
     */

    public String getRscAttributeDisplayName(String originalAttrSetName) {

        String attrSetName = originalAttrSetName;

        if (getRscImplementation().equals(McidasConstants.MCIDAS_SATELLITE)) {

            String satName = attributeSet.getApplicableResource();
            String satId = EMPTY_STRING;

            HashMap<String, String> resParm = getResourceParameters(true);

            satId = resParm.get(McidasConstants.SATELLITE_ID);

            HashMap<String, String> atrSet = attributeSet.getAttributes();

            if ((satName == null) || (satId == null)) {
                return originalAttrSetName;
            }

            SatelliteImageTypeManager satImMan = SatelliteImageTypeManager
                    .getInstance();

            if (!atrSet.containsKey(McidasConstants.IMAGE_TYPE_ID
                    + McidasConstants.CUSTOM_NAME)) {

                attrSetName = atrSet.get(McidasConstants.IMAGE_TYPE_ID);

                if (attrSetName == null) {
                    return originalAttrSetName;
                }

                atrSet.put(McidasConstants.IMAGE_TYPE_ID
                        + McidasConstants.CUSTOM_NAME, attrSetName);

                atrSet.put(McidasConstants.IMAGE_TYPE_ID, satImMan
                        .getImageId_using_satId_and_ASname(satId, attrSetName));

            }

            attrSetName = atrSet.get(McidasConstants.IMAGE_TYPE_ID
                    + McidasConstants.CUSTOM_NAME);

            satImMan.setAttributeNamesAndAliases(
                    satId + DELIMITER_COLON + attributeSet.getName().toString(),
                    attrSetName);

        }

        return attrSetName.trim();
    }

    public String getMcidasAliasedValues() {
        return mcidasAliasedValues;
    }

    /**
     * This is the attribute set used by the updateAttrSetName() method
     */
    public void setAttributeSet(AttributeSet attributeSet) {
        this.attributeSet = attributeSet;
    }
}
