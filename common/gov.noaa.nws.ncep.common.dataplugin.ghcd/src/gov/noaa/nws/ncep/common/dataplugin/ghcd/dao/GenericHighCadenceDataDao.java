/**
 * This code has unlimited rights, and is provided "as is" by the National Centers 
 * for Environmental Prediction, without warranty of any kind, either expressed or implied, 
 * including but not limited to the implied warranties of merchantability and/or fitness 
 * for a particular purpose.
 * 
 * This code has been developed by the NCEP-SIB for use in the AWIPS2 system.
 * 
 **/

package gov.noaa.nws.ncep.common.dataplugin.ghcd.dao;

import gov.noaa.nws.ncep.common.dataplugin.ghcd.GenericHighCadenceDataConstants;
import gov.noaa.nws.ncep.common.dataplugin.ghcd.GenericHighCadenceDataRecord;
import gov.noaa.nws.ncep.common.dataplugin.ghcd.product.GenericHighCadenceDataContainer;
import gov.noaa.nws.ncep.common.dataplugin.ghcd.product.GenericHighCadenceDataField;
import gov.noaa.nws.ncep.common.dataplugin.ghcd.product.GenericHighCadenceDataFieldDefinition;
import gov.noaa.nws.ncep.common.dataplugin.ghcd.product.GenericHighCadenceDataItem;
import gov.noaa.nws.ncep.common.dataplugin.ghcd.product.GenericHighCadenceDataResolution;
import gov.noaa.nws.ncep.common.dataplugin.ghcd.product.GenericHighCadenceDataTypeInfo;
import gov.noaa.nws.ncep.common.dataplugin.ghcd.query.GenericHighCadenceDataQuery;
import gov.noaa.nws.ncep.common.dataplugin.ghcd.query.GenericHighCadenceDataReqMsg.GenericHighCadenceDataReqType;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.dataplugin.PluginException;
import com.raytheon.uf.common.dataplugin.persist.DefaultPathProvider;
import com.raytheon.uf.common.datastorage.StorageException;
import com.raytheon.uf.common.pointdata.PointDataContainer;
import com.raytheon.uf.common.pointdata.PointDataView;
import com.raytheon.uf.edex.database.DataAccessLayerException;
import com.raytheon.uf.edex.database.purge.PurgeLogger;
import com.raytheon.uf.edex.database.query.DatabaseQuery;
import com.raytheon.uf.edex.pointdata.PointDataPluginDao;

/**
 * 
 * DAO for high cadence data plugin.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#     Engineer    Description
 * -------      -------     --------    -----------
 * 01/22/2014   1100        sgurung     Initial creation
 * 07/15/2014   1100        sgurung     Modified methods to include GenericHighCadenceDataTypeInfo.datatype
 * 
 * </pre>
 * 
 * @author sgurung
 * @version 1.0
 */

