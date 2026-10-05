package com.abmstudios.DigBuild;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;

public class Skybox {

    private final Mesh skyMesh;
    private final ShaderProgram skyShader;

    private final Mesh sunMesh;
    private final Mesh moonMesh;
    private final ShaderProgram objectShader;

    private final Texture sunTexture;
    private final Texture moonTexture;

    private final Matrix4 view = new Matrix4();
    private final Matrix4 projection = new Matrix4();
    private final Matrix4 combined = new Matrix4();

    private float timeOfDay = 0.25f;

    private final Vector3 sunDirection = new Vector3();
    private final Vector3 moonDirection = new Vector3();

    private final Vector3 right = new Vector3();
    private final Vector3 up = new Vector3();
    private final Vector3 position = new Vector3();

    private final Vector3 referenceUp = new Vector3();
    private final Vector3 bottomLeft = new Vector3();
    private final Vector3 bottomRight = new Vector3();
    private final Vector3 topRight = new Vector3();
    private final Vector3 topLeft = new Vector3();

    public Skybox() {

        ShaderProgram.pedantic = false;

        /*
         * =========================================================
         * LOAD SUN AND MOON TEXTURES
         * =========================================================
         */

        sunTexture = new Texture(
            Gdx.files.internal("sun.png")
        );

        moonTexture = new Texture(
            Gdx.files.internal("moon.png")
        );

        sunTexture.setFilter(
            Texture.TextureFilter.Nearest,
            Texture.TextureFilter.Nearest
        );

        moonTexture.setFilter(
            Texture.TextureFilter.Nearest,
            Texture.TextureFilter.Nearest
        );

        sunTexture.setWrap(
            Texture.TextureWrap.ClampToEdge,
            Texture.TextureWrap.ClampToEdge
        );

        moonTexture.setWrap(
            Texture.TextureWrap.ClampToEdge,
            Texture.TextureWrap.ClampToEdge
        );


        /*
         * =========================================================
         * SKY VERTEX SHADER
         * =========================================================
         */

        String skyVertexShader =
            "attribute vec3 a_position;\n" +

                "uniform mat4 u_projTrans;\n" +

                "varying vec3 v_position;\n" +

                "void main() {\n" +

                "    v_position = normalize(a_position);\n" +

                "    gl_Position = u_projTrans * vec4(a_position, 1.0);\n" +

                "}";


        /*
         * =========================================================
         * SKY FRAGMENT SHADER
         * =========================================================
         */

        String skyFragmentShader =
            "#ifdef GL_ES\n" +
                "precision mediump float;\n" +
                "#endif\n" +

                "varying vec3 v_position;\n" +
                "uniform float u_timeOfDay;\n" +

                "float random(vec2 p) {\n" +
                "    return fract(\n" +
                "        sin(dot(p, vec2(127.1, 311.7))) *\n" +
                "        43758.5453123\n" +
                "    );\n" +
                "}\n" +

                "float stars(vec3 direction) {\n" +

                "    if (direction.y <= 0.05) {\n" +
                "        return 0.0;\n" +
                "    }\n" +

                "    vec2 uv = direction.xz /\n" +
                "        (direction.y + 0.35);\n" +

                "    uv *= 35.0;\n" +

                "    vec2 cell = floor(uv);\n" +
                "    vec2 local = fract(uv) - 0.5;\n" +

                "    float rnd = random(cell);\n" +

                "    if (rnd < 0.965) {\n" +
                "        return 0.0;\n" +
                "    }\n" +

                "    vec2 starOffset = vec2(\n" +
                "        random(cell + 13.7),\n" +
                "        random(cell + 91.4)\n" +
                "    ) - 0.5;\n" +

                "    float distance = length(\n" +
                "        local - starOffset * 0.7\n" +
                "    );\n" +

                "    float size = mix(\n" +
                "        0.035,\n" +
                "        0.075,\n" +
                "        random(cell + 44.2)\n" +
                "    );\n" +

                "    float brightness = smoothstep(\n" +
                "        size,\n" +
                "        0.0,\n" +
                "        distance\n" +
                "    );\n" +

                "    return brightness * mix(\n" +
                "        0.45,\n" +
                "        1.0,\n" +
                "        random(cell + 72.3)\n" +
                "    );\n" +

                "}\n" +

                "void main() {\n" +

                "    float t = clamp(\n" +
                "        (v_position.y + 1.0) / 2.0,\n" +
                "        0.0,\n" +
                "        1.0\n" +
                "    );\n" +

                "    vec3 dayBottom = vec3(\n" +
                "        0.08,\n" +
                "        0.22,\n" +
                "        0.45\n" +
                "    );\n" +

                "    vec3 dayTop = vec3(\n" +
                "        0.55,\n" +
                "        0.80,\n" +
                "        1.00\n" +
                "    );\n" +

                "    vec3 nightBottom = vec3(\n" +
                "        0.015,\n" +
                "        0.025,\n" +
                "        0.08\n" +
                "    );\n" +

                "    vec3 nightTop = vec3(\n" +
                "        0.025,\n" +
                "        0.055,\n" +
                "        0.16\n" +
                "    );\n" +

                "    vec3 sunsetBottom = vec3(\n" +
                "        0.35,\n" +
                "        0.10,\n" +
                "        0.08\n" +
                "    );\n" +

                "    vec3 sunsetTop = vec3(\n" +
                "        0.75,\n" +
                "        0.35,\n" +
                "        0.18\n" +
                "    );\n" +

                "    vec3 daySky = mix(\n" +
                "        dayBottom,\n" +
                "        dayTop,\n" +
                "        t\n" +
                "    );\n" +

                "    vec3 nightSky = mix(\n" +
                "        nightBottom,\n" +
                "        nightTop,\n" +
                "        t\n" +
                "    );\n" +

                "    vec3 sunsetSky = mix(\n" +
                "        sunsetBottom,\n" +
                "        sunsetTop,\n" +
                "        t\n" +
                "    );\n" +

                "    float angle = (\n" +
                "        u_timeOfDay - 0.25\n" +
                "    ) * 6.2831853;\n" +

                "    float sunHeight = sin(angle);\n" +

                "    float daylight = clamp(\n" +
                "        (sunHeight + 0.12) / 0.30,\n" +
                "        0.0,\n" +
                "        1.0\n" +
                "    );\n" +

                "    daylight = daylight * daylight *\n" +
                "        (3.0 - 2.0 * daylight);\n" +

                "    float sunset = 1.0 - smoothstep(\n" +
                "        0.0,\n" +
                "        0.30,\n" +
                "        abs(sunHeight)\n" +
                "    );\n" +

                "    vec3 sky = mix(\n" +
                "        nightSky,\n" +
                "        daySky,\n" +
                "        daylight\n" +
                "    );\n" +

                "    sky = mix(\n" +
                "        sky,\n" +
                "        sunsetSky,\n" +
                "        sunset * 0.45\n" +
                "    );\n" +

                "    float starVisibility = 1.0 - daylight;\n" +

                "    float starBrightness = stars(\n" +
                "        v_position\n" +
                "    );\n" +

                "    sky += vec3(\n" +
                "        1.0,\n" +
                "        0.95,\n" +
                "        0.85\n" +
                "    ) * starBrightness * starVisibility;\n" +

                "    gl_FragColor = vec4(\n" +
                "        sky,\n" +
                "        1.0\n" +
                "    );\n" +

                "}";


        skyShader = new ShaderProgram(
            skyVertexShader,
            skyFragmentShader
        );

        if (!skyShader.isCompiled()) {

            throw new RuntimeException(
                "Skybox shader failed to compile:\n" +
                    skyShader.getLog()
            );
        }


        /*
         * =========================================================
         * SKYBOX CUBE
         * =========================================================
         */

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

            0, 1, 2,
            2, 3, 0,

            4, 5, 6,
            6, 7, 4,

            8, 9, 10,
            10, 11, 8,

            12, 13, 14,
            14, 15, 12,

            16, 17, 18,
            18, 19, 16,

            20, 21, 22,
            22, 23, 20
        };


