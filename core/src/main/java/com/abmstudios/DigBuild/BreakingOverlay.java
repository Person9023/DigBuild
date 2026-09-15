package com.abmstudios.DigBuild;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.utils.Array;

public class BreakingOverlay {

    private Model model;
    public ModelInstance instance;

    private Texture texture;

    private int stage = 0;

    public BreakingOverlay() {

        texture = new Texture(
            Gdx.files.internal("block_break.png")
        );

        texture.setFilter(
            Texture.TextureFilter.Nearest,
            Texture.TextureFilter.Nearest
        );

        generate();
    }

    public void setStage(int stage) {

        this.stage = Math.max(0, Math.min(6, stage));

        generate();
    }

    private void generate() {

        Array<Float> vertices = new Array<>();
        Array<Short> indices = new Array<>();

        float offset = 0.002f;

        // Texture atlas is 7 tiles across
        float tileWidth = 1f / 7f;

        float u0 = stage * tileWidth;
        float u1 = u0 + tileWidth;

        float v0 = 0f;
        float v1 = 1f;

        // Front (+Z)
        addFace(
            vertices, indices,
            0, 0, 1,
            offset,
            u0, v0, u1, v1
        );

        // Back (-Z)
        addFace(
            vertices, indices,
            0, 0, -1,
            offset,
            u0, v0, u1, v1
        );

        // Right (+X)
        addFace(
            vertices, indices,
            1, 0, 0,
            offset,
            u0, v0, u1, v1
        );

        // Left (-X)
        addFace(
            vertices, indices,
            -1, 0, 0,
            offset,
            u0, v0, u1, v1
        );

        // Top (+Y)
        addFace(
            vertices, indices,
            0, 1, 0,
            offset,
            u0, v0, u1, v1
        );

        // Bottom (-Y)
        addFace(
            vertices, indices,
            0, -1, 0,
            offset,
            u0, v0, u1, v1
        );

        float[] vertexArray = new float[vertices.size];

        for (int i = 0; i < vertices.size; i++) {
            vertexArray[i] = vertices.get(i);
        }

        short[] indexArray = new short[indices.size];

        for (int i = 0; i < indices.size; i++) {
            indexArray[i] = indices.get(i);
        }

        if (model != null) {
            model.dispose();
        }

        Mesh mesh = new Mesh(
            true,
            vertexArray.length / 8,
            indexArray.length,

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

        mesh.setVertices(vertexArray);
        mesh.setIndices(indexArray);

        ModelBuilder builder = new ModelBuilder();

        builder.begin();

        builder.part(
            "breaking",
            mesh,
            GL20.GL_TRIANGLES,
            new Material(
                TextureAttribute.createDiffuse(texture),
                new BlendingAttribute(
                    GL20.GL_SRC_ALPHA,
                    GL20.GL_ONE_MINUS_SRC_ALPHA
                )
            )
        );

        model = builder.end();

        instance = new ModelInstance(model);
    }

    private void addFace(
        Array<Float> vertices,
        Array<Short> indices,
        int normalX,
        int normalY,
        int normalZ,
        float offset,
        float u0,
        float v0,
        float u1,
        float v1
    ) {

        short start = (short)(vertices.size / 8);

        if (normalZ == 1) {

            // Front
            addVertex(vertices,
                0, 0, 1 + offset,
                0, 0, 1,
                u0, v0
            );

            addVertex(vertices,
                1, 0, 1 + offset,
                0, 0, 1,
                u1, v0
            );

            addVertex(vertices,
                1, 1, 1 + offset,
                0, 0, 1,
                u1, v1
            );

            addVertex(vertices,
                0, 1, 1 + offset,
                0, 0, 1,
                u0, v1
            );

        } else if (normalZ == -1) {

            // Back
            addVertex(vertices,
                1, 0, -offset,
                0, 0, -1,
                u0, v0
            );

            addVertex(vertices,
                0, 0, -offset,
                0, 0, -1,
                u1, v0
            );

            addVertex(vertices,
                0, 1, -offset,
                0, 0, -1,
                u1, v1
            );

            addVertex(vertices,
                1, 1, -offset,
                0, 0, -1,
                u0, v1
            );

        } else if (normalX == 1) {

            // Right
            addVertex(vertices,
                1 + offset, 0, 1,
                1, 0, 0,
                u0, v0
            );

            addVertex(vertices,
                1 + offset, 0, 0,
                1, 0, 0,
                u1, v0
            );

            addVertex(vertices,
                1 + offset, 1, 0,
                1, 0, 0,
                u1, v1
            );

            addVertex(vertices,
                1 + offset, 1, 1,
                1, 0, 0,
                u0, v1
            );

        } else if (normalX == -1) {

            // Left
            addVertex(vertices,
                -offset, 0, 0,
                -1, 0, 0,
                u0, v0
            );

            addVertex(vertices,
                -offset, 0, 1,
                -1, 0, 0,
                u1, v0
            );

            addVertex(vertices,
                -offset, 1, 1,
                -1, 0, 0,
                u1, v1
            );

            addVertex(vertices,
                -offset, 1, 0,
                -1, 0, 0,
                u0, v1
            );

        } else if (normalY == 1) {

            // Top
            addVertex(vertices,
                0, 1 + offset, 1,
                0, 1, 0,
                u0, v0
            );

            addVertex(vertices,
                1, 1 + offset, 1,
                0, 1, 0,
                u1, v0
            );

            addVertex(vertices,
                1, 1 + offset, 0,
                0, 1, 0,
                u1, v1
            );

            addVertex(vertices,
                0, 1 + offset, 0,
                0, 1, 0,
                u0, v1
            );

        } else {

            // Bottom
            addVertex(vertices,
                0, -offset, 0,
                0, -1, 0,
                u0, v0
            );

            addVertex(vertices,
                1, -offset, 0,
                0, -1, 0,
                u1, v0
            );

            addVertex(vertices,
                1, -offset, 1,
                0, -1, 0,
                u1, v1
            );

            addVertex(vertices,
                0, -offset, 1,
                0, -1, 0,
                u0, v1
            );
        }

        // Two triangles
        indices.add(start);
        indices.add((short)(start + 1));
        indices.add((short)(start + 2));

        indices.add(start);
        indices.add((short)(start + 2));
        indices.add((short)(start + 3));
    }

    private void addVertex(
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

    public void setPosition(int x, int y, int z) {

        instance.transform.setToTranslation(
            x,
            y,
            z
        );
    }

    public void render(
        ModelBatch modelBatch,
        Environment environment
    ) {

        modelBatch.render(
            instance,
            environment
        );
    }

    public void dispose() {

        if (model != null) {
            model.dispose();
        }

        if (texture != null) {
            texture.dispose();
        }
    }
}
