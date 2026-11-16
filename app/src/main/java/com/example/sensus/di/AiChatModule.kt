package com.example.sensus.di

import com.example.sensus.data.di.SensusContainer
import com.example.sensus.domain.repository.AiChatRepository
import com.example.sensus.domain.usecase.LoadAiConversationUseCase
import com.example.sensus.domain.usecase.SendSensiMessageUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AiChatModule {

    @Provides
    @Singleton
    fun provideAiChatRepository(container: SensusContainer): AiChatRepository {
        return container.aiChatRepository
    }

    @Provides
    @Singleton
    fun provideSendSensiMessageUseCase(container: SensusContainer): SendSensiMessageUseCase {
        return container.sendSensiMessageUseCase
    }

    @Provides
    @Singleton
    fun provideLoadAiConversationUseCase(container: SensusContainer): LoadAiConversationUseCase {
        return container.loadAiConversationUseCase
    }
}
