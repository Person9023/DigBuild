package com.abmstudios.DigBuild;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.PerspectiveCamera;

public class Player {

    public Model model;
    public ModelInstance instance;

    public float x;
    public float y;
    public float z;

    public float velocityY = 0;

    private static final float GRAVITY = 20f;
    private static final float PLAYER_HEIGHT = 1.8f;

    private boolean onGround;

    private static final float PLAYER_WIDTH = 0.6f;
    private static final float PLAYER_DEPTH = 0.6f;

    private Inventory inventory;

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getZ() {
        return z;
    }

    public Player() {

        x = 8f;
        y = 100f;
        z = 8f;

        ModelBuilder modelBuilder = new ModelBuilder();

        inventory = new Inventory();

        model = modelBuilder.createBox(
            1f,
            2f,
            1f,
            new Material(
                ColorAttribute.createDiffuse(Color.RED)
            ),
            com.badlogic.gdx.graphics.VertexAttributes.Usage.Position |
                com.badlogic.gdx.graphics.VertexAttributes.Usage.Normal
        );

        instance = new ModelInstance(model);

        instance.transform.setToTranslation(
            x,
            y + PLAYER_HEIGHT / 2f,
            z
        );
    }

    public Inventory getInventory() {
        return inventory;
    }

    private boolean sprinting = false;

    private float doubleTapTimer = 0f;
    private static final float DOUBLE_TAP_TIME = 0.25f;

    public void update(float delta, World world, PerspectiveCamera cam) {



        float speed = 5f;

        float sprintSpeed = 8f;

        float airControl = 0.5f;

        float movementMultiplier;



        // Count down the double-tap window
        if (doubleTapTimer > 0f) {
            doubleTapTimer -= delta;
        }

// Detect W being pressed
        if (Gdx.input.isKeyJustPressed(Input.Keys.W)) {

            if (doubleTapTimer > 0f) {
                // W was pressed twice quickly
                sprinting = true;
                doubleTapTimer = 0f;
            } else {
                // First W press
                doubleTapTimer = DOUBLE_TAP_TIME;
            }
        }

        if (!Gdx.input.isKeyPressed(Input.Keys.W)) {
            sprinting = false;
        }

        float currentSpeed = sprinting ? sprintSpeed : speed;

        if (onGround) {
            movementMultiplier = 1f;
        } else if (velocityY > 0) {
            movementMultiplier = airControl;
        } else {
            float fallAmount = Math.min(-velocityY / 20f, 1f);

            movementMultiplier = airControl + fallAmount * 0.4f;
        }

        float moveX = 0;
        float moveZ = 0;

// Camera's horizontal forward direction
        float forwardX = cam.direction.x;
        float forwardZ = cam.direction.z;

// Normalize the horizontal direction
        float length = (float)Math.sqrt(
            forwardX * forwardX +
                forwardZ * forwardZ
        );

        if (length != 0) {
            forwardX /= length;
            forwardZ /= length;
        }

// Right direction
        float rightX = -forwardZ;
        float rightZ = forwardX;

// Forward
        if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            moveX += forwardX * currentSpeed * delta * movementMultiplier;
            moveZ += forwardZ * currentSpeed * delta * movementMultiplier;
        }

// Backward
        if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            moveX -= forwardX * currentSpeed * delta * movementMultiplier;
            moveZ -= forwardZ * currentSpeed * delta * movementMultiplier;
        }

// Left
        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            moveX -= rightX * currentSpeed * delta* movementMultiplier;
            moveZ -= rightZ * currentSpeed * delta * movementMultiplier;
        }

// Right
        if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            moveX += rightX * currentSpeed * delta* movementMultiplier;
            moveZ += rightZ * currentSpeed * delta* movementMultiplier;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) && onGround) {
            velocityY = 8f;
            onGround = false;
        }



        // X collision

        if (moveX != 0) {

            float newX = x + moveX;

            if (!collides(newX, y, z, world) && isPositionInLoadedChunks(newX, z, world)) {
                x = newX;
            }
        }


        // Z collision

        if (moveZ != 0) {

            float newZ = z + moveZ;

            if (!collides(x, y, newZ, world) && isPositionInLoadedChunks(x, newZ, world)) {
                z = newZ;
            }
        }

        // Gravity
        velocityY -= GRAVITY * delta;

        float newY = y + velocityY * delta;

