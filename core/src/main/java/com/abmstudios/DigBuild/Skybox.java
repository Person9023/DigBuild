package com.abmstudios.DigBuild;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;

public class Skybox {

    private final Mesh mesh;
    private final ShaderProgram shader;

    private final Matrix4 transform = new Matrix4();

    public Skybox() {

        ShaderProgram.pedantic = false;

        // =========================================================
        // VERTEX SHADER
        // =========================================================

        String vertexShader =
            "attribute vec3 a_position;\n" +

                "uniform mat4 u_projTrans;\n" +

                "varying float v_height;\n" +

                "void main() {\n" +

                "    v_height = a_position.y;\n" +

                "    gl_Position = u_projTrans * vec4(a_position, 1.0);\n" +

                "}";

        // =========================================================
        // FRAGMENT SHADER
        // =========================================================

        String fragmentShader =
            "#ifdef GL_ES\n" +
                "precision mediump float;\n" +
                "#endif\n" +

                "varying float v_height;\n" +

                "void main() {\n" +

                "    // Bottom of the sky\n" +
                "    vec3 bottomColor = vec3(\n" +
                "        0.08,\n" +
                "        0.22,\n" +
                "        0.45\n" +
                "    );\n" +

                "    // Top of the sky\n" +
                "    vec3 topColor = vec3(\n" +
                "        0.55,\n" +
                "        0.80,\n" +
                "        1.00\n" +
                "    );\n" +

                "    // Convert cube height into 0-1 range\n" +
                "    float t = clamp(\n" +
                "        (v_height + 50.0) / 100.0,\n" +
                "        0.0,\n" +
                "        1.0\n" +
                "    );\n" +

                "    vec3 color = mix(\n" +
                "        bottomColor,\n" +
                "        topColor,\n" +
                "        t\n" +
                "    );\n" +

                "    gl_FragColor = vec4(color, 1.0);\n" +

                "}\n";

        shader = new ShaderProgram(
            vertexShader,
            fragmentShader
        );

        if (!shader.isCompiled()) {

            throw new RuntimeException(
                "Skybox shader failed to compile:\n" +
                    shader.getLog()
            );
        }

        // =========================================================
        // SKYBOX CUBE
        // =========================================================

        float size = 100f;

        float[] vertices = {

            // Front
            -size, -size,  size,
            size, -size,  size,
            size,  size,  size,
            -size,  size,  size,

            // Back
            -size, -size, -size,
            -size,  size, -size,
            size,  size, -size,
            size, -size, -size,

            // Left
            -size, -size, -size,
            -size, -size,  size,
            -size,  size,  size,
            -size,  size, -size,

            // Right
            size, -size, -size,
            size,  size, -size,
            size,  size,  size,
            size, -size,  size,

            // Top
            -size,  size, -size,
            -size,  size,  size,
            size,  size,  size,
            size,  size, -size,

            // Bottom
            -size, -size, -size,
            size, -size, -size,
            size, -size,  size,
            -size, -size,  size
        };

        short[] indices = {

            // Front
            0, 1, 2,
            2, 3, 0,

            // Back
            4, 5, 6,
            6, 7, 4,

            // Left
            8, 9, 10,
            10, 11, 8,

            // Right
            12, 13, 14,
            14, 15, 12,

            // Top
            16, 17, 18,
            18, 19, 16,

            // Bottom
            20, 21, 22,
            22, 23, 20
        };

        mesh = new Mesh(
            true,
            24,
            36,

            new VertexAttribute(
                VertexAttributes.Usage.Position,
                3,
                "a_position"
            )
        );

        mesh.setVertices(vertices);
        mesh.setIndices(indices);
    }

    public void render(PerspectiveCamera camera) {

        // =========================================================
        // SKYBOX FOLLOWS CAMERA
        // =========================================================

        transform.idt();

        transform.setToTranslation(
            camera.position.x,
            camera.position.y,
            camera.position.z
        );

        // =========================================================
        // SKYBOX RENDER STATE
        // =========================================================

        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthMask(false);

        Gdx.gl.glDisable(GL20.GL_CULL_FACE);

        shader.bind();

        Matrix4 combined =
            new Matrix4(camera.combined);

        combined.mul(transform);

        shader.setUniformMatrix(
            "u_projTrans",
            combined
        );

        mesh.render(
            shader,
            GL20.GL_TRIANGLES
        );

        // =========================================================
        // RESTORE NORMAL WORLD RENDERING
        // =========================================================

        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthMask(true);

        Gdx.gl.glEnable(GL20.GL_CULL_FACE);
    }

    public void dispose() {

        mesh.dispose();
        shader.dispose();
    }
}
