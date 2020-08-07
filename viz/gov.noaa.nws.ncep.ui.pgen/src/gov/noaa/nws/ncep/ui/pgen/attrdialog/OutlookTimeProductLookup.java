/**
 * This software was developed and / or modified by Raytheon Company,
 * pursuant to Contract DG133W-05-CQ-1067 with the US Government.
 *
 * U.S. EXPORT CONTROLLED TECHNICAL DATA
 * This software product contains export-restricted data whose
 * export/transfer/disclosure is restricted by U.S. law. Dissemination
 * to non-U.S. persons whether in the United States or abroad requires
 * an export license or other authorization.
 *
 * Contractor Name:        Raytheon Company
 * Contractor Address:     6825 Pine Street, Suite 340
 *                         Mail Stop B8
 *                         Omaha, NE 68106
 *                         402.291.0100
 *
 * See the AWIPS II Master Rights File ("Master Rights File.pdf") for
 * further licensing information.
 **/
package gov.noaa.nws.ncep.ui.pgen.attrdialog;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;

import gov.noaa.nws.ncep.ui.pgen.PgenStaticDataProvider;

/**
 * Object for unmarshalling the outlooktimes.xml to objects.
 * 
 * <pre>
 * SOFTWARE HISTORY
 *
 * Date          Ticket#     Engineer  Description
 * ------------- ----------- --------- -----------------------------------------
 * Aug 20,2020   80844       pbutler   Update for changing default Days/Prods activity and days from outlooktimes.xml config file.
 * 
 * </pre>
 *
 * @author pbutler
 */

public class OutlookTimeProductLookup {
    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(OutlookTimeProductLookup.class);

    private static OutlookTimeProductLookup instance;

    public OutlookTimeProductLookup getInstance() {
        if (instance == null) {
            instance = new OutlookTimeProductLookup();
        }
        return instance;
    }

    private Map<String, OutlookTimeProduct> productMap = new HashMap<String, OutlookTimeProduct>();

    OutlookTimeProductLookup() {
        init();
    }

    private void init() {
        /** Local file pointer */
        File file;

        String outlookTimesFile = PgenStaticDataProvider.getProvider()
                .getFileAbsolutePath(PgenStaticDataProvider.getProvider()
                        .getPgenLocalizationRoot() + "outlooktimes.xml");

        file = new File(outlookTimesFile);

        try {
            JAXBContext jaxbContext = JAXBContext
                    .newInstance(OutlookTimesProductSet.class);

            javax.xml.bind.Unmarshaller jaxbUnmarshaller = jaxbContext
                    .createUnmarshaller();

            OutlookTimesProductSet outlookTimesProductSet = (OutlookTimesProductSet) jaxbUnmarshaller
                    .unmarshal(file);

            List<OutlookTimeProduct> products = outlookTimesProductSet
                    .getProducts();

            for (OutlookTimeProduct product : products) {
                productMap.put(product.getType(), product);
            }

        } catch (JAXBException e) {
            statusHandler.error(
                    "Error loading context for OutlookTimeProduct, no OutlookTimeProduct will be loaded.",
                    e);
        }

    }

    // - Getters/Setters
    public Map<String, OutlookTimeProduct> getProductMap() {
        return productMap;
    }

    public void setProductMap(Map<String, OutlookTimeProduct> productMap) {
        this.productMap = productMap;
    }

}
