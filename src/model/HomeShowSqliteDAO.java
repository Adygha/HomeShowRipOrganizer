package model;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A class that represents a data access object for the HomeShow-Organizer database.
 * @author Janty Azmat
 */
public class HomeShowSqliteDAO implements AutoCloseable {

	/**
	 * An enumeration that represents the role a personnel can have in a show
	 * @author Janty Azmat
	 */
	public static enum ModelPersRole {

		/**
		 * When the personnel has no role (not accepted by database).
		 */
		NONE(""),

		/**
		 * When the personnel is an actor.
		 */
		ACTOR("Actor"),

		/**
		 * When the personnel is a director.
		 */
		DIRECTOR("Director"),

		/**
		 * When the personnel is an author.
		 */
		AUTHOR("Author");

		public static final EnumSet<ModelPersRole> ALL_BUT_NULL = EnumSet.complementOf(EnumSet.of(ModelPersRole.NONE));
		private final String meName;

		ModelPersRole(String theName) {
			this.meName = theName;
		}

		@Override
		public String toString() {
			return this.meName;
		}

		public static ModelPersRole getValueOf(String constName) {
			return ModelPersRole.valueOf(constName.toUpperCase());
		}
	}

	// Fields
	private static final String me_CONN_STR = "jdbc:sqlite:homeshow.db"; // The DBMS connection string
	private static final String me_PERSONNEL_TABLE = "CREATE TABLE IF NOT EXISTS personnel (first_name TEXT NOT NULL CHECK (first_name <> ''), "						//
			+ "last_name TEXT NOT NULL CHECK (last_name <> ''), PRIMARY KEY (first_name, last_name));";																	//
	private static final String me_SHOW_TABLE = "CREATE TABLE IF NOT EXISTS show (title TEXT NOT NULL CHECK (title <> ''), "											//
			+ "year INTEGER NOT NULL CHECK (year BETWEEN 1800 AND 2100), duration INTEGER NOT NULL CHECK (duration BETWEEN 0 AND 4000000) DEFAULT 0, "					//
			+ "folder TEXT NOT NULL DEFAULT '', is_movie BOOLEAN NOT NULL CHECK (is_movie IN (0, 1)), PRIMARY KEY (title, year));";										//
	private static final String me_GENRE_TABLE = "CREATE TABLE IF NOT EXISTS genre (name TEXT PRIMARY KEY CHECK (name <> ''));";										//
	private static final String me_PARTICIPATES_TABLE = "CREATE TABLE IF NOT EXISTS participates (personnel_fname TEXT NOT NULL, "										// Table creation statements
			+ "personnel_lname TEXT NOT NULL, show_title TEXT NOT NULL, show_year INTEGER NOT NULL, role TEXT NOT NULL CHECK (role IN ("								//
			+ ModelPersRole.ALL_BUT_NULL.toString().replaceAll("[\\[\\]]","'").replaceAll(", ","', '")																	//
			+ ")), FOREIGN KEY (personnel_fname, personnel_lname) REFERENCES personnel (first_name, last_name) ON DELETE CASCADE ON UPDATE CASCADE, "					//
			+ "FOREIGN KEY (show_title, show_year) REFERENCES show (title, year) ON DELETE CASCADE ON UPDATE CASCADE, "													//
			+ "PRIMARY KEY (personnel_fname, personnel_lname, show_title, show_year, role));";																			//
	private static final String me_HASGENRE_TABLE = "CREATE TABLE IF NOT EXISTS hasgenre (show_title TEXT NOT NULL, show_year INTEGER NOT NULL, "						//
			+ "genre_name TEXT NOT NULL REFERENCES genre (name) ON DELETE CASCADE ON UPDATE CASCADE, "																	//
			+ "FOREIGN KEY (show_title, show_year) REFERENCES show (title, year) ON DELETE CASCADE ON UPDATE CASCADE, PRIMARY KEY(show_title, show_year, genre_name));";//
	private static final String me_PERSONNEL_INS = "INSERT INTO personnel (first_name, last_name)\nVALUES (?, ?);";																//
	private static final String me_SHOW_INS = "INSERT INTO show (title, year, duration, folder, is_movie)\nVALUES (?, ?, ?, ?, ?);";											//
	private static final String me_GENRE_INS = "INSERT INTO genre (name)\nVALUES (?);";																							// Data insertion prepared statements
	private static final String me_PARTICIPATES_INS = "INSERT INTO participates (personnel_fname, personnel_lname, show_title, show_year, role)\nVALUES (?, ?, ?, ?, ?);";		//
	private static final String me_HASGENRE_INS = "INSERT INTO hasgenre (show_title, show_year, genre_name)\nVALUES (?, ?, ?);";												//
	private static final String me_QSEARCH_PERSONEL = "SELECT personnel.*, IFNULL (par_count, 0) AS show_count, role, show.* "											//
			+ "FROM personnel\nLEFT JOIN participates ON first_name=participates.personnel_fname AND last_name=participates.personnel_lname\n"							//
			+ "LEFT JOIN show ON show_title=title AND show_year=year\nLEFT JOIN (\n\tSELECT personnel_fname, personnel_lname, COUNT (*) AS par_count "					//
			+ "FROM participates\n\tWHERE personnel_fname LIKE ? AND personnel_lname LIKE ? AND IFNULL (role, 'N/A') LIKE ?\n\t"										//
			+ "GROUP BY personnel_fname, personnel_lname\n) AS count_table ON first_name=count_table.personnel_fname AND last_name=count_table.personnel_lname\n"		//
			+ "WHERE first_name LIKE ? AND last_name LIKE ? AND IFNULL (role, 'N/A') LIKE ?\nORDER BY first_name, last_name, title, year;";								// Prepared statements used in the
	private static final String me_QSEARCH_SHOW = "SELECT show.*, IFNULL (par_count, 0) AS personnel_count, personnel.*, role "											// quick search page of the app
			+ "FROM show\nLEFT JOIN participates ON participates.show_title=title AND participates.show_year=year\n"													//
			+ "LEFT JOIN personnel ON first_name=personnel_fname AND last_name=personnel_lname\nLEFT JOIN (\n\tSELECT show_title, show_year, COUNT (*) AS par_count "	//
			+ "FROM participates\n\tWHERE show_title LIKE ? AND IFNULL(NULLIF(?, show_year), -1)<0 AND IFNULL (role, 'N/A') LIKE ?\n\t"									//
			+ "GROUP BY show_title, show_year\n) AS count_table ON title=count_table.show_title AND year=count_table.show_year\n"										//
			+ "WHERE title LIKE ? AND IFNULL(NULLIF(?, year), -1)<0 AND IFNULL (role, 'N/A') LIKE ?\nORDER BY title, year, first_name, last_name;";						//
	private static final String me_SERACH_TABLE_NAME = "Search_Results";
	private Connection meConn; // The Connection
	private List<ModelDataRow> meTableTemplates; // Input row templates used in the insert page to insert data to tables
	private List<ModelDataRow> meQSearchTemplates; // Input row templates used in the quick search page to quick search data
	private Map<String, String> meJoinONs; // Will contain every table and its 'JOIN ON' with previous table
	private Set<String> meKeys; // Will contain all the keys (in the form 'table.column')

