package com.abmstudios.DigBuild;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;

public class World {

    private final int renderDistance = 2;

    // ---------------------------------------------------------
    // Save system
    // ---------------------------------------------------------

    private static final int SAVE_VERSION = 1;

    private final FileHandle saveDirectory;

    /*
     * Chunks that have actually existed in the player's world.
     *
     * Once a chunk has been seen, its complete state is saved
     * when it unloads.
     */
    private final Set<String> discoveredChunks =
        ConcurrentHashMap.newKeySet();


    // ---------------------------------------------------------
    // Loaded chunks
    // ---------------------------------------------------------

    private final Map<String, Chunk> chunks =
        new HashMap<>();

    // Chunk meshes
    private final Map<String, ChunkMesh> chunkMeshes =
        new HashMap<>();

    // Dirt timers
    private final Map<String, Float> dirtTimers =
        new HashMap<>();

    // Dropped items
    private final List<DroppedItem> droppedItems =
        new ArrayList<>();


    // ---------------------------------------------------------
    // Threaded chunk operations
    // ---------------------------------------------------------

    private final ExecutorService chunkExecutor =
        Executors.newSingleThreadExecutor();

    // Mesh generation is CPU-heavy, so it gets its own worker pool.
    private final ExecutorService meshExecutor =
        Executors.newFixedThreadPool(
            Math.max(1, Runtime.getRuntime().availableProcessors() - 1)
        );

    private final Set<String> meshesGenerating =
        ConcurrentHashMap.newKeySet();

    private final ConcurrentLinkedQueue<MeshGenerationResult>
        completedMeshOperations =
        new ConcurrentLinkedQueue<>();

    // Main-thread-only mesh revision numbers.
    // They prevent an old background mesh from replacing a newer one.
    private final Map<String, Integer> meshVersions =
        new HashMap<>();

    /*
     * A chunk operation that has completed in the background.
     */
    private final ConcurrentLinkedQueue<ChunkResult>
        completedChunkOperations =
        new ConcurrentLinkedQueue<>();

    private final Set<String> treesGenerating =
        ConcurrentHashMap.newKeySet();

    private final ConcurrentLinkedQueue<TreeGenerationResult>
        completedTreeOperations =
        new ConcurrentLinkedQueue<>();

    private final Set<String> generatingChunks =
        ConcurrentHashMap.newKeySet();

    private final Set<String> loadingChunks =
        ConcurrentHashMap.newKeySet();

    private final Set<String> savingChunks =
        ConcurrentHashMap.newKeySet();


    // Prevent trees from being generated more than once
    // while a chunk remains loaded.
    private final Set<String> treesGenerated =
        new HashSet<>();



    private static final int MAX_CHUNKS_PER_FRAME = 1;


    private static class BlockChange {

        int x;
        int y;
        int z;
        byte block;

