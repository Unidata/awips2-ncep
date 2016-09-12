package gov.noaa.nws.ncep.viz.rsc.plotdata.plotModels;

import gov.noaa.nws.ncep.common.tools.IDecoderConstantsN;
import gov.noaa.nws.ncep.edex.common.metparameters.AbstractMetParameter;
import gov.noaa.nws.ncep.edex.common.metparameters.Amount;
import gov.noaa.nws.ncep.edex.common.metparameters.MetParameterFactory;
import gov.noaa.nws.ncep.edex.common.metparameters.MetParameterFactory.NotDerivableException;
import gov.noaa.nws.ncep.edex.common.metparameters.PrecipitableWaterForEntireSounding;
import gov.noaa.nws.ncep.edex.common.metparameters.PressChange3Hr;
import gov.noaa.nws.ncep.edex.common.metparameters.PressureChange3HrAndTendency;
import gov.noaa.nws.ncep.edex.common.metparameters.StationID;
import gov.noaa.nws.ncep.edex.common.metparameters.StationLatitude;
import gov.noaa.nws.ncep.edex.common.metparameters.StationLongitude;
import gov.noaa.nws.ncep.edex.common.metparameters.StationNumber;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingCube;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingCube.QueryStatus;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingLayer2;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingProfile;
import gov.noaa.nws.ncep.viz.rsc.plotdata.conditionalfilter.ConditionalFilter;
import gov.noaa.nws.ncep.viz.rsc.plotdata.parameters.PlotParameterDefn;
import gov.noaa.nws.ncep.viz.rsc.plotdata.parameters.PlotParameterDefns;
import gov.noaa.nws.ncep.viz.rsc.plotdata.parameters.PlotParameterDefnsMngr;
import gov.noaa.nws.ncep.viz.rsc.plotdata.plotModels.elements.PlotModel;
import gov.noaa.nws.ncep.viz.rsc.plotdata.plotModels.elements.PlotModelElement;
import gov.noaa.nws.ncep.viz.rsc.plotdata.pluginplotproperties.PluginPlotProperties;
import gov.noaa.nws.ncep.viz.rsc.plotdata.queue.QueueEntry;
import gov.noaa.nws.ncep.viz.rsc.plotdata.rsc.Station;
import gov.noaa.nws.ncep.viz.rsc.plotdata.rsc.Tracer;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

import javax.measure.unit.NonSI;
import javax.measure.unit.SI;

import com.raytheon.uf.common.dataquery.requests.RequestConstraint;
import com.raytheon.uf.common.dataquery.requests.RequestConstraint.ConstraintType;
import com.raytheon.uf.common.inventory.exception.DataCubeException;
import com.raytheon.uf.common.pointdata.ParameterDescription;
import com.raytheon.uf.common.pointdata.PointDataContainer;
import com.raytheon.uf.common.pointdata.PointDataDescription.Type;
import com.raytheon.uf.common.pointdata.PointDataView;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.common.time.DataTime.FLAG;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.jobs.JobPool;
import com.raytheon.uf.viz.datacube.DataCubeContainer;
import com.raytheon.viz.pointdata.PointDataRequest;

/**
 * 
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#     Engineer       Description
 * ------------ ---------- ----------- --------------------------
 * 05/20/2013??   988        Archana.S    Initial creation.
 * 02/26/2014    1061        B. Hebbard   Don't block on JobPool cancel, so CAVE doesn't freeze if resource unloaded during long retrieval
 * 04/01/2014    1040        B. Hebbard   In requestUpperAirData, (1) clear displayStationPlotBoolList for each new station, (2) call cond filter check with newInstance vs. metPrm
 * 04/08/2014    1127        B. Hebbard   In requestSurfaceData, exclude only those obs returned from HDF5 that don't match desired time; fix dataTime association;
 *                                        removed redundant datatimes from constraint.
 * 06/17/2014     923        S. Russell   TTR 923, altered methods addToDerivedParamsList(), requestSurfaceData(), and newInstance()
 * 07/08/2014 TTR1028        B. Hebbard   In requestSurfaceData() and requestUpperAirData(), prune out stations that already have all met params they need, to avoid unnecessary querying
 * Aug 07, 2014  3478        bclement     removed PointDataDescription.Type.Double
 * 09/04/2014    1127        B. Hebbard   Exempt forecast (e.g., MOS) datatimes from check in requestSurfaceData that sees if retrieved value matches desired time.  This is because we retrieve only the refTime from datastore for comparison, which is sufficient for obs times, but not those with forecast component.
 * 12/04/2014   R5437        B. Hebbard   In addToDerivedParamsList(..), correct logic to determine (additional) base DB params needed for derived params; minor cleanups
 * 07/14/2015   R9173        Chin Chen    Use NcSoundingQuery.genericSoundingDataQuery() to query uair and modelsounding data
 * 08/31/2015   R7757        B. Hebbard   Refactor so imageCreator belongs to resource (instead of this class) for better frame status tracking; other cleanups.
 * 11/17/2015   R9579        B. Hebbard   Fix synchronization problem (affecting conditional coloring) in requestSurfaceData; various cleanups
 * 12/17/2015   R9579        B. Hebbard   Guard against ConcurrentModificationException (parametersToPlot map) and NullPointerException (stationsWithData)
 * 07/20/2016   R15950       J. Huber     Add support for temp and dewpoint reported with tenths.
 */

public class NcPlotDataRequestor {

    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(NcPlotDataRequestor.class);

    private Map<String, RequestConstraint> constraintMap;

    private String[] namesOfParametersToQuery = null;

    private String plugin;

    private String levelStr;

    // A map from the metParam name to an AbstractMetParameter. The met
    // parameter will have a value set either from the pointDataContainer query
    // or derived from the DB pointData. This map is passed to the
    // PlotModelFactory.
    //
    private HashMap<String, AbstractMetParameter> parametersToPlot = null;

    // A map from the dbParam name to a list of AbstractMetParameter objects
    // used to hold the values from the pointDataContainer.
    //
    private HashMap<String, AbstractMetParameter> dbParamToMetParamMap = null;

    // A list of derivedParameters that need to be derived from the dbParamsMap.
    //
    private ArrayList<AbstractMetParameter> derivedParameters = null;

    // A map for those parameters that determine their value from an
    // array of values from the DB. (This implements functionality previously
    // done in the PlotModelFactory for the plotFunctionTable tag and now done
    // with the arrayIndex tag in the plotParameterDefn.)
    //
    private HashMap<String, PlotParameterDefn> prioritySelectionsMap = null;

    private static final String latDbName = "latitude";

    private static final String lonDbName = "longitude";

    private static final String refTimeDbName = "refTime";

    private Map<String, String> metParamNameToDbNameMap = null;

    private HashMap<String, AbstractMetParameter> allMetParamsMap = null;

    private PlotParameterDefns plotPrmDefns = null;

    private Set<String> dbParamNamesForQuery = null;

    private Set<String> condColoringParamNames = null;

    private JobPool dataRequestJobPool = null;

    private ConcurrentLinkedQueue<QueueEntry> queueOfStations = null;

    private IMetParamRetrievalListener listener = null;

    private Set<String> condDerivedMetParamNames = null;

    private Semaphore sem1 = new Semaphore(1);

    ConditionalFilter conditionalFilter = null;

    Map<String, RequestConstraint> condFilterMap = null;

    // Indicates the dataRequestJobPool is being cancelled, so jobs should just
    // exit gracefully on return from a long datastore request.
    private boolean canceling = false;

    public void queueStationsForHdf5Query(DataTime dt,
            Collection<Station> listOfStations) {
        Tracer.print("> Entry");
        QueueEntry queueEntry = new QueueEntry(dt, listOfStations);
        queueOfStations.add(queueEntry);
        Tracer.print("About to query data for frame: "
                + Tracer.shortTimeString(dt));
        runDataQuery();
        Tracer.print("< Exit");
    }

    private void runDataQuery() {
        Tracer.print("> Entry");
        while (queueOfStations.peek() != null) {
            QueueEntry qe = queueOfStations.poll();
            if (qe != null) {
                GetDataTask task = new GetDataTask(qe.getStations(),
                        qe.getDataTime());
                dataRequestJobPool.schedule(task);
            }
        }
        Tracer.print("< Exit");
    }

    public NcPlotDataRequestor(PlotModel plotModel, String level,
            Map<String, RequestConstraint> constraintMap,
            IMetParamRetrievalListener listener, ConditionalFilter cf) {
        Tracer.print("> Entry");
        this.plugin = plotModel.getPlugin();
        this.levelStr = level;
        this.constraintMap = constraintMap;
        parametersToPlot = new HashMap<String, AbstractMetParameter>();
        derivedParameters = new ArrayList<AbstractMetParameter>();
        dbParamToMetParamMap = new HashMap<String, AbstractMetParameter>();
        prioritySelectionsMap = new HashMap<String, PlotParameterDefn>();
        allMetParamsMap = new HashMap<String, AbstractMetParameter>();
        plotPrmDefns = PlotParameterDefnsMngr.getInstance().getPlotParamDefns(
                plotModel.getPlugin());
        dataRequestJobPool = new JobPool("Requesting met param data...", 8,
                false);
        queueOfStations = new ConcurrentLinkedQueue<QueueEntry>();
        namesOfParametersToQuery = new String[0];
        metParamNameToDbNameMap = new HashMap<String, String>();
        dbParamNamesForQuery = new HashSet<String>(2);
        condColoringParamNames = new HashSet<String>(0);
        conditionalFilter = cf;
        this.listener = listener;
        try {
            condDerivedMetParamNames = new HashSet<String>(0);
            establishPlotParamDefnToMetParamMappings();
            updateListOfParamsToPlotFromCurrentPlotModel(plotModel);
            if (conditionalFilter != null) {
                setUpConditionalFilterParameters();
            }
            if (plotModel.hasAdvancedSettings()) {
                determineConditionalColoringParameters(plotModel);
            }
        } catch (VizException e) {
            statusHandler.handle(Priority.PROBLEM,
                    "Exception:  " + e.getMessage());
        }
        Tracer.print("< Exit");
    }

