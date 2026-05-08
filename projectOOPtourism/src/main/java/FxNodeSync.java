import java.util.List;

import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.layout.Pane;

public final class FxNodeSync {
    private FxNodeSync() {
    }

    public static void syncChildren(Pane container, List<Node> desiredChildren) {
        ObservableList<Node> children = container.getChildren();

        for (int targetIndex = 0; targetIndex < desiredChildren.size(); targetIndex++) {
            Node desired = desiredChildren.get(targetIndex);
            if (targetIndex < children.size() && children.get(targetIndex) == desired) {
                continue;
            }

            int currentIndex = children.indexOf(desired);
            if (currentIndex >= 0) {
                children.remove(currentIndex);
                children.add(targetIndex, desired);
            } else {
                children.add(targetIndex, desired);
            }
        }

        while (children.size() > desiredChildren.size()) {
            children.remove(children.size() - 1);
        }
    }
}
