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
 * Core analysis engine for the Auto Tune module.  The manager receives live
 * {@link Response} objects from the logger and generates {@link Recommendation}
 * instances describing suggested fuelling or MAF adjustments.
 */
public final class AutoTuneManager {
    private static final String MAF = "P12";
    private static final String AFR = "P58";
    private static final String AF_CORRECTION_1 = "P3";
    private static final String AF_LEARNING_1 = "P4";
    private ModificationProfile profile;
    private final List<Recommendation> recommendations = new ArrayList<Recommendation>();

    public void setProfile(ModificationProfile profile) {
        this.profile = profile;
    }

    /**
     * Clear any previously generated recommendations.
     */
    public void clear() {
        recommendations.clear();
    }

    /**
     * Process the supplied logger response and update the recommendation list.
     */
    public synchronized void process(Response response) {
        if (profile == null) {
            return;
        }
        double afr = findValue(response, AFR);
        double trim = findValue(response, AF_CORRECTION_1) + findValue(response, AF_LEARNING_1);
        double maf = findValue(response, MAF);

        if (!Double.isNaN(afr)) {
            double afrDev = afr - profile.getTargetAfr();
            if (Math.abs(afrDev) > profile.getAfrTolerance()) {
                double percent = afrDev / profile.getTargetAfr() * 100.0;
                recommendations.add(new Recommendation(
                        String.format("AFR %.2f dev %.2f -> adjust fuel %.2f%%", afr, afrDev, -percent)));
            }
        }

        if (!Double.isNaN(trim) && Math.abs(trim) > profile.getTrimThreshold()) {
            double adj = -trim;
            recommendations.add(new Recommendation(
                    String.format("MAF %.2f g/s adjust %.2f%%", maf, adj)));
        }
    }

    public synchronized List<Recommendation> getRecommendations() {
        return Collections.unmodifiableList(new ArrayList<Recommendation>(recommendations));
    }

    /**
     * Persist current recommendations to the provided file.
     */
    public void exportRecommendations(File file) throws IOException {
        FileWriter writer = new FileWriter(file);
        try {
            for (Recommendation rec : recommendations) {
                writer.write(rec.getMessage());
                writer.write("\n");
            }
        }
        finally {
            writer.close();
        }
    }

    /**
     * Apply recommendations directly to the provided ECU editor instance.  The
     * current implementation simply logs the recommendations; integration with
     * editor tables is left as future work.
     */
    public void applyToEditor(ECUEditor editor) {
        // Placeholder implementation.
        for (Recommendation rec : recommendations) {
            // In a full implementation this would update tables via the editor
            // API.  For now we simply print to standard out.
            System.out.println(rec.getMessage());
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
