package gov.noaa.nws.ncep.edex.plugin.modis.decoder;

import gov.noaa.nws.ncep.common.dataplugin.modis.ModisMessageData;
import gov.noaa.nws.ncep.common.dataplugin.modis.ModisRecord;
import gov.noaa.nws.ncep.common.dataplugin.modis.ModisSpatialCoverage;
import gov.noaa.nws.ncep.common.dataplugin.modis.dao.ModisDao;

import java.io.IOException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ucar.nc2.Attribute;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import com.raytheon.edex.plugin.AbstractDecoder;
import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.geospatial.interpolation.GridDownscaler;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.common.time.TimeRange;
import com.raytheon.uf.common.util.ArraysUtil;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;

/***
 * Decoder for MODIS data and spatial files
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer     Description
 * ------------ ---------- -----------  --------------------------
 * 10/01/2014   R5116      kbugenhagen  Initial creation.
 * 02/20/2015   R5116      kbugenhagen  Updated for Cyanobacterial Index (CI)
 * 
 * </pre>
 * 
 * @author kbugenhagen
 * @version 1.0
 */

public class ModisDecoder extends AbstractDecoder {

    protected Log logger = LogFactory.getLog(getClass());

    private static final String START_TIME_ID = "cw:start_time";

    private static final String TEMPORAL_EXTENT_ID = "cw:temporal_extent";

    private static final String PASS_DATE_ID = "cw:pass_date";

    private static final float OFFSET = 0.0f;

    private static final float SCALE = 0.0001f;

    private static final float DEFAULT_MISSING_VALUE = -32767.0f;

    private static final String LATITUDE_DATASET_ID = "latitude";

    private static final String LONGITUDE_DATASET_ID = "longitude";

    private static final String LATITUDE_RING_ID = "cw:polygon_latitude";

    private static final String LONGITUDE_RING_ID = "cw:polygon_longitude";

    private static NetcdfFile dataFile = null;

    // CI: Cyanobacterial Index
    private static final String CI_INDEX_ID = "Cyanobacteria";

    // rhos arrays
    private static final String RHOS_667_ID = "rhos_667";

    private static final String RHOS_678_ID = "rhos_678";

    private static final String RHOS_748_ID = "rhos_748";

    private static final float LAMBDA = 678.0f;

    private static final float LAMBDA_PLUS = 748.0f;

    private static final float LAMBDA_MINUS = 667.0f;

    /** Number of milliseconds in a day. */
    private static final long MSEC_PER_DAY = (1000L * 3600L * 24L);

    private static final GeometryFactory gf = new GeometryFactory();

    private ModisDao dao;

    public PluginDataObject[] decode(byte[] data) throws Exception {

        try {
            dataFile = NetcdfFile.openInMemory(UUID.randomUUID().toString(),
                    data);
            DataTime dataTime = extractDataTime();
            return decodeNetcdf(dataTime);
        } catch (Exception e) {
            throw new Exception("Error decoding MODIS data file", e);
        }
    }

