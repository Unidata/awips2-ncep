package gov.noaa.nws.ncep.viz.rsc.ntrans.rsc;

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import com.raytheon.uf.common.dataplugin.HDF5Util;
import com.raytheon.uf.common.dataquery.requests.DbQueryRequest;
import com.raytheon.uf.common.dataquery.requests.RequestConstraint;
import com.raytheon.uf.common.dataquery.responses.DbQueryResponse;
import com.raytheon.uf.common.datastorage.DataStoreFactory;
import com.raytheon.uf.common.datastorage.IDataStore;
import com.raytheon.uf.common.datastorage.Request;
import com.raytheon.uf.common.datastorage.StorageException;
import com.raytheon.uf.common.datastorage.records.IDataRecord;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.drawables.PaintProperties;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.jobs.JobPool;
import com.raytheon.uf.viz.core.requests.ThriftClient;
import com.raytheon.uf.viz.core.rsc.LoadProperties;

import gov.noaa.nws.ncep.common.dataplugin.ntrans.NtransRecord;
import gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsResource;
import gov.noaa.nws.ncep.viz.resources.INatlCntrsResource;
import gov.noaa.nws.ncep.viz.resources.manager.ResourceName;
import gov.noaa.nws.ncep.viz.rsc.ntrans.jcgm.Command;
import gov.noaa.nws.ncep.viz.rsc.ntrans.ncgm.NcCGM;
import gov.noaa.nws.ncep.viz.rsc.ntrans.ncgm.NcText;
import gov.noaa.nws.ncep.viz.rsc.ntrans.wireframe.SharedWireframeGenerator;
import gov.noaa.nws.ncep.viz.ui.display.NCNonMapDescriptor;

/**
 * NtransResource - Resource for Display of NTRANS Metafiles.
 * 
 * This code has been developed by the SIB for use in the AWIPS2 system.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 21 Nov 2012   838        B. Hebbard  Initial creation.
 * 25 Apr 2013   838        G. Hull     add request constraint to the query for the cycle time  
 * 30 Apr 2013   838        B. Hebbard  IOC version (for OB13.4.1)
 * 30 May 2013   838        B. Hebbard  Update for compatibility with changes by RTS in OB13.3.1
 *                                      [ DataStoreFactory.getDataStore(...) parameter ]
 * 29 Jul 2014   R4279      B. Hebbard  (TTR 1046) Add call to processNewRscDataList() in initResource()
 *                                      (instead of waiting for ANCR.paintInternal() to do it) so
 *                                      long CGM retrieval and parsing is done by the InitJob, and thus
 *                                      (1) user sees "Initializing..." and (2) GUI doesn't lock up
 * 29 Aug 2014              B. Hebbard  Remove time string and "/" separator from legend
 * 12 Sep 2014              B. Hebbard  Refactor to avoid regenerating paintables from CGM on each paint.
 * 19 Dec 2014       ?      B. Yin      Remove ScriptCreator, use Thrift Client.
 * 24 Oct 2016  R22550      bsteffen    Create all PaintableImages in init using multiple threads.
 * 05/05/2016   R28041      J. Beck     Add checks for null NtransRecord
 *                                      Replace logger and println with statusHandler
 *                                      Add javadoc to all methods
 *                                      Move all end-of-line comments
 * </pre>
 * 
 * @author bhebbard
 * @version 1.0
 */
