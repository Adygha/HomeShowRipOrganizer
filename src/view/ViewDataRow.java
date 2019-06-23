package view;

import java.util.LinkedList;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

/**
 * @author Janty Azmat
 */
public class ViewDataRow {
	// Fields
	private String meTable;
	private String mePrepStat;
	private List<ViewDataCell<?>> meCells;

	public ViewDataRow(String tableName, String prepStatement) {
		this.meTable = tableName;
		this.mePrepStat = prepStatement;
		this.meCells = new LinkedList<ViewDataCell<? extends Object>>();
	}

	public ViewDataRow(String theTable, String prepStatement, List<ViewDataCell<?>> theCells) {
		this(theTable, prepStatement);
		this.meCells.addAll(theCells);
	}

	public ViewDataRow(ViewDataRow origRow) { // Copy Constructor
		this(origRow.meTable, origRow.mePrepStat);
		this.meCells = origRow.meCells.stream().map(cell -> new ViewDataCell<>(cell)).collect(Collectors.toList());
	}

	public void addCell(ViewDataCell<?> newCell) {
		this.meCells.add(newCell);
	}

	public String getTableName() {
		return this.meTable;
	}

	public String getPreparedStatement() {
		return this.mePrepStat;
	}

	public List<ViewDataCell<?>> getCells() {
		return this.meCells;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof ViewDataRow) || ((ViewDataRow)obj).meCells.size() != this.meCells.size()) {
			return false;
		} else {
			ViewDataRow tmpOther = (ViewDataRow)obj;
			for (int i = 0; i < this.meCells.size(); i++) {
				if (!this.meCells.get(i).equals(tmpOther.meCells.get(i))) {
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public int hashCode() {
		if (this.meCells.isEmpty()) {
			return 0;
		}
		int outHash = this.meCells.get(0).hashCode();
		for (int i = 1; i < this.meCells.size(); i++) {
			outHash ^= this.meCells.get(i).hashCode();
		}
		return outHash;
	}

	@Override
	public String toString() {
		StringJoiner tmpJn = new StringJoiner(", ", "[", "]");
		this.meCells.forEach(cell -> tmpJn.add(cell.toString()));
		return tmpJn.toString();
	}
}
