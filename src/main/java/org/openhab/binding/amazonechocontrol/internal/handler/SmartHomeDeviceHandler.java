/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.amazonechocontrol.internal.handler;

import static org.openhab.binding.amazonechocontrol.internal.AmazonEchoControlBindingConstants.*;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.storage.Storage;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.amazonechocontrol.internal.Connection;
import org.openhab.binding.amazonechocontrol.internal.jsons.JsonSmartHomeDevices.SmartHomeDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Lukas Knoeller
 *
 */

public class SmartHomeDeviceHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(SmartHomeDeviceHandler.class);

    private @Nullable ScheduledFuture<?> updateStateJob;
    private @Nullable Connection connection;
    private @Nullable SmartHomeDevice smartHomeDevice;

    Storage<String> stateStorage;

    @Nullable
    AccountHandler accountHandler;
    Thing thing;

    public SmartHomeDeviceHandler(Thing thing, Storage<String> storage) {
        super(thing);
        this.thing = thing;
        this.stateStorage = storage;
    }

    public @Nullable AccountHandler findAccountHandler() {
        return this.accountHandler;
    }

    public @Nullable SmartHomeDevice findSmartHomeDevice() {
        return this.smartHomeDevice;
    }

    @Override
    public void initialize() {
        logger.info("{} initialized", getClass().getSimpleName());
        Bridge bridge = this.getBridge();
        if (bridge != null) {
            AccountHandler account = (AccountHandler) bridge.getHandler();
            this.accountHandler = account;
            if (account != null) {
                account.addSmartHomeDeviceHandler(this);
                updateStatus(ThingStatus.ONLINE);
            }
        }
    }

    @Override
    public void dispose() {
        ScheduledFuture<?> updateStateJob = this.updateStateJob;
        this.updateStateJob = null;
        if (updateStateJob != null) {
            updateStateJob.cancel(false);
        }
        super.dispose();
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        AccountHandler accountHandler = this.accountHandler;
        if (accountHandler == null) {
            return;
        }
        int waitForUpdate = -1;

        ScheduledFuture<?> updateStateJob = this.updateStateJob;
        this.updateStateJob = null;
        if (updateStateJob != null) {
            updateStateJob.cancel(false);
        }
        try {
            Map<String, String> props = this.thing.getProperties();
            String entityId = props.get(DEVICE_PROPERTY_LIGHT_ENTITY_ID);
            String channelId = channelUID.getId();
            if (command instanceof RefreshType) {
                waitForUpdate = 0;
            }
            if (channelId.equals(CHANNEL_LIGHT_STATE)) {
                if (command instanceof OnOffType) {
                    connection = accountHandler.findConnection();
                    for (Map.Entry<String, String> entry : props.entrySet()) {
                        if (entry.getKey().contains(DEVICE_PROPERTY_LIGHT_SUBDEVICE)) {
                            if (command.equals(OnOffType.ON)) {
                                connection.smartHomeCommand(entry.getValue(), DEVICE_TURN_ON, null, 0.00);
                            } else {
                                connection.smartHomeCommand(entry.getValue(), DEVICE_TURN_OFF, null, 0.00);
                            }
                        } else if (entry.getKey().contains(DEVICE_PROPERTY_LIGHT_ENTITY_ID)
                                && !entry.getKey().contains(DEVICE_PROPERTY_LIGHT_SUBDEVICE)) {
                            if (command.equals(OnOffType.ON)) {
                                connection.smartHomeCommand(entityId, DEVICE_TURN_ON, null, 0.00);
                            } else {
                                connection.smartHomeCommand(entityId, DEVICE_TURN_OFF, null, 0.00);
                            }
                        }
                    }
                    waitForUpdate = 1;
                }
            }
            if (channelId.equals(CHANNEL_LIGHT_COLOR)) {
                if (command instanceof StringType) {
                    String commandText = ((StringType) command).toFullString();
                    if (StringUtils.isNotEmpty(commandText)) {
                        connection = accountHandler.findConnection();
                        for (Map.Entry<String, String> entry : props.entrySet()) {
                            if (entry.getKey().contains(DEVICE_PROPERTY_LIGHT_SUBDEVICE)) {
                                connection.smartHomeCommand(entry.getValue(), "setColor", commandText, 0.00);
                            } else if (entry.getKey().contains(DEVICE_PROPERTY_LIGHT_ENTITY_ID)
                                    && !entry.getKey().contains(DEVICE_PROPERTY_LIGHT_SUBDEVICE)) {
                                connection.smartHomeCommand(entityId, "setColor", commandText, 0.00);
                            }
                        }
                    }
                }
            }
            if (channelId.equals(CHANNEL_LIGHT_WHITE_TEMPERATURE)) {
                if (command instanceof StringType) {
                    String commandText = ((StringType) command).toFullString();
                    if (StringUtils.isNotEmpty(commandText)) {
                        connection = accountHandler.findConnection();
                        for (Map.Entry<String, String> entry : props.entrySet()) {
                            if (entry.getKey().contains(DEVICE_PROPERTY_LIGHT_SUBDEVICE)) {
                                connection.smartHomeCommand(entry.getValue(), "setColorTemperature", commandText, 0.00);
                            } else if (entry.getKey().contains(DEVICE_PROPERTY_LIGHT_ENTITY_ID)
                                    && !entry.getKey().contains(DEVICE_PROPERTY_LIGHT_SUBDEVICE)) {
                                connection.smartHomeCommand(entityId, "setColorTemperature", commandText, 0.00);
                            }
                        }
                    }
                }
            }
            if (channelId.equals(CHANNEL_LIGHT_BRIGHTNESS)) {
                if (command instanceof PercentType) {
                    connection = accountHandler.findConnection();
                    for (Map.Entry<String, String> entry : props.entrySet()) {
                        if (entry.getKey().contains(DEVICE_PROPERTY_LIGHT_SUBDEVICE)) {
                            connection.smartHomeCommand(entry.getValue(), "setBrightness", null,
                                    ((PercentType) command).floatValue() / 100);
                        } else if (entry.getKey().contains(DEVICE_PROPERTY_LIGHT_ENTITY_ID)
                                && !entry.getKey().contains(DEVICE_PROPERTY_LIGHT_SUBDEVICE)) {
                            connection.smartHomeCommand(entityId, "setBrightness", null,
                                    ((PercentType) command).floatValue() / 100);
                        }
                    }
                    waitForUpdate = 1;
                }
            }

            if (waitForUpdate < 0) {
                return;
            }

            Runnable doRefresh = () -> {
                String state = "";
                try {
                    if (!this.thing.getProperties().containsKey(DEVICE_PROPERTY_LIGHT_SUBDEVICE + "0")) {
                        state = connection.getBulbState(this.thing);
                    } else {
                        state = connection.getLightGroupState(this.thing);
                    }
                } catch (IOException e) {
                    logger.error(e.getMessage());
                } catch (URISyntaxException e) {
                    logger.error(e.getMessage());
                }
                updateState(this.thing.getChannel(CHANNEL_LIGHT_STATE).getUID(), state);

                int brightness = -1;
                try {
                    if (!this.thing.getProperties().containsKey(DEVICE_PROPERTY_LIGHT_SUBDEVICE + "0")) {
                        brightness = connection.getBulbBrightness(this.thing);
                    } else {
                        brightness = connection.getLightGroupBrightness(this.thing);
                    }
                } catch (IOException e) {
                    logger.error(e.getMessage());
                } catch (URISyntaxException e) {
                    logger.error(e.getMessage());
                }
                updateBrightness(this.thing.getChannel(CHANNEL_LIGHT_BRIGHTNESS).getUID(), brightness);
            };

            if (command instanceof RefreshType) {
                waitForUpdate = 0;
            }

            if (waitForUpdate == 0) {
                doRefresh.run();
            } else {
                this.updateStateJob = scheduler.schedule(doRefresh, waitForUpdate, TimeUnit.MILLISECONDS);
            }
        } catch (Exception e) {
            logger.warn("Handle command failed {}", e);
        }
        if (waitForUpdate >= 0) {
            this.updateStateJob = scheduler.schedule(() -> accountHandler.updateFlashBriefingHandlers(), waitForUpdate,
                    TimeUnit.MILLISECONDS);
        }

        logger.trace("Command {} received from channel '{}'", command, channelUID);
        if (command instanceof RefreshType) {
            updateSmartHomeDevices();
        }
    }

    public void updateState(ChannelUID channelUID, String command) {
        if (command.equals("ON")) {
            updateState(channelUID, OnOffType.ON);
        } else if (command.equals("OFF")) {
            updateState(channelUID, OnOffType.OFF);
        }
    }

    public void updateBrightness(ChannelUID channelUID, int brightness) {
        updateState(channelUID, new PercentType(brightness));
    }

    public boolean initialize(AccountHandler handler) {
        updateState(CHANNEL_LIGHT_STATE, OnOffType.OFF);
        if (this.accountHandler != handler) {
            this.accountHandler = handler;
        }
        return true;
    }

    public List<SmartHomeDevice> updateSmartHomeDevices() {
        Connection currentConnection = connection;
        if (currentConnection == null) {
            return new ArrayList<SmartHomeDevice>();
        }

        List<SmartHomeDevice> smartHomeDevices = null;
        try {
            if (currentConnection.getIsLoggedIn()) {
                smartHomeDevices = currentConnection.getSmarthomeDeviceList();
            }
        } catch (IOException | URISyntaxException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getLocalizedMessage());
        }

        if (smartHomeDevices != null) {
            return smartHomeDevices;
        }

        return new ArrayList<SmartHomeDevice>();
    }

}
