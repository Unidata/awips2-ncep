package gov.noaa.nws.ncep.viz.cloudHeight;

import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.measure.UnitConverter;
import javax.xml.bind.JAXBException;

import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.locationtech.jts.geom.Coordinate;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;

import com.raytheon.uf.common.geospatial.MapUtil;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.viz.core.IDisplayPane;
import com.raytheon.uf.viz.core.drawables.ResourcePair;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.rsc.LoadProperties;
import com.raytheon.uf.viz.core.rsc.ResourceList;

import gov.noaa.nws.ncep.edex.common.metparameters.AbstractMetParameter;
import gov.noaa.nws.ncep.edex.common.metparameters.AirTemperature;
import gov.noaa.nws.ncep.edex.common.metparameters.Amount;
import gov.noaa.nws.ncep.edex.common.metparameters.DewPointTemp;
import gov.noaa.nws.ncep.edex.common.metparameters.HeightAboveSeaLevel;
import gov.noaa.nws.ncep.edex.common.metparameters.PressureLevel;
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.NcUnits;
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PCLibrary;
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary;
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PSLibrary;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingCube;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingCube.QueryStatus;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingLayer;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingLayer2;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingProfile;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingProfile.ObsSndType;
import gov.noaa.nws.ncep.gempak.parameterconversionlibrary.GempakConstants;
import gov.noaa.nws.ncep.viz.cloudHeight.CloudHeightResource.StationData;
import gov.noaa.nws.ncep.viz.cloudHeight.soundings.SoundingLevels.LevelValues;
import gov.noaa.nws.ncep.viz.cloudHeight.soundings.SoundingModel;
import gov.noaa.nws.ncep.viz.cloudHeight.soundings.SoundingModelReader;
import gov.noaa.nws.ncep.viz.cloudHeight.ui.CloudHeightDialog;
import gov.noaa.nws.ncep.viz.cloudHeight.ui.CloudHeightDialog.ComputationalMethod;
import gov.noaa.nws.ncep.viz.cloudHeight.ui.CloudHeightDialog.PixelValueMethod;
import gov.noaa.nws.ncep.viz.cloudHeight.ui.CloudHeightDialog.SoundingDataSourceType;
import gov.noaa.nws.ncep.viz.localization.NcPathManager;
import gov.noaa.nws.ncep.viz.localization.NcPathManager.NcPathConstants;
import gov.noaa.nws.ncep.viz.rsc.satellite.rsc.NcSatelliteResource;
import gov.noaa.nws.ncep.viz.rsc.satellite.units.NcIRPixelToTempConverter;
import gov.noaa.nws.ncep.viz.soundingrequest.NcSoundingQuery;
import si.uom.SI;

/**
 * Cloud Height Processor
 *
 *
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer        DescriptionN
 * ------------ ----------  ----------- --------------------------
 * 05/19/2009   #106        Greg Hull       Created
 * 07/22/2009               M. Li           TO10 -> TO11
 * 09/27/2009   #169        Greg Hull       NCMapEditor
 * 03/04/2009               M. Gao          Using localization extension to replace NmapCommon class
 * 05/23/2010               G. Hull         Use ICloudHeightCapable. Use UnitConverter (don't assume Celsius)
 * 11/18/2010   327         M. Li           add isAlreadyOpen
 * 01/05/2011   393         Archana         Added logic to compute the cloud-height using station data
 * 02/28/2011   393         Archana         Added logic to compute the cloud height
 *                                          using the moist adiabatic method
 * 03/09/2011   393         Archana         Added logic to implement pixel selection from a pixel
 *                                          area around the user clicked point.
 * 09/14/2011   457         S. Gurung       Renamed H5UAIR to NCUAIR
 * 10/06/2011   465         Archana         Updated to use NcSoundingQuery2 and NcSoundingLayer2
 * 11/18/2011               G. Hull         replace calls to getValue() with getValueAs(unit)
 * 01/27/2012   583         B. Hebbard      Fix unit deserialization issue; replace ByteDataPreparer (etc.)
 *                                          with RTS-refactored equivalents (package dataprep-->dataformat)
 * 02/03/2012   583         B. Hebbard      In getPixelValueFromTheUserClickedCoordinate remove File.exists()
 *                                          check, which will fail for non-local HDF5
 * 03/01/2012   524         B. Hebbard      When multiple cloud levels found, make "primary" the lowest
 * 04/16/2012   524/583     B. Hebbard      Give more detailed messages when "Unable to compute Cloud Height"
 *                                          for common cases "No sounding data available for selected location"
 *                                          and "Cloud temperature warmer than entire sounding"
 * 05/21/2012   524         B. Hebbard      Fix regression:  Null pointer exception on start with no SAT IR.
 * 02/11/2013   972         G. Hull         IDisplayPane instead of NCDisplayPane
 * 10/13/2013               T. Lee          Fixed station data retrieval; Fixed moist adiabatic computation error;
 *                                          Added climate cloud height
 * 03/07/2014   2791        bsteffen        Move Data Source/Destination to numeric plugin.
 * 07/14/2015   RM#9173     Chin Chen       Use NcSoundingQuery to query ncuair sounding data
 * 06/09/2016   R18511      K. Bugenhagen   Change due to refactoring. Cleanup: removed system.outs, stacktrace prints, commented code.
 * 07/26/2016   R19277      bsteffen        Move redundant code into McidasRecord.getGridGeometry()
 * 08/31/2016   R15716      S. Russell      Repaired, refactored, and renamed
 *                                          getPixelValue() into
 *                                          getDataAtCoordinate(). Updated
 *                                          getSoundingFromStationData() to
 *                                          fix left over status messages.
 * 11/07/2018   #7552       dgilling        Allow tool to work with arbitrary
 *                                          NcSatResources.
 * </pre>
 */
public class CloudHeightProcesser {

    private static IUFStatusHandler statusHandler = UFStatus
            .getHandler(CloudHeightProcesser.class);

    private final CoordinateReferenceSystem DEFAULT_MAP_PROJECTION = DefaultGeographicCRS.WGS84;

    private CloudHeightDialog cldHghtDlg = null;

    private CloudHeightResource cldHghtRsc = null;

    private NcSatelliteResource satRsc = null;

    private UnitConverter tempUnitsConverter = null;

    private UnitConverter celsiusToKelvinConverter = null;

    private NcIRPixelToTempConverter pixelToTemperatureConverter = null;

    private IDisplayPane seldPane;

    private int maxIntervalInHoursForValidStationData;

    private ArrayList<SoundingModel> sndingModels = null;

    private String sndingSrcStr = null;

    private String sndingSrcTimeStr = null;

    private double sndingSrcDist;

    private DataTime satelliteImageTime = null;

    // options (real defaults not set here)
    private SoundingDataSourceType sndingDataSrc = SoundingDataSourceType.STANDARD_ATM;

    private Double maxSndingDist = null;

