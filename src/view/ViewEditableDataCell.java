package view;

//import java.awt.Desktop;
//import java.nio.file.Paths;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;
import view.ViewHomeShowRip.ViewPersonnelRole;
import javafx.beans.InvalidationListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextFormatter.Change;
import javafx.scene.layout.VBox;

/**
 * @author Janty Azmat
 */
class ViewEditableDataCell<X> extends VBox {
	// Fields
	private static final Insets me_SML_PAD = new Insets(4.0); // For small padding
	private static final double me_SML_SPCE = 4.0; // For small spacing
	private static final String me_REGEX_MATCH = "^-?\\d+";
	private static final String me_REGEX_REPLACE = "(?!^-\\d)\\D+";
	private ViewDataCell<X> meOrigCell;
	private Control meCtrl;
	private CheckBox meSelector;
	private ConcurrentLinkedQueue<ChangeListener<Object>> meEditListeners;	//
	private ConcurrentLinkedQueue<InvalidationListener> meClearListeners;	// Concurrent to avoid multi-thread removal of listeners
	private ObjectProperty<Boolean> meTriBool;
	private Labeled meULabel;

	public ViewEditableDataCell(ViewDataCell<X> basedOnCell, boolean isUseSelector) {
		this.meOrigCell = basedOnCell;
		this.meEditListeners = new ConcurrentLinkedQueue<>();
		this.meClearListeners = new ConcurrentLinkedQueue<>();
		this.setPadding(me_SML_PAD);
		this.setSpacing(me_SML_SPCE);
		this.setStyle("-fx-border-color: black;");
		this.meSelector = new CheckBox();
		this.meSelector.setStyle("-fx-label-padding: 0;");
		this.meSelector.setVisible(isUseSelector);
		this.meSelector.setManaged(isUseSelector);
		this.setAlignment(Pos.CENTER);
		this.getChildren().add(this.meSelector);
		this.getChildren().add(new Label(this.meOrigCell.getColumnName()));
		if (this.meOrigCell.getValueClass() == String.class) {
			TextField tmpTxt = new TextField((String)this.meOrigCell.getValue());
			tmpTxt.textProperty().addListener(this::handleValueChanged);
			this.meCtrl = tmpTxt;
		} else if (this.meOrigCell.getValueClass() == Integer.class) {
			Spinner<Integer> tmpSpin = new Spinner<Integer>(Integer.MIN_VALUE, Integer.MAX_VALUE, (Integer)this.meOrigCell.getValue());
			tmpSpin.setEditable(true);
			tmpSpin.focusedProperty().addListener((obs, oldVal, newVal) -> { // To force spinner to commit its value on focus leave
				if (!newVal) {
					tmpSpin.increment(0);
//					tmpSpin.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ENTER, false, false, false, false));
				}
			});
			tmpSpin.getEditor().setTextFormatter(new TextFormatter<>(this::controlIntegerChange)); // Control the number changes
			tmpSpin.getEditor().textProperty().addListener((obs, oldVal, newVal) -> { // To allow and propagate only numbers
				int tmpNew = Integer.parseInt(newVal);
				this.handleValueChanged(new SimpleIntegerProperty(tmpSpin, ((ReadOnlyStringProperty)obs).getName(), tmpNew), Integer.parseInt(oldVal), tmpNew);
			});
			this.meCtrl = tmpSpin;
		} else if (this.meOrigCell.getValueClass() == Boolean.class) {
			CheckBox tmpChk = new CheckBox();
			tmpChk.setAllowIndeterminate(true);
			if (this.meOrigCell.getValue() == null) {
				this.meTriBool = new SimpleObjectProperty<Boolean>(tmpChk, "CheckBoxTriBool", null);
				tmpChk.setIndeterminate(true);
			} else {
				this.meTriBool = new SimpleObjectProperty<Boolean>(tmpChk, "CheckBoxTriBool", (Boolean)this.meOrigCell.getValue());
				tmpChk.setIndeterminate(false);
				tmpChk.setSelected(this.meTriBool.getValue());
			}
			tmpChk.selectedProperty().addListener((obs, oldVal, newVal) -> {
				this.meTriBool.set(newVal);
				this.handleValueChanged(obs, oldVal, this.meTriBool.getValue());
			});
			tmpChk.indeterminateProperty().addListener((obs, oldVal, newVal) -> {
				if (newVal) {
					this.meTriBool.set(null);
					this.handleValueChanged(obs, oldVal, null);
				}
			});
			this.meCtrl = tmpChk;
		} else if (this.meOrigCell.getValueClass() == ViewPersonnelRole.class) {
			ComboBox<ViewPersonnelRole> tmpBox = new ComboBox<ViewPersonnelRole>(FXCollections.observableList(Arrays.asList(ViewPersonnelRole.values())));
			tmpBox.getSelectionModel().select((ViewPersonnelRole)this.meOrigCell.getValue());
			tmpBox.valueProperty().addListener(this::handleValueChanged);
			this.meCtrl = tmpBox;
		} else if (this.meOrigCell.getValueClass() == TextField.class) { // This part voids MVC but did it to speedup assignment work
			TextField tmpTxt = new TextField();
			if (this.meSelector != null) {
				this.meSelector.setVisible(false);
			}
			tmpTxt.setVisible(false);
			this.setStyle("-fx-border-color: lightgray;");
			this.meCtrl = tmpTxt;
		}
		this.meCtrl.disableProperty().bind(this.meSelector.selectedProperty().not());
		this.meSelector.setSelected(!isUseSelector);
		this.getChildren().add(this.meCtrl);
	}

