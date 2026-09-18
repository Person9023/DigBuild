package com.abmstudios.DigBuild;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.Pixmap;
import java.util.ArrayList;
import java.util.List;

public class DroppedItemRenderer {


    private Texture texture;

// ---------------------------------------------------------
// Block models
// ---------------------------------------------------------

    private Model grassModel;
    private Model dirtModel;
    private Model stoneModel;
    private Model woodModel;
    private Model leavesModel;
    private Model planksModel;
    private Model craftingTableModel;

// ---------------------------------------------------------
// Non-block models
// ---------------------------------------------------------

    private Model stickModel;

// ---------------------------------------------------------
// Constructor
// ---------------------------------------------------------

    public DroppedItemRenderer() {

        texture = new Texture(
            Gdx.files.internal("textureatlas.png")
        );

        texture.setFilter(
            Texture.TextureFilter.Nearest,
            Texture.TextureFilter.Nearest
        );

        // -----------------------------------------------------
        // Block models
        // -----------------------------------------------------

        grassModel = createBlockModel(Block.GRASS);
        dirtModel = createBlockModel(Block.DIRT);
        stoneModel = createBlockModel(Block.STONE);
        woodModel = createBlockModel(Block.WOOD);
        leavesModel = createBlockModel(Block.LEAVES);
        planksModel = createBlockModel(Block.PLANKS);
        craftingTableModel = createBlockModel(Block.CRAFTING_TABLE);

        // -----------------------------------------------------
        // Non-block models
        // -----------------------------------------------------

        stickModel = createFlatModel(Item.STICK);
    }

// =========================================================
// BLOCK MODEL
// =========================================================

    private Model createBlockModel(byte block) {

        ModelBuilder builder = new ModelBuilder();

        Material material = new Material(
            TextureAttribute.createDiffuse(texture)
        );

        material.set(
            IntAttribute.createCullFace(GL20.GL_BACK)
        );

        builder.begin();

        builder.part(
            "item",
            ChunkMesh.createItemCube(block),
            GL20.GL_TRIANGLES,
            material
        );

        return builder.end();
    }

// =========================================================
// FLAT ITEM MODEL
// =========================================================

    private Model createFlatModel(Item item) {

        ModelBuilder builder = new ModelBuilder();

        Material textureMaterial = new Material(
            TextureAttribute.createDiffuse(texture),
            new BlendingAttribute(
                GL20.GL_SRC_ALPHA,
                GL20.GL_ONE_MINUS_SRC_ALPHA
            )
        );

        textureMaterial.set(
            IntAttribute.createCullFace(GL20.GL_NONE)
        );

        Material blackMaterial = new Material(
            ColorAttribute.createDiffuse(Color.BLACK)
        );

        blackMaterial.set(
            IntAttribute.createCullFace(GL20.GL_NONE)
        );

        builder.begin();

        builder.part(
            "frontBack",
            createFlatFrontBackMesh(item),
            GL20.GL_TRIANGLES,
            textureMaterial
        );

        builder.part(
            "blackEdges",
            createPixelEdgeMesh(item),
            GL20.GL_TRIANGLES,
            blackMaterial
        );

        return builder.end();
    }

