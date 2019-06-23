package view;

import java.util.Deque;
import java.util.LinkedList;
import javafx.event.Event;
import javafx.event.EventDispatchChain;
import javafx.event.EventDispatcher;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.stage.Popup;
import javafx.stage.Screen;

/**
 * A class that represents a floating window.
 * @author Janty Azmat
 */
class FloatingWindow {

	/**
	 * Used to initiate a node's EventDispatchChain.
	 * @author From https://stackoverflow.com/questions/49538364
	 */
	private static class SimpleChain implements EventDispatchChain { // From: https://stackoverflow.com/questions/49538364
		// Fields
	    private Deque<EventDispatcher> meDisps = new LinkedList<>();

	    @Override
	    public EventDispatchChain append(EventDispatcher eventDispatcher) {
	        this.meDisps.addLast(eventDispatcher);
	        return this;
	    }

	    @Override
	    public EventDispatchChain prepend(EventDispatcher eventDispatcher) {
	    	this.meDisps.addFirst(eventDispatcher);
	        return this;
	    }

	    @Override
	    public Event dispatchEvent(Event theEvent) {
	        if (this.meDisps.peekFirst() != null) {
	            Event tmpResEv = this.meDisps.removeFirst().dispatchEvent(theEvent, this);
	            if (tmpResEv == null) {
	            	theEvent.consume();
	                return theEvent;
	            } else {
	            	return tmpResEv;
	            }
	        } else {
	            return theEvent;
	        }
	    }
	}

	// Fields
	private static final double me_PAD = 4.0; // Height of the empty area where we can grab the window
	private Popup mePop;
	private AnchorPane meWin;
	private Polygon meArrow;
	private Pane meBord;
	private Node meCont;
	private Point2D meLastArrowPoint;
	private double meLastWidth, meLastHeight, meLastX, meLastY, meDifX, meDifY;
	private EventDispatchChain meLastChain;

	/**
	 * Default Constructor.
	 */
	public FloatingWindow() {
		this.mePop = new Popup();
		this.meWin = new AnchorPane();
		this.meArrow = new Polygon();
		this.meBord = new Pane();
		this.mePop.setAutoHide(true);
		this.mePop.setConsumeAutoHidingEvents(false);
		this.meWin.setStyle("-fx-background-color: skyblue; -fx-border-style: dotted; -fx-border-radius: 5; -fx-background-radius: 5;");
		this.meArrow.setFill(Color.SKYBLUE);
		this.meBord.getChildren().addAll(this.meArrow, this.meWin);
		this.mePop.getContent().add(this.meBord);
		this.prepareEvents();
	}

	/**
	 * Adds a node as a content for this floating window.
	 * @param theContent	the node to be added as content. Can be set to 'null' to clear the
	 * 						content (which is the same as calling 'clearContent' method)
	 */
	public void setContent(Node theContent) {
		this.clearContent();
		if (theContent != null) {
			this.meCont = theContent;
			AnchorPane.setTopAnchor(theContent, me_PAD);
			AnchorPane.setRightAnchor(theContent, me_PAD);
			AnchorPane.setBottomAnchor(theContent, me_PAD);
			AnchorPane.setLeftAnchor(theContent, me_PAD);
			this.meWin.getChildren().add(theContent);
		}
	}

	/**
	 * Clears the content of this floating window.
	 */
	public void clearContent() {
		if (this.meCont != null) {
			AnchorPane.clearConstraints(this.meCont);
			this.meCont = null;
			this.meWin.getChildren().clear();
		}
	}

	/**
	 * Shows the floating window at the specified location on the screen. It also allows specifying the pointing arrow.
	 * position and the owner node.
	 * @param ownerNode		the owner Node of the floating window. It must not be null and must be associated with a Window
	 * @param windowX		the X position of the floating window in screen coordinates
	 * @param windowY		the Y position of the floating window in screen coordinates
	 * @param windowWidth	the width of the floating window
	 * @param windowHeight	the height of the floating window
	 * @param arrowX		the X position of the head of the arrow
	 * @param arrowY		the Y position of the head of the arrow
	 */
	public void showAt(Node ownerNode, double windowX, double windowY, double windowWidth, double windowHeight, double arrowX, double arrowY) {
		this.meLastWidth = windowWidth;
		this.meLastHeight = windowHeight;
		Bounds tmpWinBounds = new BoundingBox(windowX, windowY, windowWidth, windowHeight); // Bounds based on screen
		this.meLastArrowPoint = new Point2D(arrowX, arrowY);
		Point2D tmpPnt = this.updatePositions(tmpWinBounds); // .setFocusTraversable(false);
		this.meLastChain = ownerNode.buildEventDispatchChain(new SimpleChain());
		this.mePop.show(ownerNode, tmpPnt.getX(), tmpPnt.getY());
	}

