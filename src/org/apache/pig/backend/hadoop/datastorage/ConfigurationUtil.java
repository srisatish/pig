/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.pig.backend.hadoop.datastorage;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import org.apache.hadoop.conf.Configuration;
import org.apache.pig.backend.hadoop.executionengine.util.MapRedUtil;

public class ConfigurationUtil {

    public static Configuration toConfiguration(Properties properties) {
        assert properties != null;
        final Configuration config = new Configuration(false);
        final Enumeration<Object> iter = properties.keys();
        while (iter.hasMoreElements()) {
            final String key = (String) iter.nextElement();
            final String val = properties.getProperty(key);
            config.set(key, val);
        }
        return config;
    }

    public static Properties toProperties(Configuration configuration) {
        Properties properties = new Properties();
        assert configuration != null;
        Iterator<Map.Entry<String, String>> iter = configuration.iterator();
        while (iter.hasNext()) {
            Map.Entry<String, String> entry = iter.next();
            properties.put(entry.getKey(), entry.getValue());
        }
        return properties;
    }

    /**
     * @param origConf
     * @param replaceConf
     */
    public static void mergeConf(Configuration origConf,
            Configuration replaceConf) {
        for (Entry<String, String> entry : replaceConf) {
            origConf.set(entry.getKey(), entry.getValue());
        }
        
    }
    
    public static Properties getLocalFSProperties() {
        Configuration localConf = new Configuration(false);
        localConf.addResource("core-default.xml");
        Properties props = ConfigurationUtil.toProperties(localConf);
        props.setProperty(MapRedUtil.FILE_SYSTEM_NAME, "file:///");
        return props;
    }
}