        skyMesh = new Mesh(
            true,
            24,
            36,
            new VertexAttribute(
                VertexAttributes.Usage.Position,
                3,
                "a_position"
            )
        );

        skyMesh.setVertices(vertices);
        skyMesh.setIndices(indices);


        /*
         * =========================================================
         * SUN / MOON SHADER
         * =========================================================
         */

        String objectVertexShader =
            "attribute vec3 a_position;\n" +
                "attribute vec2 a_texCoord0;\n" +

                "uniform mat4 u_projTrans;\n" +

                "varying vec2 v_texCoords;\n" +

                "void main() {\n" +

                "    v_texCoords = a_texCoord0;\n" +

                "    gl_Position = u_projTrans *\n" +
                "        vec4(a_position, 1.0);\n" +

                "}";


        String objectFragmentShader =
            "#ifdef GL_ES\n" +
                "precision mediump float;\n" +
                "#endif\n" +

                "varying vec2 v_texCoords;\n" +

                "uniform sampler2D u_texture;\n" +

                "uniform float u_alpha;\n" +

                "void main() {\n" +

                "    vec4 color = texture2D(\n" +
                "        u_texture,\n" +
                "        v_texCoords\n" +
                "    );\n" +

                "    color.a *= u_alpha;\n" +

                "    gl_FragColor = color;\n" +

