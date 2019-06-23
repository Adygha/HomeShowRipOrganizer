package view;

import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import view.ViewHomeShowRip.ViewPersonnelRole;
import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * @author Janty Azmat
 */
class ViewEditableDataRow extends HBox {

	public static enum ViewEdRowStruct {
		INSERT_BUTTON, SEARCH_BUTTON, UPDATE_BUTTON, DELETE_BUTTON, CLEAR_BUTTON, SELECTOR_CHECK;

		public static final EnumSet<ViewEdRowStruct> ALL = EnumSet.allOf(ViewEdRowStruct.class);
		public static final EnumSet<ViewEdRowStruct> ALL_BUT_SELECT = EnumSet.of(INSERT_BUTTON, SEARCH_BUTTON, UPDATE_BUTTON, DELETE_BUTTON, CLEAR_BUTTON);
	}

	// Fields
	private static final Insets me_SML_PAD = new Insets(4.0); // For small padding
	private static final double me_SML_SPCE = 4.0; // For small spacing
	private ViewDataRow meOrigRow;
	private List<ViewDataCell<?>> meAnchor;
	private List<ViewEditableDataCell<?>> meCells;
	private EventHandler<ActionEvent> meHandlerInsert;
	private EventHandler<ActionEvent> meHandlerDelete;
	private EventHandler<ActionEvent> meHandlerUpdate;
	private EventHandler<ActionEvent> meHandlerSearch;
	private InvalidationListener meValClearedListener;
	private Button meButUpdt;
	private Button meButDel;
	private EnumSet<ViewEdRowStruct> meStructFlags;

	public ViewEditableDataRow(ViewDataRow basedOnRow, EnumSet<ViewEdRowStruct> structFlags) {
		this.meStructFlags = structFlags;
		if (structFlags.contains(ViewEdRowStruct.UPDATE_BUTTON) || structFlags.contains(ViewEdRowStruct.DELETE_BUTTON)) {
			this.meValClearedListener = ev -> this.unanchorCurrentState();
		}
		this.meOrigRow = basedOnRow;
		this.meCells = new LinkedList<>();
		Label tmpLblTbl = new Label(this.meOrigRow.getTableName());
		HBox tmpHBox = new HBox();
		if (structFlags.contains(ViewEdRowStruct.INSERT_BUTTON)) {
			Button tmpButIns = new Button("Insert");
			tmpButIns.setOnAction(this::handleInsertRequest);
			tmpHBox.getChildren().add(tmpButIns);
		}
		if (structFlags.contains(ViewEdRowStruct.SEARCH_BUTTON)) {
			Button tmpButSrch = new Button("Search");
			tmpButSrch.setOnAction(this::handleSearchRequest);
			tmpHBox.getChildren().add(tmpButSrch);
		}
		if (structFlags.contains(ViewEdRowStruct.UPDATE_BUTTON)) {
			this.meButUpdt = new Button("Update");
			this.meButUpdt.setDisable(true);
			this.meButUpdt.setOnAction(this::handleUpdateRequest);
			tmpHBox.getChildren().add(this.meButUpdt);
		}
		if (structFlags.contains(ViewEdRowStruct.DELETE_BUTTON)) {
			this.meButDel = new Button("Delete");
			this.meButDel.setDisable(true);
			this.meButDel.setOnAction(this::handleDeleteRequest);
			tmpHBox.getChildren().add(this.meButDel);
		}
		if (structFlags.contains(ViewEdRowStruct.CLEAR_BUTTON)) {
			Button tmpButClr = new Button("Clear");
			tmpButClr.setOnAction(ev -> this.clearRow());
			tmpHBox.getChildren().add(tmpButClr);
		}
		VBox tmpVBox = new VBox(me_SML_SPCE, tmpLblTbl, tmpHBox);
		tmpVBox.setStyle("-fx-border-color: black;");
		tmpVBox.setPadding(me_SML_PAD);
		tmpVBox.setAlignment(Pos.CENTER);
		tmpVBox.setMinWidth(100.0);
		tmpLblTbl.setStyle("-fx-font-weight: bold;");
		this.getChildren().add(tmpVBox);
		this.setStyle("-fx-border-color: black;");
		this.meCells.addAll(this.meOrigRow.getCells().stream().map(cell -> new ViewEditableDataCell<>(cell, structFlags.contains(ViewEdRowStruct.SELECTOR_CHECK))).collect(Collectors.toList()));
		this.getChildren().addAll(this.meCells);
	}

	public String getTableName() {
		return this.meOrigRow.getTableName();
	}

