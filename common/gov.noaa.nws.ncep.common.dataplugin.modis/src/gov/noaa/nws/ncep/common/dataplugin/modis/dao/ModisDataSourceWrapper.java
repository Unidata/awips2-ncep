package gov.noaa.nws.ncep.common.dataplugin.modis.dao;

import com.raytheon.uf.common.numeric.source.DataSource;

/**
 * Wraps a {@link DataSource} and checks for an array of missingValues and
 * returns Double.NaN instead
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer     Description
 * ------------ ---------- -----------  --------------------------
 * 10/01/2014   R5116      kbugenhagen  Initial creation.
 * 
 * </pre>
 * 
 * @author kbugenhagen
 * @version 1.0
 */

public class ModisDataSourceWrapper implements DataSource {

    private DataSource dataSource;

    private float[] missingValues;

    public ModisDataSourceWrapper(DataSource dataSource, float[] missingValues) {
        this.dataSource = dataSource;
        this.missingValues = missingValues;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.common.geospatial.interpolation.data.DataSource#getDataValue
     * (int, int)
     */
    @Override
    public double getDataValue(int x, int y) {
        double value = dataSource.getDataValue(x, y);
        if (Double.isNaN(value) == false) {
            for (int i = 0; i < missingValues.length; ++i) {
                if (value == missingValues[i]) {
                    return Double.NaN;
                }
            }
        }
        return value;
    }

}
