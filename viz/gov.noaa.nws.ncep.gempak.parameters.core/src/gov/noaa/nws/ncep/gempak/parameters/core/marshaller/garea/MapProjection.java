package gov.noaa.nws.ncep.gempak.parameters.core.marshaller.garea;

import org.geotools.referencing.CRS;
import org.geotools.referencing.operation.projection.EquidistantCylindrical;
import org.geotools.referencing.operation.projection.LambertConformal1SP;
import org.geotools.referencing.operation.projection.LambertConformal2SP;
import org.geotools.referencing.operation.projection.MapProjection.AbstractProvider;
import org.geotools.referencing.operation.projection.Mercator;
import org.geotools.referencing.operation.projection.PolarStereographic;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * <pre>
 *
 *  
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer     Description
 * ------------ ---------- ----------- --------------------------
 * 23-Sep-2009    171       Archana     Initial Creation
 * 20-Mar-2017   R19634     bsteffen    Added convertToGempakString()
 * 01-Feb-2019    7720      mrichardson Incorporated changes for convertToGempakString()
 * </pre>
 * 
 * @author Archana
 * @version 1
 */
public class MapProjection {

    /**
     * @param args
     */
    private String map_projection_str, projection_class;
    private Float angle1, angle2, angle3;
    private Integer l_margin, r_margin, t_margin, b_margin;
    private boolean map_projection_str_valid;

    private enum Simple_map_projection {
        MER, NPS, SPS, LCC, SCC, CED, MCD, NOR, SOR
    }

    private enum CYL {
        MER, CED, MCD
    }

    private enum AZM {
        STR, AED, ORT, LEA, GNO
    }

    private enum CON {
        LCC, SCC
    }
    // private enum Graph_projections{LIN,LOG,KAP,POL}

    /**
     * The overloaded constructor accepts as input - a string 's' containing the
     * projection data. It initializes the 3 angles and the 4 margins to invalid
     * values. It then invokes a method called setProjectionString() with the
     * input string and uses the boolean value returned by this method to set
     * the flag 'map_projection_str_valid'.
     */
    public MapProjection(String s) {
        angle1 = -500.0f;
        angle2 = -500.0f;
        angle3 = -500.0f;
        l_margin = -1000;
        r_margin = -1000;
        t_margin = -1000;
        b_margin = -1000;
        map_projection_str_valid = parseMapProjectionString(s);

    }

    /**
     * The method isProjectionStringValid() returns the value of the boolean
     * flag 'map_projection_str_valid'. This method should be invoked before
     * using the data 'map_projection_str' or 'projection_class'.
     */
    public boolean isProjectionStringValid() {

        return map_projection_str_valid;
    }

    private boolean parseMapProjectionString(String s) {

        String[] projection_string_tokens, angle_tokens, margin_tokens;
        projection_string_tokens = s.split("/");
        boolean angle_flag;
        boolean margin_flag;
        // boolean str_flag = setProjectionString(projection_string_tokens[0]);
        setProjectionString(projection_string_tokens[0]);

        if (projection_string_tokens.length == 2) {
            angle_flag = this.setProjectionAngles(projection_string_tokens[1], ";");
        }
        if (projection_string_tokens.length == 3) {
            margin_flag = this.setProjectionMargins(projection_string_tokens[2], ";");

        }

        if (projection_string_tokens.length == 3) {
            this.map_projection_str = projection_string_tokens[0];
            if (map_projection_str.length() == 3) {

                for (CYL cyl : CYL.values()) {
                    if (map_projection_str.equals(cyl.toString())) {
                        this.projection_class = "CYL";
                        this.map_projection_str = projection_string_tokens[0];
                        break;
                        // flag = true;
                    }
                }

                for (AZM azm : AZM.values()) {
                    if (map_projection_str.equals(azm.toString())) {
                        this.projection_class = "AZM";
                        this.map_projection_str = projection_string_tokens[0];
                        break;
                        // flag = true;
                    }
                }
                for (CON con : CON.values()) {
                    if (map_projection_str.equals(con.toString())) {
                        this.projection_class = "CON";
                        this.map_projection_str = projection_string_tokens[0];
                        break;
                        // flag = true;
                    }
                }

                if (projection_string_tokens[1].length() > 0) {
                    angle_tokens = projection_string_tokens[1].split(";");
                    if (angle_tokens.length > 0) {
                        if (angle_tokens.length < 3) {
                            // TODO Send a message to indicate that there are
                            // less than 3 angles.
                            // flag = false;
                        } else if (angle_tokens.length > 3) {
                            // TODO Send a message to indicate that there are
                            // more than 3 angles.
                            // flag = false;
                        } else {
                            angle1 = Float.valueOf(angle_tokens[0]);
                            angle2 = Float.valueOf(angle_tokens[1]);
                            angle3 = Float.valueOf(angle_tokens[2]);
                        }

                    } else {
                        // flag = false;
                    }

                } else {

                    // TODO Send a message to indicate that there are NO angles.
                    // flag = false;
                }
                if (projection_string_tokens[2].length() != 0) {

                    margin_tokens = projection_string_tokens[2].split(";");
                    // if((margin_tokens.length != 0) && (margin_tokens.length
                    // == 4)){
                    if (margin_tokens.length == 4) {
                        l_margin = Integer.valueOf(margin_tokens[0]);
                        b_margin = Integer.valueOf(margin_tokens[1]);
                        r_margin = Integer.valueOf(margin_tokens[2]);
                        t_margin = Integer.valueOf(margin_tokens[3]);
                    } else {
                        // If no margins are specified (using the code NM), the
                        // 4 margin values are set to 0.
                        if ("NM".equals(projection_string_tokens[2])) {
                            this.setProjectionMargins("0, 0, 0, 0", ",");
                        }
                    }

                } else {
                    // flag = false;
                }

            }
        } else {
            // flag = false;
        }

        return true;
    }

