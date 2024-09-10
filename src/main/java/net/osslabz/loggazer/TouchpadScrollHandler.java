package net.osslabz.loggazer;

import javafx.scene.input.ScrollEvent;
import javafx.scene.input.TouchEvent;
import javafx.scene.input.TouchPoint;

public class TouchpadScrollHandler {
    private double previousY = 0;

    public void handleScrollEvent(ScrollEvent event) {
        // Check if the scroll event is triggered by a touchpad
        if (event.getSource() instanceof TouchEvent) {
            TouchEvent touchEvent = (TouchEvent) event.getSource();
            for (TouchPoint touchPoint : touchEvent.getTouchPoints()) {
                // Calculate the vertical scroll delta
                double verticalDelta = touchPoint.getY() - previousY;
                previousY = touchPoint.getY();
                
                // If the vertical delta is non-zero, consume the event to prevent vertical scrolling
                if (Math.abs(verticalDelta) > 0.1) {
                    event.consume();
                }
            }
        }
    }
}