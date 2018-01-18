/**
 * This software was developed and / or modified by NOAA/NWS/OCP/ASDT
 **/
package gov.noaa.nws.ncep.ui.pgen.attrdialog.cwadialog;

import java.util.Calendar;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

import com.raytheon.uf.common.dataplugin.text.StdTextProductContainer;
import com.raytheon.uf.common.dataplugin.text.db.StdTextProduct;
import com.raytheon.uf.common.dataplugin.text.request.ExecuteAfosCmdRequest;
import com.raytheon.uf.common.dataplugin.text.request.WriteProductRequest;
import com.raytheon.uf.common.dissemination.OUPRequest;
import com.raytheon.uf.common.dissemination.OUPResponse;
import com.raytheon.uf.common.dissemination.OfficialUserProduct;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.time.util.TimeUtil;
import com.raytheon.uf.common.wmo.WMOTimeParser;
import com.raytheon.uf.viz.core.auth.UserController;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.requests.ThriftClient;

/**
 * This utility class gets product from textdb, parse product, and disseminate
 * product.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date        Ticket#  Engineer    Description
 * ----------- -------- ----------- --------------------------
 * 12/02/2016  17469    wkwock      Initial creation
 * 
 * </pre>
 * 
 * @author wkwock
 */
public class CWAProduct {
    /** logger */
    private static final IUFStatusHandler logger = UFStatus
            .getHandler(CWAProduct.class);

    /** product ID */
    private String productId;

    /** CWSU ID */
    private String CWSUid;

    /** product Text */
    private String productTxt = null;

    /** series ID string */
    private String series = null;

    /** expire time string */
    private String expire = null;

    /** Color code */
    private Color color = null;

    /** reference Time */
    private Calendar refTime = null;

    /** is operational */
    private boolean isOperational;

    /**
     * Constructor
     * 
     * @param productId
     */
    public CWAProduct(String productId, boolean isOperational) {
        this.productId = productId;
        this.isOperational = isOperational;
    }

    /**
     * Constructor
     * 
     * @param productId
     * @param CWSUid
     */
    public CWAProduct(String productId, String CWSUid, boolean isOperational) {
        this.productId = productId;
        this.CWSUid = CWSUid;
        this.isOperational = isOperational;
        parseProduct();
    }

    /**
     * set product text
     * 
     * @param productTxt
     */
    public void setProductTxt(String productTxt) {
        this.productTxt = productTxt;
    }

    /**
     * get product text
     * 
     * @return product text
     */
    public String getProductTxt() {
        if (productTxt == null) {
            List<StdTextProduct> productList = executeAfosCmd(productId,
                    isOperational);
            if (productList != null && !productList.isEmpty()) {
                productTxt = productList.get(0).getProduct();
                refTime = Calendar.getInstance();
                refTime.setTimeInMillis(productList.get(0).getRefTime());
            } else {
                productTxt = "";
            }
        }
        return productTxt;
    }

    /**
     * Retrieve a text product from text database with an AFOS command.
     * 
     * @param afosCommand
     * @param operationalMode
     * @return
     */
    static public List<StdTextProduct> executeAfosCmd(String afosCommand,
            boolean operationalMode) {
        ExecuteAfosCmdRequest req = new ExecuteAfosCmdRequest();
        req.setAfosCommand(afosCommand);
        req.setOperationalMode(operationalMode);
        try {
            Object resp = ThriftClient.sendRequest(req);
            List<StdTextProduct> productList = ((StdTextProductContainer) resp)
                    .getProductList();
            return productList;
        } catch (VizException e) {
            logger.error("Failed to request data.", e);
        }

        return null;
    }

    /**
     * get series ID string
     * 
     * @return
     */
    public String getSeries() {
        return series;
    }

    /**
     * get expire time string
     * 
     * @return expire time string
     */
    public String getExpire() {
        return expire;
    }

    /**
     * get color
     * 
     * @return color
     */
    public Color getColor() {
        if (color == null) {
            Display display = Display.getCurrent();
            color = display.getSystemColor(SWT.COLOR_GRAY);
        }
        return color;
    }

    /**
     * parse a product
     */
    private void parseProduct() {
        // default series, expire, color etc
        series = "   ";
        expire = "        ";
        color = getColor();

        String[] lines = getProductTxt().split("\n");

        for (String line : lines) {
            if (line.startsWith(CWSUid + " CWA")
                    || line.startsWith(CWSUid + " UCWA")
                    || line.startsWith(CWSUid + " MIS")) {
                String[] items = line.split("\\s+");
                String expire_date = "";
                if (line.startsWith(CWSUid + " CWA")) {
                    series = items[2];
                    expire_date = items[5];
                } else if (line.startsWith(CWSUid + " UCWA")) {
                    series = items[2];
                    expire_date = items[5];
                } else if (line.startsWith(CWSUid + " MIS COR")) {
                    series = items[3];
                    expire_date = items[5].substring(0, 7);
                } else if (line.startsWith(CWSUid + " MIS")) {
                    series = items[2];
                    expire_date = items[4].substring(7);
                }

                if (!expire_date.isEmpty()) {
                    int expireDay = Integer
                            .parseInt(expire_date.substring(0, 2));
                    int expireHour = Integer
                            .parseInt(expire_date.substring(2, 4));
                    int expireMinute = Integer
                            .parseInt(expire_date.substring(4, 6));
                    Calendar expCal = WMOTimeParser.adjustDayHourMinute(refTime,
                            expireDay, expireHour, expireMinute);
                    expire = expire_date.substring(0, 2) + "-"
                            + expire_date.substring(2);
                    getProductColor(expCal);
                } else {
                    expire = " TEST   ";
                }
            }
        }

    }

