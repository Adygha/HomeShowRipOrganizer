package loader;

import java.sql.SQLException;
import controller.AppHive;
import model.HomeShowSqliteDAO;
import view.ViewHomeShowRip;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Just an application loading class.
 * @author Janty Azmat
 */
public class Loader extends Application {
	// Fields
	private ViewHomeShowRip meView; // This was put here as a field instead of a local variable, due to JavaFX exception handling

	@Override
	public void start(Stage primaryStage) {
		this.meView = new ViewHomeShowRip(primaryStage);
		HomeShowSqliteDAO tmpDAO = null;
		try {
			tmpDAO = new HomeShowSqliteDAO();
		} catch (SQLException e) {
			this.meView.displayError("Database access-error/timeout occured. Please check the connection string or that the required package is linked.");
			primaryStage.close();
			return;
		}
		AppHive tmpHive = new AppHive(this.meView);
		tmpHive.setDAO(tmpDAO);
		Thread.currentThread().setUncaughtExceptionHandler((thread, throwable) -> { // Added due to JavaFX exception handling (for unhandled exceptions)
			if (!(throwable instanceof NullPointerException)) { // This NullPointerException related to JavaFX tableview bug and safe to ignore but others not
				this.meView.displayError("Error: An exception was thrown during application display with message: " + throwable.getMessage() + "\nExitting application.");
				throwable.printStackTrace();
				primaryStage.close();
			}
		});;
	}

	public static void main(String[] args) {
		Application.launch(args);
	}
}
