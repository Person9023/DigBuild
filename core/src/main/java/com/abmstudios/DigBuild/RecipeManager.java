package com.abmstudios.DigBuild;

import java.util.ArrayList;
import java.util.List;

public class RecipeManager {

    private static final List<Recipe> recipes =
        new ArrayList<>();

    static {

        // 1 Wood -> 6 Planks
        recipes.add(
            new Recipe(
                2,
                2,
                new Item[][] {
                    { Item.WOOD, null },
                    { null,      null }
                },
                Item.PLANKS,
                6,
                true
            )
        );

        // 2 Planks -> 4 Sticks
        recipes.add(
            new Recipe(
                2,
                1,
                new Item[][] {
                    { Item.PLANKS, Item.PLANKS }
                },
                Item.STICK,
                4,
                true
            )
        );

        // 4 Planks -> 1 Crafting Table
        recipes.add(
            new Recipe(
                2,
                2,
                new Item[][] {
                    { Item.PLANKS, Item.PLANKS },
                    { Item.PLANKS, Item.PLANKS }
                },
                Item.CRAFTING_TABLE,
                1
            )
        );
    }

    public static Recipe findRecipe(
        ItemStack[][] grid,
        int width,
        int height
    ) {
        for (Recipe recipe : recipes) {

            if (recipe.isShapeless()) {
                if (matchesShapeless(recipe, grid, width, height)) {
                    return recipe;
                }
            } else {
                if (recipe.getWidth() > width ||
                    recipe.getHeight() > height) {
                    continue;
                }

                if (matchesShaped(recipe, grid, width, height)) {
                    return recipe;
                }
            }
        }

        return null;
    }

    private static boolean matchesShaped(
        Recipe recipe,
        ItemStack[][] grid,
        int width,
        int height
    ) {
        int recipeWidth = recipe.getWidth();
        int recipeHeight = recipe.getHeight();

        // Try every possible position inside the crafting grid.
        for (int offsetY = 0;
             offsetY <= height - recipeHeight;
             offsetY++) {

            for (int offsetX = 0;
                 offsetX <= width - recipeWidth;
                 offsetX++) {

                boolean matches = true;

                for (int y = 0; y < height && matches; y++) {
                    for (int x = 0; x < width; x++) {

                        Item required = null;

                        int recipeX = x - offsetX;
                        int recipeY = y - offsetY;

                        if (recipeX >= 0 &&
                            recipeX < recipeWidth &&
                            recipeY >= 0 &&
                            recipeY < recipeHeight) {

                            required = recipe.getIngredient(recipeX, recipeY);
                        }

                        ItemStack stack = grid[y][x];

                        if (required == null) {
                            if (stack != null) {
                                matches = false;
                                break;
                            }
                        } else {
                            if (stack == null ||
                                stack.getItem() != required) {
                                matches = false;
                                break;
                            }
                        }
                    }
                }

                if (matches) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean matchesShapeless(
        Recipe recipe,
        ItemStack[][] grid,
        int width,
        int height
    ) {
        int requiredItems = 0;
        int suppliedItems = 0;

        for (int y = 0; y < recipe.getHeight(); y++) {
            for (int x = 0; x < recipe.getWidth(); x++) {
                if (recipe.getIngredient(x, y) != null) {
                    requiredItems++;
                }
            }
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (grid[y][x] != null) {
                    suppliedItems++;
                }
            }
        }

        if (requiredItems != suppliedItems) {
            return false;
        }

        boolean[][] matched =
            new boolean[height][width];

        for (int ry = 0; ry < recipe.getHeight(); ry++) {
            for (int rx = 0; rx < recipe.getWidth(); rx++) {

                Item required =
                    recipe.getIngredient(rx, ry);

                if (required == null) {
                    continue;
                }

                boolean found = false;

                for (int y = 0; y < height && !found; y++) {
                    for (int x = 0; x < width && !found; x++) {

                        if (matched[y][x]) {
                            continue;
                        }

                        ItemStack stack = grid[y][x];

                        if (stack == null) {
                            continue;
                        }

                        if (stack.getItem() == required) {
                            matched[y][x] = true;
                            found = true;
                        }
                    }
                }

                if (!found) {
                    return false;
                }
            }
        }

        return true;
    }
}