    private boolean setProjectionString(String s) {

        boolean proj_str_set = false;

        // TODO If the projection string is blank,
        // check if the GAREA coordinates have a pre-defined projection in the
        // geog table:

        if (s.length() == 0) {
            this.map_projection_str = s;
            proj_str_set = true;
        }

        // TODO If projection string is DEF,
        // check if the GAREA coordinates have a pre-defined projection in the
        // geog table
        // else use current map projection

        if ("DEF".equals(s)) {
            this.map_projection_str = s;
            proj_str_set = true;
        }

        // TO DO May have to add code for the image drop flag
        /*
         * else if(s.equals("SAT")||s.equals("RAD")){ this.map_projection_str
         * =s; proj_str_set = true; }
         */

        else {
            // Assign default values for the angles and the margins, in the case
            // of a Simple Map Projection
            if ((s.length() == 3)) {
                for (Simple_map_projection smp : Simple_map_projection.values()) {
                    if (s.equals(smp.toString())) {

                        // The three angles are initialized to random values.
                        this.setProjectionAngles("0, 0, 0", ",");

                        // TODO The default value of the margins is 0,3,0,0 in
                        // the map mode but it is 6,4,4,1 in the graph mode
                        // May have to add logic to set the default margins in
                        // the graph mode.

                        this.setProjectionMargins("0;3;0;0", ";");
                        this.map_projection_str = s;
                        proj_str_set = true;
                        break;
                    }

                }

                // TODO Find out what needs to be done for Graph Projections,
                // once the design decisions are resolved.

                /*
                 * for(Graph_projections gp: Graph_projections.values()){
                 * if(s.equals(gp.toString())){
                 * 
                 * this.map_projection_str =s; proj_str_set = true; }
                 * 
                 * }
                 */
            } else if (s.length() < 3 && s.length() > 0) {
                proj_str_set = false;
            } else {
                proj_str_set = parseMapProjectionString(s);
            }
        }
        return proj_str_set;
    }

    /**
     * The method getProjectionString() returns a String object containing the
     * value of the projection string.
     */

    public String getProjectionString() {
        return this.map_projection_str;
    }

    /**
     * The method getProjectionClass() returns a String object containing the
     * value of the projection class.
     */

    public String getProjectionClass() {
        return this.projection_class;
    }

    /**
     * The method setProjectionMargins() accepts 4 integer values to set the
     * margin values in the following order- left, bottom, right, top.
     */
    public boolean setProjectionMargins(String s, String parseString) {

        boolean margin_flag = false;
        if (!"NM".equals(s) && !"N".equals(s)) {
            String margin_tokens[] = s.split(parseString);
            int margin;
            if (margin_tokens[0].length() > 0) {
                margin = Integer.valueOf(margin_tokens[0]).intValue();
                if (margin >= 0) {
                    this.l_margin = margin;
                }
            }
            if (margin_tokens[1].length() > 0) {
                margin = Integer.valueOf(margin_tokens[1].trim()).intValue();
                if (margin >= 0) {
                    this.b_margin = margin;
                }
            }
            if (margin_tokens[2].length() > 0) {
                margin = Integer.valueOf(margin_tokens[2].trim()).intValue();
                if (margin >= 0) {
                    this.r_margin = margin;
                }
            }
            if (margin_tokens[3].length() > 0) {
                margin = Integer.valueOf(margin_tokens[3].trim()).intValue();
                if (margin >= 0) {
                    this.t_margin = margin;
                }
            }
            if (this.l_margin != -1000 && this.b_margin != -1000 && this.r_margin != -1000 && this.t_margin != -1000) {
                margin_flag = true;
            }
        } else {
            this.l_margin = 0;
            this.b_margin = 0;
            this.r_margin = 0;
            this.t_margin = 0;
            margin_flag = true;
        }

        return margin_flag;
    }

