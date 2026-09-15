
package com.abmstudios.DigBuild;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.attributes.DepthTestAttribute;



public class ChunkMesh {

    public Mesh mesh;
    public Mesh transparentMesh;

    public Model model;
    public ModelInstance instance;

    public Texture texture;

    private static boolean shouldRenderFace(byte block, byte neighbour) {
        return neighbour == Block.AIR ||
            neighbour == Block.LEAVES;
    }



    // CPU-only mesh data. This can safely be generated on a background thread.
    public static class MeshData {
        public final float[] vertices;
        public final short[] indices;
        public final float[] transparentVertices;
        public final short[] transparentIndices;
        public final int chunkX;
        public final int chunkZ;

        public MeshData(
            float[] vertices,
            short[] indices,
            float[] transparentVertices,
            short[] transparentIndices,
            int chunkX,
            int chunkZ
        ) {
            this.vertices = vertices;
            this.indices = indices;
            this.transparentVertices = transparentVertices;
            this.transparentIndices = transparentIndices;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
        }
    }

    /**
     * Builds only the CPU-side vertex/index data.
     * No OpenGL or Gdx calls are made here, so this can run off the render thread.
     */
    public static MeshData generateData(Chunk chunk) {
        Array<Float> vertices = new Array<>();
        Array<Short> indices = new Array<>();

        Array<Float> transparentVertices = new Array<>();
        Array<Short> transparentIndices = new Array<>();

        for (int x = 0; x < Chunk.WIDTH; x++) {
            for (int y = 0; y < Chunk.HEIGHT; y++) {
                for (int z = 0; z < Chunk.DEPTH; z++) {

                    byte block = chunk.getBlock(x, y, z);

                    if (block == Block.AIR) {
                        continue;
                    }

                    Array<Float> currentVertices;
                    Array<Short> currentIndices;

                    if (block == Block.LEAVES) {
                        currentVertices = transparentVertices;
                        currentIndices = transparentIndices;
                    } else {
                        currentVertices = vertices;
                        currentIndices = indices;
                    }

                    if (shouldRenderFace(block, chunk.getBlock(x, y + 1, z))) {
                        addTopFace(currentVertices, currentIndices, x, y, z, Block.getTopTexture(block));
                    }

                    if (shouldRenderFace(block, chunk.getBlock(x, y - 1, z))) {
                        addBottomFace(currentVertices, currentIndices, x, y, z, Block.getBottomTexture(block));
                    }

                    if (shouldRenderFace(block, chunk.getBlock(x, y, z + 1))) {
                        addFrontFace(currentVertices, currentIndices, x, y, z, Block.getFrontTexture(block));
                    }

                    if (shouldRenderFace(block, chunk.getBlock(x, y, z - 1))) {
                        addBackFace(currentVertices, currentIndices, x, y, z, Block.getBackTexture(block));
                    }

                    if (shouldRenderFace(block, chunk.getBlock(x + 1, y, z))) {
                        addRightFace(currentVertices, currentIndices, x, y, z, Block.getRightTexture(block));
                    }

                    if (shouldRenderFace(block, chunk.getBlock(x - 1, y, z))) {
                        addLeftFace(currentVertices, currentIndices, x, y, z, Block.getLeftTexture(block));
                    }
                }
            }
        }

        float[] vertexArray = new float[vertices.size];
        for (int i = 0; i < vertices.size; i++) {
            vertexArray[i] = vertices.get(i);
        }

        short[] indexArray = new short[indices.size];
        for (int i = 0; i < indices.size; i++) {
            indexArray[i] = indices.get(i);
        }

        float[] transparentVertexArray = new float[transparentVertices.size];
        for (int i = 0; i < transparentVertices.size; i++) {
            transparentVertexArray[i] = transparentVertices.get(i);
        }

        short[] transparentIndexArray = new short[transparentIndices.size];
        for (int i = 0; i < transparentIndices.size; i++) {
            transparentIndexArray[i] = transparentIndices.get(i);
        }

        return new MeshData(
            vertexArray,
            indexArray,
            transparentVertexArray,
            transparentIndexArray,
            chunk.getChunkX(),
            chunk.getChunkZ()
        );
    }

