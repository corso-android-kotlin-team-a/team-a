package com.ivanmorgillo.corsoandroid.teama.network

import retrofit2.http.GET
import retrofit2.http.Query

interface RecipeService {
    @GET("filter.php?c=Beef")
    suspend fun loadRecipes(): RecipeDTO

    @GET("lookup.php")
    suspend fun loadRecipeDetails(@Query("i") idMeal: Long): RecipeDetailsDTO
}
