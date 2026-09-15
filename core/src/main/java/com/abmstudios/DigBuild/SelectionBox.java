package com.abmstudios.DigBuild;

import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;

public class SelectionBox {

    private static final float THICKNESS = 0.02f;

    private final Model edgeModel;
    private final ModelInstance[] edges = new ModelInstance[12];

    public SelectionBox() {

        Material material = new Material(
            ColorAttribute.createDiffuse(0, 0, 0, 1)
        );

        ModelBuilder modelBuilder = new ModelBuilder();

        edgeModel = modelBuilder.createBox(
            1f,
            1f,
            1f,
            material,
            VertexAttributes.Usage.Position |
                VertexAttributes.Usage.Normal
        );

        // Create 12 edges
        for (int i = 0; i < 12; i++) {
            edges[i] = new ModelInstance(edgeModel);
        }
    }

    public void setPosition(int x, int y, int z) {

        // Bottom edges
        setEdge(0, x, y, z, 0.5f, 0f, 0f,
            1f + THICKNESS, THICKNESS, THICKNESS);

        setEdge(1, x, y, z, 1f, 0f, 0.5f,
            THICKNESS, THICKNESS, 1f + THICKNESS);

        setEdge(2, x, y, z, 0.5f, 0f, 1f,
            1f + THICKNESS, THICKNESS, THICKNESS);

        setEdge(3, x, y, z, 0f, 0f, 0.5f,
            THICKNESS, THICKNESS, 1f + THICKNESS);

        // Top edges
        setEdge(4, x, y, z, 0.5f, 1f, 0f,
            1f + THICKNESS, THICKNESS, THICKNESS);

        setEdge(5, x, y, z, 1f, 1f, 0.5f,
            THICKNESS, THICKNESS, 1f + THICKNESS);

        setEdge(6, x, y, z, 0.5f, 1f, 1f,
            1f + THICKNESS, THICKNESS, THICKNESS);

        setEdge(7, x, y, z, 0f, 1f, 0.5f,
            THICKNESS, THICKNESS, 1f + THICKNESS);

        // Vertical edges
        setEdge(8, x, y, z, 0f, 0.5f, 0f,
            THICKNESS, 1f + THICKNESS, THICKNESS);

        setEdge(9, x, y, z, 1f, 0.5f, 0f,
            THICKNESS, 1f + THICKNESS, THICKNESS);

        setEdge(10, x, y, z, 1f, 0.5f, 1f,
            THICKNESS, 1f + THICKNESS, THICKNESS);

        setEdge(11, x, y, z, 0f, 0.5f, 1f,
            THICKNESS, 1f + THICKNESS, THICKNESS);
    }

    private void setEdge(
        int index,
        int blockX,
        int blockY,
        int blockZ,
        float localX,
        float localY,
        float localZ,
        float scaleX,
        float scaleY,
        float scaleZ
    ) {

        edges[index].transform
            .idt()
            .translate(
                blockX + localX,
                blockY + localY,
                blockZ + localZ
            )
            .scale(
                scaleX,
                scaleY,
                scaleZ
            );
    }

    public void render(
        ModelBatch modelBatch,
        Environment environment
    ) {

        for (ModelInstance edge : edges) {
            modelBatch.render(edge, environment);
        }
    }

    public void dispose() {
        edgeModel.dispose();
    }
}
