package com.abmstudios.DigBuild;

import java.util.Random;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

public class Chunk {

    public static final int WIDTH = 16;
    public static final int HEIGHT = 64;
    public static final int DEPTH = 16;

    private byte[][][] blocks =
        new byte[WIDTH][HEIGHT][DEPTH];

    private int[][] surfaceHeights =
        new int[WIDTH][DEPTH];

    private static final PerlinNoise noise =
        new PerlinNoise(12345);

    private int chunkX;
    private int chunkZ;

    public Chunk(int chunkX, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }


    public void renderCoalDebugLines(ShapeRenderer shapeRenderer) {

        Set<String> visited = new HashSet<>();

        int[][] directions = {
            { 1, 0, 0 },
            {-1, 0, 0 },
            { 0, 1, 0 },
            { 0,-1, 0 },
            { 0, 0, 1 },
            { 0, 0,-1 }
        };

        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                for (int z = 0; z < DEPTH; z++) {

                    if (blocks[x][y][z] != Block.COAL_ORE) {
                        continue;
                    }

                    String startKey = x + "," + y + "," + z;

                    if (visited.contains(startKey)) {
                        continue;
                    }

                    // Start a new connected vein
                    ArrayDeque<int[]> queue = new ArrayDeque<>();
                    queue.add(new int[] {x, y, z});
                    visited.add(startKey);

                    float totalX = 0;
                    float totalY = 0;
                    float totalZ = 0;
                    int count = 0;

                    while (!queue.isEmpty()) {

                        int[] pos = queue.removeFirst();

                        int bx = pos[0];
                        int by = pos[1];
                        int bz = pos[2];

                        totalX += bx;
                        totalY += by;
                        totalZ += bz;
                        count++;

                        for (int[] dir : directions) {

                            int nx = bx + dir[0];
                            int ny = by + dir[1];
                            int nz = bz + dir[2];

                            if (nx < 0 || nx >= WIDTH ||
                                ny < 0 || ny >= HEIGHT ||
                                nz < 0 || nz >= DEPTH) {
                                continue;
                            }

                            if (blocks[nx][ny][nz] != Block.COAL_ORE) {
                                continue;
                            }

                            String key = nx + "," + ny + "," + nz;

                            if (visited.add(key)) {
                                queue.addLast(new int[] {nx, ny, nz});
                            }
                        }
                    }

                    // Centre point of this vein
                    float worldX =
                        chunkX * WIDTH + totalX / count + 0.5f;

                    float worldY =
                        totalY / count + 0.5f;

                    float worldZ =
                        chunkZ * DEPTH + totalZ / count + 0.5f;

                    // Get the surface height directly above the vein
                    int localX = Math.max(
                        0,
                        Math.min(WIDTH - 1, (int)(totalX / count))
                    );

                    int localZ = Math.max(
                        0,
                        Math.min(DEPTH - 1, (int)(totalZ / count))
                    );

                    int surfaceY = getSurfaceHeight(localX, localZ);

                    // Draw only the section above the terrain
                    shapeRenderer.line(
                        worldX,
                        surfaceY + 1.05f,
                        worldZ,
                        worldX,
                        HEIGHT + 20f,
                        worldZ
                    );
                }
            }
        }
    }

    public byte getBlock(int x, int y, int z) {

        if (x < 0 || x >= WIDTH ||
            y < 0 || y >= HEIGHT ||
            z < 0 || z >= DEPTH) {

            return Block.AIR;
        }

        return blocks[x][y][z];
    }

    public void setBlock(int x, int y, int z, byte block) {

        if (x < 0 || x >= WIDTH ||
            y < 0 || y >= HEIGHT ||
            z < 0 || z >= DEPTH) {

            return;
        }

        blocks[x][y][z] = block;
    }




    private double caveNoise(int x, int y, int z) {

        double scale = 0.045;

        double noiseValue = noise.noise(
            x * scale,
            y * scale,
            z * scale
        );

        return Math.abs(noiseValue);
    }

    private void generateCoal() {

        Random random = new Random(
            10290138L
                + chunkX * 341873128712L
                + chunkZ * 132897987541L
        );

        // Number of veins generated in this chunk
        int veinCount = 8;

        for (int vein = 0; vein < veinCount; vein++) {

            // Pick a random starting position
            int x = random.nextInt(WIDTH);
            int z = random.nextInt(DEPTH);

            int surfaceHeight = surfaceHeights[x][z];

            // Keep the starting point underground
            if (surfaceHeight <= 6) {
                continue;
            }

            int y = 4 + random.nextInt(surfaceHeight - 4);

            // Each vein contains a handful of blocks
            int veinSize = 4 + random.nextInt(5);

            for (int i = 0; i < veinSize; i++) {

                // Only replace stone
                if (x >= 0 && x < WIDTH &&
                    y > 0 && y < HEIGHT &&
                    z >= 0 && z < DEPTH &&
                    blocks[x][y][z] == Block.STONE) {

                    blocks[x][y][z] = Block.COAL_ORE;
                }

                // Move to a nearby block to grow the vein
                int direction = random.nextInt(6);

                switch (direction) {
                    case 0:
                        x++;
                        break;
                    case 1:
                        x--;
                        break;
                    case 2:
                        y++;
                        break;
                    case 3:
                        y--;
                        break;
                    case 4:
                        z++;
                        break;
                    case 5:
                        z--;
                        break;
                }

                // Keep the vein inside the chunk
                x = Math.max(0, Math.min(WIDTH - 1, x));
                y = Math.max(1, Math.min(HEIGHT - 1, y));
                z = Math.max(0, Math.min(DEPTH - 1, z));
            }
        }
    }




    public void generate() {

        for (int x = 0; x < WIDTH; x++) {

            for (int z = 0; z < DEPTH; z++) {

                int worldX = chunkX * WIDTH + x;
                int worldZ = chunkZ * DEPTH + z;

                double noiseValue =
                    noise.octaveNoise(
                        worldX * 0.025,
                        worldZ * 0.025,
                        5,      // octaves
                        0.5,    // persistence
                        2.0     // lacunarity
                    );

                int height =
                    20 + (int)(noiseValue * 10);

                surfaceHeights[x][z] = height;

                for (int y = 0; y < HEIGHT; y++) {

                    // Above the surface
                    if (y > height) {

                        setBlock(x, y, z, Block.AIR);

                    }

                    // Surface
                    else if (y == height) {

                        setBlock(x, y, z, Block.GRASS);

                    }

                    // Dirt layer
                    else if (y >= height - 3) {

                        setBlock(x, y, z, Block.DIRT);

                    }

                    // Underground stone

                    else {

                        // Underground caves
                        if (y > 3 && y < height - 4) {

                            double cave = caveNoise(worldX, y, worldZ);

                            if (cave > 0.58) {
                                setBlock(x, y, z, Block.AIR);
                            } else {
                                setBlock(x, y, z, Block.STONE);
                            }

                        }




                        else {


                            setBlock(x, y, z, Block.STONE);
                        }


                    }


                }
            }
        }
        generateCoal();
    }



    public int getChunkX() {
        return chunkX;
    }

    public int getChunkZ() {
        return chunkZ;
    }

    public int getSurfaceHeight(int x, int z) {

        if (x < 0 || x >= WIDTH ||
            z < 0 || z >= DEPTH) {

            return -1;
        }

        return surfaceHeights[x][z];
    }




}
