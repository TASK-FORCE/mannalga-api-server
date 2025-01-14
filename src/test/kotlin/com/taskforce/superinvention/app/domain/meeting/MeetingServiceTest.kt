package com.taskforce.superinvention.app.domain.meeting

import com.taskforce.superinvention.app.domain.club.Club
import com.taskforce.superinvention.app.domain.club.user.ClubUser
import com.taskforce.superinvention.app.domain.club.user.ClubUserRepository
import com.taskforce.superinvention.app.domain.user.User
import com.taskforce.superinvention.app.web.dto.common.PageDto
import com.taskforce.superinvention.app.web.dto.meeting.MeetingDto
import com.taskforce.superinvention.app.web.dto.meeting.MeetingRequestDto
import com.taskforce.superinvention.common.exception.BizException
import com.taskforce.superinvention.common.exception.club.meeting.MeetingAlreadyApplicationException
import com.taskforce.superinvention.common.exception.club.meeting.MeetingIsClosedException
import com.taskforce.superinvention.common.exception.club.meeting.MeetingMemberOverflowException
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import java.time.LocalDateTime
import java.util.*
import kotlin.properties.Delegates


internal class MeetingServiceTest {

    var clubSeq by Delegates.notNull<Long>()

    var clubUserSeq by Delegates.notNull<Long>()

    var regClubUserSeq by Delegates.notNull<Long>()

    lateinit var club: Club

    lateinit var user: User

    lateinit var clubUser: ClubUser

    lateinit var pageable: Pageable

    lateinit var meeting:Meeting

    lateinit var meetingService: MeetingService

    lateinit var meetingRepository: MeetingRepository

    lateinit var clubUserRepository: ClubUserRepository

    lateinit var meetingApplicationRepository: MeetingApplicationRepository


    @BeforeEach
    fun init() {
        clubSeq = 123123
        clubUserSeq = 6742
        regClubUserSeq = 4444

        club = mockk<Club>(relaxed = true).apply {
            seq = 1231254
            name = "CLUB NAME"
            description = "description"
            maximumNumber = 50
            userCount = 10
        }
        user = mockk<User>(relaxed = true).apply {
            seq = 613
        }
        clubUser = ClubUser(
            club = club,
            user = user,
            isLiked = false
        ).apply {
            seq = regClubUserSeq
            clubUserRoles = mutableSetOf()
        }



        pageable = PageRequest.of(0, 20)

        meeting = Meeting(
                title = "title",
                content = "content",
                startTimestamp = LocalDateTime.parse("1995-12-27T02:00:00"),
                endTimestamp = LocalDateTime.parse("1995-12-27T07:00:00"),
                club = club,
                deleteFlag = false,
                maximumNumber = 20,
                regClubUser = clubUser,
                region = "홍대역 1번출구",
                regionURL = "map.kakao.com/asdas",
                cost = 12000
        ).apply {
            seq = 421
            meetingApplications = listOf()
        }

        meetingRepository = mockk()
        clubUserRepository = mockk()
        meetingApplicationRepository = mockk()
        meetingService = MeetingService(
                meetingRepository = meetingRepository,
                clubUserRepository = clubUserRepository,
                meetingApplicationRepository = meetingApplicationRepository,
                roleService = mockk()
        )
    }

    @Test
    @DisplayName("모임원이 아닌 유저의 만남 조회")
    fun getMeetingsTestWithoutClubUserRole() {
        // given
        every { meetingRepository.getPagedMeetings(clubSeq, pageable) }.returns(PageImpl(listOf(meeting), pageable, 1))

        // when
        val response: PageDto<MeetingDto> = meetingService.getMeetings(clubSeq, pageable, null)

        // then
        assertEquals(1, response.content.size)
    }