    private ComputationalMethod compMthd = ComputationalMethod.STANDARD;

    private boolean useSnglPix = false;

    private int pixAreaRad = 10;

    private PixelValueMethod pixValMthd = PixelValueMethod.MAX_VALUE;

    private String currSndingDataSource = null;

    private List<LevelValues> soundingData = null;

    private List<NcSoundingLayer2> aListOfNcSoundingLayers = null;

    private float bli;

    private String algo = "Cloud height by moist-adiabatic method...  ";

    private final String STD_SOUNDING_DATA_SOURCE = "Standard";

    StationData stnData = null;

    public static class CloudHeightData {
        // in meters
        protected double cloudHght;

        protected double cloudPres;

        CloudHeightData(double hght, double prs) {
            cloudHght = hght;
            cloudPres = prs;
        }
    }

    public CloudHeightProcesser(IDisplayPane p, CloudHeightDialog dlg)
            throws VizException {
        seldPane = p;
        cldHghtDlg = dlg;
        aListOfNcSoundingLayers = new ArrayList<>(0);
        // tell the dialog what units the data values will be in.
        cldHghtDlg.setWorkingUnits(SI.METRE, SI.CELSIUS, SI.METRE);
        celsiusToKelvinConverter = SI.CELSIUS.getConverterTo(SI.KELVIN);
        pixelToTemperatureConverter = new NcIRPixelToTempConverter();
        // set cldHghtRsc and satRsc
        getResources();

        File sndingMdlFile = NcPathManager.getInstance().getStaticFile(
                NcPathConstants.CLOUD_HEIGHT_SOUNDING_MODELS);
        if (sndingMdlFile == null || !sndingMdlFile.exists()) {
            throw new VizException("Error getting SoundingModels file?");
        }

        SoundingModelReader sndingMdlRdr = new SoundingModelReader(
                sndingMdlFile.getAbsolutePath());

        try {
            sndingModels = (ArrayList<SoundingModel>) sndingMdlRdr
                    .getSoundingModels();
        } catch (JAXBException e) {
            statusHandler.error("Error getting sounding models", e);
        }
    }

    public NcSatelliteResource getSatResource() {
        return satRsc;
    }

    public void setPane(IDisplayPane newPane) {
        if (seldPane == newPane) {
            return;
        }

        removeCloudHeightResource();
        seldPane = newPane;
        getResources();
    }

    public void processCloudHeight(Coordinate latlon, boolean mouseDown) {
        List<CloudHeightData> cloudHeights = new ArrayList<>(0);

        if (cldHghtDlg != null && cldHghtDlg.isOpen() && latlon != null) {

            cldHghtDlg.displayStatusMsg("");

            if (satRsc == null) {
                getResources();

                if (satRsc == null) {
                    cldHghtDlg.clearFields();
                    cldHghtDlg
                            .displayStatusMsg("Satellite IR Image is not loaded.");
                    return;
                }
            }

            // a user could have unloaded the cloud height resource
            if (cldHghtRsc == null) {
                getResources();
            }

            String pixValStr = "N/A"; // the 'raw' data
            Double tempC = new Double(0.0);
            Double pixVal = Double.NaN;

            // get the options
            if (mouseDown) {
                sndingDataSrc = cldHghtDlg.getSoundingDataSourceType();
                // in working units of meters
                maxSndingDist = cldHghtDlg.getMaxSoundingDist();

                compMthd = cldHghtDlg.getComputationalMethod();
                pixValMthd = cldHghtDlg.getPixelValueMethod();
                pixAreaRad = cldHghtDlg.getPixelAreaDimension();
                useSnglPix = cldHghtDlg.isPixelValueFromSinglePixel();
            }
            pixVal = getDataAtCoordinate(latlon, satRsc, useSnglPix,
                    pixAreaRad, pixValMthd);
            if (pixVal != null) {
                NumberFormat nf = new DecimalFormat("####");
                pixValStr = nf.format(pixVal);
            }

            Double tempK = null;
            if (pixelToTemperatureConverter != null && pixVal != null) {
                tempK = pixelToTemperatureConverter.convert(pixVal
                        .doubleValue());
            }

            if (tempK == null) {
                cldHghtDlg
                        .displayStatusMsg("Error: Unable to compute the brightness temperature.");
                return;
            } else {
                tempC = SI.KELVIN.getConverterTo(SI.CELSIUS).convert(
                        tempK.doubleValue());
            }
            // get the sounding data
            if (sndingDataSrc == SoundingDataSourceType.STANDARD_ATM) {
                sndingSrcStr = new String("Standard Atm");
                sndingSrcTimeStr = new String("N/A");
                sndingSrcDist = Double.NaN; // flag to display empty

                if (currSndingDataSource != this.STD_SOUNDING_DATA_SOURCE) {
                    currSndingDataSource = this.STD_SOUNDING_DATA_SOURCE;
                }

                for (SoundingModel sndMod : sndingModels) {
                    // TODO : Add checks for Summer/Winter and valid region
                    if (sndMod.getName().equalsIgnoreCase(
                            this.STD_SOUNDING_DATA_SOURCE)) {
                        soundingData = sndMod
                                .getSoundingLevels().getLevelValues();
                        cloudHeights = computeCloudHeights(soundingData, tempC);
                        break;
                    }
                }

                if (cloudHeights.isEmpty()) {
                    cloudHeights = getSoundingFromClimatology(latlon, tempC);
                    if (!cloudHeights.isEmpty()) {
                        cldHghtDlg
                                .displayStatusMsg("Climatology sounding is used...");
                    }

                }

            } else if (sndingDataSrc == SoundingDataSourceType.STATION_DATA) {
                soundingData = getSoundingFromStationData(latlon);
                if (compMthd.compareTo(ComputationalMethod.STANDARD) == 0) {

                    cloudHeights = computeCloudHeights(soundingData, tempC);

                    if ((cloudHeights != null) && cloudHeights.isEmpty()) {
                        if (aListOfNcSoundingLayers != null) {
                            float tempInKelvin = celsiusToKelvinConverter
                                    .convert(tempC).floatValue();
                            if (!(aListOfNcSoundingLayers.isEmpty())) {
                                cloudHeights = moistAdiabaticMethod(
                                        aListOfNcSoundingLayers, new Amount(
                                                tempInKelvin, SI.KELVIN));
                            }
                            if (!cloudHeights.isEmpty()) {
                                cldHghtDlg.displayStatusMsg(algo);
                            }
                        }
                    }
                } else {
                    if (compMthd.compareTo(ComputationalMethod.MOIST_ADIABATIC) == 0) {
                        float tempInKelvin = celsiusToKelvinConverter
                                .convert(tempC).floatValue();
                        cloudHeights = moistAdiabaticMethod(
                                aListOfNcSoundingLayers, new Amount(
                                        tempInKelvin, SI.KELVIN));
                        if (!cloudHeights.isEmpty()) {
                            if (bli > 0) {
                                cldHghtDlg
                                        .displayStatusMsg("Stable sounding...");
                            }
                        }
                    }
                }

                if (((cloudHeights != null) && cloudHeights.isEmpty())) {
                    if (aListOfNcSoundingLayers != null) {
                        float tempInKelvin = celsiusToKelvinConverter
                                .convert(tempC).floatValue();

                        /*
                         * If the moist adiabatic method is unable to return a
                         * valid cloud height or if no station data is returned
                         * for the current frame time ( this happens, because
                         * even if there is UAIR data in the database, if the
                         * 'nil' field in the uair table is set to TRUE, the
                         * UAIR data is considered to be invalid and therefore
                         * not retrieved by NcSoundingQuery ), so we go back by
                         * 'maxIntervalInHoursForValidStationData' hours to
                         * compute a fresh set of the sounding data
                         */
                        if (aListOfNcSoundingLayers.isEmpty()) {
                            cloudHeights = computeCloudHeightFromPreviousStationData(
                                    stnData,
                                    maxIntervalInHoursForValidStationData,
                                    satelliteImageTime, tempInKelvin);
                        }
                    }

                }

            }

            if (tempUnitsConverter != null && tempC != null) {
                // Convert the temperature into the units selected by the user
                tempC = tempUnitsConverter.convert(tempC).doubleValue();
            }

            // Update the GUI.
            cldHghtDlg.setLatLon(latlon.y, latlon.x);

            cldHghtDlg.setSoundingDataSource(sndingSrcStr);

            cldHghtDlg.setSoundingDataTime(sndingSrcTimeStr);

            cldHghtDlg.setSoundingDataDistance(sndingSrcDist);

            cldHghtDlg.setPixelValue(pixValStr);

            cldHghtDlg.setTemperature(tempC);

            cldHghtDlg.clearAltCloudHeights();

            if (cloudHeights.size() == 0) {
                cldHghtDlg.setPrimaryCloudHeight(Double.NaN, Double.NaN);
                cldHghtDlg.appendStatusMsg("Unable to compute Cloud Height");
            } else {
                cldHghtDlg.setPrimaryCloudHeight(
                        cloudHeights.get(cloudHeights.size() - 1).cloudHght,
                        cloudHeights.get(cloudHeights.size() - 1).cloudPres);
                if (cloudHeights.size() > 1) {
                    cldHghtDlg.displayStatusMsg("Multiple Cloud Levels Found.");

                    for (int ch = cloudHeights.size() - 2; ch >= 0; ch--) {
                        cldHghtDlg.addAltCloudHeight(
                                cloudHeights.get(ch).cloudHght,
                                cloudHeights.get(ch).cloudPres);
                    }
                }
            }

            // draw the marker
            cldHghtRsc.setSelectedLoc(latlon);
        }

        seldPane.refresh();
    }

