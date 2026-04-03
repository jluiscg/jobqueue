package com.lcortes.jobqueue.repository;

import com.lcortes.jobqueue.domain.Job;
import com.lcortes.jobqueue.domain.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JobRepository extends JpaRepository<Job, UUID> {

    /**
     * Atomically claims the next available, highest-priority job using PostgreSQL's SKIP LOCKED.
     * Higher priority jobs (higher integer value) are claimed first. FIFO ordering applies
     * for jobs with the exact same priority.
     *
     * @param workerId    The unique ID of the worker claiming the job.
     * @param lockMinutes How long the lock is valid before a stale-lock recovery kicks in.
     * @return The claimed Job, or empty if no jobs are available.
     */
    // IDEs will warn that UPDATE requires @Modifying. Do NOT add it.
    // The RETURNING * clause causes PostgreSQL to return a ResultSet.
    // If @Modifying is added, Spring will expect an integer return type and crash.
    @Transactional
    @Query(value = """
        UPDATE jobs
        SET status          = 'RUNNING',
            locked_by       = :workerId,
            lock_expires_at = NOW() + make_interval(mins => :lockMinutes),
            started_at      = NOW(),
            attempt_count   = attempt_count + 1
        WHERE id = (
            SELECT id FROM jobs
            WHERE status = 'QUEUED'
              AND (run_at IS NULL OR run_at <= NOW())
            ORDER BY priority DESC, created_at ASC
            LIMIT 1
            FOR UPDATE SKIP LOCKED
        )
        RETURNING *
        """, nativeQuery = true)
    Optional<Job> claimNextJob(@Param("workerId") String workerId, @Param("lockMinutes") int lockMinutes);

    /**
     * Finds jobs with optional filtering by status and type.
     * Uses JPQL so Hibernate can handle the pagination automatically.
     */
    @Query("SELECT j FROM Job j WHERE " +
            "(:status IS NULL OR j.status = :status) AND " +
            "(:type IS NULL OR j.type = :type)")
    Page<Job> findWithFilters(@Param("status") JobStatus status, @Param("type") String type, Pageable pageable);
}