	public String getColumnName() {
		return this.meOrigCell.getColumnName();
	}

	public String getTableName() {
		return this.meOrigCell.getTableName();
	}

	public X getValue() {
		return this.meOrigCell.getValue();
	}

	@SuppressWarnings("unchecked")
	public void setValue(Object newVal) {
		this.meOrigCell.setValue(newVal);
		if (String.class == this.meOrigCell.getValueClass()) {
			((TextField)this.meCtrl).setText((String)newVal);
		} else if (Integer.class == this.meOrigCell.getValueClass()) {
			((Spinner<Integer>)this.meCtrl).getValueFactory().setValue((Integer)newVal);
		} else if (Boolean.class == this.meOrigCell.getValueClass()) {
			CheckBox tmpChk = (CheckBox)this.meCtrl;
			if (newVal == null) {
				tmpChk.setSelected(false);
				tmpChk.setIndeterminate(true);
				this.meTriBool.set(null);
			} else {
				tmpChk.setIndeterminate(false);
				tmpChk.setSelected((Boolean)newVal);
				this.meTriBool.set((Boolean)newVal);
			}
		} else if (ViewPersonnelRole.class == this.meOrigCell.getValueClass()) {
			((ComboBox<ViewPersonnelRole>)this.meCtrl).getSelectionModel().select((ViewPersonnelRole)newVal);
		}
	}

	public Class<X> getValueClass() {
		return this.meOrigCell.getValueClass();
	}

	public boolean isKey() {
		return this.meOrigCell.isKey();
	}

	public boolean isSearchOnEdit() {
		return this.meOrigCell.isSearchOnEdit();
	}

	public ViewDataCell<X> getOriginalCell() {
		return this.meOrigCell;
	}

	public boolean isSelected() {
		if (!this.isVisible()) {
			return false;
		}
		return this.meSelector.isSelected();
	}

