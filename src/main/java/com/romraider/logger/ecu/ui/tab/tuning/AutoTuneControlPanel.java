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
import com.romraider.logger.ecu.definition.LoggerData;
import com.romraider.logger.ecu.tuning.AutoTuneManager;
import com.romraider.logger.ecu.tuning.ColdAirIntakeProfile;
import com.romraider.logger.ecu.tuning.ModificationProfile;
import com.romraider.logger.ecu.ui.DataRegistrationBroker;
import com.romraider.editor.ecu.ECUEditor;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

/**
 * Control panel for the Auto Tune tab providing profile selection and data
 * recording controls.
 */
public final class AutoTuneControlPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final String MASS_AIR_FLOW = "P12";
    private static final String AFR = "P58";
    private static final String AF_CORRECTION_1 = "P3";
    private static final String AF_LEARNING_1 = "P4";
    private static final String RPM = "P8";

    private final DataRegistrationBroker broker;
    private final AutoTuneManager manager;
    private final ECUEditor ecuEditor;
    private final JToggleButton recordButton = new JToggleButton("Record");
    private final JComboBox<ModificationProfile> profileList;
    private List<EcuParameter> params;
    private List<EcuSwitch> switches;
    private List<ExternalData> externals;

    public AutoTuneControlPanel(final JComponent parent, DataRegistrationBroker broker, ECUEditor ecuEditor, AutoTuneManager manager) {
        super(new GridBagLayout());
        this.broker = broker;
        this.manager = manager;
        this.ecuEditor = ecuEditor;

        profileList = new JComboBox<ModificationProfile>(
                new ModificationProfile[] { new ColdAirIntakeProfile() });
        profileList.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                manager.setProfile((ModificationProfile) profileList.getSelectedItem());
            }
        });
        manager.setProfile((ModificationProfile) profileList.getSelectedItem());

        int y = 0;
        add(new JLabel("Profile:"), grid(0, y, 1));
        add(profileList, grid(1, y++, 1));

        recordButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (recordButton.isSelected()) {
                    registerData(MASS_AIR_FLOW, AFR, AF_CORRECTION_1, AF_LEARNING_1, RPM);
                }
                else {
                    deregisterData(MASS_AIR_FLOW, AFR, AF_CORRECTION_1, AF_LEARNING_1, RPM);
                }
            }
        });
        add(recordButton, grid(0, y++, 2));

        JButton exportButton = new JButton("Export");
        exportButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = new JFileChooser();
                if (chooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
                    File file = chooser.getSelectedFile();
                    try {
                        manager.exportRecommendations(file);
                    }
                    catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });
        add(exportButton, grid(0, y++, 2));

        JButton applyButton = new JButton("Apply to Editor");
        applyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (ecuEditor != null) {
                    manager.applyToEditor(ecuEditor);
                }
            }
        });
        add(applyButton, grid(0, y++, 2));
    }

    private GridBagConstraints grid(int x, int y, int w) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.gridwidth = w;
        gbc.insets = new Insets(2,2,2,2);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        return gbc;
    }

    public boolean isRecordData() {
        return recordButton.isSelected();
    }

    public void setEcuParams(List<EcuParameter> params) {
        this.params = params;
    }

    public void setEcuSwitches(List<EcuSwitch> switches) {
        this.switches = switches;
    }

    public void setExternalDatas(List<ExternalData> externals) {
        this.externals = externals;
    }

    private void registerData(String... ids) {
        for (String id : ids) {
            LoggerData data = findData(id);
            if (data != null) broker.registerLoggerDataForLogging(data);
        }
    }

    private void deregisterData(String... ids) {
        for (String id : ids) {
            LoggerData data = findData(id);
            if (data != null) broker.deregisterLoggerDataFromLogging(data);
        }
    }

    private LoggerData findData(String id) {
        if (params != null) {
            for (EcuParameter p : params) {
                if (id.equals(p.getId())) return p;
            }
        }
        if (switches != null) {
            for (EcuSwitch s : switches) {
                if (id.equals(s.getId())) return s;
            }
        }
        if (externals != null) {
            for (ExternalData e : externals) {
                if (id.equals(e.getId())) return e;
            }
        }
        return null;
    }
}
