package com.example.chaskirider.data.session

import com.example.chaskirider.data.notifications.RiderTopicSubscription
import com.example.chaskirider.domain.model.RiderUser
import com.example.chaskirider.domain.repository.RiderSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class RiderSessionStore(private val topics: RiderTopicSubscription = RiderTopicSubscription()) : RiderSession {
    private val current = MutableStateFlow<RiderUser?>(null)
    override val user = current.asStateFlow()
    fun update(user: RiderUser?) {
        topics.bind(user?.id)
        current.value = user
    }
}
