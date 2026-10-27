package com.careerpilot.repository;

import com.careerpilot.dto.StatusCount;
import com.careerpilot.model.JobApplication;
import com.careerpilot.model.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Every query takes an ownerId, so one user can never read another user's applications.
 * JPQL queries use entity and field names (JobApplication, owner.id), not table and column names.
 */
public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {

    /** Newest first; saved-only jobs (no application date) go last. */
    @Query("""
            select a from JobApplication a
            where a.owner.id = :ownerId
            order by a.applicationDate desc nulls last, a.createdAt desc
            """)
    List<JobApplication> findAllForOwner(@Param("ownerId") Long ownerId);

    // Derived query: "OwnerId" walks the relationship to owner.id
    List<JobApplication> findByOwnerIdAndStatusOrderByApplicationDateDesc(Long ownerId, Status status);

    /** The ownership check: returns empty if the id exists but belongs to someone else. */
    Optional<JobApplication> findByIdAndOwnerId(Long id, Long ownerId);

    long countByOwnerId(Long ownerId);

    /** GROUP BY runs in the database; each row maps onto the StatusCount projection. */
    @Query("""
            select a.status as status, count(a) as total
            from JobApplication a
            where a.owner.id = :ownerId
            group by a.status
            """)
    List<StatusCount> countByStatus(@Param("ownerId") Long ownerId);

    @Query("""
            select a from JobApplication a
            where a.owner.id = :ownerId
              and a.interviewDate >= :today
            order by a.interviewDate
            """)
    List<JobApplication> findUpcomingInterviews(@Param("ownerId") Long ownerId,
                                                @Param("today") LocalDate today);

    @Query("""
            select a from JobApplication a
            where a.owner.id = :ownerId
              and a.followUpDate < :today
              and a.status not in :closedStatuses
            order by a.followUpDate
            """)
    List<JobApplication> findOverdueFollowUps(@Param("ownerId") Long ownerId,
                                              @Param("today") LocalDate today,
                                              @Param("closedStatuses") Collection<Status> closedStatuses);
}