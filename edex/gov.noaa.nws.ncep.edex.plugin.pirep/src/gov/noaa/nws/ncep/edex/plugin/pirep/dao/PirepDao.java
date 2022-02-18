package gov.noaa.nws.ncep.edex.plugin.pirep.dao;

import java.util.List;

import com.raytheon.uf.common.dataplugin.PluginException;
import com.raytheon.uf.common.pointdata.PointDataDescription;
import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.edex.database.DataAccessLayerException;
import com.raytheon.uf.edex.pointdata.PointDataPluginDao;

/**
 * This software was modified from Raytheon's pirep plugin by
 * NOAA/NWS/NCEP/NCO to order to output point data in HDF5.
 **/

import gov.noaa.nws.ncep.common.dataplugin.pirep.PirepRecord;

/**
 * Set of DAO methods for Surface Observation data.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 04/28/2011              F. J. Yen   Initial creation from pirep
 * 08/31/2011    286       qzhou       Moved this from ~edex.plugin.pirep
 * Sep 05, 2013  2316      bsteffen    Unify pirep and ncpirep.
 * Dec 14, 2016  5934      njensen     Moved to edex pirep plugin
 * Feb 16, 2022  8608      mapeters    Remove populateDataStore override that matched super
 * </pre>
 *
 * @author qzhou
 */
public class PirepDao extends PointDataPluginDao<PirepRecord> {

    private PointDataDescription pdd;

    /**
     * Creates a new PirepDao
     *
     * @throws PluginException
     */
    public PirepDao(String pluginName) throws PluginException {
        super(pluginName);
    }

    /**
     * Retrieves an Pirep report using the datauri .
     *
     * @param dataURI
     *            The dataURI to match against.
     * @return The report record if it exists.
     */
    public PirepRecord queryByDataURI(String dataURI) {
        PirepRecord report = null;
        List<?> obs = null;
        try {
            obs = queryBySingleCriteria("dataURI", dataURI);
        } catch (DataAccessLayerException e) {
            logger.error("Error querying PIREP data by URI: " + dataURI, e);
        }
        if (obs != null && !obs.isEmpty()) {
            report = (PirepRecord) obs.get(0);
        }
        return report;
    }

    /**
     * Queries for to determine if a given data uri exists on the sfcobs table.
     *
     * @param dataUri
     *            The DataURI to find.
     * @return An array of objects. If not null, there should only be a single
     *         element.
     */
    public Object[] queryDataUriColumn(final String dataUri) {

        String sql = "select datauri from awips.pirep where datauri='" + dataUri
                + "';";

        Object[] results = executeSQLQuery(sql);

        return results;
    }

    @Override
    public String[] getKeysRequiredForFileName() {
        return new String[] { "dataTime.refTime" };
    }

    @Override
    public PirepRecord newObject() {
        return new PirepRecord();
    }

    @Override
    public String getPointDataFileName(PirepRecord p) {
        return "pirep.h5";
    }

    public PointDataDescription getPointDataDescription()
            throws SerializationException {
        if (pdd == null) {

            pdd = PointDataDescription.fromStream(this.getClass()
                    .getResourceAsStream("/res/pointdata/pirep.xml"));
        }
        return pdd;
    }

}
