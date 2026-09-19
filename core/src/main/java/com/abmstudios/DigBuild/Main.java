package com.abmstudios.DigBuild;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;


                    public class Main implements ApplicationListener {
                        private InventoryUI inventoryUI;

                        private Texture crosshairTexture;
                        private SpriteBatch crosshairBatch;

                        public PerspectiveCamera cam;
                        public ModelBatch modelBatch;

                        public Environment environment;

                        public Chunk[][] chunks;
                        public ChunkMesh[][] chunkMeshes;

                        public World world;

                        public Player player;
                        private float cameraYaw = 0f;
                        private float cameraPitch = 0f;

                        private float mouseSensitivity = 0.2f;
                        public ShapeRenderer shapeRenderer;

                        private Vector3 selectedBlock = new Vector3();
                        private boolean hasSelectedBlock = false;

                        public SelectionBox selectionBox;

                        public BreakingOverlay breakingOverlay;
                        private float breakingProgress = 0f;
                        private boolean isBreaking = false;

                        private int breakingBlockX;
                        private int breakingBlockY;
                        private int breakingBlockZ;

                        private DroppedItemRenderer droppedItemRenderer;

                        private Inventory inventory;

                        private Vector3 placementBlock = new Vector3();
                        private boolean hasPlacementBlock = false;

                        public Inventory getInventory() {
                            return inventory;
                        }


                        private void updateBlockSelection() {

                            hasSelectedBlock = false;
                            hasPlacementBlock = false;

                            Vector3 rayPosition = new Vector3(cam.position);
                            Vector3 rayDirection = new Vector3(cam.direction).nor();

                            float step = 0.05f;
                            float reach = 5f;

                            int previousX = 0;
                            int previousY = 0;
                            int previousZ = 0;

                            boolean hasPreviousBlock = false;

                            for (float distance = 0; distance <= reach; distance += step) {

                                rayPosition.set(cam.position).mulAdd(rayDirection, distance);

                                int blockX = (int)Math.floor(rayPosition.x);
                                int blockY = (int)Math.floor(rayPosition.y);
                                int blockZ = (int)Math.floor(rayPosition.z);

                                // Only update the previous position when
                                // the ray enters a different block
                                if (!hasPreviousBlock ||
                                    blockX != previousX ||
                                    blockY != previousY ||
                                    blockZ != previousZ) {

                                    if (world.getBlock(blockX, blockY, blockZ) != Block.AIR) {

                                        selectedBlock.set(
                                            blockX,
                                            blockY,
                                            blockZ
                                        );

                                        selectionBox.setPosition(
                                            blockX,
                                            blockY,
                                            blockZ
                                        );

                                        hasSelectedBlock = true;

                    // The previous block is where we can place
                    // the new block.
                    if (hasPreviousBlock &&
                        world.getBlock(
                            previousX,
                            previousY,
                            previousZ
                        ) == Block.AIR) {

                        placementBlock.set(
                            previousX,
                            previousY,
                            previousZ
                        );

                        hasPlacementBlock = true;
                    }

                    return;
                }

                previousX = blockX;
                previousY = blockY;
                previousZ = blockZ;

                hasPreviousBlock = true;
            }
        }
    }

    private byte getBlockFromItem(Item item) {

        if (item == Item.GRASS) {
            return Block.GRASS;
        }

        if (item == Item.DIRT) {
            return Block.DIRT;
        }

        if (item == Item.STONE) {
            return Block.STONE;
        }

        if (item == Item.WOOD) {
            return Block.WOOD;
        }

        if (item == Item.LEAVES) {
            return Block.LEAVES;
        }

        if (item == Item.PLANKS) {
            return Block.PLANKS;
        }

        if (item == Item.CRAFTING_TABLE) {
            return Block.CRAFTING_TABLE;
        }

        return Block.AIR;
    }

    @Override
    public void create() {




        world = new World();

        droppedItemRenderer = new DroppedItemRenderer();

        environment = new Environment();
        environment.set(new ColorAttribute(
            ColorAttribute.AmbientLight,
            0.25f, 0.25f, 0.25f, 1f
        ));

        environment.add(new DirectionalLight().set(
            1.0f, 1.0f, 1.0f,
            -1f, -0.8f, -0.2f
        ));


        modelBatch = new ModelBatch();

        shapeRenderer = new ShapeRenderer();

        selectionBox = new SelectionBox();

        cam = new PerspectiveCamera(
            67,
            Gdx.graphics.getWidth(),
            Gdx.graphics.getHeight()
        );

        cam.position.set(30f, 30f, 30f);

        cam.lookAt(
            8f,
            10f,
            8f
        );

        cam.near = 0.1f;
        cam.far = 300f;

        cam.update();


        int renderDistance = 1;

        int size = renderDistance * 2 + 1;

        chunks = new Chunk[size][size];
        chunkMeshes = new ChunkMesh[size][size];

        for (int cx = -renderDistance; cx <= renderDistance; cx++) {

            for (int cz = -renderDistance; cz <= renderDistance; cz++) {

                int arrayX = cx + renderDistance;
                int arrayZ = cz + renderDistance;

                chunks[arrayX][arrayZ] = new Chunk(cx, cz);
                chunks[arrayX][arrayZ].generate();

                chunkMeshes[arrayX][arrayZ] = new ChunkMesh();
                chunkMeshes[arrayX][arrayZ].generate(
                    chunks[arrayX][arrayZ]
                );
            }
        }

        player = new Player();

        player.getInventory().load();

        inventoryUI = new InventoryUI(player.getInventory());



        breakingOverlay = new BreakingOverlay();

        cam.position.set(
            player.x,
            player.y + 1.6f,
            player.z
        );

        cam.near = 0.1f;
        cam.far = 300f;

        cam.update();

        crosshairTexture = new Texture(
            Gdx.files.internal("crosshair.png")
        );

        crosshairTexture.setFilter(
            Texture.TextureFilter.Nearest,
            Texture.TextureFilter.Nearest
        );

        crosshairBatch = new SpriteBatch();

        Gdx.input.setCursorCatched(true);


    }

    private void handleBlockBreaking(float delta) {

        // Nothing selected
        if (!hasSelectedBlock) {

            isBreaking = false;
            breakingProgress = 0f;

            breakingOverlay.setStage(0);

            return;
        }

        int selectedX = (int) selectedBlock.x;
        int selectedY = (int) selectedBlock.y;
        int selectedZ = (int) selectedBlock.z;


        // If we are already breaking something,
        // check whether we are still looking at that same block
        if (isBreaking) {

            if (selectedX != breakingBlockX ||
                selectedY != breakingBlockY ||
                selectedZ != breakingBlockZ) {

                // We looked at a different block,
                // so reset the breaking progress
                isBreaking = false;
                breakingProgress = 0f;

                breakingOverlay.setStage(0);
            }
        }


        // Holding left mouse button
        if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {

            // Start breaking a new block
            if (!isBreaking) {

                isBreaking = true;

                breakingProgress = 0f;

                breakingBlockX = selectedX;
                breakingBlockY = selectedY;
                breakingBlockZ = selectedZ;
            }

            breakingProgress += delta;

            byte breakingBlock = world.getBlock(
                breakingBlockX,
                breakingBlockY,
                breakingBlockZ
            );

            float breakingTime = getBreakingTime(breakingBlock);

            // Calculate crack stage
            int stage = (int)(
                (breakingProgress / breakingTime) * 7f
            );

            if (stage > 6) {
                stage = 6;
            }

            breakingOverlay.setStage(stage);


            // Position overlay on the block being broken
            breakingOverlay.setPosition(
                breakingBlockX,
                breakingBlockY,
                breakingBlockZ
            );


            // Break the block

            if (breakingProgress >= breakingTime) {

                // Get the block before removing it
                byte brokenBlock = world.getBlock(
                    breakingBlockX,
                    breakingBlockY,
                    breakingBlockZ
                );

                // Convert the block into its item
                // Get the item currently held
                int selectedSlot =
                    inventoryUI.getSelectedHotbarSlot();

                ItemStack stack =
                    player.getInventory().getSlot(selectedSlot);

                Item tool = null;

                if (stack != null) {
                    tool = stack.getItem();
                }

                // Check whether this tool is allowed to drop the block
                if (canMine(brokenBlock, tool)) {

                    Item item = Block.getItem(brokenBlock);

                    if (item != null) {

                        world.spawnDroppedItem(
                            item,
                            1,
                            breakingBlockX + 0.5f,
                            breakingBlockY + 0.5f,
                            breakingBlockZ + 0.5f
                        );
                    }
                }

                // Remove the block
                world.setBlock(
                    breakingBlockX,
                    breakingBlockY,
                    breakingBlockZ,
                    Block.AIR
                );

                breakingProgress = 0f;
                isBreaking = false;
                hasSelectedBlock = false;

                breakingOverlay.setStage(0);
            }



        } else {

            // Mouse released → reset
            isBreaking = false;
            breakingProgress = 0f;

            breakingOverlay.setStage(0);
        }
    }

    private void handleBlockPlacement() {

                            if (!hasSelectedBlock) {
                                return;
                            }

                            if (!Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT)) {
                                return;
                            }

                            int selectedX = (int) selectedBlock.x;
                            int selectedY = (int) selectedBlock.y;
                            int selectedZ = (int) selectedBlock.z;

                            // ---------------------------------------------------------
                            // Crafting table interaction
                            // ---------------------------------------------------------

                            if (world.getBlock(selectedX, selectedY, selectedZ)
                                == Block.CRAFTING_TABLE) {

                                inventoryUI.openCraftingTable();

                                return;
                            }

                            // ---------------------------------------------------------
                            // Normal block placement
                            // ---------------------------------------------------------

                            if (!hasPlacementBlock) {
                                return;
                            }

                            int slot = inventoryUI.getSelectedHotbarSlot();

                            ItemStack stack =
                                player.getInventory().getSlot(slot);

                            if (stack == null) {
                                return;
                            }

                            Item item = stack.getItem();

                            byte block = getBlockFromItem(item);

                            if (block == Block.AIR) {
                                return;
                            }

                            int placeX = (int) placementBlock.x;
                            int placeY = (int) placementBlock.y;
                            int placeZ = (int) placementBlock.z;

                            // Make sure the position is actually empty
                            if (world.getBlock(placeX, placeY, placeZ) != Block.AIR) {
                                return;
                            }

                            // ---------------------------------------------------------
                            // Don't allow the block to be placed inside the player
                            // ---------------------------------------------------------

                            float playerMinX = player.x - 0.3f;
                            float playerMaxX = player.x + 0.3f;

                            float playerMinY = player.y;
                            float playerMaxY = player.y + 1.8f;

                            float playerMinZ = player.z - 0.3f;
                            float playerMaxZ = player.z + 0.3f;

                            float blockMinX = placeX;
                            float blockMaxX = placeX + 1f;

                            float blockMinY = placeY;
                            float blockMaxY = placeY + 1f;

                            float blockMinZ = placeZ;
                            float blockMaxZ = placeZ + 1f;

                            boolean overlapsPlayer =
                                blockMaxX > playerMinX &&
                                    blockMinX < playerMaxX &&
                                    blockMaxY > playerMinY &&
                                    blockMinY < playerMaxY &&
                                    blockMaxZ > playerMinZ &&
                                    blockMinZ < playerMaxZ;

                            if (overlapsPlayer) {
                                return;
                            }

                            // ---------------------------------------------------------
                            // Place the block
                            // ---------------------------------------------------------

                            world.setBlock(
                                placeX,
                                placeY,
                                placeZ,
                                block
                            );

                            // Remove one item from the hotbar
                            player.getInventory().removeItem(
                                slot,
                                1
                            );

                            world.updateGrassBlocks();
                        }
    private float getBreakingTime(byte block) {

                            float breakingTime;

                            if (block == Block.GRASS) {
                                breakingTime = 0.5f;
                            }

                            else if (block == Block.DIRT) {
                                breakingTime = 0.5f;
                            }

                            else if (block == Block.STONE) {
                                breakingTime = 3f;
                            }

                            else if (block == Block.LEAVES) {
                                breakingTime = 0.2f;
                            }

                            else if (block == Block.WOOD) {
                                breakingTime = 1.5f;
                            }

                            else if (block == Block.PLANKS) {
                                breakingTime = 1.3f;
                            }

                            else if (block == Block.CRAFTING_TABLE) {
                                breakingTime = 1.3f;
                            }

                            else {
                                breakingTime = 1f;
                            }


                            // =============================================================
                            // WOODEN PICKAXE
                            // =============================================================

                            int selectedSlot =
                                inventoryUI.getSelectedHotbarSlot();

                            ItemStack stack =
                                player.getInventory().getSlot(selectedSlot);

                            if (stack != null &&
                                stack.getItem() == Item.WOODEN_PICKAXE) {

                                // Wooden pickaxe mines stone twice as fast
                                if (block == Block.STONE) {
                                    breakingTime = 2.0f;
                                }
                            }

                            return breakingTime;
                        }

    private void updateCamera() {

        float mouseX = Gdx.input.getDeltaX();
        float mouseY = Gdx.input.getDeltaY();

        cameraYaw += mouseX * mouseSensitivity;
        cameraPitch -= mouseY * mouseSensitivity;

        // Stop the player from looking completely upside-down
        cameraPitch = Math.max(-89f, Math.min(89f, cameraPitch));

        // Camera follows player's position
        cam.position.set(
            player.x,
            player.y + 1.6f,
            player.z
        );

        // Convert angles to radians
        float yaw = (float)Math.toRadians(cameraYaw);
        float pitch = (float)Math.toRadians(cameraPitch);

        // Calculate looking direction
        float directionX =
            (float)(Math.cos(pitch) * Math.sin(yaw));

        float directionY =
            (float)Math.sin(pitch);

        float directionZ =
            (float)(-Math.cos(pitch) * Math.cos(yaw));

        cam.direction.set(
            directionX,
            directionY,
            directionZ
        ).nor();

        cam.up.set(Vector3.Y);

        cam.update();
    }

    @Override
    public void render() {

        world.updateChunks(player);

        inventoryUI.update();

        boolean gamePaused = inventoryUI.isOpen();

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.app.exit();
        }

        float delta = Gdx.graphics.getDeltaTime();

        // =========================================================
        // GAME UPDATE
        // =========================================================

        if (!gamePaused) {

            world.update(delta);

            world.updateGrassBlocks();

            player.update(delta, world, cam);

            world.updateDroppedItems(player, delta);

            updateCamera();

            updateBlockSelection();

            handleBlockBreaking(delta);

            handleBlockPlacement();

            handleItemDropping();

            if (hasSelectedBlock && isBreaking) {

                breakingOverlay.setPosition(
                    (int) selectedBlock.x,
                    (int) selectedBlock.y,
                    (int) selectedBlock.z
                );
            }
        }


        // =========================================================
        // CLEAR SCREEN
        // =========================================================

        Gdx.gl.glViewport(
            0,
            0,
            Gdx.graphics.getWidth(),
            Gdx.graphics.getHeight()
        );

        Gdx.gl.glClear(
            GL20.GL_COLOR_BUFFER_BIT |
                GL20.GL_DEPTH_BUFFER_BIT
        );


        // =========================================================
        // 3D WORLD
        // =========================================================

        modelBatch.begin(cam);

        world.render(
            modelBatch,
            cam,
            environment
        );

        for (DroppedItem item : world.getDroppedItems()) {

            droppedItemRenderer.render(
                modelBatch,
                environment,
                item
            );
        }

        modelBatch.render(
            player.instance,
            environment
        );

        if (hasSelectedBlock && isBreaking) {

            breakingOverlay.render(
                modelBatch,
                environment
            );
        }

        if (hasSelectedBlock) {

            selectionBox.render(
                modelBatch,
                environment
            );
        }

        modelBatch.end();


        // =========================================================
