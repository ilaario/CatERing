package catering.businesslogic.recipe;

import java.util.ArrayList;

public class RecipeManager {

    public ArrayList<Recipe> getRecipeBook() {
        return Recipe.getAllRecipes();
    }
}
