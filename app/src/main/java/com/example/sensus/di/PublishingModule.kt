package com.example.sensus.di
import com.example.sensus.data.di.SensusContainer
import com.example.sensus.data.local.db.dao.OfferDao
import com.example.sensus.data.repository.OffersRepositoryImpl
import com.example.sensus.domain.media.ImageCompressor
import com.example.sensus.domain.repository.MediaRepository
import com.example.sensus.domain.repository.OffersRepository
import com.example.sensus.domain.usecase.ChangeOfferStatusUseCase
import com.example.sensus.domain.usecase.CreateOfferUseCase
import com.example.sensus.domain.usecase.GetMerchantInventoryUseCase
import com.example.sensus.domain.usecase.UpdateOfferUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
@Module
@InstallIn(SingletonComponent::class)
object PublishingModule {
    @Provides
    @Singleton
    fun provideOffersRepository(offerDao: OfferDao, container: SensusContainer): OffersRepository =
        OffersRepositoryImpl(offerDao, container.networkClient.offersApi)

    @Provides
    @Singleton
    fun provideMediaRepository(container: SensusContainer): MediaRepository = container.mediaRepository

    @Provides
    @Singleton
    fun provideImageCompressor(container: SensusContainer): ImageCompressor = container.imageCompressor

    @Provides
    @Singleton
    fun provideCreateOfferUseCase(offersRepository: OffersRepository, mediaRepository: MediaRepository, imageCompressor: ImageCompressor): CreateOfferUseCase =
        CreateOfferUseCase(offersRepository, mediaRepository, imageCompressor)

    @Provides
    @Singleton
    fun provideGetMerchantInventoryUseCase(offersRepository: OffersRepository): GetMerchantInventoryUseCase = GetMerchantInventoryUseCase(offersRepository)

    @Provides
    @Singleton
    fun provideUpdateOfferUseCase(offersRepository: OffersRepository, mediaRepository: MediaRepository, imageCompressor: ImageCompressor): UpdateOfferUseCase =
        UpdateOfferUseCase(offersRepository, mediaRepository, imageCompressor)

    @Provides
    @Singleton
    fun provideChangeOfferStatusUseCase(offersRepository: OffersRepository): ChangeOfferStatusUseCase = ChangeOfferStatusUseCase(offersRepository)
}