    private Mesh createPixelEdgeMesh(Item item) {

        int textureIndex = item.getTextureIndex();

        int atlasTiles = 4;
        int tileSize = 16;

        int tileX = textureIndex % atlasTiles;
        int tileY = textureIndex / atlasTiles;

        float width = 1.0f;
        float height = 1.0f;
        float thickness = 1.0f / 16.0f;

        float x0 = -width / 2f;
        float y0 = -height / 2f;

        float z0 = -thickness / 2f;
        float z1 = thickness / 2f;

        Pixmap pixmap = new Pixmap(
            Gdx.files.internal("textureatlas.png")
        );

        List<Float> vertexList = new ArrayList<>();
        List<Short> indexList = new ArrayList<>();

        short vertexCount = 0;

        try {

            int atlasWidth = pixmap.getWidth();
            int atlasHeight = pixmap.getHeight();

            int pixelSizeX = atlasWidth / atlasTiles;
            int pixelSizeY = atlasHeight / atlasTiles;

            // -------------------------------------------------
            // Check whether a pixel is transparent.
            // -------------------------------------------------

            boolean[][] visible = new boolean[tileSize][tileSize];

            for (int y = 0; y < tileSize; y++) {
                for (int x = 0; x < tileSize; x++) {

                    int px = tileX * pixelSizeX
                        + x * pixelSizeX / tileSize;

                    int py = atlasHeight
                        - 1
                        - (tileY * pixelSizeY
                        + y * pixelSizeY / tileSize);

                    int pixel = pixmap.getPixel(px, py);

                    int alpha = pixel & 0xFF;

                    visible[x][y] = alpha > 127;
                }
            }

            // -------------------------------------------------
            // Generate black edges around visible pixels.
            // -------------------------------------------------

            for (int y = 0; y < tileSize; y++) {
                for (int x = 0; x < tileSize; x++) {

                    if (!visible[x][y]) {
                        continue;
                    }

                    float px0 = x0 + width * x / tileSize;
                    float px1 = x0 + width * (x + 1) / tileSize;

                    // Flip the generated geometry vertically so the outline
                    // matches the orientation of the displayed texture.
                    int flippedY = tileSize - 1 - y;

                    float py0 = y0 + height * flippedY / tileSize;
                    float py1 = y0 + height * (flippedY + 1) / tileSize;

                    // RIGHT edge
                    if (x == tileSize - 1 || !visible[x + 1][y]) {

                        vertexCount = addBlackQuad(
                            vertexList,
                            indexList,
                            vertexCount,
                            px1, py0, z0,
                            px1, py0, z1,
                            px1, py1, z1,
                            px1, py1, z0,
                            1, 0, 0
                        );
                    }

                    // LEFT edge
                    if (x == 0 || !visible[x - 1][y]) {

                        vertexCount = addBlackQuad(
                            vertexList,
                            indexList,
                            vertexCount,
                            px0, py0, z1,
                            px0, py0, z0,
                            px0, py1, z0,
                            px0, py1, z1,
                            -1, 0, 0
                        );
                    }

                    // TOP edge
                    if (y == tileSize - 1 || !visible[x][y + 1]) {

                        vertexCount = addBlackQuad(
                            vertexList,
                            indexList,
                            vertexCount,
                            px0, py1, z1,
                            px1, py1, z1,
                            px1, py1, z0,
                            px0, py1, z0,
                            0, 1, 0
                        );
                    }

                    // BOTTOM edge
                    if (y == 0 || !visible[x][y - 1]) {

                        vertexCount = addBlackQuad(
                            vertexList,
                            indexList,
                            vertexCount,
                            px0, py0, z0,
                            px1, py0, z0,
                            px1, py0, z1,
                            px0, py0, z1,
                            0, -1, 0
                        );
                    }
                }
            }

        } finally {
            pixmap.dispose();
        }

        // -----------------------------------------------------
        // Convert lists to arrays.
        // -----------------------------------------------------

        float[] vertices = new float[vertexList.size()];

        for (int i = 0; i < vertexList.size(); i++) {
            vertices[i] = vertexList.get(i);
        }

        short[] indices = new short[indexList.size()];

        for (int i = 0; i < indexList.size(); i++) {
            indices[i] = indexList.get(i);
        }

        Mesh mesh = new Mesh(
            true,
            vertices.length / 8,
            indices.length,

            new VertexAttribute(
                VertexAttributes.Usage.Position,
                3,
                "a_position"
            ),

            new VertexAttribute(
                VertexAttributes.Usage.Normal,
                3,
                "a_normal"
            ),

            new VertexAttribute(
                VertexAttributes.Usage.TextureCoordinates,
                2,
                "a_texCoord0"
            )
        );

        mesh.setVertices(vertices);
        mesh.setIndices(indices);

        return mesh;
    }

    private short addBlackQuad(
        List<Float> vertices,
        List<Short> indices,
        short vertexCount,

        float x0, float y0, float z0,
        float x1, float y1, float z1,
        float x2, float y2, float z2,
        float x3, float y3, float z3,

        float nx, float ny, float nz
    ) {

        float[][] points = {
            {x0, y0, z0},
            {x1, y1, z1},
            {x2, y2, z2},
            {x3, y3, z3}
        };

        for (float[] p : points) {

            vertices.add(p[0]);
            vertices.add(p[1]);
            vertices.add(p[2]);

            vertices.add(nx);
            vertices.add(ny);
            vertices.add(nz);

            vertices.add(0f);
            vertices.add(0f);
        }

        indices.add(vertexCount);
        indices.add((short)(vertexCount + 1));
        indices.add((short)(vertexCount + 2));

        indices.add(vertexCount);
        indices.add((short)(vertexCount + 2));
        indices.add((short)(vertexCount + 3));

        return (short)(vertexCount + 4);
    }

// =========================================================
// ADD VERTEX
// =========================================================