    /**
     * Finds the sounding data for the station closest to the point clicked by
     * the user
     *
     * @param latlon
     *            - the coordinates of the point clicked by the user
     * @return A list of LevelValues for all the soundings from this station
     *         that are time-matched to the satellite image time
     */
    private List<LevelValues> getSoundingFromStationData(Coordinate latlon) {
        List<LevelValues> listOfLevelValues = new ArrayList<>(0);
        if (sndingDataSrc == SoundingDataSourceType.STATION_DATA) {
            sndingSrcTimeStr = "N/A";
            sndingSrcStr = "N/A";
            sndingSrcDist = Double.NaN;
            aListOfNcSoundingLayers = new ArrayList<>(0);

            // Used as a flag, reset this for a new click on the map
            if (!this.aListOfNcSoundingLayers.isEmpty()) {
                this.aListOfNcSoundingLayers.clear();
            }

            maxIntervalInHoursForValidStationData = cldHghtDlg
                    .getMaxValidIntervalInHoursForStationData();
            stnData = cldHghtRsc.getStationData(latlon, maxSndingDist,
                    satelliteImageTime, maxIntervalInHoursForValidStationData);

            if (stnData != null
                    && cldHghtRsc.minimumDistance != cldHghtRsc.INVALID_DISTANCE) {
                if (stnData.stationId != null && !stnData.stationId.isEmpty()) {
                    sndingSrcStr = new String(stnData.stationId);
                }
                sndingSrcTimeStr = new String(stnData.stationRefTime.toString());
                sndingSrcDist = cldHghtRsc.minimumDistance;

                /*
                 * Store the list of NcSoundingLayers in a member variable since
                 * it might be needed for the moist-adiabatic method of
                 * cloud-height computation
                 */
                aListOfNcSoundingLayers = getStationSounding(stnData);
                if (compMthd.compareTo(ComputationalMethod.STANDARD) == 0) {
                    listOfLevelValues = getListOfLevelData(aListOfNcSoundingLayers);
                }
            }
            // TODO could check that the station is actually different than the
            // previous station.

        }
        if (listOfLevelValues.isEmpty()) {
            cldHghtDlg
                    .displayStatusMsg("No sounding data available for selected location \n");
        }
        return listOfLevelValues;
    }

    /***
     * Determines the cloud height using the moist adiabatic method
     *
     * @param listOfNcSoundingLayer
     *            - a list of sounding data from the station closest to the
     *            user-clicked point on the IR image.
     * @param tmpk
     *            - the cloud temperature in Kelvin
     * @return a list of <code>CloudHeightData<code> containing the primary
     * height and pressure for the cloud, if the computations succeed. Otherwise, it returns
     * an empty list of  <code>CloudHeightData<code>.
     */

