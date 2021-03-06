/**
 * Copyright (C) 2015 Iasc CHEN
 * Created on 15/3/19.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.iasc.microduino.ble;

import java.util.HashMap;
import java.util.UUID;

/**
 * This class includes a small subset of standard GATT attributes for demonstration purposes.
 */
public class MyGattService {
    private static HashMap<UUID, String> attributes = new HashMap<UUID, String>();

    public static final UUID SOFT_SERIAL_SERVICE = new UUID((0xFFF0L << 32) | 0x1000, GattUtils.leastSigBits);
//    public static final UUID BLE_DOWNLOAD_SERVICE = new UUID((0xFFE0L << 32) | 0x1000, GattUtils.leastSigBits);

    static {
        attributes.put(SOFT_SERIAL_SERVICE, "Microduino BLE Serial");
//        attributes.put(BLE_DOWNLOAD_SERVICE, "Microduino BLE Uploader");
    }

    public static String lookup(UUID uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}
