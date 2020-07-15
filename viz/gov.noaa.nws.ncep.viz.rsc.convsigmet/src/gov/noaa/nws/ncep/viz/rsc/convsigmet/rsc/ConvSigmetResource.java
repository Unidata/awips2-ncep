package gov.noaa.nws.ncep.viz.rsc.convsigmet.rsc;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.swt.graphics.RGB;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.common.time.TimeRange;
import com.raytheon.uf.edex.decodertools.core.LatLonPoint;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.IGraphicsTarget.HorizontalAlignment;
import com.raytheon.uf.viz.core.IGraphicsTarget.TextStyle;
import com.raytheon.uf.viz.core.IGraphicsTarget.VerticalAlignment;
import com.raytheon.uf.viz.core.drawables.IFont;
import com.raytheon.uf.viz.core.drawables.PaintProperties;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.geom.PixelCoordinate;
import com.raytheon.uf.viz.core.rsc.LoadProperties;

import gov.noaa.nws.ncep.common.dataplugin.convsigmet.ConvSigmetLocation;
import gov.noaa.nws.ncep.common.dataplugin.convsigmet.ConvSigmetRecord;
import gov.noaa.nws.ncep.common.dataplugin.convsigmet.ConvSigmetSection;
import gov.noaa.nws.ncep.viz.common.ui.NmapCommon;
import gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsResource;
import gov.noaa.nws.ncep.viz.resources.INatlCntrsResource;
import gov.noaa.nws.ncep.viz.ui.display.NCMapDescriptor;

/**
 * ConvSigmetResource - Display Convective SIGMET data.
 *
 * This code has been developed by the SIB for use in the AWIPS2 system.
 *
 * <pre>
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer    Description
 * ------------- -------- ----------- ------------------------------------------
 * Jun 16, 2009  95       B. Hebbard  Initial creation.
 * Jun 17, 2009  115      G. Hull     Integrate with INatlCntrsResouce
 * Jul 02, 2009  134      M. Li       Use vors.xml
 * Aug 10, 2009           B. Hebbard  Convert to TO11 structure
 * Aug 19, 2009           B. Hebbard  Extend new AbstractNatlCntrsResource
 * Aug 20, 2009  145      Greg Hull   init() -> initResource()
 * Sep 21, 2009           B. Hebbard  Remove VOR processing:  Use lat/lon direct
 *                                    from DB, now that updated decoder provides
 *                                    this
 * Nov 24, 2009           Greg Hull   migrate to to11d6
 * Oct 03, 2010  307      Greg Hull   modify processRecords and updateFrameData
 *                                    to process ConvSigmetRscDataObjs instead
 *                                    of ConvSigmetRecord
 * Apr 22, 2011           B. Hebbard  Prevent label overlap where W&C or C&E
 *                                    Outlooks have coincident top points
 * Mar 09, 2012  728      B. Hebbard  Use postProcessFrameUpdate() to remove,
 *                                    for each frame, SIGMETs/Outlooks if
 *                                    superseded by any in that frame more
 *                                    recently issued for the same region
 *                                    (W/C/E) (TTR 143).
 * May 23, 2012  785      Q. Zhou     Added getName for legend.
 * Nov 05, 2015  5070     randerso    Adjust font sizes for dpi scaling
 * Jul 15, 2020  8191     randerso    Updated for changes to LatLonPoint
 *
 * </pre>
 *
 * @author bhebbard
 */
