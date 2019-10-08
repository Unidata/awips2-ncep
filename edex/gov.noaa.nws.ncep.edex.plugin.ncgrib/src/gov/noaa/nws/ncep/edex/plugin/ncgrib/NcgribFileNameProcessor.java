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
 * 03/01/2012              bsteffen    Initial creation
 * 05/28/2013    995       B. Yin      Get model name from NcgribModelNameMap
 * June  2013              T. Lee      Added NFCENS
 * 10/15/2012    2473      bsteffen    Move to ncgrib plugin
 * 5/2014                  S. Gilbert  Removed absolute path from fileName
 * 5/2014                  T. Lee      Added HYSPLIT
 * 07/11/2016    R8514     S. Russell  Updated member variable 
 *                                     HURRICANE_PATTERN and method process() 
 *                                     for new hurricane file name nomenclature.
 * 08/22/2017    7718      K. Sunil    Updated for new HWRF and HMON file names.
 * 10/08/2019    69705     K. Sunil    Do not track datasetid as a member variable.
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

    // For hysplit model
    private static final Pattern HYSPLIT_PATTERN = Pattern
            .compile("^e\\d{10}$");

    // Examples of hurricane data filenames, HWRF and GHM respectively
    // hur_hwrf.2016060806_one01e.2016060806.hwrfprs.storm.0p02.f126.grb2
    // hur_hur.2016060806_one01e.2016060806.grib.1p00.f96.grib2
    private static final Pattern HURRICANE_HWRF_PATTERN = Pattern
            .compile("^hur_h(ur|wrf)$");

    private static final String HURRICANE_MODEL_HWRF = "hwrf";

    private static final String HURRICANE_MODEL_HWRF_INNER_CORE = "hwrfInnerCore";

    private static final String HURRICANE_MODEL_HWRF_OUTER_CORE = "hwrfOuterCore";

    private static final String HURRICANE_MODEL_HWRF_GLOBAL = "hwrfGlobal";

    private static final String HURRICANE_MODEL_GHM = "ghm";

    private static final String HURRICANE_FILE_HEAD_HWRF = "hur_hwrf";

    private static final String HURRICANE_FILE_HEAD_GHM = "hur_hur";

    private static final String HURRICANE_MODEL_HMON = "hmon";

    private static final String HURRICANE_FILE_HEAD_HMON = "hmon_hmon";

    private static final String HURRICANE_MODEL_RES_P125 = "0p125";

    private static final String HURRICANE_MODEL_RES_P20 = "0p20";

    private static final String HURRICANE_MODEL_RES_P02 = "0p02";

    private static final String HURRICANE_MODEL_RES_P06 = "0p06";

    private static final String HURRICANE_MODEL_RES_1P0 = "1p00";

    protected static final String EXCHANGE_HEADER = "CamelFileNameOnly";

    private static NcgribModelNameMap modelMap = null;

    /**
     * Extract the datasetid and secondarid or the ensembleid from the name of
     * the input file
     * 
     * @see org.apache.camel.Processor#process(org.apache.camel.Exchange)
     */
    @Override
    public void process(Exchange exchange) throws Exception {

        String datasetid = null;

        String secondaryid = null;

        String ensembleid = null;

        String flName = (String) exchange.getIn()
                .getHeader("CamelFileNameOnly");

        // If filename not found in Header, look in body in case it came in from
        // an ingest queue

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

        String[] nameTokens = flName.split("\\.");

        String resolution;

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
            } else if (HYSPLIT_PATTERN.matcher(token).find()) {
                secondaryid = nameTokens[0];
                datasetid = "HYSPLIT";

                // HWRF hurricanes

            } else if (HURRICANE_HWRF_PATTERN.matcher(token).find()) {
                Matcher matcher = HURRICANE_HWRF_PATTERN.matcher(token);
                matcher.find();

                // If HWRF
                if (nameTokens[0].equalsIgnoreCase(HURRICANE_FILE_HEAD_HWRF)) {

                    // domain can be (synoptic|global|storm|core)
                    String domain = nameTokens[4];
                    resolution = nameTokens[5].toLowerCase();

                    // domain "core" and domain "storm" each have a resolution
                    // of 0p02. Differentiate them by adding the domain
                    // to the datasetid when the domain is "storm"
                    if ("global".equalsIgnoreCase(domain)) {
                        datasetid = HURRICANE_MODEL_HWRF_GLOBAL;
                    } else if (resolution
                            .equalsIgnoreCase(HURRICANE_MODEL_RES_P125)) {
                        // synoptic
                        datasetid = HURRICANE_MODEL_HWRF;
                    } else if ("storm".equalsIgnoreCase(domain) && resolution
                            .equalsIgnoreCase(HURRICANE_MODEL_RES_P02)) {
                        datasetid = HURRICANE_MODEL_HWRF_OUTER_CORE;
                    } else if ("core".equalsIgnoreCase(domain) && resolution
                            .equalsIgnoreCase(HURRICANE_MODEL_RES_P02)) {
                        datasetid = HURRICANE_MODEL_HWRF_INNER_CORE;
                    } else {
                        datasetid = HURRICANE_MODEL_HWRF + resolution;
                    }

                }
                // Else GHM
                else if (nameTokens[0]
                        .equalsIgnoreCase(HURRICANE_FILE_HEAD_GHM)) {
                    if (nameTokens[4]
                            .equalsIgnoreCase(HURRICANE_MODEL_RES_1P0)) {
                        datasetid = HURRICANE_MODEL_GHM;
                    } else {
                        // resource ID + resolution
                        resolution = nameTokens[4].toLowerCase();
                        datasetid = HURRICANE_MODEL_GHM + resolution;
                    }
                }

            }

            // Else HMON (else if the first token parsed from the filename
            // is "hmon")
            else if (nameTokens[0].equalsIgnoreCase(HURRICANE_FILE_HEAD_HMON)) {
                // Based on the 5th token (the resolution) set the datasetid
                resolution = nameTokens[5].toLowerCase();

                if (nameTokens[5].equalsIgnoreCase(HURRICANE_MODEL_RES_P20)) {
                    datasetid = HURRICANE_MODEL_HMON;
                } else if (nameTokens[5]
                        .equalsIgnoreCase(HURRICANE_MODEL_RES_P06)) {
                    datasetid = HURRICANE_MODEL_HMON + resolution;
                } else if (nameTokens[5]
                        .equalsIgnoreCase(HURRICANE_MODEL_RES_P02)) {
                    datasetid = HURRICANE_MODEL_HMON + resolution;
                }
            }

            /*
             * The secondaryid is the storm name embedded in the file name
             * Example: hur_hur.2016060806_one01e.2016060806.grib.1p00.f96.grib2
             * one01e is the name of the storm, the secondaryid
             */
            String[] underscoreSplitTokens = nameTokens[1].split("_");
            secondaryid = underscoreSplitTokens[1];

        }

        if (datasetid == null) {
            if (modelMap == null) {
                modelMap = NcgribModelNameMap.load();
            }
            // Get model name from grib file template
            datasetid = modelMap.getModelName(flName);
        } else {
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
