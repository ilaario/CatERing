package catering.businesslogic.recipe;

/**
 * KitchenProcess represents any food preparation activity in the kitchen.
 * This serves as a common interface for both Recipes and Preparations.
 */
public interface KitchenProcess {

    int getId();

    void setId(int id);

    String getName();

    boolean isRecipe();

    String getDescription();
}