    private int addFlatVertex(
        float[] vertices,
        int vertex,
        float x,
        float y,
        float z,
        float nx,
        float ny,
        float nz,
        float u,
        float v
    ) {

        vertices[vertex++] = x;
        vertices[vertex++] = y;
        vertices[vertex++] = z;

        vertices[vertex++] = nx;
        vertices[vertex++] = ny;
        vertices[vertex++] = nz;

        vertices[vertex++] = u;
        vertices[vertex++] = v;

        return vertex;
    }

// =========================================================
// ADD BLACK VERTEX
// =========================================================

    private int addBlackVertex(
        float[] vertices,
        int vertex,
        float x,
        float y,
        float z,
        float nx,
        float ny,
        float nz
    ) {

        vertices[vertex++] = x;
        vertices[vertex++] = y;
        vertices[vertex++] = z;

        vertices[vertex++] = nx;
        vertices[vertex++] = ny;
        vertices[vertex++] = nz;

        // No texture coordinates are needed for the black edge.
        vertices[vertex++] = 0f;
        vertices[vertex++] = 0f;

        return vertex;
    }

// =========================================================
// TEXTURE UV
// =========================================================

    private float[] getTextureUV(int textureIndex) {

        int atlasSize = 4;

        int tileX =
            textureIndex % atlasSize;

        int tileY =
            textureIndex / atlasSize;

        float tileSize =
            1.0f / atlasSize;

        float u0 =
            tileX * tileSize;

        float u1 =
            u0 + tileSize;

        // Convert from top-left atlas coordinates
        // to OpenGL bottom-left UV coordinates

        float v1 =
            1.0f - tileY * tileSize;

        float v0 =
            v1 - tileSize;

        return new float[]{
            u0, v0,
            u1, v0,
            u1, v1,
            u0, v1
        };
    }

// =========================================================
// FRONT + BACK MESH
// =========================================================

    private Mesh createFlatFrontBackMesh(Item item) {

        float width = 1.0f;
        float height = 1.0f;

        // 1 pixel of thickness in a 16x16 texture
        float thickness = 1.0f / 16.0f;

        float z0 = -thickness / 2.0f;
        float z1 = thickness / 2.0f;

        float x0 = -width / 2.0f;
        float x1 = width / 2.0f;

        float y0 = -height / 2.0f;
        float y1 = height / 2.0f;

        // -----------------------------------------------------
        // Texture coordinates
        // -----------------------------------------------------

        float[] uv = getTextureUV(
            item.getTextureIndex()
        );

        float u0 = uv[0];
        float v0 = uv[1];

        float u1 = uv[2];
        float v1 = uv[5];

        // -----------------------------------------------------
        // 2 faces × 4 vertices × 8 floats
        // -----------------------------------------------------

        float[] vertices = new float[2 * 4 * 8];

        int vertex = 0;

        // -----------------------------------------------------
        // FRONT
        // -----------------------------------------------------

        vertex = addFlatVertex(
            vertices,
            vertex,
            x0, y0, z1,
            0, 0, 1,
            u0, v0
        );

        vertex = addFlatVertex(
            vertices,
            vertex,
            x1, y0, z1,
            0, 0, 1,
            u1, v0
        );

        vertex = addFlatVertex(
            vertices,
            vertex,
            x1, y1, z1,
            0, 0, 1,
            u1, v1
        );

        vertex = addFlatVertex(
            vertices,
            vertex,
            x0, y1, z1,
            0, 0, 1,
            u0, v1
        );

        // -----------------------------------------------------
        // BACK
        // -----------------------------------------------------

        vertex = addFlatVertex(
            vertices,
            vertex,
            x1, y0, z0,
            0, 0, -1,
            u1, v0
        );

        vertex = addFlatVertex(
            vertices,
            vertex,
            x0, y0, z0,
            0, 0, -1,
            u0, v0
        );

        vertex = addFlatVertex(
            vertices,
            vertex,
            x0, y1, z0,
            0, 0, -1,
            u0, v1
        );

        vertex = addFlatVertex(
            vertices,
            vertex,
            x1, y1, z0,
            0, 0, -1,
            u1, v1
        );

        // -----------------------------------------------------
        // Indices
        // -----------------------------------------------------

        short[] indices = new short[]{

            // Front
            0, 1, 2,
            0, 2, 3,

            // Back
            4, 5, 6,
            4, 6, 7
        };

        // -----------------------------------------------------
        // Create mesh
        // -----------------------------------------------------

        Mesh mesh = new Mesh(
            true,
            vertices.length / 8,
            indices.length,

            new VertexAttribute(
                VertexAttributes.Usage.Position,
                3,
                "a_position"
            ),

            new VertexAttribute(
                VertexAttributes.Usage.Normal,
                3,
                "a_normal"
            ),

            new VertexAttribute(
                VertexAttributes.Usage.TextureCoordinates,
                2,
                "a_texCoord0"
            )
        );

        mesh.setVertices(vertices);
        mesh.setIndices(indices);

        return mesh;
    }

// =========================================================
// BLACK EDGE MESH
// =========================================================

