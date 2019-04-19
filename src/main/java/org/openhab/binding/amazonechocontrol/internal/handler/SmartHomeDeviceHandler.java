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
    private boolean updateStartCommand = true;

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
                                connection.smartHomeCommand(entry.getValue(), DEVICE_TURN_ON, null);
                            } else {
                                connection.smartHomeCommand(entry.getValue(), DEVICE_TURN_OFF, null);
                            }
                        } else if (entry.getKey().contains(DEVICE_PROPERTY_LIGHT_ENTITY_ID) && props.size() == 1) {
                            if (command.equals(OnOffType.ON)) {
                                connection.smartHomeCommand(entityId, DEVICE_TURN_ON, null);
                            } else {
                                connection.smartHomeCommand(entityId, DEVICE_TURN_OFF, null);
                            }
                        }
                    }
                }
            }
            if (channelId.equals(CHANNEL_LIGHT_COLOR)) {
                if (command instanceof StringType) {
                    String commandText = ((StringType) command).toFullString();
                    if (StringUtils.isNotEmpty(commandText)) {
                        updateStartCommand = true;
                        connection = accountHandler.findConnection();
                        for (Map.Entry<String, String> entry : props.entrySet()) {
                            if (entry.getKey().contains(DEVICE_PROPERTY_LIGHT_SUBDEVICE)) {
                                connection.smartHomeCommand(entry.getValue(), "setColor", commandText);
                            } else if (entry.getKey().contains(DEVICE_PROPERTY_LIGHT_ENTITY_ID) && props.size() == 1) {
                                connection.smartHomeCommand(entityId, "setColor", commandText);
                            }
                        }
                    }
                }
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