	/**
	 * Shows the floating window based on the location on the of a specified. It also allows specifying the
	 * pointing arrow to point at another specified node.
	 * @param ownerNode			the owner Node of the floating window. It must not be null and must be associated with a Window
	 * @param windowWidth		the width of the floating window
	 * @param windowHeight		the height of the floating window
	 * @param positionRelatedTo	the node that this floating window will be positioned based on
	 * @param xShift			the X shift of the floating window position from the 'positionRelatedTo' node
	 * @param yShift			the Y shift of the floating window position from the 'positionRelatedTo' node
	 * @param pointAt			the node that the arrow will point at
	 * @param theDirection		the direction where this floating window will appear based on 'positionRelatedTo' node
	 */
	public void showAt(Node ownerNode, double windowWidth, double windowHeight, Node positionRelatedTo, double xShift, double yShift, Node pointAt, Side theDirection) {
		double tmpWinX, tmpWinY, tmpArrX, tmpArrY;
		Bounds tmpRelBounds = positionRelatedTo.localToScreen(
				new BoundingBox(
						positionRelatedTo.getBoundsInLocal().getMinX() + xShift,
						positionRelatedTo.getBoundsInLocal().getMinY() + yShift,
						positionRelatedTo.getBoundsInLocal().getWidth(),
						positionRelatedTo.getBoundsInLocal().getHeight()
				)
		);
		Bounds tmpAtBounds = pointAt.localToScreen(pointAt.getBoundsInLocal());
		tmpWinX = tmpWinY = tmpArrX = tmpArrY = 0.0;
		switch (theDirection) {
			case TOP:
				tmpWinX = tmpRelBounds.getMinX();
				tmpWinY = tmpRelBounds.getMinY() - windowHeight;
				tmpArrX = tmpAtBounds.getWidth() / 2.0 + tmpAtBounds.getMinX();
				tmpArrY = tmpAtBounds.getMinY();
				break;
			case LEFT:
				tmpWinX = tmpRelBounds.getMinX() - windowWidth;
				tmpWinY = tmpRelBounds.getMinY();
				tmpArrX = tmpAtBounds.getMinX();
				tmpArrY = tmpAtBounds.getHeight() / 2.0 + tmpAtBounds.getMinY();
				break;
			case BOTTOM:
				tmpWinX = tmpRelBounds.getMinX();
				tmpWinY = tmpRelBounds.getMaxY();
				tmpArrX = tmpAtBounds.getWidth() / 2.0 + tmpAtBounds.getMinX();
				tmpArrY = tmpAtBounds.getMaxY();
				break;
			case RIGHT:
				tmpWinX = tmpRelBounds.getMaxX();
				tmpWinY = tmpRelBounds.getMinY();
				tmpArrX = tmpAtBounds.getMaxX();
				tmpArrY = tmpAtBounds.getHeight() / 2.0 + tmpAtBounds.getMinY();
		}
		this.showAt(ownerNode, tmpWinX, tmpWinY, windowWidth, windowHeight, tmpArrX, tmpArrY);
	}

	/**
	 * Hides the floating window.
	 */
	public void hide() {
		this.mePop.hide();
	}

	/**
	 * Checks if the floating window is shown.
	 * @return	'true' if the floating window is shown
	 */
	public boolean isShown() {
		return this.mePop.isShowing();
	}

	/**
	 * A private method used just to organize the events handling.
	 */
	private void prepareEvents() {
		this.meWin.setOnMousePressed(ev -> { // Take the X-difference on mouse press
			this.meDifX = ev.getScreenX() - this.meLastX;
			this.meDifY = ev.getScreenY() - this.meLastY;
		});
		this.meWin.setOnMouseDragged(ev -> { // Move the floating window when dragged
			this.updatePositions(new BoundingBox(ev.getScreenX() - this.meDifX, ev.getScreenY() - this.meDifY, this.meLastWidth, this.meLastHeight));
		});
		this.mePop.getScene().addEventFilter(KeyEvent.KEY_PRESSED, ev -> {
			if (ev.getCode() == KeyCode.TAB || ev.getCode() == KeyCode.ESCAPE) {
				this.mePop.hide();
				this.mePop.getOwnerNode().fireEvent(ev.copyFor(this.mePop.getOwnerNode(), this.mePop.getOwnerNode()));
				ev.consume();
			}
			if (ev.getCode() == KeyCode.TAB || ev.getCode() == KeyCode.LEFT || ev.getCode() == KeyCode.RIGHT || (ev.getCode() == KeyCode.A && ev.isControlDown())) {
				this.meLastChain.dispatchEvent(ev.copyFor(this.mePop.getOwnerNode(), this.mePop.getOwnerNode()));	//
				this.meLastChain = this.mePop.getOwnerNode().buildEventDispatchChain(new SimpleChain());			// Had to use dispatch chain
				ev.consume();
			}
		});
	}

