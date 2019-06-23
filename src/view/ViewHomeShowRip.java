package view;

import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import view.ViewEditableDataRow.ViewEdRowStruct;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableRow;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;

/**
 * @author Janty Azmat
 */
public class ViewHomeShowRip extends BorderPane {

	/**
	 * An enumeration that represents the role a personnel can have in a show
	 * @author Janty Azmat
	 */
	public static enum ViewPersonnelRole {

		/**
		 * When the personnel has no role.
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

		private final String meName;

		ViewPersonnelRole(String theName) {
			this.meName = theName;
		}

		@Override
		public String toString() {
			return this.meName;
		}
	}

	// Fields
	private static final double me_MIN_STAGE_WIDTH = 1050;
	private static final double me_MIN_STAGE_HEIGHT = 550;
	private static final double me_STAGE_WIDTH = 1550;
	private static final double me_STAGE_HEIGHT = 800;
	private static final String me_TITLE = "HomeShow-Rip Organizer"; // Window title
	private static final Insets me_GEN_PAD = new Insets(20.0); // For general purpose padding
	private static final Insets me_SML_PAD = new Insets(4.0); // For small padding
	private static final double me_SML_SPCE = 4.0; // For small spacing
	private static final Font me_GEN_FONT = Font.font(22.0); // For general purpose font size
	private IViewObserver meObserver;
	private HBox meTop;
	private ScrollPane meMid;
	private Label meBot;
	private ImageView meImage;
	private FloatingWindow meFlWin;

	public ViewHomeShowRip(Stage theStage) {
		theStage.setTitle(me_TITLE);
		theStage.setScene(new Scene(this));
		theStage.setMinWidth(me_MIN_STAGE_WIDTH);
		theStage.setMinHeight(me_MIN_STAGE_HEIGHT);
		theStage.setWidth(me_STAGE_WIDTH);
		theStage.setHeight(me_STAGE_HEIGHT);
		theStage.setOnCloseRequest(winev -> this.meObserver.exitRequested()); // On the event of close is requested
		theStage.centerOnScreen();
		this.meFlWin = new FloatingWindow();
		this.meMid = new ScrollPane();
		ScrollPane tmpTop = new ScrollPane();
		ScrollPane tmpBot = new ScrollPane();
		this.meTop = new HBox(25.0);
		this.meImage = new ImageView("file:play.png");
		this.meBot = new Label();
		this.meBot.setTextFill(Color.RED);	//
		this.meBot.setFont(me_GEN_FONT);	// Help label properties
		this.meTop.setPadding(me_GEN_PAD);
		tmpBot.setPadding(me_SML_PAD);
		this.meImage.fitWidthProperty().bind(this.widthProperty());
		this.meImage.fitHeightProperty().bind(this.heightProperty().subtract(tmpTop.heightProperty()).subtract(tmpBot.heightProperty()));
		this.meImage.setPreserveRatio(true);
		tmpTop.setContent(this.meTop);
		tmpBot.setContent(this.meBot);
		this.setTop(tmpTop);
		this.setBottom(tmpBot);
		this.displayMainPage();
		theStage.show();
	}

	/**
	 * A method to add a single (that's all that we need) view-request observer that responses to
	 * the requests done by user.
	 * @param theViewObserver	the observer object
	 */
	public void setViewObserver(IViewObserver theViewObserver) { // Only one observer is needed
		this.meObserver = theViewObserver;
	}

	/**
	 * Clears the whole page of the view to prepare for another page.
	 */
	public void clearPage() {
		ScrollPane tmpBot = new ScrollPane();
		this.meTop.getChildren().clear();
		this.meMid.setContent(null);
		this.setCenter(this.meMid);
		this.meBot.setText("");
		tmpBot.setPadding(me_SML_PAD);
		tmpBot.setContent(this.meBot);
		this.setBottom(tmpBot);
	}

	/**
	 * A method to display an info message to the user
	 * @param theInfoMsg	the info message
	 */
	public void displayInfo(String theInfoMsg) {
		this.displayMessage(theInfoMsg, "Information..", AlertType.INFORMATION);
	}

	/**
	 * A method to display a warning message to the user
	 * @param theWarningMsg	the warning message
	 */
	public void displayWarning(String theWarningMsg) {
		this.displayMessage(theWarningMsg, "Warning..", AlertType.WARNING);
	}

	/**
	 * A method to display an error message to the user
	 * @param theErrorMsg	the error message
	 */
	public void displayError(String theErrorMsg) {
		this.displayMessage(theErrorMsg, "Error..", AlertType.ERROR);
	}