    /**
     * Creates the actual LibGDX/OpenGL objects from CPU-side mesh data.
     * This MUST be called on the render thread.
     */
    public void applyData(MeshData data) {
        texture = new Texture(
            Gdx.files.internal("textureatlas.png")
        );

        texture.setFilter(
            Texture.TextureFilter.Nearest,
            Texture.TextureFilter.Nearest
        );

        if (mesh != null) {
            mesh.dispose();
        }

        mesh = new Mesh(
            true,
            data.vertices.length / 8,
            data.indices.length,
            new VertexAttribute(Usage.Position, 3, "a_position"),
            new VertexAttribute(Usage.Normal, 3, "a_normal"),
            new VertexAttribute(Usage.TextureCoordinates, 2, "a_texCoord0")
        );

        mesh.setVertices(data.vertices);
        mesh.setIndices(data.indices);

        if (transparentMesh != null) {
            transparentMesh.dispose();
        }

        transparentMesh = new Mesh(
            true,
            data.transparentVertices.length / 8,
            data.transparentIndices.length,
            new VertexAttribute(Usage.Position, 3, "a_position"),
            new VertexAttribute(Usage.Normal, 3, "a_normal"),
            new VertexAttribute(Usage.TextureCoordinates, 2, "a_texCoord0")
        );

        transparentMesh.setVertices(data.transparentVertices);
        transparentMesh.setIndices(data.transparentIndices);

        Material opaqueMaterial = new Material(
            TextureAttribute.createDiffuse(texture)
        );

        opaqueMaterial.set(
            IntAttribute.createCullFace(GL20.GL_BACK)
        );

        Material transparentMaterial = new Material(
            TextureAttribute.createDiffuse(texture),
            new BlendingAttribute(
                GL20.GL_SRC_ALPHA,
                GL20.GL_ONE_MINUS_SRC_ALPHA
            ),
            new DepthTestAttribute(
                GL20.GL_LEQUAL,
                false
            )
        );

        transparentMaterial.set(
            IntAttribute.createCullFace(GL20.GL_NONE)
        );

        ModelBuilder modelBuilder = new ModelBuilder();
        modelBuilder.begin();

        if (data.indices.length > 0) {
            modelBuilder.part(
                "opaque",
                mesh,
                GL20.GL_TRIANGLES,
                opaqueMaterial
            );
        }

        if (data.transparentIndices.length > 0) {
            modelBuilder.part(
                "transparent",
                transparentMesh,
                GL20.GL_TRIANGLES,
                transparentMaterial
            );
        }

        model = modelBuilder.end();
        instance = new ModelInstance(model);

        instance.transform.setToTranslation(
            data.chunkX * Chunk.WIDTH,
            0,
            data.chunkZ * Chunk.DEPTH
        );
    }

    // Kept for compatibility with code that still needs synchronous generation.
    public void generate(Chunk chunk) {
        applyData(generateData(chunk));
    }

    // =============================================================
    // TOP FACE
    // =============================================================

    private static void addTopFace(
        Array<Float> vertices,
        Array<Short> indices,
        int x,
        int y,
        int z,
        int textureIndex
    ) {

        float[] uv = getTextureUV(textureIndex);

        short start =
            (short)(vertices.size / 8);


        addVertex(
            vertices,
            x,
            y + 1,
            z,
            0,
            1,
            0,
            uv[0],
            uv[1]
        );


        addVertex(
            vertices,
            x + 1,
            y + 1,
            z,
            0,
            1,
            0,
            uv[2],
            uv[3]
        );


        addVertex(
            vertices,
            x + 1,
            y + 1,
            z + 1,
            0,
            1,
            0,
            uv[4],
            uv[5]
        );


        addVertex(
            vertices,
            x,
            y + 1,
            z + 1,
            0,
            1,
            0,
            uv[6],
            uv[7]
        );


        addQuadIndices(
            indices,
            start,
            true
        );
    }


    // =============================================================
    // BOTTOM FACE
    // =============================================================

