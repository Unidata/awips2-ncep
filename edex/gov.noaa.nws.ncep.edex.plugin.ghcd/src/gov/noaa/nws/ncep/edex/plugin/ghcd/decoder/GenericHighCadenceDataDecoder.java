/**
 * This code has unlimited rights, and is provided "as is" by the National Centers 
 * for Environmental Prediction, without warranty of any kind, either expressed or implied, 
 * including but not limited to the implied warranties of merchantability and/or fitness 
 * for a particular purpose.
 * 
 * 
 * This code has been developed by the NCEP-SIB for use in the AWIPS2 system.
 * 
 */
package gov.noaa.nws.ncep.edex.plugin.ghcd.decoder;

import gov.noaa.nws.ncep.common.dataplugin.ghcd.GenericHighCadenceDataConstants;
import gov.noaa.nws.ncep.common.dataplugin.ghcd.GenericHighCadenceDataRecord;
import gov.noaa.nws.ncep.common.dataplugin.ghcd.dao.GenericHighCadenceDataDao;
import gov.noaa.nws.ncep.common.dataplugin.ghcd.product.GenericHighCadenceDataContainer;
import gov.noaa.nws.ncep.common.dataplugin.ghcd.product.GenericHighCadenceDataField;
import gov.noaa.nws.ncep.common.dataplugin.ghcd.product.GenericHighCadenceDataFieldDefinition;
import gov.noaa.nws.ncep.common.dataplugin.ghcd.product.GenericHighCadenceDataItem;
import gov.noaa.nws.ncep.common.dataplugin.ghcd.product.GenericHighCadenceDataRange;
import gov.noaa.nws.ncep.common.dataplugin.ghcd.product.GenericHighCadenceDataTypeInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.SchemaOutputResolver;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.raytheon.edex.exception.DecoderException;
import com.raytheon.edex.plugin.AbstractDecoder;
import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.dataplugin.PluginException;
import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.LocalizationContext;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.pointdata.Dimension;
import com.raytheon.uf.common.pointdata.ParameterDescription;
import com.raytheon.uf.common.pointdata.PointDataContainer;
import com.raytheon.uf.common.pointdata.PointDataDescription;
import com.raytheon.uf.common.pointdata.PointDataDescription.Type;
import com.raytheon.uf.common.pointdata.PointDataView;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.time.DataTime;

/**
 * Decoder for generic high cadence data.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#     Engineer    Description
 * -------      -------     --------    -----------
 * 01/22/2014   1100        sgurung     Initial creation
 * 03/07/2014   1100        sgurung     Added changes to support new xml format
 * 07/15/2014   1100        sgurung     Validate the input xml file against xsd file name 
 *                                      mentioned in the input file
 * 
 * </pre>
 * 
 * @author sgurung
 * @version 1.0
 * 
 */

public class GenericHighCadenceDataDecoder extends AbstractDecoder {
    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(GenericHighCadenceDataDecoder.class);

    private String pluginName;

    private GenericHighCadenceDataDao ghcdDao;

    private static GenericHighCadenceDataDecoder instance = null;

    public GenericHighCadenceDataDecoder() throws DecoderException {
        instance = this;
        try {
            ghcdDao = new GenericHighCadenceDataDao(
                    GenericHighCadenceDataConstants.PLUGIN_NAME);

        } catch (PluginException e) {
            statusHandler.handle(Priority.PROBLEM,
                    "Error creating GenericHighCadenceDataDao", e);
        }
    }

    public static GenericHighCadenceDataDecoder getInstance() {
        if (instance != null)
            return instance;
        else {
            try {
                instance = new GenericHighCadenceDataDecoder();
            } catch (DecoderException e) {
                statusHandler
                        .handle(Priority.PROBLEM,
                                "Error getting an instance of GenericHighCadenceDataDecoder",
                                e);
            }
            return instance;
        }
    }

