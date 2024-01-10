package com.example.gamin.Render;

import android.annotation.SuppressLint;
import android.content.Context;
import android.opengl.Matrix;

import com.example.gamin.Minecraft.ChunkColumn;
import com.example.gamin.Minecraft.Slot;
import com.example.gamin.Utils.PacketUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * @noinspection ResultOfMethodCallIgnored
 */
@SuppressLint("DiscouragedApi")
public class SlotRenderer {
    static Set<Integer> specialBlocks = new HashSet<>(Arrays.asList(2, 53, 64, 71, 193, 194, 195, 196, 197, 67, 104, 105, 106, 108, 109, 114, 128, 131, 132, 134, 135, 136, 139, 156, 163, 180, 85, 113, 188, 189, 190, 191, 192));
    static Set<Integer> multiStateBlocks = new HashSet<>(Arrays.asList(17, 23, 158, 26, 27, 28, 66, 157, 29, 33, 34, 59, 60, 61, 62, 65, 69, 70, 72, 77, 86, 91, 93, 94, 96, 99, 100, 107, 115, 117, 120, 145, 183, 184, 185, 186, 187, 167, 143, 147, 148, 149, 150, 162, 43, 125, 141, 142, 154, 175, 181));
    static Map<String, List<Square>> models = new HashMap<>();
    public List<Square> squares = new ArrayList<>();
    public FloatBuffer coordsBuffer;
    public FloatBuffer colorsBuffer;
    public FloatBuffer texturesBuffer;
    float angle = 0;
    boolean upsideDown = false;
    Context context;
    short id;
    byte metadata;
    int type;
    int x;
    int y;
    int z;

