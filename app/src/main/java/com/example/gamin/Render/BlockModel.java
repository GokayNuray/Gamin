package com.example.gamin.Render;

import static com.example.gamin.Render.BlockModelUtils.getSpecialNumber;

import android.content.Context;

import com.example.gamin.Minecraft.Chunk;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class BlockModel extends Model {
    private static final Set<Integer> specialBlocks = new HashSet<>(Arrays.asList(2, 53, 64, 71, 193, 194, 195, 196, 197, 67, 104, 105, 106, 108, 109, 114, 128, 131, 132, 134, 135, 136, 139, 156, 163, 180, 85, 113, 188, 189, 190, 191, 192));

    private static final Map<Short, BlockModel> BlockModels = new HashMap<>();
    private static final Map<Integer, BlockModel> specialBlockModels = new HashMap<>();

    private static final Map<Byte, List<Face>> shapes = new HashMap<>();
    private static final Map<List<Face>, Byte> shapesInverted = new HashMap<>();
    private static final Map<Byte, Map<Byte, List<Byte>[]>> facesBlocked = new HashMap<>();

    public final byte shape;
    private final Map<List<Byte>, BlockModel> variants = new HashMap<>();

    /*
    This constructor is used for special blocks
     */
    BlockModel(Context context, String model, int angle, boolean upsideDown) throws JSONException, IOException {
        List<Square> modelSquares = new ArrayList<>();
        textureAtlas = ModelLoader.readJsonModel(context, model, modelSquares);
        List<Face> faces = new ArrayList<>();
        for (Square square : modelSquares) {
            if (angle != 0) {
                square.rotate(angle, 1, 0.5f, 0.5f, 0.5f);
                square.splitCoords();
            }
            if (upsideDown) {
                square.flip();
                square.splitCoords();
            }
            if (square.isFace) faces.add(new Face(square.squareCoords, (byte) 0));
            squares.add(square);
        }
        if (shapesInverted.containsKey(faces)) {
            this.shape = shapesInverted.get(faces);
        } else {
            this.shape = (byte) shapes.size();
            shapes.put(this.shape, faces);
            shapesInverted.put(faces, this.shape);
            //Log.v("BlockModel", "new special shape: " + ++shapeCount);
            assert shapes.size() < 250 : "Too many shapes";
        }
        setBuffers();
    }

    /*
    This constructor is used for normal blocks
     */
    BlockModel(List<Square> squares, TextureAtlas textureAtlas) {
        this.squares.addAll(squares);
        this.textureAtlas = textureAtlas;

        List<Face> faces = new ArrayList<>();
        for (byte i = 0; i < squares.size(); i++) {
            Square square = squares.get(i);
            if (square.isFace) faces.add(new Face(square.squareCoords, i));
        }
        if (shapesInverted.containsKey(faces)) {
            shape = shapesInverted.get(faces);
        } else {
            shape = (byte) shapes.size();
            shapes.put(shape, faces);
            shapesInverted.put(faces, shape);
            //Log.v("BlockModel", "new shape: " + ++shapeCount);
            assert shapes.size() < 250 : "Too many shapes";
        }

        setBuffers();
    }

    /*
    This constructor is used for removing blocked faces
     */
    private BlockModel(List<Square> squares, TextureAtlas textureAtlas, byte shape) {
        this.squares.addAll(squares);
        this.textureAtlas = textureAtlas;
        this.shape = shape;
        setBuffers();
    }

    /*
    This is the main method to get a block model
     */
    public static BlockModel getBlockModel(Context context, short block, int x, int y, int z) {
        if (BlockModels.containsKey(block)) return BlockModels.get(block);
        short id = Chunk.getBlockId(block);
        byte metadata = Chunk.getBlockMetaData(block);
        if (!specialBlocks.contains((int) id)) {
            try {
                BlockModel slotRenderer = ModelLoader.loadBlockModel(context, id, metadata);
                BlockModels.put(block, slotRenderer);
                return slotRenderer;
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            try {
                return getSpecialBlockModel(context, id, metadata, x, y, z);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    private static BlockModel getSpecialBlockModel(Context context, int id, int metadata, int x, int y, int z) {
        int specialNumber = getSpecialNumber(id, metadata, x, y, z);
        int specialId = (id << 16) | (metadata << 8) | specialNumber;
        if (specialBlockModels.containsKey(specialId)) {
            return specialBlockModels.get(specialId);
        }
        try {
            BlockModel specialSlotRenderer = BlockModelUtils.getSpecialBlockModel(context, id, metadata, specialNumber);
            specialBlockModels.put(specialId, specialSlotRenderer);
            return specialSlotRenderer;
        } catch (IOException | JSONException e) {
            throw new RuntimeException(e);
        }
    }


    public BlockModel removeBlockedFaces(byte[] otherShapes) {
        if (shape == -1) return this;
        List<Byte> removedFaces = new ArrayList<>();
        List<Face> shapeOfThis = shapes.get(shape);
        assert shapeOfThis != null : "shapeOfThis is null";

        byte[] directions = new byte[]{(byte) -1, (byte) 1, (byte) -2, (byte) 2, (byte) -3, (byte) 3};

        for (int i = 0; i < 6; i++) {
            if (otherShapes[i] == -1) continue;
            if (!facesBlocked.containsKey(shape)) facesBlocked.put(shape, new HashMap<>(6));
            Map<Byte, List<Byte>[]> shapeMap = facesBlocked.get(shape);
            if (!shapeMap.containsKey(otherShapes[i])) {
                shapeMap.put(otherShapes[i], new List[6]);
            }
            if (shapeMap.get(otherShapes[i])[i] == null) {
                shapeMap.get(otherShapes[i])[i] = findBlockedFaces(shapeOfThis, otherShapes[i], directions[i]);
            }
            removedFaces.addAll(shapeMap.get(otherShapes[i])[i]);
        }

        //sort removedFaces in descending order
        removedFaces.sort((o1, o2) -> o2 - o1);

        if (variants.containsKey(removedFaces)) return variants.get(removedFaces);

        BlockModel newSlotRenderer = createNewBlockModel(removedFaces);
        variants.put(removedFaces, newSlotRenderer);
        return newSlotRenderer;
    }

    private BlockModel createNewBlockModel(List<Byte> removedFaces) {
        List<Square> newSquares = new ArrayList<>(squares.size());
        for (int i = 0; i < squares.size(); i++) {
            if (removedFaces.contains((byte) i)) continue;
            newSquares.add(squares.get(i).copy());
        }
        if (!removedFaces.isEmpty()) assert newSquares.size() != squares.size();
        BlockModel newSlotRenderer = new BlockModel(newSquares, textureAtlas, shape);
        return newSlotRenderer;
    }

    private List<Byte> findBlockedFaces(List<Face> shapeOfThis, byte idOfOtherShape, byte direction) {
        //-3 is -z, -2 is -y, -1 is -x, 1 is x, 2 is y, 3 is z
        List<Byte> blockedFaces = new ArrayList<>(6);
        List<Face> shapeOfOther = shapes.get(idOfOtherShape);
        byte sign = direction > 0 ? (byte) 1 : (byte) -1;
        direction = (byte) (Math.abs(direction) - 1);

        faces:
        for (Face faceOfThis : shapeOfThis) {
            for (Face faceOfOther : shapeOfOther) {
                float[] coords = new float[6];
                for (int i = 0; i < 3; i++) {
                    coords[i] = faceOfOther.coords[i];
                    coords[i + 3] = faceOfOther.coords[i + 3];
                    if (i == direction) {
                        coords[i] = faceOfOther.coords[i] + sign;
                        coords[i + 3] = faceOfOther.coords[i + 3] + sign;
                    }
                }
                if (faceOfThis.isInside(coords)) {
                    blockedFaces.add(faceOfThis.index);
                    continue faces;
                }
            }
        }
        return blockedFaces;
    }

    private static class Face {
        float[] coords = new float[6];//smallest x, smallest y, smallest z, biggest x, biggest y, biggest z
        byte index;

        Face(float[] squareCoords, byte index) {
            this.index = index;

            //I wanted this to be long for no reason don't ask me why :p
            float minX = squareCoords[0], minY = squareCoords[1], minZ = squareCoords[2];
            float maxX = minX, maxY = minY, maxZ = minZ;

            for (int i = 3; i < 12; i += 3) {
                float x = squareCoords[i], y = squareCoords[i + 1], z = squareCoords[i + 2];
                if (x < minX) minX = x;
                if (y < minY) minY = y;
                if (z < minZ) minZ = z;
                if (x > maxX) maxX = x;
                if (y > maxY) maxY = y;
                if (z > maxZ) maxZ = z;
            }

            coords[0] = minX;
            coords[1] = minY;
            coords[2] = minZ;
            coords[3] = maxX;
            coords[4] = maxY;
            coords[5] = maxZ;
        }

        boolean isInside(float[] other) {
            return coords[0] >= other[0] && coords[1] >= other[1] && coords[2] >= other[2] && coords[3] <= other[3] && coords[4] <= other[4] && coords[5] <= other[5];
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(index);
            result = 31 * result + Arrays.hashCode(coords);
            return result;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Face face = (Face) o;
            return index == face.index && Arrays.equals(coords, face.coords);
        }
    }

}