	/**
	 * Default constructor.
	 * @throws SQLException	throws 'SQLException' if a database access error/timeout occurs.
	 */
	public HomeShowSqliteDAO() throws SQLException {
		this.meConn = DriverManager.getConnection(me_CONN_STR); // Initiate a connection
		try (Statement tmpStat = this.meConn.createStatement()) {	// A temporary statement object to create the tables if not yet created.
			tmpStat.executeUpdate(me_PERSONNEL_TABLE);
			tmpStat.executeUpdate(me_SHOW_TABLE);
			tmpStat.executeUpdate(me_GENRE_TABLE);
			tmpStat.executeUpdate(me_PARTICIPATES_TABLE);
			tmpStat.executeUpdate(me_HASGENRE_TABLE);
			tmpStat.executeUpdate("PRAGMA foreign_keys = ON;"); // Enforce foreign keys
		}
		this.meJoinONs = new LinkedHashMap<>(); // To keep the insertion order
		this.meKeys = new LinkedHashSet<>();
		this.meTableTemplates = new LinkedList<>();
		this.meTableTemplates.add(new ModelDataRow("personnel", me_PERSONNEL_INS, Arrays.asList(new ModelDataCell[] { // prepare the personnel insert row
			new ModelDataCell<>("first_name", "personnel", "", String.class, Types.VARCHAR, true, true),
			new ModelDataCell<>("last_name", "personnel", "", String.class, Types.VARCHAR, true, true)
		})));
		this.meJoinONs.put("personnel", null);
		this.meKeys.add("personnel.first_name");
		this.meKeys.add("personnel.last_name");
		this.meTableTemplates.add(new ModelDataRow("participates", me_PARTICIPATES_INS, Arrays.asList(new ModelDataCell[] { // prepare the participates insert row
			new ModelDataCell<>("personnel_fname", "participates", "", String.class, Types.VARCHAR, true, true),
			new ModelDataCell<>("personnel_lname", "participates", "", String.class, Types.VARCHAR, true, true),
			new ModelDataCell<>("show_title", "participates", "", String.class, Types.VARCHAR, true, true),
			new ModelDataCell<>("show_year", "participates", -1, Integer.class, Types.INTEGER, true, true),
			new ModelDataCell<>("role", "participates", ModelPersRole.NONE, ModelPersRole.class, Types.VARCHAR, true, true)
		})));
		this.meJoinONs.put("participates", "participates.personnel_fname=personnel.first_name AND participates.personnel_lname=personnel.last_name");
		this.meKeys.add("participates.personnel_fname");
		this.meKeys.add("participates.personnel_lname");
		this.meKeys.add("participates.show_title");
		this.meKeys.add("participates.show_year");
		this.meKeys.add("participates.role");
		this.meTableTemplates.add(new ModelDataRow("show", me_SHOW_INS, Arrays.asList(new ModelDataCell[] { // prepare the show insert row
			new ModelDataCell<>("title", "show", "", String.class, Types.VARCHAR, true, true),
			new ModelDataCell<>("year", "show", -1, Integer.class, Types.INTEGER, true, true),
			new ModelDataCell<>("duration", "show", -1, Integer.class, Types.INTEGER, false, true),
			new ModelDataCell<>("folder", "show", "", String.class, Types.VARCHAR, false, true),
			new ModelDataCell<>("is_movie", "show", null, Boolean.class, Types.BOOLEAN, false, true)
		})));
		this.meJoinONs.put("show", "show.title=participates.show_title AND show.year=participates.show_year");
		this.meKeys.add("show.title");
		this.meKeys.add("show.year");
		this.meTableTemplates.add(new ModelDataRow("hasgenre", me_HASGENRE_INS, Arrays.asList(new ModelDataCell[] { // prepare the hasgenre insert row
			new ModelDataCell<>("show_title", "hasgenre", "", String.class, Types.VARCHAR, true, true),
			new ModelDataCell<>("show_year", "hasgenre", -1, Integer.class, Types.INTEGER, true, true),
			new ModelDataCell<>("genre_name", "hasgenre", "", String.class, Types.VARCHAR, true, true)
		})));
		this.meJoinONs.put("hasgenre", "hasgenre.show_title=show.title AND hasgenre.show_year=show.year");
		this.meKeys.add("hasgenre.show_title");
		this.meKeys.add("hasgenre.show_year");
		this.meKeys.add("hasgenre.genre_name");
		this.meTableTemplates.add(new ModelDataRow("genre", me_GENRE_INS, Arrays.asList(new ModelDataCell[] { // prepare the genre insert row
			new ModelDataCell<>("name", "genre", "", String.class, Types.VARCHAR, true, true)
		})));
		this.meJoinONs.put("genre", "genre.name=hasgenre.genre_name");
		this.meKeys.add("genre.name");
		this.meQSearchTemplates = new LinkedList<>();
		this.meQSearchTemplates.add(new ModelDataRow("All Shows By Personnel", me_QSEARCH_PERSONEL, Arrays.asList(new ModelDataCell[] { // prepare quick search row
			new ModelDataCell<>("first_name", "personnel", "", String.class, Types.VARCHAR, true, true),
			new ModelDataCell<>("last_name", "personnel", "", String.class, Types.VARCHAR, true, true),
			new ModelDataCell<>("role", "participates", ModelPersRole.NONE, ModelPersRole.class, Types.VARCHAR, true, false)
		})));
		this.meQSearchTemplates.add(new ModelDataRow("All Personnel By Shows", me_QSEARCH_SHOW, Arrays.asList(new ModelDataCell[] { // prepare quick search row
			new ModelDataCell<>("title", "show", "", String.class, Types.VARCHAR, true, true),
			new ModelDataCell<>("year", "show", -1, Integer.class, Types.INTEGER, true, true),
			new ModelDataCell<>("role", "participates", ModelPersRole.NONE, ModelPersRole.class, Types.VARCHAR, true, false)
		})));
	}

