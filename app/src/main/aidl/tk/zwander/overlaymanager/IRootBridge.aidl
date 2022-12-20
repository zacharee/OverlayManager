package tk.zwander.overlaymanager;

import java.util.List;
import java.util.Map;
import tk.zwander.overlaymanager.proxy.OverlayInfo;

interface IRootBridge {
     void destroy() = 16777114;

    Map getAllOverlays() = 1;
    List getOverlayInfosForTarget(in String packageName) = 2;
    OverlayInfo getOverlayInfo(in String packageName) = 3;
    OverlayInfo getOverlayInfoByIdentifier(in String identifier) = 11;

    boolean setOverlayEnabled(in String packageName, boolean enabled) = 4;
    void setOverlayEnabledByIdentifier(in String identifier, boolean enabled) = 5;
    boolean setOverlayEnabledExclusive(in String packageName, boolean enabled) = 6;
    boolean setOverlayEnabledExclusiveInCategory(in String packageName) = 7;
    boolean setOverlayPriority(in String packageName, in String packageToOutrank) = 8;
    boolean setOverlayHighestPriority(in String packageName) = 9;
    boolean setOverlayLowestPriority(in String packageName) = 10;
    void clearCache(in String packageName) = 12;
}