    public void setLevelStr(String levelStr) {
        this.levelStr = levelStr;
    }

    public ConditionalFilter getConditionalFilter() {
        return conditionalFilter;
    }

    public void setConditionalFilter(ConditionalFilter conditionalFilter) {
        if (conditionalFilter != null)
            this.conditionalFilter = new ConditionalFilter(conditionalFilter);
        else
            this.conditionalFilter = null;
    }

    public void determineConditionalColoringParameters(PlotModel plotModel) {
        PlotParameterDefns plotParamDefns = PlotParameterDefnsMngr
                .getInstance().getPlotParamDefns(plotModel.getPlugin());
        Tracer.print("> Entry");
        List<PlotModelElement> plotModelElements = plotModel
                .getAllPlotModelElements();
        if (plotModelElements != null && !plotModelElements.isEmpty()) {
            for (PlotModelElement pme : plotModelElements) {
                String condParamName = pme.getConditionalParameter();
                if (condParamName != null) {
                    PlotParameterDefn thisPlotParamDefn = plotParamDefns
                            .getPlotParamDefn(condParamName);
                    if (thisPlotParamDefn != null) {
                        if (thisPlotParamDefn.getDeriveParams() != null) {
                            addToDerivedParamsList(
                                    thisPlotParamDefn.getDeriveParams(),
                                    thisPlotParamDefn);
                        } else {
                            String dbPrmName = thisPlotParamDefn
                                    .getDbParamName();
                            MetParameterFactory.getInstance().alias(
                                    thisPlotParamDefn.getMetParamName(),
                                    dbPrmName);
                            AbstractMetParameter condColoringParam = MetParameterFactory
                                    .getInstance()
                                    .createParameter(
                                            thisPlotParamDefn.getMetParamName(),
                                            thisPlotParamDefn.getPlotUnit());
                            if (!dbParamToMetParamMap.containsKey(dbPrmName))
                                dbParamToMetParamMap.put(dbPrmName,
                                        condColoringParam);
                        }
                        condColoringParamNames.add(thisPlotParamDefn
                                .getMetParamName());
                    }
                }
            }
        }
        Tracer.print("< Exit");
    }

    public void updateConditionalFilterMapFromConditionalFilter(
            ConditionalFilter cf) {
        Tracer.print("> Entry");
        if (cf != null) {
            condFilterMap = new HashMap<String, RequestConstraint>(
                    cf.getConditionalFilterMap());
        } else {
            if (condFilterMap != null) {
                condFilterMap.clear();
            }
        }
        Tracer.print("< Exit");
    }

    public Map<String, RequestConstraint> getConditionalFilterMap() {
        return condFilterMap;
    }

    /**
     * Filters the stations assuming that the conditional filter has parameters
     * that are available in the plot model
     * 
     * @param dataTime
     * @param stationSet
     */
    public synchronized void updateListOfStationsPerConditionalFilter(
            DataTime dataTime, Set<Station> stationSet) {
        Tracer.print("> Entry");
        Set<Station> filteredSetOfStations = new HashSet<Station>(0);
        if (conditionalFilter != null) {
            updateConditionalFilterMapFromConditionalFilter(this.conditionalFilter);
            synchronized (stationSet) {
                for (Station station : stationSet) {
                    if (station.parametersToPlot == null
                            || station.parametersToPlot.isEmpty()) {
                        continue;
                    }
                    List<Boolean> displayPlotBoolList = new ArrayList<Boolean>(
                            station.parametersToPlot.size());
                    boolean displayStation = true;
                    synchronized (station.parametersToPlot) {
                        for (AbstractMetParameter metPrm : station.parametersToPlot) {
                            displayPlotBoolList
                                    .add(doesStationPassTheFilterForThisMetParam(metPrm));
                        }
                    }
                    synchronized (displayPlotBoolList) {
                        for (Boolean b : displayPlotBoolList) {
                            displayStation &= b;
                        }
                        if (displayStation) {
                            synchronized (filteredSetOfStations) {
                                filteredSetOfStations.add(station);
                            }
                        }
                    }
                }
            }
        }
        boolean isThereAConditionalFilter = true;
        // TODO future - make this actual count of stations retrieving data on
        // this call for this frame, for performance statistics tracking
        listener.retrievalComplete(dataTime, filteredSetOfStations, 0,
                isThereAConditionalFilter);
        Tracer.print("< Exit");
    }

    public void setUpConditionalFilterParameters() {
        Tracer.print("> Entry");
        if (conditionalFilter != null) {
            updateConditionalFilterMapFromConditionalFilter(this.conditionalFilter);
            if (condFilterMap == null || condFilterMap.isEmpty()) {
                return;
            }
            if (!condDerivedMetParamNames.isEmpty()) {
                condDerivedMetParamNames.clear();
            }
            List<PlotParameterDefn> listOfAllPlotParamDefnsForThisPlugin = plotPrmDefns
                    .getParameterDefns();
            for (PlotParameterDefn eachPlotParamDefn : listOfAllPlotParamDefnsForThisPlugin) {
                String plotParamName = eachPlotParamDefn.getPlotParamName();
                if (condFilterMap.containsKey(plotParamName)) {
                    AbstractMetParameter condMetParam = null;
                    if (eachPlotParamDefn.getDeriveParams() != null) {
                        condDerivedMetParamNames.add(eachPlotParamDefn
                                .getMetParamName());
                        condMetParam = addToDerivedParamsList(
                                eachPlotParamDefn.getDeriveParams(),
                                eachPlotParamDefn);
                    } else {
                        MetParameterFactory.getInstance().alias(
                                eachPlotParamDefn.getMetParamName(),
                                eachPlotParamDefn.getDbParamName());
                        condMetParam = MetParameterFactory.getInstance()
                                .createParameter(
                                        eachPlotParamDefn.getMetParamName(),
                                        eachPlotParamDefn.getPlotUnit());
                        String dbParamName = eachPlotParamDefn.getDbParamName();
                        if (!dbParamToMetParamMap.containsKey(dbParamName)) {
                            dbParamToMetParamMap.put(dbParamName, condMetParam);
                            dbParamNamesForQuery.add(dbParamName);
                        }
                    }
                    determineParameterNamesForDataQuery();
                }
            }
        }
        Tracer.print("< Exit");
    }

    private Boolean doesStationPassTheFilterForThisMetParam(
            AbstractMetParameter metPrm) {
        Tracer.printX("> Entry " + metPrm);
        Boolean displayStationPlot = true;

        Set<String> condPlotParamNameSet = condFilterMap.keySet();
        List<PlotParameterDefn> listOfPlotParamDefns = plotPrmDefns
                .getParameterDefns();

        for (PlotParameterDefn plotPrmDefn : listOfPlotParamDefns) {
            if (plotPrmDefn.getMetParamName().equals(metPrm.getMetParamName())) {
                String plotParamName = plotPrmDefn.getPlotParamName();
                for (String condPlotParamName : condPlotParamNameSet) {
                    if (condPlotParamName.equals(plotParamName)) {

                        RequestConstraint reqConstraint = condFilterMap
                                .get(condPlotParamName);
                        if (reqConstraint == null) {
                            continue;
                        }

                        AbstractMetParameter condMetParam = MetParameterFactory
                                .getInstance().createParameter(
                                        plotPrmDefn.getMetParamName(),
                                        plotPrmDefn.getPlotUnit());

                        try {
                            if (!condMetParam.hasStringValue()) {
                                condMetParam.setValue(metPrm
                                        .getValueAs(condMetParam.getUnitStr()),
                                        condMetParam.getUnit());
                            } else {
                                condMetParam.setStringValue(metPrm
                                        .getStringValue());
                            }
                            String formattedPlotString = null;
                            String plotFormat = plotPrmDefn.getPlotFormat();
                            if (plotFormat != null) {
                                formattedPlotString = new String(
                                        condMetParam
                                                .getFormattedString(plotFormat));
                            } else {
                                if (condMetParam.hasStringValue()) {
                                    formattedPlotString = new String(
                                            condMetParam.getStringValue());
                                } else {
                                    formattedPlotString = new String(
                                            Double.toString(condMetParam
                                                    .getValueAs(
                                                            condMetParam
                                                                    .getUnitStr())
                                                    .doubleValue()));
                                }
                            }

                            int plotTrim = 0;
                            if (plotPrmDefn.getPlotTrim() == null) {
                                plotTrim = 0;
                            } else {
                                plotTrim = Integer.parseInt(plotPrmDefn
                                        .getPlotTrim());
                            }

                            if (plotTrim != 0) {
                                formattedPlotString = formattedPlotString
                                        .substring(plotTrim);
                            }

                            boolean result = condMetParam.hasStringValue() ? reqConstraint
                                    .evaluate(formattedPlotString)
                                    : reqConstraint.evaluate(Double
                                            .parseDouble(formattedPlotString));

                            if (result) {
                                displayStationPlot = true;
                                break;
                            } else {
                                displayStationPlot = false;
                                break;
                            }

                        } catch (Exception e) {
                            displayStationPlot = false;
                            break;
                        }

                    }
                }
            }
        }
        Tracer.printX("< Exit  " + (displayStationPlot ? "YES" : "NO"));

        return displayStationPlot;
    }

