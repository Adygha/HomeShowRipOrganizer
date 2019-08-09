package view;

//import java.awt.Desktop;
import java.io.IOException;
//import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/**
 * A class that represents essentially a TableView inside a BorderPane.
 * @author Janty Azmat
 */
class ViewTablePane extends BorderPane {
	// Fields
	private static final Insets me_SML_PAD = new Insets(4.0); // For small padding
	private static final Font me_GEN_FONT = Font.font(18.0); // For general purpose font size
	private TableView<ViewDataRow> meTable;
	private int meLeftColCount;
	private int meRightColCount;

	public ViewTablePane(List<ViewDataRow> theData, EventHandler<MouseEvent> doubleClickHandler, String splitSimilarOnCol, String splitColName) {
		this.meTable = new TableView<ViewDataRow>();
		Label tmpLabel = new Label();
		AnchorPane tmpTop = new AnchorPane(tmpLabel);
		AnchorPane tmpCen = new AnchorPane(this.meTable);
		this.anchorNode(tmpLabel);
		this.anchorNode(this.meTable);
		this.meTable.setStyle("-fx-selection-bar: sandybrown;");
		if (theData == null || theData.isEmpty()) {
			this.meTable.setPlaceholder(new Label("No Content To Show"));
		} else {
			tmpLabel.setTextFill(Color.RED);
			tmpLabel.setStyle("-fx-font-weight: bold;");
			tmpLabel.setPadding(me_SML_PAD);
			tmpLabel.setFont(me_GEN_FONT);
			tmpLabel.setWrapText(true);
			tmpLabel.setText("Statement for this search:\n" + theData.get(0).getPreparedStatement());
			if (doubleClickHandler != null) {
				this.meTable.setRowFactory(tView -> {
					TableRow<ViewDataRow> tmpRow = new TableRow<>();
					tmpRow.setOnMouseClicked(ev -> {
						if (ev.getClickCount() == 2 && !tmpRow.isEmpty()) {
							doubleClickHandler.handle(ev);
						}
					});
					return tmpRow;
				});
			}
			if (splitSimilarOnCol == null || splitSimilarOnCol.isEmpty()) {
				this.meTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
				ViewDataRow tmpLongest = theData.stream().max((row1, row2) -> row1.getCells().size() - row2.getCells().size()).get();
				this.meLeftColCount = tmpLongest.getCells().size();
				this.meRightColCount = 0;
				tmpLongest.getCells().forEach(cell -> this.meTable.getColumns().add(this.createColumn(cell)));

			} else {
				this.meTable.setColumnResizePolicy(rf -> {
					if (rf.getColumn() == null) {
						double tmpColWidth = (rf.getTable().getWidth() - 20.0) / (this.meLeftColCount + this.meRightColCount);
						int i;
						for (i = 0; i < rf.getTable().getColumns().size() - 1; i ++) {
							((TableColumn<?, ?>)rf.getTable().getColumns().get(i)).setPrefWidth(tmpColWidth);
						}
						if (!rf.getTable().getColumns().isEmpty()) {
							((TableColumn<?, ?>)rf.getTable().getColumns().get(i)).setPrefWidth(tmpColWidth * this.meRightColCount);
						}
					} else {
						rf.getColumn().setPrefWidth(rf.getColumn().getWidth() + rf.getDelta());
					}
					return true;
				});
				Map<ViewDataRow, List<ViewDataRow>> tmpSplit = new LinkedHashMap<>();
				boolean tmpFound;
				for (ViewDataRow row : theData) {
					ViewDataRow tmpLeftRow = new ViewDataRow(row.getTableName(), row.getPreparedStatement());
					ViewDataRow tmpRightRow = new ViewDataRow(row.getTableName(), row.getPreparedStatement());
					tmpFound = false;
					for (ViewDataCell<?> cell : row.getCells()) {
						if (tmpFound) {
							tmpRightRow.addCell(cell);
						} else {
							tmpLeftRow.addCell(cell);
							if (cell.getColumnName().equals(splitSimilarOnCol)) {
								tmpFound = true;
							}
						}
					}
					if (!tmpSplit.containsKey(tmpLeftRow)) {
						tmpSplit.put(tmpLeftRow, new LinkedList<>());
					}
					tmpSplit.get(tmpLeftRow).add(tmpRightRow);
				}
				theData = tmpSplit.keySet().stream().collect(Collectors.toList());
				ViewDataRow tmpLongest = theData.stream().max((row1, row2) -> row1.getCells().size() - row2.getCells().size()).get();
				this.meLeftColCount = tmpLongest.getCells().size();
				this.meRightColCount = theData.stream().flatMap(row -> tmpSplit.get(row).stream()).max((row1, row2) -> row1.getCells().size() - row2.getCells().size()).get().getCells().size();
				tmpSplit.entrySet().forEach(entry -> entry.getKey().addCell(new ViewDataCell<>(splitColName, entry.getKey().getTableName(), entry.getValue(), List.class, false, false)));
				tmpLongest.getCells().forEach(cell -> this.meTable.getColumns().add(this.createColumn(cell)));
			}
			this.meTable.getItems().addAll(FXCollections.observableList(theData));
		}
		this.setTop(tmpTop);
		this.setCenter(tmpCen);
	}

