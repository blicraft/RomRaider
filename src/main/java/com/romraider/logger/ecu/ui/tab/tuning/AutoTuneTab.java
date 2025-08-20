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

package com.romraider.logger.ecu.ui.tab.tuning;

import com.romraider.logger.ecu.definition.EcuParameter;
import com.romraider.logger.ecu.definition.EcuSwitch;
import com.romraider.logger.ecu.definition.ExternalData;
import com.romraider.editor.ecu.ECUEditor;
import com.romraider.logger.ecu.tuning.AutoTuneManager;
import com.romraider.logger.ecu.tuning.Recommendation;
import com.romraider.logger.ecu.ui.DataRegistrationBroker;
import com.romraider.logger.ecu.ui.tab.Tab;
import static java.awt.BorderLayout.CENTER;
import static java.awt.BorderLayout.WEST;
import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER;
import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS;
import java.awt.BorderLayout;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * User interface tab that displays Auto Tune recommendations and allows the
 * selection of modification profiles.
 */
public final class AutoTuneTab extends JPanel implements Tab {
    private static final long serialVersionUID = 1L;
    private final AutoTuneControlPanel controlPanel;
    private final JTextArea recommendations = new JTextArea();

    public AutoTuneTab(DataRegistrationBroker broker, ECUEditor ecuEditor, AutoTuneManager manager) {
        super(new BorderLayout(2, 2));
        controlPanel = new AutoTuneControlPanel(this, broker, ecuEditor, manager);
        JScrollPane scrollPane = new JScrollPane(controlPanel,
                VERTICAL_SCROLLBAR_ALWAYS, HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane, WEST);
        recommendations.setEditable(false);
        add(new JScrollPane(recommendations), CENTER);
    }

    @Override
    public JPanel getPanel() {
        return this;
    }

    @Override
    public boolean isRecordData() {
        return controlPanel.isRecordData();
    }

    @Override
    public void addData(double xData, double yData) {
        // Auto Tune does not plot XY data, method provided to satisfy interface.
    }

    public void displayRecommendations(List<Recommendation> recs) {
        StringBuilder sb = new StringBuilder();
        for (Recommendation rec : recs) {
            sb.append(rec.getMessage()).append('\n');
        }
        recommendations.setText(sb.toString());
    }

    public void setEcuParams(List<EcuParameter> params) {
        controlPanel.setEcuParams(params);
    }

    public void setEcuSwitches(List<EcuSwitch> switches) {
        controlPanel.setEcuSwitches(switches);
    }

    public void setExternalDatas(List<ExternalData> external) {
        controlPanel.setExternalDatas(external);
    }
}