    private List<CloudHeightData> moistAdiabaticMethod(
            List<NcSoundingLayer2> listOfNcSoundingLayer, Amount tmpk) {
        List<CloudHeightData> cldHgtDataList = new ArrayList<>(0);
        try {
            PressureLevel plev = new PressureLevel();
            plev.setValue(new Amount(600, NcUnits.MILLIBAR));
            NcSoundingLayer2 unstableLevelBelow600mb = PSLibrary.psUstb(
                    listOfNcSoundingLayer, plev);

            if (unstableLevelBelow600mb != null) {
                PressureLevel pAt600mb = unstableLevelBelow600mb.getPressure();
                AirTemperature tAt600mb = unstableLevelBelow600mb
                        .getTemperature();
                DewPointTemp dAt600mb = unstableLevelBelow600mb.getDewpoint();
                Amount pressure = null;
                Amount tmpc = null;
                Amount dwpc = null;
                if (pAt600mb != null && pAt600mb.hasValidValue()) {
                    pressure = new Amount(
                            pAt600mb.getValueAs(NcUnits.MILLIBAR),
                            NcUnits.MILLIBAR);
                }
                if (tAt600mb != null && tAt600mb.hasValidValue()) {
                    tmpc = new Amount(tAt600mb.getValueAs(SI.CELSIUS),
                            SI.CELSIUS);
                }

                if (dAt600mb != null && dAt600mb.hasValidValue()) {
                    dwpc = new Amount(dAt600mb.getValueAs(SI.CELSIUS),
                            SI.CELSIUS);
                }

                if (pressure != null && pressure.hasValidValue()
                        && tmpc != null && tmpc.hasValidValue() && dwpc != null
                        && dwpc.hasValidValue()) {

                    Amount te = PRLibrary.prThte(pressure, tmpc, dwpc);
                    Amount pres500 = new Amount(500, NcUnits.MILLIBAR);
                    Amount tguess = new Amount(0, SI.KELVIN);
                    Amount tmst = PRLibrary.prTmst(te, pres500, tguess);
                    Amount t500 = null;

                    for (NcSoundingLayer2 thisNcSounding : listOfNcSoundingLayer) {
                        if (thisNcSounding.getPressure()
                                .getValueAs(NcUnits.MILLIBAR).floatValue() == 500) {
                            t500 = new Amount(thisNcSounding.getTemperature()
                                    .getValueAs(SI.CELSIUS), SI.CELSIUS);
                            break;
                        }
                    }

                    bli = (((t500 != null && !t500.hasValidValue()) || (tmst != null && !tmst
                            .hasValidValue())) ? GempakConstants.RMISSD : (t500
                            .getValueAs(SI.CELSIUS).floatValue() - tmst
                            .getValueAs(SI.CELSIUS).floatValue()));
                    Amount pmst = PRLibrary.prPmst(te, tmpk);
                    List<NcSoundingLayer2> nearestSoundingLevels = PCLibrary
                            .pcFndl(listOfNcSoundingLayer,
                                    pmst.getValueAs(NcUnits.MILLIBAR)
                                            .floatValue(),
                                    PCLibrary.VerticalCoordinate.PRESSURE,
                                    PCLibrary.SearchOrder.BOTTOM_UP);
                    CloudHeightData newCldHgtData = new CloudHeightData(
                            Float.NaN, Float.NaN);
                    if (nearestSoundingLevels != null
                            && !nearestSoundingLevels.isEmpty()) {
                        // if bli >= 0 and a sounding is obtained- means its a
                        // stable sounding
                        if (PCLibrary.getLocationOfLevel() == PCLibrary.LevelType.BETWEEN_LEVELS) {
                            NcSoundingLayer2 interpolatedSounding;
                            interpolatedSounding = PCLibrary
                                    .interpolateBetweenTwoSoundingLayers(
                                            nearestSoundingLevels,
                                            pmst.getValueAs(NcUnits.MILLIBAR)
                                                    .floatValue(),
                                            PCLibrary.VerticalCoordinate.PRESSURE);
                            newCldHgtData = new CloudHeightData(
                                    interpolatedSounding.getGeoHeight()
                                            .getValueAs(SI.METRE).doubleValue(),
                                    interpolatedSounding.getPressure()
                                            .getValueAs(NcUnits.MILLIBAR)
                                            .doubleValue());
                        } else if (PCLibrary.getLocationOfLevel() == PCLibrary.LevelType.EXACT_MATCH) {
                            NcSoundingLayer2 tempSounding = nearestSoundingLevels
                                    .get(0);
                            newCldHgtData = new CloudHeightData(tempSounding
                                    .getGeoHeight().getValueAs(SI.METRE)
                                    .doubleValue(), tempSounding.getPressure()
                                    .getValueAs(NcUnits.MILLIBAR).doubleValue());
                        }
                    }
                    cldHgtDataList.add(newCldHgtData);

                }

            }

        } catch (Exception e) {
            statusHandler.error("Error determining cloud height", e);
        }
        return cldHgtDataList;
    }

    /**
     * Queries the database to get sounding data for the input station
     *
     * @param stationData
     *            - the station for which sounding data needs to be retrieved
     * @return a list of sounding layers if the database query succeeds or an
     *         empty list otherwise
     */
    private List<NcSoundingLayer2> getStationSounding(StationData stationData) {

        List<NcSoundingLayer2> ncSoundingLayer2List = new ArrayList<>(0);
        String[] stnIdAry = new String[1];
        if (stationData.stationId != null && !stationData.stationId.isEmpty()) {
            stnIdAry[0] = stationData.stationId;
        }
        try {

            long refTime = stationData.stationRefTime.getRefTime().getTime();
            long[] reflTimeAry = { refTime };
            long[] rangeTimeAry = { refTime }; // for ncuair, reange time is
                                               // same as ref time
            NcSoundingCube thisSoundingCube = NcSoundingQuery
                    .genericSoundingDataQuery(reflTimeAry, rangeTimeAry, null,
                            null, null, stnIdAry, ObsSndType.NCUAIR.toString(),
                            NcSoundingLayer.DataType.ALLDATA, true, null, null,
                            true, true, false);

            // TODO -- This shouldn't be necessary, given Amount.getUnit()
            // should now heal itself
            // from a null unit by using the String. see also
            // PlotModelGenerator2.plotUpperAirData()
            // Repair the 'unit' in the met params, if damaged (as in, nulled)
            // in transit.
            if (thisSoundingCube != null
                    && thisSoundingCube.getRtnStatus() == QueryStatus.OK) {
                for (NcSoundingProfile sndingProfile : thisSoundingCube
                        .getSoundingProfileList()) {
                    for (NcSoundingLayer2 sndingLayer : sndingProfile
                            .getSoundingLyLst2()) {
                        for (AbstractMetParameter metPrm : sndingLayer
                                .getMetParamsMap().values()) {
                            metPrm.syncUnits();
                        }
                    }
                }
            }

            if (thisSoundingCube != null) {
                List<NcSoundingProfile> listOfSoundingProfiles = thisSoundingCube
                        .getSoundingProfileList();
                if (listOfSoundingProfiles != null
                        && !listOfSoundingProfiles.isEmpty()) {
                    for (NcSoundingProfile eachSoundingprofile : listOfSoundingProfiles) {
                        ncSoundingLayer2List.addAll(eachSoundingprofile
                                .getSoundingLyLst2());
                    }
                }
            }
        } catch (Exception e) {
            statusHandler.error("Error getting sounding data", e);
        }

        if (ncSoundingLayer2List.isEmpty()) {
            cldHghtDlg
                    .displayStatusMsg(" CloudHeightProcessr: No sounding data found for station "
                            + stationData.stationId + " \n");
        }
        return ncSoundingLayer2List;
    }