	private void displayMessage(String theMsg, String theTitle, AlertType theType) {
		Alert tmpAlert = new Alert(theType);
		((Label)tmpAlert.getDialogPane().getChildren().get(1)).setMinWidth(600.0);
		String tmpStyle = "-fx-font-size: 16px; -fx-font-weight: bold;";
		switch (theType) {
			case ERROR:
				tmpStyle += "-fx-text-fill: red;";
				break;
			case WARNING:
				tmpStyle += "-fx-text-fill: orangered;";
				break;
			default:
				tmpStyle += "-fx-text-fill: mediumblue;";
		}
		tmpAlert.getDialogPane().getChildren().get(1).setStyle(tmpStyle);
		tmpAlert.initModality(Modality.APPLICATION_MODAL);
		tmpAlert.initOwner(this.getScene().getWindow());
		tmpAlert.setHeaderText(null);
		tmpAlert.setTitle(theTitle);
		tmpAlert.setContentText(theMsg);
		tmpAlert.showAndWait();
	}

	/**
	 * A method to display the main page of the view.
	 */
	public void displayMainPage() {
		Button tmpInsBut = new Button("Insert/Update/Delete Data");	//
		Button tmpMixSrchBut = new Button("Mixed Criteria Search");	// Buttons for the main page
		Button tmpQuickSrchBut = new Button("Quick Search");		//
		tmpInsBut.setOnAction(ev -> this.meObserver.inserPageRequested());				//
		tmpMixSrchBut.setOnAction(ev -> this.meObserver.searchPageRequested(false));	// Assigning actions to buttons
		tmpQuickSrchBut.setOnAction(ev -> this.meObserver.searchPageRequested(true));	//
		this.meBot.setText("Welcome to " + me_TITLE + ". Please select your choice.");
		this.meTop.getChildren().addAll(tmpInsBut, tmpMixSrchBut, tmpQuickSrchBut); // Add buttons to top pane
		this.setCenter(this.meImage); // Put image in main page center
	}

	public void displayInsertPage(List<ViewDataRow> tableTemplates) {
		this.meBot.setText("Here, you can insert the data needed for the application.");
		this.displayRows(tableTemplates, ViewEdRowStruct.ALL_BUT_SELECT);
	}

	public void displayCriteriaSearchPage(List<ViewDataRow> tableTemplates) {
		Button tmpSrchBut = new Button("Search");
		this.meBot.setText("Here, you can specify your own search criteria.");
		this.displayRows(tableTemplates, EnumSet.of(ViewEdRowStruct.CLEAR_BUTTON, ViewEdRowStruct.SELECTOR_CHECK));
		this.meTop.getChildren().add(tmpSrchBut);
		tmpSrchBut.setOnAction(this::handleOnCriteriaSearcAction); // Action when searching in search page is requested
	}

	public void displayQuickSearchPage(List<ViewDataRow> quickSearchTemplates) {
		List<ViewEditableDataRow> tmpTmpls = quickSearchTemplates.stream().map(row -> new ViewEditableDataRow(row, EnumSet.of(ViewEdRowStruct.SEARCH_BUTTON, ViewEdRowStruct.CLEAR_BUTTON))).collect(Collectors.toList());
		Button tmpBackBut = new Button("<< Back");
		VBox tmpCont = new VBox();
		tmpBackBut.setOnAction(ev -> this.meObserver.mainPageRequested());
		this.meTop.getChildren().add(tmpBackBut);
		this.meBot.setText("Here, you can search based on already defined criteria.");
		tmpCont.setMinWidth(this.meMid.viewportBoundsProperty().getValue().getWidth() - me_SML_SPCE);
		this.meMid.viewportBoundsProperty().addListener((obs, oldVal, newVal) -> tmpCont.setMinWidth(newVal.getWidth() - me_SML_SPCE));
		tmpCont.getChildren().addAll(tmpTmpls);
		for (int i = 0; i < tmpTmpls.size(); i++) {
			tmpTmpls.get(i).addValueEditedListener(this::handleSearchListener); // Set handler when a value is edited
			tmpTmpls.get(i).setOnSearchAction(this::handleOnQuickSearcAction);
		}
		this.meMid.setContent(tmpCont);
	}

