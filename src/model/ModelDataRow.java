package model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Janty Azmat
 */
public class ModelDataRow {
	// Fields
	private String meTable;
	private String mePrepStat;
	private List<ModelDataCell<?>> meCells;

	public ModelDataRow(String tableName, String prepStatement) {
		this.meTable = tableName;
		this.mePrepStat = prepStatement;
		this.meCells = new ArrayList<ModelDataCell<?>>();
	}

	public ModelDataRow(String theTable, String prepStatement, List<ModelDataCell<?>> theCells) {
		this(theTable, prepStatement);
		this.meCells.addAll(theCells);
	}

	public void addCell(ModelDataCell<?> newCell) {
		this.meCells.add(newCell);
	}

	public String getTableName() {
		return this.meTable;
	}

	public String getPreparedStatement() {
		return this.mePrepStat;
	}

	public List<ModelDataCell<?>> getCells() {
		return this.meCells;
	}
}
