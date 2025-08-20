/*
 * RomRaider Open-Source Tuning, Logging and Reflashing
 * Copyright (C) 2006-2024 RomRaider.com
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.romraider.logger.ecu.ui.handler.tuning;

import com.romraider.logger.ecu.comms.query.Response;
import com.romraider.logger.ecu.definition.LoggerData;
import com.romraider.logger.ecu.tuning.AutoTuneManager;
import com.romraider.logger.ecu.tuning.Recommendation;
import com.romraider.logger.ecu.ui.handler.DataUpdateHandler;
import com.romraider.logger.ecu.ui.tab.tuning.AutoTuneTab;
import java.util.List;
import javax.swing.SwingUtilities;

/**
 * {@link DataUpdateHandler} implementation that feeds logger responses into the
 * {@link AutoTuneManager} and updates the {@link AutoTuneTab} UI with generated
 * recommendations.
 */
public final class AutoTuneUpdateHandler implements DataUpdateHandler {
    private final AutoTuneManager manager;
    private AutoTuneTab autoTuneTab;

    public AutoTuneUpdateHandler(AutoTuneManager manager) {
        this.manager = manager;
    }

    @Override
    public synchronized void registerData(LoggerData loggerData) {
    }

    @Override
    public synchronized void handleDataUpdate(Response response) {
        if (autoTuneTab != null && autoTuneTab.isRecordData()) {
            manager.process(response);
            final List<Recommendation> recs = manager.getRecommendations();
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    autoTuneTab.displayRecommendations(recs);
                }
            });
        }
    }

    @Override
    public synchronized void deregisterData(LoggerData loggerData) {
    }

    @Override
    public synchronized void cleanUp() {
    }

    @Override
    public synchronized void reset() {
        manager.clear();
    }

    public void setAutoTuneTab(AutoTuneTab autoTuneTab) {
        this.autoTuneTab = autoTuneTab;
    }
}
