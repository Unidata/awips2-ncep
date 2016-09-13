/*
 * gov.noaa.nws.ncep.standalone.clipvgf.ClipVGF
 * 
 * Date created (as Jan 12, 2010)
 *
 * This code has been developed by the SIB for use in the AWIPS2 system.
 */
package gov.noaa.nws.ncep.standalone.clipvgf;

import gov.noaa.nws.ncep.standalone.joinvgf.JoinVGF;
import gov.noaa.nws.ncep.standalone.util.Util;
import gov.noaa.nws.ncep.ui.pgen.clipper.ClipProduct;
import gov.noaa.nws.ncep.ui.pgen.elements.Product;
import gov.noaa.nws.ncep.ui.pgen.file.ProductConverter;
import gov.noaa.nws.ncep.ui.pgen.file.Products;
import gov.noaa.nws.ncep.viz.common.dbQuery.NcDirectDbQuery;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.raytheon.uf.viz.core.catalog.DirectDbQuery.QueryLanguage;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.exception.VizServerSideException;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import com.vividsolutions.jts.io.WKBReader;

/**
 * This is a standalone clipping application, replacement for clipvgf.
 * <pre>
 * SOFTWARE HISTORY
 * Date       	Ticket#		Engineer	Description
 * 06/11		213			Q. Zhou		Modified convertArcToLine() for ellipse
 * 										Due to refactoring, separate multipointLine to Line, Gfa, Circle...
 * 										Added code to change arc line to arc
 * </pre>
 * @author mlaryukhin
 */
public class ClipVGF extends Thread {

	/**
	 * Input parameter, defined by user; true to clip all the elements within the bounds, false -
	 * outside.
	 */
	public static boolean	keep			= true;

	/** Input parameters. */
	public String[]			args;

	/**
	 * The table alias to be looked up in CONFIG.CLO table, which corresponds to the table name in
	 * bounds schema.
	 */
	public String			boundsTableAlias;

	/** The columnd name which is extracted from arg1[1]. */
	private String			columnName;

	/** The column value. To search for KZNY_8 it is sufficient to use KZNY. */
	private String			columnValue;

	/** EDEX http server location. */
	public static String	httpServer;

	/** Database name, currently defaulted to "ncep". */
	public String			database		= "ncep";

	/** Schema name, currently defaulted to "bounds". */
	public String			schema			= "bounds";

	/** Help file where the program description is stored. */
	public static String	HELP_FILE_NAME	= "clipvgf.hlp";

	/** Input file name. */
	private String			inputName;

	/** Output file name to store results. */
	private String			outputName;

	/** Factory */
	private GeometryFactory	geometryFactory	= new GeometryFactory();

	/** Bounds shape is to be converted to this polygon. */
	private Polygon			polygon;

	/** An instance of JoinVGF to be used for joining touching parts. */
	private JoinVGF			joinvgf;
	
	static{
		// no logger needed in apache classes
		Logger.getRootLogger().setLevel(Level.OFF);
	}

	/**
	 * Constructor.
	 * 
	 * @param args
	 *            <code>String[]</code> input parameters
	 * @throws VizException
	 */
	public ClipVGF(String[] args) throws VizException {
		this.args = args;
		checkParameters();
		createPolygon();
	}

	/**
	 * Open a product file to replace or append to the current product list.
	 * 
	 * @throws FileNotFoundException
	 */
	private List<Product> openProducts() throws FileNotFoundException {
		Products products = Util.read(inputName);
		return ProductConverter.convert(products);
	}

