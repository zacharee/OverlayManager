package tk.zwander.overlaymanager;

import java.util.Map;

interface IRootBridge {
    Map getAllOverlays();

    boolean setOverlayEnabled(in String packageName, boolean enabled);
    boolean setOverlayEnabledExclusive(in String packageName, boolean enabled);
    boolean setOverlayEnabledExclusiveInCategory(in String packageName);
    boolean setOverlayPriority(String packageName, String packageToOutrank);
    boolean setOverlayHighestPriority(String packageName);
    boolean setOverlayLowestPriority(String packageName);
}