// Moving upward
        if (velocityY > 0) {

            if (!collides(x, newY, z, world)) {

                y = newY;

            } else {

                // Find the first solid block above the player
                float playerTop = y + PLAYER_HEIGHT;

                int minX = (int)Math.floor(
                    x - PLAYER_WIDTH / 2f + 0.001f
                );

                int maxX = (int)Math.floor(
                    x + PLAYER_WIDTH / 2f - 0.001f
                );

                int minZ = (int)Math.floor(
                    z - PLAYER_DEPTH / 2f + 0.001f
                );

                int maxZ = (int)Math.floor(
                    z + PLAYER_DEPTH / 2f - 0.001f
                );

                int minY = (int)Math.floor(playerTop);
                int maxY = (int)Math.floor(
                    newY + PLAYER_HEIGHT
                );

                float lowestCeiling = Float.MAX_VALUE;

                for (int bx = minX; bx <= maxX; bx++) {

                    for (int by = minY; by <= maxY; by++) {

                        for (int bz = minZ; bz <= maxZ; bz++) {

                            if (world.getBlock(bx, by, bz) != Block.AIR) {

                                lowestCeiling = Math.min(
                                    lowestCeiling,
                                    by
                                );
                            }
                        }
                    }
                }

                if (lowestCeiling != Float.MAX_VALUE) {

                    // Put the player's head exactly underneath the block
                    y = lowestCeiling - PLAYER_HEIGHT;
                }

                velocityY = 0;
            }

            onGround = false;
        }

// Moving downward
        else {

            if (!collides(x, newY, z, world)) {

                y = newY;
                onGround = false;

            } else {

                float halfWidth = PLAYER_WIDTH / 2f;
                float halfDepth = PLAYER_DEPTH / 2f;

                int minX = (int)Math.floor(
                    x - halfWidth + 0.001f
                );

                int maxX = (int)Math.floor(
                    x + halfWidth - 0.001f
                );

                int minZ = (int)Math.floor(
                    z - halfDepth + 0.001f
                );

                int maxZ = (int)Math.floor(
                    z + halfDepth - 0.001f
                );

                int blockY = (int)Math.floor(newY - 0.001f);

                int highestGround = Integer.MIN_VALUE;

                for (int bx = minX; bx <= maxX; bx++) {

                    for (int bz = minZ; bz <= maxZ; bz++) {

                        if (world.getBlock(bx, blockY, bz) != Block.AIR) {

                            highestGround = Math.max(
                                highestGround,
                                blockY
                            );
                        }
                    }
                }

                if (highestGround != Integer.MIN_VALUE) {

                    // Put the player's feet exactly on the block
                    y = highestGround + 1;
                    velocityY = 0;
                    onGround = true;

                } else {

                    y = newY;
                    onGround = false;
                }
            }
        }

        instance.transform.setToTranslation(
            x,
            y + PLAYER_HEIGHT / 2f,
            z
        );
    }

    private boolean collides(float testX, float testY, float testZ, World world) {

        float halfWidth = PLAYER_WIDTH / 2f;
        float halfDepth = PLAYER_DEPTH / 2f;

        int minX = (int)Math.floor(testX - halfWidth + 0.001f);
        int maxX = (int)Math.floor(testX + halfWidth - 0.001f);

        int minY = (int)Math.floor(testY + 0.001f);
        int maxY = (int)Math.floor(testY + PLAYER_HEIGHT - 0.001f);

        int minZ = (int)Math.floor(testZ - halfDepth + 0.001f);
        int maxZ = (int)Math.floor(testZ + halfDepth - 0.001f);

        for (int bx = minX; bx <= maxX; bx++) {

            for (int by = minY; by <= maxY; by++) {

                for (int bz = minZ; bz <= maxZ; bz++) {

                    if (world.getBlock(bx, by, bz) != Block.AIR) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public void dispose() {
        model.dispose();
    }

    private boolean isPositionInLoadedChunks(float testX, float testZ, World world) {

        int minX = (int)Math.floor(
            testX - PLAYER_WIDTH / 2f + 0.001f
        );

        int maxX = (int)Math.floor(
            testX + PLAYER_WIDTH / 2f - 0.001f
        );

        int minZ = (int)Math.floor(
            testZ - PLAYER_DEPTH / 2f + 0.001f
        );

        int maxZ = (int)Math.floor(
            testZ + PLAYER_DEPTH / 2f - 0.001f
        );

        for (int bx = minX; bx <= maxX; bx++) {

            for (int bz = minZ; bz <= maxZ; bz++) {

                if (!world.isChunkLoadedAt(bx, bz)) {
                    return false;
                }
            }
        }

        return true;
    }
}