    /**
     * Decodes the MODIS netcdf file. The latitude and longitude arrays need to
     * be "flipped"
     * 
     * @param dataTime
     *            data time read from the database for this MODIS file.
     * @return
     */
    protected PluginDataObject[] decodeNetcdf(DataTime dataTime) {

        PluginDataObject[] rval = new PluginDataObject[0];
        ModisSpatialCoverage spatialRecord = new ModisSpatialCoverage();

        try {
            Group root = dataFile.getRootGroup();
            ModisRecord record = new ModisRecord();
            ModisMessageData messageData = null;
            float[] missingValues = new float[] { DEFAULT_MISSING_VALUE };
            String units = "";

            final String paramName = CI_INDEX_ID;

            record.setDataTime(dataTime);
            record.setParameter(paramName);

            // compute CI (Cyanobacterial Index) from rhos
            Object rawData = computeCI(spatialRecord, SCALE);

            // populate message data
            messageData = new ModisMessageData();
            messageData.setRawData(rawData);
            messageData.setScale(SCALE);
            messageData.setOffset(OFFSET);
            messageData.setMissingValues(missingValues);
            messageData.setUnitString(units);

            spatialRecord.setDataTime(dataTime);

            // Latitude and longitude arrays are stored in the HDF file to save
            // space in the database.
            float[] latitudes = null;
            float[] longitudes = null;
            for (Variable aVar : root.getVariables()) {
                String fullName = aVar.getFullName();
                if (fullName.startsWith(LATITUDE_DATASET_ID)
                        && latitudes == null) {
                    latitudes = (float[]) aVar.read().copyTo1DJavaArray();
                } else if (fullName.startsWith(LONGITUDE_DATASET_ID)
                        && longitudes == null) {
                    longitudes = (float[]) aVar.read().copyTo1DJavaArray();
                }
            }
            if (latitudes == null || longitudes == null) {
                throw new IOException(
                        "Error extracting lat/lons from data file");
            }

            int nx = spatialRecord.getNx();
            int ny = spatialRecord.getNy();

            // The latitude and longitude arrays need to be "flipped" or they
            // display upside down.
            ArraysUtil.flipHoriz(latitudes, ny, nx);
            ArraysUtil.flipHoriz(longitudes, ny, nx);

            messageData.setLatitudes(latitudes);
            messageData.setLongitudes(longitudes);

            double[] latRing = null;
            double[] lonRing = null;

            for (Attribute attr : root.getAttributes()) {
                if (LATITUDE_RING_ID.equals(attr.getName())) {
                    latRing = (double[]) attr.getValues().copyTo1DJavaArray();
                    if (lonRing != null) {
                        break;
                    }
                } else if (LONGITUDE_RING_ID.equals(attr.getName())) {
                    lonRing = (double[]) attr.getValues().copyTo1DJavaArray();
                    if (latRing != null) {
                        break;
                    }
                }
            }

            Geometry boundary = null;
            if (latRing != null && lonRing != null) {
                Coordinate[] coords = new Coordinate[latRing.length];
                for (int i = 0; i < latRing.length; ++i) {
                    coords[i] = new Coordinate(lonRing[i], latRing[i]);
                }
                boundary = gf.createPolygon(gf.createLinearRing(coords), null);
            }

            spatialRecord.setEnvelope(boundary.getBoundary());

            // Persist the coverage
            dao.saveOrUpdate(spatialRecord);

            double startTime = 0.0;
            startTime = getAttributeValue(START_TIME_ID, startTime);
            record.setStartTime(startTime);

            record.setLevels(GridDownscaler.getDownscaleSizes(spatialRecord
                    .getGridGeometry(latitudes, longitudes)).length);
            record.setCoverage(spatialRecord);
            record.setMessageData(messageData);

            Calendar insertCal = Calendar.getInstance();
            record.setInsertTime(insertCal);
            record.getDataURI();
            rval = new PluginDataObject[] { record };

        } catch (Exception e) {
            logger.error("Error decoding MODIS data file", e);
        }

        return rval;
    }

    /**
     * Compute and store the CI (Cyanobacterial Index) values
     * 
     * @param spatialRecord
     *            Spatial record
     * @param scale
     *            Scaling to be applied to rho values
     * @return Array of CI values
     * @throws IOException
     */

    private Object computeCI(ModisSpatialCoverage spatialRecord, float scale)
            throws IOException {

        // read rhos arrays used in computing CI
        short[] rhos667 = read_rhos(RHOS_667_ID, spatialRecord);
        short[] rhos678 = read_rhos(RHOS_678_ID, spatialRecord);
        short[] rhos748 = read_rhos(RHOS_748_ID, spatialRecord);

        int nx = spatialRecord.getNx();
        int ny = spatialRecord.getNy();
        float[] ci = new float[nx * ny];

        for (int i = 0; i < ny; i++) {
            for (int j = 0; j < nx; j++) {
                int idx = j + nx * i;
                float r667 = rhos667[idx] * scale;
                float r678 = rhos678[idx] * scale;
                float r748 = rhos748[idx] * scale;
                float ssCorrected = 0.0f;
                float ciValue = 0.0f;

                ssCorrected = (float) (r678 - r667 + (r667 - r748)
                        * ((LAMBDA - LAMBDA_MINUS) / (LAMBDA_PLUS - LAMBDA_MINUS)));
                ssCorrected = (float) (-1.335 * ssCorrected);
                if (ssCorrected < 0.0) {
                    ciValue = 0.0f;
                } else if (ssCorrected == 0.0) {
                    ciValue = 1.0f;
                } else {
                    ciValue = (float) (100.0 * (4.0 + Math.log10(ssCorrected)) + 0.5);
                    if (ciValue > 249.0) {
                        ciValue = 250.0f;
                    }
                    if (ciValue == 0.0f) {
                        ciValue = 1.0f;
                    }
                    if (r667 > 1 || r678 > 1 || r748 > 1) {
                        ciValue = 254.0f;
                    }
                }
                ci[idx] = ciValue;
            }
        }

        return ci;

    }