	public void setUnderLabel(String labelText) {
		if (this.meULabel == null) {
			this.meULabel = this.getColumnName() == "folder" ? new Hyperlink() : new Label();
			meULabel.setMaxWidth(this.getWidth() - (6.0 * me_SML_SPCE));
			meULabel.setAlignment(Pos.BASELINE_LEFT);
			meULabel.setWrapText(true);
			this.getChildren().add(this.meULabel);
			if (this.getColumnName() == "folder") {
				((Hyperlink)this.meULabel).setOnAction(ev -> {
					if (!this.meULabel.getText().isEmpty()) {
						try {
							String tmpCmd = System.getProperty("os.name").toLowerCase();
							if (tmpCmd.contains("win")) {
								tmpCmd = "explorer \"";
							} else if (tmpCmd.contains("mac")) {
								tmpCmd = "/usr/bin/open \"";
							} else {
								tmpCmd = "xdg-open \"";
							}
							Runtime.getRuntime().exec(tmpCmd + this.meULabel.getText() + "\"");
//							Desktop.getDesktop().browse(Paths.get(this.meULabel.getText()).toUri());
						} catch (IOException e) {}
					}
					((Hyperlink)this.meULabel).setVisited(false);
				});
			}
		}
		this.meULabel.setText(labelText);
	}

	public void clearUnderLabel() {
		this.getChildren().remove(this.meULabel);
		this.meULabel = null;
	}

	public void addValueEditedListener(ChangeListener<Object> theListener) {
		this.meEditListeners.add(theListener);
	}

	public void removeValueEditedListener(ChangeListener<Object> theListener) {
		this.meEditListeners.remove(theListener);
	}

	public void addValueClearedListener(InvalidationListener theListener) {
		this.meClearListeners.add(theListener);
	}

	public void removeValueClearedListener(InvalidationListener theListener) {
		this.meClearListeners.remove(theListener);
	}

	private void handleValueChanged(ObservableValue<? extends Object> theObs, Object oldVal, Object newVal) {
		this.meOrigCell.setValue(newVal);
		if (this.meCtrl.isFocused() && this.meOrigCell.isSearchOnEdit()) {
			this.meEditListeners.forEach(lsn -> lsn.changed(theObs, oldVal, newVal));
		}
		if (newVal == null || (String.class == this.meOrigCell.getValueClass() && ((String)newVal).isEmpty()) ||
				(Integer.class == this.meOrigCell.getValueClass() && (Integer)newVal < 0) ||
				(ViewPersonnelRole.class == this.meOrigCell.getValueClass() && (ViewPersonnelRole)newVal == ViewPersonnelRole.NONE)) {
			this.meClearListeners.forEach(lsn -> lsn.invalidated(theObs));
		}
	}

	private Change controlIntegerChange(Change theChange) {
		if (theChange.isContentChange()) {
			String tmpStr = theChange.getControlNewText();
			Long tmpLng;
			boolean tmpChanged = false;
			if (theChange.getControlText().matches(me_REGEX_MATCH) && (tmpLng = Long.parseLong(theChange.getControlText())) <= Integer.MAX_VALUE && tmpLng >= Integer.MIN_VALUE) {
				if (!(tmpStr.isEmpty() || tmpStr.matches(me_REGEX_MATCH))) {
					tmpStr = tmpStr.replaceAll(me_REGEX_REPLACE, ""); // Replace all non-digit and non-prefix-dash
					tmpChanged = true;
				}
				if (tmpStr.isEmpty() || !tmpStr.matches(me_REGEX_MATCH)) { // Added (or..) again just in case
					tmpStr = "-1";
					tmpChanged = true;
				} else if ((tmpLng = Long.parseLong(tmpStr)) > Integer.MAX_VALUE || tmpLng < Integer.MIN_VALUE) {
					do { // Trim last digit until it is acceptable as Integer
						tmpStr = tmpStr.substring(0, tmpStr.length() - 1);
					} while ((tmpLng = Long.parseLong(tmpStr)) > Integer.MAX_VALUE || tmpLng < Integer.MIN_VALUE);
					tmpChanged = true;
				}
				if (tmpChanged) {
					theChange.setRange(0, theChange.getControlText().length());
					theChange.setText(tmpStr);
					theChange.selectRange(tmpStr.length(), tmpStr.length());
				}
			}
		}
		return theChange;
	}
}