    public SlotRenderer update() {
        if (!specialBlocks.contains((int) id)) return null;
        try {
            return new SlotRenderer(context, id, metadata, type, x, y, z);
        } catch (IOException | JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public SlotRenderer(Context context, short id, byte metadata, int type, int x, int y, int z) throws IOException, JSONException {
        this.context = context;
        this.id = id;
        this.metadata = metadata;
        this.type = type;
        this.x = x;
        this.y = y;
        this.z = z;
        String model;
        float modelAngle = 0;
        float modelXAngle = 0;
        switch (type) {
            //item
            case 0:
                JSONObject item = Slot.itemsMap.get((int) id);
                assert item != null : "Item is null" + id;
                if (item.has("variations")) {
                    JSONArray variations = item.getJSONArray("variations");
                    //get the variation with the correct metadata
                    for (int i = 0; i < variations.length(); i++) {
                        JSONObject variation = variations.getJSONObject(i);
                        if (variation.getInt("metadata") == metadata) {
                            item = variation;
                        }
                    }
                }
                if (item.has("itemModel")) {
                    model = item.getString("itemModel");
                } else {
                    model = item.getString("displayName").trim().toLowerCase().replaceAll(" ", "_");
                }
                model = "models/item/" + model + ".json";
                break;
            //block
            case 1:
                id = id < 0 ? (short) (id + 256) : id;
                short slabId = id;
                if (id == 43 | id == 125 || id == 181) slabId++;
                if (specialBlocks.contains((int) id)) {
                    model = getSpecialBlockModel(id, metadata, x, y, z);
                    break;
                }
                JSONObject block = Slot.blocksMap.get((int) slabId);
                assert block != null : "Block is null" + id;
                if (block.has("variations")) {
                    JSONArray variations = block.getJSONArray("variations");
                    //get the variation with the correct metadata
                    for (int i = 0; i < variations.length(); i++) {
                        JSONObject variation = variations.getJSONObject(i);
                        if (variation.getInt("metadata") == metadata) {
                            block = variation;
                        }
                    }
                }
                if (block.has("blockModel")) {
                    model = block.getString("blockModel");
                } else if (block.has("itemModel")) {
                    model = block.getString("itemModel");
                } else {
                    model = block.getString("displayName").trim().toLowerCase().replaceAll(" ", "_");
                }
                model = "models/block/" + model + ".json";
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + type);
        }
        if (!models.containsKey(model + id + " " + metadata)) {
            InputStream is;
            List<Square> modelSquares = new ArrayList<>();
            String newModel = model;
            try {
                context.getAssets().open(model).close();
            } catch (Exception e) {
                newModel = model.replaceFirst("block", "item");
                try {
                    context.getAssets().open(newModel).close();
                } catch (Exception e2) {
                    System.out.println("bbModel not found: " + model + "  " + newModel);
                    newModel = "models/block/pumpkin_stem_growth3.json";
                }
            }
            if (multiStateBlocks.contains((int) id)) {
                newModel = getMultiStateBlockModel(id, metadata, newModel);
            }
            for (String s : newModel.split("amongus")) {
                if (s.startsWith("angle")) {
                    modelAngle = Float.parseFloat(s.substring(5));
                    continue;
                }
                if (s.startsWith("up")) {
                    modelXAngle = 90;
                    continue;
                }
                if (s.startsWith("flipped")) {
                    modelXAngle = 180;
                    continue;
                }
                if (s.startsWith("down")) {
                    modelXAngle = 270;
                    continue;
                }
                is = context.getAssets().open(s);
                byte[] b = new byte[is.available()];
                is.read(b);
                is.close();
                JSONObject jsonObject = new JSONObject(new String(b));

                do {
                    JSONObject textures = jsonObject.optJSONObject("textures");
                    if (jsonObject.has("parent")) {
                        InputStream is2 = context.getAssets().open("models/" + jsonObject.getString("parent") + ".json");
                        byte[] b2 = new byte[is2.available()];
                        is2.read(b2);
                        is2.close();
                        jsonObject = new JSONObject(new String(b2));
                    }
                    String object = jsonObject.toString();
                    int i = object.indexOf("#");
                    while (i != -1) {
                        int j = object.indexOf("\"", i);
                        String newString;
                        if (textures != null) {
                            if (textures.has(object.substring(i + 1, j))) {
                                newString = textures.getString(object.substring(i + 1, j));
                            } else {
                                newString = jsonObject.getJSONObject("textures").getString(object.substring(i + 1, j));
                            }
                        } else {
                            newString = jsonObject.getJSONObject("textures").getString(object.substring(i + 1, j));
                        }
                        String newPart1 = object.substring(0, i);
                        String newPart2 = object.substring(j);
                        object = newPart1 + newString + newPart2;
                        i = object.indexOf("#");
                    }
                    jsonObject = new JSONObject(object);
                } while (jsonObject.has("parent"));
                JSONArray elements = jsonObject.getJSONArray("elements");
                for (int i = 0; i < elements.length(); i++) {
                    float[] from = new float[3];
                    float[] to = new float[3];
                    JSONObject element = elements.getJSONObject(i);

                    JSONArray jFrom = element.getJSONArray("from");
                    for (int j = 0; j < jFrom.length(); j++)
                        from[j] = (float) (jFrom.getDouble(j) / 16);
                    JSONArray jTo = element.getJSONArray("to");
                    for (int j = 0; j < jTo.length(); j++) to[j] = (float) (jTo.getDouble(j) / 16);

                    float[] fOrigin = null;
                    int rotAngle = 0;
                    String axis = "";
                    if (element.has("rotation")) {
                        JSONObject rotation = element.getJSONObject("rotation");
                        fOrigin = new float[3];
                        JSONArray jOrigin = rotation.getJSONArray("origin");
                        for (int j = 0; j < jOrigin.length(); j++)
                            fOrigin[j] = (float) (jOrigin.getDouble(j) / 16);
                        rotAngle = rotation.getInt("angle");
                        axis = rotation.getString("axis");
                    }
                    String[] faces = {"north", "west", "south", "east", "up", "down"};
                    float[][] squareCoords = {
                            //north
                            {to[0], to[1], from[2],
                                    to[0], from[1], from[2],
                                    from[0], from[1], from[2],
                                    from[0], to[1], from[2]},
                            //west
                            {from[0], to[1], from[2],
                                    from[0], from[1], from[2],
                                    from[0], from[1], to[2],
                                    from[0], to[1], to[2]},
                            //south
                            {from[0], to[1], to[2],
                                    from[0], from[1], to[2],
                                    to[0], from[1], to[2],
                                    to[0], to[1], to[2]},
                            //east
                            {to[0], to[1], to[2],
                                    to[0], from[1], to[2],
                                    to[0], from[1], from[2],
                                    to[0], to[1], from[2]},
                            //up
                            {to[0], to[1], to[2],
                                    to[0], to[1], from[2],
                                    from[0], to[1], from[2],
                                    from[0], to[1], to[2]},
                            //down
                            {to[0], from[1], to[2],
                                    to[0], from[1], from[2],
                                    from[0], from[1], from[2],
                                    from[0], from[1], to[2]}
                    };
                    for (int j = 0; j < faces.length; j++) {
                        if (element.getJSONObject("faces").has(faces[j])) {
                            float[] squareCoords1 = squareCoords[j];
                            float[] textureCoords1;
                            JSONObject face = element.getJSONObject("faces").getJSONObject(faces[j]);
                            if (face.has("uv")) {
                                JSONArray uv = face.getJSONArray("uv");
                                textureCoords1 = new float[]{
                                        (float) (uv.getDouble(0) / 16), (float) (uv.getDouble(1) / 16),
                                        (float) (uv.getDouble(0) / 16), (float) (uv.getDouble(3) / 16),
                                        (float) (uv.getDouble(2) / 16), (float) (uv.getDouble(3) / 16),
                                        (float) (uv.getDouble(2) / 16), (float) (uv.getDouble(1) / 16)
                                };
                            } else {
                                textureCoords1 = new float[]{
                                        0.0f, 0.0f,
                                        0.0f, 1.0f,
                                        1.0f, 1.0f,
                                        1.0f, 0.0f
                                };
                            }
                            float[] color2;


                            if (face.has("tintindex")) {
                                color2 = new float[]{0.0f, 1.0f, 0.0f, 1.0f};
                            } else {
                                color2 = new float[]{1.0f, 1.0f, 1.0f, 1.0f};
                            }

                            if (face.has("rotation"))
                                textureCoords1 = rotateUV(textureCoords1, face.getInt("rotation"));

                            String texture1 = element.getJSONObject("faces").getJSONObject(faces[j]).getString("texture");
                            String textureType = texture1.split("/")[0];
                            String textureName = texture1.split("/")[1];
                            TextureAtlas atlas = TextureAtlas.atlases.get(textureType);
                            assert atlas != null : "Atlas is null: " + textureType;
                            assert atlas.offsets.containsKey(textureName + ".png") : "Texture " + textureName + " not found in " + textureType + " atlas";
                            float offset = atlas.offsets.get(textureName + ".png");
                            int atlasWidth = atlas.width;
                            int atlasHeight = atlas.height;
                            float[] textureCoords2 = new float[textureCoords1.length];
                            for (int k = 0; k < textureCoords1.length; k++) {
                                if (k % 2 == 0) {
                                    textureCoords2[k] = textureCoords1[k] * 16 / atlasWidth + offset;
                                } else {
                                    textureCoords2[k] = textureCoords1[k] * 16 / atlasHeight;
                                }
                            }

                            Square square1 = new Square(squareCoords1, color2, textureCoords2, atlas, j);
                            if (element.has("rotation")) {
                                int rotationAxis = -1;
                                if (axis.equals("x")) rotationAxis = 0;
                                if (axis.equals("y")) rotationAxis = 1;
                                if (axis.equals("z")) rotationAxis = 2;
                                assert rotationAxis != -1;
                                rotateSquare(square1, rotAngle, rotationAxis, fOrigin[0], fOrigin[1], fOrigin[2]);
                                square1.splitCoords();
                            }
                            modelSquares.add(square1);
                        }
                    }
                }
            }
            if (modelXAngle != 0) {
                int[] xRotationsTable = {4, 1, 5, 3, 2, 0};
                for (Square square : modelSquares) {
                    rotateSquare(square, modelXAngle, 0, 0.5f, 0.5f, 0.5f);
                    square.splitCoords();
                    for (float i = modelXAngle; i > 0; i -= 90)
                        square.direction = xRotationsTable[square.direction];
                }
            }
            if (modelAngle != 0)
                for (Square square : modelSquares) {
                    rotateSquare(square, modelAngle, 1, 0.5f, 0.5f, 0.5f);
                    square.splitCoords();
                    if (square.direction < 4)
                        square.direction = (square.direction + ((int) modelAngle) / 90) % 4;
                }
            models.put(model + id + " " + metadata, modelSquares);
        }

        for (Square modelSquare : Objects.requireNonNull(models.get(model + id + " " + metadata))
        ) {
            Square square = modelSquare.copy();
            if (angle != 0) {
                rotateSquare(square, angle, 1, 0.5f, 0.5f, 0.5f);
                square.splitCoords();
            }
            if (upsideDown) {
                flipSquare(square);
                square.splitCoords();
            }
            addCoordinates(square.coords1, x, y, z);
            addCoordinates(square.coords2, x, y, z);
            if (square.direction < 4)
                square.direction = (square.direction + ((int) angle) / 90) % 4;
            if (upsideDown && (square.direction > 3))
                square.direction = (square.direction + 1) % 2 + 4;
            squares.add(square);
        }
        coordsBuffer = ByteBuffer.allocateDirect(squares.size() * 6 * 3 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        colorsBuffer = ByteBuffer.allocateDirect(squares.size() * 6 * 4 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        texturesBuffer = ByteBuffer.allocateDirect(squares.size() * 6 * 2 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        for (Square square : squares) {
            coordsBuffer.put(square.coords1);
            coordsBuffer.put(square.coords2);
            colorsBuffer.put(square.squareColors);
            texturesBuffer.put(square.textures1);
            texturesBuffer.put(square.textures2);
        }
        coordsBuffer.position(0);
        colorsBuffer.position(0);
        texturesBuffer.position(0);
    }

    public void render() {
        for (Square square : squares) {

            //skip rendering if the square is not visible
            switch (square.direction) {
                //north
                case 0:
                    if (PacketUtils.z > square.coords1[2]) continue;
                    break;
                //west
                case 1:
                    if (PacketUtils.x > square.coords1[0]) continue;
                    break;
                //south
                case 2:
                    if (PacketUtils.z < square.coords1[2]) continue;
                    break;
                //east
                case 3:
                    if (PacketUtils.x < square.coords1[0]) continue;
                    break;
                //up
                case 4:
                    if (PacketUtils.y + 1.62 < square.coords1[1]) continue;
                    break;
                //down
                case 5:
                    if (PacketUtils.y + 1.62 > square.coords1[1]) continue;
                    break;
            }

            square.render();
        }
    }

    //flip the square upside down
    static void flipSquare(Square square) {
        float[] coords = square.squareCoords;
        for (int i = 1; i < coords.length; i += 3) {
            coords[i] = coords[i] - 0.5f;
            coords[i] = -coords[i];
            coords[i] = coords[i] + 0.5f;
        }
    }

    static void rotateSquare(Square square, float angle, int rotationAxis, float originX, float originY, float originZ) {
        float x = 0;
        float y = 0;
        float z = 0;
        switch (rotationAxis) {
            case 0:
                x = 1;
                break;
            case 1:
                y = 1;
                break;
            case 2:
                z = 1;
                break;
        }
        List<Float> coords = new ArrayList<>();
        for (float f : square.squareCoords) {
            coords.add(f);
        }
        coords.add(12, 1f);
        coords.add(9, 1f);
        coords.add(6, 1f);
        coords.add(3, 1f);
        float[] rotationMatrix = new float[16];
        float[] matrix = new float[16];
        for (int i = 0; i < 16; i += 4) {
            matrix[i] = coords.get(i) - originX;
            matrix[i + 1] = coords.get(i + 1) - originY;
            matrix[i + 2] = coords.get(i + 2) - originZ;
            matrix[i + 3] = coords.get(i + 3);
        }
        Matrix.setRotateM(rotationMatrix, 0, angle, x, y, z);
        Matrix.multiplyMM(matrix, 0, rotationMatrix, 0, matrix, 0);

        coords.clear();
        for (float f : matrix) {
            coords.add(f);
        }
        coords.remove(15);
        coords.remove(11);
        coords.remove(7);
        coords.remove(3);
        float[] result = new float[12];
        for (int i = 0; i < 12; i += 3) {
            result[i] = coords.get(i) + originX;
            result[i + 1] = coords.get(i + 1) + originY;
            result[i + 2] = coords.get(i + 2) + originZ;
        }
        square.squareCoords = result;
    }

    static float[] rotateUV(float[] uv, float angle) {
        List<Float> coords = new ArrayList<>();
        for (float f : uv) {
            coords.add(f);
        }
        for (int i = 8; i > 0; i--) {
            coords.add(i, 2.5f);
        }
        float[] rotationMatrix = new float[16];
        float[] matrix = new float[16];
        for (int i = 0; i < 16; i++) {
            matrix[i] = coords.get(i) - 0.5f;
        }
        Matrix.setRotateM(rotationMatrix, 0, angle, 0, 1, 0);
        Matrix.multiplyMM(matrix, 0, rotationMatrix, 0, matrix, 0);
        coords.clear();
        for (float f : matrix) {
            coords.add(Math.abs(f) < 0.0001 ? 0.0f : f);
        }
        for (int i = 8; i > 0; i--) {
            coords.remove(2 * i - 1);
        }
        float[] result = new float[8];
        for (int i = 0; i < 8; i++) {
            result[i] = coords.get(i) + 0.5f;
        }
        return result;
    }

    static void addCoordinates(float[] in, float x, float y, float z) {
        for (int i = 0; i < in.length; i++) {
            int a = i % 3;
            switch (a) {
                case 0:
                    in[i] = in[i] + x;
                    break;
                case 1:
                    in[i] = in[i] + y;
                    break;
                case 2:
                    in[i] = in[i] + z;
                    break;
            }
        }
    }

    static String getMultiStateBlockModel(int id, int metadata, String model) {
        switch (id) {

            //logs
            case 17:
            case 162:
                if ((metadata & 12) == 4) model = model + "amongus" + "angle090";
                break;

            //dispenser and dropper
            case 23:
            case 158:
                if ((metadata & 7) == 0) {
                    model = model.replaceFirst(".json", "_vertical.json") + "amongus" + "flipped";
                } else if ((metadata & 7) == 1) {
                    model = model.replaceFirst(".json", "_vertical.json");
                } else if ((metadata & 7) == 3) {
                    model = model + "amongus" + "angle180";
                } else if ((metadata & 7) == 4) {
                    model = model + "amongus" + "angle90";
                } else if ((metadata & 7) == 5) {
                    model = model + "amongus" + "angle270";
                }
                break;

            //beds
            case 26:
                if ((metadata & 8) == 8) {
                    model = "models/block/bed_head.json";
                } else {
                    model = "models/block/bed_foot.json";
                }
                if ((metadata & 3) == 3) {
                    model = model + "amongus" + "angle090";
                } else if ((metadata & 3) == 2) {
                    model = model + "amongus" + "angle180";
                } else if ((metadata & 3) == 1) {
                    model = model + "amongus" + "angle270";
                }
                break;

            //rails TODO add rescaling for raised rails
            case 27:
            case 28:
            case 66:
            case 157:
                String type = "normal_rail_";
                if (id == 27) type = "golden_rail_";
                else if (id == 28) type = "detector_rail_";
                else if (id == 157) type = "activator_rail_";
                if ((metadata & 8) == 8) {
                    if (id == 28) type = type + "powered_";
                    if ((id == 27) || (id == 157)) type = type + "active_";
                }
                if ((metadata & 7) == 0) {
                    model = "models/block/" + type + "flat.json";
                } else if ((metadata & 7) == 1) {
                    model = "models/block/" + type + "flat.json";
                    model = model + "amongus" + "angle090";
                } else if ((metadata & 7) == 2) {
                    model = "models/block/" + type + "raised_ne.json";
                    model = model + "amongus" + "angle270";
                } else if ((metadata & 7) == 3) {
                    model = "models/block/" + type + "raised_sw.json";
                    model = model + "amongus" + "angle270";
                } else if ((metadata & 7) == 4) {
                    model = "models/block/" + type + "raised_ne.json";
                } else if ((metadata & 7) == 5) {
                    model = "models/block/" + type + "raised_sw.json";
                }
                if (id == 66) {
                    if (metadata == 6) {
                        model = "models/block/normal_rail_curved.json";
                    } else if (metadata == 7) {
                        model = "models/block/normal_rail_curved.json";
                        model = model + "amongus" + "angle090";
                    } else if (metadata == 8) {
                        model = "models/block/normal_rail_curved.json";
                        model = model + "amongus" + "angle180";
                    } else if (metadata == 9) {
                        model = "models/block/normal_rail_curved.json";
                        model = model + "amongus" + "angle270";
                    }
                }
                break;

            //pistons FIXME uv coordinates broken (i changed piston.json to fix one issue but idk what is wrong with this much things)
            case 29:
            case 33:
                if ((metadata & 8) == 8) {
                    model = "models/block/piston_extended_normal.json";
                } else {
                    if (id == 29) model = "models/block/sticky_piston.json";
                    else model = "models/block/piston_normal.json";
                }
                if ((metadata & 7) == 0) {
                    model = model + "amongus" + "down";
                } else if ((metadata & 7) == 1) {
                    model = model + "amongus" + "up";
                } else if ((metadata & 7) == 3) {
                    model = model + "amongus" + "angle180";
                } else if ((metadata & 7) == 4) {
                    model = model + "amongus" + "angle90";
                } else if ((metadata & 7) == 5) {
                    model = model + "amongus" + "angle270";
                }
                break;

            //piston head
            case 34:
                if ((metadata & 8) == 0) {
                    model = "models/block/piston_head_normal.json";
                } else {
                    model = "models/block/piston_head_sticky.json";
                }
                if ((metadata & 7) == 0) {
                    model = model + "amongus" + "down";
                } else if ((metadata & 7) == 1) {
                    model = model + "amongus" + "up";
                } else if ((metadata & 7) == 3) {
                    model = model + "amongus" + "angle180";
                } else if ((metadata & 7) == 4) {
                    model = model + "amongus" + "angle90";
                } else if ((metadata & 7) == 5) {
                    model = model + "amongus" + "angle270";
                }
                break;

            //double slabs
            case 43:
            case 125:
            case 181:
                model = model + "amongus" + model.replaceFirst("half", "upper");
                break;

            //slabs
            case 44:
            case 126:
            case 182:
                if ((metadata & 0x08) == 0x08) {
                    assert model.contains("half") : "Model doesn't contain half: " + model;
                    model = model.replaceFirst("half", "upper");
                }
                break;

            //wheat
            case 59:
                model = "models/block/wheat_stage" + (metadata & 0x07) + ".json";
                break;

            //farmland
            case 60:
                if ((metadata & 0x07) == 0x07) {
                    model = "models/block/farmland_moist.json";
                } else {
                    model = "models/block/farmland_dry.json";
                }
                break;

            //ladder and furnace
            case 61:
            case 62:
            case 65:
                if ((metadata & 0x07) == 0x03) {
                    model = model + "amongus" + "angle180";
                } else if ((metadata & 0x07) == 0x04) {
                    model = model + "amongus" + "angle90";
                } else if ((metadata & 0x07) == 0x05) {
                    model = model + "amongus" + "angle270";
                }
                break;

            //lever
            case 69:
                if (!((metadata & 0x08) == 0x08)) model = model.replaceFirst(".json", "_off.json");
                if ((metadata & 0x07) == 0x00) {
                    model = model + "amongus" + "flipped";
                    model = model + "amongus" + "angle270";
                } else if ((metadata & 0x07) == 0x01) {
                    model = model + "amongus" + "down";
                    model = model + "amongus" + "angle270";
                } else if ((metadata & 0x07) == 0x02) {
                    model = model + "amongus" + "down";
                    model = model + "amongus" + "angle90";
                } else if ((metadata & 0x07) == 0x03) {
                    model = model + "amongus" + "down";
                    model = model + "amongus" + "angle180";
                } else if ((metadata & 0x07) == 0x04) {
                    model = model + "amongus" + "down";
                } else if ((metadata & 0x07) == 0x06) {
                    model = model + "amongus" + "angle90";
                } else if ((metadata & 0x07) == 0x07) {
                    model = model + "amongus" + "flipped";
                    model = model + "amongus" + "angle180";
                }
                break;

            //pressure plates
            case 70:
            case 72:
            case 147:
            case 148:
                type = "stone";
                if (id == 72) type = "wooden";
                else if (id == 147) type = "light";
                else if (id == 148) type = "heavy";
                if (metadata == 0) {
                    model = "models/block/" + type + "_pressure_plate_up.json";
                } else {
                    model = "models/block/" + type + "_pressure_plate_down.json";
                }
                break;

            //buttons
            case 77:
            case 143:
                type = "stone";
                if (id == 143) type = "wooden";
                if ((metadata & 0x08) == 0x08) {
                    model = "models/block/" + type + "_button_pressed.json";
                } else {
                    model = "models/block/" + type + "_button.json";
                }
                if ((metadata & 0x07) == 0x00) {
                    model = model + "amongus" + "flipped";
                } else if ((metadata & 0x07) == 0x01) {
                    model = model + "amongus" + "up";
                    model = model + "amongus" + "angle090";
                } else if ((metadata & 0x07) == 0x02) {
                    model = model + "amongus" + "up";
                    model = model + "amongus" + "angle270";
                } else if ((metadata & 0x07) == 0x03) {
                    model = model + "amongus" + "up";
                } else if ((metadata & 0x07) == 0x04) {
                    model = model + "amongus" + "up";
                    model = model + "amongus" + "angle180";

                }
                break;

            //pumpkin and jack o lantern
            case 86:
            case 91:
                if ((metadata & 0x07) == 0x00) {
                    model = model + "amongus" + "angle180";
                } else if ((metadata & 0x07) == 0x01) {
                    model = model + "amongus" + "angle090";
                } else if ((metadata & 0x07) == 0x03) {
                    model = model + "amongus" + "angle270";
                }
                break;

            //repeaters TODO locked repeaters
            case 93:
            case 94:
                type = "repeater";
                if (id == 94) type = "repeater_on";
                if ((metadata & 12) == 0) {
                    model = "models/block/" + type + "_1tick.json";
                } else if ((metadata & 12) == 4) {
                    model = "models/block/" + type + "_2tick.json";
                } else if ((metadata & 12) == 8) {
                    model = "models/block/" + type + "_3tick.json";
                } else {
                    model = "models/block/" + type + "_4tick.json";
                }
                if ((metadata & 3) == 1) {
                    model = model + "amongus" + "angle270";
                } else if ((metadata & 3) == 0) {
                    model = model + "amongus" + "angle180";
                } else if ((metadata & 3) == 3) {
                    model = model + "amongus" + "angle090";
                }
                break;

            //trapdoors
            case 96:
            case 167:
                type = "wooden";
                if (id == 167) type = "iron";
                if ((metadata & 0x04) == 0x04) {
                    model = "models/block/" + type + "_trapdoor_open.json";
                } else if ((metadata & 0x08) == 0x08) {
                    model = "models/block/" + type + "_trapdoor_top.json";
                } else {
                    model = "models/block/" + type + "_trapdoor_bottom.json";
                }
                if ((metadata & 0x03) == 0x01) {
                    model = model + "amongus" + "angle180";
                } else if ((metadata & 0x03) == 0x02) {
                    model = model + "amongus" + "angle090";
                } else if ((metadata & 0x03) == 0x03) {
                    model = model + "amongus" + "angle270";
                }
                break;

            //mushroom blocks
            case 99:
            case 100:
                type = "brown_mushroom_block";
                if (id == 100) type = "red_mushroom_block";

                if (metadata == 1) model = "models/block/" + type + "_nw.json";
                else if (metadata == 2) model = "models/block/" + type + "_n.json";
                else if (metadata == 3) model = "models/block/" + type + "_ne.json";
                else if (metadata == 4) model = "models/block/" + type + "_w.json";
                else if (metadata == 5) model = "models/block/" + type + "_c.json";
                else if (metadata == 6) model = "models/block/" + type + "_e.json";
                else if (metadata == 7) model = "models/block/" + type + "_sw.json";
                else if (metadata == 8) model = "models/block/" + type + "_s.json";
                else if (metadata == 9) model = "models/block/" + type + "_se.json";
                else if (metadata == 10) model = "models/block/" + type + "_stem.json";
                else if (metadata == 14) model = "models/block/" + type + "_cap_all.json";
                else if (metadata == 15) model = "models/block/" + type + "_stem_all.json";
                else model = "models/block/" + type + "_inside_all.json";
                break;

            //fence gates TODO connect fences to this
            case 107:
            case 183:
            case 184:
            case 185:
            case 186:
            case 187:
                type = "oak";
                if (id == 183) type = "spruce";
                else if (id == 184) type = "birch";
                else if (id == 185) type = "jungle";
                else if (id == 186) type = "dark_oak";
                else if (id == 187) type = "acacia";

                if ((metadata & 0x04) == 0x04) {
                    model = "models/block/" + type + "_fence_gate_open.json";
                } else {
                    model = "models/block/" + type + "_fence_gate_closed.json";
                }
                if ((metadata & 0x03) == 0x02) {
                    model = model + "amongus" + "angle180";
                } else if ((metadata & 0x03) == 0x01) {
                    model = model + "amongus" + "angle270";
                } else if ((metadata & 0x03) == 0x03) {
                    model = model + "amongus" + "angle090";
                }
                break;

            //nether wart
            case 115:
                int stage3 = 0;
                if (metadata > 0) stage3 = 1;
                if (metadata > 2) stage3 = 2;
                model = "models/block/nether_wart_stage" + stage3 + ".json";
                break;

            //brewing stand
            case 117:
                if (metadata == 0) {
                    model = "models/block/brewing_stand_empty";
                } else {
                    model = "models/block/brewing_stand_bottles_";
                }
                if ((metadata & 1) == 1) model = model + 1;
                if ((metadata & 2) == 2) model = model + 2;
                if ((metadata & 4) == 4) model = model + 3;
                model = model + ".json";
                break;

            //end portal frame
            case 120:
                if ((metadata & 4) == 4) {
                    model = "models/block/end_portal_frame_filled.json";
                } else {
                    model = "models/block/end_portal_frame_empty.json";
                }
                if ((metadata & 3) == 1) {
                    model = model + "amongus" + "angle090";
                } else if ((metadata & 3) == 0) {
                    model = model + "amongus" + "angle180";
                } else if ((metadata & 3) == 3) {
                    model = model + "amongus" + "angle270";
                }
                break;

            //comparator
            case 149:
            case 150:
                if ((metadata & 8) == 8) {
                    model = "models/block/comparator_lit";
                } else {
                    model = "models/block/comparator_unlit";
                }
                if ((metadata & 4) == 4) {
                    model = model + "_subtract.json";
                } else {
                    model = model + ".json";
                }
                if ((metadata & 3) == 1) {
                    model = model + "amongus" + "angle270";
                } else if ((metadata & 3) == 2) {
                    model = model + "amongus" + "angle180";
                } else if ((metadata & 3) == 3) {
                    model = model + "amongus" + "angle090";
                }
                break;

            //carrots
            case 141:
                int stage = 0;
                if (metadata > 1) stage = 1;
                if (metadata > 3) stage = 2;
                if (metadata > 6) stage = 3;
                model = "models/block/carrots_stage" + stage + ".json";
                break;

            //potatoes
            case 142:
                int stage2 = 0;
                if (metadata > 1) stage2 = 1;
                if (metadata > 3) stage2 = 2;
                if (metadata > 6) stage2 = 3;
                model = "models/block/potatoes_stage" + stage2 + ".json";
                break;

            //anvil
            case 145:
                if ((metadata & 12) == 0) {
                    model = "models/block/anvil_undamaged.json";
                } else if ((metadata & 12) == 4) {
                    model = "models/block/anvil_slightly_damaged.json";
                } else if ((metadata & 12) == 8) {
                    model = "models/block/anvil_very_damaged.json";
                }
                if ((metadata & 3) == 1) {
                    model = model + "amongus" + "angle270";
                } else if ((metadata & 3) == 2) {
                    model = model + "amongus" + "angle180";
                } else if ((metadata & 3) == 3) {
                    model = model + "amongus" + "angle090";
                }
                break;

            //hoppers
            case 154:
                if ((metadata & 0x07) == 0x00) {
                    model = "models/block/hopper_down.json";
                    break;
                } else {
                    model = "models/block/hopper_side.json";
                }
                if ((metadata & 0x07) == 0x03) {
                    model = model + "amongus" + "angle180";
                } else if ((metadata & 0x07) == 0x04) {
                    model = model + "amongus" + "angle90";
                } else if ((metadata & 0x07) == 0x05) {
                    model = model + "amongus" + "angle270";
                }
                break;

            //double plants FIXME fix top block always being double grass
            case 175:
                model = model.substring(model.indexOf("item/") + 5, model.indexOf(".json"));
                model = model.replace("double_", "");
                if ((metadata & 0x08) == 0x08) {
                    model = "models/block/double_" + model + "_top.json";
                } else {
                    model = "models/block/double_" + model + "_bottom.json";
                }
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + id);
        }
        return model;
    }

    String getSpecialBlockModel(int id, int metadata, int x, int y, int z) {
        String type = null;
        switch (id) {
            case 2:
                if (ChunkColumn.getBlockId(x, y + 1, z) == 78 || ChunkColumn.getBlockId(x, y + 1, z) == 80) {
                    return "models/block/grass_snowed.json";
                } else {
                    return "models/block/grass_normal.json";
                }

                //stairs
            case 53:
            case 67:
            case 108:
            case 109:
            case 114:
            case 128:
            case 134:
            case 135:
            case 136:
            case 156:
            case 163:
            case 164:
            case 180:
                Set<Integer> stairs = new HashSet<>(Arrays.asList(53, 67, 108, 109, 114, -128, -122, -121, -120, -10, -93, -92, -76));
                try {
                    JSONObject block = Objects.requireNonNull(Slot.blocksMap.get(id));
                    if (block.has("itemModel")) {
                        type = block.getString("itemModel");
                    } else {
                        type = block.getString("displayName").trim().toLowerCase().replaceAll(" ", "_");
                    }
                    type = type.replaceFirst("_stairs", "");
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                //FIXME if angle is 0, it doesn't work(it doesnt call rotateSquare)
                if ((metadata & 3) == 0) angle = 360;
                if ((metadata & 3) == 1) angle = 180;
                if ((metadata & 3) == 2) angle = 270;
                if ((metadata & 3) == 3) angle = 90;
                if ((metadata & 4) == 4) upsideDown = true;
                //if facing north or south
                if ((metadata & 2) == 2) {
                    //block in north
                    short neighbor1 = ChunkColumn.getBlock(x, y, z - 1);
                    int neighbor1Id = ChunkColumn.getBlockId(neighbor1);
                    int neighbor1Meta = ChunkColumn.getBlockMetaData(neighbor1);
                    //block in south
                    short neighbor2 = ChunkColumn.getBlock(x, y, z + 1);
                    int neighbor2Id = ChunkColumn.getBlockId(neighbor2);
                    int neighbor2Meta = ChunkColumn.getBlockMetaData(neighbor2);
                    if (stairs.contains(neighbor1Id) && (neighbor1Meta & 0x02) == 0) {
                        if ((metadata & 1) == 1) {
                            if ((neighbor1Meta & 1) == 1) angle += 90;
                            return "models/block/" + type + "_outer_stairs.json";
                        } else {
                            if ((neighbor1Meta & 1) == 0) angle += 90;
                            return "models/block/" + type + "_inner_stairs.json";
                        }
                    }
                    if (stairs.contains(neighbor2Id) && (neighbor2Meta & 0x02) == 0) {
                        if ((metadata & 1) == 1) {
                            if ((neighbor2Meta & 1) == 1) angle += 90;
                            return "models/block/" + type + "_inner_stairs.json";
                        } else {
                            if ((neighbor2Meta & 1) == 0) angle += 90;
                            return "models/block/" + type + "_outer_stairs.json";
                        }
                    }
                }
                //if facing east or west
                else {
                    //block in west
                    short neighbor1 = ChunkColumn.getBlock(x - 1, y, z);
                    int neighbor1Id = ChunkColumn.getBlockId(neighbor1);
                    int neighbor1Meta = ChunkColumn.getBlockMetaData(neighbor1);
                    //block in east
                    short neighbor2 = ChunkColumn.getBlock(x + 1, y, z);
                    int neighbor2Id = ChunkColumn.getBlockId(neighbor2);
                    int neighbor2Meta = ChunkColumn.getBlockMetaData(neighbor2);
                    if (stairs.contains(neighbor1Id) && (neighbor1Meta & 2) == 2) {
                        if ((metadata & 1) == 1) {
                            if ((neighbor1Meta & 1) == 0) angle += 90;
                            return "models/block/" + type + "_outer_stairs.json";
                        } else {
                            if ((neighbor1Meta & 1) == 1) angle += 90;
                            return "models/block/" + type + "_inner_stairs.json";
                        }
                    }
                    if (stairs.contains(neighbor2Id) && (neighbor2Meta & 0x02) == 2) {
                        if ((metadata & 1) == 1) {
                            if ((neighbor2Meta & 1) == 0) angle += 90;
                            return "models/block/" + type + "_inner_stairs.json";
                        } else {
                            if ((neighbor2Meta & 1) == 1) angle += 90;
                            return "models/block/" + type + "_outer_stairs.json";
                        }
                    }
                }
                return "models/block/" + type + "_stairs.json";

            //fences
            case 85:
            case 113:
            case 188:
            case 189:
            case 190:
            case 191:
            case 192:
                if (id == 85) type = "oak";
                else if (id == 113) type = "nether_brick";
                else if (id == 188) type = "spruce";
                else if (id == 189) type = "birch";
                else if (id == 190) type = "jungle";
                else if (id == 191) type = "dark_oak";
                else type = "acacia";
                boolean north = ChunkColumn.getBlockId(x, y, z - 1) != 0;
                boolean west = ChunkColumn.getBlockId(x - 1, y, z) != 0;
                boolean south = ChunkColumn.getBlockId(x, y, z + 1) != 0;
                boolean east = ChunkColumn.getBlockId(x + 1, y, z) != 0;
                int[] sides = new int[4];
                if (north) sides[0] = 1;
                if (west) sides[1] = 1;
                if (south) sides[2] = 1;
                if (east) sides[3] = 1;
                int sum = sides[0] + sides[1] + sides[2] + sides[3];
                if (sum == 0) {
                    return "models/block/" + type + "_fence_post.json";
                } else if (sum == 1) {
                    for (int i = 0; i < 4; i++) {
                        if (sides[i] == 1) {
                            return "models/block/" + type + "_fence_n.json";
                        }
                        angle += 90;
                    }
                } else if (sum == 2) {
                    for (int i = 0; i < 4; i++) {
                        if (sides[i] == 1 && sides[(i + 3) % 4] == 1) {
                            return "models/block/" + type + "_fence_ne.json";
                        }
                        if (sides[i] == 1 && sides[(i + 2) % 4] == 1) {
                            return "models/block/" + type + "_fence_ns.json";
                        }
                        angle += 90;
                    }
                } else if (sum == 3) {
                    angle = 270;
                    for (int i = 0; i < 4; i++) {
                        if (sides[i] == 0) {
                            return "models/block/" + type + "_fence_nse.json";
                        }
                        angle += 90;
                    }
                } else if (sum == 4) {
                    return "models/block/" + type + "_fence_nsew.json";
                }

                //stems //TODO add coloring
            case 104:
                //checking 4 sides for pumpkin
                int isPumpkinFruit = -1;
                if (ChunkColumn.getBlockId(x + 1, y, z) == 86) isPumpkinFruit = 2;
                if (ChunkColumn.getBlockId(x - 1, y, z) == 86) isPumpkinFruit = 0;
                if (ChunkColumn.getBlockId(x, y, z + 1) == 86) isPumpkinFruit = 1;
                if (ChunkColumn.getBlockId(x, y, z - 1) == 86) isPumpkinFruit = 3;
                if (isPumpkinFruit != -1) {
                    angle = isPumpkinFruit * 90;
                    return "models/block/pumpkin_stem_fruit" + ".json";
                } else {
                    return "models/block/pumpkin_stem_growth" + metadata + ".json";
                }
            case 105:
                //checking 4 sides for melon
                int isMelonFruit = -1;
                if (ChunkColumn.getBlockId(x + 1, y, z) == 103) isMelonFruit = 3;
                if (ChunkColumn.getBlockId(x - 1, y, z) == 103) isMelonFruit = 1;
                if (ChunkColumn.getBlockId(x, y, z + 1) == 103) isMelonFruit = 2;
                if (ChunkColumn.getBlockId(x, y, z - 1) == 103) isMelonFruit = 0;
                if (isMelonFruit != -1) {
                    return "models/block/melon_stem_fruit" + ".json";
                } else {
                    return "models/block/melon_stem_growth" + metadata + ".json";
                }

                //vines
            case 106:
                boolean up = ChunkColumn.getBlockId(x, y + 1, z) != 0;
                String u = up ? "u" : "";
                north = (metadata & 4) == 4;
                west = (metadata & 2) == 2;
                south = (metadata & 1) == 1;
                east = (metadata & 8) == 8;
                sides = new int[4];
                if (north) sides[0] = 1;
                if (west) sides[1] = 1;
                if (south) sides[2] = 1;
                if (east) sides[3] = 1;
                sum = sides[0] + sides[1] + sides[2] + sides[3];
                if (sum == 0)
                    return "models/block/vine_u.json";
                else if (sum == 1) {
                    angle = 180;
                    for (int i = 0; i < 4; i++) {
                        if (sides[i] == 1) {
                            return "models/block/vine_1" + u + ".json";
                        }
                        angle += 90;
                    }
                } else if (sum == 2) {
                    for (int i = 0; i < 4; i++) {
                        if (sides[i] == 1 && sides[(i + 3) % 4] == 1) {
                            return "models/block/vine_2" + u + ".json";
                        }
                        if (sides[i] == 1 && sides[(i + 2) % 4] == 1) {
                            angle += 90;
                            return "models/block/vine_2" + u + "_opposite.json";
                        }
                        angle += 90;
                    }
                } else if (sum == 3) {
                    angle = 270;
                    for (int i = 0; i < 4; i++) {
                        if (sides[i] == 0) {
                            return "models/block/vine_3" + u + ".json";
                        }
                        angle += 90;
                    }
                } else if (sum == 4) {
                    return "models/block/vine_4" + u + ".json";
                }
                //doors FIXME hitboxes are broken
            case 64:
            case 71:
            case 193:
            case 194:
            case 195:
            case 196:
            case 197:
                if (id == 64) type = "wooden";
                else if (id == 71) type = "iron";
                else if (id == 193) type = "spruce";
                else if (id == 194) type = "birch";
                else if (id == 195) type = "jungle";
                else if (id == 196) type = "acacia";
                else type = "dark_oak";
                String part;
                int hinge;
                int facing;
                boolean open;
                if ((metadata & 0x08) == 0x08) {
                    part = "top";
                    short bottom = ChunkColumn.getBlock(x, y - 1, z);
                    hinge = metadata & 1;
                    facing = ChunkColumn.getBlockMetaData(bottom) & 3;
                    open = (ChunkColumn.getBlockMetaData(bottom) & 4) == 4;
                } else {
                    part = "bottom";
                    short top = ChunkColumn.getBlock(x, y + 1, z);
                    hinge = ChunkColumn.getBlockMetaData(top) & 1;
                    facing = metadata & 3;
                    open = (metadata & 4) == 4;
                }
                if (facing == 0) angle = 360;
                if (facing == 1) angle = 270;
                if (facing == 2) angle = 180;
                if (facing == 3) angle = 90;
                if (open) {
                    angle -= 90;
                    angle += 180 * hinge;
                    hinge = (hinge + 1) % 2;
                }
                if (hinge == 1) {
                    return "models/block/" + type + "_door_" + part + "_rh.json";
                } else {
                    return "models/block/" + type + "_door_" + part + ".json";
                }

                //tripwire hook
            case 131:
                type = "tripwire_hook";
                if ((metadata & 0x04) == 0x04) {
                    type = type + "_attached";
                    if (ChunkColumn.getBlockId(x, y - 1, z) == 0) type = type + "_suspended";
                }
                if ((metadata & 0x08) == 0x08) type = type + "_powered";
                if ((metadata & 0x03) == 0x01) {
                    angle = 90;
                } else if ((metadata & 0x03) == 0x00) {
                    angle = 180;
                } else if ((metadata & 0x03) == 0x03) {
                    angle = 270;
                }
                return "models/block/" + type + ".json";

            //tripwire FIXME fix this sometime because i dont caare enough to fix it rn
            case 132:
                type = "tripwire";
                if ((metadata & 0x04) == 0x04) type = type + "_attached";
                if (ChunkColumn.getBlockId(x, y - 1, z) == 0) type = type + "_suspended";
                north = ChunkColumn.getBlockId(x, y, z - 1) == -124;
                west = ChunkColumn.getBlockId(x - 1, y, z) == -124;
                south = ChunkColumn.getBlockId(x, y, z + 1) == -124;
                east = ChunkColumn.getBlockId(x + 1, y, z) == -124;
                sides = new int[4];
                if (north) sides[0] = 1;
                if (west) sides[1] = 1;
                if (south) sides[2] = 1;
                if (east) sides[3] = 1;
                sum = sides[0] + sides[1] + sides[2] + sides[3];
                if (sum == 0) {
                    return "models/block/" + type + "_ns.json";
                } else if (sum == 1) {
                    for (int i = 0; i < 4; i++) {
                        if (sides[i] == 1) {
                            return "models/block/" + type + "_n.json";
                        }
                        angle += 90;
                    }
                } else if (sum == 2) {
                    for (int i = 0; i < 4; i++) {
                        if (sides[i] == 1 && sides[(i + 3) % 4] == 1) {
                            return "models/block/" + type + "_ne.json";
                        }
                        if (sides[i] == 1 && sides[(i + 2) % 4] == 1) {
                            angle += 90;
                            return "models/block/" + type + "_ns.json";
                        }
                        angle += 90;
                    }
                } else if (sum == 3) {
                    angle = 270;
                    for (int i = 0; i < 4; i++) {
                        if (sides[i] == 0) {
                            return "models/block/" + type + "_nse.json";
                        }
                        angle += 90;
                    }
                } else if (sum == 4) {
                    return "models/block/" + type + "_nsew.json";
                }

                //walls
            case 139:
                type = "cobblestone";
                if (metadata == 1) type = "mossy_cobblestone";
                //similar to fences
                north = ChunkColumn.getBlockId(x, y, z - 1) != 0;
                west = ChunkColumn.getBlockId(x - 1, y, z) != 0;
                south = ChunkColumn.getBlockId(x, y, z + 1) != 0;
                east = ChunkColumn.getBlockId(x + 1, y, z) != 0;
                sides = new int[4];
                if (north) sides[0] = 1;
                if (west) sides[1] = 1;
                if (south) sides[2] = 1;
                if (east) sides[3] = 1;
                sum = sides[0] + sides[1] + sides[2] + sides[3];
                if (sum == 0) {
                    return "models/block/" + type + "_wall_post.json";
                } else if (sum == 1) {
                    for (int i = 0; i < 4; i++) {
                        if (sides[i] == 1) {
                            return "models/block/" + type + "_wall_n.json";
                        }
                        angle += 90;
                    }
                } else if (sum == 2) {
                    for (int i = 0; i < 4; i++) {
                        if (sides[i] == 1 && sides[(i + 3) % 4] == 1) {
                            return "models/block/" + type + "_wall_ne.json";
                        }
                        if (sides[i] == 1 && sides[(i + 2) % 4] == 1) {
                            if (ChunkColumn.getBlockId(x, y + 1, z) != 0)
                                return "models/block/" + type + "_wall_ns_above.json";
                            return "models/block/" + type + "_wall_ns.json";
                        }
                        angle += 90;
                    }
                } else if (sum == 3) {
                    angle = 270;
                    for (int i = 0; i < 4; i++) {
                        if (sides[i] == 0) {
                            return "models/block/" + type + "_wall_nse.json";
                        }
                        angle += 90;
                    }
                } else if (sum == 4) {
                    return "models/block/" + type + "_wall_nsew.json";
                }


            default:
                throw new IllegalStateException("Unexpected value: " + id);
        }
    }

}
