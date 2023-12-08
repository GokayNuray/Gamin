package com.example.gamin.Utils;


import android.graphics.Color;
import android.opengl.GLSurfaceView;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.gamin.Minecraft.ChunkColumn;
import com.example.gamin.Minecraft.Inventory;
import com.example.gamin.Minecraft.Slot;
import com.example.gamin.R;
import com.example.gamin.Render.YourRenderer;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"ResultOfMethodCallIgnored"})
public final class PacketUtils extends AppCompatActivity {
    public static OutputStream os;
    public static OutputStream ecos;
    public static boolean isPremium;
    public static boolean isCompressed = false;
    public static int playerId;
    public static int moveLeftRight = 0;
    public static int moveForwardBack = 0;
    public static boolean isRotating = false;
    public static double x;
    public static double y;
    public static double z;
    public static double generic_movementSpeed;
    public static double motionX = 0;
    public static double motionY = 0;
    public static double motionZ = 0;
    public static boolean isOnGround = false;
    public static boolean jump;
    public static boolean isSneaking;
    public static boolean didHorizontalCollide = false;
    public static boolean isSprinting = true;
    public static float x_rot = 0.0F;
    public static float y_rot = 0.0F;
    public static Map<Long, ChunkColumn> chunkColumnMap = new HashMap<>();


    public static void calculateMovements() {
        float strafe = moveLeftRight * 0.98F;
        float forward = moveForwardBack * 0.98F;
        if (isOnGround) {
            if (isSneaking) {
                strafe = (float) (strafe * 0.3D);
                forward = (float) (forward * 0.3D);
            }
            motionY = 0.0D;
        }
        boolean canSprint = isSprinting && forward > 0.8F && !didHorizontalCollide;//TODO add hunger and potion effects and using items
        float sprintingMult = canSprint ? 1.3F : 1.0F;


        //inertia
        float mult = 0.91F;
        if (isOnGround)
            mult *= 0.6F;//friction
        float acceleration = 0.16277136F / (mult * mult * mult);
        float landMovementFactor = (float) generic_movementSpeed; //TODO : add potion effects
        float movementFactor;
        if (isOnGround)
            movementFactor = (landMovementFactor * sprintingMult * acceleration);
        else
            movementFactor = 0.02F * sprintingMult;

        //updatemotionxz
        float dist = strafe * strafe + forward * forward;
        if (dist >= 1.0E-4F) {
            dist = (float) Math.sqrt(dist);
            if (dist < 1.0F)
                dist = 1.0F;
            dist = movementFactor / dist;
            strafe *= dist;
            forward *= dist;
            float sinYaw = (float) Math.sin(x_rot * Math.PI / 180.0F);
            float cosYaw = (float) Math.cos(x_rot * Math.PI / 180.0F);
            motionX += strafe * cosYaw - forward * sinYaw;
            motionZ += forward * cosYaw + strafe * sinYaw;
        }

        if (jump && isOnGround) {//TODO water and lava jumping and maybe add jumpticks
            motionY = 0.42D;
            System.out.println("jump");
            float f = x_rot * 0.017453292F; //toRadians
            if (canSprint) {
                motionX -= Math.sin(f) * 0.2F;
                motionZ += Math.cos(f) * 0.2F;
            }
        }

        if (Math.abs(motionX) < 0.005D)
            motionX = 0.0D;
        if (Math.abs(motionY) < 0.005D)
            motionY = 0.0D;
        if (Math.abs(motionZ) < 0.005D)
            motionZ = 0.0D;

        System.out.println(motionX + " " + motionY + " " + motionZ);
        didHorizontalCollide = false;
        moveEntity(motionX, motionY, motionZ);

        motionY -= 0.08D; //gravity
        motionY *= 0.9800000190734863D; //air resistance

        motionX *= mult; //friction
        motionZ *= mult; //friction
    }

