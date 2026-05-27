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

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;

import gov.noaa.nws.ncep.ui.pgen.PgenConstant;
import gov.noaa.nws.ncep.ui.pgen.PgenStaticDataProvider;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

/**
 * Object for unmarshalling the outlooktimes.xml to objects.
 *
 * <pre>
 * SOFTWARE HISTORY
 *
 * Date          Ticket#     Engineer  Description
 * ------------- ----------- --------- -----------------------------------------
 * Aug 20,2020   80844       pbutler   Update for changing default Days/Prods activity and days from outlooktimes.xml config file.
 * Nov 30, 2020  85416       tjensen   Improve OutlookTimeProduct lookup
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

    private final Map<String, OutlookTimeProduct> productMap = new HashMap<>();

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

            Unmarshaller jaxbUnmarshaller = jaxbContext
                    .createUnmarshaller();

            OutlookTimesProductSet outlookTimesProductSet = (OutlookTimesProductSet) jaxbUnmarshaller
                    .unmarshal(file);

            List<OutlookTimeProduct> products = outlookTimesProductSet
                    .getProducts();

            for (OutlookTimeProduct product : products) {
                productMap.put(getKey(product), product);
            }

        } catch (JAXBException e) {
            statusHandler.error(
                    "Error loading context for OutlookTimeProduct, no OutlookTimeProduct will be loaded.",
                    e);
        }

    }

    /**
     * @param product
     * @return
     */
    private String getKey(OutlookTimeProduct product) {

        String fullType = PgenConstant.DEFAULT_ACTIVITY_TYPE;
        String type = product.getType();
        String subtype = product.getSubType();

        if (type != null && type.trim().length() > 0) {
            fullType = type;

            if (subtype != null && subtype.trim().length() > 0 && !subtype
                    .trim().equalsIgnoreCase(PgenConstant.DEFAULT_SUBTYPE)) {
                fullType += ("(" + subtype + ")");
            }
        }

        return fullType;
    }

    /**
     * @param pd
     * @return
     */
    public OutlookTimeProduct getProduct(String pdType) {
        OutlookTimeProduct otpValue = null;
        // - check for product default vs activity
        if (productMap.containsKey(pdType)) {
            otpValue = productMap.get(pdType);
            otpValue.setDefault(false);
        } else {
            otpValue = productMap.get("Default");
            otpValue.setDefault(true);
        }

        return otpValue;
    }
}
