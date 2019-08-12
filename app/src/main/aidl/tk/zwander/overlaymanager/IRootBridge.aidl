package tk.zwander.overlaymanager;

import android.content.om.OverlayInfo;
import java.util.List;
import java.util.Map;

interface IRootBridge {
    Map getAllOverlays();
    List getOverlayInfosForTarget(String packageName);
    OverlayInfo getOverlayInfo(String packageName);

    boolean setOverlayEnabled(in String packageName, boolean enabled);
    boolean setOverlayEnabledExclusive(in String packageName, boolean enabled);
    boolean setOverlayEnabledExclusiveInCategory(in String packageName);
    boolean setOverlayPriority(String packageName, String packageToOutrank);
    boolean setOverlayHighestPriority(String packageName);
    boolean setOverlayLowestPriority(String packageName);
}
