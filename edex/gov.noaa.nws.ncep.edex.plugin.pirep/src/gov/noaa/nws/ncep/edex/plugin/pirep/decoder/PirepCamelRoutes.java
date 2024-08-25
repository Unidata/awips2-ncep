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

package gov.noaa.nws.ncep.edex.plugin.pirep.decoder;

import com.raytheon.uf.edex.routes.EDEXRouteBuilder;

/**
 * Camel routes converted from file "pirep-ingest.xml", context "pirep-camel"
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


public class PirepCamelRoutes extends EDEXRouteBuilder {

    public PirepCamelRoutes() {
    }

    @Override
    public void configure() throws Exception {
        from("jms-durable:queue:Ingest.pirep")
          .setHeader("pluginName", constant("pirep"))
              .doTry()
                  .pipeline()
                  .bean("stringToFile")
                  .split(method("pirepSeparator", "separate")).streaming()
                      .doTry()
                          .pipeline()
                              .bean("pirepDecoder", "decode")
                              // Add next line for h5 version and change next
                              // line from indexAlert to persistIndexAlert"
                              .bean("pirepPointData", "toPointData")
                              .to("direct:persistIndexAlert")
                      .endDoTry()
                      .doCatch(Throwable.class)
                          .to("log:pirep?level=ERROR")
                      .endDoTry()
              .end()
          .end()
          .endDoTry()
          .doCatch(Throwable.class)
              .to("log:pirep?level=ERROR")
          .endDoTry()
          .end()
          .setId("pirepIngestRoute");
    }
}