        BlockChange(
            int x,
            int y,
            int z,
            byte block
        ) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.block = block;
        }
    }


    private static class TreeGenerationResult {

        String key;
        List<BlockChange> changes;

        TreeGenerationResult(
            String key,
            List<BlockChange> changes
        ) {
            this.key = key;
            this.changes = changes;
        }
    }



    // ---------------------------------------------------------
    // Constructor
    // ---------------------------------------------------------

    public World() {

        saveDirectory =
            Gdx.files.local(
                "DigBuild/saves/world/chunks"
            );

        saveDirectory.mkdirs();


        /*
         * Generate the initial 3x3 area synchronously.
         *
         * If a save exists, load it instead.
         */

        for (int chunkX = -renderDistance;
             chunkX <= renderDistance;
             chunkX++) {

            for (int chunkZ = -renderDistance;
                 chunkZ <= renderDistance;
                 chunkZ++) {

                loadOrGenerateInitialChunk(
                    chunkX,
                    chunkZ
                );
            }
        }


        /*
         * Trees only need to be generated for newly generated
         * chunks. Loaded chunks already contain their trees.
         */

        for (int chunkX = -renderDistance;
             chunkX <= renderDistance;
             chunkX++) {

            for (int chunkZ = -renderDistance;
                 chunkZ <= renderDistance;
                 chunkZ++) {

                String key =
                    getChunkKey(chunkX, chunkZ);

                if (!discoveredChunks.contains(key)) {
                    generateInitialTreesInChunk(
                        chunkX,
                        chunkZ
                    );
                }
            }
        }


        /*
         * Mark the initial chunks as discovered.
         */
        for (int chunkX = -renderDistance;
             chunkX <= renderDistance;
             chunkX++) {

            for (int chunkZ = -renderDistance;
                 chunkZ <= renderDistance;
                 chunkZ++) {

                discoveredChunks.add(
                    getChunkKey(chunkX, chunkZ)
                );
            }
        }


        // Create initial meshes.
        for (int chunkX = -renderDistance;
             chunkX <= renderDistance;
             chunkX++) {

            for (int chunkZ = -renderDistance;
                 chunkZ <= renderDistance;
                 chunkZ++) {

                generateMesh(chunkX, chunkZ);
            }
        }
    }

    private void generateInitialTreesInChunk(
        int chunkX,
        int chunkZ
    ) {
        String key =
            getChunkKey(
                chunkX,
                chunkZ
            );

        if (treesGenerated.contains(key)) {
            return;
        }

        Chunk chunk =
            chunks.get(key);

        if (chunk == null) {
            return;
        }

        byte[][][] blocks =
            new byte[
                Chunk.WIDTH
                ][
                Chunk.HEIGHT
                ][
                Chunk.DEPTH
                ];

        int[][] surfaceHeights =
            new int[
                Chunk.WIDTH
                ][
                Chunk.DEPTH
                ];

        for (int x = 0;
             x < Chunk.WIDTH;
             x++) {

            for (int y = 0;
                 y < Chunk.HEIGHT;
                 y++) {

                for (int z = 0;
                     z < Chunk.DEPTH;
                     z++) {

                    blocks[x][y][z] =
                        chunk.getBlock(
                            x,
                            y,
                            z
                        );
                }
            }
        }

        for (int x = 0;
             x < Chunk.WIDTH;
             x++) {

            for (int z = 0;
                 z < Chunk.DEPTH;
                 z++) {

                surfaceHeights[x][z] =
                    chunk.getSurfaceHeight(
                        x,
                        z
                    );
            }
        }

        List<BlockChange> changes =
            generateTreesInChunkBackground(
                chunkX,
                chunkZ,
                blocks,
                surfaceHeights
            );

        for (BlockChange change : changes) {
            setBlock(
                change.x,
                change.y,
                change.z,
                change.block,
                false
            );
        }

        treesGenerated.add(key);
    }


    private static class MeshGenerationResult {
        String key;
        int version;
        ChunkMesh.MeshData data;

        MeshGenerationResult(
            String key,
            int version,
            ChunkMesh.MeshData data
        ) {
            this.key = key;
            this.version = version;
            this.data = data;
        }
    }


    // ---------------------------------------------------------
    // Chunk operation result
    // ---------------------------------------------------------

    private static class ChunkResult {

        enum Type {
            LOADED,
            GENERATED
        }

        Type type;
        Chunk chunk;

        ChunkResult(
            Type type,
            Chunk chunk
        ) {

            this.type = type;
            this.chunk = chunk;
        }
    }


    // ---------------------------------------------------------
    // Chunk key
    // ---------------------------------------------------------

    private String getChunkKey(
        int chunkX,
        int chunkZ
    ) {

        return chunkX + "," + chunkZ;
    }


    // ---------------------------------------------------------
    // Save file
    // ---------------------------------------------------------

    private FileHandle getChunkSaveFile(
        int chunkX,
        int chunkZ
    ) {

        return saveDirectory.child(
            chunkX + "_" + chunkZ + ".chunk"
        );
    }


    // ---------------------------------------------------------
    // Get chunk
    // ---------------------------------------------------------

    public Chunk getChunk(
        int chunkX,
        int chunkZ
    ) {

        return chunks.get(
            getChunkKey(chunkX, chunkZ)
        );
    }


    // ---------------------------------------------------------
    // Get mesh
    // ---------------------------------------------------------

    public ChunkMesh getChunkMesh(
        int chunkX,
        int chunkZ
    ) {

        return chunkMeshes.get(
            getChunkKey(chunkX, chunkZ)
        );
    }


    // ---------------------------------------------------------
    // Initial chunk load
    // ---------------------------------------------------------

    private void loadOrGenerateInitialChunk(
        int chunkX,
        int chunkZ
    ) {

        String key =
            getChunkKey(chunkX, chunkZ);

        FileHandle file =
            getChunkSaveFile(chunkX, chunkZ);

        if (file.exists()) {

            try {

                Chunk chunk =
                    loadChunkFromFile(file);

                chunks.put(key, chunk);

                discoveredChunks.add(key);

                loadDroppedItemsFromChunk(
                    file
                );

                return;

            } catch (Exception e) {

                Gdx.app.error(
                    "World",
                    "Failed to load chunk " + key +
                        ". Regenerating it.",
                    e
                );
            }
        }

        Chunk chunk =
            new Chunk(chunkX, chunkZ);

        chunk.generate();

        chunks.put(key, chunk);
    }


    // ---------------------------------------------------------
    // Request chunk
    // ---------------------------------------------------------

    private void requestChunkGeneration(
        int chunkX,
        int chunkZ
    ) {

        String key =
            getChunkKey(chunkX, chunkZ);

        if (chunks.containsKey(key)) {
            return;
        }

        if (generatingChunks.contains(key) ||
            loadingChunks.contains(key)) {

            return;
        }

        FileHandle file =
            getChunkSaveFile(chunkX, chunkZ);


        /*
         * A saved chunk should be loaded rather than regenerated.
         */

        if (file.exists()) {

            if (!loadingChunks.add(key)) {
                return;
            }

            chunkExecutor.submit(() -> {

                try {

                    Chunk chunk =
                        loadChunkFromFile(file);

                    completedChunkOperations.add(
                        new ChunkResult(
                            ChunkResult.Type.LOADED,
                            chunk
                        )
                    );

                } catch (Exception e) {

                    Gdx.app.error(
                        "World",
                        "Failed to load chunk " + key,
                        e
                    );

                } finally {

                    loadingChunks.remove(key);
                }
            });

            return;
        }


        /*
         * No save exists, so generate a brand-new chunk.
         */

        if (!generatingChunks.add(key)) {
            return;
        }

        chunkExecutor.submit(() -> {

            try {

                Chunk chunk =
                    new Chunk(chunkX, chunkZ);

                chunk.generate();

                completedChunkOperations.add(
                    new ChunkResult(
                        ChunkResult.Type.GENERATED,
                        chunk
                    )
                );

            } finally {

                generatingChunks.remove(key);
            }
        });
    }


    // ---------------------------------------------------------
    // Process completed chunk operations
    // ---------------------------------------------------------

    private void processCompletedChunks() {

        processCompletedMeshes();
        processCompletedTrees();

        int processed = 0;

        while (processed < MAX_CHUNKS_PER_FRAME) {

            ChunkResult result =
                completedChunkOperations.poll();

            if (result == null) {
                break;
            }

            Chunk chunk = result.chunk;

            int chunkX =
                chunk.getChunkX();

            int chunkZ =
                chunk.getChunkZ();

            String key =
                getChunkKey(chunkX, chunkZ);


            if (chunks.containsKey(key)) {
                processed++;
                continue;
            }


            chunks.put(key, chunk);


            /*
             * Loaded saved chunks already contain their complete
             * state, including trees.
             *
             * Newly generated chunks need tree generation.
             */

            if (result.type ==
                ChunkResult.Type.LOADED) {

                /*
                 * Saved chunks already contain their complete
                 * state, including any trees.
                 */
                treesGenerated.add(key);

            } else if (result.type ==
                ChunkResult.Type.GENERATED) {

                tryGenerateTreesAround(
                    chunkX,
                    chunkZ
                );
            }

            /*
             * A chunk is considered discovered once it has
             * successfully entered the loaded world.
             */
            discoveredChunks.add(key);


            requestMeshGeneration(
                chunkX,
                chunkZ
            );

            regenerateMeshesAround(
                chunkX,
                chunkZ
            );

            processed++;
        }
    }


    // ---------------------------------------------------------
    // Update chunks around player
    // ---------------------------------------------------------

    public void updateChunks(Player player) {

        processCompletedChunks();


        int playerChunkX =
            Math.floorDiv(
                (int)Math.floor(player.getX()),
                Chunk.WIDTH
            );

        int playerChunkZ =
            Math.floorDiv(
                (int)Math.floor(player.getZ()),
                Chunk.DEPTH
            );


        /*
         * Request chunks around the player.
         */

        for (int chunkX =
             playerChunkX - renderDistance;
             chunkX <=
                 playerChunkX + renderDistance;
             chunkX++) {

            for (int chunkZ =
                 playerChunkZ - renderDistance;
                 chunkZ <=
                     playerChunkZ + renderDistance;
                 chunkZ++) {

                requestChunkGeneration(
                    chunkX,
                    chunkZ
                );
            }
        }


        /*
         * Unload chunks outside render distance.
         */
        unloadDistantChunks(
            playerChunkX,
            playerChunkZ
        );
    }


    // ---------------------------------------------------------
    // Unload distant chunks
    // ---------------------------------------------------------

    private void unloadDistantChunks(
        int playerChunkX,
        int playerChunkZ
    ) {

        List<String> chunksToUnload =
            new ArrayList<>();


        for (Map.Entry<String, Chunk> entry :
            chunks.entrySet()) {

            String key = entry.getKey();

            Chunk chunk = entry.getValue();

            int chunkX =
                chunk.getChunkX();

            int chunkZ =
                chunk.getChunkZ();


            if (Math.abs(chunkX - playerChunkX)
                > renderDistance ||
                Math.abs(chunkZ - playerChunkZ)
                    > renderDistance) {

                chunksToUnload.add(key);
            }
        }


        for (String key : chunksToUnload) {

            Chunk chunk =
                chunks.get(key);

            if (chunk == null) {
                continue;
            }

            int chunkX =
                chunk.getChunkX();

            int chunkZ =
                chunk.getChunkZ();


            /*
             * Don't unload the same chunk while a save is
             * already happening.
             */
            if (!savingChunks.add(key)) {
                continue;
            }


            /*
             * Take a snapshot of the chunk and its items on the
             * main thread before giving the data to the background
             * thread.
             */

            ChunkSaveData saveData =
                createSaveData(
                    chunkX,
                    chunkZ
                );


            /*
             * Remove from rendering immediately.
             */
            ChunkMesh mesh =
                chunkMeshes.remove(key);

            if (mesh != null) {
                mesh.dispose();
            }


            /*
             * Remove the chunk from active memory.
             */
            chunks.remove(key);

            // Invalidate any mesh that may still be generating for
            // this chunk. A completed stale result will be discarded.
            meshVersions.put(
                key,
                meshVersions.getOrDefault(key, 0) + 1
            );

            treesGenerated.remove(key);


            /*
             * Remove its dropped items from the active list.
             */
            for (int i =
                 droppedItems.size() - 1;
                 i >= 0;
                 i--) {

                DroppedItem item =
                    droppedItems.get(i);

                int itemChunkX =
                    Math.floorDiv(
                        (int)Math.floor(item.getX()),
                        Chunk.WIDTH
                    );

                int itemChunkZ =
                    Math.floorDiv(
                        (int)Math.floor(item.getZ()),
                        Chunk.DEPTH
                    );

                if (itemChunkX == chunkX &&
                    itemChunkZ == chunkZ) {

                    droppedItems.remove(i);
                }
            }


            /*
             * Save in the background.
             */
            chunkExecutor.submit(() -> {

                try {

                    saveChunkToFile(
                        saveData
                    );

                } catch (Exception e) {

                    Gdx.app.error(
                        "World",
                        "Failed to save chunk " + key,
                        e
                    );

                } finally {

                    savingChunks.remove(key);
                }
            });
        }
    }


    // ---------------------------------------------------------
    // Chunk save data
    // ---------------------------------------------------------

    private static class ChunkSaveData {

        int chunkX;
        int chunkZ;

        byte[] blocks;

        List<DroppedItemSaveData> items =
            new ArrayList<>();
    }


    private static class DroppedItemSaveData {

        Item item;
        int amount;

        float x;
        float y;
        float z;

        float velocityX;
        float velocityY;
        float velocityZ;

        float pickupCooldown;
        float animationTime;
        float throwTime;
    }


    // ---------------------------------------------------------
    // Create save snapshot
    // ---------------------------------------------------------

    private ChunkSaveData createSaveData(
        int chunkX,
        int chunkZ
    ) {

        Chunk chunk =
            chunks.get(
                getChunkKey(chunkX, chunkZ)
            );

        if (chunk == null) {
            return null;
        }


        ChunkSaveData data =
            new ChunkSaveData();

        data.chunkX = chunkX;
        data.chunkZ = chunkZ;


        /*
         * Flatten the 16x64x16 block array into one byte array.
         *
         * 16 * 64 * 16 = 16,384 bytes.
         */

        data.blocks =
            new byte[
                Chunk.WIDTH *
                    Chunk.HEIGHT *
                    Chunk.DEPTH
                ];


        int index = 0;

        for (int x = 0;
             x < Chunk.WIDTH;
             x++) {

            for (int y = 0;
                 y < Chunk.HEIGHT;
                 y++) {

                for (int z = 0;
                     z < Chunk.DEPTH;
                     z++) {

                    data.blocks[index++] =
                        chunk.getBlock(
                            x,
                            y,
                            z
                        );
                }
            }
        }


        /*
         * Save dropped items belonging to this chunk.
         */

        for (DroppedItem item :
            droppedItems) {

            int itemChunkX =
                Math.floorDiv(
                    (int)Math.floor(item.getX()),
                    Chunk.WIDTH
                );

            int itemChunkZ =
                Math.floorDiv(
                    (int)Math.floor(item.getZ()),
                    Chunk.DEPTH
                );


            if (itemChunkX != chunkX ||
                itemChunkZ != chunkZ) {

                continue;
            }


            DroppedItemSaveData itemData =
                new DroppedItemSaveData();

            itemData.item =
                item.getStack().getItem();

            itemData.amount =
                item.getStack().getAmount();

            itemData.x = item.getX();
            itemData.y = item.getY();
            itemData.z = item.getZ();

            itemData.velocityX =
                item.getVelocityX();

            itemData.velocityY =
                item.getVelocityY();

            itemData.velocityZ =
                item.getVelocityZ();

            itemData.pickupCooldown =
                item.getPickupCooldown();

            itemData.animationTime =
                item.getAnimationTime();

            itemData.throwTime =
                item.getThrowTime();


            data.items.add(itemData);
        }


        return data;
    }


    // ---------------------------------------------------------
    // Save chunk to disk
    // ---------------------------------------------------------

    private void saveChunkToFile(
        ChunkSaveData data
    ) throws IOException {

        if (data == null) {
            return;
        }


        FileHandle file =
            getChunkSaveFile(
                data.chunkX,
                data.chunkZ
            );


        try (
            DataOutputStream out =
                new DataOutputStream(
                    new BufferedOutputStream(
                        file.write(false)
                    )
                )
        ) {

            out.writeInt(SAVE_VERSION);

            out.writeInt(data.chunkX);
            out.writeInt(data.chunkZ);


            // Block data
            out.writeInt(
                data.blocks.length
            );

            out.write(
                data.blocks
            );


            // Dropped items
            out.writeInt(
                data.items.size()
            );


            for (DroppedItemSaveData item :
                data.items) {

                out.writeUTF(
                    item.item.getId()
                );

                out.writeInt(
                    item.amount
                );

                out.writeFloat(item.x);
                out.writeFloat(item.y);
                out.writeFloat(item.z);

                out.writeFloat(
                    item.velocityX
                );

                out.writeFloat(
                    item.velocityY
                );

                out.writeFloat(
                    item.velocityZ
                );

                out.writeFloat(
                    item.pickupCooldown
                );

                out.writeFloat(
                    item.animationTime
                );

                out.writeFloat(
                    item.throwTime
                );
            }
        }
    }


    // ---------------------------------------------------------
    // Load chunk from disk
    // ---------------------------------------------------------

    private Chunk loadChunkFromFile(
        FileHandle file
    ) throws IOException {

        try (
            DataInputStream in =
                new DataInputStream(
                    new BufferedInputStream(
                        file.read()
                    )
                )
        ) {

            int version =
                in.readInt();

            if (version != SAVE_VERSION) {

                throw new IOException(
                    "Unsupported chunk save version: "
                        + version
                );
            }


            int chunkX =
                in.readInt();

            int chunkZ =
                in.readInt();


            int blockCount =
                in.readInt();


            int expectedBlockCount =
                Chunk.WIDTH *
                    Chunk.HEIGHT *
                    Chunk.DEPTH;


            if (blockCount !=
                expectedBlockCount) {

                throw new IOException(
                    "Invalid block count: "
                        + blockCount
                );
            }


            Chunk chunk =
                new Chunk(
                    chunkX,
                    chunkZ
                );


            for (int x = 0;
                 x < Chunk.WIDTH;
                 x++) {

                for (int y = 0;
                     y < Chunk.HEIGHT;
                     y++) {

                    for (int z = 0;
                         z < Chunk.DEPTH;
                         z++) {

                        byte block =
                            in.readByte();

                        chunk.setBlock(
                            x,
                            y,
                            z,
                            block
                        );
                    }
                }
            }


            return chunk;
        }
    }


    // ---------------------------------------------------------
    // Load dropped items from save
    // ---------------------------------------------------------

    private void loadDroppedItemsFromChunk(
        FileHandle file
    ) throws IOException {

        /*
         * This method is only used for initial loading.
         *
         * The actual block loading and item loading are kept
         * separate so the chunk itself stays lightweight.
         */

        try (
            DataInputStream in =
                new DataInputStream(
                    new BufferedInputStream(
                        file.read()
                    )
                )
        ) {

            int version =
                in.readInt();

            if (version != SAVE_VERSION) {
                throw new IOException(
                    "Unsupported chunk save version"
                );
            }

            // Chunk coordinates
            in.readInt();
            in.readInt();

            int blockCount =
                in.readInt();

            for (int i = 0;
                 i < blockCount;
                 i++) {

                in.readByte();
            }


            int itemCount =
                in.readInt();


            for (int i = 0;
                 i < itemCount;
                 i++) {

                String itemId =
                    in.readUTF();

                int amount =
                    in.readInt();

                float x =
                    in.readFloat();

                float y =
                    in.readFloat();

                float z =
                    in.readFloat();

                float velocityX =
                    in.readFloat();

                float velocityY =
                    in.readFloat();

                float velocityZ =
                    in.readFloat();

                float pickupCooldown =
                    in.readFloat();

                float animationTime =
                    in.readFloat();

                float throwTime =
                    in.readFloat();


                Item item =
                    Item.fromId(itemId);

                if (item == null) {
                    continue;
                }


                droppedItems.add(
                    new DroppedItem(
                        item,
                        amount,
                        x,
                        y,
                        z,
                        velocityX,
                        velocityZ,
                        velocityY,
                        pickupCooldown,
                        animationTime,
                        throwTime
                    )
                );
            }
        }
    }


    // ---------------------------------------------------------
    // Check if chunk is loaded
    // ---------------------------------------------------------

    public boolean isChunkLoaded(
        int chunkX,
        int chunkZ
    ) {

        return chunks.containsKey(
            getChunkKey(chunkX, chunkZ)
        );
    }


    // ---------------------------------------------------------
    // Check world position
    // ---------------------------------------------------------

    public boolean isChunkLoadedAt(
        int worldX,
        int worldZ
    ) {

        int chunkX =
            Math.floorDiv(
                worldX,
                Chunk.WIDTH
            );

        int chunkZ =
            Math.floorDiv(
                worldZ,
                Chunk.DEPTH
            );

        return isChunkLoaded(
            chunkX,
            chunkZ
        );
    }


    // ---------------------------------------------------------
    // Generate mesh
    // ---------------------------------------------------------

    // Used for the initial world construction only. Runtime mesh
    // generation uses requestMeshGeneration() below.
    private void generateMesh(
        int chunkX,
        int chunkZ
    ) {

        String key =
            getChunkKey(chunkX, chunkZ);

        Chunk chunk =
            chunks.get(key);

        if (chunk == null) {
            return;
        }

        ChunkMesh oldMesh =
            chunkMeshes.get(key);

        if (oldMesh != null) {
            oldMesh.dispose();
        }

        ChunkMesh mesh =
            new ChunkMesh();

        mesh.generate(chunk);

        chunkMeshes.put(
            key,
            mesh
        );
    }


    // ---------------------------------------------------------
    // Background mesh generation
    // ---------------------------------------------------------

    private void requestMeshGeneration(
        int chunkX,
        int chunkZ
    ) {

        String key =
            getChunkKey(chunkX, chunkZ);

        Chunk chunk =
            chunks.get(key);

        if (chunk == null) {
            return;
        }

        if (!meshesGenerating.add(key)) {
            // There is already a mesh build running. Its version
            // will be checked when it finishes; if it is stale,
            // it will be requested again.
            return;
        }

        int version =
            meshVersions.getOrDefault(key, 0) + 1;

        meshVersions.put(key, version);

        // Snapshot the blocks on the main thread.
        byte[][][] blockSnapshot =
            new byte[
                Chunk.WIDTH
            ][
                Chunk.HEIGHT
            ][
                Chunk.DEPTH
            ];

        for (int x = 0; x < Chunk.WIDTH; x++) {
            for (int y = 0; y < Chunk.HEIGHT; y++) {
                for (int z = 0; z < Chunk.DEPTH; z++) {
                    blockSnapshot[x][y][z] =
                        chunk.getBlock(x, y, z);
                }
            }
        }

        final int finalChunkX = chunkX;
        final int finalChunkZ = chunkZ;
        final int finalVersion = version;

        meshExecutor.submit(() -> {
            ChunkMesh.MeshData data;

            try {
                // Recreate an isolated Chunk from the snapshot.
                // ChunkMesh.generateData() only performs CPU work.
                Chunk snapshot =
                    new Chunk(
                        finalChunkX,
                        finalChunkZ
                    );

                for (int x = 0; x < Chunk.WIDTH; x++) {
                    for (int y = 0; y < Chunk.HEIGHT; y++) {
                        for (int z = 0; z < Chunk.DEPTH; z++) {
                            snapshot.setBlock(
                                x,
                                y,
                                z,
                                blockSnapshot[x][y][z]
                            );
                        }
                    }
                }

                data =
                    ChunkMesh.generateData(snapshot);

            } catch (Exception e) {
                Gdx.app.error(
                    "World",
                    "Failed to generate mesh " + key,
                    e
                );
                return;

            } finally {
                // Remove the flag before publishing the result.
                // This guarantees a stale result can safely schedule
                // a fresh mesh build if the chunk changed meanwhile.
                meshesGenerating.remove(key);
            }

            completedMeshOperations.add(
                new MeshGenerationResult(
                    key,
                    finalVersion,
                    data
                )
            );
        });
    }


    private void processCompletedMeshes() {

        MeshGenerationResult result;

        while ((result =
            completedMeshOperations.poll()) != null) {

            Chunk chunk =
                chunks.get(result.key);

            if (chunk == null) {
                continue;
            }

            int currentVersion =
                meshVersions.getOrDefault(
                    result.key,
                    -1
                );

            if (result.version != currentVersion) {
                // Something changed while this mesh was being built.
                // Build a fresh version instead.
                requestMeshGeneration(
                    chunk.getChunkX(),
                    chunk.getChunkZ()
                );
                continue;
            }

            ChunkMesh oldMesh =
                chunkMeshes.get(result.key);

            if (oldMesh != null) {
                oldMesh.dispose();
            }

            ChunkMesh mesh =
                new ChunkMesh();

            // This is the only place runtime meshes are turned into
            // LibGDX/OpenGL objects, so it stays on the render thread.
            mesh.applyData(result.data);

            chunkMeshes.put(
                result.key,
                mesh
            );
        }
    }


    // ---------------------------------------------------------
    // Regenerate neighboring meshes
    // ---------------------------------------------------------

    private void regenerateMeshesAround(
        int chunkX,
        int chunkZ
    ) {

        for (int x =
             chunkX - 1;
             x <=
                 chunkX + 1;
             x++) {

            for (int z =
                 chunkZ - 1;
                 z <=
                     chunkZ + 1;
                 z++) {

                if (chunks.containsKey(
                    getChunkKey(x, z)
                )) {

                    requestMeshGeneration(
                        x,
                        z
                    );
                }
            }
        }
    }


    // ---------------------------------------------------------
    // Tree generation
    // ---------------------------------------------------------

    private void tryGenerateTreesAround(
        int chunkX,
        int chunkZ
    ) {
        for (int centerX = chunkX - 1; centerX <= chunkX + 1; centerX++) {
            for (int centerZ = chunkZ - 1; centerZ <= chunkZ + 1; centerZ++) {

                String key = getChunkKey(centerX, centerZ);

                if (treesGenerated.contains(key)) {
                    continue;
                }

                if (treesGenerating.contains(key)) {
                    continue;
                }

                // Make sure all surrounding chunks are loaded
                boolean allLoaded = true;

                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {

                        String neighbourKey =
                            getChunkKey(centerX + dx, centerZ + dz);

                        if (!chunks.containsKey(neighbourKey)) {
                            allLoaded = false;
                            break;
                        }
                    }

                    if (!allLoaded) {
                        break;
                    }
                }

                if (!allLoaded) {
                    continue;
                }

                treesGenerating.add(key);

                Chunk chunk = chunks.get(key);

                // Snapshot the chunk
                byte[][][] blockSnapshot =
                    new byte[Chunk.WIDTH][Chunk.HEIGHT][Chunk.DEPTH];

                int[][] surfaceSnapshot =
                    new int[Chunk.WIDTH][Chunk.DEPTH];

                for (int x = 0; x < Chunk.WIDTH; x++) {
                    for (int y = 0; y < Chunk.HEIGHT; y++) {
                        for (int z = 0; z < Chunk.DEPTH; z++) {
                            blockSnapshot[x][y][z] =
                                chunk.getBlock(x, y, z);
                        }
                    }
                }

                for (int x = 0; x < Chunk.WIDTH; x++) {
                    for (int z = 0; z < Chunk.DEPTH; z++) {
                        surfaceSnapshot[x][z] =
                            chunk.getSurfaceHeight(x, z);
                    }
                }

                final int finalChunkX = centerX;
                final int finalChunkZ = centerZ;

                chunkExecutor.submit(() -> {
                    try {
                        List<BlockChange> changes =
                            generateTreesInChunkBackground(
                                finalChunkX,
                                finalChunkZ,
                                blockSnapshot,
                                surfaceSnapshot
                            );

                        completedTreeOperations.add(
                            new TreeGenerationResult(
                                getChunkKey(
                                    finalChunkX,
                                    finalChunkZ
                                ),
                                changes
                            )
                        );

                    } finally {
                        treesGenerating.remove(
                            getChunkKey(
                                finalChunkX,
                                finalChunkZ
                            )
                        );
                    }
                });
            }
        }
    }

    private void processCompletedTrees() {

        TreeGenerationResult result;

        while ((result =
            completedTreeOperations.poll()) != null) {

            if (treesGenerated.contains(
                result.key
            )) {
                continue;
            }

            String[] parts =
                result.key.split(",");

            int chunkX =
                Integer.parseInt(parts[0]);

            int chunkZ =
                Integer.parseInt(parts[1]);

            /*
             * The chunk may have been unloaded while
             * the background task was running.
             */
            if (!chunks.containsKey(result.key)) {
                continue;
            }

            /*
             * Apply all calculated block changes on
             * the main thread.
             */
            for (BlockChange change :
                result.changes) {

                /*
                 * Don't overwrite something that the
                 * player may have changed while the
                 * background task was running.
                 */
                if (getBlock(
                    change.x,
                    change.y,
                    change.z
                ) == Block.AIR) {

                    setBlock(
                        change.x,
                        change.y,
                        change.z,
                        change.block,
                        false
                    );
                }
            }

            treesGenerated.add(
                result.key
            );

            /*
             * Rebuild the center and surrounding
             * chunk meshes because leaves can cross
             * chunk boundaries.
             */
            regenerateMeshesAround(
                chunkX,
                chunkZ
            );
        }
    }

    private List<BlockChange>
    generateTreesInChunkBackground(
        int chunkX,
        int chunkZ,
        byte[][][] blocks,
        int[][] surfaceHeights
    ) {

        List<BlockChange> changes =
            new ArrayList<>();

        int startX =
            chunkX * Chunk.WIDTH;

        int startZ =
            chunkZ * Chunk.DEPTH;

        for (int x = 0;
             x < Chunk.WIDTH;
             x++) {

            for (int z = 0;
                 z < Chunk.DEPTH;
                 z++) {

                int worldX =
                    startX + x;

                int worldZ =
                    startZ + z;

                int surfaceY =
                    surfaceHeights[x][z];

                if (surfaceY < 0) {
                    continue;
                }

                /*
                 * Only grow trees from grass.
                 */
                if (blocks[x][surfaceY][z] !=
                    Block.GRASS) {
                    continue;
                }

                /*
                 * 2% tree chance.
                 */
                if (Math.random() >= 0.02f) {
                    continue;
                }

                int treeHeight = 4;

                if (surfaceY + 6 >=
                    Chunk.HEIGHT) {
                    continue;
                }

                /*
                 * Trunk
                 */
                for (int y = 1;
                     y <= treeHeight;
                     y++) {

                    changes.add(
                        new BlockChange(
                            worldX,
                            surfaceY + y,
                            worldZ,
                            Block.WOOD
                        )
                    );
                }

                /*
                 * Leaves
                 */
                for (int dx = -2;
                     dx <= 2;
                     dx++) {

                    for (int dz = -2;
                         dz <= 2;
                         dz++) {

                        for (int dy = 2;
                             dy <= 4;
                             dy++) {

                            if (
                                Math.abs(dx) == 2 &&
                                    Math.abs(dz) == 2
                            ) {
                                continue;
                            }

                            if (
                                dx == 0 &&
                                    dz == 0
                            ) {
                                continue;
                            }

                            changes.add(
                                new BlockChange(
                                    worldX + dx,
                                    surfaceY +
                                        treeHeight -
                                        1 +
                                        dy -
                                        2,
                                    worldZ + dz,
                                    Block.LEAVES
                                )
                            );
                        }
                    }
                }

                /*
                 * Top leaf
                 */
                changes.add(
                    new BlockChange(
                        worldX,
                        surfaceY +
                            treeHeight +
                            1,
                        worldZ,
                        Block.LEAVES
                    )
                );
            }
        }

        return changes;
    }

    // ---------------------------------------------------------
    // Get block
    // ---------------------------------------------------------

    public byte getBlock(
        int worldX,
        int worldY,
        int worldZ
    ) {

        if (worldY < 0 ||
            worldY >= Chunk.HEIGHT) {

            return Block.AIR;
        }


        int chunkX =
            Math.floorDiv(
                worldX,
                Chunk.WIDTH
            );

        int chunkZ =
            Math.floorDiv(
                worldZ,
                Chunk.DEPTH
            );


        int localX =
            Math.floorMod(
                worldX,
                Chunk.WIDTH
            );

        int localZ =
            Math.floorMod(
                worldZ,
                Chunk.DEPTH
            );


        Chunk chunk =
            chunks.get(
                getChunkKey(
                    chunkX,
                    chunkZ
                )
            );


        if (chunk == null) {
            return Block.AIR;
        }


        return chunk.getBlock(
            localX,
            worldY,
            localZ
        );
    }


    // ---------------------------------------------------------
    // Set block
    // ---------------------------------------------------------

    public void setBlock(
        int worldX,
        int worldY,
        int worldZ,
        byte block
    ) {

        setBlock(
            worldX,
            worldY,
            worldZ,
            block,
            true
        );
    }


    private void setBlock(
        int worldX,
        int worldY,
        int worldZ,
        byte block,
        boolean updateMesh
    ) {

        if (worldY < 0 ||
            worldY >= Chunk.HEIGHT) {

            return;
        }


        int chunkX =
            Math.floorDiv(
                worldX,
                Chunk.WIDTH
            );

        int chunkZ =
            Math.floorDiv(
                worldZ,
                Chunk.DEPTH
            );


        int localX =
            Math.floorMod(
                worldX,
                Chunk.WIDTH
            );

        int localZ =
            Math.floorMod(
                worldZ,
                Chunk.DEPTH
            );


        String key =
            getChunkKey(
                chunkX,
                chunkZ
            );


        Chunk chunk =
            chunks.get(key);

        if (chunk == null) {
            return;
        }


        byte oldBlock =
            chunk.getBlock(
                localX,
                worldY,
                localZ
            );


        chunk.setBlock(
            localX,
            worldY,
            localZ,
            block
        );


        // -----------------------------------------------------
        // Dirt timer
        // -----------------------------------------------------

        String blockKey =
            worldX + "," +
                worldY + "," +
                worldZ;


        if (oldBlock == Block.DIRT &&
            block == Block.AIR) {

            int aboveY =
                worldY + 1;


            if (aboveY <
                Chunk.HEIGHT &&
                getBlock(
                    worldX,
                    aboveY,
                    worldZ
                ) == Block.AIR) {

                dirtTimers.put(
                    blockKey,
                    0f
                );
            }
        }


        if (block != Block.DIRT) {
            dirtTimers.remove(blockKey);
        }


        if (!updateMesh) {
            return;
        }


        requestMeshGeneration(
            chunkX,
            chunkZ
        );


        if (localX == 0) {

            if (chunks.containsKey(
                getChunkKey(
                    chunkX - 1,
                    chunkZ
                )
            )) {

                requestMeshGeneration(
                    chunkX - 1,
                    chunkZ
                );
            }
        }


        if (localX ==
            Chunk.WIDTH - 1) {

            if (chunks.containsKey(
                getChunkKey(
                    chunkX + 1,
                    chunkZ
                )
            )) {

                requestMeshGeneration(
                    chunkX + 1,
                    chunkZ
                );
            }
        }


        if (localZ == 0) {

            if (chunks.containsKey(
                getChunkKey(
                    chunkX,
                    chunkZ - 1
                )
            )) {

                requestMeshGeneration(
                    chunkX,
                    chunkZ - 1
                );
            }
        }


        if (localZ ==
            Chunk.DEPTH - 1) {

            if (chunks.containsKey(
                getChunkKey(
                    chunkX,
                    chunkZ + 1
                )
            )) {

                requestMeshGeneration(
                    chunkX,
                    chunkZ + 1
                );
            }
        }
    }


    // ---------------------------------------------------------
    // World update
    // ---------------------------------------------------------

    public void update(
        float delta
    ) {

        processCompletedChunks();


        // -----------------------------------------------------
        // Dirt -> grass
        // -----------------------------------------------------

        List<String> finishedTimers =
            new ArrayList<>();


        for (Map.Entry<String, Float> entry :
            dirtTimers.entrySet()) {

            String key =
                entry.getKey();

            float timer =
                entry.getValue() + delta;


            String[] parts =
                key.split(",");


            int x =
                Integer.parseInt(parts[0]);

            int y =
                Integer.parseInt(parts[1]);

            int z =
                Integer.parseInt(parts[2]);


            if (
                getBlock(x, y, z)
                    != Block.DIRT ||

                    getBlock(x, y + 1, z)
                        != Block.AIR
            ) {

                finishedTimers.add(key);

                continue;
            }


            if (timer >= 10f) {

                setBlock(
                    x,
                    y,
                    z,
                    Block.GRASS
                );

                finishedTimers.add(key);

            } else {

                entry.setValue(timer);
            }
        }


        for (String key :
            finishedTimers) {

            dirtTimers.remove(key);
        }


        updateGrassBlocks();
    }


    // ---------------------------------------------------------
    // Grass blocks
    // ---------------------------------------------------------

    public void updateGrassBlocks() {

        for (Chunk chunk :
            chunks.values()) {

            int chunkX =
                chunk.getChunkX();

            int chunkZ =
                chunk.getChunkZ();


            for (int x = 0;
                 x < Chunk.WIDTH;
                 x++) {

                for (int z = 0;
                     z < Chunk.DEPTH;
                     z++) {

                    int surfaceY =
                        chunk.getSurfaceHeight(
                            x,
                            z
                        );


                    if (surfaceY < 0) {
                        continue;
                    }


                    int worldX =
                        chunkX *
                            Chunk.WIDTH +
                            x;

                    int worldZ =
                        chunkZ *
                            Chunk.DEPTH +
                            z;


                    if (getBlock(
                        worldX,
                        surfaceY,
                        worldZ
                    ) != Block.GRASS) {

                        continue;
                    }


                    if (surfaceY + 1 >=
                        Chunk.HEIGHT) {

                        continue;
                    }


                    if (getBlock(
                        worldX,
                        surfaceY + 1,
                        worldZ
                    ) != Block.AIR) {

                        setBlock(
                            worldX,
                            surfaceY,
                            worldZ,
                            Block.DIRT
                        );
                    }
                }
            }
        }
    }


    // ---------------------------------------------------------
    // Dropped items
    // ---------------------------------------------------------

    public List<DroppedItem> getDroppedItems() {

        return droppedItems;
    }


    public void spawnDroppedItem(
        Item item,
        int amount,
        float x,
        float y,
        float z
    ) {

        droppedItems.add(
            new DroppedItem(
                item,
                amount,
                x,
                y,
                z,
                0,
                0
            )
        );
    }


    public void spawnDroppedItem(
        Item item,
        int amount,
        float x,
        float y,
        float z,
        float velocityX,
        float velocityZ
    ) {

        droppedItems.add(
            new DroppedItem(
                item,
                amount,
                x,
                y,
                z,
                velocityX,
                velocityZ
            )
        );
    }


    public void updateDroppedItems(
        Player player,
        float delta
    ) {

        float pickupDistance = 1.5f;

        float pickupDistanceSquared =
            pickupDistance *
                pickupDistance;


        for (int i =
             droppedItems.size() - 1;
             i >= 0;
             i--) {

            DroppedItem item =
                droppedItems.get(i);


            float dx =
                player.getX() -
                    item.getX();

            float dy =
                player.getY() -
                    item.getY();

            float dz =
                player.getZ() -
                    item.getZ();


            float distanceSquared =
                dx * dx +
                    dy * dy +
                    dz * dz;


            if (
                distanceSquared <=
                    pickupDistanceSquared &&

                    item.canBePickedUp()
            ) {

                boolean pickedUp =
                    player.getInventory()
                        .addItem(
                            item.getStack()
                                .getItem(),
                            item.getStack()
                                .getAmount()
                        );


                if (pickedUp) {

                    droppedItems.remove(i);

                    continue;
                }
            }


            item.update(
                delta,
                this
            );
        }
    }


    // ---------------------------------------------------------
    // Render
    // ---------------------------------------------------------

    public void render(
        com.badlogic.gdx.graphics.g3d.ModelBatch modelBatch,
        com.badlogic.gdx.graphics.Camera camera,
        com.badlogic.gdx.graphics.g3d.Environment environment
    ) {

        for (ChunkMesh mesh :
            chunkMeshes.values()) {

            if (
                mesh != null &&
                    mesh.instance != null
            ) {

                modelBatch.render(
                    mesh.instance,
                    environment
                );
            }
        }
    }


    // ---------------------------------------------------------
    // Dispose
    // ---------------------------------------------------------

    public void dispose() {

        /*
         * Save every currently loaded/discovered chunk before
         * shutting down.
         */

        for (Chunk chunk :
            new ArrayList<>(chunks.values())) {

            int chunkX =
                chunk.getChunkX();

            int chunkZ =
                chunk.getChunkZ();

            ChunkSaveData data =
                createSaveData(
                    chunkX,
                    chunkZ
                );

            try {

                saveChunkToFile(data);

            } catch (Exception e) {

                Gdx.app.error(
                    "World",
                    "Failed to save chunk before exit",
                    e
                );
            }
        }


        chunkExecutor.shutdownNow();
        meshExecutor.shutdownNow();


        for (ChunkMesh mesh :
            chunkMeshes.values()) {

            if (mesh != null) {
                mesh.dispose();
            }
        }


        chunkMeshes.clear();
        chunks.clear();
        dirtTimers.clear();
        droppedItems.clear();

        completedChunkOperations.clear();
        completedMeshOperations.clear();
        generatingChunks.clear();
        loadingChunks.clear();
        savingChunks.clear();
        meshesGenerating.clear();
        meshVersions.clear();

        treesGenerated.clear();
        discoveredChunks.clear();
    }
}