    private void establishPlotParamDefnToMetParamMappings() throws VizException {
        Tracer.print("> Entry");
        long t0 = System.nanoTime();
        List<PlotParameterDefn> listOfAllPlotParamDefnsForThisPlugin = plotPrmDefns
                .getParameterDefns();
        if (listOfAllPlotParamDefnsForThisPlugin != null
                && !listOfAllPlotParamDefnsForThisPlugin.isEmpty()) {
            for (PlotParameterDefn plotPrmDefn : listOfAllPlotParamDefnsForThisPlugin) {
                metParamNameToDbNameMap.put(plotPrmDefn.getMetParamName(),
                        plotPrmDefn.getDbParamName());
                // If this is a 'vector' parameter (wind barb, arrow,
                // directional arrow) then get the 2 component metParameters (or
                // possibly only 1 for directional arrow) and make sure they
                // exist.
                if (plotPrmDefn.isVectorParameter()) {
                    String[] vectParamNames = plotPrmDefn
                            .getMetParamNamesForVectorPlot();

                    if (vectParamNames == null) {
                        throw new VizException(
                                "Error plotting WindBarb or Arrow: Can't get components metParameters for "
                                        + plotPrmDefn.getPlotParamName());
                    }

                    for (String vectParam : vectParamNames) {
                        if (plotPrmDefns
                                .getPlotParamDefnsForMetParam(vectParam)
                                .isEmpty()) {
                            throw new VizException(
                                    "Error plotting WindBarb or Arrow: Can't find definition for component metParameter "
                                            + vectParam);
                        }
                    }
                } else { // if not a vector parameter
                    String dbPrmName = plotPrmDefn.getDbParamName();

                    if (dbPrmName == null) {
                        // derived
                        if (plotPrmDefn.getDeriveParams() != null) {
                            // TODO Do anything here at all?
                        } else {
                            continue;
                        }
                    } else if (dbParamToMetParamMap.containsKey(dbPrmName)) {
                        continue;
                    } else { // not derived(?)

                        // Alias the DB param name to the metParam (subclass of
                        // AbstractMetParameter). (This eliminates the need to
                        // have a direct mapping from the DB name to the class
                        // name.)
                        MetParameterFactory.getInstance().alias(
                                plotPrmDefn.getMetParamName(),
                                plotPrmDefn.getDbParamName());

                        // Create a metParam that will hold the value from the
                        // DB and which will be used to plot the plotParameter
                        // and possibly derive other parameter values.
                        //
                        AbstractMetParameter dbParam = MetParameterFactory
                                .getInstance().createParameter(
                                        plotPrmDefn.getMetParamName(),
                                        plotPrmDefn.getPlotUnit());
                        if (dbParam == null) {
                            statusHandler.handle(Priority.PROBLEM,
                                    "Error creating metParameter "
                                            + plotPrmDefn.getMetParamName());
                        } else {
                            // Add this prm to a map to tell us which DB params
                            // are needed when querying the DB
                            dbParamToMetParamMap.put(
                                    plotPrmDefn.getDbParamName(), dbParam);

                            // For parameters that need to lookup their value
                            // from an array of values based on a priority
                            // (e.g., for skyCover to determine the highest
                            // level of cloud cover at any level).
                            //
                            prioritySelectionsMap.put(dbPrmName, plotPrmDefn);

                            // else TODO : check for arrayIndex
                        }
                    }
                }
            }

            // If the station lat/long is not in the defns file, add them here
            // since they are needed by the PlotModelFactory to plot the data.
            //
            if (!dbParamToMetParamMap.containsKey(latDbName)) {
                MetParameterFactory.getInstance().alias(
                        StationLatitude.class.getSimpleName(), latDbName);
                AbstractMetParameter latPrm = MetParameterFactory.getInstance()
                        .createParameter(StationLatitude.class.getSimpleName(),
                                NonSI.DEGREE_ANGLE);
                dbParamToMetParamMap.put(latDbName, latPrm);
            }

            if (!dbParamToMetParamMap.containsKey(lonDbName)) {
                MetParameterFactory.getInstance().alias(
                        StationLongitude.class.getSimpleName(), lonDbName);

                AbstractMetParameter longPrm = MetParameterFactory
                        .getInstance().createParameter(
                                StationLongitude.class.getSimpleName(),
                                NonSI.DEGREE_ANGLE);

                dbParamToMetParamMap.put(lonDbName, longPrm);
            }

            dbParamNamesForQuery.add(latDbName);
            dbParamNamesForQuery.add(lonDbName);
            dbParamNamesForQuery.add(refTimeDbName);

        }

        long t1 = System.nanoTime();
        Tracer.print(" establishPlotParamDefnToMetParamMappings() took "
                + (t1 - t0) / 1000000 + " ms");
        Tracer.print("< Exit");
    }

    public void updateListOfParamsToPlotFromCurrentPlotModel(PlotModel plotModel)
            throws VizException {
        Tracer.print("> Entry");
        long t0 = System.nanoTime();

        if (dbParamNamesForQuery != null && !dbParamNamesForQuery.isEmpty()) {
            dbParamNamesForQuery.clear();
        }

        dbParamNamesForQuery.add(latDbName);
        dbParamNamesForQuery.add(lonDbName);
        dbParamNamesForQuery.add(refTimeDbName);

        if (derivedParameters != null && !derivedParameters.isEmpty()) {
            derivedParameters.clear();
        }

        if (parametersToPlot != null & !parametersToPlot.isEmpty()) {
            parametersToPlot.clear();
        }

        parametersToPlot.put(StationLatitude.class.getSimpleName(),
                dbParamToMetParamMap.get(latDbName));

        parametersToPlot.put(StationLongitude.class.getSimpleName(),
                dbParamToMetParamMap.get(lonDbName));

        if (condColoringParamNames != null && !condColoringParamNames.isEmpty()) {
            condColoringParamNames.clear();
        }

        List<String> listOfSelectedPlotParameters = plotModel
                .getPlotParamNames(true);
        for (String pltPrmName : listOfSelectedPlotParameters) {

            // get the dbParamName and determine if derived parameter

            PlotParameterDefn plotPrmDefn = plotPrmDefns
                    .getPlotParamDefn(pltPrmName);

            if (plotPrmDefn == null) {
                throw new VizException("Error creating plot metParameter "
                        + pltPrmName);
            } else if (plotPrmDefn.isVectorParameter()) {
                // Add the 1 or 2 metParameters (direction, and magnitude if
                // applicable) to paramsToPlot.
                String[] vectParamNames = plotPrmDefn
                        .getMetParamNamesForVectorPlot();
                for (String vectParam : vectParamNames) {
                    PlotParameterDefn vectPrmDefn = plotPrmDefns
                            .getPlotParamDefnsForMetParam(vectParam).get(0);
                    addToParamsToPlot(vectPrmDefn);
                }
            } else {
                addToParamsToPlot(plotPrmDefn);
            }
        }

        determineParameterNamesForDataQuery();
        long t1 = System.nanoTime();
        Tracer.print(" updateListOfParamsToPlotFromCurrentPlotModel() took "
                + (t1 - t0) / 1000000 + " ms");
        Tracer.print("< Exit");
    }

    public void setDefaultConstraintsMap(Map<String, RequestConstraint> inMap) {
        Tracer.print("> Entry");
        this.constraintMap = new HashMap<String, RequestConstraint>(inMap);
        Tracer.print("< Exit");
    }

    public void dispose() {
        Tracer.print("> Entry");
        Tracer.print("Invoking NcPlotDataRequestor.dispose()");
        canceling = true;
        if (dataRequestJobPool != null) {
            dataRequestJobPool.cancel(false); // false = don't wait for jobs to
                                              // complete
            dataRequestJobPool = null;
        }
        Tracer.print("< Exit");
    }

    private void addToParamsToPlot(PlotParameterDefn plotPrmDefn) {
        Tracer.print("> Entry");
        long t0 = System.nanoTime();

        String dbParamName = plotPrmDefn.getDbParamName();
        String metParamName = plotPrmDefn.getMetParamName();
        String[] deriveParams = plotPrmDefn.getDeriveParams(); // the input args

        // if this is a derived parameter, create a metParameter to hold the
        // derived value to be computed and plotted.

        if (deriveParams != null) {
            AbstractMetParameter derivedMetParam = addToDerivedParamsList(
                    deriveParams, plotPrmDefn);
            if (derivedMetParam == null) {
                return;
            }
            parametersToPlot.put(metParamName, derivedMetParam);
        }

        // if this is a dbParameter then save the metParameter from the
        // dbParamsMap in the parametersToPlot map.

        else if (dbParamName != null
                && dbParamToMetParamMap.containsKey(dbParamName)) {
            dbParamNamesForQuery.add(dbParamName);

            // if it is already in the map then we don't need to save it twice.
            if (!parametersToPlot.containsKey(metParamName)) {
                parametersToPlot.put(metParamName,
                        dbParamToMetParamMap.get(dbParamName));
            }
        } else if (dbParamName != null) { // TODO factor out null check
            statusHandler.handle(Priority.PROBLEM,
                    "Sanity check : dbParamName: \"" + dbParamName
                            + "\" is not in dbParamsMap");
        }

        long t1 = System.nanoTime();
        Tracer.print("addToParamsToPlot() took " + (t1 - t0) / 1000000 + " ms");
        Tracer.print("< Exit");
    }

