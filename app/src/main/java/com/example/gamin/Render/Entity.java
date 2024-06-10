package com.example.gamin.Render;

import android.content.Context;

import com.example.gamin.Minecraft.Slot;
import com.example.gamin.Utils.Collision;
import com.example.gamin.Utils.NBT;
import com.example.gamin.Utils.VarInt;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

//FIXME entity and tile entities are broken rn
public class Entity {
    public static final Map<Integer, Entity> entities = new HashMap<>();
    public static final Map<Long, List<Entity>> entityChunks = new HashMap<>();

    private static final Map<Integer, JSONObject> entityData = new HashMap<>();
    private static final Map<Integer, List<Square>> models = new HashMap<>();

    public float motionX;
    public float motionY;
    public float motionZ;
    public float[] hitbox;
    FloatBuffer coordsBuffer;
    FloatBuffer colorsBuffer;
    FloatBuffer texturesBuffer;
    List<Square> squares;
    Object[] metadata = new Object[32];
    private boolean hasChanged = true;
    private float x;
    private float y;
    private float z;

    public Entity(Context context, int id, int intX, int intY, int intZ, byte[] metadata) {
        this.x = intX / 32f;
        this.y = intY / 32f;
        this.z = intZ / 32f;
        if (id < 0) id += 256;
        if (!models.containsKey(id)) {
            String model = "bat.jem";
            String texture = "entity/bat";
            int angle = 0;
            switch (id) {
                case 0://player
                    model = "unsupported/player.jem";
                    texture = "entity/steve";
                    break;

                case 31://chest
                    model = "legacy/chest_14.jem";
                    texture = "entity/chest/normal";
                    break;

                case 50://creeper
                    model = "creeper.jem";
                    texture = "entity/creeper/creeper";
                    break;
                case 51://skeleton
                    model = "skeleton.jem";
                    texture = "entity/skeleton/skeleton";
                    break;
                case 52://spider
                    model = "spider.jem";
                    texture = "entity/spider/spider";
                    break;
                case 53://giant
                    model = "giant.jem";
                    texture = "entity/zombie/zombie";
                    break;
                case 54://zombie
                    model = "zombie.jem";
                    texture = "entity/zombie/zombie";
                    break;
                case 55://slime
                    model = "slime.jem";
                    texture = "entity/slime/slime";
                    break;
                case 56://ghast
                    model = "ghast.jem";
                    texture = "entity/ghast/ghast";
                    break;
                case 57://zombie pigman
                    model = "legacy/zombie_pigman_15.jem";
                    texture = "entity/zombie_pigman";
                    break;
                case 58://enderman
                    model = "enderman.jem";
                    texture = "entity/enderman/enderman";
                    break;
                case 59://cave spider
                    model = "cave_spider.jem";
                    texture = "entity/spider/cave_spider";
                    break;
                case 60://silverfish
                    model = "silverfish.jem";
                    texture = "entity/silverfish";
                    break;
                case 61://blaze
                    model = "blaze.jem";
                    texture = "entity/blaze";
                    break;
                case 62://magma cube
                    model = "magma_cube.jem";
                    texture = "entity/slime/magma_cube";
                    break;
                case 63://ender dragon
                    model = "legacy/dragon_14.jem";
                    texture = "entity/enderdragon/dragon";
                    break;
                case 64://wither
                    model = "wither.jem";
                    texture = "entity/wither/wither";
                    break;
                case 65://bat
                    model = "bat.jem";
                    texture = "entity/bat";
                    break;
                case 66://witch
                    model = "witch.jem";
                    texture = "entity/witch";
                    break;
                case 67://endermite
                    model = "endermite.jem";
                    texture = "entity/endermite";
                    break;
                case 68://guardian
                    model = "guardian.jem";
                    texture = "entity/guardian";
                    break;
                case 90://pig
                    model = "pig.jem";
                    texture = "entity/pig/pig";
                    break;
                case 91://sheep
                    model = "sheep.jem";
                    texture = "entity/sheep/sheep";
                    break;
                case 92://cow
                    model = "cow.jem";
                    texture = "entity/cow/cow";
                    break;
                case 93://chicken
                    model = "chicken.jem";
                    texture = "entity/chicken";
                    break;
                case 94://squid
                    model = "squid.jem";
                    texture = "entity/squid";
                    break;
                case 95://wolf
                    model = "wolf.jem";
                    texture = "entity/wolf/wolf";
                    break;
                case 96://mooshroom
                    model = "mooshroom.jem";
                    texture = "entity/cow/mooshroom";
                    break;
                case 97://snow golem
                    model = "snow_golem.jem";
                    texture = "entity/snowman";
                    break;
                case 98://ocelot
                    model = "ocelot.jem";
                    texture = "entity/cat/ocelot";
                    break;
                case 99://iron golem
                    model = "legacy/iron_golem_16.jem";
                    texture = "entity/iron_golem";
                    break;
                case 100://horse
                    model = "legacy/horse_10.jem";
                    texture = "entity/horse/horse_white";
                    break;
                case 101://rabbit
                    model = "rabbit.jem";
                    texture = "entity/rabbit/white";
                    break;
                case 120://villager
                    model = "villager.jem";
                    texture = "entity/villager/villager";
                    break;
                case 200://ender crystal
                    model = "end_crystal.jem";
                    texture = "entity/endercrystal/endercrystal";
                    break;


            }
            //List<Square> modelSquares = TileEntity.readJemModel(context, model, texture, angle);
            //models.put(id, modelSquares);
        }

        hitbox = getEntityHitbox(id);

        /*squares = models.get(id);
        assert squares != null;

        try {
            readMetadata(metadata);
        } catch (IOException e) {
            e.printStackTrace();
        }

        long pos = (((long) x / 8) << 35) | (((long) z / 8) << 6) | (((long) y / 8));

        synchronized ("entity") {
            if (!entityChunks.containsKey(pos)) {
                List<Entity> entities = new ArrayList<>();
                entities.add(this);
                entityChunks.put(pos, entities);
            } else {
                Objects.requireNonNull(entityChunks.get(pos)).add(this);
            }
        }*/
    }

