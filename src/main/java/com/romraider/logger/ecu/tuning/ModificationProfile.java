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
 * Describes the configuration parameters required for a given hardware
 * modification.  Implementations supply target AFR values and acceptable
 * thresholds used during analysis.
 */
public abstract class ModificationProfile {
    public abstract String getName();

    /** Target stoichiometric AFR for the configuration. */
    public abstract double getTargetAfr();

    /** Allowed AFR deviation before a recommendation is generated. */
    public abstract double getAfrTolerance();

    /** Absolute fuel trim percentage beyond which an adjustment is suggested. */
    public abstract double getTrimThreshold();

    @Override
    public String toString() {
        return getName();
    }
}
