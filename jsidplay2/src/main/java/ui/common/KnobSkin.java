package ui.common;

import javafx.beans.value.ChangeListener;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.scene.control.SkinBase;
import javafx.scene.control.Slider;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;

/**
 * A simple knob skin for slider.
 *
 * @author Jasper Potts (initial version)
 *
 * @author ken (behavior dependencies removed)
 *
 */
public class KnobSkin extends SkinBase<Slider> {

	private final EventHandler<KeyEvent> keyEventListener = e -> {
		if (!e.isConsumed()) {
			this.callActionForEvent(e);
		}
	};

	private double knobRadius;
	private double minAngle = -140;
	private double maxAngle = 140;
	protected double dragOffset;

	protected StackPane knob;
	private StackPane knobOverlay;
	protected StackPane knobDot;

	public KnobSkin(Slider slider) {
		super(slider);
		initialize();
	}

	private void initialize() {
		knob = new StackPane() {
			@Override
			protected void layoutChildren() {
				knobDot.setLayoutX((knob.getWidth() - knobDot.getWidth()) / 2);
				knobDot.setLayoutY(5 + knobDot.getHeight() / 2);
			}

		};
		knob.getStyleClass().setAll("knob");
		knobOverlay = new StackPane();
		knobOverlay.getStyleClass().setAll("knobOverlay");
		knobDot = new StackPane();
		knobDot.getStyleClass().setAll("knobDot");

		getChildren().setAll(knob, knobOverlay);
		knob.getChildren().add(knobDot);

		getSkinnable().addEventHandler(KeyEvent.KEY_PRESSED, this.keyEventListener);
		getSkinnable().setOnKeyPressed(ke -> getSkinnable().requestLayout());
		getSkinnable().setOnKeyReleased(ke -> getSkinnable().requestLayout());
		getSkinnable().setOnMousePressed(me -> {
			double dragStart = mouseToValue(me.getX(), me.getY());
			double zeroOneValue = (getSkinnable().getValue() - getSkinnable().getMin())
					/ (getSkinnable().getMax() - getSkinnable().getMin());
			dragOffset = zeroOneValue - dragStart;
			thumbPressed(me, dragStart);
			trackPress(me, dragStart);
			getSkinnable().requestLayout();
		});
		getSkinnable().setOnMouseReleased(me -> thumbReleased(me));
		getSkinnable().setOnMouseDragged(me -> {
			thumbDragged(me, mouseToValue(me.getX(), me.getY()) + dragOffset);
			getSkinnable().requestLayout();
		});
		getSkinnable().valueProperty().addListener(
				(ChangeListener<Number>) (observable, oldValue, newValue) -> getSkinnable().requestLayout());
	}

	@Override
	public void dispose() {
		super.dispose();
		getSkinnable().removeEventHandler(KeyEvent.ANY, this.keyEventListener);
	}

	private void callActionForEvent(KeyEvent e) {
		if (e != null && callAction(e)) {
			e.consume();
		}
	}

	private boolean callAction(KeyEvent e) {
		switch (e.getCode()) {
		case UP:
			getSkinnable().adjustValue(getSkinnable().valueProperty().get() + getSkinnable().getBlockIncrement());
			return true;
		case RIGHT:
			getSkinnable().adjustValue(getSkinnable().valueProperty().get() + getSkinnable().getMajorTickUnit());
			return true;
		case DOWN:
			getSkinnable().adjustValue(getSkinnable().valueProperty().get() - getSkinnable().getBlockIncrement());
			return true;
		case LEFT:
			getSkinnable().adjustValue(getSkinnable().valueProperty().get() - getSkinnable().getMajorTickUnit());
			return true;
		case HOME:
			getSkinnable().adjustValue(getSkinnable().getMin());
			return true;
		case END:
			getSkinnable().adjustValue(getSkinnable().getMax());
			return true;
		default:
			return false;
		}
	}