	@Override
	public void close() throws SQLException { // Required my the 'AutoClosable' interface
		if (!(this.meConn == null || this.meConn.isClosed())) { // For the connection
			this.meConn.close();
		}
	}

	/**
	 * Used to get the input row templates used in the insert page to insert data to tables.
	 * @return	a list of rows used in the insert page to insert data to tables
	 */
	public List<ModelDataRow> getTableTemplates() {
		return this.meTableTemplates;
	}

	/**
	 * Used to get the input row templates used in the quick search page to quick search data.
	 * @return	a list of rows used in the quick search page to quick search data
	 */
	public List<ModelDataRow> getQuickSearchTemplates() {
		return this.meQSearchTemplates;
	}

	/**
	 * Inserts the specified row data (that represents a table's data row) into database.
	 * @param theRow		the data row to be inserted
	 * @return				the actual statement used to insert data (the prepared statement filled with parameter data)
	 * @throws SQLException	thrown if a database access error/timeout occurs or this method is called on a closed connection
	 */
	public String insertRow(ModelDataRow theRow) throws SQLException {
		String outStr = theRow.getPreparedStatement();
		try (PreparedStatement tmpStat = this.meConn.prepareStatement(theRow.getPreparedStatement())) {
			for (int i = 0; i < theRow.getCells().size(); i++) { // Loop and set prepared statement parameters (each using its own type)
				tmpStat.setObject(i + 1, theRow.getCells().get(i).getValue(), theRow.getCells().get(i).getSqlType());
				outStr = outStr.replaceFirst("\\?", theRow.getCells().get(i).getSqlType() == Types.VARCHAR
						? "'" + theRow.getCells().get(i).getValue() + "'"
						: "" + theRow.getCells().get(i).getValue());
			}
			tmpStat.executeUpdate(); // Insert
		}
		return outStr;
	}

