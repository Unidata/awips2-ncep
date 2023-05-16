package gov.noaa.nws.ncep.standalone.vgfConverter;

import java.util.List;

import com.sun.jna.Library;
import com.sun.jna.Structure;

/**
 * WrapperC
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * 1/25/2009   203         Q. Zhou     Initial created
 * 12/12/2011  548         Q. Zhou     Added activities and contour table file argument.
 * 03/15       R6872       J. Wu       Add status/forecaster/center in vgf2xml conversion.
 * </pre>
 * 
 * @author Q. Zhou
 * @version 1
 */

public class WrapperC {

    public interface VgfXml extends Library {

        public class VG_HdrStruct extends Structure { // implements
                                                      // Structure.ByValue {
                                                      // //.ByReference
            public char delete;

            public char vg_type;

            public char vg_class;

            public char filled;

            public char closed;

            public char smooth;

            public char version;

            public char grptyp;

            public int grpnum;

            public int maj_col;

            public int min_col;

            public int recsz;

            public float range_min_lat;

            public float range_min_lon;

            public float range_max_lat;

            public float range_max_lon;

            @Override
            protected List<String> getFieldOrder() {
                // TODO Auto-generated method stub
                return null;
            }
        }

        public class LineInfo extends Structure {
            public int numpts;

            public int lintyp;

            public int lthw;

            public int width;

            public int lwhw;

            @Override
            protected List<String> getFieldOrder() {
                // TODO Auto-generated method stub
                return null;
            }
        }

        public class SpLineInfo extends Structure {
            public int numpts;

            public int spltyp;

            public int splstr;

            public int spldir;

            public int splsiz;

            public int splwid;

            @Override
            protected List<String> getFieldOrder() {
                // TODO Auto-generated method stub
                return null;
            }
        }

        public static class LineType extends Structure {
            LineInfo info;

            public float latlon[];

            @Override
            protected List<String> getFieldOrder() {
                // TODO Auto-generated method stub
                return null;
            }
        }

        public static class SpLineType extends Structure {
            SpLineInfo info;

            public float latlon[];

            @Override
            protected List<String> getFieldOrder() {
                // TODO Auto-generated method stub
                return null;
            }
        }

        /*
         * public static class elem extends Union { public LineType lin; public
         * SpLineType spl; } /*public static class fileheadtype extends
         * Structure { public String version; public String notes; }
         */

        public static class FileHeadType extends Structure {

            @Override
            protected List<String> getFieldOrder() {
                // TODO Auto-generated method stub
                return null;
            }
        }

        public static class FrontType extends Structure {

            @Override
            protected List<String> getFieldOrder() {
                // TODO Auto-generated method stub
                return null;
            }
        }

        public static class WatchBoxType extends Structure {

            @Override
            protected List<String> getFieldOrder() {
                // TODO Auto-generated method stub
                return null;
            }
        }

        public static class WatchSMType extends Structure {

            @Override
            protected List<String> getFieldOrder() {
                // TODO Auto-generated method stub
                return null;
            }
        }

        public static class SymType extends Structure {

            @Override
            protected List<String> getFieldOrder() {
                // TODO Auto-generated method stub
                return null;
            }
        }

        public static class WindType extends Structure {

            @Override
            protected List<String> getFieldOrder() {
                // TODO Auto-generated method stub
                return null;
            }
        }

        public static class TextType extends Structure {

            @Override
            protected List<String> getFieldOrder() {
                // TODO Auto-generated method stub
                return null;
            }
        }

        public static class SptxType extends Structure {

            @Override
            protected List<String> getFieldOrder() {
                // TODO Auto-generated method stub
                return null;
            }
        }

        public static class CircType extends Structure {

            @Override
            protected List<String> getFieldOrder() {
                // TODO Auto-generated method stub
                return null;
            }
        }

        public static class TrackType extends Structure {

            @Override
            protected List<String> getFieldOrder() {
                // TODO Auto-generated method stub
                return null;
            }
        }

        public static class SigmetType extends Structure {

