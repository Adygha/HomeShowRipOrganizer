package controller;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import model.HomeShowSqliteDAO;
import model.HomeShowSqliteDAO.ModelPersRole;
import model.ModelDataCell;
import model.ModelDataRow;
import view.ViewHomeShowRip;
import view.ViewHomeShowRip.ViewPersonnelRole;
import view.IViewObserver;
import view.ViewDataCell;
import view.ViewDataRow;
import javafx.scene.control.TextField;

/**
 * @author Janty Azmat
 */
public class AppHive implements IViewObserver {
	// Fields
	private ViewHomeShowRip meView;
	private HomeShowSqliteDAO meDAO;

	public AppHive(ViewHomeShowRip theView) {
		this.meView = theView;
		this.meView.setViewObserver(this); // Add self as a view-request observer (this will trigger start viewing main page)
	}

	public void setDAO(HomeShowSqliteDAO theDAO) {
		this.meDAO = theDAO;
	}

	@Override
	public void mainPageRequested() {
		this.meView.clearPage();
		this.meView.displayMainPage();
	}

	@Override
	public void inserPageRequested() {
		List<ViewDataRow> tmpViewTemplates = this.meDAO.getTableTemplates().stream().map(this::parseViewDataRow).collect(Collectors.toList());
		this.meView.clearPage();
		this.meView.displayInsertPage(tmpViewTemplates);
	}

	@Override
	public void searchPageRequested(boolean isQuick) {
		this.meView.clearPage();
		if (isQuick) {
			this.meView.displayQuickSearchPage(this.meDAO.getQuickSearchTemplates().stream().map(this::parseViewDataRow).collect(Collectors.toList()));
		} else {
			this.meView.displayCriteriaSearchPage(this.meDAO.getTableTemplates().stream().map(this::parseViewDataRow).collect(Collectors.toList()));
		}
	}

	@Override
	public void exitRequested() {
		try {
			this.meDAO.close();
		} catch (SQLException e) {
			this.meView.displayError("Error while closing database connection.");
		}
	}

	@Override
	public void insertDataRequested(ViewDataRow newData) {
		try {
			this.meView.displayInfo("Data was inserted to table '" + newData.getTableName() + "' using the statement:\n\n" + this.meDAO.insertRow(this.parseModelDataRow(newData)));
		} catch (SQLException e) {
			switch (e.getErrorCode()) {
				case 19: // When check constraint failed
					this.meView.displayWarning("Incorrect/Incomplete new '" + newData.getTableName() + "' data due to table constraint(s). Please check your data and try again.");
					break;
				default:
					this.meView.displayError("Error while inserting new '" + newData.getTableName() + "' data. Please check your data and try again.");
			}
		}
	}

	@Override
	public void deleteDataRequested(ViewDataRow deleteData) {
		try {
			this.meView.displayInfo("Data was deleted from table '" + deleteData.getTableName() + "' using the statement:\n\n" + this.meDAO.deleteRow(this.parseModelDataRow(deleteData)));
		} catch (SQLException e) {
			this.meView.displayError("Error while deleting data from table '" + deleteData.getTableName() + "'. Please check your input and try again.");
		}
	}

	@Override
	public void updateDataRequested(ViewDataRow oldData, ViewDataRow newData) {
		try {
			this.meView.displayInfo("Data was updated into table '" + oldData.getTableName() + "' using the statement:\n\n" + this.meDAO.updateRow(this.parseModelDataRow(oldData), this.parseModelDataRow(newData)));
		} catch (SQLException e) {
			this.meView.displayError("Error while updating data into table '" + oldData.getTableName() + "'. Please check your input and try again.");
		}
	}

	@Override
	public List<ViewDataRow> searchDataRequested(List<ViewDataCell<?>> searchCriteria, String prepState) {
		if (searchCriteria.isEmpty()) {
			return Collections.emptyList();
		} else {
			List<ModelDataCell<?>> tmpCrit = searchCriteria.stream().map(this::parseModelDataCell).collect(Collectors.toList());
			List<ModelDataRow> tmpRes = null;
			try {
				tmpRes = this.meDAO.getData(tmpCrit, prepState);
			} catch (SQLException e) {
				this.meView.displayError("Error while searching using requested data. Please check your data and try again.");
				return Collections.emptyList();
			}
			return tmpRes.stream().map(this::parseViewDataRow).collect(Collectors.toList());
		}
	}

