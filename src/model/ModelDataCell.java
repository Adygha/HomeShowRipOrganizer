package model;

/**
 * @author Janty Azmat
 */
public class ModelDataCell<X> {
	// Fields
	private String meColumn;
	private String meTable;
	private X meVal;
	private Class<X> meClass;
	private int meSql;
	private boolean meIsKey;
	private boolean meIsSearchOnEdit;

	public ModelDataCell(String columnName, String tableName, X theValue, Class<X> theClass, int sqlType, boolean isKey, boolean isSearchOnEdit) {
		if (theValue != null && !theClass.isInstance(theValue)) {
			throw new RuntimeException("The passed object should be an instance of the passed class. [" + theClass.getName() + ":" + theValue + "].");
		}
		this.meColumn = columnName;
		this.meTable = tableName;
		this.meVal = theValue;
		this.meClass = theClass;
		this.meSql = sqlType;
		this.meIsKey = isKey;
		this.meIsSearchOnEdit = isSearchOnEdit;
	}

	public String getColumnName() {
		return this.meColumn;
	}

	public String getTableName() {
		return this.meTable;
	}

	public X getValue() {
		return this.meVal;
	}

	public Class<X> getValueClass() {
		return this.meClass;
	}

	public int getSqlType() {
		return this.meSql;
	}

	public boolean isKey() {
		return this.meIsKey;
	}

	public boolean isSearchOnEdit() {
		return this.meIsSearchOnEdit;
	}
}
