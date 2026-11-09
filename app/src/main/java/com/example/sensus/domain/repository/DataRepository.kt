package com.example.sensus.domain.repository

import kotlinx.coroutines.flow.Flow

interface DataRepository {
    val data: Flow<List<String>>
}
