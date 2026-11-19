package com.etozhesandy.redpanda.features.profile.data

import com.etozhesandy.redpanda.core.common.dispatcher.DefaultDispatcher
import com.etozhesandy.redpanda.core.model.Attachment
import com.etozhesandy.redpanda.core.model.AttachmentType
import com.etozhesandy.redpanda.core.model.Friend
import com.etozhesandy.redpanda.core.model.Group
import com.etozhesandy.redpanda.core.model.Profile
import com.etozhesandy.redpanda.core.model.SavedPhoto
import com.etozhesandy.redpanda.core.storage.db.attachment.AttachmentDao
import com.etozhesandy.redpanda.core.storage.db.attachment.toDomain
import com.etozhesandy.redpanda.core.storage.db.friend.FriendDao
import com.etozhesandy.redpanda.core.storage.db.friend.toDomain
import com.etozhesandy.redpanda.core.storage.db.group.GroupDao
import com.etozhesandy.redpanda.core.storage.db.group.toDomain
import com.etozhesandy.redpanda.core.storage.db.profile.ProfileDao
import com.etozhesandy.redpanda.core.storage.db.profile.toDomain
import com.etozhesandy.redpanda.core.storage.db.savedphoto.SavedPhotoDao
import com.etozhesandy.redpanda.core.storage.db.savedphoto.toDomain
import com.etozhesandy.redpanda.features.profile.data.mapper.toDomain
import com.etozhesandy.redpanda.features.profile.domain.model.ArchiveFolder
import com.etozhesandy.redpanda.features.profile.domain.repository.ProfileInfoRepository
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class ProfileInfoRepositoryImpl @Inject constructor(
    private val profileDao: ProfileDao,
    private val friendDao: FriendDao,
    private val groupDao: GroupDao,
    private val savedPhotoDao: SavedPhotoDao,
    private val attachmentDao: AttachmentDao,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : ProfileInfoRepository {

    override fun observeProfile(profileId: String): Flow<Profile?> =
        profileDao.observeProfile(profileId).map { it?.toDomain() }.flowOn(defaultDispatcher)

    override suspend fun setCustomName(profileId: String, name: String?) =
        profileDao.updateCustomName(profileId, name)

    override suspend fun setNotes(profileId: String, notes: String) = profileDao.updateNotes(profileId, notes)

    override fun observeFriends(profileId: String): Flow<List<Friend>> =
        friendDao.observeFriends(profileId)
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(defaultDispatcher)

    override fun observeGroups(profileId: String): Flow<List<Group>> =
        groupDao.observeGroups(profileId)
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(defaultDispatcher)

    override fun observeSavedPhotos(profileId: String): Flow<List<SavedPhoto>> =
        savedPhotoDao.observeSavedPhotos(profileId)
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(defaultDispatcher)

    override fun observeAttachments(profileId: String): Flow<List<Attachment>> =
        attachmentDao.observeByTypesForProfile(profileId, listOf(AttachmentType.PHOTO))
            .map { entities -> entities.map { it.toDomain() } }
            .distinctUntilChanged()
            .flowOn(defaultDispatcher)

    override fun observeAttachmentsPreview(profileId: String, limit: Int): Flow<List<Attachment>> =
        attachmentDao.observeByTypesForProfilePreview(profileId, listOf(AttachmentType.PHOTO), limit)
            .map { entities -> entities.map { it.toDomain() } }
            .distinctUntilChanged()
            .flowOn(defaultDispatcher)

    override fun observeAttachmentsCount(profileId: String): Flow<Int> =
        attachmentDao.observeCountByTypesForProfile(profileId, listOf(AttachmentType.PHOTO))
            .distinctUntilChanged()

    override fun observeArchiveFilesPreview(profileId: String, limit: Int): Flow<List<Attachment>> =
        attachmentDao.observeArchiveFilesPreviewForProfile(profileId, limit)
            .map { entities -> entities.map { it.toDomain() } }
            .distinctUntilChanged()
            .flowOn(defaultDispatcher)

    override fun observeArchiveFilesCount(profileId: String): Flow<Int> =
        attachmentDao.observeArchiveFilesCountForProfile(profileId).distinctUntilChanged()

    override fun observeArchiveFiles(profileId: String): Flow<List<Attachment>> =
        attachmentDao.observeArchiveFilesForProfile(profileId)
            .map { entities -> entities.map { it.toDomain() } }
            .distinctUntilChanged()
            .flowOn(defaultDispatcher)

    override fun observeArchiveFolders(profileId: String): Flow<List<ArchiveFolder>> =
        attachmentDao.observeArchiveFolders(profileId)
            .map { summaries -> summaries.map { it.toDomain() } }
            .distinctUntilChanged()
            .flowOn(defaultDispatcher)

    override fun observeArchiveFilesInFolder(profileId: String, folder: String): Flow<List<Attachment>> =
        attachmentDao.observeArchiveFilesInFolder(profileId, folder)
            .map { entities -> entities.map { it.toDomain() } }
            .distinctUntilChanged()
            .flowOn(defaultDispatcher)
}