                "}";


        objectShader = new ShaderProgram(
            objectVertexShader,
            objectFragmentShader
        );

        if (!objectShader.isCompiled()) {

            throw new RuntimeException(
                "Sun/moon shader failed to compile:\n" +
                    objectShader.getLog()
            );
        }


        /*
         * =========================================================
         * SUN / MOON MESHES
         * =========================================================
         */

        sunMesh = createQuadMesh();
        moonMesh = createQuadMesh();
    }


    /*
     * =============================================================
     * CREATE TEXTURED QUAD
     * =============================================================
     */

    private Mesh createQuadMesh() {

        Mesh mesh = new Mesh(
            true,
            4,
            6,

            new VertexAttribute(
                VertexAttributes.Usage.Position,
                3,
                "a_position"
            ),

            new VertexAttribute(
                VertexAttributes.Usage.TextureCoordinates,
                2,
                "a_texCoord0"
            )
        );

        short[] indices = {
            0, 1, 2,
            2, 3, 0
        };

        mesh.setIndices(indices);

        return mesh;
    }


    /*
     * =============================================================
     * TIME
     * =============================================================
     */

    public void setTimeOfDay(float time) {

        timeOfDay = time;

        if (timeOfDay < 0f) {
            timeOfDay += 1f;
        }

        if (timeOfDay >= 1f) {
            timeOfDay -= 1f;
        }
    }


    /*
     * =============================================================
     * CALCULATE SUN / MOON DIRECTION
     * =============================================================
     */

    private void updateDirections() {

        float angle =
            (timeOfDay - 0.25f) *
                MathUtils.PI2;

        float height =
            MathUtils.sin(angle);

        float horizontal =
            MathUtils.cos(angle);

        sunDirection.set(
            horizontal,
            height,
            0f
        ).nor();

        moonDirection.set(
            -sunDirection.x,
            -sunDirection.y,
            -sunDirection.z
        ).nor();
    }


    /*
     * =============================================================
     * CREATE SKY OBJECT QUAD
     * =============================================================
     */

    private void updateQuad(
        Mesh mesh,
        PerspectiveCamera camera,
        Vector3 direction,
        float size
    ) {

        position.set(camera.position)
            .mulAdd(direction, 80f);


        referenceUp.set(
            0f,
            1f,
            0f
        );

        if (Math.abs(direction.y) > 0.95f) {

            referenceUp.set(
                0f,
                0f,
                1f
            );
        }


        right.set(referenceUp)
            .crs(direction)
            .nor();

        up.set(direction)
            .crs(right)
            .nor();


        bottomLeft.set(position)
            .mulAdd(right, -size)
            .mulAdd(up, -size);

        bottomRight.set(position)
            .mulAdd(right, size)
            .mulAdd(up, -size);

        topRight.set(position)
            .mulAdd(right, size)
            .mulAdd(up, size);

        topLeft.set(position)
            .mulAdd(right, -size)
            .mulAdd(up, size);


        /*
         * Position + texture coordinates.
         *
         * Bottom-left  = 0,0
         * Bottom-right = 1,0
         * Top-right    = 1,1
         * Top-left     = 0,1
         */

        float[] vertices = {

            bottomLeft.x,
            bottomLeft.y,
            bottomLeft.z,
            0f,
            0f,

            bottomRight.x,
            bottomRight.y,
            bottomRight.z,
            1f,
            0f,

            topRight.x,
            topRight.y,
            topRight.z,
            1f,
            1f,

            topLeft.x,
            topLeft.y,
            topLeft.z,
            0f,
            1f
        };

        mesh.setVertices(vertices);
    }


    /*
     * =============================================================
     * RENDER
     * =============================================================
     */

    public void render(PerspectiveCamera camera) {

        updateDirections();


        /*
         * =========================================================
         * SKYBOX
         * =========================================================
         */

        view.set(camera.view);

        view.val[Matrix4.M03] = 0f;
        view.val[Matrix4.M13] = 0f;
        view.val[Matrix4.M23] = 0f;

        projection.set(camera.projection);

        combined.set(projection);
        combined.mul(view);


        Gdx.gl.glDisable(
            GL20.GL_DEPTH_TEST
        );

        Gdx.gl.glDepthMask(false);

        Gdx.gl.glDisable(
            GL20.GL_CULL_FACE
        );


        skyShader.bind();

        skyShader.setUniformf(
            "u_timeOfDay",
            timeOfDay
        );

        skyShader.setUniformMatrix(
            "u_projTrans",
            combined
        );

        skyMesh.render(
            skyShader,
            GL20.GL_TRIANGLES
        );


        /*
         * =========================================================
         * PREPARE SUN / MOON
         * =========================================================
         */

        updateQuad(
            sunMesh,
            camera,
            sunDirection,
            7f
        );

        updateQuad(
            moonMesh,
            camera,
            moonDirection,
            5.5f
        );


        /*
         * =========================================================
         * ENABLE TEXTURE BLENDING
         * =========================================================
         */

        Gdx.gl.glEnable(
            GL20.GL_BLEND
        );

        Gdx.gl.glBlendFunc(
            GL20.GL_SRC_ALPHA,
            GL20.GL_ONE_MINUS_SRC_ALPHA
        );


        objectShader.bind();

        objectShader.setUniformMatrix(
            "u_projTrans",
            camera.combined
        );

        objectShader.setUniformi(
            "u_texture",
            0
        );


        /*
         * =========================================================
         * SUN
         * =========================================================
         */

        float daylight =
            MathUtils.clamp(
                (sunDirection.y + 0.12f) / 0.30f,
                0f,
                1f
            );

        daylight =
            daylight *
                daylight *
                (3f - 2f * daylight);


        if (sunDirection.y > -0.12f) {

            float alpha =
                MathUtils.clamp(
                    daylight * 2f,
                    0.15f,
                    1f
                );

            objectShader.setUniformf(
                "u_alpha",
                alpha
            );

            sunTexture.bind(0);

            sunMesh.render(
                objectShader,
                GL20.GL_TRIANGLES
            );
        }


        /*
         * =========================================================
         * MOON
         * =========================================================
         */

        if (moonDirection.y > -0.12f) {

            objectShader.setUniformf(
                "u_alpha",
                0.95f
            );

            moonTexture.bind(0);

            moonMesh.render(
                objectShader,
                GL20.GL_TRIANGLES
            );
        }


        /*
         * =========================================================
         * RESTORE OPENGL STATE
         * =========================================================
         */

        Gdx.gl.glDisable(
            GL20.GL_BLEND
        );

        Gdx.gl.glEnable(
            GL20.GL_DEPTH_TEST
        );

        Gdx.gl.glDepthMask(true);

        Gdx.gl.glEnable(
            GL20.GL_CULL_FACE
        );
    }


    /*
     * =============================================================
     * DISPOSE
     * =============================================================
     */

    public void dispose() {

        skyMesh.dispose();
        skyShader.dispose();

        sunMesh.dispose();
        moonMesh.dispose();

        objectShader.dispose();

        sunTexture.dispose();
        moonTexture.dispose();
    }
}