    /***
     * Creates a list of <code>LevelValues</code> using the height and pressure
     * of each <code>NcSoundingLayer2</code> in the input list of
     * <code>NcSoundingLayer2</code>
     *
     * @param listOfNcSoundingLayer
     * @return a list of {@code}LevelValues
     */
    private List<LevelValues> getListOfLevelData(
            List<NcSoundingLayer2> listOfNcSoundingLayer) {
        List<LevelValues> aListOfLevelValues = new ArrayList<>(0);
        if (listOfNcSoundingLayer != null && !listOfNcSoundingLayer.isEmpty()) {
            for (NcSoundingLayer2 eachNcSoundingLayer : listOfNcSoundingLayer) {

                LevelValues newLvl = new LevelValues();

                HeightAboveSeaLevel geoHeight = eachNcSoundingLayer
                        .getGeoHeight();
                if (geoHeight != null && geoHeight.hasValidValue()) {
                    double height = geoHeight.getValue().doubleValue();
                    newLvl.setHeight(height);
                }

                PressureLevel pressure = eachNcSoundingLayer.getPressure();
                if (pressure != null && pressure.hasValidValue()) {
                    newLvl.setPressure(pressure.getValue().doubleValue());
                }

                // temperature in the sounding layer is already stored in
                // celsius
                AirTemperature temperature = eachNcSoundingLayer
                        .getTemperature();
                if (temperature != null && temperature.hasValidValue()) {
                    newLvl.setTemperature(temperature.getValueAs(SI.CELSIUS)
                            .doubleValue());
                }
                aListOfLevelValues.add(newLvl);
            }
        }
        return aListOfLevelValues;
    }

    // TODO if the temp is lower than the min Temp in the sounding then should
    // we return posInf and disp ">max"?
    /**
     * Computes the cloud height using the Standard method
     *
     * @param soundingLevels
     *            - list of <code>LevelValues</code>
     * @param tempC
     *            - the cloud temperatue in Celsius
     * @return a list of <code>CloudHeightData</code> containing (one or more)
     *         computed cloud height and pressure information
     */
    private List<CloudHeightData> computeCloudHeights(
            List<LevelValues> soundingLevels, Double tempC) {
        List<CloudHeightData> cldHghts = new ArrayList<>(0);

        double tempMax = -9999.0;
        int lindx = 0;
        double tempCDoubleVal = tempC.doubleValue();
        if (soundingLevels != null && soundingLevels.size() > 0) {
            int soundindLevelListSize = soundingLevels.size();
            for (lindx = soundindLevelListSize - 1; lindx > 0; lindx--) {
                LevelValues topLvl = soundingLevels.get(lindx);
                LevelValues btmLvl = soundingLevels.get(lindx - 1);
                double topTemp = topLvl.getTemperature();
                double btmTemp = btmLvl.getTemperature();

                // logic from NMAP source snghgt.f
                tempMax = (btmTemp > tempMax ? btmTemp : tempMax);

                // find the maximum temperature
                tempMax = (topTemp > tempMax ? topTemp : tempMax);

                double sign = (tempCDoubleVal - topTemp)
                        * (tempCDoubleVal - btmTemp);
                if (sign <= 0.0) {
                    if (sign == 0.0) {
                        if (tempCDoubleVal == topLvl.getTemperature()) {
                            cldHghts.add(new CloudHeightData(
                                    topLvl.getHeight(), topLvl.getPressure()));
                        }
                    } else {
                        /*
                         * TODO: The logic below is partly implemented from the
                         * legacy file pcintt.f Once the entire PC-library is
                         * re-implemented in Java, this may be replaced with a
                         * call to the Java method that contains the logic in
                         * pcintt.f
                         */
                        // interpolate the temp linearly and the pressure
                        // logarithmically.
                        boolean istempCBetweenBtmTempAndTopTemp = false;
                        if (((topTemp < btmTemp) && (topTemp < tempCDoubleVal) && (tempCDoubleVal < btmTemp))
                                || ((btmTemp < topTemp)
                                        && (btmTemp < tempCDoubleVal) && (tempCDoubleVal < topTemp))) {
                            istempCBetweenBtmTempAndTopTemp = true;
                        }
                        if (istempCBetweenBtmTempAndTopTemp) {
                            if (btmLvl.getPressure() != GempakConstants.RMISSD
                                    && (topLvl.getPressure() != GempakConstants.RMISSD)) {
                                double rmult = (tempCDoubleVal - topTemp)
                                        / (btmTemp - topTemp);
                                double plog = Math.log(btmLvl.getPressure())
                                        - Math.log(topLvl.getPressure());
                                cldHghts.add(new CloudHeightData((topLvl
                                        .getHeight() + rmult
                                        * (btmLvl.getHeight() - topLvl
                                                .getHeight())),
                                        (topLvl.getPressure() * Math.exp(rmult
                                                * plog))));
                            }
                        }
                    }
                }
            }

            if (tempCDoubleVal > tempMax) {
                cldHghtDlg
                        .displayStatusMsg("Cloud temperature warmer than entire sounding \n");
            }
        }
        return cldHghts;
    }

    // check for a satellite IR image resource and for
    // an existing cloud height resource or create a new one
    private void getResources() {
        if (cldHghtRsc != null && satRsc != null) {
            return;
        }
        ResourceList rscs = seldPane.getDescriptor().getResourceList();

        for (ResourcePair r : rscs) {
            if (r.getResource() instanceof CloudHeightResource) {
                cldHghtRsc = (CloudHeightResource) r.getResource();
                break;
            } else if (r.getResource() instanceof NcSatelliteResource) {
                if (((NcSatelliteResource) r.getResource())
                        .isCloudHeightCompatible()) {
                    satRsc = (NcSatelliteResource) r.getResource();
                    satelliteImageTime = seldPane.getDescriptor()
                            .getTimeForResource(r.getResource());
                    // create a converter so we will always get the temp in
                    // celsius
                    if (satRsc.getTemperatureUnits() != SI.CELSIUS
                            || (tempUnitsConverter == null)) {
                        tempUnitsConverter = satRsc.getTemperatureUnits()
                                .getConverterTo(SI.CELSIUS);
                    }

                }
            }
        }
        if (cldHghtRsc == null) {
            try {
                CloudHeightResourceData srd = new CloudHeightResourceData();
                cldHghtRsc = srd.construct(new LoadProperties(),
                        seldPane.getDescriptor());
                seldPane.getDescriptor().getResourceList().add(cldHghtRsc);
                cldHghtRsc.init(seldPane.getTarget());
            } catch (VizException e) {
                statusHandler.error("Error creating cloud height resource", e);
            }
            seldPane.refresh();
        }
    }

    public void close() {
        removeCloudHeightResource();
    }

    private void removeCloudHeightResource() {
        satRsc = null;

        if (cldHghtRsc != null) {
            seldPane.getDescriptor().getResourceList().removeRsc(cldHghtRsc);
            cldHghtRsc = null;
            seldPane.refresh();
        }
    }