    //TODO add step assist and maybe add ladder climbing and maybe add water and lava movement and
    //TODO special block movement like soul sand and honey block and maybe add ice and slime block movement
    public static void moveEntity(double motionX, double motionY, double motionZ) {
        try {
            PacketUtils.y = Collision.calculateMovement(PacketUtils.x, PacketUtils.y, PacketUtils.z, 1, motionY)[1];
            isOnGround = Collision.calculateMovement(PacketUtils.x, PacketUtils.y, PacketUtils.z, 1, 0.0)[3] == 1.0;
            if (isSneaking && isOnGround) {
                double xx = Collision.calculateMovement(PacketUtils.x, PacketUtils.y, PacketUtils.z, 0, motionX)[0];
                while (Collision.calculateMovement(xx, PacketUtils.y, PacketUtils.z, 1, -0.98)[3] == 0.0) {
                    if (motionX > 0) {
                        xx -= 0.05;
                        if (xx < PacketUtils.x) {
                            xx = PacketUtils.x;
                            break;
                        }
                    } else {
                        xx += 0.05;
                        if (xx > PacketUtils.x) {
                            xx = PacketUtils.x;
                            break;
                        }
                    }
                }
                PacketUtils.x = xx;
                double zz = Collision.calculateMovement(PacketUtils.x, PacketUtils.y, PacketUtils.z, 2, motionZ)[2];
                while (Collision.calculateMovement(PacketUtils.x, PacketUtils.y, zz, 1, -0.98)[3] == 0.0) {
                    if (motionZ > 0) {
                        zz -= 0.05;
                        if (zz < PacketUtils.z) {
                            zz = PacketUtils.z;
                            break;
                        }
                    } else {
                        zz += 0.05;
                        if (zz > PacketUtils.z) {
                            zz = PacketUtils.z;
                            break;
                        }
                    }
                }
                PacketUtils.z = zz;
            } else {
                PacketUtils.x = Collision.calculateMovement(PacketUtils.x, PacketUtils.y, PacketUtils.z, 0, motionX)[0];
                PacketUtils.z = Collision.calculateMovement(PacketUtils.x, PacketUtils.y, PacketUtils.z, 2, motionZ)[2];
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static void write(byte packetid, List<Byte> data, Boolean isEncrypted) {
        List<Byte> gerceklist = new ArrayList<>();

        if (isEncrypted) {
            gerceklist.addAll(VarInt.writeVarInt(2 + data.size()));
            gerceklist.add((byte) 0);
        } else if (isCompressed) {
            gerceklist.addAll(VarInt.writeVarInt(2 + data.size()));
            gerceklist.add((byte) 0);
        } else {
            gerceklist.addAll(VarInt.writeVarInt(1 + data.size()));
        }
        gerceklist.add(packetid);
        gerceklist.addAll(data);
        byte[] gercekdata = new byte[gerceklist.size()];
        for (int i = 0; i < gerceklist.size(); i++) {
            gercekdata[i] = gerceklist.get(i);
        }
        Thread thread;
        if (isEncrypted) {
            thread = new Thread(() -> {
                try {
                    ecos.write(gercekdata);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } else {
            thread = new Thread(() -> {
                try {
                    os.write(gercekdata);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
        thread.start();

    }


    public static void read(GLSurfaceView glSurfaceView, YourRenderer renderer, ImageButton[] imageButtons, TextView textView, byte id, byte[] data) throws IOException {
        switch (id) {
            case 0x00://keepAlive
                System.out.println("hala yaşıyoruz");
                write((byte) 0, VarInt.writeVarInt(VarInt.readVarInt(new ByteArrayInputStream(data))), isPremium);
                break;
            case 0x01://login success
                ByteArrayInputStream datastream01 = new ByteArrayInputStream(data);
                DataInputStream dataStream01 = new DataInputStream(datastream01);
                playerId = dataStream01.readInt();
                System.out.println(dataStream01.readInt());
                break;
            case 0x02://chat
            {
                ByteArrayInputStream dataStream = new ByteArrayInputStream(data);
                int len = VarInt.readVarInt(dataStream);
                byte[] dataBuffer = new byte[len];
                dataStream.read(dataBuffer);
                String s = new String(dataBuffer);
                System.out.println(s);
                textView.append(ChatToTextView.convert(s));
                textView.setBackgroundResource(R.color.transparentgray);
                textView.setShadowLayer(1, 0, 0, Color.BLACK);
            }
            break;
            case 0x03://time update

                break;
            case 0x05://spawn pos

                break;
            case 0x06://updatehealth/hunger

                break;
            case 0x07://respawn/changeserver

                break;
            case 0x08://player pos/look
            {
                //velocity = 0;
                byte flags = data[data.length - 1];
                ByteArrayInputStream datastream08 = new ByteArrayInputStream(data);
                DataInputStream dataStream08 = new DataInputStream(datastream08);
                if ((flags & 0x01) == 0) {
                    x = dataStream08.readDouble();
                } else {
                    x += dataStream08.readDouble();
                }
                if ((flags & 0x02) == 0) {
                    y = dataStream08.readDouble();
                } else {
                    y += dataStream08.readDouble();
                }
                if ((flags & 0x04) == 0) {
                    z = dataStream08.readDouble();
                } else {
                    z += dataStream08.readDouble();
                }
                if ((flags & 0x10) == 0) {
                    x_rot = dataStream08.readFloat();
                } else {
                    x_rot += dataStream08.readFloat();
                }
                if ((flags & 0x08) == 0) {
                    y_rot = dataStream08.readFloat();
                } else {
                    y_rot += dataStream08.readFloat();
                }
            }
            break;
            case 0x09://holditem change

                break;
            case 0x20://entity properties
                ByteArrayInputStream datastream20 = new ByteArrayInputStream(data);
                DataInputStream dataStream20 = new DataInputStream(datastream20);

                int entityId = VarInt.readVarInt(dataStream20);
                System.out.println(entityId);
                int propertyCount = dataStream20.readInt();
                for (int i = 0; i < propertyCount; i++) {
                    int stringLen = VarInt.readVarInt(dataStream20);
                    byte[] buffer = new byte[stringLen];
                    dataStream20.read(buffer);
                    String key = new String(buffer);
                    System.out.println(key);
                    double value = dataStream20.readDouble();
                    System.out.println(value);
                    if (entityId == playerId && key.equals("generic.movementSpeed"))
                        generic_movementSpeed = value;
                }

                break;
            case 0x21://chunkdata
            {
                ByteArrayInputStream datastream21 = new ByteArrayInputStream(data);
                DataInputStream dataStream21 = new DataInputStream(datastream21);
                ChunkColumn chunkColumn21 = new ChunkColumn();
                int chunkX21 = dataStream21.readInt();
                int chunkZ21 = dataStream21.readInt();
                chunkColumn21.pos = ((long) chunkX21 << 32) | (chunkZ21 & 0xffffffffL);
                dataStream21.readBoolean();
                chunkColumn21.bitmask = dataStream21.readShort();
                VarInt.readVarInt(dataStream21);
                for (byte chunkY = 0; chunkY < 16; chunkY++) {
                    if ((chunkColumn21.bitmask & (1 << chunkY)) != 0) {
                        short[][][] chunk21 = new ChunkColumn().chunk[chunkY];
                        for (int j = 0; j < 4096; j++) {
                            short block = dataStream21.readShort();
                            chunk21[j % 16][j / 256][((j % 256) / 16)] = block;
                        }
                        chunkColumn21.chunk[chunkY] = chunk21;
                    }
                }
                chunkColumnMap.put(chunkColumn21.pos, chunkColumn21);
            }

            break;
            case 0x23://block change
            {
                ByteArrayInputStream datastream23 = new ByteArrayInputStream(data);
                DataInputStream dataStream23 = new DataInputStream(datastream23);
                long blockPos = dataStream23.readLong();
                long BlockX = blockPos >> 38;
                long BlockY = (blockPos >> 26) & 0xFFF;
                long BlockZ = (blockPos << 38) >> 38;
                ChunkColumn.setBlock((int) BlockX, (int) BlockY, (int) BlockZ, (short) VarInt.readVarInt(dataStream23));
            }
            break;
            case 0x26://multiple chunks
            {
                ByteArrayInputStream datastream26 = new ByteArrayInputStream(data);
                DataInputStream dataStream26 = new DataInputStream(datastream26);
                boolean skylight = dataStream26.readBoolean();

                int len26 = VarInt.readVarInt(dataStream26);
                ChunkColumn[] chunkColumns = new ChunkColumn[len26];
                for (int i = 0; i < len26; i++) {
                    ChunkColumn chunkColumn = new ChunkColumn();
                    int chunkX = dataStream26.readInt();
                    int chunkZ = dataStream26.readInt();
                    chunkColumn.pos = ((long) chunkX << 32) | (chunkZ & 0xffffffffL);
                    chunkColumn.bitmask = dataStream26.readShort();
                    chunkColumns[i] = chunkColumn;
                }
                for (int i = 0; i < len26; i++) {
                    byte chunkCount = 0;
                    for (byte chunkY = 0; chunkY < 16; chunkY++) {
                        if ((chunkColumns[i].bitmask & (1 << chunkY)) != 0) {
                            chunkCount += 1;
                            short[][][] chunk = new ChunkColumn().chunk[chunkY];
                            for (int j = 0; j < 4096; j++) {
                                short block = dataStream26.readShort();
                                chunk[j % 16][j / 256][((j % 256) / 16)] = block;
                            }
                            chunkColumns[i].chunk[chunkY] = chunk;
                        }
                    }
                    chunkColumnMap.put(chunkColumns[i].pos, chunkColumns[i]);
                    if (skylight) {
                        dataStream26.skipBytes(chunkCount * 2048);
                    }
                    dataStream26.skipBytes(chunkCount * 2048);
                    dataStream26.skipBytes(256);
                }
            }
            break;
            case 0x2D://open Window

                break;
            case 0x2E://close windows

                break;
            case 0x2F://set slot
                break;
            case 0x30://window items
            {
                System.out.println(Arrays.toString(data));

                DataInputStream is30 = new DataInputStream(new ByteArrayInputStream(data));
                byte windowId = is30.readByte();
                short len30 = is30.readShort();
                System.out.println(windowId + "windows items" + len30);
                Inventory inventory = Inventory.inventoryMap.get((windowId));
                for (int i = 0; i < len30; i++) {
                    short slotBlockId = is30.readShort();
                    if (slotBlockId != -1) {
                        byte slotCount = is30.readByte();
                        byte slotDamage = is30.readByte();
                        byte slotMetaData = is30.readByte();
                        JSONObject nbt = NBT.readtoJson(is30);
                        System.out.println(slotBlockId + nbt.toString() + i);
                        Slot slot = new Slot(slotBlockId, slotCount, slotDamage, slotMetaData, nbt);
                        assert inventory != null;
                        inventory.contents[i] = slot;
                    }
                }
                assert inventory != null;
                //inventory.update(glSurfaceView,renderer,imageButtons);
            }
            break;
            case 0x32://confirm transaction
                textView.append("transaksiyon");
                textView.append(new String(data));
                break;
            case 0x36://open sign editor

                break;
            case 0x38://playerlist(tab list??)

                break;
            case 0x3B://scoreboard objective

                break;
            case 0x3C://update score

                break;
            case 0x3D://display scoreboard

                break;
            case 0x3E://teams

                break;
            case 0x3F://plugin messages
                System.out.println(new String(data));

                break;
            case 0x40://kick
                System.out.println("kicklendik" + data.length);
                FileWriter fwrite = new FileWriter("/storage/emulated/0/NewTextFile.txt");
                for (byte b : data) {
                    char c = (char) b;
                    fwrite.write(c);
                }
                fwrite.close();

                break;
            case 0x45://title

                break;
            case 0x47://playerlist(tablist??) header and footer

                break;
            default:

                break;
        }

    }

}
