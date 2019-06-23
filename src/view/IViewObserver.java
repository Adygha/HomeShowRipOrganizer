package view;

import java.util.List;

/**
 * @author Janty Azmat
 */
public interface IViewObserver {

	/**
	 * Triggered when the main page is requested to be viewed.
	 */
	void mainPageRequested();

	/**
	 * Triggered when the insert page is requested to be viewed.
	 */
	void inserPageRequested();

	/**
	 * Triggered when the search page (quick or criteria) is requested to be viewed.
	 * @param isQuick	'true' to specify quick search page or 'false' for mixed criteria page
	 */
	void searchPageRequested(boolean isQuick);

	/**
	 * Triggered when the exiting of application is requested.
	 */
	void exitRequested();

	/**
	 * Triggered when inserting new data is requested.
	 * @param newData	the new data row to be inserted
	 */
	void insertDataRequested(ViewDataRow newData);

	/**
	 * Triggered when deleting data is requested.
	 * @param deleteData	the data row to be deleted
	 */
	void deleteDataRequested(ViewDataRow deleteData);

	/**
	 * Triggered when updating data is requested.
	 * @param oldData	the old data row to be replaced
	 * @param newData	the new data row
	 */
	void updateDataRequested(ViewDataRow oldData, ViewDataRow newData);

	/**
	 * Triggered when searching for data is requested.
	 * @param searchCriteria	the list of data cells that contains the search criteria
	 * @param prepState			(optional) the prepared statement to be used to retrieve data, or null (or empty string) to auto-build statement
	 * @return
	 */
	List<ViewDataRow> searchDataRequested(List<ViewDataCell<?>> searchCriteria, String prepState);
}