    @Test
    @DisplayName("모임원인 유저의 만남 조회")
    fun getMeetingsTestWitClubUserRole() {
        // given
        meeting.meetingApplications = listOf(
                MeetingApplication(
                        clubUser = ClubUser(
                                club = mockk(),
                                user = mockk<User>(relaxed = true).apply {
                                    seq = 61236
                                },
                                isLiked = false
                        ).apply {
                            seq = clubUserSeq
                            user = user
                            clubUserRoles = mutableSetOf()
                        },
                        meeting = meeting,
                        deleteFlag = false
                ).apply {
                    seq = 5
                }
        )

        every { meetingRepository.getPagedMeetings(clubSeq, pageable) }.returns(PageImpl(listOf(meeting), pageable, 1))

        // when
        val response = meetingService.getMeetings(clubSeq, pageable, clubUserSeq)

        // then
        assertEquals(1, response.content.size)
    }

    @Test
    @DisplayName("모임원이며 만남 생성을 한 유저의 만남 조회")
    fun getMeetingsTestWitGenerateMeetingUser() {
        // given
        meeting.meetingApplications = listOf(
                MeetingApplication(
                        clubUser = ClubUser(
                                club = mockk(),
                                user = mockk<User>(relaxed = true).apply {
                                    seq = 61236
                                },
                                isLiked = false
                        ).apply {
                            seq = regClubUserSeq
                            user = user
                            clubUserRoles = mutableSetOf()
                        },
                        meeting = meeting,
                        deleteFlag = false
                ).apply {
                    seq = 5
                }
        )

        every { meetingRepository.getPagedMeetings(clubSeq, pageable) }.returns(PageImpl(listOf(meeting), pageable, 1))

        // when
        val response = meetingService.getMeetings(clubSeq, pageable, regClubUserSeq)

        // then
        assertEquals(1, response.content.size)
    }

    @Test
    @DisplayName("모임원이 아닌 유저의 모임 개별건 조회")
    fun getMeetingTestWithoutClubUserRole() {
        // given
        every { meetingRepository.findById(meeting.seq!!) }.returns(Optional.of(meeting))

        // when
        val response = meetingService.getMeeting(meeting.seq!!, clubUserSeq)

        // then
        assertEquals(meeting.seq!!, response.seq)
        assertEquals(meeting.content, response.content)
    }
    
    @Test
    @DisplayName("존재하지 않는 모임 개별건 조회")
    fun getMeetingTestNotFountSeq() {
        // given
        every { meetingRepository.findById(meeting.seq!!) }.returns(Optional.empty())

        // when, then
        assertThrows(BizException::class.java) {
            meetingService.getMeeting(meeting.seq!!, clubUserSeq)
        }
    }

    @Test
    fun `만남생성 및 신청한 유저가 만남신청 요약정보 조회`() {
        // given
        meeting.meetingApplications = listOf(
            MeetingApplication(
                clubUser,
                meeting,
                false
            ).apply { seq = 1234 }
        )
        every { meetingRepository.findById(meeting.seq!!) }.returns(Optional.of(meeting))

        // when
        val meetingApplicationStatus = meetingService.getMeetingApplicationStatus(meetingSeq = meeting.seq!!, clubUser = clubUser)

        // then
        assertEquals(1, meetingApplicationStatus.currentCount)
        assertEquals(20, meeting.maximumNumber)
        assertTrue(meetingApplicationStatus.isCurrentUserRegMeeting)
        assertTrue(meetingApplicationStatus.isCurrentUserApplicationMeeting)
    }

    @Test
    fun `만남생성 및 신청하지 않은 유저가 만남신청 요약정보 조회`() {
        // given
        meeting.meetingApplications = listOf()
        every { meetingRepository.findById(meeting.seq!!) }.returns(Optional.of(meeting))

        // when
        val meetingApplicationStatus = meetingService.getMeetingApplicationStatus(meetingSeq = meeting.seq!!, clubUser = clubUser)

        // then
        assertEquals(0, meetingApplicationStatus.currentCount)
        assertEquals(20, meeting.maximumNumber)
        assertTrue(meetingApplicationStatus.isCurrentUserRegMeeting)
        assertFalse(meetingApplicationStatus.isCurrentUserApplicationMeeting)
    }