// CROSSHAIR
// =========================================================

        float screenWidth =
            Gdx.graphics.getWidth();

        float screenHeight =
            Gdx.graphics.getHeight();


// Use the current window dimensions
        crosshairBatch.getProjectionMatrix().setToOrtho2D(
            0f,
            0f,
            screenWidth,
            screenHeight
        );

        crosshairBatch.getTransformMatrix().idt();


// Size of the crosshair on screen
        float crosshairSize = 18f;


// Calculate exact centre
        float crosshairX =
            (screenWidth - crosshairSize) / 2f;

        float crosshairY =
            (screenHeight - crosshairSize) / 2f;


        crosshairBatch.begin();

        crosshairBatch.draw(
            crosshairTexture,
            crosshairX,
            crosshairY,
            crosshairSize,
            crosshairSize
        );

        crosshairBatch.end();

        // =========================================================
        // INVENTORY UI
        // =========================================================

        inventoryUI.render();
    }



    @Override
    public void dispose() {
        player.getInventory().save();
        modelBatch.dispose();

        world.dispose();
        player.dispose();
        shapeRenderer.dispose();
        selectionBox.dispose();
        crosshairTexture.dispose();
        crosshairBatch.dispose();
    }


    private void handleItemDropping() {

                            // Only drop items when the inventory is closed
                            if (inventoryUI.isOpen()) {
                                return;
                            }

                            // Q pressed
                            if (!Gdx.input.isKeyJustPressed(Input.Keys.Q)) {
                                return;
                            }

                            // Get the selected hotbar slot
                            int slot =
                                inventoryUI.getSelectedHotbarSlot();

                            ItemStack stack =
                                player.getInventory().getSlot(slot);

                            // Nothing to drop
                            if (stack == null) {
                                return;
                            }

                            Item item =
                                stack.getItem();


                            // ---------------------------------------------------------
                            // Direction the player is looking
                            // ---------------------------------------------------------

                            Vector3 dropDirection =
                                new Vector3(cam.direction);


                            // Only use horizontal direction for the throw
                            dropDirection.y = 0f;

                            dropDirection.nor();


                            // ---------------------------------------------------------
                            // Spawn position
                            // ---------------------------------------------------------

                            float dropDistance = 0.6f;

                            float dropX =
                                player.x +
                                    dropDirection.x * dropDistance;

                            float dropY =
                                player.y + 1.0f;

                            float dropZ =
                                player.z +
                                    dropDirection.z * dropDistance;


                            // ---------------------------------------------------------
                            // Throw velocity
                            // ---------------------------------------------------------

                            float throwSpeed = 3.0f;

                            float velocityX =
                                dropDirection.x * throwSpeed;

                            float velocityZ =
                                dropDirection.z * throwSpeed;


                            // ---------------------------------------------------------
                            // Spawn the item
                            // ---------------------------------------------------------

                            world.spawnDroppedItem(
                                item,
                                1,
                                dropX,
                                dropY,
                                dropZ,
                                velocityX,
                                velocityZ
                            );


                            // ---------------------------------------------------------
                            // Remove one item from inventory
                            // ---------------------------------------------------------

                            player.getInventory().removeItem(
                                slot,
                                1
                            );
                        }


    private boolean canMine(byte block, Item tool) {

                            // Stone requires a pickaxe
                            if (block == Block.STONE) {

                                if (tool == Item.WOODEN_PICKAXE) {
                                    return true;
                                }

                                return false;
                            }

                            // All other blocks can currently be mined
                            // with any item, including an empty hand.
                            return true;
    }

                        @Override
    public void resize(int width, int height) {

        cam.viewportWidth = width;
        cam.viewportHeight = height;

        cam.update();
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }
}
