package com.example.gamin.Utils;


import android.graphics.Color;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;

import com.example.gamin.Minecraft.Chunk;
import com.example.gamin.Minecraft.Inventory;
import com.example.gamin.Minecraft.Slot;
import com.example.gamin.R;
import com.example.gamin.Render.Entity;
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
import java.util.List;

@SuppressWarnings({"ResultOfMethodCallIgnored"})
public final class PacketUtils {
    public static OutputStream os;
    public static OutputStream ecos;
    public static boolean isPremium;
    public static boolean isCompressed = false;
    public static int moveLeftRight = 0;
    public static int moveForwardBack = 0;
    public static boolean isRotating = false;
    public static double x;
    public static double y;
    public static double z;
    public static double motionX = 0;
    public static double motionY = 0;
    public static double motionZ = 0;
    public static boolean jump;
    public static boolean isSneaking;
    public static boolean isSprinting = true;
    public static float x_rot = 0.0F;
    public static float y_rot = 0.0F;
    public static float[] targetCoords;
    public static Entity targetEntity;
    static boolean didHorizontalCollide = false;
    private static int playerId;
    private static double generic_movementSpeed;
    private static boolean isOnGround = false;

    public static void calculateMovements() {
        float strafe = moveLeftRight * 0.98F;
        float forward = moveForwardBack * 0.98F;
        if (isOnGround) {
            if (isSneaking) {
                strafe = (float) (strafe * 0.3D);
                forward = (float) (forward * 0.3D);
            }
            if (motionY < 0) motionY = 0.0D;
        }
        boolean canSprint = isSprinting && forward > 0.8F && !didHorizontalCollide;//TODO add hunger and potion effects and using items
        float sprintingMult = canSprint ? 1.3F : 1.0F;


        //inertia
        float mult = 0.91F;
        if (isOnGround) mult *= 0.6F;//friction
        float acceleration = 0.16277136F / (mult * mult * mult);
        float landMovementFactor = (float) generic_movementSpeed; //TODO : add potion effects
        float movementFactor;
        if (isOnGround) movementFactor = (landMovementFactor * sprintingMult * acceleration);
        else movementFactor = 0.02F * sprintingMult;

        //updatemotionxz
        float dist = strafe * strafe + forward * forward;
        if (dist >= 1.0E-4F) {
            dist = (float) Math.sqrt(dist);
            if (dist < 1.0F) dist = 1.0F;
            dist = movementFactor / dist;
            strafe *= dist;
            forward *= dist;
            float sinYaw = (float) Math.sin(x_rot * Math.PI / 180.0F);
            float cosYaw = (float) Math.cos(x_rot * Math.PI / 180.0F);
            motionX += strafe * cosYaw - forward * sinYaw;
            motionZ += forward * cosYaw + strafe * sinYaw;
        }

        if (jump && isOnGround) {//TODO water and lava jumping and maybe add jumpticks and head collision
            motionY = 0.42D;
            float f = x_rot * 0.017453292F; //toRadians
            if (canSprint) {
                motionX -= Math.sin(f) * 0.2F;
                motionZ += Math.cos(f) * 0.2F;
            }
        }

        //TODO not sure about x and z, research more
        if (Math.abs(motionX) < 0.005D) motionX = 0.0D;
        if (Math.abs(motionY) < 0.005D) motionY = 0.0D;
        if (Math.abs(motionZ) < 0.005D) motionZ = 0.0D;

        //System.out.println(motionX + " " + motionY + " " + motionZ);
        didHorizontalCollide = false;
        moveEntity(motionX, motionY, motionZ);

        motionY -= 0.08D; //gravity
        motionY *= 0.9800000190734863D; //air resistance

        motionX *= mult; //friction
        motionZ *= mult; //friction
    }