    private static void addBottomFace(
        Array<Float> vertices,
        Array<Short> indices,
        int x,
        int y,
        int z,
        int textureIndex
    ) {

        float[] uv = getTextureUV(textureIndex);

        short start =
            (short)(vertices.size / 8);


        addVertex(
            vertices,
            x,
            y,
            z,
            0,
            -1,
            0,
            uv[0],
            uv[1]
        );


        addVertex(
            vertices,
            x,
            y,
            z + 1,
            0,
            -1,
            0,
            uv[2],
            uv[3]
        );


        addVertex(
            vertices,
            x + 1,
            y,
            z + 1,
            0,
            -1,
            0,
            uv[4],
            uv[5]
        );


        addVertex(
            vertices,
            x + 1,
            y,
            z,
            0,
            -1,
            0,
            uv[6],
            uv[7]
        );


        addQuadIndices(
            indices,
            start,
            true
        );
    }


    // =============================================================
    // FRONT FACE
    // =============================================================

    private static void addFrontFace(
        Array<Float> vertices,
        Array<Short> indices,
        int x,
        int y,
        int z,
        int textureIndex
    ) {

        float[] uv = getTextureUV(textureIndex);

        short start =
            (short)(vertices.size / 8);


        addVertex(
            vertices,
            x,
            y,
            z + 1,
            0,
            0,
            1,
            uv[0],
            uv[1]
        );


        addVertex(
            vertices,
            x + 1,
            y,
            z + 1,
            0,
            0,
            1,
            uv[2],
            uv[3]
        );


        addVertex(
            vertices,
            x + 1,
            y + 1,
            z + 1,
            0,
            0,
            1,
            uv[4],
            uv[5]
        );


        addVertex(
            vertices,
            x,
            y + 1,
            z + 1,
            0,
            0,
            1,
            uv[6],
            uv[7]
        );


        addQuadIndices(
            indices,
            start,
            false
        );
    }


    // =============================================================
    // BACK FACE
    // =============================================================

    private static void addBackFace(
        Array<Float> vertices,
        Array<Short> indices,
        int x,
        int y,
        int z,
        int textureIndex
    ) {

        float[] uv = getTextureUV(textureIndex);

        short start =
            (short)(vertices.size / 8);


        addVertex(
            vertices,
            x + 1,
            y,
            z,
            0,
            0,
            -1,
            uv[0],
            uv[1]
        );


        addVertex(
            vertices,
            x,
            y,
            z,
            0,
            0,
            -1,
            uv[2],
            uv[3]
        );


        addVertex(
            vertices,
            x,
            y + 1,
            z,
            0,
            0,
            -1,
            uv[4],
            uv[5]
        );


        addVertex(
            vertices,
            x + 1,
            y + 1,
            z,
            0,
            0,
            -1,
            uv[6],
            uv[7]
        );


        addQuadIndices(
            indices,
            start,
            false
        );
    }


    // =============================================================
    // RIGHT FACE
    // =============================================================

    private static void addRightFace(
        Array<Float> vertices,
        Array<Short> indices,
        int x,
        int y,
        int z,
        int textureIndex
    ) {

        float[] uv = getTextureUV(textureIndex);

        short start =
            (short)(vertices.size / 8);


        addVertex(
            vertices,
            x + 1,
            y,
            z,
            1,
            0,
            0,
            uv[0],
            uv[1]
        );


        addVertex(
            vertices,
            x + 1,
            y,
            z + 1,
            1,
            0,
            0,
            uv[2],
            uv[3]
        );


        addVertex(
            vertices,
            x + 1,
            y + 1,
            z + 1,
            1,
            0,
            0,
            uv[4],
            uv[5]
        );


        addVertex(
            vertices,
            x + 1,
            y + 1,
            z,
            1,
            0,
            0,
            uv[6],
            uv[7]
        );


        addQuadIndices(
            indices,
            start,
            true
        );
    }


    // =============================================================
    // LEFT FACE
    // =============================================================

