package com.example.valenote.repository

import com.example.valenote.database.UserDao
import com.example.valenote.database.models.User

class UserRepository(private val userDao: UserDao) {

    suspend fun getUserById(id: String): User? {
        return userDao.getUserById(id)
    }

    suspend fun getUserByUsername(username: String): User? {
        return userDao.getUserByUsername(username)
    }

    suspend fun addUser(user: User): Long {
        return userDao.insertUser(user)
    }

    suspend fun updateUser(user: User): Int {
        return userDao.updateUser(user)
    }

    suspend fun deleteUser(user: User): Int {
        return userDao.deleteUser(user)
    }
}