    private Mesh createFlatEdgeMesh() {

        float width = 1.0f;
        float height = 1.0f;

        // -----------------------------------------------------
        // One pixel of thickness
        // -----------------------------------------------------

        float thickness = 1.0f / 16.0f;

        float z0 = -thickness / 2.0f;
        float z1 = thickness / 2.0f;

        float x0 = -width / 2.0f;
        float x1 = width / 2.0f;

        float y0 = -height / 2.0f;
        float y1 = height / 2.0f;

        // -----------------------------------------------------
        // 4 edge faces × 4 vertices × 8 floats
        // -----------------------------------------------------

        float[] vertices = new float[4 * 4 * 8];

        int vertex = 0;

        // =====================================================
        // RIGHT EDGE
        // =====================================================

        vertex = addBlackVertex(
            vertices,
            vertex,
            x1, y0, z1,
            1, 0, 0
        );

        vertex = addBlackVertex(
            vertices,
            vertex,
            x1, y0, z0,
            1, 0, 0
        );

        vertex = addBlackVertex(
            vertices,
            vertex,
            x1, y1, z0,
            1, 0, 0
        );

        vertex = addBlackVertex(
            vertices,
            vertex,
            x1, y1, z1,
            1, 0, 0
        );

        // =====================================================
        // LEFT EDGE
        // =====================================================

        vertex = addBlackVertex(
            vertices,
            vertex,
            x0, y0, z0,
            -1, 0, 0
        );

        vertex = addBlackVertex(
            vertices,
            vertex,
            x0, y0, z1,
            -1, 0, 0
        );

        vertex = addBlackVertex(
            vertices,
            vertex,
            x0, y1, z1,
            -1, 0, 0
        );

        vertex = addBlackVertex(
            vertices,
            vertex,
            x0, y1, z0,
            -1, 0, 0
        );

        // =====================================================
        // TOP EDGE
        // =====================================================

        vertex = addBlackVertex(
            vertices,
            vertex,
            x0, y1, z1,
            0, 1, 0
        );

        vertex = addBlackVertex(
            vertices,
            vertex,
            x1, y1, z1,
            0, 1, 0
        );

        vertex = addBlackVertex(
            vertices,
            vertex,
            x1, y1, z0,
            0, 1, 0
        );

        vertex = addBlackVertex(
            vertices,
            vertex,
            x0, y1, z0,
            0, 1, 0
        );

        // =====================================================
        // BOTTOM EDGE
        // =====================================================

        vertex = addBlackVertex(
            vertices,
            vertex,
            x0, y0, z0,
            0, -1, 0
        );

        vertex = addBlackVertex(
            vertices,
            vertex,
            x1, y0, z0,
            0, -1, 0
        );

        vertex = addBlackVertex(
            vertices,
            vertex,
            x1, y0, z1,
            0, -1, 0
        );

        vertex = addBlackVertex(
            vertices,
            vertex,
            x0, y0, z1,
            0, -1, 0
        );

        // -----------------------------------------------------
        // Indices
        // -----------------------------------------------------

        short[] indices = new short[24];

        int index = 0;

        for (short face = 0; face < 4; face++) {

            short start = (short) (face * 4);

            indices[index++] = start;
            indices[index++] = (short) (start + 1);
            indices[index++] = (short) (start + 2);

            indices[index++] = start;
            indices[index++] = (short) (start + 2);
            indices[index++] = (short) (start + 3);
        }

        // -----------------------------------------------------
        // Create mesh
        // -----------------------------------------------------

        Mesh mesh = new Mesh(
            true,
            vertices.length / 8,
            indices.length,

            new VertexAttribute(
                VertexAttributes.Usage.Position,
                3,
                "a_position"
            ),

            new VertexAttribute(
                VertexAttributes.Usage.Normal,
                3,
                "a_normal"
            ),

            new VertexAttribute(
                VertexAttributes.Usage.TextureCoordinates,
                2,
                "a_texCoord0"
            )
        );

        mesh.setVertices(vertices);
        mesh.setIndices(indices);

        return mesh;
    }

// =========================================================
// RENDER
// =========================================================