	/**
	 * Deletes the specified row data (better be only the key) from database.
	 * @param theRow		the row data to be deleted (better be only the key)
	 * @return				the actual statement used to insert data (the prepared statement filled with parameter data)
	 * @throws SQLException	thrown if a database access error/timeout occurs or this method is called on a closed connection
	 */
	public String deleteRow(ModelDataRow theRow) throws SQLException {
		List<ModelDataCell<?>> tmpKeyCells = theRow.getCells().stream().filter(cell -> cell.isKey()).collect(Collectors.toList()); // Get only key cells
		StringBuilder tmpStr = new StringBuilder("DELETE FROM ");
		String outStr; // will hold the actual statement used
		tmpStr.append(theRow.getTableName());				//
		tmpStr.append("\nWHERE ");							//
		tmpStr.append(tmpKeyCells.get(0).getColumnName());	//
		tmpStr.append("=?");								//
		tmpKeyCells.stream().skip(1).forEach(cell -> {		// Building prepared statement
			tmpStr.append(" AND ");							//
			tmpStr.append(cell.getColumnName());			//
			tmpStr.append("=?");							//
		});													//
		tmpStr.append(';');									//
		outStr = tmpStr.toString();
		try (PreparedStatement tmpStat = this.meConn.prepareStatement(outStr)) {
			for (int i = 0; i < tmpKeyCells.size(); i++) { // Loop and set prepared statement parameters (each using its own type)
				tmpStat.setObject(i + 1, tmpKeyCells.get(i).getValue(), tmpKeyCells.get(i).getSqlType());
				outStr = outStr.replaceFirst("\\?", tmpKeyCells.get(i).getSqlType() == Types.VARCHAR
						? "'" + tmpKeyCells.get(i).getValue().toString() + "'"
						: tmpKeyCells.get(i).getValue().toString());
			}
			tmpStat.executeUpdate(); // Delete
		}
		return outStr;
	}