    /**
     * Gets an array of pixels around the user clicked point
     *
     * @param startingPoint
     *            - the user clicked point
     * @param addToX
     *            - decides whether to add the incremental difference to the
     *            x-coordinate
     * @param size
     *            - number of points in the array
     * @return the array of pixels surrounding the user clicked point.
     */
    private PixelLocation[] generatePixelLocations(PixelLocation startingPoint,
            boolean addToX, int size) {
        PixelLocation[] arrayOfPixelLocations = new PixelLocation[size];
        int counter = -size / 2;
        if (addToX) {
            for (int i = 0; i < size; i++) {
                arrayOfPixelLocations[i] = new PixelLocation(
                        startingPoint.xCoord + counter, startingPoint.yCoord);
                counter++;
            }
        } else {
            for (int i = 0; i < size; i++) {
                arrayOfPixelLocations[i] = new PixelLocation(
                        startingPoint.xCoord, startingPoint.yCoord + counter);
                counter++;
            }
        }
        return arrayOfPixelLocations;
    }

    /**
     * Gets the previous valid sounding data for the input station and uses the
     * moist-adiabatic method to compute the cloud height.
     *
     * @param nearestStationData
     *            - the station closest to the user clicked point on the screen.
     * @param maxInterval
     *            - the maximum time in the past, (in hours) within which the
     *            station data is searched
     * @param imageTime
     *            - satellite image time
     * @param tempInKelvin
     *            - the cloud temperature ( in Kelvin ) at the pixel point
     *            clicked by the user
     * @return the list of <code>CloudHeightData </code>
     */
    private List<CloudHeightData> computeCloudHeightFromPreviousStationData(
            StationData nearestStationData, int maxInterval,
            DataTime imageTime, float tempInKelvin) {
        List<CloudHeightData> cloudHeightData = new ArrayList<>(0);
        if (stnData != null) {
            Calendar limitingCalendar = imageTime.getRefTimeAsCalendar();
            limitingCalendar.add(Calendar.HOUR_OF_DAY, -maxInterval);
            DataTime satelliteImageTimeLimit = new DataTime(limitingCalendar);
            for (int hourToDeduct = 12; hourToDeduct <= maxInterval; hourToDeduct++) {
                Calendar tempCalendar = nearestStationData.stationRefTime
                        .getRefTimeAsCalendar();
                tempCalendar.add(Calendar.HOUR_OF_DAY, -hourToDeduct);
                nearestStationData.stationRefTime = new DataTime(tempCalendar);
                if (satelliteImageTimeLimit
                        .greaterThan(nearestStationData.stationRefTime)) {
                    break;
                }
                this.aListOfNcSoundingLayers = getStationSounding(nearestStationData);
                if (!this.aListOfNcSoundingLayers.isEmpty()) {
                    cloudHeightData = moistAdiabaticMethod(
                            this.aListOfNcSoundingLayers, new Amount(
                                    tempInKelvin, SI.KELVIN));
                    cldHghtDlg.displayStatusMsg(algo);
                }
                if (!cloudHeightData.isEmpty()) {
                    break;
                }
            }
        }
        return cloudHeightData;
    }

    /**
     *
     * Get the satellite data located at the coordinate the user clicked on the
     * map. If the option was chosen get maximum or minimum data value from a
     * rectangular matrix of coordiantes around the point the user clicked
     *
     * @param usrClickdLatLon
     *            - this is the point the user clicked on the map translated
     *            from mouseclicks into LatLon
     * @param satRsc
     *            - a satellite Resource that has implemented the
     *            ICloudHeightCapable Interface
     * @param isSinglePixelNeeded
     *            - from the dialog, did the user want data ONLY from the point
     *            on the map s/he clicked?
     * @param pixelAreaDimension
     *            - the distance beyond the user clicked point from which to
     *            gather more data if chosen
     * @param pixelSelectionMethod
     * @return
     */

    private Double getDataAtCoordinate(Coordinate usrClickdLatLon,
            NcSatelliteResource satRsc, boolean isSinglePixelNeeded,
            int pixelAreaDimension,
            CloudHeightDialog.PixelValueMethod pixelSelectionMethod) {

        Double dataVal = Double.NaN;

        // Abort
        if (satRsc == null) {
            return dataVal;
        }

        double[] userClickedPixelCoord = new double[2];
        double[] in = new double[2];
        double[] out = new double[2];
        MathTransform localProjToLatLon = null;
        MathTransform mtGridToCRS = null;
        MathTransform latLonToLocalProj = null;
        MathTransform invmtCRSToGrid = null;

        int pixelDistFrmUsrClickPt = (pixelAreaDimension * 2) + 1;
        in[0] = MapUtil.correctLon(usrClickdLatLon.x);
        in[1] = MapUtil.correctLat(usrClickdLatLon.y);

        GridGeometry2D gridGeom = satRsc.getGridGeometry();

        try {

            // Get the appropriate coordinate systems to translate LatLon
            // into pixel coordiantes
            localProjToLatLon = CRS.findMathTransform(
                    gridGeom.getCoordinateReferenceSystem(),
                    this.DEFAULT_MAP_PROJECTION);
            latLonToLocalProj = localProjToLatLon.inverse();

            mtGridToCRS = gridGeom.getGridToCRS(PixelInCell.CELL_CORNER);
            invmtCRSToGrid = mtGridToCRS.inverse();

            // Convert the user clicked LatLon into pixel coordiantes
            latLonToLocalProj.transform(in, 0, out, 0, 1);
            invmtCRSToGrid.transform(out, 0, userClickedPixelCoord, 0, 1);

        } catch (Exception e) {
            statusHandler
                    .error("Error transforming the user clicked latlon value into pixel coordiantes",
                            e);
        }

        // If the user ONLY wants data for the point s/he clicked on the map
        if (isSinglePixelNeeded) {
            Double imageValue = satRsc.getRawIRImageValue(usrClickdLatLon);
            dataVal = new Double(imageValue);
        } else {
            // The user wants data from pixels for an entire rectangular area
            // surronging the point s/he clicked on the map.

            double[][] arrayDataValAroundUsrClikdPt = new double[pixelDistFrmUsrClickPt][pixelDistFrmUsrClickPt];

            arrayDataValAroundUsrClikdPt = getDataFrmRectAreaOfPixelCoordsArndClckdPt(
                    userClickedPixelCoord, pixelDistFrmUsrClickPt,
                    pixelAreaDimension, mtGridToCRS, localProjToLatLon, satRsc);

            // Get either maximum or most frequently data value in the
            // rectangular area around the user clicked point
            if (pixelSelectionMethod == PixelValueMethod.MAX_VALUE) {
                dataVal = new Double(
                        getMaxDataVal(arrayDataValAroundUsrClikdPt));
            } else {
                dataVal = new Double(
                        getModeOfDataValues(arrayDataValAroundUsrClikdPt));
            }

        }// end else if(isSinglePixelNeeded)

        return dataVal;
    }

