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

package com.romraider.logger.ecu.tuning;

import com.romraider.logger.ecu.comms.query.Response;
import com.romraider.logger.ecu.definition.LoggerData;
import com.romraider.editor.ecu.ECUEditor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Core analysis engine for the Auto Tune module.
 *
 * <p>On every logged {@link Response} it:
 * <ol>
 *   <li>Generates an instantaneous recommendation if AFR or fuel trim is out
 *       of range.</li>
 *   <li>Feeds the data into {@link TuneDataCollector}, which maintains
 *       per-MAF-bin Exponential Weighted Moving Averages (EWMA) and produces
 *       ML-learned correction recommendations once enough samples have
 *       accumulated.</li>
 * </ol>
 */
public final class AutoTuneManager {

    private static final String MAF           = "P12";
    private static final String AFR           = "P58";
    private static final String AF_CORRECTION = "P3";
    private static final String AF_LEARNING   = "P4";

    private ModificationProfile profile;
    private final List<Recommendation>  instantaneous = new ArrayList<Recommendation>();
    private final TuneDataCollector     collector     = new TuneDataCollector();

    public void setProfile(ModificationProfile profile) {
        this.profile = profile;
    }

    /** Clear instantaneous alerts and all ML learning data. */
    public synchronized void clear() {
        instantaneous.clear();
        collector.reset();
    }

    /** How many samples the ML collector has accumulated so far. */
    public synchronized int getTotalSamples() {
        return collector.getTotalSamples();
    }

    /**
     * Process one logger response frame.
     *
     * <p>Generates up to two instantaneous recommendations (AFR deviation,
     * fuel-trim deviation) and feeds the frame into the ML collector.
     */
    public synchronized void process(Response response) {
        if (profile == null) return;

        double afr  = findValue(response, AFR);
        double corr = findValue(response, AF_CORRECTION);
        double learn= findValue(response, AF_LEARNING);
        double maf  = findValue(response, MAF);
        double trim = (Double.isNaN(corr) ? 0 : corr) + (Double.isNaN(learn) ? 0 : learn);

        // Instantaneous AFR deviation
        if (!Double.isNaN(afr)) {
            double dev = afr - profile.getTargetAfr();
            if (Math.abs(dev) > profile.getAfrTolerance()) {
                double pct = -(dev / profile.getTargetAfr() * 100.0);
                String msg = String.format(
                        "AFR %.2f  (target %.2f, dev %+.2f) → adjust fuel %+.1f%%",
                        afr, profile.getTargetAfr(), dev, pct);
                instantaneous.add(new Recommendation(msg, Recommendation.Type.AFR));
            }
        }

        // Fuel-trim deviation (only when at least one trim channel is present)
        if (!Double.isNaN(corr) || !Double.isNaN(learn)) {
            if (Math.abs(trim) > profile.getTrimThreshold()) {
                String mafStr = Double.isNaN(maf) ? "N/A" : String.format("%.1f g/s", maf);
                String msg = String.format(
                        "Fuel trim %+.1f%%  (MAF %s) → adjust MAF scale %+.1f%%",
                        trim, mafStr, -trim);
                instantaneous.add(new Recommendation(msg, Recommendation.Type.FUEL_TRIM));
            }
        }

        // Feed into ML collector (skipped when MAF or trim not available)
        if (!Double.isNaN(maf) && (!Double.isNaN(corr) || !Double.isNaN(learn))) {
            collector.addSample(maf, trim);
        }
    }

    /**
     * Returns a snapshot combining instantaneous alerts followed by ML-learned
     * per-bin corrections.
     */
    public synchronized List<Recommendation> getRecommendations() {
        List<Recommendation> all = new ArrayList<Recommendation>(instantaneous);
        all.addAll(collector.buildLearnedRecommendations());
        return Collections.unmodifiableList(all);
    }

    /**
     * Persist the current recommendation list to {@code file}.
     * Uses try-with-resources to guarantee the writer is closed even on error.
     */
    public synchronized void exportRecommendations(File file) throws IOException {
        List<Recommendation> snapshot = getRecommendations();
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(String.format("# RomRaider AutoTune export  (%d samples)%n",
                    collector.getTotalSamples()));
            for (Recommendation rec : snapshot) {
                writer.write(String.format("[%-9s  conf=%.0f%%]  %s%n",
                        rec.getType(), rec.getConfidence() * 100, rec.getMessage()));
            }
        }
    }

    /**
     * Apply recommendations to the provided ECU editor.
     * Currently logs to stdout; full table-write integration is future work.
     */
    public void applyToEditor(ECUEditor editor) {
        for (Recommendation rec : getRecommendations()) {
            System.out.println("[AutoTune] " + rec.getMessage());
        }
    }

    private double findValue(Response response, String id) {
        Set<LoggerData> datas = response.getData();
        for (LoggerData data : datas) {
            if (id.equals(data.getId())) {
                return response.getDataValue(data);
            }
        }
        return Double.NaN;
    }
}