	private void displayRows(List<ViewDataRow> tableTemplates, EnumSet<ViewEdRowStruct> structFlags) {
		List<ViewEditableDataRow> tmpTmpls = tableTemplates.stream().map(row -> new ViewEditableDataRow(row, structFlags)).collect(Collectors.toList());
		VBox tmpCont = new VBox();
		Button tmpBackBut = new Button("<< Back");
		tmpBackBut.setOnAction(ev -> this.meObserver.mainPageRequested());
		this.meTop.getChildren().add(tmpBackBut);
		tmpCont.setMinWidth(this.meMid.viewportBoundsProperty().getValue().getWidth() - me_SML_SPCE);
		this.meMid.viewportBoundsProperty().addListener((obs, oldVal, newVal) -> tmpCont.setMinWidth(newVal.getWidth() - me_SML_SPCE));
		this.meMid.setPadding(me_SML_PAD);
		for (int i = 0; i < tableTemplates.size(); i++) { // This part voids MVC but did it to speedup assignment work
			if (tableTemplates.get(i).getTableName().equals("participates")) {
				TextField tmpPerFName = (TextField)((ViewEditableDataCell<?>)tmpTmpls.get(0).getChildren().get(1)).getChildren().get(2);
				TextField tmpPerLName = (TextField)((ViewEditableDataCell<?>)tmpTmpls.get(0).getChildren().get(2)).getChildren().get(2);
				TextField tmpPartiFName = (TextField)((ViewEditableDataCell<?>)tmpTmpls.get(1).getChildren().get(1)).getChildren().get(2);
				TextField tmpPartiLName = (TextField)((ViewEditableDataCell<?>)tmpTmpls.get(1).getChildren().get(2)).getChildren().get(2);
				tmpPerFName.getParent().setStyle("-fx-border-color: red;");
				tmpPerLName.getParent().setStyle("-fx-border-color: red;");
				tmpPartiFName.getParent().setStyle("-fx-border-color: red;");
				tmpPartiLName.getParent().setStyle("-fx-border-color: red;");
				tmpPerFName.textProperty().bindBidirectional(tmpPartiFName.textProperty());
				tmpPerLName.textProperty().bindBidirectional(tmpPartiLName.textProperty());
			} else if (tableTemplates.get(i).getTableName().equals("show")) {
				TextField tmpPartiShTitle = (TextField)((ViewEditableDataCell<?>)tmpTmpls.get(1).getChildren().get(3)).getChildren().get(2);
				@SuppressWarnings("unchecked")
				Spinner<Integer> tmpPartiShYr = (Spinner<Integer>)((ViewEditableDataCell<?>)tmpTmpls.get(1).getChildren().get(4)).getChildren().get(2);
				TextField tmpShowTitle = (TextField)((ViewEditableDataCell<?>)tmpTmpls.get(2).getChildren().get(3)).getChildren().get(2);
				@SuppressWarnings("unchecked")
				Spinner<Integer> tmpShowYr = (Spinner<Integer>)((ViewEditableDataCell<?>)tmpTmpls.get(2).getChildren().get(4)).getChildren().get(2);
				tmpPartiShTitle.getParent().setStyle("-fx-border-color: red;");
				tmpPartiShYr.getParent().setStyle("-fx-border-color: red;");
				tmpShowTitle.getParent().setStyle("-fx-border-color: red;");
				tmpShowYr.getParent().setStyle("-fx-border-color: red;");
				tmpPartiShTitle.textProperty().bindBidirectional(tmpShowTitle.textProperty());
				tmpPartiShYr.getValueFactory().valueProperty().bindBidirectional(tmpShowYr.getValueFactory().valueProperty());
			} else if (tableTemplates.get(i).getTableName().equals("hasgenre")) {
				TextField tmpHsGnShTitle = (TextField)((ViewEditableDataCell<?>)tmpTmpls.get(3).getChildren().get(3)).getChildren().get(2);
				TextField tmpShowTitle = (TextField)((ViewEditableDataCell<?>)tmpTmpls.get(2).getChildren().get(3)).getChildren().get(2);
				@SuppressWarnings("unchecked")
				Spinner<Integer> tmpHsGnShYr = (Spinner<Integer>)((ViewEditableDataCell<?>)tmpTmpls.get(3).getChildren().get(4)).getChildren().get(2);
				@SuppressWarnings("unchecked")
				Spinner<Integer> tmpShowYr = (Spinner<Integer>)((ViewEditableDataCell<?>)tmpTmpls.get(2).getChildren().get(4)).getChildren().get(2);
				tmpHsGnShTitle.getParent().setStyle("-fx-border-color: red;");
				tmpHsGnShYr.getParent().setStyle("-fx-border-color: red;");
				tmpHsGnShTitle.textProperty().bindBidirectional(tmpShowTitle.textProperty());
				tmpHsGnShYr.getValueFactory().valueProperty().bindBidirectional(tmpShowYr.getValueFactory().valueProperty());
				
			} else if (tableTemplates.get(i).getTableName().equals("genre")) {
				TextField tmpHsGnGnName = (TextField)((ViewEditableDataCell<?>)tmpTmpls.get(3).getChildren().get(5)).getChildren().get(2);
				TextField tmpGnName = (TextField)((ViewEditableDataCell<?>)tmpTmpls.get(4).getChildren().get(5)).getChildren().get(2);
				tmpHsGnGnName.getParent().setStyle("-fx-border-color: red;");
				tmpGnName.getParent().setStyle("-fx-border-color: red;");
				tmpHsGnGnName.textProperty().bindBidirectional(tmpGnName.textProperty());
			}
			tmpTmpls.get(i).addValueEditedListener(this::handleSearchListener); // Set handler when a value is edited
			if (structFlags.contains(ViewEdRowStruct.INSERT_BUTTON)) {
				tmpTmpls.get(i).setOnInsertAction(this::handleOnInsertAction); // Set handler when clicking the row's insert button
			}
			if (structFlags.contains(ViewEdRowStruct.DELETE_BUTTON)) {
				tmpTmpls.get(i).setOnDeleteAction(this::handleOnDeleteAction);
			}
			if (structFlags.contains(ViewEdRowStruct.UPDATE_BUTTON)) {
				tmpTmpls.get(i).setOnUpdateAction(this::handleOnUpdateAction);
			}
			if (structFlags.contains(ViewEdRowStruct.SEARCH_BUTTON)) {
				tmpTmpls.get(i).setOnSearchAction(ev -> this.handleSearchListener(new SimpleObjectProperty<>(ev.getSource(), ""), null, null)); // Set handler when clicking the row's insert button
			}
			tmpCont.getChildren().add(tmpTmpls.get(i));
		}
		this.meMid.setContent(tmpCont);
	}

