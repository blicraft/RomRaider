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

package com.romraider.util;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import com.romraider.Settings;
import com.romraider.swing.JProgressPane;
import com.romraider.xml.DOMSettingsBuilder;
import com.romraider.xml.DOMSettingsUnmarshaller;

public class SettingsPersistenceTest {

    @Test
    public void tabWarningThresholdsPersist() throws Exception {
        Settings settings = new Settings();
        Map<String, Integer> thresholds = new HashMap<String, Integer>();
        thresholds.put("Data", 90);
        thresholds.put("Graph", 80);
        settings.setTabWarningThresholds(thresholds);

        File file = File.createTempFile("settings", ".xml");
        new DOMSettingsBuilder().buildSettings(settings, file, new JProgressPane(), "test");

        InputSource src = new InputSource(new FileInputStream(file));
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.parse(src);
        Settings loaded = new DOMSettingsUnmarshaller().unmarshallSettings(doc.getDocumentElement());

        assertEquals(thresholds, loaded.getTabWarningThresholds());
        file.delete();
    }
}