    /**
     * Decode GHCD XML file input from SBN/ghcd
     * 
     * @param inputProdStr
     *            : XML file
     * @return PluginDataObject[]
     * @throws DecoderException
     * @throws PluginException
     */
    @SuppressWarnings("resource")
    public PluginDataObject[] decode(File inputFile) throws DecoderException,
            PluginException {

        PluginDataObject[] recordObjects = new PluginDataObject[0];
        InputStream is = null;
        GenericHighCadenceDataContainer ghcdc = null;
        GenericHighCadenceDataTypeInfo ghcdTypeInfo = null;
        GenericHighCadenceDataRecord ghcdRec = null;
        JAXBContext ctx = null;

        try {
            is = new FileInputStream(inputFile);

        } catch (FileNotFoundException e) {
            statusHandler.handle(Priority.PROBLEM, "Error reading file: "
                    + inputFile, e);
            return new PluginDataObject[0];
        }

        // validate the input file against the schema file (file name provided
        // in the input file)
        boolean validationResult = validateXmlFileAgainstSchema(inputFile);
        if (!validationResult) {
            return new PluginDataObject[0];
        }

        try {

            ctx = JAXBContext.newInstance(
                    GenericHighCadenceDataContainer.class,
                    GenericHighCadenceDataItem.class);

            if (ctx != null && is != null) {
                Unmarshaller um = ctx.createUnmarshaller();
                if (um != null) {
                    Object result = um.unmarshal(is);
                    if (result instanceof GenericHighCadenceDataContainer)
                        ghcdc = (GenericHighCadenceDataContainer) result;
                }
            }

        } catch (JAXBException e) {
            statusHandler.handle(Priority.PROBLEM, "Error unmarshalling file: "
                    + inputFile, e);
            e.printStackTrace();
            return new PluginDataObject[0];
        }

        List<PluginDataObject> rtObLst = null;

        if (ghcdc != null) {

            List<GenericHighCadenceDataItem> prodItemLst = ghcdc
                    .getDataItemLst();

            int prodItemLstSize = prodItemLst.size();
            if (prodItemLstSize <= 0)
                return new PluginDataObject[0];

            if (ghcdc.getDataTypeInfo() == null)
                return new PluginDataObject[0];

            GenericHighCadenceDataTypeInfo srcGhcdTypeInfo = ghcdc
                    .getDataTypeInfo();
            ghcdTypeInfo = new GenericHighCadenceDataTypeInfo(
                    ghcdc.getSource(), srcGhcdTypeInfo.getInstrument(),
                    srcGhcdTypeInfo.getDatatype(),
                    srcGhcdTypeInfo.getDescription(),
                    srcGhcdTypeInfo.getFieldDefList());

            setDataRange(ghcdTypeInfo);

            ghcdTypeInfo = ghcdDao.lookupUpdateGhcdTypeInfo(ghcdTypeInfo, true);

            if (ghcdTypeInfo == null)
                return new PluginDataObject[0];

            String source = ghcdc.getSource();
            String dataResolUnits = ghcdc.getDataResolution().getUnits();
            Integer dataResolVal = ghcdc.getDataResolution().getValue();
            PointDataDescription pdd = createPointDataDescription(ghcdTypeInfo);
            if (pdd != null) {
                rtObLst = new ArrayList<PluginDataObject>();
                for (GenericHighCadenceDataItem dataItem : prodItemLst) {
                    DataTime dataTime = new DataTime(dataItem.getRefTime());

                    PointDataView view = createPointDataView(pdd, dataItem,
                            source);
                    ghcdRec = new GenericHighCadenceDataRecord(ghcdTypeInfo,
                            source, getPluginName().toUpperCase(),
                            dataResolUnits, dataResolVal, view, dataTime);
                    rtObLst.add(ghcdRec);
                }
            }

        }

        if (rtObLst == null || rtObLst.size() <= 0)
            return new PluginDataObject[0];

        recordObjects = rtObLst.toArray(new PluginDataObject[rtObLst.size()]);

        return recordObjects;
    }

    private PointDataDescription createPointDataDescription(
            GenericHighCadenceDataTypeInfo prodInfo) {
        int parmsize = prodInfo.getFieldDefList().size();
        if (parmsize <= 0)
            return null;
        PointDataDescription newPdd = new PointDataDescription();
        int i = 0;

        newPdd.parameters = new ParameterDescription[parmsize];
        // + GenericHighCadenceDataConstants.MANDATORY_DATASET_NUM];
        ParameterDescription[] parameterDescriptions = newPdd.parameters;
        for (GenericHighCadenceDataFieldDefinition parm : prodInfo
                .getFieldDefList()) {
            parameterDescriptions[i] = new ParameterDescription(parm.getName(),
                    Type.STRING,
                    GenericHighCadenceDataConstants.MISSING_DATA_VALUE,
                    parm.getUnitString());
            i++;
        }

        // remove source from hdf5
        // parameterDescriptions[i] = new ParameterDescription(
        // GenericHighCadenceDataConstants.HDF5_SOURCE, Type.STRING);
        // parameterDescriptions[i]
        // .setMaxLength(GenericHighCadenceDataConstants.MAX_SOURCE_STRING_SIZE);

        newPdd.dimensions = new Dimension[1];
        newPdd.dimensions[0] = new Dimension();
        newPdd.dimensions[0].setDimensionLength(1);
        // not important but we need to set a name here
        newPdd.dimensions[0]
                .setDimensionName(GenericHighCadenceDataConstants.MAX_LEVELS);
        newPdd.resolveDimensions();
        return newPdd;
    }

