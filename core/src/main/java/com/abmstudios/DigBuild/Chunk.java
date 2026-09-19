package com.abmstudios.DigBuild;

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

                        if (y < height - 3 && y >= height -4) {
                            setBlock(x,y,z, Block.COAL_ORE);
                        }
                    }


                }
            }
        }
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
