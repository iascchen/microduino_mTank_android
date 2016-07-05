/**
 * Copyright (C) 2015 Iasc CHEN
 * Created on 15/4/27.
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

package me.iasc.microduino.joypad;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * These commands is compatible
 */
public class JoypadTankCommand {
    private final static String TAG = JoypadTankCommand.class.getSimpleName();

    static final byte[] CMD_HEAD = {(byte)0xaa, (byte)0xbb};
    static final byte CMD_CODE = (byte) 0xc8;

    static final byte CHANNEL_COUNT = 8;
    static final byte CHANNEL_LEN = CHANNEL_COUNT * 2; // CHANNEL_COUNT * Short.SIZE / Byte.SIZE;
    static final byte CMD_LEN = 4 + CHANNEL_LEN;

    public static short[] channel = {1500, 1500, 1500, 1500, 1000, 1000, 1000, 1000};

    public synchronized static byte[] compose() {

        ByteBuffer bbuffer = ByteBuffer.allocate(CMD_LEN);
        bbuffer.order(ByteOrder.LITTLE_ENDIAN);

        bbuffer.put(CMD_HEAD);
        // bbuffer.put(CHANNEL_LEN);
        bbuffer.put(CMD_CODE);

        ByteBuffer bb = ByteBuffer.allocate(CHANNEL_LEN);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < CHANNEL_COUNT; i++) {
            bb.putShort(channel[i]);
        }
        bbuffer.put(bb.array());

        bbuffer.put(getChecksum(CHANNEL_LEN, CMD_CODE, bb.array()));

        // Log.v(TAG, "" + byteArrayToHexString(bbuffer.array()));

        return bbuffer.array();
    }

    public static byte getChecksum(byte length, byte cmd, byte mydata[]) {
        byte checksum = 0;
        checksum ^= (length & 0xFF);
        checksum ^= (cmd & 0xFF);
        for (int i = 0; i < length; i++)
            checksum ^= (mydata[i] & 0xFF);
        return checksum;
    }

    public static void resetChannel(short[] cmd) {
        for (int i = 0; i < CHANNEL_COUNT; i++) {
            channel[i] = cmd[i];
        }
    }

    public static void changeChannel(int index, int value) {
        channel[index] = (short) value;
    }

    public static String byteArrayToHexString(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for (byte b : a)
            sb.append(String.format("%02x", b & 0xff));
        return sb.toString();
    }

    public static String toHexString() {
        return byteArrayToHexString(compose());
    }
}