    private AbstractMetParameter addToDerivedParamsList(String[] deriveParams,
            PlotParameterDefn plotPrmDefn) {
        Tracer.print("> Entry");
        long t0 = System.nanoTime();

        // If this is a derived parameter, create a metParameter to hold the
        // derived value to be computed and plotted.

        AbstractMetParameter derivedMetParam = MetParameterFactory
                .getInstance().createParameter(plotPrmDefn.getMetParamName(),
                        plotPrmDefn.getPlotUnit());

        if (derivedMetParam == null) {
            statusHandler.handle(
                    Priority.PROBLEM,
                    "Error creating metParameter "
                            + plotPrmDefn.getMetParamName());
            return null;
        } else {
            // If all is set then all of the available metParameters from the db
            // query are used when attempting to derive the parameter.
            // Otherwise, we are expecting a comma separated list of parameters.

            if (deriveParams.length >= 1
                    && !deriveParams[0].equalsIgnoreCase("all")) {

                ArrayList<String> preferredDeriveParameterNames = new ArrayList<String>();

                for (String dPrm : deriveParams) {
                    AbstractMetParameter deriveInputParam = MetParameterFactory
                            .getInstance().createParameter(dPrm);

                    if (deriveInputParam != null) {
                        preferredDeriveParameterNames.add(deriveInputParam
                                .getMetParamName());
                    } else {
                        statusHandler.handle(Priority.WARN, "Warning: '" + dPrm
                                + " is not a valid metParameter name");
                        return null;
                    }
                }

                derivedMetParam
                        .setPreferredDeriveParameters(preferredDeriveParameterNames);
            }

            // Determine the (top-level) method to derive this parameter from
            // the available parameters, AND the associated (bottom-level) base
            // (non-derived; i.e., mapped directly to a DB param) parameters.
            Set<AbstractMetParameter> baseAmps = new HashSet<AbstractMetParameter>();
            Method deriveMethod = derivedMetParam.getDeriveMethod(
                    dbParamToMetParamMap.values(), baseAmps);
            if (deriveMethod == null) {
                System.out.println("Unable to derive "
                        + derivedMetParam.getMetParamName()
                        + " from available parameters.");
                return null;
            }

            // For each base met parameter, get the associated DB param; store
            // complete list of these names in the met param, so we know what
            // to retrieve from the DB later.
            Set<String> baseDbParamNames = new HashSet<String>();
            for (AbstractMetParameter amp : baseAmps) {
                String dbParamName = metParamNameToDbNameMap.get(amp
                        .getMetParamName());
                if (dbParamName != null) {
                    baseDbParamNames.add(dbParamName);
                }
            }
            derivedMetParam
                    .setDbParamNamesForDerivingThisMetPrm(baseDbParamNames);

            if (derivedParameters.isEmpty()) {
                derivedParameters.add(derivedMetParam);
            } else {
                boolean addParam = true;
                for (AbstractMetParameter derivedMetPrmToCheck : derivedParameters) {
                    if (derivedMetPrmToCheck.getMetParamName().equals(
                            derivedMetParam.getMetParamName())) {
                        addParam = false;
                        break;
                    }
                }
                if (addParam) {
                    derivedParameters.add(derivedMetParam);
                }
            }

        }
        long t1 = System.nanoTime();
        Tracer.print("addToDerivedParamsList() took " + (t1 - t0) / 1000000
                + " ms for " + derivedMetParam.getMetParamName());
        Tracer.print("< Exit");
        return derivedMetParam;

    }

    private void determineDBParamNamesForDerivedParameters() {
        Tracer.print("> Entry");
        long t0 = System.nanoTime();
        synchronized (derivedParameters) {
            for (AbstractMetParameter derivedMetParameter : derivedParameters) {

                Set<String> inputPrms = derivedMetParameter
                        .getDbParamNamesForDerivingThisMetParameter();

                if (inputPrms != null && !inputPrms.isEmpty()) {
                    for (String metPrmName : inputPrms) {
                        dbParamNamesForQuery.add(metPrmName);

                        /*
                         * Add temp and dewpoint from tenths if it is not in the
                         * map because temperature and dewpoint are not
                         * requested specifically but are needed to derived a
                         * parameter.
                         */

                        if (metPrmName.equals("temperature")
                                && !dbParamNamesForQuery
                                        .contains("tempFromTenths")) {
                            dbParamNamesForQuery.add("tempFromTenths");
                        } else if (metPrmName.equals("dewpoint")
                                && !dbParamNamesForQuery
                                        .contains("dpFromTenths")) {
                            dbParamNamesForQuery.add("dpFromTenths");
                        }

                    }
                }
            }
        }

        long t1 = System.nanoTime();
        Tracer.print("determineDBParamNamesForDerivedParameters() took "
                + (t1 - t0) / 1000000 + " ms");
        Tracer.print("< Exit");

    }

    private void determineParameterNamesForDataQuery() {
        Tracer.print("> Entry");
        long t0 = System.nanoTime();

        Collection<AbstractMetParameter> metParamsToPlotCollection = parametersToPlot
                .values();
        synchronized (metParamsToPlotCollection) {
            for (AbstractMetParameter metPrm : metParamsToPlotCollection) {
                String dbName = metParamNameToDbNameMap.get(metPrm.getClass()
                        .getSimpleName());
                if (dbName != null) {
                    dbParamNamesForQuery.add(dbName);
                    /*
                     * Add temp and dewpoint from tenths to list of paramters to
                     * request from the hdf5.
                     */
                    if (dbName.equals("temperature")) {
                        dbParamNamesForQuery.add("tempFromTenths");
                    } else if (dbName.equals("dewpoint")) {
                        dbParamNamesForQuery.add("dpFromTenths");
                    }
                }
            }
        }
        if (this.derivedParameters != null && !this.derivedParameters.isEmpty()) {
            determineDBParamNamesForDerivedParameters();
        }

        this.namesOfParametersToQuery = new String[dbParamNamesForQuery.size()];
        dbParamNamesForQuery.toArray(namesOfParametersToQuery);
        long t1 = System.nanoTime();
        Tracer.print("determineParameterNamesForDataQuery() took " + (t1 - t0)
                / 1000000 + " ms");
        Tracer.print("< Exit");
    }

