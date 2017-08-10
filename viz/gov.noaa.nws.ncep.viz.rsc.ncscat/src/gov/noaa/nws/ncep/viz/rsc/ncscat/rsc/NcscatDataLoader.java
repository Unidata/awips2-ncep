package gov.noaa.nws.ncep.viz.rsc.ncscat.rsc;

import gov.noaa.nws.ncep.common.dataplugin.ncscat.NcscatMode;
import gov.noaa.nws.ncep.common.dataplugin.ncscat.NcscatPoint;
import gov.noaa.nws.ncep.common.dataplugin.ncscat.NcscatRecord;
import gov.noaa.nws.ncep.viz.resources.AbstractDataLoader;
import gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsRequestableResourceData;
import gov.noaa.nws.ncep.viz.resources.IDataLoader;
import gov.noaa.nws.ncep.viz.resources.IRscDataObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;

import com.raytheon.uf.common.dataplugin.HDF5Util;
import com.raytheon.uf.common.datastorage.DataStoreFactory;
import com.raytheon.uf.common.datastorage.IDataStore;
import com.raytheon.uf.common.datastorage.Request;
import com.raytheon.uf.common.datastorage.StorageException;
import com.raytheon.uf.common.datastorage.records.IDataRecord;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.viz.core.exception.VizException;

/**
 * NcscatDataLoader - Class for display of all types of satellite
 * scatterometer/radiometer data showing ocean surface winds.
 * 
 * This code has been developed by NCEP for use in the AWIPS2 system.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 01/27/2016   R10155     B. Hebbard  Initial creation, as part of NcscatResource refactor.
 * </pre>
 * 
 * @author bhebbard
 * @version 1.0
 */
public class NcscatDataLoader extends AbstractDataLoader implements IDataLoader {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(NcscatDataLoader.class);

    private NcscatResourceData ncscatResourceData;

    public NcscatDataLoader() {
    }

    public NcscatDataLoader(
            AbstractNatlCntrsRequestableResourceData resourceData) {
        this.resourceData = resourceData;
    }

    @Override
    public void loadData() {
        try {
            queryRecords();
        } catch (VizException e) {
            statusHandler.error("Error Querying Records:  ", e);
        }
        processNewRscDataList();
    }

    @Override
    public void setResourceData(
            AbstractNatlCntrsRequestableResourceData resourceData) {
        this.ncscatResourceData = (NcscatResourceData) resourceData;
    }

    // Override to process NcscatRowData (RDO) instead of NcscatRecord (PDO)
    // since each record may contain more than one 'convSigmetSection' each of
    // which has its own obs time and must be time matched
    // separately.
    //
    @Override
    protected IRscDataObject[] processRecord(Object pdo) {
        if (!(pdo instanceof NcscatRecord)) {
            statusHandler.error("NcscatResource expecting NcscatRecord instead of:  "
                    + pdo.getClass().getName());
            return null;
        }
        NcscatRecord ncscatRecord = (NcscatRecord) pdo;

        // Given an NcscatRecord (a PDO, previously retrieved from the DB),
        // get the HDF5 data associated with it, as raw bytes...
        byte[] hdf5Data = getRawData(ncscatRecord);
        if (hdf5Data == null) {
            return null;
        } else {
            // ...convert to point data for paint, grouped by rows...
            ArrayList<NcscatRowData> rowsDataFromPdo = processHDF5Data(hdf5Data);
            // ...and return as an array of time-matchable objects (RDOs)
            return rowsDataFromPdo.toArray(new NcscatRowData[0]);
        }
    }

