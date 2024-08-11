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

package gov.noaa.nws.ncep.edex.plugin.ncuair.decoder;

import com.raytheon.uf.edex.esb.camel.EDEXRouteBuilder;

/**
 * Camel routes converted from file "ncuair-ingest.xml", context "ncuair-camel"
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 2024-07-29   2037702    lisa.singh   Initial creation (from auto-generated)
 *
 * </pre>
 */


public class NcUairCamelRoutes extends EDEXRouteBuilder {

    public NcUairCamelRoutes() {
    }

    @Override
    public void configure() throws Exception {
        from("jms-durable:queue:Ingest.ncuair")
        // Begin ncUair routes 
        // Chin, decodes and persist one batch of records at one time for better performance
          .setHeader("pluginName", constant("ncuair"))
              .bean("stringToFile")
              .split(method("ncuairSeparator", "batchSeparate")).streaming()
              .doTry()
                  .pipeline()
                      .bean("ncuairDecoder", "decodeBatch")
                      .bean("ncuairToPointData", "toPointData")
                      .to("direct:persistIndexAlert")
              .endDoTry()
              .doCatch(Throwable.class)
                  .to("log:ncuair?level=ERROR")
              .endDoTry()
          .end()
          .end()
          .setId("ncuairIngestRoute");
    }
}
