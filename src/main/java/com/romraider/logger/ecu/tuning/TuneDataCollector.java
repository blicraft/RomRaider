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

import java.util.ArrayList;
import java.util.List;

/**
 * Accumulates logged (MAF, fuel-trim) samples and learns per-bin MAF
 * correction values using an Exponential Weighted Moving Average (EWMA).
 *
 * <p>The MAF range 0–{@value #MAF_MAX_G_S} g/s is divided into
 * {@value #BIN_COUNT} equal bins.  For each bin the EWMA smoothing factor
 * {@code alpha = 0.15} means older samples decay gradually, giving recent
 * driving conditions more weight while still retaining long-run trends.
 *
 * <p>A bin requires at least {@value #MIN_SAMPLES_FOR_CONFIDENCE} samples
 * before it is considered confident enough to emit a recommendation.
 */
public final class TuneDataCollector {

    public static final int BIN_COUNT = 12;
    public static final double MAF_MAX_G_S = 300.0;

    /** Minimum samples before a bin's correction is treated as reliable. */
    public static final int MIN_SAMPLES_FOR_CONFIDENCE = 5;

    /** EWMA smoothing factor.  Higher → faster adaptation, more noise. */
    private static final double ALPHA = 0.15;

    /** Learned correction (%) per MAF bin – positive means add fuel. */
    private final double[] correction = new double[BIN_COUNT];
    private final int[]    sampleCount = new int[BIN_COUNT];

    /** Accept one data point from the logger. */
    public synchronized void addSample(double mafGs, double trimPercent) {
        if (Double.isNaN(mafGs) || Double.isNaN(trimPercent) || mafGs < 0) {
            return;
        }
        int bin = mafBin(mafGs);
        double desired = -trimPercent; // opposite of trim → correction to apply
        if (sampleCount[bin] == 0) {
            correction[bin] = desired;
        } else {
            correction[bin] = (1.0 - ALPHA) * correction[bin] + ALPHA * desired;
        }
        sampleCount[bin]++;
    }

    /** Reset all accumulated learning data. */
    public synchronized void reset() {
        for (int i = 0; i < BIN_COUNT; i++) {
            correction[i] = 0.0;
            sampleCount[i] = 0;
        }
    }

    /** Total samples collected across all bins. */
    public synchronized int getTotalSamples() {
        int total = 0;
        for (int c : sampleCount) total += c;
        return total;
    }

    /**
     * Build recommendations for every bin that has enough samples.
     * Confidence is clamped to [0, 1] and grows as sample count rises.
     */
    public synchronized List<Recommendation> buildLearnedRecommendations() {
        List<Recommendation> recs = new ArrayList<Recommendation>();
        for (int i = 0; i < BIN_COUNT; i++) {
            if (sampleCount[i] < MIN_SAMPLES_FOR_CONFIDENCE) continue;
            if (Math.abs(correction[i]) < 0.5) continue; // below noise floor

            double binLo = i * MAF_MAX_G_S / BIN_COUNT;
            double binHi = (i + 1) * MAF_MAX_G_S / BIN_COUNT;
            double confidence = Math.min(1.0, (double) sampleCount[i] / 30.0);
            String msg = String.format(
                    "[ML] MAF %.0f–%.0f g/s  correction %+.1f%%  (%d samples)",
                    binLo, binHi, correction[i], sampleCount[i]);
            recs.add(new Recommendation(msg, Recommendation.Type.LEARNED, confidence));
        }
        return recs;
    }

    /** Snapshot of per-bin corrections for external display or export. */
    public synchronized double[] getCorrections() {
        return correction.clone();
    }

    /** Snapshot of per-bin sample counts. */
    public synchronized int[] getSampleCounts() {
        return sampleCount.clone();
    }

    private static int mafBin(double mafGs) {
        int bin = (int) (mafGs / MAF_MAX_G_S * BIN_COUNT);
        return Math.min(bin, BIN_COUNT - 1);
    }
}