public class ConvSigmetResource extends
        AbstractNatlCntrsResource<ConvSigmetResourceData, NCMapDescriptor>
        implements INatlCntrsResource {
    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(ConvSigmetResource.class);

    private ConvSigmetResourceData convSigmetResourceData;

    private class FrameData extends AbstractFrameData {

        /*
         * A map from an identifier string (like "18C", which is unique within a
         * single frame) to a structure for one displayable element (polygon,
         * line, point, etc.) for a SIGMET or Outlook location
         */
        private Map<String, ConvSigmetRscDataObj> convSigmetDataMap;

        public FrameData(DataTime frameTime, int timeInt) {
            super(frameTime, timeInt);
            convSigmetDataMap = new HashMap<>();
        }

        @Override
        public boolean updateFrameData(IRscDataObject rscDataObj) {
            if (!(rscDataObj instanceof ConvSigmetRscDataObj)) {
                statusHandler.debug(
                        "ConvSigmet.updateFrameData expecting ConvSigmetRscDataObj "
                                + " instead of: "
                                + rscDataObj.getClass().getName());
                return false;
            }

            ConvSigmetRscDataObj cSigRscData = (ConvSigmetRscDataObj) rscDataObj;

            if (cSigRscData != null && cSigRscData.csigType != ConvSigmetType.CS
                    && cSigRscData.csigType != ConvSigmetType.UNKNOWN) {
                // Note that -- unlike similar resources -- sequenceID alone is
                // not unique even within a single frame, since a CONVECTIVE
                // SIGMET
                // and OUTLOOK can both be designated, say, "1C". So we suffix
                // with
                // the class type, to ensure uniqueness within a single frame.
                // TODO: Is this good enough? Since sequence IDs 'reset' each
                // frame/hour, might want to add end time string for added
                // safety?
                String keyString = cSigRscData.sequenceID + " "
                        + cSigRscData.csigType;
                ConvSigmetRscDataObj existingConvSigmetData = convSigmetDataMap
                        .get(keyString);

                // If keyString is not in the list, or if the ref time is newer,
                // then add the data to the list
                if (existingConvSigmetData == null || cSigRscData.issueTime
                        .greaterThan(existingConvSigmetData.issueTime)) {

                    convSigmetDataMap.put(keyString, cSigRscData);
                }
            }

            return true;
        }
    }

    /**
     * Structure containing displayable information for a single displayable
     * element -- that is, a single convective SIGMET (AREA polygon, LINE line,
     * or ISOL point) or OUTLOOK. This corresponds to the information from the
     * (raw) ConvSigmetSection record, but has been 'crunched' a bit to (1)
     * reduce things to those needed for display, and (2) preprocess where
     * possible (for example, location lookups) so the paint() method can work
     * directly from this data as efficiently as possible.
     */
    private class ConvSigmetRscDataObj implements IRscDataObject {
        /** see enumeration immediately below */
        private ConvSigmetType csigType;

        /** ex: "24C" */
        private String sequenceID;

        /** issue time from bulletin */
        private DataTime issueTime;

        /** time range of individual convective SIGMET or outlook */
        private DataTime eventTime;

        /** same, but fudged so outlook 'valid' right away (per legacy) */
        private DataTime matchTime;

        /** end time of individual convective SIGMET or outlook */
        private DataTime endTime;

        /** "from" direction in bulletin */
        private int direction;

        /** in knots */
        private int speed;

        /** width of LINE or diameter of ISOL (NM) */
        private int distance;

        /** as specified with FL in bulletin */
        private int flightLevel;

        /** ex: "DVLPG" */
        private String intensity;

        /** size of following array */
        private int numPoints;

        /**
         * lat/lon of points, ordered as in bulletin (may be null if invalid
         * location)
         */
        private LatLonPoint[] points;

        @Override
        public DataTime getDataTime() {
            // return eventTime;
            return matchTime;
        }
    }

    private enum ConvSigmetType {
        /** Convective SIGMET for AREA (polygon) */
        AREA,
        /** Convective SIGMET for LINE of storms, w/ distance both sides */
        LINE,
        // (combined)
        /** Convective SIGMET for ISOLated (point, w/ distance diameter) */
        ISOL,
        /**
         * Not a Convective SIGMET itself, but area (polygon) where things might
         * develop 2-6 hours hence
         */
        OUTLOOK,
        /**
         * "NIL" Convective SIGMET; fulfilling required hourly issuance,
         * explicitly saying there are no Convective SIGMETs for this region (W,
         * C, or E)
         */
        CS,
        /** Unknown type -- something wrong */
        UNKNOWN
    }

    /**
     * Create a Convective SIGMET resource.
     *
     * @throws VizException
     */
    public ConvSigmetResource(ConvSigmetResourceData resourceData,
            LoadProperties loadProperties) throws VizException {
        super(resourceData, loadProperties);
        convSigmetResourceData = resourceData;
    }

    @Override
    protected AbstractFrameData createNewFrame(DataTime frameTime,
            int timeInt) {
        return new FrameData(frameTime, timeInt);
    }

    @Override
    public void initResource(IGraphicsTarget grphTarget) throws VizException {
        queryRecords();
    }

    /*
     * override to process ConvSigmetRscDataObj instead of ConvSigmetRecord
     * since each record may contain more than one 'convSigmetSection' each of
     * which has its own start and end time and must be time matched separately.
     */
    @Override
    protected IRscDataObject[] processRecord(Object pdo) {
        if (!(pdo instanceof ConvSigmetRecord)) {
            statusHandler
                    .debug("ConvSigmet expecting ConvSigmetRecord instead of: "
                            + pdo.getClass().getName());
            return null;
        }

        ConvSigmetRecord csigRec = (ConvSigmetRecord) pdo;
        DataTime csigTime = csigRec.getDataTime();

        Set<ConvSigmetSection> convSigmetSections = csigRec
                .getConvSigmetSection();

        List<ConvSigmetRscDataObj> csigRscDataObjs = new ArrayList<>();

        for (ConvSigmetSection csigSection : convSigmetSections) {
            ConvSigmetRscDataObj convSigmetData = getConvSigmetData(csigTime,
                    csigSection);

            if (convSigmetData != null) {
                csigRscDataObjs.add(convSigmetData);
            }
        }

        return csigRscDataObjs.toArray(new ConvSigmetRscDataObj[0]);
    }

    @Override
    protected boolean postProcessFrameUpdate() {
        //

        // for each frame...

        for (AbstractFrameData afd : frameDataMap.values()) {
            FrameData fd = (FrameData) afd;

            // ...go through all the data time matched to this frame
            // to determine, for every region (W, C, E), the latest issuance
            // of SIGMETs and of Outlooks for that region

            Map<Character, DataTime> latestSigmetIssuanceTimeForRegions = new HashMap<>();
            Map<Character, DataTime> latestOutlooksIssuanceTimeForRegions = new HashMap<>();

            for (ConvSigmetRscDataObj csigRDO : fd.convSigmetDataMap.values()) {
                String sequenceID = csigRDO.sequenceID.trim().toUpperCase();
                if (sequenceID != null && !sequenceID.isEmpty()) {
                    // the "C" in "18C"
                    Character region = sequenceID
                            .charAt(sequenceID.length() - 1);
                    switch (csigRDO.csigType) {
                    case AREA:
                    case ISOL:
                    case LINE:
                    case CS:
                        // null Convective SIGMET still counts as 'issuance'
                        DataTime latestSigmetIssuanceForThisRegion = latestSigmetIssuanceTimeForRegions
                                .get(region);
                        if (latestSigmetIssuanceForThisRegion == null
                                || csigRDO.issueTime.greaterThan(
                                        latestSigmetIssuanceForThisRegion)) {
                            latestSigmetIssuanceTimeForRegions.put(region,
                                    csigRDO.issueTime);
                        }
                        break;
                    case OUTLOOK:
                        DataTime latestOutlooksIssuanceForThisRegion = latestOutlooksIssuanceTimeForRegions
                                .get(region);
                        if (latestOutlooksIssuanceForThisRegion == null
                                || csigRDO.issueTime.greaterThan(
                                        latestOutlooksIssuanceForThisRegion)) {
                            latestOutlooksIssuanceTimeForRegions.put(region,
                                    csigRDO.issueTime);
                        }
                        break;
                    case UNKNOWN:
                        break;
                    default:
                    }
                }
            }

            // Now that we've determined the latest issuances for each region --
            // for both outlooks and actual Convective SIGMETs -- we make a
            // second
            // pass through the data time matched to this frame. This time,
            // we purge anything superseded by a later issuance.

            String[] keys = new String[1];
            keys = fd.convSigmetDataMap.keySet().toArray(keys);
            for (String key : keys) {
                ConvSigmetRscDataObj csigRDO = fd.convSigmetDataMap.get(key);
                String sequenceID = (csigRDO == null) ? null
                        : csigRDO.sequenceID;
                if (sequenceID != null && !sequenceID.isEmpty()) {
                    Character region = sequenceID
                            .charAt(sequenceID.length() - 1);
                    switch (csigRDO.csigType) {
                    case AREA:
                    case ISOL:
                    case LINE:
                    case CS:
                        DataTime latestSigmetIssuanceForThisRegion = latestSigmetIssuanceTimeForRegions
                                .get(region);
                        if (latestSigmetIssuanceForThisRegion != null
                                && latestSigmetIssuanceForThisRegion
                                        .greaterThan(csigRDO.issueTime)) {
                            fd.convSigmetDataMap.remove(key);
                        }
                        break;
                    case OUTLOOK:
                        DataTime latestOutlooksIssuanceForThisRegion = latestOutlooksIssuanceTimeForRegions
                                .get(region);
                        if (latestOutlooksIssuanceForThisRegion != null
                                && latestOutlooksIssuanceForThisRegion
                                        .greaterThan(csigRDO.issueTime)) {
                            fd.convSigmetDataMap.remove(key);
                        }
                        break;
                    case UNKNOWN:
                        break;
                    default:
                    }
                }
            }

        }
        //
        return true;
    }

    @Override
    public void paintFrame(AbstractFrameData frameData, IGraphicsTarget target,
            PaintProperties paintProps) throws VizException {

        FrameData currFrameData = (FrameData) frameData;

        // Allocate font and calculate vertical offset parameter for lines of
        // text
        IFont font = target.initializeFont("Monospace", 12,
                new IFont.Style[] { IFont.Style.BOLD });
        double screenToWorldRatio = paintProps.getCanvasBounds().width
                / paintProps.getView().getExtent().getWidth();
        Rectangle2D charSize = target.getStringBounds(font, "N");
        double charHeight = charSize.getHeight();
        double offsetY = charHeight / screenToWorldRatio;

        if (paintProps == null) {
            return;
        }

        if (currFrameData != null) {

            // Put per-hour options into arrays for easy lookup
            final Boolean[] enables = { convSigmetResourceData.hour0Enable,
                    convSigmetResourceData.hour1Enable,
                    convSigmetResourceData.hour2Enable };
            final RGB[] colors = { convSigmetResourceData.hour0Color,
                    convSigmetResourceData.hour1Color,
                    convSigmetResourceData.hour2Color };
            final int[] lineWidths = { convSigmetResourceData.hour0LineWidth,
                    convSigmetResourceData.hour1LineWidth,
                    convSigmetResourceData.hour2LineWidth };
            final Boolean[] sequenceIdEnables = {
                    convSigmetResourceData.hour0sequenceIdEnable,
                    convSigmetResourceData.hour1sequenceIdEnable,
                    convSigmetResourceData.hour2sequenceIdEnable };

            // Loop through the (preprocessed) convective SIGMET data records
            // (This should be fast.)
            Collection<ConvSigmetRscDataObj> convSigmetDataValues = currFrameData.convSigmetDataMap
                    .values();

            for (ConvSigmetRscDataObj convSigmetData : convSigmetDataValues) {

                // Check for invalid time range
                // TODO: See if this is still needed/valid...
                // if (activeFrameTime.compareTo(convSigmetData.startTime) < 0
                // ||
                // activeFrameTime.compareTo(convSigmetData.endTime) >= 0)
                // continue;

                // Just some 'safety' defaults
                boolean enable = false;
                RGB color = new RGB(155, 155, 155);
                int lineWidth = 2;
                boolean sequenceIdEnable = true;

                // Are we moving? (Decoder uses negative numbers to say no.)
                boolean inMotion = (convSigmetData.direction >= 0)
                        && (convSigmetData.speed > 0);

                for (int hour = 0; hour <= 2; hour++) {
                    switch (convSigmetData.csigType) {
                    case AREA:
                    case LINE:
                    case ISOL:
                        // these (may) have motion; set hourly parameters
                        enable = enables[hour] && (hour == 0 || inMotion);
                        color = colors[hour];
                        lineWidth = lineWidths[hour];
                        sequenceIdEnable = sequenceIdEnables[hour];
                        break;
                    case OUTLOOK:
                        // no motion; draw only zero hour (if enabled)
                        enable = (hour == 0)
                                && convSigmetResourceData.getOutlookEnable();
                        color = convSigmetResourceData.getOutlookColor();
                        lineWidth = convSigmetResourceData
                                .getOutlookLineWidth();
                        /*
                         * cannot disable sequence ID for outlooks unless
                         * outlooks disabled altogether
                         */
                        sequenceIdEnable = true;
                        break;
                    case CS:
                        // nil convective SIGMET
                        enable = false;
                        break;
                    case UNKNOWN:
                        // TODO: Sanity check error! Unrecognized class type.
                        enable = false;
                        break;
                    default:
                    }
                    if (enable) {
                        PixelCoordinate prevLoc = null;
                        /*
                         * text placed above (screen, not necessarily north)
                         * this won't interfere with the geometry
                         */
                        PixelCoordinate topLocation = null;
                        double longitudeAtTopLocation = 0.0;
                        for (int i = 0; i < convSigmetData.numPoints; i++) {
                            LatLonPoint currentPoint = convSigmetData.points[i];
                            if (currentPoint == null) {
                                /*
                                 * gracefully skip over omitted points (say,
                                 * location lookup failure)
                                 */
                                continue;
                            }
                            if (hour > 0) {
                                // extrapolate position in future
                                double heading = (convSigmetData.direction
                                        + 180) % 360;
                                double distance = hour * convSigmetData.speed;
                                currentPoint = currentPoint.positionOf(heading,
                                        distance);
                            }
                            double[] latLon = { currentPoint.getLongitude(),
                                    currentPoint.getLatitude() };
                            PixelCoordinate currLoc = new PixelCoordinate(
                                    descriptor.worldToPixel(latLon));

                            // skip first location
                            if (prevLoc != null) {
                                // draw line/polygon segment
                                target.drawLine(prevLoc.getX(), prevLoc.getY(),
                                        prevLoc.getZ(), currLoc.getX(),
                                        currLoc.getY(), currLoc.getZ(), color,
                                        lineWidth);
                            } else if (convSigmetData.numPoints == 1) {
                                /*
                                 * TODO: Check for csigType ISOL instead or in
                                 * addition?
                                 */

                                // single point; draw marker and circle
                                // tune to match NMAP
                                double delta = offsetY * 0.3;
                                target.drawLine(currLoc.getX() - delta,
                                        currLoc.getY(), currLoc.getZ(),
                                        currLoc.getX() + delta, currLoc.getY(),
                                        currLoc.getZ(), color, lineWidth);
                                target.drawLine(currLoc.getX(),
                                        currLoc.getY() - delta, currLoc.getZ(),
                                        currLoc.getX(), currLoc.getY() + delta,
                                        currLoc.getZ(), color, lineWidth);
                                double radius = convSigmetData.distance / 2.0;
                                target.drawCircle(currLoc.getX(),
                                        currLoc.getY(), currLoc.getZ(), radius,
                                        color, lineWidth);
                                // circle top
                                topLocation = new PixelCoordinate(
                                        currLoc.getX(),
                                        currLoc.getY() - radius);
                            }
                            if (topLocation == null
                                    || topLocation.getY() > currLoc.getY()) {
                                topLocation = currLoc;
                                longitudeAtTopLocation = latLon[0];
                            }
                            prevLoc = currLoc;
                        }

                        // Draw labels

                        if (topLocation != null) {

                            // Use an ArrayList since we don't know in advance
                            // how big it'll be and would like
                            List<String> labelList = new ArrayList<>();

                            HorizontalAlignment horizontalAlignment = HorizontalAlignment.LEFT;

                            if (sequenceIdEnable) {
                                if (convSigmetData.csigType == ConvSigmetType.OUTLOOK) {
                                    /*
                                     * Prevent label overlap when West & Central
                                     * OR Central & East outlook polygons have
                                     * coincident top points, by flipping some
                                     * text to left
                                     */
                                    StringBuilder outlookLabel = new StringBuilder(
                                            convSigmetData.sequenceID);
                                    outlookLabel.append(" OUTLOOK");

                                    // TODO: Tune latitude/longitude boundaries?
                                    if (convSigmetData.sequenceID.endsWith("C")
                                            && longitudeAtTopLocation > -095.0
                                            || convSigmetData.sequenceID
                                                    .endsWith("W")
                                                    && longitudeAtTopLocation > -112.0) {

                                        horizontalAlignment = HorizontalAlignment.RIGHT;
                                        outlookLabel.append("  ");
                                    }
                                    labelList.add(outlookLabel.toString());
                                } else {
                                    labelList.add(convSigmetData.sequenceID);
                                }
                            }

                            if (hour == 0
                                    && convSigmetData.csigType != ConvSigmetType.OUTLOOK) {
                                if (convSigmetResourceData.timeEnable) {
                                    String endTimeS = convSigmetData.endTime
                                            .toString();
                                    labelList.add(endTimeS.substring(8, 10)
                                            + "/" // date
                                            + endTimeS.substring(11, 13) // hour
                                            + endTimeS.substring(14, 16)); // minute
                                }

                                if (convSigmetResourceData.flightLevelEnable) {
                                    labelList.add(
                                            "FL" + convSigmetData.flightLevel);
                                }

                                if (convSigmetResourceData.motionEnable
                                        && inMotion) {
                                    labelList.add(String.format("%03d", // leading
                                                                        // zeroes
                                                                        // for
                                                                        // direction
                                            convSigmetData.direction) + " "
                                            + convSigmetData.speed + "kt");
                                }

                                if (convSigmetResourceData.intensityEnable
                                        && convSigmetData.intensity != null
                                        && !convSigmetData.intensity
                                                .isEmpty()) {
                                    labelList.add(convSigmetData.intensity);
                                }
                            }

                            if (!labelList.isEmpty()) {
                                target.drawStrings(font,
                                        labelList.toArray(new String[0]),
                                        topLocation.getX(),
                                        topLocation.getY() - offsetY
                                                * (labelList.size() + 0.5),
                                        0.0, TextStyle.NORMAL,
                                        // TODO: Dorky!!
                                        new RGB[] { color, color, color, color,
                                                color, color },
                                        horizontalAlignment,
                                        VerticalAlignment.TOP);
                            }
                        }
                    }
                }
            }
        }
        font.dispose();
    }

    private ConvSigmetRscDataObj getConvSigmetData(DataTime rTime,
            ConvSigmetSection convSigmetSection) {

        /*
         * A ConvSigmetData object holds roughly the same information as a
         * ConvSigmetSection from the database, but it's distilled down a bit to
         * only the stuff we'll need for paint() later.
         */
        if (convSigmetSection.getSequenceID() == null) {
            // bail if not worth going further
            return null;
        }

        ConvSigmetRscDataObj convSigmetData = new ConvSigmetRscDataObj();

        /*
         * Convert classType string to an enum, just to avoid string comparisons
         * during all those paint()'s.
         */
        try {
            convSigmetData.csigType = ConvSigmetType
                    .valueOf(convSigmetSection.getClassType());
        } catch (IllegalArgumentException e) {
            // TODO: Signal unrecognized classType string
            convSigmetData.csigType = ConvSigmetType.UNKNOWN;
            statusHandler.debug("Unrecognized classType: "
                    + convSigmetSection.getClassType(), e);
        }

        convSigmetData.sequenceID = convSigmetSection.getSequenceID();
        convSigmetData.issueTime = rTime;
        convSigmetData.eventTime = new DataTime(
                convSigmetSection.getStartTime(),
                new TimeRange(convSigmetSection.getStartTime(),
                        convSigmetSection.getEndTime()));
        convSigmetData.matchTime = new DataTime(rTime.getRefTimeAsCalendar(),
                new TimeRange(rTime.getRefTimeAsCalendar(),
                        convSigmetSection.getEndTime()));
        convSigmetData.endTime = new DataTime(convSigmetSection.getEndTime());
        convSigmetData.direction = convSigmetSection.getDirection();
        convSigmetData.speed = convSigmetSection.getSpeed();
        convSigmetData.distance = convSigmetSection.getDistance();
        convSigmetData.flightLevel = convSigmetSection.getFlightLevel();
        convSigmetData.intensity = convSigmetSection.getIntensity();

        // Child location records become arrays of latitude/longitude points.
        convSigmetData.numPoints = convSigmetSection.getConvSigmetLocation()
                .size();
        if (convSigmetData.numPoints > 0) {
            convSigmetData.points = new LatLonPoint[convSigmetData.numPoints];
            for (ConvSigmetLocation convSigmetLocation : convSigmetSection
                    .getConvSigmetLocation()) {
                LatLonPoint point = new LatLonPoint(
                        convSigmetLocation.getLatitude(),
                        convSigmetLocation.getLongitude());
                int index = convSigmetLocation.getIndex() - 1;
                // TODO: Add sanity checks for uniqueness and completeness of
                // indices
                convSigmetData.points[index] = point;
            }
        }

        return convSigmetData;
    }

    @Override
    public void disposeInternal() {
        super.disposeInternal();
    }

    @Override
    public void resourceAttrsModified() {
        // don't need to do anything
    }

    @Override
    public String getName() {
        String legendString = super.getName();
        FrameData fd = (FrameData) getCurrentFrame();
        if (fd == null || fd.getFrameTime() == null
                || fd.convSigmetDataMap.size() == 0) {
            return legendString + "-No Data";
        }
        return legendString + " "
                + NmapCommon.getTimeStringFromDataTime(fd.getFrameTime(), "/");
    }
}