public class NtransResource extends
        AbstractNatlCntrsResource<NtransResourceData, NCNonMapDescriptor>
        implements INatlCntrsResource {

    private String legendStr = "NTRANS ";

    private class FrameData extends AbstractFrameData {

        // FrameData holds information for a single frame (time)

        // the metadata record (PDO)
        NtransRecord ntransRecord;

        // AWIPS graphics elements of the image
        PaintableImage paintableImage;

        public FrameData(DataTime frameTime, int timeInt) {
            super(frameTime, timeInt);
        }

        /*
         * (non-Javadoc)
         * 
         * @see gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsResource.
         * AbstractFrameData#updateFrameData(gov.noaa.nws.ncep.viz.resources.
         * AbstractNatlCntrsResource.IRscDataObject)
         */
        @Override
        public boolean updateFrameData(IRscDataObject rscDataObj) {
            if (!(rscDataObj instanceof DfltRecordRscDataObj)) {

                statusHandler.info("Expecting DfltRecordRscDataObj instead of: "
                        + rscDataObj.getClass().getName());

                return false;
            }

            if (ntransRecord != null) {

                statusHandler.info(
                        "adding record to frame that has already been populated");
                // Add code here to check if the new data is a better time
                // match. If not then discard, and if so dispose of the
                // existing data and process the new record. [from GH]
                return false;
            }

            // Get PDO from the given RDO

            DfltRecordRscDataObj ntransRDO = (DfltRecordRscDataObj) rscDataObj;
            ntransRecord = (NtransRecord) ntransRDO.getPDO();

            return true;
        }

        /**
         * @param image
         */
        private void shuffleByteArray(byte[] image) {
            // Flip every even byte with its odd sibling (endianess reversal)
            for (int i = 0; i < image.length; i = i + 2) {
                byte tmp = image[i];
                image[i] = image[i + 1];
                image[i + 1] = tmp;
            }
        }

        /**
         * @param cgm
         */
        private void flipStrings(NcCGM cgm) {
            for (Command c : cgm.getCommands()) {
                if (c instanceof NcText) {
                    NcText nct = (NcText) c;
                    nct.flipString();
                }
            }
        }

        /**
         * Given an NtransRecord, if not null, locate the associated HDF5
         * data...
         * 
         * @param ntransRecord
         * @return the binary data
         */
        private byte[] getBinaryCgmFromNtransRecord(NtransRecord ntransRecord) {

            if (ntransRecord == null) {
                return null;
            }

            File location = HDF5Util.findHDF5Location(ntransRecord);

            // TODO... Investigate: Why is the following statement needed?
            // Starting in OB13.5.3, the PDO (nr) has a non-null, but bogus,
            // value in its dataURI field at this point (and earlier,
            // as soon as it is deserialized after return from the metadata
            // query). nr.getDataURI() below will get this bad value, leading
            // to failure on the ds.retrieve(...). Instead we force it to
            // synthesize the dataURI -- which getDataURI() does correctly --
            // by setting the field to null first. But why is this happening,
            // and why only in OB13.5.3, and why only for some resources...?
            // (bh)
            // (see also NCSCAT resource)

            ntransRecord.setDataURI(null);
            String group = ntransRecord.getDataURI();
            String dataset = "NTRANS";

            // @formatter:off
            /*
             * // get filename and directory for IDataStore String dir =
             * nr.getHDFPathProvider().getHDFPath(nr.getPluginName(), nr);
             * String filename = nr.getHDFPathProvider().getHDFFileName(
             * nr.getPluginName(), nr); File file = new File(dir, filename);
             */
            // @formatter:on

            // ...and retrieve it

            IDataStore ds = DataStoreFactory.getDataStore(location);
            IDataRecord dr = null;
            try {
                dr = ds.retrieve(group, dataset, Request.ALL);
            } catch (FileNotFoundException | StorageException e) {

                statusHandler.error("[EXCEPTION occurred retrieving CGM"
                        + " for metafile " + ntransRecord.getMetafileName()
                        + " product " + ntransRecord.getProductName() + "]", e);

                return null;
            }

            return (byte[]) dr.getDataObject();
        }

        /*
         * (non-Javadoc)
         * 
         * @see gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsResource.
         * AbstractFrameData#dispose()
         */
        @Override
        public void dispose() {
            if (paintableImage != null) {
                paintableImage.dispose();
                paintableImage = null;
            }
            super.dispose();
        }

        /**
         * @param target
         * @param wireframeGen
         */
        public void buildPaintableImages(IGraphicsTarget target,
                SharedWireframeGenerator wireframeGen) {

            // Get binary CGM image data from data store
            long t0 = System.currentTimeMillis();
            byte[] imageBytes = getBinaryCgmFromNtransRecord(ntransRecord);
            long t1 = System.currentTimeMillis();

            if (imageBytes == null) {
                return;
            }

            // Fix endianess if needed
            boolean flipped = false;
            if (imageBytes[0] == 96) {
                shuffleByteArray(imageBytes);
                flipped = true;
            }

            // Construct "jcgm" (Java objects) CGM representation of the image
            // from the binary CGM
            NcCGM cgmImage = new NcCGM();
            InputStream is = new ByteArrayInputStream(imageBytes);
            DataInput di = new DataInputStream(is);

            try {
                long t2 = System.currentTimeMillis();
                cgmImage.read(di); // <-- Voom!
                long t3 = System.currentTimeMillis();

                statusHandler.info("CGM image "
                        + ntransRecord.getImageByteCount()
                        + " bytes retrieved from HDF5 in " + (t1 - t0) + " ms"
                        + " and parsed in " + (t3 - t2) + " ms");
            } catch (Exception e) {

                statusHandler.info("CGM image "
                        + ntransRecord.getImageByteCount()
                        + " bytes retrieved from HDF5 in " + (t1 - t0) + " ms");

                statusHandler.error("EXCEPTION occurred interpreting CGM"
                        + " for metafile " + ntransRecord.getMetafileName()
                        + " product " + ntransRecord.getProductName(), e);
            }

            /*
             * Endianess revisited
             * 
             * if we shuffled the bytes before parsing then un-shuffle the
             * strings now that we know where they are
             */
            if (flipped) {
                flipStrings(cgmImage);
            }

            // TODO Add optional (cool) debug dump of CGM representation
            // cgmImage.showCGMCommands();
            double scale = 1000.000 / ntransRecord.getImageSizeX();
            try {
                ImageBuilder ib = new ImageBuilder(descriptor, target, scale);

                paintableImage = ib.build(cgmImage, wireframeGen);
                cgmImage = null;
            } catch (Exception e) {
                statusHandler
                        .error("[EXCEPTION occurred constructing paintable image for metafile "
                                + ntransRecord.getMetafileName() + " product "
                                + ntransRecord.getProductName() + "]");

                paintableImage = null;
                // don't keep trying on subsequent paints
                cgmImage = null;
                return;
            }
        }
    }

    /**
     * Create an NTRANS Metafile display resource
     * 
     * @param resourceData
     * @param loadProperties
     * @throws VizException
     */
    public NtransResource(NtransResourceData resourceData,
            LoadProperties loadProperties) throws VizException {
        super(resourceData, loadProperties);

        // Set the legend from the metafileName and productName.
        // NOTE: This assumes that the request type of EQUALS
        // (i.e. only one kind of metafileName and productName) (??) [from GH]

        if (resourceData.getMetadataMap().containsKey("metafileName")
                && resourceData.getMetadataMap().containsKey("productName")) {
            legendStr = " "
                    + resourceData.getMetadataMap().get("metafileName")
                            .getConstraintValue()
                    + "  " + resourceData.getMetadataMap().get("productName")
                            .getConstraintValue();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsResource#createNewFrame(
     * com.raytheon.uf.common.time.DataTime, int)
     */
    @Override
    protected FrameData createNewFrame(DataTime frameTime, int timeInt) {
        return new FrameData(frameTime, timeInt);
    }

    /*
     * (non-Javadoc) Query all the data in the DB matching the request
     * constraints (modelName, metaFile, and productName) and also match the
     * selected cycle time.
     * 
     * @see
     * gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsResource#initResource(
     * com.raytheon.uf.viz.core.IGraphicsTarget)
     */
    @Override
    public void initResource(IGraphicsTarget grphTarget) throws VizException {
        // Set initial display values from resource attributes (as if after
        // modification)

        // resourceAttrsModified(); // none now; possible future enhancement
        ResourceName rscName = getResourceData().getResourceName();

        // Set the constraints for the query
        String[] dts = rscName.getCycleTime().toString().split(" ");
        String cycleTimeStr = dts[0] + " "
                + dts[1].substring(0, dts[1].length() - 2);

        HashMap<String, RequestConstraint> reqConstraintsMap = new HashMap<>(
                resourceData.getMetadataMap());

        RequestConstraint timeConstraint = new RequestConstraint(cycleTimeStr);
        reqConstraintsMap.put("dataTime.refTime", timeConstraint);

        DbQueryRequest request = new DbQueryRequest();
        request.setConstraints(reqConstraintsMap);

        long t0 = System.currentTimeMillis();
        DbQueryResponse response = (DbQueryResponse) ThriftClient
                .sendRequest(request);
        long t1 = System.currentTimeMillis();

        statusHandler
                .info("Metadata records for " + this.newRscDataObjsQueue.size()
                        + " images retrieved from DB in " + (t1 - t0) + " ms");

        for (Map<String, Object> result : response.getResults()) {
            for (Object pdo : result.values()) {
                for (IRscDataObject dataObject : processRecord(pdo)) {
                    newRscDataObjsQueue.add(dataObject);
                }
            }
        }

        setAllFramesAsPopulated();

        /*
         * Following is done in ANCR.paintInternal too, but want to get it done
         * on the init thread since it's time-consuming and (1) we want to show
         * the "Initializing..." pacifier message, and (2) not lock up the GUI
         * thread during loading. (Might want to consider doing this in ANCR.)
         */
        if (!newRscDataObjsQueue.isEmpty()) {
            processNewRscDataList();
        }

        SharedWireframeGenerator wireframeGen = new SharedWireframeGenerator(
                descriptor, grphTarget);
        JobPool initPool = new JobPool("Loading NTrans Data", 4);

        for (AbstractFrameData afd : frameDataMap.values()) {
            if (afd instanceof FrameData) {
                final FrameData frameData = (FrameData) afd;
                initPool.schedule(new Runnable() {
                    @Override
                    public void run() {
                        frameData.buildPaintableImages(grphTarget,
                                wireframeGen);
                    }
                });
            }
        }
        initPool.join();
        wireframeGen.dispose();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsResource#paintFrame(gov.
     * noaa.nws.ncep.viz.resources.AbstractNatlCntrsResource.AbstractFrameData,
     * com.raytheon.uf.viz.core.IGraphicsTarget,
     * com.raytheon.uf.viz.core.drawables.PaintProperties)
     */
    @Override
    public void paintFrame(AbstractFrameData frameData, IGraphicsTarget target,
            PaintProperties paintProps) throws VizException {

        if (frameData == null || target == null || paintProps == null) {
            return;
        }
        FrameData fd = (FrameData) frameData;
        if (fd.ntransRecord == null) {

            return;
        }

        // If a ready-to-paint image has not yet been built for this frame, then
        // construct one from the (Java representation of the) CGM image

        if (fd.paintableImage == null) {
            fd.buildPaintableImages(target,
                    new SharedWireframeGenerator(descriptor, target));
        }

        fd.paintableImage.paint(target);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.uf.viz.core.rsc.AbstractVizResource#getName()
     */
    @Override
    public String getName() {
        FrameData fd = (FrameData) getCurrentFrame();
        if (fd == null || fd.getFrameTime() == null
                || (fd.paintableImage == null)) {
            return legendStr + "-No Data";
        }
        return legendStr;
    }
}