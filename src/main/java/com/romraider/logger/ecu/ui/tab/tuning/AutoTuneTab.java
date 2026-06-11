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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

/**
 * User interface tab that displays Auto Tune recommendations.
 *
 * <p>Instantaneous AFR alerts are shown in orange, fuel-trim alerts in red,
 * and ML-learned corrections (with confidence) in blue.  A monospaced font
 * makes columnar output easy to read.
 */
public final class AutoTuneTab extends JPanel implements Tab {

    private static final long serialVersionUID = 1L;

    private static final Color COLOR_AFR        = new Color(0xCC, 0x66, 0x00);
    private static final Color COLOR_FUEL_TRIM  = new Color(0xCC, 0x00, 0x00);
    private static final Color COLOR_LEARNED    = new Color(0x00, 0x55, 0xAA);
    private static final Color COLOR_HEADER     = new Color(0x33, 0x33, 0x33);

    private final AutoTuneControlPanel controlPanel;
    private final JTextPane            outputPane   = new JTextPane();
    private final StyledDocument       doc;

    // Named styles
    private final Style styleAfr;
    private final Style styleTrim;
    private final Style styleLearned;
    private final Style styleHeader;

    public AutoTuneTab(DataRegistrationBroker broker, ECUEditor ecuEditor, AutoTuneManager manager) {
        super(new BorderLayout(4, 4));
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        controlPanel = new AutoTuneControlPanel(this, broker, ecuEditor, manager);

        JScrollPane ctrlScroll = new JScrollPane(controlPanel,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        ctrlScroll.setPreferredSize(new java.awt.Dimension(240, 0));
        add(ctrlScroll, BorderLayout.WEST);

        outputPane.setEditable(false);
        outputPane.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        outputPane.setBackground(new Color(0xF8, 0xF8, 0xF8));
        doc = outputPane.getStyledDocument();

        StyleContext sc = StyleContext.getDefaultStyleContext();
        Style base = sc.getStyle(StyleContext.DEFAULT_STYLE);

        styleAfr     = doc.addStyle("afr",     base);
        styleTrim    = doc.addStyle("trim",    base);
        styleLearned = doc.addStyle("learned", base);
        styleHeader  = doc.addStyle("header",  base);

        StyleConstants.setForeground(styleAfr,     COLOR_AFR);
        StyleConstants.setForeground(styleTrim,    COLOR_FUEL_TRIM);
        StyleConstants.setForeground(styleLearned, COLOR_LEARNED);
        StyleConstants.setForeground(styleHeader,  COLOR_HEADER);
        StyleConstants.setBold(styleHeader, true);

        JScrollPane outScroll = new JScrollPane(outputPane,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        outScroll.setBorder(BorderFactory.createTitledBorder("Recommendations"));
        add(outScroll, BorderLayout.CENTER);
    }

    @Override public JPanel  getPanel()      { return this; }
    @Override public boolean isRecordData()  { return controlPanel.isRecordData(); }
    @Override public void    addData(double x, double y) { /* not used for XY plotting */ }

    /**
     * Refresh the recommendation display and sample counter.
     * Must be called on the EDT.
     */
    public void displayRecommendations(List<Recommendation> recs, int totalSamples) {
        try {
            doc.remove(0, doc.getLength());
        } catch (BadLocationException ignored) {}

        controlPanel.updateSampleCount(totalSamples);

        if (recs.isEmpty()) {
            appendStyled("No recommendations yet.  Start recording to collect data.\n", styleHeader);
            return;
        }

        boolean hasInstant  = false;
        boolean hasLearned  = false;
        for (Recommendation r : recs) {
            if (r.getType() == Recommendation.Type.LEARNED) hasLearned = true;
            else hasInstant = true;
        }

        if (hasInstant) {
            appendStyled("── Instantaneous alerts ──────────────────────────────────\n", styleHeader);
            for (Recommendation r : recs) {
                if (r.getType() == Recommendation.Type.LEARNED) continue;
                Style s = r.getType() == Recommendation.Type.AFR ? styleAfr : styleTrim;
                appendStyled("  " + r.getMessage() + "\n", s);
            }
        }

        if (hasLearned) {
            appendStyled("\n── ML-learned corrections  (EWMA per MAF bin) ────────────\n", styleHeader);
            for (Recommendation r : recs) {
                if (r.getType() != Recommendation.Type.LEARNED) continue;
                int bar = confidenceBar(r.getConfidence());
                String line = String.format("  %s  [conf %3.0f%%  %s]%n",
                        r.getMessage(), r.getConfidence() * 100,
                        "█".repeat(bar) + "░".repeat(10 - bar));
                appendStyled(line, styleLearned);
            }
        }
    }

    /** @deprecated Use {@link #displayRecommendations(List, int)} instead. */
    @Deprecated
    public void displayRecommendations(List<Recommendation> recs) {
        displayRecommendations(recs, 0);
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

    private void appendStyled(String text, Style style) {
        try {
            doc.insertString(doc.getLength(), text, style);
        } catch (BadLocationException ignored) {}
    }

    private static int confidenceBar(double confidence) {
        return (int) Math.round(confidence * 10);
    }
}
