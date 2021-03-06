/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.matrix.android.internal.session.room.state

import android.net.Uri
import androidx.lifecycle.LiveData
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.query.QueryStringValue
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.room.model.RoomHistoryVisibility
import im.vector.matrix.android.api.session.room.state.StateService
import im.vector.matrix.android.api.util.Cancelable
import im.vector.matrix.android.api.util.JsonDict
import im.vector.matrix.android.api.util.Optional
import im.vector.matrix.android.internal.session.content.FileUploader
import im.vector.matrix.android.internal.session.room.alias.AddRoomAliasTask
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.task.configureWith
import im.vector.matrix.android.internal.task.launchToCallback
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers

internal class DefaultStateService @AssistedInject constructor(@Assisted private val roomId: String,
                                                               private val stateEventDataSource: StateEventDataSource,
                                                               private val taskExecutor: TaskExecutor,
                                                               private val sendStateTask: SendStateTask,
                                                               private val coroutineDispatchers: MatrixCoroutineDispatchers,
                                                               private val fileUploader: FileUploader,
                                                               private val addRoomAliasTask: AddRoomAliasTask
) : StateService {

    @AssistedInject.Factory
    interface Factory {
        fun create(roomId: String): StateService
    }

    override fun getStateEvent(eventType: String, stateKey: QueryStringValue): Event? {
        return stateEventDataSource.getStateEvent(roomId, eventType, stateKey)
    }

    override fun getStateEventLive(eventType: String, stateKey: QueryStringValue): LiveData<Optional<Event>> {
        return stateEventDataSource.getStateEventLive(roomId, eventType, stateKey)
    }

    override fun getStateEvents(eventTypes: Set<String>, stateKey: QueryStringValue): List<Event> {
        return stateEventDataSource.getStateEvents(roomId, eventTypes, stateKey)
    }

    override fun getStateEventsLive(eventTypes: Set<String>, stateKey: QueryStringValue): LiveData<List<Event>> {
        return stateEventDataSource.getStateEventsLive(roomId, eventTypes, stateKey)
    }

    override fun sendStateEvent(
            eventType: String,
            stateKey: String?,
            body: JsonDict,
            callback: MatrixCallback<Unit>
    ): Cancelable {
        val params = SendStateTask.Params(
                roomId = roomId,
                stateKey = stateKey,
                eventType = eventType,
                body = body
        )
        return sendStateTask
                .configureWith(params) {
                    this.callback = callback
                }
                .executeBy(taskExecutor)
    }

    override fun updateTopic(topic: String, callback: MatrixCallback<Unit>): Cancelable {
        return sendStateEvent(
                eventType = EventType.STATE_ROOM_TOPIC,
                body = mapOf("topic" to topic),
                callback = callback,
                stateKey = null
        )
    }

    override fun updateName(name: String, callback: MatrixCallback<Unit>): Cancelable {
        return sendStateEvent(
                eventType = EventType.STATE_ROOM_NAME,
                body = mapOf("name" to name),
                callback = callback,
                stateKey = null
        )
    }

    override fun addRoomAlias(roomAlias: String, callback: MatrixCallback<Unit>): Cancelable {
        return addRoomAliasTask
                .configureWith(AddRoomAliasTask.Params(roomId, roomAlias)) {
                    this.callback = callback
                }
                .executeBy(taskExecutor)
    }

    override fun updateCanonicalAlias(alias: String, callback: MatrixCallback<Unit>): Cancelable {
        return sendStateEvent(
                eventType = EventType.STATE_ROOM_CANONICAL_ALIAS,
                body = mapOf("alias" to alias),
                callback = callback,
                stateKey = null
        )
    }

    override fun updateHistoryReadability(readability: RoomHistoryVisibility, callback: MatrixCallback<Unit>): Cancelable {
        return sendStateEvent(
                eventType = EventType.STATE_ROOM_HISTORY_VISIBILITY,
                body = mapOf("history_visibility" to readability),
                callback = callback,
                stateKey = null
        )
    }

    override fun updateAvatar(avatarUri: Uri, fileName: String, callback: MatrixCallback<Unit>): Cancelable {
        return taskExecutor.executorScope.launchToCallback(coroutineDispatchers.main, callback) {
            val response = fileUploader.uploadFromUri(avatarUri, fileName, "image/jpeg")
            sendStateEvent(
                    eventType = EventType.STATE_ROOM_AVATAR,
                    body = mapOf("url" to response.contentUri),
                    callback = callback,
                    stateKey = null
            )
        }
    }
}