    private PointDataView createPointDataView(PointDataDescription pdd,
            GenericHighCadenceDataItem dataItem, String source) {
        PointDataView pdv = null;
        try {
            PointDataContainer container = PointDataContainer.build(pdd);

            pdv = container.append();
            // set 1-dimensional mandatory dataset SOURCE
            // pdv.setString(GenericHighCadenceDataConstants.HDF5_SOURCE,
            // source);

            // set all optional datasets
            for (GenericHighCadenceDataField ghcdParm : dataItem
                    .getGhcdFields()) {
                pdv.setString(ghcdParm.getName(), ghcdParm.getValue());
            }
        } catch (OutOfMemoryError e) {
            statusHandler.handle(Priority.PROBLEM, "Out of memory error", e);
        }
        return pdv;
    }

    /**
     * 
     * @return the pluginName
     */
    public String getPluginName() {
        return pluginName;
    }

    /**
     * 
     * @param pluginName
     *            the pluginName to set
     */
    public void setPluginName(String pluginName) {
        this.pluginName = pluginName;
    }

    /**
     * 
     * @return the ghcdDao
     */
    public GenericHighCadenceDataDao getGhcdDao() {
        return ghcdDao;
    }

    /**
     * 
     * @param ghcdDao
     *            the ghcdDao to set
     */
    public void setGhcdDao(GenericHighCadenceDataDao ghcdDao) {
        this.ghcdDao = ghcdDao;
    }

