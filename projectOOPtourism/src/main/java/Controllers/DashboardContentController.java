package Controllers;

import Utils.AppContext;

public interface DashboardContentController {
    void initData(AppContext mainApp, Object data);

    default void onRemoved() {
    }
}
