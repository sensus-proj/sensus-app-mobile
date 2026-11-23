package com.example.sensus.domain.repository

import com.example.sensus.common.Result
import com.example.sensus.domain.model.CreatePostInput
import com.example.sensus.domain.model.FeedComment
import com.example.sensus.domain.model.FeedPolicy
import com.example.sensus.domain.model.FeedPost
import com.example.sensus.domain.model.FeedQuery
import com.example.sensus.domain.model.PagedFeed
import com.example.sensus.domain.model.PagedFeedComments
import com.example.sensus.domain.model.TogglePostLikeResult
import com.example.sensus.domain.model.UploadedMedia

interface FeedRepository {
    suspend fun nearby(query: FeedQuery): Result<PagedFeed>
    suspend fun createPost(input: CreatePostInput, media: UploadedMedia?): Result<FeedPost>
    suspend fun toggleLike(postId: String): Result<TogglePostLikeResult>
    suspend fun getComments(postId: String, page: Int = 1, pageSize: Int = FeedPolicy.DEFAULT_PAGE_SIZE): Result<PagedFeedComments>
    suspend fun addComment(postId: String, content: String): Result<FeedComment>
    suspend fun deletePost(postId: String): Result<Unit>
}
