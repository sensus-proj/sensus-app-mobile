package com.example.sensus.domain.repository

import com.example.sensus.common.Result
import com.example.sensus.domain.model.CreatePostInput
import com.example.sensus.domain.model.FeedPost
import com.example.sensus.domain.model.FeedQuery
import com.example.sensus.domain.model.PagedFeed
import com.example.sensus.domain.model.UploadedMedia

interface FeedRepository {
    suspend fun nearby(query: FeedQuery): Result<PagedFeed>
    suspend fun createPost(input: CreatePostInput, media: UploadedMedia?): Result<FeedPost>
}
