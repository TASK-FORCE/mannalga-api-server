package com.taskforce.superinvention.app.domain.club

import com.taskforce.superinvention.app.domain.user.User
import com.taskforce.superinvention.app.model.AppToken
import com.taskforce.superinvention.app.web.dto.kakao.KakaoToken
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import javax.transaction.Transactional

@Service
class ClubService(
        private var clubRepository: ClubRepository,
        private var clubRepositorySupport: ClubRepositorySupport,
        private var clubUserRepository: ClubUserRepository,
        private var clubUserRepositorySupport: ClubUserRepositorySupport
) {
    fun getClubBySeq(seq: Long): Club? {
        return clubRepositorySupport.findBySeq(seq)
    }

    fun getAllClubs(): List<Club>? {
        return clubRepository.findAll()
    }

    fun retrieveClubs(keyword: String): List<Club>? {
        return clubRepositorySupport.findByKeyword(keyword)
    }
}