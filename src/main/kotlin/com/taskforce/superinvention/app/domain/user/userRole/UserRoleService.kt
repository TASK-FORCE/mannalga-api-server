package com.taskforce.superinvention.app.domain.user.userRole

import com.taskforce.superinvention.app.domain.user.user.User
import org.springframework.stereotype.Service

@Service
class UserRoleService(
        val userRoleRepository: UserRoleRepository
) {

    fun addRoleToUser(user: User, roleName: String) {
        userRoleRepository.save(UserRole(user, "ROLE_$roleName"))
    }
}