    public void render(
        ModelBatch modelBatch,
        com.badlogic.gdx.graphics.g3d.Environment environment,
        DroppedItem item
    ) {

        Model model = getModel(item);

        if (model == null) {
            return;
        }

        ModelInstance instance =
            new ModelInstance(model);

        // -----------------------------------------------------
        // Position
        // -----------------------------------------------------

        float renderYOffset = 0f;
        float floatOffset = item.getFloatOffset();

        if (item.getRenderType() != DroppedItem.RenderType.BLOCK) {

            renderYOffset = 0.15f;

            // Prevent the floating animation from moving
            // the flat item below its resting position.
            floatOffset = Math.max(0f, floatOffset);
        }

        instance.transform.setToTranslation(
            item.getX(),
            item.getY() + floatOffset + renderYOffset,
            item.getZ()
        );

        // -----------------------------------------------------
        // Rotation
        // -----------------------------------------------------

        instance.transform.rotate(
            0,
            1,
            0,
            item.getRotation()
        );

        // -----------------------------------------------------
        // Scale
        // -----------------------------------------------------

        if (item.getRenderType() == DroppedItem.RenderType.BLOCK) {

            instance.transform.scale(
                0.3f,
                0.3f,
                0.3f
            );

        } else {

            /*
             * Non-block items are also currently scaled to
             * 30% of a block.
             *
             * We can give sticks, swords and tools individual
             * sizes later.
             */

            instance.transform.scale(
                0.3f,
                0.3f,
                0.3f
            );
        }

        modelBatch.render(
            instance,
            environment
        );
    }

// =========================================================
// GET MODEL
// =========================================================

    private Model getModel(DroppedItem item) {

        Item droppedItem =
            item.getStack().getItem();

        // -----------------------------------------------------
        // Blocks
        // -----------------------------------------------------

        if (droppedItem == Item.GRASS) {
            return grassModel;
        }

        if (droppedItem == Item.DIRT) {
            return dirtModel;
        }

        if (droppedItem == Item.STONE) {
            return stoneModel;
        }

        if (droppedItem == Item.WOOD) {
            return woodModel;
        }

        if (droppedItem == Item.LEAVES) {
            return leavesModel;
        }

        if (droppedItem == Item.PLANKS) {
            return planksModel;
        }

        if (droppedItem == Item.CRAFTING_TABLE) {
            return craftingTableModel;
        }

        // -----------------------------------------------------
        // Non-block items
        // -----------------------------------------------------

        if (droppedItem == Item.STICK) {
            return stickModel;
        }

        return null;
    }
// =========================================================
// DISPOSE
// =========================================================

    public void dispose() {

        if (grassModel != null) {
            grassModel.dispose();
        }

        if (dirtModel != null) {
            dirtModel.dispose();
        }

        if (stoneModel != null) {
            stoneModel.dispose();
        }

        if (woodModel != null) {
            woodModel.dispose();
        }

        if (leavesModel != null) {
            leavesModel.dispose();
        }

        if (planksModel != null) {
            planksModel.dispose();
        }

        if (craftingTableModel != null) {
            craftingTableModel.dispose();
        }

        if (stickModel != null) {
            stickModel.dispose();
        }

        if (texture != null) {
            texture.dispose();
        }
    }

}