	/**
	 * Updates the old row data (that better be a row in a table in the database) with new row data.
	 * @param oldRow		the old row data (better be only the key)
	 * @param newRow		new row data
	 * @return				the actual statement used to insert data (the prepared statement filled with parameter data)
	 * @throws SQLException	thrown if a database access error/timeout occurs or this method is called on a closed connection
	 */
	public String updateRow(ModelDataRow oldRow, ModelDataRow newRow) throws SQLException {
		List<ModelDataCell<?>> tmpKeyCells = oldRow.getCells().stream().filter(cell -> cell.isKey()).collect(Collectors.toList()); // Get only key cells
		StringBuilder tmpStr = new StringBuilder("UPDATE ");
		String outStr; // will hold the actual statement used
		tmpStr.append(oldRow.getTableName());					//
		tmpStr.append("\nSET ");								//
		tmpStr.append(newRow.getCells().get(0).getColumnName());//
		tmpStr.append("=?");									//
		newRow.getCells().stream().skip(1).forEach(cell -> {	//
			tmpStr.append(", ");								//
			tmpStr.append(cell.getColumnName());				//
			tmpStr.append("=?");								//
		});														//
		tmpStr.append("\nWHERE ");								// Building prepared statement
		tmpStr.append(tmpKeyCells.get(0).getColumnName());		//
		tmpStr.append("=?");									//
		tmpKeyCells.stream().skip(1).forEach(cell -> {			//
			tmpStr.append(" AND ");								//
			tmpStr.append(cell.getColumnName());				//
			tmpStr.append("=?");								//
		});														//
		tmpStr.append(';');										//
		outStr = tmpStr.toString();
		try (PreparedStatement tmpStat = this.meConn.prepareStatement(outStr)) {
			int i;
			for (i = 0; i < newRow.getCells().size(); i++) { // Loop and set prepared statement parameters with new data (each using its own type)
				tmpStat.setObject(i + 1, newRow.getCells().get(i).getValue(), newRow.getCells().get(i).getSqlType());
				outStr = outStr.replaceFirst("\\?", newRow.getCells().get(i).getSqlType() == Types.VARCHAR
						? "'" + newRow.getCells().get(i).getValue().toString() + "'"
						: newRow.getCells().get(i).getValue().toString());
			}
			for (i = 0; i < tmpKeyCells.size(); i++) { // Loop and set prepared statement parameters with old key data (each using its own type)
				tmpStat.setObject(newRow.getCells().size() + i + 1, tmpKeyCells.get(i).getValue(), tmpKeyCells.get(i).getSqlType());
				outStr = outStr.replaceFirst("\\?", tmpKeyCells.get(i).getSqlType() == Types.VARCHAR
						? "'" + tmpKeyCells.get(i).getValue().toString() + "'"
						: tmpKeyCells.get(i).getValue().toString());
			}
			tmpStat.executeUpdate(); // Update
		}
		return outStr;
	}