	private void handleSearchListener(ObservableValue<? extends Object> theObs, Object oldVal, Object newVal) {
		Node tmpNode = (Node)((ReadOnlyProperty<? extends Object>)theObs).getBean(); // For the control that triggered the edit change
		ViewEditableDataRow tmpEditedRow = (ViewEditableDataRow)tmpNode.getParent().getParent();
		List<ViewDataRow> tmpSearch = Collections.emptyList(); // To store search results
		List<ViewDataCell<?>> tmpCrit = tmpEditedRow.getCells().stream().filter(cell -> { // Extract the search criteria
			if (cell.isSearchOnEdit()) {
				if (String.class == cell.getValueClass()) {
					return !(cell.getValue() == null || ((String)cell.getValue()).isEmpty());
				} else if (Integer.class == cell.getValueClass()) {
					return ((Integer)cell.getValue()) > -1;
				} else if (Boolean.class == cell.getValueClass()) {
					return cell.getValue() != null;
				} else if (ViewPersonnelRole.class == cell.getValueClass()) {
					return ((ViewPersonnelRole)cell.getValue()) != ViewPersonnelRole.NONE;
				}
			}
			return false;
		}).map(ViewEditableDataCell::getOriginalCell).collect(Collectors.toList());
		if (!tmpCrit.isEmpty()) {
			tmpSearch = this.meObserver.searchDataRequested(tmpCrit, null);//.stream().map(ViewEditableDataRow::new).collect(Collectors.toList());
		}
		this.meFlWin.setContent(new ViewTablePane(tmpSearch, ev -> {
			ViewDataRow tmpDblClickRow = (ViewDataRow)((TableRow<?>)ev.getSource()).getItem();
			List<ViewEditableDataCell<?>> tmpFilteredEditedRow = tmpEditedRow.getCells().stream().filter(cell -> cell.isSearchOnEdit()).collect(Collectors.toList());
			for (int i = 0; i < tmpFilteredEditedRow.size(); i++) {
				tmpFilteredEditedRow.get(i).setValue(tmpDblClickRow.getCells().get(i).getValue());
				this.meFlWin.hide();
				this.meFlWin.clearContent();
			}
			tmpEditedRow.anchorCurrentState();
		}, null, null));
		if (!this.meFlWin.isShown()) {
			double tmpScrCen = Screen.getScreensForRectangle( // To get the height center of active screen
					this.getScene().getWindow().getX(), this.getScene().getWindow().getY(),
					this.getScene().getWindow().getWidth(), this.getScene().getWindow().getHeight()
			).get(0).getBounds().getHeight() / 2.0;
			double tmpPntAtY = tmpNode.localToScreen(tmpNode.getBoundsInLocal()).getMinY();
			double tmpShift;
			Side tmpDir;
			if (tmpPntAtY > tmpScrCen) {
				tmpShift = -15.0;
				tmpDir = Side.TOP;
			} else {
				tmpShift = 15.0;
				tmpDir = Side.BOTTOM;
			}
			this.meFlWin.showAt(tmpNode, this.getWidth() - 10.0, 400.0, tmpNode.getParent().getParent(), tmpShift, tmpShift, tmpNode, tmpDir);
		}
	}

