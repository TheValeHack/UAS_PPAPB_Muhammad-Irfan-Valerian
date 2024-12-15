package com.example.valenote.api

import com.example.valenote.database.models.Note
import com.example.valenote.database.models.User
import com.example.valenote.database.models.UserRequest
import retrofit2.Call
import retrofit2.http.*

interface ApiService {
    @GET("user/{id}")
    fun getUser(@Path("id") id: String): Call<User>

    @POST("user")
    fun saveUser(@Body user: UserRequest): Call<User>

    @GET("user")
    fun getAllUsers(): Call<List<User>>

    @GET("notes")
    fun getAllNotes(): Call<List<Note>>

    @POST("notes")
    fun createNote(@Body note: Note): Call<Note>

    @POST("notes/{id}")
    fun updateNote(@Path("id") id: String, @Body note: Note): Call<Note>

    @DELETE("notes/{id}")
    fun deleteNote(@Path("id") id: String): Call<Void>
}