    //TODO add step assist and maybe add ladder climbing and maybe add water and lava movement and
    //TODO special block movement like soul sand and honey block and maybe add ice and slime block movement
    private static void moveEntity(double motionX, double motionY, double motionZ) {
        try {
            double x = PacketUtils.x;
            double y = PacketUtils.y;
            double z = PacketUtils.z;

            y = Collision.calculateMovement(x, y, z, 1, motionY)[1];
            isOnGround = Collision.calculateMovement(x, y, z, 1, 0.0)[3] == 1.0;
            if (isSneaking && isOnGround) {
                double xx = Collision.calculateMovement(x, y, z, 0, motionX)[0];
                while (Collision.calculateMovement(xx, y, z, 1, -0.98)[3] == 0.0) {
                    if (motionX > 0) {
                        xx -= 0.05;
                        if (xx < x) {
                            xx = x;
                            break;
                        }
                    } else {
                        xx += 0.05;
                        if (xx > x) {
                            xx = x;
                            break;
                        }
                    }
                }
                x = xx;
                double zz = Collision.calculateMovement(x, y, z, 2, motionZ)[2];
                while (Collision.calculateMovement(x, y, zz, 1, -0.98)[3] == 0.0) {
                    if (motionZ > 0) {
                        zz -= 0.05;
                        if (zz < z) {
                            zz = z;
                            break;
                        }
                    } else {
                        zz += 0.05;
                        if (zz > z) {
                            zz = z;
                            break;
                        }
                    }
                }
                z = zz;
            } else {
                x = Collision.calculateMovement(x, y, z, 0, motionX)[0];
                z = Collision.calculateMovement(x, y, z, 2, motionZ)[2];
            }

            //try stepping if couldn't move
            if (didHorizontalCollide && isOnGround) {

                didHorizontalCollide = false;

                //calculate how much can we step up
                double stepUp = Collision.calculateMovement(x, y, z, 1, 0.6)[1];

                double x2 = PacketUtils.x;
                double y2 = stepUp;
                double z2 = PacketUtils.z;

                //try stepping
                x2 = Collision.calculateMovement(x2, y2, z2, 0, motionX)[0];
                z2 = Collision.calculateMovement(x2, y2, z2, 2, motionZ)[2];
                y2 = Collision.calculateMovement(x2, y2, z2, 1, -0.6)[1];

                //check which distance is longer
                double distance1 = Math.sqrt(Math.pow(PacketUtils.x - x, 2) + Math.pow(PacketUtils.y - y, 2) + Math.pow(PacketUtils.z - z, 2));
                double distance2 = Math.sqrt(Math.pow(PacketUtils.x - x2, 2) + Math.pow(PacketUtils.y - y2, 2) + Math.pow(PacketUtils.z - z2, 2));

                if (distance2 > distance1) {
                    x = x2;
                    y = y2;
                    z = z2;
                }
            }

            PacketUtils.x = x;
            PacketUtils.y = y;
            PacketUtils.z = z;
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static synchronized void write(byte packetid, List<Byte> data, Boolean isEncrypted) {
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


    public static synchronized void read(GLSurfaceView glSurfaceView, YourRenderer renderer, ImageButton[] imageButtons, TextView textView, byte id, byte[] data) throws IOException {
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

            case 0x0C://spawn player
                ByteArrayInputStream datastream0C = new ByteArrayInputStream(data);
                DataInputStream dataStream0C = new DataInputStream(datastream0C);

                int entityId0C = VarInt.readVarInt(dataStream0C);
                StringBuilder uuid = new StringBuilder();
                for (int i = 0; i < 16; i++) {
                    uuid.append(String.format("%02x", dataStream0C.readByte()));
                    if (i == 3 || i == 5 || i == 7 || i == 9) {
                        uuid.append("-");
                    }
                }
                int x0C = dataStream0C.readInt();
                int y0C = dataStream0C.readInt();
                int z0C = dataStream0C.readInt();
                byte yaw0C = dataStream0C.readByte();
                byte pitch0C = dataStream0C.readByte();
                int currentItem0C = dataStream0C.readShort();
                byte[] metadata = new byte[dataStream0C.available()];
                dataStream0C.read(metadata);
                Entity entity = new Entity(glSurfaceView.getContext(), 0, x0C, y0C, z0C, metadata);
                Entity.createEntity(entity, entityId0C);

                break;

            case 0x0E://spawn object
                ByteArrayInputStream datastream0E = new ByteArrayInputStream(data);
                DataInputStream dataStream0E = new DataInputStream(datastream0E);
                int entityId0E = VarInt.readVarInt(dataStream0E);
                int type0E = dataStream0E.readByte();
                int x0E = dataStream0E.readInt();
                int y0E = dataStream0E.readInt();
                int z0E = dataStream0E.readInt();
                byte pitch0E = dataStream0E.readByte();
                byte yaw0E = dataStream0E.readByte();
                int data0E = dataStream0E.readInt();
                Entity entity0E = new Entity(glSurfaceView.getContext(), type0E, x0E, y0E, z0E, new byte[0]);
                if (data0E != 0) {
                    entity0E.motionX = dataStream0E.readShort() / 8000f;
                    entity0E.motionY = dataStream0E.readShort() / 8000f;
                    entity0E.motionZ = dataStream0E.readShort() / 8000f;
                }
                Entity.createEntity(entity0E, entityId0E);
                break;

            case 0x0F://spawn mob
                ByteArrayInputStream datastream0F = new ByteArrayInputStream(data);
                DataInputStream dataStream0F = new DataInputStream(datastream0F);
                int entityId0F = VarInt.readVarInt(dataStream0F);
                int type0F = dataStream0F.readByte();
                int x0F = dataStream0F.readInt();
                int y0F = dataStream0F.readInt();
                int z0F = dataStream0F.readInt();
                byte yaw0F = dataStream0F.readByte();
                byte pitch0F = dataStream0F.readByte();
                byte headPitch0F = dataStream0F.readByte();
                short velocityX0F = dataStream0F.readShort();
                short velocityY0F = dataStream0F.readShort();
                short velocityZ0F = dataStream0F.readShort();
                byte[] metadata0F = new byte[dataStream0F.available()];
                dataStream0F.read(metadata0F);
                Entity entity0F = new Entity(glSurfaceView.getContext(), type0F, x0F, y0F, z0F, metadata0F);
                entity0F.motionX = velocityX0F / 8000f;
                entity0F.motionY = velocityY0F / 8000f;
                entity0F.motionZ = velocityZ0F / 8000f;
                Entity.createEntity(entity0F, entityId0F);
                break;

            case 0x12://entity velocity
                ByteArrayInputStream datastream12 = new ByteArrayInputStream(data);
                DataInputStream dataStream12 = new DataInputStream(datastream12);
                int entityId12 = VarInt.readVarInt(dataStream12);
                float velocityX = dataStream12.readShort() / 8000f;
                float velocityY = dataStream12.readShort() / 8000f;
                float velocityZ = dataStream12.readShort() / 8000f;
                if (entityId12 == playerId) {
                    motionX = velocityX;
                    motionY = velocityY;
                    motionZ = velocityZ;
                    calculateMovements();
                } else {
                    Entity entity12 = Entity.entities.get(entityId12);
                    if (entity12 != null) {
                        entity12.motionX = velocityX;
                        entity12.motionY = velocityY;
                        entity12.motionZ = velocityZ;
                    }
                }
                break;

            case 0x15://entity relative move
                ByteArrayInputStream datastream15 = new ByteArrayInputStream(data);
                DataInputStream dataStream15 = new DataInputStream(datastream15);
                int entityId15 = VarInt.readVarInt(dataStream15);
                byte dx15 = dataStream15.readByte();
                byte dy15 = dataStream15.readByte();
                byte dz15 = dataStream15.readByte();

                Entity entity15 = Entity.entities.get(entityId15);
                if (entity15 != null) {
                    entity15.move(dx15 / 32f, dy15 / 32f, dz15 / 32f);
                }

                break;

            case 0x17://entity look and relative move
                ByteArrayInputStream datastream17 = new ByteArrayInputStream(data);
                DataInputStream dataStream17 = new DataInputStream(datastream17);
                int entityId17 = VarInt.readVarInt(dataStream17);
                byte dx17 = dataStream17.readByte();
                byte dy17 = dataStream17.readByte();
                byte dz17 = dataStream17.readByte();
                byte yaw17 = dataStream17.readByte();
                byte pitch17 = dataStream17.readByte();

                Entity entity17 = Entity.entities.get(entityId17);
                if (entity17 != null) {
                    entity17.move(dx17 / 32f, dy17 / 32f, dz17 / 32f);
                }

                break;

            case 0x18://entity teleport
                ByteArrayInputStream datastream18 = new ByteArrayInputStream(data);
                DataInputStream dataStream18 = new DataInputStream(datastream18);
                int entityId18 = VarInt.readVarInt(dataStream18);
                int x18 = dataStream18.readInt();
                int y18 = dataStream18.readInt();
                int z18 = dataStream18.readInt();
                byte yaw18 = dataStream18.readByte();
                byte pitch18 = dataStream18.readByte();

                Entity entity18 = Entity.entities.get(entityId18);
                if (entity18 != null) {
                    entity18.setPos(x18 / 32f, y18 / 32f, z18 / 32f);
                }

                break;

            case 0x20://entity properties
                ByteArrayInputStream datastream20 = new ByteArrayInputStream(data);
                DataInputStream dataStream20 = new DataInputStream(datastream20);

                int entityId = VarInt.readVarInt(dataStream20);
                int propertyCount = dataStream20.readInt();
                for (int i = 0; i < propertyCount; i++) {
                    int stringLen = VarInt.readVarInt(dataStream20);
                    byte[] buffer = new byte[stringLen];
                    dataStream20.read(buffer);
                    String key = new String(buffer);
                    double value = dataStream20.readDouble();
                    if (entityId == playerId && key.equals("generic.movementSpeed"))
                        generic_movementSpeed = value;
                }

                break;
            case 0x21://chunkdata
            {
                ByteArrayInputStream datastream21 = new ByteArrayInputStream(data);
                DataInputStream dataStream21 = new DataInputStream(datastream21);
                Chunk[] chunkColumn21 = new Chunk[16];
                int chunkX21 = dataStream21.readInt();
                int chunkZ21 = dataStream21.readInt();
                long pos = ((long) chunkX21 << 32) | (chunkZ21 & 0xffffffffL);
                dataStream21.readBoolean();
                short bitmask = dataStream21.readShort();
                VarInt.readVarInt(dataStream21);
                for (byte chunkY = 0; chunkY < 16; chunkY++) {
                    if ((bitmask & (1 << chunkY)) != 0) {
                        short[] blocks = new short[16 * 16 * 16];
                        for (int j = 0; j < 4096; j++) {
                            blocks[j] = dataStream21.readShort();
                        }
                        chunkColumn21[chunkY] = new Chunk(blocks, chunkX21, chunkY, chunkZ21);
                    }
                }
                Chunk.chunkColumnMap.put(pos, chunkColumn21);
                //chunkColumn21.setRenders(glSurfaceView.getContext(), chunkX21, chunkZ21);
            }

            break;
            case 0x22://multiblock change
            {
                ByteArrayInputStream datastream22 = new ByteArrayInputStream(data);
                DataInputStream dataStream22 = new DataInputStream(datastream22);
                int chunkX22 = dataStream22.readInt();
                int chunkZ22 = dataStream22.readInt();
                int len22 = VarInt.readVarInt(dataStream22);
                for (int i = 0; i < len22; i++) {
                    int horizPos = dataStream22.readUnsignedByte();
                    byte vertPos = dataStream22.readByte();
                    int relativeX = (horizPos >> 4);
                    int relativeZ = (horizPos & 0x0F);
                    short blockraw = (short) VarInt.readVarInt(dataStream22);
                    short block = (short) ((blockraw & 255) << 8 | (blockraw >> 8));
                    Chunk.setBlock(glSurfaceView.getContext(), chunkX22 * 16 + relativeX, vertPos, chunkZ22 * 16 + relativeZ, block);
                }
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
                short blockraw = (short) VarInt.readVarInt(dataStream23);
                short block = (short) ((blockraw & 255) << 8 | (blockraw >> 8));
                Chunk.setBlock(glSurfaceView.getContext(), (int) BlockX, (int) BlockY, (int) BlockZ, block);
            }
            break;
            case 0x26://multiple chunks
            {
                ByteArrayInputStream datastream26 = new ByteArrayInputStream(data);
                DataInputStream dataStream26 = new DataInputStream(datastream26);
                boolean skylight = dataStream26.readBoolean();

                int len26 = VarInt.readVarInt(dataStream26);
                Chunk[][] chunkColumns = new Chunk[len26][16];
                long[] chunkPositions = new long[len26];
                short[] chunkBitmasks = new short[len26];
                int[] chunkXs = new int[len26];
                int[] chunkZs = new int[len26];
                for (int i = 0; i < len26; i++) {
                    Chunk[] chunkColumn = chunkColumns[i];
                    int chunkX = dataStream26.readInt();
                    int chunkZ = dataStream26.readInt();
                    chunkPositions[i] = ((long) chunkX << 32) | (chunkZ & 0xffffffffL);
                    chunkBitmasks[i] = dataStream26.readShort();
                    chunkColumns[i] = chunkColumn;
                    chunkXs[i] = chunkX;
                    chunkZs[i] = chunkZ;
                }
                Log.v("ChunkColumn", "got" + len26 + "chunks");
                for (int i = 0; i < len26; i++) {
                    byte chunkCount = 0;
                    Chunk[] chunkColumn = chunkColumns[i];
                    long pos = chunkPositions[i];
                    short bitmask = chunkBitmasks[i];
                    for (byte chunkY = 0; chunkY < 16; chunkY++) {
                        if ((bitmask & (1 << chunkY)) != 0) {
                            chunkCount += 1;
                            short[] blocks = new short[16 * 16 * 16];
                            for (int j = 0; j < 4096; j++) {
                                short block = dataStream26.readShort();
                                blocks[j] = block;
                            }
                            chunkColumn[chunkY] = new Chunk(blocks, chunkXs[i], chunkY, chunkZs[i]);
                        }
                    }
                    Chunk.chunkColumnMap.put(pos, chunkColumns[i]);
                    //chunkColumns[i].setRenders(glSurfaceView.getContext(), (int) (chunkColumns[i].pos >> 32), (int) (chunkColumns[i].pos & 0xffffffffL));
                    if (skylight) {
                        dataStream26.skipBytes(chunkCount * 2048);
                    }
                    dataStream26.skipBytes(chunkCount * 2048);
                    dataStream26.skipBytes(256);
                    //Log.v("ChunkColumn", "created chunk" + (i + 1) + "of" + len26 + " total chunks:" + chunkColumnMap.size());
                }
                //ChunkColumn.setUpdatedBuffers();
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

    public static void getTargetedObject() {
        double yaw = Math.toRadians(x_rot);
        double pitch = Math.toRadians(y_rot);
        float[] pos = {(float) x, (float) y + 1.62f, (float) z};

        float maxDistance = 5.0f;
        int result = -1; //0 +x, 1 +y, 2 +z, 3 -x, 4 -y, 5 -z

        float[] targetCoords = null;
        float[] targetHitbox = null;
        Entity targetEntity = null;

        float[] d = new float[3];
        d[0] = (float) ((-Math.sin(yaw)) * Math.cos(pitch));
        d[1] = (float) (-Math.sin(pitch));
        d[2] = (float) (Math.cos(yaw) * Math.cos(pitch));

        float[] tMax = new float[3];
        for (int i = 0; i < 3; i++) {
            tMax[i] = ((float) (d[i] >= 0 ? Math.ceil(pos[i]) : Math.floor(pos[i])) - pos[i]) / d[i];
        }

        int[] steps = new int[3];
        for (int i = 0; i < 3; i++) {
            steps[i] = d[i] >= 0 ? 1 : -1;
        }

        float[] tDelta = new float[3];
        for (int i = 0; i < 3; i++) {
            tDelta[i] = Math.abs(1 / d[i]);
        }

        int x = (int) Math.floor(pos[0]);
        int y = (int) Math.floor(pos[1]);
        int z = (int) Math.floor(pos[2]);
        for (float t = 0; t < maxDistance; ) {
            short block = Chunk.getBlock(x, y, z);
            if (block != 0) {
                for (float[] hitbox : Collision.getHitbox(block)) {
                    float[] hitboxCoords = new float[6];
                    for (int i = 0; i < 6; i++) {
                        hitboxCoords[i] = hitbox[i] + new float[]{x, y, z}[i % 3];
                    }
                    result = (int) checkIfLookingAt(hitboxCoords, pos, d, maxDistance)[0];
                    if (result != -1) {
                        maxDistance = checkIfLookingAt(hitboxCoords, pos, d, maxDistance)[1];
                        targetCoords = new float[]{x, y, z};
                        //create an array based on the face that was hit
                        //it should be an array of 18 floats, 6 vertices for two triangles that make up a square
                        switch (result) {//0 +x, 1 +y, 2 +z, 3 -x, 4 -y, 5 -z
                            case 0:
                                targetHitbox = new float[]{
                                        x + 1.01f, y + 1, z + 1,
                                        x + 1.01f, y, z + 1,
                                        x + 1.01f, y, z,
                                        x + 1.01f, y + 1, z + 1,
                                        x + 1.01f, y, z,
                                        x + 1.01f, y + 1, z
                                };
                                break;
                            case 1:
                                targetHitbox = new float[]{
                                        x + 1, y + 1.01f, z + 1,
                                        x + 1, y + 1.01f, z,
                                        x, y + 1.01f, z,
                                        x + 1, y + 1.01f, z + 1,
                                        x, y + 1.01f, z,
                                        x, y + 1.01f, z + 1
                                };
                                break;
                            case 2:
                                targetHitbox = new float[]{
                                        x + 1, y + 1, z + 1.01f,
                                        x, y + 1, z + 1.01f,
                                        x, y, z + 1.01f,
                                        x + 1, y + 1, z + 1.01f,
                                        x, y, z + 1.01f,
                                        x + 1, y, z + 1.01f
                                };
                                break;
                            case 3:
                                targetHitbox = new float[]{
                                        x - 0.1f, y + 1, z,
                                        x - 0.1f, y, z,
                                        x - 0.1f, y, z + 1,
                                        x - 0.1f, y + 1, z,
                                        x - 0.1f, y, z + 1,
                                        x - 0.1f, y + 1, z + 1
                                };
                                break;
                            case 4:
                                targetHitbox = new float[]{
                                        x + 1, y - 0.1f, z + 1,
                                        x + 1, y - 0.1f, z,
                                        x, y - 0.1f, z,
                                        x + 1, y - 0.1f, z + 1,
                                        x, y - 0.1f, z,
                                        x, y - 0.1f, z + 1
                                };
                                break;
                            case 5:
                                targetHitbox = new float[]{
                                        x + 1, y + 1, z - 0.1f,
                                        x + 1, y, z - 0.1f,
                                        x, y, z - 0.1f,
                                        x + 1, y + 1, z - 0.1f,
                                        x, y, z - 0.1f,
                                        x, y + 1, z - 0.1f
                                };
                                break;
                        }
                    }
                }
            }
            if (tMax[0] < tMax[1]) {
                if (tMax[0] < tMax[2]) {
                    x += steps[0];
                    t = tMax[0];
                    tMax[0] += tDelta[0];
                } else {
                    z += steps[2];
                    t = tMax[2];
                    tMax[2] += tDelta[2];
                }
            } else {
                if (tMax[1] < tMax[2]) {
                    y += steps[1];
                    t = tMax[1];
                    tMax[1] += tDelta[1];
                } else {
                    z += steps[2];
                    t = tMax[2];
                    tMax[2] += tDelta[2];
                }
            }
        }

        //check entity hitboxes
        synchronized ("entity") {
            long entityChunkX = (long) PacketUtils.x / 8;
            long entityChunkZ = (long) PacketUtils.z / 8;
            long entityChunkY = (long) PacketUtils.y / 8;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        long entityChunkPos = ((entityChunkX + dx) << 35) | ((entityChunkZ + dz) << 6) | (entityChunkY + dy);
                        List<Entity> entities = Entity.entityChunks.get(entityChunkPos);
                        if (entities == null) continue;
                        for (Entity entity : entities) {
                            float[] hitbox = entity.getHitbox();
                            if (hitbox == null) continue;

                            float[] entityCollisionResult = checkIfLookingAt(hitbox, pos, d, maxDistance);
                            if (entityCollisionResult[0] != -1) {
                                maxDistance = entityCollisionResult[1];
                                targetHitbox = hitbox;
                                targetEntity = entity;
                            }
                        }

                    }
                }
            }

        }

        //add target coords to the targetHitbox array
        PacketUtils.targetCoords = targetHitbox;
        PacketUtils.targetEntity = targetEntity;
    }


    private static float[] checkIfLookingAt(float[] target, float[] pos, float[] d, float maxDistance) {
        int result = -1; //0 +x, 1 +y, 2 +z, 3 -x, 4 -y, 5 -z

        float[] distances = new float[3];
        for (int i = 0; i < 3; i++) {
            if (d[i] >= 0) {
                distances[i] = (target[i] - pos[i]) / d[i];
            } else {
                distances[i] = (target[i + 3] - pos[i]) / d[i];
            }
            distances[i] = Math.abs(distances[i]);
        }

        float minDistance = Float.POSITIVE_INFINITY;
        for (int i = 0; i < 3; i++) {
            float distance = distances[i];
            boolean isInside = true;
            for (int j = 0; j < 3; j++) {
                if (i == j) continue;
                float coord = pos[j] + d[j] * distance;
                if (coord < target[j] || coord > target[j + 3]) {
                    isInside = false;
                    break;
                }
            }
            if (isInside && distance < minDistance && distance < maxDistance) {
                minDistance = distance;
                result = i;
                if (d[i] > 0) result += 3;
            }
        }
        return new float[]{result, minDistance};
    }
}
