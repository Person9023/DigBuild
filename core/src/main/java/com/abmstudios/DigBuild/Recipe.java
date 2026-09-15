package com.abmstudios.DigBuild;

public class Recipe {

    private final Item[][] ingredients;

    private final Item output;
    private final int outputAmount;

    private final int width;
    private final int height;

    private final boolean shapeless;


    public Recipe(
        int width,
        int height,
        Item[][] ingredients,
        Item output,
        int outputAmount
    ) {
        this(
            width,
            height,
            ingredients,
            output,
            outputAmount,
            false
        );
    }


    public Recipe(
        int width,
        int height,
        Item[][] ingredients,
        Item output,
        int outputAmount,
        boolean shapeless
    ) {
        this.width = width;
        this.height = height;

        this.ingredients = new Item[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {

                if (y < ingredients.length &&
                    x < ingredients[y].length) {

                    this.ingredients[y][x] =
                        ingredients[y][x];
                }
            }
        }

        this.output = output;
        this.outputAmount = outputAmount;
        this.shapeless = shapeless;
    }


    public int getWidth() {
        return width;
    }


    public int getHeight() {
        return height;
    }


    public Item getIngredient(int x, int y) {

        if (x < 0 || x >= width ||
            y < 0 || y >= height) {

            return null;
        }

        return ingredients[y][x];
    }


    public Item getOutput() {
        return output;
    }


    public int getOutputAmount() {
        return outputAmount;
    }


    public boolean isShapeless() {
        return shapeless;
    }
}
