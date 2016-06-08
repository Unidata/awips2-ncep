package gov.noaa.nws.ncep.edex.plugin.ncgrib;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

/**
 * 
 * Processor for ncep grib files, this processor has lots of hard coded
 * assumptions about file naming that need to be more generic based off ncep
 * file names.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Mar 01, 2012            bsteffen    Initial creation
 * May 29, 2013		995		B. Yin		Get model name from NcgribModelNameMap
 * June, 2013				T. Lee		Added NFCENS
 * Oct 15, 2012 2473        bsteffen    Move to ncgrib plugin
 * 5/2014                   S. Gilbert  Removed absolute path from fileName
 * 5/2014                   T. Lee      Added HYSPLIT
 * 
 * </pre>
 * 
 * @author bsteffen
 * @version 1.0
 */
public class NcgribFileNameProcessor implements Processor {

    // grab all known ensemble ids; mainly SREF
    private static final Pattern ENSEMBLE_ID_PATTERN = Pattern
            .compile("^(p|n|ctl)\\d{0,2}$");

    // grab global wind and wave ensemble IDs
    private static final Pattern ENSEMBLE_WAVE_PATTERN = Pattern
            .compile("^\\d{8}_gep(\\d{0,2}{2})$");

    // grab global wind and wave ensemble IDs
    private static final Pattern ENSEMBLE_NFC_PATTERN = Pattern
            .compile("^\\d{8}_HTSGW_(\\d{2})$");

    // anything that ends in nest is assumed to be a nested grid identifier
    // might add alaska fire weather later...
    private static final Pattern FIREWXNEST_ID_PATTERN = Pattern
            .compile("^firewxnest$");

    // This is the least generic pattern ever, are there any constraints on
    // event names, who knows?
    private static final Pattern HURRICANE_PATTERN = Pattern
            .compile("^\\d{10}_([a-z]*)\\d{1,2}[lewcs]$");

    // For hysplit model
    private static final Pattern HYSPLIT_PATTERN = Pattern
            .compile("^e\\d{10}$");

    private static NcgribModelNameMap modelMap = null;

    @Override
    public void process(Exchange exchange) throws Exception {
        String flName = (String) exchange.getIn()
                .getHeader("CamelFileNameOnly");

        /*
         * If filename not found in Header, look in body in case it came in from
         * an ingest queue
         */
        if (flName == null) {
            Object payload = exchange.getIn().getBody();
            if (payload instanceof byte[]) {
                flName = new String((byte[]) payload);
            } else if (payload instanceof String) {
                flName = (String) payload;
            }
            // Remove Path from filename
            if (flName != null) {
                File f = new File(flName);
                flName = f.getName();
            }
        }

        String datasetid = null;
        String secondaryid = null;
        String ensembleid = null;
        String[] nameTokens = flName.split("\\.");

        for (String token : nameTokens) {
            if (ENSEMBLE_ID_PATTERN.matcher(token).find()) {
                ensembleid = token;
            } else if (ENSEMBLE_WAVE_PATTERN.matcher(token).find()) {
                Matcher matcher = ENSEMBLE_WAVE_PATTERN.matcher(token);
                matcher.find();
                ensembleid = matcher.group(1);
            } else if (ENSEMBLE_NFC_PATTERN.matcher(token).find()) {
                Matcher matcher = ENSEMBLE_NFC_PATTERN.matcher(token);
                datasetid = "nfcens";
                matcher.find();
                ensembleid = matcher.group(1);
            } else if (FIREWXNEST_ID_PATTERN.matcher(token).find()) {
                datasetid = "fireWxNAM";
            } else if (HURRICANE_PATTERN.matcher(token).find()) {
                Matcher matcher = HURRICANE_PATTERN.matcher(token);
                matcher.find();
                secondaryid = matcher.group(1);
                datasetid = "GHM";

                if (nameTokens[3].equalsIgnoreCase("gribn3")) {
                    datasetid = "GHMNEST";
                } else if (nameTokens[3].equalsIgnoreCase("grib6th")) {
                    datasetid = "GHM6TH";
                } else if (nameTokens[3].equalsIgnoreCase("hwrfprs_n")) {
                    datasetid = "HWRFNEST";
                } else if (nameTokens[3].equalsIgnoreCase("hwrfprs_p")) {
                    datasetid = "HWRF";
                }

            } else if (HYSPLIT_PATTERN.matcher(token).find()) {
                secondaryid = nameTokens[0];
                datasetid = "HYSPLIT";
            }
        }

        if (modelMap == null) {
            modelMap = NcgribModelNameMap.load();
        }

        // Get model name from grib file template
        if (datasetid == null) {
            datasetid = modelMap.getModelName(flName);
        }

        if (datasetid != null) {
            exchange.getIn().setHeader("datasetid", datasetid);
        }
        if (secondaryid != null) {
            exchange.getIn().setHeader("secondaryid", secondaryid);
        }
        if (ensembleid != null) {
            exchange.getIn().setHeader("ensembleid", ensembleid);
        }
    }

}