    /**
     * This API is to generate XML schema
     */
    public void generateSchema(Class<?> target, String filePath) {

        JAXBContext ctx = null;
        try {
            ctx = JAXBContext.newInstance(target);
        } catch (JAXBException e1) {
            e1.printStackTrace();
        }
        if (ctx != null) {

            try {
                ctx.generateSchema(new GhcdSchemaOutputResolver(filePath));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    class GhcdSchemaOutputResolver extends SchemaOutputResolver {

        private String filePath;

        public GhcdSchemaOutputResolver(String filePath) {
            super();
            this.filePath = filePath;
        }

        @Override
        public Result createOutput(String namespaceUri, String suggestedFileName)
                throws IOException {

            // create new file
            File file = new File(filePath);
            //
            // create stream result
            StreamResult result = new StreamResult(file);

            // set system id
            result.setSystemId(file.toURI().toURL().toString());

            // return result
            return result;
        }
    }

    /**
     * Set the data range (min inclusive, max inclusive, min exclusive, max
     * exclusive) values for a specific high cadence data type in a map
     * 
     * @param prodInfo
     *            : GenericHighCadenceDataTypeInfo
     */
    public void setDataRangeMap(GenericHighCadenceDataTypeInfo prodInfo) {

        List<GenericHighCadenceDataFieldDefinition> fieldDefLst = prodInfo
                .getFieldDefList();

        int fieldDefLstSize = (fieldDefLst != null) ? fieldDefLst.size() : 0;
        for (int i = 0; i < fieldDefLstSize; i++) {
            GenericHighCadenceDataFieldDefinition fieldDef = fieldDefLst.get(i);
            GenericHighCadenceDataRange range = fieldDef.getDataRange();

            if (range != null) {

                double minInc = (range.getMinInclusive() != null) ? range
                        .getMinInclusive()
                        : GenericHighCadenceDataConstants.MISSING_DATA_VALUE;
                double maxInc = (range.getMaxInclusive() != null) ? range
                        .getMaxInclusive()
                        : GenericHighCadenceDataConstants.MISSING_DATA_VALUE;
                double minExc = (range.getMinExclusive() != null) ? range
                        .getMinExclusive()
                        : GenericHighCadenceDataConstants.MISSING_DATA_VALUE;
                double maxExc = (range.getMaxExclusive() != null) ? range
                        .getMaxExclusive()
                        : GenericHighCadenceDataConstants.MISSING_DATA_VALUE;

                prodInfo.getFieldDefList().get(i).setMinInclusive(minInc);
                prodInfo.getFieldDefList().get(i).setMaxInclusive(maxInc);
                prodInfo.getFieldDefList().get(i).setMinExclusive(minExc);
                prodInfo.getFieldDefList().get(i).setMaxExclusive(maxExc);
            }
        }
    }

    /**
     * Save the data range (min inclusive, max inclusive, min exclusive, max
     * exclusive) values
     * 
     * @param prodInfo
     *            : GenericHighCadenceDataTypeInfo
     */
    public void setDataRange(GenericHighCadenceDataTypeInfo prodInfo) {

        List<GenericHighCadenceDataFieldDefinition> fieldDefLst = prodInfo
                .getFieldDefList();

        int fieldDefLstSize = (fieldDefLst != null) ? fieldDefLst.size() : 0;
        for (int i = 0; i < fieldDefLstSize; i++) {
            GenericHighCadenceDataFieldDefinition fieldDef = fieldDefLst.get(i);
            GenericHighCadenceDataRange range = fieldDef.getDataRange();

            if (range != null) {

                double minInc = (range.getMinInclusive() != null) ? range
                        .getMinInclusive()
                        : GenericHighCadenceDataConstants.MISSING_DATA_VALUE;
                double maxInc = (range.getMaxInclusive() != null) ? range
                        .getMaxInclusive()
                        : GenericHighCadenceDataConstants.MISSING_DATA_VALUE;
                double minExc = (range.getMinExclusive() != null) ? range
                        .getMinExclusive()
                        : GenericHighCadenceDataConstants.MISSING_DATA_VALUE;
                double maxExc = (range.getMaxExclusive() != null) ? range
                        .getMaxExclusive()
                        : GenericHighCadenceDataConstants.MISSING_DATA_VALUE;

                prodInfo.getFieldDefList().get(i).setMinInclusive(minInc);
                prodInfo.getFieldDefList().get(i).setMaxInclusive(maxInc);
                prodInfo.getFieldDefList().get(i).setMinExclusive(minExc);
                prodInfo.getFieldDefList().get(i).setMaxExclusive(maxExc);
            }
        }
    }

    /**
     * Validate the xml file against a given schema file
     * 
     * @param xmlFile
     *            : XML file
     * @param xmlSchemaFile
     *            : XML schema file
     * @return true/false
     */
    public static boolean validateAgainstSchema(File xmlFile, File xmlSchemaFile) {

        // parse an XML document into a DOM tree
        DocumentBuilder parser = null;
        Document document;

        // do the validation, if there are any issues with the
        // validation process the input is considered invalid.
        try {

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);

            // validate the DOM tree
            parser = dbf.newDocumentBuilder();
            document = parser.parse(xmlFile);
            // create a SchemaFactory capable of understanding WXS schemas
            SchemaFactory factory = SchemaFactory
                    .newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

            // load a WXS schema, represented by a Schema instance
            Source schemaFile = new StreamSource(xmlSchemaFile);
            Schema schema = factory.newSchema(schemaFile);

            // create a Validator instance, which can be used to validate an
            // instance document
            Validator validator = schema.newValidator();
            validator.validate(new DOMSource(document));

        } catch (Exception e) {
            statusHandler.handle(Priority.PROBLEM, "Error validating file: "
                    + xmlFile + " against schema: " + xmlSchemaFile, e);
            return false;
        }

        return true;
    }

    /**
     * Validate the xml file against a schema file mentioned in the input xml
     * file
     * 
     * @param xmlFile
     *            : XML file
     * @return true/false
     */
    public static boolean validateXmlFileAgainstSchema(File xmlFile) {

        // parse an XML document into a DOM tree
        DocumentBuilder parser = null;
        Document document;
        String xsdFileName = null;

        // do the validation, if there are any issues with the
        // validation process the input is considered invalid.
        try {

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);

            // validate the DOM tree
            parser = dbf.newDocumentBuilder();
            document = parser.parse(xmlFile);

            Element root = document.getDocumentElement();

            xsdFileName = root.getAttributeNS(
                    XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI,
                    "noNamespaceSchemaLocation");

            File xsdSchemaFile = getXSDFile(xsdFileName);

            if (xsdSchemaFile == null)
                return false;

            // create a SchemaFactory capable of understanding WXS schemas
            SchemaFactory factory = SchemaFactory
                    .newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

            // load a WXS schema, represented by a Schema instance
            Source schemaFile = new StreamSource(xsdSchemaFile);
            Schema schema = factory.newSchema(schemaFile);

            // create a Validator instance, which can be used to validate an
            // instance document
            Validator validator = schema.newValidator();
            validator.validate(new DOMSource(document));

        } catch (Exception e) {
            statusHandler.handle(Priority.PROBLEM, "Error validating file: "
                    + xmlFile + " against schema: " + xsdFileName, e);
            return false;
        }

        return true;
    }

    /**
     * Returns file with given name.
     * 
     * @param xsdFileName
     *            : XSD file name
     * @return XSD File
     */
    public static File getXSDFile(String xsdFileName) {

        IPathManager pathMgr = PathManagerFactory.getPathManager();

        LocalizationContext commonStaticBase = pathMgr.getContext(
                LocalizationContext.LocalizationType.COMMON_STATIC,
                LocalizationContext.LocalizationLevel.BASE);

        String xsdPath = "";

        try {
            xsdPath = pathMgr.getFile(
                    commonStaticBase,
                    "ncep" + File.separator
                            + GenericHighCadenceDataConstants.PLUGIN_NAME
                            + File.separator + xsdFileName).getCanonicalPath();

        } catch (IOException e) {
            statusHandler.handle(Priority.PROBLEM,
                    "Error getting xsd file with name '" + xsdFileName + "'.",
                    e);
            return null;
        }

        File xsdFile = new File(xsdPath);
        return xsdFile;

    }

}