	/**
	 * Retrieves data from database according to specified criteria.
	 * @param dataCriteria	a list of data to retrieve data according to
	 * @param prepState		(optional) the prepared statement to be used to retrieve data, or null (or empty string) for an auto-built statement
	 * @return				a list of data rows that represent the retrieved data
	 * @throws SQLException	thrown if a database access error/timeout occurs or this method is called on a closed connection
	 */
	public List<ModelDataRow> getData(List<ModelDataCell<?>> dataCriteria, String prepState) throws SQLException {
		if (dataCriteria.isEmpty()) {
			return Collections.emptyList();
		}
		int i;
		String tmpStr = null;
		LinkedList<ModelDataRow> outResults = new LinkedList<>(); // Will hold the query results
		if (prepState == null || prepState.isEmpty()) { // If no prepared statement provided
			StringBuilder tmpPrepStat = new StringBuilder("SELECT DISTINCT ");
			List<String> tmpTables = dataCriteria.stream().map(ModelDataCell::getTableName).distinct().collect(Collectors.toList()); // Get table names (distinct) involved in query
			List<ModelDataCell<?>> tmpSelects = dataCriteria.stream().filter(cell -> { // To be placed after SELECT (The requested data columns. The cells should be empty)
				if (String.class == cell.getValueClass()) {
					return cell.getValue() == null || ((String)cell.getValue()).isEmpty();
				} else if (Integer.class == cell.getValueClass()) {
					return ((Integer)cell.getValue()) < 0;
				} else if (Boolean.class == cell.getValueClass()) {
					return cell.getValue() == null;
				} else if (ModelPersRole.class == cell.getValueClass()) {
					return ((ModelPersRole)cell.getValue()) == ModelPersRole.NONE;
				}
				return false;
			}).collect(Collectors.toList());
			List<ModelDataCell<?>> tmpConds = dataCriteria.stream().filter(cell -> { // To be placed as predicates (the cells should not be empty)
				if (String.class == cell.getValueClass()) {
					return !(cell.getValue() == null || ((String)cell.getValue()).isEmpty());
				} else if (Integer.class == cell.getValueClass()) {
					return ((Integer)cell.getValue())  > -1;
				} else if (Boolean.class == cell.getValueClass()) {
					return cell.getValue() != null;
				} else if (ModelPersRole.class == cell.getValueClass()) {
					return ((ModelPersRole)cell.getValue()) != ModelPersRole.NONE;
				}
				return false;
			}).collect(Collectors.toList());
			Map<String, String> tmpFKeys = new LinkedHashMap<>(this.meJoinONs); // A copy of all the table's foreign keys
			final List<String> tmpAllOrdTables = Arrays.asList(tmpFKeys.keySet().toArray(new String[0]));					//
			tmpTables.sort((tbl1, tbl2) -> tmpAllOrdTables.indexOf(tbl1) - tmpAllOrdTables.indexOf(tbl2));					//
			for (i = 0; !tmpAllOrdTables.get(i).equals(tmpTables.get(0)); i++) {											// Here, we fill in all the table names between the
				tmpFKeys.remove(tmpAllOrdTables.get(i));																	// first and last table contained in the criteria
			}																												// (in the order: personnel, participates, hasgenre,
			for (i = tmpAllOrdTables.size() - 1; !tmpAllOrdTables.get(i).equals(tmpTables.get(tmpTables.size() - 1)); i--) {// and genre).
				tmpFKeys.remove(tmpAllOrdTables.get(i));																	//
			}																												//
			List<String> tmpAllTables = Arrays.asList(tmpFKeys.keySet().toArray(new String[0]));
			if (tmpSelects.isEmpty()) {																	//
				tmpPrepStat.append("*\nFROM ");															//
			} else {																					//
				tmpPrepStat.append(tmpSelects.get(0).getTableName());									//
				tmpPrepStat.append('.');																//
				tmpPrepStat.append(tmpSelects.get(0).getColumnName());									//
				tmpSelects.stream().skip(1).forEach(cell -> {											//
					tmpPrepStat.append(", ");															//
					tmpPrepStat.append(cell.getTableName());											//
					tmpPrepStat.append('.');															//
					tmpPrepStat.append(cell.getColumnName());											//
				});																						//
				tmpPrepStat.append("\nFROM ");															//
			}																							//
			tmpPrepStat.append(tmpAllTables.get(0));													//
			tmpAllTables.stream().skip(1).forEach(tblStr -> {											//
				tmpPrepStat.append("\nJOIN ");															// Building prepared statement
				tmpPrepStat.append(tblStr);																//
				tmpPrepStat.append(" ON ");																//
				tmpPrepStat.append(tmpFKeys.get(tblStr));												//
			});																							//
			if (!tmpConds.isEmpty()) {																	//
				tmpPrepStat.append("\nWHERE ");															//
				tmpPrepStat.append(tmpConds.get(0).getTableName());										//
				tmpPrepStat.append('.');																//
				tmpPrepStat.append(tmpConds.get(0).getColumnName());									//
				tmpPrepStat.append(Types.VARCHAR == tmpConds.get(0).getSqlType() ? " LIKE ?" : "=?");	//
				tmpConds.stream().skip(1).forEach(cell -> {												//
					tmpPrepStat.append(" AND ");														//
					tmpPrepStat.append(cell.getTableName());											//
					tmpPrepStat.append('.');															//
					tmpPrepStat.append(cell.getColumnName());											//
					tmpPrepStat.append(Types.VARCHAR == cell.getSqlType() ? " LIKE ?" : "=?");			//
				});																						//
				tmpPrepStat.append(';');																//
			}
			dataCriteria = tmpConds; // To be handled in the next part
			tmpStr = tmpPrepStat.toString();
		} else { // If prepared statement was provided
			tmpStr = prepState;
		}
		try (PreparedStatement tmpStat = this.meConn.prepareStatement(tmpStr)) {
			for (i = 0; i < tmpStat.getParameterMetaData().getParameterCount(); i++) { // Loop and set prepared statement parameters (each using its own type)
				if (Types.VARCHAR == dataCriteria.get(i % dataCriteria.size()).getSqlType()) { // Check if string to add '%' around parameter
					tmpStat.setObject(i + 1, "%" + dataCriteria.get(i % dataCriteria.size()).getValue() + "%", dataCriteria.get(i % dataCriteria.size()).getSqlType());
				} else {
					tmpStat.setObject(i + 1, dataCriteria.get(i % dataCriteria.size()).getValue(), dataCriteria.get(i % dataCriteria.size()).getSqlType());
				}
				tmpStr = tmpStr.replaceFirst("\\?", dataCriteria.get(i % dataCriteria.size()).getSqlType() == Types.VARCHAR
						? "'%" + dataCriteria.get(i % dataCriteria.size()).getValue().toString() + "%'"
						: dataCriteria.get(i % dataCriteria.size()).getValue().toString());
			}
			try (ResultSet tmpRes = tmpStat.executeQuery()) { // Executing query
				while (tmpRes.next()) { // Loop and retrieve data from result
					outResults.add(new ModelDataRow(me_SERACH_TABLE_NAME, tmpStr));
					for (i = 1; i <= tmpRes.getMetaData().getColumnCount(); i++) {
						if (tmpRes.getObject(i) != null) { // Do not include NULL cells
							boolean tmpIsKey = this.meKeys.contains(tmpRes.getMetaData().getTableName(i) + "." + tmpRes.getMetaData().getColumnName(i));
							if (tmpRes.getMetaData().getColumnName(i).equals("role")) {
								outResults.getLast().addCell(new ModelDataCell<>(tmpRes.getMetaData().getColumnName(i), me_SERACH_TABLE_NAME,
										ModelPersRole.getValueOf((String)tmpRes.getObject(i)) , ModelPersRole.class, Types.VARCHAR, tmpIsKey, false));
							} else if (tmpRes.getMetaData().getColumnName(i).equals("is_movie")) {
								outResults.getLast().addCell(new ModelDataCell<>(tmpRes.getMetaData().getColumnName(i), me_SERACH_TABLE_NAME,
										((Integer)tmpRes.getObject(i)) > 0, Boolean.class, Types.BOOLEAN, tmpIsKey, false));
							} else if (String.class.isInstance(tmpRes.getObject(i))) {
								outResults.getLast().addCell(new ModelDataCell<>(tmpRes.getMetaData().getColumnName(i), me_SERACH_TABLE_NAME,
										(String)tmpRes.getObject(i), String.class, Types.VARCHAR, tmpIsKey, false));
							} else if (Integer.class.isInstance(tmpRes.getObject(i))) {
								outResults.getLast().addCell(new ModelDataCell<>(tmpRes.getMetaData().getColumnName(i), me_SERACH_TABLE_NAME,
										(Integer)tmpRes.getObject(i), Integer.class, Types.INTEGER, tmpIsKey, false));
							}
						}
					}
				}
			}
		}
		return outResults;
	}
}