    private static void addLeftFace(
        Array<Float> vertices,
        Array<Short> indices,
        int x,
        int y,
        int z,
        int textureIndex
    ) {

        float[] uv = getTextureUV(textureIndex);

        short start =
            (short)(vertices.size / 8);


        addVertex(
            vertices,
            x,
            y,
            z + 1,
            -1,
            0,
            0,
            uv[0],
            uv[1]
        );


        addVertex(
            vertices,
            x,
            y,
            z,
            -1,
            0,
            0,
            uv[2],
            uv[3]
        );


        addVertex(
            vertices,
            x,
            y + 1,
            z,
            -1,
            0,
            0,
            uv[4],
            uv[5]
        );


        addVertex(
            vertices,
            x,
            y + 1,
            z + 1,
            -1,
            0,
            0,
            uv[6],
            uv[7]
        );


        addQuadIndices(
            indices,
            start,
            true
        );
    }


    // =============================================================
    // VERTEX
    // =============================================================

    private static void addVertex(
        Array<Float> vertices,
        float x,
        float y,
        float z,
        float nx,
        float ny,
        float nz,
        float u,
        float v
    ) {

        vertices.add(x);
        vertices.add(y);
        vertices.add(z);

        vertices.add(nx);
        vertices.add(ny);
        vertices.add(nz);

        vertices.add(u);
        vertices.add(v);
    }


    // =============================================================
    // TEXTURE UV
    // =============================================================

    private static float[] getTextureUV(int textureIndex) {

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


        return new float[] {

            u0, v0,
            u1, v0,
            u1, v1,
            u0, v1
        };
    }


    // =============================================================
    // QUAD INDICES
    // =============================================================

    private static void addQuadIndices(
        Array<Short> indices,
        short start,
        boolean reverse
    ) {

        if (!reverse) {

            indices.add(start);
            indices.add((short)(start + 1));
            indices.add((short)(start + 2));

            indices.add(start);
            indices.add((short)(start + 2));
            indices.add((short)(start + 3));

        } else {

            indices.add(start);
            indices.add((short)(start + 2));
            indices.add((short)(start + 1));

            indices.add(start);
            indices.add((short)(start + 3));
            indices.add((short)(start + 2));
        }
    }

    public static Mesh createItemCube(byte block) {

        Array<Float> vertices = new Array<>();
        Array<Short> indices = new Array<>();

        // Top
        addTopFace(
            vertices,
            indices,
            0,
            0,
            0,
            Block.getTopTexture(block)
        );

        // Bottom
        addBottomFace(
            vertices,
            indices,
            0,
            0,
            0,
            Block.getBottomTexture(block)
        );

        // Front
        addFrontFace(
            vertices,
            indices,
            0,
            0,
            0,
            Block.getFrontTexture(block)
        );

        // Back
        addBackFace(
            vertices,
            indices,
            0,
            0,
            0,
            Block.getBackTexture(block)
        );

        // Right
        addRightFace(
            vertices,
            indices,
            0,
            0,
            0,
            Block.getRightTexture(block)
        );

        // Left
        addLeftFace(
            vertices,
            indices,
            0,
            0,
            0,
            Block.getLeftTexture(block)
        );

        float[] vertexArray = new float[vertices.size];

        for (int i = 0; i < vertices.size; i++) {
            vertexArray[i] = vertices.get(i);
        }

        short[] indexArray = new short[indices.size];

        for (int i = 0; i < indices.size; i++) {
            indexArray[i] = indices.get(i);
        }

        Mesh mesh = new Mesh(
            true,
            vertexArray.length / 8,
            indexArray.length,

            new VertexAttribute(
                Usage.Position,
                3,
                "a_position"
            ),

            new VertexAttribute(
                Usage.Normal,
                3,
                "a_normal"
            ),

            new VertexAttribute(
                Usage.TextureCoordinates,
                2,
                "a_texCoord0"
            )
        );

        mesh.setVertices(vertexArray);
        mesh.setIndices(indexArray);

        return mesh;
    }

    // =============================================================
    // DISPOSE
    // =============================================================

    public void dispose() {

        if (model != null) {
            model.dispose();
            model = null;
        }

        if (mesh != null) {
            mesh = null;
        }

        if (transparentMesh != null) {
            transparentMesh = null;
        }

        if (texture != null) {
            texture.dispose();
            texture = null;
        }
    }
}