    /**
     * Get the data for a rectangle of pixels arund the pixel coordinate the
     * user clicked on the map
     *
     * @param userClickePixelCoord
     * @param pixelDistFrmUsrClickPt
     * @param pixelAreaDimension
     * @param mtGridToCRS
     * @param localProjToLatLon
     * @param satRsc
     * @return
     */
    private double[][] getDataFrmRectAreaOfPixelCoordsArndClckdPt(
            double[] userClickePixelCoord, int pixelDistFrmUsrClickPt,
            int pixelAreaDimension, MathTransform mtGridToCRS,
            MathTransform localProjToLatLon, NcSatelliteResource satRsc) {

        double[][] arrayOfPixVal = new double[pixelDistFrmUsrClickPt][pixelDistFrmUsrClickPt];
        PixelLocation[][] squarePixelArea = new PixelLocation[pixelDistFrmUsrClickPt][pixelDistFrmUsrClickPt];
        int[] pixCoord = new int[2];

        // Set the pixel coordinates of where the user clicked on the map
        pixCoord[0] = (int) userClickePixelCoord[0];
        pixCoord[1] = (int) userClickePixelCoord[1];

        if (pixCoord == null || pixCoord.length != 2) {
            // Abort
            return null;
        }

        PixelLocation userClickedPoint = new PixelLocation(pixCoord[0],
                pixCoord[1]);

        // For the rectangular area around the user clicked point generate a
        // column of pixel coordinates in the middle of that rectangle
        PixelLocation[] tempPixelArea = this.generatePixelLocations(
                userClickedPoint, false, pixelDistFrmUsrClickPt);
        int midpoint = ((pixelAreaDimension));

        // For each PixelLocaton in the middle column, generate the entire row
        // to complete the rectangle of pixel coordinates around the point the
        // the user clicked.
        for (int i = 0; i < pixelDistFrmUsrClickPt; i++) {
            squarePixelArea[i][midpoint] = tempPixelArea[i];
            squarePixelArea[i] = this.generatePixelLocations(
                    squarePixelArea[i][midpoint], true, pixelDistFrmUsrClickPt);
        }

        // Get the data from the satellite tile map for each pixel coordinate
        // in the rectangular matrix of pixel coordinates
        for (int i = 0; i < pixelDistFrmUsrClickPt; i++) {
            for (int j = 0; j < pixelDistFrmUsrClickPt; j++) {

                double xCoord = (squarePixelArea[i][j].xCoord);
                double yCoord = (squarePixelArea[i][j].yCoord);

                Coordinate latlon = pixelsBackToLatlon(mtGridToCRS,
                        localProjToLatLon, xCoord, yCoord);

                arrayOfPixVal[i][j] = satRsc.getRawIRImageValue(latlon);

            }

        }

        return arrayOfPixVal;

    }

    /**
     * Convert coordinates in pixels into the same coordinate in LatLon
     *
     * @param mtGridToCRS
     * @param localProjToLatLon
     * @param xCoord
     * @param yCoord
     * @return
     */
    private Coordinate pixelsBackToLatlon(MathTransform mtGridToCRS,
            MathTransform localProjToLatLon, double xCoord, double yCoord) {
        Coordinate latlon = null;

        double[] pixelCoord = new double[2];
        double[] crsCoord = new double[2];
        double[] latlonCoord = new double[2];

        pixelCoord[0] = xCoord;
        pixelCoord[1] = yCoord;

        try {
            mtGridToCRS.transform(pixelCoord, 0, crsCoord, 0, 1);
            localProjToLatLon.transform(crsCoord, 0, latlonCoord, 0, 1);
        } catch (Exception e) {
            statusHandler
                    .error("Error tranforming pixel coordinates to latitude and longitude",
                            e);
        }

        latlon = new Coordinate(latlonCoord[0], latlonCoord[1]);

        return latlon;
    }

    /**
     * Finds the maximum pixel value in a 2D array of pixel values
     *
     * @param arrayOfPixVal
     *            - the 2D array to search
     * @return the maximum pixel value in the array
     */
    private double getMaxDataVal(double[][] arrayOfPixVal) {
        double maxPixVal = 0.0f;
        if (arrayOfPixVal != null && arrayOfPixVal.length > 0) {
            int arraySize = arrayOfPixVal.length;
            for (int i = 0; i < arraySize; i++) {
                for (int j = 0; j < arraySize; j++) {
                    if (arrayOfPixVal[i][j] > maxPixVal) {
                        maxPixVal = arrayOfPixVal[i][j];
                    }
                }
            }
        }
        return maxPixVal;
    }

    /**
     * Finds the most frequently occurring pixel value in the input 2D array of
     * pixel values.
     *
     * @param arrayOfPixVal
     *            - the input 2D array to search
     * @return the most frequently occurring pixel value
     */
    private double getModeOfDataValues(double[][] arrayOfPixVal) {
        double modePixVal = 0.0f;
        Map<Double, Integer> frequencyMap = new HashMap<>(0);
        int counter = 0;
        if (arrayOfPixVal != null && arrayOfPixVal.length > 0) {
            int arraySize = arrayOfPixVal.length;
            for (int i = 0; i < arraySize; i++) {
                for (int j = 0; j < arraySize; j++) {
                    Double currentPixVal = new Double(arrayOfPixVal[i][j]);
                    /*
                     * If the hash-map contains the pixel value, get its current
                     * counter
                     */
                    if (frequencyMap.containsKey(currentPixVal)) {
                        counter = frequencyMap.get(currentPixVal);
                    }

                    /* update the counter */
                    counter++;

                    /*
                     * put the pixel value in the hash-map, along with its
                     * corresponding counter value
                     */
                    frequencyMap.put(currentPixVal, new Integer(counter));

                    /* reset the counter value */
                    counter = 0;
                }
            }

            /*
             * Loop through the pixel values in the map to get the most frequent
             * pixel value
             */
            int mostFrequentPixVal = 0;
            Set<Double> keySet = frequencyMap.keySet();
            for (Double eachDbl : keySet) {
                if (frequencyMap.get(eachDbl).intValue() > mostFrequentPixVal) {
                    mostFrequentPixVal = frequencyMap.get(eachDbl).intValue();
                    modePixVal = eachDbl.doubleValue();
                }
            }

        }
        return modePixVal;
    }

    /**
     * Stores the x-y grid location of Mcidas Data as a single class object
     *
     * @author archana
     */
    protected class PixelLocation {
        protected int xCoord = 0;

        protected int yCoord = 0;

        public PixelLocation(int x, int y) {
            xCoord = x;
            yCoord = y;
        }

    }