	public String getPreparedStatement() {
		return this.meOrigRow.getPreparedStatement();
	}

	public ViewDataRow getOriginalRow() {
		return this.meOrigRow;
	}

	public List<ViewEditableDataCell<?>> getCells() {
		return this.meCells;
	}

	public void clearRow() {
		this.getCells().forEach(cell -> {
			if (String.class == cell.getValueClass()) {
				cell.setValue("");
			} else if (Integer.class == cell.getValueClass()) {
				cell.setValue(-1);
			} else if (Boolean.class == cell.getValueClass()) {
				cell.setValue(null);
			} else if (ViewPersonnelRole.class == cell.getValueClass()) {
				cell.setValue(ViewPersonnelRole.NONE);
			}
		});
	}

	public void anchorCurrentState() {
		if (this.meStructFlags.contains(ViewEdRowStruct.UPDATE_BUTTON) || this.meStructFlags.contains(ViewEdRowStruct.DELETE_BUTTON)) {
			this.meAnchor = this.meOrigRow.getCells().stream().filter(cell -> TextField.class != cell.getValueClass()).map(cell -> new ViewDataCell<>(cell)).collect(Collectors.toList());
			int tmpDif = this.meCells.size() - this.meAnchor.size();
			for (int i = 0; i < this.meAnchor.size(); i++) {
				this.meCells.get(i + tmpDif).setUnderLabel("" + this.meAnchor.get(i).getValue());
				this.meCells.get(i + tmpDif).addValueClearedListener(this.meValClearedListener);
			}
			this.meButUpdt.setDisable(false);
			this.meButDel.setDisable(false);
		}
	}

	public boolean isStateAnchored() {
		return this.meAnchor != null;
	}

	public ViewDataRow getAnchoredState() {
		if (this.meAnchor != null) {
			return new ViewDataRow(this.getTableName(), this.getPreparedStatement(), this.meAnchor);
		}
		return null;
	}

	public void unanchorCurrentState() {
		if (this.meAnchor != null) {
			int tmpDif = this.meCells.size() - this.meAnchor.size();
			for (int i = tmpDif; i < this.meCells.size(); i++) {
				this.meCells.get(i).clearUnderLabel();
				this.meCells.get(i).removeValueClearedListener(this.meValClearedListener);
			}
			this.meAnchor = null;
			this.meButUpdt.setDisable(true);
			this.meButDel.setDisable(true);
		}
	}

	public void addValueEditedListener(ChangeListener<Object> theListener) {
		this.meCells.forEach(cell -> {
			if (cell.isSearchOnEdit()) {
				cell.addValueEditedListener(theListener);
			}
		});
	}

	public void removeValueEditedListener(ChangeListener<Object> theListener) {
		this.meCells.forEach(cell -> {
			if (cell.isSearchOnEdit()) {
				cell.removeValueEditedListener(theListener);
			}
		});
	}

	public void setOnInsertAction(EventHandler<ActionEvent> onInsertRequestHandler) {
		this.meHandlerInsert = onInsertRequestHandler;
	}

	public void setOnDeleteAction(EventHandler<ActionEvent> onDeleteRequestHandler) {
		this.meHandlerDelete = onDeleteRequestHandler;
	}

	public void setOnUpdateAction(EventHandler<ActionEvent> onUpdateRequestHandler) {
		this.meHandlerUpdate = onUpdateRequestHandler;
	}

	public void setOnSearchAction(EventHandler<ActionEvent> onSearchRequestHandler) {
		this.meHandlerSearch = onSearchRequestHandler;
	}

	private void handleInsertRequest(ActionEvent theEvent) {
		if (this.meHandlerInsert != null) {
			this.meHandlerInsert.handle(theEvent.copyFor(this, this));
		}
		theEvent.consume();
	}

	private void handleDeleteRequest(ActionEvent theEvent) {
		if (this.meHandlerDelete != null) {
			this.meHandlerDelete.handle(theEvent.copyFor(this, this));
		}
		theEvent.consume();
	}

	private void handleUpdateRequest(ActionEvent theEvent) {
		if (this.meHandlerUpdate != null) {
			this.meHandlerUpdate.handle(theEvent.copyFor(this, this));
		}
		theEvent.consume();
	}

	private void handleSearchRequest(ActionEvent theEvent) {
		if (this.meHandlerSearch != null) {
			this.meHandlerSearch.handle(theEvent.copyFor(((Node)theEvent.getSource()).getParent(), theEvent.getTarget()));
		}
		theEvent.consume();
	}
}