	@SuppressWarnings("unchecked")
	private <X> TableColumn<ViewDataRow, X> createColumn(ViewDataCell<X> forCell) {
		TableColumn<ViewDataRow, X> outCol = new TableColumn<ViewDataRow, X>();
		outCol.setCellValueFactory(cdf -> {
			return new SimpleObjectProperty<X>((X)cdf.getValue().getCells().get(cdf.getTableView().getColumns().indexOf(cdf.getTableColumn())).getValue());
		});
		if (forCell.getColumnName().equals("folder")) {
			outCol.setCellFactory(tc -> {
				TableCell<ViewDataRow, X> outTableCell = new TableCell<ViewDataRow, X>() {
					@Override
					public void updateItem(X cellData, boolean isEmpty) {
						super.updateItem(cellData, isEmpty);
						if (isEmpty) {
							this.setGraphic(null);
							this.setText(null);
						} else {
							Hyperlink tmpHyper = new Hyperlink();
							tmpHyper.setText((String)cellData);
							tmpHyper.setOnAction(ev -> {
								try {
									String tmpCmd = System.getProperty("os.name").toLowerCase();
									if (tmpCmd.contains("win")) {
										tmpCmd = "explorer \"";
									} else if (tmpCmd.contains("mac")) {
										tmpCmd = "/usr/bin/open \"";
									} else {
										tmpCmd = "xdg-open \"";
									}
									Runtime.getRuntime().exec(tmpCmd + tmpHyper.getText() + "\"");
//									Desktop.getDesktop().browse(Paths.get(tmpHyper.getText()).toUri());
								} catch (IOException e) {}
								tmpHyper.setVisited(false);
							});
							this.setText(null);
							this.setGraphic(tmpHyper);
						}
					}
				};
				return outTableCell;
			});
		} else if (List.class == forCell.getValueClass()) {
			outCol.setCellFactory(tc -> {
				TableCell<ViewDataRow, X> outTableCell = new TableCell<ViewDataRow, X>() {
					@Override
					public void updateItem(X cellData, boolean isEmpty) {
						super.updateItem(cellData, isEmpty);
						if (isEmpty) {
							this.setGraphic(null);
							this.setText(null);
						} else {
							TableView<ViewDataRow> tmpTbl = new TableView<ViewDataRow>();
							List<ViewDataRow> tmpItemList = (List<ViewDataRow>)cellData;
							tmpTbl.setPlaceholder(new Label("No Content To Show"));
							tmpTbl.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
							tmpTbl.setMinHeight(110.0);
							tmpTbl.setPrefHeight(110.0);
							tmpTbl.setMaxHeight(200.0);
							tmpTbl.setStyle("-fx-selection-bar: sandybrown;");
							tmpItemList.stream().max((row1, row2) -> row1.getCells().size() - row2.getCells().size()).get().getCells().forEach(cell -> tmpTbl.getColumns().add(createColumn(cell)));
							tmpTbl.getItems().addAll(tmpItemList);
							this.setText(null);
							this.setGraphic(tmpTbl);
						}
					}
				};
				return outTableCell;
			});
		}
		outCol.setText(forCell.getColumnName());
		outCol.setStyle( "-fx-alignment: center;"); // -fx-padding: 10 10 10 10;
		return outCol;
	}

	private void anchorNode(Node theNode) {
		AnchorPane.setTopAnchor(theNode, 0.0);
		AnchorPane.setLeftAnchor(theNode, 0.0);
		AnchorPane.setBottomAnchor(theNode, 0.0);
		AnchorPane.setRightAnchor(theNode, 0.0);
	}
}
