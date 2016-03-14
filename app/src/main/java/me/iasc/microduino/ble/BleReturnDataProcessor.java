package me.iasc.microduino.ble;

import me.iasc.microduino.ble.characteristics.SerialByteArrayValue;
import me.iasc.microduino.ble.characteristics.SerialStringValue;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

/**
 * Copyright (C) 2015 Iasc CHEN
 * Created on 15/3/22.
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

public class BleReturnDataProcessor {
    public final static String EXTRA_RX_TX = "ble.uploader.RX_TX";

    public final static String EXTRA_IMAGE_NOTIFY = "ble.uploader.IMAGE_NOTIFY";
    public final static String EXTRA_IMAGE_BLOCK = "ble.uploader.IMAGE_BLOCK";

    public static String getExraName(final UUID characteristicUuid) {
        String ret = null;

        // TODO: Please add your code, access more characteristics
        if (MyGattCharacteristic.BLE_DOWNLOAD_IMAGE_NOTIFY.equals(characteristicUuid)) {
            ret = EXTRA_IMAGE_NOTIFY;
        } else if (MyGattCharacteristic.BLE_DOWNLOAD_IMAGE_BLOCK.equals(characteristicUuid)) {
            ret = EXTRA_IMAGE_BLOCK;
        } else if (MyGattCharacteristic.MD_RX_TX.equals(characteristicUuid)) {
            ret = EXTRA_RX_TX;
        }

        return ret;
    }

    public static String process(final String extraName, byte[] value) {
        String ret = null;

        // TODO: Please add your code, access more characteristics
        if (EXTRA_RX_TX.equals(extraName)) {
            SerialStringValue mmv = new SerialStringValue(value);
            ret = mmv.getMessage();
        } else if (EXTRA_IMAGE_NOTIFY.equals(extraName)) {
            ByteBuffer bb = ByteBuffer.allocate(4);
            bb.order(ByteOrder.LITTLE_ENDIAN);
            bb.put(value);

            int retInt = bb.getInt(0);
            ret = String.valueOf(retInt);
        } else if (EXTRA_IMAGE_BLOCK.equals(extraName)) {
            SerialByteArrayValue mmv = new SerialByteArrayValue(value);
            ret = mmv.getMessage();
        }

        return ret;
    }
}