	public void thumbPressed(MouseEvent e, double position) {
		Slider slider = getSkinnable();
		if (!slider.isFocused()) {
			slider.requestFocus();
		}
		slider.setValueChanging(true);
	}

	public void trackPress(MouseEvent e, double position) {
		Slider slider = getSkinnable();
		if (!slider.isFocused()) {
			slider.requestFocus();
		}
		if (slider.getOrientation().equals(Orientation.HORIZONTAL)) {
			slider.adjustValue(position * (slider.getMax() - slider.getMin()) + slider.getMin());
		} else {
			slider.adjustValue((1.0D - position) * (slider.getMax() - slider.getMin()) + slider.getMin());
		}

	}

	public void thumbReleased(MouseEvent e) {
		Slider slider = getSkinnable();
		slider.setValueChanging(false);
		slider.adjustValue(slider.getValue());
	}

	public void thumbDragged(MouseEvent e, double position) {
		Slider slider = this.getSkinnable();
		slider.setValue(
				Math.min(Math.max(position * (slider.getMax() - slider.getMin()) + slider.getMin(), slider.getMin()),
						slider.getMax()));
	}

	private double mouseToValue(double mouseX, double mouseY) {
		double cx = getSkinnable().getWidth() / 2;
		double cy = getSkinnable().getHeight() / 2;
		double mouseAngle = Math.toDegrees(Math.atan((mouseY - cy) / (mouseX - cx)));
		double topZeroAngle;
		if (mouseX < cx) {
			topZeroAngle = 90 - mouseAngle;
		} else {
			topZeroAngle = -(90 + mouseAngle);
		}
		return 1 - (topZeroAngle - minAngle) / (maxAngle - minAngle);
	}

	private void rotateKnob() {
		Slider s = getSkinnable();
		double zeroOneValue = (s.getValue() - s.getMin()) / (s.getMax() - s.getMin());
		double angle = minAngle + (maxAngle - minAngle) * zeroOneValue;
		knob.setRotate(angle);
	}

	@Override
	protected void layoutChildren(double x, double y, double w, double h) {
		// calculate the available space
		double cx = x + w / 2;
		double cy = y + h / 2;

		// resize thumb to preferred size
		double knobWidth = knob.prefWidth(-1);
		double knobHeight = knob.prefHeight(-1);
		knobRadius = Math.max(knobWidth, knobHeight) / 2;
		knob.resize(knobWidth, knobHeight);
		knob.setLayoutX(cx - knobRadius);
		knob.setLayoutY(cy - knobRadius);
		knobOverlay.resize(knobWidth, knobHeight);
		knobOverlay.setLayoutX(cx - knobRadius);
		knobOverlay.setLayoutY(cy - knobRadius);
		rotateKnob();
	}

	@Override
	protected double computeMinWidth(double height, double topInset, double rightInset, double bottomInset,
			double leftInset) {
		return leftInset + knob.minWidth(-1) + rightInset;
	}

	@Override
	protected double computeMinHeight(double width, double topInset, double rightInset, double bottomInset,
			double leftInset) {
		return topInset + knob.minHeight(-1) + bottomInset;
	}

	@Override
	protected double computePrefWidth(double height, double topInset, double rightInset, double bottomInset,
			double leftInset) {
		return leftInset + knob.prefWidth(-1) + rightInset;
	}

	@Override
	protected double computePrefHeight(double width, double topInset, double rightInset, double bottomInset,
			double leftInset) {
		return topInset + knob.prefHeight(-1) + bottomInset;
	}

	@Override
	protected double computeMaxWidth(double height, double topInset, double rightInset, double bottomInset,
			double leftInset) {
		return Double.MAX_VALUE;
	}

	@Override
	protected double computeMaxHeight(double width, double topInset, double rightInset, double bottomInset,
			double leftInset) {
		return Double.MAX_VALUE;
	}

}