    /**
     * The method getProjectionMargins() returns an array of Integer objects
     * containing the values of the margins in the following order: left,
     * bottom, right, top.
     */

    public Integer[] getProjectionMargins() {
        Integer[] margins = new Integer[4];
        margins[0] = this.l_margin;
        margins[1] = this.b_margin;
        margins[2] = this.r_margin;
        margins[3] = this.t_margin;
        return margins;

    }

    /**
     * The method setProjectionAngles() sets the values of the 3 angles used in
     * the projection.
     */
    private boolean setProjectionAngles(String s, String parseString) {
        // String angle_tokens[] = s.split(";");
        String angle_tokens[] = s.split(parseString);
        float angle_val;
        boolean angle_set = false;
        if (angle_tokens.length > 0) {
            if (angle_tokens[0].length() > 0) {
                angle_val = Float.valueOf(angle_tokens[0]).floatValue();
                if (angle_val >= -90.0 && angle_val <= 90.0) {
                    this.angle1 = angle_val;
                }
            }

            if (angle_tokens[1].length() > 0) {
                angle_val = Float.valueOf(angle_tokens[1]).floatValue();
                if (angle_val >= -180.0 && angle_val <= 360.0) {
                    this.angle2 = angle_val;
                }
            }

            if (angle_tokens[2].length() > 0) {
                angle_val = Float.valueOf(angle_tokens[2]).floatValue();
                if (angle_val >= 0.0 && angle_val <= 360.0) {
                    this.angle3 = angle_val;
                }
            }

            if (this.angle1 != -500.0 && this.angle2 != -500.0 && this.angle3 != -500.0) {
                angle_set = true;
            }

        }

        return angle_set;
    }

    /**
     * The method getProjectionAngles() returns an array containing the values
     * of the 3 angles used in the projection.
     */
    public Float[] getProjectionAngles() {
        Float[] angles = new Float[3];
        angles[0] = this.angle1;
        angles[1] = this.angle2;
        angles[2] = this.angle3;
        return angles;
    }

    /**
     * Convert a geotools CRS into a gempak proj string. Documentation on the
     * format of a gempak proj string is available at
     * https://www.unidata.ucar.edu/software/gempak/man/parm/proj.html.
     * 
     * In general the format is [proj id]/[angle1];[angle2];[angle3], the
     * meaning of the various angles depends on the type of projection.
     * 
     */
    public static String convertToGempakString(CoordinateReferenceSystem crs) {
        String identifier = "CED";
        /*
         * The three angle values have different meanings depending on the type
         * of map projection.
         */
        double angle1 = 0.0;
        double angle2 = 0.0;
        double angle3 = 0.0;
        org.geotools.referencing.operation.projection.MapProjection projection = CRS.getMapProjection(crs);
        if (projection == null) {
            return null;
        }
        ParameterValueGroup values = projection.getParameterValues();

        if (projection instanceof EquidistantCylindrical) {
            identifier = "CED";
            angle1 = values.parameter(AbstractProvider.LATITUDE_OF_ORIGIN.getName().getCode()).doubleValue();
            angle2 = values.parameter(AbstractProvider.CENTRAL_MERIDIAN.getName().getCode()).doubleValue();
        } else if (projection instanceof Mercator) {
            identifier = "MER";
            angle1 = values.parameter(AbstractProvider.LATITUDE_OF_ORIGIN.getName().getCode()).doubleValue();
            angle2 = values.parameter(AbstractProvider.CENTRAL_MERIDIAN.getName().getCode()).doubleValue();
        } else if (projection instanceof LambertConformal1SP) {
            identifier = "LCC";
            angle1 = values.parameter(AbstractProvider.LATITUDE_OF_ORIGIN.getName().getCode()).doubleValue();
            angle2 = values.parameter(AbstractProvider.CENTRAL_MERIDIAN.getName().getCode()).doubleValue();
            angle3 = angle1;
        } else if (projection instanceof LambertConformal2SP) {
            identifier = "LCC";
            angle1 = values.parameter(AbstractProvider.STANDARD_PARALLEL_1.getName().getCode()).doubleValue();
            angle2 = values.parameter(AbstractProvider.CENTRAL_MERIDIAN.getName().getCode()).doubleValue();
            angle3 = values.parameter(AbstractProvider.STANDARD_PARALLEL_2.getName().getCode()).doubleValue();
        } else if (projection instanceof PolarStereographic) {
            identifier = "STR";
            angle1 = values.parameter(AbstractProvider.LATITUDE_OF_ORIGIN.getName().getCode()).doubleValue();
            angle2 = values.parameter(AbstractProvider.CENTRAL_MERIDIAN.getName().getCode()).doubleValue();
        } else {
            return null;
        }

        return String.format("%s/%.1f;%.1f;%.1f", identifier, angle1, angle2, angle3);
    }
}
