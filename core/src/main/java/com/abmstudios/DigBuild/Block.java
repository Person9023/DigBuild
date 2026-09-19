package com.abmstudios.DigBuild;


public class Block {

    public static final byte AIR = 0;
    public static final byte GRASS = 1;
    public static final byte DIRT = 2;
    public static final byte STONE = 3;
    public static final byte WOOD = 4;
    public static final byte LEAVES = 5;
    public static final byte PLANKS = 6;
    public static final byte CRAFTING_TABLE = 7;
    public static final byte COAL_ORE = 8;


    public static int getTopTexture(byte block) {

        switch (block) {

            case GRASS:
                return 0;

            case DIRT:
                return 3;

            case STONE:
                return 2;

            case WOOD:
                return 5;

            case LEAVES:
                return 6;

            case PLANKS:
                return 7;
            case CRAFTING_TABLE:
                return 9;
            case COAL_ORE:
                return 13;

            default:
                return -1;
        }
    }


    public static int getBottomTexture(byte block) {

        switch (block) {

            case GRASS:
                return 3;

            case DIRT:
                return 3;

            case STONE:
                return 2;

            case WOOD:
                return 5;

            case LEAVES:
                return 6;
            case PLANKS:
                return 7;
            case CRAFTING_TABLE:
                return 10;
            case COAL_ORE:
                return 13;

            default:
                return -1;
        }
    }


    public static int getFrontTexture(byte block) {

        switch (block) {

            case GRASS:
                return 1;

            case DIRT:
                return 3;

            case STONE:
                return 2;

            case WOOD:
                return 4;

            case LEAVES:
                return 6;
            case PLANKS:
                return 7;
            case CRAFTING_TABLE:
                return 10;
            case COAL_ORE:
                return 13;
            default:
                return -1;
        }
    }


    public static int getBackTexture(byte block) {

        switch (block) {

            case GRASS:
                return 1;

            case DIRT:
                return 3;

            case STONE:
                return 2;

            case WOOD:
                return 4;

            case LEAVES:
                return 6;
            case PLANKS:
                return 7;
            case CRAFTING_TABLE:
                return 10;
            case COAL_ORE:
                return 13;
            default:
                return -1;
        }
    }


    public static int getLeftTexture(byte block) {

        switch (block) {

            case GRASS:
                return 1;

            case DIRT:
                return 3;

            case STONE:
                return 2;

            case WOOD:
                return 4;

            case LEAVES:
                return 6;
            case PLANKS:
                return 7;
            case CRAFTING_TABLE:
                return 10;
            case COAL_ORE:
                return 13;
            default:
                return -1;
        }
    }


    public static int getRightTexture(byte block) {

        switch (block) {

            case GRASS:
                return 1;

            case DIRT:
                return 3;

            case STONE:
                return 2;

            case WOOD:
                return 4;

            case LEAVES:
                return 6;
            case PLANKS:
                return 7;
            case CRAFTING_TABLE:
                return 10;
            case COAL_ORE:
                return 13;
            default:
                return -1;
        }
    }


    public static Item getItem(byte block) {

        switch (block) {

            case GRASS:
                return Item.GRASS;

            case DIRT:
                return Item.DIRT;

            case STONE:
                return Item.STONE;

            case WOOD:
                return Item.WOOD;

            case LEAVES:
                return Item.LEAVES;
            case PLANKS:
                return Item.PLANKS;
            case CRAFTING_TABLE:
                return Item.CRAFTING_TABLE;
            case COAL_ORE:
                return Item.COAL_ORE;
            default:
                return null;
        }
    }


}