    public static void loadEntities(Context context) throws IOException, JSONException {
        InputStream is = context.getAssets().open("data/entities.json");
        int size = is.available();
        byte[] buffer = new byte[size];
        is.read(buffer);
        is.close();
        String json = new String(buffer);
        JSONArray array = new JSONArray(json);
        for (int i = 0; i < array.length(); i++) {
            JSONObject entity = array.getJSONObject(i);
            entityData.put(entity.getInt("internalId"), entity);
        }
    }

    public static float[] getEntityHitbox(int id) {
        if (id < 0) id += 256;
        if (id == 0) {//player
            float[] result = new float[6];
            result[0] = -0.3f;
            result[1] = 0;
            result[2] = -0.3f;
            result[3] = 0.3f;
            result[4] = 1.8f;
            result[5] = 0.3f;
            return result;
        }
        try {
            JSONObject entity = entityData.get(id);
            if (entity == null) {
                //Log.e("Entity", "entity does not exist: " + id); TODO temporarily disabled because it's filling the log
                return null;
            }
            float width = (float) entity.getDouble("width");
            float height = (float) entity.getDouble("height");

            float[] result = new float[6];
            result[0] = -width / 2;
            result[1] = 0;
            result[2] = -width / 2;
            result[3] = width / 2;
            result[4] = height;
            result[5] = width / 2;

            return result;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void createEntity(Entity entity, int entityId0C) {
        synchronized ("entity") {
            entities.put(entityId0C, entity);
        }
    }

    public static void moveEntities() throws JSONException {
        for (Entity entity : entities.values()) {
            entity.x = (float) Collision.calculateMovement(entity.x, entity.y, entity.z, 0, entity.motionX)[0];
            entity.y = (float) Collision.calculateMovement(entity.x, entity.y, entity.z, 1, entity.motionY)[1];
            entity.z = (float) Collision.calculateMovement(entity.x, entity.y, entity.z, 2, entity.motionZ)[2];
            entity.hasChanged = true;
        }
    }

    public float[] getHitbox() {
        float[] result = null;

        if (hitbox != null) {
            result = new float[6];

            result[0] = hitbox[0] + x;
            result[1] = hitbox[1] + y;
            result[2] = hitbox[2] + z;
            result[3] = hitbox[3] + x;
            result[4] = hitbox[4] + y;
            result[5] = hitbox[5] + z;
        }

        return result;
    }

    public void move(float x, float y, float z) {
        setPos(this.x + x, this.y + y, this.z + z);
    }

    public void setPos(float x, float y, float z) {
        long oldPos = ((long) this.x / 8) << 35 | ((long) this.z / 8) << 6 | ((long) this.y / 8);
        long newPos = ((long) x / 8) << 35 | ((long) z / 8) << 6 | ((long) y / 8);
        this.x = x;
        this.y = y;
        this.z = z;
        synchronized ("entity") {

            if (oldPos != newPos) {
                if (entityChunks.containsKey(oldPos)) {
                    List<Entity> entities = entityChunks.get(oldPos);
                    assert entities != null;
                    entities.remove(this);
                    if (entities.isEmpty()) {
                        entityChunks.remove(oldPos);
                    }
                }
                if (!entityChunks.containsKey(newPos)) {
                    List<Entity> entities = new ArrayList<>();
                    entities.add(this);
                    entityChunks.put(newPos, entities);
                } else {
                    Objects.requireNonNull(entityChunks.get(newPos)).add(this);
                }
            }
        }
        hasChanged = true;
    }

    void setBuffers() {
        if (!hasChanged) return;
        int squareCount = squares.size();
        if (coordsBuffer == null) {
            coordsBuffer = FloatBuffer.allocate(squareCount * 6 * 3);
            colorsBuffer = FloatBuffer.allocate(squareCount * 6 * 4);
            texturesBuffer = FloatBuffer.allocate(squareCount * 6 * 2);

            for (Square square : squares) {
                colorsBuffer.put(square.squareColors);

                texturesBuffer.put(square.textures1);
                texturesBuffer.put(square.textures2);
            }
        }

        for (Square square : squares) {
            for (int i = 0; i < 3; i++) {
                coordsBuffer.put(square.coords1[i * 3] + x);
                coordsBuffer.put(square.coords1[i * 3 + 1] + y);
                coordsBuffer.put(square.coords1[i * 3 + 2] + z);
            }
            for (int i = 0; i < 3; i++) {
                coordsBuffer.put(square.coords2[i * 3] + x);
                coordsBuffer.put(square.coords2[i * 3 + 1] + y);
                coordsBuffer.put(square.coords2[i * 3 + 2] + z);
            }
        }

        coordsBuffer.position(0);
        colorsBuffer.position(0);
        texturesBuffer.position(0);
        hasChanged = false;
    }

    private void readMetadata(Context context, byte[] metadata) throws IOException {
        ByteArrayInputStream is = new ByteArrayInputStream(metadata);
        DataInputStream dis = new DataInputStream(is);
        while (dis.available() > 0) {
            int item = dis.readUnsignedByte();
            if (item == 127) break;
            int index = item & 0x1F;
            int type = item >> 5;

            switch (type) {
                case 0:
                    this.metadata[index] = dis.readByte();
                    break;
                case 1:
                    this.metadata[index] = dis.readShort();
                    break;
                case 2:
                    this.metadata[index] = dis.readInt();
                    break;
                case 3:
                    this.metadata[index] = dis.readFloat();
                    break;
                case 4:
                    int length = VarInt.readVarInt(dis);
                    byte[] bytes = new byte[length];
                    dis.readFully(bytes);
                    this.metadata[index] = new String(bytes);
                    break;
                case 5:
                    short slotBlockId = dis.readShort();
                    if (slotBlockId == -1) {
                        this.metadata[index] = null;
                    } else {
                        byte slotCount = dis.readByte();
                        byte slotDamage = dis.readByte();
                        byte slotMetaData = dis.readByte();
                        JSONObject slotNbt = NBT.readtoJson(dis);
                        this.metadata[index] = new Slot(context, slotBlockId, slotCount, slotDamage, slotMetaData, slotNbt);
                    }
                    break;
                case 7:
                    float[] rotation = new float[3];
                    rotation[0] = dis.readFloat();
                    rotation[1] = dis.readFloat();
                    rotation[2] = dis.readFloat();
                    this.metadata[index] = rotation;
            }
        }
    }
}
