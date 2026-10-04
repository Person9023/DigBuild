package com.abmstudios.DigBuild;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;

import java.util.ArrayList;
import java.util.List;

public class Clouds {

    private static class Cloud {

        List<ModelInstance> puffs = new ArrayList<>();

        float x;
        float z;
        float speed;
    }

    private final List<Cloud> clouds = new ArrayList<>();

    private final Model cloudModel;

    /*
     * Height of the clouds above the world.
     */
    private static final float CLOUD_HEIGHT = 55f;

    /*
     * Clouds are kept inside this distance from
     * the player.
     *
     * This is NOT a world limit.
     */
    private static final float CLOUD_RANGE = 350f;

    /*
     * How many clouds exist at once.
     */
    private static final int CLOUD_COUNT = 35;

    /*
     * Cloud movement speed.
     */
    private static final float CLOUD_SPEED_MIN = 1.0f;
    private static final float CLOUD_SPEED_MAX = 2.0f;

    public Clouds() {

        ModelBuilder builder = new ModelBuilder();

        Color cloudColor = new Color(
            0.98f,
            0.98f,
            0.98f,
            0.82f
        );

        /*
         * Emissive makes the clouds stay white instead
         * of turning grey depending on the lighting.
         */
        Material material = new Material(
            ColorAttribute.createDiffuse(cloudColor),

            ColorAttribute.createEmissive(
                new Color(
                    0.98f,
                    0.98f,
                    0.98f,
                    1.0f
                )
            ),

            new BlendingAttribute(
                GL20.GL_SRC_ALPHA,
                GL20.GL_ONE_MINUS_SRC_ALPHA,
                0.82f
            )
        );

        cloudModel = builder.createBox(
            1f,
            1f,
            1f,
            material,
            VertexAttributes.Usage.Position |
                VertexAttributes.Usage.Normal
        );

        generateClouds(0f, 0f);
    }

    /*
     * Generate the initial cloud field around the player.
     */
    private void generateClouds(
        float centreX,
        float centreZ
    ) {

        clouds.clear();

        for (int i = 0; i < CLOUD_COUNT; i++) {

            Cloud cloud = new Cloud();

            cloud.x = centreX +
                MathUtils.random(
                    -CLOUD_RANGE,
                    CLOUD_RANGE
                );

            cloud.z = centreZ +
                MathUtils.random(
                    -CLOUD_RANGE,
                    CLOUD_RANGE
                );

            cloud.speed = MathUtils.random(
                CLOUD_SPEED_MIN,
                CLOUD_SPEED_MAX
            );

            createCloudPuffs(cloud);

            clouds.add(cloud);
        }
    }

    /*
     * Creates the individual chunky sections
     * making up a cloud.
     */
    private void createCloudPuffs(Cloud cloud) {

        int puffCount = MathUtils.random(5, 10);

        for (int p = 0; p < puffCount; p++) {

            ModelInstance puff =
                new ModelInstance(cloudModel);

            float width =
                MathUtils.random(7f, 16f);

            float height =
                MathUtils.random(2f, 4f);

            float depth =
                MathUtils.random(5f, 11f);

            float offsetX =
                MathUtils.random(-12f, 12f);

            float offsetZ =
                MathUtils.random(-5f, 5f);

            float offsetY =
                MathUtils.random(-1f, 2f);

            puff.transform.setToScaling(
                width,
                height,
                depth
            );

            puff.transform.setTranslation(
                cloud.x + offsetX,
                CLOUD_HEIGHT + offsetY,
                cloud.z + offsetZ
            );

            cloud.puffs.add(puff);
        }
    }

    /*
     * Update clouds and keep the cloud field centred
     * around the camera.
     */
    public void update(
        float delta,
        PerspectiveCamera camera
    ) {

        for (Cloud cloud : clouds) {

            /*
             * Move the cloud.
             */
            cloud.x += cloud.speed * delta;

            /*
             * Move all of its puffs with it.
             */
            for (ModelInstance puff : cloud.puffs) {

                float currentX =
                    puff.transform.val[Matrix4.M03];

                float currentZ =
                    puff.transform.val[Matrix4.M23];

                puff.transform.setTranslation(
                    currentX + cloud.speed * delta,
                    puff.transform.val[Matrix4.M13],
                    currentZ
                );
            }

            /*
             * If the cloud gets too far behind the player,
             * move it to the other side.
             */
            float dx = cloud.x - camera.position.x;
            float dz = cloud.z - camera.position.z;

            /*
             * Horizontal wrapping.
             */
            if (dx > CLOUD_RANGE) {

                float distance = CLOUD_RANGE * 2f;

                cloud.x -= distance;

                movePuffs(
                    cloud,
                    -distance,
                    0f
                );
            }
            else if (dx < -CLOUD_RANGE) {

                float distance = CLOUD_RANGE * 2f;

                cloud.x += distance;

                movePuffs(
                    cloud,
                    distance,
                    0f
                );
            }

            /*
             * Depth wrapping.
             */
            if (dz > CLOUD_RANGE) {

                float distance = CLOUD_RANGE * 2f;

                cloud.z -= distance;

                movePuffs(
                    cloud,
                    0f,
                    -distance
                );
            }
            else if (dz < -CLOUD_RANGE) {

                float distance = CLOUD_RANGE * 2f;

                cloud.z += distance;

                movePuffs(
                    cloud,
                    0f,
                    distance
                );
            }
        }
    }

    /*
     * Move every puff belonging to a cloud.
     */
    private void movePuffs(
        Cloud cloud,
        float x,
        float z
    ) {

        for (ModelInstance puff : cloud.puffs) {

            float currentX =
                puff.transform.val[Matrix4.M03];

            float currentZ =
                puff.transform.val[Matrix4.M23];

            puff.transform.setTranslation(
                currentX + x,
                puff.transform.val[Matrix4.M13],
                currentZ
            );
        }
    }

    public void render(
        ModelBatch modelBatch,
        PerspectiveCamera camera,
        Environment environment
    ) {

        Gdx.gl.glEnable(GL20.GL_BLEND);

        Gdx.gl.glBlendFunc(
            GL20.GL_SRC_ALPHA,
            GL20.GL_ONE_MINUS_SRC_ALPHA
        );

        /*
         * Clouds don't write to the depth buffer.
         */
        Gdx.gl.glDepthMask(false);

        for (Cloud cloud : clouds) {

            for (ModelInstance puff : cloud.puffs) {

                modelBatch.render(
                    puff,
                    environment
                );
            }
        }

        Gdx.gl.glDepthMask(true);

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    public void dispose() {

        cloudModel.dispose();
    }
}
