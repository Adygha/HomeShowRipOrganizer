package view;

/**
 * @author Janty Azmat
 */
public class ViewDataCell<X> {
	// Fields
	private String meColumn;
	private String meTable;
	private X meVal;
	private Class<X> meClass;
	private boolean meIsKey;
	private boolean meIsSearchOnEdit;

	public ViewDataCell(String columnName, String tableName, X theValue, Class<X> theClass, boolean isKey, boolean isSearchOnEdit) {
		if (theValue != null && !theClass.isInstance(theValue)) {
			throw new RuntimeException("The passed object should be an instance of the passed class.");
		}
		this.meColumn = columnName;
		this.meTable = tableName;
		this.meVal = theValue;
		this.meClass = theClass;
		this.meIsKey = isKey;
		this.meIsSearchOnEdit = isSearchOnEdit;
	}

	public ViewDataCell(ViewDataCell<X> origCell) { // Copy Constructor
		this(origCell.meColumn, origCell.meTable, origCell.meVal, origCell.meClass, origCell.meIsKey, origCell.isSearchOnEdit());
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

	@SuppressWarnings("unchecked")
	public void setValue(Object newVal) {
		if (newVal != null && !this.meClass.isInstance(newVal)) {
			throw new RuntimeException("The passed object should be an instance of the class of this cell.");
		}
		this.meVal = (X)newVal;
	}

	public Class<X> getValueClass() {
		return this.meClass;
	}

	public boolean isKey() {
		return this.meIsKey;
	}

	public boolean isSearchOnEdit() {
		return this.meIsSearchOnEdit;
	}

	public ViewEditableDataCell<X> createEditableCell(boolean isUseSelector) {
		return new ViewEditableDataCell<>(this, isUseSelector);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj != null && obj instanceof ViewDataCell && ((ViewDataCell<?>)obj).getValueClass() == this.meClass) {
			@SuppressWarnings("unchecked")
			ViewDataCell<X> tmpOther = (ViewDataCell<X>)obj;
//			if (this.meVal.getValue().equals(tmpOther.meVal.getValue()) && this.meColumn.equals(tmpOther.meColumn) && this.meTable.equals(tmpOther.meTable)) {
			if (this.meVal.equals(tmpOther.meVal) && this.meColumn.equals(tmpOther.meColumn) && this.meTable.equals(tmpOther.meTable)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public int hashCode() {
		return this.meVal == null ? 0 : this.meVal.hashCode();
	}

	@Override
	public String toString() {
		return this.meVal == null ? "" : this.meVal.toString();
	}
}