    private Collection<Station> requestUpperAirData(
            List<Station> stationsRequestingData,
            int stationsRetrievedThisCallCount) {
        Tracer.print("> Entry" + "\n"
                + Tracer.printableStationList(stationsRequestingData));
        List<Boolean> displayStationPlotBoolList = new ArrayList<Boolean>(0);
        boolean displayStationPlot = false;
        int listSize = stationsRequestingData.size();
        long beginTime = 0;
        long endTime = Long.MAX_VALUE;
        Date refTime = null;
        List<String> stnIdLst = new ArrayList<String>(listSize);
        List<Long> rangeTimeLst = new ArrayList<Long>(listSize);
        Map<String, Station> mapOfStnidsWithStns = new HashMap<String, Station>();
        synchronized (stationsRequestingData) {
            for (Station currentStation : stationsRequestingData) {
                refTime = currentStation.info.dataTime.getRefTime();
                long stnTime = currentStation.info.dataTime.getValidTime()
                        .getTimeInMillis();
                beginTime = (beginTime < stnTime ? stnTime : beginTime);
                endTime = (endTime > stnTime ? stnTime : endTime);
                String stnId = new String(currentStation.info.stationId);
                if (stationHasAllParametersItNeeds(currentStation,
                        namesOfParametersToQuery)) {
                    Tracer.print("Station "
                            + currentStation.info.stationId
                            + " has all met params it needs; skipping data request");
                } else {
                    stnIdLst.add(stnId);
                }
                mapOfStnidsWithStns.put(stnId, currentStation);
                if (rangeTimeLst.contains(stnTime) == false) {
                    rangeTimeLst.add(stnTime);
                }
            }
        }
        if (stnIdLst.size() <= 0 || rangeTimeLst.size() <= 0) {
            // No stations, no query
            Tracer.print("SKIPPING request for UPPER AIR data because "
                    + stnIdLst.size() + " (zero) out of "
                    + stationsRequestingData.size() + " stations need it");
            return null; // TODO check to see if positive abort required
        } else {
            Tracer.print("Requesting UPPER AIR data for " + stnIdLst.size()
                    + " out of " + stationsRequestingData.size() + " stations");
        }
        long[] refTimelArray = new long[1];
        refTimelArray[0] = refTime.getTime();
        long[] rangeTimeArray = new long[rangeTimeLst.size()];
        for (int k = 0; k < rangeTimeLst.size(); k++) {
            rangeTimeArray[k] = rangeTimeLst.get(k);
        }
        String[] stnIdArray;
        stnIdArray = stnIdLst.toArray((new String[0]));
        NcSoundingCube soundingCube = null;

        // Make the query

        long t004 = System.nanoTime();
        soundingCube = PlotModelMngr.querySoundingData(refTimelArray,
                rangeTimeArray, stnIdArray, plugin, levelStr, constraintMap,
                parametersToPlot);
        long t005 = System.nanoTime();
        Tracer.print("requestUpperAirData()-->PlotModelMngr.querySoundingData() took "
                + (t005 - t004) / 1000000 + " ms");

        // TODO -- This shouldn't be necessary, given Amount.getUnit() should
        // now heal itself from a null unit by using the String.
        // Repair the 'unit' in the met params, if damaged (as in, nulled) in
        // transit. UPDATE: Suggest seeing if a JAXB XmlAdapter could be used to
        // transmit the 'unit'
        if (soundingCube != null
                && soundingCube.getRtnStatus() == QueryStatus.OK) {
            List<NcSoundingProfile> soundingProfiles = soundingCube
                    .getSoundingProfileList();
            synchronized (soundingProfiles) {
                for (NcSoundingProfile soundingProfile : soundingProfiles) {
                    List<NcSoundingLayer2> soundingLayers = soundingProfile
                            .getSoundingLyLst2();
                    synchronized (soundingLayers) {
                        for (NcSoundingLayer2 soundingLayer : soundingLayers) {
                            Collection<AbstractMetParameter> metParams = soundingLayer
                                    .getMetParamsMap().values();
                            synchronized (metParams) {
                                for (AbstractMetParameter metParam : metParams) {
                                    metParam.syncUnits();
                                }
                            }
                        }
                    }
                }
            }
        }

        if (soundingCube != null
                && soundingCube.getRtnStatus() == QueryStatus.OK) {
            List<NcSoundingProfile> soundingProfiles = soundingCube
                    .getSoundingProfileList();
            if (soundingProfiles == null || soundingProfiles.isEmpty()) {
                return null;
            }
            synchronized (soundingProfiles) {
                for (NcSoundingProfile soundingProfile : soundingProfiles) {
                    Station currentStation = mapOfStnidsWithStns
                            .get(soundingProfile.getStationId());

                    /*
                     * Next station gets a fresh start when considering
                     * conditional filters.
                     */
                    displayStationPlotBoolList.clear();

                    /*
                     * Clear the existing list of parameters to plot in each
                     * station - to guarantee an updated list if there is a
                     * re-query for parameters by editing the plot model
                     */
                    if (currentStation.parametersToPlot != null
                            && !currentStation.parametersToPlot.isEmpty()) {
                        synchronized (currentStation.parametersToPlot) {
                            currentStation.parametersToPlot.clear();
                        }
                    }

                    if (soundingProfile.getSoundingLyLst2().isEmpty()
                            || soundingProfile.getSoundingLyLst2().size() != 1) {
                        continue;
                    }

                    NcSoundingLayer2 soundingLayer = soundingProfile
                            .getSoundingLyLst2().get(0);
                    Map<String, AbstractMetParameter> soundingParamsMap = soundingLayer
                            .getMetParamsMap();

                    // Set all the paramsToPlot values to missing. (All the
                    // metParams in the paramsToPlot map are references into the
                    // derivedParamsMap and the dbParamsMap.)

                    for (AbstractMetParameter metPrm : derivedParameters) {
                        metPrm.setValueToMissing();
                    }
                    synchronized (dbParamNamesForQuery) {
                        for (String dbPrmName : dbParamNamesForQuery) {
                            AbstractMetParameter metPrm = dbParamToMetParamMap
                                    .get(dbPrmName);
                            if (metPrm == null) {
                                continue;
                            }

                            AbstractMetParameter newInstance = newInstance(metPrm);
                            if (newInstance == null) {
                                continue;
                            }
                            // TODO : the station lat/lon, elev, name and id
                            // should be set in the sounding profile
                            // but currently isn't. So instead we will get the
                            // lat/lon and id from the DBQuery.
                            String key = newInstance.getMetParamName();
                            if (soundingParamsMap.containsKey(key)) {
                                AbstractMetParameter queriedParam = soundingParamsMap
                                        .get(key);
                                if (newInstance.hasStringValue()) {
                                    newInstance.setStringValue(queriedParam
                                            .getStringValue());
                                } else {
                                    newInstance.setValue(
                                            queriedParam.getValue(),
                                            queriedParam.getUnit());
                                }
                            }

                            else if (newInstance.getMetParamName().equals(
                                    StationLatitude.class.getSimpleName())) {
                                newInstance.setValue(new Amount(soundingProfile
                                        .getStationLatitude(),
                                        NonSI.DEGREE_ANGLE));
                            } else if (newInstance.getMetParamName().equals(
                                    StationLongitude.class.getSimpleName())) {
                                newInstance.setValue(new Amount(soundingProfile
                                        .getStationLongitude(),
                                        NonSI.DEGREE_ANGLE));
                            }
                            // TODO: check stationId handling
                            else if (newInstance.getMetParamName().equals(
                                    StationID.class.getSimpleName())) {
                                if (!soundingProfile.getStationId().isEmpty()) {
                                    newInstance.setStringValue(soundingProfile
                                            .getStationId());
                                } else {
                                    newInstance.setValueToMissing();
                                }
                                // TODO: check stationId handling
                            } else if (newInstance.getMetParamName().equals(
                                    StationNumber.class.getSimpleName())) {
                                if (soundingProfile.getStationNum() != 0) {
                                    newInstance.setStringValue(new Integer(
                                            soundingProfile.getStationNum())
                                            .toString());
                                } else {
                                    newInstance.setValueToMissing();
                                }
                            } else if (newInstance.getMetParamName().equals(
                                    PrecipitableWaterForEntireSounding.class
                                            .getSimpleName())) {
                                newInstance.setValue(new Amount(soundingProfile
                                        .getPw(), SI.MILLIMETER));
                            } else {
                                statusHandler
                                        .handle(Priority.PROBLEM,
                                                "Sanity check: \""
                                                        + metPrm.getMetParamName()
                                                        + "\" is not available in the sounding data");
                            }

                            if (condFilterMap != null
                                    && !condFilterMap.isEmpty()) {
                                displayStationPlotBoolList
                                        .add(doesStationPassTheFilterForThisMetParam(newInstance));
                            }

                            if (parametersToPlot.containsKey(newInstance
                                    .getMetParamName())) {
                                currentStation.parametersToPlot
                                        .add(newInstance);
                            }

                            allMetParamsMap.put(newInstance.getMetParamName(),
                                    newInstance);

                            // TODO : for modelsoundings. what are the units?
                        }

                    }
                    Collection<AbstractMetParameter> metPrmCollection = soundingParamsMap
                            .values();
                    synchronized (derivedParameters) {
                        for (AbstractMetParameter derivedParam : derivedParameters) {
                            try {
                                synchronized (metPrmCollection) {
                                    derivedParam.derive(metPrmCollection);
                                }
                                AbstractMetParameter clonedDerivedPrm = newInstance(derivedParam);

                                if (clonedDerivedPrm == null) {
                                    continue;
                                }

                                if (parametersToPlot.containsKey(derivedParam
                                        .getMetParamName())) {
                                    currentStation.parametersToPlot
                                            .add(clonedDerivedPrm);
                                }

                                allMetParamsMap.put(
                                        clonedDerivedPrm.getMetParamName(),
                                        clonedDerivedPrm);

                            } catch (NotDerivableException e) {
                                statusHandler.handle(
                                        Priority.PROBLEM,
                                        "NotDerivableException:  "
                                                + e.getMessage());
                            }

                        }
                    }
                    /*
                     * Validate the station against conditionally derived
                     * MetParameters
                     */
                    if (condFilterMap != null && !condFilterMap.isEmpty()) {
                        synchronized (condDerivedMetParamNames) {
                            for (String condMetParamName : condDerivedMetParamNames) {
                                synchronized (derivedParameters) {
                                    for (AbstractMetParameter condDerivedParamToCheck : derivedParameters) {
                                        if (condDerivedParamToCheck
                                                .getMetParamName().equals(
                                                        condMetParamName)) {
                                            if (condDerivedParamToCheck
                                                    .hasValidValue()) {
                                                displayStationPlotBoolList
                                                        .add(doesStationPassTheFilterForThisMetParam(condDerivedParamToCheck));
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    /*
                     * Process the conditional parameter(s) (if any) for the
                     * station
                     */
                    if (condColoringParamNames != null
                            && !condColoringParamNames.isEmpty()) {
                        Collection<AbstractMetParameter> dbMetParamColl = dbParamToMetParamMap
                                .values();
                        synchronized (condColoringParamNames) {
                            for (String condColorParamName : condColoringParamNames) {

                                currentStation = processConditionalParameterForOneStation(
                                        dbMetParamColl, currentStation,
                                        condColorParamName);

                                currentStation = processConditionalParameterForOneStation(
                                        derivedParameters, currentStation,
                                        condColorParamName);
                            }
                        }
                    }

                    /*
                     * Evaluate the station against the conditional filter to
                     * decide if it needs to be plotted at all
                     */
                    if (condFilterMap != null && !condFilterMap.isEmpty()) {
                        displayStationPlot = true;
                        synchronized (displayStationPlotBoolList) {
                            for (Boolean b : displayStationPlotBoolList) {
                                displayStationPlot = displayStationPlot && b;
                            }
                        }
                        synchronized (mapOfStnidsWithStns) {
                            if (displayStationPlot) {
                                mapOfStnidsWithStns.put(
                                        currentStation.info.stationId,
                                        currentStation);
                            } else {
                                mapOfStnidsWithStns
                                        .remove(currentStation.info.stationId);
                            }
                        }
                    } else {
                        mapOfStnidsWithStns.put(currentStation.info.stationId,
                                currentStation);
                    }
                }
            }
        }

        Tracer.print("< Exit");

        return (mapOfStnidsWithStns.values());
    }

    private boolean stationHasAllParametersItNeeds(Station station,
            String[] namesOfNeededParameters) {
        nextNeededParam: for (String neededParameterName : namesOfNeededParameters) {
            AbstractMetParameter neededAMP = dbParamToMetParamMap
                    .get(neededParameterName);
            if (neededAMP == null) {
                continue;
            }
            Class<? extends AbstractMetParameter> classOfNeededParameter = neededAMP
                    .getClass();
            for (AbstractMetParameter amp : station.parametersToPlot) {
                Class<? extends AbstractMetParameter> classOfActualParameter = amp
                        .getClass();
                if (classOfActualParameter.equals(classOfNeededParameter)) {
                    continue nextNeededParam;
                }
            }
            Tracer.print("Station " + station.info.stationId + " at "
                    + Tracer.shortTimeString(station.info.dataTime)
                    + " needs parameter " + classOfNeededParameter.toString());
            return false;
        }
        return true;
    }

    private Collection<Station> requestSurfaceData(DataTime time,
            List<Station> stationsRequestingData,
            int stationsRetrievedThisCallCount) {
        Tracer.print("> Entry  " + Tracer.shortTimeString(time));

        /*
         * Overview of large steps in this method below (which we should
         * probably break out into separate methods)...
         * 
         * 1. Put all Station objects in local stationMap
         * 
         * 2. Request point data
         * 
         * ** For each PointDataView (PDV, in this case, Station), do following
         * steps 3-11... ***
         * 
         * 3. Get lat/lon from PDV, use as key to stationMap, get Station,
         * sanity check
         * 
         * 4. For each parameter to plot, get AMP object, insert as target of
         * parametersToPlot map entry
         * 
         * 5. For each String in dbParamNamesForQuery... - set met param from
         * PDV - put in parametersToPlot and dbParametersMap - add boolean to
         * displayStationPlotBoolList
         * 
         * 6. Handle derived parameters
         * 
         * 7. Validate the station against a conditional derived MetParameter
         * 
         * 8. Build list of parametersToPlot for this Station
         * 
         * 9. Process the conditional parameter(s) (if any) for the station
         * 
         * 10. Evaluate the station against the conditional filter to decide if
         * it needs to be plotted at all
         * 
         * 11. Update parametersToPlot map
         */

        // --------------------------------------------------------------------------

        //
        // 1. Put all Station objects in local stationMap
        //

        Map<String, Station> stationMap = new HashMap<String, Station>(
                stationsRequestingData.size());
        if (stationsRequestingData != null && !stationsRequestingData.isEmpty()) {
            try {
                int listSize = stationsRequestingData.size();
                Tracer.print(Tracer.shortTimeString(time)
                        + " stationsRequestingData has " + listSize
                        + " entries" + "\n"
                        + Tracer.printableStationList(stationsRequestingData));
                Map<String, RequestConstraint> map = new HashMap<String, RequestConstraint>();

                map.put("pluginName", constraintMap.get("pluginName"));
                Tracer.print(Tracer.shortTimeString(time) + " putting '"
                        + constraintMap.get("pluginName")
                        + "' as pluginName entry in map");

                RequestConstraint rc = new RequestConstraint();
                RequestConstraint timeConstraint = new RequestConstraint();
                timeConstraint.setConstraintType(ConstraintType.IN);
                rc.setConstraintType(ConstraintType.IN);
                PluginPlotProperties plotProp = PluginPlotProperties
                        .getPluginProperties(map);
                Map<String, DataTime> stationIdToDataTimeMap = new HashMap<String, DataTime>(
                        listSize);

                synchronized (stationsRequestingData) {
                    for (Station currentStation : stationsRequestingData) {

                        if (stationHasAllParametersItNeeds(currentStation,
                                namesOfParametersToQuery)) {
                            Tracer.printX("Skipping data request for station "
                                    + currentStation.info.stationId
                                    + " because it already has all met params it needs");
                            continue;
                        }

                        // Remember association between stationId and its (one!)
                        // matched time for this frame. Will use to filter out
                        // multiple station returns (in case of shared obs
                        // times) later.
                        stationIdToDataTimeMap.put(
                                currentStation.info.stationId,
                                currentStation.info.dataTime);

                        if (plotProp.hasDistinctStationId) {
                            Tracer.print(Tracer.shortTimeString(time)
                                    + " "
                                    + currentStation.info.stationId
                                    + " plotProp.hasDistinctStationId TRUE; adding stationId to constraint value list ");
                            rc.addToConstraintValueList(currentStation.info.stationId);
                            // timeConstraint strings added all at once below
                        } else {
                            Tracer.print(Tracer.shortTimeString(time)
                                    + " "
                                    + currentStation.info.stationId
                                    + " plotProp.hasDistinctStationId FALSE; adding dataURI "
                                    + currentStation.info.dataURI
                                    + " to constraint value list");
                            rc.addToConstraintValueList(currentStation.info.dataURI);
                        }

                        Tracer.print(Tracer.shortTimeString(time)
                                + " "
                                + currentStation.info.stationId
                                + " station entered into stationMap with key "
                                + formatLatLonKey(currentStation.info.latitude,
                                        currentStation.info.longitude));
                        stationMap.put(
                                formatLatLonKey(currentStation.info.latitude,
                                        currentStation.info.longitude),
                                currentStation);

                    }
                }

                //
                // 2. Request point data
                //

                Tracer.print("Requesting SURFACE data for "
                        + stationIdToDataTimeMap.size() + " out of "
                        + stationsRequestingData.size() + " stations");

                stationsRetrievedThisCallCount = stationIdToDataTimeMap.size();

                if (stationIdToDataTimeMap.isEmpty()) {
                    return (stationsRequestingData);
                }

                if (plotProp.hasDistinctStationId) {
                    Tracer.print(Tracer.shortTimeString(time)
                            + " Done with station loop; plotProp.hasDistinctStationId TRUE; adding location.stationId-to-rc entry to map");
                    map.put("location.stationId", rc);
                    // sort data times and remove duplicates...
                    SortedSet<DataTime> allDataTimesSortedSet = new TreeSet<DataTime>(
                            stationIdToDataTimeMap.values());
                    // ...and convert to strings for time request constraint
                    List<String> allDataTimesAsStrings = new ArrayList<String>(
                            allDataTimesSortedSet.size());
                    for (DataTime dt : allDataTimesSortedSet) {
                        allDataTimesAsStrings.add(dt.toString());
                    }
                    timeConstraint
                            .setConstraintValueList(allDataTimesAsStrings);
                    map.put("dataTime", timeConstraint);
                } else {
                    Tracer.print(Tracer.shortTimeString(time)
                            + " Done with station loop; plotProp.hasDistinctStationId FALSE; putting dataURI-to-rc entry in map with rc "
                            + rc);
                    map.put("dataURI", rc);
                }

                Tracer.print("About to query data for frame: "
                        + Tracer.shortTimeString(time)
                        + " datastore query map = " + map);
                boolean displayStationPlot = false;
                long t0 = System.nanoTime();
                PointDataContainer pdc = null;

                pdc = DataCubeContainer.getPointData(plugin,
                        this.namesOfParametersToQuery, null, map);
                long t1 = System.nanoTime();
                Tracer.print("DataCubeContainer.getPointData() took "
                        + (t1 - t0) / 1000000 + " ms for frame "
                        + Tracer.shortTimeString(time));
                Tracer.print("Done with query data for frame: "
                        + Tracer.shortTimeString(time)
                        + " datastore query map = " + map);

                int pdcSize = -1;
                if (pdc == null) {
                    if (!stationIdToDataTimeMap.isEmpty()) {

                        sem1.acquireUninterruptibly();

                        Tracer.print("About to call PointDataRequest.requestPointDataAllLevels(...) for frame: "
                                + Tracer.shortTimeString(time)
                                + "datastore query map = "
                                + map
                                + " Plugin "
                                + this.plugin
                                + " Parameters "
                                + this.namesOfParametersToQuery
                                + " Stations "
                                + stationIdToDataTimeMap.keySet().toArray(
                                        new String[0]));
                        pdc = PointDataRequest.requestPointDataAllLevels(
                                this.plugin,
                                this.namesOfParametersToQuery,
                                stationIdToDataTimeMap.keySet().toArray(
                                        new String[0]), map);
                        Tracer.print("Done with call PointDataRequest.requestPointDataAllLevels(...) for frame: "
                                + Tracer.shortTimeString(time)
                                + "datastore query map = "
                                + map
                                + " Plugin "
                                + this.plugin
                                + " Parameters "
                                + this.namesOfParametersToQuery
                                + " Stations "
                                + stationIdToDataTimeMap.keySet().toArray(
                                        new String[0]));
                        sem1.release();
                    }
                }

                if (pdc != null) {
                    Tracer.print("We have a non-null PDC for frame: "
                            + Tracer.shortTimeString(time)
                            + " datastore query map = "
                            + map
                            + " Plugin "
                            + this.plugin
                            + " Parameters "
                            + Arrays.toString(this.namesOfParametersToQuery)
                            + " Stations "
                            + Arrays.toString(stationIdToDataTimeMap.keySet()
                                    .toArray(new String[0])) + " PDC " + pdc);
                    pdcSize = pdc.getAllocatedSz();
                    Tracer.print("PDC for frame "
                            + Tracer.shortTimeString(time)
                            + " has allocated size " + pdc.getAllocatedSz()
                            + " and current size " + pdc.getCurrentSz());
                    pdc.setCurrentSz(pdcSize);
                    Tracer.print("PDC for frame "
                            + Tracer.shortTimeString(time)
                            + " now has allocated size " + pdc.getAllocatedSz()
                            + " and current size " + pdc.getCurrentSz());
                } else {
                    Tracer.print("< Exit  " + Tracer.shortTimeString(time)
                            + " ABNORMAL?  PDC is null");
                    return stationMap.values();
                }

                Tracer.print("Size of stationMap:    " + stationMap.size());
                Tracer.print("Number of stationIds:  "
                        + stationIdToDataTimeMap.keySet()
                                .toArray(new String[0]).length);
                Tracer.print("pdcSize:               " + pdcSize);

                /*
                 * For each PointDataView (PDV, in this case, corresponding to
                 * one Station), do following steps 3-11... ***
                 */

                // Following lock was added because the following code assumes
                // shared (outer class) instance variable dbParamToMetParamMap
                // is unaltered between write in section 5 and read in section
                // 9, or else conditional coloring can be corrupted by a
                // concurrent process. TODO: See if this sync can be handled
                // better either by a synchronize on the map, or remove
                // dependency on the shared storage by instead using temporary
                // storage in local or thread-local variables.
                sem1.acquireUninterruptibly();

                for (int uriCounter = 0; uriCounter < pdcSize; uriCounter++) {

                    PointDataView pdv = pdc.readRandom(uriCounter);

                    if (pdv == null) {
                        Tracer.print(Tracer.shortTimeString(time)
                                + " PDV is null for station " + uriCounter
                                + " -- skipping");
                        continue;
                    }

                    //
                    // 3. Get lat/lon from PDV, use as key to stationMap, get
                    // Station, sanity check
                    //

                    String key = new String(formatLatLonKey(
                            pdv.getFloat(latDbName), pdv.getFloat(lonDbName)));

                    Station currentStation = stationMap.get(key);
                    if (currentStation == null) {
                        Tracer.print(Tracer.shortTimeString(time) + " "
                                + " stationMap entry not found for key " + key
                                + " -- skipping");
                        continue;
                    }

                    String stationId = currentStation.info.stationId;

                    DataTime dataTime = stationIdToDataTimeMap.get(stationId);

                    // Caution: Single-element constructor; assumes this is an
                    // observation time, and not a forecast time. See below.
                    DataTime retrievedDataTime = new DataTime(new Date(
                            pdv.getLong(refTimeDbName)));

                    // Since the constraints we use (if
                    // plotProp.hasDistinctStationId) are "stationID" IN
                    // list-of-all-stationIDs -AND- dataTime IN
                    // list-of-all-dataTimes, a station could be retrieved
                    // for more data times than its unique time-matched time
                    // (for this frame) -- IF it happens to share another data
                    // time with another station legitimately time-matched to
                    // that other time. Here we check to make sure the time
                    // we retrieved is the one we wanted for this station;
                    // if not, ignore this obs. (An obs with the desired
                    // time should appear elsewhere in the PDC).
                    // Note that we exempt forecast (e.g., MOS) data times
                    // from this check, since we don't retrieve forecast
                    // hour from the DB for retrievedDataTime -- see above.
                    if (!dataTime.getUtilityFlags().contains(FLAG.FCST_USED)
                            && !dataTime.equals(retrievedDataTime)) {
                        Tracer.print(Tracer.shortTimeString(time)
                                + " Retrieved dataTime for station "
                                + stationId + " is " + retrievedDataTime
                                + " but matched dataTime is " + dataTime
                                + " -- skipping");
                        continue;
                    }

                    //
                    // 4. For each parameter to plot, get AMP object, insert as
                    // target of parametersToPlot map entry
                    //

                    synchronized (parametersToPlot) {
                        Set<String> pkeySet = parametersToPlot.keySet();
                        synchronized (pkeySet) {
                            try {
                                for (String prmToPlotKey : pkeySet) {
                                    AbstractMetParameter prmToPlot = parametersToPlot
                                            .get(prmToPlotKey);
                                    if (prmToPlot != null) {
                                        prmToPlot.setValueToMissing();
                                    } else {
                                        // Tracer... prmToPlot==null
                                    }
                                }
                            } catch (Exception e) {
                                statusHandler.handle(Priority.PROBLEM,
                                        "Exception:  " + e.getMessage());
                            }
                        }
                    }
                    List<Boolean> displayStationPlotBoolList = new ArrayList<Boolean>(
                            0);

                    //
                    // 5. For each String in dbParamNamesForQuery...
                    // - set met param from PDV
                    // - put in parametersToPlot and dbParametersMap
                    // - add boolean to displayStationPlotBoolList
                    //

                    synchronized (dbParamNamesForQuery) {

                        for (String dbPrm : dbParamNamesForQuery) {
                            AbstractMetParameter metPrm = dbParamToMetParamMap
                                    .get(dbPrm);
                            if (metPrm == null) {
                                continue;
                            }

                            // get the fillValue from the parameterDescription
                            // and use it to set the missingValue
                            // Sentinel for the metParameter
                            try {
                                ParameterDescription pDesc = pdc
                                        .getDescription(dbPrm);
                                if (pDesc != null) {
                                    if (pdv.getType(dbPrm) == null) {
                                        continue;
                                    }
                                    if (pDesc.getFillValue() == null) {
                                        System.out
                                                .println("Sanity Check: ParameterDescription fill Value is null");
                                        System.out
                                                .println("Update the DataStoreFactory.py and H5pyDataStore.py files");
                                        continue;
                                    }
                                    switch (pdv.getType(dbPrm)) {
                                    case FLOAT:
                                        metPrm.setMissingDataSentinel(pDesc
                                                .getFillValue().floatValue());
                                        break;
                                    case LONG:
                                        metPrm.setMissingDataSentinel(pDesc
                                                .getFillValue().longValue());
                                        break;
                                    case INT:
                                        metPrm.setMissingDataSentinel(pDesc
                                                .getFillValue().intValue());
                                        break;
                                    case STRING:
                                        break;
                                    default:
                                        break;
                                    }
                                }
                            } catch (Exception e) {
                                Tracer.print("param " + dbPrm + " not found.");
                                statusHandler.handle(Priority.PROBLEM,
                                        "Exception:  " + e.getMessage());
                            }

                            /*
                             * Set the value for Met parameters from the
                             * corresponding database value
                             */
                            setMetParamFromPDV(metPrm, pdv, dbPrm, dataTime);

                            if (parametersToPlot.containsKey(metPrm
                                    .getMetParamName())) {
                                parametersToPlot.put(metPrm.getMetParamName(),
                                        metPrm);
                            }

                            dbParamToMetParamMap.put(dbPrm, metPrm);

                            if (condFilterMap != null
                                    && !condFilterMap.isEmpty()) {
                                displayStationPlotBoolList
                                        .add(doesStationPassTheFilterForThisMetParam(metPrm));
                            }

                        }
                    }

                    //
                    // 6. Handle derived parameters
                    //

                    List<AbstractMetParameter> metParamsToDisplay;

                    /*
                     * If the raw report contains temperature or dewpoint in
                     * tenths (seperate field in hdf5) change the value of the
                     * temperature and/or dewpoint met parameters to contain the
                     * value in tenths instead of less granular integer value
                     * given.
                     */

                    Double dptt = dbParamToMetParamMap.get("dpFromTenths")
                            .getValue().doubleValue();
                    Double att = dbParamToMetParamMap.get("tempFromTenths")
                            .getValue().doubleValue();
                    if (dbParamToMetParamMap.get("dpFromTenths") != null
                            && dptt != IDecoderConstantsN.NEGATIVE_FLOAT_MISSING
                                    .doubleValue()) {

                        AbstractMetParameter dp = dbParamToMetParamMap
                                .get("dewpoint");
                        dp.setValue(dptt);
                        dbParamToMetParamMap.put("dewpoint", dp);
                    }

                    if (dbParamToMetParamMap.get("tempFromTenths") != null
                            && att != IDecoderConstantsN.NEGATIVE_FLOAT_MISSING
                                    .doubleValue()) {

                        AbstractMetParameter at = dbParamToMetParamMap
                                .get("temperature");
                        at.setValue(att);
                        dbParamToMetParamMap.put("temperature", at);
                    }

                    Collection<AbstractMetParameter> collectionOfMetParamsWithDBValues = dbParamToMetParamMap
                            .values();

                    synchronized (derivedParameters) {
                        for (AbstractMetParameter derivedParam : derivedParameters) {
                            try {
                                synchronized (collectionOfMetParamsWithDBValues) {
                                    derivedParam
                                            .derive(collectionOfMetParamsWithDBValues);
                                }
                                AbstractMetParameter clonedDerivedPrm = newInstance(derivedParam);
                                if (clonedDerivedPrm == null) {
                                    Tracer.print(Tracer.shortTimeString(time)
                                            + " "
                                            + Tracer.shortTimeString(dataTime)
                                            + " clonedDerivedPrm NULL "
                                            + currentStation.info.stationId
                                            + " " + derivedParam
                                            + " -- skipping");
                                    continue;
                                }
                                clonedDerivedPrm.setValidTime(dataTime);
                                currentStation.parametersToPlot
                                        .add(clonedDerivedPrm);
                                allMetParamsMap.put(
                                        clonedDerivedPrm.getMetParamName(),
                                        clonedDerivedPrm);
                                // Save the derived parameter so it gets painted
                                if (parametersToPlot
                                        .containsKey(clonedDerivedPrm
                                                .getMetParamName())) {
                                    parametersToPlot.put(
                                            clonedDerivedPrm.getMetParamName(),
                                            clonedDerivedPrm);
                                }
                            } catch (NotDerivableException e) {
                                statusHandler.handle(
                                        Priority.PROBLEM,
                                        "NotDerivableException:  "
                                                + e.getMessage());
                            }
                        }// end for derivedParam loop
                    }

                    /*
                     * 7. Validate the station against a conditional derived
                     * MetParameter
                     * 
                     * foreach cond met param in condDerivedMetParams foreach
                     * AMP in derivedParameters
                     */
                    if (condFilterMap != null && !condFilterMap.isEmpty()) {
                        for (String condMetParamName : condDerivedMetParamNames) {
                            synchronized (derivedParameters) {
                                for (AbstractMetParameter condDerivedParamToCheck : derivedParameters) {
                                    if (condDerivedParamToCheck
                                            .getMetParamName().equals(
                                                    condMetParamName)) {
                                        if (condDerivedParamToCheck
                                                .hasValidValue()) {
                                            displayStationPlotBoolList
                                                    .add(doesStationPassTheFilterForThisMetParam(condDerivedParamToCheck));
                                        }
                                    }
                                }
                            }
                        }
                    }

                    //
                    // 8. Build list of parametersToPlot for this Station
                    //

                    /*
                     * Clear the existing list of parameters to plot in each
                     * station - to guarantee an updated list if there is a
                     * re-query for parameters by editing the plot model
                     */
                    if (!currentStation.parametersToPlot.isEmpty()) {
                        currentStation.parametersToPlot.clear();
                    }
                    metParamsToDisplay = new ArrayList<AbstractMetParameter>(
                            parametersToPlot.values());

                    synchronized (metParamsToDisplay) {
                        try {
                            for (AbstractMetParameter metParam : metParamsToDisplay) {
                                /*
                                 * Creating a fresh copy of the met parameter
                                 * seems to be the only way that each station
                                 * retains a unique set of values as queried (or
                                 * derived).Otherwise all stations in a frame
                                 * get the MetParameter values of the last
                                 * station being processed since the list
                                 * currentStation.listOfParamsToPlot references
                                 * the AbstractMetParametervalues from
                                 * paramsToPlot
                                 */
                                AbstractMetParameter newPrm = newInstance(metParam);
                                if (newPrm == null) {
                                    continue;
                                }

                                currentStation.parametersToPlot.add(newPrm);
                            }

                        } catch (Exception e) {
                            statusHandler.handle(Priority.PROBLEM,
                                    "Exception:  " + e.getMessage());
                        }
                    }

                    /*
                     * 9. Process the conditional parameter(s) (if any) for the
                     * station
                     */
                    if (condColoringParamNames != null
                            && !condColoringParamNames.isEmpty()) {
                        Collection<AbstractMetParameter> dbMetParamColl = dbParamToMetParamMap
                                .values();
                        synchronized (condColoringParamNames) {
                            for (String condColorParamName : condColoringParamNames) {
                                currentStation = processConditionalParameterForOneStation(
                                        dbMetParamColl, currentStation,
                                        condColorParamName);
                                currentStation = processConditionalParameterForOneStation(
                                        derivedParameters, currentStation,
                                        condColorParamName);
                            }
                        }
                    }

                    /*
                     * 10. Evaluate the station against the conditional filter
                     * to decide if it needs to be plotted at all
                     */

                    if (condFilterMap != null && !condFilterMap.isEmpty()) {
                        displayStationPlot = true;
                        synchronized (displayStationPlotBoolList) {
                            for (Boolean b : displayStationPlotBoolList) {
                                displayStationPlot = displayStationPlot && b;
                            }
                        }

                        synchronized (stationMap) {
                            if (displayStationPlot) {
                                stationMap.put(key, currentStation);
                            } else {
                                stationMap.remove(key);
                            }
                        }
                    } else {
                        stationMap.put(key, currentStation);
                    }

                    //
                    // 11. Update parametersToPlot map
                    //

                    synchronized (parametersToPlot) {
                        Set<String> pkeySet = parametersToPlot.keySet();
                        synchronized (pkeySet) {
                            try {
                                for (String prmToPlotKey : pkeySet) {
                                    AbstractMetParameter prmToPlot = parametersToPlot
                                            .get(prmToPlotKey);
                                    if (prmToPlot != null) {
                                        prmToPlot.setValueToMissing();
                                    }
                                }

                            } catch (Exception e) {
                                statusHandler.handle(Priority.PROBLEM,
                                        "Exception:  " + e.getMessage());
                            }
                        }
                    }

                } // end foreach URI counter (Station)

            } catch (VizException e) {
                statusHandler.handle(Priority.PROBLEM,
                        "Exception:  " + e.getMessage());
            } catch (DataCubeException e1) {
                statusHandler.handle(Priority.PROBLEM, "DataCubeException:  "
                        + e1.getMessage());
            }

        }

        sem1.release();

        String stations = "";
        for (Station s : stationMap.values()) {
            stations += (" " + s.info.stationId);
        }

        Tracer.print("< Exit    " + Tracer.shortTimeString(time) + stations);
        return stationsRequestingData;
    }

    private void setMetParamFromPDV(AbstractMetParameter metPrm,
            PointDataView pdv, String dbParam, DataTime dt) {

        Tracer.printX("> Entry");
        metPrm.setValueToMissing();
        metPrm.setValidTime(dt);
        Type pType = pdv.getType(dbParam);

        // If this is an array then attempt to determine which
        // value in the array to use to set the metParameter.
        //
        if (pdv.getDimensions(dbParam) > 1) {

            PlotParameterDefn pltPrmDefn = prioritySelectionsMap.get(dbParam);
            if (pltPrmDefn == null) {
                return;
            }

            // if there is a priority ranking for this parameter
            //
            if (pltPrmDefn.getPrioritySelector() != null) {

                // S2N only for string lookups
                if (metPrm.hasStringValue()) {
                    String dbVals[] = pdv.getStringAllLevels(dbParam);

                    String rankedValue = pltPrmDefn.getPrioritySelector()
                            .getRankedField(dbVals);

                    metPrm.setStringValue(rankedValue);
                    return;
                } else {
                    System.out.println("Param " + dbParam
                            + " must be a string to do a priority select from "
                            + "the array of values.");
                    metPrm.setValueToMissing();
                    return;
                }
            }

            // if no arrayIndex given, just get the first in the list
            int arrayIndex = pltPrmDefn.getArrayIndex();

            if (pType == Type.STRING) {
                String dbVals[] = pdv.getStringAllLevels(dbParam);

                if (arrayIndex >= dbVals.length) {
                    metPrm.setValueToMissing();
                    return;
                }

                if (metPrm.hasStringValue()) {
                    metPrm.setStringValue(dbVals[arrayIndex]);
                } else { // parse a number from the string
                    metPrm.setValueFromString(dbVals[arrayIndex].toString(),
                            pdv.getUnit(dbParam));
                }
            } else {
                Number dbVals[] = pdv.getNumberAllLevels(dbParam);

                if (arrayIndex >= dbVals.length) {
                    metPrm.setValueToMissing();
                    return;
                }

                // TODO : should we allow this?
                if (metPrm.hasStringValue()) {
                    metPrm.setStringValue(dbVals[arrayIndex].toString());
                } else {
                    metPrm.setValue(dbVals[arrayIndex], pdv.getUnit(dbParam));
                }
            }
        } else { // set the metParam
            if (metPrm.hasStringValue()) {
                if (pType == Type.STRING) {
                    metPrm.setStringValue(pdv.getString(dbParam));
                } else {
                    if (pType == Type.INT) {
                        Integer tempInt = new Integer(pdv.getInt(dbParam));
                        metPrm.setStringValue(tempInt.toString());
                    }
                }
            } else { // metPrm is a number
                if (pType == Type.STRING) {
                    // parse a number from the string
                    metPrm.setValueFromString(pdv.getString(dbParam),
                            pdv.getUnit(dbParam));
                } else {
                    metPrm.setValue(pdv.getNumber(dbParam),
                            pdv.getUnit(dbParam));
                }
            }
        }
        Tracer.printX("< Exit");
    }

    public synchronized Station processConditionalParameterForOneStation(
            Collection<AbstractMetParameter> metPrmCollection,
            Station currentStation, String condColorParamName) {
        Tracer.print("> Entry  " + currentStation.info.toString());
        synchronized (metPrmCollection) {
            for (AbstractMetParameter thisCondColorParam : metPrmCollection) {
                if (condColorParamName.equals(thisCondColorParam
                        .getMetParamName())) {
                    AbstractMetParameter newPrm = newInstance(thisCondColorParam);
                    if (newPrm == null) {
                        Tracer.print("ERROR!  newPrm == null !!");
                        continue;
                    }
                    currentStation.conditionalColorParameters.add(newPrm);
                }
            }
        }
        Tracer.print("< Exit  " + currentStation.info.toString());
        return currentStation;
    }

    private AbstractMetParameter newInstance(
            AbstractMetParameter paramToInstantiate) {
        Tracer.printX("> Entry");
        AbstractMetParameter instantiatedPrm = null;
        try {
            instantiatedPrm = paramToInstantiate.getClass().newInstance();
            if (paramToInstantiate.hasValidValue()) {
                instantiatedPrm.setValidTime(paramToInstantiate.getValidTime());
                if (!paramToInstantiate.isUseStringValue()) {
                    instantiatedPrm.setValueAs(paramToInstantiate.getValue(),
                            paramToInstantiate.getUnitStr());
                } else {
                    instantiatedPrm.setUseStringValue(paramToInstantiate
                            .isUseStringValue());
                    instantiatedPrm.setStringValue(paramToInstantiate
                            .getStringValue());
                }
                // Also repopulate the PTND dependency ( PTSY ) back
                // into the PTND combination metparameter
                if (paramToInstantiate.getMetParamName().equalsIgnoreCase(
                        PressureChange3HrAndTendency.class.getSimpleName())
                        || paramToInstantiate.getMetParamName()
                                .equalsIgnoreCase(
                                        PressChange3Hr.class.getSimpleName())) {
                    instantiatedPrm.setAssociatedMetParam(paramToInstantiate
                            .getAssociatedMetParam());
                }
            }
        } catch (InstantiationException ie) {
            statusHandler.handle(Priority.PROBLEM, "InstantiationException:  "
                    + ie.getMessage());
            instantiatedPrm = null;
        } catch (IllegalAccessException iae) {
            statusHandler.handle(Priority.PROBLEM, "IllegalAccessException:  "
                    + iae.getMessage());
            instantiatedPrm = null;
        }
        Tracer.printX("< Exit");
        return instantiatedPrm;
    }

    private String formatLatLonKey(Number lat, Number lon) {
        return new String("" + Math.round(lat.doubleValue() * 1000.0) + ","
                + Math.round(lon.doubleValue() * 1000.0));
    }

    private final class GetDataTask implements Runnable {
        List<Station> stationsRequestingData;

        DataTime time;

        GetDataTask(Collection<Station> stationsRequestingData, DataTime time) {
            Tracer.print("> Entry");
            Tracer.print("Creating a GetDataTask for the frame time: "
                    + Tracer.shortTimeString(time) + " with "
                    + stationsRequestingData.size() + " stations");
            this.time = new DataTime(time.getRefTime());
            this.stationsRequestingData = new ArrayList<Station>(
                    stationsRequestingData);
            Tracer.print("< Exit");
        }

        @Override
        public void run() {
            Tracer.print("> Entry  START TASK " + Tracer.shortTimeString(time));
            if (levelStr == null || stationsRequestingData.isEmpty()) {
                listener.retrievalAborted(time);
            }

            Collection<Station> stationsWithData = new ArrayList<Station>(0);
            long t0 = System.nanoTime();

            int stationsRetrievedThisCallCount = 0;
            if (levelStr.equals("Surface")) {
                stationsWithData = requestSurfaceData(time,
                        stationsRequestingData, stationsRetrievedThisCallCount);
            } else {
                stationsWithData = requestUpperAirData(stationsRequestingData,
                        stationsRetrievedThisCallCount);
            }

            long t1 = System.nanoTime();

            Tracer.print("Finished getting data for "
                    + (stationsWithData == null ? 0 : stationsWithData.size())
                    + " stations in " + (t1 - t0) / 1000000 + " ms for frame: "
                    + Tracer.shortTimeString(time));

            if (canceling) {
                Tracer.print("CANCEL in progress; no plot creation will occur for frame "
                        + Tracer.shortTimeString(time));
                listener.retrievalAborted(time);
            } else if (stationsRequestingData.isEmpty()) {
                Tracer.print("*NO* stations; no plot creation will occur for frame "
                        + Tracer.shortTimeString(time));
                listener.retrievalAborted(time);
            } else {
                boolean isThereAConditionalFilter = false;
                listener.retrievalComplete(time, stationsRequestingData,
                        stationsRetrievedThisCallCount,
                        isThereAConditionalFilter);
            }

            Tracer.print("< Exit   END TASK   " + Tracer.shortTimeString(time));

        }
    }

}