    /**
     * Get the next series ID. The series ID consists of Phenomenon Number(1-6)
     * and sequential issuance number(1-99) for CWA. And for CWS/MIS, the
     * issuance number is the series ID.
     * 
     * @param isCor
     * @return next series ID
     */
    public int getNextSeriesId(boolean isCor) {
        // get the Phenomenon Number
        int phenomenonNum = 0;
        String phenomenonNumStr = productId.substring(productId.length() - 1);
        if (phenomenonNumStr.matches("[0-9]")) {// this is a CWA product
            phenomenonNum = Integer.parseInt(phenomenonNumStr);
        }

        // get the sequential issuance number
        int issuanceNum = 1; // default to 1.
        if (!getSeries().trim().isEmpty()) {
            try {
                issuanceNum = Integer.parseInt(getSeries());
                issuanceNum = issuanceNum % 100;
                if (issuanceNum == 99) {
                    issuanceNum = 1;
                } else {
                    issuanceNum++;
                }

            } catch (NumberFormatException nfe) {
                logger.error("Failed to parse " + getSeries(), nfe);
            }
        }

        // include the Phenomenon Number
        int seriesId = phenomenonNum * 100 + issuanceNum;
        return seriesId;
    }

    /**
     * get color base on expiration time
     * 
     * @param expire_time
     */
    private void getProductColor(Calendar expireTime) {
        Display display = Display.getCurrent();

        Calendar currentTime = TimeUtil.newGmtCalendar();

        long remainingMinutes = (expireTime.getTimeInMillis()
                - currentTime.getTimeInMillis()) / TimeUtil.MILLIS_PER_MINUTE;

        if (remainingMinutes > 9) {
            color = display.getSystemColor(SWT.COLOR_WHITE);
        } else if (remainingMinutes > 0) {
            color = display.getSystemColor(SWT.COLOR_YELLOW);
        } else if (remainingMinutes > -24 * 60) {
            color = display.getSystemColor(SWT.COLOR_RED);
        } else {
            color = display.getSystemColor(SWT.COLOR_GRAY);
        }
    }

    /**
     * save product to textdb and disseminate to DEFAULTNCF
     * 
     * @param site
     * @param isOperational
     * @return
     */
    public boolean sendText(String site) {
        if (isOperational) {
            // transmit product for operational only
            OUPRequest req = new OUPRequest();
            OfficialUserProduct oup = new OfficialUserProduct();
            oup.setAwipsWanPil(productId);
            oup.setSource("CWA Generator");
            oup.setAddress("DEFAULTNCF");
            oup.setNeedsWmoHeader(false);
            oup.setFilename(productId + ".txt");
            oup.setProductText(productTxt);

            req.setCheckBBB(true);
            req.setProduct(oup);
            req.setUser(UserController.getUserObject());

            OUPResponse response;
            try {
                response = (OUPResponse) ThriftClient.sendRequest(req);
                boolean success = response.isSendLocalSuccess();
                if (response.hasFailure()) {
                    Priority p = Priority.EVENTA;
                    if (!response.isAttempted()) {
                        // if was never attempted to send or store even
                        // locally
                        p = Priority.CRITICAL;
                    } else if (!response.isSendLocalSuccess()) {
                        // if send/store locally failed
                        p = Priority.CRITICAL;
                    } else if (!response.isSendWANSuccess()) {
                        // if send to WAN failed
                        if (response.getNeedAcknowledgment()) {
                            // if ack was needed, if it never sent then no
                            // ack was recieved
                            p = Priority.CRITICAL;
                        } else {
                            // if no ack was needed
                            p = Priority.EVENTA;
                        }
                    } else if (response.getNeedAcknowledgment()
                            && !response.isAcknowledged()) {
                        // if sent but not acknowledged when acknowledgement
                        // is needed
                        p = Priority.CRITICAL;
                    }

                    logger.handle(p, response.getMessage());
                }

                return success;
            } catch (VizException e) {
                logger.error("Failed to transmit product " + productId, e);
            }
        } else {
            storeProduct(productId, productTxt);
        }
        return false;
    }

    /**
     * save product to textdb
     * 
     * @param textdbId
     * @param productText
     */
    public void storeProduct(String textdbId, String productText) {
        WriteProductRequest req = new WriteProductRequest();
        req.setProductId(textdbId);
        req.setReportData(productText);
        req.setOperationalMode(isOperational);
        try {
            ThriftClient.sendRequest(req);
            Priority p = Priority.EVENTA;
            logger.handle(p, "Product " + textdbId + " has been saved");
        } catch (VizException e) {
            logger.handle(Priority.CRITICAL,
                    "Failed to store product: " + textdbId + " to textdb", e);
        }
    }
}
