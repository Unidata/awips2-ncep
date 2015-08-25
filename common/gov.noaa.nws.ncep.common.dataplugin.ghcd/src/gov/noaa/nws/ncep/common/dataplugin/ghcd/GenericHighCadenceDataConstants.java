/**
 * This code has unlimited rights, and is provided "as is" by the National Centers 
 * for Environmental Prediction, without warranty of any kind, either expressed or implied, 
 * including but not limited to the implied warranties of merchantability and/or fitness 
 * for a particular purpose.
 * 
 */
package gov.noaa.nws.ncep.common.dataplugin.ghcd;

/**
 * 
 * High cadence data constants.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 01/23/2014   1100       sgurung     Initial creation
 * 07/15/2014   1100       sgurung     Added constant DB_DATA_TYPE
 * 
 * </pre>
 * 
 * @author sgurung
 * @version 1.0
 */
public class GenericHighCadenceDataConstants {

    public static final String PLUGIN_NAME = "ghcd";

    public static final String HDF5_PDV_CURIDX = "pointDataView.curIdx";

    public static final String HDF5_PDV_ID = "id";

    public static final int MANDATORY_DATASET_NUM = 1;

    public static final String MAX_LEVELS = "maxLevels";

    // db fields defined in GenericHighCadenceDataRecord
    public static final String DB_SOURCE = "typeInfo.source";

    public static final String DB_INSTRUMENT = "typeInfo.instrument";

    public static final String DB_DATA_TYPE = "typeInfo.datatype";

    public static final String DB_REF_TIME = "dataTime.refTime";

    public static final String DB_FORECAST_TIME = "dataTime.fcstTime";

    public static final String DB_UTILITY_FLAGS = "dataTime.utilityFlags";

    public static final String DB_REPORTTYPE = "reportType";

    public static final String DB_DATA_RESOLUTION_UNITS = "dataResolUnits";

    public static final String DB_DATA_RESOLUTION_VALUE = "dataResolVal";

    public static final String MISSING_DATA_VALUE_STR = "-9999.99";

    public static final double MISSING_DATA_VALUE = -9999.99;
}