    private byte[] getRawData(NcscatRecord nsRecord) {

        // Given the NcscatRecord, locate the associated HDF5 data...
        File location = HDF5Util.findHDF5Location(nsRecord);

        // TODO... Investigate: Why is the following statement needed?
        // Starting in OB13.5.3, the PDO (nsRecord) has a non-null, but
        // bogus, value in its dataURI field at this point (and earlier,
        // as soon as it is deserialized after return from the metadata
        // query). nsRecord.getDataURI() below will get this bad value,
        // leading to failure on the ds.retrieve(...). Instead we force
        // it to synthesize the dataURI -- which getDataURI() does
        // correctly -- by setting the field to null first. But why is
        // this happening, and why only in OB13.5.3, and why only for
        // some resources...? (bh) (see also NTRANS resource)
        nsRecord.setDataURI(null); // force getDataURI() to construct one

        String group = nsRecord.getDataURI();
        String dataset = "Ncscat";

        // ...and retrieve it
        IDataStore ds = DataStoreFactory.getDataStore(location);
        IDataRecord dr;
        try {
            dr = ds.retrieve(group, dataset, Request.ALL);
        } catch (FileNotFoundException e) {
            statusHandler.error("Error retrieving from datastore", e);
            return null;
        } catch (StorageException e) {
            statusHandler.error("Error retrieving from datastore", e);
            return null;
        }

        return (byte[]) dr.getDataObject();
    }

    private ArrayList<NcscatRowData> processHDF5Data(byte[] hdf5Msg) {

        // Note: This code lifted from NcscatProcessing.processHDFData,
        // modified to (1) return point data in structures already
        // optimized for paint() and (2) preserve intermediate
        // organization by rows. Some further optimization may
        // be desirable. (TODO)

        final int shortsPerPoint = 9;
        final int bytesPerPoint = 2 * shortsPerPoint;
        int ji = 0, byteNumber = 0;
        int day, hour, min, sec;
        ArrayList<NcscatRowData> returnList = new ArrayList<NcscatRowData>();
        ByteBuffer byteBuffer = null;
        byteBuffer = ByteBuffer.allocate(hdf5Msg.length);
        byteBuffer.put(hdf5Msg, 0, hdf5Msg.length);

        while (ji < hdf5Msg.length) {
            day = byteBuffer.getShort(byteNumber);
            hour = byteBuffer.getShort(byteNumber + 2);
            min = byteBuffer.getShort(byteNumber + 4);
            sec = byteBuffer.getShort(byteNumber + 6);
            ji += 8;
            byteNumber += 8;
            Calendar startTime = Calendar.getInstance();
            String ID = "UTC";
            startTime.setTimeZone(TimeZone.getTimeZone(ID));
            if (day > startTime.get(Calendar.DAY_OF_YEAR)) {
                // Handle year rollover since obs
                startTime.add(Calendar.YEAR, -1);
            }
            startTime.set(Calendar.DAY_OF_YEAR, day);
            startTime.set(Calendar.HOUR_OF_DAY, hour);
            startTime.set(Calendar.MINUTE, min);
            startTime.set(Calendar.SECOND, sec);
            // Prepare a row object (minimized for paint), which has a
            // common timestamp for all the points it will contain
            NcscatRowData rowData = new NcscatRowData(startTime);

            NcscatMode ncscatMode = ncscatResourceData.getNcscatMode();
            int scatNumber = ncscatMode.getPointsPerRow();
            for (int j = ji; j < ji + scatNumber * bytesPerPoint
                    && byteNumber < hdf5Msg.length; j += bytesPerPoint) {
                NcscatPoint sPointObj = new NcscatPoint();
                sPointObj.setLat(byteBuffer.getShort(j));
                sPointObj.setLon(byteBuffer.getShort(j + 2));
                sPointObj.setIql(byteBuffer.getShort(j + 4));
                sPointObj.setIsp(byteBuffer.getShort(j + 6));
                sPointObj.setIdr(byteBuffer.getShort(j + 8));
                byteNumber += bytesPerPoint;
                // Above code put things into a NcscatPoint (common with
                // decoder); now convert to a NcscatPointData (local class)
                // optimized for display via paint(), and add to the row
                rowData.rowPoints
                        .add(new NcscatPointData(sPointObj, ncscatMode));
            }

            ji = byteNumber;

            // Row complete; add it to the list we're building covering all
            // rows in this HDF5 message
            returnList.add(rowData);

        } // while

        return returnList;
    }

}
