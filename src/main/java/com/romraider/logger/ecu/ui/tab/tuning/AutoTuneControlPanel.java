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
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;

/**
 * Control panel for the Auto Tune tab.
 *
 * <p>Provides profile selection, recording controls, a sample counter that
 * reflects ML data accumulation, a Clear button, Export, and Apply to Editor.
 */
public final class AutoTuneControlPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final String MASS_AIR_FLOW  = "P12";
    private static final String AFR            = "P58";
    private static final String AF_CORRECTION  = "P3";
    private static final String AF_LEARNING    = "P4";
    private static final String RPM            = "P8";

    private final DataRegistrationBroker   broker;
    private final AutoTuneManager          manager;
    private final ECUEditor                ecuEditor;

    private final JToggleButton recordButton = new JToggleButton("▶  Start Recording");
    private final JLabel        sampleLabel  = new JLabel("Samples: 0");
    private final JComboBox<ModificationProfile> profileList;

    private List<EcuParameter> params;
    private List<EcuSwitch>    switches;
    private List<ExternalData> externals;

    public AutoTuneControlPanel(
            final JComponent parent,
            DataRegistrationBroker broker,
            ECUEditor ecuEditor,
            AutoTuneManager manager) {

        super(new GridBagLayout());
        this.broker    = broker;
        this.manager   = manager;
        this.ecuEditor = ecuEditor;

        setBorder(BorderFactory.createTitledBorder("Auto Tune Controls"));

        profileList = new JComboBox<ModificationProfile>(
                new ModificationProfile[]{ new ColdAirIntakeProfile() });
        profileList.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                manager.setProfile((ModificationProfile) profileList.getSelectedItem());
            }
        });
        manager.setProfile((ModificationProfile) profileList.getSelectedItem());

        int y = 0;

        // Profile selector
        add(new JLabel("Profile:"), grid(0, y, 1));
        add(profileList,            grid(1, y++, 1));

        // Sample counter (updated externally via updateSampleCount())
        add(new JLabel("ML data:"), grid(0, y, 1));
        add(sampleLabel,            grid(1, y++, 1));

        // Record toggle
        recordButton.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                if (recordButton.isSelected()) {
                    recordButton.setText("⏹  Stop Recording");
                    registerData(MASS_AIR_FLOW, AFR, AF_CORRECTION, AF_LEARNING, RPM);
                } else {
                    recordButton.setText("▶  Start Recording");
                    deregisterData(MASS_AIR_FLOW, AFR, AF_CORRECTION, AF_LEARNING, RPM);
                }
            }
        });
        add(recordButton, grid(0, y++, 2));

        // Clear button
        JButton clearButton = new JButton("Clear Data");
        clearButton.setToolTipText("Reset all logged samples and ML corrections");
        clearButton.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                manager.clear();
                updateSampleCount(0);
            }
        });
        add(clearButton, grid(0, y++, 2));

        // Export button
        JButton exportButton = new JButton("Export Recommendations…");
        exportButton.setToolTipText("Save current recommendations to a text file");
        exportButton.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = new JFileChooser();
                chooser.setDialogTitle("Export AutoTune Recommendations");
                if (chooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
                    File file = chooser.getSelectedFile();
                    try {
                        manager.exportRecommendations(file);
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(parent,
                                "Failed to export recommendations:\n" + ex.getMessage(),
                                "Export Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });
        add(exportButton, grid(0, y++, 2));

        // Apply to editor button
        JButton applyButton = new JButton("Apply to Editor");
        applyButton.setToolTipText("Send accepted recommendations to the ECU editor tables");
        applyButton.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                if (ecuEditor != null) {
                    manager.applyToEditor(ecuEditor);
                } else {
                    JOptionPane.showMessageDialog(parent,
                            "No ECU editor is open.  Open a ROM first.",
                            "Apply to Editor",
                            JOptionPane.WARNING_MESSAGE);
                }
            }
        });
        add(applyButton, grid(0, y++, 2));
    }

    /** Called by the update handler to keep the sample counter current. */
    public void updateSampleCount(final int count) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                sampleLabel.setText("Samples: " + count);
            }
        });
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

    private GridBagConstraints grid(int x, int y, int w) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx      = x;
        gbc.gridy      = y;
        gbc.gridwidth  = w;
        gbc.insets     = new Insets(3, 4, 3, 4);
        gbc.fill       = GridBagConstraints.HORIZONTAL;
        gbc.weightx    = 1.0;
        return gbc;
    }
}
