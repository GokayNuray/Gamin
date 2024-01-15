package com.example.gamin.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.IOException;

@SuppressWarnings("ResultOfMethodCallIgnored")
final class NBT {
    public static JSONObject readtoJson(DataInputStream is) {
        try {
            if(is.readByte()==0) return new JSONObject("{}");
            is.readShort();
            return (readObject(is));
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            return (new JSONObject());
        }

    }

    private static JSONObject readObject(DataInputStream is) throws IOException, JSONException {
        JSONObject out = new JSONObject();
        boolean devam = true;
        while (devam) {
            byte id = is.readByte();
            String name = "";
            if (id !=0) {
                byte[] namebytes = new byte[is.readUnsignedShort()];
                is.read(namebytes,0, namebytes.length);
                name = new String(namebytes);
            }
            switch (id) {
                case (byte)0:
                    devam = false;
                    break;
                case (byte)1:
                    out.put(name,is.readByte());
                    break;
                case (byte)2:
                    out.put(name,is.readShort());
                    break;
                case (byte)3:
                    out.put(name,is.readInt());
                    break;
                case (byte)4:
                    out.put(name,is.readLong());
                    break;
                case (byte)5:
                    out.put(name,is.readFloat());
                    break;
                case (byte)6:
                    out.put(name,is.readDouble());
                    break;
                case (byte)8:
                    byte[] bytes8 = new byte[is.readUnsignedShort()];
                    //System.out.println("8likşey"+bytes8.length);
                    is.read(bytes8,0, bytes8.length);
                    String string8 = new String(bytes8);
                    out.put(name,string8);
                    break;
                case (byte)9:
                    out.put(name,readArray(is));
                    break;
                case (byte)10:
                    out.put(name,readObject(is));
                    break;
            }
        }
        return out;
    }

    private static JSONArray readArray(DataInputStream is) throws JSONException, IOException {
        byte listId = is.readByte();
        int listLen = is.readInt();
        JSONArray out = new JSONArray();
        if (listLen >= 0) {
            switch (listId) {
                case (byte)1:
                    for (int i = 0; i < listLen; i++) {
                        out.put(is.readByte());
                    }
                    break;
                case (byte)2:
                    for (int i = 0; i < listLen; i++) {
                        out.put(is.readShort());
                    }
                    break;
                case (byte)3:
                    for (int i = 0; i < listLen; i++) {
                        out.put(is.readInt());
                    }
                    break;
                case (byte)4:
                    for (int i = 0; i < listLen; i++) {
                        out.put(is.readLong());
                    }
                    break;
                case (byte)5:
                    for (int i = 0; i < listLen; i++) {
                        out.put(is.readFloat());
                    }
                    break;
                case (byte)6:
                    for (int i = 0; i < listLen; i++) {
                        out.put(is.readDouble());
                    }
                    break;
                case (byte)8:
                    for (int i = 0; i < listLen; i++) {
                        byte[] bytes9 = new byte[is.readUnsignedShort()];
                        is.read(bytes9,0, bytes9.length);
                        String string9 = new String(bytes9);
                        out.put(string9);
                    }
                    break;
                case (byte)9:
                    for (int i = 0; i < listLen; i++) {
                        out.put(readArray(is));
                    }
                    break;
                case (byte)10:
                    for (int i = 0; i < listLen; i++) {
                        out.put(readObject(is));
                    }
                    break;
            }
        }
        return out;
    }

}