    @Test
    fun `만남생성 및 신청 모두 하지않은 유저가 만남신청 요약정보 조회`() {
        // given
        meeting.meetingApplications = listOf(
            MeetingApplication(
                ClubUser(club, user, mockk()).apply { seq = 2222; clubUserRoles = mutableSetOf() },
                meeting,
                false
            ).apply {
                seq = 1234
            }
        )
        meeting.regClubUser = ClubUser(mockk(), mockk(), mockk()).apply { seq = 2222 }
        clubUser.seq = 1111
        every { meetingRepository.findById(meeting.seq!!) }.returns(Optional.of(meeting))

        // when
        val meetingApplicationStatus = meetingService.getMeetingApplicationStatus(meetingSeq = meeting.seq!!, clubUser = clubUser)

        // then
        assertEquals(1, meetingApplicationStatus.currentCount)
        assertEquals(20, meeting.maximumNumber)
        assertFalse(meetingApplicationStatus.isCurrentUserRegMeeting)
        assertFalse(meetingApplicationStatus.isCurrentUserApplicationMeeting)
    }

    @Test
    fun `모임원이 아닌 유저의 만남 생성은 실패해야한다`() {
        // given
        val meetingRequestDto = mockk<MeetingRequestDto>()
        val clubUserSeq = 6592L
        every { clubUserRepository.findById(clubUserSeq) }.returns(Optional.empty())

        // when & then
        assertThrows<BizException>("존재하지 않는 모임원입니다"){ meetingService.createMeeting(meetingRequestDto, clubUserSeq) }
    }
    
    @Test
    fun `활성상태가 아닌 만남에 만남신청시 실패해야 한다`() {
        // given
        meeting.endTimestamp = LocalDateTime.now().minusDays(1)
        every { meetingRepository.findById(meeting.seq!!) }.returns(Optional.of(meeting))

        // when & then
        assertThrows<MeetingIsClosedException> { meetingService.application(clubUser, meeting.seq!!) }
    }

    @Test
    fun `신청자가 꽉 찬 만남에 만남신청을 할 경우 실패해야 한다`() {
        // given
        meeting.endTimestamp = LocalDateTime.now().plusDays(1)      // 만남 활성화
        every { meetingRepository.findById(meeting.seq!!) }.returns(Optional.of(meeting))
        meeting.maximumNumber = 3
        meeting.meetingApplications = listOf(
            MeetingApplication(clubUser = mockk<ClubUser>(relaxed = true).apply { seq = 111 }, meeting = meeting, deleteFlag = false),
            MeetingApplication(clubUser = mockk<ClubUser>(relaxed = true).apply { seq = 222 }, meeting = meeting, deleteFlag = false),
            MeetingApplication(clubUser = mockk<ClubUser>(relaxed = true).apply { seq = 333 }, meeting = meeting, deleteFlag = false),
        )

        // when & then
        assertThrows<MeetingMemberOverflowException> { meetingService.application(clubUser, meetingSeq = meeting.seq!!) }
    }

    @Test
    fun `이미 신청한 만남에 만남신청을 할 경우 실패해야 한다`() {
        // given
        meeting.endTimestamp = LocalDateTime.now().plusDays(1)      // 만남 활성화
        every { meetingRepository.findById(meeting.seq!!) }.returns(Optional.of(meeting))
        meeting.maximumNumber = 10
        val alreadyApplicationdMeetingApplication = MeetingApplication(
            clubUser = mockk<ClubUser>(relaxed = true).apply { seq = clubUser.seq!! },
            meeting = meeting,
            deleteFlag = false
        )
        meeting.meetingApplications = listOf(
            alreadyApplicationdMeetingApplication
        )
        every { meetingApplicationRepository.findByClubUserAndMeeting(any(), any()) }.returns(alreadyApplicationdMeetingApplication)

        // when & then
        assertThrows<MeetingAlreadyApplicationException> { meetingService.application(clubUser, meetingSeq = meeting.seq!!) }
    }
    

}