	/**
	 * Save the products to file.
	 */
	private void saveProducts(List<Product> productList) {
		Products fileProducts = ProductConverter.convert(productList);
		if (outputName != null) {
			/*
			 * Write all products into one file.
			 */
			try {
				Util.write(outputName, fileProducts, Products.class);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Static method to clip the xml file.
	 * 
	 * @param productList
	 * 
	 * @return
	 * 
	 * @throws FileNotFoundException
	 * 
	 * @throws VizException
	 *             if database fails
	 */
	private void clip(String inputName, String outputName) throws FileNotFoundException, VizException {
		// open
		List<Product> productList = openProducts();

		// clip each Product
		for (Product p : productList) {
			new ClipProduct(polygon, keep).clipProduct(p);
		}
		// save
		saveProducts(productList);
	}

	
	/**
	 * Getter for JoinVGF instance.
	 * 
	 * @return
	 */
	private JoinVGF getJoinVGF() {
		if (joinvgf == null) {
			joinvgf = new JoinVGF(null);
		}
		return joinvgf;
	}

	/**
	 * Creates a bound polygon.
	 * 
	 * @throws VizException
	 */
	private void createPolygon() throws VizException {

		String sql = "select t.table_name from config.clo t where t.alias_name = upper('" + boundsTableAlias + "')";

		List<Object[]> firstResult = performQuery(sql);
		String tableName = null; // comes from config.clo
		if(firstResult.isEmpty()){
			// no record, this means use it as a table name
			tableName = boundsTableAlias;
		} else {
			for (Object o : firstResult.get(0)) {
				tableName = (String) o;
			}
		}
		if (tableName == null) {
			throw new IllegalArgumentException(
					"Second argument invalid format: no such record exists in config.clo table or invalid table name");
		}

		sql = "SELECT AsBinary(t.the_geom) FROM " + schema + "." + tableName + " t" + " WHERE t."
				+ columnName + " like '" + columnValue + "%'";

		List<Object[]> secondResult = performQuery(sql);

		WKBReader wkbReader = new WKBReader();
		if (secondResult != null && !secondResult.isEmpty()) {
			Object[] bound = secondResult.get(0); // take the very first one
			try {
				Geometry g = wkbReader.read((byte[]) bound[0]);
				if(g instanceof MultiPolygon){
					MultiPolygon mg = (MultiPolygon) g;
					int max = 0;
					int index = 0;
					for (int i = 0; i < mg.getNumGeometries(); i++) {
						if (max < mg.getGeometryN(i).getNumPoints()){
							max = mg.getGeometryN(i).getNumPoints();
							index = i;
						}
					}
					polygon = (Polygon) mg.getGeometryN(index);
					return;
				}
				CoordinateSequence sequence = new CoordinateArraySequence(g.getCoordinates());
				LinearRing ring = new LinearRing(sequence, geometryFactory);
				polygon = new Polygon(ring, null, geometryFactory);
			} catch (Exception e) {
				// cannot do anything here, wrong data
				// the same as database problem
				throw new VizException(e);
			}
		} else {
			throw new RuntimeException("No bounds found, please check parameters");
		}
	}

	/**
	 * This method is a workaround for non-Eclipse-plugin applications.
	 * 
	 * @param sql
	 * @return
	 * @throws VizException
	 * @throws VizServerSideException
	 */
	private List<Object[]> performQuery(String sql) throws VizException, VizServerSideException {
		
		NcDirectDbQuery.setHttpServer(httpServer);
		List<Object[]> res = NcDirectDbQuery.executeQuery(sql, database, QueryLanguage.SQL);

		return res;
	}

	
	/**
	 * Entry point.
	 * 
	 * @param args
	 */
	public void run() {
		long time = System.currentTimeMillis();
		System.out.println("\nClipping started: " + inputName);

		try {
			clip(args[0], args[3]);
		} catch (FileNotFoundException e) {
			throw new RuntimeException("Could not open " + args[0] + " file");
		} catch (VizException e) {
			throw new RuntimeException("ERROR: Database failed");
		}

		System.out.println("Clipping finished: " + (System.currentTimeMillis() - time) + "ms");
	}

	/**
	 * Checks parameters and throws IllegalArgumentException if these parameters do not pass
	 * validation.
	 * 
	 * @param args
	 * 
	 * @see ClipVGFDialog.getProgramDescription()
	 */
	private void checkParameters() {
		if (args.length < 4) {
			// ignore args[4], we always "exact", never "rough"
			throw new IllegalArgumentException("Not enough arguments");
		}

		// "STATEBNDS|<STATE>IL"
		String[] arg1 = args[1].split("\\|");
		if (arg1.length != 2 || !(arg1[1].indexOf("<") > -1)) {
			throw new IllegalArgumentException("Second argument invalid format");
		}
		boundsTableAlias = arg1[0];
		// extract <STATE> and sate as column name
		columnName = arg1[1].substring(arg1[1].indexOf("<") + 1, arg1[1].indexOf(">"));
		columnValue = arg1[1].substring(arg1[1].indexOf(">") + 1, arg1[1].length());

		if (args.length > 5) {
			httpServer = args[5];
			// VizApp.setHttpServer(httpServer);
		}

		if (boundsTableAlias.isEmpty() || columnValue.isEmpty()) {
			throw new IllegalArgumentException("boundsTable or firId are not set up correctly");
		} else if (httpServer == null) {
			throw new IllegalArgumentException("EDEX http server is not set up correctly");
		}

		inputName = args[0];
		outputName = args[3];

		if ("notkeep".equalsIgnoreCase(args[2])) {
			// explicitly leave everything what is outside of the polygon only
			// if "notkeep" is specified. everything is interpreted as "keep"
			ClipVGF.keep = false;
		} else {
			ClipVGF.keep = true;
		}
	}

	/**
	 * Description.
	 * 
	 * @return
	 */
	public static String getProgramDescription() {
		try {
			return Util.getContent(HELP_FILE_NAME).toString();
		} catch (IOException e) {
			return "";
		}
	}

	/**
	 * Entry point for command line. (Not implemented yet)
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length == 0 || "--help".equals(args[0]) || "/h".equalsIgnoreCase(args[0])) {
			System.out.println(getProgramDescription());
			return;
		} // else proceed

		try {
			ClipVGF thread = new ClipVGF(args);
			thread.start();
		} catch (VizException e) {
			System.err.println("Database error");
			e.printStackTrace();
		}
	}
}