    /**
     * get climatology sounding data for specific location.
     *
     */
    private List<CloudHeightData> getSoundingFromClimatology(Coordinate latlon,
            double tmpc) {
        List<CloudHeightData> chSummer = new ArrayList<>(0);
        List<CloudHeightData> chWinter = new ArrayList<>(0);
        List<List<CloudHeightData>> ch = new ArrayList<>(4);

        double latitude = Math.abs(latlon.y);
        Calendar cal = satelliteImageTime.getRefTimeAsCalendar();
        int jdy = cal.get(Calendar.DAY_OF_YEAR);

        if (latlon.y < 0) {
            jdy += 180;
        }

        // Acquire needed data.

        double fraction = 1.d;
        double pfraction = 0.d;
        for (SoundingModel sndMod : sndingModels) {
            if (latitude <= 15.) {
                if (sndMod.getName().equalsIgnoreCase("Summer-15deg")) {
                    chSummer = computeCloudHeights(
                            sndMod.getSoundingLevels()
                                    .getLevelValues(), tmpc);
                } else if (sndMod.getName().equalsIgnoreCase("Winter-15deg")) {
                    chWinter = computeCloudHeights(
                            sndMod.getSoundingLevels()
                                    .getLevelValues(), tmpc);
                }
                break;

            } else if (latitude > 15. && latitude <= 30.) {
                if (sndMod.getName().equalsIgnoreCase("Summer-30deg")) {
                    ch.add(computeCloudHeights(sndMod
                            .getSoundingLevels().getLevelValues(), tmpc));
                } else if (sndMod.getName().equalsIgnoreCase("Summer-15deg")) {
                    ch.add(computeCloudHeights(sndMod
                            .getSoundingLevels().getLevelValues(), tmpc));
                } else if (sndMod.getName().equalsIgnoreCase("Winter-15deg")) {
                    ch.add(computeCloudHeights(sndMod
                            .getSoundingLevels().getLevelValues(), tmpc));
                } else if (sndMod.getName().equalsIgnoreCase("Winter-30deg")) {
                    ch.add(computeCloudHeights(sndMod
                            .getSoundingLevels().getLevelValues(), tmpc));

                }
                fraction = (30. - latitude) / 15.;
                pfraction = (Math.log(30.) - Math.log(latitude))
                        / Math.log(15.);

            } else if (latitude > 30. && latitude <= 45.) {
                if (sndMod.getName().equalsIgnoreCase("Summer-45deg")) {
                    ch.add(computeCloudHeights(sndMod
                            .getSoundingLevels().getLevelValues(), tmpc));
                } else if (sndMod.getName().equalsIgnoreCase("Summer-30deg")) {
                    ch.add(computeCloudHeights(sndMod
                            .getSoundingLevels().getLevelValues(), tmpc));
                } else if (sndMod.getName().equalsIgnoreCase("Winter-30deg")) {
                    ch.add(computeCloudHeights(sndMod
                            .getSoundingLevels().getLevelValues(), tmpc));
                } else if (sndMod.getName().equalsIgnoreCase("Winter-45deg")) {
                    ch.add(computeCloudHeights(sndMod
                            .getSoundingLevels().getLevelValues(), tmpc));
                }
                fraction = (45. - latitude) / 15.;
                pfraction = (Math.log(45.) - Math.log(latitude))
                        / Math.log(15.);

            } else if (latitude > 45. && latitude <= 60.) {
                if (sndMod.getName().equalsIgnoreCase("Summer-60deg")) {
                    ch.add(computeCloudHeights(sndMod
                            .getSoundingLevels().getLevelValues(), tmpc));
                    for (int kk = 0; kk < sndMod.getSoundingLevels()
                            .getLevelValues().size(); kk++) {
                    }
                } else if (sndMod.getName().equalsIgnoreCase("Summer-45deg")) {
                    ch.add(computeCloudHeights(sndMod
                            .getSoundingLevels().getLevelValues(), tmpc));
                } else if (sndMod.getName().equalsIgnoreCase("Winter-45deg")) {
                    ch.add(computeCloudHeights(sndMod
                            .getSoundingLevels().getLevelValues(), tmpc));
                } else if (sndMod.getName().equalsIgnoreCase("Winter-60deg")) {
                    ch.add(computeCloudHeights(sndMod
                            .getSoundingLevels().getLevelValues(), tmpc));
                }
                fraction = (60. - latitude) / 15.;
                pfraction = (Math.log(60.) - Math.log(latitude))
                        / Math.log(15.);

            } else if (latitude > 60. && latitude < 75.) {
                if (sndMod.getName().equalsIgnoreCase("Summer-75deg")) {
                    ch.add(computeCloudHeights(sndMod
                            .getSoundingLevels().getLevelValues(), tmpc));
                } else if (sndMod.getName().equalsIgnoreCase("Summer-60deg")) {
                    ch.add(computeCloudHeights(sndMod
                            .getSoundingLevels().getLevelValues(), tmpc));
                } else if (sndMod.getName().equalsIgnoreCase("Winter-60deg")) {
                    ch.add(computeCloudHeights(sndMod
                            .getSoundingLevels().getLevelValues(), tmpc));
                } else if (sndMod.getName().equalsIgnoreCase("Winter-75deg")) {
                    ch.add(computeCloudHeights(sndMod
                            .getSoundingLevels().getLevelValues(), tmpc));
                }
                fraction = (75. - latitude) / 15.;
                pfraction = (Math.log(75.) - Math.log(latitude))
                        / Math.log(15.);

            } else if (latitude >= 75.) {
                if (sndMod.getName().equalsIgnoreCase("Summer-75deg")) {
                    chSummer = computeCloudHeights(
                            sndMod.getSoundingLevels()
                                    .getLevelValues(), tmpc);
                } else if (sndMod.getName().equalsIgnoreCase("Winter-75deg")) {
                    chWinter = computeCloudHeights(
                            sndMod.getSoundingLevels()
                                    .getLevelValues(), tmpc);
                }
                break;
            }

        }

        // Geographical interpolation to the location.
        double hhs = 0.0;
        double pps = 0.0;
        double hhw = 0.0;
        double ppw = 0.0;
        double hh = -9999.0;
        double pp = -9999.0;

        try {
            if (latitude > 15 && latitude < 75) {
                hhs = (ch.get(3).get(0).cloudHght)
                        + (ch.get(2).get(0).cloudHght - ch.get(3).get(0).cloudHght)
                        * fraction;
                pps = (ch.get(2).get(0).cloudPres)
                        * Math.exp((Math.log(ch.get(2).get(0).cloudPres) - Math
                                .log(ch.get(3).get(0).cloudPres)) * pfraction);
                hhw = (ch.get(1).get(0).cloudHght)
                        + (ch.get(0).get(0).cloudHght - ch.get(1).get(0).cloudHght)
                        * fraction;
                ppw = (ch.get(0).get(0).cloudPres)
                        * Math.exp((Math.log(ch.get(0).get(0).cloudPres) - Math
                                .log(ch.get(1).get(0).cloudPres)) * pfraction);
            } else {
                hhs = chSummer.get(0).cloudHght;
                pps = chSummer.get(0).cloudPres;
                hhw = chWinter.get(0).cloudHght;
                ppw = chWinter.get(0).cloudPres;
            }

            hh = (hhs - hhw) * (1. - Math.cos(jdy * (2. * Math.PI) / 365.))
                    / 2. + hhw;
            pp = (pps - ppw) * (1. - Math.cos(jdy * (2. * Math.PI) / 365.))
                    / 2. + ppw;
        } catch (Exception e) {
            return chSummer;
        }
        chSummer.add(new CloudHeightData(hh, pp));
        return chSummer;
    }
}