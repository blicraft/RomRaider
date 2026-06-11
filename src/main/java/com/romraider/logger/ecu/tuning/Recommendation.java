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

/**
 * Describes a single suggested change to an ECU table or MAF scale.
 *
 * <p>Every recommendation carries a {@link Type} so the UI can colour-code
 * entries, and a {@code confidence} value in [0, 1] indicating how much
 * logged data backs the suggestion (1.0 = fully confident).
 */
public final class Recommendation {

    public enum Type {
        /** Instantaneous AFR deviation detected. */
        AFR,
        /** Short/long-term fuel trim outside threshold. */
        FUEL_TRIM,
        /** ML-learned per-bin MAF correction from accumulated samples. */
        LEARNED
    }

    private final String message;
    private final Type   type;
    private final double confidence;

    public Recommendation(String message, Type type, double confidence) {
        this.message    = message;
        this.type       = type;
        this.confidence = Math.max(0.0, Math.min(1.0, confidence));
    }

    /** Convenience constructor for instantaneous (non-learned) alerts. */
    public Recommendation(String message, Type type) {
        this(message, type, 1.0);
    }

    public String getMessage()    { return message; }
    public Type   getType()       { return type; }
    /** Confidence in [0, 1]. */
    public double getConfidence() { return confidence; }

    @Override
    public String toString() { return message; }
}
