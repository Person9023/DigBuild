package com.abmstudios.DigBuild;

public class Item {

    private final String id;
    private final String name;
    private final int textureIndex;


    // =============================================================
    // BLOCK ITEMS
    // =============================================================

    public static final Item GRASS =
        new Item("grass", "Grass Block", 1);

    public static final Item DIRT =
        new Item("dirt", "Dirt Block", 3);

    public static final Item STONE =
        new Item("stone", "Stone", 2);

    public static final Item WOOD =
        new Item("wood", "Oak Log", 4);

    public static final Item PLANKS =
        new Item("planks", "Oak Planks", 7);

    public static final Item LEAVES =
        new Item("leaves", "Oak Leaves", 6);

    public static final Item STICK =
        new Item("stick", "Stick", 8);

    public static final Item CRAFTING_TABLE =
        new Item("crafting_table", "Crafting Table", 8);


    // =============================================================
    // CONSTRUCTOR
    // =============================================================

    public Item(
        String id,
        String name,
        int textureIndex
    ) {
        this.id = id;
        this.name = name;
        this.textureIndex = textureIndex;
    }


    // =============================================================
    // GETTERS
    // =============================================================

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getTextureIndex() {
        return textureIndex;
    }


    // =============================================================
    // SAVE / LOAD
    // =============================================================

    public static Item fromId(String id) {

        if (GRASS.getId().equals(id)) return GRASS;
        if (DIRT.getId().equals(id)) return DIRT;
        if (STONE.getId().equals(id)) return STONE;
        if (WOOD.getId().equals(id)) return WOOD;
        if (LEAVES.getId().equals(id)) return LEAVES;
        if (PLANKS.getId().equals(id)) return PLANKS;
        if (STICK.getId().equals(id)) return STICK;
        if (CRAFTING_TABLE.getId().equals(id)) return CRAFTING_TABLE;


        return null;
    }
}