	private void handleOnInsertAction(ActionEvent theEvent) {
		ViewEditableDataRow tmpRow = ((ViewEditableDataRow)theEvent.getSource());
		this.meObserver.insertDataRequested(tmpRow.getOriginalRow());
		tmpRow.clearRow();
	}

	private void handleOnDeleteAction(ActionEvent theEvent) {
		ViewEditableDataRow tmpRow = ((ViewEditableDataRow)theEvent.getSource());
		this.meObserver.deleteDataRequested(tmpRow.getAnchoredState());
		tmpRow.clearRow();
	}

	private void handleOnUpdateAction(ActionEvent theEvent) {
		ViewEditableDataRow tmpRow = ((ViewEditableDataRow)theEvent.getSource());
		this.meObserver.updateDataRequested(tmpRow.getAnchoredState(), tmpRow.getOriginalRow());
		tmpRow.clearRow();
	}

	private void handleOnCriteriaSearcAction(ActionEvent theEvent) {
		List<ViewDataCell<?>> tmpCrit = new LinkedList<>(); // To hold the search criteria
		List<ViewDataRow> tmpSearch = Collections.emptyList(); // To store search results
		this.setBottom(null);
		((VBox)this.meMid.getContent()).getChildren().forEach(row -> {
			((ViewEditableDataRow)row).getCells().forEach(eCell -> {
				if (eCell.isSelected()) {
					tmpCrit.add(eCell.getOriginalCell());
				}
			});
		});
		if (!tmpCrit.isEmpty()) {
			tmpSearch = this.meObserver.searchDataRequested(tmpCrit, null);
		}
		if (this.getCenter() instanceof SplitPane) {
			((SplitPane)this.getCenter()).getItems().set(1, new ViewTablePane(tmpSearch, null, null, null));
		} else {
			SplitPane.setResizableWithParent(this.getCenter(), false);
			SplitPane tmpSpl = new SplitPane(this.getCenter(), new ViewTablePane(tmpSearch, null, null, null));
			tmpSpl.setOrientation(Orientation.VERTICAL);
			this.setCenter(tmpSpl);
		}
	}

	private void handleOnQuickSearcAction(ActionEvent theEvent) {
		ViewEditableDataRow tmpEditedRow = (ViewEditableDataRow)((Node)theEvent.getSource()).getParent().getParent();
		List<ViewDataCell<?>> tmpCrit = tmpEditedRow.getCells().stream().map(ViewEditableDataCell::getOriginalCell).collect(Collectors.toList());
		String tmpSplitOnCol = null, tmpSplitColName = null;
		switch (tmpCrit.get(0).getTableName()) {
			case "personnel":
				tmpSplitOnCol = "show_count";
				tmpSplitColName = "show";
				break;
			case "show":
				tmpSplitOnCol = "personnel_count";
				tmpSplitColName = "personnel";
		}
		List<ViewDataRow> tmpSearch = this.meObserver.searchDataRequested(tmpCrit, tmpEditedRow.getPreparedStatement());
		this.setBottom(null);
		if (this.getCenter() instanceof SplitPane) {
			((SplitPane)this.getCenter()).getItems().set(1, new ViewTablePane(tmpSearch, null, tmpSplitOnCol, tmpSplitColName));
		} else {
			SplitPane.setResizableWithParent(this.getCenter(), false);
			SplitPane tmpSplit = new SplitPane(this.getCenter(), new ViewTablePane(tmpSearch, null, tmpSplitOnCol, tmpSplitColName));
			tmpSplit.setOrientation(Orientation.VERTICAL);
			this.setCenter(tmpSplit);
		}
		double tmpPos = ((VBox)tmpEditedRow.getParent()).getHeight() / Math.max(this.meMid.getHeight(), ((SplitPane)this.getCenter()).getHeight()) + 0.02;
		((SplitPane)this.getCenter()).setDividerPosition(0, tmpPos);
	}

	public static Region createHiddenRegion(Node bountTo, double theShift) { // This part voids MVC but did it to speedup assignment work
		bountTo = bountTo.getParent();
		Region outReg = new Region();
		bountTo.boundsInParentProperty().addListener((obs, oldVal, newVal) -> {
			outReg.setMinWidth(newVal.getMinX() + theShift);
		});
		return outReg;
	}
}
