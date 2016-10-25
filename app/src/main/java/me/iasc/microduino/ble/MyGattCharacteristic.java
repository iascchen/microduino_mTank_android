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
public class MyGattCharacteristic {
    private static HashMap<UUID, String> attributes = new HashMap<UUID, String>();

    public static final UUID MD_RX_TX = new UUID((0xFFF6L << 32) | 0x1000, GattUtils.leastSigBits);
    public static final UUID ETOH_RX_TX = new UUID((0xFFF1L << 32) | 0x1000, GattUtils.leastSigBits);

//    public static final UUID BLE_DOWNLOAD_IMAGE_NOTIFY = new UUID((0xF0C1L << 32) | 0x1000, GattUtils.leastSigBits);
//    public static final UUID BLE_DOWNLOAD_IMAGE_BLOCK = new UUID((0xF0C2L << 32) | 0x1000, GattUtils.leastSigBits);

    static {
        attributes.put(MD_RX_TX, "Microduino BLE Serial");

//        attributes.put(BLE_DOWNLOAD_IMAGE_NOTIFY, "Image Notify");
//        attributes.put(BLE_DOWNLOAD_IMAGE_BLOCK, "Image Block");
    }

    public static String lookup(UUID uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}
