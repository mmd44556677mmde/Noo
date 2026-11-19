package com.etozhesandy.redpanda.features.chat.data.di

import com.etozhesandy.redpanda.features.chat.data.AudioRepositoryImpl
import com.etozhesandy.redpanda.features.chat.data.ChatBookmarkRepositoryImpl
import com.etozhesandy.redpanda.features.chat.data.ChatRepositoryImpl
import com.etozhesandy.redpanda.features.chat.domain.repository.AudioRepository
import com.etozhesandy.redpanda.features.chat.domain.repository.ChatBookmarkRepository
import com.etozhesandy.redpanda.features.chat.domain.repository.ChatRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ChatDataModule {

    @Binds
    @Singleton
    abstract fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository

    @Binds
    abstract fun bindAudioRepository(impl: AudioRepositoryImpl): AudioRepository

    @Binds
    abstract fun bindChatBookmarkRepository(impl: ChatBookmarkRepositoryImpl): ChatBookmarkRepository
}
