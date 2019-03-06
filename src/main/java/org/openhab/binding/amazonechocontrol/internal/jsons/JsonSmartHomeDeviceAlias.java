package org.openhab.binding.amazonechocontrol.internal.jsons;

import org.eclipse.jdt.annotation.Nullable;

public class JsonSmartHomeDeviceAlias {
    public @Nullable String friendlyName;
    public @Nullable Boolean enabled;

    public JsonSmartHomeDeviceAlias(String friendlyName, Boolean enabled) {
        this.friendlyName = friendlyName;
        this.enabled = enabled;
    }
}