	private ModelDataCell<?> parseModelDataCell(ViewDataCell<?> theCell) {
		ModelDataCell<?> outCell = null;
		if (String.class == theCell.getValueClass()) {
			outCell = new ModelDataCell<>(theCell.getColumnName(), theCell.getTableName(), ((String)theCell.getValue()).trim(), String.class, Types.VARCHAR, theCell.isKey(), theCell.isSearchOnEdit());
		} else if (Integer.class == theCell.getValueClass()) {
			outCell = new ModelDataCell<>(theCell.getColumnName(), theCell.getTableName(), (Integer)theCell.getValue(), Integer.class, Types.INTEGER, theCell.isKey(), theCell.isSearchOnEdit());
		} else if (Boolean.class == theCell.getValueClass()) {
			outCell = new ModelDataCell<>(theCell.getColumnName(), theCell.getTableName(), (Boolean)theCell.getValue(), Boolean.class, Types.BOOLEAN, theCell.isKey(), theCell.isSearchOnEdit());
		} else if (ViewPersonnelRole.class == theCell.getValueClass()) {
			outCell = new ModelDataCell<>(theCell.getColumnName(), theCell.getTableName(), ModelPersRole.values()[((ViewPersonnelRole)theCell.getValue()).ordinal()], ModelPersRole.class, Types.VARCHAR, theCell.isKey(), theCell.isSearchOnEdit());
		}
		return outCell;
	}

	private ModelDataRow parseModelDataRow(ViewDataRow rowData) {
		List<ModelDataCell<?>> tmpCells = rowData.getCells().stream().filter(dcell -> dcell.getValueClass() != TextField.class).map(this::parseModelDataCell).collect(Collectors.toList());
		return new ModelDataRow(rowData.getTableName(), rowData.getPreparedStatement(), tmpCells);
	}

	private ViewDataCell<?> parseViewDataCell(ModelDataCell<?> theCell) {
		ViewDataCell<?> outCell = null;
		if (String.class == theCell.getValueClass()) {
			outCell = new ViewDataCell<>(theCell.getColumnName(), theCell.getTableName(), (String)theCell.getValue(), String.class, theCell.isKey(), theCell.isSearchOnEdit());
		} else if (Integer.class == theCell.getValueClass()) {
			outCell = new ViewDataCell<>(theCell.getColumnName(), theCell.getTableName(), (Integer)theCell.getValue(), Integer.class, theCell.isKey(), theCell.isSearchOnEdit());
		} else if (Boolean.class == theCell.getValueClass()) {
			outCell = new ViewDataCell<>(theCell.getColumnName(), theCell.getTableName(), (Boolean)theCell.getValue(), Boolean.class, theCell.isKey(), theCell.isSearchOnEdit());
		} else if (ModelPersRole.class == theCell.getValueClass()) {
			outCell = new ViewDataCell<>(theCell.getColumnName(), theCell.getTableName(), ViewPersonnelRole.values()[((ModelPersRole)theCell.getValue()).ordinal()], ViewPersonnelRole.class, theCell.isKey(), theCell.isSearchOnEdit());
		}
		return outCell;
	}

	private ViewDataRow parseViewDataRow(ModelDataRow rowData) {
		List<ViewDataCell<?>> tmpCells = new ArrayList<>();
		if (rowData.getTableName().equals("show") || rowData.getTableName().equals("hasgenre")) { // This condition voids MVC but did it to speedup assignment work
			tmpCells.add(new ViewDataCell<>("", "", createHiddenTextField(), TextField.class, false, false));
			tmpCells.add(new ViewDataCell<>("", "", createHiddenTextField(), TextField.class, false, false));
		} else if (rowData.getTableName().equals("genre")) {
			tmpCells.add(new ViewDataCell<>("", "", createHiddenTextField(), TextField.class, false, false));
			tmpCells.add(new ViewDataCell<>("", "", createHiddenTextField(), TextField.class, false, false));
			tmpCells.add(new ViewDataCell<>("", "", createHiddenTextField(), TextField.class, false, false));
			tmpCells.add(new ViewDataCell<>("", "", createHiddenTextField(), TextField.class, false, false));
		}
		rowData.getCells().stream().map(this::parseViewDataCell).forEach(tmpCells::add);
		return new ViewDataRow(rowData.getTableName(), rowData.getPreparedStatement(), tmpCells);
	}

	private static TextField createHiddenTextField() {
		TextField outTxt = new TextField();
		outTxt.setVisible(false);
		return outTxt;
	}
}
