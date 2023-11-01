package com.mobiledevproj.popcorn

data class Movies(
    val movies:List<Movie>,
    val total: Int
)

data class Movie (
    val movie_id: Int,
    val movie_title: String,
    val movie_description: String
    )
