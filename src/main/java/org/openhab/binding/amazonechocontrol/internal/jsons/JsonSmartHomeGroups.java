package org.openhab.binding.amazonechocontrol.internal.jsons;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.amazonechocontrol.internal.jsons.JsonSmartHomeGroupIdentifiers.SmartHomeGroupIdentifier;

@NonNullByDefault
public class JsonSmartHomeGroups {

    public class SmartHomeGroup {
        public @Nullable String applianceGroupName;
        public @Nullable Boolean isSpace;
        public @Nullable Boolean space;
        public @Nullable SmartHomeGroupIdentifier applianceGroupIdentifier;
        public @Nullable Boolean brightness = false;
        public @Nullable boolean color = false;
        public @Nullable boolean colorTemperature = false;
    }

    public @Nullable SmartHomeGroup @Nullable [] groups;
}
