package com.example.sensus.data

import com.example.sensus.domain.repository.DataRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

typealias DataRepository = com.example.sensus.domain.repository.DataRepository

class DefaultDataRepository : DataRepository {
    override val data: Flow<List<String>> = flow { emit(listOf("Android")) }
}