public class GenericHighCadenceDataDao extends
        PointDataPluginDao<GenericHighCadenceDataRecord> {

    private Log logger = LogFactory.getLog(getClass());

    private SimpleDateFormat hdfFileDateFormat, dbRefTimeFormat;

    public GenericHighCadenceDataDao(String pluginName) throws PluginException {
        super(pluginName);
        hdfFileDateFormat = new SimpleDateFormat("-yyyy-MM-dd-HH");
        dbRefTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    }

    @Override
    public String[] getKeysRequiredForFileName() {
        return new String[] { GenericHighCadenceDataConstants.DB_REF_TIME,
                GenericHighCadenceDataConstants.DB_SOURCE,
                GenericHighCadenceDataConstants.DB_INSTRUMENT,
                GenericHighCadenceDataConstants.DB_DATA_TYPE,
                GenericHighCadenceDataConstants.DB_DATA_RESOLUTION_UNITS,
                GenericHighCadenceDataConstants.DB_DATA_RESOLUTION_VALUE };
    }

    @Override
    public GenericHighCadenceDataRecord newObject() {
        return new GenericHighCadenceDataRecord();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.uf.edex.pointdata.PointDataPluginDao#
     * getPointDataFileName (com.raytheon.uf.common.dataplugin.PluginDataObject)
     */
    @Override
    public String getPointDataFileName(GenericHighCadenceDataRecord p) {
        return this.pluginName + "-" + p.getTypeInfo().getSource() + "-"
                + p.getTypeInfo().getInstrument() + "-"
                + p.getTypeInfo().getDatatype() + "-" + p.getDataResolUnits()
                + "-" + p.getDataResolVal() + DefaultPathProvider.HDF5_SUFFIX;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.uf.edex.pointdata.PointDataPluginDao#
     * getPointDataFileName (java.util.Map) return a full HDF5 point data file
     * name (including path) E.g.
     * /awips2/edex/data/hdf5/ghcd/source/instrument/datatype/dataResolUnit
     * /dataResolVal
     * /ghcd-source-instrument-datatype-dataResolUnit-dataResolVal-yyyy
     * -mm-dd-HH-MM.h5 E.g. /awips2/edex/data/
     * hdf5/ghcd/goes15/xrs/xray/minutes/1/ghcd-goes15-xrs-xray
     * -minutes-1-2013-05-08-19-00.h5
     */
    @Override
    public String getPointDataFileName(Map<String, Object> dbResults) {
        String source = (String) dbResults
                .get(GenericHighCadenceDataConstants.DB_SOURCE);
        String instrument = (String) dbResults
                .get(GenericHighCadenceDataConstants.DB_INSTRUMENT);
        String datatype = (String) dbResults
                .get(GenericHighCadenceDataConstants.DB_DATA_TYPE);
        String dataResolUnits = (String) dbResults
                .get(GenericHighCadenceDataConstants.DB_DATA_RESOLUTION_UNITS);
        String dataResolVal = (String) dbResults.get(
                GenericHighCadenceDataConstants.DB_DATA_RESOLUTION_VALUE)
                .toString();

        String dateStr = hdfFileDateFormat.format(dbResults
                .get(GenericHighCadenceDataConstants.DB_REF_TIME));

        String filename = PLUGIN_HDF5_DIR + source + File.separator
                + instrument + File.separator + datatype + File.separator
                + dataResolUnits + File.separator + dataResolVal
                + File.separator + this.pluginName + "-" + source + "-"
                + instrument + "-" + datatype + "-" + dataResolUnits + "-"
                + dataResolVal + dateStr + DefaultPathProvider.HDF5_SUFFIX;
        logger.info("GenericHighCadenceDataDao getPointDataFileName called and returned: "
                + filename);

        return filename;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.uf.edex.pointdata.PointDataPluginDao#getFullFilePath
     * (com .raytheon.uf.common.dataplugin.PluginDataObject) return a full HDF5
     * point data file name (including path) This is called when decoding data
     */
    @Override
    public File getFullFilePath(PluginDataObject persistable) {
        File file;
        GenericHighCadenceDataRecord rec = (GenericHighCadenceDataRecord) persistable;
        String directory = PLUGIN_HDF5_DIR + rec.getTypeInfo().getSource()
                + File.separator + rec.getTypeInfo().getInstrument()
                + File.separator + rec.getTypeInfo().getDatatype()
                + File.separator + rec.getDataResolUnits() + File.separator
                + rec.getDataResolVal();

        Date refTime = ((PluginDataObject) persistable).getDataTime()
                .getRefTime();
        String dateStr = hdfFileDateFormat.format(refTime);

        String fileName = persistable.getPluginName() + "-"
                + rec.getTypeInfo().getSource() + "-"
                + rec.getTypeInfo().getInstrument() + "-"
                + rec.getTypeInfo().getDatatype() + "-"
                + rec.getDataResolUnits() + "-" + rec.getDataResolVal()
                + dateStr + DefaultPathProvider.HDF5_SUFFIX;
        file = new File(directory + File.separator + fileName);
        return file;
    }

    /**
     * Look up target field for a given source, instrument and datatype in
     * ghcd_fielddefinition table. If not present and if createField = TRUE,
     * insert it to table.
     * 
     * @param field
     *            field definition to lookup
     * @param createField
     *            boolean value to indicate whether to create the field
     */
    public boolean lookupFieldDefinition(
            GenericHighCadenceDataFieldDefinition field, boolean createField,
            String source, String instrument, String datatype) {
        boolean status = true;
        Session sess = null;
        Transaction trans = null;
        try {
            sess = getSessionFactory().openSession();
            trans = sess.beginTransaction();

            StringBuffer sql = new StringBuffer();
            sql.append(" SELECT a.name FROM ghcd_fielddefinition a ");
            sql.append(" INNER JOIN ghcd_typeinfo_ghcd_fielddefinition b ON b.fieldDefList_id = a.id ");
            sql.append(" WHERE a.name = '" + field.getName() + "'");
            sql.append(" AND b.ghcd_typeinfo_source = '" + source + "'");
            sql.append(" AND b.ghcd_typeinfo_instrument = '" + instrument + "'");
            sql.append(" AND b.ghcd_typeinfo_datatype = '" + datatype + "'");

            logger.info(" Inside GenericHighCadenceDataDao.lookupFieldDefinition(), sql = "
                    + sql.toString());

            Object[] results = executeSQLQuery(sql.toString());

            if (results.length <= 0) {
                if (createField) {
                    sess.saveOrUpdate(field);
                    trans.commit();
                } else {
                    status = false;
                }
            }

        } catch (Exception e) {
            logger.error(
                    "lookupFieldDefinition: Error occurred while looking up Field Definition["
                            + field.getName() + "]", e);
            status = false;
            if (trans != null) {
                try {
                    trans.rollback();
                } catch (Exception e1) {
                    logger.error(
                            "lookupFieldDefinition: Error occurred while rolling back transaction",
                            e);
                }
            }
        } finally {
            if (sess != null) {
                try {
                    sess.close();
                } catch (Exception e) {
                    logger.error(
                            "lookupFieldDefinition: Error occurred while closing session",
                            e);
                }
            }
        }

        return status;
    }

    /**
     * Look up target field in ghcd_fielddefinition table. If not present and if
     * createField = TRUE, insert it to table.
     * 
     * @param field
     *            field definition to lookup
     * @param createField
     *            boolean value to indicate whether to create the field
     */
    public boolean lookupFieldDefinition(
            GenericHighCadenceDataFieldDefinition field, boolean createField) {
        boolean status = true;
        Session sess = null;
        Transaction trans = null;
        try {
            sess = getSessionFactory().openSession();
            trans = sess.beginTransaction();

            Criteria crit = sess
                    .createCriteria(GenericHighCadenceDataFieldDefinition.class);

            // Criterion parentCrit = Restrictions.eq("parent_id",
            // field.getParent());
            // crit.add(parentCrit);
            Criterion nameCrit = Restrictions.eq("name", field.getName());
            crit.add(nameCrit);
            // querying...
            List<?> vals = crit.list();

            if (vals.size() <= 0) {
                if (createField) {
                    sess.saveOrUpdate(field);
                    trans.commit();
                } else {
                    status = false;
                }
            }

        } catch (Exception e) {
            logger.error(
                    "lookupFieldDefinition: Error occurred looking up Field Definition["
                            + field.getName() + "]", e);
            status = false;
            if (trans != null) {
                try {
                    trans.rollback();
                } catch (Exception e1) {
                    logger.error(
                            "lookupFieldDefinition: Error occurred rolling back transaction",
                            e);
                }
            }
        } finally {
            if (sess != null) {
                try {
                    sess.close();
                } catch (Exception e) {
                    logger.error(
                            "lookupFieldDefinition: Error occurred closing session",
                            e);
                }
            }
        }

        return status;
    }

    private boolean mergeDataTypeInfoFieldDefList(
            GenericHighCadenceDataTypeInfo targetProdInfo,
            GenericHighCadenceDataTypeInfo sourceProdInfo) {
        List<GenericHighCadenceDataFieldDefinition> srcFieldDefList = sourceProdInfo
                .getFieldDefList();
        List<GenericHighCadenceDataFieldDefinition> tarFieldDefList = targetProdInfo
                .getFieldDefList();
        boolean merged = false;
        String source = targetProdInfo.getSource();
        String instrument = targetProdInfo.getInstrument();
        String datatype = targetProdInfo.getDatatype();

        for (int index = srcFieldDefList.size() - 1; index >= 0; index--) {
            GenericHighCadenceDataFieldDefinition pm = srcFieldDefList
                    .get(index);
            // boolean found = false;

            try {
                List<Map<String, Object>> fieldDefs = getFieldDefinitions(
                        source, instrument, datatype, pm.getName());

                if (fieldDefs.size() == 0) {
                    GenericHighCadenceDataFieldDefinition newPm = srcFieldDefList
                            .remove(index);
                    tarFieldDefList.add(newPm);
                    merged = true;
                }
            } catch (Exception e) {
                logger.error(
                        "mergeDataTypeInfoFieldDefList: Error occurred while retrieving field definitions for ["
                                + source
                                + ", "
                                + instrument
                                + ", "
                                + datatype
                                + ", " + pm.getName(), e);
            }

            // for (GenericHighCadenceDataFieldDefinition p : tarFieldDefList) {
            //
            // if (p.getParent() == pm.getParent()
            // && p.getName().equals(pm.getName())) {
            // found = true;
            // break;
            // }
            // }
            // if (found == false) {
            // GenericHighCadenceDataFieldDefinition newPm = srcFieldDefList
            // .remove(index);
            // tarFieldDefList.add(newPm);
            // merged = true;
            // }
        }
        return merged;
    }

    /**
     * This method does the following:
     * 
     * 1. look up target type in ghcd_typeInfo table. If not present and if
     * createProd = TRUE, insert it to table. 2. A complete type is returned
     * using the contents found in DB 3. If the target type contains new
     * parameters, update DB
     * 
     * @param prod
     * @param createProd
     * @return
     */

    public GenericHighCadenceDataTypeInfo lookupUpdateGhcdTypeInfo(
            GenericHighCadenceDataTypeInfo prod, boolean createProd) {
        GenericHighCadenceDataTypeInfo returnProdInfo = null;
        boolean status = true;
        Session sess = null;
        Transaction trans = null;

        try {
            sess = getSessionFactory().openSession();
            trans = sess.beginTransaction();

            Criteria crit = sess
                    .createCriteria(GenericHighCadenceDataTypeInfo.class);

            Criterion sourceCrit = Restrictions.eq("source", prod.getSource());
            crit.add(sourceCrit);
            Criterion nameCrit = Restrictions.eq("instrument",
                    prod.getInstrument());
            crit.add(nameCrit);
            Criterion typeCrit = Restrictions
                    .eq("datatype", prod.getDatatype());
            crit.add(typeCrit);
            // query
            List<?> vals = crit.list();
            if (vals.size() > 0) {
                // the product is already in DB
                GenericHighCadenceDataTypeInfo dbProdInfo = (GenericHighCadenceDataTypeInfo) vals
                        .get(0);
                // check to see if there are new fields, and merge them to
                // field list in dbProdInfo
                boolean merged = mergeDataTypeInfoFieldDefList(dbProdInfo, prod);
                if (merged == true) {
                    // if there are new fields, then update product to DB
                    for (GenericHighCadenceDataFieldDefinition pm : dbProdInfo
                            .getFieldDefList()) {
                        if (lookupFieldDefinition(pm, true,
                                dbProdInfo.getSource(),
                                dbProdInfo.getInstrument(),
                                dbProdInfo.getDatatype()) == false) {
                            break;
                        }
                    }
                    sess.saveOrUpdate(dbProdInfo);
                    trans.commit();
                    returnProdInfo = dbProdInfo;
                } else {
                    returnProdInfo = dbProdInfo;
                }

            } else if (createProd) {
                for (GenericHighCadenceDataFieldDefinition pm : prod
                        .getFieldDefList()) {
                    if (lookupFieldDefinition(pm, true, prod.getSource(),
                            prod.getInstrument(), prod.getDatatype()) == false) {
                        status = false;
                        break;
                    }
                }
                if (status) {
                    sess.saveOrUpdate(prod);
                    returnProdInfo = prod;
                    trans.commit();
                }
            } else
                status = false;
        } catch (Exception e) {
            logger.error(
                    "lookupUpdateGhcdTypeInfo: Error occurred looking up GenericHighCadenceDataTypeInfo["
                            + prod.getSource()
                            + " - "
                            + prod.getInstrument()
                            + " - " + prod.getDatatype() + "]", e);
            status = false;
            if (trans != null) {
                try {
                    trans.rollback();
                } catch (Exception e1) {
                    logger.error(
                            "lookupUpdateGhcdTypeInfo: Error occurred rolling back transaction",
                            e);
                }
            }
        } finally {
            if (sess != null) {
                try {
                    sess.close();
                } catch (Exception e) {
                    logger.error(
                            "lookupUpdateGhcdTypeInfo: Error occurred closing session",
                            e);
                }
            }
        }

        return returnProdInfo;
    }

    /*
     * To create or update high cadence data type meta data information
     */
    public GenericHighCadenceDataTypeInfo updateGhcdTypeInfo(
            GenericHighCadenceDataTypeInfo prod) {
        Session sess = null;
        boolean status = true;
        Transaction trans = null;
        GenericHighCadenceDataTypeInfo rval = null;
        try {
            sess = getSessionFactory().openSession();
            trans = sess.beginTransaction();

            for (GenericHighCadenceDataFieldDefinition pm : prod
                    .getFieldDefList()) {
                if (lookupFieldDefinition(pm, true, prod.getSource(),
                        prod.getInstrument(), prod.getDatatype()) == false) {
                    status = false;
                    break;
                }
            }

            if (status) {
                sess.saveOrUpdate(prod);

                Criteria crit = sess
                        .createCriteria(GenericHighCadenceDataTypeInfo.class);

                Criterion sourceCrit = Restrictions.eq("source",
                        prod.getSource());
                crit.add(sourceCrit);
                Criterion nameCrit = Restrictions.eq("instrument",
                        prod.getInstrument());
                crit.add(nameCrit);
                Criterion typeCrit = Restrictions.eq("datatype",
                        prod.getInstrument());
                crit.add(typeCrit);
                List<?> vals = crit.list();
                if (vals.size() > 0) {
                    rval = ((GenericHighCadenceDataTypeInfo) vals.get(0))
                            .clone();
                    // System.out
                    // .println("updateGhcdTypeInfo: new field array size="
                    // + rval.getFieldDefList().size());
                }

                trans.commit();
            }

        } catch (Exception e) {
            logger.error(
                    "updateGhcdTypeInfo: Error occurred looking up product ["
                            + prod.getSource() + "-" + prod.getInstrument()
                            + " - " + prod.getDatatype() + "]", e);

            if (trans != null) {
                try {
                    trans.rollback();
                } catch (Exception e1) {
                    logger.error(
                            "updateGhcdTypeInfo: Error occurred rolling back transaction",
                            e);
                }
            }
        } finally {
            if (sess != null) {
                try {
                    sess.close();
                } catch (Exception e) {
                    logger.error(
                            "updateGhcdTypeInfo: Error occurred closing session",
                            e);
                }
            }
        }
        return rval;

    }

    public GenericHighCadenceDataTypeInfo getGhcdTypeInfo(String source,
            String instrument, String datatype) {
        GenericHighCadenceDataTypeInfo rval = null;
        if (instrument != null && datatype != null) {
            Session sess = null;
            sess = getSessionFactory().openSession();
            sess.beginTransaction();

            Criteria crit = sess
                    .createCriteria(GenericHighCadenceDataTypeInfo.class);

            Criterion sourceCrit = Restrictions.eq("source", source);
            crit.add(sourceCrit);
            Criterion nameCrit = Restrictions.eq("instrument", instrument);
            crit.add(nameCrit);
            Criterion typeCrit = Restrictions.eq("datatype", datatype);
            crit.add(typeCrit);
            List<?> vals = crit.list();
            if (vals.size() > 0) {
                try {
                    // to avoid LazyInitializationException, we have to take
                    // care of Collection before
                    // closing session. Therefore, clone() it.
                    rval = ((GenericHighCadenceDataTypeInfo) vals.get(0))
                            .clone();
                } catch (CloneNotSupportedException e) {
                    e.printStackTrace();
                }
            }
            if (sess != null) {
                try {
                    sess.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return rval;
    }

    /*
     * Get point data container for a source. Based on queryKey.
     */
    private PointDataContainer getPointDataContainer(Date refTime,
            // GenericHighCadenceDataQueryKey quertKey,
            String dataResolUnits, Integer dataResolVal,
            GenericHighCadenceDataTypeInfo prodInfo) throws Exception {
        return (getPointDataContainer(refTime, null, // quertKey,
                dataResolUnits, dataResolVal, prodInfo));
    }

    private PointDataContainer getPointDataContainer(
            Date refTime,
            Date endTime, // GenericHighCadenceDataQueryKey quertKey,
            String dataResolUnits, Integer dataResolVal,
            GenericHighCadenceDataTypeInfo prodInfo) throws Exception {
        String source = prodInfo.getSource();
        String instrument = prodInfo.getInstrument();
        String datatype = prodInfo.getDatatype();

        PointDataContainer pdc = null;

        GenericHighCadenceDataQuery pdq = new GenericHighCadenceDataQuery(
                GenericHighCadenceDataConstants.PLUGIN_NAME);
        StringBuilder returnParametersString = new StringBuilder();

        /*
         * add return fields for both DB and HDF5
         */
        // 1st:: add return fields from HDF5. They are the parameter list
        // defined in a GHCD report
        for (GenericHighCadenceDataFieldDefinition parm : prodInfo
                .getFieldDefList()) {
            String parameter = parm.getName();
            if (returnParametersString.length() > 0) {
                returnParametersString.append(",");
            }
            returnParametersString.append(parameter);
        }
        // also add the 1 HDF5 mandatory data set
        // returnParametersString.append(","
        // + GenericHighCadenceDataConstants.HDF5_SOURCE);

        // 2nd:: add return fields form DB. the parameter name need to be
        // defined in
        // gov.noaa.nws.ncep.edex.plugin.ghcd/res/pointdata/ghcddb.xml
        // returnParametersString.append(","
        // + GenericHighCadenceDataConstants.DB_SOURCE);
        returnParametersString.append(","
                + GenericHighCadenceDataConstants.DB_REPORTTYPE);
        returnParametersString.append(","
                + GenericHighCadenceDataConstants.DB_INSTRUMENT);
        returnParametersString.append(","
                + GenericHighCadenceDataConstants.DB_DATA_TYPE);
        returnParametersString.append(","
                + GenericHighCadenceDataConstants.DB_DATA_RESOLUTION_UNITS);
        returnParametersString.append(","
                + GenericHighCadenceDataConstants.DB_DATA_RESOLUTION_VALUE);
        returnParametersString.append(","
                + GenericHighCadenceDataConstants.DB_UTILITY_FLAGS);
        returnParametersString.append(","
                + GenericHighCadenceDataConstants.DB_FORECAST_TIME);
        // returnParametersString.append(","
        // + GenericHighCadenceDataConstants.DB_REF_TIME);
        returnParametersString.append(","
                + GenericHighCadenceDataConstants.DB_SOURCE);
        // parameters defined in
        // /gov.noaa.nws.ncep.edex.plugin.ghcd/utility/common_static/base/path/ghcdPathKeys.xml
        // AND those returned by dao.getKeysRequiredForFileName()
        // will be added automatically when calling
        // GenericHighCadenceDataQuery.execute()

        // GenericHighCadenceDataQuery.setParameters() is to set return fields
        // from both DB and HDF5
        pdq.setParameters(returnParametersString.toString());

        pdq.addParameter(GenericHighCadenceDataConstants.DB_INSTRUMENT,
                instrument, "=");
        pdq.addParameter(GenericHighCadenceDataConstants.DB_DATA_TYPE,
                datatype, "=");
        // if (quertKey == GenericHighCadenceDataQueryKey.BY_SOURCE)
        // pdq.addParameter(GenericHighCadenceDataConstants.DB_SOURCE, source,
        // "=");
        // else if (quertKey ==
        // GenericHighCadenceDataQueryKey.BY_SOURCE_DATA_RESOLUTION) {
        pdq.addParameter(GenericHighCadenceDataConstants.DB_SOURCE, source, "=");
        pdq.addParameter(
                GenericHighCadenceDataConstants.DB_DATA_RESOLUTION_UNITS,
                dataResolUnits, "=");
        pdq.addParameter(
                GenericHighCadenceDataConstants.DB_DATA_RESOLUTION_VALUE,
                Integer.toString(dataResolVal), "=");
        // }

        String dateStr = dbRefTimeFormat.format(refTime);
        if (endTime != null) {
            String endDateStr = dbRefTimeFormat.format(endTime);
            pdq.addParameter(GenericHighCadenceDataConstants.DB_REF_TIME,
                    dateStr, ">=");
            pdq.addParameter(GenericHighCadenceDataConstants.DB_REF_TIME,
                    endDateStr, "<=");
        } else {
            pdq.addParameter(GenericHighCadenceDataConstants.DB_REF_TIME,
                    dateStr, "=");
        }

        pdq.requestAllLevels();

        try {
            pdc = pdq.execute();
        } catch (StorageException e) {
            logger.error("HDF5 query StorageException " + e);
        }

        return pdc;
    }

    public GenericHighCadenceDataContainer getGhcdContainer(Date refTime,
            // GenericHighCadenceDataQueryKey key,
            String source, String dataResolUnits, Integer dataResolVal,
            String instrument, String datatype) throws Exception {

        GenericHighCadenceDataTypeInfo prodInfo = getGhcdTypeInfo(source,
                instrument, datatype);
        if (prodInfo == null) {
            logger.info("GHCD type is not in DB");
            return null;
        }
        PointDataContainer pdc = getPointDataContainer(refTime, // key,
                dataResolUnits, dataResolVal, prodInfo);
        if (pdc == null) {
            return null;
        }

        GenericHighCadenceDataContainer prodCon = new GenericHighCadenceDataContainer();
        prodCon.setDataTypeInfo(prodInfo);
        prodCon.setDataResolution(new GenericHighCadenceDataResolution(
                dataResolUnits, dataResolVal));
        prodCon.setSource(source);

        for (int i = 0; i < pdc.getCurrentSz(); i++) {
            PointDataView pdv = pdc.readRandom(i);

            Set<String> parameters = new HashSet<String>(pdv.getContainer()
                    .getParameters());

            // String retSource = null;
            // if (parameters
            // .contains(GenericHighCadenceDataConstants.HDF5_SOURCE)) {
            // retSource = pdv
            // .getString(GenericHighCadenceDataConstants.HDF5_SOURCE);
            // parameters.remove(GenericHighCadenceDataConstants.HDF5_SOURCE);
            // }

            // String utFlag = null;
            // if (parameters
            // .contains(GenericHighCadenceDataConstants.DB_UTILITY_FLAGS)) {
            // utFlag = pdv
            // .getString(GenericHighCadenceDataConstants.DB_UTILITY_FLAGS);
            // parameters
            // .remove(GenericHighCadenceDataConstants.DB_UTILITY_FLAGS);
            // }
            // int forecastTime = 0;
            // if (parameters
            // .contains(GenericHighCadenceDataConstants.DB_FORECAST_TIME)) {
            // forecastTime = pdv
            // .getInt(GenericHighCadenceDataConstants.DB_FORECAST_TIME);
            // parameters
            // .remove(GenericHighCadenceDataConstants.DB_FORECAST_TIME);
            // }
            // PDV id is not returned back to user, so drop it here
            parameters.remove(GenericHighCadenceDataConstants.HDF5_PDV_ID);

            List<GenericHighCadenceDataField> ghcdFields = new ArrayList<GenericHighCadenceDataField>();

            GenericHighCadenceDataItem stnPd = new GenericHighCadenceDataItem();
            stnPd.setRefTime(refTime);
            stnPd.setGhcdFields(ghcdFields);

            for (String parm : parameters) {
                try {
                    GenericHighCadenceDataField ghcdField = new GenericHighCadenceDataField(
                            parm, pdv.getString(parm));
                    ghcdFields.add(ghcdField);
                } catch (Exception e) {
                    logger.info(" GenericHighCadenceDataField = " + parm
                            + " Exception:" + e.getMessage());
                }
            }
            prodCon.getDataItemLst().add(stnPd);
        }

        return prodCon;
    }

    public List<GenericHighCadenceDataItem> getGhcdDataItems(
            List<Date> refTimeList, // GenericHighCadenceDataQueryKey key,
            String source, String dataResolUnits, Integer dataResolVal,
            String instrument, String datatype) throws Exception {
        GenericHighCadenceDataTypeInfo prodInfo = getGhcdTypeInfo(source,
                instrument, datatype);
        if (prodInfo == null) {
            return null;
        }
        List<GenericHighCadenceDataItem> dataItemsList = new ArrayList<GenericHighCadenceDataItem>();
        for (Date refTime : refTimeList) {

            PointDataContainer pdc = getPointDataContainer(refTime, // key,
                    dataResolUnits, dataResolVal, prodInfo);
            if (pdc == null) {
                continue;
            }

            for (int i = 0; i < pdc.getCurrentSz(); i++) {
                PointDataView pdv = pdc.readRandom(i);
                Set<String> parameters = new HashSet<String>(pdv.getContainer()
                        .getParameters());

                if (parameters
                        .contains(GenericHighCadenceDataConstants.DB_REPORTTYPE)) {
                    parameters
                            .remove(GenericHighCadenceDataConstants.DB_REPORTTYPE);
                }
                String rtnSource = source;
                if (parameters
                        .contains(GenericHighCadenceDataConstants.DB_SOURCE)) {
                    rtnSource = pdv
                            .getString(GenericHighCadenceDataConstants.DB_SOURCE);
                    parameters
                            .remove(GenericHighCadenceDataConstants.DB_SOURCE);
                }
                String rtnInstrument = instrument;
                if (parameters
                        .contains(GenericHighCadenceDataConstants.DB_INSTRUMENT)) {
                    rtnInstrument = pdv
                            .getString(GenericHighCadenceDataConstants.DB_INSTRUMENT);
                    parameters
                            .remove(GenericHighCadenceDataConstants.DB_INSTRUMENT);
                }
                String rtnDatatype = datatype;
                if (parameters
                        .contains(GenericHighCadenceDataConstants.DB_DATA_TYPE)) {
                    rtnDatatype = pdv
                            .getString(GenericHighCadenceDataConstants.DB_DATA_TYPE);
                    parameters
                            .remove(GenericHighCadenceDataConstants.DB_DATA_TYPE);
                }
                String rtnDataResolUnits = dataResolUnits;
                if (parameters
                        .contains(GenericHighCadenceDataConstants.DB_DATA_RESOLUTION_UNITS)) {
                    rtnDataResolUnits = pdv
                            .getString(GenericHighCadenceDataConstants.DB_DATA_RESOLUTION_UNITS);
                    parameters
                            .remove(GenericHighCadenceDataConstants.DB_DATA_RESOLUTION_UNITS);
                }
                Integer rtnDataResolVal = dataResolVal;
                if (parameters
                        .contains(GenericHighCadenceDataConstants.DB_DATA_RESOLUTION_VALUE)) {
                    rtnDataResolVal = pdv
                            .getInt(GenericHighCadenceDataConstants.DB_DATA_RESOLUTION_VALUE);
                    parameters
                            .remove(GenericHighCadenceDataConstants.DB_DATA_RESOLUTION_VALUE);
                }
                String utFlag = null;
                if (parameters
                        .contains(GenericHighCadenceDataConstants.DB_UTILITY_FLAGS)) {
                    utFlag = pdv
                            .getString(GenericHighCadenceDataConstants.DB_UTILITY_FLAGS);
                    parameters
                            .remove(GenericHighCadenceDataConstants.DB_UTILITY_FLAGS);
                }
                int forecastTime = 0;
                if (parameters
                        .contains(GenericHighCadenceDataConstants.DB_FORECAST_TIME)) {
                    forecastTime = pdv
                            .getInt(GenericHighCadenceDataConstants.DB_FORECAST_TIME);
                    parameters
                            .remove(GenericHighCadenceDataConstants.DB_FORECAST_TIME);
                }
                // PDV id is not returned back to user, so drop it here
                parameters.remove(GenericHighCadenceDataConstants.HDF5_PDV_ID);

                List<GenericHighCadenceDataField> ghcdParameters = new ArrayList<GenericHighCadenceDataField>();

                GenericHighCadenceDataItem dataItem = new GenericHighCadenceDataItem();
                dataItem.setRefTime(refTime);
                for (String parm : parameters) {
                    // System.out.println(" ************** parm = " + parm
                    // + " value = " + pdv.getString(parm));
                    GenericHighCadenceDataField ghcdParm = new GenericHighCadenceDataField(
                            parm, pdv.getString(parm));
                    ghcdParameters.add(ghcdParm);
                }
                // System.out.println(" ************** ghcdParameters size = "
                // + ghcdParameters.size());
                dataItem.setGhcdFields(ghcdParameters);

                List<GenericHighCadenceDataField> fields = dataItem
                        .getGhcdFields();

                // for (GenericHighCadenceDataField field : fields) {
                // System.out.println(" ************** field = "
                // + field.getName() + " value = " + field.getValue());
                // }
                dataItemsList.add(dataItem);
            }
        }
        return dataItemsList;
    }

    /**
     * Note: copy from PluginDao, modified code to get GHCD HDF5 file path
     * correctly.
     * 
     * Purges data from the database for this plugin with the given reference
     * time matching the given productKeys. If refTime is null, will purge all
     * data associated with the productKeys. Hdf5 must be purged separately as
     * most hdf5 files can't be purged with a single reference time. Use the
     * passed map to track what needs to be done with hdf5.
     * 
     * @param refTime
     *            The reftime to delete data for. A null will purge all data for
     *            the productKeys.
     * @param productKeys
     *            The product key/values to use as a constraint for deletions.
     *            Should be in key value pairs.
     * @param trackHdf5
     *            If true will use trackToUri to populate hdf5FileToUriPurged
     *            map.
     * @param trackToUri
     *            If true will track each URI that needs to be deleted from
     *            HDF5, if false will only track the hdf5 files that need to be
     *            deleted.
     * @param hdf5FileToUriPurged
     *            Map to be populated by purgeDataByRefTime of all the hdf5
     *            files that need to be updated. If trackToUri is true, each
     *            file will have the exact data URI's to be removed from each
     *            file. If trackToUri is false, the map will have a null entry
     *            for the list and only track the files.
     * @return Number of rows deleted from database.
     * @throws DataAccessLayerException
     */
    @Override
    @SuppressWarnings("unchecked")
    public int purgeDataByRefTime(Date refTime,
            Map<String, String> productKeys, boolean trackHdf5,
            boolean trackToUri, Map<String, List<String>> hdf5FileToUriPurged)
            throws DataAccessLayerException {

        int results = 0;

        DatabaseQuery dataQuery = new DatabaseQuery(this.daoClass);
        if ((hdf5FileToUriPurged != null)) {
            for (String key : hdf5FileToUriPurged.keySet()) {
                List<String> pairLst = hdf5FileToUriPurged.get(key);
                PurgeLogger.logInfo(
                        "starting purgeDataByRefTime hdf5FileToUriPurged map key="
                                + key, pluginName);
                if (pairLst != null) {
                    for (String val : pairLst) {
                        PurgeLogger.logInfo(
                                "starting purgeDataByRefTime hdf5FileToUriPurged map val="
                                        + val, pluginName);
                    }
                }
            }
        }
        if (refTime != null) {
            dataQuery.addQueryParam(PURGE_VERSION_FIELD, refTime);
        }

        if ((productKeys != null) && (productKeys.size() > 0)) {
            for (Map.Entry<String, String> pair : productKeys.entrySet()) {
                dataQuery.addQueryParam(pair.getKey(), pair.getValue());
                PurgeLogger.logInfo(" purgeDataByRefTime product map key="
                        + pair.getKey() + " value=" + pair.getValue(),
                        pluginName);
            }
        }

        List<PluginDataObject> pdos = null;

        dataQuery.setMaxResults(500);

        // fields for hdf5 purge
        String previousFile = null;
        StringBuilder pathBuilder = new StringBuilder();

        int loopCount = 0;
        do {
            pdos = (List<PluginDataObject>) this.queryByCriteria(dataQuery);
            if ((pdos != null) && !pdos.isEmpty()) {
                this.delete(pdos);

                if (trackHdf5 && (hdf5FileToUriPurged != null)) {
                    for (PluginDataObject pdo : pdos) {
                        pathBuilder.setLength(0);
                        GenericHighCadenceDataRecord rec = (GenericHighCadenceDataRecord) pdo;
                        String directory = PLUGIN_HDF5_DIR
                                + rec.getTypeInfo().getSource()
                                + File.separator
                                + rec.getTypeInfo().getInstrument()
                                + File.separator
                                + rec.getTypeInfo().getDatatype()
                                + File.separator + rec.getDataResolUnits()
                                + File.separator + rec.getDataResolVal();
                        int forecasttime = rec.getDataTime().getFcstTime();
                        String dateStr = hdfFileDateFormat.format(refTime);
                        // + "-f" + forecasttime;
                        String fileName = this.pluginName + "-"
                                + rec.getTypeInfo().getSource() + "-"
                                + rec.getTypeInfo().getInstrument() + "-"
                                + rec.getTypeInfo().getDatatype() + "-"
                                + rec.getDataResolUnits() + "-"
                                + rec.getDataResolVal() + dateStr
                                + DefaultPathProvider.HDF5_SUFFIX;
                        String file = directory + File.separator + fileName;
                        PurgeLogger.logInfo(++loopCount
                                + " purgeDataByRefTime file=" + file,
                                pluginName);
                        if (trackToUri) {
                            List<String> uriList = hdf5FileToUriPurged
                                    .get(file);
                            if (uriList == null) {
                                // sizing to 50 as most data types have numerous
                                // entries in a file
                                uriList = new ArrayList<String>(50);
                                hdf5FileToUriPurged.put(file, uriList);
                            }
                            uriList.add(file);
                        } else {
                            // only need to track file, tracking last file
                            // instead of constantly indexing hashMap
                            if (!file.equals(previousFile)) {
                                hdf5FileToUriPurged.put(file, null);
                                previousFile = file;
                            }
                        }
                    }
                }

                results += pdos.size();
            }

        } while ((pdos != null) && !pdos.isEmpty());
        if ((hdf5FileToUriPurged != null)) {
            for (String key : hdf5FileToUriPurged.keySet()) {
                List<String> pairLst = hdf5FileToUriPurged.get(key);
                PurgeLogger.logInfo(
                        "leaving purgeDataByRefTime hdf5FileToUriPurged map key="
                                + key, pluginName);
                if (pairLst != null) {
                    for (String val : pairLst) {
                        PurgeLogger.logInfo(
                                "leaving purgeDataByRefTime hdf5FileToUriPurged map val="
                                        + val, pluginName);
                    }
                }
            }
        }
        return results;
    }

    public Object[] getGhcdAvailDataTypes(GenericHighCadenceDataReqType reqType) {
        String queryStr;
        Object[] rtnobjArray;
        switch (reqType) {
        case GET_GHCD_ALL_AVAILABLE_TYPES_BY_DATA_RESOLUTION:
            queryStr = new String(
                    "SELECT DISTINCT typeInfo_source, typeInfo_instrument, typeInfo_datatype, dataResolUnits, dataResolVal FROM ghcd");
            break;
        case GET_GHCD_ALL_AVAILABLE_TYPES:
            queryStr = new String(
                    "SELECT DISTINCT source, instrument, datatype FROM ghcd_typeInfo");
            break;
        default:
            return null;
        }
        rtnobjArray = executeSQLQuery(queryStr);

        return rtnobjArray;
    }

    /**
     * Retrieves field definitions for a given source, instrument, datatype. <br>
     * 
     * @param source
     *            The source of data
     * @param instrument
     *            The instrument
     * @param datatype
     *            The datatype
     * @param fieldDefName
     *            The field definition name to look for
     * @return List of data map containing the results of the query for each of
     *         the columns
     * @throws Exception
     */
    public List<Map<String, Object>> getFieldDefinitions(final String source,
            final String instrument, final String datatype,
            final String fieldDefName) throws Exception {

        StringBuffer sql = new StringBuffer();
        sql.append(" SELECT a.name, a.datatype, a.description, a.units, a.minInclusive, a.minExclusive, a.maxInclusive, a.maxExclusive FROM ghcd_fielddefinition a ");
        sql.append(" INNER JOIN ghcd_typeinfo_ghcd_fielddefinition b ON b.fieldDefList_id = a.id ");
        sql.append(" AND b.ghcd_typeinfo_source = '" + source + "'");
        sql.append(" AND b.ghcd_typeinfo_instrument = '" + instrument + "'");
        sql.append(" AND b.ghcd_typeinfo_datatype = '" + datatype + "'");
        if (fieldDefName != null) {
            sql.append(" AND a.name = '" + fieldDefName + "'");
        }

        logger.info(" Inside GenericHighCadenceDataDao.lookupFieldDefinition(), sql = "
                + sql.toString());

        Object[] results = executeSQLQuery(sql.toString());

        if (results.length == 0) {
            return new ArrayList<Map<String, Object>>();
        }

        String[] fieldNames = { "name", "datatype", "description", "units",
                "minInclusive", "minExclusive", "maxInclusive", "maxExclusive" };
        List<Map<String, Object>> resultList = new ArrayList<Map<String, Object>>(
                results.length);
        for (Object obj : results) {
            if (obj instanceof Object[] == false) {
                obj = new Object[] { obj };
            }
            Object[] objs = (Object[]) obj;
            if (objs.length != fieldNames.length) {
                throw new Exception(
                        "Column count returned does not match expected column count");
            }
            Map<String, Object> resultMap = new HashMap<String, Object>(
                    objs.length * 2);
            for (int i = 0; i < fieldNames.length; ++i) {
                resultMap.put(fieldNames[i], objs[i]);
            }
            resultList.add(resultMap);
        }

        return resultList;

    }

}