    /**
     * Reads in a rho array from the netcdf.
     * 
     * @param rhosName
     *            Name of the rho variable
     * @param spatialRecord
     *            Spatial record
     * @return Array of values
     * @throws IOException
     */
    private short[] read_rhos(String rhosName,
            ModisSpatialCoverage spatialRecord) throws IOException {
        Group root = dataFile.getRootGroup();

        Variable var = root.findVariable(rhosName);
        if (var == null) {
            logger.error("Error decoding netcdf file - could not find variable: "
                    + rhosName);
            return null;
        }

        Integer nx = var.getDimension(3).getLength();
        Integer ny = var.getDimension(2).getLength();
        if (spatialRecord.getNx() == null) {
            spatialRecord.setNx(nx);
        }
        if (spatialRecord.getNy() == null) {
            spatialRecord.setNy(ny);
        }

        Object rawData = var.read().copyTo1DJavaArray();
        if (rawData == null) {
            logger.warn("Could not find rawdata for dataset entry: "
                    + var.getFullName());
        }

        return (short[]) rawData;
    }

    /**
     * Flip short array along a vertical line.
     * 
     * @param baseArray
     *            Array to be flipped
     * @param height
     *            Height of array
     * @param width
     *            Width of array
     * @return
     */
    public static void flipHoriz(short[] baseArray, int height, int width) {

        short temp = 0;

        for (int i = 0; i < height / 2; i++) {
            for (int j = 0; j < width; j++) {
                temp = baseArray[j + width * i];
                baseArray[j + width * i] = baseArray[j + width
                        * (height - i - 1)];
                baseArray[j + width * (height - i - 1)] = temp;
            }
        }
    }

    /**
     * Extracts the data time from the netcdf.
     * 
     * @return the data time
     * @throws ParseException
     */
    private DataTime extractDataTime() throws ParseException {

        Integer passDate = 0;
        double startTime = 0.0;
        double temporalExtent = 0.0;

        passDate = getAttributeValue(PASS_DATE_ID, passDate);
        startTime = getAttributeValue(START_TIME_ID, startTime);
        temporalExtent = getAttributeValue(TEMPORAL_EXTENT_ID, temporalExtent);

        if (passDate == 0 || startTime == 0.0 || temporalExtent == 0.0) {
            throw new ParseException("Error extracting dataTime", passDate);
        }

        long startMillis = (long) (passDate * MSEC_PER_DAY + startTime * 1000L);
        Date startDate = new Date(startMillis);
        Date endDate = new Date((long) (startMillis + temporalExtent * 1000L));
        DataTime dataTime = new DataTime(startDate.getTime(), new TimeRange(
                startDate, endDate));

        return dataTime;
    }

    /**
     * Get an attribute value from the netcdf
     * 
     * @param name
     *            Name of attribute
     * @param var
     *            Attribute value
     * @return
     */
    @SuppressWarnings("unchecked")
    private <T> T getAttributeValue(String name, T var) {
        Attribute attrib = dataFile.findGlobalAttribute(name);
        if (attrib == null) {
            var = null;
        }
        if (var instanceof Integer) {
            var = (T) Integer.valueOf(attrib.getNumericValue().intValue());
        } else if (var instanceof Double) {
            var = (T) Double.valueOf(attrib.getNumericValue().doubleValue());
        } else if (var instanceof String) {
            var = (T) String.valueOf(attrib.getStringValue());
        }
        return var;
    }

    public ModisDao getDao() {
        return dao;
    }

    public void setDao(ModisDao dao) {
        this.dao = dao;
    }

}