	/**
	 * A private method used to update the position (used by dragged event too).
	 * @param newBounds	new bounds, in screen coordinates, for the inside pane (that is shown as the window)
	 * @return			the position of the inside pane (that is shown as the window)
	 */
	private Point2D updatePositions(Bounds newBounds) {
		double tmpX = Math.min(newBounds.getMinX(), this.meLastArrowPoint.getX());				//
		double tmpY = Math.min(newBounds.getMinY(), this.meLastArrowPoint.getY());				// Maximum possible bounds so that the
		double tmpWidth = Math.max(newBounds.getMaxX(), this.meLastArrowPoint.getX()) - tmpX;	// popup window can contain all
		double tmpHeight = Math.max(newBounds.getMaxY(), this.meLastArrowPoint.getY()) - tmpY;	//
		Rectangle2D tmpVis = Screen.getScreensForRectangle(tmpX, tmpY, tmpWidth, tmpHeight).get(0).getVisualBounds();// Get Working screen visual area
		double tmpXdif = 0.0, tmpYdif = 0.0; // Helps to keep window inside screen
		if (tmpWidth > tmpVis.getWidth()) {
			tmpWidth = tmpVis.getWidth();
		}
		if (tmpHeight > tmpVis.getHeight()) {
			tmpHeight = tmpVis.getHeight();
		}
		if (tmpX < tmpVis.getMinX()) {
			tmpXdif = tmpVis.getMinX() - tmpX;
			tmpX += tmpXdif;
		}
		if (tmpY < tmpVis.getMinY()) {
			tmpYdif = tmpVis.getMinY() - tmpY;
			tmpY += tmpYdif;
		}
		if (tmpX + tmpWidth > tmpVis.getMaxX()) {
			tmpXdif = tmpVis.getMaxX() - tmpX - tmpWidth;
			tmpX += tmpXdif;
		}
		if (tmpY + tmpHeight > tmpVis.getMaxY()) {
			tmpYdif = tmpVis.getMaxY() - tmpY - tmpHeight;
			tmpY += tmpYdif;
		}
		this.mePop.setX(tmpX);			//
		this.mePop.setY(tmpY);			//
		this.mePop.setWidth(tmpWidth);	// Update popup window bounds
		this.mePop.setHeight(tmpHeight);//
		this.meLastX = newBounds.getMinX() + tmpXdif;	// For the window dragging events
		this.meLastY = newBounds.getMinY() + tmpYdif;	//
		newBounds = this.meBord.screenToLocal(newBounds);	// Update bounds based on local coordinates
		this.meWin.setLayoutX(newBounds.getMinX() + tmpXdif);	//
		this.meWin.setLayoutY(newBounds.getMinY() + tmpYdif);	//
		this.meWin.setPrefWidth(newBounds.getWidth());			// Set inside pane bounds
		this.meWin.setPrefHeight(newBounds.getHeight());		//
		this.updateArrow(); // Update arrow location/direction
		return new Point2D(tmpX, tmpY);
	}

	/**
	 * A private method used to update the shape and position of the arrow pointer.
	 */
	private void updateArrow() {
		Point2D tmpPnt = this.meBord.screenToLocal(this.meLastArrowPoint.getX(), this.meLastArrowPoint.getY());
		this.meArrow.getPoints().clear();
		this.meArrow.getPoints().add(tmpPnt.getX());	//
		this.meArrow.getPoints().add(tmpPnt.getY());	// Set tip of arrow
		double tmpRadiusLen = Math.min(this.meWin.getPrefWidth(), this.meWin.getPrefHeight()) / 3.0;// The radius of the imaginary circle that the arrow starts on
		double tmpCenLenOnX = this.meWin.getPrefWidth() / 2.0 + this.meWin.getLayoutX() - tmpPnt.getX();//Projection of center line (arrowtip-center) on X-axis
		double tmpCenLenOnY = this.meWin.getPrefHeight() / 2.0 + this.meWin.getLayoutY() - tmpPnt.getY();//Projection of center line (arrowtip-center) on Y-axis
		double tmpCenLen = Math.sqrt(tmpCenLenOnX * tmpCenLenOnX + tmpCenLenOnY * tmpCenLenOnY);// Center line length
		double tmpTanLen = Math.sqrt(tmpCenLen * tmpCenLen - tmpRadiusLen * tmpRadiusLen); // Tangent line length (arrowtip-tangentpoint)
		double tmpAng = Math.asin(tmpRadiusLen / tmpCenLen); // Angle between tmpCenLen and tmpTanLen
		double tmpComplAng = Math.atan2(tmpCenLenOnY, tmpCenLenOnX); // Compliment angle for tmpAng to X-axis (between tmpCenLen and X-axis)
		double tmpThetaPosit = tmpComplAng + tmpAng;	//
		double tmpThetaNegat = tmpComplAng - tmpAng;	// The angles between X-axis and the positive and negative tangent
		this.meArrow.getPoints().add(tmpTanLen * Math.cos(tmpThetaPosit) + tmpPnt.getX()); // Add positive tangent X
		this.meArrow.getPoints().add(tmpTanLen * Math.sin(tmpThetaPosit) + tmpPnt.getY()); // Add positive tangent Y
		this.meArrow.getPoints().add(tmpTanLen * Math.cos(tmpThetaNegat) + tmpPnt.getX()); // Add negative tangent X
		this.meArrow.getPoints().add(tmpTanLen * Math.sin(tmpThetaNegat) + tmpPnt.getY()); // Add negative tangent Y
	}
}