            @Override
            protected List<String> getFieldOrder() {
                // TODO Auto-generated method stub
                return null;
            }
        }

        public static class CCFType extends Structure {

            @Override
            protected List<String> getFieldOrder() {
                // TODO Auto-generated method stub
                return null;
            }
        }

        public static class ListType extends Structure {

            @Override
            protected List<String> getFieldOrder() {
                // TODO Auto-generated method stub
                return null;
            }
        }

        public static class AshType extends Structure {

            @Override
            protected List<String> getFieldOrder() {
                // TODO Auto-generated method stub
                return null;
            }
        }

        public static class VolType extends Structure {

            @Override
            protected List<String> getFieldOrder() {
                // TODO Auto-generated method stub
                return null;
            }
        }

        public static class JetType extends Structure {

            @Override
            protected List<String> getFieldOrder() {
                // TODO Auto-generated method stub
                return null;
            }
        }

        public static class GfaType extends Structure {

            @Override
            protected List<String> getFieldOrder() {
                // TODO Auto-generated method stub
                return null;
            }
        }

        public static class TcaType extends Structure {

            @Override
            protected List<String> getFieldOrder() {
                // TODO Auto-generated method stub
                return null;
            }
        }

        public static class TcerrType extends Structure {

            @Override
            protected List<String> getFieldOrder() {
                // TODO Auto-generated method stub
                return null;
            }
        }

        public static class TctrkType extends Structure {

            @Override
            protected List<String> getFieldOrder() {
                // TODO Auto-generated method stub
                return null;
            }
        }

        public static class TcbklType extends Structure {

            @Override
            protected List<String> getFieldOrder() {
                // TODO Auto-generated method stub
                return null;
            }
        }

        public class Elem extends Structure {

            public FileHeadType fhed;

            public FrontType frt;

            public LineType lin;

            public SpLineType spl;

            public WatchBoxType wbx;

            public WatchSMType wsm;

            public SymType sym;

            public WindType wnd;

            public TextType txt;

            public SptxType spt;

            public CircType cir;

            public TrackType trk;

            public SigmetType sig;

            public CCFType ccf;

            public ListType lst;

            public AshType ash;

            public VolType vol;

            public JetType jet;

            public GfaType gfa;

            public TcaType tca;

            public TcerrType tce;

            public TctrkType tct;

            public TcbklType tcb;

            @Override
            protected List<String> getFieldOrder() {
                // TODO Auto-generated method stub
                return null;
            }
        }

        public class VG_DBStruct extends Structure {
            public VG_HdrStruct hdr;

            public Elem elem; // el not find resource

            @Override
            protected List<String> getFieldOrder() {
                // TODO Auto-generated method stub
                return null;
            }

        }

        int vgfToXml(String infile, String outfile, String activity,
                String subActivity, String site, String forecaster,
                String status, String contTbl);

        int compareVgf(String infile, String outfile);

        // public int getDbStruct(VG_DBStruct el);
        void printf(String format, Object... args);
    }

    /*
     * public static void main(String[] args) { // test cWrapper VgfXml wrap =
     * (VgfXml)Native.loadLibrary( "VgfXml", VgfXml.class); VgfXml.VG_HdrStruct
     * hdr = new VgfXml.VG_HdrStruct(); VgfXml.LineType lin = new
     * VgfXml.LineType(); VgfXml.LineInfo info = new VgfXml.LineInfo();
     * VgfXml.Elem elem = new VgfXml.Elem(); VgfXml.VG_DBStruct el = new
     * VgfXml.VG_DBStruct();
     * 
     * int i= el.hdr.vg_type; int j = el.elem.lin.info.numpts; //el.elem wrong
     * System.out.print(i); //0
     * 
     * //System.out.print(j); String infile =
     * "/export/cdbsrv/qzhou/work/data1/line.vgf"; String outfile =
     * "/export/cdbsrv/qzhou/work/data1/line2x.xml"; wrap.vgfToXml( infile,
     * outfile); }
     */

}
